package com.urfavxbf.kanade.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun systemFollowsDevice() {
        assertTrue(ThemeMode.SYSTEM.isDark(true))
        assertFalse(ThemeMode.SYSTEM.isDark(false))
    }

    @Test fun explicitModeOverridesDevice() {
        assertTrue(ThemeMode.DARK.isDark(false))
        assertFalse(ThemeMode.LIGHT.isDark(true))
    }

    @Test fun restoresOrMigratesPreferences() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null, null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored(null, true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored(null, false))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("SYSTEM", true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored("LIGHT", true))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK", false))
    }
}
