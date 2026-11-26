package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import de.putzalarm.app.audio.Sfx
import de.putzalarm.app.data.ChestGear
import de.putzalarm.app.data.Child
import de.putzalarm.app.data.MapLen
import de.putzalarm.app.data.MaxLevel
import de.putzalarm.app.data.Monsters
import de.putzalarm.app.data.spoken
import de.putzalarm.app.ui.art.Gear
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.LocalOutfit
import de.putzalarm.app.ui.art.Mascot
import de.putzalarm.app.ui.art.MonsterImage
import de.putzalarm.app.ui.art.Star
import de.putzalarm.app.ui.art.drawSparkle
import de.putzalarm.app.ui.art.drawStar
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.u
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun ink(w: Float) = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)

// ---------------------------------------------------------------- Schatzkiste

private val Wood = Color(0xFFB4693A)
private val WoodDark = Color(0xFF7A4122)
private val Gold = Color(0xFFFFC928)

/**
 * Eine Schatzkiste im Raster 100 × 90 (Mitte unten bei 50/88). [open] 0 = zu, 1 = Deckel ganz auf.
 * Mit offenem Deckel leuchtet es golden heraus.
 */
fun DrawScope.drawChest(open: Float = 0f, glow: Float = 0f) {
    val s = size.minDimension / 100f
    translate((size.width - 100f * s) / 2, (size.height - 90f * s) / 2) {
        scale(s, s, Offset.Zero) {
            val w = 4f
            if (open > 0.05f) {
                // Goldschein aus der offenen Kiste
                drawOval(Gold.copy(alpha = 0.55f * open), Offset(12f, 34f), Size(76f, 20f))
                for (i in 0 until 5) drawCircle(Gold, 6f, Offset(28f + i * 11f, 44f - (i % 2) * 3f))
            }
            // Kasten
            drawRoundRect(Wood, Offset(10f, 44f), Size(80f, 44f), CornerRadius(6f))
            for (x in listOf(22f, 72f)) drawRect(Gold, Offset(x, 44f), Size(7f, 44f))
            drawRoundRect(WoodDark, Offset(10f, 44f), Size(80f, 44f), CornerRadius(6f), style = ink(w))
            drawRoundRect(M.Ink, Offset(10f, 44f), Size(80f, 44f), CornerRadius(6f), style = ink(w * 0.7f))
            // Schloss
            if (open < 0.5f) {
                drawRoundRect(Gold, Offset(43f, 46f), Size(14f, 16f), CornerRadius(3f))
                drawRoundRect(M.Ink, Offset(43f, 46f), Size(14f, 16f), CornerRadius(3f), style = ink(2.5f))
                drawCircle(M.Ink, 2.4f, Offset(50f, 53f))
            }
            // Deckel: klappt nach hinten auf (wird flacher und rutscht nach oben)
            val lift = open.coerceIn(0f, 1f)
            val lidTop = 18f - lift * 14f
            val lidH = (28f * (1f - lift * 0.75f)).coerceAtLeast(5f)
            val lid = Path().apply {
                moveTo(10f, lidTop + lidH)
                lineTo(10f, lidTop + lidH * 0.45f)
                quadraticBezierTo(50f, lidTop - lidH * 0.35f, 90f, lidTop + lidH * 0.45f)
                lineTo(90f, lidTop + lidH)
                close()
            }
            drawPath(lid, if (lift > 0.5f) WoodDark else Wood)
            for (x in listOf(22f, 72f)) drawRect(Gold, Offset(x, lidTop + lidH * 0.1f), Size(7f, lidH * 0.9f))
            drawPath(lid, M.Ink, style = ink(w * 0.7f))
            if (glow > 0f) for (i in 0 until 3) drawSparkle(Offset(25f + i * 25f, 30f - lift * 10f), 6f * glow, Color.White)
        }
    }
}

@Composable
fun ChestIcon(modifier: Modifier = Modifier, open: Float = 0f) {
    Canvas(modifier.aspectRatio(100f / 90f)) { drawChest(open) }
}

// ---------------------------------------------------------------- Monster-Stufen

/** Kleine Sterne für die Stufe eines Album-Monsters: Stufe 1 zeigt keinen, Stufe 5 vier. */
@Composable
fun LevelStars(level: Int, modifier: Modifier = Modifier, star: Dp = 1.8.u) {
    if (level <= 1) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(0.15.u)) {
        repeat((level - 1).coerceAtMost(MaxLevel - 1)) { Star(true, Modifier.size(star)) }
    }
}

