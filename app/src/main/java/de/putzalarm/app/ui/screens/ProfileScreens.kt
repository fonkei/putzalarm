package de.putzalarm.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import de.putzalarm.app.data.Child
import de.putzalarm.app.ui.art.Icon
import de.putzalarm.app.ui.art.IconArt
import de.putzalarm.app.ui.art.HeroPose
import de.putzalarm.app.ui.art.Mascot
import de.putzalarm.app.ui.theme.ComicButton
import de.putzalarm.app.ui.theme.M
import de.putzalarm.app.ui.theme.comic
import de.putzalarm.app.ui.theme.lilita
import de.putzalarm.app.ui.theme.nunito
import de.putzalarm.app.ui.theme.u
import java.io.File

/** „Wer putzt?“: Das Kind tippt auf sein eigenes Foto. Im Elternmodus öffnet ein Tipp den Editor. */
@Composable
fun ProfilePickerScreen(
    children: List<Child>,
    photoDir: File,
    editMode: Boolean,
    onPick: (Child) -> Unit,
    onAdd: () -> Unit,
    onParents: () -> Unit,
    onDoneEditing: () -> Unit,
) {
    DottedBackground {
        Row(Modifier.align(Alignment.TopEnd).padding(3.5.u), horizontalArrangement = Arrangement.spacedBy(1.4.u)) {
            if (editMode) {
                ComicButton(onDoneEditing, bg = M.Ink, radius = 3.u, border = 0.45.u, shadow = 0.6.u, padding = 1.2.u) {
                    BasicText("Fertig", style = lilita(2.4, M.Yellow))
                }
            } else {
                ComicButton(onParents, bg = Color.White, radius = 3.u, border = 0.45.u, shadow = 0.6.u, padding = 1.2.u) {
                    BasicText("Eltern", style = lilita(2.4))
                }
            }
        }
        Column(
            Modifier.fillMaxSize().padding(top = 7.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(if (editMode) "Welches Kind bearbeiten?" else "Wer putzt?", style = lilita(7, align = TextAlign.Center))
            Spacer(Modifier.height(4.u))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 5.u, vertical = 1.u),
                horizontalArrangement = Arrangement.spacedBy(2.6.u),
            ) {
                for (c in children) {
                    ProfileCard(onClick = { onPick(c) }) {
                        Box {
                            ChildAvatar(c, photoDir, 16.u)
                            if (editMode) {
                                Box(Modifier.align(Alignment.BottomEnd).size(5.u).comic(M.Pink, 5.u, 0.45.u).padding(1.u)) {
                                    IconArt(Icon.PENCIL, color = Color.White)
                                }
                            }
                        }
                        BasicText(c.name, style = lilita(4, align = TextAlign.Center), maxLines = 1)
                        val streak = c.streak
                        BasicText(if (streak == 1) "1 Tag" else "$streak Tage", style = nunito(1.6, M.Pink))
                    }
                }
                AddCard(onAdd)
            }
            if (children.isEmpty()) {
                Spacer(Modifier.height(2.u))
                BasicText("Tippe auf das Plus, um das erste Kind anzulegen.", style = nunito(2.2))
            }
        }
        if (children.isEmpty()) Mascot(Modifier.align(Alignment.BottomStart).padding(start = 4.u).width(22.u))
    }
}

@Composable
private fun ProfileCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    ComicButton(onClick, Modifier.width(21.u), bg = Color.White, radius = 3.u, border = 0.6.u, shadow = 1.u, padding = 2.u) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.8.u)) { content() }
    }
}

@Composable
private fun AddCard(onAdd: () -> Unit) {
    Column(
        Modifier
            .width(21.u)
            .border(BorderStroke(0.6.u, M.Ink), RoundedCornerShape(3.u))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onAdd)
            .padding(2.u),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.8.u),
    ) {
        Box(Modifier.size(16.u).border(BorderStroke(0.6.u, M.Ink), CircleShape), contentAlignment = Alignment.Center) {
            BasicText("+", style = lilita(9))
        }
        BasicText("Neu", style = lilita(4))
        BasicText(" ", style = nunito(1.6))
    }
}

/** Elternbereich-Sperre: eine kleine Rechenaufgabe, die Kinder im Vorschulalter noch nicht lösen. */
@Composable
fun ParentGateScreen(onPass: () -> Unit, onCancel: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    val task = remember(round) {
        val a = (3..9).random()
        val b = (3..9).random()
        val right = a * b
        val options = (setOf(right) + generateSequence { right + listOf(-10, -6, -3, 3, 4, 7, 9).random() }.filter { it > 0 && it != right }.distinct().take(2)).shuffled()
        Triple("$a × $b = ?", right, options)
    }
    DottedBackground(M.Blue) {
        RoundIconButton(Icon.CLOSE, onCancel, Modifier.align(Alignment.TopStart).padding(3.u))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Elternbereich", style = nunito(2.2, M.Yellow), modifier = Modifier.comic(M.Ink, 3.u, 0.u).padding(horizontal = 2.u, vertical = 0.6.u))
            Spacer(Modifier.height(2.u))
            BasicText(task.first, style = lilita(8, Color.White))
            Spacer(Modifier.height(1.u))
            BasicText(if (wrong) "Nicht ganz. Neue Aufgabe!" else "Bitte die richtige Zahl antippen.", style = nunito(2, Color.White))
            Spacer(Modifier.height(3.u))
            Row(horizontalArrangement = Arrangement.spacedBy(3.u)) {
                for (o in task.third) {
                    ComicButton(
                        onClick = {
                            if (o == task.second) onPass() else { wrong = true; round++ }
                        },
                        modifier = Modifier.width(14.u),
                        bg = Color.White,
                    ) { BasicText("$o", style = lilita(6, align = TextAlign.Center)) }
                }
            }
        }
        Mascot(Modifier.align(Alignment.BottomEnd).padding(end = 4.u).width(16.u).offset(y = 2.u), pose = HeroPose.FLEX)
    }
}
