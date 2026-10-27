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

sealed interface Screen {
    data class Picker(val editMode: Boolean = false) : Screen
    data class Gate(val then: Screen) : Screen
    data class Edit(val childId: String?) : Screen
    data class Home(val childId: String) : Screen
    data class Brush(val childId: String, val camera: Boolean) : Screen
    data class Result(val childId: String, val result: SessionResult, val sticker: Int?) : Screen
    data class Album(val childId: String, val highlight: Int? = null, val back: Screen? = null) : Screen
}

@Composable
fun App(store: ProfileStore, voice: Voice) {
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

    AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
        when (s) {
            is Screen.Picker -> ProfilePickerScreen(
                children = store.children,
                photoDir = store.photoDir,
                editMode = s.editMode,
                onPick = { c -> screen = if (s.editMode) Screen.Edit(c.id) else Screen.Home(c.id) },
                onAdd = { screen = if (s.editMode) Screen.Edit(null) else Screen.Gate(Screen.Edit(null)) },
                onParents = { screen = Screen.Gate(Screen.Picker(editMode = true)) },
                onDoneEditing = { screen = Screen.Picker() },
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
                    onSwitch = { screen = Screen.Picker() },
                    greet = voice::say,
                )
            } ?: LaunchedEffect(Unit) { screen = Screen.Picker() }
            is Screen.Brush -> store.get(s.childId)?.let { c ->
                BrushScreen(
                    child = c,
                    cameraAllowed = s.camera,
                    voice = voice,
                    onFinished = { r ->
                        val sticker = store.recordSession(c.id, r.monsters)
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
        }
    }
}
