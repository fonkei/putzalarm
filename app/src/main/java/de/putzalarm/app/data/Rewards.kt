package de.putzalarm.app.data

import androidx.compose.ui.graphics.Color
import de.putzalarm.app.ui.art.Costume
import de.putzalarm.app.ui.art.Gear
import de.putzalarm.app.ui.art.GearSlot
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Outfit

/** Umhangfarben für Super Bürsti; die erste (Pink) hat er von Anfang an. */
val CapeColors = listOf(
    Color(0xFFFF3D7F), Color(0xFF2F6BFF), Color(0xFFFFC928), Color(0xFF4CD964), Color(0xFF9B5BFF), Color(0xFF2A2A3A),
    Color(0xFFFF3B30), Color(0xFFDDE3EC),
    // Neu im November 2026 (nur hinten anhängen, die Nummer ist gespeichert)
    Color(0xFFFF8A00), Color(0xFF00C2C7), Color(0xFFFFA8D2),
)
private val CapeWords = listOf("pinker", "blauer", "goldener", "grüner", "lila", "schwarzer", "roter", "silberner", "oranger", "türkiser", "rosa")

/** Etwas, das ein Kind mit gesammelten Sternen für Super Bürsti freischaltet. [stars]: ab so vielen Sternen insgesamt. */
sealed interface Reward {
    val stars: Int
    /** So sagt Super Bürsti es an. */
    val spoken: String
}

data class MoveReward(override val stars: Int, val pose: HeroPose, override val spoken: String) : Reward
data class CapeReward(override val stars: Int, val cape: Int) : Reward {
    override val spoken get() = "ein ${CapeWords[cape]} Umhang"
}
data class CostumeReward(override val stars: Int, val costume: Costume, override val spoken: String) : Reward

/** Hintergrund für den Startbildschirm (siehe [Backdrop]). */
data class BackdropReward(override val stars: Int, val backdrop: Backdrop) : Reward {
    override val spoken get() = backdrop.spoken
}

/** Ein Effekt, der im Ergebnis über den Bildschirm regnet. */
data class EffectReward(override val stars: Int, val effect: Effect) : Reward {
    override val spoken get() = effect.spoken
}

/** Ein Hut, Helm oder eine Brille für Super Bürsti. */
data class GearReward(override val stars: Int, val gear: Gear) : Reward {
    override val spoken get() = gear.spoken
}

/** So sagt Super Bürsti ein Ausrüstungsteil an. */
val Gear.spoken: String
    get() = when (this) {
        Gear.PARTY -> "ein Partyhut"
        Gear.COWBOY -> "ein Cowboyhut"
        Gear.WIKINGER -> "ein Wikingerhelm"
        Gear.FEUERWEHR -> "ein Feuerwehrhelm"
        Gear.BAUHELM -> "ein Bauarbeiterhelm"
        Gear.PIRAT -> "ein Piratenhut"
        Gear.KRONE -> "eine goldene Krone"
        Gear.ZAUBERER -> "ein Zauberhut"
        Gear.SONNENBRILLE -> "eine coole Sonnenbrille"
        Gear.HERZBRILLE -> "eine Herzchenbrille"
        Gear.TAUCHER -> "eine Taucherbrille mit Schnorchel"
    }

/** Ausrüstung, die es nur in den Schatzkisten der Schatzkarte gibt, in dieser Reihenfolge (eine pro Kiste). */
val ChestGear = listOf(Gear.PIRAT, Gear.KRONE, Gear.TAUCHER, Gear.ZAUBERER)

/** Super Bürsti erzählt Witze, wenn man ihn antippt. */
data class JokesReward(override val stars: Int) : Reward {
    override val spoken get() = "Super Bürsti kann jetzt Witze erzählen. Tipp ihn mal an"
}

/** Hintergründe für den Start; Reihenfolge = gespeicherte Nummer, nur hinten anhängen. */
enum class Backdrop(val spoken: String) {
    DOTS("die gelben Punkte"), SPACE("der Weltraum-Hintergrund"), SEA("die Unterwasserwelt"),
    JUNGLE("der Dschungel"), RAINBOW("der Regenbogen"),
}

/** Effekte im Ergebnis; Reihenfolge = gespeicherte Nummer, nur hinten anhängen. */
enum class Effect(val spoken: String) {
    NONE(""), CONFETTI("Konfetti zum Feiern"), BUBBLES("Seifenblasen zum Feiern"), STARS("ein Sternenregen zum Feiern"),
}

