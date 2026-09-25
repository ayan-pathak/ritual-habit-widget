package com.ayan.ritual.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.render.Accent
import com.ayan.ritual.render.Beat
import com.ayan.ritual.render.Cat
import com.ayan.ritual.render.MochiArt
import com.ayan.ritual.render.MochiBody
import com.ayan.ritual.render.MochiMotion
import com.ayan.ritual.render.Mood
import com.ayan.ritual.render.Pose
import com.ayan.ritual.render.SlabModel
import com.ayan.ritual.render.SlabRenderer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ayan.ritual.data.Goal
import com.ayan.ritual.data.Habit
import java.time.LocalDate

/**
 * A rendered card, sized to its box. The bitmap is cached against the model so
 * scrolling a list of rituals doesn't redraw 365 cells a frame.
 */
@Composable
fun RitualCard(
    model: SlabModel,
    height: Dp,
    modifier: Modifier = Modifier,
    header: Boolean = true,
    footer: Boolean = true,
    action: Boolean = false,
    cat: Boolean = true,
    quarterRuler: Boolean = false,
    cornerDp: Float = 24f,
    padDp: Float = 17f,
    scale: Float = 1f
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val wPx = constraints.maxWidth
        val hPx = with(density) { height.roundToPx() }
        val bitmap = remember(wPx, hPx, model, header, footer, action, cat, quarterRuler, padDp, scale) {
            SlabRenderer.render(
                wPx, hPx, model,
                SlabRenderer.Config(
                    density = density.density * scale,
                    header = header,
                    footer = footer,
                    action = action,
                    cat = cat,
                    quarterRuler = quarterRuler,
                    cornerDp = cornerDp,
                    padDp = padDp
                )
            )
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Mochi on a coloured tile, the app's only mascot surface.
 *
 * He breathes and blinks while nothing is happening, and answers a change of
 * [mood] with a beat: a hop when a day is marked, a sink when a streak breaks.
 * The motion is derived from state exactly as the face is, so it never fires
 * for decoration. Pass a [motion] to drive a beat the mood alone cannot say.
 *
 * Each mood is rasterised once at this size and then moved, rather than
 * redrawn: seventy-five paths a frame is not worth it for a squash.
 */
@Composable
fun MochiTile(
    mood: Mood,
    tile: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    inset: Dp = 8.dp,
    motion: MochiMotion? = null
) {
    val density = LocalDensity.current
    val px = with(density) { height.toPx() }
    val w = with(density) { Cat.widthFor(px).toDp() }

    val animated = animationsAllowed()
    // Remembered unconditionally, then discarded if the caller brought its
    // own: a `remember` behind an `if` would shift the slot table under us.
    val own = remember { MochiMotion(mood) }
    val rig = motion ?: own

    var shown by remember { mutableStateOf(mood) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var rise by remember { mutableFloatStateOf(0f) }

    // The first composition is not a change, so opening a screen never starts
    // him hopping; only a mood that moves under him does.
    var last by remember { mutableStateOf(mood) }
    LaunchedEffect(mood, animated) {
        if (!animated) {
            rig.snapTo(mood)
        } else if (mood != last) {
            rig.play(
                when (mood) {
                    Mood.PLEASED -> Beat.MARK
                    Mood.LET_DOWN -> Beat.MISS
                    else -> Beat.SETTLE
                },
                mood
            )
        }
        last = mood
    }

    LaunchedEffect(rig, animated) {
        if (!animated) return@LaunchedEffect
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                // A dropped frame must not become a lurch, so the step is
                // capped: he falls behind the clock rather than teleporting.
                val dt = if (previous == 0L) 0f
                else ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
                previous = now
                rig.advance(dt)
                scaleX = rig.scaleX
                scaleY = rig.scaleY
                rise = rig.offsetY
                shown = rig.mood
            }
        }
    }

    val frames = remember(px) { HashMap<Mood, ImageBitmap>() }

    // He grows out of the bottom edge rather than floating in the middle of
    // the tile: a portrait cropped by its own frame reads as a character, and
    // a portrait centred in one reads as a sticker. The hop then lifts him
    // clear of that edge, which is the whole point of the hop.
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(tile)
            .padding(start = inset, end = inset, top = inset * 0.8f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.size(w, height)) {
            val face = if (animated) shown else mood
            val image = frames.getOrPut(face) {
                val bitmap = android.graphics.Bitmap.createBitmap(
                    size.width.roundToInt().coerceAtLeast(1),
                    size.height.roundToInt().coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                Cat.draw(android.graphics.Canvas(bitmap), 0f, 0f, size.height, face)
                bitmap.asImageBitmap()
            }
            withTransform({
                // From the bottom centre: a squash presses him onto the tile
                // instead of shrinking him toward the middle of the frame.
                scale(scaleX, scaleY, Offset(size.width / 2f, size.height))
                translate(0f, rise * size.height / MochiArt.VIEW_H)
            }) {
                drawImage(image)
            }
        }
    }
}

/**
 * Mochi with his whole body, in the [pose] for the screen he is on.
 *
 * He stands on the bottom edge of the box that holds him: the frame is pulled
 * down so its ground line lands exactly on that edge, and a parent that clips
 * hides the few pixels below it. [height] is the whole frame, headroom for a
 * hop included. Bump [kick] to make him hop in answer to something tapped;
 * the first composition never counts, so opening a screen does not start him.
 *
 * Handing him a different [pose] blends into it rather than cutting. A
 * [Pose.POP] plays once from the start and then calls [onDone]. [ground] is
 * the pose whose ground line to stand on, when that is not the one he is
 * in yet (walking in toward it).
 *
 * The frame is a function of the clock alone, so this only keeps time. With
 * animations off he holds a single frame, and a pop is skipped.
 */
@Composable
fun MochiPose(
    pose: Pose,
    height: Dp,
    modifier: Modifier = Modifier,
    kick: Int = 0,
    say: String? = null,
    cheer: Boolean = false,
    dir: Float = -1f,
    walkLift: Float = 0f,
    ground: Pose = pose,
    onDone: (() -> Unit)? = null
) {
    val animated = animationsAllowed()
    val once = pose == Pose.POP
    val seed = remember { (Math.random() * 100).toFloat() }
    val phase = remember { if (once) 0f else (Math.random() * 2).toFloat() }
    var clock by remember { mutableFloatStateOf(if (once) 0f else 0.9f) }
    var kickAt by remember { mutableFloatStateOf(-10f) }
    var seen by remember { mutableIntStateOf(kick) }
    var shown by remember { mutableStateOf(pose) }
    var from by remember { mutableStateOf<Pose?>(null) }
    var switchedAt by remember { mutableFloatStateOf(-10f) }

    LaunchedEffect(kick) {
        if (kick != seen) { seen = kick; kickAt = clock }
    }
    LaunchedEffect(pose) {
        if (pose != shown) { from = shown; switchedAt = clock; shown = pose }
    }
    LaunchedEffect(animated) {
        if (!animated) {
            if (once) onDone?.invoke()
            return@LaunchedEffect
        }
        val end = if (once) MochiBody.popSeconds(cheer) else Float.MAX_VALUE
        var first = 0L
        while (clock < end) {
            withFrameNanos { now ->
                if (first == 0L) first = now
                clock = phase + (now - first) / 1_000_000_000f
            }
        }
        onDone?.invoke()
    }

    val sink = height * (1f - MochiBody.groundAt(ground))
    val extras = remember(say, cheer, dir, walkLift) { MochiBody.Extras(say, cheer, dir, walkLift) }
    Canvas(modifier.offset(y = sink).size(height * MochiBody.RATIO, height)) {
        val t = if (animated) clock else 0.9f
        val now = shown
        drawIntoCanvas {
            MochiBody.draw(
                it.nativeCanvas, size.height, now, t,
                kickAge = if (animated) t - kickAt else -1f, seed = seed,
                mood = if (cheer) Mood.PLEASED else now.mood, extras = extras,
                from = from, sinceSwitch = if (animated) t - switchedAt else MochiBody.BLEND
            )
        }
    }
}

/**
 * Draws the content at ([x], [y]) from where it would sit, without taking up
 * any room: for Mochi appearing over something and leaving again, which must
 * not push the page around while he is there.
 */
fun Modifier.floating(x: Dp, y: Dp) = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(0, 0) { placeable.place(x.roundToPx(), y.roundToPx()) }
}

