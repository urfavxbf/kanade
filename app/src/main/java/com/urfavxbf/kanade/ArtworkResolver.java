package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkResolver {
    public interface Callback { void onArtworkLoaded(Bitmap bitmap); }
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private ArtworkResolver() {}
    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = resolveSync(appContext, song);
            if (bitmap != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onArtworkLoaded(bitmap));
        });
    }
    public static Bitmap resolveSync(Context context, AudioFile song) {
        if (song == null) return null;
        Bitmap bitmap = decodeArtworkUrl(song.getAlbumArtUri());
        if (bitmap != null) return bitmap;
        bitmap = extractEmbeddedArtwork(context, song.getUri());
        if (bitmap != null) return bitmap;
        return extractEmbeddedArtwork(context, song.getPath());
    }
    private static Bitmap extractEmbeddedArtwork(Context context, String source) {
        if (source == null || source.trim().isEmpty()) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Uri parsed = Uri.parse(source);
            if (parsed.getScheme() == null) retriever.setDataSource(source); else retriever.setDataSource(context, parsed);
            byte[] data = retriever.getEmbeddedPicture();
            return data == null || data.length == 0 ? null : BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) { return null; }
        finally { try { retriever.release(); } catch (Exception ignored) {} }
    }
    private static Bitmap decodeArtworkUrl(String artworkUri) {
        if (artworkUri == null || artworkUri.trim().isEmpty()) return null;
        String value = artworkUri.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) return null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(value).openConnection();
            connection.setConnectTimeout(8000); connection.setReadTimeout(10000); connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Kanade/1.0"); connection.connect();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            try (InputStream input = connection.getInputStream()) { return BitmapFactory.decodeStream(input); }
        } catch (Exception ignored) { return null; }
        finally { if (connection != null) connection.disconnect(); }
    }
}
