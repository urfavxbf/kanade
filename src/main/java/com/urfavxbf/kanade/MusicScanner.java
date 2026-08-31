package com.urfavxbf.kanade;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MusicScanner {

    private static final String PREF_NAME =
            "kanade_music_cache";

    private static final String KEY_SONGS =
            "songs";

    private final Context context;

    private final SharedPreferences preferences;

    public MusicScanner(Context context) {

        this.context =
                context.getApplicationContext();

        preferences =
                this.context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    public ArrayList<AudioFile> scanMusic() {

        ArrayList<AudioFile> cachedSongs =
                loadCachedSongs();

        /*
         * Cache exists.
         *
         * Do NOT scan MediaStore again.
         */
        if (!cachedSongs.isEmpty()) {
            return cachedSongs;
        }

        /*
         * No cache yet.
         * Perform the first MediaStore scan.
         */
        ArrayList<AudioFile> songs =
                scanMediaStore();

        /*
         * Save the result locally.
         */
        saveSongs(songs);

        return songs;
    }

    /*
     * Force a fresh MediaStore scan.
     *
     * This will be used later by Pull-To-Refresh
     * or a manual "Rescan Music" option.
     */
    public ArrayList<AudioFile> refreshMusic() {

        ArrayList<AudioFile> songs =
                scanMediaStore();

        saveSongs(songs);

        return songs;
    }

    private ArrayList<AudioFile> scanMediaStore() {

        ArrayList<AudioFile> songs =
                new ArrayList<>();

        ContentResolver resolver =
                context.getContentResolver();

        Uri collection;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q) {

            collection =
                    MediaStore.Audio.Media.getContentUri(
                            MediaStore.VOLUME_EXTERNAL
                    );

        } else {

            collection =
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.MIME_TYPE
        };

        String selection =
                MediaStore.Audio.Media.IS_MUSIC
                        + " != 0"
                        + " AND "
                        + MediaStore.Audio.Media.DURATION
                        + " > 0";

        String sortOrder =
                MediaStore.Audio.Media.TITLE
                        + " COLLATE NOCASE ASC";

        Cursor cursor = null;

        try {

            cursor = resolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
            );

            if (cursor == null) {
                return songs;
            }

            int idIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                    );

            int titleIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                    );

            int artistIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                    );

            int albumIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM
                    );

            int albumIdIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM_ID
                    );

            int pathIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DATA
                    );

            int durationIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                    );

            int dateAddedIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DATE_ADDED
                    );

            int mimeTypeIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.MIME_TYPE
                    );

            while (cursor.moveToNext()) {

                long id =
                        cursor.getLong(idIndex);

                String title =
                        cursor.getString(titleIndex);

                String artist =
                        cursor.getString(artistIndex);

                String album =
                        cursor.getString(albumIndex);

                long albumId =
                        cursor.getLong(albumIdIndex);

                String path =
                        cursor.getString(pathIndex);

                long duration =
                        cursor.getLong(durationIndex);

                long dateAdded =
                        cursor.getLong(dateAddedIndex);

                /*
                 * Ignore invalid paths.
                 */
                if (path == null ||
                        path.trim().isEmpty()) {

                    continue;
                }

                /*
                 * Ignore invalid duration.
                 */
                if (duration <= 0) {
                    continue;
                }

                /*
                 * Ignore entries explicitly marked
                 * as non-audio MIME types.
                 */
                if (mimeTypeIndex >= 0) {

                    String mimeType =
                            cursor.getString(
                                    mimeTypeIndex
                            );

                    if (mimeType != null &&
                            !mimeType.trim().isEmpty() &&
                            !mimeType
                                    .toLowerCase()
                                    .startsWith("audio/")) {

                        continue;
                    }
                }

                Uri contentUri =
                        ContentUris.withAppendedId(
                                collection,
                                id
                        );

                String uri =
                        contentUri.toString();

                AudioFile audioFile =
                        new AudioFile(
                                id,
                                title,
                                artist,
                                album,
                                uri,
                                path,
                                duration,
                                dateAdded
                        );

                /*
                 * Album artwork URI.
                 */
                if (albumId > 0) {

                    Uri albumArtUri =
                            ContentUris.withAppendedId(
                                    MediaStore.Audio.Albums
                                            .EXTERNAL_CONTENT_URI,
                                    albumId
                            );

                    audioFile.setAlbumArtUri(
                            albumArtUri.toString()
                    );
                }

                songs.add(audioFile);
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return songs;
    }

    private void saveSongs(
            ArrayList<AudioFile> songs) {

        try {

            JSONArray jsonArray =
                    new JSONArray();

            for (AudioFile song : songs) {

                JSONObject object =
                        new JSONObject();

                object.put(
                        "id",
                        song.getId()
                );

                object.put(
                        "title",
                        song.getTitle()
                );

                object.put(
                        "artist",
                        song.getArtist()
                );

                object.put(
                        "album",
                        song.getAlbum()
                );

                object.put(
                        "uri",
                        song.getUri()
                );

                object.put(
                        "path",
                        song.getPath()
                );

                object.put(
                        "duration",
                        song.getDuration()
                );

                object.put(
                        "dateAdded",
                        song.getDateAdded()
                );

                object.put(
                        "albumArtUri",
                        song.getAlbumArtUri()
                );

                jsonArray.put(object);
            }

            preferences.edit()
                    .putString(
                            KEY_SONGS,
                            jsonArray.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private ArrayList<AudioFile> loadCachedSongs() {

        ArrayList<AudioFile> songs =
                new ArrayList<>();

        String json =
                preferences.getString(
                        KEY_SONGS,
                        null
                );

        if (json == null ||
                json.trim().isEmpty()) {

            return songs;
        }

        try {

            JSONArray jsonArray =
                    new JSONArray(json);

            for (int i = 0;
                    i < jsonArray.length();
                    i++) {

                JSONObject object =
                        jsonArray.getJSONObject(i);

                long id =
                        object.optLong(
                                "id"
                        );

                String title =
                        object.optString(
                                "title",
                                null
                        );

                String artist =
                        object.optString(
                                "artist",
                                null
                        );

                String album =
                        object.optString(
                                "album",
                                null
                        );

                String uri =
                        object.optString(
                                "uri",
                                null
                        );

                String path =
                        object.optString(
                                "path",
                                null
                        );

                long duration =
                        object.optLong(
                                "duration"
                        );

                long dateAdded =
                        object.optLong(
                                "dateAdded"
                        );

                String albumArtUri =
                        object.optString(
                                "albumArtUri",
                                null
                        );

                AudioFile audioFile =
                        new AudioFile(
                                id,
                                title,
                                artist,
                                album,
                                uri,
                                path,
                                duration,
                                dateAdded
                        );

                if (albumArtUri != null &&
                        !albumArtUri.equals("null") &&
                        !albumArtUri.trim().isEmpty()) {

                    audioFile.setAlbumArtUri(
                            albumArtUri
                    );
                }

                songs.add(audioFile);
            }

        } catch (Exception e) {

            /*
             * If cache is corrupted,
             * clear it so the next call
             * performs a fresh scan.
             */
            preferences.edit()
                    .remove(KEY_SONGS)
                    .apply();

            songs.clear();
        }

        return songs;
    }

    public void clearCache() {

        preferences.edit()
                .remove(KEY_SONGS)
                .apply();
    }
}