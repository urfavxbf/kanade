package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistExchangeTest {
    @Test fun roundTripLocalUri() {
        val track = Track("local:1", "First", "Artist", "content://media/external/audio/media/1",
            source = TrackSource.LOCAL)
        val text = PlaylistExchange.export(listOf(track))
        assertEquals(listOf(track), PlaylistExchange.import(text, listOf(track)))
        assertEquals(emptyList<Track>(), PlaylistExchange.import("#EXTM3U\nfile:///missing", listOf(track)))
    }
}
