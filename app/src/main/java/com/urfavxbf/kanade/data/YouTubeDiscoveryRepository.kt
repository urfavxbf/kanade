package com.urfavxbf.kanade.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnail: String,
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
}

/** YouTube Data API search returns metadata only; it is NOT a playable audio source. */
object YouTubeSearchUrl {
    fun build(query: String, apiKey: String): String {
        require(query.trim().length in 1..120) { "Search must be 1 to 120 characters." }
        require(apiKey.isNotBlank()) { "YouTube API key is required." }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val key = URLEncoder.encode(apiKey.trim(), "UTF-8")
        return "https://www.googleapis.com/youtube/v3/search" +
            "?part=snippet&type=video&videoCategoryId=10&regionCode=PH" +
            "&safeSearch=moderate&maxResults=15&q=$encoded&key=$key"
    }
}

class YouTubeDiscoveryRepository(private val context: Context) {
    suspend fun search(query: String, key: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val connection = (URL(YouTubeSearchUrl.build(query, key)).openConnection()
            as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 14_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Android-Package", context.packageName)
            signingCertificateSha1()?.let { setRequestProperty("X-Android-Cert", it) }
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException(when (status) {
                    400 -> "Invalid YouTube request or API key."
                    401 -> "YouTube API authentication failed."
                    403 -> "YouTube API blocked this request. Check key restrictions, enabled API, or daily quota."
                    429 -> "YouTube API rate limit reached."
                    else -> "YouTube API error (HTTP $status)."
                })
            }
            val body = connection.inputStream.bufferedReader().use { it.readText().take(900_000) }
            val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until items.length()) {
                    val result = items.optJSONObject(i) ?: continue
                    val id = result.optJSONObject("id")?.optString("videoId").orEmpty()
                    if (!id.matches(Regex("[a-zA-Z0-9_-]{11}"))) continue
                    val snippet = result.optJSONObject("snippet") ?: continue
                    // HTML entities returned by the API are decoded for display, not rewritten.
                    val title = Html.fromHtml(snippet.optString("title"), Html.FROM_HTML_MODE_LEGACY).toString()
                    val channel = Html.fromHtml(snippet.optString("channelTitle"), Html.FROM_HTML_MODE_LEGACY).toString()
                    val thumbnails = snippet.optJSONObject("thumbnails")
                    val thumb = thumbnails?.optJSONObject("medium")?.optString("url")
                        ?: thumbnails?.optJSONObject("default")?.optString("url").orEmpty()
                    add(YouTubeVideo(id, title, channel, thumb))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun signingCertificateSha1(): String? = runCatching {
        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= 28) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        @Suppress("DEPRECATION")
        val certificate = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            info.signatures?.firstOrNull()?.toByteArray()
        } ?: return@runCatching null
        MessageDigest.getInstance("SHA-1").digest(certificate)
            .joinToString("") { "%02X".format(it) }
    }.getOrNull()
}
