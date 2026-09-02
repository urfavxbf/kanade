package com.urfavxbf.kanade;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicBrainzClient {

    private static final String API_URL =
            "https://musicbrainz.org/ws/2";

    private static final String USER_AGENT =
            "KanadeMusicPlayer/1.0.0";

    private static final int SEARCH_LIMIT = 10;

    public MusicBrainzClient() {
    }

    /*
     * Existing search method.
     *
     * Kept for compatibility.
     */
    public ArrayList<MusicBrainzRecording>
            searchRecordings(
                    String title,
                    String artist) {

        return searchRecordings(
                title,
                artist,
                0
        );
    }

    /*
     * Duration-aware recording search.
     *
     * durationSeconds may be 0 when unavailable.
     */
    public ArrayList<MusicBrainzRecording>
            searchRecordings(
                    String title,
                    String artist,
                    long durationSeconds) {

        ArrayList<MusicBrainzRecording>
                results =
                new ArrayList<>();

        if (isEmpty(title) &&
                isEmpty(artist)) {

            return results;
        }

        HttpURLConnection connection =
                null;

        try {

            StringBuilder query =
                    new StringBuilder();

            /*
             * Title.
             */
            if (!isEmpty(title)) {

                query.append(
                        "recording:"
                );

                query.append(
                        escapeQueryValue(
                                title
                        )
                );
            }

            /*
             * Artist.
             */
            if (!isEmpty(artist)) {

                if (query.length() > 0) {

                    query.append(
                            "%20AND%20"
                    );
                }

                query.append(
                        "artist:"
                );

                query.append(
                        escapeQueryValue(
                                artist
                        )
                );
            }

            /*
             * If duration is available,
             * use MusicBrainz' quantized
             * duration field.
             *
             * qdur = duration in milliseconds / 2000.
             */
            if (durationSeconds > 0) {

                if (query.length() > 0) {

                    query.append(
                            "%20AND%20"
                    );
                }

                long durationMs =
                        durationSeconds * 1000L;

                long qdur =
                        durationMs / 2000L;

                query.append(
                        "qdur:"
                );

                query.append(
                        qdur
                );
            }

            String requestUrl =
                    API_URL
                            + "/recording?query="
                            + query
                            + "&fmt=json"
                            + "&limit="
                            + SEARCH_LIMIT;

            URL url =
                    new URL(
                            requestUrl
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    20000
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 &&
                    responseCode < 300) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();

                return results;
            }

            String response =
                    readStream(
                            inputStream
                    );

            if (isEmpty(response)) {

                return results;
            }

            JSONObject root =
                    new JSONObject(
                            response
                    );

            JSONArray recordings =
                    root.optJSONArray(
                            "recordings"
                    );

            if (recordings == null) {

                return results;
            }

            for (int i = 0;
                    i < recordings.length();
                    i++) {

                JSONObject recording =
                        recordings.optJSONObject(
                                i
                        );

                if (recording == null) {

                    continue;
                }

                MusicBrainzRecording parsed =
                        parseRecording(
                                recording,
                                durationSeconds
                        );

                if (parsed != null) {

                    results.add(
                            parsed
                    );
                }
            }

            /*
             * Sort strongest candidate first.
             */
            Collections.sort(
                    results,
                    new Comparator<MusicBrainzRecording>() {

                        @Override
                        public int compare(
                                MusicBrainzRecording a,
                                MusicBrainzRecording b) {

                            return Double.compare(
                                    b.getMatchScore(),
                                    a.getMatchScore()
                            );
                        }
                    }
            );

        } catch (Exception ignored) {

            /*
             * Online metadata must never
             * break Kanade.
             */

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }

        return results;
    }

    /*
     * Direct MusicBrainz recording lookup.
     */
    public MusicBrainzRecording
            getRecording(
                    String recordingId) {

        if (isEmpty(recordingId)) {

            return null;
        }

        HttpURLConnection connection =
                null;

        try {

            String requestUrl =
                    API_URL
                            + "/recording/"
                            + URLEncoder.encode(
                                    recordingId,
                                    "UTF-8"
                            )
                            + "?fmt=json"
                            + "&inc=artists+releases+release-groups";

            URL url =
                    new URL(
                            requestUrl
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    20000
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode < 200 ||
                    responseCode >= 300) {

                return null;
            }

            String response =
                    readStream(
                            connection.getInputStream()
                    );

            if (isEmpty(response)) {

                return null;
            }

            JSONObject recording =
                    new JSONObject(
                            response
                    );

            return parseRecording(
                    recording,
                    0
            );

        } catch (Exception ignored) {

            return null;

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    /*
     * Parse a MusicBrainz recording.
     */
    private MusicBrainzRecording
            parseRecording(
                    JSONObject recording,
                    long localDurationSeconds) {

        if (recording == null) {

            return null;
        }

        String id =
                clean(
                        recording.optString(
                                "id",
                                null
                        )
                );

        String title =
                clean(
                        recording.optString(
                                "title",
                                null
                        )
                );

        String artist =
                extractArtist(
                        recording
                );

        String album =
                extractReleaseTitle(
                        recording
                );

        String releaseGroup =
                extractReleaseGroupTitle(
                        recording
                );

        String disambiguation =
                clean(
                        recording.optString(
                                "disambiguation",
                                null
                        )
                );

        long durationMs =
                recording.optLong(
                        "length",
                        0
                );

        long durationSeconds =
                durationMs > 0
                        ? durationMs / 1000L
                        : 0;

        int searchScore =
                recording.optInt(
                        "score",
                        0
                );

        double matchScore =
                calculateMatchScore(
                        title,
                        artist,
                        durationSeconds,
                        localDurationSeconds,
                        searchScore
                );

        return new MusicBrainzRecording(
                id,
                title,
                artist,
                album,
                releaseGroup,
                disambiguation,
                searchScore,
                durationMs,
                matchScore
        );
    }

    /*
     * Calculate our own candidate score.
     *
     * MusicBrainz score alone is not enough because
     * several releases can contain the same recording.
     */
    private double calculateMatchScore(
            String title,
            String artist,
            long remoteDurationSeconds,
            long localDurationSeconds,
            int musicBrainzScore) {

        double score = 0.0;

        /*
         * MusicBrainz search score:
         * maximum contribution = 30.
         */
        score +=
                Math.max(
                        0,
                        Math.min(
                                100,
                                musicBrainzScore
                        )
                ) * 0.30;

        /*
         * Duration:
         * maximum contribution = 40.
         */
        if (localDurationSeconds > 0 &&
                remoteDurationSeconds > 0) {

            long difference =
                    Math.abs(
                            localDurationSeconds
                                    - remoteDurationSeconds
                    );

            if (difference == 0) {

                score += 40.0;

            } else if (difference <= 1) {

                score += 38.0;

            } else if (difference <= 2) {

                score += 35.0;

            } else if (difference <= 3) {

                score += 30.0;

            } else if (difference <= 5) {

                score += 20.0;

            } else if (difference <= 10) {

                score += 10.0;
            }
        }

        /*
         * Title and artist are already part of
         * the MusicBrainz query, so the returned
         * search score represents those matches.
         */
        return score;
    }

    private String extractArtist(
            JSONObject object) {

        JSONArray artists =
                object.optJSONArray(
                        "artist-credit"
                );

        if (artists == null) {

            return null;
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
                i < artists.length();
                i++) {

            JSONObject credit =
                    artists.optJSONObject(
                            i
                    );

            if (credit == null) {

                continue;
            }

            JSONObject artist =
                    credit.optJSONObject(
                            "artist"
                    );

            if (artist == null) {

                continue;
            }

            String name =
                    clean(
                            artist.optString(
                                    "name",
                                    null
                            )
                    );

            if (name == null) {

                continue;
            }

            if (builder.length() > 0) {

                builder.append(
                        ", "
                );
            }

            builder.append(
                    name
            );

            /*
             * Preserve join phrases such as:
             *
             * Artist feat. Artist
             */
            String joinphrase =
                    credit.optString(
                            "joinphrase",
                            ""
                    );

            if (!isEmpty(joinphrase)) {

                builder.append(
                        joinphrase
                );
            }
        }

        return builder.length() > 0
                ? builder.toString().trim()
                : null;
    }

    private String extractReleaseTitle(
            JSONObject object) {

        JSONArray releases =
                object.optJSONArray(
                        "releases"
                );

        if (releases == null ||
                releases.length() == 0) {

            return null;
        }

        JSONObject release =
                releases.optJSONObject(
                        0
                );

        if (release == null) {

            return null;
        }

        return clean(
                release.optString(
                        "title",
                        null
                )
        );
    }

    private String extractReleaseGroupTitle(
            JSONObject object) {

        JSONArray releaseGroups =
                object.optJSONArray(
                        "release-group"
                );

        if (releaseGroups == null ||
                releaseGroups.length() == 0) {

            /*
             * Some MusicBrainz responses may use
             * "release-group" as an object when
             * looking up a single recording.
             */
            JSONObject releaseGroup =
                    object.optJSONObject(
                            "release-group"
                    );

            if (releaseGroup != null) {

                return clean(
                        releaseGroup.optString(
                                "title",
                                null
                        )
                );
            }

            return null;
        }

        JSONObject releaseGroup =
                releaseGroups.optJSONObject(
                        0
                );

        if (releaseGroup == null) {

            return null;
        }

        return clean(
                releaseGroup.optString(
                        "title",
                        null
                )
        );
    }

    private String escapeQueryValue(
            String value) {

        if (value == null) {

            return "";
        }

        String cleaned =
                value.trim();

        try {

            return URLEncoder.encode(
                    "\"" + cleaned + "\"",
                    "UTF-8"
            );

        } catch (Exception e) {

            return cleaned;
        }
    }

    private String clean(
            String value) {

        if (value == null) {

            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    private String readStream(
            InputStream inputStream)
            throws Exception {

        if (inputStream == null) {

            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while ((line =
                reader.readLine()) != null) {

            builder.append(
                    line
            );
        }

        reader.close();

        return builder.toString();
    }

    private boolean isEmpty(
            String value) {

        return value == null ||
                value.trim().isEmpty();
    }

    public static class MusicBrainzRecording {

        private final String id;
        private final String title;
        private final String artist;
        private final String album;
        private final String releaseGroup;
        private final String disambiguation;
        private final int score;
        private final long durationMs;
        private final double matchScore;

        /*
         * Backward-compatible constructor.
         */
        public MusicBrainzRecording(
                String id,
                String title,
                String artist,
                String album,
                String disambiguation,
                int score) {

            this(
                    id,
                    title,
                    artist,
                    album,
                    null,
                    disambiguation,
                    score,
                    0,
                    score
            );
        }

        public MusicBrainzRecording(
                String id,
                String title,
                String artist,
                String album,
                String releaseGroup,
                String disambiguation,
                int score,
                long durationMs,
                double matchScore) {

            this.id =
                    id;

            this.title =
                    title;

            this.artist =
                    artist;

            this.album =
                    album;

            this.releaseGroup =
                    releaseGroup;

            this.disambiguation =
                    disambiguation;

            this.score =
                    score;

            this.durationMs =
                    durationMs;

            this.matchScore =
                    matchScore;
        }

        public String getId() {
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

        public String getReleaseGroup() {
            return releaseGroup;
        }

        public String getDisambiguation() {
            return disambiguation;
        }

        public int getScore() {
            return score;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public long getDurationSeconds() {
            return durationMs / 1000L;
        }

        public double getMatchScore() {
            return matchScore;
        }
    }
}