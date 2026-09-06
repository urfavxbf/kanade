package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;

public final class ArtworkSync {

    private ArtworkSync() {}

    public static void resolve(Context context, AudioFile song, ArtworkCallback callback) {
        if (context == null || song == null || callback == null) return;
        Bitmap cached = ArtworkCache.get(song.getUri());
        if (cached != null) {
            callback.onArtwork(cached);
            return;
        }
        ArtworkResolver.resolve(context, song, bitmap -> {
            ArtworkCache.put(song.getUri(), bitmap);
            callback.onArtwork(bitmap);
        });
    }

    public interface ArtworkCallback {
        void onArtwork(Bitmap bitmap);
    }
}
