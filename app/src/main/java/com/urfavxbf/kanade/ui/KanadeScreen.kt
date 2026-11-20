package com.urfavxbf.kanade.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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

private data class KanadePalette(
    val background: Color, val backgroundTop: Color, val backgroundBottom: Color,
    val surface: Color, val accent: Color, val accentSoft: Color,
    val text: Color, val muted: Color, val error: Color,
    val tint: Color, val glass: Color, val glassBorder: Color,
    val artworkStart: Color, val artworkEnd: Color,
)

private val DarkPalette = KanadePalette(
    background = Color(0xFF07140E), backgroundTop = Color(0xFF183D2B),
    backgroundBottom = Color(0xFF091C13), surface = Color(0xFF243E30),
    accent = Color(0xFF80EAB1), accentSoft = Color(0xFF40B979),
    text = Color(0xFFF1FFF4), muted = Color(0xFFB1C9B9), error = Color(0xFFFFB4A6),
    tint = Color.White, glass = Color(0xFF173A29), glassBorder = Color.White,
    artworkStart = Color(0xFF386B4C), artworkEnd = Color(0xFF14291C),
)
private val LightPalette = KanadePalette(
    background = Color(0xFFF2FAF3), backgroundTop = Color(0xFFD9F1DF),
    backgroundBottom = Color(0xFFE9F6EB), surface = Color(0xFFD5EBDC),
    accent = Color(0xFF11603A), accentSoft = Color(0xFF318B59),
    text = Color(0xFF10281A), muted = Color(0xFF496455), error = Color(0xFF9F3027),
    tint = Color(0xFF144D2D), glass = Color(0xFFEAF7EC), glassBorder = Color(0xFF2C6D48),
    artworkStart = Color(0xFFB5DDC1), artworkEnd = Color(0xFF7EBA92),
)
private val LocalKanadePalette = staticCompositionLocalOf { DarkPalette }
private val Background: Color @Composable get() = LocalKanadePalette.current.background
private val SurfaceLight: Color @Composable get() = LocalKanadePalette.current.surface
private val Accent: Color @Composable get() = LocalKanadePalette.current.accent
private val PrimaryText: Color @Composable get() = LocalKanadePalette.current.text
private val Muted: Color @Composable get() = LocalKanadePalette.current.muted
private val ErrorColor: Color @Composable get() = LocalKanadePalette.current.error

private enum class Page { LIBRARY, ONLINE, PLAYLISTS, INSIGHTS }
private enum class Overlay { NONE, QUEUE, LYRICS, AUDIO, PICK_PLAYLIST, EDIT_TAGS }

@Composable
fun KanadeScreen(model: KanadeViewModel) {
    val darkTheme by model.darkTheme.collectAsState()
    CompositionLocalProvider(LocalKanadePalette provides if (darkTheme) DarkPalette else LightPalette) {
        KanadeContent(model, darkTheme)
    }
}

