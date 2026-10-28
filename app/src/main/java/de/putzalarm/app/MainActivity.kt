package de.putzalarm.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.putzalarm.app.audio.Sfx
import de.putzalarm.app.audio.Voice
import de.putzalarm.app.data.ProfileStore
import de.putzalarm.app.ui.theme.PutzalarmTheme

class MainActivity : ComponentActivity() {
    private lateinit var voice: Voice
    private lateinit var sfx: Sfx

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Vollbild wie ein Spiel, und der Bildschirm bleibt beim Putzen an.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        de.putzalarm.app.ui.art.Sprites.init(resources, packageName)
        Thread { de.putzalarm.app.ui.art.Sprites.preload(de.putzalarm.app.ui.art.Costume.GOLD) }.start()
        val store = ProfileStore(applicationContext)
        voice = Voice(this)
        sfx = Sfx(this)
        voice.renderScreams(java.io.File(cacheDir, "rufe"), de.putzalarm.app.ui.screens.BrushGame.Screams) { shown, file ->
            sfx.addWord(shown, file)
        }
        setContent {
            PutzalarmTheme { App(store, voice, sfx) }
        }
    }

    override fun onDestroy() {
        voice.shutdown()
        sfx.release()
        super.onDestroy()
    }
}
