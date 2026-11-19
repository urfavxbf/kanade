package com.urfavxbf.kanade.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import com.urfavxbf.kanade.data.YouTubeLink
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.ui.platform.LocalContext
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
import com.urfavxbf.kanade.data.YouTubeVideo
import com.urfavxbf.kanade.lyrics.LrcParser
import com.urfavxbf.kanade.data.MusicCatalogLinks
import com.urfavxbf.kanade.data.MusicCatalogProvider
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import com.urfavxbf.kanade.playback.PlaybackState

private val Background = Color(0xFF141513)
internal val Surface = Color(0xFF20211F)
private val SurfaceLight = Color(0xFF323430)
internal val Accent = Color(0xFFFFAB7D)
private val Pink = Color(0xFFFFC8A7)
internal val PrimaryText = Color(0xFFFAF4E8)
internal val Muted = Color(0xFFBCBDB2)

private fun openSafeLink(context: android.content.Context, url: String) {
    if (url.startsWith("https://")) runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private enum class Page { HOME, LIBRARY, ONLINE, PLAYLISTS, INSIGHTS }
private enum class Overlay { NONE, PLAYER, QUEUE, LYRICS, AUDIO, PICK_PLAYLIST, EDIT_TAGS }

@Composable
fun KanadeScreen(model: KanadeViewModel) {
    val context = LocalContext.current
    val library by model.library.collectAsState()
    val discover by model.discover.collectAsState()
    val youtube by model.youtube.collectAsState()
    val youtubeConfigured by model.youtubeConfigured.collectAsState()
    val youtubeConsent by model.youtubeConsent.collectAsState()
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

    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var query by rememberSaveable { mutableStateOf("") }
    var onlineQuery by rememberSaveable { mutableStateOf("") }
    var onlineProvider by rememberSaveable { mutableStateOf("Audius") }
    var audioQuery by rememberSaveable { mutableStateOf("") }
    var youtubeKeyDialog by rememberSaveable { mutableStateOf(false) }
    var searchAfterKey by rememberSaveable { mutableStateOf(false) }
    var keyError by remember { mutableStateOf(false) }
    var youtubeKeyInput by remember { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }
    var selectedPlaylistId by rememberSaveable { mutableStateOf(-1L) }
    var selectedSmart by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var folder by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Title") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    val queuedVideos by model.videoQueue.videos.collectAsState()
    var showVideoQueue by rememberSaveable { mutableStateOf(false) }
    var watchIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var pendingMix by rememberSaveable { mutableStateOf(false) }
    var pendingSearch by rememberSaveable { mutableStateOf(false) }
    var setup by rememberSaveable { mutableStateOf(false) }
    fun watch(ids: List<String>) {
        model.playback.pause()
        watchIds = ArrayList(ids.distinct())
    }
    fun playStarterMix() {
        watch(listOf("5NV6Rdv1a3I", "djV11Xbc914", "dQw4w9WgXcQ"))
    }
    fun searchVideo() {
        val id = YouTubeLink.videoId(onlineQuery)
        when {
            !youtubeConsent -> { pendingSearch = true; setup = true }
            id != null -> watch(listOf(id))
            !youtubeConfigured -> { searchAfterKey = true; youtubeKeyDialog = true }
            else -> model.searchYouTube(onlineQuery)
        }
    }
    fun findMusicVideo(title: String, artist: String) {
        overlay = Overlay.NONE
        page = Page.ONLINE
        onlineProvider = "YouTube"
        onlineQuery = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ").take(120)
        searchVideo()
    }
    BackHandler(page == Page.PLAYLISTS && selectedPlaylistId >= 0) { selectedPlaylistId = -1L }
    BackHandler(page == Page.LIBRARY && (album.isNotEmpty() || folder.isNotEmpty())) { album = ""; folder = "" }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) model.loadLocal()
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(model::openAudio)
    }
    BackHandler(page == Page.INSIGHTS) { page = Page.HOME }
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
        Modifier.fillMaxSize().background(Background).safeDrawingPadding().imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("kanade", 27.sp, PrimaryText, FontWeight.Black, Modifier.weight(1f))
            if (page == Page.ONLINE && onlineProvider == "YouTube") Chip("YouTube settings") { setup = true }
            else if (page == Page.HOME) Chip("Listening history") { page = Page.INSIGHTS }
        }
        Box(Modifier.weight(1f)) {
            when (page) {
                Page.HOME -> {
                    val openError by model.audioOpenError.collectAsState()
                    MusicHome(
                        playback = playback,
                        recent = recent,
                        favorites = favorites,
                        songs = library.tracks,
                        openError = openError,
                        pickFile = { audioPicker.launch(arrayOf("audio/*")) },
                        showLibrary = { page = Page.LIBRARY },
                        discover = { onlineProvider = "Audius"; page = Page.ONLINE },
                        openPlayer = { overlay = Overlay.PLAYER },
                        toggle = model.playback::toggle,
                        play = model::playListTracks,
                    )
                }
                Page.LIBRARY -> Column {
                    Header("Record collection", "Songs, artists and albums on your device.")
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
                            AlbumShelf(library.tracks, query) { album = it; query = "" }
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
                                    onQueue = model.playback::add,
                                    onFindVideo = { findMusicVideo(it.title, it.artist) },
                                    onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                                    onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                            }
                        }
                    }
                }
                Page.ONLINE -> Column {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Songs", onlineProvider == "Audius") { onlineProvider = "Audius" }
                        Chip("Music videos", onlineProvider == "YouTube") { onlineProvider = "YouTube" }
                        if (onlineProvider == "YouTube") Chip("Video queue · ${queuedVideos.size}") {
                            showVideoQueue = true
                            if (!youtubeConsent) setup = true
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    if (onlineProvider == "YouTube") {
                        val listState = rememberLazyListState()
                        LaunchedEffect(youtube.results) {
                            if (youtube.results.isNotEmpty()) listState.scrollToItem(5)
                        }
                        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { RecordSleeve {
                                if (youtubeConsent) playStarterMix() else { pendingMix = true; setup = true }
                            } }
                            item { Header("Find your music", "Search the Philippines catalog or paste a video link.") }
                            item { SearchField(onlineQuery, "Song, artist or YouTube link", { onlineQuery = it }, ::searchVideo) }
                            item {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("OPM", "BINI", "SB19", "Ben&Ben", "Pinoy indie").forEach { term ->
                                        Chip(term, onlineQuery == term) { onlineQuery = term; searchVideo() }
                                    }
                                }
                            }
                            item { AccentAction(if (YouTubeLink.videoId(onlineQuery) != null) "Open video" else "Search YouTube") { searchVideo() } }
                            item { Label(youtube.message, 13.sp, Muted) }
                            items(youtube.results, key = { it.id }) { video ->
                                YouTubeResultRow(video, queuedVideos.any { it.id == video.id }, { model.videoQueue.add(video) }) {
                                    val selection = youtube.results.dropWhile { it.id != video.id }
                                    watch(selection.map { it.id })
                                }
                            }
                        }
                    } else {
                        Header("Discover songs", "Search artists and tracks on Audius.")
                        Spacer(Modifier.height(10.dp))
                        SearchField(audioQuery, "Search for a song or artist…", { audioQuery = it },
                            { model.search(audioQuery) })
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccentAction("Search songs") { model.search(audioQuery) }
                            Spacer(Modifier.width(10.dp))
                            Label(discover.message, 11.sp, Muted, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(13.dp))
                        if (discover.tracks.isEmpty()) {
                            EmptyState("♬", "Explore Audius",
                                "These tracks can play directly in Kanade. OPM availability depends on the Audius catalog.")
                        } else {
                            TrackList(discover.tracks, favoriteIds,
                                onPlay = model::playOnline,
                                onFavorite = model::toggleFavorite,
                                onQueue = model.playback::add,
                                onFindVideo = { findMusicVideo(it.title, it.artist) },
                                onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                                onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                        }
                    }
                }
                Page.PLAYLISTS -> Column {
                    Header("Playlists", "Your handpicked tracks, saved on this device.")
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
                        if (playlists.isEmpty()) EmptyState("≡", "Create your first playlist", "Give it a name above, then add songs from your library.")
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
                            Chip("Delete") { confirmDelete = true }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (selectedTracks.isEmpty()) EmptyState("♫", "Playlist is empty",
                            "Open a track’s actions and choose Add to playlist.")
                        else TrackList(selectedTracks, favoriteIds,
                            onPlay = { model.playListTracks(selectedTracks, it) },
                            onFavorite = model::toggleFavorite,
                            onQueue = model.playback::add,
                            onFindVideo = { findMusicVideo(it.title, it.artist) },
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
                        onQueue = model.playback::add,
                        onFindVideo = { findMusicVideo(it.title, it.artist) },
                        onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                        onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                }
            }
        }

        if (playback.hasItem) {
            Spacer(Modifier.height(7.dp))
            MiniPlayer(playback, { overlay = Overlay.PLAYER }, model.playback::toggle, model.playback::next)
        }
        playback.error?.let { message ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(message, 12.sp, Pink, modifier = Modifier.weight(1f))
                Chip("Dismiss") { model.playback.clearError() }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            listOf(Page.HOME, Page.LIBRARY, Page.ONLINE, Page.PLAYLISTS).forEach { tab ->
                val active = tab == page || (tab == Page.HOME && page == Page.INSIGHTS)
                val title = when (tab) {
                    Page.HOME -> "Home"
                    Page.ONLINE -> "Discover"
                    Page.LIBRARY -> "Library"
                    Page.PLAYLISTS -> "Playlists"
                    Page.INSIGHTS -> "Insights"
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) SurfaceLight else Background)
                    .selectable(active, role = Role.Tab) { page = tab }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(title, 13.sp, if (active) Accent else Muted, FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(18.dp).height(2.dp).background(if (active) Accent else Color.Transparent))
                }
            }
        }
    }

    if (confirmDelete) FeatureDialog("Delete playlist?", { confirmDelete = false }) {
        Label("Remove this playlist from your library? Your tracks will stay on the device.", 14.sp, Muted)
        Spacer(Modifier.height(12.dp))
        AccentAction("Delete playlist") {
            model.deletePlaylist(selectedPlaylistId)
            selectedPlaylistId = -1L
            confirmDelete = false
        }
    }
    if (youtubeConsent && showVideoQueue) FeatureDialog("YouTube queue", { showVideoQueue = false }) {
        var link by rememberSaveable { mutableStateOf("") }
        var linkMessage by rememberSaveable { mutableStateOf("") }
        fun addLink() {
            val id = YouTubeLink.videoId(link)
            if (id == null) {
                linkMessage = "Paste a YouTube video link."
                return
            }
            if (queuedVideos.any { it.id == id }) linkMessage = "Already in the queue."
            else {
                model.videoQueue.add(YouTubeVideo(id, "YouTube video · $id", "", ""))
                linkMessage = "Video added."
            }
            link = ""
        }
        SearchField(link, "Paste a YouTube video link", { link = it; linkMessage = "" }, ::addLink)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Add link", onClick = ::addLink)
            if (queuedVideos.isNotEmpty()) AccentAction("Play queue") {
                showVideoQueue = false
                watch(queuedVideos.map { it.id })
            }
        }
        if (linkMessage.isNotEmpty()) Label(linkMessage, 12.sp, Accent)
        Spacer(Modifier.height(12.dp))
        if (queuedVideos.isEmpty()) Label("Add a link above or choose + Queue on a search result.", 14.sp, Muted)
        else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(queuedVideos.size, key = { queuedVideos[it].id }) { index ->
                val video = queuedVideos[index]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Label("${index + 1}. ${video.title}", 14.sp, PrimaryText, modifier = Modifier.weight(1f), lines = 2)
                    if (index > 0) TinyButton("↑", "Move ${video.title} earlier") { model.videoQueue.move(video.id, -1) }
                    if (index < queuedVideos.lastIndex) TinyButton("↓", "Move ${video.title} later") { model.videoQueue.move(video.id, 1) }
                    TinyButton("×", "Remove ${video.title}") { model.videoQueue.remove(video.id) }
                }
            }
        }
    }
    if (youtubeConsent && watchIds.isNotEmpty()) FeatureDialog("YouTube player", { watchIds = arrayListOf() }) {
        YouTubePlayer(watchIds)
    }
    if (setup) FeatureDialog("YouTube settings", { setup = false; pendingSearch = false; pendingMix = false; showVideoQueue = false }) {
        Label("Search uses your Google API key. Opening a video loads YouTube’s embedded player and connects to Google.", 14.sp, Muted)
        Spacer(Modifier.height(12.dp))
        SimpleRow("Privacy policy", "Kanade") { openSafeLink(context, "https://github.com/urfavxbf/kanade/blob/main/docs/privacy-policy.md") }
        SimpleRow("Terms of use", "Kanade") { openSafeLink(context, "https://github.com/urfavxbf/kanade/blob/main/docs/terms.md") }
        SimpleRow("Terms of Service", "YouTube") { openSafeLink(context, "https://www.youtube.com/t/terms") }
        SimpleRow("Privacy policy", "Google") { openSafeLink(context, "https://policies.google.com/privacy") }
        Spacer(Modifier.height(12.dp))
        if (!youtubeConsent) AccentAction("Agree and continue") {
            model.setYouTubePrivacyConsent(true)
            setup = false
            if (pendingMix) {
                pendingMix = false
                playStarterMix()
            } else if (pendingSearch) {
                pendingSearch = false
                val id = YouTubeLink.videoId(onlineQuery)
                if (id != null) watch(listOf(id))
                else if (!youtubeConfigured) { searchAfterKey = true; youtubeKeyDialog = true }
                else model.searchYouTube(onlineQuery)
            }
        } else {
            AccentAction(if (youtubeConfigured) "Change search key" else "Add search key") { setup = false; youtubeKeyDialog = true }
            Spacer(Modifier.height(8.dp))
            Chip("Clear key and consent") { watchIds = arrayListOf(); model.clearYouTubeConfiguration(); setup = false }
        }
    }
    if (youtubeKeyDialog) FeatureDialog("YouTube Data API key", { youtubeKeyDialog = false; youtubeKeyInput = ""; searchAfterKey = false; keyError = false }) {
        Label("Create a YouTube Data API v3 key in Google Cloud Console. Restrict it to YouTube Data API and the Android app's signing certificate.",
            12.sp, Muted)
        Spacer(Modifier.height(8.dp))
        Label("Android app restriction", 13.sp, Accent, FontWeight.Bold)
        SelectionContainer { Label(context.packageName, 14.sp, PrimaryText) }
        val fingerprint = remember(model) { model.youtubeKeyFingerprint() }
        if (fingerprint != null) {
            Label("SHA-1 of this installation", 12.sp, Muted)
            SelectionContainer { Label(fingerprint, 14.sp, PrimaryText) }
        }
        Spacer(Modifier.height(8.dp))
        Chip("Google Cloud credentials ↗") {
            openSafeLink(context, "https://console.cloud.google.com/apis/credentials")
        }
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = youtubeKeyInput, onValueChange = { youtubeKeyInput = it.take(180); keyError = false },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            decorationBox = { input ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SurfaceLight).padding(15.dp)) {
                    if (youtubeKeyInput.isEmpty()) Label("Paste Google API key", 13.sp, Muted)
                    input()
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        if (keyError) Label("Enter the API key from Google Cloud Credentials.", 13.sp, Pink)
        AccentAction("Save key on this device") {
            if (model.configureYouTubeKey(youtubeKeyInput)) {
                youtubeKeyInput = ""
                youtubeKeyDialog = false
                keyError = false
                if (searchAfterKey) model.searchYouTube(onlineQuery)
                searchAfterKey = false
            } else keyError = true
        }
        Spacer(Modifier.height(9.dp))
        Label("No key is bundled with the APK. This setting is local to your device. YouTube searches consume your project's API quota.",
            11.sp, Muted)
    }

    when (overlay) {
        Overlay.NONE -> Unit
        Overlay.PLAYER -> Dialog(onDismissRequest = { overlay = Overlay.NONE },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Background).safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Label("Now playing", 20.sp, PrimaryText, FontWeight.Bold, Modifier.weight(1f))
                    Chip("Close") { overlay = Overlay.NONE }
                }
                Spacer(Modifier.height(16.dp))
                AlbumImage(playback.artworkUri, Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(16.dp))
                Label(playback.title, 22.sp, PrimaryText, FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Label(playback.artist, 15.sp, Muted)
                Spacer(Modifier.height(12.dp))
                TransportControls(playback, model, { overlay = it })
                Spacer(Modifier.height(12.dp))
                SimpleRow("Find music video", "Search YouTube") { findMusicVideo(playback.title, playback.artist) }
            }
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
            Label(if (playback.sleepMinutesRemaining > 0) "SLEEP TIMER · ${playback.sleepMinutesRemaining} MIN LEFT" else "SLEEP TIMER · OFF", 12.sp, Accent, FontWeight.SemiBold)
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
                for (preset in listOf("Off", "Bass", "Vocal")) Chip(preset, playback.equalizerPreset == preset) { model.playback.equalizer(preset) }
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
internal fun Label(
    text: String, size: TextUnit, color: Color = PrimaryText,
    weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier,
    lines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier = modifier, maxLines = lines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight))
}

