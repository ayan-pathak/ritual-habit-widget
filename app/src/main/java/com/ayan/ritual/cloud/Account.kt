package com.ayan.ritual.cloud

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
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

    fun signIn(email: String, password: String, onDone: (Boolean) -> Unit = {}) {
        val instance = auth ?: return onDone(false)
        _busy.value = true
        _error.value = null
        instance.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _busy.value = false
                if (!task.isSuccessful) _error.value = readable(task.exception?.message)
                onDone(task.isSuccessful)
            }
    }

    fun createAccount(email: String, password: String, onDone: (Boolean) -> Unit = {}) {
        val instance = auth ?: return onDone(false)
        _busy.value = true
        _error.value = null
        instance.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _busy.value = false
                if (!task.isSuccessful) _error.value = readable(task.exception?.message)
                onDone(task.isSuccessful)
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
            _error.value = readable(failure.message)
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

    /** Firebase's messages are usable; its exception names are not. */
    private fun readable(message: String?): String =
        message?.substringBefore(" [") ?: "That didn't work. Try again."
}
