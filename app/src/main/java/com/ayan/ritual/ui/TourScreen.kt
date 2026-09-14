package com.ayan.ritual.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.ShareCardRenderer
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
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopStart
        ) {
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
        body = "The widget is the whole product. Your year sits on the home screen, and the pill marks today without the app opening.",
        art = { PhoneHomeScreen() }
    ),
    Panel(
        caps = "When it is worth showing",
        title = "A year,\nas a story.",
        body = "Any ritual becomes a story card, with the grid you actually filled on it. No watermark, nothing to sign up for.",
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
    val doy = today.dayOfYear
    // A year that looks lived in rather than sampled: kept from January with
    // the gaps a real one has, and a run at the end for the card to be proud
    // of. The break before that run is what makes the streak number true.
    val done = buildSet {
        for (d in 1 until doy) if ((d * 7) % 9 != 0 && (d * 5) % 14 != 0) add(d)
        for (d in (doy - 9).coerceAtLeast(1) until doy) add(d)
        remove((doy - 10).coerceAtLeast(1))
        if (marked) add(doy)
    }
    return SlabModel(
        title = "Read before sleep",
        slot = "Evening",
        accent = accentAt(0),
        year = year,
        today = today,
        doneDaysOfYear = done,
        streak = if (marked) 10 else 9,
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

/**
 * The widget where it lives: a phone, a wallpaper, and the card sitting among
 * everything else on a home screen.
 *
 * The card inside is the real [SlabRenderer] output at a scaled density, so
 * this is a photograph of the widget rather than a drawing of one. The icons
 * around it are deliberately blank rounded squares — the point is the shape of
 * the space Ritual takes up next to everything else, and naming anything else
 * on the phone would be someone else's brand in our onboarding.
 */
@Composable
private fun PhoneHomeScreen() {
    val scale = 0.38f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Column(
            Modifier
                .size(width = 176.dp, height = 318.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(WALLPAPER)
                .padding(horizontal = 9.dp)
        ) {
            // Status bar: the time, and three bars that are not a logo.
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "9:41",
                    style = Caps.copy(fontSize = 8.sp, letterSpacing = 0.sp, color = WALL_INK)
                )
                Spacer(Modifier.weight(1f))
                Canvas(Modifier.size(width = 13.dp, height = 7.dp)) {
                    val w = size.width / 4.6f
                    listOf(0.4f, 0.7f, 1f).forEachIndexed { i, tall ->
                        drawRoundRect(
                            color = WALL_INK,
                            topLeft = Offset(i * w * 1.55f, size.height * (1f - tall)),
                            size = Size(w, size.height * tall),
                            cornerRadius = CornerRadius(w * 0.35f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            IconRow()
            Spacer(Modifier.height(12.dp))

            // The widget itself, at the size a 4x2 takes on a home screen.
            RitualCard(
                model = demoModel(false),
                height = 74.dp,
                action = true,
                cornerDp = 24f,
                scale = scale
            )

            Spacer(Modifier.height(12.dp))
            IconRow()
            Spacer(Modifier.weight(1f))

            // The dock.
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WALL_INK.copy(alpha = 0.10f))
                    .padding(vertical = 7.dp)
            ) { IconRow(count = 4, size = 26.dp) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Blank app icons: the shape of the neighbourhood, not the neighbours. */
@Composable
private fun IconRow(count: Int = 4, size: Dp = 30.dp) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(count) {
            Box(
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.28f))
                    .background(WALL_INK.copy(alpha = 0.13f))
            )
        }
    }
}

private val WALLPAPER = Color(0xFF3A382F)
private val WALL_INK = Color(0xFFF2EFE3)

/**
 * The story card itself, at a twentieth of the size.
 *
 * [ShareCardRenderer] sizes everything off the canvas width, so this is the
 * same composition Instagram gets rather than a sketch of it — down to the
 * wordmark under the block.
 */
@Composable
private fun StoryArt() {
    val density = LocalDensity.current
    val height = 286.dp
    val width = height * (ShareCardRenderer.STORY_W.toFloat() / ShareCardRenderer.STORY_H)
    val model = demoModel(true)
    val bitmap = remember(model, height) {
        with(density) {
            ShareCardRenderer.render(model, width.roundToPx(), height.roundToPx())
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, InkFaint), RoundedCornerShape(14.dp))
        )
    }
}
