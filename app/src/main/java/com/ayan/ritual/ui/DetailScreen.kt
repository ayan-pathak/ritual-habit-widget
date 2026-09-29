package com.ayan.ritual.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.billing.Unlock
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.HabitStore
import com.ayan.ritual.data.Moments
import com.ayan.ritual.render.MONTH_INITIALS
import com.ayan.ritual.render.MochiBody
import com.ayan.ritual.render.Pose
import com.ayan.ritual.render.accentAt
import com.ayan.ritual.share.StoryShare
import com.ayan.ritual.widget.RitualWidgetProvider
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun DetailScreen(
    habit: Habit,
    onBack: () -> Unit,
    onPaywall: (PaywallReason) -> Unit = {},
    onEdit: () -> Unit = {},
    onBrag: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val accent = accentAt(habit.accentIndex)
    var celebrate by remember { mutableIntStateOf(0) }
    var celebrating by remember { mutableStateOf(false) }
    var saying by remember { mutableStateOf("") }
    // The page makes room below its last row while he is up, so he rises
    // into space of his own instead of over the button just pressed.
    var room by remember { mutableStateOf(false) }
    val roomHeight by animateDpAsState(
        if (room) RISE_HEIGHT * 0.6f else 0.dp,
        tween(400, easing = FastOutSlowInEasing), label = "room"
    )
    val scroll = rememberScrollState()
    LaunchedEffect(celebrate) {
        if (celebrate == 0) return@LaunchedEffect
        room = true
        val start = System.nanoTime()
        while (System.nanoTime() - start < 480_000_000L) {
            withFrameNanos { }
            scroll.scrollTo(scroll.maxValue)
        }
        delay((MochiBody.popSeconds(true) * 1000).toLong() - 980L)
        room = false
    }

    val firstYear = remember(habit.createdEpochDay, habit.done) {
        val earliest = minOf(habit.createdEpochDay, habit.done.minOrNull() ?: habit.createdEpochDay)
        LocalDate.ofEpochDay(earliest).year
    }
    var year by remember(habit.id) { mutableIntStateOf(today.year) }
    val viewingNow = year == today.year
    val model = habit.toModel(today, year)

    Box(Modifier.fillMaxSize().background(Cream)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
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
                // Name, time, colour, sentence, and deleting it, all live there.
                RoundButton(onClick = onEdit, size = 40.dp) {
                    Canvas(Modifier.size(16.dp)) {
                        val w = size.width
                        val pencil = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * .66f, w * .12f); lineTo(w * .88f, w * .34f)
                            lineTo(w * .34f, w * .88f); lineTo(w * .12f, w * .88f); lineTo(w * .12f, w * .66f); close()
                        }
                        drawPath(pencil, Ink, style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = w * .12f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        drawLine(Ink, Offset(w * .56f, w * .22f), Offset(w * .78f, w * .44f), w * .12f, StrokeCap.Round)
                    }
                }
            }

            // ── Title ───────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    CapsLabel(habit.slot)
                    Spacer(Modifier.height(5.dp))
                    // The identity leads and the task sits under it, because the
                    // sentence is what someone came for and the task is only how
                    // it gets paid for. A habit written before identities existed
                    // has none, and falls back to leading with its name.
                    if (habit.identity.isNotBlank()) {
                        Text(
                            habit.identity,
                            style = Display.copy(fontSize = 25.sp, lineHeight = 27.sp)
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            habit.name,
                            style = Body.copy(fontSize = 13.sp, color = InkSoft)
                        )
                    } else {
                        Text(habit.name, style = Display.copy(fontSize = 34.sp, lineHeight = 35.sp))
                    }
                }
                MochiTile(
                    mood = model.mood,
                    tile = Color(accent.block),
                    height = 48.dp,
                    corner = 18.dp,
                    inset = 11.dp
                )
            }

            // ── The field ───────────────────────────────────────────────────────
            Box(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                RitualCard(
                    model = model,
                    height = 132.dp,
                    header = false,
                    footer = false,
                    cat = false,
                    quarterRuler = true,
                    cornerDp = 24f,
                    padDp = 18f
                )
            }

            // ── The thirty ──────────────────────────────────────────────────────
            GoalBand(habit, today, Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(24.dp))

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
                    // Sharing is one of the three things the unlock buys, and the
                    // wall it opens says so in the words of the thing just tapped.
                    onClick = {
                        if (Unlock.unlocked) StoryShare.shareStreak(context, model)
                        else onPaywall(PaywallReason.SHARE)
                    },
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
                    if (!Unlock.unlocked)
                        "Part of the unlock"
                    else if (StoryShare.isInstagramInstalled(context))
                        "Opens Instagram Stories, with the link copied for a sticker"
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
                    InkPill(
                        label = if (model.doneToday) "Marked today" else "Mark today",
                        onClick = {
                            val marking = !model.doneToday
                            val next = HabitStore.toggle(context, habit.id, today)
                            RitualWidgetProvider.refreshAll(context)
                            if (marking && next != null) {
                                // Past the thirty, every tenth day kept is worth a
                                // post, and he says so before asking.
                                val brag = Moments.bragDue(context, next, today)
                                saying = if (brag > 0) "$brag days!" else Moments.keptLine(next, today)
                                celebrate++; celebrating = true
                                if (brag > 0) onBrag(brag)
                            } else {
                                celebrating = false; room = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                } else {
                    Text(
                        "$year is closed. Come back to this year to mark a day.",
                        style = Body.copy(fontSize = 13.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(roomHeight))
                Spacer(Modifier.height(30.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }

        // A kept day: he comes up from below the bottom of the screen, grinning,
        // hops once with hearts, says the count, and sinks back out of sight. The
        // screen's own edge is the ledge, so there is nothing for his paws.
        if (celebrating) key(celebrate) {
            BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding().clipToBounds()) {
                val w = RISE_HEIGHT * MochiBody.RATIO
                MochiPose(
                    Pose.POP, RISE_HEIGHT,
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (maxWidth - w) * 0.3f, y = RISE_HEIGHT * (1f - MochiBody.ledgeAt())),
                    cheer = true, paws = false, say = saying,
                    onDone = { celebrating = false }
                )
            }
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

/** Mochi rising from the bottom of the screen when a day is kept. */
private val RISE_HEIGHT = 210.dp
