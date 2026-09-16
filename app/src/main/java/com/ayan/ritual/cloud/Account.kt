package com.ayan.ritual.cloud

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Who this device is signed in as, if anyone.
 *
 * Three ways in — Google, Apple, and an email and password — and they all
 * land on the same uid, which is the only thing the rest of the app knows
 * about. What a Ritual account *is* does not change with how you opened it.
 *
 * Each is offered only when it can actually work: Google needs the web client
 * id the google-services plugin generates, and both federated providers need
 * a Firebase project. Email is the floor, because it is the one that behaves
 * identically on both platforms.
 *
 * Signing in is entirely optional. Ritual works with no account at all, on
 * device, exactly as it did before; nothing here may become load-bearing for
 * marking a day.
 */
object Account {

    private val _uid = mutableStateOf<String?>(null)
    private val _email = mutableStateOf<String?>(null)
    private val _busy = mutableStateOf(false)
    private val _error = mutableStateOf<String?>(null)

    val uid: String? get() = _uid.value
    val uidState get() = _uid
    val email: String? get() = _email.value
    val emailState get() = _email
    val busyState get() = _busy
    val errorState get() = _error

    val signedIn: Boolean get() = _uid.value != null

    /**
     * Whether there is a Firebase project to sign in to at all.
     *
     * `google-services.json` is per-installation configuration and is not in
     * the repository, so a build made without one has no auth to offer. That
     * build must still be a working app — so callers ask this before putting
     * a sign-in step in anybody's way.
     */
    val available: Boolean get() = auth != null

    private var auth: FirebaseAuth? = null
    private var listening = false

    /** Safe to call on every launch; the first call wins. */
    fun start() {
        val instance = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return
        auth = instance
        if (listening) return
        listening = true
        instance.addAuthStateListener { a ->
            _uid.value = a.currentUser?.uid
            _email.value = a.currentUser?.email
        }
    }


    /**
     * The only email path the app offers: one button that ends with you signed
     * in, whether or not you had an account a moment ago.
     *
     * Asking someone to declare up front whether they are new is a question
     * they should not have to answer — they know their address and they know
     * their password, and Firebase already knows which of the two calls is the
     * right one. Creating first and falling back on a collision keeps the
     * error honest: a wrong password reports a wrong password rather than an
     * address that is already taken.
     */
    fun continueWithEmail(email: String, password: String, onDone: (Boolean) -> Unit = {}) {
        val instance = auth ?: return onDone(false)
        _busy.value = true
        _error.value = null
        instance.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { made ->
                if (made.isSuccessful) {
                    _busy.value = false
                    onDone(true)
                    return@addOnCompleteListener
                }
                if (made.exception !is FirebaseAuthUserCollisionException) {
                    _busy.value = false
                    _error.value = readable(made.exception?.message)
                    onDone(false)
                    return@addOnCompleteListener
                }
                // The address is already an account, so this is a return.
                instance.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { back ->
                        _busy.value = false
                        if (!back.isSuccessful) _error.value = readable(back.exception?.message)
                        onDone(back.isSuccessful)
                    }
            }
    }


    /**
     * Whether a Google button can do anything.
     *
     * The web client id is generated into resources by the google-services
     * plugin, which only runs when there is a google-services.json — so this
     * is looked up by name rather than referenced as R.string, which would not
     * compile in a checkout without one.
     */
    fun googleAvailable(context: Context): Boolean = webClientId(context) != null