/** Größe eines Album-Monsters je Stufe: es wächst mit jedem Aufstieg ein Stück. */
fun levelScale(level: Int): Float = 0.82f + 0.06f * (level - 1)

// ---------------------------------------------------------------- Schatzkarte

/** Lage der Felder auf der Karte (Anteil von Breite und Höhe): Start, neun Felder, Schatzkiste. */
private val MapPoints = listOf(
    0.07f to 0.8f, 0.17f to 0.63f, 0.29f to 0.75f, 0.41f to 0.62f, 0.34f to 0.43f, 0.45f to 0.27f,
    0.58f to 0.34f, 0.66f to 0.53f, 0.78f to 0.63f, 0.87f to 0.45f, 0.88f to 0.22f,
)

/** Drei Karten im Wechsel: Insel, Dschungel, Eisland. */
private data class MapTheme(val sea: Color, val land: Color, val deco: Color, val decoDark: Color)
private val Themes = listOf(
    MapTheme(Color(0xFF5AC8FA), Color(0xFFFFE08A), Color(0xFF4CD964), Color(0xFF8A5A2B)),
    MapTheme(Color(0xFF2FA39C), Color(0xFF9BE27A), Color(0xFF2E9E4F), Color(0xFF6B4A2B)),
    MapTheme(Color(0xFF7FB8FF), Color(0xFFF4FAFF), Color(0xFF2E9E4F), Color(0xFF6B4A2B)),
)

private fun DrawScope.drawMap(theme: MapTheme, field: Float, time: Float) {
    val w = size.width
    val h = size.height
    val u = h / 100f
    drawRect(theme.sea)
    // Wellen im Meer
    for (i in 0 until 14) {
        val x = ((i * 0.137f + time * 0.01f) % 1f) * w
        val y = ((i * 0.291f) % 1f) * h
        drawArc(Color.White.copy(alpha = 0.45f), 180f, 180f, false, Offset(x, y), Size(5f * u, 2.4f * u), style = ink(0.6f * u))
    }
    val pts = MapPoints.map { (x, y) -> Offset(x * w, y * h) }
    // Land: Kreise entlang des Wegs, erst als dicker Rand, dann gefüllt
    val r = 14f * u
    val blobs = buildList {
        for (i in 0 until pts.size - 1) for (k in 0..3) add(pts[i] + (pts[i + 1] - pts[i]) * (k / 4f))
        add(pts.last())
    }
    blobs.forEach { drawCircle(M.Ink, r + 0.8f * u, it) }
    blobs.forEach { drawCircle(theme.land, r, it) }
    // Bäume am Rand
    listOf(0.12f to 0.42f, 0.24f to 0.9f, 0.52f to 0.12f, 0.55f to 0.55f, 0.74f to 0.33f, 0.95f to 0.62f, 0.3f to 0.52f).forEach { (x, y) ->
        val c = Offset(x * w, y * h)
        drawLine(theme.decoDark, c, c + Offset(0f, 6f * u), 1.6f * u, StrokeCap.Round)
        drawCircle(theme.deco, 3.6f * u, c)
        drawCircle(M.Ink, 3.6f * u, c, style = Stroke(0.5f * u))
    }
    // Gestrichelter Weg
    val path = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
    drawPath(path, Color(0xFFB4693A), style = Stroke(1.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 2.4f * u))))
    // Felder: schon gegangene mit Stern, die anderen weiß; das letzte ist die Kiste
    pts.forEachIndexed { i, p ->
        if (i == pts.size - 1) return@forEachIndexed
        val done = i <= field
        drawCircle(M.Ink, 4.4f * u, p + Offset(0f, 0.6f * u))
        drawCircle(if (done) M.Yellow else Color.White, 4.4f * u, p)
        drawCircle(M.Ink, 4.4f * u, p, style = Stroke(0.6f * u))
        if (done && i > 0) drawStar(p, 5f * u)
    }
    // Rotes X unter der Kiste
    val x = pts.last() + Offset(0f, 7f * u)
    drawLine(Color(0xFFFF3B30), x + Offset(-4f * u, -2.5f * u), x + Offset(4f * u, 2.5f * u), 1.6f * u, StrokeCap.Round)
    drawLine(Color(0xFFFF3B30), x + Offset(4f * u, -2.5f * u), x + Offset(-4f * u, 2.5f * u), 1.6f * u, StrokeCap.Round)
}

