package com.urfavxbf.kanade;

public final class ArtworkSource {

    private ArtworkSource() {}

    public static String resolveUri(AudioFile song) {
        if (song == null) return null;
        String artwork = song.getAlbumArtUri();
        if (artwork != null && !artwork.trim().isEmpty()) return artwork;
        return song.getPath();
    }
}
