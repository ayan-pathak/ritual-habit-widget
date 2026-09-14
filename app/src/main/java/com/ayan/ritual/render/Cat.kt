package com.ayan.ritual.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

/** What Mochi is doing, derived from the streak — never chosen for decoration. */
enum class Mood {
    AWAKE, PLEASED, RESTING, LET_DOWN;

    /** Whether this face has eyes to shut, which is what makes a blink read. */
    val opensEyes: Boolean get() = this == AWAKE || this == LET_DOWN
}

/**
 * Mochi, four drawn portraits, replayed as vector paths.
 *
 * The four moods are one drawing with a different face. They come off a 2x2
 * sheet, and `tools/mochi.py` windows each quadrant out of it and registers
 * them against each other, so the head sits on the same coordinates in every
 * mood: the silhouette never shifts and he never looks redrawn between them.
 *
 * The paths live in [MochiArt], generated from `art/mochi` by that same
 * script, which also writes the Swift the iOS side reads — so the shapes the
 * two platforms draw come out of one conversion and cannot drift.
 *
 * **Why this is not a VectorDrawable.** The drawing is built from shapes that
 * abut rather than overlap, and each one antialiases its own edge, so the
 * background shows through every boundary as a hairline. Covering it means
 * growing each shape by half a pixel — and it has to be half a *device* pixel,
 * because the seam is one device pixel wide however far the art is scaled. A
 * VectorDrawable's stroke is in viewport units and scales with the drawing, so
 * a width that closes the seam at 30dp is a fat outline at 176px. Replaying
 * the paths means the stroke can be set per draw, from the scale in hand.
 */
object Cat {

    /** Width over height, so a caller can size him from either one. */
    const val ASPECT = MochiArt.VIEW_W / MochiArt.VIEW_H

    /** Width Mochi occupies when he is drawn [height] tall. */
    fun widthFor(height: Float) = height * ASPECT

    // ── The art, parsed once ────────────────────────────────────────────

    private class Item(
        val path: Path,
        val alpha: Int,
        val solid: Int,
        val gradient: IntArray?,
        val x0: Float, val y0: Float, val x1: Float, val y1: Float
    )

    private val cache = HashMap<Mood, List<Item>>(4)

    private fun source(mood: Mood) = when (mood) {
        Mood.AWAKE -> MochiArt.awake
        Mood.PLEASED -> MochiArt.pleased
        Mood.RESTING -> MochiArt.resting
        Mood.LET_DOWN -> MochiArt.letDown
    }

    private fun items(mood: Mood): List<Item> = cache.getOrPut(mood) {
        source(mood).lineSequence().mapNotNull { line ->
            if (line.isEmpty()) return@mapNotNull null
            val f = line.split('\t')
            when (f.getOrNull(0)) {
                "S" -> Item(
                    parse(f[3]), alphaOf(f[2]), f[1].toLong(16).toInt(),
                    null, 0f, 0f, 0f, 0f
                )
                "G" -> Item(
                    parse(f[8]), alphaOf(f[7]), 0,
                    intArrayOf(f[1].toLong(16).toInt(), f[2].toLong(16).toInt()),
                    f[3].toFloat(), f[4].toFloat(), f[5].toFloat(), f[6].toFloat()
                )
                else -> null
            }
        }.toList()
    }

    private fun alphaOf(text: String) =
        (text.toFloat().coerceIn(0f, 1f) * 255f).toInt()

    /**
     * Absolute `M`/`L`/`C`/`Z` only, which is all the converter ever emits.
     * Scanned over the raw chars rather than split into tokens, because this
     * runs over tens of thousands of numbers the first time a mood is drawn.
     */
    private fun parse(d: String): Path {
        val path = Path()
        var i = 0
        var startX = 0f
        var startY = 0f

        fun number(): Float {
            while (i < d.length && (d[i] == ' ' || d[i] == ',')) i++
            var sign = 1f
            if (i < d.length && d[i] == '-') { sign = -1f; i++ }
            var value = 0.0
            while (i < d.length && d[i] in '0'..'9') {
                value = value * 10 + (d[i] - '0'); i++
            }
            if (i < d.length && d[i] == '.') {
                i++
                var scale = 0.1
                while (i < d.length && d[i] in '0'..'9') {
                    value += (d[i] - '0') * scale
                    scale /= 10
                    i++
                }
            }
            return sign * value.toFloat()
        }

        while (i < d.length) {
            when (d[i]) {
                'M' -> {
                    i++
                    startX = number(); startY = number()
                    path.moveTo(startX, startY)
                }
                'L' -> { i++; path.lineTo(number(), number()) }
                'C' -> {
                    i++
                    path.cubicTo(number(), number(), number(), number(), number(), number())
                }
                'Z', 'z' -> { i++; path.close() }
                else -> i++
            }
        }
        return path
    }

    // ── Drawing ─────────────────────────────────────────────────────────

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Draws Mochi into the box whose top-left is ([left], [top]) and whose
     * height is [height].
     */
    fun draw(canvas: Canvas, left: Float, top: Float, height: Float, mood: Mood) {
        if (height <= 0f) return
        val scale = height / MochiArt.VIEW_H
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)

        // One device pixel, expressed in the coordinates now in force. Half of
        // it lands outside each shape, which is exactly the seam.
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f / scale
        paint.strokeJoin = Paint.Join.ROUND

        for (item in items(mood)) {
            if (item.gradient == null) {
                paint.shader = null
                paint.color = item.solid
            } else {
                paint.color = 0xFF000000.toInt()
                paint.shader = LinearGradient(
                    item.x0, item.y0, item.x1, item.y1,
                    item.gradient[0], item.gradient[1], Shader.TileMode.CLAMP
                )
            }
            paint.alpha = item.alpha
            canvas.drawPath(item.path, paint)
        }
        paint.shader = null
        canvas.restore()
    }

    /**
     * The mood the app should show given today's state and the running streak.
     *
     * Both faces smile with their eyes closed. [Mood.AWAKE]'s wide green eyes
     * read as a stare at the sizes he is actually drawn at — a 24dp badge is
     * two bright discs and not much else — so they are not used, and neither
     * is [Mood.LET_DOWN]: a sad cat is a punishment for a missed day, and a
     * missed day is already its own empty square.
     *
     * What is left still carries the reward. [Mood.RESTING] is a calm closed
     * smile and [Mood.PLEASED] is a blushing grin, so marking today still
     * changes his face, and that change still only ever answers state.
     */
    fun moodFor(doneToday: Boolean, streak: Int, missedYesterday: Boolean): Mood =
        if (doneToday) Mood.PLEASED else Mood.RESTING
}
