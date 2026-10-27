package de.putzalarm.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import de.putzalarm.app.ui.art.GermKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Ein Monster aus dem Sammelalbum. */
data class Monster(val id: Int, val name: String, val color: Color, val kind: GermKind)

private val Palette = listOf(
    Color(0xFFFF5D8F), Color(0xFF7BDC5A), Color(0xFFFFB627), Color(0xFF5AC8FA),
    Color(0xFFB57BFF), Color(0xFFFF7A5C), Color(0xFF3EE0B0), Color(0xFFFFD23F),
)

/** Farben der Bakterien, die beim Putzen herausfliegen. */
val GermColors = Palette.take(5)

val Monsters: List<Monster> = listOf(
    "Zucki", "Säurix", "Klebolf", "Plaquo", "Schmatzi", "Krümel", "Bröselbert", "Kariesto",
    "Schleimi", "Lutschi", "Bonbonix", "Knatschi", "Mampfred", "Kaugummo", "Limonaldo", "Schokolinchen",
    "Gummibärt", "Pommesine", "Klebrina", "Zwickzwack", "Schnuddel", "Fiesling", "Brausebold", "Keksi",
).mapIndexed { i, name ->
    Monster(i, name, Palette[(i * 3) % Palette.size], GermKind.entries[i % GermKind.entries.size])
}

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
) {
    private val days: Set<LocalDate>
        get() = sessions.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()

    /** Heute schon geputzt? Morgens = vor 15 Uhr, abends = ab 15 Uhr (wie Sonne und Mond auf dem Start). */
    fun brushedToday(evening: Boolean): Boolean {
        val today = LocalDate.now()
        return sessions.any {
            val t = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            t.toLocalDate() == today && (t.hour >= 15) == evening
        }
    }

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

    /** Für Mo bis So der laufenden Woche: morgens (vor 15 Uhr) und abends geputzt? */
    val weekSlots: List<Pair<Boolean, Boolean>>
        get() {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            val times = sessions.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            return (0L until 7L).map { i ->
                val day = monday.plusDays(i)
                val that = times.filter { it.toLocalDate() == day }
                that.any { it.hour < 15 } to that.any { it.hour >= 15 }
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

    /** Trägt eine fertige Putzrunde ein und schaltet einen neuen Sticker frei. Gibt dessen Id zurück. */
    fun recordSession(id: String, monstersChased: Int): Int? {
        val c = get(id) ?: return null
        val locked = Monsters.map { it.id }.filterNot { it in c.stickers }
        val fresh = locked.randomOrNull()
        upsert(
            c.copy(
                monsters = c.monsters + monstersChased,
                sessions = (c.sessions + System.currentTimeMillis()).takeLast(200),
                stickers = if (fresh != null) c.stickers + fresh else c.stickers,
            ),
        )
        return fresh
    }

    private fun load(): List<Child> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
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
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val arr = JSONArray()
        for (c in children) {
            arr.put(
                JSONObject()
                    .put("id", c.id).put("name", c.name).put("age", c.age)
                    .put("photo", c.photo ?: "").put("avatar", c.avatar)
                    .put("stickers", JSONArray(c.stickers)).put("monsters", c.monsters)
                    .put("sessions", JSONArray(c.sessions)),
            )
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}

private fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else (0 until length()).map { getInt(it) }
private fun JSONArray?.longs(): List<Long> = if (this == null) emptyList() else (0 until length()).map { getLong(it) }
