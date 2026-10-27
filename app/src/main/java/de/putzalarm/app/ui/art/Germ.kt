package de.putzalarm.app.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin

enum class GermKind { BLOB, SPIKE, BEAN, CYCLOPS }

fun shade(c: Color, amount: Float): Color =
    if (amount < 0) lerp(c, Color.Black, -amount) else lerp(c, Color.White, amount)

private val PupilInk = Color(0xFF1D0D22)
private val MouthInk = Color(0xFF3B0F24)
private val Tongue = Color(0xFFFF7AA2)

private fun round(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun bodyPath(kind: GermKind, seed: Float): Path = Path().apply {
    when (kind) {
        GermKind.SPIKE -> for (i in 0 until 18) {
            val a = i / 18f * Math.PI.toFloat() * 2f
            val r = if (i % 2 == 1) 29f else 39f
            if (i == 0) moveTo(cos(a) * r, sin(a) * r) else lineTo(cos(a) * r, sin(a) * r)
        }
        GermKind.BEAN -> addOval(androidx.compose.ui.geometry.Rect(-31f, -37f, 31f, 37f))
        else -> {
            val k = if (kind == GermKind.CYCLOPS) 7 else 5
            for (i in 0..60) {
                val a = i / 60f * Math.PI.toFloat() * 2f
                val r = 35f + 4f * sin(k * a + seed)
                if (i == 0) moveTo(cos(a) * r, sin(a) * r) else lineTo(cos(a) * r, sin(a) * r)
            }
        }
    }
    close()
}

/**
 * Zeichnet ein Monster-Bakterium wie im Mockup (Einheitenraster 120 × 120, Mitte = 0,0).
 * [wiggle] bewegt die Ärmchen (-1..1), [look] verschiebt die Pupillen, [tint] färbt alles
 * einfarbig (für gesperrte Sticker).
 */
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
    val s = sizePx / 120f
    fun c(x: Color) = tint ?: x
    val body = c(color)
    val dk = c(shade(color, -0.32f))
    val lt = c(shade(color, 0.45f))
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, Offset.Zero)
        scale(s, s, Offset.Zero)
    }) {
        // Ärmchen
        val hy = -24f + wiggle * 7f
        val arms = Path().apply {
            moveTo(-30f, 4f); quadraticTo(-46f, -6f, -44f, hy)
            moveTo(30f, 4f); quadraticTo(46f, -6f, 44f, hy)
        }
        drawPath(arms, dk, alpha = alpha, style = round(5f))
        for (x in listOf(-44f, 44f)) {
            drawCircle(body, 5f, Offset(x, hy - 2f), alpha = alpha)
            drawCircle(dk, 5f, Offset(x, hy - 2f), alpha = alpha, style = Stroke(2.5f))
        }
        // Fühler der Bohne
        if (kind == GermKind.BEAN) {
            val ant = Path().apply {
                moveTo(-10f, -34f); quadraticTo(-16f, -50f, -24f, -52f)
                moveTo(10f, -34f); quadraticTo(16f, -50f, 24f, -52f)
            }
            drawPath(ant, dk, alpha = alpha, style = round(3.5f))
            for (x in listOf(-24f, 24f)) {
                drawCircle(lt, 5f, Offset(x, -52f), alpha = alpha)
                drawCircle(dk, 5f, Offset(x, -52f), alpha = alpha, style = Stroke(2f))
            }
        }
        // Körper
        val p = bodyPath(kind, seed)
        if (kind == GermKind.SPIKE) {
            drawPath(p, body, alpha = alpha)
            drawPath(p, body, alpha = alpha, style = round(9f))
            scale(1.06f, 1.06f, Offset.Zero) { drawPath(p, dk, alpha = alpha, style = round(3f)) }
        } else {
            drawPath(p, body, alpha = alpha)
            drawPath(p, dk, alpha = alpha, style = Stroke(3f))
        }
        if (tint != null) return@withTransform
        // Flecken und Glanz
        drawCircle(dk, 4f, Offset(18f, 16f), alpha = 0.35f * alpha)
        drawCircle(dk, 3f, Offset(-20f, 20f), alpha = 0.35f * alpha)
        rotate(-25f, Offset(-15f, -26f)) {
            drawOval(Color.White, Offset(-23f, -30.5f), Size(16f, 9f), alpha = 0.45f * alpha)
        }
        // Augen
        val px = look * 2f
        if (kind == GermKind.CYCLOPS) {
            drawCircle(Color.White, 14f, Offset(0f, -10f), alpha = alpha)
            drawCircle(dk, 14f, Offset(0f, -10f), alpha = alpha, style = Stroke(2.5f))
            drawCircle(PupilInk, 6.5f, Offset(px, -13f), alpha = alpha)
            drawCircle(Color.White, 2f, Offset(px + 2f, -15f), alpha = alpha)
            drawLine(dk, Offset(-14f, -30f), Offset(14f, -30f), 3.5f, StrokeCap.Round, alpha = alpha)
        } else {
            for (x in listOf(-13f, 13f)) {
                drawCircle(Color.White, 9.5f, Offset(x, -10f), alpha = alpha)
                drawCircle(dk, 9.5f, Offset(x, -10f), alpha = alpha, style = Stroke(2.5f))
                drawCircle(PupilInk, 4.6f, Offset(x + px, -12.5f), alpha = alpha)
                drawCircle(Color.White, 1.5f, Offset(x + px + 1.5f, -14f), alpha = alpha)
            }
            drawLine(dk, Offset(-22f, -21f), Offset(-7f, -26f), 3.5f, StrokeCap.Round, alpha = alpha)
            drawLine(dk, Offset(22f, -21f), Offset(7f, -26f), 3.5f, StrokeCap.Round, alpha = alpha)
        }
        // Schreimund
        drawOval(MouthInk, Offset(-10f, 1.5f), Size(20f, 25f), alpha = alpha)
        drawOval(Tongue, Offset(-6f, 17f), Size(12f, 8f), alpha = alpha)
        drawRect(Color.White, Offset(-5f, 2.5f), Size(4f, 4f), alpha = alpha)
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
