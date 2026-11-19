package com.urfavxbf.kanade.data

import java.net.URI

object YouTubeLink {
    private val idPattern = Regex("[A-Za-z0-9_-]{11}")

    fun videoId(input: String): String? {
        val uri = runCatching { URI(input.trim()) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.userInfo != null) return null
        val path = uri.path.orEmpty()
        val id = when (uri.host?.lowercase()) {
            "youtu.be" -> path.removePrefix("/")
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com" -> when {
                path == "/watch" -> uri.rawQuery.orEmpty().split('&')
                    .firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                path.startsWith("/shorts/") -> path.removePrefix("/shorts/")
                path.startsWith("/live/") -> path.removePrefix("/live/")
                else -> null
            }
            else -> null
        }
        return id?.takeIf(idPattern::matches)
    }
}
