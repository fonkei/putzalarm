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

/**
 * Maskottchen „Super Bürsti“: eine schlanke Zahnbürste als Superheld. Der Bürstenkopf ist der Kopf
 * mit Heldenmaske, die Borsten sind die Frisur, der Griff ist der Körper mit B-Wappen. Raster 240 × 320.
 */
@Composable
fun Mascot(modifier: Modifier = Modifier, wave: Boolean = true) {
    val t = rememberInfiniteTransition(label = "mascot")
    val flex by t.animateFloat(0f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "flex")
    Canvas(modifier.aspectRatio(240f / 320f)) {
        val s = size.width / 240f
        scale(s, s, Offset.Zero) { drawSuperBuersti(if (wave) flex else 0.5f) }
    }
}

private val BristleSky = Color(0xFFCFE9FF)
private val CapeShade = Color(0xFFD92A68)

private fun DrawScope.drawSuperBuersti(flex: Float) {
    val ink = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Umhang
    val cape = Path().apply {
        moveTo(104f, 162f); quadraticTo(62f, 222f, 58f, 304f); lineTo(120f, 290f); lineTo(182f, 304f); quadraticTo(178f, 222f, 136f, 162f); close()
    }
    drawPath(cape, M.Pink)
    drawPath(cape, M.Ink, style = ink)
    drawPath(Path().apply { moveTo(138f, 166f); quadraticTo(168f, 220f, 174f, 290f) }, CapeShade, style = round(5f))
    // Beine und Schuhe
    drawLine(M.Ink, Offset(112f, 276f), Offset(112f, 296f), 8f, StrokeCap.Round)
    drawLine(M.Ink, Offset(128f, 276f), Offset(128f, 296f), 8f, StrokeCap.Round)
    for (x in listOf(107f, 133f)) {
        drawOval(M.Pink, Offset(x - 13f, 294f), Size(26f, 14f))
        drawOval(M.Ink, Offset(x - 13f, 294f), Size(26f, 14f), style = Stroke(4f))
    }
    // Körper (Griff) mit B-Wappen
    drawRoundRect(M.Blue, Offset(101f, 150f), Size(38f, 132f), CornerRadius(19f))
    drawRoundRect(M.Ink, Offset(101f, 150f), Size(38f, 132f), CornerRadius(19f), style = Stroke(6f))
    drawCircle(M.Yellow, 14f, Offset(120f, 194f))
    drawCircle(M.Ink, 14f, Offset(120f, 194f), style = Stroke(4f))
    drawLetterB(Offset(120f, 194f), 18f)
    // Arme in Muskel-Pose, die Fäuste wippen
    val lift = (flex - 0.5f) * 8f
    for (side in listOf(-1f, 1f)) {
        val sx = 120f + side * 17f
        val fx = 120f + side * 46f
        val fy = 146f - lift
        drawPath(Path().apply { moveTo(sx, 180f); quadraticTo(120f + side * 42f, 176f, fx, fy + 6f) }, M.Ink, style = round(9f))
        drawCircle(Color.White, 11f, Offset(fx, fy))
        drawCircle(M.Ink, 11f, Offset(fx, fy), style = Stroke(4f))
        drawLine(M.Ink, Offset(fx - side * 8f, fy - 12f), Offset(fx - side * 2f, fy - 22f), 4f, StrokeCap.Round)
        drawLine(M.Ink, Offset(fx + side * 6f, fy - 16f), Offset(fx + side * 8f, fy - 28f), 4f, StrokeCap.Round)
    }
    // Borsten als Frisur
    for (i in 0 until 5) {
        val h = 30f + (if (i % 2 == 1) 6f else 0f) - kotlin.math.abs(i - 2) * 3f
        val tl = Offset(91f + i * 12f, 60f - h)
        drawRoundRect(if (i % 2 == 1) BristleSky else Color.White, tl, Size(10f, h + 8f), CornerRadius(5f))
        drawRoundRect(M.Ink, tl, Size(10f, h + 8f), CornerRadius(5f), style = Stroke(4f))
    }
    // Kopf (Bürstenkopf)
    drawRoundRect(M.Mint, Offset(86f, 54f), Size(68f, 102f), CornerRadius(22f))
    drawRoundRect(M.Ink, Offset(86f, 54f), Size(68f, 102f), CornerRadius(22f), style = Stroke(6f))
    // Heldenmaske mit flatterndem Band
    val mask = Path().apply {
        moveTo(89f, 94f); quadraticTo(120f, 84f, 151f, 94f); lineTo(151f, 116f); quadraticTo(134f, 122f, 120f, 113f); quadraticTo(106f, 122f, 89f, 116f); close()
    }
    drawPath(mask, M.Ink)
    drawPath(Path().apply { moveTo(151f, 99f); lineTo(167f, 93f - lift * 0.5f); lineTo(163f, 104f); close() }, M.Ink)
    // Augen
    for ((x, px) in listOf(108f to 109.5f, 132f to 133.5f)) {
        drawOval(Color.White, Offset(x - 9f, 93f), Size(18f, 22f))
        drawOval(M.Ink, Offset(x - 9f, 93f), Size(18f, 22f), style = Stroke(3.5f))
        drawCircle(M.Ink, 4.8f, Offset(px, 106f))
        drawCircle(Color.White, 1.6f, Offset(px + 1.5f, 103.5f))
    }
    // Grinsen und Bäckchen
    val mouth = Path().apply { moveTo(106f, 128f); quadraticTo(120f, 142f, 134f, 128f); quadraticTo(120f, 135f, 106f, 128f); close() }
    drawPath(mouth, M.Ink)
    drawPath(mouth, M.Ink, style = Stroke(4f, join = StrokeJoin.Round))
    val teeth = Path().apply { moveTo(110f, 130.5f); quadraticTo(120f, 134f, 130f, 130.5f); lineTo(128.5f, 134f); quadraticTo(120f, 136.5f, 111.5f, 134f); close() }
    drawPath(teeth, Color.White)
    drawOval(M.Pink, Offset(89.5f, 122.5f), Size(11f, 7f), alpha = 0.45f)
    drawOval(M.Pink, Offset(139.5f, 122.5f), Size(11f, 7f), alpha = 0.45f)
}

/** Ein dickes „B“ als Pfad, damit es ohne Schrift im Wappen sitzt. */
private fun DrawScope.drawLetterB(c: Offset, h: Float) {
    val w = h * 0.7f
    val l = c.x - w / 2
    val t = c.y - h / 2
    val outer = Path().apply {
        moveTo(l, t); lineTo(l + w * 0.6f, t); quadraticTo(l + w, t, l + w, t + h * 0.25f)
        quadraticTo(l + w, t + h * 0.45f, l + w * 0.7f, t + h * 0.48f)
        quadraticTo(l + w * 1.05f, t + h * 0.55f, l + w * 1.05f, t + h * 0.75f)
        quadraticTo(l + w * 1.05f, t + h, l + w * 0.6f, t + h); lineTo(l, t + h); close()
    }
    drawPath(outer, M.Ink)
    drawRoundRect(M.Yellow, Offset(l + w * 0.3f, t + h * 0.16f), Size(w * 0.38f, h * 0.22f), CornerRadius(h * 0.1f))
    drawRoundRect(M.Yellow, Offset(l + w * 0.3f, t + h * 0.58f), Size(w * 0.44f, h * 0.24f), CornerRadius(h * 0.1f))
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
