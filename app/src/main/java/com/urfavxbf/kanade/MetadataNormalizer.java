package com.urfavxbf.kanade;

import java.io.File;
import java.util.Locale;

public class MetadataNormalizer {

    private MetadataNormalizer() {
    }

    public static String normalizeTitle(
            String title,
            String path) {

        String result = clean(title);

        if (isUnknown(result)) {

            result = extractFileName(path);
        }

        if (isUnknown(result)) {

            return "Unknown Title";
        }

        /*
         * Remove common track-number prefixes.
         *
         * 01 - Song
         * 01. Song
         * 01 Song
         * 01_ Song
         */
        result = result.replaceFirst(
                "^\\s*\\d{1,3}\\s*[-._]\\s*",
                ""
        );

        result = result.replaceFirst(
                "^\\s*\\d{1,3}\\s+",
                ""
        );

        /*
         * Remove common upload/release suffixes.
         */
        result = result.replaceAll(
                "\\s*\\[(?i:official\\s+(audio|video)|lyrics?|lyric\\s+video|audio|video|hd|hq|4k)\\]\\s*$",
                ""
        );

        result = result.replaceAll(
                "\\s*\\((?i:official\\s+(audio|video)|lyrics?|lyric\\s+video|audio|video|hd|hq|4k)\\)\\s*$",
                ""
        );

        return clean(result);
    }

    public static String normalizeArtist(
            String artist,
            String path) {

        String result = clean(artist);

        if (isUnknown(result)) {

            /*
             * Only use filename as a fallback when the
             * filename looks like:
             *
             * Artist - Title.mp3
             */
            String fileName =
                    extractFileName(path);

            int separator =
                    fileName.indexOf(" - ");

            if (separator > 0) {

                result =
                        fileName.substring(
                                0,
                                separator
                        );
            }
        }

        if (isUnknown(result)) {

            return "Unknown Artist";
        }

        return clean(result);
    }

    public static String normalizeAlbum(
            String album) {

        String result =
                clean(album);

        if (isUnknown(result)) {

            return "Unknown Album";
        }

        return result;
    }

    public static String normalizeAlbumArtist(
            String albumArtist,
            String artist) {

        String result =
                clean(albumArtist);

        if (isUnknown(result)) {

            result =
                    clean(artist);
        }

        if (isUnknown(result)) {

            return "Unknown Artist";
        }

        return result;
    }

    public static String normalizeGenre(
            String genre) {

        String result =
                clean(genre);

        if (isUnknown(result)) {

            return null;
        }

        return result;
    }

    public static String normalizeYear(
            String year) {

        String result =
                clean(year);

        if (isUnknown(result)) {

            return null;
        }

        /*
         * Extract a four-digit year if the metadata
         * contains something like:
         *
         * 2024-05-10
         * 2024
         */
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("\\b(19|20)\\d{2}\\b")
                        .matcher(result);

        if (matcher.find()) {

            return matcher.group();
        }

        return result;
    }

    private static String extractFileName(
            String path) {

        if (path == null ||
                path.trim().isEmpty()) {

            return null;
        }

        try {

            String name =
                    new File(path)
                            .getName();

            int extension =
                    name.lastIndexOf('.');

            if (extension > 0) {

                name =
                        name.substring(
                                0,
                                extension
                        );
            }

            return clean(name);

        } catch (Exception e) {

            return null;
        }
    }

    private static String clean(
            String value) {

        if (value == null) {

            return null;
        }

        String result =
                value.trim();

        if (result.isEmpty()) {

            return null;
        }

        result =
                result.replace(
                        "\u0000",
                        ""
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        return result.trim();
    }

    private static boolean isUnknown(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return true;
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.US
                        );

        return normalized.equals("unknown")
                || normalized.equals("unknown artist")
                || normalized.equals("unknown album")
                || normalized.equals("unknown title")
                || normalized.equals("<unknown>")
                || normalized.equals("null")
                || normalized.equals("n/a")
                || normalized.equals("na");
    }
}