package com.urfavxbf.kanade.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Opens provider-owned searches. Kanade never accesses, extracts or proxies
 * protected streams from either service.
 */
enum class MusicCatalogProvider { YOUTUBE_MUSIC, SPOTIFY }

object MusicCatalogLinks {
    fun search(provider: MusicCatalogProvider, input: String): String {
        val query = input.trim().take(120).ifBlank { "OPM Filipino music" }
        val escaped = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20")
        return when (provider) {
            MusicCatalogProvider.YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$escaped"
            MusicCatalogProvider.SPOTIFY -> "https://open.spotify.com/search/$escaped"
        }
    }
}
