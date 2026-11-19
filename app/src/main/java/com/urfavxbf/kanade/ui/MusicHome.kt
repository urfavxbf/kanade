package com.urfavxbf.kanade.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.playback.PlaybackState

@Composable
internal fun MusicHome(
    playback: PlaybackState, recent: List<Track>, favorites: List<Track>, songs: List<Track>,
    openError: String?, pickFile: () -> Unit, showLibrary: () -> Unit, discover: () -> Unit,
    openPlayer: () -> Unit, toggle: () -> Unit, play: (List<Track>, Track) -> Unit,
) {
    val latest = remember(songs) { songs.sortedByDescending { it.addedAtMs }.take(12) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Label("Your music", 32.sp, PrimaryText, FontWeight.Bold) }
        if (playback.hasItem) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable(role = Role.Button, onClick = openPlayer),
                    verticalAlignment = Alignment.CenterVertically) {
                    AlbumImage(playback.artworkUri, Modifier.size(74.dp))
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        Label(if (playback.playing) "Now playing" else "Continue listening", 12.sp, Accent)
                        Label(playback.title, 18.sp, PrimaryText, FontWeight.Bold, lines = 2)
                        Label(playback.artist, 13.sp, Muted, lines = 1)
                    }
                }
                Chip(if (playback.playing) "Pause" else "Resume", onClick = toggle)
            }
        }
        item {
            if (playback.hasItem) SimpleRow("Open audio file", "Choose a song from your phone", pickFile)
            else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                .clickable(role = Role.Button, onClick = pickFile).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AlbumImage(null, Modifier.size(82.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Label("Open audio file", 21.sp, PrimaryText, FontWeight.Bold)
                    Label("Choose a song from your phone", 13.sp, Muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            SimpleRow("Browse library", if (songs.isEmpty()) "Songs, albums and artists" else "${songs.size} songs on this device", showLibrary)
            openError?.let { Label(it, 13.sp, Accent) }
        }
        if (recent.isNotEmpty()) item { SongShelf("Recently played", recent, play) }
        if (favorites.isNotEmpty()) item { SongShelf("Favorites", favorites, play) }
        if (latest.isNotEmpty()) item { SongShelf("Recently added", latest, play) }
        item { SimpleRow("Discover songs", "Search for music to play in Kanade", discover) }
    }
}

@Composable
private fun SongShelf(title: String, songs: List<Track>, play: (List<Track>, Track) -> Unit) {
    Label(title, 19.sp, PrimaryText, FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(songs.take(12)) { track ->
            Column(Modifier.width(124.dp).clickable(role = Role.Button) { play(songs, track) }) {
                AlbumImage(track.artworkUri, Modifier.size(124.dp))
                Spacer(Modifier.height(6.dp))
                Label(track.title, 14.sp, PrimaryText, FontWeight.Bold, lines = 1)
                Label(track.artist, 12.sp, Muted, lines = 1)
            }
        }
    }
}
