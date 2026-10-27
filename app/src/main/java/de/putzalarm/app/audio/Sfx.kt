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
    private var lastScream = 0L
    private val words = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /** Ein gesprochenes Monster-Wort (siehe [Voice.renderScreams]) als Sound übernehmen. */
    fun addWord(text: String, file: java.io.File) {
        words[text] = pool.load(file.path, 1)
    }

    /**
     * Ein Monster ruft [text] mit kleiner, hoher Stimme. Solange die Wörter noch nicht fertig
     * gesprochen vorliegen, quiekt es stattdessen. Höchstens alle [gapMs] Millisekunden,
     * damit die Rufe verständlich bleiben.
     */
    fun scream(text: String, gapMs: Long = 650) {
        val now = System.currentTimeMillis()
        if (now - lastScream < gapMs) return
        lastScream = now
        val word = words[text]
        if (word != null) play(word, 1f, 1.12f + Random.nextFloat() * 0.18f)
        else play(screams.random(), 0.6f, 0.95f + Random.nextFloat() * 0.2f)
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
}
