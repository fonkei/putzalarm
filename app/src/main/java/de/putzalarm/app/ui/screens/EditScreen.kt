package de.putzalarm.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import de.putzalarm.app.camera.CameraPreview
import de.putzalarm.app.camera.importProfilePhoto
import de.putzalarm.app.camera.newImageCapture
import de.putzalarm.app.camera.takeProfilePhoto
import de.putzalarm.app.data.AvatarMonsters
import de.putzalarm.app.data.Child
import de.putzalarm.app.ui.art.GermImage
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.nunito
import de.putzalarm.app.ui.theme.u
import java.io.File

/** Kind anlegen oder bearbeiten: Name, Alter und Foto (oder ein Monster als Bild). */
@Composable
fun EditChildScreen(
    initial: Child?,
    photoDir: File,
    onSave: (Child) -> Unit,
    onDelete: (Child) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age ?: 5) }
    var photo by remember { mutableStateOf(initial?.photo) }
    var avatar by remember { mutableStateOf(initial?.avatar ?: 0) }
    var capturing by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    // Aufgenommene, aber nicht gespeicherte Fotos wieder wegräumen.
    fun replacePhoto(new: String?) {
        val old = photo
        if (old != null && old != initial?.photo) File(photoDir, old).delete()
        photo = new
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) capturing = true }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importProfilePhoto(context, uri, photoDir)?.let { replacePhoto(it) }
    }

    if (capturing) {
        PhotoCapture(photoDir, onDone = { new -> capturing = false; if (new != null) replacePhoto(new) }, onCancel = { capturing = false })
        return
    }
    BackHandler { replacePhoto(initial?.photo); onCancel() }

    Box(Modifier.fillMaxSize().background(M.Blue)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 5.u, vertical = 4.u)) {
            // Links: Foto
            Column(Modifier.width(36.u).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icon.CLOSE, { replacePhoto(initial?.photo); onCancel() }, size = 6.u)
                    Spacer(Modifier.width(2.u))
                    BasicText(if (initial == null) "Neues Kind" else "Kind ändern", style = lilita(5, Color.White))
                }
                Spacer(Modifier.height(3.u))
                Box(contentAlignment = Alignment.BottomEnd) {
                    ChildAvatar(photo, avatar, photoDir, 24.u, border = 0.9.u)
                    Box(
                        Modifier.size(7.u).comic(M.Pink, 7.u, 0.6.u).clickable { startCapture(context, permission) { capturing = true } }.padding(1.5.u),
                    ) { IconArt(Icon.CAMERA, color = Color.White) }
                }
                Spacer(Modifier.height(2.5.u))
                Row(horizontalArrangement = Arrangement.spacedBy(1.4.u)) {
                    SmallButton("Foto", Icon.CAMERA) { startCapture(context, permission) { capturing = true } }
                    SmallButton("Galerie", Icon.GALLERY) {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                Spacer(Modifier.height(2.u))
                BasicText("…oder ein Monster als Bild:", style = nunito(1.7, Color.White))
                Spacer(Modifier.height(1.u))
                Row(horizontalArrangement = Arrangement.spacedBy(1.u)) {
                    AvatarMonsters.forEachIndexed { i, m ->
                        val sel = photo == null && avatar == i
                        Box(
                            Modifier
                                .size(5.u)
                                .scale(if (sel) 1.18f else 1f)
                                .background(Color.White, CircleShape)
                                .border(0.45.u, if (sel) M.Yellow else M.Ink, CircleShape)
                                .clickable { avatar = i; replacePhoto(null) },
                            contentAlignment = Alignment.Center,
                        ) { GermImage(m.color, m.kind, Modifier.fillMaxWidth(0.82f)) }
                    }
                }
            }
            Spacer(Modifier.width(5.u))
            // Rechts: Formular
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(1.u)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BasicText("Elternbereich", style = nunito(1.8, M.Yellow), modifier = Modifier.comic(M.Ink, 3.u, 0.u).padding(horizontal = 1.8.u, vertical = 0.6.u))
                }
                Spacer(Modifier.height(2.u))
                BasicText("NAME", style = nunito(1.9, Color.White))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(16) },
                    singleLine = true,
                    textStyle = lilita(4.4),
                    cursorBrush = SolidColor(M.Pink),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().comic(Color.White, 2.u, 0.5.u).padding(horizontal = 2.u, vertical = 1.4.u),
                )
                Spacer(Modifier.height(1.6.u))
                BasicText("ALTER", style = nunito(1.9, Color.White))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.u)) {
                    for (a in 3..9) {
                        val sel = age == a
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.u)
                                .scale(if (sel) 1.15f else 1f)
                                .comic(if (sel) M.Yellow else Color.White, 6.u, 0.45.u, if (sel) 0.6.u else 0.u)
                                .clickable { age = a },
                            contentAlignment = Alignment.Center,
                        ) { BasicText(if (a == 9) "9+" else "$a", style = lilita(2.8)) }
                    }
                }
                BasicText(
                    if (age <= 3) "Putzdauer: 90 Sekunden" else "Putzdauer: 2 Minuten",
                    style = nunito(1.6, Color.White.copy(alpha = 0.85f), weight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.u), verticalAlignment = Alignment.CenterVertically) {
                    if (initial != null) {
                        RoundIconButton(Icon.TRASH, { askDelete = true }, bg = if (askDelete) M.Pink else Color.White, tint = if (askDelete) Color.White else M.Ink)
                    }
                    ComicButton(
                        onClick = {
                            val base = initial ?: Child()
                            onSave(base.copy(name = name.trim().ifEmpty { "Kind" }, age = age, photo = photo, avatar = avatar))
                        },
                        modifier = Modifier.weight(1f),
                    ) { BasicText("Speichern", style = lilita(3.6, Color.White, TextAlign.Center)) }
                }
            }
        }
        if (askDelete && initial != null) {
            Box(Modifier.fillMaxSize().background(M.Ink.copy(alpha = 0.7f)).clickable { askDelete = false }, contentAlignment = Alignment.Center) {
                Column(
                    Modifier.comic(Color.White, 3.u, 0.6.u, 1.u).padding(4.u),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.u),
                ) {
                    BasicText("${initial.name} wirklich löschen?", style = lilita(4))
                    BasicText("Fortschritt und Sticker gehen dabei verloren.", style = nunito(2))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.u)) {
                        ComicButton({ askDelete = false }, bg = Color.White) { BasicText("Behalten", style = lilita(3)) }
                        ComicButton({ onDelete(initial) }) { BasicText("Löschen", style = lilita(3, Color.White)) }
                    }
                }
            }
        }
    }
}