/**
 * Mochi walking in from [from] away (negative: from the left) to where the
 * layout puts him, then settling into [pose]. Once per visit to the screen:
 * an arrival is a greeting, and a picture already on the wall is not.
 */
@Composable
fun MochiWalkIn(
    pose: Pose,
    height: Dp,
    from: Dp,
    modifier: Modifier = Modifier,
    walkLift: Float = 0f,
    say: String? = null
) {
    val animated = animationsAllowed()
    val walkX = remember { Animatable(if (animated) from.value else 0f) }
    var arrived by remember { mutableStateOf(!animated) }
    LaunchedEffect(Unit) {
        if (arrived) return@LaunchedEffect
        val secs = (abs(from.value) / 130f).coerceIn(1.4f, 2.6f)
        launch { delay(((secs - 0.25f) * 1000).toLong()); arrived = true }
        walkX.animateTo(0f, tween((secs * 1000).toInt(), easing = CubicBezierEasing(0.3f, 0.55f, 0.4f, 1f)))
    }
    MochiPose(
        pose = if (arrived) pose else Pose.WALK,
        height = height,
        modifier = modifier.offset { IntOffset(walkX.value.dp.roundToPx(), 0) },
        dir = if (from.value > 0f) -1f else 1f,
        walkLift = walkLift,
        ground = pose,
        say = say
    )
}

