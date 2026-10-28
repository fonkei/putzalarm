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
fun WardrobeScreen(child: Child, onPick: (costume: String, cape: Int) -> Unit, onBack: () -> Unit, say: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val outfit = child.outfit
    var preview by remember { mutableStateOf<HeroPose?>(null) }
    fun locked(stars: Int) = say(if (stars - child.stars == 1) "Noch ein Stern, dann ist das frei!" else "Noch ${stars - child.stars} Sterne, dann ist das frei!")
    Row(Modifier.fillMaxSize().background(M.Lilac).padding(horizontal = 4.u, vertical = 3.u), horizontalArrangement = Arrangement.spacedBy(3.u)) {
        Box(Modifier.weight(0.75f).fillMaxHeight()) {
            RoundIconButton(Icon.BACK, onBack, Modifier.align(Alignment.TopStart), size = 6.u)
            Mascot(Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.88f), pose = preview, onTap = { preview = null })
        }
        Column(Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(1.6.u)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.u)) {
                StarChip(child.stars)
                NextRewardChip(child.stars)
            }
            Shelf {
                Costume.entries.forEach { c ->
                    val need = costumeStars(c)
                    Tile(
                        Modifier.weight(1f).aspectRatio(0.8f), need, child.stars, selected = outfit.costume == c,
                        onClick = { if (child.stars >= need) onPick(c.key, child.cape) else locked(need) },
                    ) { Mascot(Modifier.fillMaxSize(), pose = HeroPose.FLEX, outfit = outfit.copy(costume = c), still = true) }
                }
            }
            Shelf {
                CapeColors.forEachIndexed { i, col ->
                    val need = capeStars(i)
                    Tile(
                        Modifier.weight(1f).aspectRatio(0.8f), need, child.stars, selected = outfit.cape == col,
                        onClick = { if (child.stars >= need) onPick(child.costume, i) else locked(need) },
                    ) { CapeSwatch(col, Modifier.fillMaxSize().padding(0.8.u)) }
                }
            }
            LazyRow(
                Modifier.fillMaxWidth().weight(1f).comic(Color.White, 2.4.u, 0.45.u).padding(1.2.u),
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
private fun Tile(modifier: Modifier, need: Int, have: Int, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val open = have >= need
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
            Row(
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
    }
}
