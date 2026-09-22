package com.ayan.ritual.data

/**
 * The bet: thirty days, of which twenty four must be kept.
 *
 * Thirty because it is close enough to believe in on the day you decide to
 * change, which is the only day anyone decides anything. Ninety was the first
 * number we tried and it is too far away to be a bet; it is a wish.
 *
 * Eighty per cent, said as six missed days, because that is the same fact in
 * a form a person can act on. Six is a budget you are spending. Eighty per
 * cent is a grade you are failing, and a habit app that grades people is the
 * app this one exists to be an alternative to.
 */
object Goal {
    const val DAYS = 30
    const val ALLOWED_MISSES = 6
    const val NEEDED = DAYS - ALLOWED_MISSES
}
