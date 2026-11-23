package com.urfavxbf.kanade.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MusicBrainzArtworkTest {
    private val group = "ab123456-1111-2222-3333-444444444444"
    private val release = "cd123456-1111-2222-3333-444444444444"
    private val front = "https://coverartarchive.org"
    private val album = """{"id":"$release","title":"Talaarawan","status":"Official",
        "artist-credit":[{"artist":{"name":"BINI"}}],"release-group":{"id":"$group"}}"""

    @Test fun tracksFromTheSameAlbumShareTheLookup() {
        assertEquals(CoverQuery.forMusic("BINI", "Talaarawan", "Salamin, Salamin"),
            CoverQuery.forMusic("BINI - Topic", "Talaarawan", "Pantropiko"))
    }

    @Test fun missingMetadataDoesNotTriggerABroadSearch() {
        assertNull(CoverQuery.forMusic("Unknown artist", "Talaarawan"))
        assertNull(CoverQuery.forMusic("BINI", "Unknown album"))
        assertEquals(CoverQuery("BINI", "Pantropiko", false), CoverQuery.forMusic("BINI", "<unknown>", "Pantropiko"))
    }

    @Test fun albumLooksUpGroupThenEditionIgnoringCaseAndDuplicateHits() {
        val query = CoverQuery("bini", "TALAARAWAN", true)
        val response = JSONObject("""{"releases":[$album,$album]}""")
        assertEquals(listOf("$front/release-group/$group/front-500", "$front/release/$release/front-500"),
            MusicBrainzArtwork.covers(query, response))
        assertTrue(MusicBrainzArtwork.covers(query.copy(artist = "Other artist"), response).isEmpty())
    }

    @Test fun recordingRejectsWrongVersionAndCompilation() {
        val query = CoverQuery("BINI", "Pantropiko", false)
        val recording = """{"title":"Pantropiko","artist-credit":[{"name":"BINI"}],"releases":[$album]}"""
        fun lookup(item: String) = MusicBrainzArtwork.covers(query, JSONObject("""{"recordings":[$item]}"""))
        assertEquals(2, lookup(recording).size)
        assertTrue(lookup(recording.replace("Pantropiko", "Pantropiko (Live)")).isEmpty())
        assertTrue(lookup(recording.replace("\"id\":\"$group\"", """"id":"$group","secondary-types":["Compilation"]""")).isEmpty())
    }

    @Test fun invalidIdentifiersAndEmptyResultsKeepPlaceholder() {
        val query = CoverQuery("BINI", "Talaarawan", true)
        assertTrue(MusicBrainzArtwork.covers(query, JSONObject()).isEmpty())
        assertTrue(MusicBrainzArtwork.covers(query,
            JSONObject("""{"releases":[$album]}""".replace(group, "../bad").replace(release, "bad"))).isEmpty())
    }
}
