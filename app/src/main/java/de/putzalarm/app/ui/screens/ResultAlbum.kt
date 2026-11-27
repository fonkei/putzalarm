package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import de.putzalarm.app.data.SessionOutcome
import de.putzalarm.app.data.Giants
import de.putzalarm.app.data.GiantStars
import de.putzalarm.app.data.ChestGear
import de.putzalarm.app.data.chestGear
import de.putzalarm.app.ui.art.GiantImage
import de.putzalarm.app.data.newRewards
import de.putzalarm.app.data.effectNow
import de.putzalarm.app.ui.art.CelebrationOverlay
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
fun ResultScreen(
    child: Child, result: SessionResult, outcome: SessionOutcome, onHome: () -> Unit, onAlbum: () -> Unit, say: (String) -> Unit,
    sfx: de.putzalarm.app.audio.Sfx? = null,
) {
    val newSticker = outcome.sticker
    val earned = outcome.starsTotal
    val clean = result.zones.count { it }
    val stars = when { result.stars >= 10 -> 3; result.stars >= 6 -> 2; else -> 1 }
    val missed = result.zones.indexOfFirst { !it }
    val unlocked = newRewards(child.stars - earned, child.stars)
    // Erst die Überraschungen (Ei, dann Schatzkiste), danach das Ergebnis mit Ansage
    val scenes = remember { listOfNotNull("ei".takeIf { outcome.egg != null || outcome.eggStars > 0 }, "kiste".takeIf { outcome.chest != null }) }
    var scene by remember { mutableStateOf(0) }
    val ready = scene >= scenes.size
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        val sticker = newSticker?.let { Monsters[it] }?.let { if (it.rare) " Wow, ein seltenes Monster: ${it.name}!" else " Neu im Album: ${it.name}!" } ?: ""
        val starLine = if (earned == 1) " Ein Stern für dich!" else if (earned > 1) " $earned Sterne für dich!" else ""
        val reward = unlocked.lastOrNull()?.let { " Und neu für Super Bürsti: ${it.spoken}!" } ?: ""
        val after = child.bossDamageNow
        val before = (after - earned).coerceAtLeast(0)
        // Besiegte Bosse sagt Bürsti gleich zuerst an, damit es nicht in der langen Ansage untergeht
        val boss = if (after / BossHp > before / BossHp) "Boss besiegt! Du hast ${bossOfWeek(before / BossHp).name} geschafft! Wahnsinn! " else ""
        val giant = result.giant?.let { Giants.getOrNull(it) }?.let { g ->
            if (result.giantBeaten) "" else " ${g.name} ist entkommen. Nächstes Mal schaffst du ihn!"
        } ?: ""
        val grown = outcome.levelUps.firstOrNull()?.let { (id, lv) -> " ${Monsters[id].name} ist gewachsen!" } ?: ""
        val giantWon = result.giant?.let { Giants.getOrNull(it) }?.takeIf { result.giantBeaten }?.let { "Riesen-Boss besiegt! ${it.name} ist weg! " } ?: ""
        say(boss + giantWon + (if (clean == 4) "Super geputzt! Du hast ${result.monsters} Monster verjagt!" else "Gut gemacht! ${result.monsters} Monster verjagt.") + starLine + giant + sticker + grown + reward)
    }
    BackHandler(onBack = { if (ready) onHome() else scene++ })
    Box(Modifier.fillMaxSize()) {
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
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(1.4.u)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.u), verticalAlignment = Alignment.CenterVertically) {
                    val card = Modifier.height(8.5.u).padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 1.u)
                    Row(card.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.u)) {
                        GermImage(Color(0xFF7BDC5A), de.putzalarm.app.ui.art.GermKind.BLOB, Modifier.width(5.5.u))
                        BasicText("${result.monsters}", style = lilita(4.4))
                    }
                    Row(card.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.u)) {
                        Star(true, Modifier.width(5.u))
                        BasicText("+$earned", style = lilita(4.4))
                    }
                }
                BossHit(
                    child, earned,
                    Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 2.4.u, vertical = 1.4.u),
                )
                // Was dazwischen passt, darf scrollen; die Knöpfe unten bleiben immer ganz
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(1.4.u),
                ) {
                    if (newSticker != null) {
                        val m = Monsters[newSticker]
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(if (m.rare) RareGold else M.Yellow, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 1.2.u),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.6.u),
                        ) {
                            MonsterImage(m, Modifier.size(6.5.u))
                            Column {
                                BasicText("Neu im Album:", style = nunito(1.8))
                                BasicText(m.name, style = lilita(3.4))
                            }
                        }
                    } else if (missed >= 0) {
                        Box(Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(M.Yellow, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 1.4.u)) {
                            BasicText("Da verstecken sich noch welche. Nächstes Mal erwischst du sie!", style = nunito(1.8))
                        }
                    }
                    // Riesen-Boss dieser Runde: besiegt mit Sternen, sonst fliegt er davon
                    result.giant?.let { id ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(if (result.giantBeaten) M.Mint else Color.White, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 0.8.u),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.6.u),
                        ) {
                            // Das echte, farbige Monster; besiegt mit dickem rotem Kreuz darüber
                            BossImage(Giants[id], Modifier.size(9.u), beaten = result.giantBeaten)
                            if (result.giantBeaten) repeat(GiantStars) { Star(true, Modifier.width(3.4.u)) }
                        }
                    }
                    // Monster, die durch erneutes Verjagen gewachsen sind
                    if (outcome.levelUps.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(Color.White, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 0.8.u),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.u),
                        ) {
                            outcome.levelUps.take(4).forEachIndexed { i, (id, lv) ->
                                val grow = remember { Animatable(levelScale(lv - 1)) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2600L + i * 400L)
                                    grow.animateTo(levelScale(lv) * 1.25f, tween(250))
                                    grow.animateTo(levelScale(lv), spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
                                }
                                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(7.u), contentAlignment = Alignment.Center) {
                                        MonsterImage(Monsters[id], Modifier.fillMaxSize().scale(grow.value))
                                    }
                                    LevelStars(lv, star = 1.5.u)
                                }
                            }
                        }
                    }
                    // Schatzkarte: ein Feld weiter
                    MapStrip(
                        outcome.mapBefore,
                        Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(Color(0xFFBFE8FF), 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 0.8.u),
                    )
                    if (unlocked.isNotEmpty()) {
                        // Neu freigeschaltet: Bilder der neuen Sachen für Super Bürsti
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 0.8.u).comic(M.Mint, 2.4.u, 0.5.u, 0.8.u).padding(horizontal = 1.8.u, vertical = 1.u),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.6.u),
                        ) {
                            IconArt(Icon.HANGER, Modifier.size(4.6.u))
                            unlocked.forEach { RewardImage(it, Modifier.height(7.u)) }
                        }
                    }
                }
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
    // Freigeschalteter Feier-Effekt über allem (fängt keine Berührungen ab)
    CelebrationOverlay(child.effectNow)
    when (scenes.getOrNull(scene)) {
        "ei" -> EggOverlay(outcome.egg, outcome.eggStars, say, sfx) { scene++ }
        "kiste" -> ChestOverlay(outcome.chest?.let { child.chestGear.getOrNull(it) }, outcome.chestStars, say, sfx) { scene++ }
    }
    }
}

