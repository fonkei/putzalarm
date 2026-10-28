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
    data class Brush(val childId: String, val camera: Boolean) : Screen
    data class Result(val childId: String, val result: SessionResult, val sticker: Int?) : Screen
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
        is Screen.Edit -> childId
        else -> null
    }

@Composable
fun App(store: ProfileStore, voice: Voice, sfx: Sfx) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Picker()) }
    var pendingBrush by remember { mutableStateOf<String?>(null) }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingBrush?.let { screen = Screen.Brush(it, granted) }
        pendingBrush = null
    }

    fun startBrushing(childId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            screen = Screen.Brush(childId, true)
        } else {
            pendingBrush = childId
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // Bilder für das Kostüm des aktuellen Kindes im Hintergrund vorladen
    val costume = screen.childId?.let { store.get(it) }?.outfit?.costume ?: Costume.GOLD
    LaunchedEffect(costume) { withContext(Dispatchers.Default) { Sprites.preload(costume) } }

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
                    onSwitch = { screen = Screen.Picker() },
                    greet = voice::say,
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
                    onFinished = { r ->
                        val sticker = store.recordSession(c.id, r.monsters, r.stars)
                        // Ein neues Kostüm oder ein neuer Umhang wird gleich angezogen
                        store.get(c.id)?.let { now ->
                            var dressed = now
                            for (rw in newRewards(c.stars, now.stars)) when (rw) {
                                is CostumeReward -> dressed = dressed.copy(costume = rw.costume.key)
                                is CapeReward -> dressed = dressed.copy(cape = rw.cape)
                                else -> Unit
                            }
                            if (dressed != now) store.upsert(dressed)
                        }
                        screen = Screen.Result(c.id, r, sticker)
                    },
                    onQuit = { screen = Screen.Home(c.id) },
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Result -> store.get(s.childId)?.let { c ->
                ResultScreen(
                    child = c,
                    result = s.result,
                    newSticker = s.sticker,
                    onHome = { screen = Screen.Home(c.id) },
                    onAlbum = { screen = Screen.Album(c.id, s.sticker, back = Screen.Home(c.id)) },
                    say = voice::say,
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Album -> store.get(s.childId)?.let { c ->
                AlbumScreen(c, s.highlight, onBack = { screen = s.back ?: Screen.Home(c.id) })
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Wardrobe -> store.get(s.childId)?.let { c ->
                WardrobeScreen(
                    child = c,
                    onPick = { costumeKey, cape -> store.upsert(c.copy(costume = costumeKey, cape = cape)) },
                    onBack = { screen = Screen.Home(c.id) },
                    say = voice::say,
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
        }
        }
    }
}
