package com.ayan.ritual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Goal
import com.ayan.ritual.data.Habit
import com.ayan.ritual.render.Pose
import com.ayan.ritual.render.accentAt
import java.time.LocalDate

/**
 * The one moment the app raises its voice.
 *
 * Thirty days are done and enough of them were kept, so a sentence that was a
 * wish on day one is now a fact. Saying that, once, plainly, is the entire
 * product: everything else here is bookkeeping in service of this screen.
 *
 * No confetti and no badge. A number someone earned needs no decoration, and
 * decoration is how an app admits the number is not worth much. What it does
 * instead is state the sentence and the arithmetic behind it, and then get
 * out of the way.
 *
 * It does not end anything. The count keeps going, the grid keeps filling,
 * and the sentence stays true on day two hundred. A habit that stops the day
 * it is achieved was never a habit.
 */
@Composable
fun BuiltScreen(habit: Habit, onClaim: () -> Unit, onLater: () -> Unit) {
    val today = LocalDate.now()
    val accent = accentAt(habit.accentIndex)
    val missed = habit.goalMissed(today).coerceAtMost(Goal.ALLOWED_MISSES)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val block = maxHeight * 0.58f

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            CapsLabel("Day ${Goal.DAYS}")
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(block)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(accent.block))
            ) {
                Column(Modifier.align(Alignment.TopStart).padding(24.dp)) {
                    Text(
                        "You can say it now.",
                        style = Display.copy(
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                            color = Color(accent.onBlock)
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        habit.identity.ifBlank { habit.name },
                        style = Display.copy(
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            color = Color(accent.onBlock)
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${habit.goalKept()} of ${Goal.DAYS} days kept. " +
                            if (missed == 0) "None missed." else "$missed missed.",
                        style = Body.copy(
                            fontSize = 13.sp,
                            color = Color(accent.onBlock).copy(alpha = 0.70f)
                        )
                    )
                }
                // On the bottom edge, as everywhere, and jumping for joy with
                // confetti: they earned it, and the app is not asking for
                // anything.
                MochiPose(Pose.PARTY, block * 0.7f, Modifier.align(Alignment.BottomCenter))
            }

            Spacer(Modifier.weight(1f))

            InkPill(
                label = "Put it on the shelf",
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Keep going. The count does not stop.",
                style = Body.copy(fontSize = 13.sp, color = InkFaint),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .clickable(onClick = onLater)
                    .padding(vertical = 9.dp)
            )
        }
    }
}
