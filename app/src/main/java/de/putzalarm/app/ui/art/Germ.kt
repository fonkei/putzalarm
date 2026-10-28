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

/**
 * Boss-Monster: ein großes Monster mit Hörnern und rotem Zacken-Schein, das bedrohlich wackelt.
 * [beaten] zeigt es platt und blass mit Pflaster-Kreuz, [tint] färbt es für noch nicht besiegte Bosse im Album ein.
 */
@Composable
fun BossImage(boss: de.putzalarm.app.data.Boss, modifier: Modifier = Modifier, beaten: Boolean = false, tint: Color? = null, hit: Float = 0f) {
    val t = rememberInfiniteTransition(label = "boss")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Canvas(modifier.aspectRatio(1f)) {
        val d = size.minDimension
        val c = Offset(center.x, center.y + d * 0.06f)
        val wob = if (beaten || tint != null) 0f else sin(time * 2 * PI.toFloat()) * 4f
        if (!beaten && tint == null) {
            // Zacken-Schein hinter dem Boss
            val spikes = Path()
            val n = 14
            for (i in 0 until n * 2) {
                val a = (i / (n * 2f) + time / n) * 2 * PI.toFloat()
                val r = if (i % 2 == 0) d * 0.49f else d * 0.38f
                val x = c.x + kotlin.math.cos(a) * r
                val y = c.y + sin(a) * r
                if (i == 0) spikes.moveTo(x, y) else spikes.lineTo(x, y)
            }
            spikes.close()
            drawPath(spikes, Color(0xFFFF3B30).copy(alpha = 0.85f))
            drawPath(spikes, M.Ink, style = Stroke(d * 0.015f, join = StrokeJoin.Round))
        }
        // Hörner
        val hornCol = tint ?: if (beaten) Color(0xFFBBB3C8) else Color(0xFFFFF3D6)
        for (side in listOf(-1f, 1f)) {
            val bx = c.x + side * d * 0.2f
            val by = c.y - d * 0.24f
            val horn = Path().apply {
                moveTo(bx - side * d * 0.07f, by + d * 0.04f)
                quadraticBezierTo(bx + side * d * 0.1f, by - d * 0.06f, bx + side * d * 0.13f, by - d * 0.22f)
                quadraticBezierTo(bx + side * d * 0.02f, by - d * 0.1f, bx - side * d * 0.04f, by - d * 0.06f)
                close()
            }
            drawPath(horn, hornCol)
            drawPath(horn, M.Ink.copy(alpha = if (tint != null) 0.4f else 1f), style = Stroke(d * 0.02f, join = StrokeJoin.Round))
        }
        val shake = hit * sin(time * 40 * PI.toFloat()) * d * 0.03f
        withTransform({ if (beaten) scale(1.05f, 0.8f, Offset(c.x, c.y + d * 0.35f)) }) {
            drawGerm(Offset(c.x + shake, c.y), d * 0.82f, boss.color, boss.kind, rotation = wob, tint = tint ?: if (beaten) Color(0xFFBBB3C8) else null)
        }
        if (beaten) {
            // Pflaster-Kreuz: besiegt!
            for (r in listOf(40f, -40f)) withTransform({ rotate(r, c) }) {
                drawRoundRect(
                    Color(0xFFFFD9B0), Offset(c.x - d * 0.3f, c.y - d * 0.06f), androidx.compose.ui.geometry.Size(d * 0.6f, d * 0.12f),
                    androidx.compose.ui.geometry.CornerRadius(d * 0.04f),
                )
                drawRoundRect(
                    M.Ink, Offset(c.x - d * 0.3f, c.y - d * 0.06f), androidx.compose.ui.geometry.Size(d * 0.6f, d * 0.12f),
                    androidx.compose.ui.geometry.CornerRadius(d * 0.04f), style = Stroke(d * 0.015f),
                )
            }
        }
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
