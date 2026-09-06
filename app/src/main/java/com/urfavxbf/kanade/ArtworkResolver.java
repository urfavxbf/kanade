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

    public interface Callback {
        void onArtworkResolved(Bitmap bitmap);
    }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private ArtworkResolver() {}

    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = null;
            String artUri = song.getAlbumArtUri();
            if (artUri != null && !artUri.trim().isEmpty()) {
                bitmap = loadRemoteOrUri(appContext, artUri);
            }
            if (bitmap == null) {
                bitmap = loadEmbeddedArtwork(appContext, song.getUri());
            }
            if (bitmap != null) {
                Bitmap result = bitmap;
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.post(() -> callback.onArtworkResolved(result));
            }
        });
    }

    private static Bitmap loadEmbeddedArtwork(Context context, String uri) {
        if (uri == null || uri.trim().isEmpty()) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (uri.startsWith("http://") || uri.startsWith("https://")) return null;
            retriever.setDataSource(context, Uri.parse(uri));
            byte[] data = retriever.getEmbeddedPicture();
            return decode(data);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private static Bitmap loadRemoteOrUri(Context context, String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                } finally {
                    connection.disconnect();
                }
            }
            return BitmapFactory.decodeFile(Uri.parse(value).getPath());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap decode(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) {
            return null;
        }
    }
}
