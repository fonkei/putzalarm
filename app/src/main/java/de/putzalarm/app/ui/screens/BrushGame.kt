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
import de.putzalarm.app.data.Giant
import de.putzalarm.app.data.Monster
import de.putzalarm.app.ui.art.Pattern
import de.putzalarm.app.ui.art.GermKind
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FlyingGerm(
    var x: Float, var y: Float, var vx: Float, var vy: Float, var rot: Float,
    val spin: Float, val size: Float, val color: Color, val kind: GermKind, val seed: Float,
    val life: Float, val scream: String?,
    /** Tiefe: > 0 fliegt auf die Kamera zu (wird größer), < 0 fliegt nach hinten weg (wird kleiner). */
    val vz: Float = 0f,
    /** Ein Monster aus dem eigenen Album (mit Muster und, wenn selten, Krone), sonst null. */
    val album: Monster? = null,
) {
    val pattern: Pattern get() = album?.pattern ?: Pattern.PLAIN
    var age = 0f

    /** Größenfaktor durch die Tiefe, 1 = auf Höhe des Mundes. */
    var z = 1f
}

class Splat(val x: Float, val y: Float, val color: Color, val size: Float, val plopp: Boolean) {
    var age = 0f
    val drops = List(7) { Triple(Random.nextFloat() * 6.283f, 0.5f + Random.nextFloat() * 0.7f, 0.12f + Random.nextFloat() * 0.14f) }
}

enum class Phase { COUNTDOWN, RUN, DONE }

/** Riesen-Boss während des Putzens: kommt angeflogen, kämpft, wird besiegt oder haut am Ende ab. */
enum class GiantPhase { WAITING, ARRIVING, FIGHT, BEATEN, ESCAPED }

/** Ein Stern, der vom Mund zum Riesen-Boss fliegt. [t] läuft von 0 bis 1. */
class Shot(val from: Offset, var t: Float = 0f)

interface GameEvents {
    fun onCountdown(n: Int) {}
    fun onZone(zone: Int) {}
    /** Ein Monster will [text] rufen; false, wenn gerade ein anderes redet. */
    fun onScream(text: String): Boolean = false
    fun onPlopp() {}
    fun onCombo() {}
    fun onIdle() {}
    fun onFinish() {}
    fun onGiantArrive(giant: Giant) {}
    fun onGiantHit() {}
    fun onGiantBeaten(giant: Giant) {}
}

/** [zoneStars]: 0 bis 3 Sterne je Kieferzone, je nachdem, wie lange dort wirklich geschrubbt wurde. */
data class SessionResult(
    val monsters: Int, val zones: List<Boolean>, val zoneStars: List<Int> = zones.map { if (it) 2 else 0 },
    /** Wie oft welches Album-Monster wieder verjagt wurde (Id → Anzahl). */
    val chased: Map<Int, Int> = emptyMap(),
    /** Riesen-Boss dieser Runde (Id) und ob er besiegt wurde. */
    val giant: Int? = null,
    val giantBeaten: Boolean = false,
) {
    val stars get() = zoneStars.sum()
}

/** Sterne für eine Zone aus dem Anteil der Zeit, in der geschrubbt wurde. Ab 2 Sternen gilt sie als sauber. */
fun starsFor(activeShare: Float): Int = when {
    activeShare >= 0.7f -> 3
    activeShare >= 0.4f -> 2
    activeShare >= 0.15f -> 1
    else -> 0
}

/**
 * Spiellogik des Putz-Bildschirms, unabhängig vom Zeichnen: Countdown, 4 Zonen à ¼ der Zeit,
 * Bakterien fliegen nur, solange wirklich geschrubbt wird, und Dauerschrubben baut eine Combo auf.
 */
