package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTrackIdTest {
    @Test fun acceptsCanonicalVideoUrls() {
        assertEquals("dQw4w9WgXcQ", YouTubeTrackId.fromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTrackId.fromUrl("https://music.youtube.com/watch?x=1&v=dQw4w9WgXcQ"))
    }

    @Test fun rejectsInvalidIdsAndUntrustedHosts() {
        assertNull(YouTubeTrackId.fromUrl("https://evil.example/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubeTrackId.fromUrl("https://www.youtube.com/watch?v=../../test"))
    }
}
