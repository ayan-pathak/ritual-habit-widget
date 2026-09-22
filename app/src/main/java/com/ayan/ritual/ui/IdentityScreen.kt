package com.ayan.ritual.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.accentAt

/**
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
private enum class Step { NAME, IDENTITY }

/** Four sentences, as examples rather than options. */
private val SUGGESTIONS = listOf(
    "reads regularly",
    "works out consistently",
    "is off junk",
    "keeps a protein heavy diet"
)

@Composable
fun IdentityScreen(onDone: (String) -> Unit) {
    var step by remember { mutableStateOf(Step.NAME) }
    var name by remember { mutableStateOf(Onboarding.name) }

    when (step) {
        Step.NAME -> NameStep(
            name = name,
            onName = { name = it },
            onNext = {
                Onboarding.setName(name)
                step = Step.IDENTITY
            }
        )

        Step.IDENTITY -> IdentityStep(
            start = Onboarding.sentenceStart(),
            onDone = onDone
        )
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("Ritual", style = Display.copy(fontSize = 30.sp, lineHeight = 32.sp))
        Spacer(Modifier.height(28.dp))
        Text(
            "What should we call you?",
            style = Display.copy(fontSize = 24.sp, lineHeight = 27.sp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "It goes on your own page and nowhere else. Skip it and Ritual will just say \"I\".",
            style = Body.copy(fontSize = 13.sp, color = InkSoft)
        )
        Spacer(Modifier.height(22.dp))
        PlainField(
            value = name,
            onValueChange = { if (it.length <= 20) onName(it) },
            placeholder = "Ayan"
        )

        Spacer(Modifier.weight(1f))

        InkPill(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(
            "Skip",
            style = Body.copy(fontSize = 13.sp, color = InkFaint),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable { onName(""); onNext() }
                .padding(vertical = 9.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IdentityStep(start: String, onDone: (String) -> Unit) {
    // The field opens already started, with the caret after the prefix, so
    // writing your own means finishing a sentence rather than composing one.
    var field by remember {
        mutableStateOf(TextFieldValue(start, TextRange(start.length)))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            "What identity are you building through consistency?",
            style = Display.copy(fontSize = 24.sp, lineHeight = 27.sp)
        )

        // The examples come before the field, and loose rather than listed. A
        // list reads as a menu you must choose from; four cards at different
        // heights read as things lying about that you may pick up. Either way
        // nobody meets an empty box with no idea what shape of answer fits.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SuggestionCloud(start = start, onPick = onDone)
        }

        CapsLabel("Or write your own")
        Spacer(Modifier.height(8.dp))
        // The prefix is theirs to delete if they want a different sentence,
        // but it is never re-added underneath them.
        SentenceField(value = field, onValueChange = { if (it.text.length <= 72) field = it })

        Spacer(Modifier.height(20.dp))
        InkPill(
            label = "Continue",
            onClick = { onDone(field.text.trim()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The four examples, adrift.
 *
 * Each carries its own phase so they never move as a block, which is the
 * difference between paper on a desk and a carousel. The motion is six pixels
 * and eight seconds: enough to read as loose, not enough to chase.
 */
@Composable
private fun SuggestionCloud(start: String, onPick: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val drift = rememberInfiniteTransition(label = "drift")

        SUGGESTIONS.forEachIndexed { i, tail ->
            val phase by drift.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7600 + i * 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "drift$i"
            )
            val lift = kotlin.math.sin((phase + i * 0.27f) * 2f * Math.PI).toFloat() * 5f
            val fromLeft = i % 2 == 0
            val x = if (fromLeft) w * (if (i == 0) 0f else 0.06f) else w * 0.10f
            val y = h * (0.03f + i * 0.245f)

            Box(
                Modifier
                    .offset(x = x, y = y + lift.dp)
                    .align(if (fromLeft) Alignment.TopStart else Alignment.TopEnd)
                    .widthIn(max = w * 0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Paper)
                    .border(1.dp, InkFaint, RoundedCornerShape(16.dp))
                    .clickable { onPick(start + tail) }
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(accentAt(i).block))
                    )
                    Text(
                        start + tail,
                        style = Body.copy(fontSize = 13.sp, color = Ink)
                    )
                }
            }
        }
    }
}

/** A line to write on, with nothing around it but the rule underneath. */
@Composable
private fun PlainField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Ruled {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Body.copy(fontSize = 17.sp, lineHeight = 22.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = Body.copy(fontSize = 17.sp, lineHeight = 22.sp, color = InkFaint)
                    )
                }
                inner()
            }
        )
    }
}

/** The same line, for a sentence that arrives already started. */
@Composable
private fun SentenceField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    Ruled {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Body.copy(fontSize = 17.sp, lineHeight = 23.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Ruled(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        content()
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(InkFaint))
    }
}
