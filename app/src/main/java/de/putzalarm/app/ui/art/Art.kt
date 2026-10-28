package de.putzalarm.app.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlin.random.Random
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.IntSize
import de.putzalarm.app.ui.theme.M
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.unit.IntOffset

private fun round(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

/**
 * Maskottchen „Super Bürsti“: eine Zahnbürste als Superheld mit Heldenkörper, als fertig gerenderte 3D-Bilder
 * (Körper im Kostüm plus eingefärbter Umhang, siehe [LocalOutfit]). Mit [pose] macht er immer dieselbe Bewegung,
 * ohne wechselt er alle paar Sekunden zufällig zwischen den Moves, die er schon kann.
 * [still] zeigt nur ein ruhiges Einzelbild (z. B. als Vorschau im Schrank), [outfit] überschreibt das Outfit.
 */
@Composable
fun Mascot(
    modifier: Modifier = Modifier,
    pose: HeroPose? = null,
    onTap: (() -> Unit)? = null,
    outfit: Outfit? = null,
    still: Boolean = false,
) {
    val look = outfit ?: LocalOutfit.current
    val moves = look.moves.ifEmpty { HeroPose.Basic }
    var current by remember { mutableStateOf(pose ?: HeroPose.WAVE) }
    var frame by remember { mutableIntStateOf(if (still && pose != HeroPose.FLEX) 1 else 0) }
    var poke by remember { mutableIntStateOf(0) }
    // 0 = Lächeln, 1 = Grinsen, 2 = breites Lachen mit glänzenden Zähnen
    var mouth by remember { mutableIntStateOf(0) }
    var clockMs by remember { mutableStateOf(0L) }
    if (!still) LaunchedEffect(pose, poke) {
        // Nach einem Antippen springt er sofort in eine neue Bewegung
        current = pose ?: if (poke == 0) HeroPose.WAVE else moves.filter { it != current }.random()
        var start = 0L
        var next = 3200L
        var t0 = 0L
        if (poke > 0) mouth = 2
        while (true) {
            withFrameMillis { now ->
                if (start == 0L) start = now
                if (t0 == 0L) t0 = now + if (poke > 0) 1500L else 1800L
                clockMs = now
                // Meist ein Lächeln, ab und zu grinsen oder breit lachen; beim Jubeln immer lachen
                if (now > t0) {
                    mouth = if (mouth > 0) 0 else if (Random.nextBoolean()) 1 else 2
                    t0 = now + if (mouth > 0) Random.nextLong(900L, 2000L) else Random.nextLong(1800L, 4200L)
                }
                var elapsed = now - start
                if (pose == null && elapsed > next) {
                    current = moves.filter { it != current }.random()
                    start = now
                    elapsed = 0L
                    next = Random.nextLong(2600L, 4200L)
                }
                frame = ((elapsed / current.frameMs) % current.frames).toInt()
            }
        }
    }
    val tap = if (onTap == null) Modifier else Modifier.clickable(remember { MutableInteractionSource() }, null) { poke++; onTap() }
    val capeTint = remember(look.cape) { ColorFilter.tint(look.cape, BlendMode.Modulate) }
    val shown = if (still) pose ?: HeroPose.FLEX else current
    Canvas(modifier.aspectRatio(3f / 4f).then(tap)) {
        val fs = Sprites.frameSize
        val sx = size.width / fs.width
        val sy = size.height / fs.height
        fun put(p: Placed, filter: ColorFilter? = null): IntOffset {
            val dst = IntOffset((p.at.x * sx).roundToInt(), (p.at.y * sy).roundToInt())
            drawImage(
                p.img, dstOffset = dst, dstSize = IntSize((p.img.width * sx).roundToInt(), (p.img.height * sy).roundToInt()),
                filterQuality = FilterQuality.Medium, colorFilter = filter,
            )
            return dst
        }
        drawOval(Color.Black, Offset(size.width * 0.3f, size.height * 0.905f), Size(size.width * 0.42f, size.height * 0.05f), alpha = 0.18f)
        Sprites.cape(shown, frame)?.let { put(it, capeTint) }
        put(Sprites.hero(look.costume, shown, frame))
        val m = if (still) 0 else if (shown == HeroPose.JUBEL) 2 else mouth
        if (m > 0) Sprites.heroMouth(look.costume, shown, frame, m)?.let { crop ->
            val dst = put(crop)
            val ms = Sprites.mouthSize
            if (m == 2) {
                // Blitzen an den Zähnen
                val k = sin(clockMs / 160.0).toFloat().coerceAtLeast(0f)
                if (k > 0.05f) {
                    val c = Offset(dst.x + ms.width * sx * 0.72f, dst.y + ms.height * sy * 0.5f)
                    rotate(clockMs / 6f % 360f, c) { drawSparkle(c, size.width * 0.035f * k, Color.White) }
                }
            }
        }
    }
}

private val StarPath = Path().apply {
    val pts = listOf(50f to 6f, 62f to 36f, 94f to 38f, 69f to 58f, 77f to 90f, 50f to 72f, 23f to 90f, 31f to 58f, 6f to 38f, 38f to 36f)
    pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    close()
}

@Composable
fun Star(on: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val s = size.width / 100f
        scale(s, s, Offset.Zero) {
            drawPath(StarPath, if (on) M.Yellow else Color.White.copy(alpha = 0.3f))
            drawPath(StarPath, M.Ink, style = round(6f))
        }
    }
}

