package de.putzalarm.app.ui.art

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Wie Super Bürsti gerade aussieht und welche Moves er kann. */
data class Outfit(
    val costume: Costume = Costume.GOLD,
    val cape: Color = Color(0xFFFF3D7F),
    val moves: List<HeroPose> = HeroPose.Basic,
    /** Getragene Ausrüstung (Hut und Brille); Darth Bürsti trägt seinen Helm und nichts darüber. */
    val gear: List<Gear> = emptyList(),
)

/** Das Outfit des aktuellen Kindes; jede [Mascot] darunter nimmt es. */
val LocalOutfit = compositionLocalOf { Outfit() }
