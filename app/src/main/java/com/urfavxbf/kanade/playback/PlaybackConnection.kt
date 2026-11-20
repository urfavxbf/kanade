package com.urfavxbf.kanade.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class QueueEntry(val id: String, val title: String, val artist: String, val artworkUri: String? = null)

data class PlaybackState(
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val playing: Boolean = false,
    val hasItem: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val index: Int = 0,
    val queue: List<QueueEntry> = emptyList(),
    val error: String? = null,
)

class PlaybackConnection(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kanade_queue", Context.MODE_PRIVATE)
    private val audioPrefs = app.getSharedPreferences("kanade_audio", Context.MODE_PRIVATE)
    private val _onlineBoost = MutableStateFlow(audioPrefs.getInt("online_boost", 0).coerceIn(0, 600))
    val onlineBoost: StateFlow<Int> = _onlineBoost
    private val future: ListenableFuture<MediaController> =
        MediaController.Builder(
            app,
            SessionToken(app, ComponentName(app, KanadePlaybackService::class.java)),
        ).buildAsync()

    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Track>, Int>? = null
    private var knownTracks = linkedMapOf<String, Track>()
    private var lastSaveAt = 0L
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(
                error = "Could not play this track. Try another song or check your connection."
            )
        }
    }

    init {
        future.addListener({
            try {
                val connected = future.get()
                controller = connected
                connected.addListener(listener)
                restore(connected)
                pendingQueue?.let { (tracks, index) ->
                    pendingQueue = null
                    play(tracks, index)
                }
                sync()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Player unavailable. Please reopen Kanade.")
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun play(tracks: List<Track>, position: Int) {
        if (tracks.isEmpty()) return
        _state.value = _state.value.copy(error = null)
        if (activeSession.isNotEmpty()) {
            saveSession(activeSession)
            activeSession = ""
            sessions.edit().remove("active").apply()
        }
        station = ""
        val target = position.coerceIn(0, tracks.lastIndex)
        val player = controller
        if (player == null) {
            pendingQueue = tracks to target
            return
        }
        tracks.forEach { knownTracks[it.id] = it }
        player.setMediaItems(tracks.map(Track::mediaItem), target, 0L)
        player.prepare()
        player.play()
        persist(force = true)
        sync()
    }

    fun playAt(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) {
            it.seekToDefaultPosition(index)
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.play()
        } }
    }

    fun add(track: Track, next: Boolean = false) {
        knownTracks[track.id] = track
        controller?.let { player ->
            if (next && player.currentMediaItem != null) {
                player.addMediaItem(player.currentMediaItemIndex + 1, track.mediaItem())
            } else player.addMediaItem(track.mediaItem())
            persist(force = true)
        }
    }

    fun remove(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
        persist(force = true)
    }

    fun move(from: Int, to: Int) {
        controller?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) {
            it.moveMediaItem(from, to)
            persist(force = true)
        } }
    }

    private val sessions = app.getSharedPreferences("queue_sessions", Context.MODE_PRIVATE)
    val sessionNames = MutableStateFlow(sessions.getStringSet("names", emptySet()).orEmpty().sorted())
    private var activeSession = sessions.getString("active", "").orEmpty()
    var station: String
        get() = prefs.getString("station", "").orEmpty()
        set(value) { prefs.edit().putString("station", value).apply() }

    fun queueTracks(): List<Track> {
        val player = controller ?: return emptyList()
        return (0 until player.mediaItemCount).mapNotNull { index ->
            knownTracks[player.getMediaItemAt(index).mediaId]
        }
    }

    fun saveSession(name: String) {
        val key = name.trim().take(60)
        if (key.isEmpty() || controller?.mediaItemCount == 0 || controller == null) return
        persist(force = true)
        val names = sessionNames.value.toSet() + key
        val snapshot = JSONObject(prefs.all).toString()
        sessions.edit().putString("queue:$key", snapshot).putStringSet("names", names)
            .putString("active", key).apply()
        activeSession = key
        sessionNames.value = names.sorted()
    }

    fun openSession(name: String) {
        val player = controller ?: return
        if (name == activeSession && player.mediaItemCount > 0) return
        if (activeSession.isNotEmpty()) saveSession(activeSession)
        val snapshot = sessions.getString("queue:$name", null)?.let(::JSONObject) ?: return
        val editor = prefs.edit().clear()
        for (key in snapshot.keys()) {
            when (val value = snapshot.get(key)) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Number -> if (key == "position") editor.putLong(key, value.toLong()) else editor.putInt(key, value.toInt())
            }
        }
        player.pause()
        player.clearMediaItems()
        editor.apply()
        restore(player)
        player.prepare()
        player.play()
        activeSession = name
        sessions.edit().putString("active", name).apply()
        sync()
    }

    fun clear() {
        controller?.clearMediaItems()
        knownTracks.clear()
        prefs.edit().clear().apply()
        sync()
    }

    fun toggle() { controller?.let {
        if (it.isPlaying) it.pause() else {
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.play()
        }
    } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        controller?.let { it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        } }
    }

    fun setSpeed(speed: Float) { controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2f)) }

    fun setLoop(track: String, a: Long, b: Long) {
        app.startService(Intent(app, KanadePlaybackService::class.java).apply {
            action = KanadePlaybackService.ACTION_LOOP
            putExtra("track", track); putExtra("a", a); putExtra("b", b)
        })
    }

    fun sleep(minutes: Int) {
        app.startService(Intent(app, KanadePlaybackService::class.java).apply {
            action = KanadePlaybackService.ACTION_SLEEP
            putExtra(KanadePlaybackService.EXTRA_MINUTES, minutes.coerceIn(0, 180))
        })
    }

    fun setOnlineBoost(gain: Int) {
        val value = gain.coerceIn(0, 600)
        _onlineBoost.value = value
        audioPrefs.edit().putInt("online_boost", value).apply()
        app.startService(Intent(app, KanadePlaybackService::class.java).apply {
            action = KanadePlaybackService.ACTION_ONLINE_BOOST
            putExtra(KanadePlaybackService.EXTRA_BOOST, value)
        })
    }

    fun equalizer(preset: String) {
        app.startService(Intent(app, KanadePlaybackService::class.java).apply {
            action = KanadePlaybackService.ACTION_EQUALIZER
            putExtra(KanadePlaybackService.EXTRA_PRESET, preset)
        })
    }

    fun sync() {
        val player = controller ?: return
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = PlaybackState(
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri?.toString(),
            playing = player.isPlaying,
            hasItem = player.currentMediaItem != null,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            speed = player.playbackParameters.speed,
            index = player.currentMediaItemIndex.coerceAtLeast(0),
            queue = (0 until player.mediaItemCount).map { i ->
                val item = player.getMediaItemAt(i)
                QueueEntry(item.mediaId, item.mediaMetadata.title?.toString().orEmpty(),
                    item.mediaMetadata.artist?.toString().orEmpty(), item.mediaMetadata.artworkUri?.toString())
            },
            error = _state.value.error,
        )
        persist()
    }

    private fun persist(force: Boolean = false) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastSaveAt < 5000) return
        lastSaveAt = now
        val list = JSONArray()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val track = knownTracks[item.mediaId]
            list.put(JSONObject()
                .put("id", item.mediaId)
                .put("title", item.mediaMetadata.title?.toString().orEmpty())
                .put("artist", item.mediaMetadata.artist?.toString().orEmpty())
                .put("uri", track?.uri ?: item.localConfiguration?.uri?.toString().orEmpty())
                .put("artwork", item.mediaMetadata.artworkUri?.toString())
                .put("source", track?.source?.name ?: when {
                    item.mediaId.startsWith("audius:") -> "AUDIUS"
                    item.mediaId.startsWith("youtube:") -> "YOUTUBE"
                    else -> "LOCAL"
                })
                .put("duration", track?.durationMs ?: 0L)
                .put("album", track?.album.orEmpty())
                .put("folder", track?.folder.orEmpty()))
        }
        prefs.edit().putString("items", list.toString())
            .putInt("index", player.currentMediaItemIndex.coerceAtLeast(0))
            .putLong("position", player.currentPosition.coerceAtLeast(0L))
            .putInt("repeat", player.repeatMode)
            .putBoolean("shuffle", player.shuffleModeEnabled).apply()
    }

    private fun restore(player: MediaController) {
        if (player.mediaItemCount > 0) return
        try {
            val array = JSONArray(prefs.getString("items", "[]"))
            val tracks = (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val uri = o.optString("uri")
                val id = o.optString("id")
                if (uri.isBlank() || id.isBlank()) null else Track(
                    id = id, title = o.optString("title"),
                    artist = o.optString("artist"), uri = uri,
                    artworkUri = o.optString("artwork").takeIf { it.startsWith("http") || it.startsWith("content:") },
                    source = runCatching { TrackSource.valueOf(o.optString("source")) }.getOrDefault(TrackSource.LOCAL),
                    durationMs = o.optLong("duration"), album = o.optString("album"), folder = o.optString("folder"),
                )
            }
            if (tracks.isEmpty()) return
            tracks.forEach { knownTracks[it.id] = it }
            player.setMediaItems(tracks.map(Track::mediaItem), prefs.getInt("index", 0).coerceIn(0, tracks.lastIndex),
                prefs.getLong("position", 0L).coerceAtLeast(0L))
            player.repeatMode = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)
            player.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
            // Restore a paused queue; never automatically start audio on process recreation.
        } catch (_: Exception) {
            prefs.edit().clear().apply()
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun close() {
        pendingQueue = null
        controller?.removeListener(listener)
        controller = null
        MediaController.releaseFuture(future)
    }
}