/** Pro Putzrunde gibt es bis zu 12 Sterne, so kommt etwa alle ein bis zwei Tage etwas Neues. */
val Rewards: List<Reward> = listOf(
    MoveReward(10, HeroPose.MOONWALK, "der Moonwalk"),
    CapeReward(20, 1),
    MoveReward(35, HeroPose.KARATE, "Karate-Kicks"),
    CostumeReward(50, Costume.CHROM, "der glänzende Chrom-Anzug"),
    CapeReward(70, 2),
    MoveReward(90, HeroPose.ROBOT, "der Roboter-Tanz"),
    MoveReward(115, HeroPose.GUITAR, "die Luftgitarre"),
    CapeReward(140, 3),
    CostumeReward(170, Costume.NEON, "der leuchtende Neon-Anzug"),
    MoveReward(200, HeroPose.SPIN, "die Pirouette"),
    CapeReward(235, 4),
    MoveReward(270, HeroPose.FLY, "das Fliegen"),
    CapeReward(310, 5),
    // Ab hier neu im November 2026: noch mehr verschiedene Gewinne
    BackdropReward(330, Backdrop.SPACE),
    JokesReward(360),
    EffectReward(380, Effect.CONFETTI),
    CostumeReward(400, Costume.DARTH, "Darth Bürsti mit Lichtschwert-Zahnbürste"),
    BackdropReward(440, Backdrop.SEA),
    EffectReward(480, Effect.BUBBLES),
    CapeReward(520, 6),
    BackdropReward(560, Backdrop.JUNGLE),
    EffectReward(600, Effect.STARS),
    CapeReward(650, 7),
    BackdropReward(700, Backdrop.RAINBOW),
    // Neu im November 2026: Hüte, Brillen, neue Moves, Kostüme und Umhänge (auch dazwischen; wer schon so viele
    // Sterne hat, findet sie gleich frei im Schrank)
    GearReward(25, Gear.PARTY),
    MoveReward(60, HeroPose.FLOSS, "der Zahnseide-Tanz"),
    GearReward(80, Gear.SONNENBRILLE),
    MoveReward(125, HeroPose.DAB, "der Dab"),
    GearReward(155, Gear.COWBOY),
    MoveReward(185, HeroPose.CHICKEN, "der Hühnertanz"),
    GearReward(250, Gear.HERZBRILLE),
    MoveReward(290, HeroPose.HULA, "der Hula-Tanz"),
    GearReward(345, Gear.WIKINGER),
    CostumeReward(420, Costume.FEUER, "der feuerrote Anzug"),
    MoveReward(460, HeroPose.STOMP, "das Monster-Stampfen"),
    GearReward(540, Gear.FEUERWEHR),
    MoveReward(580, HeroPose.RUN, "das Rennen"),
    GearReward(620, Gear.BAUHELM),
    CapeReward(680, 8),
    CostumeReward(750, Costume.EIS, "der glitzernde Eis-Anzug"),
    CapeReward(820, 9),
    CapeReward(900, 10),
).sortedBy { it.stars }

/** Sterne, ab denen ein Kostüm, ein Umhang oder ein Move frei ist (0 = von Anfang an). */
fun costumeStars(c: Costume) = Rewards.filterIsInstance<CostumeReward>().firstOrNull { it.costume == c }?.stars ?: 0
fun capeStars(i: Int) = Rewards.filterIsInstance<CapeReward>().firstOrNull { it.cape == i }?.stars ?: 0
fun moveStars(p: HeroPose) = Rewards.filterIsInstance<MoveReward>().firstOrNull { it.pose == p }?.stars ?: 0
fun backdropStars(b: Backdrop) = Rewards.filterIsInstance<BackdropReward>().firstOrNull { it.backdrop == b }?.stars ?: 0
fun effectStars(e: Effect) = Rewards.filterIsInstance<EffectReward>().firstOrNull { it.effect == e }?.stars ?: 0
/** Ausrüstung: Kisten-Teile sind mit der passenden Schatzkiste frei, die übrigen ab ihren Sternen. */
fun gearStars(g: Gear) = Rewards.filterIsInstance<GearReward>().firstOrNull { it.gear == g }?.stars ?: 0
/** Die Kisten-Schätze in der Reihenfolge dieses Kindes. */
val Child.chestGear: List<Gear>
    get() = chestOrder.mapNotNull { Gear.of(it) }.takeIf { it.toSet() == ChestGear.toSet() && it.size == ChestGear.size } ?: ChestGear
fun Child.hasGear(g: Gear): Boolean = chestGear.indexOf(g).let { i -> if (i >= 0) chests > i else stars >= gearStars(g) }
/** Zieht [g] an (ersetzt den Hut bzw. die Brille). */
fun Child.wearing(g: Gear): Child = if (g.slot == GearSlot.HAT) copy(hat = g.key) else copy(glasses = g.key)
val Child.hasJokes get() = stars >= (Rewards.filterIsInstance<JokesReward>().firstOrNull()?.stars ?: 0)

/** Gewählter Hintergrund und Effekt, wenn schon frei (sonst die Grundeinstellung). */
val Child.backdropNow: Backdrop
    get() = Backdrop.entries.getOrNull(backdrop)?.takeIf { stars >= backdropStars(it) } ?: Backdrop.DOTS
val Child.effectNow: Effect
    get() = Effect.entries.getOrNull(effect)?.takeIf { stars >= effectStars(it) } ?: Effect.NONE

/** Was beim Sprung von [before] auf [after] Sterne neu dazukommt. */
fun newRewards(before: Int, after: Int) = Rewards.filter { it.stars in (before + 1)..after }

fun nextReward(stars: Int) = Rewards.firstOrNull { it.stars > stars }

/** So sieht Super Bürsti für dieses Kind aus: gewähltes Kostüm und Umhang (wenn frei) und alle freien Moves. */
val Child.outfit: Outfit
    get() {
        val suit = Costume.entries.firstOrNull { it.key == this.costume }?.takeIf { stars >= costumeStars(it) } ?: Costume.GOLD
        val capeIdx = this.cape.takeIf { it in CapeColors.indices && stars >= capeStars(it) } ?: 0
        val gear = listOfNotNull(Gear.of(hat)?.takeIf { it.slot == GearSlot.HAT }, Gear.of(glasses)?.takeIf { it.slot == GearSlot.GLASSES })
            .filter { hasGear(it) }
        return Outfit(suit, CapeColors[capeIdx], HeroPose.entries.filter { stars >= moveStars(it) }, gear)
    }
