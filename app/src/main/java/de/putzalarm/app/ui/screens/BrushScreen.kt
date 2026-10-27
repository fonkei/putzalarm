package de.putzalarm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import de.putzalarm.app.audio.Sfx
import de.putzalarm.app.audio.Voice
import de.putzalarm.app.camera.CameraPreview
import de.putzalarm.app.camera.FaceMotionAnalyzer
import de.putzalarm.app.camera.FaceReading
import de.putzalarm.app.data.Child
import de.putzalarm.app.ui.art.GermImage
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.JawHud
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Mascot
import de.putzalarm.app.ui.art.drawGerm
import de.putzalarm.app.ui.art.drawToothbrush
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.Lilita
import de.putzalarm.app.ui.theme.LocalU
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.nunito
import de.putzalarm.app.ui.theme.u
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sin

/**
 * Der Putz-Bildschirm. Die Frontkamera zeigt das Kind, aus dem erkannten Mund fliegen
 * Bakterien heraus, solange es schrubbt. Ohne Kamera-Erlaubnis läuft eine Variante mit Maskottchen.
 */
@Composable
fun BrushScreen(
    child: Child,
    cameraAllowed: Boolean,
    sensitivity: de.putzalarm.app.data.Sensitivity,
    showMeter: Boolean,
    voice: Voice,
    sfx: Sfx,
    onFinished: (SessionResult) -> Unit,
    onQuit: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val finished by rememberUpdatedState(onFinished)
    var cameraOk by remember { mutableStateOf(cameraAllowed) }
    val analyzer = remember { if (cameraAllowed) FaceMotionAnalyzer() else null }
    DisposableEffect(Unit) { onDispose { analyzer?.close() } }

    val game = remember {
        BrushGame(child.brushSeconds, object : GameEvents {
            override fun onCountdown(n: Int) = if (n == 0) sfx.go() else sfx.beep()
            override fun onZone(zone: Int) {
                sfx.ding()
                voice.say(BrushGame.ZoneLines[zone])
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            override fun onScream(text: String) = sfx.scream(text)
            override fun onPlopp() = sfx.plopp()
            override fun onCombo() = sfx.combo()
            override fun onIdle() = voice.say("Schrubb, schrubb! Die Monster verstecken sich!")
            override fun onFinish() {
                sfx.fanfare()
                voice.say("Fertig! Super geputzt, ${child.name}!")
            }
        })
    }
    var paused by remember { mutableStateOf(false) }
    var viewSize by remember { mutableStateOf(Size.Zero) }
    val track = remember { Tracking() }
    val unitPx = with(LocalDensity.current) { LocalU.current.toPx() }

    BackHandler { paused = true }

    LaunchedEffect(Unit) {
        sfx.beep()
        var last = 0L
        var active = false
        var onTime = 0f
        var offTime = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (paused || viewSize == Size.Zero) return@withFrameNanos
                val w = viewSize.width
                val h = viewSize.height
                val r = analyzer?.latest?.get() ?: FaceReading()
                track.face = r
                val fresh = System.currentTimeMillis() - r.timeMs < 1500
                // Putzen zählt nur mit erkanntem Gesicht und deutlicher Bewegung am Mund,
                // die kurz anhält. So fliegt nichts, wenn das Kind nur still in die Kamera schaut.
                val face = cameraOk && fresh && r.hasFace
                onTime = if (face && r.motion > sensitivity.on) onTime + dt else 0f
                offTime = if (!face || r.motion < sensitivity.off) offTime + dt else 0f
                active = when {
                    !cameraOk -> true
                    !active -> onTime > 0.25f
                    else -> offTime < 0.6f
                }
                track.active = active
                val target = if (cameraOk && fresh && r.hasFace) toView(r, w, h) else Offset(w / 2, h * 0.55f)
                val m = track.mouth
                track.mouth = if (m == Offset.Zero) target else m + (target - m) * 0.35f
                game.step(dt, active, track.mouth, unitPx, w, h)
            }
        }
    }
    LaunchedEffect(game.phase) {
        if (game.phase == Phase.DONE) {
            delay(2600)
            // Den Schluss-Spruch ausreden lassen, bevor das Ergebnis kommt (höchstens 8 Sekunden warten)
            var waited = 0
            while (voice.speaking && waited < 8000) { delay(100); waited += 100 }
            delay(300)
            finished(game.result)
        }
    }

    Box(Modifier.fillMaxSize().background(M.Cream)) {
        if (cameraOk && analyzer != null) {
            CameraPreview(Modifier.fillMaxSize(), analyzer = analyzer, onError = { cameraOk = false })
        } else {
            DottedBackground(M.Cream) {
                Mascot(Modifier.align(Alignment.Center).fillMaxSize(0.62f), pose = HeroPose.FLEX)
            }
        }
        GameLayer(game, track, cameraOk, if (showMeter) sensitivity else null, Modifier.fillMaxSize().onSizeChanged { viewSize = Size(it.width.toFloat(), it.height.toFloat()) })
        Hud(game, Modifier.fillMaxSize(), onPause = { paused = true })
        if (game.phase == Phase.COUNTDOWN) CountdownOverlay(game.countdown)
        if (game.phase == Phase.DONE) DoneOverlay()
        if (paused) PauseOverlay(onResume = { paused = false }, onQuit = { voice.stop(); onQuit() })
    }
}

