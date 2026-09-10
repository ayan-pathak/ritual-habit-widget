package com.ayan.ritual.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.time.LocalDate

/**
 * The 1080x1920 image that goes to an Instagram story.
 *
 * It has to survive being seen for two seconds at thumb size, so it carries
 * one number, one grid, and the cat — nothing else. Everything is sized off
 * the canvas width, so the same code makes a square post if we ever need one.
 */
object ShareCardRenderer {

    const val STORY_W = 1080
    const val STORY_H = 1920

    fun render(model: SlabModel, width: Int = STORY_W, height: Int = STORY_H): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), width.toFloat(), height.toFloat(), model)
        return bmp
    }

    private fun draw(canvas: Canvas, w: Float, h: Float, model: SlabModel) {
        val u = w / 1080f
        fun s(v: Float) = v * u
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val accent = model.accent

        canvas.drawColor(Palette.CREAM)

        // A single colour block anchors the composition; the story's safe area
        // keeps clear of Instagram's own chrome top and bottom.
        val margin = s(84f)
        val blockTop = h * 0.235f
        val block = RectF(margin, blockTop, w - margin, blockTop + s(990f))
        p.color = accent.block
        canvas.drawRoundRect(block, s(64f), s(64f), p)

        val onBlock = accent.onBlock
        val pad = s(64f)

        // ── Ritual name ─────────────────────────────────────────────────────
        val slot = SlabRenderer.paint(s(30f), onBlock.withAlpha(160), Fonts.semiBold(), 0.10f)
        canvas.drawText(model.slot.uppercase(), block.left + pad, block.top + pad + s(26f), slot)

        val namePaint = SlabRenderer.paint(s(76f), onBlock, Fonts.extraBold(), -0.03f)
        val name = SlabRenderer.fit(namePaint, model.title, block.width() - pad * 2)
        canvas.drawText(name, block.left + pad, block.top + pad + s(110f), namePaint)

        // ── The number that matters ─────────────────────────────────────────
        val bigPaint = SlabRenderer.paint(s(300f), onBlock, Fonts.extraBold(), -0.05f)
        canvas.drawText(model.streak.toString(), block.left + pad - s(8f), block.top + s(430f), bigPaint)

        val unit = SlabRenderer.paint(s(38f), onBlock.withAlpha(190), Fonts.extraBold(), 0.02f)
        canvas.drawText(
            if (model.streak == 1) "DAY IN A ROW" else "DAYS IN A ROW",
            block.left + pad, block.top + s(492f), unit
        )

        // ── Mochi ───────────────────────────────────────────────────────────
        val catH = s(176f)
        val catW = Cat.widthFor(catH)
        val catBox = RectF(
            block.right - pad - catW - s(38f),
            block.top + s(250f),
            block.right - pad + s(2f),
            block.top + s(250f) + catH + s(38f)
        )
        p.color = if (model.doneToday) Palette.INK else Palette.PAPER
        canvas.drawRoundRect(catBox, s(34f), s(34f), p)
        Cat.draw(canvas, catBox.centerX() - catW / 2f, catBox.centerY() - catH / 2f, catH, model.mood)

        // ── The year ────────────────────────────────────────────────────────
        val cols = GridGeo.colsFor(model.year)
        val gapRatio = 0.30f
        val gridW = block.width() - pad * 2
        val cell = gridW / (cols + gapRatio * (cols - 1))
        val gap = cell * gapRatio
        val gridTop = block.top + s(620f)
        val startOff = GridGeo.startOffset(model.year)
        val yearLen = GridGeo.lengthOf(model.year)
        val todayDoy = if (model.today.year == model.year) model.today.dayOfYear else -1

        val r = cell * 0.30f
        val rect = RectF()
        for (col in 0 until cols) {
            for (row in 0 until GridGeo.ROWS) {
                val doy = col * GridGeo.ROWS + row - startOff + 1
                if (doy < 1 || doy > yearLen) continue
                val l = block.left + pad + col * (cell + gap)
                val t = gridTop + row * (cell + gap)
                rect.set(l, t, l + cell, t + cell)
                p.color = when {
                    model.doneDaysOfYear.contains(doy) -> Palette.INK
                    todayDoy > 0 && doy > todayDoy -> onBlock.withAlpha(23)
                    else -> onBlock.withAlpha(52)
                }
                canvas.drawRoundRect(rect, r, r, p)
            }
        }

        val gridBottom = gridTop + GridGeo.ROWS * cell + (GridGeo.ROWS - 1) * gap

        // ── Tally ───────────────────────────────────────────────────────────
        val rule = Paint().apply { color = onBlock.withAlpha(46) }
        canvas.drawRect(
            block.left + pad, gridBottom + s(56f),
            block.right - pad, gridBottom + s(56f) + s(4f), rule
        )

        val tallyY = gridBottom + s(150f)
        val figure = SlabRenderer.paint(s(64f), onBlock, Fonts.extraBold(), -0.02f)
        val caption = SlabRenderer.paint(s(26f), onBlock.withAlpha(160), Fonts.semiBold(), 0.10f)

        canvas.drawText(model.totalDone.toString(), block.left + pad, tallyY, figure)
        canvas.drawText("LIT IN ${model.year}", block.left + pad, tallyY + s(42f), caption)

        val midX = block.left + block.width() * 0.5f
        canvas.drawText(model.remaining.toString(), midX, tallyY, figure)
        canvas.drawText("SQUARES LEFT", midX, tallyY + s(42f), caption)

        // ── Wordmark ────────────────────────────────────────────────────────
        val mark = SlabRenderer.paint(s(34f), Palette.INK, Fonts.extraBold(), -0.01f).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("RITUAL", w / 2f, block.bottom + s(110f), mark)

        val sub = SlabRenderer.paint(s(24f), Palette.INK.withAlpha(130), Fonts.semiBold(), 0.14f).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ONE SQUARE A DAY", w / 2f, block.bottom + s(152f), sub)
    }
}
