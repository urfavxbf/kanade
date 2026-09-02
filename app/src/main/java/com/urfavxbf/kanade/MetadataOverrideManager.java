package com.urfavxbf.kanade;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Stores user-edited metadata locally without modifying the original audio file.
 * Overrides are keyed by the MediaStore content URI.
 */
public class MetadataOverrideManager {

    private static final String PREF_NAME =
            "kanade_metadata_overrides";

    private static final String KEY_OVERRIDES =
            "overrides";

    private final SharedPreferences preferences;

    public MetadataOverrideManager(Context context) {
        Context appContext = context.getApplicationContext();

        preferences = appContext.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public boolean save(AudioFile song) {
        if (song == null || isEmpty(song.getUri())) {
            return false;
        }

        try {
            JSONObject allOverrides = getAllOverrides();
            JSONObject override = new JSONObject();

            putNullable(override, "title", song.getTitle());
            putNullable(override, "artist", song.getArtist());
            putNullable(override, "album", song.getAlbum());
            putNullable(override, "albumArtist", song.getAlbumArtist());
            putNullable(override, "genre", song.getGenre());
            putNullable(override, "composer", song.getComposer());
            putNullable(override, "year", song.getYear());
            putNullable(override, "trackNumber", song.getTrackNumber());
            putNullable(override, "discNumber", song.getDiscNumber());

            allOverrides.put(song.getUri(), override);

            preferences.edit()
                    .putString(
                            KEY_OVERRIDES,
                            allOverrides.toString()
                    )
                    .apply();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void apply(AudioFile song) {
        if (song == null || isEmpty(song.getUri())) {
            return;
        }

        try {
            JSONObject allOverrides = getAllOverrides();
            JSONObject override = allOverrides.optJSONObject(
                    song.getUri()
            );

            if (override == null) {
                return;
            }

            song.setTitle(
                    readNullable(override, "title", song.getTitle())
            );
            song.setArtist(
                    readNullable(override, "artist", song.getArtist())
            );
            song.setAlbum(
                    readNullable(override, "album", song.getAlbum())
            );
            song.setAlbumArtist(
                    readNullable(
                            override,
                            "albumArtist",
                            song.getAlbumArtist()
                    )
            );
            song.setGenre(
                    readNullable(override, "genre", song.getGenre())
            );
            song.setComposer(
                    readNullable(override, "composer", song.getComposer())
            );
            song.setYear(
                    readNullable(override, "year", song.getYear())
            );
            song.setTrackNumber(
                    readNullable(
                            override,
                            "trackNumber",
                            song.getTrackNumber()
                    )
            );
            song.setDiscNumber(
                    readNullable(
                            override,
                            "discNumber",
                            song.getDiscNumber()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasOverride(String uri) {
        if (isEmpty(uri)) {
            return false;
        }

        return getAllOverrides().has(uri);
    }

    public boolean remove(String uri) {
        if (isEmpty(uri)) {
            return false;
        }

        try {
            JSONObject allOverrides = getAllOverrides();

            if (!allOverrides.has(uri)) {
                return false;
            }

            allOverrides.remove(uri);

            preferences.edit()
                    .putString(
                            KEY_OVERRIDES,
                            allOverrides.toString()
                    )
                    .apply();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearAll() {
        preferences.edit()
                .remove(KEY_OVERRIDES)
                .apply();
    }

    private JSONObject getAllOverrides() {
        String json = preferences.getString(
                KEY_OVERRIDES,
                null
        );

        if (isEmpty(json)) {
            return new JSONObject();
        }

        try {
            return new JSONObject(json);
        } catch (Exception e) {
            preferences.edit()
                    .remove(KEY_OVERRIDES)
                    .apply();

            return new JSONObject();
        }
    }

    private void putNullable(
            JSONObject object,
            String key,
            String value) throws Exception {

        if (value == null) {
            object.put(key, JSONObject.NULL);
            return;
        }

        object.put(key, value);
    }

    private String readNullable(
            JSONObject object,
            String key,
            String fallback) {

        if (!object.has(key) || object.isNull(key)) {
            return fallback;
        }

        String value = object.optString(key, null);

        if (value == null) {
            return fallback;
        }

        return value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