/** Einfache Comic-Symbole, damit Kinder ohne Lesen bedienen können. */
enum class Icon { PLAY, PAUSE, CLOSE, HOME, CHECK, CAMERA, GALLERY, SUN, MOON, BACK, TRASH, PENCIL, LOCK, HANGER }

@Composable
fun IconArt(icon: Icon, modifier: Modifier = Modifier, color: Color = M.Ink) {
    Canvas(modifier.aspectRatio(1f)) { drawIconArt(icon, color) }
}

fun DrawScope.drawIconArt(icon: Icon, color: Color) {
    val s = size.minDimension / 40f
    val w = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    translate((size.width - 40f * s) / 2, (size.height - 40f * s) / 2) {
        scale(s, s, Offset.Zero) {
            when (icon) {
                Icon.PLAY -> {
                    val p = Path().apply { moveTo(12f, 6f); lineTo(34f, 20f); lineTo(12f, 34f); close() }
                    drawPath(p, color); drawPath(p, color, style = w)
                }
                Icon.PAUSE -> {
                    drawRoundRect(color, Offset(9f, 7f), Size(8f, 26f), CornerRadius(2.5f))
                    drawRoundRect(color, Offset(23f, 7f), Size(8f, 26f), CornerRadius(2.5f))
                }
                Icon.CLOSE -> {
                    drawLine(color, Offset(10f, 10f), Offset(30f, 30f), 6f, StrokeCap.Round)
                    drawLine(color, Offset(30f, 10f), Offset(10f, 30f), 6f, StrokeCap.Round)
                }
                Icon.BACK -> {
                    val p = Path().apply { moveTo(24f, 8f); lineTo(12f, 20f); lineTo(24f, 32f) }
                    drawPath(p, color, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Icon.HOME -> {
                    val p = Path().apply { moveTo(5f, 20f); lineTo(20f, 6f); lineTo(35f, 20f); lineTo(31f, 20f); lineTo(31f, 34f); lineTo(9f, 34f); lineTo(9f, 20f); close() }
                    drawPath(p, color); drawPath(p, color, style = w)
                }
                Icon.CHECK -> {
                    val p = Path().apply { moveTo(8f, 21f); lineTo(16f, 29f); lineTo(32f, 11f) }
                    drawPath(p, color, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Icon.CAMERA -> {
                    drawRoundRect(color, Offset(4f, 11f), Size(32f, 22f), CornerRadius(5f))
                    drawRoundRect(color, Offset(13f, 7f), Size(14f, 6f), CornerRadius(2f))
                    drawCircle(Color.White, 7f, Offset(20f, 22f))
                    drawCircle(color, 4f, Offset(20f, 22f))
                }
                Icon.GALLERY -> {
                    drawRoundRect(color, Offset(4f, 7f), Size(32f, 26f), CornerRadius(4f), style = w)
                    val m = Path().apply { moveTo(7f, 30f); lineTo(16f, 19f); lineTo(22f, 25f); lineTo(27f, 20f); lineTo(33f, 30f); close() }
                    drawPath(m, color)
                    drawCircle(color, 3f, Offset(27f, 13f))
                }
                Icon.SUN -> {
                    val sun = Color(0xFFFFB627)
                    drawCircle(sun, 8f, Offset(20f, 20f))
                    for (i in 0 until 8) {
                        rotate(i * 45f, Offset(20f, 20f)) { drawLine(sun, Offset(20f, 3f), Offset(20f, 8f), 3f, StrokeCap.Round) }
                    }
                }
                Icon.MOON -> {
                    val full = Path().apply { addOval(androidx.compose.ui.geometry.Rect(6f, 6f, 34f, 34f)) }
                    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(15f, 1f, 41f, 27f)) }
                    drawPath(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, full, bite), color)
                }
                Icon.TRASH -> {
                    drawLine(color, Offset(7f, 11f), Offset(33f, 11f), 4f, StrokeCap.Round)
                    drawRoundRect(color, Offset(15f, 5f), Size(10f, 6f), CornerRadius(2f), style = Stroke(3f))
                    val b = Path().apply { moveTo(10f, 14f); lineTo(12f, 35f); lineTo(28f, 35f); lineTo(30f, 14f); close() }
                    drawPath(b, color)
                }
                Icon.LOCK -> {
                    drawRoundRect(color, Offset(11f, 6f), Size(18f, 22f), CornerRadius(9f), style = Stroke(4.5f))
                    drawRoundRect(color, Offset(7f, 17f), Size(26f, 19f), CornerRadius(4f))
                    drawCircle(Color.White, 2.6f, Offset(20f, 25f))
                    drawLine(Color.White, Offset(20f, 25f), Offset(20f, 30f), 2.6f, StrokeCap.Round)
                }
                Icon.HANGER -> {
                    val hook = Path().apply { moveTo(16f, 11f); cubicTo(16f, 5f, 25f, 5f, 24f, 11f); cubicTo(23.5f, 14f, 20f, 14f, 20f, 18f) }
                    drawPath(hook, color, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val bar = Path().apply { moveTo(20f, 18f); lineTo(36f, 30f); lineTo(4f, 30f); close() }
                    drawPath(bar, color, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Icon.PENCIL -> {
                    rotate(45f, Offset(20f, 20f)) {
                        drawRoundRect(color, Offset(15f, 2f), Size(10f, 28f), CornerRadius(2f))
                        val tip = Path().apply { moveTo(15f, 31f); lineTo(25f, 31f); lineTo(20f, 39f); close() }
                        drawPath(tip, color)
                    }
                }
            }
        }
    }
}
