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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import de.putzalarm.app.ui.theme.M
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Milchzahn-Gebiss (20 Zähne) von oben gesehen, in einem 400 × 330-Raster. Pro Kieferhälfte von hinten nach
 * vorn: zwei Backenzähne, ein Eckzahn, zwei Schneidezähne, jeweils mit eigener Form und Kauflächen-Rillen.
 * Zonen: 0 = oben links, 1 = oben rechts, 2 = unten links, 3 = unten rechts (so wie das Kind sie im Spiegelbild sieht).
 */
private enum class ToothType(val w: Float, val h: Float) { MOLAR2(34f, 32f), MOLAR1(30f, 29f), CANINE(22f, 23f), INCISOR2(19f, 17f), INCISOR1(23f, 18f) }

private class Tooth(
    val x: Float, val y: Float, val rot: Float, val w: Float, val h: Float,
    val zone: Int, val index: Int, val type: ToothType, val flip: Float,
) {
    val outline: Path = toothOutline(type, w, h)
    val grooves: Path = toothGrooves(type, w, h)
}

private val ArchTypes = listOf(
    ToothType.MOLAR2, ToothType.MOLAR1, ToothType.CANINE, ToothType.INCISOR2, ToothType.INCISOR1,
    ToothType.INCISOR1, ToothType.INCISOR2, ToothType.CANINE, ToothType.MOLAR1, ToothType.MOLAR2,
)

/** Ein Kieferbogen: Punkte der Mittellinie (für das Zahnfleisch) und die Zähne, nach Bogenlänge verteilt. */
private class Arch(val points: List<Offset>, val teeth: List<Tooth>)

private fun buildArch(arch: Int, cy: Float, dir: Float, rx: Float, ry: Float): Arch {
    val a0 = 0.07f * PI.toFloat()
    val n = 400
    val pts = (0..n).map { k ->
        val a = a0 + (PI.toFloat() - 2 * a0) * k / n
        Offset(200f - rx * cos(a), cy + dir * ry * sin(a))
    }
    val len = FloatArray(pts.size)
    for (i in 1 until pts.size) len[i] = len[i - 1] + (pts[i] - pts[i - 1]).getDistance()
    val gap = 2.5f
    val sc = len.last() / (ArchTypes.sumOf { it.w.toDouble() }.toFloat() + gap * (ArchTypes.size - 1))
    var pos = 0f
    val teeth = ArchTypes.mapIndexed { i, t ->
        val w = t.w * sc
        val c = pos + w / 2
        pos += w + gap * sc
        val k = len.indices.minBy { abs(len[it] - c) }
        val p = pts[k]
        val d = pts[min(k + 1, n)] - pts[max(k - 1, 0)]
        val ang = atan2(d.y, d.x) * 180f / PI.toFloat()
        // Lokal zeigt +y zur Bogenmitte: oben stimmt das schon, unten wird gespiegelt.
        Tooth(p.x, p.y, ang, w, t.h * min(sc, 1.15f), arch * 2 + if (i < 5) 0 else 1, i, t, if (dir < 0) 1f else -1f)
    }
    return Arch(pts.filterIndexed { i, _ -> i % 8 == 0 || i == n }, teeth)
}

private val Arches = listOf(buildArch(0, 160f, -1f, 150f, 114f), buildArch(1, 172f, 1f, 142f, 106f))
private val Teeth: List<Tooth> = Arches.flatMap { it.teeth }

/** Umriss im Zahn-Raster: x entlang des Bogens, y > 0 zur Bogenmitte (innen). */
private fun toothOutline(t: ToothType, w: Float, h: Float) = Path().apply {
    when (t) {
        ToothType.INCISOR1, ToothType.INCISOR2 -> {
            // Schneidezahn: breite, leicht gewölbte Außenseite, nach innen schmaler
            moveTo(-w / 2, -h * 0.05f)
            cubicTo(-w * 0.35f, -h * 0.55f, w * 0.35f, -h * 0.55f, w / 2, -h * 0.05f)
            cubicTo(w * 0.45f, h * 0.35f, w * 0.18f, h * 0.5f, 0f, h * 0.5f)
            cubicTo(-w * 0.18f, h * 0.5f, -w * 0.45f, h * 0.35f, -w / 2, -h * 0.05f)
        }
        ToothType.CANINE -> {
            // Eckzahn: rundliche Raute mit Spitze in der Mitte
            moveTo(0f, -h / 2)
            quadraticTo(w * 0.42f, -h * 0.42f, w / 2, 0f)
            quadraticTo(w * 0.42f, h * 0.45f, 0f, h / 2)
            quadraticTo(-w * 0.42f, h * 0.45f, -w / 2, 0f)
            quadraticTo(-w * 0.42f, -h * 0.42f, 0f, -h / 2)
        }
        else -> addRoundRect(RoundRect(-w / 2, -h / 2, w / 2, h / 2, CornerRadius(min(w, h) * 0.38f)))
    }
    close()
}