/** Die Schatzkarte: jedes Putzen ein Feld weiter, auf dem letzten wartet die Schatzkiste. */
@Composable
fun TreasureMapScreen(child: Child, onBack: () -> Unit, say: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val field = child.mapField
    val left = MapLen - field
    LaunchedEffect(Unit) {
        say(
            when (left) {
                1 -> "Noch einmal putzen, dann gehört die Schatzkiste dir!"
                MapLen -> "Eine neue Schatzkarte! Jedes Mal Putzen bringt dich ein Feld weiter bis zur Schatzkiste."
                else -> "Noch $left mal putzen, dann erreichst du die Schatzkiste!"
            },
        )
    }
    val t = rememberInfiniteTransition(label = "karte")
    val time by t.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "hüpfen")
    val theme = Themes[child.chests % Themes.size]
    BoxWithConstraints(Modifier.fillMaxSize().background(theme.sea)) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) { drawMap(theme, field.toFloat(), time) }
        // Kiste auf dem letzten Feld
        val chestAt = MapPoints.last()
        val chestW = h * 0.17f
        ChestIcon(
            Modifier.width(chestW).offset(x = w * chestAt.first - chestW / 2, y = h * chestAt.second - chestW * 0.7f)
                .scale(1f + 0.05f * bob),
        )
        // Super Bürsti steht auf seinem Feld
        val p = MapPoints[field]
        val mh = h * 0.26f
        Mascot(
            Modifier.height(mh).offset(x = w * p.first - mh * 0.375f, y = h * p.second - mh * 0.92f - (1.2.u * bob)),
            pose = HeroPose.WAVE,
        )
        RoundIconButton(Icon.BACK, onBack, Modifier.align(Alignment.TopStart).padding(3.u), size = 6.u)
        // Oben rechts: was in den Kisten steckt (gefunden oder noch verschlossen)
        Row(
            Modifier.align(Alignment.TopEnd).padding(3.u).comic(Color.White, 2.4.u, 0.45.u, 0.6.u).padding(horizontal = 1.4.u, vertical = 0.8.u),
            horizontalArrangement = Arrangement.spacedBy(1.2.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChestIcon(Modifier.width(5.u), open = 1f)
            ChestGear.forEachIndexed { i, g ->
                Box(Modifier.size(7.u, 9.u), contentAlignment = Alignment.Center) {
                    GearPreview(g, Modifier.fillMaxSize(), locked = child.chests <= i)
                }
            }
        }
    }
}

/** Super Bürsti mit einem Ausrüstungsteil als ruhiges Bild; gesperrt blass mit Schloss. */
@Composable
fun GearPreview(g: Gear, modifier: Modifier = Modifier, locked: Boolean = false) {
    val look = LocalOutfit.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Mascot(
            Modifier.fillMaxSize().then(if (locked) Modifier.scale(0.95f) else Modifier),
            pose = HeroPose.FLEX, still = true,
            outfit = look.copy(gear = look.gear.filter { it.slot != g.slot } + g),
        )
        if (locked) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
            IconArt(Icon.LOCK, Modifier.fillMaxWidth(0.45f), color = M.Ink)
        }
    }
}

/** Kleine Karte im Ergebnis: Super Bürstis Stern springt ein Feld weiter; am Ende steht die Kiste. */
@Composable
fun MapStrip(before: Int, modifier: Modifier = Modifier) {
    val from = (before % MapLen).toFloat()
    val pos = remember { Animatable(from) }
    LaunchedEffect(Unit) {
        delay(2200)
        pos.animateTo(from + 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(4.u)) {
            val n = MapLen
            val step = (size.width - size.height) / n
            val y = size.height / 2
            val r = size.height * 0.22f
            for (i in 0 until n) {
                val c = Offset(size.height / 2 + i * step, y)
                drawCircle(if (i <= pos.value) M.Yellow else Color.White, r, c)
                drawCircle(M.Ink, r, c, style = Stroke(r * 0.3f))
            }
            val k = pos.value
            val hop = sin((k - kotlin.math.floor(k)) * PI.toFloat()) * size.height * 0.35f
            drawStar(Offset(size.height / 2 + k * step, y - hop), size.height * 0.75f)
        }
        ChestIcon(Modifier.width(5.u), open = if (pos.value >= MapLen - 0.05f) 1f else 0f)
    }
}

