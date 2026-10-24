package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicCatalogLinksTest {
    @Test fun opensProviderCatalogWithEncodedOPMQuery() {
        assertEquals(
            "https://music.youtube.com/search?q=Ben%26Ben%20Leaves",
            MusicCatalogLinks.search(MusicCatalogProvider.YOUTUBE_MUSIC, "Ben&Ben Leaves"),
        )
        assertEquals(
            "https://open.spotify.com/search/Pinoy%20indie",
            MusicCatalogLinks.search(MusicCatalogProvider.SPOTIFY, "  Pinoy indie  "),
        )
    }

    @Test fun emptySearchDefaultsToPinoyMusic() {
        assertEquals(
            "https://music.youtube.com/search?q=OPM%20Filipino%20music",
            MusicCatalogLinks.search(MusicCatalogProvider.YOUTUBE_MUSIC, "   "),
        )
    }

    @Test fun unicodeArtistSearchIsEncoded() {
        val value = MusicCatalogLinks.search(MusicCatalogProvider.YOUTUBE_MUSIC, "Música OPM")
        assertEquals("https://music.youtube.com/search?q=M%C3%BAsica%20OPM", value)
    }
}
