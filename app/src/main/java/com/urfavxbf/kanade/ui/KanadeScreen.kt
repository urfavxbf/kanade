package com.urfavxbf.kanade.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.combinedClickable
import android.net.Uri
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.core.tween
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.clipToBounds
import androidx.media3.common.VideoSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import com.urfavxbf.kanade.data.MusicMoods
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.urfavxbf.kanade.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.urfavxbf.kanade.data.YouTubeMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import com.urfavxbf.kanade.lyrics.OnlineLyrics
import com.urfavxbf.kanade.lyrics.LrcParser
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import com.urfavxbf.kanade.playback.PlaybackState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

private val appFonts = linkedMapOf(
    "Kanade Sans" to FontFamily.SansSerif,
    "Open Sans" to FontFamily(Font(R.font.open_sans)),
    "System" to FontFamily.Default,
    "Serif" to FontFamily.Serif,
    "Monospace" to FontFamily.Monospace,
)
private val LocalReduceMotion = staticCompositionLocalOf { false }
private val LocalAppFont = staticCompositionLocalOf { appFonts.getValue("Kanade Sans") }

internal data class KanadePalette(
    val background: Color, val backgroundTop: Color, val backgroundBottom: Color,
    val surface: Color, val accent: Color, val accentSoft: Color,
    val text: Color, val muted: Color, val error: Color,
    val tint: Color, val glass: Color, val glassBorder: Color,
    val artworkStart: Color, val artworkEnd: Color,
    val flat: Boolean = false,
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
private const val PLAIN = "Plain"
private const val PLAIN_DARK = "Plain Dark"
private const val PLAIN_LIGHT = "Plain Light"

private fun plainPalette(dark: Boolean): KanadePalette {
    val background = if (dark) Color(0xFF141414) else Color(0xFFFAFAFA)
    val surface = if (dark) Color(0xFF262626) else Color.White
    val text = if (dark) Color(0xFFF5F5F5) else Color(0xFF202020)
    val secondary = if (dark) Color(0xFFBBBBBB) else Color(0xFF555555)
    return KanadePalette(
        background = background, backgroundTop = background, backgroundBottom = background,
        surface = surface, accent = text, accentSoft = secondary, text = text, muted = secondary,
        error = if (dark) DarkPalette.error else LightPalette.error, tint = text,
        glass = surface, glassBorder = secondary, artworkStart = surface, artworkEnd = surface,
        flat = true,
    )
}

// The references use different palettes; offer each as a separate choice while keeping text legible.
internal val colorThemes = linkedMapOf(
    PLAIN_DARK to Color(0xFF141414),
    PLAIN_LIGHT to Color(0xFFFAFAFA),
    "Kanade Green" to Color(0xFF40B979),
    "Moss" to Color(0xFF5F7A52),
    "Forest" to Color(0xFFC8E64A),
    "Pistachio" to Color(0xFFC5DE8E),
    "Olive" to Color(0xFF5B6236),
    "Aubergine" to Color(0xFF8B598C),
    "Lilac" to Color(0xFFCDB8F0),
    "Lavender" to Color(0xFFC9B8F0),
    "Ink Blue" to Color(0xFF405A9C),
    "Cobalt" to Color(0xFF354AC5),
    "Neon Rose" to Color(0xFFFF3D6E),
    "Cherry Cola" to Color(0xFF9C3540),
    "Butter" to Color(0xFFF2E2A0),
    "Espresso" to Color(0xFF795548),
    "Glacier" to Color(0xFF2F6BFF),
    "Solar Flare" to Color(0xFFFF8A00),
    "Lime Soda" to Color(0xFF2BE0A0),
    "Ultraviolet" to Color(0xFF8A2BFF),
    "Fog" to Color(0xFFA9B4C6),
    "Dusk" to Color(0xFF8D99AE),
    "Apricot" to Color(0xFFEE7D52),
    "Papaya" to Color(0xFFFF8000),
    "Verde" to Color(0xFF4FB81C),
    "French Blue" to Color(0xFF1560D4),
    "Rosso" to Color(0xFFD40000),
)

/**
 * 60/30/10 theme system:
 * 60% dominant neutral canvas, 30% coordinated surface/chrome,
 * 10% saturated accent reserved for progress, selection and actions.
 * These percentages describe visual hierarchy, not literal pixel counts.
 */
internal fun paletteFor(name: String, dark: Boolean): KanadePalette {
    if (name == PLAIN || name == PLAIN_DARK || name == PLAIN_LIGHT) {
        return plainPalette(when (name) {
            PLAIN_DARK -> true
            PLAIN_LIGHT -> false
            else -> dark
        })
    }
    val seed = colorThemes[name] ?: colorThemes.getValue("Kanade Green")
    val base = if (dark) DarkPalette else LightPalette
    val dominant = if (dark) Color(0xFF050606) else Color(0xFFF7F8FA)
    val secondary = if (dark) Color(0xFF1B1D20) else Color(0xFFE8ECF1)
    val canvas = lerp(dominant, seed, if (dark) 0.035f else 0.018f)
    val top = lerp(dominant, seed, if (dark) 0.085f else 0.045f)
    val bottom = lerp(dominant, seed, if (dark) 0.045f else 0.025f)
    val surface = lerp(secondary, seed, if (dark) 0.12f else 0.075f)
    val glass = lerp(secondary, seed, if (dark) 0.095f else 0.06f)
    val accent = accessibleAccent(
        if (dark) lerp(seed, Color.White, 0.26f) else lerp(seed, Color.Black, 0.22f),
        listOf(canvas, top, bottom, surface, glass), dark,
    )
    return base.copy(
        // 60%: low-chroma, readable background even for neon theme seeds.
        background = canvas,
        backgroundTop = canvas,
        backgroundBottom = canvas,
        // 30%: tonal layers and glass, subtly coordinated with the selected hue.
        surface = surface,
        glass = glass,
        glassBorder = if (dark) Color(0xFFEDF1F7) else Color(0xFF647080),
        artworkStart = lerp(secondary, seed, 0.18f),
        artworkEnd = lerp(dominant, seed, 0.075f),
        // 10%: focus color, never the entire background or text body.
        accent = accent,
        accentSoft = lerp(accent, secondary, 0.30f),
        text = if (dark) Color(0xFFF5F7FA) else Color(0xFF161C24),
        muted = if (dark) Color(0xFFB7C0CC) else Color(0xFF556170),
        tint = if (dark) Color.White else Color(0xFF222A34),
    )
}

private val LocalKanadePalette = staticCompositionLocalOf { DarkPalette }
private val LocalSurfaceBackdrop = staticCompositionLocalOf<io.github.fletchmckee.liquid.LiquidState?> { null }
private val Background: Color @Composable get() = LocalKanadePalette.current.background
private val SurfaceLight: Color @Composable get() = LocalKanadePalette.current.surface
private val Accent: Color @Composable get() = LocalKanadePalette.current.accent
private val PrimaryText: Color @Composable get() = LocalKanadePalette.current.text
private val Muted: Color @Composable get() = LocalKanadePalette.current.muted
private val ErrorColor: Color @Composable get() = LocalKanadePalette.current.error

private class ChromeMetrics {
    var pageTop by mutableStateOf(0)
    var bottomHeight by mutableStateOf(0)
}
private val LocalChromeMetrics = staticCompositionLocalOf { ChromeMetrics() }

// Keep the scrollable content behind the page controls, while retaining reachable endpoints.
@Composable
private fun underChrome(): Pair<Modifier, PaddingValues> {
    val chrome = LocalChromeMetrics.current
    val density = LocalDensity.current
    var listTop by remember { mutableStateOf(0) }
    val overlap = (listTop - chrome.pageTop).coerceAtLeast(0)
    val padding = with(density) {
        PaddingValues(top = overlap.toDp(), bottom = chrome.bottomHeight.toDp() + 12.dp)
    }
    val modifier = Modifier.zIndex(-1f)
        .onGloballyPositioned { listTop = it.positionInRoot().y.toInt() }
        .layout { measurable, constraints ->
            val extra = overlap.coerceAtMost(4096)
            val placeable = measurable.measure(constraints.copy(maxHeight = constraints.maxHeight + extra))
            layout(placeable.width, constraints.maxHeight) { placeable.placeRelative(0, -extra) }
        }
    return modifier to padding
}

private val homeSections = listOf("Continue listening", "Quick picks", "Pinned playlists", "Recently played", "Start radio", "Listening insights")

private enum class Page { HOME, LIBRARY, ONLINE, PLAYLISTS, INSIGHTS, SETTINGS }
private enum class Overlay { HOME_EDITOR, FOLDERS, TOOLS, SESSIONS, NONE, QUEUE, LYRICS, SEARCH_LYRICS, PICK_PLAYLIST, EDIT_TAGS }

@Composable
fun KanadeScreen(model: KanadeViewModel, darkTheme: Boolean, themeMode: ThemeMode, account: String, accountAction: String, onAccountAction: () -> Unit) {
    val colorTheme by model.colorTheme.collectAsState()
    val font by model.appFont.collectAsState()
    val reduced by model.reduceMotion.collectAsState()
    CompositionLocalProvider(LocalReduceMotion provides reduced, LocalAppFont provides (appFonts[font] ?: appFonts.getValue("Kanade Sans")),
        LocalKanadePalette provides paletteFor(colorTheme, darkTheme)) {
        KanadeContent(model, themeMode, account, accountAction, onAccountAction)
    }
}

@Composable
private fun KanadeContent(model: KanadeViewModel, themeMode: ThemeMode, account: String, accountAction: String, onAccountAction: () -> Unit) {
    val library by model.library.collectAsState()
    val discover by model.discover.collectAsState()
    val homeDiscovery by model.homeDiscovery.collectAsState()
    val artistPage by model.artistPage.collectAsState()
    val albumPage by model.albumPage.collectAsState()
    val playback by model.playback.state.collectAsState()
    val favorites by model.favorites.collectAsState()
    val favoriteIds by model.favoriteIds.collectAsState()
    val playlists by model.playlists.collectAsState()
    val radioMessage by model.radioMessage.collectAsState()
    val recent by model.recent.collectAsState()
    val topTracks by model.topTracks.collectAsState()
    val selectedTracks by model.selectedPlaylistTracks.collectAsState()
    val totalPlays by model.totalPlays.collectAsState()
    val totalListenMs by model.totalListenMs.collectAsState()
    val prefs = model.options
    var revision by remember { mutableStateOf(0) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val homeOrder = remember(revision) {
        val saved = prefs.getString("home_order", "").orEmpty().split("|")
        (saved + homeSections).filter { it in homeSections }.distinct()
    }
    val hiddenSections = remember(revision) { prefs.getStringSet("home_hidden", emptySet()).orEmpty() }
    val offsetKey = "offset:${playback.mediaId}"
    val lyricDelay = remember(revision, offsetKey) { prefs.getLong(offsetKey, 0L) }
    val lyrics by model.lyrics.collectAsState()

    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    val pageStates = rememberSaveableStateHolder()
    val pins by model.pins.collectAsState()
    var settingsReturn by rememberSaveable { mutableStateOf(Page.HOME) }
    var scope by rememberSaveable { mutableStateOf("All") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val undo by model.removedSong.collectAsState()
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showArtist by rememberSaveable { mutableStateOf(false) }
    var showAlbum by rememberSaveable { mutableStateOf(false) }
    var artistReturnsToAlbum by rememberSaveable { mutableStateOf(false) }
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
    BackHandler(enabled = showArtist && !showAlbum && !showNowPlaying && overlay == Overlay.NONE) { showArtist = false; if (artistReturnsToAlbum) showAlbum = true; artistReturnsToAlbum = false }
    BackHandler(enabled = showAlbum && !showNowPlaying && overlay == Overlay.NONE) { showAlbum = false }
    BackHandler(enabled = page == Page.SETTINGS && !showAlbum && !showArtist && !showNowPlaying && overlay == Overlay.NONE) { page = settingsReturn }
    BackHandler(enabled = showNowPlaying && overlay == Overlay.NONE) { showNowPlaying = false }
    LaunchedEffect(playback.hasItem) { if (!playback.hasItem) showNowPlaying = false }

    val chrome = remember { ChromeMetrics() }
    // Separate source layers: nav/mini-player sample page content while inputs
    // sample the base background. Search controls never sample their own glass.
    val reduceNavigationMotion = LocalReduceMotion.current
    val chromeGlass = rememberLiquidState()
    val searchGlass = rememberLiquidState()
    CompositionLocalProvider(LocalChromeMetrics provides chrome, LocalSurfaceBackdrop provides searchGlass) {
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().liquefiable(searchGlass).background(Brush.verticalGradient(
        listOf(LocalKanadePalette.current.backgroundTop, Background, LocalKanadePalette.current.backgroundBottom))))
    AnimatedContent(targetState = showNowPlaying, transitionSpec = {
        val duration = if (reduceNavigationMotion) 0 else 220
        if (targetState) (slideInVertically(tween(duration)) { it / 6 } + fadeIn(tween(duration))) togetherWith fadeOut(tween(duration))
        else fadeIn(tween(duration)) togetherWith (slideOutVertically(tween(duration)) { it / 6 } + fadeOut(tween(duration)))
    }, label = "player screen") { playerVisible ->
    if (!playerVisible) Box(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        // Capture the scrolling page under the chrome, not just a flat gradient.
        // Exclude the navbar and mini-player, which remain sibling overlays.
        Box(Modifier.fillMaxSize().liquefiable(chromeGlass)
            .background(Brush.verticalGradient(listOf(
                LocalKanadePalette.current.backgroundTop, Background, LocalKanadePalette.current.backgroundBottom
            )))
            .onGloballyPositioned { chrome.pageTop = it.positionInRoot().y.toInt() }) {
            Crossfade(targetState = when {
                showAlbum && albumPage.album != null -> "album"
                showArtist && artistPage.artist != null -> "artist"
                else -> "page"
            }, animationSpec = tween(if (reduceNavigationMotion) 0 else 220), label = "detail screen") { section ->
            if (section == "album") {
                AlbumScreen(albumPage, favoriteIds, onBack = { showAlbum = false },
                    onPlay = { model.playListTracks(albumPage.tracks, it) }, onFavorite = model::toggleFavorite,
                    onRadio = model::startRadio, onQueue = { model.playback.add(it, true) },
                    onAppend = { model.playback.add(it, false) },
                    onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                    onMore = model::moreAlbumTracks, onRetry = model::retryAlbum,
                    onArtist = { entry -> model.openArtist(entry); artistReturnsToAlbum = true; showAlbum = false; showArtist = true })
            } else if (section == "artist") {
                ArtistScreen(artistPage, favoriteIds, onBack = { showArtist = false; if (artistReturnsToAlbum) showAlbum = true; artistReturnsToAlbum = false },
                    onPlay = { model.playListTracks(artistPage.tracks, it) }, onFavorite = model::toggleFavorite,
                    onRadio = model::startRadio, onQueue = { model.playback.add(it, true) },
                    onAppend = { model.playback.add(it, false) },
                    onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                    onMoreTracks = model::moreArtistTracks, onRetryTracks = model::retryArtistTracks,
                    onMoreAlbums = model::moreArtistAlbums, onAlbum = { entry ->
                        model.openAlbum(entry); artistReturnsToAlbum = false; showAlbum = true
                    })
            } else AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (reduceNavigationMotion) {
                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                    } else {
                        (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it / 8 }) +
                            fadeIn(animationSpec = tween(240))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { -it / 8 }) +
                                fadeOut(animationSpec = tween(220)))
                    }
                },
                label = "Navigation",
            ) { displayedPage ->
            pageStates.SaveableStateProvider(displayedPage.name) {
            when (displayedPage) {
                Page.HOME -> {
                    val bottom = with(LocalDensity.current) { chrome.bottomHeight.toDp() }
                    LazyColumn(contentPadding = PaddingValues(bottom = bottom + 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Label("Home", 32.sp, PrimaryText, FontWeight.ExtraBold, Modifier.weight(1f).combinedClickable(
                                    onClick = { overlay = Overlay.HOME_EDITOR }, onLongClick = { overlay = Overlay.HOME_EDITOR }))
                                Chip("Edit Home") { overlay = Overlay.HOME_EDITOR }
                                BasicText("⚙", Modifier.motionClick {
                                    settingsReturn = page; page = Page.SETTINGS
                                }.semantics { contentDescription = "Settings" }.padding(10.dp),
                                    style = TextStyle(color = PrimaryText, fontSize = 26.sp))
                            }
                        }
                        val quick = (favorites + topTracks + recent + library.tracks.take(6)).distinctBy { it.id }.take(6)
                        for (section in homeOrder) {
                            if (section in hiddenSections) continue
                            when (section) {
                                "Continue listening" -> {
                                    val resume = recent.firstOrNull()
                                    if (playback.hasItem || resume != null) item {
                                        Row(Modifier.fillMaxWidth().glassSurface(18.dp).motionClick {
                                            if (playback.hasItem) {
                                                if (!playback.playing) model.playback.toggle()
                                                showNowPlaying = true
                                            } else resume?.let { model.playListTracks(recent, it) }
                                        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AlbumImage(if (playback.hasItem) playback.artworkUri else resume?.artworkUri, Modifier.size(86.dp))
                                            Spacer(Modifier.width(14.dp))
                                            Column(Modifier.weight(1f)) {
                                                Label("Continue listening", 12.sp, Accent, FontWeight.Bold)
                                                Label(if (playback.hasItem) playback.title else resume?.title.orEmpty(), 21.sp, PrimaryText, FontWeight.Bold, lines = 2)
                                                Label(if (playback.hasItem) playback.artist else resume?.artist.orEmpty(), 13.sp, Muted, lines = 1)
                                            }
                                        }
                                    }
                                }
                                "Quick picks" -> {
                                    if (quick.isEmpty()) item {
                                        SimpleRow("Open local music", "Browse this device") { page = Page.LIBRARY }
                                        SimpleRow("Find music online", "Search songs and artists") { scope = "Online"; page = Page.ONLINE }
                                    } else {
                                        item {
                                            DashboardShelf("Quick picks", quick) { model.playListTracks(quick, it) }
                                        }
                                    }
                                }
                                "Pinned playlists" -> {
                                    val pinned = playlists.filter { it.id.toString() in pins }
                                    if (pinned.isNotEmpty()) item {
                                        Label("Pinned playlists", 20.sp, PrimaryText, FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(pinned, key = { it.id }) { playlist ->
                                                Chip(playlist.name) {
                                                    selectedPlaylistId = playlist.id; model.selectPlaylist(playlist.id); page = Page.PLAYLISTS
                                                }
                                            }
                                        }
                                    }
                                }
                                "Recently played" -> {
                                    if (recent.isNotEmpty()) item {
                                        DashboardShelf("Recently played", recent.take(12)) { model.playListTracks(recent, it) }
                                    }
                                }
                                "Start radio" -> {
                                    val stations = (recent + quick).distinctBy { it.artist.lowercase() }.take(8)
                                    if (stations.isNotEmpty()) item {
                                        DashboardShelf("Start radio", stations, onSelect = model::startRadio)
                                    }
                                }
                                "Listening insights" -> {
                                    item { SimpleRow("Listening insights", "History and smart collections") { page = Page.INSIGHTS } }
                                }
                            }
                        }
                        if (homeDiscovery.loading) item { Label("Finding music…", 13.sp, Muted) }
                        homeDiscovery.error?.let { message -> item { Label(message, 13.sp, Muted); Chip("Retry") { model.loadHomeDiscovery() } } }
                        if (homeDiscovery.recommended.isNotEmpty()) item {
                            DashboardShelf("Recommended · popular searches", homeDiscovery.recommended.take(12)) { model.playListTracks(homeDiscovery.recommended, it) }
                        }
                        if (homeDiscovery.hits.isNotEmpty()) item {
                            DashboardShelf("Top hits · search results", homeDiscovery.hits.take(12)) { model.playListTracks(homeDiscovery.hits, it) }
                        }
                        if (homeDiscovery.artists.isNotEmpty()) item {
                            CatalogShelf("Artists to explore", homeDiscovery.artists) { entry ->
                                model.openArtist(entry); showArtist = true
                            }
                        }
                        if (homeDiscovery.albums.isNotEmpty()) item {
                            CatalogShelf("Albums to explore", homeDiscovery.albums) { entry ->
                                model.openAlbum(entry); showAlbum = true
                            }
                        }
                    }
                }
                Page.LIBRARY -> Column {
                    Header("Library", "Your music, all in one place.")
                    Spacer(Modifier.height(12.dp))
                    if (!model.hasPermission()) {
                        EmptyState(KanadeIcon.MUSIC, "Grant music permission", "Browse MP3 music stored on your device.") {
                            permissions.launch(model.permission())
                        }
                    } else {
                        Chip("MP3 folders") { overlay = Overlay.FOLDERS }
                        SearchField(query, "Search songs, albums, artists, folders…", { query = it }, {}, glassBackdrop = searchGlass)
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (choice in listOf("All", "Favorites", "Recent", "Top", "Folders", "Albums", "Artists", "Duplicates")) {
                                Chip(choice, filter == choice) { filter = choice; folder = ""; album = "" }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(if (library.loading) "Scanning…" else "${library.tracks.size} on-device MP3s",
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
                            val (underlay, insets) = underChrome()
                            LazyColumn(modifier = underlay, contentPadding = insets, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(folders) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.folder.ifBlank { "Unknown folder" } == item }} tracks") {
                                        folder = item
                                    }
                                }
                            }
                        } else if (filter == "Albums" && album.isBlank()) {
                            val albums = library.tracks.map { it.album.ifBlank { "Unknown album" } }.distinct().sorted()
                            val (underlay, insets) = underChrome()
                            LazyColumn(modifier = underlay, contentPadding = insets, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(albums) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.album.ifBlank { "Unknown album" } == item }} tracks") {
                                        model.openAlbum(YouTubeMusicRepository.CatalogItem(item, "Album", library.tracks.firstOrNull { it.album.ifBlank { "Unknown album" } == item }?.artworkUri)); showAlbum = true
                                    }
                                }
                            }
                        } else if (filter == "Artists" && album.isBlank()) {
                            val artists = library.tracks.map { it.artist }.distinct().sorted()
                            val (underlay, insets) = underChrome()
                            LazyColumn(modifier = underlay, contentPadding = insets, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(artists) { item ->
                                    SimpleRow(item, "${library.tracks.count { it.artist == item }} tracks") {
                                        model.openArtist(YouTubeMusicRepository.CatalogItem(item, "Artist", library.tracks.firstOrNull { it.artist == item }?.artworkUri)); showArtist = true
                                    }
                                }
                            }
                        } else {
                            if ((filter == "Folders" && folder.isNotEmpty()) ||
                                ((filter == "Albums" || filter == "Artists") && album.isNotEmpty())) {
                                IconTextButton(KanadeIcon.BACK, "Back") { folder = ""; album = "" }
                                Spacer(Modifier.height(8.dp))
                            }
                            val indexedIds = remember(library.tracks) { library.tracks.mapTo(hashSetOf()) { it.id } }
                            val source = when (filter) {
                                "Favorites" -> favorites.filter { it.id in indexedIds }
                                "Recent" -> recent.filter { it.id in indexedIds }
                                "Top" -> topTracks.filter { it.id in indexedIds }
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
                                    onFavorite = model::toggleFavorite, onRadio = model::startRadio,
                                    onQueue = { model.playback.add(it, true) }, onAppend = { model.playback.add(it, false) },
                                    onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                                    onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                            }
                        }
                    }
                }
                Page.ONLINE -> {
                    var selectedMood by remember { mutableStateOf<MusicMoods.Mood?>(null) }
                    var moods by remember { mutableStateOf<List<MusicMoods.Mood>>(emptyList()) }
                    var moodPlaylists by remember { mutableStateOf<List<YouTubeMusicRepository.CatalogItem>>(emptyList()) }
                    var moodLoading by remember { mutableStateOf(false) }
                    var moodError by remember { mutableStateOf<String?>(null) }
                    var moodAttempt by remember { mutableStateOf(0) }
                    LaunchedEffect(selectedMood, moodAttempt) {
                        moodLoading = true
                        moodError = null
                        try {
                            if (selectedMood == null) {
                                moods = MusicMoods.moods()
                                if (moods.isEmpty()) moodError = "No moods available right now."
                            } else {
                                moodPlaylists = MusicMoods.playlists(selectedMood!!)
                                if (moodPlaylists.isEmpty()) moodError = "No playlists available for this mood."
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (_: Exception) { moodError = "Couldn't load moods. Check your connection and try again." }
                        finally { moodLoading = false }
                    }
                    BackHandler(enabled = selectedMood != null && !showAlbum && !showArtist && !showNowPlaying && overlay == Overlay.NONE) { selectedMood = null }
                    val submit: () -> Unit = {
                        submitted = onlineQuery.trim()
                        if (scope != "On device") model.search(submitted)
                    }
                    val needle = onlineQuery.trim()
                    val local = remember(library.tracks, needle, scope) {
                        if (scope == "Online" || needle.isEmpty()) emptyList() else library.tracks.filter {
                            it.title.contains(needle, true) || it.artist.contains(needle, true) || it.album.contains(needle, true)
                        }
                    }
                    val showOnline = scope != "On device" && submitted.isNotBlank() && submitted == needle
                    val matches = (if (showOnline) local + discover.tracks else local).distinctBy { it.id }
                    val localArtists = local.map { it.artist }.filter { it.isNotBlank() && it != "Unknown artist" }.distinct()
                        .map { name -> YouTubeMusicRepository.CatalogItem(name, "Artist", local.firstOrNull { it.artist == name }?.artworkUri) }
                    val localAlbums = local.filter { it.album.isNotBlank() }.distinctBy { it.album.lowercase() }
                        .map { YouTubeMusicRepository.CatalogItem(it.album, it.artist, it.artworkUri) }
                    val artists = (localArtists + if (showOnline) discover.artists else emptyList()).distinctBy { it.name.lowercase() }
                    val albums = (localAlbums + if (showOnline) discover.albums else emptyList()).distinctBy { it.name.lowercase() }
                    TrackList(matches, favoriteIds,
                        onPlay = { model.playListTracks(matches, it) },
                        onFavorite = model::toggleFavorite, onRadio = model::startRadio,
                        onQueue = { model.playback.add(it, true) }, onAppend = { model.playback.add(it, false) },
                        onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                        onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS },
                        pagination = if (showOnline) discover else null, onLoadMore = model::loadMoreSearchResults,
                        searchArtists = artists, searchAlbums = albums, searchLayout = true,
                        onArtist = { entry -> model.openArtist(entry); showArtist = true },
                        onAlbum = { entry -> model.openAlbum(entry); showAlbum = true },
                        searchLeading = {
                            Column {
                                Header("Search", "Search on-device MP3s and YouTube Music songs.")
                                SearchField(onlineQuery, "Song, album or artist", { onlineQuery = it }, submit, glassBackdrop = searchGlass)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (choice in listOf("All", "On device", "Online")) {
                                        Chip(choice, scope == choice) {
                                            scope = choice
                                            if (choice != "On device" && onlineQuery.isNotBlank()) submit()
                                        }
                                    }
                                    Chip("Search") { submit() }
                                }
                                if (scope != "Online" && !model.hasPermission()) Chip("Allow local music") { permissions.launch(model.permission()) }
                                if (showOnline) Label(discover.message, 12.sp, Muted)
                            }
                        },
                        searchBrowse = if (needle.isEmpty()) {{
                            Column(modifier = if (LocalReduceMotion.current) Modifier else Modifier.animateContentSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Spacer(Modifier.height(4.dp))
                                if (selectedMood != null) Chip("All moods") { selectedMood = null }
                                Label(selectedMood?.name ?: "Explore moods", 20.sp, PrimaryText, FontWeight.Bold)
                                if (moodLoading) Label("Loading…", 13.sp, Muted)
                                moodError?.let { message ->
                                    Label(message, 13.sp, Muted)
                                    Chip("Retry") { moodAttempt++ }
                                }
                                if (selectedMood != null && !moodLoading) moodPlaylists.forEach { entry ->
                                    CatalogResultRow(entry) { model.openAlbum(it); showAlbum = true }
                                }
                                if (selectedMood == null) for (pair in moods.chunked(2)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        pair.forEach { mood ->
                                            MoodTile(mood.name, Modifier.weight(1f)) { moodPlaylists = emptyList(); selectedMood = mood }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }} else null,
                        searchEmptyMessage = if (needle.isNotEmpty() && matches.isEmpty() && artists.isEmpty() && albums.isEmpty())
                            if (discover.searching && showOnline) "Searching…" else "No results shown." else null)
                }
                Page.PLAYLISTS -> Column {
                    Chip("Queue sessions") { overlay = Overlay.SESSIONS }
                    Header("My playlists", "Saved locally. No account required.")
                    Spacer(Modifier.height(12.dp))
                    if (selectedPlaylistId < 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                SearchField(playlistName, "New playlist name", { playlistName = it }, {},
                                    showSearchIcon = false, glassBackdrop = searchGlass)
                            }
                            Spacer(Modifier.width(8.dp))
                            AccentAction("Create") {
                                model.createPlaylist(playlistName)
                                playlistName = ""
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        val (underlay, insets) = underChrome()
                        LazyColumn(modifier = underlay, contentPadding = insets, verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                            Chip(if (selectedPlaylistId.toString() in pins) "Unpin" else "Pin to Home") { model.togglePin(selectedPlaylistId) }
                            Chip("Import") { m3uImporter.launch(arrayOf("audio/x-mpegurl", "text/*", "*/*")) }
                            Chip("Export") { m3uExporter.launch("kanade-playlist.m3u") }
                            Chip("Delete") { model.deletePlaylist(selectedPlaylistId); selectedPlaylistId = -1L }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (undo?.playlistId == selectedPlaylistId) Chip("Undo removal") { model.undoRemoval() }
                        if (selectedTracks.isEmpty()) EmptyState(KanadeIcon.MUSIC, "Playlist is empty",
                            "Open a song’s menu to add music.")
                        else TrackList(selectedTracks, favoriteIds,
                            onPlay = { model.playListTracks(selectedTracks, it) },
                            onFavorite = model::toggleFavorite, onRadio = model::startRadio,
                            onQueue = { model.playback.add(it, true) }, onAppend = { model.playback.add(it, false) },
                            onPlaylist = { model.removeFromPlaylist(selectedPlaylistId, it.id) },
                            removeFromPlaylist = true,
                            onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                    }
                }
                Page.SETTINGS -> SettingsScreen(model, playback, themeMode, account, accountAction, onAccountAction)
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
                        onFavorite = model::toggleFavorite, onRadio = model::startRadio,
                        onQueue = { model.playback.add(it, true) }, onAppend = { model.playback.add(it, false) },
                        onPlaylist = { selectedTrack = it; overlay = Overlay.PICK_PLAYLIST },
                        onTags = { selectedTrack = it; overlay = Overlay.EDIT_TAGS })
                }
            }
        }
        }
        }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .onGloballyPositioned { chrome.bottomHeight = it.size.height }) {
        AnimatedVisibility(visible = playback.hasItem,
            enter = fadeIn(tween(if (reduceNavigationMotion) 0 else 180)) + expandVertically(tween(if (reduceNavigationMotion) 0 else 220)),
            exit = fadeOut(tween(if (reduceNavigationMotion) 0 else 140)) + shrinkVertically(tween(if (reduceNavigationMotion) 0 else 180))) {
            Column {
                Spacer(Modifier.height(7.dp))
                MiniPlayer(playback, backdrop = chromeGlass, onToggle = model.playback::toggle,
                    onNext = model.playback::next, onExpand = { showNowPlaying = true })
            }
        }
        if (radioMessage.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(radioMessage, 13.sp, PrimaryText, modifier = Modifier.weight(1f))
                TinyButton(KanadeIcon.CLOSE, "Dismiss radio status", onClick = model::dismissRadioMessage)
            }
        }
        playback.error?.let { message ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(message, 12.sp, ErrorColor, modifier = Modifier.weight(1f))
                Chip("Dismiss") { model.playback.clearError() }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().warmRaisedGlass(chromeGlass, radius = 24.dp, elevation = 12.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for ((tab, label, icon) in listOf(
                Triple(Page.HOME, "Home", KanadeIcon.NAV_HOME),
                Triple(Page.LIBRARY, "Library", KanadeIcon.NAV_LIBRARY),
                Triple(Page.ONLINE, "Search", KanadeIcon.NAV_SEARCH),
                Triple(Page.PLAYLISTS, "Playlists", KanadeIcon.NAV_PLAYLISTS),
            )) {
                val active = page == tab
                val duration = if (LocalReduceMotion.current) 0 else 150
                val iconScale by animateFloatAsState(if (active) 1f else 0.91f,
                    animationSpec = tween(duration), label = "navigation icon")
                Column(Modifier.weight(1f).heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (active) Color(0xFFF2E2D2).copy(alpha = 0.09f) else Color.Transparent)
                    .motionSelectable(selected = active, role = Role.Tab, onClick = { showAlbum = false; showArtist = false; artistReturnsToAlbum = false; page = tab })
                    .semantics { contentDescription = label },
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
                        KanadeGlyph(icon, if (active) PrimaryText else Muted, selected = active, size = 24.dp)
                    }
                    AnimatedVisibility(visible = active,
                        enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
                        exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
                        Label(label, 11.sp, PrimaryText, FontWeight.Bold, Modifier.padding(top = 2.dp), lines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        }
    } else {
        val mode by model.videoMode.collectAsState()
        NowPlayingScreen(playback, lyrics, mode,
            onClose = { showNowPlaying = false }, onToggle = model.playback::toggle,
            onNext = model.playback::next, onPrevious = model.playback::previous,
            onShuffle = model.playback::toggleShuffle, onRepeat = model.playback::cycleRepeat,
            onSeek = model.playback::seek, onPlayAt = model.playback::playAt,
            onLyrics = { overlay = Overlay.LYRICS }, lyricDelay = lyricDelay, onTools = { overlay = Overlay.TOOLS })
    }
    }
    }

    when (overlay) {
        Overlay.HOME_EDITOR -> FeatureDialog("Edit Home", { overlay = Overlay.NONE }) {
            homeOrder.forEachIndexed { index, section ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        ThemeChoice(section, section !in hiddenSections) {
                            val next = if (section in hiddenSections) hiddenSections - section else hiddenSections + section
                            prefs.edit().putStringSet("home_hidden", next).apply()
                        }
                    }
                    fun move(to: Int) {
                        val order = homeOrder.toMutableList()
                        order.add(to, order.removeAt(index))
                        prefs.edit().putString("home_order", order.joinToString("|")).apply()
                    }
                    if (index > 0) TinyButton(KanadeIcon.UP, "Move $section up") { move(index - 1) }
                    if (index < homeOrder.lastIndex) TinyButton(KanadeIcon.DOWN, "Move $section down") { move(index + 1) }
                }
            }
            Chip("Restore defaults") { prefs.edit().remove("home_order").remove("home_hidden").apply() }
        }
        Overlay.FOLDERS -> FeatureDialog("MP3 folders", { overlay = Overlay.NONE }) {
            val folders by model.folders.collectAsState()
            val included = remember(revision) { prefs.getStringSet("include", emptySet()).orEmpty() }
            val excluded = remember(revision) { prefs.getStringSet("exclude", emptySet()).orEmpty() }
            Chip("All folders") {
                prefs.edit().remove("include").remove("exclude").apply()
                model.applyFolders()
            }
            for (folder in (folders + included + excluded).distinct().sorted()) {
                Label(folder.ifBlank { "Storage root" }, 14.sp, PrimaryText)
                Row {
                    for (mode in listOf("Default", "Include", "Exclude")) {
                        ThemeChoice(mode, mode == when (folder) { in included -> "Include"; in excluded -> "Exclude"; else -> "Default" }) {
                            prefs.edit().putStringSet("include", if (mode == "Include") included + folder else included - folder)
                                .putStringSet("exclude", if (mode == "Exclude") excluded + folder else excluded - folder).apply()
                            model.applyFolders()
                        }
                    }
                }
            }
        }
        Overlay.SESSIONS -> FeatureDialog("Queue sessions", { overlay = Overlay.NONE }) {
            val names by model.playback.sessionNames.collectAsState()
            var name by rememberSaveable { mutableStateOf("") }
            SearchField(name, "Session name", { name = it.take(60) }, {})
            if (name.isNotBlank() && playback.hasItem) Chip(if (name.trim() in names) "Replace session" else "Save session") {
                model.playback.saveSession(name); name = ""
            }
            for (saved in names) SimpleRow(saved, "Resume session") { model.playback.openSession(saved); overlay = Overlay.NONE }
        }
        Overlay.TOOLS -> androidx.compose.runtime.key(playback.mediaId) {
            FeatureDialog("Player tools", { overlay = Overlay.NONE }) {
                if (radioMessage.isNotBlank()) Label(radioMessage, 13.sp, PrimaryText)
                val id = playback.mediaId
                val station = model.playback.station
                if (station.isNotEmpty()) {
                    var mix by rememberSaveable { mutableStateOf("Radio · " + (model.findTrack(station)?.title ?: playback.title)) }
                    SearchField(mix, "Playlist name", { mix = it }, {})
                    Chip("Save mix as playlist") { model.saveRadioMix(mix) }
                    if (station != id) Chip("Hide from this station") { model.hideStationSong(); overlay = Overlay.NONE }
                }
                Chip("Queue sessions") { overlay = Overlay.SESSIONS }
                val key = "marks:$id"
                val marks = remember(revision, key) { org.json.JSONObject(prefs.getString(key, "{}") ?: "{}") }
                var name by rememberSaveable { mutableStateOf("") }
                SearchField(name, "Bookmark name", { name = it }, {})
                Chip("Bookmark " + DurationFormatter.format(playback.positionMs)) {
                    val time = playback.positionMs
                    val copy = org.json.JSONObject(marks.toString()).put(time.toString(), name.trim().ifEmpty { DurationFormatter.format(time) })
                    prefs.edit().putString(key, copy.toString()).apply()
                    name = ""
                }
                for (time in marks.keys().asSequence().mapNotNull { it.toLongOrNull() }.sorted()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Chip(marks.getString(time.toString()) + " · " + DurationFormatter.format(time)) {
                                model.playback.seek(time); overlay = Overlay.NONE
                            }
                        }
                        TinyButton(KanadeIcon.REMOVE, "Delete bookmark") {
                            val copy = org.json.JSONObject(marks.toString())
                            copy.remove(time.toString())
                            prefs.edit().putString(key, copy.toString()).apply()
                        }
                    }
                }
                var pointA by rememberSaveable(id) { mutableStateOf(prefs.getLong("loopA:$id", -1L)) }
                val loop by com.urfavxbf.kanade.playback.KanadePlaybackService.loop.collectAsState()
                Label(loop?.takeIf { it.first == id }?.let { "Loop " + DurationFormatter.format(it.second) + " – " + DurationFormatter.format(it.third) }
                    ?: if (pointA >= 0) "A · " + DurationFormatter.format(pointA) else "A–B repeat", 14.sp, PrimaryText)
                Row {
                    Chip("Set A") {
                        pointA = playback.positionMs
                        prefs.edit().putLong("loopA:$id", pointA).apply()
                        model.playback.setLoop(id, -1, -1)
                    }
                    if (pointA >= 0 && playback.positionMs >= pointA + 500) Chip("Set B") {
                        model.playback.setLoop(id, pointA, playback.positionMs)
                    }
                    Chip("Clear") {
                        pointA = -1; prefs.edit().remove("loopA:$id").apply()
                        model.playback.setLoop(id, -1, -1)
                    }
                }
            }
        }
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
        Overlay.SEARCH_LYRICS -> androidx.compose.runtime.key(playback.mediaId) {
            LyricsSearchDialog(playback.title, playback.artist, { overlay = Overlay.LYRICS }) { match ->
                model.chooseLyrics(playback.mediaId, match)
                overlay = Overlay.LYRICS
            }
        }
        Overlay.LYRICS -> FeatureDialog("Synchronized lyrics", { overlay = Overlay.NONE }) {
            if (playback.mediaId.isNotBlank()) {
                AccentAction("Search lyrics") { overlay = Overlay.SEARCH_LYRICS }
                Spacer(Modifier.height(8.dp))
            }
            AccentAction("Choose LRC file") { lyricsPicker.launch(arrayOf("text/*", "*/*")) }
            Spacer(Modifier.height(13.dp))
            if (lyrics.lines.isEmpty()) Label(lyrics.plain ?: lyrics.message, 14.sp, Muted)
            else {
                Label("Lyrics timing · ${lyricDelay} ms", 14.sp, PrimaryText)
                Row {
                    Chip("Earlier") { prefs.edit().putLong(offsetKey, lyricDelay - 250).apply() }
                    Chip("Later") { prefs.edit().putLong(offsetKey, lyricDelay + 250).apply() }
                    Chip("Reset") { prefs.edit().remove(offsetKey).apply() }
                }
                val active = LrcParser.current(lyrics.lines, lyricClock(playback.positionMs, lyricDelay))
                val listState = rememberLazyListState()
                val dragged by listState.interactionSource.collectIsDraggedAsState()
                var follow by remember(playback.mediaId) { mutableStateOf(true) }
                val reduced = LocalReduceMotion.current
                LaunchedEffect(dragged) { if (dragged) follow = false }
                if (!follow) Chip("Back to current line") { follow = true }
                LaunchedEffect(active, follow, lyrics.lines) {
                    if (follow && active >= 0) {
                        if (reduced) listState.scrollToItem(active) else listState.animateScrollToItem(active)
                    }
                }
                LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 155.dp), modifier = Modifier.fillMaxWidth().height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    items(lyrics.lines.size) { i ->
                        val line = lyrics.lines[i]
                        Label(line.text, if (active == i) 19.sp else 15.sp,
                            if (active == i) PrimaryText else Muted,
                            if (active == i) FontWeight.Bold else FontWeight.Normal,
                            Modifier.fillMaxWidth().clickable { model.playback.seek(lyricSeek(line.timeMs, lyricDelay)) })
                    }
                }
            }
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
}