/** Karten für Schlüpflinge aus dem Überraschungsei: zartes Pastell. */
private val EggCard = Color(0xFFDDF5FF)
/** Karten für seltene Monster: Gold, gesperrt ein blasses Gold. */
private val RareGold = Color(0xFFFFE27A)
/** Gesperrte seltene Kachel: blass, damit sie nicht wie schon gefunden aussieht; die Krone als Umriss verrät das Besondere. */
private val RareLocked = Color(0xFFEDE6D6)

/** Der Boss der Woche bekommt die neuen Sterne ab: Er wackelt, und sein Lebensbalken schrumpft. Besiegt bekommt er ein rotes Kreuz. */
@Composable
private fun BossHit(child: Child, stars: Int, modifier: Modifier) {
    val after = child.bossDamageNow
    val before = (after - stars).coerceAtLeast(0)
    // Der Boss, der zu Beginn dieser Runde dran war
    val k = before / BossHp
    val boss = bossOfWeek(k)
    val wins = after / BossHp > k
    val left = remember { Animatable(1f - (before % BossHp).toFloat() / BossHp) }
    val hit = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3400L)
        if (stars <= 0) return@LaunchedEffect
        hit.snapTo(1f)
        left.animateTo(if (wins) 0f else 1f - (after % BossHp).toFloat() / BossHp, tween(1100))
        hit.animateTo(0f, tween(500))
    }
    val beaten = left.value <= 0f
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.4.u)) {
        BossImage(boss, Modifier.width(7.u), beaten = beaten, hit = hit.value)
        Column(Modifier.weight(1f).padding(end = 0.6.u), verticalArrangement = Arrangement.spacedBy(1.u)) {
            BossBar(left.value, Modifier.fillMaxWidth().height(2.6.u))
            if (beaten) Row(horizontalArrangement = Arrangement.spacedBy(0.6.u)) { repeat(3) { Star(true, Modifier.width(2.6.u)) } }
        }
        // Gleich danach ist der nächste Boss dran
        if (beaten) BossImage(child.currentBoss, Modifier.width(5.u))
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
fun AlbumScreen(child: Child, highlight: Int?, onBack: () -> Unit, say: (String) -> Unit = {}) {
    var shown by remember { mutableStateOf<de.putzalarm.app.data.Monster?>(null) }
    BackHandler { if (shown != null) shown = null else onBack() }
    Box(Modifier.fillMaxSize()) {
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
        // Boss-Monster: besiegte (als Boss der Woche oder als Riesen-Boss) in Farbe mit rotem Kreuz, noch nie besiegte nur als Schatten
        Row(
            Modifier.fillMaxWidth().comic(Color.White, 2.4.u, 0.5.u).padding(horizontal = 2.u, vertical = 0.6.u),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Bosses.forEach { b ->
                val wins = child.bossWinsOf(b.id)
                Row(
                    Modifier.clickable(remember { MutableInteractionSource() }, null) {
                        say(if (wins > 0) "${b.name}! Den hast du schon ${if (wins == 1) "einmal" else "$wins mal"} besiegt." else "Diesen Boss hast du noch nicht besiegt. Vielleicht kommt er bald!")
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BossImage(b, Modifier.width(8.u), beaten = wins > 0, tint = if (wins > 0) null else M.Ink.copy(alpha = 0.25f))
                    if (wins > 1) BasicText("×$wins", style = lilita(2.4))
                }
            }
        }
        Spacer(Modifier.height(0.8.u))
        LazyVerticalGrid(
            GridCells.Fixed(5),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 1.6.u, bottom = 2.u),
            horizontalArrangement = Arrangement.spacedBy(2.u),
            verticalArrangement = Arrangement.spacedBy(2.u),
        ) {
            items(Monsters, key = { it.id }) { m ->
                val owned = m.id in child.stickers
                val fresh = m.id == highlight
                val lv = if (owned) child.level(m.id) else 1
                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null) {
                        shown = m
                        say(if (owned) "Das ist ${m.name}!" else "Den hast du noch nicht. Putz weiter, dann findest du ihn!")
                    },
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.82f)
                            .rotate(if (fresh) -3f else 0f)
                            .comic(
                                when {
                                    fresh -> M.Yellow
                                    m.rare -> if (owned) RareGold else RareLocked
                                    m.egg -> if (owned) EggCard else RareLocked
                                    owned -> Color.White
                                    else -> M.LockPink
                                },
                                2.u, 0.5.u,
                            )
                            .padding(1.u),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Je öfter es verjagt wurde, desto größer ist es
                        MonsterImage(m, Modifier.fillMaxWidth(0.68f).scale(levelScale(lv)), tint = if (owned) null else M.Ink.copy(alpha = 0.25f))
                        // Platz für zwei Zeilen, damit lange Namen ganz zu lesen sind
                        Box(Modifier.fillMaxWidth().height(6.u), contentAlignment = Alignment.Center) {
                            if (owned) BasicText(m.name, style = lilita(2.4, align = TextAlign.Center), maxLines = 2)
                            else IconArt(Icon.LOCK, Modifier.size(3.u), color = M.Ink.copy(alpha = 0.3f))
                        }
                    }
                    LevelStars(lv, Modifier.align(Alignment.TopStart).padding(1.u), star = 1.6.u)
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
    shown?.let { m -> MonsterCard(m, owned = m.id in child.stickers, level = child.level(m.id), onClose = { shown = null }) }
    }
}

