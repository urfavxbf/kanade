package com.urfavxbf.kanade.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urfavxbf.kanade.data.DisplayTagOverrides
import com.urfavxbf.kanade.data.YouTubeMusicRepository
import com.urfavxbf.kanade.data.SearchPagination
import org.schabi.newpipe.extractor.Page
import com.urfavxbf.kanade.data.KanadeDatabase
import com.urfavxbf.kanade.data.LocalMusicRepository
import com.urfavxbf.kanade.data.MusicStore
import com.urfavxbf.kanade.data.Playlist
import com.urfavxbf.kanade.data.PlaylistExchange
import com.urfavxbf.kanade.data.toTrack
import com.urfavxbf.kanade.lyrics.OnlineLyrics
import com.urfavxbf.kanade.lyrics.LrcParser
import com.urfavxbf.kanade.lyrics.LyricLine
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import com.urfavxbf.kanade.playback.PlaybackConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryState(
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)
data class DiscoverState(
    val tracks: List<Track> = emptyList(),
    val artists: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    val albums: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    val searching: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val loadMoreError: String? = null,
    val message: String = "Search for songs or artists on YouTube.",
)
data class HomeDiscovery(
    val recommended: List<Track> = emptyList(), val hits: List<Track> = emptyList(),
    val artists: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    val albums: List<YouTubeMusicRepository.CatalogItem> = emptyList(),
    val loading: Boolean = true, val error: String? = null,
)
data class LyricsState(val lines: List<LyricLine> = emptyList(), val message: String = "Lyrics unavailable", val plain: String? = null)