@Composable
private fun KanadeContent(model: KanadeViewModel, darkTheme: Boolean) {
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
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
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
    BackHandler(enabled = showNowPlaying && overlay == Overlay.NONE) { showNowPlaying = false }
    LaunchedEffect(playback.hasItem) { if (!playback.hasItem) showNowPlaying = false }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(LocalKanadePalette.current.backgroundTop, Background, LocalKanadePalette.current.backgroundBottom)))) {
    if (!showNowPlaying) Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Accent, LocalKanadePalette.current.accentSoft))),
                contentAlignment = Alignment.Center,
            ) { KanadeGlyph(KanadeIcon.MUSIC, Background, selected = true, size = 24.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Label("KANADE", 21.sp, PrimaryText, FontWeight.Bold)
                Label("Your sound. Your space.", 11.sp, Muted)
            }
            Chip(if (darkTheme) "Light mode" else "Dark mode") { model.toggleTheme() }
        }

        Spacer(Modifier.height(20.dp))

        Box(Modifier.weight(1f)) {
            when (page) {
                Page.LIBRARY -> Column {
                    Header("Your library", "Your music stays yours, even offline.")
                    Spacer(Modifier.height(12.dp))
                    if (!model.hasPermission()) {
                        EmptyState(KanadeIcon.MUSIC, "Grant music permission", "Browse audio stored on your device.") {
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
                            TinyButton(KanadeIcon.REFRESH, "Refresh library") { model.loadLocal() }
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
                                IconTextButton(KanadeIcon.BACK, "Back") { folder = ""; album = "" }
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
                                EmptyState(KanadeIcon.MUSIC, "Nothing here yet", library.error ?: "Try another filter or scan again.") {
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
                    Header("Online music", "Search YouTube and play inside Kanade")
                    Spacer(Modifier.height(10.dp))
                    SearchField(onlineQuery, "Search songs or artists…", { onlineQuery = it },
                        { model.search(onlineQuery) })
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccentAction("Search") { model.search(onlineQuery) }
                        Spacer(Modifier.width(10.dp))
                        Label(discover.message, 11.sp, Muted, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(13.dp))
                    if (discover.tracks.isEmpty() && !discover.hasMore) {
                        EmptyState(KanadeIcon.SEARCH,
                            if (discover.searching) "Searching…" else "Find music",
                            if (discover.searching) "Loading results from YouTube." else "Search for a song or artist to start listening.")
                    } else {
                        TrackList(discover.tracks, favoriteIds,
                            onPlay = model::playOnline,
                            onFavorite = model::toggleFavorite,
                            onQueue = { model.playback.add(it, true) },
                            onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                            onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS },
                            pagination = discover, onLoadMore = model::loadMoreSearchResults)
                    }
                }
                Page.PLAYLISTS -> Column {
                    Header("My playlists", "Saved locally. No account required.")
                    Spacer(Modifier.height(12.dp))
                    if (selectedPlaylistId < 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                SearchField(playlistName, "New playlist name", { playlistName = it }, {}, showSearchIcon = false)
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
                            IconTextButton(KanadeIcon.BACK, "All") { selectedPlaylistId = -1L }
                            Spacer(Modifier.width(8.dp))
                            Label(playlist?.name ?: "Playlist", 18.sp, PrimaryText, FontWeight.SemiBold,
                                modifier = Modifier.weight(1f), lines = 1)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Chip("Import") { m3uImporter.launch(arrayOf("audio/x-mpegurl", "text/*", "*/*")) }
                            Chip("Export") { m3uExporter.launch("kanade-playlist.m3u") }
                            Chip("Delete") { model.deletePlaylist(selectedPlaylistId); selectedPlaylistId = -1L }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (selectedTracks.isEmpty()) EmptyState(KanadeIcon.MUSIC, "Playlist is empty",
                            "Use the + button on a track to add music.")
                        else TrackList(selectedTracks, favoriteIds,
                            onPlay = { model.playListTracks(selectedTracks, it) },
                            onFavorite = model::toggleFavorite,
                            onQueue = { model.playback.add(it, true) },
                            onPlaylist = { model.removeFromPlaylist(selectedPlaylistId, it.id) },
                            removeFromPlaylist = true,
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
                    if (tracks.isEmpty()) EmptyState(KanadeIcon.INSIGHTS, "Make some memories", "Play some tracks to build your personal history.")
                    else TrackList(tracks, favoriteIds,
                        onPlay = { model.playListTracks(tracks, it) },
                        onFavorite = model::toggleFavorite,
                        onQueue = { model.playback.add(it, true) },
                        onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                        onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                }
            }
        }

        if (playback.hasItem) {
            Spacer(Modifier.height(7.dp))
            MiniPlayer(playback, onToggle = model.playback::toggle,
                onNext = model.playback::next, onExpand = { showNowPlaying = true })
        }
        playback.error?.let { message ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(message, 12.sp, ErrorColor, modifier = Modifier.weight(1f))
                Chip("Dismiss") { model.playback.clearError() }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().glassSurface(20.dp).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (tab in Page.entries) {
                val (label, icon) = when (tab) {
                    Page.LIBRARY -> "Library" to KanadeIcon.LIBRARY
                    Page.ONLINE -> "Online" to KanadeIcon.ONLINE
                    Page.PLAYLISTS -> "Playlists" to KanadeIcon.PLAYLIST
                    Page.INSIGHTS -> "Insights" to KanadeIcon.INSIGHTS
                }
                val active = page == tab
                Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                    .selectable(selected = active, role = Role.Tab, onClick = { page = tab })
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    KanadeGlyph(icon, if (active) Accent else Muted, selected = active)
                    Spacer(Modifier.height(4.dp))
                    Label(label, 11.sp, if (active) Accent else Muted,
                        if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    } else {
        NowPlayingScreen(playback,
            onClose = { showNowPlaying = false }, onToggle = model.playback::toggle,
            onNext = model.playback::next, onPrevious = model.playback::previous,
            onShuffle = model.playback::toggleShuffle, onRepeat = model.playback::cycleRepeat,
            onSeek = model.playback::seek, onQueue = { overlay = Overlay.QUEUE },
            onLyrics = { overlay = Overlay.LYRICS }, onAudio = { overlay = Overlay.AUDIO })
    }
    }

    when (overlay) {
        Overlay.NONE -> Unit
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
                                Label(entry.title,
                                    13.sp, if (index == playback.index) Accent else PrimaryText,
                                    lines = 1)
                                Label(entry.artist, 11.sp, Muted, lines = 1)
                            }
                            if (index > 0) TinyButton(KanadeIcon.UP, "Move up") { model.playback.move(index, index - 1) }
                            if (index < playback.queue.lastIndex) TinyButton(KanadeIcon.DOWN, "Move down") { model.playback.move(index, index + 1) }
                            TinyButton(KanadeIcon.CLOSE, "Remove from queue") { model.playback.remove(index) }
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
                SearchField(editTitle, "Title", { editTitle = it }, {}, showSearchIcon = false)
                Spacer(Modifier.height(8.dp))
                SearchField(editArtist, "Artist", { editArtist = it }, {}, showSearchIcon = false)
                Spacer(Modifier.height(8.dp))
                SearchField(editAlbum, "Album", { editAlbum = it }, {}, showSearchIcon = false)
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
        .background(if (selected) Accent else LocalKanadePalette.current.tint.copy(alpha = 0.08f))
        .border(1.dp, LocalKanadePalette.current.glassBorder.copy(alpha = if (selected) 0.0f else 0.14f), RoundedCornerShape(13.dp))
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
private fun SearchField(value: String, hint: String, change: (String) -> Unit, search: () -> Unit, showSearchIcon: Boolean = true) {
    BasicTextField(
        value = value, onValueChange = change, singleLine = true,
        textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(LocalKanadePalette.current.tint.copy(alpha = 0.07f))
                .border(1.dp, LocalKanadePalette.current.glassBorder.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (showSearchIcon) {
                    KanadeGlyph(KanadeIcon.SEARCH, Muted, size = 19.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isBlank()) Label(hint, 13.sp, Muted)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun AlbumImage(artwork: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(listOf(LocalKanadePalette.current.artworkStart, LocalKanadePalette.current.artworkEnd))),
        contentAlignment = Alignment.Center) {
        KanadeGlyph(KanadeIcon.MUSIC, Accent, selected = true, size = 28.dp)
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
    removeFromPlaylist: Boolean = false,
    onTags: (Track) -> Unit = {},
    pagination: DiscoverState? = null,
    onLoadMore: (() -> Unit)? = null,
) {
    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    val listState = rememberLazyListState()
    val nearEnd by remember(listState, tracks.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= (tracks.size - 4).coerceAtLeast(0)
        }
    }
    LaunchedEffect(nearEnd, pagination?.hasMore, pagination?.loadingMore, pagination?.loadMoreError, tracks.size) {
        if (nearEnd && pagination?.hasMore == true && !pagination.searching &&
            !pagination.loadingMore && pagination.loadMoreError == null) onLoadMore?.invoke()
    }
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(tracks, key = { it.id }) { track ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(LocalKanadePalette.current.tint.copy(alpha = 0.055f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumImage(track.artworkUri, Modifier.size(54.dp).clickable { onPlay(track) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { onPlay(track) }) {
                    Label(track.title, 13.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
                    Label(track.artist, 11.sp, Muted, lines = 1)
                    Label(when (track.source) { TrackSource.LOCAL -> "ON DEVICE"; TrackSource.AUDIUS -> "AUDIUS STREAM"; TrackSource.YOUTUBE -> "YOUTUBE" },
                        9.sp, Accent)
                }
                TinyButton(KanadeIcon.FAVORITE, if (track.id in favoriteIds) "Remove from favorites" else "Add to favorites", selected = track.id in favoriteIds) { onFavorite(track) }
                TinyButton(KanadeIcon.QUEUE, "Track actions for ${track.title}") { actionsTrack = track }
            }
        }
        if (pagination != null) item(key = "online-pagination-footer") {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    pagination.loadingMore -> Label("Loading more songs…", 12.sp, Muted)
                    pagination.loadMoreError != null -> {
                        Label(pagination.loadMoreError, 12.sp, Muted)
                        Spacer(Modifier.height(8.dp))
                        AccentAction("Try again") { onLoadMore?.invoke() }
                    }
                    !pagination.hasMore && tracks.isNotEmpty() -> Label("End of results", 12.sp, Muted)
                    pagination.hasMore -> Label("Loading next results…", 12.sp, Muted)
                }
            }
        }
    }
    actionsTrack?.let { track ->
        FeatureDialog(track.title, { actionsTrack = null }) {
            SimpleRow("Play next", "Add this song to the queue") { actionsTrack = null; onQueue(track) }
            Spacer(Modifier.height(7.dp))
            SimpleRow(if (removeFromPlaylist) "Remove from playlist" else "Add to playlist", "Manage your music") {
                actionsTrack = null; onPlaylist(track)
            }
            if (track.source == TrackSource.LOCAL) {
                Spacer(Modifier.height(7.dp))
                SimpleRow("Edit display metadata", "Change the labels shown in Kanade") { actionsTrack = null; onTags(track) }
            }
        }
    }
}

@Composable
private fun TinyButton(icon: KanadeIcon, description: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(39.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        KanadeGlyph(icon, if (selected) Accent else Muted, selected = selected, size = 21.dp)
    }
}

@Composable
private fun IconTextButton(icon: KanadeIcon, text: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceLight)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KanadeGlyph(icon, PrimaryText, size = 17.dp)
        Spacer(Modifier.width(5.dp))
        Label(text, 12.sp, PrimaryText)
    }
}

@Composable private fun SimpleRow(title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .background(LocalKanadePalette.current.tint.copy(alpha = 0.07f)).clickable(onClick = click).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label(title, 14.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
            Label(subtitle, 11.sp, Muted, lines = 1)
        }
        KanadeGlyph(KanadeIcon.FORWARD, Muted, size = 19.dp)
    }
}

@Composable private fun StatCard(value: String, title: String, modifier: Modifier) {
    Column(modifier.glassSurface(19.dp).padding(18.dp)) {
        Label(value, 28.sp, Accent, FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Label(title, 12.sp, Muted)
    }
}

@Composable
private fun EmptyState(icon: KanadeIcon, title: String, detail: String, action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 45.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(23.dp)).background(SurfaceLight),
            contentAlignment = Alignment.Center) { KanadeGlyph(icon, Accent, selected = true, size = 36.dp) }
        Spacer(Modifier.height(13.dp))
        Label(title, 17.sp, PrimaryText, FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Label(detail, 13.sp, Muted, modifier = Modifier.padding(horizontal = 25.dp))
        if (action != null) { Spacer(Modifier.height(16.dp)); AccentAction("Scan / allow") { action() } }
    }
}

@Composable
private fun Modifier.glassSurface(radius: androidx.compose.ui.unit.Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    val palette = LocalKanadePalette.current
    return clip(shape).background(Brush.linearGradient(listOf(
        palette.glass.copy(alpha = 0.94f), palette.glass.copy(alpha = 0.83f),
        palette.glass.copy(alpha = 0.94f))))
        .border(1.dp, palette.glassBorder.copy(alpha = 0.16f), shape)
}

@Composable
private fun MiniPlayer(state: PlaybackState, onToggle: () -> Unit, onNext: () -> Unit, onExpand: () -> Unit) {
    Column(Modifier.fillMaxWidth().glassSurface(18.dp)) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onExpand)
            .semantics { contentDescription = "Open now playing" }
            .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AlbumImage(state.artworkUri, Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Label(state.title, 14.sp, PrimaryText, FontWeight.Bold, lines = 1)
                Label(state.artist, 12.sp, Muted, lines = 1)
            }
            TinyButton(if (state.playing) KanadeIcon.PAUSE else KanadeIcon.PLAY,
                if (state.playing) "Pause" else "Play", selected = true, onClick = onToggle)
            TinyButton(KanadeIcon.NEXT, "Next", onClick = onNext)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(LocalKanadePalette.current.tint.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(if (state.durationMs > 0) {
                (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
            } else 0f).height(3.dp).background(Accent))
        }
    }
}

@Composable
private fun NowPlayingScreen(
    state: PlaybackState, onClose: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit,
    onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit, onSeek: (Long) -> Unit,
    onQueue: () -> Unit, onLyrics: () -> Unit, onAudio: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (!state.artworkUri.isNullOrBlank()) {
            AsyncImage(model = state.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(55.dp).alpha(0.16f))
        }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        val artworkSize = minOf((maxWidth - 48.dp).coerceAtLeast(0.dp), maxHeight * 0.40f, 360.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TinyButton(KanadeIcon.BACK, "Back to library", onClick = onClose)
                Spacer(Modifier.weight(1f))
                Label("NOW PLAYING", 11.sp, Muted, FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TinyButton(KanadeIcon.QUEUE, "Queue", onClick = onQueue)
            }
            Spacer(Modifier.height(20.dp))
            AlbumImage(state.artworkUri, Modifier.size(artworkSize))
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth()) {
                Label(state.title, 25.sp, PrimaryText, FontWeight.Bold, lines = 2)
                Spacer(Modifier.height(5.dp))
                Label(state.artist, 15.sp, Muted, lines = 1)
            }
            Spacer(Modifier.height(18.dp))
            PlaybackProgress(state, onSeek)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TinyButton(KanadeIcon.SHUFFLE, if (state.shuffle) "Shuffle on" else "Shuffle off",
                    selected = state.shuffle, onClick = onShuffle)
                TinyButton(KanadeIcon.PREVIOUS, "Previous", onClick = onPrevious)
                Box(Modifier.size(62.dp).clip(CircleShape).background(Accent)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = if (state.playing) "Pause" else "Play" },
                    contentAlignment = Alignment.Center) {
                    KanadeGlyph(if (state.playing) KanadeIcon.PAUSE else KanadeIcon.PLAY,
                        Background, selected = true, size = 27.dp)
                }
                TinyButton(KanadeIcon.NEXT, "Next", onClick = onNext)
                TinyButton(if (state.repeatMode == Player.REPEAT_MODE_ONE) KanadeIcon.REPEAT_ONE else KanadeIcon.REPEAT,
                    when (state.repeatMode) { Player.REPEAT_MODE_ONE -> "Repeat one"; Player.REPEAT_MODE_ALL -> "Repeat all"; else -> "Repeat off" },
                    selected = state.repeatMode != Player.REPEAT_MODE_OFF, onClick = onRepeat)
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().glassSurface(18.dp).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconTextButton(KanadeIcon.LYRICS, "Lyrics", onLyrics)
                IconTextButton(KanadeIcon.AUDIO, "Audio", onAudio)
                IconTextButton(KanadeIcon.QUEUE, "Queue", onQueue)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Label(it, 12.sp, ErrorColor)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    }
}

