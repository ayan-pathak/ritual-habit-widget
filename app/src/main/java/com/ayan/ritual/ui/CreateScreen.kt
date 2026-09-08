package com.ayan.ritual.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.render.ACCENTS
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.accentAt
import com.ayan.ritual.widget.RitualWidgetProvider
import java.time.LocalDate

private val SLOTS = listOf("Morning", "Midday", "Evening", "Anytime")

@Composable
fun CreateScreen(onDone: (Habit) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var slotIndex by remember { mutableIntStateOf(0) }
    var accentIndex by remember { mutableIntStateOf(0) }
    val accent = accentAt(accentIndex)
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.padding(start = 20.dp, top = 14.dp)) {
            RoundButton(onClick = onBack, size = 40.dp) {
                Canvas(Modifier.size(16.dp)) {
                    drawLine(Ink, Offset(size.width * .62f, size.height * .18f),
                        Offset(size.width * .3f, size.height * .5f), size.width * .14f, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .3f, size.height * .5f),
                        Offset(size.width * .62f, size.height * .82f), size.width * .14f, StrokeCap.Round)
                }
            }
        }

        Text(
            "New ritual",
            style = Display,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )

        // ── Name ────────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
            CapsLabel("What will you keep")
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Paper)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 26) name = it },
                    singleLine = true,
                    textStyle = Display.copy(fontSize = 22.sp, lineHeight = 24.sp),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text(
                                "Read before sleep",
                                style = Display.copy(fontSize = 22.sp, lineHeight = 24.sp, color = InkFaint)
                            )
                        }
                        inner()
                    }
                )
            }
        }

        // ── Slot ────────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            CapsLabel("When")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLOTS.forEachIndexed { i, s ->
                    val on = i == slotIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(if (on) Ink else Color.Transparent)
                            .then(
                                if (on) Modifier
                                else Modifier.border(
                                    androidx.compose.foundation.BorderStroke(1.5.dp, InkFaint), CircleShape
                                )
                            )
                            .clickable { slotIndex = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s,
                            style = Caps.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.sp,
                                color = if (on) Paper else InkSoft
                            )
                        )
                    }
                }
            }
        }

        // ── Colour ──────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            CapsLabel("Its colour")
            Spacer(Modifier.height(10.dp))
            ColourTiles(
                accents = ACCENTS,
                selectedIndex = accentIndex,
                onSelect = { accentIndex = it }
            )
        }

        // ── Preview ─────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
            CapsLabel("On your home screen")
            Spacer(Modifier.height(12.dp))
            RitualCard(
                model = SlabModel(
                    title = name.ifBlank { "Read before sleep" },
                    slot = SLOTS[slotIndex],
                    accent = accent,
                    year = today.year,
                    today = today,
                    doneDaysOfYear = emptySet(),
                    streak = 0,
                    totalDone = 0,
                    remaining = Habit.remainingIn(today.year, today),
                    doneToday = false,
                    mood = Mood.AWAKE
                ),
                height = 172.dp
            )
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
            InkPill(
                label = "Start today",
                onClick = {
                    val habit = HabitStore.create(context, name, SLOTS[slotIndex], accentIndex)
                    RitualWidgetProvider.refreshAll(context)
                    onDone(habit)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(34.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
