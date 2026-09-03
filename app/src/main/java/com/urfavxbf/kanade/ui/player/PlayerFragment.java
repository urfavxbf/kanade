package com.urfavxbf.kanade.ui.player;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;

public class PlayerFragment extends Fragment {

    private ImageButton btnFullPlayerBack;
    private ImageButton btnFullPrevious;
    private ImageButton btnFullPlayPause;
    private ImageButton btnFullNext;
    private ImageButton playerMore;

    private ImageView fullPlayerAlbumArt;

    private TextView fullPlayerTitle;
    private TextView fullPlayerArtist;
    private TextView fullPlayerElapsed;
    private TextView fullPlayerDuration;

    private EqualizerSeekBar seekFullPlayer;

    private CutoutRippleView cutoutRippleView;

    private LinearLayout lyricsContainer;
    private TextView lyricsPrevious;
    private TextView lyricsCurrent;
    private TextView lyricsNext;

    private View playerVisualContainer;
    private View fullPlayerAlbumCard;
    private View songInfoContainer;

    private ImageButton btnEqualizer;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnQueue;

    private boolean receiverRegistered = false;
    private boolean colorReceiverRegistered = false;
    private boolean audioAnalysisReceiverRegistered = false;

    private boolean userIsSeeking = false;
    private boolean isPlaying = false;
    private boolean lyricsMode = false;

    private String currentUri = null;

    private int currentPosition = 0;
    private int currentDuration = 0;

    private boolean shuffleEnabled = false;

    private int repeatMode = MusicPlayerService.REPEAT_OFF;

    private int currentAccentColor = Color.rgb(201, 196, 255);

    private int currentBackgroundColor = Color.rgb(16, 17, 26);

    private ValueAnimator backgroundAnimator;

    private final BroadcastReceiver playerReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    String action = intent.getAction();

                    if (!MusicPlayerService.ACTION_STATE_CHANGED.equals(action)) {

                        return;
                    }

