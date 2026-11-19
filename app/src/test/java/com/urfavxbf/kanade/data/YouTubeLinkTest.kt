package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeLinkTest {
    private val id = "a_b-c012345"

    @Test fun supportsSharedAndMusicLinks() {
        for (link in listOf("https://youtu.be/$id?t=8", "https://music.youtube.com/watch?list=1&v=$id",
            "https://www.youtube.com/shorts/$id", "https://youtube.com/live/$id")) {
            assertEquals(id, YouTubeLink.videoId(link))
        }
    }

    @Test fun rejectsForeignHostsAndInvalidIds() {
        for (link in listOf("https://youtube.com.example/watch?v=$id", "https://youtu.be/a/b",
            "https://youtube.com@bad.example/watch?v=$id", "https://youtube.com/watch?v=%22", "pinoy rock")) {
            assertNull(YouTubeLink.videoId(link))
        }
    }
}
