package de.putzalarm.app.ui.art

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der neue Super Bürsti als echte 3D-Figur (Skelett, sprechendes Gesicht, Stoff-Umhang).
 * Die Szene liegt in assets/buersti (gebaut mit design/pro/build.py) und läuft in einer durchsichtigen WebView.
 */
object Buersti3D {
    /** false, wenn das Gerät kein 3D kann oder zu langsam ist: dann zeigt die App wieder die fertigen Bilder. */
    var enabled by mutableStateOf(true)
}

/** Sprachausgabe -> Mund von Bürsti. [de.putzalarm.app.audio.Voice] meldet, was gerade gesprochen wird. */
object BuerstiBus {
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<(String) -> Unit>()

    fun listen(l: (String) -> Unit): () -> Unit {
        listeners += l
        return { listeners -= l }
    }

    private fun send(js: String) = main.post { listeners.toList().forEach { it(js) } }

    /** Ein Stück Text beginnt; [durMs] ist die Länge einer Aufnahme (0 = Gerätestimme, die Wortgrenzen meldet). */
    fun segment(text: String, durMs: Int = 0) = send("B.say(${JSONObject.quote(text)},$durMs)")

    /** Die Gerätestimme beginnt das Wort an Stelle [start] im zuletzt gemeldeten Text. */
    fun range(start: Int) = send("B.range($start)")

    fun end() = send("B.end()")
}

private const val TAG = "Buersti3D"

private fun Outfit.toJson(pose: HeroPose?) = JSONObject().apply {
    put("costume", costume.key)
    put("cape", String.format("#%06X", cape.toArgb() and 0xFFFFFF))
    put("hat", gear.firstOrNull { it.slot == GearSlot.HAT }?.key ?: JSONObject.NULL)
    put("glasses", gear.firstOrNull { it.slot == GearSlot.GLASSES }?.key ?: JSONObject.NULL)
    put("moves", JSONArray(moves.ifEmpty { HeroPose.Basic }.map { it.key }))
    put("pose", pose?.key ?: JSONObject.NULL)
}.toString()

/**
 * Bürsti in 3D. Bis die Szene steht, zeigt [fallback] die alten Bilder darunter (die WebView ist so lange durchsichtig).
 * Antippen geht an die App ([onTap]) und lässt ihn eine neue Bewegung machen.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "JavascriptInterface")
@Composable
fun Mascot3D(
    modifier: Modifier,
    pose: HeroPose?,
    look: Outfit,
    onTap: (() -> Unit)?,
    fallback: @Composable (Modifier) -> Unit,
) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val web = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // Berührungen gehen an die App, nicht an die Szene
            setOnTouchListener { _, _ -> true }
            // Meldungen der Szene landen im Geräteprotokoll (adb logcat -s Buersti3D)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    Log.i(TAG, "${m.message()} (${m.lineNumber()})")
                    return true
                }
            }
            Log.i(TAG, "WebView ${WebView.getCurrentWebViewPackage()?.versionName}")
            // Testversionen: vom PC aus per chrome://inspect hineinschauen
            if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
            addJavascriptInterface(object {
                @JavascriptInterface fun ready() { Log.i(TAG, "bereit"); post { ready = true } }
                @JavascriptInterface fun fail() { Log.w(TAG, "kein WebGL"); post { Buersti3D.enabled = false } }
                @JavascriptInterface fun slow() { Log.w(TAG, "zu langsam"); post { Buersti3D.enabled = false } }
                @JavascriptInterface fun log(msg: String) { Log.i(TAG, msg) }
            }, "Android")
            loadUrl("file:///android_asset/buersti/index.html#" + Uri.encode(look.toJson(pose)))
        }
    }
    LaunchedEffect(ready, look, pose) {
        if (ready) web.evaluateJavascript("B.set(${look.toJson(pose)})", null)
    }
    DisposableEffect(web) {
        val off = BuerstiBus.listen { js -> if (ready) web.evaluateJavascript(js, null) }
        onDispose {
            off()
            web.stopLoading()
            web.destroy()
        }
    }
    Box(modifier.aspectRatio(3f / 4f)) {
        if (!ready) fallback(Modifier.fillMaxSize())
        AndroidView({ web }, Modifier.fillMaxSize())
        if (onTap != null) {
            Box(
                Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {
                    if (ready) web.evaluateJavascript("B.poke()", null)
                    onTap()
                },
            )
        }
    }
}
