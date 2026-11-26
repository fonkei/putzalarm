package de.putzalarm.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import de.putzalarm.app.ui.art.GermKind
import de.putzalarm.app.ui.art.Pattern
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.UUID

/** Die vier ursprünglichen Formen in ihrer alten Reihenfolge. */
private val OldKinds = listOf(GermKind.BLOB, GermKind.SPIKE, GermKind.BEAN, GermKind.CYCLOPS)

/** Ein Monster aus dem Sammelalbum. */
/** [rare]: seltenes Monster mit Krone, gibt es nur nach sehr gründlichem Putzen. [egg]: schlüpft nur aus dem Überraschungsei. */
data class Monster(
    val id: Int, val name: String, val color: Color, val kind: GermKind, val rare: Boolean = false, val pattern: Pattern = Pattern.PLAIN,
    val egg: Boolean = false,
)

private val Palette = listOf(
    Color(0xFFFF5D8F), Color(0xFF7BDC5A), Color(0xFFFFB627), Color(0xFF5AC8FA),
    Color(0xFFB57BFF), Color(0xFFFF7A5C), Color(0xFF3EE0B0), Color(0xFFFFD23F),
)

/** Farben der Bakterien, die beim Putzen herausfliegen. */
val GermColors = Palette.take(5)

/**
 * Alle Album-Monster. Die Id ist ihr Platz im Album und steht in den gespeicherten Stickern der Kinder:
 * Neue Monster immer nur hinten anhängen, nie umsortieren oder löschen.
 */
val Monsters: List<Monster> = listOf(
    "Zucki", "Säurix", "Klebolf", "Plaquo", "Schmatzi", "Krümel", "Bröselbert", "Kariesto",
    "Schleimi", "Lutschi", "Bonbonix", "Knatschi", "Mampfred", "Kaugummo", "Limonaldo", "Schokolinchen",
    "Gummibärt", "Pommesine", "Klebrina", "Zwickzwack", "Schnuddel", "Fiesling", "Brausebold", "Keksi",
).mapIndexed { i, name ->
    // Die ersten vier Formen, so wie sie vor den neuen Formen verteilt waren
    Monster(i, name, Palette[(i * 3) % Palette.size], OldKinds[i % 4])
} + listOf(
    "König Karius", "Glitzerine", "Baron Bonbon", "Lady Lakritz",
    "Prinz Pudding", "Kaiserin Kandis", "Herzog Honig", "Fürst Fruchtgummi",
).mapIndexed { i, name ->
    Monster(24 + i, name, Palette[(i * 5 + 2) % Palette.size], OldKinds[(i + 1) % 4], rare = true)
} + listOf(
    // Neu im November 2026: Würfel, Sterne, Tropfen, Quallen, dazu gestreift und getupft
    Triple("Würfelwilli", GermKind.CUBE, Pattern.PLAIN),
    Triple("Sternchen-Sepp", GermKind.STAR, Pattern.PLAIN),
    Triple("Tropfi", GermKind.DROP, Pattern.PLAIN),
    Triple("Qualli", GermKind.JELLY, Pattern.PLAIN),
    Triple("Streifi", GermKind.BLOB, Pattern.STRIPES),
    Triple("Tupfine", GermKind.BEAN, Pattern.DOTS),
    Triple("Zebrazahn", GermKind.CUBE, Pattern.STRIPES),
    Triple("Punktepaule", GermKind.STAR, Pattern.DOTS),
    Triple("Klötzchen", GermKind.CUBE, Pattern.DOTS),
    Triple("Zackenzora", GermKind.SPIKE, Pattern.STRIPES),
    Triple("Glibberglotz", GermKind.JELLY, Pattern.STRIPES),
    Triple("Wabbelwilma", GermKind.DROP, Pattern.DOTS),
    Triple("Kringel", GermKind.CYCLOPS, Pattern.STRIPES),
    Triple("Tüpfelbert", GermKind.BLOB, Pattern.DOTS),
    Triple("Streusel", GermKind.STAR, Pattern.STRIPES),
    Triple("Wackelpudding", GermKind.JELLY, Pattern.PLAIN),
    Triple("Brummkreisel", GermKind.CYCLOPS, Pattern.DOTS),
    Triple("Nudelnick", GermKind.BEAN, Pattern.STRIPES),
    Triple("Popcorna", GermKind.SPIKE, Pattern.DOTS),
    Triple("Lollipopp", GermKind.DROP, Pattern.STRIPES),
    Triple("Karamello", GermKind.CUBE, Pattern.PLAIN),
    Triple("Zimtzicke", GermKind.STAR, Pattern.PLAIN),
    Triple("Marmeladus", GermKind.JELLY, Pattern.DOTS),
    Triple("Brezelbär", GermKind.DROP, Pattern.PLAIN),
    Triple("Waffelwolf", GermKind.CUBE, Pattern.STRIPES),
    Triple("Sirupsepp", GermKind.BEAN, Pattern.PLAIN),
    Triple("Honigtropf", GermKind.DROP, Pattern.DOTS),
    Triple("Plätzchen-Paul", GermKind.STAR, Pattern.DOTS),
).mapIndexed { i, (name, kind, pattern) ->
    Monster(32 + i, name, Palette[(i * 3 + 1) % Palette.size], kind, pattern = pattern)
} + listOf(
    Triple("Graf Glitzerstreif", GermKind.JELLY, Pattern.STRIPES),
    Triple("Prinzessin Punkteglanz", GermKind.STAR, Pattern.DOTS),
    Triple("Sultan Sahnetupf", GermKind.DROP, Pattern.DOTS),
    Triple("Zar Zuckerwürfel", GermKind.CUBE, Pattern.STRIPES),
).mapIndexed { i, (name, kind, pattern) ->
    Monster(60 + i, name, Palette[(i * 3 + 4) % Palette.size], kind, rare = true, pattern = pattern)
} + listOf(
    // Neu im November 2026: Schlüpflinge, die es nur im Überraschungsei gibt (Form und Farbe wie in tools/render_extras.js)
    Triple("Schlupfi", GermKind.BLOB, 0),
    Triple("Sternküken", GermKind.STAR, 7),
    Triple("Tröpfchen", GermKind.DROP, 3),
    Triple("Bohnibaby", GermKind.BEAN, 4),
    Triple("Würfelküken", GermKind.CUBE, 6),
    Triple("Wackelbaby", GermKind.JELLY, 1),
    Triple("Pieksküken", GermKind.SPIKE, 2),
    Triple("Guckiküken", GermKind.CYCLOPS, 5),
).mapIndexed { i, (name, kind, c) ->
    Monster(64 + i, name, Palette[c], kind, pattern = Pattern.EGG, egg = true)
}

