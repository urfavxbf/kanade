package com.urfavxbf.kanade.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp3FormatTest {
    @Test fun acceptsMp3RegardlessOfCaseAndHandlesMissingMetadata() {
        assertTrue(isMp3("Song.MP3", "audio/mpeg"))
        assertTrue(isMp3("Song.mp3", null))
        assertTrue(isMp3("Song", "audio/mp3"))
    }

    @Test fun excludesOtherFormatsAndConflictingMetadata() {
        assertFalse(isMp3("Song.mp2", "audio/mpeg"))
        assertFalse(isMp3("Song.m4a", "audio/mp4"))
        assertFalse(isMp3("Song.mp3", "audio/flac"))
        assertFalse(isMp3("Song", null))
    }
}
