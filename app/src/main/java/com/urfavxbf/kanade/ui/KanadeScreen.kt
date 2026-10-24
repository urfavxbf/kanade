package com.urfavxbf.kanade.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.urfavxbf.kanade.lyrics.LrcParser
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import com.urfavxbf.kanade.playback.PlaybackState

private val Background = Color(0xFF0B0D16)
private val Surface = Color(0xFF171925)
private val SurfaceLight = Color(0xFF292A3D)
private val Accent = Color(0xFFBDA5FF)
private val Pink = Color(0xFFF6A7D4)
private val PrimaryText = Color(0xFFF6F3FF)
private val Muted = Color(0xFFA7A6BB)

private enum class Page { LIBRARY, ONLINE, PLAYLISTS, INSIGHTS }
private enum class Overlay { NONE, PLAYER, QUEUE, LYRICS, AUDIO, PICK_PLAYLIST, EDIT_TAGS }

@Composable
fun KanadeScreen(model: KanadeViewModel) {
    val library by model.library.collectAsState()
    val discover by model.discover.collectAsState()
    val playback by model.playback.state.collectAsState()
    val favorites by model.favorites.collectAsState()
    val favoriteIds by model.favoriteIds.collectAsState()
    val playlists by model.playlists.collectAsState()
    val recent by model.recent.collectAsState()
    val topTracks by model.topTracks.collectAsState()
    val selectedTracks by model.selectedPlaylistTracks.collectAsState()
    val totalPlays by model.totalPlays.collectAsState()
    val totalListenMs by model.totalListenMs.collectAsState()
    val lyrics by model.lyrics.collectAsState()

    var page by rememberSaveable { mutableStateOf(Page.LIBRARY) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var query by rememberSaveable { mutableStateOf("") }
    var onlineQuery by rememberSaveable { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }
    var selectedPlaylistId by rememberSaveable { mutableStateOf(-1L) }
    var selectedSmart by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var folder by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Title") }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) model.loadLocal()
    }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) model.loadLyrics(uri)
    }
    val m3uImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && selectedPlaylistId >= 0) model.importM3u(uri, selectedPlaylistId)
    }
    val m3uExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri: Uri? ->
        if (uri != null && selectedPlaylistId >= 0) model.exportM3u(uri, selectedTracks)
    }

    LaunchedEffect(Unit) { if (model.hasPermission()) model.loadLocal() }

    Column(
        Modifier.fillMaxSize().background(Background)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Accent, Pink))),
                contentAlignment = Alignment.Center,
            ) { Label("♫", 27.sp, Background, FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Label("KANADE", 21.sp, PrimaryText, FontWeight.Bold)
                Label("Your sound. Your space.", 11.sp, Muted)
            }
            Label("HI-FI • MUSIC", 10.sp, Accent, modifier = Modifier
                .clip(RoundedCornerShape(12.dp)).background(Surface).padding(10.dp))
        }

        Spacer(Modifier.height(19.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for (tab in Page.entries) {
                Chip(when (tab) {
                    Page.LIBRARY -> "Library"
                    Page.ONLINE -> "Online"
                    Page.PLAYLISTS -> "Playlists"
                    Page.INSIGHTS -> "Insights"
                }, selected = page == tab) { page = tab }
            }
        }
        Spacer(Modifier.height(18.dp))

        Box(Modifier.weight(1f)) {
            when (page) {
                Page.LIBRARY -> Column {
                    Header("Your library", "Your music stays yours, even offline.")
                    Spacer(Modifier.height(12.dp))
                    if (!model.hasPermission()) {
                        EmptyState("♫", "Grant music permission", "Browse audio stored on your device.") {
                            permissions.launch(model.permission())
                        }
                    } else {
                        SearchField(query, "Search songs, albums, artists, folders…", { query = it }, {})
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (choice in listOf("All", "Favorites", "Recent", "Top", "Folders", "Albums", "Artists", "Duplicates")) {
                                Chip(choice, filter == choice) { filter = choice; folder = ""; album = "" }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(if (library.loading) "Scanning…" else "${library.tracks.size} on-device tracks",
                                11.sp, Muted, modifier = Modifier.weight(1f))
                            Chip("Sort: $sort") {
                                sort = when (sort) {
                                    "Title" -> "Artist"
                                    "Artist" -> "Album"
                                    "Album" -> "Duration"
                                    else -> "Title"
                                }
                            }
                            Chip("↻") { model.loadLocal() }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (filter == "Folders" && folder.isBlank()) {
                            val folders = library.tracks.map { it.folder.ifBlank { "Unknown folder" } }.distinct().sorted()
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(folders) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.folder.ifBlank { "Unknown folder" } == item }} tracks") {
                                        folder = item
                                    }
                                }
                            }
                        } else if (filter == "Albums" && album.isBlank()) {
                            val albums = library.tracks.map { it.album.ifBlank { "Unknown album" } }.distinct().sorted()
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(albums) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.album.ifBlank { "Unknown album" } == item }} tracks") {
                                        album = item
                                    }
                                }
                            }
                        } else if (filter == "Artists" && album.isBlank()) {
                            val artists = library.tracks.map { it.artist }.distinct().sorted()
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(artists) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.artist == item }} tracks") {
                                        album = item
                                    }
                                }
                            }
                        } else {
                            if ((filter == "Folders" && folder.isNotEmpty()) ||
                                ((filter == "Albums" || filter == "Artists") && album.isNotEmpty())) {
                                Chip("← Back") { folder = ""; album = "" }
                                Spacer(Modifier.height(8.dp))
                            }
                            val source = when (filter) {
                                "Favorites" -> favorites.filter { it.source == TrackSource.LOCAL }
                                "Recent" -> recent.filter { it.source == TrackSource.LOCAL }
                                "Top" -> topTracks.filter { it.source == TrackSource.LOCAL }
                                "Folders" -> library.tracks.filter { it.folder.ifBlank { "Unknown folder" } == folder }
                                "Albums" -> library.tracks.filter { it.album.ifBlank { "Unknown album" } == album }
                                "Artists" -> library.tracks.filter { it.artist == album }
                                "Duplicates" -> library.tracks.groupBy { Triple(it.title.lowercase(), it.artist.lowercase(), it.durationMs) }
                                    .filterValues { it.size > 1 }.values.flatten()
                                else -> library.tracks
                            }
                            val shown = remember(source, query, sort) {
                                source.filter {
                                    listOf(it.title, it.artist, it.album, it.folder, it.filename).any { field ->
                                        field.contains(query, ignoreCase = true)
                                    }
                                }.let { results -> when (sort) {
                                    "Artist" -> results.sortedBy { it.artist.lowercase() }
                                    "Album" -> results.sortedBy { it.album.lowercase() }
                                    "Duration" -> results.sortedByDescending { it.durationMs }
                                    else -> results.sortedBy { it.title.lowercase() }
                                } }
                            }
                            if (shown.isEmpty()) {
                                EmptyState("♪", "Nothing here yet", library.error ?: "Try another filter or scan again.") {
                                    model.loadLocal()
                                }
                            } else {
                                TrackList(shown, favoriteIds,
                                    onPlay = { model.playListTracks(shown, it) },
                                    onFavorite = model::toggleFavorite,
                                    onQueue = { model.playback.add(it, true) },
                                    onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                                    onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                            }
                        }
                    }
                }
                Page.ONLINE -> Column {
                    Header("Discover music", "Explore independent artists on Audius.")
                    Spacer(Modifier.height(13.dp))
                    SearchField(onlineQuery, "Search Audius tracks and artists", { onlineQuery = it },
                        { model.search(onlineQuery) })
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccentAction("Search Audius") { model.search(onlineQuery) }
                        Spacer(Modifier.width(10.dp))
                        Label(discover.message, 11.sp, Muted, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(15.dp))
                    if (discover.tracks.isEmpty()) {
                        EmptyState("♬", "Explore new sounds",
                            "Streaming requires internet. Your local library works offline.")
                    } else {
                        TrackList(discover.tracks, favoriteIds,
                            onPlay = model::playOnline,
                            onFavorite = model::toggleFavorite,
                            onQueue = { model.playback.add(it, true) },
                            onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                            onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                    }
                }
                Page.PLAYLISTS -> Column {
                    Header("My playlists", "Saved locally. No account required.")
                    Spacer(Modifier.height(12.dp))
                    if (selectedPlaylistId < 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                SearchField(playlistName, "New playlist name", { playlistName = it }, {})
                            }
                            Spacer(Modifier.width(8.dp))
                            AccentAction("Create") {
                                model.createPlaylist(playlistName)
                                playlistName = ""
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(playlists, key = { it.id }) { playlist ->
                                SimpleRow(playlist.name, "Tap to open") {
                                    selectedPlaylistId = playlist.id
                                    model.selectPlaylist(playlist.id)
                                }
                            }
                        }
                    } else {
                        val playlist = playlists.firstOrNull { it.id == selectedPlaylistId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Chip("← All") { selectedPlaylistId = -1L }
                            Spacer(Modifier.width(8.dp))
                            Label(playlist?.name ?: "Playlist", 18.sp, PrimaryText, FontWeight.SemiBold,
                                modifier = Modifier.weight(1f))
                            Chip("Import") { m3uImporter.launch(arrayOf("audio/x-mpegurl", "text/*", "*/*")) }
                            Chip("Export") { m3uExporter.launch("kanade-playlist.m3u") }
                            Chip("Delete") { model.deletePlaylist(selectedPlaylistId); selectedPlaylistId = -1L }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (selectedTracks.isEmpty()) EmptyState("♫", "Playlist is empty",
                            "Use the + button on a track to add music.")
                        else TrackList(selectedTracks, favoriteIds,
                            onPlay = { model.playListTracks(selectedTracks, it) },
                            onFavorite = model::toggleFavorite,
                            onQueue = { model.playback.add(it, true) },
                            onPlaylist = { model.removeFromPlaylist(selectedPlaylistId, it.id) },
                            playlistAction = "−",
                            onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                    }
                }
                Page.INSIGHTS -> Column {
                    Header("Your listening", "Private listening history, stored on this device.")
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(totalPlays.toString(), "Plays", Modifier.weight(1f))
                        StatCard("${totalListenMs / 3_600_000}h", "Listening time", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    Label("SMART COLLECTIONS", 11.sp, Accent, FontWeight.SemiBold)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (choice in listOf("Most played", "Recently played", "Favorites", "Recently added")) {
                            Chip(choice, selectedSmart == choice) { selectedSmart = choice }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val tracks = if (selectedSmart.isBlank()) recent else model.smartTracks(selectedSmart)
                    if (tracks.isEmpty()) EmptyState("◇", "Make some memories", "Play some tracks to build your personal history.")
                    else TrackList(tracks, favoriteIds,
                        onPlay = { model.playListTracks(tracks, it) },
                        onFavorite = model::toggleFavorite,
                        onQueue = { model.playback.add(it, true) },
                        onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST })
                }
            }
        }

        if (playback.hasItem) {
            Spacer(Modifier.height(7.dp))
            PlayerPanel(playback,
                onToggle = model.playback::toggle,
                onNext = model.playback::next,
                onPrevious = model.playback::previous,
                onShuffle = model.playback::toggleShuffle,
                onRepeat = model.playback::cycleRepeat,
                onSeek = model.playback::seek,
                onQueue = { overlay = Overlay.QUEUE },
                onLyrics = { overlay = Overlay.LYRICS },
                onAudio = { overlay = Overlay.AUDIO },
                onExpand = { overlay = Overlay.PLAYER })
        }
        playback.error?.let { message ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(message, 12.sp, Pink, modifier = Modifier.weight(1f))
                Chip("Dismiss") { model.playback.clearError() }
            }
        }
        Spacer(Modifier.height(9.dp))
    }

    when (overlay) {
        Overlay.NONE -> Unit
        Overlay.PLAYER -> FeatureDialog("Now playing", { overlay = Overlay.NONE }) {
            AlbumImage(playback.artworkUri, Modifier.fillMaxWidth().height(240.dp))
            Spacer(Modifier.height(16.dp))
            Label(playback.title, 22.sp, PrimaryText, FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Label(playback.artist, 15.sp, Muted)
            Spacer(Modifier.height(12.dp))
            PlayerPanel(playback,
                onToggle = model.playback::toggle,
                onNext = model.playback::next,
                onPrevious = model.playback::previous,
                onShuffle = model.playback::toggleShuffle,
                onRepeat = model.playback::cycleRepeat,
                onSeek = model.playback::seek,
                onQueue = { overlay = Overlay.QUEUE },
                onLyrics = { overlay = Overlay.LYRICS },
                onAudio = { overlay = Overlay.AUDIO },
                onExpand = {})
        }
        Overlay.QUEUE -> FeatureDialog("Up next", { overlay = Overlay.NONE }) {
            if (playback.queue.isEmpty()) Label("Queue is empty", 14.sp, Muted)
            else {
                Row {
                    Chip("Clear queue") { model.playback.clear(); overlay = Overlay.NONE }
                    Spacer(Modifier.width(8.dp))
                    Label("${playback.queue.size} tracks", 12.sp, Muted)
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().height(370.dp)) {
                    items(playback.queue.indices.toList()) { index ->
                        val entry = playback.queue[index]
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { model.playback.playAt(index) }) {
                                Label("${if (index == playback.index) "♪ " else ""}${entry.title}",
                                    13.sp, if (index == playback.index) Accent else PrimaryText,
                                    lines = 1)
                                Label(entry.artist, 11.sp, Muted, lines = 1)
                            }
                            if (index > 0) Chip("↑") { model.playback.move(index, index - 1) }
                            if (index < playback.queue.lastIndex) Chip("↓") { model.playback.move(index, index + 1) }
                            Chip("×") { model.playback.remove(index) }
                        }
                    }
                }
            }
        }
        Overlay.LYRICS -> FeatureDialog("Synchronized lyrics", { overlay = Overlay.NONE }) {
            AccentAction("Choose LRC file") { lyricsPicker.launch(arrayOf("text/*", "*/*")) }
            Spacer(Modifier.height(13.dp))
            if (lyrics.lines.isEmpty()) Label(lyrics.message, 14.sp, Muted)
            else {
                val active = LrcParser.current(lyrics.lines, playback.positionMs)
                val listState = rememberLazyListState()
                LaunchedEffect(active) { if (active >= 0) listState.animateScrollToItem(active.coerceAtLeast(0)) }
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    items(lyrics.lines.size) { i ->
                        val line = lyrics.lines[i]
                        Label(line.text, if (active == i) 19.sp else 15.sp,
                            if (active == i) PrimaryText else Muted,
                            if (active == i) FontWeight.Bold else FontWeight.Normal,
                            Modifier.fillMaxWidth().clickable { model.playback.seek(line.timeMs) })
                    }
                }
            }
        }
        Overlay.AUDIO -> FeatureDialog("Audio controls", { overlay = Overlay.NONE }) {
            Label("SLEEP TIMER", 12.sp, Accent, FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (minutes in listOf(0, 15, 30, 60, 90)) {
                    Chip(if (minutes == 0) "Off" else "${minutes}m") { model.playback.sleep(minutes) }
                }
            }
            Spacer(Modifier.height(17.dp))
            Label("EQUALIZER", 12.sp, Accent, FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                for (preset in listOf("Off", "Bass", "Vocal")) Chip(preset) { model.playback.equalizer(preset) }
            }
            Spacer(Modifier.height(17.dp))
            Label("PLAYBACK SPEED", 12.sp, Accent, FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                for (speed in listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) {
                    Chip("${speed}×", playback.speed == speed) { model.playback.setSpeed(speed) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Label("Gapless playback uses Media3's supported codec path. Equalizer effects depend on device audio routing.",
                12.sp, Muted)
        }
        Overlay.EDIT_TAGS -> FeatureDialog("Edit display metadata", { overlay = Overlay.NONE }) {
            val track = selectedTrack
            if (track != null && track.source == TrackSource.LOCAL) {
                var editTitle by remember(track.id) { mutableStateOf(track.title) }
                var editArtist by remember(track.id) { mutableStateOf(track.artist) }
                var editAlbum by remember(track.id) { mutableStateOf(track.album) }
                Label("Changes only affect Kanade's labels, not your original music file.", 12.sp, Muted)
                Spacer(Modifier.height(12.dp))
                SearchField(editTitle, "Title", { editTitle = it }, {})
                Spacer(Modifier.height(8.dp))
                SearchField(editArtist, "Artist", { editArtist = it }, {})
                Spacer(Modifier.height(8.dp))
                SearchField(editAlbum, "Album", { editAlbum = it }, {})
                Spacer(Modifier.height(14.dp))
                AccentAction("Save displayed metadata") {
                    model.editDisplayTags(track, editTitle, editArtist, editAlbum)
                    overlay = Overlay.NONE
                }
            }
        }
        Overlay.PICK_PLAYLIST -> FeatureDialog("Add to playlist", { overlay = Overlay.NONE }) {
            if (playlists.isEmpty()) Label("Create a playlist in the Playlists tab first.", 13.sp, Muted)
            for (playlist in playlists) {
                SimpleRow(playlist.name, "Add selected track") {
                    selectedTrack?.let { model.addToPlaylist(playlist.id, it) }
                    overlay = Overlay.NONE
                }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String) {
    Label(title, 25.sp, PrimaryText, FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Label(subtitle, 13.sp, Muted)
}

@Composable
private fun Label(
    text: String, size: TextUnit, color: Color = PrimaryText,
    weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier,
    lines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier = modifier, maxLines = lines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight))
}

@Composable private fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(13.dp))
        .background(if (selected) Accent else SurfaceLight)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Label(text, 12.sp, if (selected) Background else PrimaryText,
            if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun AccentAction(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(Accent)
        .clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 14.dp)) {
        Label(text, 12.sp, Background, FontWeight.Bold)
    }
}

@Composable
private fun SearchField(value: String, hint: String, change: (String) -> Unit, search: () -> Unit) {
    BasicTextField(
        value = value, onValueChange = change, singleLine = true,
        textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Surface).padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (value.isBlank()) Label(hint, 13.sp, Muted)
                inner()
            }
        },
    )
}

