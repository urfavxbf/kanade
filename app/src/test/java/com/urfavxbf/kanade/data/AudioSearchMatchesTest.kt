package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSearchMatchesTest {
    private fun track(title: String, artist: String) =
        Track(title, title, artist, "https://example.com", source = TrackSource.AUDIUS)

    @Test fun prioritizesArtistCreditAndRejectsUnrelatedFuzzyHits() {
        val original = track("Praning", "Flow G")
        val collaboration = track("Live (feat. Flow G)", "Guest")
        val unrelated = track("Ñengo Flow x Bad Bunny", "Olazaran")
        val scattered = track("10 I Make the Flow ft. Fatt G", "gLife")
        val results = AudioSearchMatches.rank(" flow-g ", listOf(unrelated, scattered, collaboration, original))
        assertEquals(listOf(original, collaboration), results)
    }

    @Test fun supportsTitleAndArtistAndReturnsEmptyWhenMissing() {
        val hit = track("Música", "Artist")
        assertEquals(listOf(hit), AudioSearchMatches.rank("artist musica", listOf(hit)))
        assertEquals(emptyList<Track>(), AudioSearchMatches.rank("Mhot", listOf(track("2hot4u", "NUMERO13"))))
    }
}
