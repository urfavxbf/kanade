package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.ffmpegkit_maintained.ytdlp.YtDlp;
import dev.ffmpegkit_maintained.ytdlp.YtDlpException;
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest;
import dev.ffmpegkit_maintained.ytdlp.YtDlpResponse;

import org.json.JSONObject;

/** Resolves a direct YouTube audio stream off the main thread. */
public final class YouTubeNativeAudioResolver {

    private static final String TAG = "KanadeYTResolver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kanade-YouTube-Resolver");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile boolean initialized;

    private YouTubeNativeAudioResolver() {
    }

    public interface Callback {
        void onResolved(@NonNull String url, long durationMs);
        void onError(@NonNull String message);
    }

    public static void initialize(@NonNull Context context) {
        if (initialized) {
            return;
        }
        synchronized (YouTubeNativeAudioResolver.class) {
            if (initialized) {
                return;
            }
            try {
                YtDlp.init(context.getApplicationContext());
                initialized = true;
            } catch (YtDlpException exception) {
                Log.e(TAG, "yt-dlp initialization failed", exception);
            }
        }
    }

    public static void resolve(
            @NonNull Context context,
            @NonNull String videoId,
            @NonNull Callback callback) {
        initialize(context);
        if (!initialized) {
            callback.onError("YouTube resolver initialization failed");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
                YtDlpRequest request = new YtDlpRequest(watchUrl)
                        .addOption("--no-playlist")
                        .addOption("--no-warnings")
                        .addOption("--skip-download")
                        .addOption("--dump-single-json")
                        .addOption("-f", "bestaudio/best");

                YtDlpResponse response = YtDlp.execute(request, null);
                if (!response.isSuccess()) {
                    String error = response.getErrorOutput();
                    callback.onError(error == null || error.isEmpty()
                            ? "YouTube stream resolution failed"
                            : error.trim());
                    return;
                }

                JSONObject json = new JSONObject(response.getOutput());
                String url = json.optString("url", "");
                if (url.isEmpty()) {
                    callback.onError("YouTube returned no playable audio stream");
                    return;
                }

                long durationMs = Math.max(0L,
                        Math.round(json.optDouble("duration", 0d) * 1000d));
                callback.onResolved(url, durationMs);
            } catch (Exception exception) {
                Log.e(TAG, "YouTube stream resolution failed", exception);
                callback.onError(exception.getMessage() == null
                        ? "YouTube stream resolution failed"
                        : exception.getMessage());
            }
        });
    }
}
