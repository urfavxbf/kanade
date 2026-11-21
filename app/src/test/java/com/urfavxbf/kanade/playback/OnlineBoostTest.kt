package com.urfavxbf.kanade.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineBoostTest {
    @Test fun boostOnlyAppliesToYoutubeAndIsBounded() {
        assertEquals(300, onlineBoostGain("youtube:abc", 300))
        assertEquals(600, onlineBoostGain("youtube:abc", 900))
        assertEquals(0, onlineBoostGain("youtube:abc", -1))
        assertEquals(0, onlineBoostGain("local:abc", 600))
        assertEquals(0, onlineBoostGain(null, 600))
    }
}
