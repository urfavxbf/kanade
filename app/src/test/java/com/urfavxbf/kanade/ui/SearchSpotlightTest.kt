package com.urfavxbf.kanade.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchSpotlightTest {
    @Test fun exactArtistBeatsRelatedSongs() {
        assertEquals(SearchSpotlight(ResultKind.ARTIST, 1),
            searchSpotlight(" bini ", listOf("BINI Medley"), listOf("BINI Fans", "BINI"), emptyList()))
    }

    @Test fun exactAlbumOpensEvenWithoutTracks() {
        assertEquals(SearchSpotlight(ResultKind.ALBUM, 0),
            searchSpotlight("Talaarawan", emptyList(), emptyList(), listOf("Talaarawan")))
    }

    @Test fun equalMatchesKeepProviderOrderAndPreferTracks() {
        assertEquals(SearchSpotlight(ResultKind.TRACK, 1),
            searchSpotlight("Blue", listOf("Dark Blue", "Blue", "Blue"), emptyList(), listOf("Blue")))
    }

    @Test fun emptySearchHasNoSpotlight() {
        assertNull(searchSpotlight("  ", listOf("Song"), emptyList(), emptyList()))
        assertNull(searchSpotlight("Song", emptyList(), emptyList(), emptyList()))
    }
}
