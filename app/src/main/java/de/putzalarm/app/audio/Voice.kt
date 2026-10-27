package de.putzalarm.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Sprachausgabe über die Android-Sprachsynthese. Ersetzt Text für Kinder, die noch nicht lesen,
 * und lässt die Monster quieken. Fehlt die Sprachsynthese, bleibt die App einfach stumm.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var lastSqueak = 0L

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.GERMAN)
        ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /** Ansage in normaler Stimme. Unterbricht, was gerade läuft. */
    fun say(text: String) {
        if (!ready) return
        tts.setPitch(1.1f)
        tts.setSpeechRate(1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say")
    }

    /** Hohes, schnelles Quieken eines Monsters, höchstens alle [gapMs] Millisekunden. */
    fun squeak(text: String, gapMs: Long = 1300) {
        if (!ready || tts.isSpeaking) return
        val now = System.currentTimeMillis()
        if (now - lastSqueak < gapMs) return
        lastSqueak = now
        tts.setPitch(2.0f)
        tts.setSpeechRate(1.5f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "squeak")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
