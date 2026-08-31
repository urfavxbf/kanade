package com.urfavxbf.kanade.ui.player;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;
import com.urfavxbf.kanade.databinding.PlayerBinding;

import java.io.InputStream;
import java.util.ArrayList;

public class PlayerFragment extends Fragment {

    private PlayerBinding binding;

    private MusicRepository musicRepository;
    private MusicPlayerController playerController;

    private AlbumColorManager albumColorManager;

    private final ArrayList<AudioFile> songs =
            new ArrayList<>();

    private String currentUri;

    private boolean userSeeking = false;

    private int currentAccentColor =
            Color.rgb(201, 196, 255);

    private int currentBackgroundColor =
            Color.rgb(16, 17, 26);

    private ValueAnimator colorAnimator;

    private final BroadcastReceiver playerReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!MusicPlayerService.ACTION_STATE_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    boolean isPlaying =
                            intent.getBooleanExtra(
                                    MusicPlayerService.EXTRA_IS_PLAYING,
                                    false
                            );

                    String uri =
                            intent.getStringExtra(
                                    MusicPlayerService.EXTRA_CURRENT_URI
                            );

                    int position =
                            intent.getIntExtra(
                                    MusicPlayerService.EXTRA_POSITION,
                                    0
                            );

                    int duration =
                            intent.getIntExtra(
                                    MusicPlayerService.EXTRA_DURATION,
                                    0
                            );

