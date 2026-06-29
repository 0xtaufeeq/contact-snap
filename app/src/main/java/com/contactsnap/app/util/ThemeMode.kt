package com.contactsnap.app.util

/** User-selectable app theme. */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    AMOLED("amoled", "AMOLED (pure black)");

    companion object {
        val DEFAULT = DARK
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