@Composable
private fun SettingsScreen(model: KanadeViewModel, playback: PlaybackState, themeMode: ThemeMode, account: String, accountAction: String, onAccountAction: () -> Unit) {
    val onlineBoost by model.playback.onlineBoost.collectAsState()
    val colorTheme by model.colorTheme.collectAsState()
    val plays by model.totalPlays.collectAsState()
    val listeningMs by model.totalListenMs.collectAsState()
    val bottomInset = with(LocalDensity.current) { LocalChromeMetrics.current.bottomHeight.toDp() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Settings", "Make Kanade feel like yours.")
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(SurfaceLight.copy(alpha = 0.65f)).padding(16.dp)) {
            Label(account, 16.sp, PrimaryText, FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Chip(accountAction, onClick = onAccountAction)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(SurfaceLight.copy(alpha = 0.65f)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f)) {
                Label("${listeningMs / 3_600_000}h", 30.sp, PrimaryText, FontWeight.Bold)
                Label("Listening time", 12.sp, Muted)
            }
            Column(Modifier.weight(1f)) {
                Label(plays.toString(), 30.sp, PrimaryText, FontWeight.Bold)
                Label("Plays", 12.sp, Muted)
            }
        }
        Spacer(Modifier.height(22.dp))
        Label("APPEARANCE", 12.sp, Accent, FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Label("Theme", 15.sp, PrimaryText, FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((mode, title) in listOf(ThemeMode.SYSTEM to "Follow system", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")) {
                ThemeChoice(title, themeMode == mode) { model.setThemeMode(mode) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Label("App font", 15.sp, PrimaryText, FontWeight.SemiBold)
        val selectedFont by model.appFont.collectAsState()
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((name, family) in appFonts) {
                CompositionLocalProvider(LocalAppFont provides family) {
                    ThemeChoice(name, name == selectedFont) { model.setAppFont(name) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Label("Color palette", 15.sp, PrimaryText, FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Label("Choose a look for light and dark mode.", 12.sp, Muted)
        Spacer(Modifier.height(9.dp))
        Column(Modifier.fillMaxWidth().glassSurface(16.dp).padding(12.dp)) {
            Label(if (playback.hasItem) playback.title else "Song title", 18.sp, PrimaryText, FontWeight.Bold)
            Label(if (playback.hasItem) playback.artist else "Artist", 14.sp, Muted)
            if (playback.hasItem) Chip(if (playback.playing) "Pause" else "Play") { model.playback.toggle() }
            else KanadeGlyph(KanadeIcon.PLAY, Accent, bold = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val selectedPalette = if (colorTheme == PLAIN) {
                if (themeMode.isDark(androidx.compose.foundation.isSystemInDarkTheme())) PLAIN_DARK else PLAIN_LIGHT
            } else colorTheme
            for (group in colorThemes.entries.chunked(3)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((name, color) in group) {
                        val plain = name == PLAIN_DARK || name == PLAIN_LIGHT
                        Column(Modifier.weight(1f).glassSurface(16.dp)
                            .motionSelectable(selected = selectedPalette == name, role = Role.RadioButton, onClick = {
                                if (plain) model.setThemeMode(if (name == PLAIN_DARK) ThemeMode.DARK else ThemeMode.LIGHT)
                                model.setColorTheme(if (plain) PLAIN else name)
                            })
                            .padding(9.dp)) {
                            Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(9.dp))
                                .then(if (plain) Modifier.background(color)
                                    else Modifier.background(Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.52f))))))
                            Spacer(Modifier.height(6.dp))
                            Label(name, 11.sp, PrimaryText, if (selectedPalette == name) FontWeight.Bold else FontWeight.Normal, lines = 1)
                            if (selectedPalette == name) Label("Selected", 10.sp, Accent, FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        val mode by model.videoMode.collectAsState()
        val reduced by model.reduceMotion.collectAsState()
        Label("Video backdrop", 15.sp, PrimaryText, FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            for (option in listOf("Always", "Wi-Fi only", "Off")) ThemeChoice(option, mode == option) { model.changeVideoMode(option) }
        }
        ThemeChoice("Reduce motion", reduced) { model.changeMotion(!reduced) }
        Label("AUDIO", 12.sp, Accent, FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Label("ONLINE VOLUME BOOST", 12.sp, Accent, FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for ((gain, title) in listOf(0 to "Off", 300 to "+3 dB", 600 to "+6 dB")) {
                ThemeChoice(title, onlineBoost == gain) { model.playback.setOnlineBoost(gain) }
            }
        }
        Spacer(Modifier.height(7.dp))
        Label("For online songs. Higher levels may change sound quality.", 12.sp, Muted)
        Spacer(Modifier.height(17.dp))
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
        Spacer(Modifier.height(bottomInset + 12.dp))
    }
}

@Composable
private fun ThemeChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) Accent else LocalKanadePalette.current.tint.copy(alpha = 0.08f),
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 180), label = "selection color")
    Box(Modifier.clip(RoundedCornerShape(13.dp))
        .background(color)
        .motionSelectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Label(text, 12.sp, if (selected) Background else PrimaryText,
            if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun Header(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Label(title, 32.sp, PrimaryText, FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Label(subtitle, 13.sp, Muted)
    }
}

@Composable
private fun Label(
    text: String, size: TextUnit, color: Color = PrimaryText,
    weight: FontWeight = FontWeight.Medium, modifier: Modifier = Modifier,
    lines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier = modifier, maxLines = lines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontFamily = LocalAppFont.current, color = color, fontSize = size, fontWeight = weight))
}

@Composable private fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) Accent else LocalKanadePalette.current.tint.copy(alpha = 0.08f),
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 180), label = "chip color")
    Box(Modifier.clip(RoundedCornerShape(13.dp))
        .background(color)
        .motionClick(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Label(text, 12.sp, if (selected) Background else PrimaryText,
            if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun AccentAction(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(Accent)
        .motionClick(onClick = onClick).padding(horizontal = 17.dp, vertical = 14.dp)) {
        Label(text, 12.sp, Background, FontWeight.Bold)
    }
}

@Composable
private fun Modifier.motionClick(role: Role = Role.Button, onClickLabel: String? = null, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !LocalReduceMotion.current) 0.96f else 1f,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 130), label = "press scale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, role = role, onClickLabel = onClickLabel, onClick = onClick)
}

@Composable
private fun Modifier.motionSelectable(selected: Boolean, role: Role, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !LocalReduceMotion.current) 0.96f else 1f,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 130), label = "selection press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .selectable(selected = selected, interactionSource = interaction, indication = null, role = role, onClick = onClick)
}

@Composable
private fun LyricsSearchDialog(title: String, artist: String, close: () -> Unit, choose: (OnlineLyrics.Match) -> Unit) {
    var query by rememberSaveable { mutableStateOf(listOf(title, artist).joinToString(" ").trim()) }
    var submitted by rememberSaveable { mutableStateOf("") }
    var attempt by remember { mutableStateOf(0) }
    var matches by remember { mutableStateOf(emptyList<OnlineLyrics.Match>()) }
    var message by remember { mutableStateOf("") }
    val submit = {
        submitted = query.trim()
        matches = emptyList()
        attempt++
    }
    LaunchedEffect(submitted, attempt) {
        if (submitted.isBlank()) return@LaunchedEffect
        message = "Searching LRCLIB…"
        try {
            matches = OnlineLyrics.search(submitted)
            message = if (matches.isEmpty()) "No lyrics found. Try a shorter title or artist name." else ""
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            message = "Could not reach LRCLIB. Check your connection and try again."
        }
    }
    FeatureDialog("Search lyrics", close) {
        SearchField(query, "Song title or artist", {
            query = it
            submitted = ""
            matches = emptyList()
            message = ""
        }, { if (query.isNotBlank()) submit() })
        Spacer(Modifier.height(8.dp))
        AccentAction("Search LRCLIB") { if (query.isNotBlank()) submit() }
        if (message.isNotEmpty()) Label(message, 14.sp, Muted)
        for (match in matches) {
            Column(Modifier.fillMaxWidth().motionClick { choose(match) }.padding(vertical = 10.dp)) {
                Label(match.title, 16.sp, PrimaryText, FontWeight.Bold)
                Label(listOf(match.artist, match.album).filter { it.isNotBlank() }.joinToString(" · "), 13.sp, Muted)
                val type = if (match.lyrics.synced.isEmpty()) "Plain lyrics" else "Synced lyrics"
                Label("$type · ${DurationFormatter.format(match.duration * 1000)}", 12.sp, Accent)
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    hint: String,
    change: (String) -> Unit,
    search: () -> Unit,
    showSearchIcon: Boolean = true,
    glassBackdrop: io.github.fletchmckee.liquid.LiquidState? = null,
) {
    BasicTextField(
        value = value, onValueChange = change, singleLine = true,
        textStyle = TextStyle(fontFamily = LocalAppFont.current, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(SurfaceLight.copy(alpha = 0.72f))
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
private fun DashboardShelf(title: String, tracks: List<Track>, onSelect: (Track) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(title, 20.sp, PrimaryText, FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = { it.id }) { track ->
                Column(Modifier.width(144.dp).motionClick { onSelect(track) }) {
                    AlbumImage(track.artworkUri, Modifier.size(144.dp))
                    Spacer(Modifier.height(5.dp))
                    Label(if (track.source == TrackSource.YOUTUBE) songTitle(track.title, track.artist) else track.title,
                        14.sp, PrimaryText, FontWeight.Bold, lines = 2)
                    Label(track.artist, 12.sp, Muted, lines = 1)
                }
            }
        }
    }
}

@Composable
private fun CatalogShelf(title: String, entries: List<YouTubeMusicRepository.CatalogItem>, onSelect: (YouTubeMusicRepository.CatalogItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(title, 20.sp, PrimaryText, FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries) { entry ->
                Column(Modifier.width(128.dp).motionClick { onSelect(entry) }) {
                    AlbumImage(entry.artwork, Modifier.size(128.dp))
                    Spacer(Modifier.height(5.dp))
                    Label(entry.name, 14.sp, PrimaryText, FontWeight.Bold, lines = 2)
                    Label(entry.subtitle, 12.sp, Muted, lines = 1)
                }
            }
        }
    }
}

@Composable
private fun MoodTile(name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = listOf(Color(0xFF3A4A59), Color(0xFF675929), Color(0xFF3D5A49), Color(0xFF63343A), Color(0xFF493D60))
    val color = colors[(name.hashCode() and Int.MAX_VALUE) % colors.size]
    Box(modifier.height(88.dp).clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.38f))))
        .motionClick(onClick = onClick)
        .semantics { contentDescription = "Browse $name playlists" }.padding(14.dp)) {
        Label(name, 15.sp, Color.White, FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.58f), lines = 2)
    }
}

