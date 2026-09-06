package com.urfavxbf.kanade;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ArtworkNetwork {

    private ArtworkNetwork() {}

    public static Bitmap load(String uri) {
        if (uri == null || uri.trim().isEmpty()) return null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            } finally {
                connection.disconnect();
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
