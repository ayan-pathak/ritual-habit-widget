package com.ayan.ritual.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.cloud.Account
import com.ayan.ritual.cloud.CloudSync
import com.ayan.ritual.render.Mood

/**
 * The account, which is only ever about one thing: carrying a practice from
 * one phone to the next.
 *
 * The first launch asks for an account, but nothing after it does: sign out
 * here and every square stays exactly where it is, on the phone, with no
 * network involved. This screen is where that is undone and redone.
 */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val uid by Account.uidState
    val email by Account.emailState

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Row(Modifier.padding(start = 20.dp, top = 14.dp)) {
            RoundButton(onClick = onBack, size = 40.dp) {
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    drawLine(Ink, Offset(size.width * .62f, size.height * .18f),
                        Offset(size.width * .3f, size.height * .5f), size.width * .14f, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .3f, size.height * .5f),
                        Offset(size.width * .62f, size.height * .82f), size.width * .14f, StrokeCap.Round)
                }
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Text(
                if (uid != null) "Your rituals\nfollow you." else "Keep your\nsquares safe.",
                style = Display.copy(fontSize = 34.sp, lineHeight = 35.sp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (uid != null)
                    "Signed in as $email. Every ritual on this phone is mirrored to your account, and any phone you sign in on picks them up."
                else
                    "An account exists so a new phone can pick up where the old one left off. Ritual works perfectly well without one.",
                style = Body
            )
        }

        if (uid == null) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
                // Named, not a trailing lambda: SignInBlock grew an onSkip
                // after this argument, and a trailing lambda binds to the
                // last parameter, so it quietly became the skip handler.
                // There is nothing to skip to from here anyway.
                SignInBlock(label = "Sign in", onSignedIn = {})
            }
        } else {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Lime)
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MochiTile(Mood.PLEASED, Color.Transparent, height = 40.dp, inset = 0.dp)
                        Column {
                            Text("Mirrored", style = Display.copy(fontSize = 19.sp, lineHeight = 20.sp, color = OnLime))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Last written just now",
                                style = Body.copy(fontSize = 12.sp, color = OnLimeSoft)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                InkPill(
                    label = "Sign out",
                    onClick = {
                        CloudSync.stop()
                        Account.signOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    background = Cream,
                    content = Ink,
                    border = Ink
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Signing out leaves every ritual on this phone exactly where it is.",
                    style = Body.copy(fontSize = 12.sp, color = InkFaint),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(26.dp))
                DeleteAccount()
            }
        }

        // The tour asked this once; this is where it is changed afterwards.
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 30.dp)) {
            CapsLabel("Appearance")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Appearance.entries.forEach { value ->
                    val on = Look.appearance == value
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) Ink else Color.Transparent)
                            .then(
                                if (on) Modifier
                                else Modifier.border(
                                    BorderStroke(1.5.dp, InkFaint), RoundedCornerShape(999.dp)
                                )
                            )
                            .clickable { Look.set(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (value == Appearance.LIGHT) "Light" else "Dark",
                            style = Caps.copy(
                                fontSize = 12.sp,
                                letterSpacing = 0.sp,
                                color = if (on) OnInk else InkSoft
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(34.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

/**
 * Deleting the account, which Play requires to be reachable from inside the
 * app and not only from a web page.
 *
 * One tap opens the box and a second one inside it does the thing. That is
 * the whole ceremony: enough that nobody arrives here by accident, not so
 * much that someone who means it has to fight the app for it.
 */
@Composable
private fun DeleteAccount() {
    var asking by remember { mutableStateOf(false) }
    val busy by Account.busyState
    val error by Account.errorState

    if (!asking) {
        Text(
            "Delete your account",
            style = Body.copy(fontSize = 13.sp, color = Red),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Account.errorState.value = null
                    asking = true
                }
                .padding(vertical = 10.dp)
        )
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Paper)
            .padding(20.dp)
    ) {
        Text(
            "Delete your account?",
            style = Display.copy(fontSize = 19.sp, lineHeight = 22.sp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your account goes, and every ritual stored under it goes with it. " +
                "Nothing is kept afterwards.",
            style = Body.copy(fontSize = 13.sp, color = InkSoft)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "The rituals on this phone stay exactly where they are. " +
                "Uninstalling Ritual removes those.",
            style = Body.copy(fontSize = 13.sp, color = InkSoft)
        )

        if (error != null) {
            Spacer(Modifier.height(14.dp))
            Text(error!!, style = Body.copy(fontSize = 13.sp, color = Red))
        }

        Spacer(Modifier.height(18.dp))
        InkPill(
            label = if (busy) "Deleting" else "Delete everything",
            onClick = { if (!busy) Account.deleteAccount { gone -> if (gone) asking = false } },
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            background = Red,
            // Fixed, not Paper: Paper follows the theme and this red does not.
            content = Color(0xFFF4F2EA)
        )
        Spacer(Modifier.height(10.dp))
        InkPill(
            label = "Keep it",
            onClick = {
                if (!busy) {
                    Account.errorState.value = null
                    asking = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            background = Color.Transparent,
            content = Ink,
            border = Ink
        )
    }
}
