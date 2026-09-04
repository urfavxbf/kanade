package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;
import java.util.Locale;

public class MusicRepository {

    private static final Object cacheLock = new Object();

    private static volatile ArrayList<AudioFile> cachedSongs;

    private final MusicScanner musicScanner;
    private final MetadataOverrideManager metadataOverrideManager;

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

                    if (songs == null) {
                        songs = new ArrayList<>();
                    }

                    applyOverrides(songs);

                    cachedSongs = copySongs(songs);
                    songs = cachedSongs;
                }
            }
        }

        return copySongs(songs);
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
                query.trim().toLowerCase(Locale.ROOT);

        for (AudioFile song : allSongs) {

            if (song == null) {
                continue;
            }

            if (song.getTitle() != null &&
                    song.getTitle()
                            .toLowerCase(Locale.ROOT)
                            .contains(searchQuery)) {

                results.add(song);
                continue;
            }

            if (song.getArtist() != null &&
                    song.getArtist()
                            .toLowerCase(Locale.ROOT)
                            .contains(searchQuery)) {

                results.add(song);
                continue;
            }

            if (song.getAlbum() != null &&
                    song.getAlbum()
                            .toLowerCase(Locale.ROOT)
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

            if (songs == null) {
                songs = new ArrayList<>();
            }

            applyOverrides(songs);

            cachedSongs = copySongs(songs);

            return copySongs(cachedSongs);
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

    private ArrayList<AudioFile> copySongs(
            ArrayList<AudioFile> songs) {

        ArrayList<AudioFile> copies =
                new ArrayList<>();

        if (songs == null || songs.isEmpty()) {
            return copies;
        }

        copies.ensureCapacity(songs.size());

        for (AudioFile song : songs) {

            if (song != null) {
                copies.add(copySong(song));
            }
        }

        return copies;
    }

    private AudioFile copySong(AudioFile song) {

        AudioFile copy = new AudioFile(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getUri(),
                song.getPath(),
                song.getDuration(),
                song.getDateAdded()
        );

        copy.setAlbumArtUri(song.getAlbumArtUri());
        copy.setAlbumArtist(song.getAlbumArtist());
        copy.setGenre(song.getGenre());
        copy.setComposer(song.getComposer());
        copy.setYear(song.getYear());
        copy.setTrackNumber(song.getTrackNumber());
        copy.setDiscNumber(song.getDiscNumber());

        return copy;
    }
}
