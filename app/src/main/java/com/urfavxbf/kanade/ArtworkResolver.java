package com.urfavxbf.kanade;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkResolver {

    public interface Callback {
        void onArtworkResolved(Bitmap bitmap);
    }

    private static final String CHANNEL_ID = "kanade_music_playback";
    private static final int NOTIFICATION_ID = 1001;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object INIT_LOCK = new Object();

    private static volatile boolean initialized;
    private static volatile Bitmap currentArtwork;
    private static volatile String currentArtworkUri;
    private static volatile MainActivity mainActivity;

    private ArtworkResolver() {}

    public static void initialize(Context context) {
        if (context == null || initialized) return;
        synchronized (INIT_LOCK) {
            if (initialized) return;
            Context appContext = context.getApplicationContext();

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                    String uri = intent.getStringExtra(MusicPlayerService.EXTRA_CURRENT_URI);
                    if (uri == null || uri.trim().isEmpty()) return;
                    AudioFile song = findSong(appContext, uri);
                    if (song != null) resolveAndSync(appContext, song);
                }
            };

            IntentFilter filter = new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter);
            }

            if (appContext instanceof Application) {
                ((Application) appContext).registerActivityLifecycleCallbacks(
                        new Application.ActivityLifecycleCallbacks() {
                            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { trackActivity(activity); }
                            @Override public void onActivityStarted(@NonNull Activity activity) { trackActivity(activity); applyCurrentArtwork(activity); }
                            @Override public void onActivityResumed(@NonNull Activity activity) { trackActivity(activity); applyCurrentArtwork(activity); }
                            @Override public void onActivityPaused(@NonNull Activity activity) {}
                            @Override public void onActivityStopped(@NonNull Activity activity) {}
                            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) {}
                            @Override public void onActivityDestroyed(@NonNull Activity activity) { if (activity == mainActivity) mainActivity = null; }
                        }
                );
            }
            initialized = true;
        }
    }

    private static void trackActivity(Activity activity) {
        if (activity instanceof MainActivity) mainActivity = (MainActivity) activity;
    }

    private static void applyCurrentArtwork(Activity activity) {
        Bitmap bitmap = currentArtwork;
        if (bitmap == null || bitmap.isRecycled()) return;
        applyArtwork(activity, bitmap);
    }

    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = resolveBlocking(appContext, song);
            if (bitmap != null) MAIN_HANDLER.post(() -> callback.onArtworkResolved(bitmap));
        });
    }

    private static void resolveAndSync(Context context, AudioFile song) {
        String uri = song.getUri();
        Bitmap cached = currentArtwork;
        if (uri != null && uri.equals(currentArtworkUri) && cached != null && !cached.isRecycled()) {
            sync(context, song, cached);
            return;
        }
        resolve(context, song, bitmap -> {
            currentArtworkUri = uri;
            currentArtwork = bitmap;
            sync(context, song, bitmap);
        });
    }

    private static Bitmap resolveBlocking(Context context, AudioFile song) {
        String artworkUri = song.getAlbumArtUri();
        if (artworkUri != null && !artworkUri.trim().isEmpty()) {
            Bitmap remote = loadRemoteOrUri(context, artworkUri);
            if (remote != null) return remote;
        }
        return loadEmbeddedArtwork(context, song.getUri());
    }

    private static void sync(Context context, AudioFile song, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return;
        updateMediaSession(song, bitmap);
        updateNotification(context, song, bitmap);
        Activity activity = mainActivity;
        if (activity != null) applyArtwork(activity, bitmap);
    }

    private static void updateMediaSession(AudioFile song, Bitmap bitmap) {
        MusicPlayerService service = findServiceInstance();
        if (service == null) return;
        try {
            Field field = MusicPlayerService.class.getDeclaredField("mediaSession");
            field.setAccessible(true);
            Object value = field.get(service);
            if (!(value instanceof MediaSessionCompat)) return;
            MediaSessionCompat session = (MediaSessionCompat) value;
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, safe(song.getTitle(), "Unknown title"))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, safe(song.getArtist(), "Unknown artist"))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, safe(song.getAlbum(), "Unknown album"))
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
            if (song.getDuration() > 0) builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());
            session.setMetadata(builder.build());
        } catch (Exception ignored) {}
    }

    private static MusicPlayerService findServiceInstance() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentThread = activityThread.getDeclaredMethod("currentActivityThread");
            currentThread.setAccessible(true);
            Object thread = currentThread.invoke(null);
            Field servicesField = activityThread.getDeclaredField("mServices");
            servicesField.setAccessible(true);
            Object value = servicesField.get(thread);
            if (value instanceof Map) {
                for (Object service : ((Map<?, ?>) value).values()) {
                    if (service instanceof MusicPlayerService) return (MusicPlayerService) service;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void updateNotification(Context context, AudioFile song, Bitmap bitmap) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            Intent content = new Intent(context, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(safe(song.getTitle(), "Unknown title"))
                    .setContentText(safe(song.getArtist(), "Unknown artist"))
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setContentIntent(PendingIntent.getActivity(context, 500, content, pendingIntentFlags()))
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", servicePendingIntent(context, MusicPlayerService.ACTION_PREVIOUS, 501)))
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", servicePendingIntent(context, MusicPlayerService.ACTION_PAUSE, 502)))
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", servicePendingIntent(context, MusicPlayerService.ACTION_NEXT, 503)))
                    .setLargeIcon(bitmap);
            MediaSessionCompat session = getMediaSession();
            if (session != null) builder.setStyle(new MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {}
    }

    private static MediaSessionCompat getMediaSession() {
        MusicPlayerService service = findServiceInstance();
        if (service == null) return null;
        try {
            Field field = MusicPlayerService.class.getDeclaredField("mediaSession");
            field.setAccessible(true);
            Object value = field.get(service);
            return value instanceof MediaSessionCompat ? (MediaSessionCompat) value : null;
        } catch (Exception ignored) { return null; }
    }

    private static void applyArtwork(Activity activity, Bitmap bitmap) {
        if (activity == null || bitmap == null || bitmap.isRecycled()) return;
        MAIN_HANDLER.post(() -> {
            try {
                View view = activity.findViewById(R.id.miniAlbumArt);
                if (view instanceof ImageView) ((ImageView) view).setImageBitmap(bitmap);
            } catch (Exception ignored) {}
        });
    }

    private static Bitmap loadEmbeddedArtwork(Context context, String uri) {
        if (uri == null || uri.trim().isEmpty() || uri.startsWith("http://") || uri.startsWith("https://")) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, Uri.parse(uri));
            return decode(retriever.getEmbeddedPicture());
        } catch (Exception ignored) { return null; }
        finally { try { retriever.release(); } catch (Exception ignored) {} }
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
                try (InputStream input = connection.getInputStream()) { return BitmapFactory.decodeStream(input); }
                finally { connection.disconnect(); }
            }
            return BitmapFactory.decodeFile(Uri.parse(value).getPath());
        } catch (Exception ignored) { return null; }
    }

    private static Bitmap decode(byte[] data) {
        if (data == null || data.length == 0) return null;
        try { return BitmapFactory.decodeByteArray(data, 0, data.length); } catch (Exception ignored) { return null; }
    }

    private static AudioFile findSong(Context context, String uri) {
        try {
            ArrayList<AudioFile> songs = new MusicRepository(context).getAllSongs();
            for (AudioFile song : songs) if (song != null && uri.equals(song.getUri())) return song;
        } catch (Exception ignored) {}
        return null;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static PendingIntent servicePendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MusicPlayerService.class).setAction(action);
        return PendingIntent.getService(context, requestCode, intent, pendingIntentFlags());
    }

    private static int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }
}
