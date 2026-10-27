package de.putzalarm.app.ui.art

import androidx.compose.foundation.Canvas
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
