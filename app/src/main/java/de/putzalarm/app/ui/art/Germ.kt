package de.putzalarm.app.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import de.putzalarm.app.data.Monster
import de.putzalarm.app.ui.theme.M
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Formen der Monster. Neue immer hinten anhängen: die Reihenfolge steckt in gespeicherten Alben. */
enum class GermKind { BLOB, SPIKE, BEAN, CYCLOPS, CUBE, STAR, DROP, JELLY }

/** Hautmuster (gestreift und getupft gibt es als Album-Bild ohne Zappel-Bild). EGG = Schlüpfling in der Eierschale. Nur hinten anhängen. */
enum class Pattern { PLAIN, STRIPES, DOTS, EGG }

fun shade(c: Color, amount: Float): Color =
    if (amount < 0) lerp(c, Color.Black, -amount) else lerp(c, Color.White, amount)

/**
 * Zeichnet ein Monster-Bakterium als fertig gerendertes 3D-Bild (Raster 120 × 120 wie früher, Mitte = center).
 * [wiggle] wählt das Bild mit den hoch- oder runtergerissenen Ärmchen, [tint] färbt alles einfarbig
 * (für gesperrte Sticker). [seed] und [look] gibt es nur noch aus Kompatibilität.
 */
@Suppress("UNUSED_PARAMETER")
fun DrawScope.drawGerm(
    center: Offset,
    sizePx: Float,
    color: Color,
    kind: GermKind,
    seed: Float = 2f,
    rotation: Float = 0f,
    wiggle: Float = 0f,
    look: Float = 0f,
    tint: Color? = null,
    alpha: Float = 1f,
    pattern: Pattern = Pattern.PLAIN,
) {
    if (sizePx < 1f) return
    val img = Sprites.germ(kind, color, if (wiggle >= 0f) 0 else 1, pattern)
    val px = sizePx.roundToInt()
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, Offset.Zero)
    }) {
        drawImage(
            img,
            dstOffset = IntOffset(-px / 2, -px / 2),
            dstSize = IntSize(px, px),
            alpha = alpha,
            colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) },
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** Ein einzelnes, ruhiges Monster als Bild (z. B. Sticker oder Profil-Avatar). */
@Composable
fun GermImage(
    color: Color,
    kind: GermKind,
    modifier: Modifier = Modifier,
    seed: Float = 2f,
    tint: Color? = null,
    pattern: Pattern = Pattern.PLAIN,
) {
    Canvas(modifier.aspectRatio(1f)) {
        drawGerm(center, size.minDimension * 0.86f, color, kind, seed, tint = tint, pattern = pattern)
    }
}

/**
 * Ein Monster aus dem Album. Seltene Monster tragen eine goldene Krone und funkeln.
 * Mit [tint] (gesperrt) bleibt bei seltenen die Krone als Umriss sichtbar, damit man sieht: hier wartet etwas Besonderes.
 */
@Composable
fun MonsterImage(m: Monster, modifier: Modifier = Modifier, tint: Color? = null) {
    if (!m.rare) {
        GermImage(m.color, m.kind, modifier, tint = tint, pattern = m.pattern)
        return
    }
    val t = rememberInfiniteTransition(label = "selten")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Canvas(modifier.aspectRatio(1f)) {
        val d = size.minDimension
        drawGerm(Offset(center.x, center.y + d * 0.07f), d * 0.78f, m.color, m.kind, tint = tint, pattern = m.pattern)
        drawCrown(Offset(center.x, d * 0.16f), d * 0.42f, locked = tint != null)
        if (tint != null) return@Canvas
        val spots = listOf(Offset(0.16f, 0.3f), Offset(0.86f, 0.42f), Offset(0.24f, 0.86f), Offset(0.8f, 0.82f))
        spots.forEachIndexed { i, p ->
            val phase = (time + i * 0.27f) % 1f
            if (phase < 0.45f) {
                val k = sin(phase / 0.45f * PI.toFloat())
                val c = Offset(p.x * d, p.y * d)
                drawSparkle(c, d * 0.09f * k, M.Yellow)
                drawSparkle(c, d * 0.055f * k, Color.White)
            }
        }
    }
}

/** Goldene Krone mit drei Zacken, [top] = Mitte der Oberkante, [w] = Breite. */
fun DrawScope.drawCrown(top: Offset, w: Float, locked: Boolean = false) {
    val h = w * 0.62f
    val x0 = top.x - w / 2
    val p = Path().apply {
        moveTo(x0, top.y + h)
        lineTo(x0 - w * 0.04f, top.y + h * 0.18f)
        lineTo(x0 + w * 0.27f, top.y + h * 0.55f)
        lineTo(top.x, top.y)
        lineTo(x0 + w * 0.73f, top.y + h * 0.55f)
        lineTo(x0 + w * 1.04f, top.y + h * 0.18f)
        lineTo(x0 + w, top.y + h)
        close()
    }
    val ink = Stroke(w * 0.06f, join = StrokeJoin.Round)
    if (locked) {
        drawPath(p, M.Yellow.copy(alpha = 0.35f))
        drawPath(p, M.Ink.copy(alpha = 0.45f), style = ink)
        return
    }
    drawPath(p, M.Yellow)
    // Glanz oben links und ein Band mit Edelsteinen
    drawRect(Color(0xFFFFB627), Offset(x0 + w * 0.02f, top.y + h * 0.72f), androidx.compose.ui.geometry.Size(w * 0.96f, h * 0.2f))
    drawPath(p, M.Ink, style = ink)
    drawCircle(Color(0xFFFF3D7F), w * 0.07f, Offset(top.x, top.y + h * 0.82f))
    drawCircle(Color(0xFF2F6BFF), w * 0.055f, Offset(x0 + w * 0.22f, top.y + h * 0.82f))
    drawCircle(Color(0xFF2F6BFF), w * 0.055f, Offset(x0 + w * 0.78f, top.y + h * 0.82f))
    for (x in listOf(x0 - w * 0.04f, top.x, x0 + w * 1.04f)) {
        val y = if (x == top.x) top.y else top.y + h * 0.18f
        drawCircle(M.Yellow, w * 0.07f, Offset(x, y))
        drawCircle(M.Ink, w * 0.07f, Offset(x, y), style = Stroke(w * 0.04f))
    }
}

