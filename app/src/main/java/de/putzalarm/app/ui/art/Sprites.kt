package de.putzalarm.app.ui.art

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject

/** Posen von Super Bürsti, je mit ihren Einzelbildern (aus tools/render_sprites.js). */
enum class HeroPose(val key: String, val frames: Int, val frameMs: Int) {
    FLEX("flex", 2, 420), WAVE("wave", 4, 120), JUBEL("jubel", 4, 110),
    DANCE("dance", 4, 190), BRUSH("brush", 4, 110), BOX("box", 4, 200),
    MOONWALK("moonwalk", 4, 300), KARATE("karate", 4, 330), JACK("jack", 4, 220), SPIN("spin", 4, 160),
    ROBOT("robot", 4, 280), GUITAR("guitar", 4, 170), FLY("fly", 4, 420);

    companion object {
        /** Bewegungen, zwischen denen Super Bürsti im Show-Modus wechselt. */
        val Show = entries.toList()
    }
}

/**
 * Fertig gerenderte 3D-Bilder von Super Bürsti und den Monstern aus res/drawable-nodpi.
 * Jedes Bild wird beim ersten Gebrauch geladen und dann behalten; [preload] lädt alle im Hintergrund vor.
 */
object Sprites {
    // Gleiche Reihenfolge wie die Palette in data/Data.kt und in tools/render_sprites.js
    private val palette = listOf(0xFFFF5D8F, 0xFF7BDC5A, 0xFFFFB627, 0xFF5AC8FA, 0xFFB57BFF, 0xFFFF7A5C, 0xFF3EE0B0, 0xFFFFD23F)
        .map { Color(it) }
    private lateinit var res: Resources
    private lateinit var pkg: String
    private val cache = HashMap<String, ImageBitmap>()

    /** Wo der Mund-Ausschnitt (Grinsen/Lachen) auf jedem Bild von Super Bürsti liegt, in Bildpunkten. */
    private var mouthAt: Map<String, IntOffset> = emptyMap()
    var mouthSize = IntSize(84, 64)
        private set

    fun init(resources: Resources, packageName: String) {
        res = resources
        pkg = packageName
        runCatching {
            val id = resources.getIdentifier("hero_mouths", "raw", packageName)
            val o = JSONObject(resources.openRawResource(id).bufferedReader().readText())
            mouthSize = IntSize(o.getInt("w"), o.getInt("h"))
            val at = o.getJSONObject("at")
            mouthAt = at.keys().asSequence().associateWith { k -> at.getJSONArray(k).let { IntOffset(it.getInt(0), it.getInt(1)) } }
        }
    }

    /** Mund-Ausschnitt für [mouth] 1 = Grinsen, 2 = Lachen, mit Position im Bild (450 × 600). */
    fun heroMouth(pose: HeroPose, frame: Int, mouth: Int): Pair<ImageBitmap, IntOffset>? {
        val base = "hero_${pose.key}_${frame % pose.frames}"
        val at = mouthAt[base] ?: return null
        return get(base + if (mouth >= 2) "_laugh" else "_grin") to at
    }

    fun preload() {
        for (p in HeroPose.entries) for (i in 0 until p.frames) { hero(p, i); heroMouth(p, i, 1); heroMouth(p, i, 2) }
        for (k in GermKind.entries) for (c in palette.indices) for (f in 0..1) get(germName(k, c, f))
    }

    fun hero(pose: HeroPose, frame: Int): ImageBitmap = get("hero_${pose.key}_${frame % pose.frames}")

    fun germ(kind: GermKind, color: Color, frame: Int): ImageBitmap = get(germName(kind, colorIndex(color), frame and 1))

    private fun germName(kind: GermKind, c: Int, f: Int) = "germ_${kind.name.lowercase()}_c${c}_f$f"

    /** Passende Palettenfarbe; unbekannte Farben bekommen die ähnlichste. */
    private fun colorIndex(c: Color): Int = palette.indices.minBy {
        val p = palette[it]
        (p.red - c.red).let { d -> d * d } + (p.green - c.green).let { d -> d * d } + (p.blue - c.blue).let { d -> d * d }
    }

    @SuppressLint("DiscouragedApi")
    private fun get(name: String): ImageBitmap = synchronized(cache) {
        cache.getOrPut(name) {
            val id = res.getIdentifier(name, "drawable", pkg)
            BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false }).asImageBitmap()
        }
    }
}
