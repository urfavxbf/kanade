package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeSearchUrlTest {
    @Test fun buildsPhilippinesMusicDiscoveryRequest() {
        val url = YouTubeSearchUrl.build("  Ben&Ben leaves ", "AIza_test")
        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/search?"))
        assertTrue(url.contains("regionCode=PH"))
        assertTrue(url.contains("videoCategoryId=10"))
        assertTrue(url.contains("type=video"))
        assertTrue(url.contains("q=Ben%26Ben+leaves"))
        assertTrue(url.contains("key=AIza_test"))
    }

    @Test fun onlyExplicitQueriesAreAllowed() {
        assertThrows(IllegalArgumentException::class.java) { YouTubeSearchUrl.build("", "key") }
        assertThrows(IllegalArgumentException::class.java) { YouTubeSearchUrl.build("OPM", "") }
        assertThrows(IllegalArgumentException::class.java) { YouTubeSearchUrl.build("A".repeat(121), "key") }
    }

    @Test fun encodesUnicodeQuery() {
        assertTrue(YouTubeSearchUrl.build("Música OPM", "key").contains("q=M%C3%BAsica+OPM"))
    }
}
