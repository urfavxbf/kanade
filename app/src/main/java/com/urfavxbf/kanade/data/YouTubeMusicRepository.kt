package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Signed-out YouTube Music catalog, using the InnerTube song search used by SMusic. */
internal object YouTubeMusicRepository {
    private enum class Client(val apiName: String, val version: String, val number: String) {
        WEB("WEB_REMIX", "1.20260114.01.00", "67"),
        ANDROID("ANDROID_MUSIC", "7.27.52", "21"),
    }
    private val videoId = Regex("[A-Za-z0-9_-]{11}")

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("query", query.trim())
            .put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        songs(post("search", body, Client.WEB))
    }

    private fun post(endpoint: String, body: JSONObject, profile: Client): JSONObject {
        val client = JSONObject().put("clientName", profile.apiName).put("clientVersion", profile.version)
            .put("hl", "en").put("gl", "PH")
        body.put("context", JSONObject().put("client", client))
        val request = URL("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false")
            .openConnection() as HttpURLConnection
        try {
            request.connectTimeout = 10000
            request.readTimeout = 15000
            request.requestMethod = "POST"
            request.doOutput = true
            request.setRequestProperty("Content-Type", "application/json")
            request.setRequestProperty("Origin", "https://music.youtube.com")
            request.setRequestProperty("X-YouTube-Client-Name", profile.number)
            request.setRequestProperty("X-YouTube-Client-Version", profile.version)
            request.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (request.responseCode !in 200..299) throw IOException("YouTube Music HTTP ${request.responseCode}")
            return request.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            request.disconnect()
        }
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)

    internal fun songs(response: JSONObject): List<Track> {
        val contents = response.getJSONObject("contents")
        val sections = contents.optJSONObject("sectionListRenderer") ?: contents
            .optJSONObject("tabbedSearchResultsRenderer")?.optJSONArray("tabs").objects()
            .firstNotNullOfOrNull { it.optJSONObject("tabRenderer")?.optJSONObject("content")?.optJSONObject("sectionListRenderer") }
            ?: return emptyList()
        return sections.optJSONArray("contents").objects().flatMap { section ->
            section.optJSONObject("musicShelfRenderer")?.optJSONArray("contents").objects()
                .mapNotNull { it.optJSONObject("musicResponsiveListItemRenderer")?.let(::song) }
        }.distinctBy { it.id }
    }

    private fun song(row: JSONObject): Track? {
        val columns = row.optJSONArray("flexColumns").objects().map {
            it.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs").objects()
        }
        val heading = columns.firstOrNull()?.firstOrNull() ?: return null
        val title = heading.optString("text").trim()
        val id = row.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf(videoId::matches)
            ?: heading.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
        if (id == null || !videoId.matches(id) || title.isEmpty()) return null
        val metadata = columns.drop(1).flatten()
        val artists = metadata.filter {
            it.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")?.startsWith("UC") == true
        }.map { it.optString("text") }.distinct()
        val fixed = row.optJSONArray("fixedColumns").objects().flatMap {
            it.optJSONObject("musicResponsiveListItemFixedColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs").objects()
        }
        val time = (fixed + metadata).map { it.optString("text") }.firstOrNull { it.matches(Regex("(?:\\d+:){1,2}\\d{2}")) }
        val seconds = time?.split(':')?.fold(0L) { total, part -> total * 60 + part.toLong() } ?: 0L
        val image = row.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails").objects().lastOrNull()?.optString("url")?.takeIf { it.startsWith("https://") }
        return Track("ytmusic:$id", title, artists.joinToString(", ").ifBlank { "Unknown artist" },
            "ytmusic://track/$id", image, seconds * 1000, TrackSource.YOUTUBE_MUSIC)
    }

    // Media3 calls this on its loader thread when a queue entry opens, including after process recreation.
    fun resolve(id: String): String {
        if (!videoId.matches(id)) throw IOException("Invalid YouTube Music item")
        val response = post("player", JSONObject().put("videoId", id), Client.ANDROID)
        val status = response.optJSONObject("playabilityStatus")?.optString("status")
        if (status != "OK") throw IOException("YouTube Music playback status: ${status ?: "unknown"}")
        return streamUrl(response) ?: throw IOException("YouTube Music did not supply a playable audio stream")
    }

    internal fun streamUrl(response: JSONObject): String? {
        if (response.optJSONObject("playabilityStatus")?.optString("status") != "OK") return null
        return response.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats").objects()
            .filter { it.optString("mimeType").startsWith("audio/") && !it.has("drmFamilies") }
            .sortedByDescending { it.optInt("bitrate") }
            .map { it.optString("url") }.firstOrNull {
                val uri = runCatching { URI(it) }.getOrNull()
                uri?.scheme == "https" && uri.host?.endsWith(".googlevideo.com") == true
            }
    }
}
