package com.urfavxbf.kanade.data

import android.net.Uri
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Official Audius read-only API. Does not cache or download copyrighted streams. */
class AudiusRepository {
    suspend fun search(input: String): List<Track> = withContext(Dispatchers.IO) {
        val query = input.trim()
        require(query.isNotEmpty() && query.length <= 120) { "Invalid search query" }
        val endpoint = "https://api.audius.co/v1/tracks/search?query=${Uri.encode(query)}&limit=25"
        var lastFailure: IOException? = null
        for (attempt in 0..2) {
            currentCoroutineContext().ensureActive()
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    if (!AudiusRetryPolicy.retryable(status, attempt)) {
                        throw IOException(if (status == 429) "Audius rate limit reached" else "Audius HTTP $status")
                    }
                    lastFailure = IOException("Audius HTTP $status")
                } else {
                    val data = connection.inputStream.bufferedReader().use {
                        it.readText().take(2_000_000)
                    }
                    currentCoroutineContext().ensureActive()
                    return@withContext parseTracks(data)
                }
            } catch (e: IOException) {
                lastFailure = e
                if (attempt >= 2) throw e
            } finally {
                connection.disconnect()
            }
            delay((400L * (attempt + 1)).coerceAtMost(1_200L))
        }
        throw lastFailure ?: IOException("Audius request failed")
    }

    private fun parseTracks(content: String): List<Track> {
        val json = JSONObject(content).optJSONArray("data") ?: return emptyList()
        return buildList {
            val ids = mutableSetOf<String>()
            for (i in 0 until json.length()) {
                val raw = json.optJSONObject(i) ?: continue
                val id = raw.optString("id")
                val title = raw.optString("title")
                if (id.isBlank() || title.isBlank() || !ids.add(id)) continue
                if (!raw.optBoolean("is_streamable", raw.optBoolean("isStreamable", true))) continue
                val artwork = raw.optJSONObject("artwork")
                val artist = raw.optJSONObject("user")?.optString("name")
                    ?.takeIf { it.isNotBlank() } ?: "Unknown artist"
                add(Track(
                    id = "audius:$id",
                    title = title,
                    artist = artist,
                    uri = "https://api.audius.co/v1/tracks/${Uri.encode(id)}/stream",
                    artworkUri = artwork?.optString("480x480")?.takeIf { it.startsWith("https://") }
                        ?: artwork?.optString("150x150")?.takeIf { it.startsWith("https://") },
                    durationMs = raw.optLong("duration") * 1_000,
                    source = TrackSource.AUDIUS,
                    album = raw.optString("album_name"),
                ))
            }
        }
    }
}

object AudiusRetryPolicy {
    /** Retry only transient server/rate-limit errors, at most twice. */
    fun retryable(status: Int, attempt: Int): Boolean =
        attempt < 2 && (status == 429 || status in 500..599)
}
