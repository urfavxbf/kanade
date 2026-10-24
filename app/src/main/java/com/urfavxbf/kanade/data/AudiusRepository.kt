package com.urfavxbf.kanade.data

import android.net.Uri
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AudiusRepository {
    suspend fun search(input: String): List<Track> = withContext(Dispatchers.IO) {
        val query = input.trim()
        require(query.isNotEmpty() && query.length <= 120) { "Invalid search query" }
        val endpoint = "https://api.audius.co/v1/tracks/search?query=${Uri.encode(query)}&limit=25"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw java.io.IOException("Audius HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until json.length()) {
                    val raw = json.optJSONObject(i) ?: continue
                    val id = raw.optString("id")
                    val title = raw.optString("title")
                    if (id.isBlank() || title.isBlank()) continue
                    if (!raw.optBoolean("is_streamable", true)) continue
                    val artwork = raw.optJSONObject("artwork")
                    val artist = raw.optJSONObject("user")?.optString("name")
                        ?.takeIf { it.isNotBlank() } ?: "Unknown artist"
                    add(
                        Track(
                            id = "audius:$id",
                            title = title,
                            artist = artist,
                            uri = "https://api.audius.co/v1/tracks/${Uri.encode(id)}/stream",
                            artworkUri = artwork?.optString("480x480")?.takeIf { it.isNotBlank() }
                                ?: artwork?.optString("150x150")?.takeIf { it.isNotBlank() },
                            durationMs = raw.optLong("duration") * 1_000,
                            source = TrackSource.AUDIUS,
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