class KanadeViewModel(app: Application) : AndroidViewModel(app) {
    private val local = LocalMusicRepository(app.contentResolver)
    private val tagOverrides = DisplayTagOverrides(app)
    private val store = MusicStore(KanadeDatabase.open(app).musicDao())
    private val lyricPrefs = app.getSharedPreferences("kanade_lyrics", Context.MODE_PRIVATE)
    private val appearancePrefs = app.getSharedPreferences("kanade_appearance", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(ThemeMode.fromStored(
        appearancePrefs.getString("theme_mode", null),
        if (appearancePrefs.contains("dark_theme")) appearancePrefs.getBoolean("dark_theme", true) else null,
    ))
    val themeMode: StateFlow<ThemeMode> = _themeMode
    private val _colorTheme = MutableStateFlow(appearancePrefs.getString("color_theme", "Kanade Green") ?: "Kanade Green")
    val colorTheme: StateFlow<String> = _colorTheme
    private val _appFont = MutableStateFlow(appearancePrefs.getString("app_font", "Kanade Sans") ?: "Kanade Sans")
    val appFont: StateFlow<String> = _appFont

    fun setAppFont(name: String) {
        appearancePrefs.edit().putString("app_font", name).apply()
        _appFont.value = name
    }

    val reduceMotion = MutableStateFlow(appearancePrefs.getBoolean("reduce_motion", false))
    val videoMode = MutableStateFlow(appearancePrefs.getString("video_mode", "Always") ?: "Always")
    val pins = MutableStateFlow(appearancePrefs.getStringSet("pinned_playlists", emptySet()).orEmpty().toSet())

    fun changeMotion(reduce: Boolean) {
        reduceMotion.value = reduce
        appearancePrefs.edit().putBoolean("reduce_motion", reduce).apply()
    }

    fun changeVideoMode(mode: String) {
        videoMode.value = mode
        appearancePrefs.edit().putString("video_mode", mode).apply()
    }

    fun togglePin(id: Long) {
        val key = id.toString()
        val next = if (key in pins.value) pins.value - key else pins.value + key
        pins.value = next
        appearancePrefs.edit().putStringSet("pinned_playlists", next).apply()
    }

    val playback = PlaybackConnection(app)
    private val known = mutableMapOf<String, Track>()

    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _discover = MutableStateFlow(DiscoverState())
    val discover: StateFlow<DiscoverState> = _discover
    private val _homeDiscovery = MutableStateFlow(HomeDiscovery())
    val homeDiscovery: StateFlow<HomeDiscovery> = _homeDiscovery
    private var homeJob: Job? = null
    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists
    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks
    private val _recent = MutableStateFlow<List<Track>>(emptyList())
    val recent: StateFlow<List<Track>> = _recent
    private val _topTracks = MutableStateFlow<List<Track>>(emptyList())
    val topTracks: StateFlow<List<Track>> = _topTracks
    private val _totalPlays = MutableStateFlow(0)
    val totalPlays: StateFlow<Int> = _totalPlays
    private val _totalListenMs = MutableStateFlow(0L)
    val totalListenMs: StateFlow<Long> = _totalListenMs
    private val _lyrics = MutableStateFlow(LyricsState())
    val lyrics: StateFlow<LyricsState> = _lyrics

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var searchGeneration = 0L
    private var activeSearch = ""
    private var nextSearchPage: Page? = null
    private var playlistJob: Job? = null
    private var lyricJob: Job? = null
    private var lastLyricTrack = ""
    private var playingId = ""
    private var listeningMs = 0L
    private var recordedEntryId: Long? = null

    init {
        loadHomeDiscovery()
        viewModelScope.launch { store.favorites.collect { _favoriteIds.value = it.toSet() } }
        viewModelScope.launch { store.favoriteTracks.collect { _favorites.value = it.map { item -> item.toTrack() } } }
        viewModelScope.launch { store.playlists.collect { _playlists.value = it } }
        viewModelScope.launch { store.recent.collect { _recent.value = it.map { item -> item.toTrack() } } }
        viewModelScope.launch { store.topPlayedTracks.collect { _topTracks.value = it.map { item -> item.toTrack() } } }
        viewModelScope.launch { store.totalPlays.collect { _totalPlays.value = it } }
        viewModelScope.launch { store.totalListenMs.collect { _totalListenMs.value = it } }
        viewModelScope.launch {
            while (isActive) {
                playback.sync()
                val state = playback.state.value
                if (playingId != state.mediaId) {
                    recordedEntryId?.let { store.updateListenDuration(it, listeningMs) }
                    playingId = state.mediaId
                    listeningMs = 0L
                    recordedEntryId = null
                    if (lastLyricTrack != playingId) {
                        lastLyricTrack = playingId
                        loadSavedLyrics(playingId)
                    }
                }
                if (state.playing && state.mediaId.isNotBlank()) listeningMs += 800
                if (recordedEntryId == null && listeningMs >= 15_200) {
                    findTrack(state.mediaId)?.let { recordedEntryId = store.recordPlay(it, listeningMs) }
                } else if (recordedEntryId != null && listeningMs > 0 && listeningMs % 4_000 < 800) {
                    recordedEntryId?.let { store.updateListenDuration(it, listeningMs) }
                }
                delay(800)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        appearancePrefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setColorTheme(name: String) {
        _colorTheme.value = name
        appearancePrefs.edit().putString("color_theme", name).apply()
    }

    val options = getApplication<Application>().getSharedPreferences("listening_options", Context.MODE_PRIVATE)
    val folders = MutableStateFlow<List<String>>(emptyList())
    private var scanned = emptyList<Track>()

    fun applyFolders() {
        val include = options.getStringSet("include", emptySet()).orEmpty()
        val exclude = options.getStringSet("exclude", emptySet()).orEmpty()
        val visible = scanned.filter { folderVisible(it.folder, include, exclude) }
        _library.value = LibraryState(tracks = visible)
    }

    fun saveRadioMix(name: String) {
        val tracks = playback.queueTracks()
        if (name.isBlank() || tracks.isEmpty() || tracks.size != playback.state.value.queue.size) {
            _radioMessage.value = "Could not save this mix. Check the playlist name and queue."
            return
        }
        viewModelScope.launch {
            try {
                store.saveMix(name.trim().take(80), tracks)
                _radioMessage.value = "Playlist saved"
            } catch (e: android.database.SQLException) {
                _radioMessage.value = "Could not save the playlist. Try another name."
            }
        }
    }

    fun hideStationSong() {
        val station = playback.station
        val current = playback.state.value
        if (station.isEmpty() || current.mediaId == station || current.mediaId.isEmpty()) return
        val key = "hidden:$station"
        options.edit().putStringSet(key, options.getStringSet(key, emptySet()).orEmpty() + current.mediaId).apply()
        playback.remove(current.index)
    }

    fun permission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(), permission()
    ) == PackageManager.PERMISSION_GRANTED

    fun loadLocal() {
        if (!hasPermission()) {
            _library.value = LibraryState(error = "Allow music access to show your offline songs.")
            return
        }
        viewModelScope.launch {
            _library.value = _library.value.copy(loading = true, error = null)
            try {
                val tracks = local.load().map(tagOverrides::apply)
                tracks.forEach { known[it.id] = it }
                scanned = tracks
                folders.value = tracks.map { it.folder }.distinct().sorted()
                applyFolders()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _library.value = LibraryState(error = "Could not scan your music. Try again.")
            }
        }
    }

    private suspend fun <T> homeResult(fallback: T, block: suspend () -> T): T =
        try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { fallback }

    fun loadHomeDiscovery() {
        homeJob?.cancel()
        _homeDiscovery.value = HomeDiscovery()
        homeJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.supervisorScope {
                    val recommended = async { homeResult(emptyList()) { YouTubeMusicRepository.search("popular music Philippines").tracks } }
                    val hits = async { homeResult(emptyList()) { YouTubeMusicRepository.search("top hits Philippines").tracks } }
                    val artists = async { homeResult(emptyList()) { YouTubeMusicRepository.searchCatalog("popular Philippines", true) } }
                    val albums = async { homeResult(emptyList()) { YouTubeMusicRepository.searchCatalog("popular Philippines", false) } }
                    val first = recommended.await()
                    val top = hits.await()
                    val performers = artists.await()
                    val records = albums.await()
                    (first + top).forEach { known[it.id] = it }
                    _homeDiscovery.value = HomeDiscovery(first, top, performers, records, false,
                        if (first.isEmpty() && top.isEmpty() && performers.isEmpty() && records.isEmpty()) "Could not load music. Check your connection." else null)
                }
            } catch (e: CancellationException) { throw e }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchGeneration++
        nextSearchPage = null
        activeSearch = ""
        val normalized = query.trim()
        if (normalized.isEmpty() || normalized.length > 120) {
            _discover.value = DiscoverState(message = "Enter a song or artist (up to 120 characters).")
            return
        }
        if (!hasNetwork()) {
            _discover.value = DiscoverState(message = "You're offline. Your local songs still work.")
            return
        }
        activeSearch = normalized
        val generation = searchGeneration
        _discover.value = DiscoverState(searching = true, message = "Searching YouTube…")
        searchJob = viewModelScope.launch {
            launch {
                try {
                    val artists = YouTubeMusicRepository.searchCatalog(normalized, true)
                    if (generation == searchGeneration) _discover.value = _discover.value.copy(artists = artists)
                } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
            launch {
                try {
                    val albums = YouTubeMusicRepository.searchCatalog(normalized, false)
                    if (generation == searchGeneration) _discover.value = _discover.value.copy(albums = albums)
                } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
            try {
                val page = YouTubeMusicRepository.search(normalized)
                if (generation != searchGeneration) return@launch
                nextSearchPage = page.nextPage
                page.tracks.forEach { known[it.id] = it }
                _discover.value = _discover.value.copy(tracks = page.tracks, searching = false, hasMore = page.nextPage != null,
                    message = if (page.tracks.isEmpty() && page.nextPage == null) "No matches. Try another song."
                        else "${page.tracks.size} results")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (generation == searchGeneration) {
                    _discover.value = _discover.value.copy(searching = false, message = "Couldn't load YouTube results. Check your connection and try again.")
                }
            }
        }
    }

    fun loadMoreSearchResults() {
        val page = nextSearchPage ?: return
        val state = _discover.value
        if (state.searching || state.loadingMore || activeSearch.isBlank()) return
        if (!hasNetwork()) {
            _discover.value = state.copy(loadMoreError = "You're offline. Connect and try again.")
            return
        }
        val query = activeSearch
        val generation = searchGeneration
        _discover.value = state.copy(loadingMore = true, loadMoreError = null)
        loadMoreJob = viewModelScope.launch {
            try {
                val result = YouTubeMusicRepository.searchNext(query, page)
                if (generation != searchGeneration) return@launch
                result.tracks.forEach { known[it.id] = it }
                val combined = SearchPagination.merge(_discover.value.tracks, result.tracks)
                val next = result.nextPage?.takeUnless { SearchPagination.sameCursor(page, it) }
                nextSearchPage = next
                val noNewTracks = combined.size == _discover.value.tracks.size
                _discover.value = _discover.value.copy(
                    tracks = combined, loadingMore = false, hasMore = next != null,
                    loadMoreError = if (noNewTracks && next != null) "No new songs on this page. Tap to keep searching." else null,
                    message = "${combined.size} results",
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (generation == searchGeneration) {
                    _discover.value = _discover.value.copy(loadingMore = false,
                        loadMoreError = "Couldn't load more results. Check your connection and try again.")
                }
            }
        }
    }

    fun hasNetwork(): Boolean {
        val manager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private var radioJob: Job? = null
    private val _radioMessage = MutableStateFlow("")
    val radioMessage: StateFlow<String> = _radioMessage

    fun dismissRadioMessage() { _radioMessage.value = "" }

    fun startRadio(seed: Track) {
        radioJob?.cancel()
        val online = hasNetwork()
        if (!online && seed.source != TrackSource.LOCAL) {
            _radioMessage.value = "Choose an on-device song to start radio offline."
            return
        }
        _radioMessage.value = "Finding songs for radio…"
        val before = playback.state.value
        radioJob = viewModelScope.launch {
            val discovered = if (online && seed.artist.isNotBlank()) {
                try {
                    YouTubeMusicRepository.search(seed.artist.removeSuffix(" - Topic")).tracks
                        .filter { it.durationMs in 30_000L..900_000L }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { emptyList() }
            } else emptyList()
            val local = library.value.tracks
            val localIds = local.mapTo(hashSetOf()) { it.id }
            val saved = (favorites.value + recent.value).filter { it.source != TrackSource.LOCAL || it.id in localIds }
            val pool = discovered + saved + local
            val queue = withContext(Dispatchers.Default) { radioQueue(seed, pool.filterNot { it.id in options.getStringSet("hidden:${seed.id}", emptySet()).orEmpty() }, online) }
            val now = playback.state.value
            if (now.mediaId != before.mediaId || now.queue != before.queue) {
                dismissRadioMessage()
                return@launch
            }
            if (queue.size < 2) {
                _radioMessage.value = "No other songs available for this station yet."
                return@launch
            }
            queue.forEach { known[it.id] = it }
            playback.play(queue, 0)
            playback.station = seed.id
            _radioMessage.value = "Radio started · ${queue.size} songs"
        }
    }

    private fun cancelRadioRequest() {
        radioJob?.cancel()
        dismissRadioMessage()
    }

    fun playLocal(track: Track) {
        cancelRadioRequest()
        val tracks = library.value.tracks
        playback.play(tracks, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
    }
    fun playOnline(track: Track) {
        cancelRadioRequest()
        if (!hasNetwork()) {
            _discover.value = discover.value.copy(message = "Online playback needs a connection.")
            return
        }
        playback.play(listOf(track), 0)
    }
    fun playListTracks(tracks: List<Track>, track: Track) {
        cancelRadioRequest()
        tracks.forEach { known[it.id] = it }
        playback.play(tracks, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
    }

    fun findTrack(id: String): Track? =
        known[id] ?: library.value.tracks.firstOrNull { it.id == id }
            ?: favorites.value.firstOrNull { it.id == id }
            ?: recent.value.firstOrNull { it.id == id }
            ?: topTracks.value.firstOrNull { it.id == id }

    fun editDisplayTags(track: Track, title: String, artist: String, album: String) {
        if (!track.id.startsWith("local:")) return
        val updated = tagOverrides.save(track, title, artist, album)
        known[track.id] = updated
        _library.value = _library.value.copy(tracks = library.value.tracks.map {
            if (it.id == track.id) updated else it
        })
        viewModelScope.launch { store.updateTrack(updated) }
    }

    fun toggleFavorite(track: Track) = viewModelScope.launch { store.toggleFavorite(track) }
    fun createPlaylist(name: String) = viewModelScope.launch { store.newPlaylist(name) }
    fun addToPlaylist(id: Long, track: Track) = viewModelScope.launch { store.addToPlaylist(id, track) }
    val removedSong = MutableStateFlow<com.urfavxbf.kanade.data.PlaylistItem?>(null)
    fun removeFromPlaylist(id: Long, trackId: String) = viewModelScope.launch {
        removedSong.value = store.removeFromPlaylist(id, trackId)
    }
    fun undoRemoval() = viewModelScope.launch {
        val item = removedSong.value ?: return@launch
        removedSong.value = null
        store.restore(item)
    }
    fun deletePlaylist(id: Long) = viewModelScope.launch { store.removePlaylist(id) }
    fun selectPlaylist(id: Long) {
        playlistJob?.cancel()
        playlistJob = viewModelScope.launch {
            store.playlist(id).collect { _selectedPlaylistTracks.value = it.map { song -> song.toTrack() } }
        }
    }

    fun smartTracks(type: String): List<Track> = when (type) {
        "Most played" -> topTracks.value
        "Recently played" -> recent.value
        "Favorites" -> favorites.value
        "Recently added" -> library.value.tracks.sortedByDescending { it.addedAtMs }.take(100)
        else -> emptyList()
    }

    fun exportM3u(uri: Uri, tracks: List<Track>) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                it.write(PlaylistExchange.export(tracks))
            }
        }
    }

    fun importM3u(uri: Uri, playlistId: Long) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { it.readText() } ?: return@runCatching
            val songs = PlaylistExchange.import(text, library.value.tracks)
            songs.forEach { store.addToPlaylist(playlistId, it) }
        }
    }

    fun loadLyrics(uri: Uri) {
        val id = playback.state.value.mediaId
        if (id.isBlank()) return
        val app = getApplication<Application>()
        try { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { }
        lyricPrefs.edit().remove("match:$id").putString(id, uri.toString()).apply()
        loadSavedLyrics(id)
    }

    fun chooseLyrics(id: String, match: OnlineLyrics.Match) {
        if (id.isBlank() || id != playback.state.value.mediaId) return
        lyricJob?.cancel()
        lyricPrefs.edit().remove(id).putString("match:$id", match.raw).apply()
        _lyrics.value = LyricsState(match.lyrics.synced, "", match.lyrics.plain)
    }

    private fun loadSavedLyrics(id: String) {
        lyricJob?.cancel()
        _lyrics.value = LyricsState(message = "Loading lyrics…")
        lyricPrefs.getString("match:$id", null)?.let { raw ->
            val saved = runCatching { OnlineLyrics.decode(org.json.JSONObject(raw)) }.getOrNull()
            if (saved != null) {
                _lyrics.value = LyricsState(saved.synced, "", saved.plain)
                return
            }
        }
        val uri = lyricPrefs.getString(id, null)
        if (uri == null) {
            val state = playback.state.value
            if (id.isBlank()) { _lyrics.value = LyricsState(); return }
            lyricJob = viewModelScope.launch {
                try {
                    val result = OnlineLyrics.get(state.title, state.artist, state.durationMs)
                    _lyrics.value = when {
                        result == null -> LyricsState(message = "Lyrics unavailable for this song.")
                        result.synced.isNotEmpty() -> LyricsState(lines = result.synced, message = "")
                        !result.plain.isNullOrBlank() -> LyricsState(message = "", plain = result.plain)
                        else -> LyricsState(message = "Lyrics unavailable for this song.")
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _lyrics.value = LyricsState(message = "Could not load online lyrics.")
                }
            }
            return
        }
        lyricJob = viewModelScope.launch {
            try {
                val contents = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(Uri.parse(uri))
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                val parsed = LrcParser.parse(contents)
                _lyrics.value = LyricsState(parsed,
                    if (parsed.isEmpty()) "No synchronized timestamps found in this LRC file." else "")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _lyrics.value = LyricsState(message = "This lyrics file is no longer accessible.")
            }
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        homeJob?.cancel()
        loadMoreJob?.cancel()
        playlistJob?.cancel()
        lyricJob?.cancel()
        playback.close()
        super.onCleared()
    }
}
