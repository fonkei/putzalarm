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
import de.putzalarm.app.data.newRewards
import de.putzalarm.app.data.BossHp
import de.putzalarm.app.data.Bosses
import de.putzalarm.app.data.bossOfWeek
import de.putzalarm.app.ui.art.BossImage
import de.putzalarm.app.ui.art.BossBar
import de.putzalarm.app.ui.art.GermImage
import androidx.compose.animation.core.tween
import de.putzalarm.app.ui.art.MonsterImage
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
    val unlocked = newRewards(child.stars - result.stars, child.stars)
    LaunchedEffect(Unit) {
        val sticker = newSticker?.let { Monsters[it] }?.let { if (it.rare) " Wow, ein seltenes Monster: ${it.name}!" else " Neu im Album: ${it.name}!" } ?: ""
        val starLine = if (result.stars == 1) " Ein Stern für dich!" else if (result.stars > 1) " ${result.stars} Sterne für dich!" else ""
        val reward = unlocked.lastOrNull()?.let { " Und neu für Super Bürsti: ${it.spoken}!" } ?: ""
        val after = child.bossDamageNow
        val boss = if (after >= BossHp && after - result.stars < BossHp) " Und du hast den Boss ${bossOfWeek().name} besiegt! Wahnsinn!" else ""
        say((if (clean == 4) "Super geputzt! Du hast ${result.monsters} Monster verjagt!" else "Gut gemacht! ${result.monsters} Monster verjagt.") + starLine + boss + sticker + reward)
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.6.u), verticalAlignment = Alignment.CenterVertically) {
                    val card = Modifier.height(9.u).padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.4.u, vertical = 1.2.u)
                    Row(card.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.8.u)) {
                        GermImage(Color(0xFF7BDC5A), de.putzalarm.app.ui.art.GermKind.BLOB, Modifier.width(5.5.u))
                        BasicText("${result.monsters}", style = lilita(4.4))
                    }
                    Row(card.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.8.u)) {
                        Star(true, Modifier.width(5.u))
                        BasicText("+${result.stars}", style = lilita(4.4))
                    }
                    BossHit(child, result.stars, card.weight(1.3f))
                }
                if (newSticker != null) {
                    val m = Monsters[newSticker]
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(if (m.rare) RareGold else M.Yellow, 2.4.u, 0.5.u, 0.8.u).padding(1.6.u),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.6.u),
                    ) {
                        MonsterImage(m, Modifier.size(8.u))
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
                if (unlocked.isNotEmpty()) {
                    // Neu freigeschaltet: Bilder der neuen Sachen für Super Bürsti
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(M.Mint, 2.4.u, 0.5.u, 0.8.u).padding(1.4.u),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.6.u),
                    ) {
                        IconArt(Icon.HANGER, Modifier.size(5.u))
                        unlocked.forEach { RewardImage(it, Modifier.height(9.u)) }
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

/** Karten für seltene Monster: Gold, gesperrt ein blasses Gold. */
private val RareGold = Color(0xFFFFE27A)
private val RareLocked = Color(0xFFF3D98C)

/** Der Boss der Woche bekommt die neuen Sterne ab: Er wackelt, und sein Lebensbalken schrumpft. */
@Composable
private fun BossHit(child: Child, stars: Int, modifier: Modifier) {
    val boss = bossOfWeek()
    val after = child.bossDamageNow
    val before = (after - stars).coerceAtLeast(0)
    val left = remember { Animatable(1f - before.toFloat() / BossHp) }
    val hit = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3400L)
        if (before >= BossHp) return@LaunchedEffect
        hit.snapTo(1f)
        left.animateTo((1f - after.toFloat() / BossHp).coerceAtLeast(0f), tween(1100))
        hit.animateTo(0f, tween(500))
    }
    val beaten = left.value <= 0f
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.8.u)) {
        BossImage(boss, Modifier.width(6.u), beaten = beaten, hit = hit.value)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.6.u)) {
            BossBar(left.value, Modifier.fillMaxWidth().height(2.u))
            if (beaten) Row(horizontalArrangement = Arrangement.spacedBy(0.3.u)) { repeat(3) { Star(true, Modifier.width(2.4.u)) } }
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
        Spacer(Modifier.height(1.6.u))
        // Besiegte Bosse (einer pro Woche); noch nie besiegte nur als Schatten
        Row(
            Modifier.fillMaxWidth().comic(Color.White, 2.4.u, 0.5.u).padding(horizontal = 2.u, vertical = 1.u),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Bosses.forEach { b ->
                val wins = child.bossWins.count { it == b.id }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BossImage(b, Modifier.width(7.u), beaten = wins > 0, tint = if (wins > 0) null else M.Ink.copy(alpha = 0.25f))
                    if (wins > 1) BasicText("×$wins", style = lilita(2.4))
                }
            }
        }
        Spacer(Modifier.height(0.8.u))
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
                            .comic(
                                when {
                                    fresh -> M.Yellow
                                    m.rare -> if (owned) RareGold else RareLocked
                                    owned -> Color.White
                                    else -> M.LockPink
                                },
                                2.u, 0.5.u,
                            )
                            .padding(1.u),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        MonsterImage(m, Modifier.fillMaxWidth(0.72f), tint = if (owned) null else M.Ink.copy(alpha = 0.25f))
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
