package com.ayan.ritual.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.accentAt
import java.time.LocalDate

/**
 * Three panels, once, before the grid.
 *
 * Each one shows the thing rather than describing it: the card as it will
 * look, the card on a home screen, the card as a story. The tour is short
 * because the app is — there is one gesture in it, and the panels exist to
 * say where that gesture pays off rather than to teach it.
 *
 * How it should look is asked first, on its own, because everything after it
 * is then shown the way it will actually be seen. A theme question at the end
 * asks someone to imagine the screens they have just been walked through.
 */
@Composable
fun TourScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = PANELS.size + 1          // the look question, then the three panels
    val last = steps - 1

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        ProgressRail(step = step, of = steps)

        Spacer(Modifier.height(30.dp))

        // The stage keeps its height across panels, so the pill at the bottom
        // never moves while someone is tapping it four times.
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) {
            if (step == 0) LookPanel() else TellPanel(PANELS[step - 1])
        }

        InkPill(
            label = if (step == last) "Start keeping days" else "Next",
            onClick = {
                if (step == last) {
                    Onboarding.markSawTour()
                    onDone()
                } else step++
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
            if (step != last) {
                Text(
                    "Skip",
                    style = Body.copy(fontSize = 13.sp, color = InkFaint),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            Onboarding.markSawTour()
                            onDone()
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** A segment per panel, filling as they are walked. */
@Composable
private fun ProgressRail(step: Int, of: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(of) { i ->
            // Each segment fills rather than switching on, so the rail reads as
            // one bar being drawn rather than four lights being lit.
            val fill by animateFloatAsState(
                targetValue = if (i <= step) 1f else 0f,
                animationSpec = tween(durationMillis = 320),
                label = "rail"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(InkFaint)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ink)
                )
            }
        }
    }
}

private class Panel(
    val caps: String,
    val title: String,
    val body: String,
    val art: @Composable () -> Unit
)

private val PANELS: List<Panel> = listOf(
    Panel(
        caps = "One gesture",
        title = "Name it.\nThen keep it.",
        body = "A ritual is a name and a colour. Every day you keep it, one square fills in, and the card turns to ink to say so.",
        art = { CardArt(marked = false) }
    ),
    Panel(
        caps = "On your home screen",
        title = "Never open\nthe app.",
        body = "The widget is the whole product. Your year sits on the home screen and the pill marks today without the app ever opening.",
        art = { WidgetArt() }
    ),
    Panel(
        caps = "When it is worth showing",
        title = "A year,\nas a story.",
        body = "Any ritual becomes a card sized for Instagram Stories, with the grid you actually filled on it. No watermark, no badge, nothing to sign up for.",
        art = { StoryArt() }
    )
)

@Composable
private fun TellPanel(panel: Panel) {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel(panel.caps)
        Spacer(Modifier.height(8.dp))
        Text(panel.title, style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(panel.body, style = Body)
        Spacer(Modifier.height(26.dp))
        panel.art()
    }
}

/**
 * The one question the tour asks rather than answers.
 *
 * It is answered by tapping the thing itself: two slabs of the actual colours,
 * and the whole app repaints behind the one that is tapped. A row of radio
 * buttons would be asking someone to read the word "dark" and imagine it.
 */
@Composable
private fun LookPanel() {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel("Before anything else")
        Spacer(Modifier.height(8.dp))
        Text("How should\nit look?", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Pick one and the rest of this walkthrough is shown that way. It is in the settings afterwards, so nothing here is final.",
            style = Body
        )
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LookChoice("Light", Appearance.LIGHT, Modifier.weight(1f))
            LookChoice("Dark", Appearance.DARK, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LookChoice(label: String, value: Appearance, modifier: Modifier = Modifier) {
    val chosen = Look.appearance == value
    val page = if (value == Appearance.DARK) Color(0xFF16150F) else Color(0xFFE7E3D4)
    val mark = if (value == Appearance.DARK) Color(0xFFEDE9DA) else Color(0xFF12120F)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(page)
            .border(
                width = if (chosen) 2.5.dp else 1.dp,
                color = if (chosen) Ink else InkFaint,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { Look.set(value) }
            .padding(16.dp)
    ) {
        // A scrap of the grid in that theme's own two colours, which says more
        // than the word does.
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val cell = size.width / 11f
            val gap = cell * 0.30f
            val step = cell * 0.78f + gap
            for (col in 0 until 8) {
                for (row in 0 until 4) {
                    val on = (col * 4 + row) % 3 != 2
                    drawRoundRect(
                        color = if (on) mark else mark.copy(alpha = 0.22f),
                        topLeft = Offset(col * step, row * step),
                        size = Size(cell * 0.78f, cell * 0.78f),
                        cornerRadius = CornerRadius(cell * 0.24f)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            label,
            style = Display.copy(fontSize = 17.sp, lineHeight = 18.sp, color = mark)
        )
    }
}

// ── The three things it is showing ──────────────────────────────────────────

private fun demoModel(marked: Boolean): SlabModel {
    val today = LocalDate.now()
    val year = today.year
    val done = buildSet {
        for (back in 1..58) if ((back * 7) % 11 != 0) add(today.dayOfYear - back)
        if (marked) add(today.dayOfYear)
    }.filter { it >= 1 }.toSet()
    return SlabModel(
        title = "Read before sleep",
        slot = "Evening",
        accent = accentAt(0),
        year = year,
        today = today,
        doneDaysOfYear = done,
        streak = 9,
        totalDone = done.size,
        remaining = Habit.remainingIn(year, today),
        doneToday = marked,
        mood = if (marked) Mood.PLEASED else Mood.RESTING
    )
}

@Composable
private fun CardArt(marked: Boolean) {
    RitualCard(model = demoModel(marked), height = 172.dp)
}

/** The card as it sits on a home screen, with the pill it is really tapped by. */
@Composable
private fun WidgetArt() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Ink.copy(alpha = 0.86f))
            .padding(14.dp)
    ) {
        RitualCard(model = demoModel(false), height = 158.dp, action = true, footer = true)
    }
}

/** The proportion of the thing, rather than the thing: 1080 by 1920. */
@Composable
private fun StoryArt() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Column(
            Modifier
                .size(width = 118.dp, height = 210.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Lime)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("2026", style = Display.copy(fontSize = 15.sp, lineHeight = 16.sp, color = Color(0xFF12120F)))
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                val cell = size.width / 13f
                val gap = cell * 0.32f
                val step = cell * 0.76f + gap
                for (col in 0 until 10) {
                    for (row in 0 until 7) {
                        val on = (col * 7 + row) % 4 != 3
                        drawRoundRect(
                            color = Color(0xFF12120F).copy(alpha = if (on) 1f else 0.22f),
                            topLeft = Offset(col * step, row * step),
                            size = Size(cell * 0.76f, cell * 0.76f),
                            cornerRadius = CornerRadius(cell * 0.22f)
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.size(26.dp)) {
                // Instagram's rounded square, ring and corner dot.
                val w = size.width
                val ink = Color(0xFF12120F)
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * .06f, w * .06f),
                    size = Size(w * .88f, w * .88f),
                    cornerRadius = CornerRadius(w * .28f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .10f)
                )
                drawCircle(ink, radius = w * .21f, center = Offset(w * .5f, w * .5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .10f))
                drawCircle(ink, radius = w * .055f, center = Offset(w * .72f, w * .28f))
            }
        }
    }
}
