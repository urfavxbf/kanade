package com.urfavxbf.kanade.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumSmoothingTest {
    @Test fun `rising levels stay bounded and interpolate smoothly`() {
        val next = smoothSpectrum(floatArrayOf(0f, 0.2f), floatArrayOf(1f, 0.8f), 1f / 60f)
        assertTrue(next[0] > 0f && next[0] < 1f)
        assertTrue(next[1] > 0.2f && next[1] < 0.8f)
    }

    @Test fun `elapsed time produces equivalent results across frame rates`() {
        var fast = floatArrayOf(0f)
        var slow = floatArrayOf(0f)
        repeat(60) { fast = smoothSpectrum(fast, floatArrayOf(1f), 1f / 60f) }
        repeat(30) { slow = smoothSpectrum(slow, floatArrayOf(1f), 1f / 30f) }
        assertEquals(slow[0], fast[0], 0.0001f)
    }

    @Test fun `silence decays and missing bands safely approach zero`() {
        val next = smoothSpectrum(floatArrayOf(1f), floatArrayOf(), 1f / 60f)
        assertTrue(next[0] > 0f && next[0] < 1f)
        assertEquals(1f, smoothSpectrum(floatArrayOf(1f), floatArrayOf(0f), 0f)[0], 0f)
    }
}
