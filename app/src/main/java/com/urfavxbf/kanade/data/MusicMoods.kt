package com.urfavxbf.kanade.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Browse provider-curated mood categories and playlists, without text searches. */
object MusicMoods {
    data class Mood(val name: String, val params: String)
    private val allowed = setOf("chill", "feel good", "romance", "sad", "melancholy", "happy", "relax", "sleep", "energize", "party")

    private fun request(browseId: String, params: String? = null): JSONObject {
        val version = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val body = JSONObject().put("context", JSONObject().put("client", JSONObject()
            .put("clientName", "WEB_REMIX").put("clientVersion", "1.$version.01.00")
            .put("hl", "en").put("gl", "PH"))).put("browseId", browseId)
        if (params != null) body.put("params", params)
        val connection = URL("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) throw IOException("Mood browse unavailable")
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun visit(value: Any?, action: (JSONObject) -> Unit) {
        when (value) {
            is JSONObject -> { action(value); val keys = value.keys(); while (keys.hasNext()) visit(value.opt(keys.next()), action) }
            is JSONArray -> for (i in 0 until value.length()) visit(value.opt(i), action)
        }
    }

    internal fun parseMoods(root: JSONObject): List<Mood> {
        val result = mutableListOf<Mood>()
        visit(root) { node ->
            val button = node.optJSONObject("musicNavigationButtonRenderer") ?: return@visit
            val name = button.optJSONObject("buttonText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val endpoint = button.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
            val params = endpoint?.optString("params").orEmpty()
            if (name.lowercase(Locale.ROOT) in allowed && params.isNotBlank()) result += Mood(name, params)
        }
        return result.distinctBy { it.name.lowercase(Locale.ROOT) }
    }

    internal fun parsePlaylists(root: JSONObject): List<YouTubeMusicRepository.CatalogItem> {
        val result = mutableListOf<YouTubeMusicRepository.CatalogItem>()
        visit(root) { node ->
            val item = node.optJSONObject("musicTwoRowItemRenderer") ?: return@visit
            val name = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val browse = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            val id = browse.removePrefix("VL")
            if (!id.matches(Regex("(?:PL|RD|OLAK5uy_)[A-Za-z0-9_-]+")) || name.isBlank()) return@visit
            var image: String? = null
            var largestArea = -1L
            visit(item.optJSONObject("thumbnailRenderer")) { candidate ->
                val url = candidate.optString("url")
                val area = candidate.optInt("width").toLong() * candidate.optInt("height")
                if (url.startsWith("https://") && area > largestArea) {
                    image = url
                    largestArea = area
                }
            }
            result += YouTubeMusicRepository.CatalogItem(name, "Playlist", image, "https://www.youtube.com/playlist?list=$id")
        }
        return result.distinctBy { it.url }
    }

    suspend fun moods(): List<Mood> = withContext(Dispatchers.IO) { parseMoods(request("FEmusic_moods_and_genres")) }
    suspend fun playlists(mood: Mood): List<YouTubeMusicRepository.CatalogItem> = withContext(Dispatchers.IO) {
        parsePlaylists(request("FEmusic_moods_and_genres_category", mood.params))
    }
}