                    handlePlayerState(intent);
                }
            };

    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED.equals(intent.getAction())) {

                        return;
                    }

                    String uri =
                            intent.getStringExtra(
                                    AlbumColorManager.EXTRA_CURRENT_URI);

                    if (uri == null || uri.trim().isEmpty()) {

                        return;
                    }

                    /*
                     * IMPORTANT:
                     *
                     * Ignore color updates belonging to a song
                     * that is no longer the actual player song.
                     */
                    if (currentUri == null || !uri.equals(currentUri)) {

                        return;
                    }

                    int accent =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                                    currentAccentColor);

                    int background =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                                    currentBackgroundColor);

                    applyPlayerColors(accent, background);
                }
            };

    private final BroadcastReceiver audioAnalysisReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!MusicPlayerService.ACTION_AUDIO_ANALYSIS.equals(
                            intent.getAction())) {

                        return;
                    }

                    byte[] fft =
                            intent.getByteArrayExtra(
                                    MusicPlayerService.EXTRA_FFT);

                    float bass =
                            intent.getFloatExtra(
                                    MusicPlayerService.EXTRA_BASS,
                                    0f);

                    float energy =
                            intent.getFloatExtra(
                                    MusicPlayerService.EXTRA_ENERGY,
                                    0f);

                    boolean beat =
                            intent.getBooleanExtra(
                                    MusicPlayerService.EXTRA_BEAT,
                                    false);

                    float beatIntensity =
                            intent.getFloatExtra(
                                    MusicPlayerService.EXTRA_BEAT_INTENSITY,
                                    0f);

                    int sampleRate =
                            intent.getIntExtra(
                                    MusicPlayerService.EXTRA_SAMPLE_RATE,
                                    44100);

                    /*
                     * Send real FFT data to the equalizer.
                     */
                    if (seekFullPlayer != null) {

                        if (fft != null && fft.length > 1) {

                            seekFullPlayer.setFFTData(
                                    fft,
                                    sampleRate);

                        } else {

                            seekFullPlayer.clearFFTData();
                        }

                        seekFullPlayer.setAudioLevel(
                                energy);

                        seekFullPlayer.setBassLevel(
                                bass);

                        seekFullPlayer.setBeatDetected(
                                beat);
                    }

                    /*
                     * Send the exact same audio analysis
                     * to the punch-hole visualizer.
                     */
                    if (cutoutRippleView != null) {

                        cutoutRippleView.setAudioData(
                                energy,
                                bass);

                        cutoutRippleView.setBeatDetected(
                                beat,
                                beatIntensity);
                    }
                }
            };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.player,
                container,
                false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(
                view,
                savedInstanceState);

        initializeViews(view);

        setupControls();

        setupLyricsMode();

        setupSeekBar();

        registerPlayerReceiver();

        registerColorReceiver();

        registerAudioAnalysisReceiver();

        loadExistingColors();

        requestCurrentPlayerState();
    }

    private void initializeViews(View view) {


        btnFullPrevious =
                view.findViewById(
                        R.id.btnFullPrevious);

        btnFullPlayPause =
                view.findViewById(
                        R.id.btnFullPlayPause);

        btnFullNext =
                view.findViewById(
                        R.id.btnFullNext);

        playerMore =
                view.findViewById(
                        R.id.playerMore);

        fullPlayerAlbumArt =
                view.findViewById(
                        R.id.fullPlayerAlbumArt);

        fullPlayerAlbumCard =
                view.findViewById(
                        R.id.fullPlayerAlbumCard);

        playerVisualContainer =
                view.findViewById(
                        R.id.playerVisualContainer);

        songInfoContainer =
                view.findViewById(
                        R.id.songInfoContainer);

        cutoutRippleView =
                view.findViewById(
                        R.id.cutoutRippleView);

        fullPlayerTitle =
                view.findViewById(
                        R.id.fullPlayerTitle);

        fullPlayerArtist =
                view.findViewById(
                        R.id.fullPlayerArtist);

        fullPlayerElapsed =
                view.findViewById(
                        R.id.fullPlayerElapsed);

        fullPlayerDuration =
                view.findViewById(
                        R.id.fullPlayerDuration);

        seekFullPlayer =
                view.findViewById(
                        R.id.seekFullPlayer);

        lyricsContainer =
                view.findViewById(
                        R.id.lyricsContainer);

        lyricsPrevious =
                view.findViewById(
                        R.id.lyricsPrevious);

        lyricsCurrent =
                view.findViewById(
                        R.id.lyricsCurrent);

        lyricsNext =
                view.findViewById(
                        R.id.lyricsNext);

        btnEqualizer =
                view.findViewById(
                        R.id.btnEqualizer);

        btnShuffle =
                view.findViewById(
                        R.id.btnShuffle);

        btnRepeat =
                view.findViewById(
                        R.id.btnRepeat);

        btnQueue =
                view.findViewById(
                        R.id.btnQueue);

        if (seekFullPlayer != null) {

            seekFullPlayer.setMax(1000);

            seekFullPlayer.setProgress(0);
        }

        if (cutoutRippleView != null) {

            cutoutRippleView.setAccentColor(
                    currentAccentColor);
        }

        if (fullPlayerElapsed != null) {

            fullPlayerElapsed.setText(
                    formatTime(0));
        }

        if (fullPlayerDuration != null) {

            fullPlayerDuration.setText(
                    formatTime(0));
        }

        updatePlaybackModeButtons();
    }

    private void setupControls() {

        if (btnFullPlayerBack != null) {

            btnFullPlayerBack.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            if (getActivity() != null) {

                                getActivity().onBackPressed();
                            }
                        }
                    });
        }

        if (btnFullPrevious != null) {

            btnFullPrevious.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            sendServiceAction(
                                    MusicPlayerService.ACTION_PREVIOUS);
                        }
                    });
        }

        if (btnFullNext != null) {

            btnFullNext.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            sendServiceAction(
                                    MusicPlayerService.ACTION_NEXT);
                        }
                    });
        }

        if (btnFullPlayPause != null) {

            btnFullPlayPause.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            if (isPlaying) {

                                sendServiceAction(
                                        MusicPlayerService.ACTION_PAUSE);

                            } else {

                                Intent intent =
                                        new Intent(
                                                requireContext(),
                                                MusicPlayerService.class);

                                intent.setAction(
                                        MusicPlayerService.ACTION_PLAY);

                                if (currentUri != null
                                        && !currentUri.trim().isEmpty()) {

                                    intent.putExtra(
                                            MusicPlayerService.EXTRA_SONG_URI,
                                            currentUri);
                                }

                                startMusicService(intent);
                            }
                        }
                    });
        }
    }

    private void setupLyricsMode() {

        if (fullPlayerAlbumCard != null) {

            fullPlayerAlbumCard.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            toggleLyricsMode();
                        }
                    });
        }

        if (btnShuffle != null) {

            btnShuffle.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            sendServiceAction(
                                    MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
                        }
                    });
        }

        if (btnRepeat != null) {

            btnRepeat.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            sendServiceAction(
                                    MusicPlayerService.ACTION_TOGGLE_REPEAT);
                        }
                    });
        }

        if (btnQueue != null) {

            btnQueue.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            Toast.makeText(
                                            requireContext(),
                                            "Queue",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }

        if (btnEqualizer != null) {

            btnEqualizer.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            Toast.makeText(
                                            requireContext(),
                                            "Equalizer settings",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
    }

    private void toggleLyricsMode() {

        if (lyricsMode) {

            exitLyricsMode();

        } else {

            enterLyricsMode();
        }
    }

    private void enterLyricsMode() {

        if (lyricsMode) {

            return;
        }

        lyricsMode = true;

        if (lyricsContainer != null) {

            lyricsContainer.setVisibility(
                    View.VISIBLE);

            lyricsContainer.setAlpha(0f);

            lyricsContainer.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start();
        }

        if (fullPlayerAlbumCard != null) {

            if (playerVisualContainer.getWidth() <= 0
                    || playerVisualContainer.getHeight() <= 0) {

                fullPlayerAlbumCard.post(
                        new Runnable() {

                            @Override
                            public void run() {

                                animateAlbumToLyricsMode();
                            }
                        });

            } else {

                animateAlbumToLyricsMode();
            }
        }

        updateLyricsPlaceholder();
    }

    private void animateAlbumToLyricsMode() {

        if (fullPlayerAlbumCard == null
                || playerVisualContainer == null) {

            return;
        }

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        float targetSize =
                76f * density;

        float currentWidth =
                fullPlayerAlbumCard.getWidth();

        float currentHeight =
                fullPlayerAlbumCard.getHeight();

        if (currentWidth <= 0) {

            currentWidth =
                    300f * density;
        }

        if (currentHeight <= 0) {

            currentHeight =
                    300f * density;
        }

        float scaleX =
                targetSize / currentWidth;

        float scaleY =
                targetSize / currentHeight;

        float targetX =
                -(playerVisualContainer.getWidth() / 2f)
                        + (targetSize / 2f)
                        + (8f * density);

        float targetY =
                (playerVisualContainer.getHeight() / 2f)
                        - (targetSize / 2f)
                        - (8f * density);

        AnimatorSet animator =
                new AnimatorSet();

        ObjectAnimator scaleXAnimator =
                ObjectAnimator.ofFloat(
                        fullPlayerAlbumCard,
                        View.SCALE_X,
                        fullPlayerAlbumCard.getScaleX(),
                        scaleX);

        ObjectAnimator scaleYAnimator =
                ObjectAnimator.ofFloat(
                        fullPlayerAlbumCard,
                        View.SCALE_Y,
                        fullPlayerAlbumCard.getScaleY(),
                        scaleY);

        ObjectAnimator xAnimator =
                ObjectAnimator.ofFloat(
                        fullPlayerAlbumCard,
                        View.TRANSLATION_X,
                        fullPlayerAlbumCard.getTranslationX(),
                        targetX);

        ObjectAnimator yAnimator =
                ObjectAnimator.ofFloat(
                        fullPlayerAlbumCard,
                        View.TRANSLATION_Y,
                        fullPlayerAlbumCard.getTranslationY(),
                        targetY);

        animator.playTogether(
                scaleXAnimator,
                scaleYAnimator,
                xAnimator,
                yAnimator);

        animator.setDuration(420);

        animator.setInterpolator(
                new android.view.animation.DecelerateInterpolator());

        animator.start();
    }

    private void exitLyricsMode() {

        if (!lyricsMode) {

            return;
        }

        lyricsMode = false;

        if (lyricsContainer != null) {

            lyricsContainer
                    .animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(
                            new Runnable() {

                                @Override
                                public void run() {

                                    if (!lyricsMode) {

                                        lyricsContainer.setVisibility(
                                                View.GONE);
                                    }
                                }
                            })
                    .start();
        }

        if (fullPlayerAlbumCard != null) {

            AnimatorSet animator =
                    new AnimatorSet();

            ObjectAnimator scaleXAnimator =
                    ObjectAnimator.ofFloat(
                            fullPlayerAlbumCard,
                            View.SCALE_X,
                            fullPlayerAlbumCard.getScaleX(),
                            1f);

            ObjectAnimator scaleYAnimator =
                    ObjectAnimator.ofFloat(
                            fullPlayerAlbumCard,
                            View.SCALE_Y,
                            fullPlayerAlbumCard.getScaleY(),
                            1f);

            ObjectAnimator xAnimator =
                    ObjectAnimator.ofFloat(
                            fullPlayerAlbumCard,
                            View.TRANSLATION_X,
                            fullPlayerAlbumCard.getTranslationX(),
                            0f);

            ObjectAnimator yAnimator =
                    ObjectAnimator.ofFloat(
                            fullPlayerAlbumCard,
                            View.TRANSLATION_Y,
                            fullPlayerAlbumCard.getTranslationY(),
                            0f);

            animator.playTogether(
                    scaleXAnimator,
                    scaleYAnimator,
                    xAnimator,
                    yAnimator);

            animator.setDuration(420);

            animator.setInterpolator(
                    new android.view.animation.DecelerateInterpolator());

            animator.start();
        }
    }

    private void updateLyricsPlaceholder() {

        if (lyricsPrevious != null) {

            lyricsPrevious.setText(
                    "Previous lyric");
        }

        if (lyricsCurrent != null) {

            lyricsCurrent.setText(
                    "Synchronized Lyrics");
        }

        if (lyricsNext != null) {

            lyricsNext.setText(
                    "Next lyric");
        }
    }

    private void setupSeekBar() {

        if (seekFullPlayer == null) {

            return;
        }

        seekFullPlayer.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            android.widget.SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (!fromUser) {

                            return;
                        }

                        if (currentDuration <= 0) {

                            return;
                        }

                        int position =
                                Math.round(
                                        (progress / 1000f)
                                                * currentDuration);

                        currentPosition =
                                position;

                        if (fullPlayerElapsed != null) {

                            fullPlayerElapsed.setText(
                                    formatTime(position));
                        }

                        updateLyrics(position);
                    }

                    @Override
                    public void onStartTrackingTouch(
                            android.widget.SeekBar seekBar) {

                        userIsSeeking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(
                            android.widget.SeekBar seekBar) {

                        if (currentDuration <= 0) {

                            userIsSeeking = false;

                            return;
                        }

                        int progress =
                                seekBar.getProgress();

                        int position =
                                Math.round(
                                        (progress / 1000f)
                                                * currentDuration);

                        currentPosition =
                                position;

                        sendSeekCommand(position);

                        updateLyrics(position);

                        userIsSeeking = false;
                    }
                });
    }

    private void handlePlayerState(Intent intent) {

        boolean playing =
                intent.getBooleanExtra(
                        MusicPlayerService.EXTRA_IS_PLAYING,
                        false);

        String uri =
                intent.getStringExtra(
                        MusicPlayerService.EXTRA_CURRENT_URI);

        int position =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_POSITION,
                        0);

        int duration =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_DURATION,
                        0);

        shuffleEnabled =
                intent.getBooleanExtra(
                        MusicPlayerService.EXTRA_SHUFFLE_STATE,
                        shuffleEnabled);

        repeatMode =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_REPEAT_STATE,
                        repeatMode);

        isPlaying =
                playing;

        if (uri != null
                && !uri.trim().isEmpty()) {

            boolean songChanged =
                    currentUri == null
                            || !uri.equals(currentUri);

            if (songChanged) {

                currentUri =
                        uri;

                /*
                 * Clear stale audio visualization
                 * immediately when changing songs.
                 */
                if (seekFullPlayer != null) {

                    seekFullPlayer.clearFFTData();

                    seekFullPlayer.setAudioLevel(0f);

                    seekFullPlayer.setBassLevel(0f);

                    seekFullPlayer.setBeatDetected(false);
                }

                if (cutoutRippleView != null) {

                    cutoutRippleView.clearAudioData();
                }

                /*
                 * Clear previous album art immediately.
                 */
                if (fullPlayerAlbumArt != null) {

                    fullPlayerAlbumArt.setImageResource(
                            R.drawable.ic_play);
                }

                loadCurrentSong(uri);
            }
        }

        if (duration > 0) {

            currentDuration =
                    duration;

            if (fullPlayerDuration != null) {

                fullPlayerDuration.setText(
                        formatTime(duration));
            }
        }

        if (!userIsSeeking) {

            if (position >= 0) {

                currentPosition =
                        position;
            }

            if (fullPlayerElapsed != null) {

                fullPlayerElapsed.setText(
                        formatTime(currentPosition));
            }

            updateSeekProgress(
                    currentPosition,
                    currentDuration);

            updateLyrics(
                    currentPosition);
        }

        updatePlayPauseIcon();

        updatePlaybackModeButtons();

        if (seekFullPlayer != null) {

            seekFullPlayer.setEqualizerPlaying(
                    playing);
        }
    }

    private void updateLyrics(int position) {

        if (!lyricsMode) {

            return;
        }

        updateLyricsPlaceholder();
    }

    private void updateSeekProgress(
            int position,
            int duration) {

        if (seekFullPlayer == null) {

            return;
        }

        if (duration <= 0) {

            seekFullPlayer.setProgress(0);

            return;
        }

        if (position < 0) {

            position = 0;
        }

        if (position > duration) {

            position = duration;
        }

        int progress =
                Math.round(
                        (position / (float) duration)
                                * 1000f);

        if (progress < 0) {

            progress = 0;
        }

        if (progress > 1000) {

            progress = 1000;
        }

        seekFullPlayer.setProgress(
                progress);
    }

    private void updatePlayPauseIcon() {

        if (btnFullPlayPause == null
                || !isAdded()) {

            return;
        }

        if (isPlaying) {

            btnFullPlayPause.setImageDrawable(
                    AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_pause));

        } else {

            btnFullPlayPause.setImageResource(
                    R.drawable.ic_play);
        }

        btnFullPlayPause.setColorFilter(
                currentAccentColor);
    }

    private void updatePlaybackModeButtons() {

        int accentColor =
                currentAccentColor;

        int inactiveColor =
                createInactiveAccentColor(
                        accentColor);

        if (btnShuffle != null) {

            btnShuffle.setColorFilter(
                    shuffleEnabled
                            ? accentColor
                            : inactiveColor);
        }

        if (btnRepeat != null) {

            btnRepeat.setColorFilter(
                    repeatMode != MusicPlayerService.REPEAT_OFF
                            ? accentColor
                            : inactiveColor);
        }

        if (btnEqualizer != null) {

            btnEqualizer.setColorFilter(
                    inactiveColor);
        }

        if (btnQueue != null) {

            btnQueue.setColorFilter(
                    inactiveColor);
        }
    }

    private int createInactiveAccentColor(
            int accentColor) {

        int red =
                Color.red(accentColor);

        int green =
                Color.green(accentColor);

        int blue =
                Color.blue(accentColor);

        red =
                Math.round(
                        red * 0.62f
                                + 168f * 0.38f);

        green =
                Math.round(
                        green * 0.62f
                                + 171f * 0.38f);

        blue =
                Math.round(
                        blue * 0.62f
                                + 185f * 0.38f);

        return Color.rgb(
                clampColor(red),
                clampColor(green),
                clampColor(blue));
    }

    private int clampColor(int value) {

        if (value < 0) {

            return 0;
        }

        if (value > 255) {

            return 255;
        }

        return value;
    }

    private void sendServiceAction(
            String action) {

        if (!isAdded()) {

            return;
        }

        Intent intent =
                new Intent(
                        requireContext(),
                        MusicPlayerService.class);

        intent.setAction(action);

        startMusicService(intent);
    }

    private void sendSeekCommand(
            int position) {

        if (!isAdded()) {

            return;
        }

        Intent intent =
                new Intent(
                        requireContext(),
                        MusicPlayerService.class);

        intent.setAction(
                MusicPlayerService.ACTION_SEEK);

        intent.putExtra(
                MusicPlayerService.EXTRA_SEEK_POSITION,
                position);

        startMusicService(intent);
    }

    private void startMusicService(
            Intent intent) {

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.O) {

                requireContext()
                        .startForegroundService(intent);

            } else {

                requireContext()
                        .startService(intent);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void requestCurrentPlayerState() {

        /*
         * The service broadcast is the source of truth.
         */
    }

    private void loadCurrentSong(
            final String uri) {

        if (!isAdded()
                || uri == null
                || uri.trim().isEmpty()) {

            return;
        }

        final String requestedUri =
                uri;

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        AudioFile foundSong =
                                null;

                        try {

                            MusicRepository repository =
                                    new MusicRepository(
                                            requireContext()
                                                    .getApplicationContext());

                            ArrayList<AudioFile> songs =
                                    repository.getAllSongs();

                            if (songs != null) {

                                for (AudioFile song : songs) {

                                    if (song != null
                                            && song.getUri() != null
                                            && requestedUri.equals(
                                                    song.getUri())) {

                                        foundSong =
                                                song;

                                        break;
                                    }
                                }
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }

                        final AudioFile result =
                                foundSong;

                        if (!isAdded()) {

                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                if (!isAdded()) {

                                                    return;
                                                }

                                                if (currentUri == null
                                                        || !requestedUri.equals(
                                                                currentUri)) {

                                                    return;
                                                }

                                                if (result == null) {

                                                    return;
                                                }

                                                updateSongInformation(
                                                        result,
                                                        requestedUri);
                                            }
                                        });
                    }
                })
                .start();
    }

    private void updateSongInformation(
            AudioFile song,
            String requestedUri) {

        if (song == null
                || !isAdded()
                || requestedUri == null
                || currentUri == null
                || !requestedUri.equals(
                        currentUri)) {

            return;
        }

        if (fullPlayerTitle != null) {

            fullPlayerTitle.setText(
                    safeText(
                            song.getTitle(),
                            "No song"));
        }

        if (fullPlayerArtist != null) {

            fullPlayerArtist.setText(
                    safeText(
                            song.getArtist(),
                            "Unknown artist"));
        }

        loadAlbumArt(
                song,
                requestedUri);

        applyAlbumColor(
                song,
                requestedUri);
    }

    private void loadAlbumArt(
            final AudioFile song,
            final String requestedUri) {

        if (!isAdded()
                || song == null
                || requestedUri == null
                || currentUri == null
                || !requestedUri.equals(
                        currentUri)) {

            return;
        }

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap =
                                null;

                        try {

                            android.net.Uri songUri =
                                    android.net.Uri.parse(
                                            requestedUri);

                            bitmap =
                                    requireContext()
                                            .getContentResolver()
                                            .loadThumbnail(
                                                    songUri,
                                                    new android.util.Size(
                                                            800,
                                                            800),
                                                    null);

                        } catch (Exception ignored) {
                        }

                        if (bitmap == null) {

                            String path =
                                    song.getPath();

                            if (path != null
                                    && !path.trim().isEmpty()) {

                                android.media.MediaMetadataRetriever retriever =
                                        new android.media.MediaMetadataRetriever();

                                try {

                                    retriever.setDataSource(
                                            path);

                                    byte[] artwork =
                                            retriever.getEmbeddedPicture();

                                    if (artwork != null
                                            && artwork.length > 0) {

                                        bitmap =
                                                BitmapFactory.decodeByteArray(
                                                        artwork,
                                                        0,
                                                        artwork.length);
                                    }

                                } catch (Exception ignored) {

                                } finally {

                                    try {

                                        retriever.release();

                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }

                        final Bitmap result =
                                bitmap;

                        if (!isAdded()) {

                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                if (!isAdded()
                                                        || fullPlayerAlbumArt == null) {

                                                    return;
                                                }

                                                if (currentUri == null
                                                        || !requestedUri.equals(
                                                                currentUri)) {

                                                    return;
                                                }

                                                if (result != null) {

                                                    fullPlayerAlbumArt
                                                            .setImageBitmap(
                                                                    result);

                                                } else {

                                                    fullPlayerAlbumArt
                                                            .setImageResource(
                                                                    R.drawable.ic_play);
                                                }
                                            }
                                        });
                    }
                })
                .start();
    }

    private void applyAlbumColor(
            AudioFile song,
            String requestedUri) {

        if (song == null
                || !isAdded()
                || requestedUri == null
                || currentUri == null
                || !requestedUri.equals(
                        currentUri)) {

            return;
        }

        try {

            AlbumColorManager manager =
                    AlbumColorManager.getInstance(
                            requireContext()
                                    .getApplicationContext());

            String songUri =
                    song.getUri();

            if (songUri == null
                    || !requestedUri.equals(songUri)
                    || !requestedUri.equals(currentUri)) {

                return;
            }

            manager.setCurrentSong(
                    song);

            if (currentUri == null
                    || !requestedUri.equals(currentUri)) {

                return;
            }

            applyPlayerColors(
                    manager.getCurrentAccentColor(),
                    manager.getCurrentBackgroundColor());

        } catch (Exception ignored) {
        }
    }

    private void loadExistingColors() {

        if (!isAdded()) {

            return;
        }

        try {

            AlbumColorManager manager =
                    AlbumColorManager.getInstance(
                            requireContext()
                                    .getApplicationContext());

            String uri =
                    manager.getCurrentUri();

            if (uri != null
                    && !uri.trim().isEmpty()
                    && currentUri != null
                    && uri.equals(currentUri)) {

                applyPlayerColors(
                        manager.getCurrentAccentColor(),
                        manager.getCurrentBackgroundColor());
            }

        } catch (Exception ignored) {
        }
    }

    private void applyPlayerColors(
            final int accentColor,
            final int backgroundColor) {

        if (!isAdded()) {

            return;
        }

        currentAccentColor =
                accentColor;

        currentBackgroundColor =
                backgroundColor;

        if (getView() != null) {

            final int startColor =
                    getCurrentBackgroundColor(
                            getView().getBackground());

            if (backgroundAnimator != null) {

                backgroundAnimator.cancel();
            }

            backgroundAnimator =
                    ValueAnimator.ofArgb(
                            startColor,
                            backgroundColor);

            backgroundAnimator.setDuration(
                    350);

            backgroundAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation) {

                            if (getView() != null) {

                                getView()
                                        .setBackgroundColor(
                                                (Integer)
                                                        animation
                                                                .getAnimatedValue());
                            }
                        }
                    });

            backgroundAnimator.start();
        }

        if (cutoutRippleView != null) {

            cutoutRippleView.setAccentColor(
                    accentColor);
        }

        if (btnFullPlayerBack != null) {

            btnFullPlayerBack.setColorFilter(
                    accentColor);
        }

        if (playerMore != null) {

            playerMore.setColorFilter(
                    accentColor);
        }

        if (seekFullPlayer != null) {

            seekFullPlayer.setEqualizerColor(
                    accentColor);
        }

        if (btnFullPrevious != null) {

            btnFullPrevious.setColorFilter(
                    accentColor);
        }

        if (btnFullNext != null) {

            btnFullNext.setColorFilter(
                    accentColor);
        }

        if (btnFullPlayPause != null) {

            btnFullPlayPause.setColorFilter(
                    accentColor);
        }

        updatePlaybackModeButtons();

        if (lyricsCurrent != null) {

            lyricsCurrent.setTextColor(
                    accentColor);
        }
    }

    private int getCurrentBackgroundColor(
            android.graphics.drawable.Drawable drawable) {

        if (drawable
                instanceof android.graphics.drawable.ColorDrawable) {

            return ((android.graphics.drawable.ColorDrawable)
                    drawable)
                    .getColor();
        }

        return currentBackgroundColor;
    }

    private void registerPlayerReceiver() {

        if (receiverRegistered
                || !isAdded()) {

            return;
        }

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                MusicPlayerService.ACTION_STATE_CHANGED);

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                requireContext()
                        .registerReceiver(
                                playerReceiver,
                                filter,
                                Context.RECEIVER_NOT_EXPORTED);

            } else {

                requireContext()
                        .registerReceiver(
                                playerReceiver,
                                filter);
            }

            receiverRegistered =
                    true;

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void registerColorReceiver() {

        if (colorReceiverRegistered
                || !isAdded()) {

            return;
        }

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                AlbumColorManager.ACTION_COLORS_CHANGED);

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                requireContext()
                        .registerReceiver(
                                colorReceiver,
                                filter,
                                Context.RECEIVER_NOT_EXPORTED);

            } else {

                requireContext()
                        .registerReceiver(
                                colorReceiver,
                                filter);
            }

            colorReceiverRegistered =
                    true;

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void registerAudioAnalysisReceiver() {

        if (audioAnalysisReceiverRegistered
                || !isAdded()) {

            return;
        }

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                MusicPlayerService.ACTION_AUDIO_ANALYSIS);

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                requireContext()
                        .registerReceiver(
                                audioAnalysisReceiver,
                                filter,
                                Context.RECEIVER_NOT_EXPORTED);

            } else {

                requireContext()
                        .registerReceiver(
                                audioAnalysisReceiver,
                                filter);
            }

            audioAnalysisReceiverRegistered =
                    true;

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void unregisterPlayerReceiver() {

        if (!receiverRegistered
                || getContext() == null) {

            return;
        }

        try {

            requireContext()
                    .unregisterReceiver(
                            playerReceiver);

        } catch (Exception ignored) {
        }

        receiverRegistered =
                false;
    }

    private void unregisterColorReceiver() {

        if (!colorReceiverRegistered
                || getContext() == null) {

            return;
        }

        try {

            requireContext()
                    .unregisterReceiver(
                            colorReceiver);

        } catch (Exception ignored) {
        }

        colorReceiverRegistered =
                false;
    }

    private void unregisterAudioAnalysisReceiver() {

        if (!audioAnalysisReceiverRegistered
                || getContext() == null) {

            return;
        }

        try {

            requireContext()
                    .unregisterReceiver(
                            audioAnalysisReceiver);

        } catch (Exception ignored) {
        }

        audioAnalysisReceiverRegistered =
                false;
    }

    private String formatTime(
            int milliseconds) {

        if (milliseconds < 0) {

            milliseconds = 0;
        }

        long totalSeconds =
                milliseconds / 1000L;

        long seconds =
                totalSeconds % 60L;

        long minutes =
                totalSeconds / 60L;

        if (minutes >= 60L) {

            long hours =
                    minutes / 60L;

            minutes =
                    minutes % 60L;

            return String.format(
                    java.util.Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds);
        }

        return String.format(
                java.util.Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds);
    }

    private String safeText(
            String value,
            String fallback) {

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value;
    }

    @Override
    public void onResume() {

        super.onResume();

        if (!receiverRegistered) {

            registerPlayerReceiver();
        }

        if (!colorReceiverRegistered) {

            registerColorReceiver();
        }

        if (!audioAnalysisReceiverRegistered) {

            registerAudioAnalysisReceiver();
        }

        /*
         * Do not let AlbumColorManager select a song here.
         */
        if (currentUri != null) {

            loadExistingColors();
        }
    }

    @Override
    public void onPause() {

        unregisterPlayerReceiver();

        unregisterColorReceiver();

        unregisterAudioAnalysisReceiver();

        super.onPause();
    }

    @Override
    public void onDestroyView() {

        unregisterPlayerReceiver();

        unregisterColorReceiver();

        unregisterAudioAnalysisReceiver();

        if (backgroundAnimator != null) {

            backgroundAnimator.cancel();

            backgroundAnimator =
                    null;
        }

        if (seekFullPlayer != null) {

            seekFullPlayer.setEqualizerPlaying(
                    false);

            seekFullPlayer.clearFFTData();

            seekFullPlayer.setAudioLevel(
                    0f);

            seekFullPlayer.setBassLevel(
                    0f);

            seekFullPlayer.setBeatDetected(
                    false);
        }

        if (cutoutRippleView != null) {

            cutoutRippleView.clearAudioData();
        }

        /*
         * Invalidate all pending album-art/color callbacks.
         */
        currentUri =
                null;

        super.onDestroyView();
    }
}