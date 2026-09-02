package com.urfavxbf.kanade;

public class MusicMetadataCandidate {

    private String recordingId;
    private String acoustId;
    private String releaseId;
    private String albumArtUrl;

    private String title;
    private String artist;
    private String album;
    private String albumArtist;
    private String genre;
    private String composer;
    private String year;
    private String trackNumber;
    private String discNumber;

    private long durationMs;

    private double matchScore;
    private int musicBrainzScore;

    private String releaseGroup;
    private String disambiguation;

    private String source;

    public MusicMetadataCandidate() {
    }

    public String getRecordingId() { return recordingId; }
    public void setRecordingId(String recordingId) { this.recordingId = recordingId; }

    public String getAcoustId() { return acoustId; }
    public void setAcoustId(String acoustId) { this.acoustId = acoustId; }

    public String getReleaseId() { return releaseId; }
    public void setReleaseId(String releaseId) { this.releaseId = releaseId; }

    public String getAlbumArtUrl() { return albumArtUrl; }
    public void setAlbumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; }

    public boolean hasAlbumArt() {
        return albumArtUrl != null && !albumArtUrl.trim().isEmpty();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getAlbumArtist() { return albumArtist; }
    public void setAlbumArtist(String albumArtist) { this.albumArtist = albumArtist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getComposer() { return composer; }
    public void setComposer(String composer) { this.composer = composer; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getTrackNumber() { return trackNumber; }
    public void setTrackNumber(String trackNumber) { this.trackNumber = trackNumber; }

    public String getDiscNumber() { return discNumber; }
    public void setDiscNumber(String discNumber) { this.discNumber = discNumber; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getDurationSeconds() { return durationMs / 1000L; }

    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public int getMusicBrainzScore() { return musicBrainzScore; }
    public void setMusicBrainzScore(int musicBrainzScore) { this.musicBrainzScore = musicBrainzScore; }

    public String getReleaseGroup() { return releaseGroup; }
    public void setReleaseGroup(String releaseGroup) { this.releaseGroup = releaseGroup; }

    public String getDisambiguation() { return disambiguation; }
    public void setDisambiguation(String disambiguation) { this.disambiguation = disambiguation; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSafeTitle() { return isEmpty(title) ? "Unknown Title" : title; }
    public String getSafeArtist() { return isEmpty(artist) ? "Unknown Artist" : artist; }
    public String getSafeAlbum() { return isEmpty(album) ? "Unknown Album" : album; }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
