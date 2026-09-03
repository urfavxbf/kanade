package com.urfavxbf.kanade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.databinding.PlayerBinding;

public class PlayerActivity extends AppCompatActivity {

    private PlayerBinding binding;

    private boolean receiverRegistered = false;

    private boolean colorReceiverRegistered = false;

    private boolean isPlaying = false;

    private String currentUri = null;

    private int currentPosition = 0;

    private int currentDuration = 0;

    /*
     * CENTRALIZED ALBUM COLORS
     */
    private AlbumColorManager albumColorManager;

    private int currentAccentColor =
            Color.rgb(
                    201,
                    196,
                    255
            );

    private int currentBackgroundColor =
            Color.rgb(
                    16,
                    17,
                    26
            );

    private android.animation.ValueAnimator colorAnimator;

    /*
     * Receives album colors from AlbumColorManager.
     */
    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    String colorUri =
                            intent.getStringExtra(
                                    AlbumColorManager.EXTRA_CURRENT_URI
                            );

                    /*
                     * Ignore colors belonging to another song.
                     */
                    if (currentUri != null &&
                            colorUri != null &&
                            !currentUri.equals(colorUri)) {
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

    private final BroadcastReceiver playerReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!MusicPlayerService.ACTION_STATE_CHANGED.equals(
                            intent.getAction()
                    )) {
                        return;
                    }

                    updatePlayerUI(intent);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                PlayerBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        /*
         * -----------------------------------------------------
         * ALBUM COLOR MANAGER
         * -----------------------------------------------------
         */

        albumColorManager =
                AlbumColorManager.getInstance(
                        getApplicationContext()
                );

