package de.putzalarm.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

/**
 * Sprachansagen. Feste Sätze kommen als fertige Aufnahmen aus assets/stimme (erzeugt mit
 * tools/stimme_saetze.py z. B. mit VoiceStudio), alles andere, z. B. Namen und Zahlen, spricht die Android-Sprachsynthese.
 * Fehlen die Aufnahmen, spricht die Sprachsynthese alles wie früher. Ersetzt Text für Kinder, die noch nicht lesen.
 * Nimmt die natürlichste deutsche Stimme, die das Gerät hat. Fehlt die Sprachsynthese, bleibt die App stumm.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val assets = context.applicationContext.assets
    private val main = Handler(Looper.getMainLooper())

    /** Rolle -> (normalisierter Satz -> Datei unter assets/stimme). Leer, wenn es keine Aufnahmen gibt. */
    private val recorded: Map<String, Map<String, String>> by lazy {
        runCatching {
            val o = org.json.JSONObject(assets.open("stimme/index.json").bufferedReader().use { it.readText() })
            o.keys().asSequence().filter { it != "_meta" }.associateWith { role ->
                val r = o.getJSONObject(role)
                r.keys().asSequence().associateWith { r.getString(it) }
            }
        }.getOrDefault(emptyMap())
    }

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
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = done(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = done(utteranceId)
        })
        pendingClips?.invoke()
        pendingClips = null
    }

    private var pendingClips: (() -> Unit)? = null
    private var speakVoice: android.speech.tts.Voice? = null
    private var monsterVoice: android.speech.tts.Voice? = null

    @Volatile
    private var rendering = false
    @Volatile
    private var renderDone: ((String) -> Unit)? = null

    private fun done(id: String?) {
        if (id == null) return
        if (id.startsWith("clip")) renderDone?.invoke(id)
        else if (id.startsWith("seg")) main.post { if (id == "seg$segId") next() }
    }

    /**
     * Spricht die Monster-Rufe einmal mit hoher Stimme in Dateien, damit sie danach
     * sofort und überlappend als Sound abgespielt werden können.
     */
    fun renderScreams(dir: File, phrases: Map<String, String>, onClip: (String, File) -> Unit) {
        val job = {
            dir.mkdirs()
            // Schon gesprochene Rufe bleiben von Start zu Start liegen; neu gesprochen wird nur, was fehlt.
            // Der Dateiname hängt an Text, Stimme und Stimmlage, damit Änderungen neu gesprochen werden.
            val tag = "${monsterVoice?.name}|1.45|0.92"
            val todo = mutableMapOf<String, Pair<String, File>>()
            phrases.forEach { (shown, spoken) ->
                // Fertige Aufnahmen (mehrere ängstliche Stimmen) haben Vorrang vor der Gerätestimme
                val recs = recorded.filterKeys { it.startsWith("monster") }.values.mapNotNull { it[norm(spoken)] }
                val files = recs.mapNotNull { name ->
                    runCatching {
                        val f = File(dir, "el_" + name.replace('/', '_'))
                        if (f.length() < 500) assets.open("stimme/$name").use { inp -> f.outputStream().use { inp.copyTo(it) } }
                        f
                    }.getOrNull()
                }
                if (files.isNotEmpty()) { files.forEach { onClip(shown, it) }; return@forEach }
                val file = File(dir, "ruf_${"$spoken|$tag".hashCode().toUInt()}.wav")
                if (file.length() > 1000) onClip(shown, file) else todo["clip${todo.size}"] = shown to file
            }
            if (todo.isNotEmpty()) {
                val lastId = "clip${todo.size - 1}"
                renderDone = { id ->
                    todo[id]?.let { (shown, file) -> if (file.length() > 1000) onClip(shown, file) }
                    if (id == lastId) rendering = false
                }
                // Kleine, hohe Stimme, aber natürliche Betonung; die Stimmlage variiert beim Abspielen.
                rendering = true
                monsterVoice?.let { tts.voice = it }
                tts.setPitch(1.45f)
                tts.setSpeechRate(0.92f)
                todo.forEach { (id, v) -> tts.synthesizeToFile(phrases.getValue(v.first), Bundle(), v.second, id) }
                restoreVoice()
            }
        }
        if (ready) job() else pendingClips = job
    }

    private fun restoreVoice() {
        speakVoice?.let { tts.voice = it }
        tts.setPitch(1.0f)
        tts.setSpeechRate(0.95f)
    }

    private val queue = ArrayDeque<Pair<Boolean, String>>() // (Aufnahme?, Datei oder Text)
    private var player: MediaPlayer? = null
    private var segId = 0
    private var sayPitch = 1f

    /**
     * Ansage. Unterbricht, was gerade läuft. [role] wählt die aufgenommene Stimme ("buersti" oder "darth");
     * [pitch] gilt für die Teile, die die Gerätestimme spricht.
     */
    fun say(text: String, pitch: Float = 1f, role: String = "buersti") {
        halt()
        sayPitch = pitch
        val rec = recorded[role].orEmpty()
        val segs = mutableListOf<Pair<Boolean, String>>()
        fun speak(t: String) {
            val last = segs.lastOrNull()
            if (last != null && !last.first) segs[segs.size - 1] = false to (last.second + " " + t) else segs += false to t
        }
        for (sen in sentences(text)) {
            val whole = rec[norm(sen)]
            if (whole != null) { segs += true to whole; continue }
            for (p in parts(sen)) rec[norm(p)]?.let { segs += true to it } ?: speak(p)
        }
        if (segs.none { it.first }) {
            // Keine Aufnahme dabei: wie früher alles in einem Rutsch mit der Gerätestimme
            if (!ready) return
            queue += false to text
        } else queue += segs
        next()
    }

    private fun next() {
        val (isClip, what) = queue.removeFirstOrNull() ?: return
        val id = ++segId
        if (isClip) {
            val ok = runCatching {
                assets.openFd("stimme/$what").use { fd ->
                    player = MediaPlayer().apply {
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        setOnCompletionListener { it.release(); if (player === it) player = null; if (id == segId) next() }
                        prepare()
                        start()
                    }
                }
            }.isSuccess
            if (!ok) next()
        } else {
            if (!ready) { next(); return }
            // Während die Monster-Rufe noch entstehen, nicht dazwischenfunken, sondern hinten anstellen.
            if (sayPitch != 1f) tts.setPitch(sayPitch)
            tts.speak(what, if (rendering) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH, null, "seg$id")
            if (sayPitch != 1f) tts.setPitch(1f)
        }
    }

    private fun halt() {
        queue.clear()
        segId++
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        if (ready && !rendering) tts.stop()
    }

    /** Spricht gerade etwas (z. B. der Schluss-Spruch)? */
    val speaking: Boolean get() = queue.isNotEmpty() || player != null || (ready && tts.isSpeaking)

    fun stop() {
        halt()
        if (ready) tts.stop()
    }

    fun shutdown() {
        halt()
        tts.shutdown()
    }

    companion object {
        // Gleiche Regeln wie tools/stimme_saetze.py (sentences, parts, norm)
        private val Sentence = Regex("[^.!?]+[.!?]*")
        private val Part = Regex("[^,:]+[,:]*")
        fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex(" +"), " ").trim()
        fun sentences(t: String) = Sentence.findAll(t).map { it.value.trim() }.filter { norm(it).isNotEmpty() }.toList()
        fun parts(t: String) = Part.findAll(t).map { it.value.trim() }.filter { norm(it).isNotEmpty() }.toList()
    }
}
