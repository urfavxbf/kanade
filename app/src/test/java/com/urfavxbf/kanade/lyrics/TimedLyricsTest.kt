package com.urfavxbf.kanade.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class TimedLyricsTest {
    @Test fun repeatedEnhancedLrcUsesRelativeOffsetsAndPreservesSpaces() {
        val result = LrcParser.parse("[offset:-100]\n[00:01.00][00:11.00]<00:01.00>Hold <00:01.50>on")
        assertEquals(listOf("Hold on", "Hold on"), result.map { it.text })
        assertEquals(listOf(900L, 1400L), result.first().segments.map { it.timeMs })
        assertEquals(listOf(10900L, 11400L), result.last().segments.map { it.timeMs })
        assertEquals(listOf("Hold ", "on"), result.first().let { line ->
            line.segments.map { line.text.substring(it.start, it.end) }
        })
    }

    @Test fun videoCuesUseTheirOwnClockAndRespectSilence() {
        val cues = VideoCaptions.parse("WEBVTT\n\n00:01.000 --> 00:03.500 align:start\n<c>Sing</c> <00:02.100>along\n\n00:00:05.000 --> 00:00:08.000\nAgain")
        assertEquals("Sing along", cues.first().text)
        assertEquals(listOf(1000L, 2100L), cues.first().segments.map { it.timeMs })
        assertEquals(-1, LrcParser.current(cues, 3500))
        assertEquals(1, LrcParser.current(cues, 5100))
        assertEquals(emptyList<LyricSegment>(), cues.last().segments)
    }
}
