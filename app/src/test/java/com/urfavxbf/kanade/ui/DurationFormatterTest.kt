package com.urfavxbf.kanade.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {
    @Test fun zeroAndNegative() {
        assertEquals("0:00", DurationFormatter.format(0))
        assertEquals("0:00", DurationFormatter.format(-3_000))
    }
    @Test fun minuteAndHour() {
        assertEquals("3:07", DurationFormatter.format(187_000))
        assertEquals("1:02:03", DurationFormatter.format(3_723_000))
    }
}
