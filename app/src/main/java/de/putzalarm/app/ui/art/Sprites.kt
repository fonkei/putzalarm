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
        /** Bewegungen, die Super Bürsti von Anfang an kann; die übrigen schaltet man mit Sternen frei. */
        val Basic = listOf(FLEX, WAVE, JUBEL, DANCE, BRUSH, BOX, JACK)
    }
}

/** Kostüme von Super Bürsti (Körperbilder je Kostüm, der Umhang wird extra eingefärbt). */
enum class Costume(val key: String) { GOLD("gold"), CHROM("chrom"), NEON("neon") }

/** Ein Bild mit seiner Lage im ganzen Bild von Super Bürsti (450 × 600 Bildpunkte). */
class Placed(val img: ImageBitmap, val at: IntOffset)

/**
 * Fertig gerenderte 3D-Bilder von Super Bürsti und den Monstern aus res/drawable-nodpi.
 * Super Bürsti besteht aus zwei Ebenen: dem Körper im Kostüm und dem weißen Umhang, den die App einfärbt.
 * Jedes Bild wird beim ersten Gebrauch geladen und dann behalten; [preload] lädt ein Kostüm im Hintergrund vor.
 */
object Sprites {
    // Gleiche Reihenfolge wie die Palette in data/Data.kt und in tools/render_sprites.js
    private val palette = listOf(0xFFFF5D8F, 0xFF7BDC5A, 0xFFFFB627, 0xFF5AC8FA, 0xFFB57BFF, 0xFFFF7A5C, 0xFF3EE0B0, 0xFFFFD23F)
        .map { Color(it) }
    private lateinit var res: Resources
    private lateinit var pkg: String
    private val cache = HashMap<String, ImageBitmap>()

    /** Wo jedes zugeschnittene Bild und der Mund-Ausschnitt (Grinsen/Lachen) im ganzen Bild liegen. */
    private var at: Map<String, IntOffset> = emptyMap()
    private var mouthAt: Map<String, IntOffset> = emptyMap()
    var frameSize = IntSize(450, 600)
        private set
    var mouthSize = IntSize(84, 64)
        private set

    fun init(resources: Resources, packageName: String) {
        res = resources
        pkg = packageName
        runCatching {
            val id = resources.getIdentifier("hero_layout", "raw", packageName)
            val o = JSONObject(resources.openRawResource(id).bufferedReader().readText())
            frameSize = IntSize(o.getInt("w"), o.getInt("h"))
            mouthSize = IntSize(o.getInt("mw"), o.getInt("mh"))
            fun JSONObject.offsets() = keys().asSequence().associateWith { k -> getJSONArray(k).let { IntOffset(it.getInt(0), it.getInt(1)) } }
            at = o.getJSONObject("at").offsets()
            mouthAt = o.getJSONObject("mouth").offsets()
        }
    }

    private fun frameKey(pose: HeroPose, frame: Int) = "${pose.key}_${frame % pose.frames}"

    fun hero(costume: Costume, pose: HeroPose, frame: Int): Placed {
        val name = "hero_${costume.key}_${frameKey(pose, frame)}"
        return Placed(get(name), at[name] ?: IntOffset.Zero)
    }

    /** Der weiße Umhang zum selben Bild, oder null, wenn er ganz verdeckt ist. */
    fun cape(pose: HeroPose, frame: Int): Placed? {
        val name = "cape_${frameKey(pose, frame)}"
        val pos = at[name] ?: return null
        return Placed(get(name), pos)
    }

    /** Mund-Ausschnitt für [mouth] 1 = Grinsen, 2 = Lachen. */
    fun heroMouth(costume: Costume, pose: HeroPose, frame: Int, mouth: Int): Placed? {
        val key = frameKey(pose, frame)
        val pos = mouthAt[key] ?: return null
        return Placed(get("hero_${costume.key}_${key}" + if (mouth >= 2) "_laugh" else "_grin"), pos)
    }

    /** Lädt alle Bilder eines Kostüms vor und gibt die Bilder anderer Kostüme frei (bis auf ihr Vorschaubild). */
    fun preload(costume: Costume) {
        synchronized(cache) {
            cache.keys.removeAll { k -> k.startsWith("hero_") && !k.startsWith("hero_${costume.key}_") && !k.endsWith("_flex_0") }
        }
        for (p in HeroPose.entries) for (i in 0 until p.frames) {
            hero(costume, p, i); cape(p, i); heroMouth(costume, p, i, 1); heroMouth(costume, p, i, 2)
        }
        for (k in GermKind.entries) for (c in palette.indices) for (f in 0..1) get(germName(k, c, f))
    }

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