@Composable
private fun AlbumImage(artwork: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(listOf(LocalKanadePalette.current.artworkStart, LocalKanadePalette.current.artworkEnd))),
        contentAlignment = Alignment.Center) {
        KanadeGlyph(KanadeIcon.MUSIC, Accent, selected = true, size = 28.dp)
        Crossfade(targetState = artwork, animationSpec = tween(if (LocalReduceMotion.current) 0 else 220), label = "album artwork") { image ->
            if (!image.isNullOrEmpty()) AsyncImage(model = image, contentDescription = "Album cover",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AlbumScreen(
    state: AlbumPageState, favoriteIds: Set<String>, onBack: () -> Unit,
    onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit, onRadio: (Track) -> Unit,
    onQueue: (Track) -> Unit, onAppend: (Track) -> Unit, onPlaylist: (Track) -> Unit,
    onMore: () -> Unit, onRetry: () -> Unit, onArtist: (YouTubeMusicRepository.CatalogItem) -> Unit,
) {
    val album = state.album ?: return
    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    val bottom = with(LocalDensity.current) { LocalChromeMetrics.current.bottomHeight.toDp() + 20.dp }
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf {
        val info = listState.layoutInfo
        info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 3
    } }
    LaunchedEffect(nearEnd, state.hasMore, state.loading, state.error, state.tracks.size) {
        if (nearEnd && state.hasMore && !state.loading && state.error == null) onMore()
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        item(key = "album-header") {
            Column(Modifier.fillMaxWidth()) {
                IconTextButton(KanadeIcon.BACK, "Back", onBack)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AlbumImage(album.artwork, Modifier.size(220.dp))
                }
                Spacer(Modifier.height(20.dp))
                Label(album.name, 27.sp, PrimaryText, FontWeight.Bold, lines = 3)
                Label(if (album.url == null) "Album · On device" else "Album · YouTube", 13.sp, Muted)
                if (album.subtitle.isNotBlank() && album.subtitle != "Album") {
                    if (album.url == null || album.artistUrl != null) {
                        Label(album.subtitle, 15.sp, Accent, FontWeight.SemiBold,
                            modifier = Modifier.motionClick {
                                onArtist(YouTubeMusicRepository.CatalogItem(album.subtitle, "Artist", null, album.artistUrl))
                            }.padding(vertical = 8.dp))
                    } else Label(album.subtitle, 15.sp, Muted, modifier = Modifier.padding(vertical = 8.dp))
                }
                if (state.tracks.isNotEmpty()) {
                    Label("${state.tracks.size} tracks${if (state.hasMore) " loaded" else ""}", 12.sp, Muted)
                    Spacer(Modifier.height(12.dp))
                    AccentAction("Play all") { onPlay(state.tracks.first()) }
                }
                Spacer(Modifier.height(12.dp))
                Label("Tracks", 20.sp, PrimaryText, FontWeight.Bold)
                if (!state.loading && state.error == null && state.tracks.isEmpty()) Label("No tracks available.", 13.sp, Muted)
            }
        }
        items(state.tracks, key = { it.id }) { track ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumImage(track.artworkUri ?: album.artwork, Modifier.size(54.dp).clickable { onPlay(track) })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).motionClick { onPlay(track) }) {
                    Label(track.title, 14.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
                    Label(track.artist, 12.sp, Muted, lines = 1)
                    if (track.durationMs > 0) Label(DurationFormatter.format(track.durationMs), 11.sp, Muted)
                }
                TinyButton(KanadeIcon.FAVORITE, if (track.id in favoriteIds) "Remove from favorites" else "Add to favorites", selected = track.id in favoriteIds) { onFavorite(track) }
                TinyButton(KanadeIcon.MORE, "Track actions for ${track.title}") { actionsTrack = track }
            }
        }
        item(key = "album-footer") {
            when {
                state.loading -> Label("Loading album…", 13.sp, Muted)
                state.error != null -> { Label(state.error, 13.sp, Muted); Chip("Try again", onClick = onRetry) }
                state.hasMore -> Chip("More tracks", onClick = onMore)
            }
        }
    }
    actionsTrack?.let { track ->
        FeatureDialog(track.title, { actionsTrack = null }) {
            SimpleRow("Start radio", "Build a mix from this song") { actionsTrack = null; onRadio(track) }
            SimpleRow("Play next", "Add this song to the queue") { actionsTrack = null; onQueue(track) }
            SimpleRow("Add to queue", "After the last song") { actionsTrack = null; onAppend(track) }
            SimpleRow("Add to playlist", "Manage your music") { actionsTrack = null; onPlaylist(track) }
        }
    }
}

