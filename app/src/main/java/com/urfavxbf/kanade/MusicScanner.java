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
import java.util.Locale;

public class MusicScanner {

    private static final String PREF_NAME =
            "kanade_music_cache";

    private static final String KEY_SONGS =
            "songs";

    /*
     * Increase this whenever the cached song structure
     * or metadata logic changes.
     */
    private static final String KEY_CACHE_VERSION =
            "cache_version";

    private static final int CACHE_VERSION =
            2;

    private final Context context;

    private final SharedPreferences preferences;

    private final MetadataResolver metadataResolver;

    public MusicScanner(Context context) {

        this.context =
                context.getApplicationContext();

        preferences =
                this.context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        metadataResolver =
                new MetadataResolver(
                        this.context
                );
    }

    public ArrayList<AudioFile> scanMusic() {

        /*
         * If the cache was created by an older version
         * of the scanner, discard it and scan again.
         */
        if (!isCacheValid()) {

            clearCache();
        }

        ArrayList<AudioFile> cachedSongs =
                loadCachedSongs();

        if (!cachedSongs.isEmpty()) {

            return cachedSongs;
        }

        ArrayList<AudioFile> songs =
                scanMediaStore();

        saveSongs(songs);

        return songs;
    }

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

        /*
         * Keep the original MediaStore information,
         * while also requesting richer metadata where
         * the platform provides it.
         */
        String[] projection = {

                MediaStore.Audio.Media._ID,

                MediaStore.Audio.Media.TITLE,

                MediaStore.Audio.Media.ARTIST,

                MediaStore.Audio.Media.ALBUM,

                MediaStore.Audio.Media.DATA,

                MediaStore.Audio.Media.DURATION,

                MediaStore.Audio.Media.DATE_ADDED,

                MediaStore.Audio.Media.IS_MUSIC,

                MediaStore.Audio.Media.MIME_TYPE,

                MediaStore.Audio.Media.ALBUM_ARTIST,

                MediaStore.Audio.Media.GENRE,

                MediaStore.Audio.Media.COMPOSER,

                MediaStore.Audio.Media.YEAR,

                MediaStore.Audio.Media.TRACK,

                MediaStore.Audio.Media.DISC_NUMBER
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

            cursor =
                    resolver.query(
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

            int isMusicIndex =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.IS_MUSIC
                    );

            int mimeTypeIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.MIME_TYPE
                    );

