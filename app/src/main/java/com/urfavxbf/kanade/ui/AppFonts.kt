package com.urfavxbf.kanade.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.urfavxbf.kanade.R

internal const val DEFAULT_APP_FONT = "Poppins"

internal val appFonts = linkedMapOf(
    DEFAULT_APP_FONT to FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
        Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    ),
    "Kanade Sans" to FontFamily.SansSerif,
    "Open Sans" to FontFamily(Font(R.font.open_sans)),
    "System" to FontFamily.Default,
    "Serif" to FontFamily.Serif,
    "Monospace" to FontFamily.Monospace,
)

internal val LocalAppFont = staticCompositionLocalOf { appFonts.getValue(DEFAULT_APP_FONT) }
