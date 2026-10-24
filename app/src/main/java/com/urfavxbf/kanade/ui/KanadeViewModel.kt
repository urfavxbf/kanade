package com.urfavxbf.kanade.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urfavxbf.kanade.data.AudiusRepository
import com.urfavxbf.kanade.data.LocalMusicRepository
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.playback.PlaybackConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

class KanadeViewModel(app: Application) : AndroidViewModel(app) {
    private val local = LocalMusicRepository(app.contentResolver)
    private val audius = AudiusRepository()
    val playback = PlaybackConnection(app)
    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _discover = MutableStateFlow(DiscoverState())
    val discover: StateFlow<DiscoverState> = _discover
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                playback.sync()
                delay(800)
            }
        }
    }

    fun permission(): String = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else Manifest.permission.READ_EXTERNAL_STORAGE

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
                _library.value = LibraryState(tracks = local.load())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _library.value = LibraryState(error = "Could not load your local music.")
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            _discover.value = DiscoverState(message = "Enter a song or artist.")
            return
        }
        if (normalized.length > 120) {
            _discover.value = DiscoverState(message = "Search is too long.")
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
                _discover.value = DiscoverState(
                    tracks = tracks,
                    message = if (tracks.isEmpty()) "No matches. Try another song." else "${tracks.size} results",
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _discover.value = DiscoverState(message = "Couldn't reach Audius. Check your connection.")
            }
        }
    }

    fun hasNetwork(): Boolean {
        val manager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
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

    override fun onCleared() {
        searchJob?.cancel()
        playback.close()
        super.onCleared()
    }
}
