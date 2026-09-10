package com.ayan.ritual.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** What Mochi is doing, derived from the streak — never chosen for decoration. */
enum class Mood { AWAKE, PLEASED, RESTING, LET_DOWN }

/**
 * Mochi, a 40x32 bitmap. The same cat iOS draws, from the same generator.
 *
 * Only the brow, eye and mouth rows change between moods, so the silhouette
 * never shifts — that is what keeps a pixel mascot from looking redrawn each
 * time. The head is an ellipse biased low with a gaussian bulge at cheek
 * level, so the face is widest where the muzzle is and the line curves *under*
 * it into the chin. A rounded rectangle chamfers there; this does not, which
 * is the whole difference between a puffy cheek and a hard jaw. The ears taper
 * to a single pixel at the tip.
 *
 * The extra pixels go into detail and never into changing the shape: an iris
 * with a dark rim, a lit arc along its bottom and a catchlight; blush that
 * falls off into the coat rather than ending on a hard edge; a fan of three
 * whiskers either side, drawn in mid grey so they read on the accent block and
 * on ink alike; ears with a pink inner held a pixel off the outer rim.
 *
 * See tools/mochi.py — the bitmap is generated, so edit it there.
 */
object Cat {

    const val COLS = 40
    const val ROWS = 32

    private val BASE = arrayOf(
        "...........K................K...........",
        "..........KLK..............KLK..........",
        "..........KPK.....KKKK.....KPK..........",
        ".........KLPLKKKKKLLLLKKKKKGPGK.........",
        ".........KPPPLLLLLGGGGLLLGGPPPK.........",
        "........KPPPPPGGGGGGGGGGGGPPPPPK........",
        ".......KLLLLLLGGGGGGGGGGGGLLLLLSK.......",
        ".......KKLLLLLLGGGGGGGGGGLLLLLLKK.......",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        ".........KLGGGGGGGGGGGGGGGGGGGK.........",
        "........KLGGGGGGGGGGGGGGGGGGGGSK........",
        "........KLGGGGGGGGGGGGGGGGGGGGGK........",
        ".......KLRRRGGGGGGGGGGGGGGGGRRRSK.......",
        ".......KHHHHRGGGGGGGGGGGGGGRHHHHK.......",
        "..S...KRHHHHHGGGGGGGGGGGGGGHHHHHRK....S.",
        "..SSS.KRHHHHRGGGGGKKKKGGGGGRHHHHRK.SSS..",
        "....SSKGGRRRGGGGGHPPPPHGGGGGRRRGGKS.....",
        "...SSSKGGGGGGGGGMMMPPMMMGGGGGGGGSKSSS...",
        ".SSS...KGGGGGGGMMMMMMMMMMGGGGGGSK...SSS.",
        ".....SS.KGGGGGGGMMMMMMMMGGGGGGSKSSS.....",
        "...SS....KSGGGGGGMMMMMMGGGGGGSK....SS...",
        "..S.......KKSGGGGGGGGGGGGGGSKK.......S..",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KGGGGGGGGGGGGGGK............",
        "............KKKKKKKKKKKKKKKK............"
    )

    private fun rowsFor(mood: Mood): Array<String> {
        val r = BASE.copyOf()
        when (mood) {
            Mood.AWAKE -> {
                r[8] = ".........KLGKKKKKGGGGGGKKKKKGGK........."
                r[9] = ".........KLKDEEEDKGGGGKDEEEDKGK........."
                r[10] = ".........KLKEWBBEKGGGGKEWBBEKGK........."
                r[11] = "........KLGKEBBBEKGGGGKEBBBEKGSK........"
                r[12] = "........KLGKDAAADKGGGGKDAAADKGGK........"
                r[13] = ".......KLRRRKKKKKGGGGGGKKKKKRRRSK......."
                r[19] = ".SSS...KGGGGGGGMMMKMMKMMMGGGGGGSK...SSS."
                r[20] = ".....SS.KGGGGGGGMMMKKMMMGGGGGGSKSSS....."
            }
            Mood.PLEASED -> {
                r[10] = ".........KLGGKKKGGGGGGGGKKKGGGK........."
                r[11] = "........KLGGKGGGKGGGGGGKGGGKGGSK........"
                r[12] = "........KLGKKGGGKKGGGGKKGGGKKGGK........"
                r[19] = ".SSS...KGGGGGGGMMKKMMKKMMGGGGGGSK...SSS."
                r[20] = ".....SS.KGGGGGGGMMMKKMMMGGGGGGSKSSS....."
            }
            Mood.RESTING -> {
                r[11] = "........KLGKKKKKKKGGGGKKKKKKKGSK........"
                r[12] = "........KLGGKKKKKGGGGGGKKKKKGGGK........"
            }
            Mood.LET_DOWN -> {
                r[5] = "........KPPPPPGKKKGGGGGKKKPPPPPK........"
                r[6] = ".......KLLLLLKKKGGGGGGGGGKKKLLLSK......."
                r[7] = ".......KKLLLKKKGGGGGGGGGGLKKKLLKK......."
                r[8] = ".........KLGKKKKKGGGGGGKKKKKGGK........."
                r[9] = ".........KLKKKKKKKGGGGKKKKKKKGK........."
                r[10] = ".........KLKEBBBEKGGGGKEBBBEKGK........."
                r[11] = "........KLGKEBBBEKGGGGKEBBBEKGSK........"
                r[12] = "........KLGKDAAADKGGGGKDAAADKGGK........"
                r[13] = ".......KLRRRKKKKKGGGGGGKKKKKRRRSK......."
                r[19] = ".SSS...KGGGGGGGMMMMKKMMMMGGGGGGSK...SSS."
                r[20] = ".....SS.KGGGGGGGMKKMMKKMGGGGGGSKSSS....."
            }
        }
        return r
    }

    private fun colorOf(ch: Char): Int = when (ch) {
        'K' -> 0xFF12120F.toInt()   // outline
        'L' -> 0xFFA8A89E.toInt()   // coat, lit rim
        'G' -> 0xFF8A8A80.toInt()   // coat
        'S' -> 0xFF6E6E66.toInt()   // coat, shaded rim — and the whiskers
        'P' -> 0xFFE0A3A3.toInt()   // ear pink, nose
        'H' -> 0xFFD69B96.toInt()   // blushed cheek
        'R' -> 0xFFAE968C.toInt()   // blush, falling off into the coat
        'M' -> 0xFFE8E3D2.toInt()   // muzzle
        'E' -> 0xFFC9F73F.toInt()   // iris — carries the brand colour
        'A' -> 0xFFE6FC9C.toInt()   // iris, lit along the bottom
        'D' -> 0xFF84A828.toInt()   // iris, rim
        'B' -> 0xFF12120F.toInt()   // pupil
        'W' -> 0xFFF4F2EA.toInt()   // catchlight
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
