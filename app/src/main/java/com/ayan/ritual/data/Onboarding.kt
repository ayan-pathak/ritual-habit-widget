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
    private const val KEY_NAME = "name_v1"
    private const val KEY_STAGE = "flow_stage_v2"
    private const val KEY_DRAFT_IDENTITY = "flow_identity_v2"
    private const val KEY_FIRST_HABIT = "flow_first_habit_v2"
    private const val KEY_FLOW_DONE = "flow_done_v2"

    private var prefs: SharedPreferences? = null
    private var _sawTour = false
    private var _sawPaywall = false
    private var _firstMark = -1L
    private var _skippedSignIn = false
    private var _name = ""

    /** Safe to call on every launch; the widget receiver never needs this. */
    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _sawTour = p.getBoolean(KEY_SAW_TOUR, false)
        _sawPaywall = p.getBoolean(KEY_SAW_PAYWALL, false)
        _firstMark = p.getLong(KEY_FIRST_MARK, -1L)
        _skippedSignIn = p.getBoolean(KEY_SKIPPED_SIGN_IN, false)
        _name = p.getString(KEY_NAME, "") ?: ""
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
     * What to call them, or blank.
     *
     * It exists so an identity can be a sentence about a person rather than a
     * line of app copy. "Ayan is someone who reads regularly" is a claim;
     * "You are someone who reads regularly" is a compliment, and people
     * discount compliments from software. Blank is a supported answer: the
     * sentence simply starts at "I am".
     */
    val name: String get() = _name

    fun setName(value: String) {
        _name = value.trim()
        prefs?.edit()?.putString(KEY_NAME, _name)?.apply()
    }

    /** How a sentence about them should start, with or without a name. */
    fun sentenceStart(): String = if (_name.isBlank()) "I am someone who " else "$_name is someone who "

    /**
     * Where the one onboarding flow has got to, or blank before it starts.
     *
     * Kept on disk rather than in the composition, so rotating the phone, a
     * trip out to a browser for sign-in, or Android reclaiming the process
     * resumes the step someone was on instead of asking everything again.
     */
    val stage: String get() = prefs?.getString(KEY_STAGE, "") ?: ""

    fun setStage(value: String) {
        prefs?.edit()?.putString(KEY_STAGE, value)?.apply()
    }

    /** The identity sentence while it is still being written. */
    val draftIdentity: String get() = prefs?.getString(KEY_DRAFT_IDENTITY, "") ?: ""

    fun setDraftIdentity(value: String) {
        prefs?.edit()?.putString(KEY_DRAFT_IDENTITY, value)?.apply()
    }

    /** The ritual the flow created, so the widget and share steps can show it. */
    val firstHabitId: String get() = prefs?.getString(KEY_FIRST_HABIT, "") ?: ""

    fun setFirstHabitId(value: String) {
        prefs?.edit()?.putString(KEY_FIRST_HABIT, value)?.apply()
    }

    /**
     * Whether the flow still has something to ask.
     *
     * Someone who installed before this flow existed already has rituals and
     * no stage on disk, and must never be walked through it. Someone who has
     * just built their first ritual inside it has both, and still has the
     * widget and the share card ahead of them.
     */
    fun needsFlow(hasRituals: Boolean): Boolean {
        if (prefs?.getBoolean(KEY_FLOW_DONE, false) == true) return false
        return !hasRituals || stage.isNotEmpty()
    }

    fun finishFlow() {
        _sawTour = true
        prefs?.edit()
            ?.putBoolean(KEY_FLOW_DONE, true)
            ?.putBoolean(KEY_SAW_TOUR, true)
            ?.remove(KEY_STAGE)
            ?.remove(KEY_DRAFT_IDENTITY)
            ?.apply()
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
