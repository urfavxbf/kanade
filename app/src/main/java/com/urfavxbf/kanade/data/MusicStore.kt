package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource

fun Track.saved() = SavedTrack(
    id, title, artist, uri, artworkUri, durationMs, source.name, album, folder
)

fun SavedTrack.toTrack() = Track(
    id = id, title = title, artist = artist, uri = uri,
    artworkUri = artwork, durationMs = durationMs,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL),
    album = album, folder = folder,
)

class MusicStore(private val dao: MusicDao) {
    val favorites = dao.favorites()
    val favoriteTracks = dao.favoriteTracks()
    val playlists = dao.playlists()
    val recent = dao.recentTracks()
    val top = dao.topTracks(50)
    val topPlayedTracks = dao.topPlayedTracks()
    val totalPlays = dao.totalPlays()
    val totalListenMs = dao.totalListenMs()

    fun playlist(id: Long) = dao.playlistTracks(id)

    suspend fun updateTrack(track: Track) = dao.saveTrack(track.saved())

    suspend fun toggleFavorite(track: Track) {
        dao.saveTrack(track.saved())
        if (dao.isFavorite(track.id)) dao.removeFavorite(track.id)
        else dao.addFavorite(Favorite(track.id, System.currentTimeMillis()))
    }

    suspend fun newPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed.length <= 80) dao.createPlaylist(Playlist(name = trimmed))
    }

    suspend fun addToPlaylist(id: Long, track: Track) {
        dao.saveTrack(track.saved())
        dao.addItem(PlaylistItem(id, track.id, dao.nextPosition(id)))
    }

    suspend fun removeFromPlaylist(id: Long, trackId: String) = dao.removeItem(id, trackId)
    suspend fun removePlaylist(id: Long) {
        dao.clearPlaylistItems(id)
        dao.deletePlaylist(id)
    }

    suspend fun recordPlay(track: Track, listenMs: Long): Long {
        dao.saveTrack(track.saved())
        return dao.addPlay(PlayEntry(trackId = track.id, listenMs = listenMs))
    }

    suspend fun updateListenDuration(entryId: Long, listenMs: Long) {
        dao.updatePlay(entryId, listenMs)
    }
}
