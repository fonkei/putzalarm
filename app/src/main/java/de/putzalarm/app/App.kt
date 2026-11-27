package de.putzalarm.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.putzalarm.app.audio.Sfx
import de.putzalarm.app.audio.Voice
import de.putzalarm.app.data.ProfileStore
import de.putzalarm.app.ui.screens.AlbumScreen
import de.putzalarm.app.ui.screens.BrushScreen
import de.putzalarm.app.ui.screens.EditChildScreen
import de.putzalarm.app.ui.screens.HomeScreen
import de.putzalarm.app.ui.screens.ParentGateScreen
import de.putzalarm.app.ui.screens.ProfilePickerScreen
import de.putzalarm.app.ui.screens.ResultScreen
import de.putzalarm.app.ui.screens.SessionResult
import de.putzalarm.app.ui.screens.WardrobeScreen
import de.putzalarm.app.data.outfit
import de.putzalarm.app.data.newRewards
import de.putzalarm.app.data.CostumeReward
import de.putzalarm.app.data.CapeReward
import de.putzalarm.app.data.BackdropReward
import de.putzalarm.app.data.EffectReward
import de.putzalarm.app.data.GearReward
import de.putzalarm.app.data.ChestGear
import de.putzalarm.app.data.chestGear
import de.putzalarm.app.data.Giants
import de.putzalarm.app.data.SessionOutcome
import de.putzalarm.app.data.wearing
import de.putzalarm.app.ui.screens.TreasureMapScreen
import de.putzalarm.app.ui.art.Costume
import de.putzalarm.app.ui.art.LocalOutfit
import de.putzalarm.app.ui.art.Outfit
import de.putzalarm.app.ui.art.Sprites
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface Screen {
    data class Picker(val editMode: Boolean = false) : Screen
    data class Gate(val then: Screen) : Screen
    data class Edit(val childId: String?) : Screen
    data class Home(val childId: String) : Screen
    /** [giant]: Riesen-Boss, der in dieser Runde auftaucht (Id), oder null. */
    data class Brush(val childId: String, val camera: Boolean, val giant: Int? = null) : Screen
    data class Result(val childId: String, val result: SessionResult, val outcome: SessionOutcome) : Screen
    data class TreasureMap(val childId: String) : Screen
    data class Album(val childId: String, val highlight: Int? = null, val back: Screen? = null) : Screen
    data class Wardrobe(val childId: String) : Screen
}

/** Zu welchem Kind ein Bildschirm gehört (für Super Bürstis Outfit). */
private val Screen.childId: String?
    get() = when (this) {
        is Screen.Home -> childId
        is Screen.Brush -> childId
        is Screen.Result -> childId
        is Screen.Album -> childId
        is Screen.Wardrobe -> childId
        is Screen.TreasureMap -> childId
        is Screen.Edit -> childId
        else -> null
    }

