package com.urfavxbf.kanade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urfavxbf.kanade.data.Playlist
import com.urfavxbf.kanade.model.Track

@Composable
internal fun MusicResults(
    input: String, type: String, library: List<Track>, online: List<Track>, playlists: List<Playlist>,
    play: (List<Track>, Track) -> Unit, openArtist: (String, Boolean) -> Unit, openAlbum: (String) -> Unit,
    openPlaylist: (Long) -> Unit,
) {
    val query = input.trim()
    val songs = remember(library, online, query) {
        val words = query.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        val local = library.filter { track ->
            val metadata = "${track.title} ${track.artist} ${track.album}"
            words.all { metadata.contains(it, ignoreCase = true) }
        }
        (local + online).distinctBy { it.id }
    }
    val localArtists = remember(library) { library.mapTo(mutableSetOf()) { it.artist } }
    val artists = remember(localArtists, online, query) {
        (localArtists + online.map { it.artist }).filter { it.contains(query, true) }.sortedBy { it.lowercase() }
    }
    val albums = remember(library, query) { library.map { it.album }.filter { it.isNotBlank() && it.contains(query, true) }.distinct().sorted() }
    val lists = playlists.filter { it.name.contains(query, true) }
    val all = type == "All"
    val displaySongs = if (all) songs.take(8) else if (type == "Songs") songs else emptyList()
    val displayArtists = if (all) artists.take(4) else if (type == "Artists") artists else emptyList()
    val displayAlbums = if (all) albums.take(4) else if (type == "Albums") albums else emptyList()
    val displayLists = if (all) lists.take(4) else if (type == "Playlists") lists else emptyList()
    LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (displaySongs.isNotEmpty()) {
            item { Label("Songs", 18.sp, PrimaryText, FontWeight.Bold) }
            items(displaySongs) { song ->
                Row(Modifier.fillMaxWidth().clickable { play(songs, song) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AlbumImage(song.artworkUri, Modifier.size(52.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Label(song.title, 15.sp, PrimaryText, FontWeight.Bold, lines = 1)
                        Label("${song.artist} · ${song.source.label}", 12.sp, Muted, lines = 1)
                    }
                    Label(DurationFormatter.format(song.durationMs), 11.sp, Muted)
                }
            }
        }
        if (displayArtists.isNotEmpty()) {
            item { Label("Artists", 18.sp, PrimaryText, FontWeight.Bold) }
            items(displayArtists) { artist ->
                val local = artist in localArtists
                SimpleRow(artist, if (local) "Library artist" else online.firstOrNull { it.artist == artist }?.source?.label.orEmpty()) { openArtist(artist, local) }
            }
        }
        if (displayAlbums.isNotEmpty()) {
            item { Label("Albums", 18.sp, PrimaryText, FontWeight.Bold) }
            items(displayAlbums) { SimpleRow(it, "Library album") { openAlbum(it) } }
        }
        if (displayLists.isNotEmpty()) {
            item { Label("Playlists", 18.sp, PrimaryText, FontWeight.Bold) }
            items(displayLists) { SimpleRow(it.name, "Saved playlist") { openPlaylist(it.id) } }
        }
        if (displaySongs.isEmpty() && displayArtists.isEmpty() && displayAlbums.isEmpty() && displayLists.isEmpty()) {
            item { Label(if (query.isBlank()) "Enter a song, artist or album name." else "No matches in this collection.", 14.sp, Muted) }
        }
    }
}
