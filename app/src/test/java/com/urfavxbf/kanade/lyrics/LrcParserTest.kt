package com.urfavxbf.kanade.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test fun parsesTimesAndSorts() {
        val result = LrcParser.parse("[00:12.42]Second\n[00:01.5][00:05.75]First")
        assertEquals(listOf(1_500L, 5_750L, 12_420L), result.map { it.timeMs })
        assertEquals("First", result.first().text)
    }
    @Test fun handlesOffsetsAndBlankLines() {
        val lines = LrcParser.parse("[offset:-400]\n[00:00.20]Hi\n[00:59.99]Bye\n[03:90]Bad")
        assertEquals(2, lines.size)
        assertEquals(0L, lines.first().timeMs)
        assertEquals(59_590L, lines.last().timeMs)
    }
    @Test fun findsCurrentLine() {
        val lines = listOf(LyricLine(1000, "One"), LyricLine(3000, "Two"))
        assertEquals(-1, LrcParser.current(lines, 0))
        assertEquals(0, LrcParser.current(lines, 2000))
        assertEquals(1, LrcParser.current(lines, 8000))
    }
}