@Composable
private fun PlaybackProgress(state: PlaybackState, onSeek: (Long) -> Unit) {
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(24.dp)
        .semantics {
            contentDescription = "Playback position"
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            setProgress { value ->
                if (state.durationMs > 0) { onSeek((state.durationMs * value.coerceIn(0f, 1f)).toLong()); true }
                else false
            }
        }
        .pointerInput(state.durationMs) {
            detectTapGestures { point ->
                if (size.width > 0 && state.durationMs > 0L) {
                    onSeek((state.durationMs * (point.x / size.width).coerceIn(0f, 1f)).toLong())
                }
            }
        }, contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(LocalKanadePalette.current.tint.copy(alpha = 0.25f)))
        Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(Accent))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(DurationFormatter.format(state.positionMs), 10.sp, Muted)
        Label(DurationFormatter.format(state.durationMs), 10.sp, Muted)
    }
}

@Composable
private fun FeatureDialog(title: String, dismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.9f)
                .glassSurface(25.dp).verticalScroll(rememberScrollState()).padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label(title, 20.sp, PrimaryText, FontWeight.Bold, Modifier.weight(1f), lines = 2)
                    TinyButton(KanadeIcon.CLOSE, "Close dialog") { dismiss() }
                }
                Spacer(Modifier.height(15.dp))
                content()
            }
        }
    }
}
