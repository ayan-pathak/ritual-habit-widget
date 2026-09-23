package com.ayan.ritual.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.render.accentAt
import kotlinx.coroutines.delay

/*
 * Who they are trying to become, asked before anything is tracked.
 *
 * Nobody wants a habit. A habit is a means, and the thing someone actually
 * arrives with is a sentence they cannot yet say about themselves. Asking for
 * that sentence first makes the daily tap a vote rather than an entry, and it
 * gives the grid something to be evidence *of*.
 *
 * Two steps, because they are two different questions and stacking them makes
 * a form. The name comes first and is skippable; without it the sentence
 * simply starts at "I am".
 */

/** Sentence endings, offered as examples rather than options. */
internal val SUGGESTIONS = listOf(
    "reads before bed",
    "moves every day",
    "drinks enough water",
    "writes a little daily",
    "sleeps on time",
    "is off junk food"
)

/** How a sentence about them starts, with or without a name. */
internal fun sentenceStart(name: String): String =
    if (name.isBlank()) "I am someone who " else "${name.trim()} is someone who "

/** The colour a sentence ending is shown in: its chip's, or lime for their own words. */
internal fun accentIndexFor(tail: String): Int =
    SUGGESTIONS.indexOf(tail.trim()).coerceAtLeast(0)

@Composable
internal fun NameStep(name: String, onName: (String) -> Unit, onNext: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        CapsLabel("Nice to meet you")
        Spacer(Modifier.height(8.dp))
        Text("What should we\ncall you?", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Only you see it. It's how your sentence will start on the next screen.",
            style = Body.copy(color = InkSoft)
        )
        Spacer(Modifier.height(28.dp))
        PlainField(
            value = name,
            onValueChange = { if (it.length <= 20) onName(it) },
            placeholder = "Your first name"
        )

        Spacer(Modifier.weight(1f))

        InkPill(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
        QuietLink("Skip, just say “I”", onSkip)
    }
}

/**
 * The sentence, finished rather than composed.
 *
 * The sentence is the hero of the screen and it is never empty: while nothing
 * is chosen it types its own examples out, one after another, so the shape of
 * an answer is obvious before anyone has to think of one. Tapping an example
 * lands it in the sentence in that example's colour; writing their own does
 * the same in lime. Nothing here advances on its own, so trying one on is
 * free.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IdentityStep(start: String, tail: String, onTail: (String) -> Unit, onNext: () -> Unit) {
    val chosen = tail.isNotBlank()
    val accent = accentAt(accentIndexFor(tail))

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CapsLabel("One small sentence")
            Spacer(Modifier.height(8.dp))
            Text("Finish this\nsentence.", style = Display.copy(fontSize = 32.sp, lineHeight = 34.sp))
            Spacer(Modifier.height(12.dp))
            Text(
                "Something small you'd like to be true about you. It doesn't have to be true yet. That's what the next thirty days are for.",
                style = Body.copy(color = InkSoft)
            )
            Spacer(Modifier.height(22.dp))

            SentenceCard(start = start, tail = tail, accentBlock = Color(accent.block), accentOn = Color(accent.onBlock))

            Spacer(Modifier.height(20.dp))
            CapsLabel(if (chosen) "Try another" else "Tap one to try it on")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SUGGESTIONS.forEachIndexed { i, s ->
                    SuggestionChip(
                        text = s,
                        index = i,
                        selected = tail.trim() == s,
                        onClick = { onTail(s) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            CapsLabel("Or in your own words")
            Spacer(Modifier.height(6.dp))
            PlainField(
                value = if (SUGGESTIONS.contains(tail.trim())) "" else tail,
                onValueChange = { if (it.length <= 48) onTail(it) },
                placeholder = "plays guitar on weekends",
                capitalization = KeyboardCapitalization.None
            )
            Spacer(Modifier.height(18.dp))
        }

        Spacer(Modifier.height(12.dp))
        InkPill(
            label = if (chosen) "That's me" else "Pick one to continue",
            onClick = { if (chosen) onNext() },
            modifier = Modifier.fillMaxWidth(),
            background = if (chosen) Ink else InkFaint,
            content = if (chosen) Paper else InkSoft
        )
    }
}

/** The sentence on its own card, typing examples until it has a real ending. */
@Composable
private fun SentenceCard(start: String, tail: String, accentBlock: Color, accentOn: Color) {
    val typing = typewriter(SUGGESTIONS, active = tail.isBlank())
    val caret by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "caretAlpha"
    )
    // The ending pops a little when it changes, so a tap visibly lands.
    var pop by remember { mutableStateOf(1f) }
    LaunchedEffect(tail) {
        if (tail.isNotBlank()) {
            pop = 1.04f
            delay(140)
        }
        pop = 1f
    }
    val scale by animateFloatAsState(pop, spring(dampingRatio = 0.45f), label = "pop")

    val sentence = buildAnnotatedString {
        withStyle(SpanStyle(color = Ink)) { append(start) }
        if (tail.isNotBlank()) {
            withStyle(SpanStyle(color = accentOn, background = accentBlock)) { append(" ${tail.trim()}") }
            withStyle(SpanStyle(color = Ink)) { append(".") }
        } else {
            withStyle(SpanStyle(color = InkSoft)) { append(typing) }
            withStyle(SpanStyle(color = Ink.copy(alpha = caret))) { append("|") }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Paper)
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Text(
            sentence,
            style = Display.copy(fontSize = 27.sp, lineHeight = 34.sp),
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * One example as a flat block of its own colour. They arrive one after
 * another rather than all at once, which is most of what makes the screen
 * feel alive, and the chosen one is ringed in ink.
 */
@Composable
private fun SuggestionChip(text: String, index: Int, selected: Boolean, onClick: () -> Unit) {
    val accent = accentAt(index)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120L + index * 70L)
        shown = true
    }
    val enter by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "chipIn"
    )
    val ring by animateColorAsState(if (selected) Ink else Color.Transparent, label = "ring")

    Box(
        Modifier
            .scale(0.85f + 0.15f * enter)
            .alpha(enter.coerceIn(0f, 1f))
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(Color(accent.block))
            .border(BorderStroke(2.5.dp, ring), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (selected) "✓  $text" else text,
            style = Body.copy(fontSize = 14.sp, lineHeight = 18.sp, color = Color(accent.onBlock))
        )
    }
}

/** Types each example out and takes it back, forever, while [active]. */
@Composable
private fun typewriter(examples: List<String>, active: Boolean): String {
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var i = 0
        while (true) {
            val word = examples[i % examples.size]
            for (n in 1..word.length) { shown = word.take(n); delay(55) }
            delay(1400)
            for (n in word.length downTo 0) { shown = word.take(n); delay(22) }
            delay(260)
            i++
        }
    }
    return shown
}

/** A secondary way out: readable, and a full-size target, but never loud. */
@Composable
internal fun QuietLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = Body.copy(fontSize = 14.sp, color = InkSoft),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

/** A line to write on, with nothing around it but the rule underneath. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
) {
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Body.copy(fontSize = 18.sp, lineHeight = 24.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = Body.copy(fontSize = 18.sp, lineHeight = 24.sp, color = InkSoft.copy(alpha = 0.45f))
                    )
                }
                inner()
            }
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(InkFaint))
    }
}
