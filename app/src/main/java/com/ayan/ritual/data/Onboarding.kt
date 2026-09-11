package com.ayan.ritual.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * The two answers the first launch needs, remembered so it never asks twice.
 *
 * Both are decisions to *stop* asking. Ritual keeps every square on the device
 * whether or not anyone ever signs in, so the sign-in step has to be one a
 * person can walk past — and once they have walked past it, walking past it
 * again on every launch would be nagging rather than offering.
 */
object Onboarding {

    private const val PREFS = "ritual_onboarding"
    private const val KEY_SKIPPED = "skipped_sign_in_v1"
    private const val KEY_SAW_PAYWALL = "saw_paywall_v1"

    private var prefs: SharedPreferences? = null
    private val _skipped = mutableStateOf(false)
    private var _sawPaywall = false

    /** Safe to call on every launch; the widget receiver never needs this. */
    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _skipped.value = p.getBoolean(KEY_SKIPPED, false)
        _sawPaywall = p.getBoolean(KEY_SAW_PAYWALL, false)
    }

    /** Whether someone has said they would rather not have an account. */
    val skippedSignIn: Boolean get() = _skipped.value
    val skippedSignInState get() = _skipped

    fun skipSignIn() {
        _skipped.value = true
        prefs?.edit()?.putBoolean(KEY_SKIPPED, true)?.apply()
    }

    /** Whether the unlock has been offered once, unprompted. */
    val sawPaywall: Boolean get() = _sawPaywall

    fun markSawPaywall() {
        _sawPaywall = true
        prefs?.edit()?.putBoolean(KEY_SAW_PAYWALL, true)?.apply()
    }
}