// ---------------------------------------------------------------- Überraschungsei und Schatzkiste (große Szenen)

/** Zeichnet das Überraschungsei (cremeweiß mit bunten Tupfen und Zickzack-Band) und [cracks] Risse. */
private fun DrawScope.drawEgg(cracks: Int, color: Color) {
    val w = size.width
    val h = size.height
    val egg = Path().apply { addOval(Rect(w * 0.14f, h * 0.04f, w * 0.86f, h * 0.98f)) }
    drawPath(egg, Color(0xFFFFF6E6))
    // Zickzack-Band in der Farbe des Monsters
    val band = Path().apply {
        moveTo(w * 0.15f, h * 0.55f)
        for (i in 0..12) lineTo(w * (0.15f + i * 0.058f), h * (if (i % 2 == 0) 0.55f else 0.49f))
        lineTo(w * 0.85f, h * 0.62f)
        for (i in 12 downTo 0) lineTo(w * (0.15f + i * 0.058f), h * (if (i % 2 == 0) 0.62f else 0.56f))
        close()
    }
    drawPath(band, color.copy(alpha = 0.85f))
    listOf(0.32f to 0.25f, 0.62f to 0.2f, 0.5f to 0.36f, 0.3f to 0.78f, 0.68f to 0.8f, 0.48f to 0.88f).forEachIndexed { i, (x, y) ->
        drawCircle(listOf(M.Pink, M.Mint, M.Yellow, M.Blue)[i % 4].copy(alpha = 0.8f), w * 0.04f, Offset(w * x, h * y))
    }
    drawPath(egg, M.Ink, style = ink(w * 0.025f))
    // Risse
    val crackLines = listOf(
        listOf(0.5f to 0.1f, 0.46f to 0.2f, 0.54f to 0.28f, 0.47f to 0.38f),
        listOf(0.2f to 0.4f, 0.3f to 0.44f, 0.27f to 0.52f, 0.38f to 0.58f),
        listOf(0.82f to 0.38f, 0.72f to 0.45f, 0.76f to 0.52f, 0.64f to 0.6f),
    )
    crackLines.take(cracks).forEach { line ->
        val p = Path().apply { line.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(w * x, h * y) else lineTo(w * x, h * y) } }
        drawPath(p, M.Ink, style = ink(w * 0.02f))
    }
}

/**
 * Überraschungsei: wackelt, nach drei Mal Antippen springt es auf und der Schlüpfling (oder ein Sternenhaufen) kommt heraus.
 * Ohne Antippen schlüpft es nach einer Weile von selbst. Danach schließt ein Tipp die Szene.
 */
