package com.urfavxbf.kanade.ui

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?, legacyDark: Boolean?): ThemeMode =
            entries.firstOrNull { it.name == value }
                ?: legacyDark?.let { if (it) DARK else LIGHT }
                ?: SYSTEM
    }
}
