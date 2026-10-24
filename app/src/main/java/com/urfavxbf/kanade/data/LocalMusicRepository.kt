package com.urfavxbf.kanade.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicRepository(private val resolver: ContentResolver) {
    suspend fun load(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )
        val results = mutableListOf<Track>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val albumId = cursor.getLong(albumIndex)
                results += Track(
                    id = "local:$id",
                    title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "Unknown track",
                    artist = cursor.getString(artistIndex)?.takeIf { it.isNotBlank() } ?: "Unknown artist",
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    artworkUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(
                            android.net.Uri.parse("content://media/external/audio/albumart"), albumId
                        ).toString()
                    } else null,
                    durationMs = cursor.getLong(durationIndex),
                    source = TrackSource.LOCAL,
                )
            }
        }
        results
    }
}
