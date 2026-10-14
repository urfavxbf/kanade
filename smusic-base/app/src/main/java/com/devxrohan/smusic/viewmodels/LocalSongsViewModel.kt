package com.devxrohan.smusic.viewmodels

import android.app.Application
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devxrohan.smusic.models.LocalSong
import com.urfavxbf.kanade.AudioFile
import com.urfavxbf.kanade.MetadataNormalizer
import com.urfavxbf.kanade.MetadataOverrideManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalSongsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val metadataOverrides = MetadataOverrideManager(application)

    private val _songs = MutableStateFlow<List<LocalSong>>(emptyList())
    val songs: StateFlow<List<LocalSong>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val filteredSongs: StateFlow<List<LocalSong>> = _filteredSongs.asStateFlow()

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadSongs()
        }
    }

    init {
        application.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context = getApplication<Application>()
            val songList = mutableListOf<LocalSong>()

            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID,
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            try {
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId
                        )

                        val rawTitle = cursor.getString(titleCol) ?: "Unknown Title"
                        val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val rawAlbum = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val dateAdded = cursor.getLong(dateCol)

                        // Adapt Kanade's saved overrides to SMusic's local-library model.
                        val legacySong = AudioFile(
                            id,
                            rawTitle,
                            rawArtist,
                            rawAlbum,
                            contentUri.toString(),
                            null,
                            duration,
                            dateAdded
                        )
                        metadataOverrides.apply(legacySong)

                        songList += LocalSong(
                            id = id,
                            title = MetadataNormalizer.normalizeTitle(legacySong.title, null),
                            artist = MetadataNormalizer.normalizeArtist(legacySong.artist, null),
                            album = MetadataNormalizer.normalizeAlbum(legacySong.album),
                            duration = duration,
                            uri = contentUri,
                            albumArtUri = albumArtUri,
                            size = cursor.getLong(sizeCol),
                            dateAdded = dateAdded,
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _songs.value = songList
            applyFilter(songList, _searchQuery.value)
            _isLoading.value = false
        }
    }

    fun saveMetadata(song: LocalSong, title: String, artist: String, album: String): Boolean {
        val editedSong = AudioFile(
            song.id,
            title.trim().ifEmpty { song.title },
            artist.trim().ifEmpty { song.artist },
            album.trim().ifEmpty { song.album },
            song.uri.toString(),
            null,
            song.duration,
            song.dateAdded
        )
        val saved = metadataOverrides.save(editedSong)
        if (saved) loadSongs()
        return saved
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilter(_songs.value, query)
    }

    private fun applyFilter(songs: List<LocalSong>, query: String) {
        _filteredSongs.value = if (query.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}
