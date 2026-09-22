package com.ayan.ritual.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import java.time.LocalDate

/**
 * What I have built.
 *
 * The list of true sentences about someone, each with the grid that earned
 * it. Not a trophy case and not an archive: a description of the person they
 * currently are, with the evidence attached.
 *
 * Present tense throughout, because nothing here is finished. The thirty days
 * were the proof, not the end, and the count keeps climbing after it.
 *
 * Years are headings inside the page rather than the title of it. "What I
 * have built this year" reads well on 31 December and lies on 2 January, and
 * if it were true it would mean January deletes someone's proof, which is the
 * one thing this app must never do. As a heading it costs nothing and every
 * December grows a page worth keeping.
 */
@Composable
fun ShelfScreen(habits: List<Habit>, onBack: () -> Unit, onShare: () -> Unit) {
    val built = habits.filter { it.isBuilt }.sortedByDescending { it.builtEpochDay }
    val byYear = built.groupBy { LocalDate.ofEpochDay(it.builtEpochDay!!).year }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Row(Modifier.padding(start = 20.dp, top = 14.dp)) {
            RoundButton(onClick = onBack, size = 40.dp) {
                Canvas(Modifier.size(16.dp)) {
                    drawLine(
                        Ink, Offset(size.width * .62f, size.height * .18f),
                        Offset(size.width * .3f, size.height * .5f), size.width * .14f, StrokeCap.Round
                    )
                    drawLine(
                        Ink, Offset(size.width * .3f, size.height * .5f),
                        Offset(size.width * .62f, size.height * .82f), size.width * .14f, StrokeCap.Round
                    )
                }
            }
        }

        Text(
            "What I have built",
            style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )

        if (built.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Nothing yet. Keep a ritual thirty days, missing no more than " +
                    "six, and the sentence you wrote about yourself lands here.",
                style = Body.copy(fontSize = 14.sp, color = InkSoft),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        byYear.keys.sortedDescending().forEach { year ->
            Spacer(Modifier.height(26.dp))
            CapsLabel(year.toString(), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                byYear.getValue(year).forEach { ShelfEntry(it) }
            }
        }

        if (built.isNotEmpty()) {
            Spacer(Modifier.height(30.dp))
            InkPill(
                label = "Share what I built",
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                background = Paper,
                content = Ink,
                border = Ink
            )
        }

        Spacer(Modifier.height(30.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** One identity, its count, and the grid that earned it. No start date. */
@Composable
private fun ShelfEntry(habit: Habit) {
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Paper)
            .padding(16.dp)
    ) {
        Text(
            habit.identity.ifBlank { habit.name },
            style = Body.copy(fontSize = 14.sp, color = InkSoft)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${habit.totalDone} days",
            style = Display.copy(fontSize = 26.sp, lineHeight = 28.sp)
        )
        Spacer(Modifier.height(14.dp))
        RitualCard(
            model = habit.toModel(today, today.year),
            height = 76.dp,
            header = false,
            footer = false,
            cat = false,
            cornerDp = 16f,
            padDp = 11f
        )
    }
}
