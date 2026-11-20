package com.urfavxbf.kanade.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SongTitleTest {
    @Test fun trimsVideoLabelsAndMatchingArtistOnly() {
        assertEquals("BATUGAN", songTitle("BATUGAN – FLOW G (OFFICIAL MUSIC VIDEO)", "Flow G"))
        assertEquals("BATUGAN", songTitle("Flow G - BATUGAN [Official Audio]", "Flow G - Topic"))
        assertEquals("Song - Live (Acoustic)", songTitle("Song - Live (Acoustic)", "Band"))
        assertEquals("A - B", songTitle("A - B", "Label Channel"))
    }
}
