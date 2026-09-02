package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;

public class MusicRepository {

    private final MusicScanner musicScanner;
    private final MetadataOverrideManager metadataOverrideManager;

    public MusicRepository(Context context) {

        musicScanner =
                new MusicScanner(context);

        metadataOverrideManager =
                new MetadataOverrideManager(context);
    }

    public ArrayList<AudioFile> getAllSongs() {

        ArrayList<AudioFile> songs =
                musicScanner.scanMusic();

        applyOverrides(songs);

        return songs;
    }

    public ArrayList<AudioFile> searchSongs(
            String query) {

        ArrayList<AudioFile> allSongs =
                musicScanner.scanMusic();

        applyOverrides(allSongs);

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

        ArrayList<AudioFile> songs =
                musicScanner.refreshMusic();

        applyOverrides(songs);

        return songs;
    }

    public void clearMusicCache() {

        musicScanner.clearCache();
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
