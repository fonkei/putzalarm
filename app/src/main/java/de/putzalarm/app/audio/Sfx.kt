package de.putzalarm.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import de.putzalarm.app.R
import kotlin.random.Random

/** Comic-Geräusche: Monster-Rufe, PLOPP und Gongs. Erzeugt von tools/gen_sounds.py. */
class Sfx(context: Context) {
    private val app = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private fun load(res: Int) = pool.load(app, res, 1)

    private val screams = listOf(
        R.raw.scream_iiih, R.raw.scream_aaah, R.raw.scream_uaaah,
        R.raw.scream_wheee, R.raw.scream_waaah, R.raw.scream_eek,
    ).map(::load)
    private val plopp = load(R.raw.plopp)
    private val ding = load(R.raw.ding)
    private val beep = load(R.raw.beep)
    private val go = load(R.raw.go)
    private val combo = load(R.raw.combo)
    private val fanfare = load(R.raw.fanfare)
    private var busyUntil = 0L
    private val words = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Long>>()

    /** Ein gesprochenes Monster-Wort (siehe [Voice.renderScreams]) als Sound übernehmen, mit seiner Länge in ms. */
    fun addWord(text: String, file: java.io.File) {
        words[text] = pool.load(file.path, 1) to wavMillis(file)
    }

    /** Länge einer WAV-Datei aus ihrem Kopf (Bytes pro Sekunde); im Zweifel 1,5 Sekunden. */
    private fun wavMillis(file: java.io.File): Long = runCatching {
        val head = ByteArray(44)
        file.inputStream().use { it.read(head) }
        val byteRate = java.nio.ByteBuffer.wrap(head, 28, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        if (byteRate <= 0) 1500L else (file.length() - 44) * 1000L / byteRate
    }.getOrDefault(1500L)

    /**
     * Ein Monster ruft [text], jedes mit seiner eigenen Stimme: mal tief und langsam, mal hoch und flott.
     * Es redet immer nur eins, und es darf ausreden; danach kurz Pause. Gibt false zurück, wenn gerade
     * ein anderes spricht (dann bekommt das Monster auch keine Sprechblase).
     */
    fun scream(text: String): Boolean {
        val now = System.currentTimeMillis()
        if (now < busyUntil) return false
        val word = words[text]
        if (word != null) {
            val rate = Voices.random()
            play(word.first, 1f, rate)
            busyUntil = now + (word.second / rate).toLong() + 450L
        } else {
            play(screams.random(), 0.6f, 0.95f + Random.nextFloat() * 0.2f)
            busyUntil = now + 900L
        }
        return true
    }

    fun plopp() = play(plopp, 0.6f, 0.85f + Random.nextFloat() * 0.5f)
    fun ding() = play(ding, 0.8f)
    fun beep() = play(beep, 0.8f)
    fun go() = play(go, 0.9f)
    fun combo() = play(combo, 0.7f)
    fun fanfare() = play(fanfare, 0.9f)

    private fun play(id: Int, volume: Float, rate: Float = 1f) {
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()

    private companion object {
        /** Abspieltempo = Stimmlage: tief und gemütlich bis hoch und quietschig. */
        val Voices = listOf(0.72f, 0.85f, 1.0f, 1.15f, 1.3f)
    }
}
