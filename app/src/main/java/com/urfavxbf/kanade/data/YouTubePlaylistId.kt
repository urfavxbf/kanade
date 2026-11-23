package com.urfavxbf.kanade.data

import java.net.URI

internal object YouTubePlaylistId {
    fun fromUrl(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "youtube.com", "music.youtube.com") || uri.path != "/playlist") return null
        val ids = uri.rawQuery?.split('&')?.filter { it.startsWith("list=") }.orEmpty()
        if (ids.size != 1) return null
        ids.single().substringAfter('=').takeIf { Regex("[A-Za-z0-9_-]{1,200}").matches(it) }
    }.getOrNull()
}
