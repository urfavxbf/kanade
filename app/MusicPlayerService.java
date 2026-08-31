package com.urfavxbf.kanade;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;

import java.util.ArrayList;

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

    private MediaPlayer mediaPlayer;

    private String currentUri;

    private ArrayList<AudioFile> queue =
            new ArrayList<>();

    private int currentIndex = -1;

    private boolean isUpdatingPosition = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_PLAY.equals(action)) {

            String uri =
                    intent.getStringExtra(
                            EXTRA_SONG_URI
                    );

            if (uri != null &&
                    !uri.trim().isEmpty()) {

                playSong(uri);

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

            seekTo(position);
        }

        return START_NOT_STICKY;
    }

    private void playSong(String uri) {

        try {

            if (uri == null ||
                    uri.trim().isEmpty()) {
                return;
            }

            /*
             * Make sure the queue exists.
             */
            loadQueue();

            /*
             * Find the selected song in the queue.
             */
            currentIndex =
                    findSongIndex(uri);

            currentUri = uri;

            /*
             * Update centralized album colors.
             *
             * The service is the source of truth for the
             * currently playing song. AlbumColorManager
             * handles extraction, caching and broadcasting.
             */
            try {

                if (currentIndex >= 0 &&
                        currentIndex < queue.size()) {

                    AudioFile currentSong =
                            queue.get(currentIndex);

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

            releasePlayer();

            mediaPlayer =
                    MediaPlayer.create(
                            this,
                            Uri.parse(uri)
                    );

            if (mediaPlayer == null) {

                currentUri = null;
                currentIndex = -1;

                sendPlaybackState(false);

                return;
            }

            mediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(
                                MediaPlayer mp) {

                            /*
                             * Automatically continue to
                             * the next song.
                             */
                            playNext();
                        }
                    }
            );

            mediaPlayer.start();

            sendPlaybackState(true);

            startPositionUpdates();

        } catch (Exception e) {

            e.printStackTrace();

            releasePlayer();

            currentUri = null;
            currentIndex = -1;

            sendPlaybackState(false);
        }
    }

    private void togglePlayPause() {

        if (mediaPlayer == null) {
            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();

                sendPlaybackState(false);

            } else {

                mediaPlayer.start();

                sendPlaybackState(true);

                startPositionUpdates();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void pauseSong() {

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.isPlaying()) {

                    mediaPlayer.pause();
                }

                sendPlaybackState(false);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    private void playNext() {

        loadQueue();

        if (queue.isEmpty()) {
            return;
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

        } else {

            currentIndex++;

            if (currentIndex >= queue.size()) {
                currentIndex = 0;
            }
        }

        AudioFile nextSong =
                queue.get(currentIndex);

        if (nextSong == null ||
                nextSong.getUri() == null) {
            return;
        }

        playSong(
                nextSong.getUri()
        );
    }

    private void playPrevious() {

        loadQueue();

        if (queue.isEmpty()) {
            return;
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

        } else {

            currentIndex--;

            if (currentIndex < 0) {

                currentIndex =
                        queue.size() - 1;
            }
        }

        AudioFile previousSong =
                queue.get(currentIndex);

        if (previousSong == null ||
                previousSong.getUri() == null) {
            return;
        }

        playSong(
                previousSong.getUri()
        );
    }

    private void seekTo(int position) {

        if (mediaPlayer == null) {
            return;
        }

        try {

            if (!mediaPlayer.isPlaying() &&
                    !mediaPlayer.isLooping()) {

                /*
                 * MediaPlayer can still seek while paused,
                 * so we intentionally allow it.
                 */
            }

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

            mediaPlayer.seekTo(position);

            sendPlaybackState(
                    mediaPlayer.isPlaying()
            );

        } catch (Exception e) {

            e.printStackTrace();
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

                queue.addAll(result);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private int findSongIndex(String uri) {

        if (uri == null) {
            return -1;
        }

        for (int i = 0;
                i < queue.size();
                i++) {

            AudioFile song =
                    queue.get(i);

            if (song != null &&
                    uri.equals(song.getUri())) {

                return i;
            }
        }

        return -1;
    }

    private void startPositionUpdates() {

        if (isUpdatingPosition) {
            return;
        }

        isUpdatingPosition = true;

        new Thread(new Runnable() {
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
                                mediaPlayer.getCurrentPosition();

                        int duration =
                                mediaPlayer.getDuration();

                        sendPlaybackState(
                                true,
                                position,
                                duration
                        );

                        Thread.sleep(500);

                    } catch (Exception e) {

                        break;
                    }
                }

                isUpdatingPosition = false;
            }
        }).start();
    }

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

        sendBroadcast(intent);
    }

    private void releasePlayer() {

        isUpdatingPosition = false;

        if (mediaPlayer != null) {

            try {

                mediaPlayer.stop();

            } catch (Exception ignored) {
            }

            try {

                mediaPlayer.release();

            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {

        isUpdatingPosition = false;

        sendPlaybackState(false);

        releasePlayer();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}