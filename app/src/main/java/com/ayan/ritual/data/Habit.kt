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
    val done: Set<Long>
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