@Composable
fun App(store: ProfileStore, voice: Voice, sfx: Sfx) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Picker()) }
    var pendingBrush by remember { mutableStateOf<String?>(null) }
    // Neue oder alte Stimmen (Elternbereich)
    SideEffect { voice.useRecordings = store.newVoices; sfx.useRecordings = store.newVoices }

    // Ob ein Riesen-Boss kommt, wird beim Start gewürfelt
    fun giantFor(childId: String) = store.get(childId)?.rollGiant()?.id

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingBrush?.let { screen = Screen.Brush(it, granted, giantFor(it)) }
        pendingBrush = null
    }

    fun startBrushing(childId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            screen = Screen.Brush(childId, true, giantFor(childId))
        } else {
            pendingBrush = childId
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // Bilder für das Kostüm des aktuellen Kindes im Hintergrund vorladen
    val look = screen.childId?.let { store.get(it) }?.outfit
    val costume = look?.costume ?: Costume.GOLD
    val gear = look?.gear.orEmpty()
    LaunchedEffect(costume, gear) { withContext(Dispatchers.Default) { Sprites.preload(costume, gear) } }

    AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
        val outfit = s.childId?.let { store.get(it) }?.outfit ?: Outfit()
        CompositionLocalProvider(LocalOutfit provides outfit) {
        when (s) {
            is Screen.Picker -> ProfilePickerScreen(
                children = store.children,
                photoDir = store.photoDir,
                editMode = s.editMode,
                onPick = { c -> screen = if (s.editMode) Screen.Edit(c.id) else Screen.Home(c.id) },
                onAdd = { screen = if (s.editMode) Screen.Edit(null) else Screen.Gate(Screen.Edit(null)) },
                onParents = { screen = Screen.Gate(Screen.Picker(editMode = true)) },
                onDoneEditing = { screen = Screen.Picker() },
                sensitivity = store.sensitivity,
                onSensitivity = store::updateSensitivity,
                showMeter = store.showMeter,
                onShowMeter = store::updateShowMeter,
                music = store.music,
                onMusic = store::updateMusic,
                newVoices = store.newVoices,
                onNewVoices = store::updateNewVoices,
            )
            is Screen.Gate -> ParentGateScreen(onPass = { screen = s.then }, onCancel = { screen = Screen.Picker() })
            is Screen.Edit -> EditChildScreen(
                initial = s.childId?.let { store.get(it) },
                photoDir = store.photoDir,
                onSave = { c -> store.upsert(c); screen = Screen.Picker() },
                onDelete = { c -> store.delete(c.id); screen = Screen.Picker() },
                onCancel = { screen = Screen.Picker() },
            )
            is Screen.Home -> store.get(s.childId)?.let { c ->
                HomeScreen(
                    child = c,
                    photoDir = store.photoDir,
                    onStart = { startBrushing(c.id) },
                    onAlbum = { screen = Screen.Album(c.id, back = s) },
                    onWardrobe = { screen = Screen.Wardrobe(c.id) },
                    onMap = { screen = Screen.TreasureMap(c.id) },
                    onSwitch = { screen = Screen.Picker() },
                    // Darth Bürsti spricht mit tiefer Stimme (eigene Aufnahmen, sonst tiefe Gerätestimme)
                    greet = { text -> if (c.outfit.costume == Costume.DARTH) voice.say(text, 0.62f, "darth") else voice.say(text) },
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Brush -> store.get(s.childId)?.let { c ->
                BrushScreen(
                    child = c,
                    cameraAllowed = s.camera,
                    sensitivity = store.sensitivity,
                    showMeter = store.showMeter,
                    music = store.music,
                    voice = voice,
                    sfx = sfx,
                    giant = s.giant?.let { Giants.getOrNull(it) },
                    onFinished = { r ->
                        val outcome = store.recordSession(c.id, r.monsters, r.stars, r.chased, r.giant, r.giantBeaten) ?: SessionOutcome()
                        // Ein neues Kostüm, ein neuer Umhang oder ein neuer Hut wird gleich angezogen
                        store.get(c.id)?.let { now ->
                            var dressed = now
                            for (rw in newRewards(c.stars, now.stars)) when (rw) {
                                is CostumeReward -> dressed = dressed.copy(costume = rw.costume.key)
                                is CapeReward -> dressed = dressed.copy(cape = rw.cape)
                                is BackdropReward -> dressed = dressed.copy(backdrop = rw.backdrop.ordinal)
                                is EffectReward -> dressed = dressed.copy(effect = rw.effect.ordinal)
                                is GearReward -> dressed = dressed.wearing(rw.gear)
                                else -> Unit
                            }
                            outcome.chest?.let { now.chestGear.getOrNull(it) }?.let { dressed = dressed.wearing(it) }
                            if (dressed != now) store.upsert(dressed)
                        }
                        screen = Screen.Result(c.id, r, outcome)
                    },
                    onQuit = { screen = Screen.Home(c.id) },
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Result -> store.get(s.childId)?.let { c ->
                ResultScreen(
                    child = c,
                    result = s.result,
                    outcome = s.outcome,
                    onHome = { screen = Screen.Home(c.id) },
                    onAlbum = { screen = Screen.Album(c.id, s.outcome.egg ?: s.outcome.sticker, back = Screen.Home(c.id)) },
                    say = voice::say,
                    sfx = sfx,
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Album -> store.get(s.childId)?.let { c ->
                AlbumScreen(c, s.highlight, onBack = { screen = s.back ?: Screen.Home(c.id) }, say = voice::say)
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.TreasureMap -> store.get(s.childId)?.let { c ->
                TreasureMapScreen(c, onBack = { screen = Screen.Home(c.id) }, say = voice::say)
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Wardrobe -> store.get(s.childId)?.let { c ->
                WardrobeScreen(
                    child = c,
                    onPick = { store.upsert(it) },
                    onBack = { screen = Screen.Home(c.id) },
                    say = voice::say,
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
        }
        }
    }
}