private fun startCapture(context: android.content.Context, permission: androidx.activity.result.ActivityResultLauncher<String>, go: () -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) go()
    else permission.launch(Manifest.permission.CAMERA)
}

@Composable
private fun SmallButton(label: String, icon: Icon, onClick: () -> Unit) {
    ComicButton(onClick, bg = Color.White, radius = 2.u, border = 0.45.u, shadow = 0.6.u, padding = 1.u) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.8.u)) {
            IconArt(icon, Modifier.size(3.u))
            BasicText(label, style = lilita(2.4))
        }
    }
}

/** Kamera mit Kreis-Maske für ein sauberes Profilbild. */
@Composable
private fun PhotoCapture(photoDir: File, onDone: (String?) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val capture = remember { newImageCapture() }
    var busy by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(M.Ink)) {
        CameraPreview(Modifier.fillMaxSize(), imageCapture = capture, onError = { onDone(null) })
        val yellow = M.Yellow
        val dash = 2.u
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            val r = size.height * 0.36f
            val c = Offset(size.width / 2, size.height * 0.42f)
            drawRect(M.Ink.copy(alpha = 0.72f))
            drawCircle(Color.Black, r, c, blendMode = BlendMode.Clear)
            drawCircle(yellow, r, c, style = Stroke(width = r * 0.03f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.6f))))
        }
        RoundIconButton(Icon.CLOSE, onCancel, Modifier.align(Alignment.TopStart).padding(3.u))
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.u), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Gesicht in den Kreis", style = lilita(3.2, Color.White))
            Spacer(Modifier.height(1.5.u))
            Box(
                Modifier
                    .size(10.u)
                    .background(if (busy) M.Gum else M.Pink, CircleShape)
                    .border(0.9.u, Color.White, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null, enabled = !busy) {
                        busy = true
                        takeProfilePhoto(context, capture, photoDir) { onDone(it) }
                    },
            )
        }
    }
}
