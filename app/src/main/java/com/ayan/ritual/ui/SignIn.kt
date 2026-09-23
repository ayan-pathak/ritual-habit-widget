package com.ayan.ritual.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayan.ritual.cloud.Account
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
 * cannot drift into two different sign-ins. The first launch passes an
 * [onSkip] and the account screen does not, which is the only difference
 * between them: there is somewhere to go past a first launch, and nowhere to
 * go past a screen someone opened on purpose.
 */
@Composable
fun SignInBlock(label: String, onSignedIn: () -> Unit, onSkip: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy by Account.busyState
    val error by Account.errorState
    val note by Account.noteState
    val googleReady = remember { Account.googleAvailable(context) }

    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailChosen by remember { mutableStateOf(false) }

    // Signing in is an account and nothing more. Backup is part of the
    // unlock, and RitualApp starts it once both are true.
    val landed: (Boolean) -> Unit = { ok -> if (ok) onSignedIn() }

    Column(Modifier.fillMaxWidth()) {
        /* Whatever fails, the reason lands here. It used to sit inside the
           email branch, where a Google or Apple failure set a message that
           nothing ever drew: the sheet closed and the screen sat there.

           The build stamp under it is only drawn when something has already
           gone wrong, which is the one moment anybody needs to know which
           build they are holding. Half of diagnosing a sign-in is finding out
           whether the phone has the fix on it yet. */
        if (error != null || note != null) {
            Text(
                error ?: note!!,
                style = Body.copy(
                    fontSize = 13.sp,
                    color = if (error != null) Red else InkSoft
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildStamp(context) + "\n" + certStamp(context),
                style = Body.copy(fontSize = 11.sp, color = InkFaint)
            )
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

        /* The way past, where there is one.

           It is drawn the way the tour draws its Skip, quiet and last, for the
           same reason: an offer nobody is obliged to take should not look like
           one of the buttons. Signing in is what carries the squares to a
           second phone, and nothing else, so a first launch that cannot
           complete it should still reach the grid. Whoever waves it off finds
           the same three buttons in Account whenever they want them. */
        val skip = onSkip
        if (skip != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Continue without an account",
                style = Body.copy(fontSize = 14.sp, color = InkSoft),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { skip() }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

/**
 * Which build this is, read off the installed package rather than BuildConfig,
 * so it needs no build-system feature switched on to exist.
 *
 * It is shown under a failed sign-in and nowhere else, together with
 * [certStamp]. A version number in the corner of a working screen is clutter;
 * a version number under an error is the difference between "the fix is not
 * on the phone yet" and "the fix does not work".
 */
private fun buildStamp(context: android.content.Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
            info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    "Build ${info.versionName} ($code)"
}.getOrDefault("Build unknown")

/**
 * The SHA-1 of the certificate this copy of the app is actually signed with.
 *
 * Every party to a Google sign-in knows this value except the person trying to
 * debug one. The console lists what is registered, the app is signed with
 * whatever it is signed with, and when the two disagree the failure says
 * nothing about certificates at all. So the app reports its own, and the
 * comparison stops being a matter of trusting that the right row was copied
 * out of the right page.
 *
 * It sits under a failed sign-in, next to the build stamp, and nowhere else.
 */
private fun certStamp(context: android.content.Context): String = runCatching {
    val pm = context.packageManager
    val signatures: Array<android.content.pm.Signature> =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
            // apkContentsSigners is what a verifier sees. On a build Play
            // re-signed it is Play's key, which is the whole point of asking.
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            ).signatures ?: emptyArray()
        }
    val first = signatures.firstOrNull()
    if (first == null) {
        "SHA-1 unavailable"
    } else {
        java.security.MessageDigest.getInstance("SHA-1")
            .digest(first.toByteArray())
            .joinToString(":") { byte -> "%02X".format(byte) }
    }
}.getOrElse { "SHA-1 unreadable" }
