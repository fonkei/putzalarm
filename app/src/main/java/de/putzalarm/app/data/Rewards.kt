package de.putzalarm.app.data

import androidx.compose.ui.graphics.Color
import de.putzalarm.app.ui.art.Costume
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Outfit

/** Umhangfarben für Super Bürsti; die erste (Pink) hat er von Anfang an. */
val CapeColors = listOf(Color(0xFFFF3D7F), Color(0xFF2F6BFF), Color(0xFFFFC928), Color(0xFF4CD964), Color(0xFF9B5BFF), Color(0xFF2A2A3A))
private val CapeWords = listOf("pinker", "blauer", "goldener", "grüner", "lila", "schwarzer")

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
)

/** Sterne, ab denen ein Kostüm, ein Umhang oder ein Move frei ist (0 = von Anfang an). */
fun costumeStars(c: Costume) = Rewards.filterIsInstance<CostumeReward>().firstOrNull { it.costume == c }?.stars ?: 0
fun capeStars(i: Int) = Rewards.filterIsInstance<CapeReward>().firstOrNull { it.cape == i }?.stars ?: 0
fun moveStars(p: HeroPose) = Rewards.filterIsInstance<MoveReward>().firstOrNull { it.pose == p }?.stars ?: 0

/** Was beim Sprung von [before] auf [after] Sterne neu dazukommt. */
fun newRewards(before: Int, after: Int) = Rewards.filter { it.stars in (before + 1)..after }

fun nextReward(stars: Int) = Rewards.firstOrNull { it.stars > stars }

/** So sieht Super Bürsti für dieses Kind aus: gewähltes Kostüm und Umhang (wenn frei) und alle freien Moves. */
val Child.outfit: Outfit
    get() {
        val suit = Costume.entries.firstOrNull { it.key == this.costume }?.takeIf { stars >= costumeStars(it) } ?: Costume.GOLD
        val capeIdx = this.cape.takeIf { it in CapeColors.indices && stars >= capeStars(it) } ?: 0
        return Outfit(suit, CapeColors[capeIdx], HeroPose.entries.filter { stars >= moveStars(it) })
    }
