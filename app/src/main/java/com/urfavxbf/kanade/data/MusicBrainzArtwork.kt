package com.urfavxbf.kanade.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.concurrent.TimeUnit

internal data class CoverQuery(val artist: String, val title: String, val album: Boolean) {
    companion object {
        fun forMusic(artist: String, album: String, track: String = ""): CoverQuery? {
            val performer = artist.trim().removeSuffix(" - Topic").trim()
            if (performer.isEmpty() || performer.lowercase() in setOf("<unknown>", "unknown artist", "various artists")) return null
            val hasAlbum = album.isNotBlank() && !album.equals("Unknown album", true) && album != "<unknown>"
            val title = (if (hasAlbum) album else track).trim()
            return if (title.isBlank()) null else CoverQuery(performer, title, hasAlbum)
        }
    }
}

internal object MusicBrainzArtwork {
    private const val ROOT = "https://coverartarchive.org"
    private val mbid = Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    private val accents = Regex("\\p{M}+")
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val lock = Mutex()
    private data class Cached(val urls: List<String>, val expiresAt: Long)
    private val cache = LinkedHashMap<CoverQuery, Cached>(128, 0.75f, true)
    private var nextRequestAt = 0L
    private var retryAt = 0L
    private fun clock() = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())

    suspend fun find(query: CoverQuery): List<String> = withContext(Dispatchers.IO) {
        lock.withLock {
            val now = clock()
            cache[query]?.takeIf { now < it.expiresAt }?.let { return@withLock it.urls }
            if (now < retryAt) return@withLock emptyList()
            delay((nextRequestAt - now).coerceAtLeast(0L))
            nextRequestAt = clock() + 1100L
            val entity = if (query.album) "release" else "recording"
            fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            val terms = URLEncoder.encode("$entity:${quote(query.title)} AND artist:${quote(query.artist)}", "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/$entity/?query=$terms&fmt=json&limit=8")
            val connection = url.openConnection() as HttpURLConnection
            val urls = try {
                connection.connectTimeout = 4000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Kanade/0.1.0 (https://github.com/urfavxbf/kanade)")
                connection.setRequestProperty("Accept", "application/json")
                when (connection.responseCode) {
                    200 -> connection.inputStream.bufferedReader().use { covers(query, JSONObject(it.readText())) }
                    429, 503 -> {
                        val seconds = connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(1L, 86400L) ?: 60L
                        retryAt = clock() + seconds * 1000L
                        return@withLock emptyList()
                    }
                    else -> return@withLock emptyList()
                }
            } catch (_: IOException) {
                retryAt = clock() + 5000L
                return@withLock emptyList()
            } catch (_: JSONException) {
                return@withLock emptyList()
            } finally {
                connection.disconnect()
            }
            cache[query] = Cached(urls, clock() + if (urls.isEmpty()) 600_000L else 86_400_000L)
            if (cache.size > 256) cache.remove(cache.keys.first())
            urls
        }
    }

    private fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(accents, "").lowercase().replace(punctuation, " ").trim()

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun matches(query: CoverQuery, item: JSONObject): Boolean {
        if (normalize(item.optString("title")) != normalize(query.title)) return false
        val credits = item.optJSONArray("artist-credit").objects()
        val credited = credits.joinToString("") {
            (it.optString("name").ifBlank { it.optJSONObject("artist")?.optString("name").orEmpty() }) + it.optString("joinphrase")
        }
        return normalize(credited) == normalize(query.artist)
    }

    internal fun covers(query: CoverQuery, response: JSONObject): List<String> {
        val releases = if (query.album) {
            response.optJSONArray("releases").objects().filter { matches(query, it) }
        } else {
            response.optJSONArray("recordings").objects().filter { matches(query, it) }
                .flatMap { it.optJSONArray("releases").objects() }
        }
        val eligible = releases.filter { release ->
            val types = release.optJSONObject("release-group")?.optJSONArray("secondary-types")
            val compilation = types != null && (0 until types.length()).any { types.optString(it) == "Compilation" }
            (query.album || !compilation) && release.optString("status") in listOf("", "Official")
        }.sortedWith(compareBy<JSONObject> { it.optString("status") != "Official" }
            .thenBy { it.optString("date").ifEmpty { "9999" } })
        return buildList {
            for (release in eligible) {
                val group = release.optJSONObject("release-group")?.optString("id").orEmpty()
                if (mbid.matches(group)) add("$ROOT/release-group/$group/front-500")
                val id = release.optString("id")
                if (mbid.matches(id)) add("$ROOT/release/$id/front-500")
            }
        }.distinct().take(3)
    }
}