/** Album-Monster werden größer, je öfter man sie wieder verjagt: ab so vielen Malen gibt es Stufe 2, 3, 4 und 5. */
val LevelSteps = listOf(3, 8, 15, 25)
const val MaxLevel = 5

/** Stufe eines Monsters (1 bis 5) nach [chased]-mal Verjagen. */
fun monsterLevel(chased: Int): Int = 1 + LevelSteps.count { chased >= it }

/** Riesen-Bosse: tauchen ab und zu mitten beim Putzen auf (an jedem Tag) und werden mit kräftigem Schrubben besiegt. */
data class Giant(val id: Int, val name: String)

/** Reihenfolge = Bildnummer (giant_<id>_f0) und gespeicherte Siege; nur hinten anhängen. */
val Giants = listOf(
    Giant(0, "Bonbon-Bulle"), Giant(1, "Schoko-Flatterich"), Giant(2, "Zacken-Zyklop"), Giant(3, "Kaugummi-Kaiser"),
    Giant(4, "Krakenkrümel"), Giant(5, "Lakritz-Drache"), Giant(6, "Karamell-König"), Giant(7, "Limo-Teufelchen"),
)

/** Bonus-Sterne für einen besiegten Riesen-Boss. */
const val GiantStars = 3

/** Felder auf der Schatzkarte; auf dem letzten steht die Schatzkiste. Jedes Putzen ist ein Feld weiter. */
const val MapLen = 10

/** Bonus-Sterne aus einer Schatzkiste, wenn es keine Kisten-Ausrüstung mehr gibt. */
const val ChestStars = 10

