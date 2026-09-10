package com.ayan.ritual.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.time.LocalDate

/** Everything the renderer needs, with no dependency on how it was stored. */
data class SlabModel(
    val title: String,
    val slot: String,
    val accent: Accent,
    val year: Int,
    val today: LocalDate,
    val doneDaysOfYear: Set<Int>,
    val streak: Int,
    val totalDone: Int,
    val remaining: Int,
    val doneToday: Boolean,
    val mood: Mood = Mood.AWAKE
)

/**
 * Draws the card: a flat colour block carrying one year of days.
 *
 * The same routine backs the home-screen widget and every card inside the app,
 * so the two can never drift apart. Marking today inverts the block to ink and
 * the grid to the ritual's colour — that flip is the whole visual reward.
 */
object SlabRenderer {

    data class Config(
        val density: Float,
        val header: Boolean = true,
        val footer: Boolean = true,
        val action: Boolean = false,
        val cat: Boolean = true,
        val quarterRuler: Boolean = false,
        val cornerDp: Float = 24f,
        val padDp: Float = 17f,
        val fillBackground: Boolean = true
    )

    const val ACTION_HEIGHT_DP = 34f

    fun render(widthPx: Int, heightPx: Int, model: SlabModel, cfg: Config): Bitmap {
        val bmp = Bitmap.createBitmap(
            widthPx.coerceAtLeast(1),
            heightPx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        draw(Canvas(bmp), widthPx.toFloat(), heightPx.toFloat(), model, cfg)
        return bmp
    }

    fun draw(canvas: Canvas, w: Float, h: Float, model: SlabModel, cfg: Config) {
        val d = cfg.density
        fun dp(v: Float) = v * d

        val pad = dp(cfg.padDp)
        val corner = dp(cfg.cornerDp)
        val card = RectF(0f, 0f, w, h)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Marked days flip the card to ink and light the grid in the ritual's
        // colour, so a glance across the home screen says what is still open.
        val lit = model.doneToday
        val block = if (lit) Palette.INK else model.accent.block
        val onBlock = if (lit) Palette.PAPER else model.accent.onBlock
        val markInk = if (lit) model.accent.block else Palette.INK
        val missed = onBlock.withAlpha(52)
        val future = onBlock.withAlpha(23)

        if (cfg.fillBackground) {
            p.color = block
            canvas.drawRoundRect(card, corner, corner, p)
        }

        // ── Layout ──────────────────────────────────────────────────────────
        val headerH = if (cfg.header) dp(38f) else 0f
        val footerH = if (cfg.footer) dp(22f) else 0f
        val actionH = if (cfg.action) dp(ACTION_HEIGHT_DP) else 0f

        var bottom = h - pad
        if (cfg.action || cfg.footer) {
            bottom -= maxOf(actionH, footerH)
        }

        val gridTopBound = pad + headerH + dp(6f)
        val gridBottomBound = (bottom - dp(10f)).coerceAtLeast(gridTopBound + dp(14f))

        val cols = GridGeo.colsFor(model.year)
        val gapRatio = 0.30f
        val innerW = w - 2f * pad
        var cell = innerW / (cols + gapRatio * (cols - 1))
        var gap = cell * gapRatio
        var gridH = GridGeo.ROWS * cell + (GridGeo.ROWS - 1) * gap

        val zoneH = gridBottomBound - gridTopBound
        if (gridH > zoneH) {
            cell = zoneH / (GridGeo.ROWS + gapRatio * (GridGeo.ROWS - 1))
            gap = cell * gapRatio
            gridH = GridGeo.ROWS * cell + (GridGeo.ROWS - 1) * gap
        }
        val gridW = cols * cell + (cols - 1) * gap
        val gridLeft = pad
        val gridTop = gridTopBound + ((zoneH - gridH) / 2f).coerceAtLeast(0f)

        val yearLen = GridGeo.lengthOf(model.year)
        val startOff = GridGeo.startOffset(model.year)
        val isCurrentYear = model.today.year == model.year
        val todayDoy = if (isCurrentYear) model.today.dayOfYear else -1

        // ── Header ──────────────────────────────────────────────────────────
        if (cfg.header) {
            var textRight = w - pad

            if (cfg.cat) {
                val px = dp(0.95f)
                val catW = Cat.widthFor(px)
                val catH = Cat.heightFor(px)
                val boxW = catW + dp(11f)
                val boxH = catH + dp(9f)
                val boxL = w - pad - boxW
                val box = RectF(boxL, pad - dp(2f), boxL + boxW, pad - dp(2f) + boxH)
                p.color = if (lit) model.accent.block else Palette.INK
                canvas.drawRoundRect(box, dp(10f), dp(10f), p)
                Cat.draw(
                    canvas,
                    box.centerX() - catW / 2f,
                    box.centerY() - catH / 2f,
                    px,
                    model.mood
                )
                textRight = boxL - dp(10f)
            } else {
                // No cat: the streak takes the corner instead.
                val num = paint(dp(17f), onBlock, Fonts.extraBold(), -0.02f).apply {
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(model.streak.toString(), w - pad, pad + dp(15f), num)
                val cap = paint(dp(8f), onBlock.withAlpha(150), Fonts.semiBold(), 0.10f).apply {
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("DAY STREAK", w - pad, pad + dp(26f), cap)
                textRight = w - pad - dp(58f)
            }

            val slotPaint = paint(dp(8.5f), onBlock.withAlpha(150), Fonts.semiBold(), 0.10f)
            canvas.drawText(model.slot.uppercase(), pad, pad + dp(8f), slotPaint)

            val namePaint = paint(dp(17f), onBlock, Fonts.extraBold(), -0.02f)
            val name = fit(namePaint, model.title, (textRight - pad).coerceAtLeast(dp(30f)))
            canvas.drawText(name, pad, pad + dp(27f), namePaint)

            if (cfg.cat) {
                val streakPaint = paint(dp(8.5f), onBlock.withAlpha(170), Fonts.semiBold(), 0.10f)
                canvas.drawText(
                    "${model.streak} DAY STREAK",
                    pad, pad + dp(37f), streakPaint
                )
            }
        }

        // ── Quarter ruler ───────────────────────────────────────────────────
        if (cfg.quarterRuler) {
            val rp = paint(dp(8f), onBlock.withAlpha(140), Fonts.semiBold(), 0.10f)
            val marks = listOf(1 to "JAN", 4 to "APR", 7 to "JUL", 10 to "OCT")
            for ((month, label) in marks) {
                val col = GridGeo.colOf(LocalDate.of(model.year, month, 1))
                canvas.drawText(label, gridLeft + col * (cell + gap), gridTop - dp(5f), rp)
            }
        }

        // ── The field ───────────────────────────────────────────────────────
        val r = cell * 0.30f
        val rect = RectF()
        for (col in 0 until cols) {
            for (row in 0 until GridGeo.ROWS) {
                val doy = col * GridGeo.ROWS + row - startOff + 1
                if (doy < 1 || doy > yearLen) continue
                val l = gridLeft + col * (cell + gap)
                val t = gridTop + row * (cell + gap)
                rect.set(l, t, l + cell, t + cell)

                p.color = when {
                    model.doneDaysOfYear.contains(doy) -> markInk
                    isCurrentYear && doy > todayDoy -> future
                    else -> missed
                }
                canvas.drawRoundRect(rect, r, r, p)

                if (doy == todayDoy) {
                    // Today wears a ring whether or not it is filled.
                    p.color = markInk
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = maxOf(dp(1.2f), 1.3f)
                    val ring = RectF(rect).apply { inset(-dp(1.3f), -dp(1.3f)) }
                    canvas.drawRoundRect(ring, r * 1.7f, r * 1.7f, p)
                    p.style = Paint.Style.FILL
                }
            }
        }

        // ── Footer ──────────────────────────────────────────────────────────
        if (cfg.footer || cfg.action) {
            val baseY = h - pad - dp(4f)

            val litPaint = paint(dp(12f), onBlock, Fonts.extraBold(), 0f)
            val litText = "${model.totalDone} lit"
            canvas.drawText(litText, pad, baseY, litPaint)
            var x = pad + litPaint.measureText(litText) + dp(9f)

            p.color = onBlock.withAlpha(90)
            canvas.drawCircle(x + dp(2f), baseY - dp(3.5f), dp(2f), p)
            x += dp(13f)

            val leftPaint = paint(dp(12f), onBlock.withAlpha(160), Fonts.semiBold(), 0f)
            canvas.drawText("${model.remaining} to go", x, baseY, leftPaint)

            if (cfg.action) {
                drawAction(canvas, w, h, pad, model, lit, d)
            }
        }
    }

    /** The mark-today control. The widget lays a transparent hit target over it. */
    private fun drawAction(
        canvas: Canvas, w: Float, h: Float, pad: Float,
        model: SlabModel, lit: Boolean, d: Float
    ) {
        fun dp(v: Float) = v * d
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val hgt = dp(ACTION_HEIGHT_DP)
        val label = if (lit) "Done" else "Mark"
        val labelPaint = paint(dp(11.5f), if (lit) Palette.INK else Palette.PAPER, Fonts.extraBold(), 0f)
        val checkW = if (lit) dp(15f) else 0f
        val wide = labelPaint.measureText(label) + dp(28f) + checkW
        val right = w - pad
        val r = RectF(right - wide, h - pad - hgt, right, h - pad)

        p.color = if (lit) model.accent.block else Palette.INK
        canvas.drawRoundRect(r, hgt / 2f, hgt / 2f, p)

        var textX = r.centerX() - labelPaint.measureText(label) / 2f + checkW / 2f
        canvas.drawText(label, textX, r.centerY() + dp(4f), labelPaint)

        if (lit) {
            // Hand-drawn check: no font dependency, crisp at any size.
            val cx = textX - dp(9f)
            val cy = r.centerY()
            val s = dp(3.6f)
            p.color = Palette.INK
            p.style = Paint.Style.STROKE
            p.strokeWidth = maxOf(dp(1.9f), 2f)
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            val path = android.graphics.Path().apply {
                moveTo(cx - s, cy + s * 0.05f)
                lineTo(cx - s * 0.15f, cy + s * 0.72f)
                lineTo(cx + s, cy - s * 0.72f)
            }
            canvas.drawPath(path, p)
            p.style = Paint.Style.FILL
        }
    }

    fun paint(size: Float, color: Int, face: Typeface, tracking: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = face
            letterSpacing = tracking
            isSubpixelText = true
        }

    /** Truncates [text] to [maxWidth], adding an ellipsis only when it overflows. */
    fun fit(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end.coerceAtLeast(1)) + "…"
    }
}
