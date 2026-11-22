package com.urfavxbf.kanade.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun accentsRemainReadableAcrossThemesAndSurfaces() {
        for (name in colorThemes.keys.filterNot { it.startsWith("Plain") }) {
            for (dark in listOf(false, true)) {
                val palette = paletteFor(name, dark)
                for (background in listOf(palette.background, palette.backgroundTop,
                    palette.backgroundBottom, palette.surface, palette.glass)) {
                    val contrast = contrastRatio(palette.accent, background)
                    assertTrue("$name dark=$dark contrast=$contrast", contrast >= 4.49f)
                }
            }
        }
    }
}
