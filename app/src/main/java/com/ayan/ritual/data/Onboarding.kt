package com.ayan.ritual.data

import android.content.Context
import android.content.SharedPreferences

/**
 * The one answer the first launch needs, remembered so it never asks twice.
 *
 * It is a decision to *stop* asking: the unlock is offered once, on the way
 * out of the welcome, and offering it again on every launch would be nagging
 * rather than offering. A second ritual asks for itself when it is wanted.
 */
object Onboarding {

    private const val PREFS = "ritual_onboarding"
    private const val KEY_SAW_PAYWALL = "saw_paywall_v1"

    private var prefs: SharedPreferences? = null
    private var _sawPaywall = false

    /** Safe to call on every launch; the widget receiver never needs this. */
    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _sawPaywall = p.getBoolean(KEY_SAW_PAYWALL, false)
    }

    /** Whether the unlock has been offered once, unprompted. */
    val sawPaywall: Boolean get() = _sawPaywall

    fun markSawPaywall() {
        _sawPaywall = true
        prefs?.edit()?.putBoolean(KEY_SAW_PAYWALL, true)?.apply()
    }
}
