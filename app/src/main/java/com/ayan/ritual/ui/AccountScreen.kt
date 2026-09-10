package com.ayan.ritual.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
 * Signing in is optional and stays optional. Everything Ritual does works with
 * no account on a device that has never seen a network, and nothing on this
 * screen is a step anyone has to take before keeping a day.
 */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uid by Account.uidState
    val email by Account.emailState
    val busy by Account.busyState
    val error by Account.errorState

    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

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
                CapsLabel("Email")
                Spacer(Modifier.height(8.dp))
                Field(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "you@example.com",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(16.dp))
                CapsLabel("Password")
                Spacer(Modifier.height(8.dp))
                Field(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "At least six characters",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    secret = true
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, style = Body.copy(fontSize = 13.sp, color = Red))
                }

                Spacer(Modifier.height(20.dp))
                InkPill(
                    label = if (busy) "Working…" else if (creating) "Create account" else "Sign in",
                    onClick = {
                        val done: (Boolean) -> Unit = { ok ->
                            if (ok) Account.uid?.let { CloudSync.start(context, it) }
                        }
                        if (creating) Account.createAccount(address, password, done)
                        else Account.signIn(address, password, done)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (creating) "I already have an account" else "I need an account",
                    style = Body.copy(fontSize = 13.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { creating = !creating }
                        .padding(vertical = 8.dp)
                )
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
                        MochiTile(Mood.PLEASED, Paper, pixel = 1.dp, corner = 14.dp, inset = 9.dp)
                        Column {
                            Text("Mirrored", style = Display.copy(fontSize = 19.sp, lineHeight = 20.sp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Last written just now",
                                style = Body.copy(fontSize = 12.sp)
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

        Spacer(Modifier.height(34.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Field(
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