package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;

public final class ArtworkPlaybackResolver {

    private ArtworkPlaybackResolver() {}

    public static void resolve(Context context, AudioFile song, Callback callback) {
        if (context == null || song == null || callback == null) return;
        Bitmap cached = ArtworkCache.get(song.getUri());
        if (cached != null) {
            callback.onResolved(cached);
            return;
        }
        ArtworkResolver.resolve(context, song, bitmap -> {
            ArtworkCache.put(song.getUri(), bitmap);
            callback.onResolved(bitmap);
        });
    }

    public interface Callback {
        void onResolved(Bitmap bitmap);
    }
}