/** Schneidekante, Eckzahn-Spitze oder Kauflächen-Rillen der Backenzähne. */
private fun toothGrooves(t: ToothType, w: Float, h: Float) = Path().apply {
    when (t) {
        ToothType.INCISOR1, ToothType.INCISOR2 -> {
            moveTo(-w * 0.32f, -h * 0.2f); quadraticTo(0f, -h * 0.32f, w * 0.32f, -h * 0.2f)
        }
        ToothType.CANINE -> {
            moveTo(-w * 0.3f, 0f); lineTo(0f, -h * 0.12f); lineTo(w * 0.3f, 0f)
            moveTo(0f, -h * 0.12f); lineTo(0f, h * 0.25f)
        }
        else -> {
            moveTo(-w * 0.28f, -h * 0.02f); quadraticTo(0f, h * 0.08f, w * 0.28f, -h * 0.02f)
            for (sx in listOf(-1f, 1f)) {
                moveTo(sx * w * 0.28f, -h * 0.02f); lineTo(sx * w * 0.36f, -h * 0.3f)
                moveTo(sx * w * 0.28f, -h * 0.02f); lineTo(sx * w * 0.34f, h * 0.28f)
            }
            moveTo(0f, h * 0.06f); lineTo(0f, -h * 0.32f)
        }
    }
}

private val BrushSpots = listOf(Offset(150f, 120f), Offset(290f, 120f), Offset(150f, 222f), Offset(290f, 222f))

private fun DrawScope.drawGums() {
    for (arch in Arches) {
        val p = Path().apply {
            arch.points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        drawPath(p, M.Ink, style = Stroke(54f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, M.Gum, style = Stroke(47f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private inline fun DrawScope.inTooth(t: Tooth, block: DrawScope.() -> Unit) =
    withTransform({ translate(t.x, t.y); rotate(t.rot, Offset.Zero); scale(1f, t.flip, Offset.Zero) }, block)

private fun DrawScope.drawTooth(t: Tooth, fill: Color, stroke: Color, sw: Float, alpha: Float = 1f) {
    inTooth(t) {
        drawPath(t.outline, fill, alpha = alpha)
        // Glanz auf der Außenseite und etwas Schatten innen, damit der Zahn plastisch wirkt
        drawOval(lerp(fill, Color.White, 0.75f), Offset(-t.w * 0.36f, -t.h * 0.3f), Size(t.w * 0.36f, t.h * 0.2f), alpha = alpha)
        drawPath(t.grooves, shade(fill, -0.25f), alpha = 0.75f * alpha, style = Stroke(1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(t.outline, stroke, alpha = alpha, style = Stroke(sw, join = StrokeJoin.Round))
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

/**
 * Gebiss für das Ergebnis: geputzte Zonen strahlen weiß und blitzen. Über jeden sauberen Zahn läuft
 * ein Glanzstreifen, und überall funkeln kleine Sterne auf. Verpasste Zonen sind gelblich mit einem frechen Monster.
 */
@Composable
fun JawResult(done: List<Boolean>, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "shine")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "time")
    val glow by t.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
    Canvas(modifier.aspectRatio(400f / 330f)) {
        val s = size.width / 400f
        scale(s, s, Offset.Zero) {
            drawGums()
            for (tooth in Teeth) {
                val clean = done.getOrElse(tooth.zone) { false }
                if (clean) drawCircle(Color.White, max(tooth.w, tooth.h) * 0.8f, Offset(tooth.x, tooth.y), alpha = glow * 0.5f)
            }
            for (tooth in Teeth) {
                val clean = done.getOrElse(tooth.zone) { false }
                drawTooth(tooth, if (clean) Color.White else M.Dirty, M.Ink, 3.5f)
                if (!clean) continue
                // Glanzstreifen, der von links nach rechts über die Zähne wandert
                val sweep = ((time * 1.4f - tooth.x / 400f * 0.6f) % 1f + 1f) % 1f
                inTooth(tooth) {
                    clipPath(tooth.outline) {
                        val bx = -tooth.w + sweep * tooth.w * 3f
                        val band = Path().apply {
                            moveTo(bx, -tooth.h); lineTo(bx + tooth.w * 0.28f, -tooth.h)
                            lineTo(bx - tooth.w * 0.12f, tooth.h); lineTo(bx - tooth.w * 0.4f, tooth.h); close()
                        }
                        drawPath(band, Color(0xFFDDF7FF), alpha = 0.9f)
                    }
                }
            }
            // Funkelnde Sterne: jeder saubere Zahn blitzt zu seiner eigenen Zeit auf
            for (tooth in Teeth) {
                if (!done.getOrElse(tooth.zone) { false }) continue
                val phase = ((time + tooth.index * 0.137f + tooth.zone * 0.29f) % 1f)
                if (phase > 0.4f) continue
                val k = sin(phase / 0.4f * PI.toFloat())
                val c = Offset(tooth.x + tooth.w * 0.22f, tooth.y - tooth.h * 0.25f * tooth.flip)
                withTransform({ rotate(phase * 120f, c) }) {
                    drawSparkle(c, 13f * k, M.Yellow)
                    drawSparkle(c, 8f * k, Color.White)
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
