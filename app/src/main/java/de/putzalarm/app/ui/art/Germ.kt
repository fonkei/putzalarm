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

enum class GermKind { BLOB, SPIKE, BEAN, CYCLOPS }

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
) {
    if (sizePx < 1f) return
    val img = Sprites.germ(kind, color, if (wiggle >= 0f) 0 else 1)
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
) {
    Canvas(modifier.aspectRatio(1f)) {
        drawGerm(center, size.minDimension * 0.86f, color, kind, seed, tint = tint)
    }
}

/**
 * Ein Monster aus dem Album. Seltene Monster tragen eine goldene Krone und funkeln.
 * Mit [tint] (gesperrt) bleibt bei seltenen die Krone als Umriss sichtbar, damit man sieht: hier wartet etwas Besonderes.
 */
@Composable
fun MonsterImage(m: Monster, modifier: Modifier = Modifier, tint: Color? = null) {
    if (!m.rare) {
        GermImage(m.color, m.kind, modifier, tint = tint)
        return
    }
    val t = rememberInfiniteTransition(label = "selten")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Canvas(modifier.aspectRatio(1f)) {
        val d = size.minDimension
        drawGerm(Offset(center.x, center.y + d * 0.07f), d * 0.78f, m.color, m.kind, tint = tint)
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
