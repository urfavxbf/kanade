package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MusicRepository {

    private static final Object cacheLock = new Object();

    private static volatile ArrayList<AudioFile> cachedSongs;

    private static final Object remoteSongsLock = new Object();
    private static final LinkedHashMap<String, AudioFile> remoteSongs = new LinkedHashMap<>();

    private final MusicScanner musicScanner;
    private final MetadataOverrideManager metadataOverrideManager;

    public MusicRepository(Context context) {

        ArtworkResolver.initialize(context);

        musicScanner =
                new MusicScanner(context);

        metadataOverrideManager =
                new MetadataOverrideManager(context);
    }

    public static void registerRemoteSong(
            String uri,
            String title,
            String artist,
            String artworkUri,
            long duration) {

        if (uri == null || uri.trim().isEmpty()) {
            return;
        }

        AudioFile song = new AudioFile(
                Long.MIN_VALUE + Math.abs((long) uri.hashCode()),
                title,
                artist,
                title,
                uri,
                artworkUri,
                duration,
                System.currentTimeMillis()
        );

        song.setAlbumArtUri(artworkUri);

        synchronized (remoteSongsLock) {
            remoteSongs.put(uri, song);

            while (remoteSongs.size() > 100) {
                String firstKey = remoteSongs.keySet().iterator().next();
                remoteSongs.remove(firstKey);
            }
        }
    }

    public static void removeRemoteSong(String uri) {

        if (uri == null || uri.trim().isEmpty()) {
            return;
        }

        synchronized (remoteSongsLock) {
            remoteSongs.remove(uri);
        }
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

        ArrayList<AudioFile> result = copySongs(songs);

        synchronized (remoteSongsLock) {
            for (AudioFile remoteSong : remoteSongs.values()) {
                if (remoteSong != null) {
                    result.add(copySong(remoteSong));
                }
            }
        }

        return result;
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