/** Wird pro Bild beschrieben und nur beim Zeichnen gelesen, damit nicht jedes Bild neu komponiert wird. */
private class Tracking {
    var face = FaceReading()
    var mouth = Offset.Zero
    var active = false
}

/** Rechnet Mund-Koordinaten aus dem Kamerabild auf den Bildschirm um (Füllen + Spiegeln wie die Vorschau). */
private fun toView(r: FaceReading, w: Float, h: Float): Offset {
    val iw = r.imageW.toFloat()
    val ih = r.imageH.toFloat()
    val sc = max(w / iw, h / ih)
    val dx = (w - iw * sc) / 2
    val dy = (h - ih * sc) / 2
    return Offset(w - (r.mouthX * iw * sc + dx), r.mouthY * ih * sc + dy)
}

@Composable
private fun GameLayer(game: BrushGame, track: Tracking, cameraOk: Boolean, meter: de.putzalarm.app.data.Sensitivity?, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val unit = LocalU.current
    val density = LocalDensity.current
    val bubbleStyle = TextStyle(fontFamily = Lilita, fontSize = with(density) { (unit * 2.1f).toSp() }, color = Color(0xFF2A1033))
    val ploppStyle = TextStyle(fontFamily = Lilita, fontSize = with(density) { (unit * 3.4f).toSp() }, color = M.Yellow)
    val ploppOutline = ploppStyle.copy(color = M.Ink, drawStyle = Stroke(with(density) { (unit * 0.7f).toPx() }))
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val tick = game.frame
        val face = track.face
        val mouth = track.mouth
        val u = unit.toPx()
        // Rahmen um den erkannten Mund
        if (cameraOk && face.hasFace && game.phase != Phase.DONE) drawMouthBrackets(mouth, face, u)
        for (s in game.splats) {
            val k = s.age / 0.55f
            for ((a, dist, r) in s.drops) {
                val d = s.size * dist * (0.3f + k)
                drawCircle(s.color, s.size * r * (1f - k * 0.6f), Offset(s.x + kotlin.math.cos(a) * d, s.y + sin(a) * d), alpha = 1f - k)
            }
            if (s.plopp) {
                val layout = measurer.measure("PLOPP!", ploppOutline)
                val tl = Offset(s.x - layout.size.width / 2, s.y - layout.size.height / 2 - k * 2 * u)
                drawText(layout, topLeft = tl, alpha = 1f - k)
                drawText(measurer.measure("PLOPP!", ploppStyle), topLeft = tl, alpha = 1f - k)
            }
        }
        // Weiter hinten zuerst, damit die nahen Monster vorne drüber fliegen
        for (g in game.germs.sortedBy { it.z }) {
            val pop = (g.age / 0.25f).coerceAtMost(1f)
            val grow = if (pop < 1f) pop * 1.15f else 1f + 0.15f * (1f - ((g.age - 0.25f) / 0.2f).coerceIn(0f, 1f))
            drawGerm(
                Offset(g.x, g.y), g.size * g.z * grow, g.color, g.kind, g.seed, g.rot,
                wiggle = sin(g.age * 18f), look = sin(g.age * 7f),
            )
            if (g.scream != null && g.age in 0.05f..1.1f) {
                val layout = measurer.measure(g.scream, bubbleStyle)
                val pw = layout.size.width + 1.8f * u
                val ph = layout.size.height + 1f * u
                val tl = Offset(g.x - pw / 2, g.y - g.size * g.z * 0.6f - ph)
                drawRoundRect(Color.Black.copy(alpha = 0.18f), tl + Offset(0f, 0.3f * u), Size(pw, ph), CornerRadius(1.2f * u))
                drawRoundRect(Color.White, tl, Size(pw, ph), CornerRadius(1.2f * u))
                drawText(layout, topLeft = tl + Offset(0.9f * u, 0.5f * u))
            }
        }
        // Hinweis, wenn das Kind aufhört zu schrubben: eine wackelnde Bürste neben dem Mund
        if (game.idle && game.phase == Phase.RUN) {
            val wob = sin(game.time * 14f) * 1.2f * u
            translate(mouth.x + 9 * u + wob, mouth.y + 2 * u) {
                scale(u / 9f, u / 9f, Offset.Zero) { rotate(-20f, Offset.Zero) { drawToothbrush() } }
            }
        }
        // Messanzeige für Eltern: Balken = Bewegung am Mund, Strich = Schwelle, Punkt oben = Gesicht erkannt
        if (meter != null) {
            val bx = 4.5f * u
            val top = size.height - 36f * u
            val bh = 22f * u
            val bwid = 2.2f * u
            drawRoundRect(Color.Black.copy(alpha = 0.45f), Offset(bx, top), Size(bwid, bh), CornerRadius(0.6f * u))
            val lv = face.motion.coerceIn(0f, 1f)
            drawRoundRect(if (track.active) M.Mint else M.Pink, Offset(bx, top + bh * (1f - lv)), Size(bwid, bh * lv), CornerRadius(0.6f * u))
            val ty = top + bh * (1f - meter.on)
            drawLine(Color.White, Offset(bx - 0.6f * u, ty), Offset(bx + bwid + 0.6f * u, ty), 0.35f * u)
            drawCircle(if (face.hasFace) M.Mint else Color.Red, 0.9f * u, Offset(bx + bwid / 2, top - 1.6f * u))
        }
        // Comic-Rahmen
        val bw = 1.6f * u
        drawRoundRect(M.Yellow, Offset(bw / 2, bw / 2), Size(size.width - bw, size.height - bw), CornerRadius(2f * u), style = Stroke(bw))
        drawRoundRect(M.Ink, Offset(bw, bw), Size(size.width - 2 * bw, size.height - 2 * bw), CornerRadius(1.2f * u), style = Stroke(0.5f * u))
    }
}