@Composable
private fun AlbumImage(artwork: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF4F3E70), Color(0xFF241E39)))),
        contentAlignment = Alignment.Center) {
        Label("♫", 28.sp, Accent)
        if (!artwork.isNullOrEmpty()) {
            AsyncImage(model = artwork, contentDescription = "Album cover",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>, favoriteIds: Set<String>,
    onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit,
    onQueue: (Track) -> Unit, onPlaylist: (Track) -> Unit,
    playlistAction: String = "+",
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tracks, key = { it.id }) { track ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Surface).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumImage(track.artworkUri, Modifier.size(54.dp).clickable { onPlay(track) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { onPlay(track) }) {
                    Label(track.title, 13.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
                    Label(track.artist, 11.sp, Muted, lines = 1)
                    Label(if (track.source == TrackSource.LOCAL) "ON DEVICE" else "AUDIUS STREAM",
                        9.sp, Accent)
                }
                TinyButton(if (track.id in favoriteIds) "♥" else "♡", "Favorite") { onFavorite(track) }
                TinyButton("⇥", "Play next") { onQueue(track) }
                TinyButton(playlistAction, "Playlist") { onPlaylist(track) }
                if (track.source == TrackSource.LOCAL) TinyButton("✎", "Edit display metadata") { onTags(track) }
            }
        }
    }
}

