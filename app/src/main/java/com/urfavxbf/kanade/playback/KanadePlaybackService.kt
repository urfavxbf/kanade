package com.urfavxbf.kanade.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.urfavxbf.kanade.data.YouTubeMusicRepository
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background-owned playback and sleep timer. No Activity is required for an active timer.
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class KanadePlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private var effect: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var loudnessSessionId = -1
    private var effectSessionId: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("kanade_audio", Context.MODE_PRIVATE) }
    private val timer = object : Runnable {
        override fun run() {
            val deadline = prefs.getLong("sleepDeadline", 0L)
            if (deadline == 0L) return
            if (System.currentTimeMillis() >= deadline) {
                prefs.edit().remove("sleepDeadline").apply()
                player?.pause()
            } else handler.postDelayed(this, (deadline - System.currentTimeMillis()).coerceIn(500L, 15_000L))
        }
    }

    companion object {
        const val ACTION_SLEEP = "com.urfavxbf.kanade.SLEEP"
        const val ACTION_ONLINE_BOOST = "com.urfavxbf.kanade.ONLINE_BOOST"
        const val EXTRA_BOOST = "boost"
        const val ACTION_EQUALIZER = "com.urfavxbf.kanade.EQUALIZER"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_PRESET = "preset"
    }

    override fun onCreate() {
        super.onCreate()
        val dataSource = ResolvingDataSource.Factory(DefaultDataSource.Factory(this)) { spec ->
            if (spec.uri.scheme == "kanadeyt" && spec.uri.host == "track") {
                val id = spec.uri.lastPathSegment.orEmpty()
                spec.withUri(Uri.parse(YouTubeMusicRepository.streamUrl(id)))
            } else spec
        }
        val instance = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        instance.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) { applyEqualizer(); applyOnlineBoost() }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                applyOnlineBoost()
            }
        })
        player = instance
        session = MediaSession.Builder(this, instance).build()
        handler.post(timer)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SLEEP -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).coerceIn(0, 180)
                val deadline = if (minutes == 0) 0L else System.currentTimeMillis() + minutes * 60_000L
                prefs.edit().putLong("sleepDeadline", deadline).apply()
                handler.removeCallbacks(timer)
                if (deadline > 0) handler.post(timer)
                return START_STICKY
            }
            ACTION_ONLINE_BOOST -> {
                val gain = intent.getIntExtra(EXTRA_BOOST, 0).coerceIn(0, 600)
                prefs.edit().putInt("online_boost", gain).apply()
                applyOnlineBoost()
                return START_STICKY
            }
            ACTION_EQUALIZER -> {
                val preset = intent.getStringExtra(EXTRA_PRESET).orEmpty()
                if (preset in setOf("Off", "Bass", "Vocal")) {
                    prefs.edit().putString("eq", preset).apply()
                    applyEqualizer()
                }
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun applyOnlineBoost() {
        val current = player ?: return
        val gain = onlineBoostGain(current.currentMediaItem?.mediaId, prefs.getInt("online_boost", 0))
        val audioId = current.audioSessionId
        try {
            if (gain == 0 || audioId == C.AUDIO_SESSION_ID_UNSET || audioId == 0) {
                loudness?.enabled = false
                return
            }
            if (loudness == null || loudnessSessionId != audioId) {
                loudness?.release()
                loudness = null
                loudnessSessionId = -1
                loudness = LoudnessEnhancer(audioId)
                loudnessSessionId = audioId
            }
            loudness?.setTargetGain(gain)
            loudness?.enabled = true
        } catch (_: RuntimeException) {
            runCatching { loudness?.release() }
            loudness = null
            loudnessSessionId = -1
        }
    }

    private fun applyEqualizer() {
        val current = player ?: return
        val preset = prefs.getString("eq", "Off") ?: "Off"
        val audioId = current.audioSessionId
        if (audioId == C.AUDIO_SESSION_ID_UNSET || audioId == 0) return
        try {
            if (preset == "Off") {
                effect?.enabled = false
                return
            }
            if (effect == null || effectSessionId != audioId) {
                effect?.release()
                effect = Equalizer(0, audioId)
                effectSessionId = audioId
            }
            val eq = effect ?: return
            val levels = eq.bandLevelRange
            for (i in 0 until eq.numberOfBands.toInt()) {
                val boost = when (preset) {
                    "Bass" -> if (i < eq.numberOfBands / 3) 0.55f else 0f
                    "Vocal" -> if (i in 1 until (eq.numberOfBands - 1).toInt()) 0.32f else 0f
                    else -> 0f
                }
                eq.setBandLevel(i.toShort(), (levels[1] * boost).toInt().toShort())
            }
            eq.enabled = true
        } catch (_: Exception) {
            // Equalizer support is hardware/route-dependent; audio always takes priority.
            effect?.release()
            effect = null
            effectSessionId = -1
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        effect?.release()
        effect = null
        loudness?.release()
        loudness = null
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }
}