/** Was eine Putzrunde gebracht hat (für den Ergebnis-Bildschirm). */
data class SessionOutcome(
    /** Neuer Album-Sticker (normal oder selten). */
    val sticker: Int? = null,
    /** Schlüpfling aus dem Überraschungsei; [eggStars] > 0, wenn es schon alle gab und das Ei Sterne enthielt. */
    val egg: Int? = null,
    val eggStars: Int = 0,
    /** Album-Monster, die eine Stufe aufgestiegen sind: Id und neue Stufe. */
    val levelUps: List<Pair<Int, Int>> = emptyList(),
    /** Schritte auf der Schatzkarte vor dieser Runde. */
    val mapBefore: Int = 0,
    /** Nummer der Schatzkiste, die diesmal aufging (0 = erste), sonst null; [chestStars] > 0 bei Sternen statt Ausrüstung. */
    val chest: Int? = null,
    val chestStars: Int = 0,
    /** Alle Sterne dieser Runde: Zonen-Sterne plus Boni. */
    val starsTotal: Int = 0,
)

/** Boss-Monster: jede Woche (Mo bis So) kommt eins. Besiegt wird es mit den Sternen der ganzen Woche. */
data class Boss(val id: Int, val name: String, val color: Color, val kind: GermKind)

val Bosses = listOf(
    Boss(0, "Karies-King", Color(0xFFB57BFF), GermKind.SPIKE),
    Boss(1, "Riesen-Zucki", Color(0xFFFF5D8F), GermKind.BLOB),
    Boss(2, "Kaugummi-Koloss", Color(0xFF7BDC5A), GermKind.BEAN),
    Boss(3, "Schoko-Schreck", Color(0xFFFF7A5C), GermKind.CYCLOPS),
    Boss(4, "Mega-Plaquo", Color(0xFFFFB627), GermKind.SPIKE),
    Boss(5, "Bonbon-Brutalo", Color(0xFF5AC8FA), GermKind.BLOB),
)

/** Lebenspunkte eines Bosses: zweimal am Tag gut putzen schafft ihn in etwa fünf Tagen, einmal am Tag reicht nicht. */
const val BossHp = 80

/** Kennung der laufenden Woche, z. B. "2026-44". */
fun weekKey(day: LocalDate = LocalDate.now()): String =
    "${day.get(IsoFields.WEEK_BASED_YEAR)}-${day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

/** Der Boss dieser Woche. */
fun bossOfWeek(day: LocalDate = LocalDate.now()): Boss = Bosses[day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % Bosses.size]

/** So viele Sterne (von 12) braucht eine Putzrunde, damit ein seltenes Monster ins Album kommt. */
const val RareStars = 11

/** Monster, die Kinder statt eines Fotos als Profilbild nehmen können. */
val AvatarMonsters = listOf(0, 3, 4, 1, 2, 5).map { Monsters[it] }