class BrushGame(
    val duration: Int,
    private val events: GameEvents,
    /** Album-Monster des Kindes: ein Teil der herausfliegenden Monster sind diese und steigen dadurch auf. */
    private val album: List<Monster> = emptyList(),
    /** Riesen-Boss, der in dieser Runde auftaucht, oder null. */
    val giant: Giant? = null,
) {
    var phase by mutableStateOf(Phase.COUNTDOWN); private set
    var countdown by mutableIntStateOf(3); private set
    var secondsLeft by mutableIntStateOf(duration); private set
    var monsters by mutableIntStateOf(0); private set
    var combo by mutableIntStateOf(1); private set
    var zone by mutableIntStateOf(0); private set
    var idle by mutableStateOf(false); private set
    val zoneDone = mutableStateListOf(false, false, false, false)
    val zoneStars = mutableStateListOf(0, 0, 0, 0)

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

    /** Wie oft welches Album-Monster in dieser Runde verjagt wurde. */
    private val chased = HashMap<Int, Int>()

    // Riesen-Boss: taucht zu Beginn von Zone 2 oder 3 auf; besiegt ist er nach [giantHpMax] Sekunden kräftigem Schrubben
    var giantPhase by mutableStateOf(GiantPhase.WAITING); private set
    val giantHpMax = duration * 0.2f
    var giantHp = giantHpMax; private set
    /** Zeit in der aktuellen Boss-Phase, Treffer-Wackeln (0 bis 1) und Lage auf dem Bildschirm. */
    var giantT = 0f; private set
    var giantHit = 0f; private set
    var giantPos = Offset.Zero; private set
    val shots = ArrayList<Shot>()
    private val giantZone = if (Random.nextBoolean()) 1 else 2
    private var shotAcc = 0f
    private var hitSoundT = 0f

    val result get() = SessionResult(
        monsters, zoneDone.toList(), zoneStars.toList(), chased.toMap(),
        giant?.id?.takeIf { giantPhase != GiantPhase.WAITING }, giantPhase == GiantPhase.BEATEN,
    )

    /** [intensity]: wie kräftig gerade geschrubbt wird (0 bis 1); davon hängt ab, wie viele Monster herausfliegen. */
    fun step(dt: Float, active: Boolean, intensity: Float, mouth: Offset, unitPx: Float, w: Float, h: Float) {
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
                    closeZone()
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
                // Jedes herausfliegende Monster zählt. Wie viele es pro Sekunde sind, hängt davon ab, wie kräftig
                // geschrubbt wird (0,6 bis 3 pro Sekunde) und von der Combo; so ist die Zahl jedes Mal anders.
                if (active) {
                    spawnAcc += dt * (0.6f + 2.4f * intensity.coerceIn(0f, 1f)) * (0.8f + 0.12f * combo)
                    while (spawnAcc >= 1f) {
                        spawnAcc -= 1f
                        spawn(mouth, unitPx)
                    }
                }
                stepGiant(dt, active, mouth, unitPx, w, h)
                if (elapsed >= duration) {
                    if (giantPhase == GiantPhase.FIGHT || giantPhase == GiantPhase.ARRIVING) { giantPhase = GiantPhase.ESCAPED; giantT = 0f }
                    closeZone()
                    secondsLeft = 0
                    idle = false
                    phase = Phase.DONE
                    events.onFinish()
                }
            }
            Phase.DONE -> if (giantPhase == GiantPhase.BEATEN || giantPhase == GiantPhase.ESCAPED) giantT += dt
        }
        animate(dt, unitPx, w, h)
        frame++
    }

    private fun stepGiant(dt: Float, active: Boolean, mouth: Offset, u: Float, w: Float, h: Float) {
        val g = giant ?: return
        // Er schwebt auf der Seite, auf der das Gesicht gerade nicht ist
        val target = Offset(if (mouth.x < w / 2) w * 0.75f else w * 0.25f, h * 0.36f)
        giantPos = if (giantPos == Offset.Zero) target else giantPos + (target - giantPos) * (dt * 1.5f).coerceAtMost(1f)
        giantHit = (giantHit - dt * 3f).coerceAtLeast(0f)
        when (giantPhase) {
            GiantPhase.WAITING -> if (zone >= giantZone && elapsed < duration - 8f) {
                giantPhase = GiantPhase.ARRIVING; giantT = 0f; events.onGiantArrive(g)
            }
            GiantPhase.ARRIVING -> { giantT += dt; if (giantT > 1.6f) { giantPhase = GiantPhase.FIGHT; giantT = 0f } }
            GiantPhase.FIGHT -> {
                giantT += dt
                if (active) {
                    giantHp -= dt * (0.85f + 0.15f * combo)
                    // Sterne fliegen vom Mund zum Boss
                    shotAcc += dt
                    if (shotAcc > 0.3f) { shotAcc = 0f; shots += Shot(mouth) }
                }
                if (giantHp <= 0f) { giantHp = 0f; giantPhase = GiantPhase.BEATEN; giantT = 0f; shots.clear(); events.onGiantBeaten(g) }
            }
            GiantPhase.BEATEN, GiantPhase.ESCAPED -> giantT += dt
        }
        val shotIter = shots.iterator()
        while (shotIter.hasNext()) {
            val s = shotIter.next()
            s.t += dt * 2.2f
            if (s.t >= 1f) {
                shotIter.remove()
                giantHit = 1f
                hitSoundT -= 1f
                if (hitSoundT <= 0f) { hitSoundT = 3f; events.onGiantHit() }
            }
        }
    }

    private fun closeZone() {
        zoneStars[zone] = starsFor(zoneActive[zone] / zoneLen)
        zoneDone[zone] = zoneStars[zone] >= 2
    }

    private fun spawn(mouth: Offset, u: Float) {
        val side = Random.nextFloat() < 0.2f
        val deg = if (side) (if (Random.nextBoolean()) Random.nextFloat() * 35f - 5f else 185f - Random.nextFloat() * 35f)
        else -170f + Random.nextFloat() * 160f
        val a = Math.toRadians(deg.toDouble()).toFloat()
        // Manche fliegen auf die Kamera zu oder nach hinten weg, statt nur zur Seite
        val r = Random.nextFloat()
        val vz = when {
            r < 0.07f -> 1.0f + Random.nextFloat() * 0.5f
            r < 0.17f -> -(0.7f + Random.nextFloat() * 0.4f)
            else -> 0f
        }
        val speed = (26f + Random.nextFloat() * 22f) * u * when { vz > 0f -> 0.6f; vz < 0f -> 0.35f; else -> 1f }
        val scream = if (Random.nextFloat() < 0.35f) Screams.keys.random().takeIf { events.onScream(it) } else null
        // Etwa jedes dritte Monster ist ein alter Bekannter aus dem Album
        val pick = if (album.isNotEmpty() && Random.nextFloat() < 0.35f) album.random() else null
        germs += FlyingGerm(
            x = mouth.x + (Random.nextFloat() - 0.5f) * 3f * u,
            y = mouth.y + (Random.nextFloat() - 0.5f) * 1.5f * u,
            vx = cos(a) * speed, vy = sin(a) * speed,
            rot = Random.nextFloat() * 40f - 20f, spin = Random.nextFloat() * 160f - 80f,
            size = (6f + Random.nextFloat() * 3.5f) * u,
            color = pick?.color ?: GermColors.random(), kind = pick?.kind ?: GermKind.entries.random(), seed = Random.nextFloat() * 6f,
            life = if (vz > 0f) 9f else 1.3f + Random.nextFloat() * 1.1f, scream = scream, vz = vz, album = pick,
        )
        if (pick != null) chased[pick.id] = (chased[pick.id] ?: 0) + 1
        monsters++
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
            // Perspektive: Größe wächst/schrumpft gleichmäßig, wer näher kommt, driftet auch schneller nach außen
            if (g.vz != 0f) {
                g.z *= 1f + g.vz * dt
                if (g.vz > 0f) { g.vx *= 1f + g.vz * dt * 0.5f; g.vy *= 1f + g.vz * dt * 0.5f }
            }
            val sz = g.size * g.z
            val gone = g.x < -sz || g.x > w + sz || g.y < -sz || g.y > h + sz || g.z < 0.22f
            if (g.vz > 0f && g.z > 4.2f) {
                // Klatscht direkt vor der Kamera an die Scheibe
                splats += Splat(g.x, g.y, g.color, sz.coerceAtMost(22f * u), plopp = true)
                events.onPlopp()
                iter.remove()
            } else if (gone) iter.remove()
            else if (g.age >= g.life) {
                val s = Splat(g.x, g.y, g.color, g.size * g.z, plopp = Random.nextFloat() < 0.4f)
                splats += s
                if (s.plopp) events.onPlopp()
                iter.remove()
            }
        }
        splats.forEach { s -> s.age += dt }
        splats.removeAll { s -> s.age > 0.55f }
    }

    companion object {
        /** Was die Monster rufen: Sprechblasen-Text und, wie die Sprachausgabe es sprechen soll. */
        val Screams = linkedMapOf(
            "Hilfe!" to "Hilfe!",
            "Mamaaa!" to "Mamaa!",
            "Aaaah!" to "Aaaaaaa!",
            "Nein, nein, nein!" to "Nein, nein, nein!",
            "Iiiih!" to "Iiiiiii!",
            "Wir hauen ab!" to "Wir hauen ab!",
            "Nicht die Bürste!" to "Nicht die Bürste!",
            "Rettet euch!" to "Rettet euch!",
            "Uaaah!" to "Uaaaaa!",
            "Rette sich, wer kann!" to "Rette sich, wer kann!",
            "Alle raus hier!" to "Alle raus hier!",
            "Die Bürste kommt!" to "Die Bürste kommt!",
            "Ich will zu Mama!" to "Ich will zu Mama!",
            "Schaum! Überall Schaum!" to "Schaum! Überall Schaum!",
            "Oh nein, Zahnpasta!" to "Oh nein, Zahnpasta!",
            "Das kitzelt!" to "Das kitzelt!",
            "Weg hier!" to "Weg hier!",
            "Ich war das nicht!" to "Ich war das nicht!",
            "Nicht so doll!" to "Nicht so doll!",
            "Ich komm wieder!" to "Ich komm wieder!",
            "Wir brauchen Zucker!" to "Wir brauchen Zucker!",
            "Tschüss, Zahn!" to "Tschüss, Zahn!",
            "Mein schönes Versteck!" to "Mein schönes Versteck!",
            "Zu sauber hier!" to "Zu sauber hier!",
            "Oh! Haaland!" to "Oh! Haaland!",
            "Ihhhh Zahnpasta!\nDavon wird mir schlecht!\nNix wie weg hier!" to "Iiiiih, Zahnpasta! Davon wird mir schlecht! Nix wie weg hier!",
            "Wer hat das Licht angemacht?" to "Wer hat das Licht angemacht?",
            "Das ist nicht fair!" to "Das ist nicht fair!",
            "Ich bin doch so süß!" to "Ich bin doch so süß!",
            "Pfefferminz! Bäh!" to "Pfefferminz! Bäh!",
            "Ich hab mich verlaufen!" to "Ich hab mich verlaufen!",
            "Mein Zuckerschloss!" to "Mein Zuckerschloss!",
            "Ich kann nicht schwimmen!" to "Ich kann nicht schwimmen!",
            "Ich wollte nur kuscheln!" to "Ich wollte nur kuscheln!",
            "Wo ist mein Bonbon?" to "Wo ist mein Bonbon?",
            "Abflug!" to "Abflug!",
            "Nicht kitzeln!" to "Nicht kitzeln!",
            "Das sag ich meiner Oma!" to "Das sag ich meiner Oma!",
            "Super Bürsti ist zu stark!" to "Super Bürsti ist zu stark!",
            "Ich hol den Boss!" to "Ich hol den Boss!",
            "So ein Schaumschläger!" to "So ein Schaumschläger!",
            "Ich will nach Hause!" to "Ich will nach Hause!",
            "Mir ist schwindelig!" to "Mir ist schwindelig!",
            "Schnell, versteckt euch!" to "Schnell, versteckt euch!",
            "Schaumparty? Nein danke!" to "Schaumparty? Nein danke!",
            "Meine Frisur!" to "Meine Frisur!",
            "Ich war nur zu Besuch!" to "Ich war nur zu Besuch!",
            "Das war mein Lieblingszahn!" to "Das war mein Lieblingszahn!",
            "Ich komm nie wieder!" to "Ich komm nie wieder!",
            // Neu am 26.11.2026
            "Ich putz mir nie die Zähne!" to "Ich putz mir nie die Zähne!",
            "Mein Zahn, mein Zuhause!" to "Mein Zahn, mein Zuhause!",
            "Ich bin nur ein kleiner Krümel!" to "Ich bin nur ein kleiner Krümel!",
            "Darf ich wenigstens\nmein Gummibärchen mitnehmen?" to "Darf ich wenigstens mein Gummibärchen mitnehmen?",
            "Ich hab doch Hausaufgaben!" to "Ich hab doch Hausaufgaben!",
            "Das ist mein Mittagsschlaf!" to "Das ist mein Mittagsschlaf!",
            "Ich ruf die Zahnfee an!" to "Ich ruf die Zahnfee an!",
            "Bürsti, wir können\ndoch Freunde sein!" to "Bürsti, wir können doch Freunde sein!",
            "Ich bin allergisch gegen sauber!" to "Ich bin allergisch gegen sauber!",
            "Mein Pudding wartet auf mich!" to "Mein Pudding wartet auf mich!",
            "Hier riecht es nach Minze, igitt!" to "Hier riecht es nach Minze, igitt!",
            "Ich bin zu jung zum Schrubben!" to "Ich bin zu jung zum Schrubben!",
            "Der hat ja eine Krone auf!" to "Der hat ja eine Krone auf!",
            "Ein Pirat! Alle Mann von Bord!" to "Ein Pirat! Alle Mann von Bord!",
            "Hilfe, ein Zauberer!" to "Hilfe, ein Zauberer!",
            "Hör auf zu tanzen,\ndas macht mir Angst!" to "Hör auf zu tanzen, das macht mir Angst!",
            "Mit Sonnenbrille sieht er\nnoch gefährlicher aus!" to "Mit Sonnenbrille sieht er noch gefährlicher aus!",
            "Ich sag es dem Riesen-Boss!" to "Ich sag es dem Riesen-Boss!",
            "Der Boss ist auch nicht mehr da!" to "Der Boss ist auch nicht mehr da!",
            "Wer hat das Ei gelegt?" to "Wer hat das Ei gelegt?",
            "Unser Schatz!\nDen kriegst du nicht!" to "Unser Schatz! Den kriegst du nicht!",
            "Ich flieg in die Badewanne!" to "Ich flieg in die Badewanne!",
            "Bis morgen, du Bürste!" to "Bis morgen, du Bürste!",
            "Ich zieh in den Mülleimer!" to "Ich zieh in den Mülleimer!",
        )
        val ZoneLines = listOf("Los! Erst oben!", "Jetzt die andere Seite oben!", "Jetzt unten!", "Und jetzt die andere Seite unten!")
    }
}
