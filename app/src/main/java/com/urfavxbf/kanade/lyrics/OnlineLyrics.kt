package com.urfavxbf.kanade.lyrics

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object OnlineLyrics {
    data class Result(val synced: List<LyricLine>, val plain: String?)

    suspend fun get(title: String, artist: String, durationMs: Long): Result? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null
        val url = Uri.parse("https://lrclib.net/api/get").buildUpon()
            .appendQueryParameter("track_name", title)
            .appendQueryParameter("artist_name", artist)
            .apply { if (durationMs > 0) appendQueryParameter("duration", (durationMs / 1000).toString()) }
            .build().toString()
        request(url)?.let { decode(JSONObject(it)) }
    }

    data class Match(val title: String, val artist: String, val album: String, val duration: Long,
                     val lyrics: Result, val raw: String)

    suspend fun search(query: String): List<Match> = withContext(Dispatchers.IO) {
        val url = Uri.parse("https://lrclib.net/api/search").buildUpon()
            .appendQueryParameter("q", query.trim()).build().toString()
        val rows = JSONArray(request(url) ?: "[]")
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val lyrics = decode(row)
                if (lyrics.synced.isEmpty() && lyrics.plain == null) continue
                add(Match(row.optString("trackName"), row.optString("artistName"),
                    row.optString("albumName"), row.optLong("duration"), lyrics, row.toString()))
            }
        }
    }

    fun decode(json: JSONObject): Result {
        val synced = json.optString("syncedLyrics", "").takeUnless { it == "null" }.orEmpty()
        val plain = json.optString("plainLyrics", "").takeUnless { it.isBlank() || it == "null" }
        return Result(LrcParser.parse(synced), plain)
    }

    private fun request(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Kanade/0.1 (Android music player)")
            if (connection.responseCode == 404) return null
            if (connection.responseCode != 200) throw IOException("Lyrics unavailable")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }
}
