package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.Page

class SearchPaginationTest {
    private fun track(id: String) = Track(id = id, title = id, artist = "artist", uri = id, source = TrackSource.YOUTUBE)

    @Test fun appendsNewResultsWithoutDuplicatesAndKeepsOrder() {
        val existing = listOf(track("a"), track("b"))
        val incoming = listOf(track("b"), track("c"), track("c"))
        assertEquals(listOf("a", "b", "c"), SearchPagination.merge(existing, incoming).map { it.id })
    }

    @Test fun identifiesRepeatedCursorWithoutDiscardingDifferentTokens() {
        val page = Page("https://example.com/search", "token-a", byteArrayOf(1, 2))
        assertTrue(SearchPagination.sameCursor(page, Page("https://example.com/search", "token-a", byteArrayOf(1, 2))))
        assertFalse(SearchPagination.sameCursor(page, Page("https://example.com/search", "token-b", byteArrayOf(1, 2))))
        assertFalse(SearchPagination.sameCursor(page, null))
    }
}
