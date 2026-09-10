package com.ayan.ritual.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.ayan.ritual.R

/** What Mochi is doing, derived from the streak — never chosen for decoration. */
enum class Mood { AWAKE, PLEASED, RESTING, LET_DOWN }

/**
 * Mochi, four drawn portraits.
 *
 * The four moods are one illustration with a different face, exported at a
 * single registration: the head sits on the same pixels in every one, so the
 * silhouette never shifts and he never looks redrawn between moods. That was
 * the rule when he was a 20x18 grid and it is still the rule now.
 *
 * They ship as bitmaps rather than as a vector, and the reason is the
 * architectural one: the widget can only be handed a `Bitmap` through
 * `RemoteViews.setImageViewBitmap`, so the app and the widget can only draw
 * the same pixels if the source *is* pixels. One PNG per mood, drawn into
 * whatever Canvas asks for it, is the same art on both surfaces by
 * construction — and the same art iOS draws, from the same four files.
 *
 * [load] must run before [draw], like [Fonts.load] — the widget receiver can
 * start the process cold.
 */
object Cat {

    /** The exported art, in pixels. Every mood is this size and registered alike. */
    const val ART_W = 480
    const val ART_H = 496

    /** Width over height, so a caller can size him from either one. */
    const val ASPECT = ART_W.toFloat() / ART_H.toFloat()

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val cache = HashMap<Mood, Bitmap>(4)
    private var app: Context? = null

    fun load(context: Context) {
        app = context.applicationContext
    }

    /** Width Mochi occupies when he is drawn [height] tall. */
    fun widthFor(height: Float) = height * ASPECT

    /**
     * Draws Mochi into the box whose top-left is ([left], [top]) and whose
     * height is [height]. A missing bitmap draws nothing rather than taking
     * the widget down with it.
     */
    fun draw(canvas: Canvas, left: Float, top: Float, height: Float, mood: Mood) {
        val bmp = bitmap(mood) ?: return
        canvas.drawBitmap(
            bmp, null,
            RectF(left, top, left + widthFor(height), top + height),
            paint
        )
    }

    private fun bitmap(mood: Mood): Bitmap? {
        cache[mood]?.let { return it }
        val res = app?.resources ?: return null
        val id = when (mood) {
            Mood.AWAKE -> R.drawable.mochi_awake
            Mood.PLEASED -> R.drawable.mochi_pleased
            Mood.RESTING -> R.drawable.mochi_resting
            Mood.LET_DOWN -> R.drawable.mochi_let_down
        }
        val bmp = runCatching { BitmapFactory.decodeResource(res, id) }.getOrNull() ?: return null
        cache[mood] = bmp
        return bmp
    }

    /** The mood the app should show given today's state and the running streak. */
    fun moodFor(doneToday: Boolean, streak: Int, missedYesterday: Boolean): Mood = when {
        doneToday -> Mood.PLEASED
        missedYesterday && streak == 0 -> Mood.LET_DOWN
        else -> Mood.AWAKE
    }
}