@Composable internal fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(13.dp))
        .background(if (selected) Accent else SurfaceLight)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Label(text, 12.sp, if (selected) Background else PrimaryText,
            if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun AccentAction(text: String, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(13.dp)).background(Accent)
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
internal fun AlbumImage(artwork: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF735243), Color(0xFF392E26)))),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * .4f
            drawCircle(Color(0xFF23241F), radius)
            for (ring in 4..9) drawCircle(Color(0xFF46443C), radius * ring / 10f, style = Stroke(1.dp.toPx()))
            drawCircle(Accent, radius * .34f)
            drawCircle(Background, radius * .06f)
        }
        if (!artwork.isNullOrEmpty()) {
            AsyncImage(model = artwork, contentDescription = "Album cover",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AlbumShelf(tracks: List<Track>, query: String, select: (String) -> Unit) {
    val albums = remember(tracks) {
        tracks.groupBy { it.album.ifBlank { "Unknown album" } }
            .entries.sortedBy { it.key.lowercase() }
    }
    val matches = remember(albums, query) {
        albums.filter { (name, songs) ->
            name.contains(query, ignoreCase = true) || songs.any { song ->
                listOf(song.artist, song.title, song.filename, song.folder).any { it.contains(query, ignoreCase = true) }
            }
        }
    }
    if (matches.isEmpty()) {
        EmptyState("♪", "No albums found", if (query.isBlank()) "Scan local music to browse album covers." else "Try another artist, song or album.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(matches, key = { it.key }) { (name, songs) ->
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Surface)
                .clickable(role = Role.Button) { select(name) }.padding(9.dp)) {
                AlbumImage(songs.firstNotNullOfOrNull { it.artworkUri }, Modifier.fillMaxWidth().aspectRatio(1f))
                Column(Modifier.padding(start = 3.dp, top = 8.dp, bottom = 4.dp)) {
                    Label(name, 15.sp, PrimaryText, FontWeight.Bold, lines = 2)
                    val artist = songs.first().artist
                    val credit = if (songs.all { it.artist == artist }) artist else "Various artists"
                    Label(credit, 12.sp, Muted, lines = 1)
                    Label(if (songs.size == 1) "1 track" else "${songs.size} tracks", 11.sp, Accent)
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>, favoriteIds: Set<String>,
    onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit,
    onQueue: (Track, Boolean) -> Unit, onPlaylist: (Track) -> Unit,
    onFindVideo: (Track) -> Unit,
    playlistAction: String = "+",
    onTags: (Track) -> Unit = {},
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
                var menu by remember { mutableStateOf(false) }
                TinyButton("⋯", "More actions for ${track.title}") { menu = true }
                if (menu) FeatureDialog(track.title, { menu = false }) {
                    SimpleRow("Find music video", "Search YouTube") { menu = false; onFindVideo(track) }
                    SimpleRow("Play next", track.artist) { menu = false; onQueue(track, true) }
                    SimpleRow("Add to end of queue", track.artist) { menu = false; onQueue(track, false) }
                    SimpleRow(if (playlistAction == "−") "Remove from playlist" else "Add to playlist", track.artist) { menu = false; onPlaylist(track) }
                    if (track.source == TrackSource.LOCAL) SimpleRow("Edit track details", track.album) { menu = false; onTags(track) }
                }
            }
        }
    }
}

@Composable
private fun TinyButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick)
        .semantics { contentDescription = description },
        contentAlignment = Alignment.Center) { Label(symbol, 18.sp, Accent) }
}

