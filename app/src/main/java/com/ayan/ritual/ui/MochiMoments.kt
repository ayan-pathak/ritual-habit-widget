package com.ayan.ritual.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Habit
import com.ayan.ritual.render.Pose
import com.ayan.ritual.render.accentAt
import kotlinx.coroutines.delay

/* Mochi's own bubble is cream with an ink outline whatever the appearance,
   the same as the one he draws, so these are fixed rather than themed. */
private val BubbleFill = Color(0xFFF4F2EA)
private val BubbleInk = Color(0xFF12120F)

/**
 * A speech bubble for Mochi, as wide as what it says, its tail at the bottom
 * left toward his face. It pops in after [delayMillis].
 */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, delayMillis: Long = 0L) {
    val animated = animationsAllowed()
    var shown by remember { mutableStateOf(!animated) }
    LaunchedEffect(Unit) {
        if (animated) { delay(delayMillis); shown = true }
    }
    val s by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "bubble"
    )
    Column(
        modifier.graphicsLayer {
            scaleX = s; scaleY = s; alpha = s.coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(0.1f, 1f)
        }
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(BubbleFill)
                .border(2.5.dp, BubbleInk, CircleShape)
                .padding(horizontal = 15.dp, vertical = 9.dp)
        ) {
            Text(text, style = Display.copy(fontSize = 17.sp, lineHeight = 18.sp, color = BubbleInk))
        }
        Canvas(Modifier.padding(start = 16.dp).offset(y = (-2.5).dp).size(16.dp, 11.dp)) {
            val tail = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width * 0.15f, size.height); close()
            }
            drawPath(tail, BubbleFill)
            val w = 2.5.dp.toPx()
            drawLine(BubbleInk, Offset(0f, 0f), Offset(size.width * 0.15f, size.height), w, StrokeCap.Round)
            drawLine(BubbleInk, Offset(size.width, 0f), Offset(size.width * 0.15f, size.height), w, StrokeCap.Round)
        }
    }
}

/**
 * Past day thirty, every tenth day kept: Mochi with his phone out, saying
 * they deserve a post, and a way to make it. Asked once per milestone.
 */
@Composable
fun BragSheet(habit: Habit, days: Int, onShare: () -> Unit, onDismiss: () -> Unit) {
    val accent = accentAt(habit.accentIndex)
    var up by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { up = true }
    val k by animateFloatAsState(if (up) 1f else 0f, tween(320), label = "sheet")
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = k }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - k) * size.height }
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Paper)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(38.dp, 4.dp)
                    .clip(CircleShape)
                    .background(InkFaint)
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(accent.block))
            ) {
                MochiPose(Pose.SELFIE, 150.dp, Modifier.align(Alignment.BottomCenter))
                SpeechBubble(
                    "you deserve it!",
                    Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp),
                    delayMillis = 500L
                )
            }
            Spacer(Modifier.height(18.dp))
            CapsLabel("$days days kept")
            Spacer(Modifier.height(6.dp))
            Text("Time for a post.", style = Display.copy(fontSize = 26.sp, lineHeight = 28.sp))
            Spacer(Modifier.height(8.dp))
            Text(
                "${habit.identity.ifBlank { habit.name }.trim().removeSuffix(".")}. " +
                    "Another ten days of proof. Your real grid, as a story.",
                style = Body.copy(color = InkSoft)
            )
            Spacer(Modifier.height(18.dp))
            InkPill(
                label = "Post it on Instagram",
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Canvas(Modifier.size(17.dp)) {
                        val w = size.width
                        drawRoundRect(Paper, Offset(w * .06f, w * .06f), Size(w * .88f, w * .88f),
                            CornerRadius(w * .28f), style = Stroke(w * .12f))
                        drawCircle(Paper, radius = w * .21f, center = Offset(w * .5f, w * .5f), style = Stroke(w * .12f))
                        drawCircle(Paper, radius = w * .06f, center = Offset(w * .72f, w * .28f))
                    }
                }
            )
            QuietLink("Not now", onDismiss)
        }
    }
}
