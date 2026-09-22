package com.ayan.ritual.data

import java.time.LocalDate
import java.time.Year

/**
 * One practice, and every day it was kept.
 *
 * Days are stored as epoch-days: a single Long per completion, timezone-free
 * and trivially cheap to hold a decade of.
 */
data class Habit(
    val id: String,
    val name: String,
    /** When in the day this belongs — shown above the name on every card. */
    val slot: String,
    val accentIndex: Int,
    val createdEpochDay: Long,
    val done: Set<Long>,
    /**
     * Who this makes them, in their own words: "Ayan is someone who reads
     * regularly". Shown verbatim wherever the practice appears, because a
     * sentence someone wrote about themselves is the thing they came for and
     * the task underneath it is only the evidence.
     *
     * Stored as typed rather than as a predicate to be re-conjugated. Three
     * grammars for one fact reads well in a mockup and breaks on the first
     * person who writes a sentence nobody anticipated.
     */
    val identity: String = "",
    /**
     * Day one of the thirty. Usually the day it was created, but its own
     * field so a goal can be taken again after a lapse without rewriting
     * history.
     */
    val goalStartEpochDay: Long = createdEpochDay,
    /** The day the thirty were cleared, or null while it is still running. */
    val builtEpochDay: Long? = null
) {
    fun isDone(date: LocalDate): Boolean = done.contains(date.toEpochDay())

    /** Days completed in [year], as day-of-year numbers, ready for the grid. */
    fun daysOfYear(year: Int): Set<Int> {
        val first = LocalDate.of(year, 1, 1).toEpochDay()
        val last = LocalDate.of(year, 12, 31).toEpochDay()
        val out = HashSet<Int>()
        for (day in done) {
            if (day in first..last) out.add((day - first).toInt() + 1)
        }
        return out
    }

    fun totalIn(year: Int): Int = daysOfYear(year).size

    /** Consecutive days ending today — or yesterday, so a day in progress doesn't read as broken. */
    fun streak(today: LocalDate): Int {
        var day = today.toEpochDay()
        if (!done.contains(day)) day -= 1
        var n = 0
        while (done.contains(day)) {
            n++
            day--
        }
        return n
    }

    /** True when yesterday was missed and today is not yet marked — Mochi's cue. */
    fun missedYesterday(today: LocalDate): Boolean =
        !done.contains(today.minusDays(1).toEpochDay()) && !isDone(today)

    fun bestStreak(): Int {
        if (done.isEmpty()) return 0
        val sorted = done.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /* ── The thirty days ────────────────────────────────────────────────
       Everything below reads the same window: thirty days from the start,
       of which twenty four must be kept. Six may be missed, and six is said
       out loud everywhere rather than eighty per cent, because a budget you
       are spending is something a person can hold in their head and a grade
       you are failing is not. */

    /** Days of the goal that have already happened, 0 before it starts. */
    fun goalElapsed(today: LocalDate): Int =
        ((today.toEpochDay() - goalStartEpochDay) + 1)
            .coerceIn(0L, Goal.DAYS.toLong()).toInt()

    /** Days kept inside the window, whatever was marked outside it. */
    fun goalKept(): Int {
        val last = goalStartEpochDay + Goal.DAYS - 1
        return done.count { it in goalStartEpochDay..last }
    }

    /** Days spent from the allowance. Never negative, never over the cap. */
    fun goalMissed(today: LocalDate): Int =
        (goalElapsed(today) - goalKept()).coerceAtLeast(0)

    /** What is left of the six. Zero means the next miss ends it. */
    fun allowanceLeft(today: LocalDate): Int =
        (Goal.ALLOWED_MISSES - goalMissed(today)).coerceAtLeast(0)

    /** True once the thirty are done and enough of them were kept. */
    fun goalMet(today: LocalDate): Boolean =
        goalElapsed(today) >= Goal.DAYS && goalKept() >= Goal.NEEDED

    /**
     * True when the allowance is spent and the goal can no longer be met.
     * Worth knowing early: there is no point counting down to a day that
     * cannot arrive. It costs the goal and never the grid.
     */
    fun goalOutOfReach(today: LocalDate): Boolean =
        goalMissed(today) > Goal.ALLOWED_MISSES

    /** Already claimed and on the shelf. */
    val isBuilt: Boolean get() = builtEpochDay != null

    /** Days kept in total, which is what the shelf and the story card count. */
    val totalDone: Int get() = done.size

    companion object {
        /** Squares still unwritten in [year] — the days after today. */
        fun remainingIn(year: Int, today: LocalDate): Int =
            if (today.year != year) {
                if (today.year < year) Year.of(year).length() else 0
            } else {
                Year.of(year).length() - today.dayOfYear
            }
    }
}
