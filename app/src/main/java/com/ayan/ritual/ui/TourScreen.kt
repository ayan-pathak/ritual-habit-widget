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
import androidx.compose.foundation.layout.BoxWithConstraints
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
        body = "A name and a colour. Every day you keep it, one square fills in, and the card turns to ink to say so.",
        art = { CardArt(marked = false) }
    ),
    Panel(
        caps = "On your home screen",
        title = "Never open\nthe app.",
        body = "Your year sits on the home screen, and the pill marks today without the app opening.",
        art = { PhoneHomeScreen() }
    ),
    Panel(
        caps = "When it is worth showing",
        title = "A year,\nas a story.",
        body = "The grid you actually filled, sized for a story. No watermark, nothing to sign up for.",
        art = { StoryArt() }
    )
)

@Composable
private fun TellPanel(panel: Panel) {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel(panel.caps)
        Spacer(Modifier.height(8.dp))
        Text(panel.title, style = Display.copy(fontSize = 28.sp, lineHeight = 30.sp))
        Spacer(Modifier.height(10.dp))
        Text(panel.body, style = Body.copy(fontSize = 13.sp, lineHeight = 19.sp))
        Spacer(Modifier.height(22.dp))
        panel.art()
        Spacer(Modifier.height(24.dp))
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
 * The widget where it lives: the top of a home screen, at the size it really is.
 *
 * Cropped rather than shrunk. A whole phone scaled down to fit this panel put
 * the year's grid at about a pixel a day, which is the one thing on the card
 * that has to survive — so this is a window onto a real home screen instead,
 * full width, cut off below. The card inside is the real [SlabRenderer] output
 * at very nearly its own size.
 *
 * The icons around it are blank rounded squares on purpose: the point is the
 * shape of the space Ritual takes up next to everything else, and naming
 * anything else on that phone would be someone else's brand in our onboarding.
 */
@Composable
private fun PhoneHomeScreen() {
    // The panel is 353dp across, so treating it as a 353dp-wide phone makes
    // every dp inside it the dp it would be on the real thing.
    val widgetWidth = 321.dp
    Column(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(WALLPAPER)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("9:41", style = Caps.copy(fontSize = 12.sp, letterSpacing = 0.sp, color = WALL_INK))
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.size(width = 19.dp, height = 11.dp)) {
                val w = size.width / 4.6f
                listOf(0.45f, 0.72f, 1f).forEachIndexed { i, tall ->
                    drawRoundRect(
                        color = WALL_INK,
                        topLeft = Offset(i * w * 1.55f, size.height * (1f - tall)),
                        size = Size(w, size.height * tall),
                        cornerRadius = CornerRadius(w * 0.35f)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        IconRow()
        Spacer(Modifier.height(16.dp))

        RitualCard(
            model = demoModel(false),
            height = 156.dp,
            action = true,
            cornerDp = 26f,
            scale = 0.91f
        )

        Spacer(Modifier.height(16.dp))
        // Half a row, because a home screen carries on below the fold.
        IconRow()
    }
}

/** Blank app icons: the shape of the neighbourhood, not the neighbours. */
@Composable
private fun IconRow(count: Int = 4, size: Dp = 56.dp) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(count) {
            Box(
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.26f))
                    .background(WALL_INK.copy(alpha = 0.13f))
            )
        }
    }
}

private val WALLPAPER = Color(0xFF3A382F)
private val WALL_INK = Color(0xFFF2EFE3)

/**
 * The story card, at the width of the panel and its own proportions.
 *
 * 1080 by 1920, not a thumbnail of it: at anything smaller the year collapses
 * into a smear and the one thing worth showing is gone. It is taller than the
 * panel, which is why the panel scrolls.
 */
@Composable
private fun StoryArt() {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth
        val height = width * (ShareCardRenderer.STORY_H.toFloat() / ShareCardRenderer.STORY_W)
        val model = demoModel(true)
        val bitmap = remember(model, width) {
            with(density) {
                ShareCardRenderer.render(model, width.roundToPx(), height.roundToPx())
            }
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, InkFaint), RoundedCornerShape(18.dp))
        )
    }
}
