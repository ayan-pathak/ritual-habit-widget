package com.ayan.ritual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.billing.Unlock
import com.ayan.ritual.data.Habit
import com.ayan.ritual.render.ACCENTS
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.accentAt
import java.time.LocalDate
import java.time.Year

@Composable
fun HomeScreen(
    habits: List<Habit>,
    onOpen: (Habit) -> Unit,
    onCreate: () -> Unit,
    onPaywall: () -> Unit = {},
    onAccount: () -> Unit = {}
) {
    val unlocked by Unlock.unlockedState
    val canCreate = unlocked || habits.size < Unlock.FREE_LIMIT
    val startRitual = { if (canCreate) onCreate() else onPaywall() }

    val today = LocalDate.now()
    val year = today.year
    val yearLen = Year.of(year).length()
    val remaining = Habit.remainingIn(year, today)

    // Mochi in the corner speaks for the whole app: pleased once every ritual
    // is marked, awake while any is still open.
    val allDone = habits.isNotEmpty() && habits.all { it.isDone(today) }
    val headerMood = if (allDone) Mood.PLEASED else Mood.AWAKE

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ritual",
                    style = Display.copy(fontSize = 19.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp),
                    modifier = Modifier.weight(1f)
                )
                MochiTile(
                    mood = headerMood,
                    tile = if (allDone) Lime else Paper,
                    pixel = 1.5.dp,
                    corner = 999.dp,
                    inset = 7.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onAccount)
                )
            }
        }

        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                Text("$remaining squares\nleft this year", style = Display)
                Row(
                    Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(Ink))
                    Text(
                        "Day ${today.dayOfYear} of $yearLen",
                        style = Body.copy(fontSize = 13.sp, color = InkSoft)
                    )
                }
            }
        }

        if (habits.isEmpty()) {
            item { EmptyState(onCreate) }
        } else {
            items(habits, key = { it.id }) { habit ->
                Box(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onOpen(habit) }
                ) {
                    RitualCard(model = habit.toModel(today, year), height = 172.dp)
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InkPill("New ritual", startRitual, Modifier.weight(1f))
                    RoundButton(onClick = startRitual) {
                        androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
                            val s = size.width * 0.3f
                            val g = size.width * 0.14f
                            listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f).forEach { (cx, cy) ->
                                drawRoundRect(
                                    color = Ink,
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        cx * (s + g) + g * 0.5f, cy * (s + g) + g * 0.5f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(s, s),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.26f)
                                )
                            }
                        }
                    }
                }
                if (!canCreate) {
                    Text(
                        "One ritual is free. Unlock the rest for ${Unlock.price ?: "$4.99"}, once.",
                        style = Body.copy(fontSize = 12.sp, color = InkFaint),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 28.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Lime)
                .padding(24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MochiTile(Mood.AWAKE, Paper, pixel = 4.dp, corner = 18.dp, inset = 14.dp)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Nothing to keep yet.",
                    style = Display.copy(fontSize = 24.sp, lineHeight = 27.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Name one practice. Every day you keep it fills a square.",
                    style = Body.copy(color = Color(0xB312120F)),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        InkPill("Start a ritual", onCreate, Modifier.fillMaxWidth())
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Shared shape for every card in the app, so the widget can't drift from it. */
fun Habit.toModel(today: LocalDate, year: Int): SlabModel = SlabModel(
    title = name,
    slot = slot,
    accent = accentAt(accentIndex),
    year = year,
    today = today,
    doneDaysOfYear = daysOfYear(year),
    streak = streak(today),
    totalDone = totalIn(year),
    remaining = Habit.remainingIn(year, today),
    doneToday = isDone(today),
    mood = Cat.moodFor(isDone(today), streak(today), missedYesterday(today))
)
