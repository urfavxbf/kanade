package com.urfavxbf.kanade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerService extends Service {

    public static final String ACTION_PLAY = "com.urfavxbf.kanade.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.urfavxbf.kanade.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.urfavxbf.kanade.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.urfavxbf.kanade.ACTION_PREVIOUS";
    public static final String EXTRA_SONG_URI = "com.urfavxbf.kanade.EXTRA_SONG_URI";
    public static final String ACTION_STATE_CHANGED = "com.urfavxbf.kanade.ACTION_STATE_CHANGED";
    public static final String EXTRA_IS_PLAYING = "com.urfavxbf.kanade.EXTRA_IS_PLAYING";
    public static final String EXTRA_CURRENT_URI = "com.urfavxbf.kanade.EXTRA_CURRENT_URI";
    public static final String ACTION_SEEK = "com.urfavxbf.kanade.ACTION_SEEK";
    public static final String EXTRA_SEEK_POSITION = "com.urfavxbf.kanade.EXTRA_SEEK_POSITION";
    public static final String EXTRA_DURATION = "com.urfavxbf.kanade.EXTRA_DURATION";
    public static final String EXTRA_POSITION = "com.urfavxbf.kanade.EXTRA_POSITION";
    public static final String ACTION_SET_SHUFFLE = "com.urfavxbf.kanade.ACTION_SET_SHUFFLE";
    public static final String ACTION_TOGGLE_SHUFFLE = "com.urfavxbf.kanade.ACTION_TOGGLE_SHUFFLE";
    public static final String EXTRA_SHUFFLE_ENABLED = "com.urfavxbf.kanade.EXTRA_SHUFFLE_ENABLED";
    public static final String EXTRA_SHUFFLE_STATE = "com.urfavxbf.kanade.EXTRA_SHUFFLE_STATE";
    public static final String ACTION_SET_REPEAT = "com.urfavxbf.kanade.ACTION_SET_REPEAT";
    public static final String ACTION_TOGGLE_REPEAT = "com.urfavxbf.kanade.ACTION_TOGGLE_REPEAT";
    public static final String EXTRA_REPEAT_MODE = "com.urfavxbf.kanade.EXTRA_REPEAT_MODE";
    public static final String EXTRA_REPEAT_STATE = "com.urfavxbf.kanade.EXTRA_REPEAT_STATE";
    public static final String ACTION_SET_QUEUE_AND_PLAY = "com.urfavxbf.kanade.ACTION_SET_QUEUE_AND_PLAY";
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;
    public static final String ACTION_ADD_TO_QUEUE = "com.urfavxbf.kanade.ACTION_ADD_TO_QUEUE";
    public static final String ACTION_CLEAR_QUEUE = "com.urfavxbf.kanade.ACTION_CLEAR_QUEUE";
    public static final String ACTION_PLAY_QUEUE_ITEM = "com.urfavxbf.kanade.ACTION_PLAY_QUEUE_ITEM";
    public static final String ACTION_REMOVE_FROM_QUEUE = "com.urfavxbf.kanade.ACTION_REMOVE_FROM_QUEUE";
    public static final String ACTION_REQUEST_QUEUE = "com.urfavxbf.kanade.ACTION_REQUEST_QUEUE";
    public static final String EXTRA_QUEUE_INDEX = "com.urfavxbf.kanade.EXTRA_QUEUE_INDEX";
    public static final String ACTION_QUEUE_CHANGED = "com.urfavxbf.kanade.ACTION_QUEUE_CHANGED";
    public static final String EXTRA_QUEUE_SIZE = "com.urfavxbf.kanade.EXTRA_QUEUE_SIZE";
    public static final String EXTRA_QUEUE_URIS = "com.urfavxbf.kanade.EXTRA_QUEUE_URIS";
    public static final String EXTRA_QUEUE_TITLES = "com.urfavxbf.kanade.EXTRA_QUEUE_TITLES";
    public static final String EXTRA_QUEUE_ARTISTS = "com.urfavxbf.kanade.EXTRA_QUEUE_ARTISTS";
    public static final String EXTRA_QUEUE_ALBUMS = "com.urfavxbf.kanade.EXTRA_QUEUE_ALBUMS";
    public static final String ACTION_SET_QUEUE_ORDER = "com.urfavxbf.kanade.ACTION_SET_QUEUE_ORDER";
    public static final String ACTION_AUDIO_ANALYSIS = "com.urfavxbf.kanade.ACTION_AUDIO_ANALYSIS";
    public static final String EXTRA_FFT = "com.urfavxbf.kanade.EXTRA_FFT";
    public static final String EXTRA_BASS = "com.urfavxbf.kanade.EXTRA_BASS";
    public static final String EXTRA_ENERGY = "com.urfavxbf.kanade.EXTRA_ENERGY";
    public static final String EXTRA_BEAT = "com.urfavxbf.kanade.EXTRA_BEAT";
    public static final String EXTRA_BEAT_INTENSITY = "com.urfavxbf.kanade.EXTRA_BEAT_INTENSITY";
    public static final String EXTRA_SAMPLE_RATE = "com.urfavxbf.kanade.EXTRA_SAMPLE_RATE";

    private static final String CHANNEL_ID = "kanade_music_playback";
    private static final int NOTIFICATION_ID = 1001;

    private MediaPlayer mediaPlayer;
    private String currentUri;
    private final ArrayList<AudioFile> queue = new ArrayList<>();
    private int currentIndex = -1;
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean hasAudioFocus;
    private boolean wasPlayingBeforeFocusLoss;
    private boolean shuffleEnabled;
    private int repeatMode = REPEAT_OFF;
    private final Random random = new Random();
    private final ArrayList<Integer> shuffleHistory = new ArrayList<>();
    private Visualizer audioVisualizer;
    private int audioVisualizerSampleRate = 44100;
    private volatile boolean audioAnalysisRunning;
    private float smoothedEnergy;
    private float energyBaseline;
    private float smoothedBass;
    private float previousBass;
    private long lastBeatTime;
    private static final long MIN_BEAT_INTERVAL_MS = 115L;

    private HandlerThread playbackThread;
    private Handler playbackHandler;
    private final Handler positionHandler = new Handler(Looper.getMainLooper());
    private boolean isUpdatingPosition;
    private final Runnable positionUpdateRunnable = new Runnable() {
        @Override public void run() {
            if (!isUpdatingPosition) return;
            try {
                if (mediaPlayer == null || !mediaPlayer.isPlaying()) { stopPositionUpdates(); return; }
                int position = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                sendPlaybackState(true, position, duration);
                updateMediaSessionPosition(position, duration);
                positionHandler.postDelayed(this, 500L);
            } catch (Exception ignored) { stopPositionUpdates(); }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        playbackThread = new HandlerThread("KanadePlayback");
        playbackThread.start();
        playbackHandler = new Handler(playbackThread.getLooper());
        try {
            mediaSession = new MediaSessionCompat(this, "KanadeMusic");
            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override public void onPlay() { postPlayback(() -> togglePlayPause()); }
                @Override public void onPause() { postPlayback(() -> pauseSong()); }
                @Override public void onSkipToNext() { postPlayback(() -> playNext()); }
                @Override public void onSkipToPrevious() { postPlayback(() -> playPrevious()); }
                @Override public void onSeekTo(long position) { postPlayback(() -> seekTo((int) position)); }
                @Override public void onStop() { postPlayback(() -> stopPlayback()); }
            });
            mediaSession.setActive(false);
            updateMediaSessionState(false);
        } catch (Exception e) { mediaSession = null; }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        final String action = intent.getAction();
        final Intent copy = new Intent(intent);
        postPlayback(() -> handleCommand(action, copy));
        return START_NOT_STICKY;
    }

    private void postPlayback(Runnable runnable) {
        if (playbackHandler != null) playbackHandler.post(runnable);
    }

    private void handleCommand(String action, Intent intent) {
        try { if (mediaSession != null) MediaButtonReceiver.handleIntent(mediaSession, intent); } catch (Exception ignored) {}
        if (ACTION_PLAY.equals(action)) {
            String uri = intent.getStringExtra(EXTRA_SONG_URI);
            if (mediaPlayer != null && currentUri != null && (uri == null || uri.trim().isEmpty() || uri.equals(currentUri))) {
                resumeCurrent();
            } else if (uri != null && !uri.trim().isEmpty()) {
                if (currentUri == null || !uri.equals(currentUri)) playSong(uri);
            } else togglePlayPause();
        } else if (ACTION_PAUSE.equals(action)) pauseSong();
        else if (ACTION_NEXT.equals(action)) playNext();
        else if (ACTION_PREVIOUS.equals(action)) playPrevious();
        else if (ACTION_SEEK.equals(action)) seekTo(intent.getIntExtra(EXTRA_SEEK_POSITION, 0));
        else if (ACTION_SET_SHUFFLE.equals(action)) setShuffle(intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false));
        else if (ACTION_TOGGLE_SHUFFLE.equals(action)) setShuffle(!shuffleEnabled);
        else if (ACTION_SET_REPEAT.equals(action)) setRepeatMode(intent.getIntExtra(EXTRA_REPEAT_MODE, REPEAT_OFF));
        else if (ACTION_TOGGLE_REPEAT.equals(action)) toggleRepeatMode();
        else if (ACTION_ADD_TO_QUEUE.equals(action)) addToQueue(intent.getStringExtra(EXTRA_SONG_URI));
        else if (ACTION_CLEAR_QUEUE.equals(action)) clearQueue();
        else if (ACTION_PLAY_QUEUE_ITEM.equals(action)) playQueueItem(intent.getIntExtra(EXTRA_QUEUE_INDEX, -1));
        else if (ACTION_REMOVE_FROM_QUEUE.equals(action)) removeFromQueue(intent.getIntExtra(EXTRA_QUEUE_INDEX, -1));
        else if (ACTION_REQUEST_QUEUE.equals(action)) sendQueueChanged();
        else if (ACTION_SET_QUEUE_ORDER.equals(action)) setQueueOrder(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS), intent.getIntExtra(EXTRA_QUEUE_INDEX, currentIndex));
        else if (ACTION_SET_QUEUE_AND_PLAY.equals(action)) setQueueAndPlay(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS), intent.getIntExtra(EXTRA_QUEUE_INDEX, 0));
    }

    private void setQueueAndPlay(ArrayList<String> uris, int index) {
        if (uris == null || uris.isEmpty()) return;
        queue.clear();
        for (String uri : uris) { AudioFile song = findSongByUri(uri); if (song != null) queue.add(song); }
        if (queue.isEmpty()) return;
        currentIndex = Math.max(0, Math.min(index, queue.size() - 1));
        shuffleHistory.clear();
        AudioFile song = queue.get(currentIndex);
        if (song != null && song.getUri() != null) playSong(song.getUri());
        sendQueueChanged();
    }

    private void playSong(String uri) {
        if (uri == null || uri.trim().isEmpty()) return;
        if (!requestAudioFocus()) return;
        wasPlayingBeforeFocusLoss = false;
        if (queue.isEmpty()) loadQueue();
        int foundIndex = findSongIndex(uri);
        if (foundIndex >= 0) currentIndex = foundIndex;
        else {
            AudioFile song = findSongByUri(uri);
            if (song != null) { queue.add(song); currentIndex = queue.size() - 1; }
            else currentIndex = -1;
        }
        currentUri = uri;
        updateCurrentAlbumColor();
        releasePlayer();
        final String requestedUri = uri;
        final MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        try {
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnCompletionListener(mp -> postPlayback(this::handleCompletion));
            player.setOnErrorListener((mp, what, extra) -> {
                postPlayback(() -> handlePlayerError(requestedUri));
                return true;
            });
            player.setOnPreparedListener(mp -> postPlayback(() -> {
                if (mediaPlayer != mp || !requestedUri.equals(currentUri)) { try { mp.release(); } catch (Exception ignored) {} return; }
                try {
                    startPlaybackPrepared(mp);
                } catch (Exception e) {
                    handlePlayerError(requestedUri);
                }
            }));
            player.setDataSource(this, Uri.parse(uri));
            startPlaybackForeground();
            sendPlaybackState(false);
            player.prepareAsync();
        } catch (Exception e) {
            try { player.release(); } catch (Exception ignored) {}
            if (mediaPlayer == player) mediaPlayer = null;
            handlePlayerError(requestedUri);
        }
    }

    private void startPlaybackPrepared(MediaPlayer player) {
        if (!requestAudioFocus()) return;
        player.start();
        startAudioAnalysis();
        updateMediaMetadata();
        updateMediaSessionState(true);
        updateNotification();
        sendPlaybackState(true);
        sendQueueChanged();
        startPositionUpdates();
    }

    private void handlePlayerError(String uri) {
        if (uri != null && uri.equals(currentUri)) {
            releasePlayer();
            updateMediaSessionState(false);
            updateNotification();
            sendPlaybackState(false);
        }
    }

    private void resumeCurrent() {
        if (mediaPlayer == null) { if (currentUri != null) playSong(currentUri); return; }
        try {
            if (!mediaPlayer.isPlaying()) {
                if (!requestAudioFocus()) return;
                mediaPlayer.start();
                startAudioAnalysis();
                startPositionUpdates();
                updateMediaSessionState(true);
                updateNotification();
                sendPlaybackState(true);
            }
        } catch (Exception ignored) {}
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) { if (currentUri != null) playSong(currentUri); return; }
        try { if (mediaPlayer.isPlaying()) pauseSong(); else resumeCurrent(); } catch (Exception ignored) {}
    }

    private void pauseSong() {
        wasPlayingBeforeFocusLoss = false;
        if (mediaPlayer == null) return;
        try { if (mediaPlayer.isPlaying()) mediaPlayer.pause(); } catch (Exception ignored) {}
        stopAudioAnalysisCapture();
        stopPositionUpdates();
        updateMediaSessionState(false);
        updateNotification();
        sendPlaybackState(false);
    }

    private void playNext() {
        loadQueueIfNeeded();
        if (queue.isEmpty()) return;
        if (repeatMode == REPEAT_ONE && currentIndex >= 0 && currentIndex < queue.size()) { playSong(queue.get(currentIndex).getUri()); return; }
        int next;
        if (shuffleEnabled) next = getRandomShuffleIndex();
        else {
            if (currentIndex < 0) currentIndex = currentUri == null ? 0 : Math.max(0, findSongIndex(currentUri));
            next = currentIndex + 1;
            if (next >= queue.size()) {
                if (repeatMode == REPEAT_ALL) next = 0;
                else { currentIndex = queue.size() - 1; pauseSong(); return; }
            }
        }
        currentIndex = next;
        AudioFile song = queue.get(currentIndex);
        if (song != null && song.getUri() != null) playSong(song.getUri());
    }

    private void playPrevious() {
        loadQueueIfNeeded();
        if (queue.isEmpty()) return;
        if (mediaPlayer != null) { try { if (mediaPlayer.getCurrentPosition() > 3000) { mediaPlayer.seekTo(0); return; } } catch (Exception ignored) {} }
        if (currentIndex < 0) currentIndex = Math.max(0, findSongIndex(currentUri));
        int previous = shuffleEnabled ? getPreviousShuffleIndex() : currentIndex - 1;
        if (previous < 0) previous = repeatMode == REPEAT_ALL ? queue.size() - 1 : 0;
        currentIndex = previous;
        AudioFile song = queue.get(currentIndex);
        if (song != null && song.getUri() != null) playSong(song.getUri());
    }

    private void handleCompletion() {
        if (repeatMode == REPEAT_ONE && currentUri != null) playSong(currentUri); else playNext();
    }

    private void setShuffle(boolean enabled) { shuffleEnabled = enabled; shuffleHistory.clear(); sendShuffleState(); sendQueueChanged(); }
    private int getRandomShuffleIndex() {
        if (queue.size() <= 1) return 0;
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) if (i != currentIndex && !shuffleHistory.contains(i)) candidates.add(i);
        if (candidates.isEmpty()) for (int i = 0; i < queue.size(); i++) if (i != currentIndex) candidates.add(i);
        if (candidates.isEmpty()) return currentIndex;
        if (currentIndex >= 0 && !shuffleHistory.contains(currentIndex)) shuffleHistory.add(currentIndex);
        return candidates.get(random.nextInt(candidates.size()));
    }
    private int getPreviousShuffleIndex() {
        while (!shuffleHistory.isEmpty()) { int value = shuffleHistory.remove(shuffleHistory.size() - 1); if (value >= 0 && value < queue.size() && value != currentIndex) return value; }
        return Math.max(0, currentIndex);
    }
    private void sendShuffleState() { Intent intent = new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName()); intent.putExtra(EXTRA_SHUFFLE_STATE, shuffleEnabled); intent.putExtra(EXTRA_REPEAT_STATE, repeatMode); sendBroadcast(intent); }
    private void setRepeatMode(int mode) { repeatMode = Math.max(REPEAT_OFF, Math.min(REPEAT_ONE, mode)); sendShuffleState(); }
    private void toggleRepeatMode() { repeatMode = repeatMode == REPEAT_OFF ? REPEAT_ALL : repeatMode == REPEAT_ALL ? REPEAT_ONE : REPEAT_OFF; sendShuffleState(); }

    private void addToQueue(String uri) {
        if (uri == null || uri.trim().isEmpty()) return;
        AudioFile song = findSongByUri(uri);
        if (song == null || findSongIndex(uri) >= 0) return;
        queue.add(song);
        if (currentIndex < 0) currentIndex = 0;
        sendQueueChanged();
    }

    private void removeFromQueue(int index) {
        if (index < 0 || index >= queue.size()) return;
        boolean current = index == currentIndex;
        queue.remove(index);
        if (queue.isEmpty()) { stopPlayback(); return; }
        if (index < currentIndex) { currentIndex--; sendQueueChanged(); return; }
        if (!current) { sendQueueChanged(); return; }
        currentIndex = Math.min(index, queue.size() - 1);
        AudioFile song = queue.get(currentIndex);
        if (song != null && song.getUri() != null) playSong(song.getUri());
        sendQueueChanged();
    }

    private void clearQueue() {
        AudioFile current = currentIndex >= 0 && currentIndex < queue.size() ? queue.get(currentIndex) : null;
        queue.clear(); shuffleHistory.clear();
        if (current != null) { queue.add(current); currentIndex = 0; } else currentIndex = -1;
        sendQueueChanged();
    }

    private void playQueueItem(int index) {
        if (index < 0 || index >= queue.size()) return;
        AudioFile song = queue.get(index);
        if (song == null || song.getUri() == null) return;
        shuffleHistory.clear(); currentIndex = index; playSong(song.getUri());
    }

    private void setQueueOrder(ArrayList<String> orderedUris, int requestedCurrentIndex) {
        if (orderedUris == null || orderedUris.isEmpty()) return;
        ArrayList<AudioFile> reordered = new ArrayList<>();
        for (String uri : orderedUris) { AudioFile song = findSongByUri(uri); if (song != null) reordered.add(song); }
        if (reordered.isEmpty()) return;
        String playingUri = currentUri; queue.clear(); queue.addAll(reordered); currentIndex = -1;
        if (playingUri != null) currentIndex = findSongIndex(playingUri);
        if (currentIndex < 0 && requestedCurrentIndex >= 0 && requestedCurrentIndex < queue.size()) currentIndex = requestedCurrentIndex;
        sendQueueChanged();
    }

    private void sendQueueChanged() {
        Intent intent = new Intent(ACTION_QUEUE_CHANGED).setPackage(getPackageName());
        ArrayList<String> uris = new ArrayList<>(), titles = new ArrayList<>(), artists = new ArrayList<>(), albums = new ArrayList<>();
        for (AudioFile song : queue) {
            uris.add(song == null || song.getUri() == null ? "" : song.getUri());
            titles.add(song == null ? "Unknown title" : safeText(song.getTitle(), "Unknown title"));
            artists.add(song == null ? "Unknown artist" : safeText(song.getArtist(), "Unknown artist"));
            albums.add(song == null ? "Unknown album" : safeText(song.getAlbum(), "Unknown album"));
        }
        intent.putStringArrayListExtra(EXTRA_QUEUE_URIS, uris);
        intent.putStringArrayListExtra(EXTRA_QUEUE_TITLES, titles);
        intent.putStringArrayListExtra(EXTRA_QUEUE_ARTISTS, artists);
        intent.putStringArrayListExtra(EXTRA_QUEUE_ALBUMS, albums);
        intent.putExtra(EXTRA_QUEUE_SIZE, queue.size());
        intent.putExtra(EXTRA_QUEUE_INDEX, currentIndex);
        sendBroadcast(intent);
    }

    private void loadQueueIfNeeded() { if (queue.isEmpty()) loadQueue(); }
    private void loadQueue() {
        try {
            ArrayList<AudioFile> result = new MusicRepository(getApplicationContext()).getAllSongs();
            if (result != null) { queue.clear(); queue.addAll(result); if (currentUri != null) currentIndex = findSongIndex(currentUri); }
        } catch (Exception ignored) {}
        sendQueueChanged();
    }
    private AudioFile findSongByUri(String uri) {
        if (uri == null) return null;
        for (AudioFile song : queue) if (song != null && uri.equals(song.getUri())) return song;
        try { for (AudioFile song : new MusicRepository(getApplicationContext()).getAllSongs()) if (song != null && uri.equals(song.getUri())) return song; } catch (Exception ignored) {}
        return null;
    }
    private int findSongIndex(String uri) {
        if (uri == null) return -1;
        for (int i = 0; i < queue.size(); i++) { AudioFile song = queue.get(i); if (song != null && uri.equals(song.getUri())) return i; }
        return -1;
    }

    private void seekTo(int position) {
        if (mediaPlayer == null) return;
        try { int duration = mediaPlayer.getDuration(); if (duration <= 0) return; position = Math.max(0, Math.min(position, duration)); mediaPlayer.seekTo(position); boolean playing = isPlayerPlaying(); updateMediaSessionState(playing); sendPlaybackState(playing); } catch (Exception ignored) {}
    }
    private void startPositionUpdates() { positionHandler.removeCallbacks(positionUpdateRunnable); isUpdatingPosition = true; positionHandler.post(positionUpdateRunnable); }
    private void stopPositionUpdates() { isUpdatingPosition = false; positionHandler.removeCallbacks(positionUpdateRunnable); }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        postPlayback(() -> {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) { boolean playing = isPlayerPlaying(); pauseSong(); wasPlayingBeforeFocusLoss = playing; abandonAudioFocus(); }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { boolean playing = isPlayerPlaying(); pauseSong(); wasPlayingBeforeFocusLoss = playing; }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && wasPlayingBeforeFocusLoss) { wasPlayingBeforeFocusLoss = false; resumeCurrent(); }
        });
    };

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(audioFocusChangeListener).build();
                hasAudioFocus = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            } else hasAudioFocus = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } catch (Exception ignored) { hasAudioFocus = false; }
        return hasAudioFocus;
    }
    private void abandonAudioFocus() { if (audioManager == null) return; try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest); else audioManager.abandonAudioFocus(audioFocusChangeListener); } catch (Exception ignored) {} hasAudioFocus = false; }

    private void updateMediaSessionState(boolean playing) {
        if (mediaSession == null) return;
        int state = currentUri == null ? PlaybackStateCompat.STATE_NONE : playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mediaPlayer != null) try { position = mediaPlayer.getCurrentPosition(); } catch (Exception ignored) {}
        try { mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_STOP).setState(state, position, playing ? 1f : 0f).build()); mediaSession.setActive(currentUri != null); } catch (Exception ignored) {}
    }
    private void updateMediaSessionPosition(int position, int duration) { updateMediaSessionState(isPlayerPlaying()); }
    private void updateMediaMetadata() {
        if (mediaSession == null || currentIndex < 0 || currentIndex >= queue.size()) return;
        AudioFile song = queue.get(currentIndex); if (song == null) return;
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE, safeText(song.getTitle(), "Unknown title")).putString(MediaMetadataCompat.METADATA_KEY_ARTIST, safeText(song.getArtist(), "Unknown artist")).putString(MediaMetadataCompat.METADATA_KEY_ALBUM, safeText(song.getAlbum(), "Unknown album"));
        if (mediaPlayer != null) try { builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration()); } catch (Exception ignored) {}
        Bitmap art = loadAlbumArt(song); if (art != null) { builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art); builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art); }
        try { mediaSession.setMetadata(builder.build()); } catch (Exception ignored) {}
    }
    private void updateCurrentAlbumColor() { try { if (currentIndex >= 0 && currentIndex < queue.size()) { AudioFile song = queue.get(currentIndex); if (song != null) AlbumColorManager.getInstance(getApplicationContext()).setCurrentSong(song); } } catch (Exception ignored) {} }
    private Bitmap loadAlbumArt(AudioFile song) {
        if (song == null || song.getPath() == null || song.getPath().trim().isEmpty()) return null;
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try { retriever.setDataSource(song.getPath()); byte[] data = retriever.getEmbeddedPicture(); if (data != null && data.length > 0) return BitmapFactory.decodeByteArray(data, 0, data.length); } catch (Exception ignored) {} finally { try { retriever.release(); } catch (Exception ignored) {} }
        return null;
    }

    private void startAudioAnalysis() {
        if (mediaPlayer == null) return;
        try {
            releaseAudioVisualizer();
            int sessionId = mediaPlayer.getAudioSessionId(); if (sessionId <= 0) return;
            audioVisualizer = new Visualizer(sessionId);
            int[] range = Visualizer.getCaptureSizeRange();
            int size = 1024; if (range != null && range.length >= 2) size = Math.max(range[0], Math.min(1024, range[1]));
            audioVisualizer.setCaptureSize(size);
            int rate = Math.min(8000000, Math.max(1, Visualizer.getMaxCaptureRate()));
            audioVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}
                @Override public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) { processFFTData(fft, samplingRate); }
            }, rate, false, true);
            audioVisualizer.setEnabled(true); audioAnalysisRunning = true; resetAudioAnalysisState();
        } catch (Exception ignored) { releaseAudioVisualizer(); }
    }
    private void processFFTData(byte[] fft, int rateMilliHz) {
        if (!audioAnalysisRunning || fft == null || fft.length < 4) return;
        try {
            if (rateMilliHz > 0) audioVisualizerSampleRate = rateMilliHz / 1000;
            float bass = calculateBassEnergy(fft), energy = calculateOverallEnergy(fft);
            smoothedBass = smoothValue(smoothedBass, bass, .45f); smoothedEnergy = smoothValue(smoothedEnergy, energy, .32f);
            energyBaseline = energyBaseline <= 0 ? smoothedEnergy : smoothValue(energyBaseline, smoothedEnergy, .025f);
            float bassRise = smoothedBass - previousBass, energyRise = smoothedEnergy - energyBaseline;
            boolean beat = detectBeat(smoothedBass, smoothedEnergy, bassRise, energyRise);
            float intensity = Math.max(0, Math.min(1, bassRise * 6f + energyRise * 4.5f + smoothedBass * .45f + smoothedEnergy * .3f));
            previousBass = smoothedBass;
            byte[] copy = new byte[fft.length]; System.arraycopy(fft, 0, copy, 0, fft.length);
            Intent intent = new Intent(ACTION_AUDIO_ANALYSIS).setPackage(getPackageName()).putExtra(EXTRA_FFT, copy).putExtra(EXTRA_BASS, smoothedBass).putExtra(EXTRA_ENERGY, smoothedEnergy).putExtra(EXTRA_BEAT, beat).putExtra(EXTRA_BEAT_INTENSITY, intensity).putExtra(EXTRA_SAMPLE_RATE, audioVisualizerSampleRate);
            sendBroadcast(intent);
        } catch (Exception ignored) {}
    }
    private float calculateBassEnergy(byte[] fft) { return calculateBandEnergy(fft, 20f, 250f); }
    private float calculateOverallEnergy(byte[] fft) { return calculateBandEnergy(fft, 20f, 20000f); }
    private float calculateBandEnergy(byte[] fft, float low, float high) {
        if (fft == null || fft.length < 4) return 0f;
        int bins = fft.length / 2; float nyquist = Math.max(1f, audioVisualizerSampleRate / 2f); int start = Math.max(1, Math.round(Math.min(1, low / nyquist) * (bins - 1))); int end = Math.min(bins - 1, Math.round(Math.min(1, high / nyquist) * (bins - 1))); if (end <= start) end = Math.min(bins - 1, start + 1);
        float total = 0; int count = 0; for (int bin = start; bin <= end; bin++) { int i = bin * 2; if (i + 1 >= fft.length) break; float r = fft[i], im = fft[i + 1]; float magnitude = (float)Math.sqrt(r * r + im * im) / 181.02f; total += (float)Math.sqrt(Math.min(1, Math.max(0, magnitude))); count++; } return count == 0 ? 0 : Math.min(1, total / count);
    }
    private float smoothValue(float current, float target, float factor) { return current + (target - current) * factor; }
    private boolean detectBeat(float bass, float energy, float bassRise, float energyRise) { long now = System.currentTimeMillis(); if (now - lastBeatTime < MIN_BEAT_INTERVAL_MS) return false; boolean detected = energy > Math.max(.04f, energyBaseline * .55f) && bass > .055f && (bassRise > .032f + energyBaseline * .055f || energyRise > .022f + energyBaseline * .04f); if (detected) lastBeatTime = now; return detected; }
    private void resetAudioAnalysisState() { smoothedEnergy = 0; energyBaseline = 0; smoothedBass = 0; previousBass = 0; lastBeatTime = 0; }
    private void stopAudioAnalysisCapture() { audioAnalysisRunning = false; if (audioVisualizer != null) try { audioVisualizer.setEnabled(false); } catch (Exception ignored) {} resetAudioAnalysisState(); }
    private void releaseAudioVisualizer() { audioAnalysisRunning = false; if (audioVisualizer != null) { try { audioVisualizer.setEnabled(false); } catch (Exception ignored) {} try { audioVisualizer.setDataCaptureListener(null, 0, false, false); } catch (Exception ignored) {} try { audioVisualizer.release(); } catch (Exception ignored) {} audioVisualizer = null; } resetAudioAnalysisState(); }

    private void sendPlaybackState(boolean playing) { int position = 0, duration = 0; if (mediaPlayer != null) try { position = mediaPlayer.getCurrentPosition(); duration = mediaPlayer.getDuration(); } catch (Exception ignored) {} sendPlaybackState(playing, position, duration); }
    private void sendPlaybackState(boolean playing, int position, int duration) { Intent intent = new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName()).putExtra(EXTRA_IS_PLAYING, playing).putExtra(EXTRA_CURRENT_URI, currentUri).putExtra(EXTRA_POSITION, position).putExtra(EXTRA_DURATION, duration).putExtra(EXTRA_SHUFFLE_STATE, shuffleEnabled).putExtra(EXTRA_REPEAT_STATE, repeatMode); sendBroadcast(intent); }
    private boolean isPlayerPlaying() { if (mediaPlayer == null) return false; try { return mediaPlayer.isPlaying(); } catch (Exception ignored) { return false; } }

    private void createNotificationChannel() { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return; NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW); channel.setDescription("Kanade music playback controls"); channel.setShowBadge(false); NotificationManager manager = getSystemService(NotificationManager.class); if (manager != null) manager.createNotificationChannel(channel); }
    private void startPlaybackForeground() { try { Notification notification = buildNotification(); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK); else startForeground(NOTIFICATION_ID, notification); } catch (Exception ignored) {} }
    private Notification buildNotification() {
        String title = "Kanade Music", artist = "Not playing"; Bitmap artwork = null;
        if (currentIndex >= 0 && currentIndex < queue.size()) { AudioFile song = queue.get(currentIndex); if (song != null) { title = safeText(song.getTitle(), "Unknown title"); artist = safeText(song.getArtist(), "Unknown artist"); artwork = loadAlbumArt(song); } }
        Intent contentIntent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 500, contentIntent, pendingIntentFlags());
        PendingIntent previous = createServicePendingIntent(ACTION_PREVIOUS, 501), next = createServicePendingIntent(ACTION_NEXT, 503), playPause = createServicePendingIntent(isPlayerPlaying() ? ACTION_PAUSE : ACTION_PLAY, 502);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(artist).setCategory(NotificationCompat.CATEGORY_TRANSPORT).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(isPlayerPlaying()).setContentIntent(contentPendingIntent).addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", previous)).addAction(new NotificationCompat.Action(isPlayerPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, isPlayerPlaying() ? "Pause" : "Play", playPause)).addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", next));
        if (mediaSession != null) try { builder.setStyle(new MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(0, 1, 2)); } catch (Exception ignored) {}
        if (artwork != null) builder.setLargeIcon(artwork);
        return builder.build();
    }
    private void updateNotification() { if (currentUri == null) return; try { NotificationManager manager = getSystemService(NotificationManager.class); if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification()); } catch (Exception ignored) {} }
    private PendingIntent createServicePendingIntent(String action, int requestCode) { Intent intent = new Intent(this, MusicPlayerService.class).setAction(action); return PendingIntent.getService(this, requestCode, intent, pendingIntentFlags()); }
    private int pendingIntentFlags() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT; }
    private String safeText(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value; }

    private void stopPlayback() {
        stopPositionUpdates(); stopAudioAnalysisCapture(); wasPlayingBeforeFocusLoss = false; releasePlayer(); abandonAudioFocus(); currentUri = null; currentIndex = -1; shuffleHistory.clear(); updateMediaSessionState(false);
        try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true); } catch (Exception ignored) {}
        try { NotificationManager manager = getSystemService(NotificationManager.class); if (manager != null) manager.cancel(NOTIFICATION_ID); } catch (Exception ignored) {}
        sendPlaybackState(false); sendQueueChanged();
    }
    private void releasePlayer() {
        stopPositionUpdates(); releaseAudioVisualizer();
        if (mediaPlayer != null) { MediaPlayer player = mediaPlayer; mediaPlayer = null; try { player.setOnPreparedListener(null); player.setOnCompletionListener(null); player.setOnErrorListener(null); } catch (Exception ignored) {} try { player.stop(); } catch (Exception ignored) {} try { player.reset(); } catch (Exception ignored) {} try { player.release(); } catch (Exception ignored) {} }
    }
    @Override public void onDestroy() {
        stopPositionUpdates(); wasPlayingBeforeFocusLoss = false; abandonAudioFocus(); releasePlayer(); currentUri = null;
        if (mediaSession != null) { try { mediaSession.setActive(false); mediaSession.release(); } catch (Exception ignored) {} mediaSession = null; }
        try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true); } catch (Exception ignored) {}
        if (playbackThread != null) { playbackThread.quitSafely(); playbackThread = null; playbackHandler = null; }
        super.onDestroy();
    }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
