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
import com.urfavxbf.kanade.data.YouTubeDiscoveryRepository
import com.urfavxbf.kanade.data.YouTubeVideo
import com.urfavxbf.kanade.data.DisplayTagOverrides
import com.urfavxbf.kanade.data.AudiusRepository
import com.urfavxbf.kanade.data.KanadeDatabase
import com.urfavxbf.kanade.data.LocalMusicRepository
import com.urfavxbf.kanade.data.MusicStore
import com.urfavxbf.kanade.data.Playlist
import com.urfavxbf.kanade.data.PlaylistExchange
import com.urfavxbf.kanade.data.toTrack
import com.urfavxbf.kanade.lyrics.LrcParser
import com.urfavxbf.kanade.lyrics.LyricLine
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.playback.PlaybackConnection
import kotlinx.coroutines.CancellationException
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
    val searching: Boolean = false,
    val message: String = "Search for songs or artists on Audius.",
)
data class LyricsState(val lines: List<LyricLine> = emptyList(), val message: String = "Select a local LRC lyrics file.")

data class YouTubeDiscoveryState(
    val results: List<YouTubeVideo> = emptyList(),
    val loading: Boolean = false,
    val message: String = "Search the YouTube music catalog (metadata only; no Kanade playback).",
)

class KanadeViewModel(app: Application) : AndroidViewModel(app) {
    private val local = LocalMusicRepository(app.contentResolver)
    private val tagOverrides = DisplayTagOverrides(app)
    private val audius = AudiusRepository()
    private val youtubeRepo = YouTubeDiscoveryRepository(app)
    private val youtubePreferences = app.getSharedPreferences("kanade_youtube_api", Context.MODE_PRIVATE)
    private val _youtube = MutableStateFlow(YouTubeDiscoveryState())
    val youtube: StateFlow<YouTubeDiscoveryState> = _youtube
    private val _youtubeConfigured = MutableStateFlow(!youtubePreferences.getString("apiKey", null).isNullOrBlank())
    val youtubeConfigured: StateFlow<Boolean> = _youtubeConfigured
    private val _youtubeConsent = MutableStateFlow(youtubePreferences.getBoolean("privacyConsent", false))
    val youtubeConsent: StateFlow<Boolean> = _youtubeConsent
    private val store = MusicStore(KanadeDatabase.open(app).musicDao())
    private val lyricPrefs = app.getSharedPreferences("kanade_lyrics", Context.MODE_PRIVATE)
    val playback = PlaybackConnection(app)
    private val known = mutableMapOf<String, Track>()

    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _discover = MutableStateFlow(DiscoverState())
    val discover: StateFlow<DiscoverState> = _discover
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
    private var youtubeSearchJob: Job? = null
    private var playlistJob: Job? = null
    private var lyricJob: Job? = null
    private var lastLyricTrack = ""
    private var playingId = ""
    private var listeningMs = 0L
    private var recordedEntryId: Long? = null

    init {
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
                _library.value = LibraryState(tracks = tracks)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _library.value = LibraryState(error = "Could not scan your music. Try again.")
            }
        }
    }

    /**
     * The user enters a Google Cloud project API key in their own app instance.
     * Never bundle a shared key into a distributable APK.
     */
    fun configureYouTubeKey(input: String) {
        val normalized = input.trim()
        if (normalized.length !in 20..180 || !normalized.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            _youtube.value = YouTubeDiscoveryState(message = "Invalid key format. Paste the key from Google Cloud Console.")
            return
        }
        youtubeSearchJob?.cancel()
        youtubePreferences.edit().putString("apiKey", normalized).apply()
        _youtubeConfigured.value = true
        _youtube.value = YouTubeDiscoveryState(message = "API key saved on this device. Search YouTube to test it.")
    }

    fun setYouTubePrivacyConsent(agreed: Boolean) {
        youtubePreferences.edit().putBoolean("privacyConsent", agreed).apply()
        _youtubeConsent.value = agreed
        if (!agreed) {
            youtubeSearchJob?.cancel()
            _youtube.value = YouTubeDiscoveryState(message = "YouTube discovery disabled until privacy consent is granted.")
        }
    }

    fun clearYouTubeConfiguration() {
        youtubeSearchJob?.cancel()
        youtubePreferences.edit().remove("apiKey").putBoolean("privacyConsent", false).apply()
        _youtubeConsent.value = false
        _youtubeConfigured.value = false
        _youtube.value = YouTubeDiscoveryState()
    }

    fun searchYouTube(query: String) {
        youtubeSearchJob?.cancel()
        if (!_youtubeConsent.value) {
            _youtube.value = YouTubeDiscoveryState(message = "Read and agree to the privacy policy before using YouTube discovery.")
            return
        }
        val key = youtubePreferences.getString("apiKey", null)
        if (key.isNullOrBlank()) {
            _youtube.value = YouTubeDiscoveryState(message = "Configure your YouTube Data API key first.")
            return
        }
        if (query.trim().length !in 1..120) {
            _youtube.value = YouTubeDiscoveryState(message = "Enter 1 to 120 characters.")
            return
        }
        if (!hasNetwork()) {
            _youtube.value = YouTubeDiscoveryState(message = "Internet required to search YouTube.")
            return
        }
        youtubeSearchJob = viewModelScope.launch {
            _youtube.value = YouTubeDiscoveryState(loading = true, message = "Searching YouTube...")
            try {
                val results = youtubeRepo.search(query, key)
                _youtube.value = YouTubeDiscoveryState(results = results, message =
                    if (results.isEmpty()) "No matches in this search. Try another title or artist."
                    else "YouTube results for Philippines. These are not playable in Kanade.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _youtube.value = YouTubeDiscoveryState(message = e.message ?: "YouTube search failed.")
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        val normalized = query.trim()
        if (normalized.isEmpty() || normalized.length > 120) {
            _discover.value = DiscoverState(message = "Enter a song or artist (up to 120 characters).")
            return
        }
        if (!hasNetwork()) {
            _discover.value = DiscoverState(message = "You're offline. Your local songs still work.")
            return
        }
        searchJob = viewModelScope.launch {
            _discover.value = DiscoverState(searching = true, message = "Searching Audius…")
            try {
                val tracks = audius.search(normalized)
                tracks.forEach { known[it.id] = it }
                _discover.value = DiscoverState(tracks = tracks,
                    message = if (tracks.isEmpty()) "No matches. Try another song." else "${tracks.size} results")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _discover.value = DiscoverState(message = "Couldn't reach Audius. Check your connection.")
            }
        }
    }

    fun hasNetwork(): Boolean {
        val manager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun playLocal(track: Track) {
        val tracks = library.value.tracks
        playback.play(tracks, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
    }
    fun playOnline(track: Track) {
        if (!hasNetwork()) {
            _discover.value = discover.value.copy(message = "Online playback needs a connection.")
            return
        }
        playback.play(listOf(track), 0)
    }
    fun playListTracks(tracks: List<Track>, track: Track) {
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
    fun removeFromPlaylist(id: Long, trackId: String) = viewModelScope.launch { store.removeFromPlaylist(id, trackId) }
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
        lyricPrefs.edit().putString(id, uri.toString()).apply()
        loadSavedLyrics(id)
    }

    private fun loadSavedLyrics(id: String) {
        lyricJob?.cancel()
        _lyrics.value = LyricsState()
        val uri = lyricPrefs.getString(id, null) ?: return
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
        youtubeSearchJob?.cancel()
        playlistJob?.cancel()
        lyricJob?.cancel()
        playback.close()
        super.onCleared()
    }
}
