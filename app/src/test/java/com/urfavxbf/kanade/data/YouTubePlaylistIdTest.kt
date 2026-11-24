package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubePlaylistIdTest {
    @Test fun acceptsAlbumPlaylistLinks() {
        assertEquals("OLAK5uy_test-123", YouTubePlaylistId.fromUrl("https://music.youtube.com/playlist?list=OLAK5uy_test-123"))
        assertEquals("PL123", YouTubePlaylistId.fromUrl("https://www.youtube.com/playlist?x=1&list=PL123"))
    }
    @Test fun rejectsUntrustedOrAmbiguousLinks() {
        assertNull(YouTubePlaylistId.fromUrl("https://youtube.com.evil.example/playlist?list=PL123"))
        assertNull(YouTubePlaylistId.fromUrl("https://youtube.com/playlist?list=PL123&list=OTHER"))
        assertNull(YouTubePlaylistId.fromUrl("https://youtube.com/playlist?list=../../other"))
        assertNull(YouTubePlaylistId.fromUrl("http://youtube.com/playlist?list=PL123"))
    }
}
