package com.urfavxbf.kanade.ui

internal enum class ResultKind { TRACK, ARTIST, ALBUM }
internal data class SearchSpotlight(val kind: ResultKind, val index: Int)

internal fun searchSpotlight(
    query: String, tracks: List<String>, artists: List<String>, albums: List<String>,
): SearchSpotlight? {
    val needle = query.trim()
    if (needle.isEmpty()) return null
    var best: SearchSpotlight? = null
    var bestScore = 0
    fun consider(names: List<String>, kind: ResultKind) {
        for ((index, name) in names.withIndex()) {
            val score = when {
                name.equals(needle, ignoreCase = true) -> 4
                name.startsWith(needle, ignoreCase = true) -> 3
                name.contains(needle, ignoreCase = true) -> 2
                else -> 1
            }
            if (score > bestScore) {
                best = SearchSpotlight(kind, index)
                bestScore = score
            }
        }
    }
    consider(tracks, ResultKind.TRACK)
    consider(artists, ResultKind.ARTIST)
    consider(albums, ResultKind.ALBUM)
    return best
}
