package de.putzalarm.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import de.putzalarm.app.data.GermColors
import de.putzalarm.app.ui.art.GermKind
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FlyingGerm(
    var x: Float, var y: Float, var vx: Float, var vy: Float, var rot: Float,
    val spin: Float, val size: Float, val color: Color, val kind: GermKind, val seed: Float,
    val life: Float, val scream: String?,
) {
    var age = 0f
}

class Splat(val x: Float, val y: Float, val color: Color, val size: Float, val plopp: Boolean) {
    var age = 0f
    val drops = List(7) { Triple(Random.nextFloat() * 6.283f, 0.5f + Random.nextFloat() * 0.7f, 0.12f + Random.nextFloat() * 0.14f) }
}

enum class Phase { COUNTDOWN, RUN, DONE }

interface GameEvents {
    fun onCountdown(n: Int) {}
    fun onZone(zone: Int) {}
    fun onScream(text: String) {}
    fun onPlopp() {}
    fun onCombo() {}
    fun onIdle() {}
    fun onFinish() {}
}

data class SessionResult(val monsters: Int, val zones: List<Boolean>)

/**
 * Spiellogik des Putz-Bildschirms, unabhängig vom Zeichnen: Countdown, 4 Zonen à ¼ der Zeit,
 * Bakterien fliegen nur, solange wirklich geschrubbt wird, und Dauerschrubben baut eine Combo auf.
 */
class BrushGame(val duration: Int, private val events: GameEvents) {
    var phase by mutableStateOf(Phase.COUNTDOWN); private set
    var countdown by mutableIntStateOf(3); private set
    var secondsLeft by mutableIntStateOf(duration); private set
    var monsters by mutableIntStateOf(0); private set
    var combo by mutableIntStateOf(1); private set
    var zone by mutableIntStateOf(0); private set
    var idle by mutableStateOf(false); private set
    val zoneDone = mutableStateListOf(false, false, false, false)

    /** Wird jedes Bild erhöht, damit die Zeichenfläche neu malt. */
    var frame by mutableLongStateOf(0L); private set

    var elapsed = 0f; private set
    var time = 0f; private set
    val germs = ArrayList<FlyingGerm>()
    val splats = ArrayList<Splat>()

    private val zoneLen = duration / 4f
    private val zoneActive = FloatArray(4)
    private var countdownT = 0f
    private var activeRun = 0f
    private var inactive = 0f
    private var idleHintT = 0f
    private var spawnAcc = 0f

    val result get() = SessionResult(monsters, zoneDone.toList())

    fun step(dt: Float, active: Boolean, mouth: Offset, unitPx: Float, w: Float, h: Float) {
        time += dt
        when (phase) {
            Phase.COUNTDOWN -> {
                countdownT += dt
                val n = (3 - countdownT.toInt()).coerceAtLeast(0)
                if (n != countdown) { countdown = n; events.onCountdown(n) }
                if (countdownT >= 3.7f) { phase = Phase.RUN; events.onZone(0) }
            }
            Phase.RUN -> {
                elapsed += dt
                val z = (elapsed / zoneLen).toInt().coerceAtMost(3)
                if (z != zone) {
                    zoneDone[zone] = zoneActive[zone] >= zoneLen * 0.4f
                    zone = z
                    events.onZone(z)
                }
                secondsLeft = ceil(duration - elapsed).toInt().coerceAtLeast(0)
                if (active) {
                    zoneActive[zone] += dt
                    activeRun += dt
                    inactive = 0f
                } else {
                    inactive += dt
                    if (inactive > 1f) activeRun = 0f
                }
                val newCombo = 1 + (activeRun / 6f).toInt().coerceAtMost(4)
                if (newCombo > combo) events.onCombo()
                combo = newCombo
                idle = inactive > 3f
                if (idle) {
                    idleHintT += dt
                    if (idleHintT > 6f) { idleHintT = 0f; events.onIdle() }
                } else idleHintT = 4f
                if (active) {
                    spawnAcc += dt
                    val every = 0.42f / (0.7f + 0.3f * combo)
                    while (spawnAcc >= every) {
                        spawnAcc -= every
                        spawn(mouth, unitPx)
                    }
                }
                if (elapsed >= duration) {
                    zoneDone[zone] = zoneActive[zone] >= zoneLen * 0.4f
                    secondsLeft = 0
                    idle = false
                    phase = Phase.DONE
                    events.onFinish()
                }
            }
            Phase.DONE -> Unit
        }
        animate(dt, unitPx, w, h)
        frame++
    }

    private fun spawn(mouth: Offset, u: Float) {
        val side = Random.nextFloat() < 0.2f
        val deg = if (side) (if (Random.nextBoolean()) Random.nextFloat() * 35f - 5f else 185f - Random.nextFloat() * 35f)
        else -170f + Random.nextFloat() * 160f
        val a = Math.toRadians(deg.toDouble()).toFloat()
        val speed = (26f + Random.nextFloat() * 22f) * u
        val scream = if (Random.nextFloat() < 0.35f) Screams.random() else null
        germs += FlyingGerm(
            x = mouth.x + (Random.nextFloat() - 0.5f) * 3f * u,
            y = mouth.y + (Random.nextFloat() - 0.5f) * 1.5f * u,
            vx = cos(a) * speed, vy = sin(a) * speed,
            rot = Random.nextFloat() * 40f - 20f, spin = Random.nextFloat() * 160f - 80f,
            size = (6f + Random.nextFloat() * 3.5f) * u,
            color = GermColors.random(), kind = GermKind.entries.random(), seed = Random.nextFloat() * 6f,
            life = 1.3f + Random.nextFloat() * 1.1f, scream = scream,
        )
        monsters++
        if (scream != null) events.onScream(scream)
    }

    private fun animate(dt: Float, u: Float, w: Float, h: Float) {
        val drag = 1f - 0.7f * dt
        val iter = germs.iterator()
        while (iter.hasNext()) {
            val g = iter.next()
            g.age += dt
            g.x += g.vx * dt
            g.y += g.vy * dt
            g.vx *= drag
            g.vy = g.vy * drag + 4f * u * dt
            g.rot += g.spin * dt
            val gone = g.x < -g.size || g.x > w + g.size || g.y < -g.size || g.y > h + g.size
            if (gone) iter.remove()
            else if (g.age >= g.life) {
                val s = Splat(g.x, g.y, g.color, g.size, plopp = Random.nextFloat() < 0.4f)
                splats += s
                if (s.plopp) events.onPlopp()
                iter.remove()
            }
        }
        splats.forEach { s -> s.age += dt }
        splats.removeAll { s -> s.age > 0.55f }
    }

    companion object {
        val Screams = listOf("Iiiih!", "Hilfe!", "Neeein!", "Aaah!", "Uaaah!", "Nicht die Bürste!", "Wir hauen ab!", "Mamaaa!", "Huch!")
        val ZoneLines = listOf("Los! Erst oben!", "Jetzt die andere Seite oben!", "Jetzt unten!", "Und jetzt die andere Seite unten!")
    }
}
