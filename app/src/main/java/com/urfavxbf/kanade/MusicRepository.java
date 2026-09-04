package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;

public class MusicRepository {

    private final MusicScanner musicScanner;
    private final MetadataOverrideManager metadataOverrideManager;
    private final Object cacheLock = new Object();

    private volatile ArrayList<AudioFile> cachedSongs;

    public MusicRepository(Context context) {

        musicScanner =
                new MusicScanner(context);

        metadataOverrideManager =
                new MetadataOverrideManager(context);
    }

    public ArrayList<AudioFile> getAllSongs() {

        ArrayList<AudioFile> songs = cachedSongs;

        if (songs == null) {

            synchronized (cacheLock) {

                songs = cachedSongs;

                if (songs == null) {

                    songs = musicScanner.scanMusic();

                    applyOverrides(songs);

                    if (songs == null) {
                        songs = new ArrayList<>();
                    }

                    cachedSongs = new ArrayList<>(songs);
                }
            }
        }

        return new ArrayList<>(songs);
    }

    public ArrayList<AudioFile> searchSongs(
            String query) {

        ArrayList<AudioFile> allSongs =
                getAllSongs();

        ArrayList<AudioFile> results =
                new ArrayList<>();

        if (query == null ||
                query.trim().isEmpty()) {

            return allSongs;
        }

        String searchQuery =
                query.trim().toLowerCase();

        for (AudioFile song : allSongs) {

            if (song == null) {
                continue;
            }

            if (song.getTitle() != null &&
                    song.getTitle()
                            .toLowerCase()
                            .contains(searchQuery)) {

                results.add(song);
                continue;
            }

            if (song.getArtist() != null &&
                    song.getArtist()
                            .toLowerCase()
                            .contains(searchQuery)) {

                results.add(song);
                continue;
            }

            if (song.getAlbum() != null &&
                    song.getAlbum()
                            .toLowerCase()
                            .contains(searchQuery)) {

                results.add(song);
            }
        }

        return results;
    }

    public ArrayList<AudioFile> refreshMusic() {

        synchronized (cacheLock) {

            ArrayList<AudioFile> songs =
                    musicScanner.refreshMusic();

            applyOverrides(songs);

            if (songs == null) {
                songs = new ArrayList<>();
            }

            cachedSongs = new ArrayList<>(songs);

            return new ArrayList<>(cachedSongs);
        }
    }

    public void clearMusicCache() {

        synchronized (cacheLock) {

            cachedSongs = null;
            musicScanner.clearCache();
        }
    }

    private void applyOverrides(
            ArrayList<AudioFile> songs) {

        if (songs == null) {
            return;
        }

        for (AudioFile song : songs) {

            if (song == null) {
                continue;
            }

            metadataOverrideManager.apply(song);
        }
    }
}
