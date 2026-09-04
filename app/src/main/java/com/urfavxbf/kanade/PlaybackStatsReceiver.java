package com.urfavxbf.kanade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PlaybackStatsReceiver extends BroadcastReceiver {

    private static final Object LOCK = new Object();
    private static final String PREF_NAME = "kanade_playback_receiver";
    private static final String KEY_LAST_URI = "last_uri";
    private static final String KEY_PLAYING = "playing";
    private static final String KEY_LAST_USAGE = "last_usage";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String uri = intent.getStringExtra(MusicPlayerService.EXTRA_CURRENT_URI);
        boolean playing = intent.getBooleanExtra(MusicPlayerService.EXTRA_IS_PLAYING, false);
        if (uri == null || uri.trim().isEmpty()) {
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences state = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        synchronized (LOCK) {
            String previousUri = state.getString(KEY_LAST_URI, "");
            boolean previousPlaying = state.getBoolean(KEY_PLAYING, false);
            long now = System.currentTimeMillis();

            boolean newPlay = playing && (!previousPlaying || !uri.equals(previousUri));
            if (newPlay) {
                AudioFile song = findSong(appContext, uri);
                if (song != null) {
                    new PlaybackStatsManager(appContext).recordPlay(song);
                }
            }

            if (playing) {
                long lastUsage = state.getLong(KEY_LAST_USAGE, 0L);
                if (now - lastUsage >= 60_000L) {
                    PlaybackStatsManager statsManager = new PlaybackStatsManager(appContext);
                    statsManager.recordUsageMinute(now);
                    state.edit().putLong(KEY_LAST_USAGE, now).apply();
                }
            }

            state.edit()
                    .putString(KEY_LAST_URI, uri)
                    .putBoolean(KEY_PLAYING, playing)
                    .apply();
        }
    }

    private AudioFile findSong(Context context, String uri) {
        for (AudioFile song : new MusicRepository(context).getAllSongs()) {
            if (song != null && uri.equals(song.getUri())) {
                return song;
            }
        }
        return null;
    }
}
