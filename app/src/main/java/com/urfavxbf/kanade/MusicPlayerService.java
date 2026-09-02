package com.urfavxbf.kanade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MusicPlayerService extends Service {

    public static final String ACTION_PLAY =
            "com.urfavxbf.kanade.ACTION_PLAY";

    public static final String ACTION_PAUSE =
            "com.urfavxbf.kanade.ACTION_PAUSE";

    public static final String ACTION_NEXT =
            "com.urfavxbf.kanade.ACTION_NEXT";

    public static final String ACTION_PREVIOUS =
            "com.urfavxbf.kanade.ACTION_PREVIOUS";

    public static final String EXTRA_SONG_URI =
            "com.urfavxbf.kanade.EXTRA_SONG_URI";

    public static final String ACTION_STATE_CHANGED =
            "com.urfavxbf.kanade.ACTION_STATE_CHANGED";

    public static final String EXTRA_IS_PLAYING =
            "com.urfavxbf.kanade.EXTRA_IS_PLAYING";

    public static final String EXTRA_CURRENT_URI =
            "com.urfavxbf.kanade.EXTRA_CURRENT_URI";

    public static final String ACTION_SEEK =
            "com.urfavxbf.kanade.ACTION_SEEK";

    public static final String EXTRA_SEEK_POSITION =
            "com.urfavxbf.kanade.EXTRA_SEEK_POSITION";

    public static final String EXTRA_DURATION =
            "com.urfavxbf.kanade.EXTRA_DURATION";

    public static final String EXTRA_POSITION =
            "com.urfavxbf.kanade.EXTRA_POSITION";

    /*
     * SHUFFLE
     */

    public static final String ACTION_SET_SHUFFLE =
            "com.urfavxbf.kanade.ACTION_SET_SHUFFLE";

    public static final String ACTION_TOGGLE_SHUFFLE =
            "com.urfavxbf.kanade.ACTION_TOGGLE_SHUFFLE";

    public static final String EXTRA_SHUFFLE_ENABLED =
            "com.urfavxbf.kanade.EXTRA_SHUFFLE_ENABLED";

    public static final String EXTRA_SHUFFLE_STATE =
            "com.urfavxbf.kanade.EXTRA_SHUFFLE_STATE";

    /*
     * REPEAT
     */

    public static final String ACTION_SET_REPEAT =
            "com.urfavxbf.kanade.ACTION_SET_REPEAT";

    public static final String ACTION_TOGGLE_REPEAT =
            "com.urfavxbf.kanade.ACTION_TOGGLE_REPEAT";

    public static final String EXTRA_REPEAT_MODE =
            "com.urfavxbf.kanade.EXTRA_REPEAT_MODE";

    public static final String EXTRA_REPEAT_STATE =
            "com.urfavxbf.kanade.EXTRA_REPEAT_STATE";

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    /*
     * QUEUE
     */

    public static final String ACTION_ADD_TO_QUEUE =
            "com.urfavxbf.kanade.ACTION_ADD_TO_QUEUE";

    public static final String ACTION_CLEAR_QUEUE =
            "com.urfavxbf.kanade.ACTION_CLEAR_QUEUE";

    public static final String ACTION_PLAY_QUEUE_ITEM =
            "com.urfavxbf.kanade.ACTION_PLAY_QUEUE_ITEM";

    public static final String EXTRA_QUEUE_INDEX =
            "com.urfavxbf.kanade.EXTRA_QUEUE_INDEX";

    public static final String ACTION_QUEUE_CHANGED =
            "com.urfavxbf.kanade.ACTION_QUEUE_CHANGED";

    public static final String EXTRA_QUEUE_SIZE =
            "com.urfavxbf.kanade.EXTRA_QUEUE_SIZE";

    private static final String CHANNEL_ID =
            "kanade_music_playback";

    private static final int NOTIFICATION_ID =
            1001;

    private MediaPlayer mediaPlayer;

    private String currentUri;

    private final ArrayList<AudioFile> queue =
            new ArrayList<>();

    private int currentIndex = -1;

    private volatile boolean isUpdatingPosition = false;

    private MediaSessionCompat mediaSession;

    private AudioManager audioManager;

    private AudioFocusRequest audioFocusRequest;

    private boolean hasAudioFocus = false;

    /*
     * Playback modes
     */

    private boolean shuffleEnabled = false;

    private int repeatMode = REPEAT_OFF;

    private final Random random = new Random();

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        audioManager =
                (AudioManager) getSystemService(
                        AUDIO_SERVICE
                );

        try {

            mediaSession =
                    new MediaSessionCompat(
                            this,
                            "KanadeMusic"
                    );

            mediaSession.setCallback(
                    new MediaSessionCompat.Callback() {

                        @Override
                        public void onPlay() {

                            if (mediaPlayer == null) {

                                if (currentUri != null) {

                                    playSong(
                                            currentUri
                                    );
                                }

                                return;
                            }

                            if (!requestAudioFocus()) {
                                return;
                            }

                            try {

                                if (!mediaPlayer.isPlaying()) {

                                    mediaPlayer.start();

                                    startPositionUpdates();

                                    updateMediaSessionState(
                                            true
                                    );

                                    updateNotification();

                                    sendPlaybackState(
                                            true
                                    );
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
                        public void onSeekTo(
                                long position) {

                            seekTo(
                                    (int) position
                            );
                        }

                        @Override
                        public void onStop() {

                            stopPlayback();
                        }
                    }
            );

            /*
             * Do not use the deprecated
             * FLAG_HANDLES_MEDIA_BUTTONS or
             * FLAG_HANDLES_TRANSPORT_CONTROLS.
             */

            mediaSession.setActive(
                    false
            );

            updateMediaSessionState(
                    false
            );

        } catch (Exception e) {

            e.printStackTrace();

            mediaSession = null;
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent == null) {

            return START_NOT_STICKY;
        }

        String action =
                intent.getAction();

        /*
         * IMPORTANT:
         *
         * MediaButtonReceiver.handleIntent()
         * does NOT return boolean in this version.
         *
         * Therefore we call it directly.
         */

        try {

            if (mediaSession != null) {

                MediaButtonReceiver.handleIntent(
                        mediaSession,
                        intent
                );
            }

        } catch (Exception ignored) {
        }

        if (ACTION_PLAY.equals(action)) {

            String uri =
                    intent.getStringExtra(
                            EXTRA_SONG_URI
                    );

            if (uri != null &&
                    !uri.trim().isEmpty()) {

                playSong(
                        uri
                );

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

            int position =
                    intent.getIntExtra(
                            EXTRA_SEEK_POSITION,
                            0
                    );

            seekTo(
                    position
            );

        } else if (ACTION_SET_SHUFFLE.equals(action)) {

            boolean enabled =
                    intent.getBooleanExtra(
                            EXTRA_SHUFFLE_ENABLED,
                            false
                    );

            setShuffle(
                    enabled
            );

        } else if (ACTION_TOGGLE_SHUFFLE.equals(action)) {

            setShuffle(
                    !shuffleEnabled
            );

        } else if (ACTION_SET_REPEAT.equals(action)) {

            int mode =
                    intent.getIntExtra(
                            EXTRA_REPEAT_MODE,
                            REPEAT_OFF
                    );

            setRepeatMode(
                    mode
            );

        } else if (ACTION_TOGGLE_REPEAT.equals(action)) {

            toggleRepeatMode();

        } else if (ACTION_ADD_TO_QUEUE.equals(action)) {

            String uri =
                    intent.getStringExtra(
                            EXTRA_SONG_URI
                    );

            addToQueue(
                    uri
            );

        } else if (ACTION_CLEAR_QUEUE.equals(action)) {

            clearQueue();

        } else if (ACTION_PLAY_QUEUE_ITEM.equals(action)) {

            int index =
                    intent.getIntExtra(
                            EXTRA_QUEUE_INDEX,
                            -1
                    );

            playQueueItem(
                    index
            );
        }

        return START_NOT_STICKY;
    }

    /*
     * ---------------------------------------------------------
     * PLAYBACK
     * ---------------------------------------------------------
     */

    private void playSong(
            String uri) {

        try {

            if (uri == null ||
                    uri.trim().isEmpty()) {

                return;
            }

            if (!requestAudioFocus()) {

                return;
            }

            /*
             * Only build the normal MediaStore queue when
             * the queue has not already been manually created.
             */

            if (queue.isEmpty()) {

                loadQueue();
            }

            int foundIndex =
                    findSongIndex(
                            uri
                    );

            if (foundIndex >= 0) {

                currentIndex =
                        foundIndex;

            } else {

                /*
                 * The requested song is not currently in
                 * the queue. Add it if possible.
                 */

                AudioFile song =
                        findSongByUri(
                                uri
                        );

                if (song != null) {

                    queue.add(
                            song
                    );

                    currentIndex =
                            queue.size() - 1;

                } else {

                    currentIndex = -1;
                }
            }

            currentUri =
                    uri;

            updateCurrentAlbumColor();

            releasePlayer();

            mediaPlayer =
                    MediaPlayer.create(
                            this,
                            Uri.parse(uri)
                    );

            if (mediaPlayer == null) {

                currentUri = null;
                currentIndex = -1;

                abandonAudioFocus();

                updateMediaSessionState(
                        false
                );

                sendPlaybackState(
                        false
                );

                return;
            }

            try {

                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_MEDIA
                                )
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_MUSIC
                                )
                                .build()
                );

            } catch (Exception ignored) {
            }

            mediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(
                                MediaPlayer mp) {

                            handleCompletion();
                        }
                    }
            );

            mediaPlayer.setOnErrorListener(
                    new MediaPlayer.OnErrorListener() {

                        @Override
                        public boolean onError(
                                MediaPlayer mp,
                                int what,
                                int extra) {

                            releasePlayer();

                            updateMediaSessionState(
                                    false
                            );

                            updateNotification();

                            sendPlaybackState(
                                    false
                            );

                            return true;
                        }
                    }
            );

            mediaPlayer.start();

            startPlaybackForeground();

            updateMediaMetadata();

            updateMediaSessionState(
                    true
            );

            updateNotification();

            sendPlaybackState(
                    true
            );

            sendQueueChanged();

            startPositionUpdates();

        } catch (Exception e) {

            e.printStackTrace();

            releasePlayer();

            currentUri = null;
            currentIndex = -1;

            abandonAudioFocus();

            updateMediaSessionState(
                    false
            );

            sendPlaybackState(
                    false
            );
        }
    }

    private void togglePlayPause() {

        if (mediaPlayer == null) {

            if (currentUri != null) {

                playSong(
                        currentUri
                );
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

                mediaPlayer.start();

                startPositionUpdates();

                updateMediaSessionState(
                        true
                );

                updateNotification();

                sendPlaybackState(
                        true
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void pauseSong() {

        if (mediaPlayer == null) {

            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
            }

            isUpdatingPosition =
                    false;

            updateMediaSessionState(
                    false
            );

            updateNotification();

            sendPlaybackState(
                    false
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
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

        /*
         * Repeat ONE:
         * replay the exact same song.
         */

        if (repeatMode == REPEAT_ONE &&
                currentIndex >= 0 &&
                currentIndex < queue.size()) {

            AudioFile currentSong =
                    queue.get(
                            currentIndex
                    );

            if (currentSong != null &&
                    currentSong.getUri() != null) {

                playSong(
                        currentSong.getUri()
                );

            }

            return;
        }

        int nextIndex;

        if (shuffleEnabled) {

            nextIndex =
                    getRandomDifferentIndex();

        } else {

            if (currentIndex < 0) {

                if (currentUri != null) {

                    currentIndex =
                            findSongIndex(
                                    currentUri
                            );
                }

                if (currentIndex < 0) {

                    currentIndex = 0;
                }
            }

            nextIndex =
                    currentIndex + 1;

            if (nextIndex >= queue.size()) {

                if (repeatMode == REPEAT_ALL) {

                    nextIndex = 0;

                } else {

                    /*
                     * Repeat OFF:
                     * stay at the final track and pause.
                     */

                    currentIndex =
                            queue.size() - 1;

                    if (mediaPlayer != null) {

                        try {

                            mediaPlayer.seekTo(
                                    0
                            );

                        } catch (Exception ignored) {
                        }
                    }

                    pauseSong();

                    return;
                }
            }
        }

        currentIndex =
                nextIndex;

        AudioFile nextSong =
                queue.get(
                        currentIndex
                );

        if (nextSong == null ||
                nextSong.getUri() == null) {

            return;
        }

        playSong(
                nextSong.getUri()
        );
    }

    private void playPrevious() {

        loadQueueIfNeeded();

        if (queue.isEmpty()) {

            return;
        }

        /*
         * If the current song has been playing for more
         * than 3 seconds, previous should restart it.
         */

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.getCurrentPosition() > 3000) {

                    mediaPlayer.seekTo(
                            0
                    );

                    return;
                }

            } catch (Exception ignored) {
            }
        }

        if (currentIndex < 0) {

            if (currentUri != null) {

                currentIndex =
                        findSongIndex(
                                currentUri
                        );
            }

            if (currentIndex < 0) {

                currentIndex = 0;
            }
        }

        int previousIndex;

        if (shuffleEnabled) {

            previousIndex =
                    getRandomDifferentIndex();

        } else {

            previousIndex =
                    currentIndex - 1;

            if (previousIndex < 0) {

                if (repeatMode == REPEAT_ALL) {

                    previousIndex =
                            queue.size() - 1;

                } else {

                    previousIndex = 0;
                }
            }
        }

        currentIndex =
                previousIndex;

        AudioFile previousSong =
                queue.get(
                        currentIndex
                );

        if (previousSong == null ||
                previousSong.getUri() == null) {

            return;
        }

        playSong(
                previousSong.getUri()
        );
    }

    private void handleCompletion() {

        if (repeatMode == REPEAT_ONE) {

            if (currentUri != null) {

                playSong(
                        currentUri
                );
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

    private void setShuffle(
            boolean enabled) {

        shuffleEnabled =
                enabled;

        sendShuffleState();

        sendQueueChanged();
    }

    private int getRandomDifferentIndex() {

        if (queue.size() <= 1) {

            return 0;
        }

        int index;

        do {

            index =
                    random.nextInt(
                            queue.size()
                    );

        } while (
                index == currentIndex
        );

        return index;
    }

    private void sendShuffleState() {

        Intent intent =
                new Intent(
                        ACTION_STATE_CHANGED
                );

        intent.setPackage(
                getPackageName()
        );

        intent.putExtra(
                EXTRA_SHUFFLE_STATE,
                shuffleEnabled
        );

        intent.putExtra(
                EXTRA_REPEAT_STATE,
                repeatMode
        );

        sendBroadcast(
                intent
        );
    }

    /*
     * ---------------------------------------------------------
     * REPEAT
     * ---------------------------------------------------------
     */

    private void setRepeatMode(
            int mode) {

        if (mode < REPEAT_OFF ||
                mode > REPEAT_ONE) {

            mode =
                    REPEAT_OFF;
        }

        repeatMode =
                mode;

        sendShuffleState();
    }

    private void toggleRepeatMode() {

        if (repeatMode == REPEAT_OFF) {

            repeatMode =
                    REPEAT_ALL;

        } else if (repeatMode == REPEAT_ALL) {

            repeatMode =
                    REPEAT_ONE;

        } else {

            repeatMode =
                    REPEAT_OFF;
        }

        sendShuffleState();
    }

    /*
     * ---------------------------------------------------------
     * QUEUE
     * ---------------------------------------------------------
     */

    private void addToQueue(
            String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        AudioFile song =
                findSongByUri(
                        uri
                );

        if (song == null) {

            return;
        }

        /*
         * Do not duplicate the exact same URI.
         */

        if (findSongIndex(uri) >= 0) {

            return;
        }

        queue.add(
                song
        );

        if (currentIndex < 0) {

            currentIndex = 0;
        }

        sendQueueChanged();
    }

    private void clearQueue() {

        AudioFile currentSong = null;

        if (currentIndex >= 0 &&
                currentIndex < queue.size()) {

            currentSong =
                    queue.get(
                            currentIndex
                    );
        }

        queue.clear();

        if (currentSong != null) {

            queue.add(
                    currentSong
            );

            currentIndex = 0;

        } else {

            currentIndex = -1;
        }

        sendQueueChanged();
    }

    private void playQueueItem(
            int index) {

        if (index < 0 ||
                index >= queue.size()) {

            return;
        }

        AudioFile song =
                queue.get(
                        index
                );

        if (song == null ||
                song.getUri() == null) {

            return;
        }

        currentIndex =
                index;

        playSong(
                song.getUri()
        );
    }

    private void sendQueueChanged() {

        Intent intent =
                new Intent(
                        ACTION_QUEUE_CHANGED
                );

        intent.setPackage(
                getPackageName()
        );

        intent.putExtra(
                EXTRA_QUEUE_SIZE,
                queue.size()
        );

        intent.putExtra(
                EXTRA_QUEUE_INDEX,
                currentIndex
        );

        sendBroadcast(
                intent
        );
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

            MusicRepository repository =
                    new MusicRepository(
                            getApplicationContext()
                    );

            ArrayList<AudioFile> result =
                    repository.getAllSongs();

            if (result != null) {

                queue.clear();

                queue.addAll(
                        result
                );

                if (currentUri != null) {

                    int index =
                            findSongIndex(
                                    currentUri
                            );

                    if (index >= 0) {

                        currentIndex =
                                index;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        sendQueueChanged();
    }

    private AudioFile findSongByUri(
            String uri) {

        if (uri == null) {

            return null;
        }

        /*
         * First search the existing queue.
         */

        for (AudioFile song : queue) {

            if (song != null &&
                    uri.equals(
                            song.getUri()
                    )) {

                return song;
            }
        }

        /*
         * Then search MusicRepository.
         */

        try {

            MusicRepository repository =
                    new MusicRepository(
                            getApplicationContext()
                    );

            ArrayList<AudioFile> songs =
                    repository.getAllSongs();

            if (songs != null) {

                for (AudioFile song : songs) {

                    if (song != null &&
                            uri.equals(
                                    song.getUri()
                            )) {

                        return song;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    private int findSongIndex(
            String uri) {

        if (uri == null) {

            return -1;
        }

        for (int i = 0;
                i < queue.size();
                i++) {

            AudioFile song =
                    queue.get(
                            i
                    );

            if (song != null &&
                    uri.equals(
                            song.getUri()
                    )) {

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

    private void seekTo(
            int position) {

        if (mediaPlayer == null) {

            return;
        }

        try {

            int duration =
                    mediaPlayer.getDuration();

            if (duration <= 0) {

                return;
            }

            if (position < 0) {

                position = 0;
            }

            if (position > duration) {

                position = duration;
            }

            mediaPlayer.seekTo(
                    position
            );

            boolean playing =
                    isPlayerPlaying();

            updateMediaSessionState(
                    playing
            );

            sendPlaybackState(
                    playing
            );

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

        if (isUpdatingPosition) {

            return;
        }

        isUpdatingPosition =
                true;

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        while (isUpdatingPosition) {

                            try {

                                if (mediaPlayer == null) {

                                    break;
                                }

                                if (!mediaPlayer.isPlaying()) {

                                    break;
                                }

                                int position =
                                        mediaPlayer
                                                .getCurrentPosition();

                                int duration =
                                        mediaPlayer
                                                .getDuration();

                                sendPlaybackState(
                                        true,
                                        position,
                                        duration
                                );

                                updateMediaSessionPosition(
                                        position,
                                        duration
                                );

                                Thread.sleep(
                                        500
                                );

                            } catch (Exception e) {

                                break;
                            }
                        }

                        isUpdatingPosition =
                                false;
                    }
                }
        ).start();
    }

    /*
     * ---------------------------------------------------------
     * AUDIO FOCUS
     * ---------------------------------------------------------
     */

    private boolean requestAudioFocus() {

        if (audioManager == null) {

            return true;
        }

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                if (audioFocusRequest == null) {

                    audioFocusRequest =
                            new AudioFocusRequest.Builder(
                                    AudioManager.AUDIOFOCUS_GAIN
                            )
                            .setAudioAttributes(
                                    new android.media.AudioAttributes.Builder()
                                            .setUsage(
                                                    android.media.AudioAttributes.USAGE_MEDIA
                                            )
                                            .setContentType(
                                                    android.media.AudioAttributes.CONTENT_TYPE_MUSIC
                                            )
                                            .build()
                            )
                            .setOnAudioFocusChangeListener(
                                    focusChange -> {

                                        if (focusChange ==
                                                AudioManager.AUDIOFOCUS_LOSS) {

                                            pauseSong();

                                        } else if (
                                                focusChange ==
                                                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

                                            pauseSong();

                                        } else if (
                                                focusChange ==
                                                        AudioManager.AUDIOFOCUS_GAIN) {

                                            if (mediaPlayer != null &&
                                                    !mediaPlayer.isPlaying() &&
                                                    currentUri != null) {

                                                try {

                                                    mediaPlayer.start();

                                                    startPositionUpdates();

                                                    updateMediaSessionState(
                                                            true
                                                    );

                                                    updateNotification();

                                                    sendPlaybackState(
                                                            true
                                                    );

                                                } catch (Exception ignored) {
                                                }
                                            }
                                        }
                                    }
                            )
                            .build();
                }

                int result =
                        audioManager.requestAudioFocus(
                                audioFocusRequest
                        );

                hasAudioFocus =
                        result ==
                                AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

            } else {

                int result =
                        audioManager.requestAudioFocus(
                                focusChange -> {

                                    if (focusChange ==
                                            AudioManager.AUDIOFOCUS_LOSS ||
                                            focusChange ==
                                                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

                                        pauseSong();
                                    }
                                },
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN
                        );

                hasAudioFocus =
                        result ==
                                AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            }

        } catch (Exception e) {

            e.printStackTrace();

            hasAudioFocus = true;
        }

        return hasAudioFocus;
    }

    private void abandonAudioFocus() {

        if (audioManager == null) {

            return;
        }

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                if (audioFocusRequest != null) {

                    audioManager.abandonAudioFocusRequest(
                            audioFocusRequest
                    );
                }

            } else {

                audioManager.abandonAudioFocus(
                        null
                );
            }

        } catch (Exception ignored) {
        }

        hasAudioFocus =
                false;
    }

    /*
     * ---------------------------------------------------------
     * MEDIA SESSION
     * ---------------------------------------------------------
     */

    private void updateMediaSessionState(
            boolean playing) {

        if (mediaSession == null) {

            return;
        }

        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_STOP;

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

            state =
                    PlaybackStateCompat.STATE_NONE;

        } else if (playing) {

            state =
                    PlaybackStateCompat.STATE_PLAYING;

        } else {

            state =
                    PlaybackStateCompat.STATE_PAUSED;
        }

        long position =
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;

        if (mediaPlayer != null) {

            try {

                position =
                        mediaPlayer.getCurrentPosition();

            } catch (Exception ignored) {
            }
        }

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(
                                actions
                        )
                        .setState(
                                state,
                                position,
                                playing ? 1f : 0f
                        )
                        .build();

        try {

            mediaSession.setPlaybackState(
                    playbackState
            );

            mediaSession.setActive(
                    currentUri != null
            );

        } catch (Exception ignored) {
        }
    }

    private void updateMediaSessionPosition(
            int position,
            int duration) {

        if (mediaSession == null) {

            return;
        }

        boolean playing =
                isPlayerPlaying();

        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_STOP;

        int state =
                playing
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(
                                actions
                        )
                        .setState(
                                state,
                                position,
                                playing ? 1f : 0f
                        )
                        .build();

        try {

            mediaSession.setPlaybackState(
                    playbackState
            );

        } catch (Exception ignored) {
        }
    }

    private void updateMediaMetadata() {

        if (mediaSession == null) {

            return;
        }

        if (currentIndex < 0 ||
                currentIndex >= queue.size()) {

            return;
        }

        AudioFile song =
                queue.get(
                        currentIndex
                );

        if (song == null) {

            return;
        }

        MediaMetadataCompat.Builder builder =
                new MediaMetadataCompat.Builder();

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                safeText(
                        song.getTitle(),
                        "Unknown title"
                )
        );

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                safeText(
                        song.getArtist(),
                        "Unknown artist"
                )
        );

        builder.putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                safeText(
                        song.getAlbum(),
                        "Unknown album"
                )
        );

        if (mediaPlayer != null) {

            try {

                builder.putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer.getDuration()
                );

            } catch (Exception ignored) {
            }
        }

        Bitmap albumArt =
                loadAlbumArt(
                        song
                );

        if (albumArt != null) {

            builder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    albumArt
            );

            builder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ART,
                    albumArt
            );
        }

        try {

            mediaSession.setMetadata(
                    builder.build()
            );

        } catch (Exception ignored) {
        }
    }

    /*
     * ---------------------------------------------------------
     * ALBUM COLOR
     * ---------------------------------------------------------
     */

    private void updateCurrentAlbumColor() {

        try {

            if (currentIndex >= 0 &&
                    currentIndex < queue.size()) {

                AudioFile currentSong =
                        queue.get(
                                currentIndex
                        );

                if (currentSong != null) {

                    AlbumColorManager
                            .getInstance(
                                    getApplicationContext()
                            )
                            .setCurrentSong(
                                    currentSong
                            );
                }
            }

        } catch (Exception ignored) {
        }
    }

    /*
     * ---------------------------------------------------------
     * ALBUM ART
     * ---------------------------------------------------------
     */

    private Bitmap loadAlbumArt(
            AudioFile song) {

        if (song == null) {

            return null;
        }

        String path =
                song.getPath();

        if (path == null ||
                path.trim().isEmpty()) {

            return null;
        }

        android.media.MediaMetadataRetriever retriever =
                new android.media.MediaMetadataRetriever();

        try {

            retriever.setDataSource(
                    path
            );

            byte[] artwork =
                    retriever.getEmbeddedPicture();

            if (artwork != null &&
                    artwork.length > 0) {

                return BitmapFactory.decodeByteArray(
                        artwork,
                        0,
                        artwork.length
                );
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

    private String safeText(
            String value,
            String fallback) {

        if (value == null ||
                value.trim().isEmpty()) {

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

            Notification notification =
                    buildNotification();

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private Notification buildNotification() {

        String title =
                "Kanade Music";

        String artist =
                "Not playing";

        Bitmap artwork =
                null;

        if (currentIndex >= 0 &&
                currentIndex < queue.size()) {

            AudioFile song =
                    queue.get(
                            currentIndex
                    );

            if (song != null) {

                title =
                        safeText(
                                song.getTitle(),
                                "Unknown title"
                        );

                artist =
                        safeText(
                                song.getArtist(),
                                "Unknown artist"
                        );

                artwork =
                        loadAlbumArt(
                                song
                        );
            }
        }

        Intent contentIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        contentIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent contentPendingIntent =
                PendingIntent.getActivity(
                        this,
                        500,
                        contentIntent,
                        pendingIntentFlags()
                );

        PendingIntent previousPendingIntent =
                createServicePendingIntent(
                        ACTION_PREVIOUS,
                        501
                );

        boolean playing =
                isPlayerPlaying();

        PendingIntent playPausePendingIntent;

        if (playing) {

            playPausePendingIntent =
                    createServicePendingIntent(
                            ACTION_PAUSE,
                            502
                    );

        } else {

            playPausePendingIntent =
                    createServicePendingIntent(
                            ACTION_PLAY,
                            502
                    );
        }

        PendingIntent nextPendingIntent =
                createServicePendingIntent(
                        ACTION_NEXT,
                        503
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                .setSmallIcon(
                        android.R.drawable.ic_media_play
                )
                .setContentTitle(
                        title
                )
                .setContentText(
                        artist
                )
                .setCategory(
                        NotificationCompat.CATEGORY_TRANSPORT
                )
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                )
                .setOnlyAlertOnce(
                        true
                )
                .setOngoing(
                        playing
                )
                .setContentIntent(
                        contentPendingIntent
                )
                .addAction(
                        new NotificationCompat.Action(
                                android.R.drawable.ic_media_previous,
                                "Previous",
                                previousPendingIntent
                        )
                )
                .addAction(
                        new NotificationCompat.Action(
                                playing
                                        ? android.R.drawable.ic_media_pause
                                        : android.R.drawable.ic_media_play,
                                playing
                                        ? "Pause"
                                        : "Play",
                                playPausePendingIntent
                        )
                )
                .addAction(
                        new NotificationCompat.Action(
                                android.R.drawable.ic_media_next,
                                "Next",
                                nextPendingIntent
                        )
                );

        if (mediaSession != null) {

            try {

                builder.setStyle(
                        new MediaStyle()
                                .setMediaSession(
                                        mediaSession.getSessionToken()
                                )
                                .setShowActionsInCompactView(
                                        0,
                                        1,
                                        2
                                )
                );

            } catch (Exception ignored) {
            }
        }

        if (artwork != null) {

            builder.setLargeIcon(
                    artwork
            );
        }

        return builder.build();
    }

    private void updateNotification() {

        if (currentUri == null) {

            return;
        }

        try {

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            if (manager != null) {

                manager.notify(
                        NOTIFICATION_ID,
                        buildNotification()
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private PendingIntent createServicePendingIntent(
            String action,
            int requestCode) {

        Intent intent =
                new Intent(
                        this,
                        MusicPlayerService.class
                );

        intent.setAction(
                action
        );

        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                pendingIntentFlags()
        );
    }

    private int pendingIntentFlags() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            return PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    /*
     * ---------------------------------------------------------
     * PLAYBACK STATE
     * ---------------------------------------------------------
     */

    private void sendPlaybackState(
            boolean isPlaying) {

        int position = 0;
        int duration = 0;

        if (mediaPlayer != null) {

            try {

                position =
                        mediaPlayer.getCurrentPosition();

                duration =
                        mediaPlayer.getDuration();

            } catch (Exception ignored) {
            }
        }

        sendPlaybackState(
                isPlaying,
                position,
                duration
        );
    }

    private void sendPlaybackState(
            boolean isPlaying,
            int position,
            int duration) {

        Intent intent =
                new Intent(
                        ACTION_STATE_CHANGED
                );

        intent.setPackage(
                getPackageName()
        );

        intent.putExtra(
                EXTRA_IS_PLAYING,
                isPlaying
        );

        intent.putExtra(
                EXTRA_CURRENT_URI,
                currentUri
        );

        intent.putExtra(
                EXTRA_POSITION,
                position
        );

        intent.putExtra(
                EXTRA_DURATION,
                duration
        );

        intent.putExtra(
                EXTRA_SHUFFLE_STATE,
                shuffleEnabled
        );

        intent.putExtra(
                EXTRA_REPEAT_STATE,
                repeatMode
        );

        sendBroadcast(
                intent
        );
    }

    /*
     * ---------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------
     */

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

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Music Playback",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Kanade music playback controls"
        );

        channel.setShowBadge(
                false
        );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * STOP / RELEASE
     * ---------------------------------------------------------
     */

    private void stopPlayback() {

        isUpdatingPosition =
                false;

        releasePlayer();

        abandonAudioFocus();

        currentUri = null;

        currentIndex = -1;

        if (mediaSession != null) {

            try {

                PlaybackStateCompat playbackState =
                        new PlaybackStateCompat.Builder()
                                .setActions(
                                        PlaybackStateCompat.ACTION_PLAY |
                                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                )
                                .setState(
                                        PlaybackStateCompat.STATE_NONE,
                                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                        0f
                                )
                                .build();

                mediaSession.setPlaybackState(
                        playbackState
                );

                mediaSession.setActive(
                        false
                );

            } catch (Exception ignored) {
            }
        }

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.N) {

                stopForeground(
                        STOP_FOREGROUND_REMOVE
                );

            } else {

                stopForeground(
                        true
                );
            }

        } catch (Exception ignored) {
        }

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.cancel(
                    NOTIFICATION_ID
            );
        }

        sendPlaybackState(
                false
        );

        sendQueueChanged();
    }

    private void releasePlayer() {

        isUpdatingPosition =
                false;

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

        isUpdatingPosition =
                false;

        abandonAudioFocus();

        sendPlaybackState(
                false
        );

        releasePlayer();

        if (mediaSession != null) {

            try {

                mediaSession.setActive(
                        false
                );

                mediaSession.release();

            } catch (Exception ignored) {
            }

            mediaSession = null;
        }

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.N) {

                stopForeground(
                        STOP_FOREGROUND_REMOVE
                );

            } else {

                stopForeground(
                        true
                );
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}