data class Child(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val age: Int = 5,
    val photo: String? = null,
    val avatar: Int = 0,
    val stickers: List<Int> = emptyList(),
    val monsters: Int = 0,
    val sessions: List<Long> = emptyList(),
    /** Alle je gesammelten Sterne (0 bis 3 pro Kieferzone und Putzrunde). */
    val stars: Int = 0,
    /** Gewähltes Kostüm und Umhang von Super Bürsti (siehe Rewards.kt). */
    val costume: String = "gold",
    val cape: Int = 0,
    /** Schaden am Boss in der Woche [bossWeek] (= Sterne dieser Woche) und alle besiegten Bosse (Ids, je Woche einer). */
    val bossWeek: String = "",
    val bossDamage: Int = 0,
    val bossWins: List<Int> = emptyList(),
    /** Gewählter Hintergrund und Ergebnis-Effekt (Nummern aus Backdrop und Effect). */
    val backdrop: Int = 0,
    val effect: Int = 0,
    /** Neu im November 2026 (alle mit Standardwert, alte Profile laden unverändert): */
    /** Wie oft jedes Album-Monster schon wieder verjagt wurde (Id → Anzahl); davon hängt seine Stufe ab. */
    val chases: Map<Int, Int> = emptyMap(),
    /** Putzrunden seit dem letzten Überraschungsei. */
    val eggTimer: Int = 0,
    /** Alle Schritte auf der Schatzkarte (ein Schritt je Putzrunde, seit es die Karte gibt). */
    val mapSteps: Int = 0,
    /** Putzrunden seit dem letzten Riesen-Boss und alle besiegten Riesen-Bosse (Ids). */
    val giantTimer: Int = 0,
    val giantWins: List<Int> = emptyList(),
    /** Getragener Hut und getragene Brille (Gear-Key, leer = keins). */
    val hat: String = "",
    val glasses: String = "",
) {
    /** Geöffnete Schatzkisten und das Feld auf der aktuellen Karte (0 bis MapLen - 1). */
    val chests: Int get() = mapSteps / MapLen
    val mapField: Int get() = mapSteps % MapLen

    fun level(monsterId: Int): Int = monsterLevel(chases[monsterId] ?: 0)

    /** Welcher Riesen-Boss als Nächstes kommt: der am seltensten besiegte (bei Gleichstand der erste). */
    val nextGiant: Giant get() = Giants.minBy { g -> giantWins.count { it == g.id } * 100 + g.id }

    /** Ob in der nächsten Putzrunde ein Riesen-Boss auftaucht: spätestens nach drei Runden ohne, sonst etwa jedes dritte Mal. */
    fun rollGiant(): Giant? = if (giantTimer >= 3 || kotlin.random.Random.nextFloat() < 0.3f) nextGiant else null

    /** Schaden am Boss dieser Woche; in einer neuen Woche fängt es bei 0 an. */
    val bossDamageNow: Int get() = if (bossWeek == weekKey()) bossDamage else 0
    val bossBeatenNow: Boolean get() = bossDamageNow >= BossHp

    private val days: Set<LocalDate>
        get() = sessions.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()

    /** Tage in Folge geputzt; wer heute noch nicht dran war, verliert die Serie nicht. */
    val streak: Int
        get() {
            val d = days
            var day = LocalDate.now()
            if (day !in d) day = day.minusDays(1)
            var n = 0
            while (day in d) { n++; day = day.minusDays(1) }
            return n
        }

    /** Für Mo bis So der laufenden Woche: wurde an dem Tag geputzt? */
    val week: List<Boolean>
        get() {
            val d = days
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            return (0L until 7L).map { monday.plusDays(it) in d }
        }

    /** Für Mo bis So der laufenden Woche: morgens (vor 15 Uhr) und abends geputzt? (Sonne und Mond auf dem Start) */
    val weekSlots: List<DaySlots>
        get() {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            val times = sessions.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            return (0L until 7L).map { i ->
                val day = monday.plusDays(i)
                val hours = times.filter { it.toLocalDate() == day }.map { it.hour }
                DaySlots(hours.any { it < 15 }, hours.any { it >= 15 })
            }
        }

    /** Putzdauer in Sekunden. Die Kleinsten starten mit 90 Sekunden. */
    val brushSeconds: Int get() = if (age <= 3) 90 else 120
}

/** Hält die Kinderprofile als JSON-Datei im App-Speicher. Nichts verlässt das Gerät. */
/** Wie leicht Schrubben erkannt wird. [on]: Bewegung, ab der es losgeht; [off]: darunter hört es auf. */
enum class Sensitivity(val label: String, val on: Float, val off: Float) {
    EASY("Locker", 0.12f, 0.05f), NORMAL("Normal", 0.2f, 0.09f), STRICT("Streng", 0.32f, 0.15f)
}