/** Großansicht eines Monsters aus dem Album; antippen schließt sie wieder. */
@Composable
private fun MonsterCard(m: de.putzalarm.app.data.Monster, owned: Boolean, level: Int = 1, onClose: () -> Unit) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "hüpfen")
    val bounce by t.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "k",
    )
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(m) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
    Box(
        Modifier.fillMaxSize().background(M.Ink.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.scale(pop.value).fillMaxHeight(0.86f).aspectRatio(0.8f)
                .comic(if (!owned) (if (m.rare || m.egg) RareLocked else M.LockPink) else if (m.rare) RareGold else if (m.egg) EggCard else Color.White, 4.u, 0.7.u, 1.2.u).padding(3.u),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MonsterImage(
                m,
                Modifier.fillMaxWidth(0.9f).offset(y = (-2 * bounce).u).scale(1f + 0.04f * bounce, 1f - 0.04f * bounce),
                tint = if (owned) null else M.Ink.copy(alpha = 0.25f),
            )
            if (owned) LevelStars(level, star = 4.u)
            Spacer(Modifier.height(2.u))
            if (owned) BasicText(m.name, style = lilita(5.4, align = TextAlign.Center), maxLines = 2)
            else IconArt(Icon.LOCK, Modifier.size(7.u), color = M.Ink.copy(alpha = 0.3f))
        }
    }
}

