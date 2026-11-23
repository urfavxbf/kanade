package com.urfavxbf.kanade.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMoodsTest {
    @Test fun `only actual supported mood categories with provider params are shown`() {
        val payload = JSONObject("""{"items":[
            {"musicNavigationButtonRenderer":{"buttonText":{"runs":[{"text":"Chill"}]},"clickCommand":{"browseEndpoint":{"params":"CHILL_TOKEN"}}}},
            {"musicNavigationButtonRenderer":{"buttonText":{"runs":[{"text":"Commute"}]},"clickCommand":{"browseEndpoint":{"params":"COMMUTE_TOKEN"}}}},
            {"musicNavigationButtonRenderer":{"buttonText":{"runs":[{"text":"Sad"}]},"clickCommand":{"browseEndpoint":{}}}}
        ]}""")
        assertEquals(listOf(MusicMoods.Mood("Chill", "CHILL_TOKEN")), MusicMoods.parseMoods(payload))
    }

    @Test fun `playlist browse items retain provider playlist id`() {
        val payload = JSONObject("""{"contents":[
            {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Calm tracks"}]},"navigationEndpoint":{"browseEndpoint":{"browseId":"VLPLabc_123"}}}},
            {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Not a playlist"}]},"navigationEndpoint":{"browseEndpoint":{"browseId":"UCabc123"}}}}
        ]}""")
        val playlists = MusicMoods.parsePlaylists(payload)
        assertEquals(1, playlists.size)
        assertEquals("Calm tracks", playlists.first().name)
        assertEquals("https://www.youtube.com/playlist?list=PLabc_123", playlists.first().url)
        assertTrue(MusicMoods.parsePlaylists(JSONObject()).isEmpty())
    }
}
