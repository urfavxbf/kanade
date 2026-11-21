package com.urfavxbf.kanade.ui

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource

internal fun radioQueue(seed: Track, candidates: List<Track>, online: Boolean): List<Track> {
    fun artist(track: Track) = track.artist.trim().removeSuffix(" - Topic").lowercase()
    fun title(track: Track) = songTitle(track.title, track.artist).trim().lowercase()
    val performer = artist(seed)
    val ranked = candidates.filter { online || it.source == TrackSource.LOCAL }
        .distinctBy { it.id }
        .shuffled()
        .sortedByDescending { track ->
            when {
                performer.isNotBlank() && performer != "unknown artist" && performer == artist(track) -> 3
                seed.genre.isNotBlank() && seed.genre.equals(track.genre, true) -> 2
                seed.album.isNotBlank() && seed.album.equals(track.album, true) -> 1
                else -> 0
            }
        }
    return (listOf(seed) + ranked).distinctBy { it.id }.distinctBy { artist(it) to title(it) }.take(25)
}
