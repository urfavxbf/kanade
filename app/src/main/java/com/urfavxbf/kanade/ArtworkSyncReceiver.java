package com.urfavxbf.kanade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

public final class ArtworkSyncReceiver extends BroadcastReceiver {

    public static final String ACTION_ARTWORK_CHANGED = "com.urfavxbf.kanade.ACTION_ARTWORK_CHANGED";
    public static final String EXTRA_ARTWORK_URI = "com.urfavxbf.kanade.EXTRA_ARTWORK_URI";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
        String uri = intent.getStringExtra(MusicPlayerService.EXTRA_CURRENT_URI);
        if (uri == null || uri.trim().isEmpty()) return;
        AudioFile song = MusicRepository.findSongByUri(uri);
        if (song == null) return;
        ArtworkResolver.resolve(context, song, bitmap -> {
            ArtworkCache.put(uri, bitmap);
            Intent result = new Intent(ACTION_ARTWORK_CHANGED);
            result.setPackage(context.getPackageName());
            result.putExtra(MusicPlayerService.EXTRA_CURRENT_URI, uri);
            result.putExtra(EXTRA_ARTWORK_URI, song.getAlbumArtUri());
            context.sendBroadcast(result);
        });
    }
}
