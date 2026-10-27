package de.putzalarm.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import de.putzalarm.app.data.AvatarMonsters
import de.putzalarm.app.data.Child
import de.putzalarm.app.ui.art.GermImage
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.u
import java.io.File

/** Gelber Hintergrund mit Punkteraster wie im Mockup. */
@Composable
fun DottedBackground(color: Color = M.Yellow, content: @Composable BoxScope.() -> Unit) {
    val step = 6.u
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .drawBehind {
                val s = step.toPx()
                val r = s * 0.12f
                var y = s / 2
                while (y < size.height) {
                    var x = s / 2
                    while (x < size.width) {
                        drawCircle(M.Ink.copy(alpha = 0.08f), r, Offset(x, y))
                        x += s
                    }
                    y += s
                }
            },
        content = content,
    )
}

/** Rundes Profilbild: Foto des Kindes oder ein Monster. */
@Composable
fun ChildAvatar(photo: String?, avatar: Int, photoDir: File, size: Dp, modifier: Modifier = Modifier, border: Dp = size * 0.04f) {
    val bmp = remember(photo) {
        photo?.let { runCatching { BitmapFactory.decodeFile(File(photoDir, it).path)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (bmp != null) M.Cream else M.Lilac)
            .border(border, M.Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val m = AvatarMonsters[avatar.coerceIn(0, AvatarMonsters.lastIndex)]
            GermImage(m.color, m.kind, Modifier.fillMaxWidth(0.8f))
        }
    }
}

@Composable
fun ChildAvatar(child: Child, photoDir: File, size: Dp, modifier: Modifier = Modifier) =
    ChildAvatar(child.photo, child.avatar, photoDir, size, modifier)

/** Kleiner runder Symbolknopf (zurück, schließen, Pause …). */
@Composable
fun RoundIconButton(icon: Icon, onClick: () -> Unit, modifier: Modifier = Modifier, bg: Color = Color.White, size: Dp = 7.u, tint: Color = M.Ink) {
    ComicButton(onClick, modifier.size(size, size + 0.7.u), bg = bg, radius = size, border = 0.45.u, shadow = 0.7.u, padding = size * 0.22f) {
        IconArt(icon, Modifier.aspectRatio(1f), color = tint)
    }
}

@Composable
fun Pad(content: @Composable BoxScope.() -> Unit) = Box(Modifier.fillMaxSize().padding(4.u), content = content)
