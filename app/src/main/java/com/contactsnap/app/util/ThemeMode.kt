package com.contactsnap.app.util

/** User-selectable app theme. */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        val DEFAULT = SYSTEM
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
