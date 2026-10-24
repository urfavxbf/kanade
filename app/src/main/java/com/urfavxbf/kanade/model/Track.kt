package com.urfavxbf.kanade.model

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val artworkUri: String? = null,
    val durationMs: Long = 0L,
    val source: TrackSource,
    val album: String = "",
    val folder: String = "",
    val filename: String = "",
    val genre: String = "",
    val addedAtMs: Long = 0,
) {
    fun mediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUri?.let(android.net.Uri::parse))
                .build()
        )
        .build()
}

enum class TrackSource { LOCAL, AUDIUS }
