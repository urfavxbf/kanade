package com.urfavxbf.kanade.data

import java.net.URI

object YouTubeTrackId {
    private val videoId = Regex("[A-Za-z0-9_-]{11}")

    fun fromUrl(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "youtube.com", "music.youtube.com")) return null
        if (uri.path != "/watch") return null
        uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }
            ?.substringAfter('=')?.takeIf { videoId.matches(it) }
    }.getOrNull()

    fun isValid(id: String): Boolean = videoId.matches(id)
}
