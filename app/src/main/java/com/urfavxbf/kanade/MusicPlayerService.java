package com.urfavxbf.kanade;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import android.media.audiofx.Visualizer;

import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerService extends MediaSessionService {

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

    private ExoPlayer player;
    private MediaSession mediaSession;
    private final ArrayList<AudioFile> queue = new ArrayList<>();
    private int currentIndex = -1;
    private String currentUri;
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

    @Override public void onCreate() {
        super.onCreate();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attributes, true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build();

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    startAudioAnalysis();
                    sendPlaybackState(true);
                } else {
                    stopAudioAnalysis();
                    sendPlaybackState(false);
                }
            }

            @Override public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                syncCurrentFromPlayer();
                updateCurrentAlbumColor();
                updateMediaMetadata();
                sendQueueChanged();
                sendPlaybackState(player.isPlaying());
            }

            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    syncCurrentFromPlayer();
                    updateMediaMetadata();
                    sendPlaybackState(player.isPlaying());
                } else if (state == Player.STATE_ENDED) {
                    sendPlaybackState(false);
                }
            }

            @Override public void onPlayerError(PlaybackException error) {
                sendPlaybackState(false);
            }
        });

        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Override public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && !MediaSessionService.SERVICE_INTERFACE.equals(intent.getAction())) {
            handleCommand(intent.getAction(), new Intent(intent));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleCommand(String action, Intent intent) {
        if (ACTION_PLAY.equals(action)) {
            String uri = intent.getStringExtra(EXTRA_SONG_URI);
            if (uri != null && !uri.trim().isEmpty()) playSong(uri);
            else if (player != null && player.getPlaybackState() != Player.STATE_IDLE) player.play();
        } else if (ACTION_PAUSE.equals(action)) {
            if (player != null) player.pause();
        } else if (ACTION_NEXT.equals(action)) {
            playNext();
        } else if (ACTION_PREVIOUS.equals(action)) {
            playPrevious();
        } else if (ACTION_SEEK.equals(action)) {
            seekTo(intent.getIntExtra(EXTRA_SEEK_POSITION, 0));
        } else if (ACTION_SET_SHUFFLE.equals(action)) {
            setShuffle(intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false));
        } else if (ACTION_TOGGLE_SHUFFLE.equals(action)) {
            setShuffle(!shuffleEnabled);
        } else if (ACTION_SET_REPEAT.equals(action)) {
            setRepeatMode(intent.getIntExtra(EXTRA_REPEAT_MODE, REPEAT_OFF));
        } else if (ACTION_TOGGLE_REPEAT.equals(action)) {
            toggleRepeatMode();
        } else if (ACTION_ADD_TO_QUEUE.equals(action)) {
            addToQueue(intent.getStringExtra(EXTRA_SONG_URI));
        } else if (ACTION_CLEAR_QUEUE.equals(action)) {
            clearQueue();
        } else if (ACTION_PLAY_QUEUE_ITEM.equals(action)) {
            playQueueItem(intent.getIntExtra(EXTRA_QUEUE_INDEX, -1));
        } else if (ACTION_REMOVE_FROM_QUEUE.equals(action)) {
            removeFromQueue(intent.getIntExtra(EXTRA_QUEUE_INDEX, -1));
        } else if (ACTION_REQUEST_QUEUE.equals(action)) {
            sendQueueChanged();
        } else if (ACTION_SET_QUEUE_ORDER.equals(action)) {
            setQueueOrder(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS),
                    intent.getIntExtra(EXTRA_QUEUE_INDEX, currentIndex));
        } else if (ACTION_SET_QUEUE_AND_PLAY.equals(action)) {
            setQueueAndPlay(intent.getStringArrayListExtra(EXTRA_QUEUE_URIS),
                    intent.getIntExtra(EXTRA_QUEUE_INDEX, 0));
        }
    }

    private void setQueueAndPlay(ArrayList<String> uris, int index) {
        if (uris == null || uris.isEmpty()) return;

        ArrayList<AudioFile> newQueue = resolveSongs(uris);
        if (newQueue.isEmpty()) return;

        queue.clear();
        queue.addAll(newQueue);
        currentIndex = Math.max(0, Math.min(index, queue.size() - 1));
        shuffleHistory.clear();
        rebuildPlayerQueue(currentIndex, 0L, true);
    }

    private void playSong(String uri) {
        if (uri == null || uri.trim().isEmpty()) return;

        if (queue.isEmpty()) loadQueue();
        int foundIndex = findSongIndex(uri);

        if (foundIndex >= 0) {
            currentIndex = foundIndex;
        } else {
            AudioFile song = findSongByUri(uri);
            if (song != null) {
                queue.add(song);
                currentIndex = queue.size() - 1;
            } else {
                return;
            }
        }

        currentUri = uri;
        updateCurrentAlbumColor();
        rebuildPlayerQueue(currentIndex, 0L, true);
    }

    private void rebuildPlayerQueue(int index, long positionMs, boolean play) {
        if (player == null || queue.isEmpty()) return;

        ArrayList<MediaItem> items = new ArrayList<>();
        for (AudioFile song : queue) {
            MediaItem item = toMediaItem(song);
            if (item != null) items.add(item);
        }

        if (items.isEmpty()) return;

        index = Math.max(0, Math.min(index, items.size() - 1));
        currentIndex = index;
        currentUri = queue.get(index).getUri();

        player.setMediaItems(items, index, Math.max(0L, positionMs));
        player.setShuffleModeEnabled(shuffleEnabled);
        player.setRepeatMode(toPlayerRepeatMode(repeatMode));
        player.prepare();
        if (play) player.play();

        updateMediaMetadata();
        sendQueueChanged();
        sendPlaybackState(play);
    }

    private MediaItem toMediaItem(AudioFile song) {
        if (song == null || song.getUri() == null || song.getUri().trim().isEmpty()) return null;

        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .setTitle(safeText(song.getTitle(), "Unknown title"))
                .setArtist(safeText(song.getArtist(), "Unknown artist"))
                .setAlbumTitle(safeText(song.getAlbum(), "Unknown album"));

        String path = song.getPath();
        if (path != null && !path.trim().isEmpty()) {
            try {
                metadata.setArtworkUri(Uri.parse(path));
            } catch (Exception ignored) {}
        }

        return new MediaItem.Builder()
                .setMediaId(song.getUri())
                .setUri(Uri.parse(song.getUri()))
                .setMediaMetadata(metadata.build())
                .build();
    }

    private int toPlayerRepeatMode(int mode) {
        if (mode == REPEAT_ONE) return Player.REPEAT_MODE_ONE;
        if (mode == REPEAT_ALL) return Player.REPEAT_MODE_ALL;
        return Player.REPEAT_MODE_OFF;
    }

    private void playNext() {
        if (player == null) return;
        syncCurrentFromPlayer();

        if (shuffleEnabled) {
            int next = getRandomShuffleIndex();
            if (next >= 0) {
                currentIndex = next;
                player.seekTo(next, 0L);
                player.play();
            }
            return;
        }

        if (currentIndex + 1 < queue.size()) {
            currentIndex++;
            player.seekTo(currentIndex, 0L);
            player.play();
        } else if (repeatMode == REPEAT_ALL && !queue.isEmpty()) {
            currentIndex = 0;
            player.seekTo(0, 0L);
            player.play();
        } else {
            player.pause();
        }
    }

    private void playPrevious() {
        if (player == null || queue.isEmpty()) return;

        if (player.getCurrentPosition() > 3000L) {
            player.seekTo(0L);
            return;
        }

        syncCurrentFromPlayer();

        if (shuffleEnabled) {
            int previous = getPreviousShuffleIndex();
            currentIndex = previous;
            player.seekTo(previous, 0L);
        } else if (currentIndex > 0) {
            currentIndex--;
            player.seekTo(currentIndex, 0L);
        } else if (repeatMode == REPEAT_ALL) {
            currentIndex = queue.size() - 1;
            player.seekTo(currentIndex, 0L);
        } else {
            player.seekTo(0L);
        }

        player.play();
    }

    private void seekTo(int position) {
        if (player == null || player.getCurrentMediaItem() == null) return;
        long duration = player.getDuration();
        if (duration == androidx.media3.common.C.TIME_UNSET) return;
        player.seekTo(Math.max(0L, Math.min(position, duration)));
        sendPlaybackState(player.isPlaying());
    }

    private void setShuffle(boolean enabled) {
        shuffleEnabled = enabled;
        shuffleHistory.clear();
        if (player != null) player.setShuffleModeEnabled(enabled);
        sendShuffleState();
        sendQueueChanged();
    }

    private void setRepeatMode(int mode) {
        repeatMode = Math.max(REPEAT_OFF, Math.min(REPEAT_ONE, mode));
        if (player != null) player.setRepeatMode(toPlayerRepeatMode(repeatMode));
        sendShuffleState();
    }

    private void toggleRepeatMode() {
        repeatMode = repeatMode == REPEAT_OFF
                ? REPEAT_ALL
                : repeatMode == REPEAT_ALL ? REPEAT_ONE : REPEAT_OFF;
        if (player != null) player.setRepeatMode(toPlayerRepeatMode(repeatMode));
        sendShuffleState();
    }

    private int getRandomShuffleIndex() {
        if (queue.size() <= 1) return currentIndex;

        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            if (i != currentIndex && !shuffleHistory.contains(i)) candidates.add(i);
        }
        if (candidates.isEmpty()) {
            shuffleHistory.clear();
            for (int i = 0; i < queue.size(); i++) {
                if (i != currentIndex) candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return currentIndex;

        if (currentIndex >= 0) shuffleHistory.add(currentIndex);
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int getPreviousShuffleIndex() {
        while (!shuffleHistory.isEmpty()) {
            int value = shuffleHistory.remove(shuffleHistory.size() - 1);
            if (value >= 0 && value < queue.size() && value != currentIndex) return value;
        }
        return Math.max(0, currentIndex);
    }

    private void addToQueue(String uri) {
        if (uri == null || uri.trim().isEmpty() || findSongIndex(uri) >= 0) return;

        AudioFile song = findSongByUri(uri);
        if (song == null) return;

        queue.add(song);

        if (player != null) {
            MediaItem item = toMediaItem(song);
            if (item != null) {
                if (player.getMediaItemCount() == 0) {
                    currentIndex = 0;
                    currentUri = song.getUri();
                    player.setMediaItem(item);
                    player.prepare();
                } else {
                    player.addMediaItem(item);
                }
            }
        }

        sendQueueChanged();
    }

    private void removeFromQueue(int index) {
        if (index < 0 || index >= queue.size()) return;

        boolean removingCurrent = index == currentIndex;
        queue.remove(index);

        if (queue.isEmpty()) {
            if (player != null) player.clearMediaItems();
            currentIndex = -1;
            currentUri = null;
            sendQueueChanged();
            sendPlaybackState(false);
            return;
        }

        if (player != null && index < player.getMediaItemCount()) {
            player.removeMediaItem(index);
        }

        if (index < currentIndex) currentIndex--;
        else if (removingCurrent) currentIndex = Math.min(index, queue.size() - 1);

        syncCurrentFromPlayer();
        sendQueueChanged();
    }

    private void clearQueue() {
        if (player == null || player.getMediaItemCount() == 0) {
            queue.clear();
            currentIndex = -1;
            currentUri = null;
            sendQueueChanged();
            return;
        }

        boolean wasPlaying = player.isPlaying();
        MediaItem current = player.getCurrentMediaItem();
        long position = Math.max(0L, player.getCurrentPosition());
        player.clearMediaItems();

        queue.clear();
        if (current != null && current.mediaId != null) {
            AudioFile song = findSongByUri(current.mediaId);
            if (song != null) queue.add(song);
        }

        if (!queue.isEmpty()) {
            currentIndex = 0;
            currentUri = queue.get(0).getUri();
            player.setMediaItem(current);
            player.prepare();
            player.seekTo(position);
            if (wasPlaying) player.play();
        } else {
            currentIndex = -1;
            currentUri = null;
        }

        sendQueueChanged();
    }

    private void playQueueItem(int index) {
        if (player == null || index < 0 || index >= queue.size()) return;
        shuffleHistory.clear();
        currentIndex = index;
        player.seekTo(index, 0L);
        player.play();
    }

    private void setQueueOrder(ArrayList<String> orderedUris, int requestedCurrentIndex) {
        if (orderedUris == null || orderedUris.isEmpty()) return;

        String playingUri = currentUri;
        long position = player == null ? 0L : Math.max(0L, player.getCurrentPosition());

        ArrayList<AudioFile> reordered = resolveSongs(orderedUris);
        if (reordered.isEmpty()) return;

        queue.clear();
        queue.addAll(reordered);

        int newIndex = findSongIndex(playingUri);
        if (newIndex < 0) newIndex = Math.max(0, Math.min(requestedCurrentIndex, queue.size() - 1));

        boolean wasPlaying = player != null && player.isPlaying();
        rebuildPlayerQueue(newIndex, position, wasPlaying);
    }

    private ArrayList<AudioFile> resolveSongs(ArrayList<String> uris) {
        ArrayList<AudioFile> result = new ArrayList<>();
        for (String uri : uris) {
            AudioFile song = findSongByUri(uri);
            if (song != null) result.add(song);
        }
        return result;
    }

    private void loadQueue() {
        try {
            ArrayList<AudioFile> result = new MusicRepository(getApplicationContext()).getAllSongs();
            if (result != null) {
                queue.clear();
                queue.addAll(result);
                if (currentUri != null) currentIndex = findSongIndex(currentUri);
            }
        } catch (Exception ignored) {}
        sendQueueChanged();
    }

    private AudioFile findSongByUri(String uri) {
        if (uri == null) return null;

        for (AudioFile song : queue) {
            if (song != null && uri.equals(song.getUri())) return song;
        }

        try {
            for (AudioFile song : new MusicRepository(getApplicationContext()).getAllSongs()) {
                if (song != null && uri.equals(song.getUri())) return song;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private int findSongIndex(String uri) {
        if (uri == null) return -1;
        for (int i = 0; i < queue.size(); i++) {
            AudioFile song = queue.get(i);
            if (song != null && uri.equals(song.getUri())) return i;
        }
        return -1;
    }

    private void syncCurrentFromPlayer() {
        if (player == null) return;

        int index = player.getCurrentMediaItemIndex();
        if (index >= 0 && index < queue.size()) {
            currentIndex = index;
            AudioFile song = queue.get(index);
            currentUri = song == null ? player.getCurrentMediaItem().mediaId : song.getUri();
        }
    }

    private void updateCurrentAlbumColor() {
        try {
            if (currentIndex >= 0 && currentIndex < queue.size()) {
                AudioFile song = queue.get(currentIndex);
                if (song != null) AlbumColorManager.getInstance(getApplicationContext()).setCurrentSong(song);
            }
        } catch (Exception ignored) {}
    }

    private void updateMediaMetadata() {
        syncCurrentFromPlayer();
    }

    private void sendShuffleState() {
        Intent intent = new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName());
        intent.putExtra(EXTRA_SHUFFLE_STATE, shuffleEnabled);
        intent.putExtra(EXTRA_REPEAT_STATE, repeatMode);
        sendBroadcast(intent);
    }

    private void sendQueueChanged() {
        Intent intent = new Intent(ACTION_QUEUE_CHANGED).setPackage(getPackageName());
        ArrayList<String> uris = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> artists = new ArrayList<>();
        ArrayList<String> albums = new ArrayList<>();

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

    private void sendPlaybackState(boolean playing) {
        int position = 0;
        long duration = 0L;

        try {
            if (player != null) {
                position = (int) Math.max(0L, player.getCurrentPosition());
                duration = player.getDuration();
                if (duration == androidx.media3.common.C.TIME_UNSET) duration = 0L;
            }
        } catch (Exception ignored) {}

        Intent intent = new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName());
        intent.putExtra(EXTRA_IS_PLAYING, playing);
        intent.putExtra(EXTRA_CURRENT_URI, currentUri);
        intent.putExtra(EXTRA_POSITION, position);
        intent.putExtra(EXTRA_DURATION, (int) Math.min(Integer.MAX_VALUE, duration));
        intent.putExtra(EXTRA_QUEUE_INDEX, currentIndex);
        sendBroadcast(intent);
    }

    private void startAudioAnalysis() {
        if (player == null || audioAnalysisRunning) return;

        try {
            int sessionId = player.getAudioSessionId();
            if (sessionId <= 0) return;

            releaseAudioVisualizer();

            audioVisualizer = new Visualizer(sessionId);
            int[] range = Visualizer.getCaptureSizeRange();
            int size = 1024;
            if (range != null && range.length >= 2) {
                size = Math.max(range[0], Math.min(1024, range[1]));
            }

            audioVisualizer.setCaptureSize(size);
            int rate = Math.min(8000000, Math.max(1, Visualizer.getMaxCaptureRate()));

            audioVisualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        @Override public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}

                        @Override public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                            processFFTData(fft, samplingRate);
                        }
                    },
                    rate,
                    false,
                    true
            );

            audioVisualizer.setEnabled(true);
            audioAnalysisRunning = true;
            resetAudioAnalysisState();
        } catch (Exception ignored) {
            releaseAudioVisualizer();
        }
    }

    private void processFFTData(byte[] fft, int rateMilliHz) {
        if (!audioAnalysisRunning || fft == null || fft.length < 4) return;

        try {
            if (rateMilliHz > 0) audioVisualizerSampleRate = rateMilliHz / 1000;

            float bass = calculateBassEnergy(fft);
            float energy = calculateOverallEnergy(fft);

            smoothedBass = smoothValue(smoothedBass, bass, .45f);
            smoothedEnergy = smoothValue(smoothedEnergy, energy, .32f);
            energyBaseline = energyBaseline <= 0
                    ? smoothedEnergy
                    : smoothValue(energyBaseline, smoothedEnergy, .025f);

            float bassRise = smoothedBass - previousBass;
            float energyRise = smoothedEnergy - energyBaseline;
            boolean beat = detectBeat(smoothedBass, smoothedEnergy, bassRise, energyRise);
            float intensity = Math.max(0, Math.min(1,
                    bassRise * 6f + energyRise * 4.5f
                            + smoothedBass * .45f + smoothedEnergy * .3f));

            previousBass = smoothedBass;

            byte[] copy = new byte[fft.length];
            System.arraycopy(fft, 0, copy, 0, fft.length);

            Intent intent = new Intent(ACTION_AUDIO_ANALYSIS).setPackage(getPackageName())
                    .putExtra(EXTRA_FFT, copy)
                    .putExtra(EXTRA_BASS, smoothedBass)
                    .putExtra(EXTRA_ENERGY, smoothedEnergy)
                    .putExtra(EXTRA_BEAT, beat)
                    .putExtra(EXTRA_BEAT_INTENSITY, intensity)
                    .putExtra(EXTRA_SAMPLE_RATE, audioVisualizerSampleRate);

            sendBroadcast(intent);
        } catch (Exception ignored) {}
    }

    private float calculateBassEnergy(byte[] fft) {
        return calculateBandEnergy(fft, 20f, 250f);
    }

    private float calculateOverallEnergy(byte[] fft) {
        return calculateBandEnergy(fft, 20f, 20000f);
    }

    private float calculateBandEnergy(byte[] fft, float low, float high) {
        if (fft == null || fft.length < 4) return 0f;

        int bins = fft.length / 2;
        float nyquist = Math.max(1f, audioVisualizerSampleRate / 2f);
        int start = Math.max(1, Math.round(Math.min(1, low / nyquist) * (bins - 1)));
        int end = Math.min(bins - 1, Math.round(Math.min(1, high / nyquist) * (bins - 1)));

        if (end <= start) end = Math.min(bins - 1, start + 1);

        double total = 0d;
        int count = 0;

        for (int i = start; i <= end; i++) {
            int index = i * 2;
            if (index + 1 >= fft.length) break;

            float real = fft[index];
            float imaginary = fft[index + 1];
            total += Math.sqrt(real * real + imaginary * imaginary);
            count++;
        }

        return count == 0 ? 0f : Math.min(1f, (float) (total / count / 128d));
    }

    private float smoothValue(float current, float target, float amount) {
        return current + (target - current) * amount;
    }

    private boolean detectBeat(float bass, float energy, float bassRise, float energyRise) {
        long now = System.currentTimeMillis();
        if (now - lastBeatTime < MIN_BEAT_INTERVAL_MS) return false;

        if (bassRise > .045f && energyRise > .008f) {
            lastBeatTime = now;
            return true;
        }

        if (bassRise > .09f && bass > .12f) {
            lastBeatTime = now;
            return true;
        }

        return false;
    }

    private void resetAudioAnalysisState() {
        smoothedEnergy = 0f;
        energyBaseline = 0f;
        smoothedBass = 0f;
        previousBass = 0f;
        lastBeatTime = 0L;
    }

    private void stopAudioAnalysis() {
        audioAnalysisRunning = false;
        releaseAudioVisualizer();
    }

    private void releaseAudioVisualizer() {
        audioAnalysisRunning = false;

        if (audioVisualizer != null) {
            try { audioVisualizer.setEnabled(false); } catch (Exception ignored) {}
            try { audioVisualizer.release(); } catch (Exception ignored) {}
            audioVisualizer = null;
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (player != null && player.isPlaying()) return;
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        stopAudioAnalysis();

        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}
