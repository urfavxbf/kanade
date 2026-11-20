package com.urfavxbf.kanade.data

private val mp3MimeTypes = setOf("audio/mpeg", "audio/mp3", "audio/x-mp3", "audio/mpeg3", "audio/x-mpeg-3")

internal fun isMp3(filename: String, mimeType: String?): Boolean {
    val extension = filename.trim().substringAfterLast('.', "").lowercase()
    val mime = mimeType.orEmpty().substringBefore(';').trim().lowercase()
    if (extension.isEmpty()) return mime in mp3MimeTypes
    return extension == "mp3" && (mime in mp3MimeTypes || mime.isEmpty() || mime == "application/octet-stream")
}