@Composable
private fun TinyButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(39.dp).clip(CircleShape).clickable(onClick = onClick)
        .semantics { contentDescription = description },
        contentAlignment = Alignment.Center) { Label(symbol, 18.sp, Accent) }
}

@Composable private fun SimpleRow(title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .background(Surface).clickable(onClick = click).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label(title, 14.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
            Label(subtitle, 11.sp, Muted, lines = 1)
        }
        Label("›", 24.sp, Accent)
    }
}

@Composable private fun StatCard(value: String, title: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(19.dp)).background(Surface).padding(18.dp)) {
        Label(value, 28.sp, Accent, FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Label(title, 12.sp, Muted)
    }
}

@Composable
private fun EmptyState(icon: String, title: String, detail: String, action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 45.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(23.dp)).background(SurfaceLight),
            contentAlignment = Alignment.Center) { Label(icon, 36.sp, Accent) }
        Spacer(Modifier.height(13.dp))
        Label(title, 17.sp, PrimaryText, FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Label(detail, 13.sp, Muted, modifier = Modifier.padding(horizontal = 25.dp))
        if (action != null) { Spacer(Modifier.height(16.dp)); AccentAction("Scan / allow") { action() } }
    }
}

@Composable
private fun PlayerPanel(
    state: PlaybackState, onToggle: () -> Unit, onNext: () -> Unit,
    onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit,
    onSeek: (Long) -> Unit, onQueue: () -> Unit,
    onLyrics: () -> Unit, onAudio: () -> Unit, onExpand: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF332640), Color(0xFF1C1B2B), Surface)))
        .padding(horizontal = 13.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumImage(state.artworkUri, Modifier.size(51.dp).clickable { onExpand() })
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).clickable { onExpand() }) {
                Label(state.title, 14.sp, PrimaryText, FontWeight.Bold, lines = 1)
                Label(state.artist, 12.sp, Muted, lines = 1)
            }
            TinyButton("⏮", "Previous") { onPrevious() }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Accent).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center) {
                Label(if (state.playing) "❚❚" else "▶", 17.sp, Background, FontWeight.Bold)
            }
            TinyButton("⏭", "Next") { onNext() }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(14.dp)
            .pointerInput(state.durationMs) {
                detectTapGestures { point ->
                    if (size.width > 0 && state.durationMs > 0L) {
                        onSeek((state.durationMs * (point.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
            }, contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceLight))
            Box(Modifier.fillMaxWidth(
                if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                else 0f
            ).height(4.dp).clip(CircleShape).background(Accent))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(DurationFormatter.format(state.positionMs), 10.sp, Muted)
            Spacer(Modifier.weight(1f))
            TinyButton("⇄", "Shuffle") { onShuffle() }
            Label(if (state.shuffle) "ON" else "OFF", 9.sp, if (state.shuffle) Accent else Muted)
            TinyButton(when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> "↻"
                Player.REPEAT_MODE_ONE -> "↺"
                else -> "×"
            }, "Repeat mode") { onRepeat() }
            TinyButton("☷", "Queue") { onQueue() }
            TinyButton("♫", "Lyrics") { onLyrics() }
            TinyButton("☷", "Audio settings") { onAudio() }
            Spacer(Modifier.weight(1f))
            Label(DurationFormatter.format(state.durationMs), 10.sp, Muted)
        }
    }
}

@Composable
private fun FeatureDialog(title: String, dismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
            .background(Color(0xFF1C1D2C)).padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(title, 20.sp, PrimaryText, FontWeight.Bold, Modifier.weight(1f))
                Chip("✕") { dismiss() }
            }
            Spacer(Modifier.height(15.dp))
            content()
        }
    }
}
