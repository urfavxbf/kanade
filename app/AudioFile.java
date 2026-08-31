package com.urfavxbf.kanade;

public class AudioFile {

    private long id;
    private String title;
    private String artist;
    private String album;
    private String uri;
    private String path;
    private long duration;
    private long dateAdded;
    private String albumArtUri;

    public AudioFile(
            long id,
            String title,
            String artist,
            String album,
            String uri,
            String path,
            long duration,
            long dateAdded) {

        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
        this.path = path;
        this.duration = duration;
        this.dateAdded = dateAdded;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public long getDuration() {
        return duration;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public String getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(String albumArtUri) {
        this.albumArtUri = albumArtUri;
    }
}