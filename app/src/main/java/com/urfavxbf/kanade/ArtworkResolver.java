package com.urfavxbf.kanade;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkResolver {
    public interface Callback { void onArtworkResolved(Bitmap bitmap); }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final Map<String, Bitmap> CACHE = new java.util.HashMap<>();
    private static volatile String currentUri;
    private static volatile Bitmap currentBitmap;
    private static volatile boolean initialized;
    private static Application application;
    private static Activity activeActivity;
    private static BroadcastReceiver stateReceiver;

    private ArtworkResolver() {}

    public static void initialize(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (initialized) return;
            initialized = true;
            application = appContext instanceof Application
                    ? (Application) appContext
                    : null;
        }

        if (appContext instanceof Application) {
            Application app = (Application) appContext;
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(@NonNull Activity activity, android.os.Bundle state) {}
                @Override public void onActivityStarted(@NonNull Activity activity) {
                    activeActivity = activity;
                    refreshActiveActivity();
                }
                @Override public void onActivityResumed(@NonNull Activity activity) {
                    activeActivity = activity;
                    refreshActiveActivity();
                }
                @Override public void onActivityPaused(@NonNull Activity activity) {}
                @Override public void onActivityStopped(@NonNull Activity activity) {
                    if (activeActivity == activity) activeActivity = null;
                }
                @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull android.os.Bundle outState) {}
                @Override public void onActivityDestroyed(@NonNull Activity activity) {
                    if (activeActivity == activity) activeActivity = null;
                }
            });

            stateReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    if (intent == null || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                    String uri = intent.getStringExtra(MusicPlayerService.EXTRA_CURRENT_URI);
                    if (uri == null || uri.trim().isEmpty()) return;
                    AudioFile song = MusicRepository.findSongByUri(uri);
                    if (song == null) return;
                    resolve(context, song, bitmap -> {
                        if (uri.equals(currentUri)) applyToActivePlayer(bitmap, uri);
                    });
                }
            };

            IntentFilter filter = new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                app.registerReceiver(stateReceiver, filter);
            }
        }
    }

    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Context appContext = context.getApplicationContext();
        String uri = song.getUri();
        if (uri != null && uri.equals(currentUri) && currentBitmap != null && !currentBitmap.isRecycled()) {
            MAIN_HANDLER.post(() -> callback.onArtworkResolved(currentBitmap));
            return;
        }
        Bitmap cached = uri == null ? null : getCached(uri);
        if (cached != null) {
            currentUri = uri;
            currentBitmap = cached;
            MAIN_HANDLER.post(() -> callback.onArtworkResolved(cached));
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bitmap = resolveBlocking(appContext, song);
            if (bitmap == null) return;
            if (uri != null) {
                synchronized (LOCK) {
                    CACHE.put(uri, bitmap);
                }
            }
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

    private static Bitmap getCached(String uri) {
        synchronized (LOCK) {
            Bitmap bitmap = CACHE.get(uri);
            return bitmap != null && !bitmap.isRecycled() ? bitmap : null;
        }
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

    private static void refreshActiveActivity() {
        final String uri = currentUri;
        final Bitmap bitmap = currentBitmap;
        if (uri == null || bitmap == null || bitmap.isRecycled()) return;
        MAIN_HANDLER.post(() -> applyToActivePlayer(bitmap, uri));
    }

    private static void applyToActivePlayer(Bitmap bitmap, String uri) {
        Activity activity = activeActivity;
        if (activity == null || activity.isFinishing() || bitmap == null || bitmap.isRecycled()) return;

        ImageView fullPlayer = activity.findViewById(R.id.fullPlayerAlbumArt);
        if (fullPlayer != null) fullPlayer.setImageBitmap(bitmap);

        ImageView miniPlayer = activity.findViewById(R.id.miniAlbumArt);
        if (miniPlayer != null) miniPlayer.setImageBitmap(bitmap);
    }
}