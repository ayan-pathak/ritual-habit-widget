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
                SignInBlock(label = "Sign in") { }
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
