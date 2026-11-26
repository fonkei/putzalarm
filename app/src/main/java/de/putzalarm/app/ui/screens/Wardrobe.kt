package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import de.putzalarm.app.data.CapeColors
import de.putzalarm.app.data.Backdrop
import de.putzalarm.app.data.BackdropReward
import de.putzalarm.app.data.Effect
import de.putzalarm.app.data.EffectReward
import de.putzalarm.app.data.JokesReward
import de.putzalarm.app.data.backdropNow
import de.putzalarm.app.data.backdropStars
import de.putzalarm.app.data.effectNow
import de.putzalarm.app.data.effectStars
import de.putzalarm.app.ui.art.BackdropPreview
import de.putzalarm.app.ui.art.EffectPreview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import de.putzalarm.app.data.CapeReward
import de.putzalarm.app.data.Child
import de.putzalarm.app.data.CostumeReward
import de.putzalarm.app.data.MoveReward
import de.putzalarm.app.data.Reward
import de.putzalarm.app.data.capeStars
import de.putzalarm.app.data.costumeStars
import de.putzalarm.app.data.moveStars
import de.putzalarm.app.data.nextReward
import de.putzalarm.app.data.outfit
import de.putzalarm.app.ui.art.Costume
import de.putzalarm.app.ui.art.Gear
import de.putzalarm.app.ui.art.GearSlot
import de.putzalarm.app.data.ChestGear
import de.putzalarm.app.data.gearStars
import de.putzalarm.app.data.hasGear
import de.putzalarm.app.data.wearing
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.LocalOutfit
import de.putzalarm.app.ui.art.Mascot
import de.putzalarm.app.ui.art.Star
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.u

/**
 * Bürstis Schrank: Kostüme, Umhänge und Moves. Was schon frei ist, tippt das Kind einfach an;
 * Gesperrtes zeigt ein Schloss und wie viele Sterne man dafür insgesamt braucht.
 */
