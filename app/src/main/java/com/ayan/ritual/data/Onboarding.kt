package com.ayan.ritual.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * What the first few days need to remember, so nothing is asked twice.
 *
 * Every value here is a decision to *stop* doing something: stop showing the
 * tour, stop mentioning the unlock. The unlock in particular is deliberately
 * not offered on the way in — someone who has not kept a day yet has nothing
 * to lose and no reason to pay, and asking then is the version of this app
 * nobody would have downloaded. It waits until they have marked a day and
 * come back the next one, which is the first moment a streak exists to keep.
 */
object Onboarding {

    private const val PREFS = "ritual_onboarding"
    private const val KEY_SAW_TOUR = "saw_tour_v1"
    private const val KEY_SAW_PAYWALL = "saw_paywall_v1"
    private const val KEY_FIRST_MARK = "first_mark_epoch_day_v1"
    private const val KEY_SKIPPED_SIGN_IN = "skipped_sign_in_v1"

    private var prefs: SharedPreferences? = null
    private var _sawTour = false
    private var _sawPaywall = false
    private var _firstMark = -1L
    private var _skippedSignIn = false

    /** Safe to call on every launch; the widget receiver never needs this. */
    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _sawTour = p.getBoolean(KEY_SAW_TOUR, false)
        _sawPaywall = p.getBoolean(KEY_SAW_PAYWALL, false)
        _firstMark = p.getLong(KEY_FIRST_MARK, -1L)
        _skippedSignIn = p.getBoolean(KEY_SKIPPED_SIGN_IN, false)
    }

    /** Whether the three panels have been walked through. */
    val sawTour: Boolean get() = _sawTour

    fun markSawTour() {
        _sawTour = true
        prefs?.edit()?.putBoolean(KEY_SAW_TOUR, true)?.apply()
    }

    /** Whether the unlock has been offered once, unprompted. */
    val sawPaywall: Boolean get() = _sawPaywall

    fun markSawPaywall() {
        _sawPaywall = true
        prefs?.edit()?.putBoolean(KEY_SAW_PAYWALL, true)?.apply()
    }

    /**
     * Whether the way in was declined once.
     *
     * Sign-in is how the squares reach a second device, and it is worth
     * asking for on the first launch, but it is not what the app is for: every
     * square already lives on the phone, and a first launch that cannot get
     * past a login is an app nobody sees. So the ask happens once, and once
     * waved off it does not come back on its own. It stays one tap away in
     * Account, which is where someone goes when they actually want it.
     */
    val skippedSignIn: Boolean get() = _skippedSignIn

    fun markSkippedSignIn() {
        _skippedSignIn = true
        prefs?.edit()?.putBoolean(KEY_SKIPPED_SIGN_IN, true)?.apply()
    }

    /**
     * The first day anyone ever marked, or -1. Written once and never moved,
     * so unmarking it later does not restart the clock on the offer below.
     */
    fun rememberFirstMark(day: LocalDate) {
        if (_firstMark >= 0) return
        _firstMark = day.toEpochDay()
        prefs?.edit()?.putLong(KEY_FIRST_MARK, _firstMark)?.apply()
    }

    /**
     * Whether this launch is the one that mentions the unlock: a day has been
     * kept, and at least one more has turned over since. A streak has to have
     * survived a night before it is worth anything to anyone.
     */
    fun unlockIsWorthMentioning(today: LocalDate): Boolean =
        !_sawPaywall && _firstMark >= 0 && today.toEpochDay() > _firstMark
}
