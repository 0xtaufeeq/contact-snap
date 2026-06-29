package com.contactsnap.app.util

/** Phone-number normalization. */
object Phones {
    // Whitespace, ASCII hyphen, and the common Unicode hyphen/dash/minus variants.
    private val STRIP = Regex("[\\s\\-\\u2010-\\u2015\\u2212]")

    /** Removes spaces and hyphens, keeping digits, a leading +, and any other chars. */
    fun normalize(raw: String): String = raw.replace(STRIP, "").trim()
}
