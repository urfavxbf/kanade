package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;

public final class ArtworkSync {
    private ArtworkSync() {}
    public static void load(Context context, AudioFile song, ArtworkCallback callback) {
        if (song == null || callback == null) return;
        String key = song.getAlbumArtUri();
        if (key == null || key.trim().isEmpty()) key = song.getUri();
        Bitmap cached = ArtworkCache.get(key);
        if (cached != null) { callback.onLoaded(cached); return; }
        final String cacheKey = key;
        ArtworkResolver.resolve(context, song, bitmap -> {
            if (bitmap != null && cacheKey != null) ArtworkCache.put(cacheKey, bitmap);
            callback.onLoaded(bitmap);
        });
    }
    public interface ArtworkCallback { void onLoaded(Bitmap bitmap); }
}
