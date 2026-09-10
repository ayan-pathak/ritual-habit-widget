package com.ayan.ritual.cloud

import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth

/**
 * Who this device is signed in as, if anyone.
 *
 * Email and password rather than Google or Apple, because the point of an
 * account here is moving a practice from one phone to another — and email is
 * the only sign-in that works the same on both platforms. The rest can be
 * added later without changing what a Ritual account *is*.
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

    fun signOut() {
        auth?.signOut()
        _error.value = null
    }

    /** Firebase's messages are usable; its exception names are not. */
    private fun readable(message: String?): String =
        message?.substringBefore(" [") ?: "That didn't work. Try again."
}
