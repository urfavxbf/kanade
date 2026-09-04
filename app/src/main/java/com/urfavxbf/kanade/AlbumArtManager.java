package com.urfavxbf.kanade;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AlbumArtManager {

    private static final String CACHE_DIR = "album_art";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int MAX_DECODE_DIMENSION = 1024;

    private final Context context;
    private final File cacheDirectory;

    public AlbumArtManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }

        this.context = context.getApplicationContext();
        this.cacheDirectory = new File(
                this.context.getFilesDir(),
                CACHE_DIR
        );

        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }
    }

    public Uri getCachedAlbumArtUri(AudioFile song) {
        File file = getCachedAlbumArtFile(song);
        return file.exists() ? Uri.fromFile(file) : null;
    }

    public File getCachedAlbumArtFile(AudioFile song) {
        return new File(
                cacheDirectory,
                createCacheKey(song) + ".jpg"
        );
    }

    public boolean hasCachedAlbumArt(AudioFile song) {
        File file = getCachedAlbumArtFile(song);
        return file.exists() && file.length() > 0;
    }

    public String downloadAndCache(
            AudioFile song,
            String imageUrl) {

        if (song == null || isEmpty(imageUrl)) {
            return null;
        }

        HttpURLConnection connection = null;
        File tempFile = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Kanade Music Player/1.0");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }

            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE) {
                return null;
            }

            if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
                return null;
            }

            File targetFile = getCachedAlbumArtFile(song);
            tempFile = new File(
                    targetFile.getAbsolutePath() + ".tmp-" + System.nanoTime()
            );

            try (InputStream input = new BufferedInputStream(
                    connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[8192];
                int read;
                long total = 0;

                while ((read = input.read(buffer)) != -1) {
                    total += read;

                    if (total > MAX_FILE_SIZE) {
                        return null;
                    }

                    output.write(buffer, 0, read);
                }

                output.getFD().sync();
            }

            if (!isValidImage(tempFile)) {
                return null;
            }

            if (targetFile.exists() && !targetFile.delete()) {
                return null;
            }

            if (!tempFile.renameTo(targetFile)) {
                return null;
            }

            return Uri.fromFile(targetFile).toString();

        } catch (Exception e) {
            return null;

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public Bitmap loadCachedBitmap(AudioFile song) {
        File file = getCachedAlbumArtFile(song);

        if (!file.exists() || file.length() <= 0) {
            return null;
        }

        String path = file.getAbsolutePath();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight
        );
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            return BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError error) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean deleteCachedAlbumArt(AudioFile song) {
        if (song == null) {
            return false;
        }

        File file = getCachedAlbumArtFile(song);
        return !file.exists() || file.delete();
    }

    public void clearAllCachedAlbumArt() {
        if (!cacheDirectory.exists()) {
            return;
        }

        File[] files = cacheDirectory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.isFile()) {
                file.delete();
            }
        }
    }

    private boolean isValidImage(File file) {
        if (file == null || !file.exists() || file.length() <= 0) {
            return false;
        }

        BitmapFactory.Options options =
                new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(
                file.getAbsolutePath(),
                options
        );

        return options.outWidth > 0 && options.outHeight > 0;
    }

    private int calculateInSampleSize(int width, int height) {
        int sampleSize = 1;
        int largestDimension = Math.max(width, height);

        while (largestDimension / sampleSize > MAX_DECODE_DIMENSION) {
            if (sampleSize > Integer.MAX_VALUE / 2) {
                break;
            }
            sampleSize *= 2;
        }

        return sampleSize;
    }

    private String createCacheKey(AudioFile song) {
        String source = null;

        if (song != null) {
            source = song.getUri();

            if (isEmpty(source)) {
                source = song.getPath();
            }

            if (isEmpty(source)) {
                source = safe(song.getTitle()) + "|"
                        + safe(song.getArtist()) + "|"
                        + safe(song.getAlbum());
            }
        }

        if (isEmpty(source)) {
            source = "unknown";
        }

        return sha256(source);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder builder = new StringBuilder(bytes.length * 2);
            final char[] hex = "0123456789abcdef".toCharArray();

            for (byte b : bytes) {
                int unsigned = b & 0xff;
                builder.append(hex[unsigned >>> 4]);
                builder.append(hex[unsigned & 0x0f]);
            }

            return builder.toString();

        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
