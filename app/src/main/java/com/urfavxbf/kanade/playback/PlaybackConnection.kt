package com.urfavxbf.kanade.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.urfavxbf.kanade.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val hasItem: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val error: String? = null,
)

class PlaybackConnection(context: Context) {
    private val app = context.applicationContext
    private val future: ListenableFuture<MediaController> =
        MediaController.Builder(
            app,
            SessionToken(app, ComponentName(app, KanadePlaybackService::class.java)),
        ).buildAsync()

    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Track>, Int>? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(error = "This track could not be played.")
        }
    }

    init {
        future.addListener({
            try {
                val connected = future.get()
                controller = connected
                connected.addListener(listener)
                sync()
                pendingQueue?.let { (tracks, index) ->
                    pendingQueue = null
                    play(tracks, index)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Player is unavailable. Please reopen Kanade.")
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun play(tracks: List<Track>, position: Int) {
        if (tracks.isEmpty()) return
        val target = position.coerceIn(0, tracks.lastIndex)
        val player = controller
        if (player == null) {
            pendingQueue = tracks to target
            return
        }
        player.setMediaItems(tracks.map(Track::mediaItem), target, 0L)
        player.prepare()
        player.play()
        sync()
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun sync() {
        val player = controller ?: return
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = PlaybackState(
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            playing = player.isPlaying,
            hasItem = player.currentMediaItem != null,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            shuffle = player.shuffleModeEnabled,
            error = _state.value.error,
        )
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun close() {
        pendingQueue = null
        controller?.removeListener(listener)
        controller = null
        MediaController.releaseFuture(future)
    }
}
