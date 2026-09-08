package com.ayan.ritual.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** What Mochi is doing, derived from the streak — never chosen for decoration. */
enum class Mood { AWAKE, PLEASED, RESTING, LET_DOWN }

/**
 * Mochi, a 20x18 bitmap.
 *
 * Only the eye and mouth rows change between moods, so the silhouette never
 * shifts — that is what keeps a pixel mascot from looking redrawn each time.
 */
object Cat {

    const val COLS = 20
    const val ROWS = 18

    private val BASE = arrayOf(
        "....KK........KK....",
        "...KDGK......KGDK...",
        "...KGPK......KPGK...",
        "..KGGPGKKKKKKGPGGK..",
        "..KGGGGGGGGGGGGGGK..",
        ".KGGGGGGGGGGGGGGGGK.",
        ".KGGGGGGGGGGGGGGGGK.",
        ".KGEEEGGGGGGGGEEEGK.",
        ".KGEBEGGGGGGGGEBEGK.",
        ".KGEEEGGGGGGGGEEEGK.",
        ".KGGGGGGGPPGGGGGGGK.",
        ".KGGGGGGKPPKGGGGGGK.",
        "..KGGGGGGKKGGGGGGK..",
        "..KKGGGGGGGGGGGGKK..",
        "....KGGGGGGGGGGK....",
        "....KGGGGGGGGGGK....",
        "....KGGGGGGGGGGK....",
        "....KKKKKKKKKKKK...."
    )

    private fun rowsFor(mood: Mood): Array<String> {
        val r = BASE.copyOf()
        when (mood) {
            Mood.AWAKE -> Unit
            Mood.PLEASED -> {
                r[7] = ".KGGKGGGGGGGGGGKGGK."
                r[8] = ".KGKGKGGGGGGGGKGKGK."
                r[9] = ".KGGGGGGGGGGGGGGGGK."
                r[11] = ".KGGGGGKKPPKKGGGGGK."
            }
            Mood.RESTING -> {
                r[7] = ".KGGGGGGGGGGGGGGGGK."
                r[8] = ".KGKKKGGGGGGGGKKKGK."
                r[9] = ".KGGGGGGGGGGGGGGGGK."
            }
            Mood.LET_DOWN -> {
                r[7] = ".KGKGKGGGGGGGGKGKGK."
                r[8] = ".KGGKGGGGGGGGGGKGGK."
                r[9] = ".KGGGGGGGGGGGGGGGGK."
                r[12] = "..KGGGGKGGGGKGGGGK.."
            }
        }
        return r
    }

    private fun colorOf(ch: Char): Int = when (ch) {
        'K' -> 0xFF12120F.toInt()   // outline
        'D' -> 0xFF56564E.toInt()   // ear shadow
        'G' -> 0xFF8A8A80.toInt()   // coat
        'P' -> 0xFFE0A3A3.toInt()   // ear pink, nose
        'E' -> 0xFFCFE85F.toInt()   // eye — carries the brand colour
        'B' -> 0xFF12120F.toInt()   // pupil
        else -> 0
    }

    /** Width this cat occupies when each pixel is [px] wide. */
    fun widthFor(px: Float) = COLS * px

    /** Height this cat occupies when each pixel is [px] wide. */
    fun heightFor(px: Float) = ROWS * px

    /**
     * Draws Mochi with his top-left at [left], [top], one bitmap pixel per
     * [px] device pixels. Pixels are drawn a hair oversized so no seams show
     * between them at fractional scales.
     */
    fun draw(canvas: Canvas, left: Float, top: Float, px: Float, mood: Mood) {
        val paint = Paint()
        val bleed = 0.5f
        val rows = rowsFor(mood)
        val rect = RectF()
        for (y in 0 until ROWS) {
            val row = rows[y]
            for (x in 0 until COLS) {
                val c = colorOf(row[x])
                if (c == 0) continue
                paint.color = c
                rect.set(
                    left + x * px,
                    top + y * px,
                    left + x * px + px + bleed,
                    top + y * px + px + bleed
                )
                canvas.drawRect(rect, paint)
            }
        }
    }

    /** The mood the app should show given today's state and the running streak. */
    fun moodFor(doneToday: Boolean, streak: Int, missedYesterday: Boolean): Mood = when {
        doneToday -> Mood.PLEASED
        missedYesterday && streak == 0 -> Mood.LET_DOWN
        else -> Mood.AWAKE
    }
}
