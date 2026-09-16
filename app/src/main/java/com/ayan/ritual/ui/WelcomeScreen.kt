package com.ayan.ritual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.render.Mood

/**
 * The first thing a new install shows.
 *
 * The name, then what the app is, then the three ways in — in that order,
 * because that is the order the questions arrive in: what is this, what does
 * it do, how do I start. Everything that was a second decision is gone. There
 * is no new-or-returning toggle, because [com.ayan.ritual.cloud.Account]
 * works that out from the address; and the three ways in sit as one group,
 * because they are one offer made three ways rather than a main path and a
 * fallback.
 *
 * The block takes the top half of the screen and the ways in sit on the
 * bottom edge, with the slack between them rather than underneath. A column
 * of content that stops two thirds of the way down reads as a form someone
 * forgot to finish; the same content pushed to both edges reads as a screen.
 * That is also why the height is measured rather than guessed: a fixed dp
 * that fills a tall phone leaves a short one scrolling to reach the buttons.
 *
 * It is skipped outright when there is no Firebase project configured,
 * because a sign-in nobody can complete is a locked door.
 */
@Composable
fun WelcomeScreen(onSignedIn: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Measured inside the insets, so this is the glass the screen actually
        // has. When the keyboard comes up it shrinks, the block shrinks with
        // it, and the fields stay in front of the person typing into them.
        val screen = maxHeight
        val block = screen * 0.53f

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = screen)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Ritual",
                    style = Display.copy(fontSize = 40.sp, lineHeight = 42.sp)
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(block)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Lime)
                ) {
                    Text(
                        "A year is a grid\nof empty squares.",
                        style = Display.copy(fontSize = 30.sp, lineHeight = 32.sp, color = OnLime),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(26.dp)
                    )
                    // On the bottom edge of the block, the way he stands on
                    // every other surface in the app, and with no tile behind
                    // him: a pale panel under a pale cat was the one place two
                    // near-whites met and he lost his edges to it.
                    MochiTile(
                        mood = Mood.PLEASED,
                        tile = Color.Transparent,
                        height = block * 0.54f,
                        inset = 0.dp,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            Column {
                Spacer(Modifier.height(22.dp))
                Text(
                    "Every square you fill is mirrored to your account, so a new " +
                        "phone picks up exactly where the old one left off.",
                    style = Body.copy(fontSize = 13.sp, color = InkSoft)
                )
                Spacer(Modifier.height(16.dp))
                SignInBlock(label = "Start your journey", onSignedIn = onSignedIn)
            }
        }
    }
}