        /*
         * Use cached colors immediately.
         */
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
                v -> {

                    Intent intent =
                            new Intent(
                                    PlayerActivity.this,
                                    MusicPlayerService.class
                            );

                    if (isPlaying) {

                        intent.setAction(
                                MusicPlayerService.ACTION_PAUSE
                        );

                    } else {

                        intent.setAction(
                                MusicPlayerService.ACTION_PLAY
                        );
                    }

                    startService(
                            intent
                    );
                }
        );

        /*
         * NEXT
         */

        binding.btnFullNext.setOnClickListener(
                v -> sendServiceAction(
                        MusicPlayerService.ACTION_NEXT
                )
        );

        /*
         * PREVIOUS
         */

        binding.btnFullPrevious.setOnClickListener(
                v -> sendServiceAction(
                        MusicPlayerService.ACTION_PREVIOUS
                )
        );

        /*
         * SEEK BAR
         */

        binding.seekFullPlayer.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    private boolean userChanging = false;

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (!fromUser) {
                            return;
                        }

                        userChanging = true;

                        binding.fullPlayerElapsed.setText(
                                formatTime(
                                        progress
                                )
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {

                        userChanging = true;
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {

                        userChanging = false;

                        sendSeekCommand(
                                seekBar.getProgress()
                        );
                    }
                }
        );

        /*
         * MORE MENU
         */

        binding.playerMore.setOnClickListener(
                v -> showPlayerMenu(v)
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerPlayerReceiver();

        registerColorReceiver();

        /*
         * Re-apply cached album colors.
         */
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
    protected void onStop() {

        unregisterPlayerReceiver();

        unregisterColorReceiver();

        if (colorAnimator != null) {

            colorAnimator.cancel();

            colorAnimator = null;
        }

        super.onStop();
    }

    /*
     * ---------------------------------------------------------
     * PLAYER RECEIVER
     * ---------------------------------------------------------
     */

    private void registerPlayerReceiver() {

        if (receiverRegistered) {
            return;
        }

        IntentFilter filter =
                new IntentFilter(
                        MusicPlayerService.ACTION_STATE_CHANGED
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                    playerReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );

        } else {

            ContextCompat.registerReceiver(
                    this,
                    playerReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        receiverRegistered = true;
    }

    private void unregisterPlayerReceiver() {

        if (!receiverRegistered) {
            return;
        }

        try {

            unregisterReceiver(
                    playerReceiver
            );

        } catch (Exception ignored) {
        }

        receiverRegistered = false;
    }

    /*
     * ---------------------------------------------------------
     * COLOR RECEIVER
     * ---------------------------------------------------------
     */

    private void registerColorReceiver() {

        if (colorReceiverRegistered) {
            return;
        }

        IntentFilter filter =
                new IntentFilter(
                        AlbumColorManager.ACTION_COLORS_CHANGED
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                    colorReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );

        } else {

            ContextCompat.registerReceiver(
                    this,
                    colorReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        colorReceiverRegistered = true;
    }

    private void unregisterColorReceiver() {

        if (!colorReceiverRegistered) {
            return;
        }

        try {

            unregisterReceiver(
                    colorReceiver
            );

        } catch (Exception ignored) {
        }

        colorReceiverRegistered = false;
    }

    /*
     * ---------------------------------------------------------
     * PLAYER UI
     * ---------------------------------------------------------
     */

    private void updatePlayerUI(
            Intent intent) {

        isPlaying =
                intent.getBooleanExtra(
                        MusicPlayerService.EXTRA_IS_PLAYING,
                        false
                );

        binding.seekFullPlayer.setEqualizerPlaying(
                isPlaying
        );

        currentPosition =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_POSITION,
                        0
                );

        currentDuration =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_DURATION,
                        0
                );

        String newUri =
                intent.getStringExtra(
                        MusicPlayerService.EXTRA_CURRENT_URI
                );

        /*
         * PLAY / PAUSE
         */

        if (isPlaying) {

            binding.btnFullPlayPause.setImageResource(
                    R.drawable.ic_pause
            );

        } else {

            binding.btnFullPlayPause.setImageResource(
                    R.drawable.ic_play
            );
        }

        /*
         * SEEK BAR
         */

        if (currentDuration > 0) {

            binding.seekFullPlayer.setMax(
                    currentDuration
            );

            binding.seekFullPlayer.setProgress(
                    Math.min(
                            currentPosition,
                            currentDuration
                    )
            );

            binding.fullPlayerElapsed.setText(
                    formatTime(
                            currentPosition
                    )
            );

            binding.fullPlayerDuration.setText(
                    formatTime(
                            currentDuration
                    )
            );

        } else {

            binding.seekFullPlayer.setMax(
                    0
            );

            binding.seekFullPlayer.setProgress(
                    0
            );

            binding.fullPlayerElapsed.setText(
                    "0:00"
            );

            binding.fullPlayerDuration.setText(
                    "0:00"
            );
        }

        /*
         * SONG CHANGED
         */

        if (newUri != null &&
                !newUri.trim().isEmpty()) {

            boolean songChanged =
                    !newUri.equals(
                            currentUri
                    );

            currentUri =
                    newUri;

            if (songChanged) {

                loadSongInfo(
                        newUri
                );
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * SONG INFO
     * ---------------------------------------------------------
     */

    private void loadSongInfo(
            String uriString) {

        if (uriString == null ||
                uriString.trim().isEmpty()) {

            return;
        }

        try {

            Uri uri =
                    Uri.parse(
                            uriString
                    );

            String[] projection = {
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID
            };

            try (
                    android.database.Cursor cursor =
                            getContentResolver().query(
                                    uri,
                                    projection,
                                    null,
                                    null,
                                    null
                            )
            ) {

                if (cursor == null ||
                        !cursor.moveToFirst()) {

                    loadAlbumArtFromSong(uri);

                    return;
                }

                int titleIndex =
                        cursor.getColumnIndex(
                                MediaStore.Audio.Media.TITLE
                        );

                int artistIndex =
                        cursor.getColumnIndex(
                                MediaStore.Audio.Media.ARTIST
                        );

                int albumIdIndex =
                        cursor.getColumnIndex(
                                MediaStore.Audio.Media.ALBUM_ID
                        );

                /*
                 * TITLE
                 */

                if (titleIndex >= 0) {

                    String title =
                            cursor.getString(
                                    titleIndex
                            );

                    binding.fullPlayerTitle.setText(
                            safeText(
                                    title,
                                    "No song"
                            )
                    );
                }

                /*
                 * ARTIST
                 */

                if (artistIndex >= 0) {

                    String artist =
                            cursor.getString(
                                    artistIndex
                            );

                    binding.fullPlayerArtist.setText(
                            safeText(
                                    artist,
                                    "Unknown artist"
                            )
                    );
                }

                /*
                 * ALBUM ART
                 */

                if (albumIdIndex >= 0) {

                    long albumId =
                            cursor.getLong(
                                    albumIdIndex
                            );

                    if (albumId > 0) {

                        loadAlbumArt(
                                albumId,
                                uri
                        );

                    } else {

                        loadAlbumArtFromSong(
                                uri
                        );
                    }

                } else {

                    loadAlbumArtFromSong(
                            uri
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            try {

                loadAlbumArtFromSong(
                        Uri.parse(
                                uriString
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * ALBUM ART
     * ---------------------------------------------------------
     */

    private void loadAlbumArt(
            long albumId,
            Uri songUri) {

        if (albumId <= 0) {

            loadAlbumArtFromSong(
                    songUri
            );

            return;
        }

        Uri albumArtUri =
                Uri.parse(
                        "content://media/external/audio/albumart/"
                                + albumId
                );

        new Thread(
                () -> {

                    Bitmap bitmap = null;

                    try {

                        BitmapFactory.Options options =
                                new BitmapFactory.Options();

                        options.inPreferredConfig =
                                Bitmap.Config.ARGB_8888;

                        try (
                                java.io.InputStream inputStream =
                                        getContentResolver()
                                                .openInputStream(
                                                        albumArtUri
                                                )
                        ) {

                            if (inputStream != null) {

                                bitmap =
                                        BitmapFactory.decodeStream(
                                                inputStream,
                                                null,
                                                options
                                        );
                            }
                        }

                    } catch (Exception ignored) {
                    }

                    Bitmap finalBitmap =
                            bitmap;

                    if (finalBitmap == null) {

                        loadAlbumArtFromSong(
                                songUri
                        );

                        return;
                    }

                    runOnUiThread(
                            () -> {

                                if (binding == null) {

                                    if (!finalBitmap.isRecycled()) {
                                        finalBitmap.recycle();
                                    }

                                    return;
                                }

                                if (currentUri == null ||
                                        songUri == null ||
                                        !currentUri.equals(
                                                songUri.toString()
                                        )) {

                                    if (!finalBitmap.isRecycled()) {
                                        finalBitmap.recycle();
                                    }

                                    return;
                                }

                                binding.fullPlayerAlbumArt
                                        .setImageBitmap(
                                                finalBitmap
                                        );

                                int dominantColor =
                                        getDominantColor(
                                                finalBitmap
                                        );

                                int backgroundColor =
                                        getPlayerBackgroundColor(
                                                dominantColor
                                        );

                                animateThemeColors(
                                        dominantColor,
                                        backgroundColor
                                );
                            }
                    );

                }
        ).start();
    }

    /*
     * ---------------------------------------------------------
     * SONG THUMBNAIL FALLBACK
     * ---------------------------------------------------------
     */

    private void loadAlbumArtFromSong(
            Uri songUri) {

        if (songUri == null) {

            setDefaultAlbumArt();

            return;
        }

        new Thread(
                () -> {

                    Bitmap bitmap = null;

                    try {

                        if (Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q) {

                            bitmap =
                                    getContentResolver()
                                            .loadThumbnail(
                                                    songUri,
                                                    new android.util.Size(
                                                            800,
                                                            800
                                                    ),
                                                    null
                                            );

                        } else {

                            android.media.MediaMetadataRetriever retriever =
                                    new android.media.MediaMetadataRetriever();

                            try {

                                retriever.setDataSource(
                                        PlayerActivity.this,
                                        songUri
                                );

                                byte[] artwork =
                                        retriever.getEmbeddedPicture();

                                if (artwork != null &&
                                        artwork.length > 0) {

                                    bitmap =
                                            BitmapFactory.decodeByteArray(
                                                    artwork,
                                                    0,
                                                    artwork.length
                                            );
                                }

                            } finally {

                                try {

                                    retriever.release();

                                } catch (Exception ignored) {
                                }
                            }
                        }

                    } catch (Exception ignored) {
                    }

                    Bitmap finalBitmap =
                            bitmap;

                    runOnUiThread(
                            () -> {

                                if (binding == null) {

                                    if (finalBitmap != null &&
                                            !finalBitmap.isRecycled()) {
                                        finalBitmap.recycle();
                                    }

                                    return;
                                }

                                if (currentUri == null ||
                                        !currentUri.equals(
                                                songUri.toString()
                                        )) {

                                    if (finalBitmap != null &&
                                            !finalBitmap.isRecycled()) {
                                        finalBitmap.recycle();
                                    }

                                    return;
                                }

                                if (finalBitmap != null) {

                                    binding.fullPlayerAlbumArt
                                            .setImageBitmap(
                                                    finalBitmap
                                            );

                                    int dominantColor =
                                            getDominantColor(
                                                    finalBitmap
                                            );

                                    int backgroundColor =
                                            getPlayerBackgroundColor(
                                                    dominantColor
                                            );

                                    animateThemeColors(
                                            dominantColor,
                                            backgroundColor
                                    );

                                } else {

                                    setDefaultAlbumArt();
                                }
                            }
                    );

                }
        ).start();
    }

    private void setDefaultAlbumArt() {

        if (binding == null) {
            return;
        }

        binding.fullPlayerAlbumArt.setImageResource(
                R.drawable.ic_play
        );

        int accent =
                Color.rgb(
                        201,
                        196,
                        255
                );

        int background =
                Color.rgb(
                        16,
                        17,
                        26
                );

        animateThemeColors(
                accent,
                background
        );
    }

    /*
     * ---------------------------------------------------------
     * DOMINANT COLOR
     * ---------------------------------------------------------
     */

    private int getDominantColor(
            Bitmap bitmap) {

        if (bitmap == null ||
                bitmap.isRecycled()) {

            return Color.rgb(
                    201,
                    196,
                    255
            );
        }

        int width =
                bitmap.getWidth();

        int height =
                bitmap.getHeight();

        if (width <= 0 ||
                height <= 0) {

            return Color.rgb(
                    201,
                    196,
                    255
            );
        }

        long red = 0;
        long green = 0;
        long blue = 0;

        int count = 0;

        int stepX =
                Math.max(
                        1,
                        width / 32
                );

        int stepY =
                Math.max(
                        1,
                        height / 32
                );

        for (int y = 0;
             y < height;
             y += stepY) {

            for (int x = 0;
                 x < width;
                 x += stepX) {

                int pixel =
                        bitmap.getPixel(
                                x,
                                y
                        );

                int alpha =
                        Color.alpha(
                                pixel
                        );

                if (alpha < 180) {
                    continue;
                }

                int r =
                        Color.red(
                                pixel
                        );

                int g =
                        Color.green(
                                pixel
                        );

                int b =
                        Color.blue(
                                pixel
                        );

                int brightness =
                        (r + g + b) / 3;

                if (brightness < 25) {
                    continue;
                }

                red += r;
                green += g;
                blue += b;

                count++;
            }
        }

        if (count == 0) {

            return Color.rgb(
                    201,
                    196,
                    255
            );
        }

        int averageRed =
                (int) (red / count);

        int averageGreen =
                (int) (green / count);

        int averageBlue =
                (int) (blue / count);

        float[] hsv =
                new float[3];

        Color.colorToHSV(
                Color.rgb(
                        averageRed,
                        averageGreen,
                        averageBlue
                ),
                hsv
        );

        hsv[1] =
                Math.min(
                        1f,
                        hsv[1] * 1.35f
                );

        hsv[2] =
                Math.max(
                        0.55f,
                        Math.min(
                                1f,
                                hsv[2] * 1.15f
                        )
                );

        return Color.HSVToColor(
                hsv
        );
    }

    /*
     * ---------------------------------------------------------
     * PLAYER THEME COLORS
     * ---------------------------------------------------------
     */

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
                android.animation.ValueAnimator.ofFloat(
                        0.0f,
                        1.0f
                );

        colorAnimator.setDuration(
                450
        );

        colorAnimator.addUpdateListener(
                animation -> {

                    float fraction =
                            (Float)
                                    animation.getAnimatedValue();

                    int accent =
                            (Integer)
                                    new android.animation.ArgbEvaluator()
                                            .evaluate(
                                                    fraction,
                                                    oldAccentColor,
                                                    newAccentColor
                                            );

                    int background =
                            (Integer)
                                    new android.animation.ArgbEvaluator()
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
        );

        colorAnimator.addListener(
                new android.animation.AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(
                            android.animation.Animator animation) {

                        currentAccentColor =
                                newAccentColor;

                        currentBackgroundColor =
                                newBackgroundColor;

                        applyThemeColors(
                                currentAccentColor,
                                currentBackgroundColor
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

        /*
         * PLAYER BACKGROUND
         */
        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        /*
         * EQUALIZER
         */
        binding.seekFullPlayer
                .setEqualizerColor(
                        accentColor
                );

        binding.seekFullPlayer
                .setEqualizerBackgroundColor(
                        backgroundColor
                );

        /*
         * PLAYER CONTROLS
         */
        binding.btnFullPlayPause
                .setColorFilter(
                        accentColor
                );

        binding.btnFullPrevious
                .setColorFilter(
                        accentColor
                );

        binding.btnFullNext
                .setColorFilter(
                        accentColor
                );

        binding.playerMore
                .setColorFilter(
                        accentColor
                );

        /*
         * TEXT
         */
        binding.fullPlayerTitle
                .setTextColor(
                        getTitleTextColor(
                                backgroundColor
                        )
                );

        binding.fullPlayerArtist
                .setTextColor(
                        getSecondaryTextColor(
                                backgroundColor
                        )
                );

        binding.fullPlayerElapsed
                .setTextColor(
                        getSecondaryTextColor(
                                backgroundColor
                        )
                );

        binding.fullPlayerDuration
                .setTextColor(
                        getSecondaryTextColor(
                                backgroundColor
                        )
                );

        /*
         * SYSTEM NAVIGATION BAR
         */
        Window window =
                getWindow();

        if (window != null) {

            window.setNavigationBarColor(
                    backgroundColor
            );

            window.setStatusBarColor(
                    backgroundColor
            );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M) {

                int systemUiVisibility =
                        window.getDecorView()
                                .getSystemUiVisibility();

                if (isColorLight(
                        backgroundColor
                )) {

                    systemUiVisibility |=
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

                    systemUiVisibility |=
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

                } else {

                    systemUiVisibility &=
                            ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

                    systemUiVisibility &=
                            ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }

                window.getDecorView()
                        .setSystemUiVisibility(
                                systemUiVisibility
                        );
            }
        }
    }

    private int getBackgroundColor(
            int accentColor) {

        float[] hsv =
                new float[3];

        Color.colorToHSV(
                accentColor,
                hsv
        );

        hsv[1] =
                Math.min(
                        0.55f,
                        hsv[1] * 0.55f
                );

        hsv[2] =
                0.10f;

        return Color.HSVToColor(
                hsv
        );
    }

    /*
     * Same player background calculation used
     * by AlbumColorManager.
     */
    private int getPlayerBackgroundColor(
            int accentColor) {

        int red =
                Color.red(
                        accentColor
                );

        int green =
                Color.green(
                        accentColor
                );

        int blue =
                Color.blue(
                        accentColor
                );

        red =
                Math.max(
                        8,
                        (int) (red * 0.20f)
                );

        green =
                Math.max(
                        8,
                        (int) (green * 0.20f)
                );

        blue =
                Math.max(
                        10,
                        (int) (blue * 0.23f)
                );

        return Color.rgb(
                red,
                green,
                blue
        );
    }

    private int getTitleTextColor(
            int backgroundColor) {

        return isColorLight(
                backgroundColor
        )
                ? Color.rgb(
                        20,
                        20,
                        25
                )
                : Color.rgb(
                        241,
                        241,
                        246
                );
    }

    private int getSecondaryTextColor(
            int backgroundColor) {

        return isColorLight(
                backgroundColor
        )
                ? Color.rgb(
                        80,
                        80,
                        90
                )
                : Color.rgb(
                        146,
                        148,
                        167
                );
    }

    private boolean isColorLight(
            int color) {

        double luminance =
                (
                        0.299 * Color.red(color)
                                +
                        0.587 * Color.green(color)
                                +
                        0.114 * Color.blue(color)
                );

        return luminance > 160;
    }

    /*
     * ---------------------------------------------------------
     * PLAYER MORE MENU
     * ---------------------------------------------------------
     */

    private void showPlayerMenu(
            View anchor) {

        if (currentUri == null ||
                currentUri.trim().isEmpty()) {

            return;
        }

        android.widget.PopupMenu popupMenu =
                new android.widget.PopupMenu(
                        this,
                        anchor
                );

        popupMenu.getMenu().add(
                "Add to favorites"
        );

        popupMenu.getMenu().add(
                "Add to Playlist"
        );

        popupMenu.getMenu().add(
                "Delete"
        );

        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String title =
                            item.getTitle()
                                    .toString();

                    if ("Add to favorites".equals(
                            title
                    )) {

                        PlaylistManager playlistManager =
                                new PlaylistManager(
                                        this
                                );

                        boolean added =
                                playlistManager.toggleFavorite(
                                        currentUri
                                );

                        android.widget.Toast.makeText(
                                this,
                                added
                                        ? "Added to favorites"
                                        : "Removed from favorites",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    if ("Add to Playlist".equals(
                            title
                    )) {

                        android.widget.Toast.makeText(
                                this,
                                "Add to Playlist",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    if ("Delete".equals(
                            title
                    )) {

                        deleteCurrentSong();

                        return true;
                    }

                    return false;
                }
        );

        popupMenu.show();
    }

    /*
     * ---------------------------------------------------------
     * DELETE CURRENT SONG
     * ---------------------------------------------------------
     */

    private void deleteCurrentSong() {

        if (currentUri == null ||
                currentUri.trim().isEmpty()) {

            return;
        }

        try {

            Uri uri =
                    Uri.parse(
                            currentUri
                    );

            int deleted =
                    getContentResolver().delete(
                            uri,
                            null,
                            null
                    );

            if (deleted > 0) {

                android.widget.Toast.makeText(
                        this,
                        "Song deleted",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

                finish();

            } else {

                android.widget.Toast.makeText(
                        this,
                        "Unable to delete song",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }

        } catch (SecurityException e) {

            android.widget.Toast.makeText(
                    this,
                    "Permission required to delete this song",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            e.printStackTrace();

            android.widget.Toast.makeText(
                    this,
                    "Unable to delete song",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * ---------------------------------------------------------
     * SERVICE COMMANDS
     * ---------------------------------------------------------
     */

    private void sendSeekCommand(
            int position) {

        Intent intent =
                new Intent(
                        this,
                        MusicPlayerService.class
                );

        intent.setAction(
                MusicPlayerService.ACTION_SEEK
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_SEEK_POSITION,
                position
        );

        startService(
                intent
        );
    }

    private void sendServiceAction(
            String action) {

        Intent intent =
                new Intent(
                        this,
                        MusicPlayerService.class
                );

        intent.setAction(
                action
        );

        startService(
                intent
        );
    }

    /*
     * ---------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------
     */

    private String formatTime(
            int milliseconds) {

        if (milliseconds < 0) {
            milliseconds = 0;
        }

        int totalSeconds =
                milliseconds / 1000;

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                java.util.Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private String safeText(
            String value,
            String fallback) {

        if (value == null ||
                value.trim().isEmpty() ||
                "<unknown>".equalsIgnoreCase(value)) {

            return fallback;
        }

        return value;
    }

    @Override
    protected void onDestroy() {

        unregisterPlayerReceiver();

        unregisterColorReceiver();

        if (colorAnimator != null) {

            colorAnimator.cancel();

            colorAnimator = null;
        }

        binding = null;

        super.onDestroy();
    }
}