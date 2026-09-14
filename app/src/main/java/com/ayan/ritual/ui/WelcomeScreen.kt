package com.ayan.ritual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
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
 * It is skipped outright when there is no Firebase project configured,
 * because a sign-in nobody can complete is a locked door.
 */
@Composable
fun WelcomeScreen(onSignedIn: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(22.dp))

        // The name first, set the way it is set everywhere else in the app.
        Text(
            "Ritual",
            style = Display.copy(fontSize = 19.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp)
        )

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Lime)
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // No tile behind him. A pale panel under a pale cat was the
                // one place in the app where two near-whites met, and he lost
                // his edges to it; standing straight on the colour, he keeps
                // them.
                MochiTile(Mood.PLEASED, Color.Transparent, height = 72.dp, inset = 0.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "A year is a grid\nof empty squares.",
                    style = Display.copy(fontSize = 26.sp, lineHeight = 28.sp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "Every square you fill is mirrored to your account, so a new phone picks up exactly where the old one left off.",
            style = Body
        )

        Spacer(Modifier.height(22.dp))
        SignInBlock(label = "Start your journey", onSignedIn = onSignedIn)

        Spacer(Modifier.height(34.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}
