package com.ayan.ritual.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.cloud.Account
import com.ayan.ritual.cloud.CloudSync
import kotlinx.coroutines.launch

/**
 * The three ways in, as one group.
 *
 * Google, Apple and an email address are the same offer made three ways, so
 * they are stacked together with nothing between them — three buttons, one
 * decision. Email is a button like the other two and only becomes a form once
 * it is chosen, because two empty fields sitting open under two one-tap
 * buttons make the whole screen look like work when most people will never
 * touch them.
 *
 * There is no question about whether you already have an account either:
 * [Account.continueWithEmail] works that out, because the person holding the
 * address and the password already knows the answer and should not have to
 * say it twice.
 *
 * One block, used by the first launch and by the account screen, so the two
 * cannot drift into two different sign-ins.
 */
@Composable
fun SignInBlock(label: String, onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy by Account.busyState
    val error by Account.errorState
    val googleReady = remember { Account.googleAvailable(context) }

    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailChosen by remember { mutableStateOf(false) }

    // Whoever they came in as, the mirror starts on the same uid.
    val landed: (Boolean) -> Unit = { ok ->
        if (ok) {
            Account.uid?.let { CloudSync.start(context, it) }
            onSignedIn()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        /* Whatever fails, the reason lands here. It used to sit inside the
           email branch, where a Google or Apple failure set a message that
           nothing ever drew: the sheet closed and the screen sat there. */
        if (error != null) {
            Text(error!!, style = Body.copy(fontSize = 13.sp, color = Red))
            Spacer(Modifier.height(14.dp))
        }

        if (googleReady) {
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
            Spacer(Modifier.height(10.dp))
        }

        InkPill(
            label = "Continue with Apple",
            onClick = { context.findActivity()?.let { Account.signInWithApple(it, landed) } },
            modifier = Modifier.fillMaxWidth(),
            background = Paper,
            content = Ink,
            border = Ink
        )

        Spacer(Modifier.height(10.dp))

        if (!emailChosen) {
            InkPill(
                label = "Continue with email",
                onClick = { emailChosen = true },
                modifier = Modifier.fillMaxWidth(),
                background = Paper,
                content = Ink,
                border = Ink
            )
        } else {
            Spacer(Modifier.height(8.dp))
            CapsLabel("Email")
            Spacer(Modifier.height(8.dp))
            Field(
                value = address,
                onValueChange = { address = it },
                placeholder = "you@example.com",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            CapsLabel("Password")
            Spacer(Modifier.height(8.dp))
            Field(
                value = password,
                onValueChange = { password = it },
                placeholder = "At least six characters",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                secret = true
            )

            Spacer(Modifier.height(18.dp))
            InkPill(
                label = if (busy) "Working…" else label,
                onClick = { Account.continueWithEmail(address, password, landed) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