private fun DrawScope.drawMouthBrackets(mouth: Offset, face: FaceReading, u: Float) {
    val w = max(14f * u, face.faceW * size.width * 0.75f)
    val h = w * 0.62f
    val l = mouth.x - w / 2
    val t = mouth.y - h / 2
    val arm = 2.4f * u
    val sw = 0.5f * u
    val c = Color.White.copy(alpha = 0.9f)
    for ((x, sx) in listOf(l to 1f, l + w to -1f)) for ((y, sy) in listOf(t to 1f, t + h to -1f)) {
        drawLine(c, Offset(x, y), Offset(x + sx * arm, y), sw, StrokeCap.Round)
        drawLine(c, Offset(x, y), Offset(x, y + sy * arm), sw, StrokeCap.Round)
    }
}

@Composable
private fun Hud(game: BrushGame, modifier: Modifier, onPause: () -> Unit) {
    Box(modifier.padding(3.5.u)) {
        // Punkte
        Row(
            Modifier.align(Alignment.TopStart).padding(bottom = 0.7.u).comic(Color.White, 5.u, 0.5.u, 0.7.u).padding(start = 0.8.u, end = 2.2.u, top = 0.6.u, bottom = 0.6.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.u),
        ) {
            GermImage(Color(0xFF7BDC5A), de.putzalarm.app.ui.art.GermKind.BLOB, Modifier.size(5.4.u), seed = 3f)
            BasicText("${game.monsters}", style = lilita(4.6))
        }
        // Timer
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.CenterHorizontally) {
            TimerRing(game, Modifier.size(15.u))
            Spacer(Modifier.height(2.u))
            ComboBadge(game.combo)
        }
        // Gebiss mit aktiver Zone
        Box(
            Modifier.align(Alignment.CenterStart).width(18.u).padding(bottom = 0.7.u).comic(Color.White, 3.u, 0.5.u, 0.7.u).padding(horizontal = 1.8.u, vertical = 1.2.u),
        ) {
            JawHud(if (game.phase == Phase.DONE) -1 else game.zone, game.zoneDone, Modifier.fillMaxWidth())
        }
        // Super Bürsti feuert unten rechts an und jubelt bei jeder Combo und am Ende
        var cheer by remember { mutableStateOf(false) }
        LaunchedEffect(game.combo) {
            if (game.combo >= 2) { cheer = true; delay(1600); cheer = false }
        }
        if (game.phase != Phase.COUNTDOWN) {
            Mascot(
                Modifier.align(Alignment.BottomEnd).fillMaxHeight(0.36f).offset(x = 3.u, y = 2.u),
                pose = if (cheer || game.phase == Phase.DONE) HeroPose.JUBEL else HeroPose.BRUSH,
            )
        }
        if (game.phase == Phase.RUN) {
            RoundIconButton(Icon.PAUSE, onPause, Modifier.align(Alignment.BottomStart), size = 6.u)
        }
    }
}

