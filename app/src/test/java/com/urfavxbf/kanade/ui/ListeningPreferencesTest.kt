package com.urfavxbf.kanade.ui

import org.junit.Assert.*
import org.junit.Test

class ListeningPreferencesTest {
    @Test fun nestedFolderExclusionsWinAndRespectPathBoundaries() {
        val include = setOf("Music/")
        val exclude = setOf("Music/Recordings")
        assertTrue(folderVisible("Music/OPM/", include, exclude))
        assertFalse(folderVisible("Music/Recordings/Work/", include, exclude))
        assertFalse(folderVisible("Music2/", include, exclude))
        assertTrue(folderVisible("Download/", emptySet(), emptySet()))
    }

    @Test fun adjustedSeekReturnsToTheSameLyric() {
        for (delay in listOf(-500L, 0L, 1000L)) {
            assertEquals(3000L, lyricClock(lyricSeek(3000L, delay), delay))
        }
        assertEquals(0L, lyricSeek(100L, -500L))
    }
}