@Composable
private fun ArtistScreen(
    state: ArtistPageState, favoriteIds: Set<String>, onBack: () -> Unit,
    onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit, onRadio: (Track) -> Unit,
    onQueue: (Track) -> Unit, onAppend: (Track) -> Unit, onPlaylist: (Track) -> Unit,
    onMoreTracks: () -> Unit, onRetryTracks: () -> Unit, onMoreAlbums: () -> Unit,
    onAlbum: (YouTubeMusicRepository.CatalogItem) -> Unit,
) {
    val artist = state.artist ?: return
    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    val online = artist.url != null
    val bottom = with(LocalDensity.current) { LocalChromeMetrics.current.bottomHeight.toDp() + 20.dp }
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf {
        val info = listState.layoutInfo
        info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 3
    } }
    LaunchedEffect(nearEnd, state.tracksMore, state.loadingTracks, state.trackError, state.tracks.size) {
        if (nearEnd && state.tracksMore && !state.loadingTracks && state.trackError == null) onMoreTracks()
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottom), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "artist-header") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                IconTextButton(KanadeIcon.BACK, "Back", onBack)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(176.dp).clip(CircleShape).background(LocalKanadePalette.current.tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    KanadeGlyph(KanadeIcon.MUSIC, Accent, selected = true, size = 48.dp)
                    if (!artist.artwork.isNullOrBlank()) AsyncImage(model = artist.artwork, contentDescription = "${artist.name} artist image",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(16.dp))
                Label(artist.name, 30.sp, PrimaryText, FontWeight.Bold, lines = 3)
                Label(if (online) "Artist · YouTube" else "Artist · On device", 13.sp, Muted)
                if (state.subscribers >= 0) Label("${java.text.NumberFormat.getIntegerInstance().format(state.subscribers)} subscribers", 13.sp, Muted)
                Spacer(Modifier.height(12.dp))
                if (state.description.isNotBlank()) Label(state.description, 14.sp, PrimaryText, lines = Int.MAX_VALUE)
                else Label(if (online) "Artist biography is unavailable from this source." else "Showing music stored on this device.", 13.sp, Muted)
                if (online) Label("Albums and tracks are search results and may include related artists.", 12.sp, Muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item(key = "artist-albums") {
            Column(Modifier.padding(top = 18.dp)) {
                if (state.albums.isNotEmpty()) CatalogShelf(if (online) "Album results" else "Albums", state.albums, onAlbum)
                else Label("Albums", 20.sp, PrimaryText, FontWeight.Bold)
                when {
                    state.loadingAlbums -> Label("Loading albums…", 13.sp, Muted)
                    state.albumError != null -> { Label(state.albumError, 13.sp, Muted); Chip("Retry albums", onClick = onMoreAlbums) }
                    state.albumsMore -> Chip("More albums", onClick = onMoreAlbums)
                    state.albums.isEmpty() -> Label("No albums available.", 13.sp, Muted)
                }
            }
        }
        item(key = "artist-tracks-heading") {
            Column(Modifier.padding(top = 18.dp)) {
                Label(if (online) "Track results" else "Tracks", 20.sp, PrimaryText, FontWeight.Bold)
                if (state.tracks.isNotEmpty()) AccentAction("Play all") { onPlay(state.tracks.first()) }
                if (state.tracks.isEmpty() && !state.loadingTracks && state.trackError == null) Label("No tracks available.", 13.sp, Muted)
            }
        }
        items(state.tracks, key = { it.id }) { track ->
            Row(Modifier.fillMaxWidth().motionClick { onPlay(track) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AlbumImage(track.artworkUri, Modifier.size(54.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Label(track.title, 14.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
                    Label(track.artist, 12.sp, Muted, lines = 1)
                }
                TinyButton(KanadeIcon.FAVORITE, if (track.id in favoriteIds) "Remove from favorites" else "Add to favorites", selected = track.id in favoriteIds) { onFavorite(track) }
                TinyButton(KanadeIcon.MORE, "Track actions for ${track.title}") { actionsTrack = track }
            }
        }
        item(key = "artist-tracks-footer") {
            when {
                state.loadingTracks -> Label("Loading tracks…", 13.sp, Muted)
                state.trackError != null -> { Label(state.trackError, 13.sp, Muted); Chip("Retry tracks", onClick = onRetryTracks) }
                state.tracksMore -> Chip("More tracks", onClick = onMoreTracks)
            }
        }
    }
    actionsTrack?.let { track ->
        FeatureDialog(track.title, { actionsTrack = null }) {
            SimpleRow("Start radio", "Build a mix from this song") { actionsTrack = null; onRadio(track) }
            SimpleRow("Play next", "Add this song to the queue") { actionsTrack = null; onQueue(track) }
            SimpleRow("Add to queue", "After the last song") { actionsTrack = null; onAppend(track) }
            SimpleRow("Add to playlist", "Manage your music") { actionsTrack = null; onPlaylist(track) }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>, favoriteIds: Set<String>,
    onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit, onRadio: (Track) -> Unit,
    onQueue: (Track) -> Unit, onPlaylist: (Track) -> Unit,
    onAppend: (Track) -> Unit, removeFromPlaylist: Boolean = false,
    onTags: (Track) -> Unit = {},
    pagination: DiscoverState? = null,
    onLoadMore: (() -> Unit)? = null,
    searchArtists: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    searchAlbums: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    searchLayout: Boolean = false,
    onArtist: (YouTubeMusicRepository.CatalogItem) -> Unit = {},
    onAlbum: (YouTubeMusicRepository.CatalogItem) -> Unit = {},
    searchLeading: (@Composable () -> Unit)? = null,
    searchBrowse: (@Composable () -> Unit)? = null,
    searchEmptyMessage: String? = null,
) {
    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    val listState = rememberLazyListState()
    val prefixCount = if (!searchLayout) 0 else
        (if (searchLeading != null) 1 else 0) + (if (searchBrowse != null) 1 else 0) +
        (if (searchEmptyMessage != null) 1 else 0) + (if (tracks.isNotEmpty()) 2 else 0) +
        (if (searchArtists.isNotEmpty()) 1 + searchArtists.size else 0) +
        (if (searchAlbums.isNotEmpty()) 1 + searchAlbums.size else 0)
    val nearEnd by remember(listState, tracks.size, prefixCount) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= prefixCount + (tracks.size - 4).coerceAtLeast(0)
        }
    }
    LaunchedEffect(nearEnd, pagination?.hasMore, pagination?.loadingMore, pagination?.loadMoreError, tracks.size) {
        if (nearEnd && pagination?.hasMore == true && !pagination.searching &&
            !pagination.loadingMore && pagination.loadMoreError == null) onLoadMore?.invoke()
    }
    val (underlay, insets) = underChrome()
    val bottomPadding = with(LocalDensity.current) { LocalChromeMetrics.current.bottomHeight.toDp() + 12.dp }
    LazyColumn(state = listState, modifier = if (searchLayout) Modifier.fillMaxSize() else underlay,
        contentPadding = if (searchLayout) PaddingValues(bottom = bottomPadding) else insets,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (searchLeading != null) item(key = "search-leading") { searchLeading() }
        if (searchBrowse != null) item(key = "search-browse") { searchBrowse() }
        if (searchEmptyMessage != null) item(key = "search-empty") { Label(searchEmptyMessage, 14.sp, Muted) }
        if (searchLayout && tracks.isNotEmpty()) {
            item(key = "search-top-result") {
                val top = tracks.first()
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Label("Top result", 20.sp, PrimaryText, FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().motionClick { onPlay(top) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AlbumImage(top.artworkUri, Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Label(top.title, 16.sp, PrimaryText, FontWeight.Bold, lines = 2)
                            Label("Track · ${top.artist}", 12.sp, Muted, lines = 1)
                        }
                    }
                }
            }
        }
        if (searchLayout && searchArtists.isNotEmpty()) {
            item(key = "search-artists-heading") { Label("Artists", 20.sp, PrimaryText, FontWeight.Bold, Modifier.padding(top = 12.dp, bottom = 4.dp)) }
            items(searchArtists, key = { "artist:${it.name}" }) { entry -> CatalogResultRow(entry, onArtist) }
        }
        if (searchLayout && searchAlbums.isNotEmpty()) {
            item(key = "search-albums-heading") { Label("Albums", 20.sp, PrimaryText, FontWeight.Bold, Modifier.padding(top = 12.dp, bottom = 4.dp)) }
            items(searchAlbums, key = { "album:${it.name}" }) { entry -> CatalogResultRow(entry, onAlbum) }
        }
        if (searchLayout && tracks.isNotEmpty()) item(key = "search-tracks-heading") { Label("Tracks", 20.sp, PrimaryText, FontWeight.Bold, Modifier.padding(top = 12.dp, bottom = 4.dp)) }
        items(tracks, key = { "track:${it.id}" }) { track ->
            Row(
                (if (LocalReduceMotion.current) Modifier else Modifier.animateItem()).fillMaxWidth().clip(RoundedCornerShape(if (searchLayout) 0.dp else 15.dp))
                    .background(if (searchLayout) Color.Transparent else LocalKanadePalette.current.tint.copy(alpha = 0.055f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumImage(track.artworkUri, Modifier.size(54.dp).motionClick { onPlay(track) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).motionClick { onPlay(track) }) {
                    Label(if (track.source == TrackSource.YOUTUBE) songTitle(track.title, track.artist) else track.title, 13.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
                    Label(track.artist, 11.sp, Muted, lines = 1)
                    Label(when (track.source) { TrackSource.LOCAL -> "ON DEVICE"; TrackSource.AUDIUS -> "AUDIUS STREAM"; TrackSource.YOUTUBE -> "YOUTUBE" },
                        9.sp, Accent)
                    if (track.durationMs > 0) Label(DurationFormatter.format(track.durationMs), 11.sp, Muted)
                }
                TinyButton(KanadeIcon.FAVORITE, if (track.id in favoriteIds) "Remove from favorites" else "Add to favorites", selected = track.id in favoriteIds) { onFavorite(track) }
                TinyButton(KanadeIcon.MORE, "Track actions for ${track.title}") { actionsTrack = track }
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
            SimpleRow("Start radio", "Build a mix from this song") { actionsTrack = null; onRadio(track) }
            SimpleRow(if (track.id in favoriteIds) "Remove favorite" else "Favorite", track.artist) { actionsTrack = null; onFavorite(track) }
            SimpleRow("Add to queue", "After the last song") { actionsTrack = null; onAppend(track) }
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
private fun CatalogResultRow(entry: YouTubeMusicRepository.CatalogItem, onSelect: (YouTubeMusicRepository.CatalogItem) -> Unit) {
    Row(Modifier.fillMaxWidth().motionClick { onSelect(entry) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        AlbumImage(entry.artwork, Modifier.size(50.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Label(entry.name, 14.sp, PrimaryText, FontWeight.SemiBold, lines = 1)
            Label(entry.subtitle, 12.sp, Muted, lines = 1)
        }
    }
}

@Composable
private fun TinyButton(icon: KanadeIcon, description: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(39.dp).clip(CircleShape).motionClick(onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        KanadeGlyph(icon, if (selected) Accent else Muted, selected = selected, size = 21.dp)
    }
}

@Composable
private fun IconTextButton(icon: KanadeIcon, text: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceLight)
        .motionClick(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KanadeGlyph(icon, PrimaryText, size = 17.dp)
        Spacer(Modifier.width(5.dp))
        Label(text, 12.sp, PrimaryText)
    }
}

@Composable private fun SimpleRow(title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .glassSurface(15.dp).motionClick(onClick = click).padding(15.dp),
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
private fun Modifier.glassSurface(radius: androidx.compose.ui.unit.Dp, dialog: Boolean = false): Modifier =
    clip(RoundedCornerShape(radius)).background(SurfaceLight)

@Composable
private fun MiniPlayer(
    state: PlaybackState,
    backdrop: io.github.fletchmckee.liquid.LiquidState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().warmRaisedGlass(backdrop, radius = 24.dp, elevation = 12.dp)
        .pointerInput(Unit) {
        var dy = 0f
        detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = {
            if (dy < -32.dp.toPx()) onExpand()
        }) { change, delta -> change.consume(); dy += delta }
    }) {
        Row(Modifier.fillMaxWidth().motionClick(onClick = onExpand)
            .semantics { contentDescription = "Open now playing" }
            .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AlbumImage(state.artworkUri, Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Label(state.title, 14.sp, PrimaryText, FontWeight.Bold, lines = 1)
                Label(state.artist, 12.sp, Muted, lines = 1)
            }
            MinimalGlassControlButton(
                if (state.playing) KanadeIcon.PAUSE else KanadeIcon.PLAY,
                if (state.playing) "Pause" else "Play",
                active = state.playing, reduceMotion = LocalReduceMotion.current, backdrop = backdrop,
                contentColor = if (state.playing) Accent else PrimaryText, onClick = onToggle,
            )
            Spacer(Modifier.width(4.dp))
            MinimalGlassControlButton(
                KanadeIcon.NEXT, "Next",
                reduceMotion = LocalReduceMotion.current, backdrop = backdrop, contentColor = PrimaryText, onClick = onNext,
            )
        }
        val miniProgress = if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else 0f
        val miniReduceMotion = LocalReduceMotion.current
        val smoothMiniProgress by animateFloatAsState(
            targetValue = miniProgress,
            animationSpec = tween(if (miniReduceMotion) 0 else 220, easing = LinearEasing),
            label = "Mini player progress",
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 5.dp)
            .height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.19f))) {
            Box(Modifier.fillMaxWidth(smoothMiniProgress).height(2.dp).background(Accent))
        }
    }
}

@Composable
private fun onWifi(): Boolean {
    val manager = LocalContext.current.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun connected() = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    var wifi by remember(manager) { mutableStateOf(connected()) }
    DisposableEffect(manager) {
        val listener = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
            override fun onLost(network: Network) { wifi = connected() }
        }
        manager.registerDefaultNetworkCallback(listener)
        wifi = connected()
        onDispose { manager.unregisterNetworkCallback(listener) }
    }
    return wifi
}

@Composable
private fun VideoBackdrop(state: PlaybackState) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestPlayback by rememberUpdatedState(state)
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var videoReady by remember { mutableStateOf(false) }
    var aspect by remember { mutableStateOf(1f) }
    val videoPlayer = remember { ExoPlayer.Builder(context).build().apply { volume = 0f } }
    DisposableEffect(owner, videoPlayer) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (!foreground) videoPlayer.pause()
        }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
            override fun onRenderedFirstFrame() { videoReady = true }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { videoReady = false }
        }
        videoPlayer.addListener(listener)
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); videoPlayer.removeListener(listener); videoPlayer.release() }
    }
    LaunchedEffect(state.mediaId, foreground) {
        videoReady = false
        videoPlayer.stop()
        videoPlayer.clearMediaItems()
        if (foreground && state.mediaId.startsWith("youtube:")) {
            val id = state.mediaId.removePrefix("youtube:")
            val url = try { withContext(Dispatchers.IO) { YouTubeMusicRepository.videoUrl(id) } }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { null }
            val playback = resolvedVideoPlayback(state.mediaId, latestPlayback)
            if (url != null && playback != null) {
                // Resolving a stream takes time; playback may have been paused or sought meanwhile.
                videoPlayer.setMediaItem(MediaItem.fromUri(url))
                videoPlayer.prepare()
                videoPlayer.seekTo(playback.positionMs)
                videoPlayer.setPlaybackSpeed(playback.speed)
                videoPlayer.playWhenReady = playback.playing
            }
        }
    }
    LaunchedEffect(state.positionMs, state.playing, state.speed, foreground) {
        if (foreground && videoPlayer.mediaItemCount > 0) {
            if (kotlin.math.abs(videoPlayer.currentPosition - state.positionMs) > 1800) videoPlayer.seekTo(state.positionMs)
            videoPlayer.setPlaybackSpeed(state.speed)
            videoPlayer.playWhenReady = state.playing
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        val width = maxOf(maxWidth, maxHeight * aspect)
        AndroidView(factory = { TextureView(it).apply { isOpaque = false; videoPlayer.setVideoTextureView(this) } },
            modifier = Modifier.requiredSize(width, width / aspect).alpha(if (videoReady) 1f else 0f))
    }
}

@Composable
private fun PlayerButton(icon: KanadeIcon, description: String, large: Boolean = false,
    selected: Boolean = false, onClick: () -> Unit) {
    val size = if (large) 60.dp else 48.dp
    Box(Modifier.size(size)
        .motionClick(onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        KanadeGlyph(icon, if (selected) lerp(Accent, Color.White, 0.70f) else Color.White,
            selected = selected, size = if (large) 34.dp else 24.dp, bold = true)
        if (selected) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
            .width(10.dp).height(2.dp).background(Color.White))
    }
}

@Composable
private fun QueueCoverStack(state: PlaybackState, coverSize: Dp, onPlayAt: (Int) -> Unit) {
    if (state.queue.isEmpty()) {
        AlbumImage(state.artworkUri, Modifier.size(coverSize))
        return
    }
    val current = state.index.coerceIn(state.queue.indices)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = current)
    val reduced = LocalReduceMotion.current
    LaunchedEffect(state.mediaId, current, reduced) {
        if (reduced) list.scrollToItem(current) else list.animateScrollToItem(current)
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = minOf(coverSize, maxWidth)
        val overlap = side * 0.42f
        LazyRow(
            state = list,
            modifier = Modifier.fillMaxWidth().height(side),
            contentPadding = PaddingValues(horizontal = (maxWidth - side) / 2),
            horizontalArrangement = Arrangement.spacedBy(-overlap),
            flingBehavior = rememberSnapFlingBehavior(list),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(state.queue.size, key = { index -> "${state.queue[index].id}:$index" }) { index ->
                val entry = state.queue[index]
                val distance = remember(list, index) {
                    derivedStateOf {
                        val info = list.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                        if (item == null || item.size == 0) 1f else {
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            (kotlin.math.abs(item.offset + item.size / 2f - center) / item.size).coerceAtMost(1f)
                        }
                    }
                }
                Box(Modifier.size(side).zIndex(1f - distance.value)
                    .graphicsLayer {
                        val depth = if (reduced) 0f else distance.value
                        scaleX = 1f - depth * 0.22f
                        scaleY = scaleX
                        rotationZ = if (reduced) 0f else (index - current).coerceIn(-1, 1) * depth * 5f
                        alpha = 1f - depth * 0.18f
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .motionClick(onClickLabel = "Play song") {
                        if (index != current || !state.playing) onPlayAt(index)
                    }
                    .semantics { contentDescription = "${entry.title}, ${entry.artist}" }) {
                    AlbumImage(entry.artworkUri, Modifier.fillMaxSize())
                    if (index == current) Label(if (state.playing) "Playing" else "Paused", 11.sp, Color.White,
                        modifier = Modifier.align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.78f)).padding(4.dp))
                    if (entry.artworkUri.isNullOrBlank()) {
                        Label(entry.title, 12.sp, PrimaryText, FontWeight.Bold,
                            Modifier.align(Alignment.BottomCenter).padding(8.dp), lines = 2)
                    }
                }
            }
        }
    }
    val centered by remember(list, current) { derivedStateOf {
        val info = list.layoutInfo
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2f - center) }?.index ?: current
    } }
    state.queue.getOrNull(centered)?.let { entry ->
        val title = if (entry.id.startsWith("youtube:")) songTitle(entry.title, entry.artist) else entry.title
        Label(if (centered == current) entry.artist else "$title · ${entry.artist}", 14.sp, Color.White, lines = 2)
    }

}

