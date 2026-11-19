package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import java.text.Normalizer
import java.util.Locale

internal object AudioSearchMatches {
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "")
            .lowercase(Locale.ROOT).replace(separators, " ").trim()

    fun rank(query: String, tracks: List<Track>): List<Track> {
        val name = normalize(query)
        if (name.isEmpty()) return emptyList()
        val phrase = " $name "
        return tracks.mapNotNull { track ->
            val artist = normalize(track.artist)
            val title = normalize(track.title)
            val priority = when {
                artist == name -> 0
                title == name -> 1
                phrase in " $artist " -> 2
                phrase in " $title " -> 3
                phrase in " $artist $title " || phrase in " $title $artist " -> 4
                else -> return@mapNotNull null
            }
            priority to track
        }.sortedBy { it.first }.map { it.second }
    }
}
