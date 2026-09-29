package com.ayan.ritual.data

import android.content.Context
import java.time.LocalDate

/**
 * What Mochi says when a day is kept, and when a run is worth a post.
 *
 * Shared by the app and the widget receiver, which can start the process
 * cold, so every entry point takes a context and reads its own preferences.
 */
object Moments {

    private const val PREFS = "ritual_moments"
    private const val KEY_BRAGGED = "bragged_"
    private const val KEY_PENDING_ID = "pending_brag_id"
    private const val KEY_PENDING_N = "pending_brag_n"
    private const val KEY_SAY = "widget_say_"
    private const val KEY_SAY_UNTIL = "widget_say_until_"

    /** Past day thirty, every this many days kept is worth a post. */
    const val BRAG_EVERY = 10

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * The count, as he says it when a day is kept: the run when there is one,
     * otherwise the total, so the number is always the bigger good news.
     */
    fun keptLine(habit: Habit, today: LocalDate): String {
        val streak = habit.streak(today)
        return when {
            streak > 1 -> "$streak in a row!"
            habit.totalDone > 1 -> "${habit.totalDone} days now!"
            else -> "day one!"
        }
    }

    /**
     * The days kept, when today's mark makes it a round number past the
     * thirty that has not been asked about yet; otherwise zero.
     */
    fun bragDue(context: Context, habit: Habit, today: LocalDate): Int {
        val n = habit.totalDone
        if (!habit.isBuilt || n < 30 + BRAG_EVERY || n % BRAG_EVERY != 0 || !habit.isDone(today)) return 0
        return if (prefs(context).getInt(KEY_BRAGGED + habit.id, 0) < n) n else 0
    }

    /** Asked once per milestone, whatever the answer. */
    fun markBragged(context: Context, habitId: String, n: Int) {
        val p = prefs(context)
        val edit = p.edit().putInt(KEY_BRAGGED + habitId, n)
        if (p.getString(KEY_PENDING_ID, null) == habitId) edit.remove(KEY_PENDING_ID).remove(KEY_PENDING_N)
        edit.apply()
    }

    /** A milestone kept from the widget, to ask about when the app next opens. */
    fun setPendingBrag(context: Context, habitId: String, n: Int) {
        prefs(context).edit().putString(KEY_PENDING_ID, habitId).putInt(KEY_PENDING_N, n).apply()
    }

    fun pendingBrag(context: Context): Pair<String, Int>? {
        val p = prefs(context)
        val id = p.getString(KEY_PENDING_ID, null) ?: return null
        return id to p.getInt(KEY_PENDING_N, 0)
    }

    /** Something for the widget to say until [untilMillis], or nothing. */
    fun setWidgetSay(context: Context, widgetId: Int, say: String?, untilMillis: Long) {
        val edit = prefs(context).edit()
        if (say == null) edit.remove(KEY_SAY + widgetId).remove(KEY_SAY_UNTIL + widgetId)
        else edit.putString(KEY_SAY + widgetId, say).putLong(KEY_SAY_UNTIL + widgetId, untilMillis)
        edit.apply()
    }

    fun widgetSay(context: Context, widgetId: Int, nowMillis: Long): String? {
        val p = prefs(context)
        return if (p.getLong(KEY_SAY_UNTIL + widgetId, 0L) > nowMillis) p.getString(KEY_SAY + widgetId, null) else null
    }
}
