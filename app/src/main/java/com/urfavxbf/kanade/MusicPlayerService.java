// MusicPlayerService.java
// Playback stability + Queue Management
// - Handler/Runnable position updates
// - Improved audio-focus recovery
// - Shuffle history for real Previous behavior
// - Queue add / remove / clear
// - Queue item playback
// - Queue state request/broadcast

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
    public static final String ACTION_SET_QUEUE_AND_PLAY =
            "com.urfavxbf.kanade.ACTION_SET_QUEUE_AND_PLAY";

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    /*
     * ---------------------------------------------------------
     * QUEUE ACTIONS
     * ---------------------------------------------------------
     */

    public static final String ACTION_ADD_TO_QUEUE = "com.urfavxbf.kanade.ACTION_ADD_TO_QUEUE";

    public static final String ACTION_CLEAR_QUEUE = "com.urfavxbf.kanade.ACTION_CLEAR_QUEUE";

    public static final String ACTION_PLAY_QUEUE_ITEM =
            "com.urfavxbf.kanade.ACTION_PLAY_QUEUE_ITEM";

    public static final String ACTION_REMOVE_FROM_QUEUE =
            "com.urfavxbf.kanade.ACTION_REMOVE_FROM_QUEUE";

    public static final String ACTION_REQUEST_QUEUE = "com.urfavxbf.kanade.ACTION_REQUEST_QUEUE";

    public static final String EXTRA_QUEUE_INDEX = "com.urfavxbf.kanade.EXTRA_QUEUE_INDEX";

    public static final String ACTION_QUEUE_CHANGED = "com.urfavxbf.kanade.ACTION_QUEUE_CHANGED";

    public static final String EXTRA_QUEUE_SIZE = "com.urfavxbf.kanade.EXTRA_QUEUE_SIZE";

    public static final String EXTRA_QUEUE_URIS = "com.urfavxbf.kanade.EXTRA_QUEUE_URIS";

    public static final String EXTRA_QUEUE_TITLES = "com.urfavxbf.kanade.EXTRA_QUEUE_TITLES";

    public static final String EXTRA_QUEUE_ARTISTS = "com.urfavxbf.kanade.EXTRA_QUEUE_ARTISTS";

    public static final String EXTRA_QUEUE_ALBUMS = "com.urfavxbf.kanade.EXTRA_QUEUE_ALBUMS";
    public static final String ACTION_SET_QUEUE_ORDER =
            "com.urfavxbf.kanade.ACTION_SET_QUEUE_ORDER";

    /*
     * ---------------------------------------------------------
     * AUDIO ANALYSIS
     * ---------------------------------------------------------
     */

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

    private volatile boolean isUpdatingPosition = false;

    private final Handler positionHandler = new Handler(Looper.getMainLooper());

    private final Runnable positionUpdateRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!isUpdatingPosition) {
                        return;
                    }

                    try {

                        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {

                            stopPositionUpdates();
                            return;
                        }

                        int position = mediaPlayer.getCurrentPosition();

                        int duration = mediaPlayer.getDuration();

                        sendPlaybackState(true, position, duration);

                        updateMediaSessionPosition(position, duration);

                        if (isUpdatingPosition) {

                            positionHandler.postDelayed(this, 500);
                        }

                    } catch (Exception e) {

                        stopPositionUpdates();
                    }
                }
            };

    private MediaSessionCompat mediaSession;

    private AudioManager audioManager;

    private AudioFocusRequest audioFocusRequest;

    private boolean hasAudioFocus = false;

    private boolean wasPlayingBeforeFocusLoss = false;

    private boolean shuffleEnabled = false;

    private int repeatMode = REPEAT_OFF;

    private final Random random = new Random();

    private final ArrayList<Integer> shuffleHistory = new ArrayList<>();

    private Visualizer audioVisualizer;

    private int audioVisualizerSampleRate = 44100;

    private volatile boolean audioAnalysisRunning = false;

    private float smoothedEnergy = 0f;

    private float energyBaseline = 0f;

    private float smoothedBass = 0f;

    private float previousBass = 0f;

    private long lastBeatTime = 0L;

    private static final long MIN_BEAT_INTERVAL_MS = 115L;

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        try {

            mediaSession = new MediaSessionCompat(this, "KanadeMusic");

            mediaSession.setCallback(
                    new MediaSessionCompat.Callback() {

                        @Override
                        public void onPlay() {

                            if (mediaPlayer == null) {

                                if (currentUri != null) {
                                    playSong(currentUri);
                                }

                                return;
                            }

                            if (!requestAudioFocus()) {
                                return;
                            }

                            try {

                                if (!mediaPlayer.isPlaying()) {

                                    wasPlayingBeforeFocusLoss = false;

                                    mediaPlayer.start();

                                    startAudioAnalysis();

                                    startPositionUpdates();

                                    updateMediaSessionState(true);

                                    updateNotification();

                                    sendPlaybackState(true);
                                }

                            } catch (Exception e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onPause() {

                            pauseSong();
                        }

                        @Override
                        public void onSkipToNext() {

                            playNext();
                        }

                        @Override
                        public void onSkipToPrevious() {

                            playPrevious();
                        }

                        @Override
                        public void onSeekTo(long position) {

                            seekTo((int) position);
                        }

                        @Override
                        public void onStop() {

                            stopPlayback();
                        }
                    });

            mediaSession.setActive(false);

            updateMediaSessionState(false);

        } catch (Exception e) {

            e.printStackTrace();

            mediaSession = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        try {

            if (mediaSession != null) {

                MediaButtonReceiver.handleIntent(mediaSession, intent);
            }

        } catch (Exception ignored) {
        }

        if (ACTION_PLAY.equals(action)) {

            String uri = intent.getStringExtra(EXTRA_SONG_URI);

            /*
             * Resume the existing MediaPlayer when
             * the requested URI is the current song.
             *
             * This prevents playback position from
             * resetting when Play is pressed.
             */
            if (mediaPlayer != null
                    && currentUri != null
                    && (uri == null || uri.trim().isEmpty() || uri.equals(currentUri))) {

                try {

                    if (!mediaPlayer.isPlaying()) {

                        if (!requestAudioFocus()) {
                            return START_NOT_STICKY;
                        }

                        wasPlayingBeforeFocusLoss = false;

                        mediaPlayer.start();

                        startAudioAnalysis();

                        startPositionUpdates();

                        updateMediaSessionState(true);

                        updateNotification();

                        sendPlaybackState(true);
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                }

            } else if (uri != null && !uri.trim().isEmpty()) {

                if (currentUri == null || !uri.equals(currentUri)) {

                    playSong(uri);
                }

            } else {

                togglePlayPause();
            }

        } else if (ACTION_PAUSE.equals(action)) {

            pauseSong();

        } else if (ACTION_NEXT.equals(action)) {

            playNext();

        } else if (ACTION_PREVIOUS.equals(action)) {

            playPrevious();

        } else if (ACTION_SEEK.equals(action)) {

            int position = intent.getIntExtra(EXTRA_SEEK_POSITION, 0);

            seekTo(position);

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

            ArrayList<String> orderedUris = intent.getStringArrayListExtra(EXTRA_QUEUE_URIS);

            int requestedCurrentIndex = intent.getIntExtra(EXTRA_QUEUE_INDEX, currentIndex);

            setQueueOrder(orderedUris, requestedCurrentIndex);
        } else if (ACTION_REQUEST_QUEUE.equals(action)) {

            sendQueueChanged();
        }

        return START_NOT_STICKY;
    }

    private void setQueueOrder(ArrayList<String> orderedUris, int requestedCurrentIndex) {

        if (orderedUris == null || orderedUris.isEmpty()) {

            return;
        }

        ArrayList<AudioFile> reordered = new ArrayList<>();

        for (String uri : orderedUris) {

            if (uri == null || uri.trim().isEmpty()) {

                continue;
            }

            AudioFile song = findSongByUri(uri);

            if (song != null) {

                reordered.add(song);
            }
        }

        if (reordered.isEmpty()) {
            return;
        }

        String playingUri = currentUri;

        queue.clear();

        queue.addAll(reordered);

        currentIndex = -1;

        if (playingUri != null) {

            currentIndex = findSongIndex(playingUri);
        }

        if (currentIndex < 0
                && requestedCurrentIndex >= 0
                && requestedCurrentIndex < queue.size()) {

            currentIndex = requestedCurrentIndex;
        }

        sendQueueChanged();
    }

    /*
     * ---------------------------------------------------------
     * PLAY SONG
     * ---------------------------------------------------------
     */

    private void playSong(String uri) {

        try {

            if (uri == null || uri.trim().isEmpty()) {

                return;
            }

            if (!requestAudioFocus()) {
                return;
            }

            wasPlayingBeforeFocusLoss = false;

            if (queue.isEmpty()) {

                loadQueue();
            }

            int foundIndex = findSongIndex(uri);

            if (foundIndex >= 0) {

                currentIndex = foundIndex;

            } else {

                AudioFile song = findSongByUri(uri);

                if (song != null) {

                    queue.add(song);

                    currentIndex = queue.size() - 1;

                } else {

                    currentIndex = -1;
                }
            }

            currentUri = uri;

            updateCurrentAlbumColor();

            releasePlayer();

            mediaPlayer = MediaPlayer.create(this, Uri.parse(uri));

            if (mediaPlayer == null) {

                currentUri = null;

                currentIndex = -1;

                abandonAudioFocus();

                updateMediaSessionState(false);

                sendPlaybackState(false);

                return;
            }

            try {

                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build());

            } catch (Exception ignored) {
            }

            mediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {

                            handleCompletion();
                        }
                    });

            mediaPlayer.setOnErrorListener(
                    new MediaPlayer.OnErrorListener() {

                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {

                            releasePlayer();

                            updateMediaSessionState(false);

                            updateNotification();

                            sendPlaybackState(false);

                            return true;
                        }
                    });

            mediaPlayer.start();

            startAudioAnalysis();

            startPlaybackForeground();

            updateMediaMetadata();

            updateMediaSessionState(true);

            updateNotification();

            sendPlaybackState(true);

            sendQueueChanged();

            startPositionUpdates();

        } catch (Exception e) {

            e.printStackTrace();

            releasePlayer();

            currentUri = null;

            currentIndex = -1;

            abandonAudioFocus();

            updateMediaSessionState(false);

            sendPlaybackState(false);
        }
    }

    /*
     * ---------------------------------------------------------
     * PLAY / PAUSE
     * ---------------------------------------------------------
     */

    private void togglePlayPause() {

        if (mediaPlayer == null) {

            if (currentUri != null) {

                playSong(currentUri);
            }

            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                pauseSong();

            } else {

                if (!requestAudioFocus()) {
                    return;
                }

                wasPlayingBeforeFocusLoss = false;

                mediaPlayer.start();

                startAudioAnalysis();

                startPositionUpdates();

                updateMediaSessionState(true);

                updateNotification();

                sendPlaybackState(true);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void pauseSong() {

        wasPlayingBeforeFocusLoss = false;

        if (mediaPlayer == null) {
            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
            }

            stopAudioAnalysisCapture();

            stopPositionUpdates();

            updateMediaSessionState(false);

            updateNotification();

            sendPlaybackState(false);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /*
     * ---------------------------------------------------------
     * AUDIO ANALYSIS
     * ---------------------------------------------------------
     */

    private void startAudioAnalysis() {

        if (mediaPlayer == null) {
            return;
        }

        try {

            if (audioVisualizer != null) {

                resetAudioAnalysisState();

                try {

                    audioVisualizer.setEnabled(true);

                    audioAnalysisRunning = true;

                    return;

                } catch (Exception ignored) {

                    releaseAudioVisualizer();
                }
            }

            int audioSessionId = mediaPlayer.getAudioSessionId();

            if (audioSessionId <= 0) {
                return;
            }

            audioVisualizer = new Visualizer(audioSessionId);

            int[] captureRange = Visualizer.getCaptureSizeRange();

            int captureSize = chooseCaptureSize(captureRange);

            try {

                audioVisualizer.setCaptureSize(captureSize);

            } catch (Exception e) {

                try {

                    if (captureRange != null && captureRange.length >= 2) {

                        audioVisualizer.setCaptureSize(captureRange[0]);
                    }

                } catch (Exception ignored) {

                    releaseAudioVisualizer();

                    return;
                }
            }

            int maxCaptureRate;

            try {

                maxCaptureRate = Visualizer.getMaxCaptureRate();

            } catch (Exception e) {

                maxCaptureRate = 8000000;
            }

            int requestedCaptureRate = Math.min(8000000, maxCaptureRate);

            if (requestedCaptureRate <= 0) {

                requestedCaptureRate = 8000000;
            }

            audioVisualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {

                        @Override
                        public void onWaveFormDataCapture(
                                Visualizer visualizer, byte[] waveform, int samplingRate) {}

                        @Override
                        public void onFftDataCapture(
                                Visualizer visualizer, byte[] fft, int samplingRate) {

                            processFFTData(fft, samplingRate);
                        }
                    },
                    requestedCaptureRate,
                    false,
                    true);

            audioVisualizer.setEnabled(true);

            resetAudioAnalysisState();

            audioAnalysisRunning = true;

        } catch (Exception e) {

            e.printStackTrace();

            releaseAudioVisualizer();
        }
    }

    private int chooseCaptureSize(int[] range) {

        if (range == null || range.length < 2) {

            return 1024;
        }

        int min = range[0];

        int max = range[1];

        int preferred = 1024;

        if (preferred >= min && preferred <= max) {

            return preferred;
        }

        int size = min;

        if (size < 64) {

            size = 64;
        }

        int best = size;

        while (size <= max) {

            if (size <= preferred) {

                best = size;

            } else {

                break;
            }

            if (size > 16384) {

                break;
            }

            size *= 2;
        }

        if (best < min) {

            best = min;
        }

        if (best > max) {

            best = max;
        }

        return best;
    }

    private void processFFTData(byte[] fft, int samplingRateMilliHz) {

        if (!audioAnalysisRunning) {
            return;
        }

        if (fft == null || fft.length < 4) {

            return;
        }

        try {

            if (samplingRateMilliHz > 0) {

                audioVisualizerSampleRate = samplingRateMilliHz / 1000;
            }

            if (audioVisualizerSampleRate <= 0) {

                audioVisualizerSampleRate = 44100;
            }

            byte[] fftCopy = new byte[fft.length];

            System.arraycopy(fft, 0, fftCopy, 0, fft.length);

            float bass = calculateBassEnergy(fft);

            float energy = calculateOverallEnergy(fft);

            smoothedBass = smoothValue(smoothedBass, bass, 0.45f);

            smoothedEnergy = smoothValue(smoothedEnergy, energy, 0.32f);

            if (energyBaseline <= 0f) {

                energyBaseline = smoothedEnergy;

            } else {

                energyBaseline = smoothValue(energyBaseline, smoothedEnergy, 0.025f);
            }

            float bassRise = smoothedBass - previousBass;

            float energyRise = smoothedEnergy - energyBaseline;

            boolean beat = detectBeat(smoothedBass, smoothedEnergy, bassRise, energyRise);

            float beatIntensity =
                    calculateBeatIntensity(smoothedBass, smoothedEnergy, bassRise, energyRise);

            previousBass = smoothedBass;

            broadcastAudioAnalysis(
                    fftCopy,
                    smoothedBass,
                    smoothedEnergy,
                    beat,
                    beatIntensity,
                    audioVisualizerSampleRate);

        } catch (Exception ignored) {
        }
    }

    private float calculateBassEnergy(byte[] fft) {

        if (fft == null || fft.length < 4) {

            return 0f;
        }

        int binCount = fft.length / 2;

        float nyquist = audioVisualizerSampleRate / 2f;

        if (nyquist <= 0f) {

            nyquist = 22050f;
        }

        int startBin = frequencyToFFTBin(20f, binCount, nyquist);

        int endBin = frequencyToFFTBin(250f, binCount, nyquist);

        if (startBin < 1) {

            startBin = 1;
        }

        if (endBin >= binCount) {

            endBin = binCount - 1;
        }

        if (endBin <= startBin) {

            endBin = Math.min(binCount - 1, startBin + 4);
        }

        float total = 0f;

        int count = 0;

        for (int bin = startBin; bin <= endBin; bin++) {

            int index = bin * 2;

            if (index + 1 >= fft.length) {
                break;
            }

            float real = fft[index];

            float imaginary = fft[index + 1];

            float magnitude = (float) Math.sqrt(real * real + imaginary * imaginary);

            float normalized = magnitude / 181.02f;

            if (normalized > 1f) {

                normalized = 1f;
            }

            normalized = (float) Math.sqrt(normalized);

            total += normalized;

            count++;
        }

        if (count <= 0) {
            return 0f;
        }

        float result = total / count;

        if (result > 1f) {

            result = 1f;
        }

        return result;
    }

    private float calculateOverallEnergy(byte[] fft) {

        if (fft == null || fft.length < 4) {

            return 0f;
        }

        int binCount = fft.length / 2;

        if (binCount <= 1) {
            return 0f;
        }

        float total = 0f;

        float weightedTotal = 0f;

        int count = 0;

        for (int bin = 1; bin < binCount; bin++) {

            int index = bin * 2;

            if (index + 1 >= fft.length) {
                break;
            }

            float real = fft[index];

            float imaginary = fft[index + 1];

            float magnitude = (float) Math.sqrt(real * real + imaginary * imaginary);

            float normalized = magnitude / 181.02f;

            if (normalized > 1f) {

                normalized = 1f;
            }

            normalized = (float) Math.sqrt(normalized);

            float position = bin / (float) binCount;

            float bassWeight = 1f + (1f - position) * 0.45f;

            weightedTotal += normalized * bassWeight;

            total += normalized;

            count++;
        }

        if (count <= 0) {
            return 0f;
        }

        float average = total / count;

        float weightedAverage = weightedTotal / count;

        float energy = average * 0.55f + weightedAverage * 0.45f;

        if (energy > 1f) {

            energy = 1f;
        }

        return energy;
    }

    private int frequencyToFFTBin(float frequency, int binCount, float nyquist) {

        if (frequency <= 0f || binCount <= 1 || nyquist <= 0f) {

            return 1;
        }

        float normalized = frequency / nyquist;

        if (normalized < 0f) {

            normalized = 0f;
        }

        if (normalized > 1f) {

            normalized = 1f;
        }

        int bin = Math.round(normalized * (binCount - 1));

        if (bin < 1) {

            bin = 1;
        }

        if (bin >= binCount) {

            bin = binCount - 1;
        }

        return bin;
    }

    private float smoothValue(float current, float target, float factor) {

        return current + (target - current) * factor;
    }

    private boolean detectBeat(float bass, float energy, float bassRise, float energyRise) {

        long now = System.currentTimeMillis();

        if (now - lastBeatTime < MIN_BEAT_INTERVAL_MS) {

            return false;
        }

        float bassThreshold = 0.032f + energyBaseline * 0.055f;

        float energyThreshold = 0.022f + energyBaseline * 0.040f;

        boolean bassTransient = bassRise > bassThreshold;

        boolean energyTransient = energyRise > energyThreshold;

        boolean enoughEnergy = energy > Math.max(0.040f, energyBaseline * 0.55f);

        boolean strongBassBeat = bassTransient && bass > 0.075f;

        boolean combinedBeat = bassTransient && energyTransient && bass > 0.055f;

        boolean energyBeat = energyTransient && bassRise > bassThreshold * 0.65f && bass > 0.060f;

        boolean detected = enoughEnergy && (strongBassBeat || combinedBeat || energyBeat);

        if (detected) {

            lastBeatTime = now;
        }

        return detected;
    }

    private float calculateBeatIntensity(
            float bass, float energy, float bassRise, float energyRise) {

        float bassComponent = bassRise * 6.0f;

        float energyComponent = energyRise * 4.5f;

        float levelComponent = bass * 0.45f + energy * 0.30f;

        float intensity = bassComponent + energyComponent + levelComponent;

        if (bassRise > 0.10f) {

            intensity += 0.12f;
        }

        if (bassRise > 0.16f) {

            intensity += 0.15f;
        }

        if (intensity < 0f) {

            intensity = 0f;
        }

        if (intensity > 1f) {

            intensity = 1f;
        }

        return intensity;
    }

    private void broadcastAudioAnalysis(
            byte[] fft,
            float bass,
            float energy,
            boolean beat,
            float beatIntensity,
            int sampleRate) {

        try {

            Intent intent = new Intent(ACTION_AUDIO_ANALYSIS);

            intent.setPackage(getPackageName());

            intent.putExtra(EXTRA_FFT, fft);

            intent.putExtra(EXTRA_BASS, bass);

            intent.putExtra(EXTRA_ENERGY, energy);

            intent.putExtra(EXTRA_BEAT, beat);

            intent.putExtra(EXTRA_BEAT_INTENSITY, beatIntensity);

            intent.putExtra(EXTRA_SAMPLE_RATE, sampleRate);

            sendBroadcast(intent);

        } catch (Exception ignored) {
        }
    }

    private void resetAudioAnalysisState() {

        smoothedEnergy = 0f;

        energyBaseline = 0f;

        smoothedBass = 0f;

        previousBass = 0f;

        lastBeatTime = 0L;
    }

    private void stopAudioAnalysisCapture() {

        audioAnalysisRunning = false;

        if (audioVisualizer != null) {

            try {

                audioVisualizer.setEnabled(false);

            } catch (Exception ignored) {
            }
        }

        resetAudioAnalysisState();

        broadcastAudioAnalysis(new byte[0], 0f, 0f, false, 0f, audioVisualizerSampleRate);
    }

    private void releaseAudioVisualizer() {

        audioAnalysisRunning = false;

        if (audioVisualizer != null) {

            try {

                audioVisualizer.setEnabled(false);

            } catch (Exception ignored) {
            }

            try {

                audioVisualizer.setDataCaptureListener(null, 0, false, false);

            } catch (Exception ignored) {
            }

            try {

                audioVisualizer.release();

            } catch (Exception ignored) {
            }

            audioVisualizer = null;
        }

        resetAudioAnalysisState();

        broadcastAudioAnalysis(new byte[0], 0f, 0f, false, 0f, audioVisualizerSampleRate);
    }

    /*
     * ---------------------------------------------------------
     * NEXT / PREVIOUS
     * ---------------------------------------------------------
     */

    private void playNext() {

        loadQueueIfNeeded();

        if (queue.isEmpty()) {
            return;
        }

        if (repeatMode == REPEAT_ONE && currentIndex >= 0 && currentIndex < queue.size()) {

            AudioFile currentSong = queue.get(currentIndex);

            if (currentSong != null && currentSong.getUri() != null) {

                playSong(currentSong.getUri());
            }

            return;
        }

        int nextIndex;

        if (shuffleEnabled) {

            if (currentIndex >= 0 && currentIndex < queue.size()) {

                if (shuffleHistory.isEmpty()
                        || shuffleHistory.get(shuffleHistory.size() - 1) != currentIndex) {

                    shuffleHistory.add(currentIndex);
                }
            }

            nextIndex = getRandomShuffleIndex();

        } else {

            if (currentIndex < 0) {

                if (currentUri != null) {

                    currentIndex = findSongIndex(currentUri);
                }

                if (currentIndex < 0) {

                    currentIndex = 0;
                }
            }

            nextIndex = currentIndex + 1;

            if (nextIndex >= queue.size()) {

                if (repeatMode == REPEAT_ALL) {

                    nextIndex = 0;

                } else {

                    currentIndex = queue.size() - 1;

                    if (mediaPlayer != null) {

                        try {

                            mediaPlayer.seekTo(0);

                        } catch (Exception ignored) {
                        }
                    }

                    pauseSong();

                    return;
                }
            }
        }

        currentIndex = nextIndex;

        AudioFile nextSong = queue.get(currentIndex);

        if (nextSong == null || nextSong.getUri() == null) {

            return;
        }

        playSong(nextSong.getUri());
    }

    private void playPrevious() {

        loadQueueIfNeeded();

        if (queue.isEmpty()) {
            return;
        }

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.getCurrentPosition() > 3000) {

                    mediaPlayer.seekTo(0);

                    return;
                }

            } catch (Exception ignored) {
            }
        }

        if (currentIndex < 0) {

            if (currentUri != null) {

                currentIndex = findSongIndex(currentUri);
            }

            if (currentIndex < 0) {

                currentIndex = 0;
            }
        }

        int previousIndex;

        if (shuffleEnabled) {

            previousIndex = getPreviousShuffleIndex();

        } else {

            previousIndex = currentIndex - 1;

            if (previousIndex < 0) {

                if (repeatMode == REPEAT_ALL) {

                    previousIndex = queue.size() - 1;

                } else {

                    previousIndex = 0;
                }
            }
        }

        currentIndex = previousIndex;

        AudioFile previousSong = queue.get(currentIndex);

        if (previousSong == null || previousSong.getUri() == null) {

            return;
        }

        playSong(previousSong.getUri());
    }

    private void handleCompletion() {

        if (repeatMode == REPEAT_ONE) {

            if (currentUri != null) {

                playSong(currentUri);
            }

            return;
        }

        playNext();
    }

    /*
     * ---------------------------------------------------------
     * SHUFFLE
     * ---------------------------------------------------------
     */

    private void setShuffle(boolean enabled) {

        shuffleEnabled = enabled;

        shuffleHistory.clear();

        sendShuffleState();

        sendQueueChanged();
    }

    private int getRandomShuffleIndex() {

        if (queue.size() <= 1) {

            return 0;
        }

        ArrayList<Integer> candidates = new ArrayList<>();

        for (int i = 0; i < queue.size(); i++) {

            if (i == currentIndex) {
                continue;
            }

            if (!shuffleHistory.contains(i)) {

                candidates.add(i);
            }
        }

        if (candidates.isEmpty()) {

            for (int i = 0; i < queue.size(); i++) {

                if (i != currentIndex) {

                    candidates.add(i);
                }
            }
        }

        if (candidates.isEmpty()) {

            return currentIndex;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private int getPreviousShuffleIndex() {

        while (!shuffleHistory.isEmpty()) {

            int last = shuffleHistory.remove(shuffleHistory.size() - 1);

            if (last >= 0 && last < queue.size() && last != currentIndex) {

                return last;
            }
        }

        return currentIndex;
    }

    private void sendShuffleState() {

        Intent intent = new Intent(ACTION_STATE_CHANGED);

        intent.setPackage(getPackageName());

        intent.putExtra(EXTRA_SHUFFLE_STATE, shuffleEnabled);

        intent.putExtra(EXTRA_REPEAT_STATE, repeatMode);

        sendBroadcast(intent);
    }

    /*
     * ---------------------------------------------------------
     * REPEAT
     * ---------------------------------------------------------
     */

    private void setRepeatMode(int mode) {

        if (mode < REPEAT_OFF || mode > REPEAT_ONE) {

            mode = REPEAT_OFF;
        }

        repeatMode = mode;

        sendShuffleState();
    }

    private void toggleRepeatMode() {

        if (repeatMode == REPEAT_OFF) {

            repeatMode = REPEAT_ALL;

        } else if (repeatMode == REPEAT_ALL) {

            repeatMode = REPEAT_ONE;

        } else {

            repeatMode = REPEAT_OFF;
        }

        sendShuffleState();
    }

    /*
     * ---------------------------------------------------------
     * QUEUE
     * ---------------------------------------------------------
     */

    private void addToQueue(String uri) {

        if (uri == null || uri.trim().isEmpty()) {

            return;
        }

        AudioFile song = findSongByUri(uri);

        if (song == null) {
            return;
        }

        /*
         * Prevent duplicate queue entries.
         */
        if (findSongIndex(uri) >= 0) {

            return;
        }

        queue.add(song);

        if (currentIndex < 0) {

            currentIndex = 0;
        }

        sendQueueChanged();
    }

    private void removeFromQueue(int index) {

        if (index < 0 || index >= queue.size()) {

            return;
        }

        boolean removingCurrent = index == currentIndex;

        queue.remove(index);

        /*
         * Queue became empty.
         */
        if (queue.isEmpty()) {

            currentIndex = -1;

            currentUri = null;

            stopPlayback();

            return;
        }

        /*
         * Update shuffle history because
         * removing an item changes queue indexes.
         */
        ArrayList<Integer> updatedHistory = new ArrayList<>();

        for (Integer historyIndex : shuffleHistory) {

            if (historyIndex == null) {
                continue;
            }

            int value = historyIndex;

            /*
             * Removed item no longer exists.
             */
            if (value == index) {
                continue;
            }

            /*
             * Items after the removed item
             * move one position backward.
             */
            if (value > index) {

                value--;
            }

            if (value >= 0 && value < queue.size()) {

                updatedHistory.add(value);
            }
        }

        shuffleHistory.clear();

        shuffleHistory.addAll(updatedHistory);

        /*
         * Removing an item before the current
         * song shifts currentIndex backward.
         */
        if (index < currentIndex) {

            currentIndex--;

            sendQueueChanged();

            return;
        }

        /*
         * Removing a song after current song
         * does not affect playback.
         */
        if (!removingCurrent) {

            sendQueueChanged();

            return;
        }

        /*
         * Current song was removed.
         *
         * Play the song that moved into the
         * removed position. If it was the last
         * item, play the new last item.
         */
        int nextIndex;

        if (index < queue.size()) {

            nextIndex = index;

        } else {

            nextIndex = queue.size() - 1;
        }

        AudioFile nextSong = queue.get(nextIndex);

        if (nextSong == null || nextSong.getUri() == null) {

            currentIndex = nextIndex;

            sendQueueChanged();

            return;
        }

        currentIndex = nextIndex;

        currentUri = nextSong.getUri();

        shuffleHistory.clear();

        playSong(nextSong.getUri());

        sendQueueChanged();
    }

    private void clearQueue() {

        AudioFile currentSong = null;

        if (currentIndex >= 0 && currentIndex < queue.size()) {

            currentSong = queue.get(currentIndex);
        }

        queue.clear();

        shuffleHistory.clear();

        /*
         * Preserve the currently playing song.
         */
        if (currentSong != null) {

            queue.add(currentSong);

            currentIndex = 0;

        } else {

            currentIndex = -1;
        }

        sendQueueChanged();
    }

    private void playQueueItem(int index) {

        if (index < 0 || index >= queue.size()) {

            return;
        }

        AudioFile song = queue.get(index);

        if (song == null || song.getUri() == null) {

            return;
        }

        /*
         * Explicit queue selection starts a
         * fresh shuffle history.
         */
        shuffleHistory.clear();

        currentIndex = index;

        playSong(song.getUri());
    }

    private void sendQueueChanged() {

        Intent intent = new Intent(ACTION_QUEUE_CHANGED);

        intent.setPackage(getPackageName());

        ArrayList<String> uris = new ArrayList<>();

        ArrayList<String> titles = new ArrayList<>();

        ArrayList<String> artists = new ArrayList<>();

        ArrayList<String> albums = new ArrayList<>();

        for (AudioFile song : queue) {

            if (song == null) {

                uris.add("");

                titles.add("Unknown title");

                artists.add("Unknown artist");

                albums.add("Unknown album");

                continue;
            }

            String uri = song.getUri();

            if (uri == null) {

                uri = "";
            }

            String title = song.getTitle();

            if (title == null || title.trim().isEmpty()) {

                title = "Unknown title";
            }

            String artist = song.getArtist();

            if (artist == null || artist.trim().isEmpty()) {

                artist = "Unknown artist";
            }

            String album = song.getAlbum();

            if (album == null || album.trim().isEmpty()) {

                album = "Unknown album";
            }

            uris.add(uri);

            titles.add(title);

            artists.add(artist);

            albums.add(album);
        }

        intent.putStringArrayListExtra(EXTRA_QUEUE_URIS, uris);

        intent.putStringArrayListExtra(EXTRA_QUEUE_TITLES, titles);

        intent.putStringArrayListExtra(EXTRA_QUEUE_ARTISTS, artists);

        intent.putStringArrayListExtra(EXTRA_QUEUE_ALBUMS, albums);

        intent.putExtra(EXTRA_QUEUE_SIZE, queue.size());

        intent.putExtra(EXTRA_QUEUE_INDEX, currentIndex);

        sendBroadcast(intent);
    }

    /*
     * ---------------------------------------------------------
     * QUEUE LOADING
     * ---------------------------------------------------------
     */

    private void loadQueueIfNeeded() {

        if (queue.isEmpty()) {

            loadQueue();
        }
    }

    private void loadQueue() {

        try {

            MusicRepository repository = new MusicRepository(getApplicationContext());

            ArrayList<AudioFile> result = repository.getAllSongs();

            if (result != null) {

                queue.clear();

                queue.addAll(result);

                shuffleHistory.clear();

                if (currentUri != null) {

                    int index = findSongIndex(currentUri);

                    if (index >= 0) {

                        currentIndex = index;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        sendQueueChanged();
    }

    private AudioFile findSongByUri(String uri) {

        if (uri == null) {
            return null;
        }

        for (AudioFile song : queue) {

            if (song != null && uri.equals(song.getUri())) {

                return song;
            }
        }

        try {

            MusicRepository repository = new MusicRepository(getApplicationContext());

            ArrayList<AudioFile> songs = repository.getAllSongs();

            if (songs != null) {

                for (AudioFile song : songs) {

                    if (song != null && uri.equals(song.getUri())) {

                        return song;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    private int findSongIndex(String uri) {

        if (uri == null) {
            return -1;
        }

        for (int i = 0; i < queue.size(); i++) {

            AudioFile song = queue.get(i);

            if (song != null && uri.equals(song.getUri())) {

                return i;
            }
        }

        return -1;
    }

    /*
     * ---------------------------------------------------------
     * SEEK
     * ---------------------------------------------------------
     */

    private void seekTo(int position) {

        if (mediaPlayer == null) {
            return;
        }

        try {

            int duration = mediaPlayer.getDuration();

            if (duration <= 0) {
                return;
            }

            if (position < 0) {

                position = 0;
            }

            if (position > duration) {

                position = duration;
            }

            mediaPlayer.seekTo(position);

            boolean playing = isPlayerPlaying();

            updateMediaSessionState(playing);

            sendPlaybackState(playing);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /*
     * ---------------------------------------------------------
     * POSITION UPDATES
     * ---------------------------------------------------------
     */

    private void startPositionUpdates() {

        positionHandler.removeCallbacks(positionUpdateRunnable);

        isUpdatingPosition = true;

        positionHandler.post(positionUpdateRunnable);
    }

    private void stopPositionUpdates() {

        isUpdatingPosition = false;

        positionHandler.removeCallbacks(positionUpdateRunnable);
    }

    /*
     * ---------------------------------------------------------
     * AUDIO FOCUS
     * ---------------------------------------------------------
     */

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {

                @Override
                public void onAudioFocusChange(int focusChange) {

                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {

                        boolean wasPlaying = isPlayerPlaying();

                        pauseSong();

                        wasPlayingBeforeFocusLoss = wasPlaying;

                        abandonAudioFocus();

                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

                        boolean wasPlaying = isPlayerPlaying();

                        pauseSong();

                        wasPlayingBeforeFocusLoss = wasPlaying;

                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                        if (wasPlayingBeforeFocusLoss
                                && mediaPlayer != null
                                && !mediaPlayer.isPlaying()
                                && currentUri != null) {

                            try {

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                    int result = audioManager.requestAudioFocus(audioFocusRequest);

                                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                                        return;
                                    }

                                    hasAudioFocus = true;

                                } else {

                                    int result =
                                            audioManager.requestAudioFocus(
                                                    audioFocusChangeListener,
                                                    AudioManager.STREAM_MUSIC,
                                                    AudioManager.AUDIOFOCUS_GAIN);

                                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                                        return;
                                    }

                                    hasAudioFocus = true;
                                }

                                mediaPlayer.start();

                                wasPlayingBeforeFocusLoss = false;

                                startAudioAnalysis();

                                startPositionUpdates();

                                updateMediaSessionState(true);

                                updateNotification();

                                sendPlaybackState(true);

                            } catch (Exception e) {

                                e.printStackTrace();
                            }
                        }
                    }
                }
            };

    private boolean requestAudioFocus() {

        if (audioManager == null) {

            return true;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                if (audioFocusRequest == null) {

                    audioFocusRequest =
                            new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                    .setAudioAttributes(
                                            new android.media.AudioAttributes.Builder()
                                                    .setUsage(
                                                            android.media.AudioAttributes
                                                                    .USAGE_MEDIA)
                                                    .setContentType(
                                                            android.media.AudioAttributes
                                                                    .CONTENT_TYPE_MUSIC)
                                                    .build())
                                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                                    .build();
                }

                int result = audioManager.requestAudioFocus(audioFocusRequest);

                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

            } else {

                int result =
                        audioManager.requestAudioFocus(
                                audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN);

                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            }

        } catch (Exception e) {

            e.printStackTrace();

            hasAudioFocus = false;
        }

        return hasAudioFocus;
    }

    private void abandonAudioFocus() {

        if (audioManager == null) {
            return;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                if (audioFocusRequest != null) {

                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                }

            } else {

                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }

        } catch (Exception ignored) {
        }

        hasAudioFocus = false;
    }

    /*
     * ---------------------------------------------------------
     * MEDIA SESSION
     * ---------------------------------------------------------
     */

    private void updateMediaSessionState(boolean playing) {

        if (mediaSession == null) {
            return;
        }

        long actions =
                PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_STOP;

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.isPlaying()) {

                    playing = true;
                }

            } catch (Exception ignored) {
            }
        }

        int state;

        if (currentUri == null) {

            state = PlaybackStateCompat.STATE_NONE;

        } else if (playing) {

            state = PlaybackStateCompat.STATE_PLAYING;

        } else {

            state = PlaybackStateCompat.STATE_PAUSED;
        }

        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;

        if (mediaPlayer != null) {

            try {

                position = mediaPlayer.getCurrentPosition();

            } catch (Exception ignored) {
            }
        }

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(actions)
                        .setState(state, position, playing ? 1f : 0f)
                        .build();

        try {

            mediaSession.setPlaybackState(playbackState);

            mediaSession.setActive(currentUri != null);

        } catch (Exception ignored) {
        }
    }

    private void updateMediaSessionPosition(int position, int duration) {

        if (mediaSession == null) {
            return;
        }

        boolean playing = isPlayerPlaying();

        long actions =
                PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_STOP;

        int state = playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(actions)
                        .setState(state, position, playing ? 1f : 0f)
                        .build();

        try {

            mediaSession.setPlaybackState(playbackState);

        } catch (Exception ignored) {
        }
    }

    private void updateMediaMetadata() {

        if (mediaSession == null) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= queue.size()) {

            return;
        }

        AudioFile song = queue.get(currentIndex);

        if (song == null) {
            return;
        }

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE, safeText(song.getTitle(), "Unknown title"));

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                safeText(song.getArtist(), "Unknown artist"));

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM, safeText(song.getAlbum(), "Unknown album"));

        if (mediaPlayer != null) {

            try {

                builder.putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());

            } catch (Exception ignored) {
            }
        }

        Bitmap albumArt = loadAlbumArt(song);

        if (albumArt != null) {

            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);

            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, albumArt);
        }

        try {

            mediaSession.setMetadata(builder.build());

        } catch (Exception ignored) {
        }
    }

    private void updateCurrentAlbumColor() {

        try {

            if (currentIndex >= 0 && currentIndex < queue.size()) {

                AudioFile currentSong = queue.get(currentIndex);

                if (currentSong != null) {

                    AlbumColorManager.getInstance(getApplicationContext())
                            .setCurrentSong(currentSong);
                }
            }

        } catch (Exception ignored) {
        }
    }

    private Bitmap loadAlbumArt(AudioFile song) {

        if (song == null) {
            return null;
        }

        String path = song.getPath();

        if (path == null || path.trim().isEmpty()) {

            return null;
        }

        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();

        try {

            retriever.setDataSource(path);

            byte[] artwork = retriever.getEmbeddedPicture();

            if (artwork != null && artwork.length > 0) {

                return BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
            }

        } catch (Exception ignored) {

        } finally {

            try {

                retriever.release();

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String safeText(String value, String fallback) {

        if (value == null || value.trim().isEmpty()) {

            return fallback;
        }

        return value;
    }

    /*
     * ---------------------------------------------------------
     * NOTIFICATION
     * ---------------------------------------------------------
     */

    private void startPlaybackForeground() {

        try {

            Notification notification = buildNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);

            } else {

                startForeground(NOTIFICATION_ID, notification);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private Notification buildNotification() {

        String title = "Kanade Music";

        String artist = "Not playing";

        Bitmap artwork = null;

        if (currentIndex >= 0 && currentIndex < queue.size()) {

            AudioFile song = queue.get(currentIndex);

            if (song != null) {

                title = safeText(song.getTitle(), "Unknown title");

                artist = safeText(song.getArtist(), "Unknown artist");

                artwork = loadAlbumArt(song);
            }
        }

        Intent contentIntent = new Intent(this, MainActivity.class);

        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent =
                PendingIntent.getActivity(this, 500, contentIntent, pendingIntentFlags());

        PendingIntent previousPendingIntent = createServicePendingIntent(ACTION_PREVIOUS, 501);

        boolean playing = isPlayerPlaying();

        PendingIntent playPausePendingIntent =
                createServicePendingIntent(playing ? ACTION_PAUSE : ACTION_PLAY, 502);

        PendingIntent nextPendingIntent = createServicePendingIntent(ACTION_NEXT, 503);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle(title)
                        .setContentText(artist)
                        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOnlyAlertOnce(true)
                        .setOngoing(playing)
                        .setContentIntent(contentPendingIntent)
                        .addAction(
                                new NotificationCompat.Action(
                                        android.R.drawable.ic_media_previous,
                                        "Previous",
                                        previousPendingIntent))
                        .addAction(
                                new NotificationCompat.Action(
                                        playing
                                                ? android.R.drawable.ic_media_pause
                                                : android.R.drawable.ic_media_play,
                                        playing ? "Pause" : "Play",
                                        playPausePendingIntent))
                        .addAction(
                                new NotificationCompat.Action(
                                        android.R.drawable.ic_media_next,
                                        "Next",
                                        nextPendingIntent));

        if (mediaSession != null) {

            try {

                builder.setStyle(
                        new MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2));

            } catch (Exception ignored) {
            }
        }

        if (artwork != null) {

            builder.setLargeIcon(artwork);
        }

        return builder.build();
    }

    private void updateNotification() {

        if (currentUri == null) {
            return;
        }

        try {

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {

                manager.notify(NOTIFICATION_ID, buildNotification());
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private PendingIntent createServicePendingIntent(String action, int requestCode) {

        Intent intent = new Intent(this, MusicPlayerService.class);

        intent.setAction(action);

        return PendingIntent.getService(this, requestCode, intent, pendingIntentFlags());
    }

    private int pendingIntentFlags() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    /*
     * ---------------------------------------------------------
     * PLAYBACK STATE
     * ---------------------------------------------------------
     */

    private void sendPlaybackState(boolean isPlaying) {

        int position = 0;

        int duration = 0;

        if (mediaPlayer != null) {

            try {

                position = mediaPlayer.getCurrentPosition();

                duration = mediaPlayer.getDuration();

            } catch (Exception ignored) {
            }
        }

        sendPlaybackState(isPlaying, position, duration);
    }

    private void sendPlaybackState(boolean isPlaying, int position, int duration) {

        Intent intent = new Intent(ACTION_STATE_CHANGED);

        intent.setPackage(getPackageName());

        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);

        intent.putExtra(EXTRA_CURRENT_URI, currentUri);

        intent.putExtra(EXTRA_POSITION, position);

        intent.putExtra(EXTRA_DURATION, duration);

        intent.putExtra(EXTRA_SHUFFLE_STATE, shuffleEnabled);

        intent.putExtra(EXTRA_REPEAT_STATE, repeatMode);

        sendBroadcast(intent);
    }

    private boolean isPlayerPlaying() {

        if (mediaPlayer == null) {
            return false;
        }

        try {

            return mediaPlayer.isPlaying();

        } catch (Exception ignored) {

            return false;
        }
    }

    /*
     * ---------------------------------------------------------
     * NOTIFICATION CHANNEL
     * ---------------------------------------------------------
     */

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW);

        channel.setDescription("Kanade music playback controls");

        channel.setShowBadge(false);

        NotificationManager manager = getSystemService(NotificationManager.class);

        if (manager != null) {

            manager.createNotificationChannel(channel);
        }
    }

    /*
     * ---------------------------------------------------------
     * STOP / RELEASE
     * ---------------------------------------------------------
     */

    private void stopPlayback() {

        stopPositionUpdates();

        wasPlayingBeforeFocusLoss = false;

        releasePlayer();

        abandonAudioFocus();

        currentUri = null;

        currentIndex = -1;

        shuffleHistory.clear();

        if (mediaSession != null) {

            try {

                PlaybackStateCompat playbackState =
                        new PlaybackStateCompat.Builder()
                                .setActions(
                                        PlaybackStateCompat.ACTION_PLAY
                                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                                .setState(
                                        PlaybackStateCompat.STATE_NONE,
                                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                        0f)
                                .build();

                mediaSession.setPlaybackState(playbackState);

                mediaSession.setActive(false);

            } catch (Exception ignored) {
            }
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                stopForeground(STOP_FOREGROUND_REMOVE);

            } else {

                stopForeground(true);
            }

        } catch (Exception ignored) {
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {

            manager.cancel(NOTIFICATION_ID);
        }

        sendPlaybackState(false);

        sendQueueChanged();
    }

    private void releasePlayer() {

        stopPositionUpdates();

        releaseAudioVisualizer();

        if (mediaPlayer != null) {

            try {

                mediaPlayer.stop();

            } catch (Exception ignored) {
            }

            try {

                mediaPlayer.reset();

            } catch (Exception ignored) {
            }

            try {

                mediaPlayer.release();

            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    /*
     * ---------------------------------------------------------
     * DESTROY
     * ---------------------------------------------------------
     */

    @Override
    public void onDestroy() {

        stopPositionUpdates();

        wasPlayingBeforeFocusLoss = false;

        abandonAudioFocus();

        sendPlaybackState(false);

        releasePlayer();

        if (mediaSession != null) {

            try {

                mediaSession.setActive(false);

                mediaSession.release();

            } catch (Exception ignored) {
            }

            mediaSession = null;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                stopForeground(STOP_FOREGROUND_REMOVE);

            } else {

                stopForeground(true);
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
