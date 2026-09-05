package com.urfavxbf.kanade.ui.youtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

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
        YouTubePlaybackKeepAlive.install((android.app.Application) applicationContext);

        Intent serviceIntent = new Intent(applicationContext, YouTubeMediaService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent);
        } else {
            applicationContext.startService(serviceIntent);
        }
    }
}
