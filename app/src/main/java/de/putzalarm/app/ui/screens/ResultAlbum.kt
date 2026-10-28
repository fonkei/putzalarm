package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import de.putzalarm.app.data.Child
import de.putzalarm.app.data.Monsters
import de.putzalarm.app.ui.art.GermImage
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.JawResult
import de.putzalarm.app.ui.art.Star
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.nunito
import de.putzalarm.app.ui.theme.u

/** Ergebnis nach dem Putzen: Sterne, sauberes Gebiss, Monster-Zahl und der neue Sticker. */
@Composable
fun ResultScreen(child: Child, result: SessionResult, newSticker: Int?, onHome: () -> Unit, onAlbum: () -> Unit, say: (String) -> Unit) {
    val clean = result.zones.count { it }
    val stars = when { result.stars >= 10 -> 3; result.stars >= 6 -> 2; else -> 1 }
    val missed = result.zones.indexOfFirst { !it }
    LaunchedEffect(Unit) {
        val sticker = newSticker?.let { " Neu im Album: ${Monsters[it].name}!" } ?: ""
        val starLine = if (result.stars == 1) " Ein Stern für dich!" else if (result.stars > 1) " ${result.stars} Sterne für dich!" else ""
        say((if (clean == 4) "Super geputzt! Du hast ${result.monsters} Monster verjagt!" else "Gut gemacht! ${result.monsters} Monster verjagt.") + starLine + sticker)
    }
    BackHandler(onBack = onHome)
    Box(Modifier.fillMaxSize().background(M.Blue).padding(horizontal = 5.u, vertical = 4.u)) {
        BasicText(if (clean == 4) "Super geputzt!" else "Gut gemacht!", style = lilita(6.6, Color.White), modifier = Modifier.align(Alignment.TopStart))
        Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(1.2.u)) {
            for (i in 0 until 3) {
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(300L + i * 350L)
                    pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
                }
                Star(i < stars, Modifier.width(7.u).scale(pop.value))
            }
        }
        Row(Modifier.fillMaxSize().padding(top = 12.u), horizontalArrangement = Arrangement.spacedBy(4.u)) {
            Box(Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxHeight(0.95f).aspectRatio(400f / 330f)) {
                    JawResult(result.zones, Modifier.fillMaxSize())
                    // 0 bis 3 Sterne in der Ecke jeder Kieferzone (oben links, oben rechts, unten links, unten rechts)
                    val corners = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                    result.zoneStars.forEachIndexed { zone, n -> ZoneStars(n, zone, Modifier.align(corners[zone]).fillMaxWidth(0.22f)) }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.u)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.u)) {
                    Column(Modifier.weight(1f).padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(2.u)) {
                        BasicText("${result.monsters}", style = lilita(6))
                        BasicText("Monster verjagt", style = nunito(1.9))
                    }
                    Row(
                        Modifier.weight(1f).padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(2.u),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.u),
                    ) {
                        Star(true, Modifier.width(5.5.u))
                        BasicText("+${result.stars}", style = lilita(6))
                    }
                }
                if (newSticker != null) {
                    val m = Monsters[newSticker]
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(M.Yellow, 2.4.u, 0.5.u, 0.8.u).padding(1.6.u),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.6.u),
                    ) {
                        GermImage(m.color, m.kind, Modifier.size(8.u))
                        Column {
                            BasicText("Neu im Album:", style = nunito(1.9))
                            BasicText(m.name, style = lilita(4))
                        }
                    }
                } else if (missed >= 0) {
                    Box(Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(M.Yellow, 2.4.u, 0.5.u, 0.8.u).padding(2.u)) {
                        BasicText("Da verstecken sich noch welche. Nächstes Mal erwischst du sie!", style = nunito(1.9))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.u)) {
                    ComicButton(onAlbum, Modifier.weight(1f), bg = Color.White) {
                        BasicText("Album", style = lilita(3.4, align = TextAlign.Center))
                    }
                    ComicButton(onHome, Modifier.weight(1f)) {
                        IconArt(Icon.CHECK, Modifier.size(5.u), color = Color.White)
                    }
                }
            }
        }
    }
}

/** Sterne einer Kieferzone, die nacheinander aufploppen. */
@Composable
private fun ZoneStars(n: Int, zone: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(0.3.u)) {
        for (i in 0 until 3) {
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1400L + zone * 450L + i * 160L)
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
            }
            Star(i < n, Modifier.weight(1f).scale(pop.value))
        }
    }
}

/** Monster-Album: jedes verjagte Monster wird ein Sticker. Gesperrte zeigen nur ihren Schatten. */
@Composable
fun AlbumScreen(child: Child, highlight: Int?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(M.Pink).padding(horizontal = 5.u, vertical = 3.5.u)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icon.BACK, onBack, size = 6.u)
            Spacer(Modifier.width(2.u))
            BasicText("Monster-Album", style = lilita(6, Color.White), modifier = Modifier.weight(1f))
            BasicText(
                "${child.stickers.size} / ${Monsters.size}",
                style = lilita(3),
                modifier = Modifier.comic(Color.White, 3.u, 0.5.u).padding(horizontal = 2.u, vertical = 0.6.u),
            )
        }
        Spacer(Modifier.height(2.u))
        LazyVerticalGrid(
            GridCells.Fixed(6),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 1.6.u, bottom = 2.u),
            horizontalArrangement = Arrangement.spacedBy(2.u),
            verticalArrangement = Arrangement.spacedBy(2.u),
        ) {
            items(Monsters, key = { it.id }) { m ->
                val owned = m.id in child.stickers
                val fresh = m.id == highlight
                Box {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.92f)
                            .rotate(if (fresh) -3f else 0f)
                            .comic(if (fresh) M.Yellow else if (owned) Color.White else M.LockPink, 2.u, 0.5.u)
                            .padding(1.u),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GermImage(m.color, m.kind, Modifier.fillMaxWidth(0.72f), tint = if (owned) null else M.Ink.copy(alpha = 0.25f))
                        BasicText(if (owned) m.name else "?", style = lilita(2.2, align = TextAlign.Center), maxLines = 1)
                    }
                    if (fresh) {
                        BasicText(
                            "NEU!",
                            style = lilita(2, Color.White),
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.2.u, y = (-1.4).u).comic(M.Blue, 1.u, 0.4.u).padding(horizontal = 1.u, vertical = 0.3.u),
                        )
                    }
                }
            }
        }
    }
}