@Composable internal fun SimpleRow(title: String, subtitle: String, click: () -> Unit) {
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
private fun TransportControls(state: PlaybackState, model: KanadeViewModel, open: (Overlay) -> Unit) {
    val player = model.playback
    val accentColor = android.graphics.Color.rgb(255, 171, 125)
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        factory = { context -> android.widget.SeekBar(context).apply {
            max = 1000
            contentDescription = "Playback position"
            progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            thumbTintList = progressTintList
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: android.widget.SeekBar, position: Int, fromUser: Boolean) {
                    if (fromUser) player.seek(player.state.value.durationMs * position / bar.max)
                }
                override fun onStartTrackingTouch(bar: android.widget.SeekBar) = Unit
                override fun onStopTrackingTouch(bar: android.widget.SeekBar) = Unit
            })
        } },
        update = { bar ->
            bar.isEnabled = state.durationMs > 0
            if (!bar.isPressed) bar.progress = (state.positionMs * bar.max / state.durationMs.coerceAtLeast(1)).toInt()
        }
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(DurationFormatter.format(state.positionMs), 12.sp, Muted)
        Label(DurationFormatter.format(state.durationMs), 12.sp, Muted)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically) {
        TinyButton("⏮", "Previous track", player::previous)
        AccentAction(if (state.playing) "Pause" else "Play", player::toggle)
        TinyButton("⏭", "Next track", player::next)
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("Shuffle", state.shuffle, player::toggleShuffle)
        val repeat = when (state.repeatMode) {
            Player.REPEAT_MODE_ALL -> "Repeat all"
            Player.REPEAT_MODE_ONE -> "Repeat one"
            else -> "Repeat off"
        }
        Chip(repeat, state.repeatMode != Player.REPEAT_MODE_OFF, player::cycleRepeat)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("Up next") { open(Overlay.QUEUE) }
        Chip("Lyrics") { open(Overlay.LYRICS) }
        Chip("Sound") { open(Overlay.AUDIO) }
    }
}