/**
 * False when the device has animations turned off, at which point Mochi holds
 * still and swaps faces outright.
 */
@Composable
private fun animationsAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }
}

@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = InkSoft) {
    Text(text.uppercase(), style = Caps.copy(color = color), modifier = modifier)
}

/** The primary control: a solid black pill. Nothing else in the app is this loud. */
@Composable
fun InkPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
    background: Color = Ink,
    content: Color = Paper,
    border: Color? = null,
    leading: @Composable (() -> Unit)? = null
) {
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(BorderStroke(2.dp, border), CircleShape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = content),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            leading?.invoke()
            Text(
                label,
                style = Caps.copy(
                    color = content,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A circular icon button with a 2dp outline — the app's secondary action. */
@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    background: Color = Color.Transparent,
    border: Color? = Ink,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(BorderStroke(2.dp, border), CircleShape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun StatCell(value: String, label: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Column(modifier) {
        Text(value, style = Display.copy(fontSize = 24.sp, lineHeight = 25.sp, color = color))
        CapsLabel(label, color = InkSoft, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The colour chooser: six flat tiles, the chosen one ringed in ink. */
@Composable
fun ColourTiles(
    accents: List<Accent>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        accents.forEachIndexed { index, accent ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(accent.block))
                    .then(
                        // Fixed ink: the swatch is a fixed colour, so the ring
                        // that marks it has to be the ink chosen against it.
                        if (selected) Modifier.border(BorderStroke(2.5.dp, OnLime), RoundedCornerShape(15.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onSelect(index) }
                    )
            )
        }
    }
}

/**
 * The one text field in the app. Paper on cream, no outline, and no label
 * inside it — the caps label above does that job.
 */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardOptions,
    secret: Boolean = false
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Paper)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Body.copy(fontSize = 16.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = keyboard,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = Body.copy(fontSize = 16.sp, color = InkFaint))
                }
                inner()
            }
        )
    }
}

/**
 * The thirty days, and what is left of the six.
 *
 * Six bars rather than a percentage, and spent ones filled in red, because a
 * budget is something a person can hold in their head and act on. "Eighty per
 * cent" is a grade, and the moment a habit app grades someone it has become
 * the thing this one exists to be an alternative to.
 *
 * A spent day is never framed as damage. The bar fills, the grid keeps every
 * square it had, and nothing resets.
 */
@Composable
fun GoalBand(habit: Habit, today: LocalDate, modifier: Modifier = Modifier) {
    val built = habit.builtEpochDay
    Column(modifier.fillMaxWidth()) {
        when {
            built != null -> {
                CapsLabel("Built")
                Spacer(Modifier.height(7.dp))
                Text(
                    "${habit.totalDone} days kept, and still counting.",
                    style = Body.copy(fontSize = 13.sp, color = InkSoft)
                )
            }

            habit.goalOutOfReach(today) -> {
                CapsLabel("The thirty")
                Spacer(Modifier.height(7.dp))
                Text(
                    "All six missed days are spent, so this run cannot reach " +
                        "thirty. Every square you have stays exactly where it is.",
                    style = Body.copy(fontSize = 13.sp, color = InkSoft)
                )
            }

            else -> {
                val used = habit.goalMissed(today).coerceAtMost(Goal.ALLOWED_MISSES)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CapsLabel("Day ${habit.goalElapsed(today)} of ${Goal.DAYS}")
                    CapsLabel("$used of ${Goal.ALLOWED_MISSES} missed days used")
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(Goal.ALLOWED_MISSES) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i < used) Red else InkFaint)
                        )
                    }
                }
            }
        }
    }
}
