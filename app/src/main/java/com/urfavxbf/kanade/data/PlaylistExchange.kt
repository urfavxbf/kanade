package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track

/** M3U export/import is URI-based and scoped to files accessible on this device. */
object PlaylistExchange {
    fun export(tracks: List<Track>): String = buildString {
        appendLine("#EXTM3U")
        for (track in tracks) {
            val seconds = track.durationMs / 1000
            appendLine("#EXTINF:$seconds,${track.artist.replace("\n", " ")} - ${track.title.replace("\n", " ")}")
            appendLine(track.uri)
        }
    }

    fun import(playlist: String, available: List<Track>): List<Track> {
        val byUri = available.associateBy { it.uri }
        return playlist.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { byUri[it] }
            .distinctBy { it.id }.toList()
    }
}