@Composable
fun EggOverlay(monsterId: Int?, stars: Int, say: (String) -> Unit, sfx: Sfx?, onDone: () -> Unit) {
    val m = monsterId?.let { Monsters.getOrNull(it) }
    var taps by remember { mutableIntStateOf(0) }
    val hatch = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val t = rememberInfiniteTransition(label = "ei")
    val wob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "wackeln")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "strahlen")
    fun open() {
        if (hatch.value > 0f || hatch.isRunning) return
        scope.launch {
            sfx?.fanfare()
            hatch.animateTo(1f, tween(900))
            say(if (m != null) "Wow! ${m.name} ist geschlüpft! Ab ins Album!" else "Im Ei waren $stars Sterne für dich!")
        }
    }
    LaunchedEffect(Unit) {
        say("Ein Überraschungsei! Tipp ganz oft drauf!")
        delay(14000)
        open()
    }
    Box(
        Modifier.fillMaxSize().background(M.Ink.copy(alpha = 0.78f))
            .clickable(remember { MutableInteractionSource() }, null) {
                if (hatch.value >= 1f) onDone()
                else if (hatch.value == 0f) {
                    taps++
                    sfx?.plopp()
                    if (taps >= 3) open()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val k = hatch.value
        // Strahlen hinter dem Ei
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension * (0.45f + 0.15f * k)
            rotate(spin, c) {
                for (i in 0 until 12) rotate(i * 30f, c) {
                    val p = Path().apply { moveTo(c.x, c.y); lineTo(c.x - r * 0.12f, c.y - r); lineTo(c.x + r * 0.12f, c.y - r); close() }
                    drawPath(p, M.Yellow.copy(alpha = 0.18f + 0.2f * k))
                }
            }
        }
        val eggColor = m?.color ?: M.Yellow
        Box(Modifier.fillMaxHeight(0.62f).aspectRatio(0.78f), contentAlignment = Alignment.Center) {
            if (k < 1f) {
                // Obere Hälfte fliegt hoch, untere fällt weg
                val shake = if (k == 0f) wob * (4f + taps * 4f) else 0f
                Canvas(Modifier.fillMaxSize().rotate(shake)) {
                    val hgt = size.height
                    translate(0f, -k * hgt * 0.8f) {
                        rotate(-k * 70f, Offset(size.width / 2, hgt * 0.5f)) {
                            clipRect(bottom = hgt * 0.55f) { drawEgg(taps.coerceAtMost(3), eggColor) }
                        }
                    }
                    translate(0f, k * hgt * 0.6f) {
                        clipRect(top = hgt * 0.55f) { drawEgg(taps.coerceAtMost(3), eggColor) }
                    }
                }
            }
            if (k > 0.2f) {
                val pop = ((k - 0.2f) / 0.8f).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth(0.95f).scale(0.3f + 0.75f * pop), contentAlignment = Alignment.Center) {
                    if (m != null) MonsterImage(m, Modifier.fillMaxWidth())
                    else Row(horizontalArrangement = Arrangement.spacedBy(1.u), verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { Star(true, Modifier.width(12.u)) }
                    }
                }
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 0 until 6) {
                        val a = i / 6f * 6.283f + spin / 60f
                        val r = size.width * (0.35f + 0.1f * pop)
                        drawSparkle(center + Offset(cos(a) * r, sin(a) * r), size.width * 0.06f * pop, M.Yellow)
                    }
                }
            }
        }
        if (k >= 1f && m == null) BasicText("+$stars", style = lilita(8, M.Yellow), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.u))
    }
}

/** Schatzkiste: wackelt, antippen öffnet sie, heraus kommt ein Ausrüstungsteil (an Super Bürsti gezeigt) oder Sterne. */
@Composable
fun ChestOverlay(gear: Gear?, stars: Int, say: (String) -> Unit, sfx: Sfx?, onDone: () -> Unit) {
    val open = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val t = rememberInfiniteTransition(label = "kiste")
    val wob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "wackeln")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "strahlen")
    fun doOpen() {
        if (open.value > 0f || open.isRunning) return
        scope.launch {
            sfx?.fanfare()
            open.animateTo(1f, tween(800))
            say(if (gear != null) "Du hast den Schatz gefunden: ${gear.spoken}! Super Bürsti probiert ihn gleich an!" else "In der Schatzkiste waren $stars Sterne!")
        }
    }
    LaunchedEffect(Unit) {
        say("Du hast die Schatzkiste erreicht! Tipp drauf und mach sie auf!")
        delay(14000)
        doOpen()
    }
    Box(
        Modifier.fillMaxSize().background(Color(0xFF1B3A6B).copy(alpha = 0.92f))
            .clickable(remember { MutableInteractionSource() }, null) { if (open.value >= 1f) onDone() else doOpen() },
        contentAlignment = Alignment.Center,
    ) {
        val k = open.value
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(center.x, size.height * 0.62f)
            val r = size.minDimension * (0.3f + 0.5f * k)
            if (k > 0f) rotate(spin, c) {
                for (i in 0 until 14) rotate(i * (360f / 14), c) {
                    val p = Path().apply { moveTo(c.x, c.y); lineTo(c.x - r * 0.1f, c.y - r); lineTo(c.x + r * 0.1f, c.y - r); close() }
                    drawPath(p, M.Yellow.copy(alpha = 0.35f * k))
                }
            }
        }
        Box(Modifier.fillMaxHeight(0.42f).aspectRatio(100f / 90f).offset(y = 12.u).rotate(if (k == 0f) wob * 4f else 0f)) {
            Canvas(Modifier.fillMaxSize()) { drawChest(k, glow = k) }
        }
        if (k > 0.3f) {
            val pop = ((k - 0.3f) / 0.7f).coerceIn(0f, 1f)
            Box(Modifier.fillMaxHeight(0.55f).aspectRatio(0.75f).offset(y = (-6).u).scale(0.3f + 0.7f * pop), contentAlignment = Alignment.Center) {
                if (gear != null) GearPreview(gear, Modifier.fillMaxSize())
                else BasicText("+$stars", style = lilita(10, M.Yellow))
            }
        }
    }
}
