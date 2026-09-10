package com.ayan.ritual.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.ayan.ritual.R
import kotlin.math.roundToInt

/** What Mochi is doing, derived from the streak — never chosen for decoration. */
enum class Mood { AWAKE, PLEASED, RESTING, LET_DOWN }

/**
 * Mochi, four drawn portraits, as vectors.
 *
 * The four moods are one drawing with a different face. They come off a 2x2
 * sheet, and `tools/mochi.py` windows each quadrant out of it and registers
 * them against each other, so the head sits on the same coordinates in every
 * mood: the silhouette never shifts and he never looks redrawn between them.
 * That was the rule when he was a 20x18 grid and it is still the rule.
 *
 * They are `VectorDrawable`s rather than PNGs because he is drawn at sizes an
 * order of magnitude apart — 30dp in the widget header, 176px on the story
 * card — and a raster picked for one is wrong for the other. A vector is
 * resolved at whatever size the Canvas asks for, so the widget bitmap and the
 * share card are both drawn at their own full resolution.
 *
 * [load] must run before [draw], like [Fonts.load] — the widget receiver can
 * start the process cold.
 */
object Cat {

    /** The viewport the art was generated in. Only its ratio matters here. */
    private const val VIEW_W = 883.12f
    private const val VIEW_H = 913.06f

    /** Width over height, so a caller can size him from either one. */
    const val ASPECT = VIEW_W / VIEW_H

    private val cache = HashMap<Mood, Drawable>(4)
    private var app: Context? = null

    fun load(context: Context) {
        app = context.applicationContext
    }

    /** Width Mochi occupies when he is drawn [height] tall. */
    fun widthFor(height: Float) = height * ASPECT

    /**
     * Draws Mochi into the box whose top-left is ([left], [top]) and whose
     * height is [height]. A drawable that failed to load draws nothing rather
     * than taking the widget down with it.
     */
    fun draw(canvas: Canvas, left: Float, top: Float, height: Float, mood: Mood) {
        val art = drawable(mood) ?: return
        val l = left.roundToInt()
        val t = top.roundToInt()
        art.setBounds(l, t, l + widthFor(height).roundToInt(), t + height.roundToInt())
        art.draw(canvas)
    }

    private fun drawable(mood: Mood): Drawable? {
        cache[mood]?.let { return it }
        val res = app?.resources ?: return null
        val id = when (mood) {
            Mood.AWAKE -> R.drawable.mochi_awake
            Mood.PLEASED -> R.drawable.mochi_pleased
            Mood.RESTING -> R.drawable.mochi_resting
            Mood.LET_DOWN -> R.drawable.mochi_let_down
        }
        val art = runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
            ?: return null
        // Each mood is drawn at several sizes; without this they would share
        // one Drawable state and fight over the bounds.
        val own = art.mutate()
        cache[mood] = own
        return own
    }

    /** The mood the app should show given today's state and the running streak. */
    fun moodFor(doneToday: Boolean, streak: Int, missedYesterday: Boolean): Mood = when {
        doneToday -> Mood.PLEASED
        missedYesterday && streak == 0 -> Mood.LET_DOWN
        else -> Mood.AWAKE
    }
}
