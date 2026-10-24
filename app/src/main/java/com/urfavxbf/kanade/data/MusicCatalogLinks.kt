package com.urfavxbf.kanade.data

import java.net.URLEncoder

/**
 * Opens provider-owned searches. Kanade never accesses, extracts or proxies
 * protected streams from any provider.
 */
enum class MusicCatalogProvider { SOUNDCLOUD, YOUTUBE_MUSIC, SPOTIFY }

object MusicCatalogLinks {
    fun search(provider: MusicCatalogProvider, input: String): String {
        val query = input.trim().take(120).ifBlank { "OPM Filipino music" }
        val escaped = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return when (provider) {
            MusicCatalogProvider.SOUNDCLOUD -> "https://soundcloud.com/search?q=$escaped"
            MusicCatalogProvider.YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$escaped"
            MusicCatalogProvider.SPOTIFY -> "https://open.spotify.com/search/$escaped"
        }
    }
}