    private fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        return if (id == 0) null else context.getString(id)
    }

    /**
     * Google, through Credential Manager rather than the retired GoogleSignIn
     * client — one sheet, which also offers a saved password or a passkey.
     *
     * Cancelling is not an error: the sheet is dismissed by tapping outside
     * it, which people do constantly, and a red line under it every time
     * would read as a fault rather than a choice.
     */
    suspend fun signInWithGoogle(activity: Activity): Boolean {
        val instance = auth ?: return false
        val serverClientId = webClientId(activity) ?: run {
            _error.value = "This build has no Google client id."
            return false
        }
        _busy.value = true
        _error.value = null
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
                .build()
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val token = GoogleIdTokenCredential.createFrom(response.credential.data).idToken
            instance.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            true
        } catch (cancelled: androidx.credentials.exceptions.GetCredentialCancellationException) {
            false
        } catch (failure: Exception) {
            // Credential Manager throws plenty of exceptions carrying no
            // message. The class name is not friendly, but it is a great deal
            // better than a sheet that closes onto nothing.
            _error.value = readable(failure.message ?: failure.javaClass.simpleName)
            false
        } finally {
            _busy.value = false
        }
    }

    /**
     * Apple, which on Android is Firebase's own web flow rather than a native
     * sheet. pendingAuthResult picks the flow back up when the process was
     * killed while the browser was in front.
     */
    fun signInWithApple(activity: Activity, onDone: (Boolean) -> Unit = {}) {
        val instance = auth ?: return onDone(false)
        _busy.value = true
        _error.value = null
        val provider = OAuthProvider.newBuilder("apple.com")
            .setScopes(listOf("email", "name"))
            .build()
        val task = instance.pendingAuthResult
            ?: instance.startActivityForSignInWithProvider(activity, provider)
        task.addOnCompleteListener { done ->
            _busy.value = false
            if (!done.isSuccessful) _error.value = readable(done.exception?.message)
            onDone(done.isSuccessful)
        }
    }

    fun signOut() {
        auth?.signOut()
        _error.value = null
    }

    /**
     * Deletes the account and everything stored under it, which Play requires
     * to be reachable from inside the app and not only from a web page.
     *
     * Order matters and is not negotiable: the cloud copy goes first, while
     * this account is still the one asking, because the rules only let an
     * account near its own path. Delete the user first and the rituals under
     * it become unreachable rather than deleted.
     *
     * The rituals on the phone are left exactly where they are. That is what
     * the deletion page promises, and it is the honest behaviour: this is a
     * local-first app, and someone closing an account has not asked to lose
     * the year they kept. Uninstalling removes that copy.
     *
     * Firebase refuses to delete a user whose sign-in is old, which is the one
     * failure that is worth explaining rather than retrying.
     */
    fun deleteAccount(onDone: (Boolean) -> Unit = {}) {
        val instance = auth ?: return onDone(false)
        val user = instance.currentUser ?: return onDone(false)
        _busy.value = true
        _error.value = null
        CloudSync.deleteEverything(user.uid) { cloudGone ->
            if (!cloudGone) {
                _busy.value = false
                _error.value = "Could not reach your rituals in the cloud. " +
                    "Try again with a connection."
                return@deleteEverything onDone(false)
            }
            user.delete().addOnCompleteListener { done ->
                _busy.value = false
                if (done.isSuccessful) {
                    _error.value = null
                } else {
                    _error.value =
                        if (done.exception is FirebaseAuthRecentLoginRequiredException)
                            "Firebase wants a fresh sign-in before it deletes an " +
                                "account. Sign out, sign back in, and delete again."
                        else readable(done.exception?.message)
                }
                onDone(done.isSuccessful)
            }
        }
    }

    /**
     * Firebase's messages are usable; its exception names are not, and two of
     * its failures are so common during setup that guessing at them from
     * "that didn't work" wastes an afternoon. Both are configuration rather
     * than anything the person tapping can fix, so both say so plainly.
     */
    private fun readable(message: String?): String {
        val text = message?.substringBefore(" [")?.takeIf { it.isNotBlank() }
            ?: return "That didn't work. Try again."
        return when {
            // The provider is off in Firebase, Authentication, Sign-in method.
            text.contains("OPERATION_NOT_ALLOWED", true) ||
                text.contains("operation is not allowed", true) ->
                "That way in is not switched on for Ritual yet."

            // The signing certificate of this build is not registered against
            // the Firebase project, so Play Services hands back a token that
            // nobody will accept.
            text.contains("Developer console is not set up", true) ||
                text.contains("28444") || text.contains("ApiException: 10") ->
                "Google sign-in is not set up for this build of Ritual."

            text.contains("network", true) || text.contains("timeout", true) ->
                "No connection. Try again when you have one."

            else -> text
        }
    }"
}
