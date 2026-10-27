package de.putzalarm.app.ui.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import de.putzalarm.app.R

/** Farben der Designrichtung A „Monsterjagd“. */
object M {
    val Pink = Color(0xFFFF3D7F)
    val Yellow = Color(0xFFFFD23F)
    val Blue = Color(0xFF2F6BFF)
    val Mint = Color(0xFF3EE0B0)
    val Ink = Color(0xFF24123A)
    val Cream = Color(0xFFFFE9A8)
    val Gum = Color(0xFFFF8FB1)
    val LockPink = Color(0xFFFFD0DF)
    val Lilac = Color(0xFFE9DCFF)
    val Dirty = Color(0xFFFFE08A)
    val White = Color.White
}

val Lilita = FontFamily(Font(R.font.lilita_one))

@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

/**
 * Wie im Mockup sind alle Maße relativ zur Bildschirmbreite: 1 u ≈ 1 % der Breite
 * (bei sehr flachen Bildschirmen begrenzt durch die Höhe).
 */
val LocalU = staticCompositionLocalOf { 10.dp }

val Number.u: Dp
    @Composable @ReadOnlyComposable get() = LocalU.current * toFloat()

val Number.us: TextUnit
    @Composable @ReadOnlyComposable get() = with(LocalDensity.current) { (LocalU.current * toFloat()).toSp() }

@Composable
fun PutzalarmTheme(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = minOf(maxWidth / 100, maxHeight / 64)
        CompositionLocalProvider(LocalU provides unit) { content() }
    }
}

@Composable
@ReadOnlyComposable
fun lilita(size: Number, color: Color = M.Ink, align: TextAlign = TextAlign.Start) =
    TextStyle(fontFamily = Lilita, fontSize = size.us, lineHeight = size.us * 1.05f, color = color, textAlign = align)

@Composable
@ReadOnlyComposable
fun nunito(size: Number, color: Color = M.Ink, weight: FontWeight = FontWeight.Black, align: TextAlign = TextAlign.Start) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.us, lineHeight = size.us * 1.3f, color = color, textAlign = align)

/** Comic-Kasten: Fläche, dicke Tinten-Kontur und ein harter Schatten nach unten. */
fun Modifier.comic(
    bg: Color,
    radius: Dp,
    border: Dp,
    shadow: Dp = 0.dp,
    borderColor: Color = M.Ink,
): Modifier = drawBehind {
    val r = radius.toPx()
    val bw = border.toPx()
    val sh = shadow.toPx()
    if (sh > 0f) drawRoundRect(borderColor, topLeft = Offset(0f, sh), size = size, cornerRadius = CornerRadius(r))
    drawRoundRect(bg, size = size, cornerRadius = CornerRadius(r))
    if (bw > 0f) {
        drawRoundRect(
            borderColor,
            topLeft = Offset(bw / 2, bw / 2),
            size = Size(size.width - bw, size.height - bw),
            cornerRadius = CornerRadius((r - bw / 2).coerceAtLeast(0f)),
            style = Stroke(bw),
        )
    }
}

/** Großer Comic-Knopf, der beim Drücken in seinen Schatten sinkt. */
@Composable
fun ComicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = M.Pink,
    radius: Dp = 3.u,
    border: Dp = 0.6.u,
    shadow: Dp = 1.1.u,
    padding: Dp = 2.u,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink = if (pressed) shadow * 0.8f else 0.dp
    Box(
        modifier
            .padding(bottom = shadow)
            .offset(y = sink)
            .comic(bg, radius, border, shadow - sink)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
