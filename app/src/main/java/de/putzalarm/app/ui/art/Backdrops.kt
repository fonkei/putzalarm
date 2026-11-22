package de.putzalarm.app.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import de.putzalarm.app.data.Backdrop
import de.putzalarm.app.data.Effect
import de.putzalarm.app.ui.theme.M
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hintergründe für den Startbildschirm. Alle bleiben hell, damit die dunkle Schrift gut lesbar bleibt.
 */
@Composable
fun BackdropBox(backdrop: Backdrop, modifier: Modifier = Modifier.fillMaxSize(), content: @Composable BoxScope.() -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "hintergrund")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) { drawBackdrop(backdrop, time) }
        content()
    }
}

fun DrawScope.drawBackdrop(b: Backdrop, time: Float) {
    val w = size.width
    val h = size.height
    val u = w / 100f
    when (b) {
        Backdrop.DOTS -> {
            drawRect(M.Yellow)
            val s = 6 * u
            var y = s / 2
            while (y < h) { var x = s / 2; while (x < w) { drawCircle(M.Ink.copy(alpha = 0.08f), s * 0.12f, Offset(x, y)); x += s }; y += s }
        }
        Backdrop.SPACE -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFD9CCFF), Color(0xFFF1E9FF))))
            val r = Random(7)
            repeat(70) {
                val p = Offset(r.nextFloat() * w, r.nextFloat() * h)
                val k = 0.5f + 0.5f * sin((time * 6 + r.nextFloat()) * 2 * PI.toFloat())
                drawSparkle(p, u * (0.5f + r.nextFloat() * 0.8f) * (0.6f + 0.4f * k), Color(0xFF9B7BFF).copy(alpha = 0.35f + 0.45f * k))
            }
            // Planet mit Ring und ein kleiner Mond
            val c = Offset(w * 0.83f, h * 0.2f)
            drawCircle(Color(0xFFFFB3D1), 7 * u, c)
            drawCircle(Color(0xFFFF8FB8), 7 * u, Offset(c.x + 1.5f * u, c.y + 1.5f * u), alpha = 0.35f)
            drawOval(M.Ink.copy(alpha = 0.25f), Offset(c.x - 11 * u, c.y - 1.6f * u), Size(22 * u, 3.2f * u), style = Stroke(0.7f * u))
            drawCircle(Color(0xFFFFE08A), 2.4f * u, Offset(w * 0.6f + sin(time * 2 * PI.toFloat()) * 3 * u, h * 0.1f))
        }
        Backdrop.SEA -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFBFF3FF), Color(0xFF8FE3F5))))
            // Seegras unten
            for (i in 0 until 9) {
                val x = w * (0.04f + i * 0.12f)
                val sway = sin((time * 3 + i * 0.3f) * 2 * PI.toFloat()) * 2 * u
                val p = Path().apply { moveTo(x, h); quadraticBezierTo(x + sway, h - 10 * u, x + sway * 1.5f, h - (14 + i % 3 * 4) * u) }
                drawPath(p, Color(0xFF3EC28F).copy(alpha = 0.55f), style = Stroke(1.8f * u))
            }
            // Blasen steigen auf
            val r = Random(3)
            repeat(26) {
                val x = r.nextFloat() * w
                val speed = 0.5f + r.nextFloat()
                val y = h - ((time * speed + r.nextFloat()) % 1f) * (h + 6 * u)
                val rad = u * (0.6f + r.nextFloat() * 1.4f)
                drawCircle(Color.White.copy(alpha = 0.55f), rad, Offset(x + sin((time * 8 + it) * 2f) * u, y), style = Stroke(0.35f * u))
            }
        }
        Backdrop.JUNGLE -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFDDF7C8), Color(0xFFC3EFA7))))
            val r = Random(11)
            repeat(18) {
                val c = Offset(r.nextFloat() * w, if (it % 2 == 0) r.nextFloat() * h * 0.25f else h - r.nextFloat() * h * 0.25f)
                val ang = r.nextFloat() * 360f + sin((time * 2 + it) * 2 * PI.toFloat()) * 6f
                rotate(ang, c) {
                    val leaf = Path().apply {
                        moveTo(c.x, c.y); quadraticBezierTo(c.x + 5 * u, c.y - 4 * u, c.x + 11 * u, c.y)
                        quadraticBezierTo(c.x + 5 * u, c.y + 4 * u, c.x, c.y)
                    }
                    drawPath(leaf, Color(0xFF6CCB5F).copy(alpha = 0.6f))
                    drawLine(Color(0xFF3E9C3A).copy(alpha = 0.5f), c, Offset(c.x + 10 * u, c.y), 0.3f * u)
                }
            }
        }
        Backdrop.RAINBOW -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFE6F6FF), Color(0xFFFFF6E0))))
            val cols = listOf(0xFFFF6B6B, 0xFFFFB627, 0xFFFFE066, 0xFF7BDC5A, 0xFF5AC8FA, 0xFFB57BFF).map { Color(it).copy(alpha = 0.45f) }
            val c = Offset(w * 0.75f, h * 1.05f)
            cols.forEachIndexed { i, col ->
                val rad = (62 - i * 4.5f) * u
                drawArc(col, 180f, 180f, false, Offset(c.x - rad, c.y - rad), Size(rad * 2, rad * 2), style = Stroke(4.5f * u))
            }
            // Wolken ziehen langsam vorbei
            for (i in 0 until 3) {
                val x = ((time + i / 3f) % 1f) * (w + 30 * u) - 15 * u
                val y = h * (0.12f + i * 0.13f)
                for ((dx, rr) in listOf(0f to 3.5f, 3.5f to 4.5f, 7.5f to 3.2f)) drawCircle(Color.White.copy(alpha = 0.85f), rr * u, Offset(x + dx * u, y))
            }
        }
    }
}

