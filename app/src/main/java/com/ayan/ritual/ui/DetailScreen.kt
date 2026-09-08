package com.ayan.ritual.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.render.MONTH_INITIALS
import com.ayan.ritual.render.accentAt
import com.ayan.ritual.share.StoryShare
import com.ayan.ritual.widget.RitualWidgetProvider
import java.time.LocalDate

@Composable
fun DetailScreen(habit: Habit, onBack: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val accent = accentAt(habit.accentIndex)
    var confirmingDelete by remember { mutableStateOf(false) }

    val firstYear = remember(habit.createdEpochDay, habit.done) {
        val earliest = minOf(habit.createdEpochDay, habit.done.minOrNull() ?: habit.createdEpochDay)
        LocalDate.ofEpochDay(earliest).year
    }
    var year by remember(habit.id) { mutableIntStateOf(today.year) }
    val viewingNow = year == today.year
    val model = habit.toModel(today, year)

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {

        // ── Bar ─────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundButton(onClick = onBack, size = 40.dp) {
                Canvas(Modifier.size(16.dp)) {
                    drawLine(Ink, Offset(size.width * .62f, size.height * .18f),
                        Offset(size.width * .3f, size.height * .5f), size.width * .14f, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .3f, size.height * .5f),
                        Offset(size.width * .62f, size.height * .82f), size.width * .14f, StrokeCap.Round)
                }
            }
            Spacer(Modifier.weight(1f))

            YearStep(true, year > firstYear) { if (year > firstYear) year-- }
            Text(year.toString(), style = Display.copy(fontSize = 15.sp, lineHeight = 16.sp))
            YearStep(false, year < today.year) { if (year < today.year) year++ }
        }

        // ── Title ───────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                CapsLabel(habit.slot)
                Spacer(Modifier.height(5.dp))
                Text(habit.name, style = Display.copy(fontSize = 34.sp, lineHeight = 35.sp))
            }
            MochiTile(
                mood = model.mood,
                tile = Color(accent.block),
                pixel = 3.dp,
                corner = 18.dp,
                inset = 11.dp
            )
        }

        // ── The field ───────────────────────────────────────────────────────
        Box(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            RitualCard(
                model = model,
                height = 200.dp,
                header = false,
                footer = false,
                cat = false,
                quarterRuler = true,
                cornerDp = 24f,
                padDp = 18f
            )
        }

        // ── Tally ───────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCell(habit.streak(today).toString(), "Streak")
            StatCell(habit.bestStreak().toString(), "Longest")
            StatCell(habit.totalIn(year).toString(), "Lit")
            StatCell(Habit.remainingIn(year, today).toString(), "Left", color = InkSoft)
        }

        // ── Cadence ─────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
            CapsLabel("By month")
            Spacer(Modifier.height(12.dp))
            MonthBars(habit, year)
        }

        // ── Share ───────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
            InkPill(
                label = "Share streak",
                onClick = { StoryShare.shareStreak(context, model) },
                modifier = Modifier.fillMaxWidth(),
                background = Color(accent.block),
                content = Ink,
                border = Ink,
                leading = {
                    Canvas(Modifier.size(17.dp)) {
                        // Instagram's rounded square with a ring and a corner dot.
                        val w = size.width
                        drawRoundRect(
                            color = Ink,
                            topLeft = Offset(w * .06f, w * .06f),
                            size = Size(w * .88f, w * .88f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .28f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .12f)
                        )
                        drawCircle(Ink, radius = w * .21f, center = Offset(w * .5f, w * .5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .12f))
                        drawCircle(Ink, radius = w * .06f, center = Offset(w * .72f, w * .28f))
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (StoryShare.isInstagramInstalled(context))
                    "Opens Instagram Stories"
                else
                    "Instagram isn't installed — you'll get the share sheet",
                style = Body.copy(fontSize = 12.sp, color = InkFaint),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── The act ─────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
            if (viewingNow) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkPill(
                        label = if (model.doneToday) "Marked today" else "Mark today",
                        onClick = {
                            HabitStore.toggle(context, habit.id, today)
                            RitualWidgetProvider.refreshAll(context)
                        },
                        modifier = Modifier.weight(1f),
                        background = if (model.doneToday) Cream else Ink,
                        content = if (model.doneToday) Ink else Paper,
                        border = if (model.doneToday) Ink else null,
                        leading = if (model.doneToday) {
                            {
                                Canvas(Modifier.size(16.dp)) {
                                    drawLine(Ink, Offset(size.width * .18f, size.height * .53f),
                                        Offset(size.width * .4f, size.height * .74f), size.width * .15f, StrokeCap.Round)
                                    drawLine(Ink, Offset(size.width * .4f, size.height * .74f),
                                        Offset(size.width * .82f, size.height * .29f), size.width * .15f, StrokeCap.Round)
                                }
                            }
                        } else null
                    )
                    RoundButton(
                        onClick = { confirmingDelete = !confirmingDelete },
                        background = if (confirmingDelete) Red else Color.Transparent,
                        border = if (confirmingDelete) Red else Ink
                    ) {
                        Canvas(Modifier.size(18.dp)) {
                            val c = if (confirmingDelete) Paper else Ink
                            drawLine(c, Offset(size.width * .22f, size.height * .22f),
                                Offset(size.width * .78f, size.height * .78f), size.width * .13f, StrokeCap.Round)
                            drawLine(c, Offset(size.width * .78f, size.height * .22f),
                                Offset(size.width * .22f, size.height * .78f), size.width * .13f, StrokeCap.Round)
                        }
                    }
                }
            } else {
                Text(
                    "$year is closed. Come back to this year to mark a day.",
                    style = Body.copy(fontSize = 13.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (confirmingDelete) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Delete “${habit.name}”? Every square it holds goes with it.",
                    style = Body.copy(fontSize = 13.sp)
                )
                Spacer(Modifier.height(10.dp))
                InkPill(
                    label = "Delete forever",
                    onClick = {
                        HabitStore.delete(context, habit.id)
                        RitualWidgetProvider.refreshAll(context)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    background = Red,
                    content = Paper
                )
            }

            Spacer(Modifier.height(30.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Twelve bars: how much of each month was kept. */
@Composable
private fun MonthBars(habit: Habit, year: Int) {
    val done = habit.daysOfYear(year)
    val density = LocalDensity.current
    val lens = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val ratios = remember(done, year) {
        var start = 1
        (0 until 12).map { m ->
            var hits = 0
            for (d in start until start + lens[m]) if (done.contains(d)) hits++
            start += lens[m]
            hits / lens[m].toFloat()
        }
    }

    Column {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val gap = with(density) { 6.dp.toPx() }
            val barW = (size.width - gap * 11) / 12f
            val radius = androidx.compose.ui.geometry.CornerRadius(barW * 0.22f)
            ratios.forEachIndexed { i, filled ->
                val x = i * (barW + gap)
                drawRoundRect(InkFaint, Offset(x, 0f), Size(barW, size.height), radius)
                if (filled > 0f) {
                    val h = size.height * filled
                    drawRoundRect(Ink, Offset(x, size.height - h), Size(barW, h), radius)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth()) {
            MONTH_INITIALS.forEach {
                Text(
                    it,
                    style = Caps.copy(fontSize = 9.sp, letterSpacing = 0.sp, color = InkFaint),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun YearStep(pointsLeft: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(11.dp)) {
            val tint = if (enabled) Ink else InkFaint
            val tip = if (pointsLeft) size.width * .24f else size.width * .76f
            val base = if (pointsLeft) size.width * .7f else size.width * .3f
            drawLine(tint, Offset(base, size.height * .12f), Offset(tip, size.height * .5f),
                size.width * .16f, StrokeCap.Round)
            drawLine(tint, Offset(tip, size.height * .5f), Offset(base, size.height * .88f),
                size.width * .16f, StrokeCap.Round)
        }
    }
}
