package de.putzalarm.app.ui.art

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import de.putzalarm.app.ui.theme.M
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Milchzahn-Gebiss (20 Zähne) in einem 400 × 330-Raster. Zonen:
 * 0 = oben links, 1 = oben rechts, 2 = unten links, 3 = unten rechts (so wie das Kind sie im Spiegelbild sieht).
 */
private class Tooth(val x: Float, val y: Float, val rot: Float, val w: Float, val h: Float, val zone: Int, val index: Int)

private val Teeth: List<Tooth> = buildList {
    for ((arch, cy, dir) in listOf(Triple(0, 150f, -1f), Triple(1, 190f, 1f))) {
        for (i in 0 until 10) {
            val a = PI.toFloat() * (0.04f + 0.92f * (i / 9f))
            val x = 200f - 160f * cos(a)
            val y = cy + dir * 120f * sin(a)
            val fc = abs(i - 4.5f)
            val deg = a * 180f / PI.toFloat()
            val rot = if (dir < 0) deg - 90f else 90f - deg
            add(Tooth(x, y, rot, 20f + fc * 3.4f, 28f + fc * 1.6f, arch * 2 + if (i < 5) 0 else 1, i))
        }
    }
}

private val BrushSpots = listOf(Offset(150f, 120f), Offset(290f, 120f), Offset(150f, 222f), Offset(290f, 222f))

private fun DrawScope.drawGums() {
    val st = Stroke(60f, cap = StrokeCap.Round)
    drawArc(M.Gum, 180f, 180f, false, Offset(40f, 30f), Size(320f, 240f), style = st)
    drawArc(M.Gum, 0f, 180f, false, Offset(40f, 70f), Size(320f, 240f), style = st)
}

private fun DrawScope.drawTooth(t: Tooth, fill: Color, stroke: Color, sw: Float, alpha: Float = 1f) {
    withTransform({ translate(t.x, t.y); rotate(t.rot, Offset.Zero) }) {
        val tl = Offset(-t.w / 2, -t.h / 2)
        val sz = Size(t.w, t.h)
        val r = CornerRadius(t.w * 0.42f)
        drawRoundRect(fill, tl, sz, r, alpha = alpha)
        drawRoundRect(stroke, tl, sz, r, alpha = alpha, style = Stroke(sw))
    }
}

/** Gebiss für den Putz-Bildschirm: aktive Zone blinkt gelb, fertige Zonen sind grün, eine Bürste springt mit. */
@Composable
fun JawHud(activeZone: Int, done: List<Boolean>, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "jaw")
    val pulse by t.animateFloat(1f, 0.55f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "pulse")
    val zone = activeZone.coerceIn(0, 3)
    val spot by animateOffsetAsState(BrushSpots[zone], spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "brush")
    Canvas(modifier.aspectRatio(400f / 330f)) {
        val s = size.width / 400f
        scale(s, s, Offset.Zero) {
            drawGums()
            for (tooth in Teeth) {
                val now = tooth.zone == zone && activeZone >= 0
                when {
                    now -> drawTooth(tooth, M.Yellow, M.Pink, 8f, pulse)
                    done.getOrElse(tooth.zone) { false } -> drawTooth(tooth, M.Mint, M.Ink, 5f)
                    else -> drawTooth(tooth, Color.White, M.Ink, 5f)
                }
            }
            if (activeZone >= 0) {
                val flipX = if (zone % 2 == 1) -1f else 1f
                val flipY = if (zone >= 2) -1f else 1f
                withTransform({ translate(spot.x, spot.y); scale(flipX, flipY, Offset.Zero); rotate(-20f, Offset.Zero) }) {
                    drawToothbrush()
                }
            }
        }
    }
}

fun DrawScope.drawToothbrush(handle: Color = M.Pink) {
    val st = Stroke(5f)
    drawRoundRect(handle, Offset(-14f, -9f), Size(90f, 18f), CornerRadius(9f))
    drawRoundRect(M.Ink, Offset(-14f, -9f), Size(90f, 18f), CornerRadius(9f), style = st)
    drawRoundRect(handle, Offset(-40f, -12f), Size(34f, 24f), CornerRadius(6f))
    drawRoundRect(M.Ink, Offset(-40f, -12f), Size(34f, 24f), CornerRadius(6f), style = st)
    drawRoundRect(Color(0xFFE9FBFF), Offset(-38f, -26f), Size(30f, 14f), CornerRadius(3f))
    drawRoundRect(M.Ink, Offset(-38f, -26f), Size(30f, 14f), CornerRadius(3f), style = Stroke(4f))
}

/** Gebiss für das Ergebnis: geputzte Zonen strahlen weiß, verpasste sind gelblich mit einem frechen Monster. */
@Composable
fun JawResult(done: List<Boolean>, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(400f / 330f)) {
        val s = size.width / 400f
        scale(s, s, Offset.Zero) {
            drawGums()
            for (tooth in Teeth) {
                val clean = done.getOrElse(tooth.zone) { false }
                drawTooth(tooth, if (clean) Color.White else M.Dirty, M.Ink, 3.5f)
                if (clean && tooth.index % 3 == 1) {
                    withTransform({ translate(tooth.x, tooth.y); rotate(tooth.rot, Offset.Zero) }) { drawSparkle(Offset(tooth.w * 0.12f, -tooth.h * 0.3f), 6f) }
                }
            }
            for (zone in 0..3) {
                if (done.getOrElse(zone) { false }) continue
                val anchor = Teeth.filter { it.zone == zone }.let { it[it.size / 2] }
                val up = zone < 2
                drawGerm(
                    Offset(anchor.x, anchor.y + if (up) 8f else -8f), 64f,
                    if (zone % 2 == 0) Color(0xFF7BDC5A) else Color(0xFFFF5D8F),
                    if (zone % 2 == 0) GermKind.BLOB else GermKind.SPIKE,
                    rotation = if (zone % 2 == 0) -10f else 12f,
                )
            }
        }
    }
}

fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color = M.Mint) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r * 0.3f, c.y - r * 0.3f); lineTo(c.x + r, c.y); lineTo(c.x + r * 0.3f, c.y + r * 0.3f)
        lineTo(c.x, c.y + r); lineTo(c.x - r * 0.3f, c.y + r * 0.3f); lineTo(c.x - r, c.y); lineTo(c.x - r * 0.3f, c.y - r * 0.3f); close()
    }
    drawPath(p, color)
}