@Composable
private fun FeatureDialog(title: String, dismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
            .background(Color(0xFF252621)).padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(title, 20.sp, PrimaryText, FontWeight.Bold, Modifier.weight(1f))
                Chip("✕") { dismiss() }
            }
            Spacer(Modifier.height(15.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) { content() }
        }
    }
}

@Composable
private fun YouTubeResultRow(video: YouTubeVideo, queued: Boolean, enqueue: () -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Surface).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = video.thumbnail,
            contentDescription = "YouTube thumbnail for ${video.title}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 108.dp, height = 69.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Label(video.title, 13.sp, PrimaryText, FontWeight.SemiBold, lines = 2)
            Spacer(Modifier.height(3.dp))
            Label(video.channelTitle, 11.sp, Muted, lines = 1)
            Label("YouTube", 11.sp, Accent)
        }
        Column(horizontalAlignment = Alignment.End) {
            Chip("Play", onClick = onOpen)
            if (queued) Label("Queued", 12.sp, Accent, modifier = Modifier.padding(8.dp))
            else Chip("+ Queue", onClick = enqueue)
        }
    }
}

@Composable
private fun RecordSleeve(play: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp))
        .background(Color(0xFFE89265)).clickable(role = Role.Button, onClickLabel = "Play Pop essentials on YouTube", onClick = play)) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height * .81f
            val center = Offset(size.width * .94f, size.height * .58f)
            drawCircle(Color(0xFF242521), radius, center)
            for (ring in 10..26) drawCircle(Color(0xFF45443C), radius * ring / 27f, center, style = Stroke(1.dp.toPx()))
            drawCircle(Color(0xFFF5CEA3), radius * .32f, center)
            drawCircle(Color(0xFF242521), radius * .045f, center)
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) {
            Label("YOUTUBE  /  3 VIDEOS", 10.sp, Background, FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BasicText("Pop\nessentials", style = TextStyle(color = Background, fontSize = 41.sp,
                lineHeight = 39.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(7.dp))
            Label("▶   Play mix", 13.sp, Background, FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniPlayer(state: PlaybackState, expand: () -> Unit, toggle: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLight).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = expand), verticalAlignment = Alignment.CenterVertically) {
            AlbumImage(state.artworkUri, Modifier.size(46.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Label(state.title, 14.sp, PrimaryText, FontWeight.Bold, lines = 1)
                Label(state.artist, 12.sp, Muted, lines = 1)
            }
        }
        TinyButton(if (state.playing) "Ⅱ" else "▶", if (state.playing) "Pause" else "Play", toggle)
        TinyButton("›", "Next track", next)
    }
}
