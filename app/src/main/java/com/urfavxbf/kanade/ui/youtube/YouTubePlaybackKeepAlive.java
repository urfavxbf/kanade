package com.urfavxbf.kanade.ui.youtube;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.urfavxbf.kanade.R;

/**
 * Keeps the process-scoped YouTube WebView attached to the existing player host
 * while the Activity moves to the background. This is required because the
 * controller intentionally detaches the WebView during Activity stop.
 */
public final class YouTubePlaybackKeepAlive {

    private static boolean registered;

    private YouTubePlaybackKeepAlive() {
    }

    public static void install(@NonNull Application application) {
        if (registered) {
            return;
        }
        registered = true;

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(
                    @NonNull Activity activity,
                    Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (!YouTubePlaybackManager.isActive()
                        || !YouTubePlaybackManager.isPlaying()
                        || activity.isFinishing()) {
                    return;
                }

                View host = activity.findViewById(R.id.playerVisualContainer);
                if (host != null) {
                    host.post(() -> {
                        if (YouTubePlaybackManager.isActive()
                                && YouTubePlaybackManager.isPlaying()
                                && !activity.isFinishing()) {
                            YouTubePlaybackManager.attachTo(host);
                        }
                    });
                }
            }

            @Override
            public void onActivitySaveInstanceState(
                    @NonNull Activity activity,
                    @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}