@Composable
fun WardrobeScreen(child: Child, onPick: (Child) -> Unit, onBack: () -> Unit, say: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val outfit = child.outfit
    var preview by remember { mutableStateOf<HeroPose?>(null) }
    fun locked(stars: Int) = say(if (stars - child.stars == 1) "Noch ein Stern, dann ist das frei!" else "Noch ${stars - child.stars} Sterne, dann ist das frei!")
    Row(Modifier.fillMaxSize().background(M.Lilac).padding(horizontal = 4.u, vertical = 3.u), horizontalArrangement = Arrangement.spacedBy(3.u)) {
        Box(Modifier.weight(0.75f).fillMaxHeight()) {
            RoundIconButton(Icon.BACK, onBack, Modifier.align(Alignment.TopStart), size = 6.u)
            Mascot(Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.88f), pose = preview, onTap = { preview = null })
        }
        Column(
            Modifier.weight(1.25f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(1.6.u),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.u)) {
                StarChip(child.stars)
                NextRewardChip(child.stars)
            }
            Shelf {
                Costume.entries.forEach { c ->
                    val need = costumeStars(c)
                    Tile(
                        Modifier.weight(1f).aspectRatio(0.8f), need, child.stars, selected = outfit.costume == c,
                        onClick = { if (child.stars >= need) onPick(child.copy(costume = c.key)) else locked(need) },
                    ) { Mascot(Modifier.fillMaxSize(), pose = HeroPose.FLEX, outfit = outfit.copy(costume = c), still = true) }
                }
            }
            Shelf {
                CapeColors.forEachIndexed { i, col ->
                    val need = capeStars(i)
                    Tile(
                        Modifier.weight(1f).aspectRatio(0.8f), need, child.stars, selected = outfit.cape == col,
                        onClick = { if (child.stars >= need) onPick(child.copy(cape = i)) else locked(need) },
                    ) { CapeSwatch(col, Modifier.fillMaxSize().padding(0.8.u)) }
                }
            }
            // Hüte und Helme, dann Brillen; das erste Feld zieht alles aus. Kisten-Teile gibt es nur auf der Schatzkarte.
            for (slot in GearSlot.entries) {
                val worn = if (slot == GearSlot.HAT) child.hat else child.glasses
                LazyRow(
                    Modifier.fillMaxWidth().height(15.u).comic(Color.White, 2.4.u, 0.45.u).padding(1.2.u),
                    horizontalArrangement = Arrangement.spacedBy(1.2.u),
                ) {
                    item {
                        Tile(
                            Modifier.fillMaxHeight().aspectRatio(0.75f), 0, child.stars, selected = outfit.gear.none { it.slot == slot },
                            onClick = { onPick(if (slot == GearSlot.HAT) child.copy(hat = "") else child.copy(glasses = "")) },
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { IconArt(Icon.CLOSE, Modifier.fillMaxWidth(0.4f)) } }
                    }
                    items(Gear.entries.filter { it.slot == slot }.sortedBy { if (it in ChestGear) 10_000 + ChestGear.indexOf(it) else gearStars(it) }) { g ->
                        val chestIdx = ChestGear.indexOf(g)
                        val have = child.hasGear(g)
                        Tile(
                            Modifier.fillMaxHeight().aspectRatio(0.75f), if (chestIdx >= 0) -1 else gearStars(g), child.stars, selected = worn == g.key && have,
                            open = have,
                            onClick = {
                                when {
                                    have -> onPick(child.wearing(g))
                                    chestIdx >= 0 -> say("Das ist in einer Schatzkiste versteckt. Putz fleißig, dann findest du sie auf der Schatzkarte!")
                                    else -> locked(gearStars(g))
                                }
                            },
                        ) { GearPreview(g, Modifier.fillMaxSize()) }
                    }
                }
            }
            Shelf {
                Backdrop.entries.forEach { b ->
                    val need = backdropStars(b)
                    Tile(
                        Modifier.weight(1f).aspectRatio(1.2f), need, child.stars, selected = child.backdropNow == b,
                        onClick = { if (child.stars >= need) onPick(child.copy(backdrop = b.ordinal)) else locked(need) },
                    ) { BackdropPreview(b, Modifier.fillMaxSize().clip(RoundedCornerShape(1.u))) }
                }
            }
            Shelf {
                Effect.entries.forEach { e ->
                    val need = effectStars(e)
                    Tile(
                        Modifier.weight(1f).aspectRatio(1.2f), need, child.stars, selected = child.effectNow == e,
                        onClick = { if (child.stars >= need) onPick(child.copy(effect = e.ordinal)) else locked(need) },
                    ) {
                        if (e == Effect.NONE) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { IconArt(Icon.CLOSE, Modifier.fillMaxWidth(0.4f)) }
                        else EffectPreview(e, Modifier.fillMaxSize().clip(RoundedCornerShape(1.u)))
                    }
                }
            }
            LazyRow(
                Modifier.fillMaxWidth().height(18.u).comic(Color.White, 2.4.u, 0.45.u).padding(1.2.u),
                horizontalArrangement = Arrangement.spacedBy(1.2.u),
            ) {
                items(HeroPose.entries.toList()) { p ->
                    val need = moveStars(p)
                    Tile(
                        Modifier.fillMaxHeight().aspectRatio(0.75f), need, child.stars, selected = preview == p,
                        onClick = { if (child.stars >= need) preview = p else locked(need) },
                    ) { Mascot(Modifier.fillMaxSize(), pose = p, still = true) }
                }
            }
        }
    }
}

@Composable
private fun Shelf(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().comic(Color.White, 2.4.u, 0.45.u).padding(1.2.u),
        horizontalArrangement = Arrangement.spacedBy(1.2.u),
        content = content,
    )
}

/** Eine Kachel im Schrank; gesperrt mit Schloss und Sternenzahl, gewählt mit dickem blauem Rand. */
@Composable
private fun Tile(
    modifier: Modifier, need: Int, have: Int, selected: Boolean, onClick: () -> Unit,
    /** Ob frei; sonst nach Sternen. [need] < 0 = steckt in einer Schatzkiste (Kiste statt Sternenzahl). */
    open: Boolean = have >= need,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .comic(if (open) M.Cream else Color(0xFFDDD3EE), 1.6.u, if (selected) 0.7.u else 0.35.u, borderColor = if (selected) M.Blue else M.Ink)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(0.5.u),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().then(if (open) Modifier else Modifier.padding(0.4.u))) { content() }
        if (!open) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
            IconArt(Icon.LOCK, Modifier.fillMaxWidth(0.4f).align(Alignment.Center), color = M.Ink)
            if (need < 0) ChestIcon(Modifier.align(Alignment.BottomCenter).width(4.u))
            else Row(
                Modifier.align(Alignment.BottomCenter).comic(M.Yellow, 1.u, 0.3.u).padding(horizontal = 0.6.u, vertical = 0.2.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.3.u),
            ) {
                Star(true, Modifier.width(2.u))
                BasicText("$need", style = lilita(2))
            }
        }
    }
}

