package com.ayan.ritual.render

import java.time.LocalDate
import java.time.Year

/**
 * The commit-grid coordinate system: weeks run left to right, weekdays top to
 * bottom, Sunday first — the arrangement everyone already knows how to read.
 */
object GridGeo {

    const val ROWS = 7

    /** Row that Jan 1 of [year] occupies. Sunday = 0. */
    fun startOffset(year: Int): Int = LocalDate.of(year, 1, 1).dayOfWeek.value % 7

    /** Number of week-columns needed to hold [year]. */
    fun colsFor(year: Int): Int {
        val len = Year.of(year).length()
        return (startOffset(year) + len - 1) / ROWS + 1
    }

    /** Column of the week containing [date]. */
    fun colOf(date: LocalDate): Int = (startOffset(date.year) + date.dayOfYear - 1) / ROWS

    /** Row (weekday) of [date]. */
    fun rowOf(date: LocalDate): Int = (startOffset(date.year) + date.dayOfYear - 1) % ROWS

    /** Column at which each month of [year] begins, for the month ruler. */
    fun monthColumns(year: Int): List<Pair<Int, Int>> =
        (1..12).map { m -> m to colOf(LocalDate.of(year, m, 1)) }

    fun lengthOf(year: Int): Int = Year.of(year).length()
}

val MONTH_INITIALS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
val MONTH_SHORT = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
)
