package de.putzalarm.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Sprachansagen über die Android-Sprachsynthese. Ersetzt Text für Kinder, die noch nicht lesen.
 * Nimmt die natürlichste deutsche Stimme, die das Gerät hat. Fehlt die Sprachsynthese, bleibt die App stumm.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.GERMANY)
        ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ready) return
        // Beste Stimme: höchste Qualität, lokal installierte vor Netzwerk-Stimmen, de-DE vor anderen deutschen
        val best = runCatching {
            tts.voices
                ?.filter { it.locale.language == "de" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                ?.maxWithOrNull(
                    compareBy<android.speech.tts.Voice> { it.quality }
                        .thenBy { if (it.isNetworkConnectionRequired) 0 else 1 }
                        .thenBy { if (it.locale.country == "DE") 1 else 0 },
                )
        }.getOrNull()
        if (best != null) tts.voice = best
        tts.setPitch(1.0f)
        tts.setSpeechRate(0.95f)
    }

    /** Ansage. Unterbricht, was gerade läuft. */
    fun say(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