@Composable
private fun NowPlayingScreen(
    state: PlaybackState, lyrics: LyricsState, videoMode: String, onClose: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit,
    onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit, onSeek: (Long) -> Unit,
    onPlayAt: (Int) -> Unit, onLyrics: () -> Unit, lyricDelay: Long, onTools: () -> Unit,
) {
    val glassBackdrop = rememberLiquidState()
    Box(Modifier.fillMaxSize()) {
        // Only the artwork/backdrop is sampled: never the player controls themselves.
        Box(Modifier.fillMaxSize().background(Background)) {
            if (!state.artworkUri.isNullOrBlank()) {
                AsyncImage(model = state.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(55.dp).alpha(0.38f))
            }
            if (state.mediaId.startsWith("youtube:") && (videoMode == "Always" || (videoMode == "Wi-Fi only" && onWifi()))) VideoBackdrop(state)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                Color(0xFF070B14).copy(alpha = 0.42f),
                Color(0xFF070B14).copy(alpha = 0.22f),
                Color(0xFF070B14).copy(alpha = 0.76f)
            ))))
        }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            val wide = maxWidth > maxHeight && maxWidth >= 520.dp
            val coverSize = if (wide) 100.dp else (maxHeight * 0.22f).coerceIn(96.dp, 184.dp)
            val shortWindow = maxHeight < 320.dp
            Column(Modifier.fillMaxSize()
                .then(if (shortWindow) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val upper = if (shortWindow) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState())
                Column(upper.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(KanadeIcon.BACK, "Back", onClick = onClose)
                        val title = remember(state.mediaId, state.title, state.artist) {
                            if (state.mediaId.startsWith("youtube:")) songTitle(state.title, state.artist) else state.title
                        }
                        BasicText(title, Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, style = TextStyle(fontFamily = LocalAppFont.current,
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                                textAlign = TextAlign.Center, shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 6f)))
                        PlayerButton(KanadeIcon.MORE, "Player tools", onClick = onTools)
                    }
                    Spacer(Modifier.height(12.dp))
                    val cover: @Composable () -> Unit = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            QueueCoverStack(state, coverSize, onPlayAt)
                        }
                    }
                    val lyricStage: @Composable () -> Unit = {
                        val index = LrcParser.current(lyrics.lines, lyricClock(state.positionMs, lyricDelay))
                        val line = lyrics.lines.getOrNull(index)
                        val reduceLyricMotion = LocalReduceMotion.current
                        Column(Modifier.fillMaxWidth().motionClick(onClick = onLyrics)
                            .padding(horizontal = 12.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            if (line != null) {
                                lyrics.lines.getOrNull(index - 1)?.let { previous ->
                                    Label(previous.text, 16.sp, Color.White.copy(alpha = 0.48f), lines = 2)
                                    Spacer(Modifier.height(12.dp))
                                }
                                AnimatedContent(
                                    targetState = index,
                                    transitionSpec = {
                                        if (reduceLyricMotion) {
                                            fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                        } else {
                                            (slideInVertically(animationSpec = tween(330), initialOffsetY = { it / 3 }) +
                                                fadeIn(animationSpec = tween(330))) togetherWith
                                                (slideOutVertically(animationSpec = tween(260), targetOffsetY = { -it / 3 }) +
                                                    fadeOut(animationSpec = tween(260)))
                                        }
                                    },
                                    label = "Synchronized lyric transition",
                                ) { activeIndex ->
                                    Label(lyrics.lines.getOrNull(activeIndex)?.text.orEmpty(),
                                        30.sp, Color.White, FontWeight.Bold, lines = 3)
                                }
                                lyrics.lines.getOrNull(index + 1)?.let { next ->
                                    Spacer(Modifier.height(12.dp))
                                    Label(next.text, 16.sp, Color.White.copy(alpha = 0.56f), lines = 2)
                                }
                            } else if (lyrics.lines.isNotEmpty()) {
                                Label("♪", 30.sp, Color.White, FontWeight.Bold)
                            } else {
                                Label(lyrics.plain ?: lyrics.message, 16.sp, Color.White, lines = 3)
                            }
                        }
                    }
                    if (wide) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { cover() }
                            Box(Modifier.weight(2f)) { lyricStage() }
                        }
                    } else {
                        cover()
                        Spacer(Modifier.height(18.dp))
                        lyricStage()
                    }
                    state.error?.let { Label(it, 12.sp, ErrorColor) }
                }
                Spacer(Modifier.height(8.dp))
                // One continuous glass panel for BOTH the active equalizer seekbar
                // and all five controls. Every button remains a separate element.
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF1B1D20)).padding(12.dp)) {
                    PlaybackProgress(state, onSeek)
                    Spacer(Modifier.height(9.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MinimalGlassControlButton(
                            KanadeIcon.SHUFFLE,
                            if (state.shuffle) "Shuffle on" else "Shuffle off",
                            active = state.shuffle,
                            reduceMotion = LocalReduceMotion.current,
                            backdrop = glassBackdrop, glass = false,
                            onClick = onShuffle,
                        )
                        MinimalGlassControlButton(
                            KanadeIcon.PREVIOUS, "Previous",
                            reduceMotion = LocalReduceMotion.current,
                            backdrop = glassBackdrop, glass = false,
                            onClick = onPrevious,
                        )
                        LiquidGlassPlayPauseButton(
                            playing = state.playing,
                            backdrop = glassBackdrop,
                            reduceMotion = LocalReduceMotion.current,
                            onToggle = onToggle,
                        )
                        MinimalGlassControlButton(
                            KanadeIcon.NEXT, "Next",
                            reduceMotion = LocalReduceMotion.current,
                            backdrop = glassBackdrop, glass = false,
                            onClick = onNext,
                        )
                        MinimalGlassControlButton(
                            if (state.repeatMode == Player.REPEAT_MODE_ONE) KanadeIcon.REPEAT_ONE else KanadeIcon.REPEAT,
                            if (state.repeatMode == Player.REPEAT_MODE_OFF) "Repeat off" else "Repeat on",
                            active = state.repeatMode != Player.REPEAT_MODE_OFF,
                            reduceMotion = LocalReduceMotion.current,
                            backdrop = glassBackdrop, glass = false,
                            onClick = onRepeat,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgress(state: PlaybackState, onSeek: (Long) -> Unit) {
    var preview by remember(state.mediaId) { mutableStateOf<Float?>(null) }
    val rawProgress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 220, easing = LinearEasing),
        label = "Smooth playback progress",
    )
    val progress = preview ?: animatedProgress
    Box(Modifier.fillMaxWidth().height(40.dp)
        .semantics {
            contentDescription = "Playback position"
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            setProgress { value ->
                if (state.durationMs > 0) { onSeek((state.durationMs * value.coerceIn(0f, 1f)).toLong()); true }
                else false
            }
        }
        .pointerInput(state.mediaId, state.durationMs) {
            if (state.durationMs <= 0) return@pointerInput
            try {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    fun fraction(x: Float) = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    preview = fraction(down.position.x)
                    do {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        preview = fraction(change.position.x)
                        change.consume()
                        if (!change.pressed) onSeek((state.durationMs * (preview ?: 0f)).toLong())
                    } while (change.pressed)
                    preview = null
                }
            } finally { preview = null }
        }, contentAlignment = Alignment.CenterStart) {
        SpectrumBar(state.artworkUri, progress, state.playing, LocalReduceMotion.current, Accent)
        preview?.let {
            BoxWithConstraints(Modifier.fillMaxWidth().align(Alignment.TopStart)) {
                val time = DurationFormatter.format((state.durationMs * it).toLong())
                Box(Modifier.padding(start = ((maxWidth - 48.dp).coerceAtLeast(0.dp) * it))
                    .background(Color.Black.copy(alpha = 0.8f))) { Label(time, 12.sp, Color.White) }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(DurationFormatter.format(state.positionMs), 12.sp, Color.White)
        Label(DurationFormatter.format(state.durationMs), 12.sp, Color.White)
    }
}

@Composable
private fun FeatureDialog(title: String, dismiss: () -> Unit, content: @Composable () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    val reduced = LocalReduceMotion.current
    LaunchedEffect(Unit) { entered = true }
    val dialogScale by animateFloatAsState(if (entered || reduced) 1f else 0.96f,
        animationSpec = tween(if (reduced) 0 else 180), label = "dialog entrance")
    val dialogAlpha by animateFloatAsState(if (entered || reduced) 1f else 0f,
        animationSpec = tween(if (reduced) 0 else 160), label = "dialog opacity")
    Dialog(onDismissRequest = dismiss) {
        BoxWithConstraints(Modifier.fillMaxWidth().graphicsLayer {
            scaleX = dialogScale; scaleY = dialogScale; alpha = dialogAlpha
        }) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.9f)
                .glassSurface(25.dp, dialog = true).verticalScroll(rememberScrollState()).padding(19.dp)) {
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