/** Ob an einem Tag morgens (vor 15 Uhr) und abends geputzt wurde. */
data class DaySlots(val morning: Boolean, val evening: Boolean)

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    /** Empfindlichkeit der Putzerkennung (Elternbereich). */
    var sensitivity by mutableStateOf(
        Sensitivity.entries.getOrElse(prefs.getInt("empfindlichkeit", Sensitivity.NORMAL.ordinal)) { Sensitivity.NORMAL },
    )
        private set

    /** Zeigt beim Putzen eine kleine Messanzeige, um die Erkennung zu prüfen. */
    var showMeter by mutableStateOf(prefs.getBoolean("messanzeige", false))
        private set

    /** Lustige Musik beim Putzen. */
    var music by mutableStateOf(prefs.getBoolean("musik", true))
        private set

    /** Neue, aufgenommene Stimmen (VoiceStudio). Aus = alte Gerätestimme wie früher; die Kinderdaten bleiben davon unberührt. */
    var newVoices by mutableStateOf(prefs.getBoolean("neue_stimmen", true))
        private set

    fun updateNewVoices(on: Boolean) {
        newVoices = on
        prefs.edit().putBoolean("neue_stimmen", on).apply()
    }

    fun updateMusic(on: Boolean) {
        music = on
        prefs.edit().putBoolean("musik", on).apply()
    }

    fun updateSensitivity(s: Sensitivity) {
        sensitivity = s
        prefs.edit().putInt("empfindlichkeit", s.ordinal).apply()
    }

    fun updateShowMeter(on: Boolean) {
        showMeter = on
        prefs.edit().putBoolean("messanzeige", on).apply()
    }

    private val file = File(context.filesDir, "kinder.json")
    private val backup = File(context.filesDir, "kinder.sicherung.json")
    val photoDir = File(context.filesDir, "fotos").apply { mkdirs() }

    var children by mutableStateOf(load())
        private set

    fun get(id: String): Child? = children.firstOrNull { it.id == id }

    fun upsert(child: Child) {
        val old = get(child.id)
        if (old?.photo != null && old.photo != child.photo) File(photoDir, old.photo).delete()
        children = if (old == null) children + child else children.map { if (it.id == child.id) child else it }
        save()
    }

    fun delete(id: String) {
        get(id)?.photo?.let { File(photoDir, it).delete() }
        children = children.filterNot { it.id == id }
        save()
    }

    /**
     * Trägt eine fertige Putzrunde ein: Sticker, Überraschungsei, Monster-Stufen, Schatzkarte, Riesen-Boss und Sterne.
     * [zoneStars]: Sterne aus den Kieferzonen, [chased]: wie oft welches Album-Monster verjagt wurde,
     * [giant]: Riesen-Boss dieser Runde (oder null) und ob er besiegt wurde. Boni wie Kisten-Ausrüstung schaltet die Karte frei.
     */
    fun recordSession(
        id: String, monstersChased: Int, zoneStars: Int,
        chased: Map<Int, Int> = emptyMap(), giant: Int? = null, giantBeaten: Boolean = false,
    ): SessionOutcome? {
        val c = get(id) ?: return null
        // Nach sehr gründlichem Putzen gibt es ein seltenes Monster, sonst ein normales (Schlüpflinge nur aus dem Ei)
        val (rare, normal) = Monsters.filterNot { it.id in c.stickers || it.egg }.partition { it.rare }
        val fresh = (if (zoneStars >= RareStars) rare.randomOrNull() ?: normal.randomOrNull() else normal.randomOrNull())?.id
        // Überraschungsei: etwa jede vierte Runde, spätestens nach fünf Runden ohne
        val eggNow = c.eggTimer >= 4 || kotlin.random.Random.nextFloat() < 0.2f
        val eggMonster = if (eggNow) Monsters.filter { it.egg && it.id !in c.stickers }.randomOrNull()?.id else null
        val eggStars = if (eggNow && eggMonster == null) 5 else 0
        // Monster-Stufen
        val chases = c.chases.toMutableMap()
        val ups = mutableListOf<Pair<Int, Int>>()
        for ((mid, n) in chased) {
            if (n <= 0) continue
            val before = monsterLevel(chases[mid] ?: 0)
            chases[mid] = (chases[mid] ?: 0) + n
            val after = monsterLevel(chases[mid]!!)
            if (after > before) ups += mid to after
        }
        // Schatzkarte: ein Feld weiter; auf dem letzten Feld geht die Kiste auf
        val steps = c.mapSteps + 1
        val chest = if (steps % MapLen == 0) steps / MapLen - 1 else null
        val chestStars = if (chest != null && chest >= ChestGear.size) ChestStars else 0
        val giantStars = if (giant != null && giantBeaten) GiantStars else 0
        val total = zoneStars + giantStars + eggStars + chestStars
        upsert(
            c.copy(
                monsters = c.monsters + monstersChased,
                stars = c.stars + total,
                bossWeek = weekKey(),
                bossDamage = c.bossDamageNow + total,
                bossWins = if (c.bossDamageNow < BossHp && c.bossDamageNow + total >= BossHp) c.bossWins + bossOfWeek().id else c.bossWins,
                sessions = (c.sessions + System.currentTimeMillis()).takeLast(200),
                stickers = c.stickers + listOfNotNull(fresh, eggMonster),
                chases = chases,
                eggTimer = if (eggNow) 0 else c.eggTimer + 1,
                mapSteps = steps,
                giantTimer = if (giant != null) 0 else c.giantTimer + 1,
                giantWins = if (giant != null && giantBeaten) c.giantWins + giant else c.giantWins,
            ),
        )
        return SessionOutcome(fresh, eggMonster, eggStars, ups, c.mapSteps, chest, chestStars, total)
    }

    /**
     * Lädt die Profile. Ist die Datei kaputt, kommt die Sicherung von vor dem letzten Speichern dran;
     * die kaputte Datei wird nie überschrieben, sondern mit Zeitstempel beiseitegelegt.
     */
    private fun load(): List<Child> {
        if (!file.exists() && !backup.exists()) return emptyList()
        parse(file)?.let { return it }
        if (file.exists()) file.copyTo(File(file.parentFile, "kinder.kaputt-${System.currentTimeMillis()}.json"), overwrite = false)
        return parse(backup) ?: emptyList()
    }

    private fun parse(f: File): List<Child>? = try {
        if (!f.exists()) null else {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Child(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    age = o.optInt("age", 5),
                    photo = o.optString("photo").takeIf { it.isNotEmpty() },
                    avatar = o.optInt("avatar"),
                    stickers = o.optJSONArray("stickers").ints(),
                    monsters = o.optInt("monsters"),
                    sessions = o.optJSONArray("sessions").longs(),
                    stars = o.optInt("stars"),
                    costume = o.optString("costume", "gold"),
                    cape = o.optInt("cape"),
                    bossWeek = o.optString("bossWeek"),
                    bossDamage = o.optInt("bossDamage"),
                    bossWins = o.optJSONArray("bossWins").ints(),
                    backdrop = o.optInt("backdrop"),
                    effect = o.optInt("effect"),
                    chases = o.optJSONObject("chases").intMap(),
                    eggTimer = o.optInt("eggTimer"),
                    mapSteps = o.optInt("mapSteps"),
                    giantTimer = o.optInt("giantTimer"),
                    giantWins = o.optJSONArray("giantWins").ints(),
                    hat = o.optString("hat"),
                    glasses = o.optString("glasses"),
                )
            }
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Speichert sicher: erst die bisherige, lesbare Datei als Sicherung kopieren, dann in eine Zwischendatei
     * schreiben, auf die Platte zwingen und erst dann umbenennen. So geht nie mehr als die letzte Änderung verloren.
     * Neue Felder kommen immer mit Standardwert dazu, alte werden nie umbenannt (sonst wären Daten weg).
     */
    private fun save() {
        val arr = JSONArray()
        for (c in children) {
            arr.put(
                JSONObject()
                    .put("id", c.id).put("name", c.name).put("age", c.age)
                    .put("photo", c.photo ?: "").put("avatar", c.avatar)
                    .put("stickers", JSONArray(c.stickers)).put("monsters", c.monsters)
                    .put("sessions", JSONArray(c.sessions))
                    .put("stars", c.stars)
                    .put("costume", c.costume).put("cape", c.cape)
                    .put("bossWeek", c.bossWeek).put("bossDamage", c.bossDamage).put("bossWins", JSONArray(c.bossWins))
                    .put("backdrop", c.backdrop).put("effect", c.effect)
                    .put("chases", JSONObject().apply { c.chases.forEach { (k, v) -> put(k.toString(), v) } })
                    .put("eggTimer", c.eggTimer).put("mapSteps", c.mapSteps)
                    .put("giantTimer", c.giantTimer).put("giantWins", JSONArray(c.giantWins))
                    .put("hat", c.hat).put("glasses", c.glasses),
            )
        }
        synchronized(this) {
            if (parse(file) != null) file.copyTo(backup, overwrite = true)
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp).use { out ->
                out.write(arr.toString().toByteArray())
                out.fd.sync()
            }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }
}

private fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else (0 until length()).map { getInt(it) }
private fun JSONObject?.intMap(): Map<Int, Int> =
    if (this == null) emptyMap() else keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to optInt(k) } }.toMap()
private fun JSONArray?.longs(): List<Long> = if (this == null) emptyList() else (0 until length()).map { getLong(it) }
