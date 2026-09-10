package com.ayan.ritual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.cloud.Account
import com.ayan.ritual.cloud.CloudSync
import com.ayan.ritual.data.Onboarding
import com.ayan.ritual.render.Mood
import kotlinx.coroutines.launch

/**
 * The first thing a new install shows: sign in, or say you would rather not.
 *
 * It is a door, not a wall. Every square Ritual keeps lives on the device, and
 * an account exists for exactly one reason — carrying a practice to the next
 * phone. So the way past this screen without one is a plain, visible choice
 * rather than a small grey line, and taking it costs nothing at all.
 *
 * It is also skipped outright when there is no Firebase project configured,
 * because a sign-in nobody can complete is a locked door.
 */
@Composable
fun WelcomeScreen(onSignedIn: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy by Account.busyState
    val error by Account.errorState
    val googleReady = remember { Account.googleAvailable(context) }

    // Whoever they came in as, the mirror starts on the same uid.
    val landed: (Boolean) -> Unit = { ok ->
        if (ok) {
            Account.uid?.let { CloudSync.start(context, it) }
            onSignedIn()
        }
    }

    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Lime)
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MochiTile(Mood.PLEASED, Paper, height = 64.dp, corner = 20.dp, inset = 16.dp)
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
            "Sign in and every square you fill is mirrored to your account, so a new phone picks up exactly where the old one left off.",
            style = Body
        )

        if (googleReady) {
            Spacer(Modifier.height(22.dp))
            InkPill(
                label = "Continue with Google",
                onClick = {
                    scope.launch {
                        val activity = context.findActivity() ?: return@launch
                        landed(Account.signInWithGoogle(activity))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                background = Paper,
                content = Ink,
                border = Ink
            )
        }

        Spacer(Modifier.height(10.dp))
        InkPill(
            label = "Continue with Apple",
            onClick = { context.findActivity()?.let { Account.signInWithApple(it, landed) } },
            modifier = Modifier.fillMaxWidth(),
            background = Paper,
            content = Ink,
            border = Ink
        )

        Spacer(Modifier.height(22.dp))
        Text(
            "or use an email",
            style = Body.copy(fontSize = 12.sp, color = InkFaint),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))
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
                if (creating) Account.createAccount(address, password, landed)
                else Account.signIn(address, password, landed)
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

        Spacer(Modifier.height(20.dp))
        InkPill(
            label = "Use Ritual on this phone",
            onClick = {
                Onboarding.skipSignIn()
                onSkip()
            },
            modifier = Modifier.fillMaxWidth(),
            background = Cream,
            content = Ink,
            border = Ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "No account, no network, nothing to lose track of. You can sign in later from the cat in the corner.",
            style = Body.copy(fontSize = 12.sp, color = InkFaint),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(34.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}
