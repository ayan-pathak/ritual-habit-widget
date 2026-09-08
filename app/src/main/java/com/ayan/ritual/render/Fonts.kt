package com.ayan.ritual.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.ayan.ritual.R

/**
 * Archivo, in the three weights the design uses.
 *
 * One variable TTF ships in the APK; the font-family XMLs pin the `wght` axis
 * so the Canvas renderer and Compose draw the very same instances.
 */
object Fonts {

    private var extraBold: Typeface? = null
    private var semiBold: Typeface? = null
    private var medium: Typeface? = null

    fun load(context: Context) {
        if (extraBold != null) return
        val app = context.applicationContext
        // A missing or unloadable font must never take the widget down with it.
        extraBold = runCatching { ResourcesCompat.getFont(app, R.font.archivo_extrabold) }
            .getOrNull() ?: Typeface.create("sans-serif-black", Typeface.NORMAL)
        semiBold = runCatching { ResourcesCompat.getFont(app, R.font.archivo_semibold) }
            .getOrNull() ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        medium = runCatching { ResourcesCompat.getFont(app, R.font.archivo_medium) }
            .getOrNull() ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    fun extraBold(): Typeface = extraBold ?: Typeface.create("sans-serif-black", Typeface.NORMAL)
    fun semiBold(): Typeface = semiBold ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    fun medium(): Typeface = medium ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
}
