package com.urfavxbf.kanade;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import androidx.palette.graphics.Palette;

import java.io.InputStream;
import java.util.HashMap;

public class AlbumColorManager {

    public static final String ACTION_COLORS_CHANGED =
            "com.urfavxbf.kanade.ACTION_COLORS_CHANGED";

    public static final String EXTRA_ACCENT_COLOR =
            "com.urfavxbf.kanade.EXTRA_ACCENT_COLOR";

    public static final String EXTRA_BACKGROUND_COLOR =
            "com.urfavxbf.kanade.EXTRA_BACKGROUND_COLOR";

    public static final String EXTRA_CURRENT_URI =
            "com.urfavxbf.kanade.EXTRA_COLOR_CURRENT_URI";

    private static AlbumColorManager instance;

    private final Context context;

    private final HashMap<String, ColorSet> colorCache =
            new HashMap<>();

    private String currentUri;

    private int currentAccentColor =
            Color.rgb(201, 196, 255);

    private int currentBackgroundColor =
            Color.rgb(16, 17, 26);

    private AlbumColorManager(
            Context context) {

        this.context =
                context.getApplicationContext();
    }

    public static synchronized AlbumColorManager getInstance(
            Context context) {

        if (instance == null) {

            instance =
                    new AlbumColorManager(
                            context
                    );
        }

        return instance;
    }

    public void setCurrentSong(
            final AudioFile song) {

        if (song == null) {
            return;
        }

        final String uri =
                song.getUri();

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        if (uri.equals(currentUri)) {
            return;
        }

        currentUri = uri;

        ColorSet cached =
                colorCache.get(uri);

        if (cached != null) {

            applyColors(
                    uri,
                    cached
            );

            return;
        }

        applyColors(
                uri,
                new ColorSet(
                        Color.rgb(
                                201,
                                196,
                                255
                        ),
                        Color.rgb(
                                16,
                                17,
                                26
                        )
                )
        );

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap =
                        loadAlbumArt(song);

                if (bitmap == null) {
                    return;
                }

                ColorSet colors =
                        extractColors(bitmap);

                if (bitmap != null &&
                        !bitmap.isRecycled()) {

                    bitmap.recycle();
                }

                if (colors == null) {
                    return;
                }

                colorCache.put(
                        uri,
                        colors
                );

                if (!uri.equals(currentUri)) {
                    return;
                }

                applyColors(
                        uri,
                        colors
                );
            }
        }).start();
    }

    private Bitmap loadAlbumArt(
            AudioFile song) {

        if (song == null) {
            return null;
        }

        /*
         * =====================================================
         * 1. EMBEDDED ARTWORK FROM THE ACTUAL AUDIO FILE
         * =====================================================
         *
         * This is the safest source because the artwork belongs
         * directly to the song file.
         */

        String path =
                song.getPath();

        if (path != null &&
                !path.trim().isEmpty()) {

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

                    Bitmap bitmap =
                            BitmapFactory.decodeByteArray(
                                    artwork,
                                    0,
                                    artwork.length
                            );

                    if (bitmap != null) {

                        return bitmap;
                    }
                }

            } catch (Exception ignored) {

            } finally {

                try {

                    retriever.release();

                } catch (Exception ignored) {
                }
            }
        }

        /*
         * =====================================================
         * 2. FALLBACK TO THE ACTUAL SONG URI
         * =====================================================
         *
         * This is per-song.
         *
         * We intentionally do NOT use:
         *
         * song.getAlbumArtUri()
         *
         * because that is album-level artwork.
         */

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q) {

            try {

                String songUri =
                        song.getUri();

                if (songUri != null &&
                        !songUri.trim().isEmpty()) {

                    return context
                            .getContentResolver()
                            .loadThumbnail(
                                    Uri.parse(songUri),
                                    new Size(
                                            512,
                                            512
                                    ),
                                    null
                            );
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private ColorSet extractColors(
            Bitmap bitmap) {

        if (bitmap == null) {
            return null;
        }

        try {

            Bitmap paletteBitmap =
                    bitmap;

            if (bitmap.getWidth() > 300 ||
                    bitmap.getHeight() > 300) {

                paletteBitmap =
                        Bitmap.createScaledBitmap(
                                bitmap,
                                300,
                                300,
                                true
                        );
            }

            Palette palette =
                    Palette.from(
                            paletteBitmap
                    )
                    .maximumColorCount(24)
                    .generate();

            int dominant =
                    palette.getDominantColor(
                            Color.rgb(
                                    201,
                                    196,
                                    255
                            )
                    );

            int accent =
                    palette.getVibrantColor(
                            dominant
                    );

            if (isTooDark(accent)) {

                accent =
                        palette.getLightVibrantColor(
                                accent
                        );
            }

            if (isTooDark(accent)) {

                accent =
                        palette.getMutedColor(
                                accent
                        );
            }

            if (isTooDark(accent)) {

                accent =
                        dominant;
            }

            accent =
                    softenAccentColor(
                            accent
                    );

            int background =
                    createPlayerBackground(
                            accent
                    );

            if (paletteBitmap != bitmap &&
                    !paletteBitmap.isRecycled()) {

                paletteBitmap.recycle();
            }

            return new ColorSet(
                    accent,
                    background
            );

        } catch (Exception e) {

            return null;
        }
    }

    private void applyColors(
            String uri,
            ColorSet colors) {

        if (colors == null) {
            return;
        }

        currentAccentColor =
                colors.accentColor;

        currentBackgroundColor =
                colors.backgroundColor;

        Intent intent =
                new Intent(
                        ACTION_COLORS_CHANGED
                );

        intent.setPackage(
                context.getPackageName()
        );

        intent.putExtra(
                EXTRA_ACCENT_COLOR,
                currentAccentColor
        );

        intent.putExtra(
                EXTRA_BACKGROUND_COLOR,
                currentBackgroundColor
        );

        intent.putExtra(
                EXTRA_CURRENT_URI,
                uri
        );

        context.sendBroadcast(
                intent
        );
    }

    public int getCurrentAccentColor() {
        return currentAccentColor;
    }

    public int getCurrentBackgroundColor() {
        return currentBackgroundColor;
    }

    public String getCurrentUri() {
        return currentUri;
    }

    private int createPlayerBackground(
            int accentColor) {

        int red =
                Color.red(accentColor);

        int green =
                Color.green(accentColor);

        int blue =
                Color.blue(accentColor);

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

    private int softenAccentColor(
            int color) {

        float[] hsv =
                new float[3];

        Color.colorToHSV(
                color,
                hsv
        );

        hsv[1] =
                Math.min(
                        hsv[1],
                        0.78f
                );

        hsv[2] =
                Math.max(
                        hsv[2],
                        0.65f
                );

        return Color.HSVToColor(
                hsv
        );
    }

    private boolean isTooDark(
            int color) {

        double luminance =
                (0.2126 *
                        Color.red(color))
                +
                (0.7152 *
                        Color.green(color))
                +
                (0.0722 *
                        Color.blue(color));

        return luminance < 45;
    }

    private static class ColorSet {

        final int accentColor;
        final int backgroundColor;

        ColorSet(
                int accentColor,
                int backgroundColor) {

            this.accentColor =
                    accentColor;

            this.backgroundColor =
                    backgroundColor;
        }
    }
}