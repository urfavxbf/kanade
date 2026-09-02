package com.urfavxbf.kanade;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class MetadataResolver {

    private final Context context;

    public MetadataResolver(Context context) {

        this.context =
                context.getApplicationContext();
    }

    public void resolve(
            AudioFile audioFile) {

        if (audioFile == null) {

            return;
        }

        String uriString =
                audioFile.getUri();

        if (uriString == null ||
                uriString.trim().isEmpty()) {

            normalizeExistingMetadata(
                    audioFile
            );

            return;
        }

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {

            Uri uri =
                    Uri.parse(uriString);

            retriever.setDataSource(
                    context,
                    uri
            );

            String title =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_TITLE
                    );

            String artist =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_ARTIST
                    );

            String album =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_ALBUM
                    );

            String albumArtist =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_ALBUMARTIST
                    );

            String genre =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_GENRE
                    );

            String composer =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_COMPOSER
                    );

            String year =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_YEAR
                    );

            String trackNumber =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_CD_TRACK_NUMBER
                    );

            String discNumber =
                    retriever.extractMetadata(
                            MediaMetadataRetriever
                                    .METADATA_KEY_DISC_NUMBER
                    );

            /*
             * Use MediaStore values as fallback when the
             * embedded tag is missing.
             */
            if (isEmpty(title)) {

                title =
                        audioFile.getTitle();
            }

            if (isEmpty(artist)) {

                artist =
                        audioFile.getArtist();
            }

            if (isEmpty(album)) {

                album =
                        audioFile.getAlbum();
            }

            /*
             * Normalize the metadata.
             */
            title =
                    MetadataNormalizer
                            .normalizeTitle(
                                    title,
                                    audioFile.getPath()
                            );

            artist =
                    MetadataNormalizer
                            .normalizeArtist(
                                    artist,
                                    audioFile.getPath()
                            );

            album =
                    MetadataNormalizer
                            .normalizeAlbum(
                                    album
                            );

            albumArtist =
                    MetadataNormalizer
                            .normalizeAlbumArtist(
                                    albumArtist,
                                    artist
                            );

            genre =
                    MetadataNormalizer
                            .normalizeGenre(
                                    genre
                            );

            year =
                    MetadataNormalizer
                            .normalizeYear(
                                    year
                            );

            /*
             * Update AudioFile.
             */
            audioFile.setTitle(
                    title
            );

            audioFile.setArtist(
                    artist
            );

            audioFile.setAlbum(
                    album
            );

            audioFile.setAlbumArtist(
                    albumArtist
            );

            audioFile.setGenre(
                    genre
            );

            audioFile.setComposer(
                    cleanOptional(composer)
            );

            audioFile.setYear(
                    year
            );

            audioFile.setTrackNumber(
                    cleanOptional(trackNumber)
            );

            audioFile.setDiscNumber(
                    cleanOptional(discNumber)
            );

        } catch (Exception e) {

            /*
             * A broken/unreadable tag should NOT remove
             * the song from the library.
             *
             * Fall back to MediaStore metadata.
             */
            normalizeExistingMetadata(
                    audioFile
            );

        } finally {

            try {

                retriever.release();

            } catch (Exception ignored) {
            }
        }
    }

    private void normalizeExistingMetadata(
            AudioFile audioFile) {

        String title =
                MetadataNormalizer
                        .normalizeTitle(
                                audioFile.getTitle(),
                                audioFile.getPath()
                        );

        String artist =
                MetadataNormalizer
                        .normalizeArtist(
                                audioFile.getArtist(),
                                audioFile.getPath()
                        );

        String album =
                MetadataNormalizer
                        .normalizeAlbum(
                                audioFile.getAlbum()
                        );

        audioFile.setTitle(
                title
        );

        audioFile.setArtist(
                artist
        );

        audioFile.setAlbum(
                album
        );

        audioFile.setAlbumArtist(
                MetadataNormalizer
                        .normalizeAlbumArtist(
                                null,
                                artist
                        )
        );
    }

    private String cleanOptional(
            String value) {

        if (value == null) {

            return null;
        }

        String result =
                value.trim();

        if (result.isEmpty()) {

            return null;
        }

        return result;
    }

    private boolean isEmpty(
            String value) {

        return value == null ||
                value.trim().isEmpty();
    }
}