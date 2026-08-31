package com.urfavxbf.kanade;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {

    private static final String PREF_NAME =
            "kanade_favorites";

    private static final String KEY_FAVORITES =
            "favorites";

    private final SharedPreferences preferences;

    public FavoriteManager(Context context) {

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    public boolean isFavorite(String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        return getFavorites().contains(uri);
    }

    public boolean toggleFavorite(String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return false;
        }

        Set<String> favorites =
                getFavorites();

        boolean isNowFavorite;

        if (favorites.contains(uri)) {

            favorites.remove(uri);
            isNowFavorite = false;

        } else {

            favorites.add(uri);
            isNowFavorite = true;
        }

        saveFavorites(favorites);

        return isNowFavorite;
    }

    public void addFavorite(String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        Set<String> favorites =
                getFavorites();

        favorites.add(uri);

        saveFavorites(favorites);
    }

    public void removeFavorite(String uri) {

        if (uri == null ||
                uri.trim().isEmpty()) {

            return;
        }

        Set<String> favorites =
                getFavorites();

        favorites.remove(uri);

        saveFavorites(favorites);
    }

    public Set<String> getFavorites() {

        Set<String> favorites =
                new HashSet<>();

        String json =
                preferences.getString(
                        KEY_FAVORITES,
                        "[]"
                );

        try {

            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                    i < array.length();
                    i++) {

                String uri =
                        array.optString(i, null);

                if (uri != null &&
                        !uri.trim().isEmpty()) {

                    favorites.add(uri);
                }
            }

        } catch (Exception ignored) {
        }

        return favorites;
    }

    private void saveFavorites(
            Set<String> favorites) {

        JSONArray array =
                new JSONArray();

        for (String uri : favorites) {

            if (uri != null &&
                    !uri.trim().isEmpty()) {

                array.put(uri);
            }
        }

        preferences.edit()
                .putString(
                        KEY_FAVORITES,
                        array.toString()
                )
                .apply();
    }

    public void clearFavorites() {

        preferences.edit()
                .remove(KEY_FAVORITES)
                .apply();
    }
}