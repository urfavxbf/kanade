package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;

public class MusicRepository {

    private final MusicScanner musicScanner;

    public MusicRepository(Context context) {

        musicScanner =
                new MusicScanner(context);
    }

    public ArrayList<AudioFile> getAllSongs() {

        return musicScanner.scanMusic();
    }

    public ArrayList<AudioFile> searchSongs(
            String query) {

        ArrayList<AudioFile> allSongs =
                musicScanner.scanMusic();

        ArrayList<AudioFile> results =
                new ArrayList<>();

        if (query == null ||
                query.trim().isEmpty()) {

            return allSongs;
        }

        String searchQuery =
                query.trim().toLowerCase();

        for (AudioFile song : allSongs) {

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

        return musicScanner.refreshMusic();
    }

    public void clearMusicCache() {

        musicScanner.clearCache();
    }
}