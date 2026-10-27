package de.putzalarm.app.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
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
        // Die Stimme, die in den Android-Einstellungen gewählt ist, hat Vorrang (z. B. eine männliche).
        // Nur wenn dort keine deutsche Stimme eingestellt ist, sucht die App selbst eine aus.
        val system = runCatching { tts.defaultVoice }.getOrNull()
        // Sonst beste Stimme: höchste Qualität, lokal installierte vor Netzwerk-Stimmen, de-DE vor anderen deutschen
        val best = runCatching {
            tts.voices
                ?.filter { it.locale.language == "de" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                ?.maxWithOrNull(
                    compareBy<android.speech.tts.Voice> { it.quality }
                        .thenBy { if (it.isNetworkConnectionRequired) 0 else 1 }
                        .thenBy { if (it.locale.country == "DE") 1 else 0 },
                )
        }.getOrNull()
        speakVoice = if (system != null && system.locale.language == "de") system else best
        // Für die Monster-Rufe lieber eine Frauenstimme (klingt hochgepitcht niedlicher). Android verrät das
        // Geschlecht nicht; bei Google sind „dea“ und „deb“ die Frauenstimmen, sonst nimmt die App eine andere als die Systemstimme.
        monsterVoice = runCatching {
            val de = tts.voices?.filter { it.locale.language == "de" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }.orEmpty()
            de.filter { "-dea-" in it.name || "-deb-" in it.name }.maxByOrNull { if (it.isNetworkConnectionRequired) 0 else 1 }
                ?: best.takeIf { it != speakVoice }
                ?: de.firstOrNull { it != speakVoice }
        }.getOrNull() ?: speakVoice
        speakVoice?.let { tts.voice = it }
        restoreVoice()
        pendingClips?.invoke()
        pendingClips = null
    }

    private var pendingClips: (() -> Unit)? = null
    private var speakVoice: android.speech.tts.Voice? = null
    private var monsterVoice: android.speech.tts.Voice? = null

    @Volatile
    private var rendering = false

    /**
     * Spricht die Monster-Rufe einmal mit hoher Stimme in Dateien, damit sie danach
     * sofort und überlappend als Sound abgespielt werden können.
     */
    fun renderScreams(dir: File, phrases: Map<String, String>, onClip: (String, File) -> Unit) {
        val job = {
            dir.mkdirs()
            val files = mutableMapOf<String, Pair<String, File>>()
            phrases.entries.forEachIndexed { i, (shown, spoken) -> files["clip$i"] = shown to File(dir, "ruf_$i.wav") }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    val (shown, file) = files[utteranceId] ?: return
                    if (file.length() > 1000) onClip(shown, file)
                    if (utteranceId == "clip${phrases.size - 1}") rendering = false
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == "clip${phrases.size - 1}") rendering = false
                }
            })
            // Kleine, hohe Stimme, aber natürliche Betonung; der Rest kommt beim Abspielen dazu.
            rendering = true
            monsterVoice?.let { tts.voice = it }
            tts.setPitch(1.45f)
            tts.setSpeechRate(1.05f)
            phrases.entries.forEachIndexed { i, (_, spoken) ->
                tts.synthesizeToFile(spoken, Bundle(), File(dir, "ruf_$i.wav"), "clip$i")
            }
            restoreVoice()
        }
        if (ready) job() else pendingClips = job
    }

    private fun restoreVoice() {
        speakVoice?.let { tts.voice = it }
        tts.setPitch(1.0f)
        tts.setSpeechRate(0.95f)
    }

    /** Ansage. Unterbricht, was gerade läuft. */
    fun say(text: String) {
        if (!ready) return
        // Während die Monster-Rufe noch entstehen, nicht dazwischenfunken, sondern hinten anstellen.
        tts.speak(text, if (rendering) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH, null, "say")
    }

    /** Spricht gerade etwas (z. B. der Schluss-Spruch)? */
    val speaking: Boolean get() = ready && tts.isSpeaking

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
