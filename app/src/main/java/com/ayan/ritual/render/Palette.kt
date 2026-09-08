package com.ayan.ritual.render

/**
 * Flat, warm, high-contrast. No gradients, no glow, no shadow.
 *
 * A kept day is solid ink on a colour block; a missed one is a wash of that
 * same ink. Nothing emits, so a dense year stays legible where a glowing grid
 * smears into blobs.
 */
object Palette {
    val CREAM = 0xFFE7E3D4.toInt()
    val PAPER = 0xFFF4F2EA.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val INK = 0xFF12120F.toInt()

    /** Ink at the weights the grid uses. */
    val INK_MISSED = 0x3312120F
    val INK_FUTURE = 0x1712120F
    val INK_RULE = 0x2E12120F
    val INK_SOFT = 0x8F12120F
}

/**
 * A ritual's colour. [block] is the card, [onBlock] everything drawn on it.
 * [inverted] is the same colour used as ink once the day is marked and the
 * card flips to black.
 */
data class Accent(
    val name: String,
    val block: Int,
    val onBlock: Int = Palette.INK
)

val ACCENTS: List<Accent> = listOf(
    Accent("Lime", 0xFFC9F73F.toInt()),
    Accent("Red", 0xFFE5331C.toInt(), Palette.PAPER),
    Accent("Sage", 0xFF9DB8A4.toInt()),
    Accent("Camel", 0xFFBC9F76.toInt()),
    Accent("Butter", 0xFFEDE55C.toInt()),
    Accent("Orange", 0xFFF26A1B.toInt())
)

fun accentAt(index: Int): Accent = ACCENTS[((index % ACCENTS.size) + ACCENTS.size) % ACCENTS.size]

/** Applies an alpha (0..255) to a packed ARGB colour, preserving its RGB. */
fun Int.withAlpha(alpha: Int): Int =
    (this and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

/** Linear blend between two packed ARGB colours. */
fun blend(a: Int, b: Int, t: Float): Int {
    val f = t.coerceIn(0f, 1f)
    fun ch(shift: Int): Int {
        val av = (a shr shift) and 0xFF
        val bv = (b shr shift) and 0xFF
        return (av + (bv - av) * f).toInt().coerceIn(0, 255)
    }
    return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
