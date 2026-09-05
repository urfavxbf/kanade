package com.urfavxbf.kanade.ui.youtube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.urfavxbf.kanade.R;

/**
 * Foreground/media-session host for native YouTube audio playback.
 * The YouTube WebView remains the video UI, while Audio Only uses ExoPlayer
 * with a resolved direct audio stream so playback survives Activity teardown.
 */
public final class YouTubeMediaService extends Service {

    public static final String ACTION_PLAY = "com.urfavxbf.kanade.YOUTUBE_SERVICE_PLAY";
    public static final String ACTION_PAUSE = "com.urfavxbf.kanade.YOUTUBE_SERVICE_PAUSE";
    public static final String ACTION_NEXT = "com.urfavxbf.kanade.YOUTUBE_SERVICE_NEXT";
    public static final String ACTION_PREVIOUS = "com.urfavxbf.kanade.YOUTUBE_SERVICE_PREVIOUS";
    public static final String ACTION_STOP = "com.urfavxbf.kanade.YOUTUBE_SERVICE_STOP";

    private static final String TAG = "KanadeYTService";
    private static final String CHANNEL_ID = "kanade_youtube_playback";
    private static final int NOTIFICATION_ID = 2001;

    private MediaSessionCompat mediaSession;
    private ExoPlayer nativePlayer;
    private boolean receiverRegistered;
    private boolean resolving;
    private String resolvedVideoId = "";

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !YouTubePlaybackManager.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            syncNativePlayback();
            updateSession();
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateSession();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED
                    && YouTubePlaybackManager.isAudioOnly()
                    && YouTubePlaybackManager.isActive()) {
                resolvedVideoId = "";
                resolving = false;
                YouTubePlaybackManager.next();
            }
            updateSession();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(TAG, "Native YouTube playback failed", error);
            resolving = false;
            resolvedVideoId = "";
            updateSession();
        }
    };

    public static void start(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, YouTubeMediaService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    public static void stop(@NonNull Context context) {
        Intent intent = new Intent(context.getApplicationContext(), YouTubeMediaService.class)
                .setAction(ACTION_STOP);
        context.getApplicationContext().startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        nativePlayer = new ExoPlayer.Builder(this).build();
        nativePlayer.setWakeMode(C.WAKE_MODE_NETWORK);
        nativePlayer.addListener(playerListener);

        mediaSession = new MediaSessionCompat(this, "KanadeYouTube");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (!YouTubePlaybackManager.isPlaying()) {
                    YouTubePlaybackManager.toggle();
                }
            }

            @Override
            public void onPause() {
                if (YouTubePlaybackManager.isPlaying()) {
                    YouTubePlaybackManager.toggle();
                }
            }

            @Override
            public void onSkipToNext() {
                YouTubePlaybackManager.next();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
            }

            @Override
            public void onSeekTo(long position) {
                if (YouTubePlaybackManager.isAudioOnly() && nativePlayer != null) {
                    nativePlayer.seekTo(Math.max(0L, position));
                } else {
                    YouTubePlaybackManager.seekTo(position / 1000d);
                }
            }

            @Override
            public void onStop() {
                YouTubePlaybackManager.stop();
                stopNativePlayer();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        });

        IntentFilter filter = new IntentFilter(YouTubePlaybackManager.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        receiverRegistered = true;

        startForeground(NOTIFICATION_ID, buildNotification());
        syncNativePlayback();
        updateSession();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                YouTubePlaybackManager.stop();
                stopNativePlayer();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
            if (ACTION_PLAY.equals(action) && !YouTubePlaybackManager.isPlaying()) {
                YouTubePlaybackManager.toggle();
            } else if (ACTION_PAUSE.equals(action) && YouTubePlaybackManager.isPlaying()) {
                YouTubePlaybackManager.toggle();
            } else if (ACTION_NEXT.equals(action)) {
                YouTubePlaybackManager.next();
            } else if (ACTION_PREVIOUS.equals(action)) {
                playPrevious();
            }
        }
        syncNativePlayback();
        updateSession();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(stateReceiver);
            } catch (IllegalArgumentException ignored) {
                // Receiver was already unregistered.
            }
            receiverRegistered = false;
        }
        if (nativePlayer != null) {
            nativePlayer.removeListener(playerListener);
            nativePlayer.release();
            nativePlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void syncNativePlayback() {
        if (nativePlayer == null || !YouTubePlaybackManager.isActive()) {
            return;
        }

        if (!YouTubePlaybackManager.isAudioOnly()) {
            stopNativePlayer();
            return;
        }

        String currentId = YouTubePlaybackManager.getVideoId();
        if (currentId == null || currentId.isEmpty()) {
            return;
        }

        if (!currentId.equals(resolvedVideoId) && !resolving) {
            resolveAndPlay(currentId);
            return;
        }

        if (currentId.equals(resolvedVideoId)) {
            if (YouTubePlaybackManager.isPlaying()) {
                nativePlayer.play();
            } else {
                nativePlayer.pause();
            }
        }
    }

    private void resolveAndPlay(@NonNull String currentId) {
        resolving = true;
        resolvedVideoId = "";
        nativePlayer.pause();

        YouTubeNativeAudioResolver.resolve(this, currentId,
                new YouTubeNativeAudioResolver.Callback() {
                    @Override
                    public void onResolved(@NonNull String url, long durationMs) {
                        runOnMainThread(() -> {
                            resolving = false;
                            if (!YouTubePlaybackManager.isActive()
                                    || !YouTubePlaybackManager.isAudioOnly()
                                    || !currentId.equals(YouTubePlaybackManager.getVideoId())) {
                                return;
                            }

                            resolvedVideoId = currentId;
                            nativePlayer.setMediaItem(MediaItem.fromUri(url));
                            nativePlayer.prepare();
                            if (YouTubePlaybackManager.isPlaying()) {
                                nativePlayer.play();
                            }
                            updateSession();
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        runOnMainThread(() -> {
                            resolving = false;
                            resolvedVideoId = "";
                            Log.e(TAG, "Audio resolution error: " + message);
                            updateSession();
                        });
                    }
                });
    }

    private void stopNativePlayer() {
        resolving = false;
        resolvedVideoId = "";
        if (nativePlayer != null) {
            nativePlayer.stop();
            nativePlayer.clearMediaItems();
        }
    }

    private void runOnMainThread(@NonNull Runnable runnable) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            runnable.run();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
        }
    }

    private void playPrevious() {
        java.util.List<YouTubePlaybackManager.QueueItem> items =
                YouTubePlaybackManager.getQueue();
        String currentId = YouTubePlaybackManager.getVideoId();
        for (int i = 0; i < items.size(); i++) {
            if (currentId.equals(items.get(i).videoId)) {
                if (i > 0) {
                    YouTubePlaybackManager.playQueueItem(i - 1);
                } else if (YouTubePlaybackManager.isAudioOnly() && nativePlayer != null) {
                    nativePlayer.seekTo(0L);
                } else {
                    YouTubePlaybackManager.seekTo(0d);
                }
                return;
            }
        }
        if (YouTubePlaybackManager.isAudioOnly() && nativePlayer != null) {
            nativePlayer.seekTo(0L);
        } else {
            YouTubePlaybackManager.seekTo(0d);
        }
    }

    private void updateSession() {
        if (mediaSession == null) {
            return;
        }

        long position;
        long duration;
        boolean nativeMode = YouTubePlaybackManager.isAudioOnly() && nativePlayer != null;
        if (nativeMode) {
            position = Math.max(0L, nativePlayer.getCurrentPosition());
            duration = Math.max(0L, nativePlayer.getDuration());
        } else {
            position = Math.max(0L,
                    Math.round(YouTubePlaybackManager.getPositionSeconds() * 1000d));
            duration = Math.max(0L,
                    Math.round(YouTubePlaybackManager.getDurationSeconds() * 1000d));
        }

        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, safeTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, safeChannel())
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        YouTubePlaybackManager.getVideoId());
        if (duration > 0L) {
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        }
        mediaSession.setMetadata(metadata.build());

        boolean playing = nativeMode
                ? nativePlayer.isPlaying()
                : YouTubePlaybackManager.isPlaying();
        int state = playing
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, 1f)
                .build());
        mediaSession.setActive(YouTubePlaybackManager.isActive());

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent contentIntent = launchIntent == null
                ? null
                : PendingIntent.getActivity(
                        this,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean playing = YouTubePlaybackManager.isAudioOnly() && nativePlayer != null
                ? nativePlayer.isPlaying()
                : YouTubePlaybackManager.isPlaying();

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_youtube)
                .setContentTitle(safeTitle())
                .setContentText(safeChannel())
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession == null ? null : mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_previous,
                        "Previous",
                        actionPendingIntent(ACTION_PREVIOUS, 10)))
                .addAction(new NotificationCompat.Action(
                        playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play",
                        actionPendingIntent(playing ? ACTION_PAUSE : ACTION_PLAY, 11)))
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_next,
                        "Next",
                        actionPendingIntent(ACTION_NEXT, 12)))
                .build();
    }

    private PendingIntent actionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, YouTubeMediaService.class).setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "YouTube playback",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Background controls for Kanade YouTube playback");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @NonNull
    private String safeTitle() {
        String value = YouTubePlaybackManager.getTitle();
        return value == null || value.isEmpty() ? "YouTube" : value;
    }

    @NonNull
    private String safeChannel() {
        String value = YouTubePlaybackManager.getChannel();
        return value == null || value.isEmpty() ? "Kanade" : value;
    }
}
