package com.ayan.ritual.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Goal
import com.ayan.ritual.data.Habit
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.accentAt
import com.ayan.ritual.share.StoryShare
import com.ayan.ritual.widget.RitualWidgetProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The whole way in, as one flow.
 *
 * There used to be three — a sign-in screen, a four-panel tour and the
 * identity questions — each with its own start button, so a new install
 * felt like it was being onboarded twice. This is the only one: how it
 * should look, what to call them, the sentence, the bet, the ritual that
 * pays for it, and then the two things the app is actually for — the widget
 * and the story card — shown with their own ritual rather than a demo.
 *
 * The step lives on disk ([Onboarding.stage]), so rotation, Back, a trip to
 * the share sheet or Android reclaiming the process all come back to the
 * step someone was on.
 */
internal enum class FlowStep { LOOK, NAME, IDENTITY, COMMIT, CREATE, WIDGET, SHARE }

/** The steps before the ritual exists, which is what the progress rail counts. */
private val ASKING = listOf(FlowStep.LOOK, FlowStep.NAME, FlowStep.IDENTITY, FlowStep.COMMIT)

@Composable
fun OnboardingFlow(habits: List<Habit>, onFinished: (habitId: String?) -> Unit) {
    val context = LocalContext.current
    var step by remember {
        mutableStateOf(FlowStep.entries.firstOrNull { it.name == Onboarding.stage } ?: FlowStep.LOOK)
    }
    var name by remember { mutableStateOf(Onboarding.name) }
    var tail by remember { mutableStateOf(Onboarding.draftIdentity) }
    val start = sentenceStart(name)
    val identity = start + tail.trim()
    val habit = habits.firstOrNull { it.id == Onboarding.firstHabitId }

    // A double tap on a primary button must not land on whatever the next
    // step has under the same spot (the name step's Skip sits right there),
    // so each step ignores taps until it has finished arriving.
    var arrivedAt by remember { mutableStateOf(0L) }
    fun settled() = android.os.SystemClock.uptimeMillis() - arrivedAt > 450
    fun go(next: FlowStep) {
        if (!settled()) return
        arrivedAt = android.os.SystemClock.uptimeMillis()
        step = next
        Onboarding.setStage(next.name)
    }
    LaunchedEffect(Unit) { if (Onboarding.stage.isEmpty()) Onboarding.setStage(step.name) }

    // The ritual steps need the ritual. If it is gone (deleted from another
    // device, say), go back to making one rather than showing nothing.
    if ((step == FlowStep.WIDGET || step == FlowStep.SHARE) && habit == null) {
        LaunchedEffect(Unit) { go(FlowStep.CREATE) }
        return
    }

    // Back walks the flow backwards instead of leaving the app. Once the
    // ritual exists there is nothing behind it to undo, so Back finishes.
    BackHandler(enabled = step != FlowStep.LOOK) {
        when (step) {
            FlowStep.WIDGET, FlowStep.SHARE -> onFinished(habit?.id)
            else -> go(FlowStep.entries[step.ordinal - 1])
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally(tween(320)) { w -> if (forward) w / 5 else -w / 5 } + fadeIn(tween(320))) togetherWith
                fadeOut(tween(160))
        },
        label = "flow"
    ) { current ->
        if (current == FlowStep.CREATE) {
            CreateScreen(
                identity = identity,
                title = "Now, the ritual.",
                lead = "One small thing you'll do each day that makes it true.",
                initialAccent = accentIndexFor(tail),
                onDone = {
                    Onboarding.setFirstHabitId(it.id)
                    go(FlowStep.WIDGET)
                },
                onBack = { go(FlowStep.COMMIT) }
            )
            return@AnimatedContent
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(Cream)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            if (current in ASKING) ProgressRail(step = ASKING.indexOf(current), of = ASKING.size)
            else CapsLabel("Your ritual is ready")
            Spacer(Modifier.height(28.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (current) {
                    FlowStep.LOOK -> LookStep(onNext = { go(FlowStep.NAME) })

                    FlowStep.NAME -> NameStep(
                        name = name,
                        onName = { name = it },
                        onNext = {
                            Onboarding.setName(name)
                            go(FlowStep.IDENTITY)
                        },
                        onSkip = {
                            if (!settled()) return@NameStep
                            name = ""
                            Onboarding.setName("")
                            go(FlowStep.IDENTITY)
                        }
                    )

                    FlowStep.IDENTITY -> IdentityStep(
                        start = start,
                        tail = tail,
                        onTail = {
                            tail = it
                            Onboarding.setDraftIdentity(it)
                        },
                        onNext = { go(FlowStep.COMMIT) }
                    )

                    FlowStep.COMMIT -> CommitStep(
                        identity = identity,
                        accentIndex = accentIndexFor(tail),
                        onNext = { go(FlowStep.CREATE) }
                    )

                    FlowStep.WIDGET -> WidgetStep(
                        habit = habit!!,
                        onNext = { go(FlowStep.SHARE) }
                    )

                    FlowStep.SHARE -> ShareStep(
                        habit = habit!!,
                        onShare = { model ->
                            StoryShare.shareStreak(context, model)
                            onFinished(habit.id)
                        },
                        onSkip = { onFinished(habit.id) }
                    )

                    FlowStep.CREATE -> Unit
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Steps ───────────────────────────────────────────────────────────────────

/**
 * How it should look, answered by tapping the thing itself: two slabs in the
 * theme's actual colours, and the whole flow repaints behind the one tapped.
 */
@Composable
private fun LookStep(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        CapsLabel("Before anything else")
        Spacer(Modifier.height(8.dp))
        Text("How should\nit look?", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Pick one and everything after this is shown that way. You can change it in settings any time.",
            style = Body.copy(color = InkSoft)
        )
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LookChoice("Light", Appearance.LIGHT, Modifier.weight(1f))
            LookChoice("Dark", Appearance.DARK, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        InkPill(label = "Next", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The bet, said once and plainly: thirty days from today they get to say the
 * sentence. The date is a real date, and the thirty squares are the ones
 * they are about to fill, with today's already waiting.
 */
@Composable
private fun CommitStep(identity: String, accentIndex: Int, onNext: () -> Unit) {
    val accent = accentAt(accentIndex)
    val on = Color(accent.onBlock)
    val day = LocalDate.now().plusDays(Goal.DAYS.toLong())
    val date = day.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))

    Column(Modifier.fillMaxSize()) {
        CapsLabel("Thirty days from today")
        Spacer(Modifier.height(8.dp))
        Text("On $date,\nyou get to say it.", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(22.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(accent.block))
                .padding(22.dp)
        ) {
            Text("“$identity.”", style = Display.copy(fontSize = 25.sp, lineHeight = 30.sp, color = on))
            Spacer(Modifier.height(22.dp))
            ThirtySquares(color = on)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "One square a day. Keep ${Goal.NEEDED} of the ${Goal.DAYS} and it's yours. " +
                "There are ${Goal.ALLOWED_MISSES} spare days, because life happens.",
            style = Body.copy(color = InkSoft)
        )

        Spacer(Modifier.weight(1f))
        InkPill(label = "Let’s build your ritual", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

/** Thirty empty squares in three rows; the first one, today, breathes. */
@Composable
private fun ThirtySquares(color: Color) {
    val pulse by rememberInfiniteTransition(label = "today").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "todayAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(10) { col ->
                    val today = row == 0 && col == 0
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color.copy(alpha = if (today) 0.2f + 0.6f * pulse else 0.18f))
                    )
                }
            }
        }
    }
}

/**
 * The widget, offered with their own card on a home screen. On a launcher
 * that can pin, one tap puts it there; on one that cannot, it says how.
 */
@Composable
private fun WidgetStep(habit: Habit, onNext: () -> Unit) {
    val context = LocalContext.current
    val model = habit.toModel(LocalDate.now(), LocalDate.now().year)
    var manual by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text("Keep it where\nyou'll see it.", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            if (manual)
                "Long-press an empty spot on your home screen, tap Widgets, and drag Ritual out."
            else
                "Put it on your home screen. The pill marks today in one tap, without opening the app.",
            style = Body.copy(color = InkSoft)
        )
        Spacer(Modifier.height(20.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { PhoneHomeScreen(model) }
        Spacer(Modifier.height(12.dp))
        InkPill(
            label = if (manual) "Done" else "Add to home screen",
            onClick = {
                if (manual || requestPinWidget(context)) onNext() else manual = true
            },
            modifier = Modifier.fillMaxWidth()
        )
        QuietLink("I'll do it later", onNext)
    }
}

/**
 * The story card, with their sentence on it, offered once while it is fresh.
 * Sharing it here is free, like every share: the unlock only ever buys more
 * rituals.
 */
@Composable
private fun ShareStep(habit: Habit, onShare: (com.ayan.ritual.render.SlabModel) -> Unit, onSkip: () -> Unit) {
    val model = habit.toModel(LocalDate.now(), LocalDate.now().year)
    val glow by rememberInfiniteTransition(label = "nudge").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "nudgeAlpha"
    )

    Column(Modifier.fillMaxSize()) {
        Text("Tell people who\nyou're becoming.", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Saying it out loud makes it easier to keep. Here's your card, with your sentence on it.",
            style = Body.copy(color = InkSoft)
        )
        Spacer(Modifier.height(18.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) { StoryArt(model) }
        Spacer(Modifier.height(12.dp))
        InkPill(
            label = "Share to your story",
            onClick = { onShare(model) },
            modifier = Modifier.fillMaxWidth().alpha(glow)
        )
        QuietLink("Not now", onSkip)
    }
}

/** Asks the launcher to pin the widget. False when it cannot be asked. */
private fun requestPinWidget(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, RitualWidgetProvider::class.java), null, null)
}
