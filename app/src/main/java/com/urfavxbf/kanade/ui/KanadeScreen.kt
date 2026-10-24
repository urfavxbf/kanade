package com.urfavxbf.kanade.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BasicText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.playback.PlaybackState

private val Background = Color(0xFF0B0D16)
private val Surface = Color(0xFF171925)
private val SurfaceLight = Color(0xFF242437)
private val Accent = Color(0xFFBDA5FF)
private val Pink = Color(0xFFF6A7D4)
private val PrimaryText = Color(0xFFF6F3FF)
private val Muted = Color(0xFF9898AE)

private enum class Page { LIBRARY, DISCOVER }

@Composable
fun KanadeScreen(model: KanadeViewModel) {
    val library by model.library.collectAsState()
    val discover by model.discover.collectAsState()
    val playback by model.playback.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(Page.LIBRARY) }
    var libraryFilter by rememberSaveable { mutableStateOf("") }
    var onlineSearch by rememberSaveable { mutableStateOf("") }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) model.loadLocal()
    }
    LaunchedEffect(Unit) {
        if (model.hasPermission()) model.loadLocal()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Accent, Pink))),
                contentAlignment = Alignment.Center,
            ) { KanadeText("♫", size = 26.sp, color = Background, weight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                KanadeText("KANADE", size = 21.sp, weight = FontWeight.Bold)
                KanadeText("Your sound. Your space.", size = 11.sp, color = Muted)
            }
            Box(
                Modifier.clip(RoundedCornerShape(18.dp)).background(SurfaceLight)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                KanadeText("HI-FI  •  MUSIC", size = 10.sp, color = Accent, weight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(30.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PageTab("My library", page == Page.LIBRARY, Modifier.weight(1f)) { page = Page.LIBRARY }
            PageTab("Explore online", page == Page.DISCOVER, Modifier.weight(1f)) { page = Page.DISCOVER }
        }
        Spacer(Modifier.height(25.dp))

        Box(Modifier.weight(1f)) {
            when (page) {
                Page.LIBRARY -> {
                    Column {
                        KanadeText("Made for your music", size = 25.sp, weight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        KanadeText("On-device songs. No signal required.", size = 13.sp, color = Muted)
                        Spacer(Modifier.height(20.dp))
                        if (!model.hasPermission()) {
                            EmptyState(
                                symbol = "♫",
                                title = "Your music belongs here",
                                detail = "Grant music access to discover the tracks stored on your phone.",
                                action = "Allow music access",
                            ) { permissions.launch(model.permission()) }
                        } else {
                            SearchField(
                                value = libraryFilter,
                                hint = "Find in my library",
                                onChange = { libraryFilter = it },
                                onSearch = {},
                            )
                            Spacer(Modifier.height(16.dp))
                            val shown = remember(library.tracks, libraryFilter) {
                                library.tracks.filter {
                                    it.title.contains(libraryFilter, ignoreCase = true) ||
                                        it.artist.contains(libraryFilter, ignoreCase = true)
                                }
                            }
                            KanadeText(
                                when {
                                    library.loading -> "Scanning device…"
                                    library.error != null -> library.error.orEmpty()
                                    else -> "${shown.size} TRACKS  •  OFFLINE"
                                },
                                size = 11.sp, color = Muted, weight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(13.dp))
                            if (!library.loading && shown.isEmpty()) {
                                EmptyState("♪", "Nothing to play yet",
                                    "Audio on your device appears here automatically.",
                                    "Rescan device") { model.loadLocal() }
                            } else {
                                TrackList(shown, onSelect = model::playLocal)
                            }
                        }
                    }
                }
                Page.DISCOVER -> {
                    Column {
                        KanadeText("A world of sound", size = 25.sp, weight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        KanadeText("Stream music from the Audius catalog.", size = 13.sp, color = Muted)
                        Spacer(Modifier.height(20.dp))
                        SearchField(
                            value = onlineSearch,
                            hint = "Artists, songs, moods…",
                            onChange = { onlineSearch = it },
                            onSearch = { model.search(onlineSearch) },
                        )
                        Spacer(Modifier.height(15.dp))
                        ActionChip("Search Audius") { model.search(onlineSearch) }
                        Spacer(Modifier.height(19.dp))
                        KanadeText(discover.message, size = 12.sp, color = Muted)
                        Spacer(Modifier.height(13.dp))
                        when {
                            discover.searching -> EmptyState("◌", "Searching Audius",
                                "Finding music for you.")
                            discover.tracks.isEmpty() -> EmptyState(
                                "♬", "Discover something new",
                                "Find artists and tracks. Online playback needs a connection."
                            )
                            else -> TrackList(discover.tracks, onSelect = model::playOnline)
                        }
                    }
                }
            }
        }

        if (playback.hasItem) {
            PlayerPanel(
                state = playback,
                onToggle = model.playback::toggle,
                onNext = model.playback::next,
                onPrevious = model.playback::previous,
                onShuffle = model.playback::toggleShuffle,
                onSeek = model.playback::seek,
            )
        }
        playback.error?.let { message ->
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KanadeText(message, color = Pink, size = 12.sp, modifier = Modifier.weight(1f))
                Box(Modifier.clickable { model.playback.clearError() }.padding(8.dp)) {
                    KanadeText("Dismiss", color = Accent, size = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PageTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) SurfaceLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        KanadeText(label, size = 13.sp, color = if (selected) PrimaryText else Muted,
            weight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SearchField(
    value: String, hint: String, onChange: (String) -> Unit, onSearch: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        maxLines = 1,
        cursorBrush = SolidColor(Accent),
        textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { input ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Surface).padding(horizontal = 18.dp, vertical = 17.dp)
            ) {
                if (value.isEmpty()) KanadeText(hint, color = Muted, size = 14.sp)
                input()
            }
        },
    )
}

@Composable
private fun TrackList(tracks: List<Track>, onSelect: (Track) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(tracks, key = { it.id }) { track ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Surface)
                    .clickable(onClickLabel = "Play ${track.title}", onClick = { onSelect(track) })
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(SurfaceLight, Color(0xFF3D305B)))),
                    contentAlignment = Alignment.Center,
                ) { KanadeText("♫", size = 28.sp, color = Accent) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    KanadeText(track.title, weight = FontWeight.SemiBold, size = 14.sp, lines = 1)
                    Spacer(Modifier.height(5.dp))
                    KanadeText(track.artist, color = Muted, size = 12.sp, lines = 1)
                }
                KanadeText("▶", color = Accent, size = 16.sp)
                Spacer(Modifier.width(7.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(
    symbol: String,
    title: String,
    detail: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(85.dp).clip(RoundedCornerShape(27.dp))
                .background(Brush.linearGradient(listOf(SurfaceLight, Surface))),
            contentAlignment = Alignment.Center,
        ) { KanadeText(symbol, size = 40.sp, color = Accent) }
        Spacer(Modifier.height(17.dp))
        KanadeText(title, size = 17.sp, weight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        KanadeText(detail, color = Muted, size = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            ActionChip(action, onAction)
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(15.dp)).background(Accent)
            .clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
    ) { KanadeText(label, size = 13.sp, color = Background, weight = FontWeight.Bold) }
}

@Composable
private fun PlayerPanel(
    state: PlaybackState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF29233C), Color(0xFF1A1928), Color(0xFF161724))
                )
            ).padding(horizontal = 17.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(47.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Accent, Pink))),
                contentAlignment = Alignment.Center,
            ) { KanadeText("♫", size = 27.sp, color = Background) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                KanadeText(state.title, size = 14.sp, weight = FontWeight.SemiBold, lines = 1)
                KanadeText(state.artist, size = 12.sp, color = Muted, lines = 1)
            }
            PlayerControl("⏮", "Previous", onPrevious)
            PlayerControl(if (state.playing) "❚❚" else "▶", "Play or pause", onToggle, accent = true)
            PlayerControl("⏭", "Next", onNext)
        }
        Spacer(Modifier.height(13.dp))
        Box(
            Modifier.fillMaxWidth().height(12.dp)
                .pointerInput(state.durationMs) {
                    detectTapGestures { point ->
                        if (size.width > 0 && state.durationMs > 0L) {
                            onSeek((state.durationMs * (point.x / size.width).coerceIn(0f, 1f)).toLong())
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceLight))
            Box(
                Modifier.fillMaxWidth(
                    if (state.durationMs > 0L) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                    else 0f
                ).height(4.dp).clip(CircleShape).background(Accent),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanadeText(DurationFormatter.format(state.positionMs), color = Muted, size = 10.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onShuffle)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                KanadeText("SHUFFLE", color = if (state.shuffle) Accent else Muted,
                    size = 10.sp, weight = FontWeight.Medium)
            }
            Spacer(Modifier.weight(1f))
            KanadeText(DurationFormatter.format(state.durationMs), color = Muted, size = 10.sp)
        }
    }
}

@Composable
private fun PlayerControl(symbol: String, label: String, onClick: () -> Unit, accent: Boolean = false) {
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .background(if (accent) Accent else Color.Transparent)
            .clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        KanadeText(symbol, size = if (accent) 19.sp else 22.sp,
            color = if (accent) Background else PrimaryText, weight = FontWeight.Bold)
    }
}

@Composable
private fun KanadeText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = PrimaryText,
    weight: FontWeight = FontWeight.Normal,
    lines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight),
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}
