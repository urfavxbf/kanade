package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkResolver {
    public interface Callback { void onArtworkResolved(Bitmap bitmap); }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile String currentUri;
    private static volatile Bitmap currentBitmap;

    private ArtworkResolver() {}

    public static void initialize(Context context) {
        // Kept as a no-op compatibility entry point. Artwork is synchronized by the playback service.
    }

    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Context appContext = context.getApplicationContext();
        String uri = song.getUri();
        if (uri != null && uri.equals(currentUri) && currentBitmap != null && !currentBitmap.isRecycled()) {
            MAIN_HANDLER.post(() -> callback.onArtworkResolved(currentBitmap));
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bitmap = resolveBlocking(appContext, song);
            if (bitmap == null) return;
            currentUri = uri;
            currentBitmap = bitmap;
            MAIN_HANDLER.post(() -> callback.onArtworkResolved(bitmap));
        });
    }

    public static Bitmap getCurrentArtwork(String uri) {
        Bitmap bitmap = currentBitmap;
        if (uri == null || !uri.equals(currentUri) || bitmap == null || bitmap.isRecycled()) return null;
        return bitmap;
    }

    private static Bitmap resolveBlocking(Context context, AudioFile song) {
        Bitmap bitmap = loadLocalEmbeddedArtwork(context, song.getUri());
        if (bitmap != null) return bitmap;

        String artworkUri = song.getAlbumArtUri();
        if (artworkUri != null && !artworkUri.trim().isEmpty()) {
            bitmap = loadImage(artworkUri);
            if (bitmap != null) return bitmap;
        }

        String path = song.getPath();
        if (path != null && !path.trim().isEmpty()) {
            bitmap = loadImage(path);
            if (bitmap != null) return bitmap;
        }
        return null;
    }

    private static Bitmap loadLocalEmbeddedArtwork(Context context, String uri) {
        if (uri == null || uri.trim().isEmpty() || uri.startsWith("http://") || uri.startsWith("https://")) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, Uri.parse(uri));
            byte[] data = retriever.getEmbeddedPicture();
            if (data == null || data.length == 0) return null;
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private static Bitmap loadImage(String value) {
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                } finally {
                    connection.disconnect();
                }
            }
            Uri uri = Uri.parse(value);
            if ("file".equalsIgnoreCase(uri.getScheme())) return BitmapFactory.decodeFile(uri.getPath());
            return BitmapFactory.decodeFile(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