            int albumArtistIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.ALBUM_ARTIST
                    );

            int genreIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.GENRE
                    );

            int composerIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.COMPOSER
                    );

            int yearIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.YEAR
                    );

            int trackIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.TRACK
                    );

            int discIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.DISC_NUMBER
                    );

            while (cursor.moveToNext()) {

                long id =
                        cursor.getLong(
                                idIndex
                        );

                String title =
                        cursor.getString(
                                titleIndex
                        );

                String artist =
                        cursor.getString(
                                artistIndex
                        );

                String album =
                        cursor.getString(
                                albumIndex
                        );

                String path =
                        cursor.getString(
                                pathIndex
                        );

                long duration =
                        cursor.getLong(
                                durationIndex
                        );

                long dateAdded =
                        cursor.getLong(
                                dateAddedIndex
                        );

                int isMusic =
                        cursor.getInt(
                                isMusicIndex
                        );

                if (isMusic == 0) {

                    continue;
                }

                if (path == null ||
                        path.trim().isEmpty()) {

                    continue;
                }

                if (duration <= 0) {

                    continue;
                }

                /*
                 * MIME type validation.
                 */
                if (mimeTypeIndex >= 0) {

                    String mimeType =
                            cursor.getString(
                                    mimeTypeIndex
                            );

                    if (mimeType != null &&
                            !mimeType.trim().isEmpty()) {

                        mimeType =
                                mimeType
                                        .trim()
                                        .toLowerCase(
                                                Locale.US
                                        );

                        if (!mimeType.startsWith(
                                "audio/"
                        )) {

                            continue;
                        }
                    }
                }

                String lowerPath =
                        path
                                .trim()
                                .toLowerCase(
                                        Locale.US
                                );

                if (!isSupportedMusicFile(
                        lowerPath
                )) {

                    continue;
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
                 * First use richer MediaStore metadata
                 * when available.
                 */
                if (albumArtistIndex >= 0) {

                    audioFile.setAlbumArtist(
                            cleanOptional(
                                    cursor.getString(
                                            albumArtistIndex
                                    )
                            )
                    );
                }

                if (genreIndex >= 0) {

                    audioFile.setGenre(
                            cleanOptional(
                                    cursor.getString(
                                            genreIndex
                                    )
                            )
                    );
                }

                if (composerIndex >= 0) {

                    audioFile.setComposer(
                            cleanOptional(
                                    cursor.getString(
                                            composerIndex
                                    )
                            )
                    );
                }

                if (yearIndex >= 0) {

                    audioFile.setYear(
                            cleanOptional(
                                    cursor.getString(
                                            yearIndex
                                    )
                            )
                    );
                }

                if (trackIndex >= 0) {

                    audioFile.setTrackNumber(
                            cleanOptional(
                                    String.valueOf(
                                            cursor.getInt(
                                                    trackIndex
                                            )
                                    )
                            )
                    );
                }

                if (discIndex >= 0) {

                    audioFile.setDiscNumber(
                            cleanOptional(String.valueOf(cursor.getInt(discIndex))));
                }
                /*
                 * Resolve the actual embedded metadata from
                 * the audio file and normalize it.
                 *
                 * This also has a safe fallback to the
                 * MediaStore metadata above.
                 */
                metadataResolver.resolve(audioFile);

                songs.add(
                        audioFile
                );
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

    private boolean isSupportedMusicFile(
            String path) {

        if (path == null ||
                path.trim().isEmpty()) {

            return false;
        }

        String lowerPath =
                path
                        .trim()
                        .toLowerCase(
                                Locale.US
                        );

        return lowerPath.endsWith(".mp3")
                || lowerPath.endsWith(".m4a")
                || lowerPath.endsWith(".aac")
                || lowerPath.endsWith(".flac")
                || lowerPath.endsWith(".ogg")
                || lowerPath.endsWith(".oga")
                || lowerPath.endsWith(".opus")
                || lowerPath.endsWith(".wav")
                || lowerPath.endsWith(".amr")
                || lowerPath.endsWith(".3gp")
                || lowerPath.endsWith(".3ga")
                || lowerPath.endsWith(".wma")
                || lowerPath.endsWith(".webm");
    }

    private void saveSongs(
            ArrayList<AudioFile> songs) {

        if (songs == null) {

            songs =
                    new ArrayList<>();
        }

        try {

            JSONArray jsonArray =
                    new JSONArray();

            for (AudioFile song : songs) {

                if (song == null) {

                    continue;
                }

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
                        "albumArtist",
                        song.getAlbumArtist()
                );

                object.put(
                        "genre",
                        song.getGenre()
                );

                object.put(
                        "composer",
                        song.getComposer()
                );

                object.put(
                        "year",
                        song.getYear()
                );

                object.put(
                        "trackNumber",
                        song.getTrackNumber()
                );

                object.put(
                        "discNumber",
                        song.getDiscNumber()
                );

                /*
                 * Album artwork is intentionally not
                 * persisted here.
                 */
                object.put(
                        "albumArtUri",
                        JSONObject.NULL
                );

                jsonArray.put(
                        object
                );
            }

            preferences.edit()
                    .putString(
                            KEY_SONGS,
                            jsonArray.toString()
                    )
                    .putInt(
                            KEY_CACHE_VERSION,
                            CACHE_VERSION
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
                        jsonArray.getJSONObject(
                                i
                        );

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

                audioFile.setAlbumArtist(
                        object.optString(
                                "albumArtist",
                                null
                        )
                );

                audioFile.setGenre(
                        object.optString(
                                "genre",
                                null
                        )
                );

                audioFile.setComposer(
                        object.optString(
                                "composer",
                                null
                        )
                );

                audioFile.setYear(
                        object.optString(
                                "year",
                                null
                        )
                );

                audioFile.setTrackNumber(
                        object.optString(
                                "trackNumber",
                                null
                        )
                );

                audioFile.setDiscNumber(
                        object.optString(
                                "discNumber",
                                null
                        )
                );

                if (path == null ||
                        path.trim().isEmpty()) {

                    continue;
                }

                if (duration <= 0) {

                    continue;
                }

                if (!isSupportedMusicFile(
                        path
                )) {

                    continue;
                }

                songs.add(
                        audioFile
                );
            }

        } catch (Exception e) {

            preferences.edit()
                    .remove(KEY_SONGS)
                    .remove(KEY_CACHE_VERSION)
                    .apply();

            songs.clear();
        }

        return songs;
    }

    private boolean isCacheValid() {

        int version =
                preferences.getInt(
                        KEY_CACHE_VERSION,
                        0
                );

        return version ==
                CACHE_VERSION;
    }

    private String cleanOptional(
            String value) {

        if (value == null) {

            return null;
        }

        String result =
                value.trim();

        if (result.isEmpty()) {

            return null;
        }

        return result;
    }

    public void clearCache() {

        preferences.edit()
                .remove(KEY_SONGS)
                .remove(KEY_CACHE_VERSION)
                .apply();
    }
}