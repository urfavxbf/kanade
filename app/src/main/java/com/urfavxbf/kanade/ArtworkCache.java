package com.urfavxbf.kanade;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ArtworkCache {

    private static final int MAX_ENTRIES = 40;
    private static final Map<String, Bitmap> CACHE = new LinkedHashMap<String, Bitmap>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private ArtworkCache() {}

    public static synchronized Bitmap get(String key) {
        return key == null ? null : CACHE.get(key);
    }

    public static synchronized void put(String key, Bitmap bitmap) {
        if (key == null || key.trim().isEmpty() || bitmap == null) return;
        CACHE.put(key, bitmap);
    }
}
