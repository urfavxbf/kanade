package com.urfavxbf.kanade.ui

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SongRadioTest {
    private val seed = Track("seed", "First", "Band", "seed-uri", source = TrackSource.LOCAL)

    @Test fun seedsFirstAndRanksArtistBeforeUnrelatedMusic() {
        val same = seed.copy(id = "same", title = "Second")
        val other = seed.copy(id = "other", artist = "Another artist")
        assertEquals(listOf(seed, same, other), radioQueue(seed, listOf(other, same, same, seed), true))
    }

    @Test fun excludesRemoteTracksOfflineAndCapsTheQueue() {
        val remote = seed.copy(id = "remote", title = "Online", source = TrackSource.YOUTUBE)
        assertEquals(listOf(seed), radioQueue(seed, listOf(remote), false))
        val tracks = (1..40).map { seed.copy(id = it.toString(), title = "Song $it") }
        assertEquals(25, radioQueue(seed, tracks, true).size)
    }
}