/** Kleines Vorschaubild eines Hintergrunds (Schrank, Belohnungen). */
@Composable
fun BackdropPreview(b: Backdrop, modifier: Modifier) {
    Canvas(modifier) { drawBackdrop(b, 0.2f) }
}

/**
 * Feier-Effekt im Ergebnis: Konfetti, Seifenblasen oder Sternenregen, ein paar Sekunden lang.
 */
@Composable
fun CelebrationOverlay(effect: Effect, modifier: Modifier = Modifier.fillMaxSize()) {
    if (effect == Effect.NONE) return
    val t = rememberInfiniteTransition(label = "feier")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "zeit")
    val seeds = remember(effect) { List(60) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier) { drawEffect(effect, time, seeds) }
}

fun DrawScope.drawEffect(effect: Effect, time: Float, seeds: List<Triple<Float, Float, Float>>) {
    val w = size.width
    val h = size.height
    val u = w / 100f
    val cols = listOf(M.Pink, M.Yellow, M.Blue, M.Mint, Color(0xFFB57BFF))
    seeds.forEachIndexed { i, (a, b, c) ->
        val k = (time * (0.6f + c * 0.8f) + a) % 1f
        when (effect) {
            Effect.CONFETTI -> {
                val p = Offset(b * w + sin((k * 6 + a * 10) * 2f) * 3 * u, k * (h + 4 * u) - 2 * u)
                rotate(k * 720f + a * 360f, p) { drawRect(cols[i % cols.size], Offset(p.x - 0.8f * u, p.y - 0.4f * u), Size(1.6f * u, 0.8f * u)) }
            }
            Effect.BUBBLES -> {
                val p = Offset(b * w + sin((k * 4 + a * 10) * 2f) * 2 * u, h - k * (h + 6 * u))
                val r = (1.2f + c * 2.2f) * u
                drawCircle(Color.White.copy(alpha = 0.25f), r, p)
                drawCircle(cols[i % cols.size].copy(alpha = 0.6f), r, p, style = Stroke(0.3f * u))
                drawCircle(Color.White.copy(alpha = 0.8f), r * 0.25f, Offset(p.x - r * 0.4f, p.y - r * 0.4f))
            }
            Effect.STARS -> {
                val p = Offset(b * w, k * (h + 4 * u) - 2 * u)
                rotate(k * 360f, p) { drawSparkle(p, (1f + c * 1.6f) * u, if (i % 3 == 0) Color.White else M.Yellow) }
            }
            Effect.NONE -> Unit
        }
    }
}

/** Kleines Bild eines Effekts (Schrank, Belohnungen). */
@Composable
fun EffectPreview(e: Effect, modifier: Modifier) {
    val seeds = remember(e) { List(14) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier) {
        drawRect(M.Lilac)
        drawEffect(e, 0.4f, seeds)
    }
}