@Composable
private fun TimerRing(game: BrushGame, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE") val tick = game.frame
            val s = size.width / 100f
            drawCircle(Color.White, 46f * s)
            drawCircle(M.Ink, 46f * s, style = Stroke(5f * s))
            drawCircle(M.Cream, 38f * s, style = Stroke(10f * s))
            val left = if (game.phase == Phase.COUNTDOWN) 1f else (1f - game.elapsed / game.duration).coerceIn(0f, 1f)
            if (left > 0f) {
                drawArc(M.Pink, -90f, 360f * left, false, Offset(12f * s, 12f * s), Size(76f * s, 76f * s), style = Stroke(10f * s, cap = StrokeCap.Round))
            }
        }
        val t = game.secondsLeft
        BasicText(
            if (game.phase == Phase.DONE) "Fertig!" else "${t / 60}:${(t % 60).toString().padStart(2, '0')}",
            style = lilita(if (game.phase == Phase.DONE) 3 else 4.4, align = TextAlign.Center),
        )
    }
}

@Composable
private fun ComboBadge(combo: Int) {
    val bump = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo >= 2) {
            bump.snapTo(1.5f)
            bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    if (combo < 2) return
    Box(Modifier.scale(bump.value).rotate(8f).padding(bottom = 0.7.u).comic(M.Pink, 1.4.u, 0.5.u, 0.7.u).padding(horizontal = 2.u, vertical = 0.6.u)) {
        BasicText("$combo× Combo!", style = lilita(3.4, Color.White))
    }
}

@Composable
private fun CountdownOverlay(n: Int) {
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(n) {
        pop.snapTo(0.4f)
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val text = if (n == 0) "Los!" else "$n"
        Box(Modifier.scale(pop.value)) {
            BasicText(text, style = lilita(22, M.Ink).copy(drawStyle = Stroke(with(LocalDensity.current) { 1.6.u.toPx() })))
            BasicText(text, style = lilita(22, if (n == 0) M.Pink else M.Yellow))
        }
    }
}

@Composable
private fun DoneOverlay() {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.scale(pop.value).rotate(-6f)) {
            BasicText("Super!", style = lilita(16, M.Ink).copy(drawStyle = Stroke(with(LocalDensity.current) { 1.4.u.toPx() })))
            BasicText("Super!", style = lilita(16, M.Yellow))
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(M.Ink.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.u), verticalAlignment = Alignment.CenterVertically) {
            ComicButton(onQuit, Modifier.size(14.u, 15.u), bg = Color.White, radius = 14.u, padding = 3.5.u) {
                IconArt(Icon.HOME, Modifier.fillMaxSize())
            }
            ComicButton(onResume, Modifier.size(22.u, 23.u), radius = 22.u, padding = 5.u) {
                IconArt(Icon.PLAY, Modifier.fillMaxSize(), color = Color.White)
            }
        }
    }
}
