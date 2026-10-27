package de.putzalarm.app.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import de.putzalarm.app.ui.theme.M

private fun round(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Maskottchen „Kapitän Borste“, eine Zahnbürste mit Umhang. Raster 300 × 330. */
@Composable
fun Mascot(modifier: Modifier = Modifier, wave: Boolean = true) {
    val t = rememberInfiniteTransition(label = "mascot")
    val arm by t.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "arm")
    Canvas(modifier.aspectRatio(300f / 330f)) {
        val s = size.width / 300f
        scale(s, s, Offset.Zero) {
            val cape = Path().apply {
                moveTo(150f, 120f); quadraticTo(60f, 170f, 70f, 330f); lineTo(230f, 330f); quadraticTo(240f, 170f, 150f, 120f); close()
            }
            drawPath(cape, M.Pink)
            drawPath(cape, M.Ink, style = round(7f))
            drawRoundRect(M.Blue, Offset(105f, 110f), Size(90f, 220f), CornerRadius(44f))
            drawRoundRect(M.Ink, Offset(105f, 110f), Size(90f, 220f), CornerRadius(44f), style = Stroke(7f))
            for ((x, top) in listOf(110f to 8f, 130f to 2f, 150f to 0f, 170f to 2f, 190f to 8f)) {
                drawLine(M.Ink, Offset(x, 30f), Offset(x, top), 7f, StrokeCap.Round)
            }
            drawRoundRect(M.Mint, Offset(95f, 30f), Size(110f, 80f), CornerRadius(18f))
            drawRoundRect(M.Ink, Offset(95f, 30f), Size(110f, 80f), CornerRadius(18f), style = Stroke(7f))
            for ((x, px) in listOf(132f to 135f, 170f to 173f)) {
                drawCircle(Color.White, 13f, Offset(x, 168f))
                drawCircle(M.Ink, 13f, Offset(x, 168f), style = Stroke(5f))
                drawCircle(M.Ink, 5.5f, Offset(px, 166f))
            }
            val mouth = Path().apply { moveTo(126f, 198f); quadraticTo(151f, 222f, 176f, 198f); close() }
            drawPath(mouth, M.Ink)
            drawPath(mouth, M.Ink, style = round(6f))
            drawLine(M.Ink, Offset(108f, 148f), Offset(192f, 148f), 12f, StrokeCap.Round)
            rotate(if (wave) arm * 18f - 6f else 0f, Offset(106f, 230f)) {
                val a = Path().apply { moveTo(106f, 230f); quadraticTo(60f, 200f, 52f, 160f) }
                drawPath(a, M.Ink, style = round(10f))
                drawCircle(Color.White, 16f, Offset(52f, 154f))
                drawCircle(M.Ink, 16f, Offset(52f, 154f), style = Stroke(6f))
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
enum class Icon { PLAY, PAUSE, CLOSE, HOME, CHECK, CAMERA, GALLERY, SUN, MOON, BACK, TRASH, PENCIL }

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
