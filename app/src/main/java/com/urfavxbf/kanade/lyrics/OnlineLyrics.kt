package com.urfavxbf.kanade.lyrics

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Kanade/0.1 (Android music player)")
            if (connection.responseCode == 404) return@withContext null
            if (connection.responseCode != 200) throw IOException("Lyrics unavailable")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val synced = json.optString("syncedLyrics", "").takeUnless { it == "null" }.orEmpty()
            val plain = json.optString("plainLyrics", "").takeUnless { it.isBlank() || it == "null" }
            Result(LrcParser.parse(synced), plain)
        } finally { connection.disconnect() }
    }
}
