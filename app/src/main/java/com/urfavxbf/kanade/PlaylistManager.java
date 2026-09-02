package com.urfavxbf.kanade;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PlaylistManager {

    private static final String PREF_NAME =
            "kanade_playlists";

    private static final String KEY_PLAYLISTS =
            "playlists";

    public static final String FAVORITES_PLAYLIST =
            "Favorites";

    private static final String OLD_PREF_NAME =
            "kanade_favorites";

    private static final String OLD_KEY_FAVORITES =
            "favorites";

    private final SharedPreferences preferences;
    private final SharedPreferences oldFavoritePreferences;

    public PlaylistManager(Context context) {

        Context appContext =
                context.getApplicationContext();

        preferences =
                appContext.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        oldFavoritePreferences =
                appContext.getSharedPreferences(
                        OLD_PREF_NAME,
                        Context.MODE_PRIVATE
                );

        migrateOldFavorites();
    }

    /*
     * ---------------------------------------------------------
     * PLAYLIST CREATION
     * ---------------------------------------------------------
     */

    public boolean createPlaylist(String name) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        String cleanName =
                name.trim();

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                cleanName
        )) {

            return false;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        if (containsPlaylistIgnoreCase(
                playlists,
                cleanName
        )) {

            return false;
        }

        playlists.put(
                cleanName,
                new LinkedHashSet<>()
        );

        savePlaylistMap(
                playlists
        );

        return true;
    }

    public boolean deletePlaylist(String name) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                name.trim()
        )) {

            return false;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        name.trim()
                );

        if (actualName == null) {
            return false;
        }

        playlists.remove(
                actualName
        );

        savePlaylistMap(
                playlists
        );

        return true;
    }

    public boolean renamePlaylist(
            String oldName,
            String newName) {

        if (oldName == null ||
                newName == null) {

            return false;
        }

        String oldClean =
                oldName.trim();

        String newClean =
                newName.trim();

        if (oldClean.isEmpty() ||
                newClean.isEmpty()) {

            return false;
        }

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                oldClean
        )) {

            return false;
        }

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                newClean
        )) {

            return false;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualOldName =
                findActualPlaylistName(
                        playlists,
                        oldClean
                );

        if (actualOldName == null) {
            return false;
        }

        if (containsPlaylistIgnoreCase(
                playlists,
                newClean
        )) {

            return false;
        }

        LinkedHashSet<String> songs =
                playlists.remove(
                        actualOldName
                );

        playlists.put(
                newClean,
                songs == null
                        ? new LinkedHashSet<>()
                        : songs
        );

        savePlaylistMap(
                playlists
        );

        return true;
    }

    /*
     * ---------------------------------------------------------
     * PLAYLIST QUERIES
     * ---------------------------------------------------------
     */

    public ArrayList<String> getPlaylists() {

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        ArrayList<String> result =
                new ArrayList<>();

        /*
         * Favorites is always shown first.
         */
        result.add(
                FAVORITES_PLAYLIST
        );

        for (String name : playlists.keySet()) {

            if (name == null) {
                continue;
            }

            if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                    name
            )) {
                continue;
            }

            result.add(
                    name
            );
        }

        return result;
    }

    public int getPlaylistSongCount(
            String playlistName) {

        if (playlistName == null) {
            return 0;
        }

        String cleanName =
                playlistName.trim();

        /*
         * IMPORTANT:
         *
         * Do not call getFavorites() here.
         * getFavorites() calls getPlaylistSongs(),
         * which would call this method again for Favorites
         * and cause infinite recursion.
         */
        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                cleanName
        )) {

            Map<String, LinkedHashSet<String>> playlists =
                    getPlaylistMap();

            String actualFavoritesName =
                    findActualPlaylistName(
                            playlists,
                            FAVORITES_PLAYLIST
                    );

            if (actualFavoritesName == null) {
                return 0;
            }

            LinkedHashSet<String> favorites =
                    playlists.get(
                            actualFavoritesName
                    );

            return favorites == null
                    ? 0
                    : favorites.size();
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        cleanName
                );

        if (actualName == null) {
            return 0;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        return songs == null
                ? 0
                : songs.size();
    }

    public ArrayList<String> getPlaylistSongs(
            String playlistName) {

        ArrayList<String> result =
                new ArrayList<>();

        if (playlistName == null ||
                playlistName.trim().isEmpty()) {

            return result;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        playlistName.trim()
                );

        /*
         * Favorites is stored in the same playlist map.
         *
         * Do NOT call getFavorites() here because
         * getFavorites() calls this method.
         */
        if (actualName == null) {
            return result;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        if (songs != null) {

            result.addAll(
                    songs
            );
        }

        return result;
    }

    public boolean playlistExists(
            String playlistName) {

        if (playlistName == null ||
                playlistName.trim().isEmpty()) {

            return false;
        }

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                playlistName.trim()
        )) {

            return true;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        return containsPlaylistIgnoreCase(
                playlists,
                playlistName.trim()
        );
    }

    /*
     * ---------------------------------------------------------
     * SONG OPERATIONS
     * ---------------------------------------------------------
     */

    public boolean addSongToPlaylist(
            String playlistName,
            String uri) {

        if (playlistName == null ||
                playlistName.trim().isEmpty() ||
                uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        String cleanName =
                playlistName.trim();

        String cleanUri =
                uri.trim();

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        cleanName
                );

        /*
         * Favorites is created automatically.
         */
        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                cleanName
        )) {

            actualName =
                    FAVORITES_PLAYLIST;

            if (!playlists.containsKey(
                    actualName
            )) {

                playlists.put(
                        actualName,
                        new LinkedHashSet<>()
                );
            }
        }

        if (actualName == null) {
            return false;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        if (songs == null) {

            songs =
                    new LinkedHashSet<>();

            playlists.put(
                    actualName,
                    songs
            );
        }

        boolean added =
                songs.add(
                        cleanUri
                );

        if (added) {

            savePlaylistMap(
                    playlists
            );
        }

        return added;
    }

    public boolean removeSongFromPlaylist(
            String playlistName,
            String uri) {

        if (playlistName == null ||
                playlistName.trim().isEmpty() ||
                uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        playlistName.trim()
                );

        if (actualName == null) {
            return false;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        if (songs == null) {
            return false;
        }

        boolean removed =
                songs.remove(
                        uri.trim()
                );

        if (removed) {

            savePlaylistMap(
                    playlists
            );
        }

        return removed;
    }

    public boolean isSongInPlaylist(
            String playlistName,
            String uri) {

        if (playlistName == null ||
                uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        if (FAVORITES_PLAYLIST.equalsIgnoreCase(
                playlistName.trim()
        )) {

            return isFavorite(
                    uri
            );
        }

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        playlistName.trim()
                );

        if (actualName == null) {
            return false;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        return songs != null &&
                songs.contains(
                        uri.trim()
                );
    }

    /*
     * ---------------------------------------------------------
     * FAVORITES
     * ---------------------------------------------------------
     */

    public boolean isFavorite(
            String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        return isSongInPlaylistInternal(
                FAVORITES_PLAYLIST,
                uri.trim()
        );
    }

    public boolean toggleFavorite(
            String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        if (isFavorite(uri)) {

            removeFavorite(uri);

            return false;
        }

        addFavorite(uri);

        return true;
    }

    public void addFavorite(
            String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        addSongToPlaylist(
                FAVORITES_PLAYLIST,
                uri
        );
    }

    public void removeFavorite(
            String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        removeSongFromPlaylist(
                FAVORITES_PLAYLIST,
                uri
        );
    }

    public Set<String> getFavorites() {

        /*
         * Directly read Favorites from the playlist map.
         *
         * Previously this called getPlaylistSongs(),
         * while getPlaylistSongs() called getFavorites(),
         * creating infinite recursion.
         */
        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        FAVORITES_PLAYLIST
                );

        if (actualName == null) {

            return new LinkedHashSet<>();
        }

        LinkedHashSet<String> favorites =
                playlists.get(
                        actualName
                );

        if (favorites == null) {

            return new LinkedHashSet<>();
        }

        return new LinkedHashSet<>(
                favorites
        );
    }

    public void clearFavorites() {

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        playlists.put(
                FAVORITES_PLAYLIST,
                new LinkedHashSet<>()
        );

        savePlaylistMap(
                playlists
        );
    }

    /*
     * ---------------------------------------------------------
     * INTERNAL STORAGE
     * ---------------------------------------------------------
     */

    private Map<String, LinkedHashSet<String>>
    getPlaylistMap() {

        Map<String, LinkedHashSet<String>> playlists =
                new LinkedHashMap<>();

        String json =
                preferences.getString(
                        KEY_PLAYLISTS,
                        "{}"
                );

        try {

            JSONObject root =
                    new JSONObject(json);

            JSONArray names =
                    root.names();

            if (names != null) {

                for (int i = 0;
                        i < names.length();
                        i++) {

                    String playlistName =
                            names.optString(
                                    i,
                                    ""
                            );

                    if (playlistName.trim().isEmpty()) {
                        continue;
                    }

                    JSONArray songsArray =
                            root.optJSONArray(
                                    playlistName
                            );

                    LinkedHashSet<String> songs =
                            new LinkedHashSet<>();

                    if (songsArray != null) {

                        for (int j = 0;
                                j < songsArray.length();
                                j++) {

                            String uri =
                                    songsArray.optString(
                                            j,
                                            ""
                                    );

                            if (uri != null &&
                                    !uri.trim().isEmpty()) {

                                songs.add(
                                        uri
                                );
                            }
                        }
                    }

                    playlists.put(
                            playlistName,
                            songs
                    );
                }
            }

        } catch (Exception ignored) {
        }

        return playlists;
    }

    private void savePlaylistMap(
            Map<String, LinkedHashSet<String>> playlists) {

        JSONObject root =
                new JSONObject();

        try {

            for (Map.Entry<String,
                    LinkedHashSet<String>> entry
                    : playlists.entrySet()) {

                String name =
                        entry.getKey();

                if (name == null ||
                        name.trim().isEmpty()) {

                    continue;
                }

                JSONArray songs =
                        new JSONArray();

                LinkedHashSet<String> uris =
                        entry.getValue();

                if (uris != null) {

                    for (String uri : uris) {

                        if (uri != null &&
                                !uri.trim().isEmpty()) {

                            songs.put(
                                    uri
                            );
                        }
                    }
                }

                root.put(
                        name,
                        songs
                );
            }

        } catch (Exception ignored) {
        }

        preferences.edit()
                .putString(
                        KEY_PLAYLISTS,
                        root.toString()
                )
                .apply();
    }

    private boolean isSongInPlaylistInternal(
            String playlistName,
            String uri) {

        Map<String, LinkedHashSet<String>> playlists =
                getPlaylistMap();

        String actualName =
                findActualPlaylistName(
                        playlists,
                        playlistName
                );

        if (actualName == null) {
            return false;
        }

        LinkedHashSet<String> songs =
                playlists.get(
                        actualName
                );

        return songs != null &&
                songs.contains(uri);
    }

    private String findActualPlaylistName(
            Map<String, LinkedHashSet<String>> playlists,
            String name) {

        if (playlists == null ||
                name == null) {

            return null;
        }

        for (String existingName
                : playlists.keySet()) {

            if (existingName != null &&
                    existingName.equalsIgnoreCase(
                            name.trim()
                    )) {

                return existingName;
            }
        }

        return null;
    }

    private boolean containsPlaylistIgnoreCase(
            Map<String, LinkedHashSet<String>> playlists,
            String name) {

        return findActualPlaylistName(
                playlists,
                name
        ) != null;
    }

    /*
     * ---------------------------------------------------------
     * OLD FAVORITE MANAGER MIGRATION
     * ---------------------------------------------------------
     */

    private void migrateOldFavorites() {

        /*
         * Already migrated.
         */
        if (preferences.contains(
                KEY_PLAYLISTS
        )) {

            return;
        }

        String oldJson =
                oldFavoritePreferences.getString(
                        OLD_KEY_FAVORITES,
                        null
                );

        Map<String, LinkedHashSet<String>> playlists =
                new LinkedHashMap<>();

        LinkedHashSet<String> favorites =
                new LinkedHashSet<>();

        if (oldJson != null &&
                !oldJson.trim().isEmpty()) {

            try {

                JSONArray array =
                        new JSONArray(
                                oldJson
                        );

                for (int i = 0;
                        i < array.length();
                        i++) {

                    String uri =
                            array.optString(
                                    i,
                                    ""
                            );

                    if (uri != null &&
                            !uri.trim().isEmpty()) {

                        favorites.add(
                                uri
                        );
                    }
                }

            } catch (Exception ignored) {
            }
        }

        playlists.put(
                FAVORITES_PLAYLIST,
                favorites
        );

        savePlaylistMap(
                playlists
        );

        /*
         * We intentionally remove the old storage only
         * after successfully creating the new storage.
         */
        oldFavoritePreferences.edit()
                .remove(
                        OLD_KEY_FAVORITES
                )
                .apply();
    }
}