/**
 * Boss-Monster als echtes, farbiges 3D-Monster (dieselben Bilder wie die Riesen-Bosse), das bedrohlich wackelt.
 * [beaten] zeigt es weiter in Farbe, mit einem dicken roten Kreuz darüber. [tint] färbt es als Schatten (noch nie besiegt).
 */
@Composable
fun BossImage(boss: de.putzalarm.app.data.Boss, modifier: Modifier = Modifier, beaten: Boolean = false, tint: Color? = null, hit: Float = 0f) {
    val t = rememberInfiniteTransition(label = "boss")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Canvas(modifier.aspectRatio(1f)) {
        val d = size.minDimension
        val moving = !beaten && tint == null
        val shake = hit * sin(time * 40 * PI.toFloat()) * d * 0.03f
        drawGiant(
            boss.id, Offset(center.x + shake, center.y), d, frame = if (moving && time > 0.5f) 1 else 0,
            rotation = if (moving) sin(time * 2 * PI.toFloat()) * 4f else 0f, hit = hit, tint = tint,
        )
        if (beaten) drawBeatenCross(center, d)
    }
}

/** Dickes rotes Kreuz mit dunklem Rand: dieses Monster ist besiegt. */
fun DrawScope.drawBeatenCross(c: Offset, d: Float) {
    val r = d * 0.36f
    for ((a, b) in listOf(Offset(-r, -r) to Offset(r, r), Offset(r, -r) to Offset(-r, r))) {
        drawLine(M.Ink, c + a, c + b, d * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    for ((a, b) in listOf(Offset(-r, -r) to Offset(r, r), Offset(r, -r) to Offset(-r, r))) {
        drawLine(Color(0xFFFF3B30), c + a, c + b, d * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/**
 * Zeichnet Riesen-Boss [id] (fertiges 3D-Bild) mit Mitte [center] und Breite [sizePx]. [hit] (0 bis 1) lässt ihn rot aufblitzen,
 * [tint] färbt ihn einfarbig (noch nie besiegt im Album).
 */
fun DrawScope.drawGiant(
    id: Int, center: Offset, sizePx: Float, frame: Int = 0, rotation: Float = 0f,
    hit: Float = 0f, tint: Color? = null, alpha: Float = 1f, squash: Float = 1f,
) {
    if (sizePx < 1f) return
    val img = Sprites.giant(id, frame)
    val px = sizePx.roundToInt()
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, Offset.Zero)
        scale(1f / squash, squash, Offset(0f, px / 2f))
    }) {
        val dst = IntOffset(-px / 2, -px / 2)
        val sz = IntSize(px, px)
        drawImage(img, dstOffset = dst, dstSize = sz, alpha = alpha, filterQuality = FilterQuality.Medium,
            colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) })
        if (hit > 0.02f && tint == null) drawImage(
            img, dstOffset = dst, dstSize = sz, alpha = alpha * hit * 0.6f, filterQuality = FilterQuality.Medium,
            colorFilter = ColorFilter.tint(Color(0xFFFF2A2A), BlendMode.SrcIn),
        )
    }
}

/** Ein Riesen-Boss als Bild, der langsam wackelt und mit den Ärmchen fuchtelt (für Album und Ergebnis). */
@Composable
fun GiantImage(id: Int, modifier: Modifier = Modifier, tint: Color? = null, still: Boolean = false) {
    val t = rememberInfiniteTransition(label = "riese")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Canvas(modifier.aspectRatio(1f)) {
        val moving = !still && tint == null
        drawGiant(
            id, center, size.minDimension, frame = if (moving && time > 0.5f) 1 else 0,
            rotation = if (moving) sin(time * 2 * PI.toFloat()) * 4f else 0f, tint = tint,
        )
    }
}

/** Lebensbalken eines Bosses: [left] = verbleibender Anteil (1 = voll, 0 = besiegt). */
@Composable
fun BossBar(left: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Color(0xFFE9DCFF), cornerRadius = r)
        if (left > 0f) drawRoundRect(
            if (left > 0.3f) Color(0xFFFF3B30) else Color(0xFFFFB627),
            size = androidx.compose.ui.geometry.Size(size.width * left.coerceIn(0f, 1f).coerceAtLeast(0.06f), size.height), cornerRadius = r,
        )
        drawRoundRect(M.Ink, cornerRadius = r, style = Stroke(size.height * 0.18f))
    }
}