/** Ein Umhang als kleines Bild in seiner Farbe. */
@Composable
fun CapeSwatch(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(0.8f)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.32f, h * 0.1f); lineTo(w * 0.68f, h * 0.1f)
            cubicTo(w * 0.8f, h * 0.45f, w * 0.92f, h * 0.75f, w * 0.95f, h * 0.92f)
            quadraticBezierTo(w * 0.5f, h * 0.82f, w * 0.05f, h * 0.92f)
            cubicTo(w * 0.08f, h * 0.75f, w * 0.2f, h * 0.45f, w * 0.32f, h * 0.1f)
            close()
        }
        drawPath(p, color)
        drawPath(p, M.Ink, style = Stroke(w * 0.05f, join = StrokeJoin.Round))
        drawCircle(M.Yellow, w * 0.08f, Offset(w * 0.32f, h * 0.12f))
        drawCircle(M.Yellow, w * 0.08f, Offset(w * 0.68f, h * 0.12f))
    }
}

@Composable
fun StarChip(stars: Int, size: Dp = 4.u, text: Double = 3.2) {
    Row(
        Modifier.comic(Color.White, 3.u, 0.45.u, 0.6.u).padding(horizontal = 1.4.u, vertical = 0.6.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.8.u),
    ) {
        Star(true, Modifier.size(size))
        BasicText("$stars", style = lilita(text))
    }
}

/** Was als Nächstes frei wird: kleines Bild und ein Balken, der sich mit Sternen füllt. */
@Composable
fun NextRewardChip(stars: Int, modifier: Modifier = Modifier) {
    val next = nextReward(stars) ?: return
    val prev = de.putzalarm.app.data.Rewards.lastOrNull { it.stars <= stars }?.stars ?: 0
    val k = ((stars - prev).toFloat() / (next.stars - prev)).coerceIn(0f, 1f)
    Row(
        modifier.comic(Color.White, 3.u, 0.45.u, 0.6.u).padding(horizontal = 1.2.u, vertical = 0.5.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.u),
    ) {
        Box(Modifier.width(14.u).height(1.8.u).comic(M.Lilac, 1.u, 0.3.u)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(k.coerceAtLeast(0.08f)).comic(M.Yellow, 1.u, 0.3.u))
        }
        RewardImage(next, Modifier.height(5.u))
    }
}

/** Bild einer Belohnung: Super Bürsti mit dem Move oder Kostüm, oder der Umhang. */
@Composable
fun RewardImage(r: Reward, modifier: Modifier = Modifier) {
    val look = LocalOutfit.current
    when (r) {
        is MoveReward -> Mascot(modifier, pose = r.pose, still = true)
        is CostumeReward -> Mascot(modifier, pose = HeroPose.FLEX, outfit = look.copy(costume = r.costume), still = true)
        is CapeReward -> CapeSwatch(CapeColors[r.cape], modifier)
        is BackdropReward -> BackdropPreview(r.backdrop, modifier.aspectRatio(1.2f).clip(RoundedCornerShape(1.u)))
        is EffectReward -> EffectPreview(r.effect, modifier.aspectRatio(1.2f).clip(RoundedCornerShape(1.u)))
        is JokesReward -> JokeBubble(modifier)
        is de.putzalarm.app.data.GearReward -> GearPreview(r.gear, modifier.aspectRatio(0.75f))
    }
}

/** Sprechblase mit „Haha“: Super Bürsti erzählt Witze. */
@Composable
fun JokeBubble(modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1.3f).comic(Color.White, 2.u, 0.35.u), contentAlignment = Alignment.Center) {
        BasicText("Haha", style = lilita(2.2))
    }
}
