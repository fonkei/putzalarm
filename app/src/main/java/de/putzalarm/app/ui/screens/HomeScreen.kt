package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import de.putzalarm.app.data.Child
import de.putzalarm.app.data.Monsters
import de.putzalarm.app.ui.art.GermImage
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.Mascot
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.nunito
import de.putzalarm.app.ui.theme.u
import java.io.File
import java.time.LocalTime

/** Start: Kapitän Borste, großer Los-Knopf, Wochen-Serie und Zugang zum Album. */
@Composable
fun HomeScreen(
    child: Child,
    photoDir: File,
    onStart: () -> Unit,
    onAlbum: () -> Unit,
    onSwitch: () -> Unit,
    greet: (String) -> Unit,
) {
    LaunchedEffect(child.id) { greet("Hallo ${child.name}! Die Monster warten schon.") }
    BackHandler(onBack = onSwitch)
    val evening = LocalTime.now().hour >= 15
    DottedBackground {
        Mascot(Modifier.align(Alignment.BottomStart).padding(start = 4.u).fillMaxHeight(0.86f))
        Row(Modifier.align(Alignment.TopStart).padding(3.5.u), verticalAlignment = Alignment.CenterVertically) {
            ComicButton(onSwitch, bg = Color.White, radius = 10.u, border = 0.45.u, shadow = 0.6.u, padding = 0.4.u) {
                ChildAvatar(child, photoDir, 6.u)
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(3.5.u), horizontalArrangement = Arrangement.spacedBy(1.4.u)) {
            DayChip(Icon.SUN, !evening)
            DayChip(Icon.MOON, evening)
        }
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd).padding(end = 5.u),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText("Hallo ${child.name}!", style = lilita(7.6), maxLines = 2)
            Spacer(Modifier.height(1.6.u))
            BasicText("Die Monster warten schon. ${if (child.brushSeconds < 120) "90 Sekunden" else "2 Minuten"}, 4 Zonen.", style = nunito(2.4, weight = androidx.compose.ui.text.font.FontWeight.ExtraBold))
            Spacer(Modifier.height(2.6.u))
            ComicButton(onStart, Modifier.fillMaxWidth(), padding = 2.2.u) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.6.u)) {
                    IconArt(Icon.PLAY, Modifier.size(5.u), color = Color.White)
                    BasicText("Monster verjagen!", style = lilita(4.4, Color.White, TextAlign.Center))
                }
            }
            Spacer(Modifier.height(2.6.u))
            val week = child.week
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.u)) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { i, d ->
                    val on = week[i]
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).comic(if (on) M.Blue else Color.White, 10.u, 0.45.u),
                        contentAlignment = Alignment.Center,
                    ) { BasicText(d, style = lilita(2, if (on) Color.White else M.Ink)) }
                }
            }
            Spacer(Modifier.height(1.u))
            val s = child.streak
            BasicText(
                when (s) { 0 -> "Heute geht's los!"; 1 -> "1 Tag geputzt"; else -> "$s Tage in Folge geputzt" },
                style = nunito(2),
            )
            Spacer(Modifier.height(2.u))
            ComicButton(onAlbum, bg = Color.White, radius = 2.u, border = 0.45.u, shadow = 0.7.u, padding = 1.u) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.u)) {
                    val last = child.stickers.lastOrNull()?.let { Monsters[it] } ?: Monsters[0]
                    GermImage(last.color, last.kind, Modifier.width(5.u))
                    BasicText("Album  ${child.stickers.size} / ${Monsters.size}", style = lilita(2.6))
                }
            }
        }
    }
}

@Composable
private fun DayChip(icon: Icon, selected: Boolean) {
    Box(Modifier.comic(if (selected) M.Ink else Color.White, 3.u, 0.45.u).padding(horizontal = 1.8.u, vertical = 0.6.u)) {
        IconArt(icon, Modifier.size(3.u), color = if (selected) M.Yellow else M.Ink)
    }
}
