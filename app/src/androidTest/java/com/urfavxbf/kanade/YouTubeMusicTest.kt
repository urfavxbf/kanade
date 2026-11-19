package com.urfavxbf.kanade

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.urfavxbf.kanade.data.YouTubeMusicRepository
import com.urfavxbf.kanade.ui.KanadeViewModel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class YouTubeMusicTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun parsesSongCreditsAndOnlyAcceptsAvailableAudio() {
        val row = """{"playlistItemData":{"videoId":"ABCDEFGHIJK"},"flexColumns":[
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"}]}}},
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}},{"text":"3:25"}]}}}]}"""
        val json = JSONObject("""{"contents":{"sectionListRenderer":{"contents":[{"musicShelfRenderer":{"contents":[{"musicResponsiveListItemRenderer":$row}]}}]}}}""")
        val song = YouTubeMusicRepository.songs(json).single()
        assertEquals("Artist", song.artist)
        assertEquals("ytmusic://track/ABCDEFGHIJK", song.uri)
        assertEquals(205000L, song.durationMs)
        val url = "https://example.googlevideo.com/media"
        val response = JSONObject("""{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[{"url":"$url","mimeType":"audio/mp4","bitrate":128000}]}}""")
        assertEquals(url, YouTubeMusicRepository.streamUrl(response))
        response.getJSONObject("playabilityStatus").put("status","LOGIN_REQUIRED")
        assertNull(YouTubeMusicRepository.streamUrl(response))
    }

    @Test fun liveOpmSearchAndNativePlayback() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMusic") == "true")
        val matches = try {
            runBlocking { YouTubeMusicRepository.search("Flow G") }
        } catch (e: IOException) {
            Log.i("KanadeMusicProbe", "Live search unavailable: ${e.message?.take(100)}")
            assumeTrue("Live catalog unavailable", false)
            return
        }
        val track = matches.firstOrNull { it.artist.contains("Flow G", true) }
        Log.i("KanadeMusicProbe", "Search returned ${matches.size} songs. Matched credit: ${track?.artist}")
        assertNotNull("Live search did not find the OPM artist", track)
        val song = requireNotNull(track)
        try {
            YouTubeMusicRepository.resolve(song.id.removePrefix("ytmusic:"))
        } catch (e: IOException) {
            Log.i("KanadeMusicProbe", "Live playback unavailable: ${e.message?.take(100)}")
            assumeTrue("Provider did not supply playable audio", false)
            return
        }
        lateinit var model: KanadeViewModel
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[KanadeViewModel::class.java]
            model.playListTracks(listOf(song), song)
        }
        compose.waitUntil(30000) {
            val state = model.playback.state.value
            (state.mediaId == song.id && state.playing && state.positionMs > 1000) || state.error != null
        }
        assertNull(model.playback.state.value.error)
        Log.i("KanadeMusicProbe", "Verified native online playback for ${song.artist} — ${song.title}")
    }
}