                    updatePlayer(
                            uri,
                            isPlaying,
                            position,
                            duration
                    );
                }
            };

    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    int accentColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                                    currentAccentColor
                            );

                    int backgroundColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                                    currentBackgroundColor
                            );

                    animateThemeColors(
                            accentColor,
                            backgroundColor
                    );
                }
            };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = PlayerBinding.inflate(
                inflater,
                container,
                false
        );

        playerController =
                new MusicPlayerController(
                        requireContext()
                );

        musicRepository =
                new MusicRepository(
                        requireContext()
                );

        albumColorManager =
                AlbumColorManager.getInstance(
                        requireContext().getApplicationContext()
                );

        currentAccentColor =
                albumColorManager
                        .getCurrentAccentColor();

        currentBackgroundColor =
                albumColorManager
                        .getCurrentBackgroundColor();

        applyThemeColors(
                currentAccentColor,
                currentBackgroundColor
        );

        binding.btnFullPlayPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        playerController.play();
                    }
                }
        );

        binding.btnFullNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        playerController.next();
                    }
                }
        );

        binding.btnFullPrevious.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        playerController.previous();
                    }
                }
        );

        binding.seekFullPlayer.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (fromUser) {
                            userSeeking = true;
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {

                        userSeeking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {

                        userSeeking = false;

                        sendSeekCommand(
                                seekBar.getProgress()
                        );
                    }
                }
        );

        loadMusic();

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter playerFilter =
                new IntentFilter(
                        MusicPlayerService.ACTION_STATE_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                playerReceiver,
                playerFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        IntentFilter colorFilter =
                new IntentFilter(
                        AlbumColorManager.ACTION_COLORS_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                colorReceiver,
                colorFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        if (albumColorManager != null) {

            currentAccentColor =
                    albumColorManager
                            .getCurrentAccentColor();

            currentBackgroundColor =
                    albumColorManager
                            .getCurrentBackgroundColor();

            applyThemeColors(
                    currentAccentColor,
                    currentBackgroundColor
            );
        }
    }

    @Override
    public void onStop() {

        try {

            requireContext()
                    .unregisterReceiver(
                            playerReceiver
                    );

        } catch (Exception ignored) {
        }

        try {

            requireContext()
                    .unregisterReceiver(
                            colorReceiver
                    );

        } catch (Exception ignored) {
        }

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        super.onStop();
    }

    private void loadMusic() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (musicRepository == null) {
                    return;
                }

                final ArrayList<AudioFile> result =
                        musicRepository.getAllSongs();

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                songs.clear();

                                if (result != null) {
                                    songs.addAll(result);
                                }
                            }
                        }
                );
            }
        }).start();
    }

    private void updatePlayer(
            String uri,
            boolean isPlaying,
            int position,
            int duration) {

        if (binding == null) {
            return;
        }

        if (uri == null ||
                uri.trim().isEmpty()) {
            return;
        }

        boolean songChanged =
                !uri.equals(currentUri);

        if (duration > 0) {

            if (binding.seekFullPlayer.getMax()
                    != duration) {

                binding.seekFullPlayer.setMax(
                        duration
                );
            }

            if (!userSeeking) {

                binding.seekFullPlayer.setProgress(
                        Math.min(
                                Math.max(position, 0),
                                duration
                        )
                );
            }
        }

        binding.btnFullPlayPause.setImageResource(
        isPlaying
                ? R.drawable.ic_pause
                : R.drawable.ic_play
);

binding.seekFullPlayer.setEqualizerPlaying(
        isPlaying
);

        /*
         * IMPORTANT:
         *
         * Never use a hardcoded black tint here.
         * Player state broadcasts happen frequently and
         * would overwrite the dynamic album color.
         */
        binding.btnFullPlayPause.setImageTintList(
                ColorStateList.valueOf(
                        currentAccentColor
                )
        );

        if (!songChanged) {
            return;
        }

        AudioFile currentSong = null;

        for (AudioFile song : songs) {

            if (song != null &&
                    uri.equals(song.getUri())) {

                currentSong = song;
                break;
            }
        }

        if (currentSong == null) {
            return;
        }

        currentUri = uri;

        String title =
                currentSong.getTitle();

        if (title == null ||
                title.trim().isEmpty()) {

            title = "Unknown song";
        }

        binding.fullPlayerTitle.setText(
                title
        );

        String artist =
                currentSong.getArtist();

        if (artist == null ||
                artist.trim().isEmpty()) {

            artist = "Unknown artist";
        }

        binding.fullPlayerArtist.setText(
                artist
        );

        binding.fullPlayerAlbumArt.setImageResource(
                R.drawable.ic_play
        );

        loadAlbumArt(
                currentSong,
                uri
        );
    }

    private void loadAlbumArt(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        final String albumArtUri =
                song.getAlbumArtUri();

        if (albumArtUri == null ||
                albumArtUri.trim().isEmpty()) {

            loadAlbumArtThumbnail(
                    song,
                    requestedUri
            );

            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = null;

                try {

                    Uri uri =
                            Uri.parse(albumArtUri);

                    InputStream inputStream =
                            requireContext()
                                    .getContentResolver()
                                    .openInputStream(uri);

                    if (inputStream != null) {

                        bitmap =
                                BitmapFactory.decodeStream(
                                        inputStream
                                );

                        inputStream.close();
                    }

                } catch (Exception ignored) {
                }

                final Bitmap result = bitmap;

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                if (!requestedUri.equals(
                                        currentUri)) {
                                    return;
                                }

                                if (result != null) {

                                    binding.fullPlayerAlbumArt
                                            .setImageBitmap(
                                                    result
                                            );

                                } else {

                                    loadAlbumArtThumbnail(
                                            song,
                                            requestedUri
                                    );
                                }
                            }
                        }
                );
            }
        }).start();
    }

    private void loadAlbumArtThumbnail(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = null;

                try {

                    bitmap =
                            requireContext()
                                    .getContentResolver()
                                    .loadThumbnail(
                                            Uri.parse(
                                                    song.getUri()
                                            ),
                                            new android.util.Size(
                                                    800,
                                                    800
                                            ),
                                            null
                                    );

                } catch (Exception ignored) {
                }

                final Bitmap result = bitmap;

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                if (!requestedUri.equals(
                                        currentUri)) {
                                    return;
                                }

                                if (result != null) {

                                    binding.fullPlayerAlbumArt
                                            .setImageBitmap(
                                                    result
                                            );

                                } else {

                                    binding.fullPlayerAlbumArt
                                            .setImageResource(
                                                    R.drawable.ic_play
                                            );
                                }
                            }
                        }
                );
            }
        }).start();
    }

    private void sendSeekCommand(
            int progress) {

        if (playerController == null) {
            return;
        }

        playerController.seekTo(
                progress
        );
    }

    private void animateThemeColors(
            final int newAccentColor,
            final int newBackgroundColor) {

        if (binding == null) {
            return;
        }

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        final int oldAccentColor =
                currentAccentColor;

        final int oldBackgroundColor =
                currentBackgroundColor;

        colorAnimator =
                ValueAnimator.ofFloat(
                        0.0f,
                        1.0f
                );

        colorAnimator.setDuration(450);

        colorAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float fraction =
                                (Float)
                                        animation.getAnimatedValue();

                        int accent =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldAccentColor,
                                                        newAccentColor
                                                );

                        int background =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldBackgroundColor,
                                                        newBackgroundColor
                                                );

                        applyThemeColors(
                                accent,
                                background
                        );
                    }
                }
        );

        colorAnimator.start();
    }

    private void applyThemeColors(
            int accentColor,
            int backgroundColor) {

        if (binding == null) {
            return;
        }

        currentAccentColor =
                accentColor;

        currentBackgroundColor =
                backgroundColor;

        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        /*
         * Controls remain transparent because the
         * updated player.xml removed the old container
         * background.
         */
        binding.fullPlayerControls
                .setBackgroundColor(
                        Color.TRANSPARENT
                );

        binding.btnFullPrevious
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        binding.btnFullNext
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        /*
         * Play/pause icon uses the album accent.
         *
         * This is the important fix.
         */
        binding.btnFullPlayPause
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        binding.seekFullPlayer.setEqualizerColor(
        currentAccentColor
);

binding.seekFullPlayer.setEqualizerBackgroundColor(
        currentBackgroundColor
);

        binding.fullPlayerTitle
                .setTextColor(
                        Color.WHITE
                );

        binding.fullPlayerArtist
                .setTextColor(
                        blendColors(
                                currentAccentColor,
                                Color.WHITE,
                                0.35f
                        )
                );

        binding.playerTopTitle
                .setTextColor(
                        Color.WHITE
                );

        binding.btnFullPlayerBack
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        binding.playerMore
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );
    }

    private int darkenColor(
            int color,
            float factor) {

        int red =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.red(color)
                                                * factor
                                )
                        )
                );

        int green =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.green(color)
                                                * factor
                                )
                        )
                );

        int blue =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.blue(color)
                                                * factor
                                )
                        )
                );

        return Color.rgb(
                red,
                green,
                blue
        );
    }

    private int blendColors(
            int color1,
            int color2,
            float ratio) {

        ratio =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                ratio
                        )
                );

        int inverse =
                1 - Math.round(
                        ratio * 255
                );

        int r =
                (
                        Color.red(color1) * inverse
                                +
                        Color.red(color2)
                                * (255 - inverse)
                ) / 255;

        int g =
                (
                        Color.green(color1) * inverse
                                +
                        Color.green(color2)
                                * (255 - inverse)
                ) / 255;

        int b =
                (
                        Color.blue(color1) * inverse
                                +
                        Color.blue(color2)
                                * (255 - inverse)
                ) / 255;

        return Color.rgb(
                r,
                g,
                b
        );
    }

    @Override
    public void onDestroyView() {

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        currentUri = null;
        userSeeking = false;

        binding = null;

        super.onDestroyView();
    }
}