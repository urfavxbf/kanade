package com.urfavxbf.kanade.ui.youtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/** Starts the YouTube foreground service when playback becomes active. */
public final class YouTubeMediaPlaybackReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !YouTubePlaybackManager.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        if (!intent.getBooleanExtra(YouTubePlaybackManager.EXTRA_PLAYING, false)) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        Intent serviceIntent = new Intent(applicationContext, YouTubeMediaService.class);
        ContextCompat.startForegroundService(applicationContext, serviceIntent);
    }
}
