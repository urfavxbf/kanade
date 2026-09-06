package com.urfavxbf.kanade;

import android.graphics.Bitmap;

public final class ArtworkMetadataProvider {

    private ArtworkMetadataProvider() {}

    public static Bitmap getCached(String uri) {
        return ArtworkCache.get(uri);
    }
}
