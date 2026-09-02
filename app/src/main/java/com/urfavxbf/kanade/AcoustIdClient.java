package com.urfavxbf.kanade;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AcoustIdClient {

    private static final String API_URL =
            "https://api.acoustid.org/v2/lookup";

    /*
     * Registered AcoustID application key.
     */
    private static final String CLIENT_KEY =
            "6PAW65YfNr";

    public AcoustIdClient() {
    }

    public AcoustIdResult lookup(
            String fingerprint,
            long durationSeconds) {

        if (fingerprint == null ||
                fingerprint.trim().isEmpty()) {

            return new AcoustIdResult(
                    false,
                    "Fingerprint is empty.",
                    null,
                    new ArrayList<AcoustIdMatch>()
            );
        }

        if (durationSeconds <= 0) {

            return new AcoustIdResult(
                    false,
                    "Invalid duration.",
                    null,
                    new ArrayList<AcoustIdMatch>()
            );
        }

        if (CLIENT_KEY.trim().isEmpty() ||
                "PASTE_YOUR_ACOUSTID_KEY_HERE".equals(
                        CLIENT_KEY
                )) {

            return new AcoustIdResult(
                    false,
                    "AcoustID application key is not configured.",
                    null,
                    new ArrayList<AcoustIdMatch>()
            );
        }

        HttpURLConnection connection =
                null;

        try {

            /*
             * Ask AcoustID for:
             *
             * - recordings
             * - recordingids
             * - releasegroups
             *
             * We also explicitly request JSON.
             */
            String meta =
                    "recordings+recordingids+releasegroups";

            String query =
                    "client="
                            + URLEncoder.encode(
                                    CLIENT_KEY,
                                    "UTF-8"
                            )
                            + "&duration="
                            + durationSeconds
                            + "&fingerprint="
                            + URLEncoder.encode(
                                    fingerprint,
                                    "UTF-8"
                            )
                            + "&meta="
                            + URLEncoder.encode(
                                    meta,
                                    "UTF-8"
                            )
                            + "&format=json";

            URL url =
                    new URL(
                            API_URL
                                    + "?"
                                    + query
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

                String error =
                        readStream(
                                inputStream
                        );

                return new AcoustIdResult(
                        false,
                        "AcoustID HTTP "
                                + responseCode
                                + ": "
                                + error,
                        error,
                        new ArrayList<AcoustIdMatch>()
                );
            }

            String response =
                    readStream(
                            inputStream
                    );

            /*
             * Parse the initial fingerprint lookup.
             */
            AcoustIdResult initialResult =
                    parseResponse(
                            response
                    );

            /*
             * If we already received recording
             * information, no second request is needed.
             */
            if (hasRecordingMetadata(
                    initialResult
            )) {

                return initialResult;
            }

            /*
             * AcoustID may return only the track ID.
             *
             * In that case resolve each AcoustID
             * track ID separately.
             */
            ArrayList<AcoustIdMatch> resolvedMatches =
                    resolveTrackIds(
                            initialResult.getMatches()
                    );

            /*
             * Preserve the original raw response,
             * because the test dialog displays it.
             */
            String combinedRawResponse =
                    response;

            if (!resolvedMatches.isEmpty()) {

                combinedRawResponse =
                        response
                                + "\n\n"
                                + "========== "
                                + "TRACK ID LOOKUPS "
                                + "==========\n\n";

                for (AcoustIdMatch match :
                        resolvedMatches) {

                    String rawTrackResponse =
                            match.getRawTrackResponse();

                    if (rawTrackResponse != null &&
                            !rawTrackResponse
                                    .trim()
                                    .isEmpty()) {

                        combinedRawResponse +=
                                "AcoustID Track ID: "
                                        + match.getAcoustId()
                                        + "\n"
                                        + rawTrackResponse
                                        + "\n\n";
                    }
                }
            }

            return new AcoustIdResult(
                    initialResult.isSuccess(),
                    initialResult.getMessage(),
                    combinedRawResponse,
                    resolvedMatches
            );

        } catch (Exception e) {

            String error =
                    e.getMessage();

            if (error == null ||
                    error.trim().isEmpty()) {

                error =
                        "AcoustID request failed.";
            }

            return new AcoustIdResult(
                    false,
                    error,
                    null,
                    new ArrayList<AcoustIdMatch>()
            );

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    /*
     * Resolve AcoustID track IDs into
     * MusicBrainz Recording IDs and metadata.
     */
    private ArrayList<AcoustIdMatch>
            resolveTrackIds(
                    ArrayList<AcoustIdMatch> matches) {

        ArrayList<AcoustIdMatch> resolved =
                new ArrayList<>();

        if (matches == null ||
                matches.isEmpty()) {

            return resolved;
        }

        for (AcoustIdMatch original :
                matches) {

            if (original == null) {
                continue;
            }

            String acoustId =
                    original.getAcoustId();

            if (acoustId == null ||
                    acoustId.trim().isEmpty()) {

                resolved.add(original);
                continue;
            }

            AcoustIdMatch resolvedMatch =
                    lookupByTrackId(
                            acoustId,
                            original.getScore()
                    );

            if (resolvedMatch != null) {

                resolved.add(
                        resolvedMatch
                );

            } else {

                resolved.add(
                        original
                );
            }
        }

        return resolved;
    }

    /*
     * Lookup an AcoustID Track ID directly.
     *
     * This is the important fallback when the
     * fingerprint lookup returns only:
     *
     * {
     *   "id": "...",
     *   "score": ...
     * }
     */
    private AcoustIdMatch lookupByTrackId(
            String acoustId,
            double score) {

        HttpURLConnection connection =
                null;

        try {

            String meta =
                    "recordings+recordingids+releasegroups";

            String query =
                    "client="
                            + URLEncoder.encode(
                                    CLIENT_KEY,
                                    "UTF-8"
                            )
                            + "&trackid="
                            + URLEncoder.encode(
                                    acoustId,
                                    "UTF-8"
                            )
                            + "&meta="
                            + URLEncoder.encode(
                                    meta,
                                    "UTF-8"
                            )
                            + "&format=json";

            URL url =
                    new URL(
                            API_URL
                                    + "?"
                                    + query
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

                return null;
            }

            String response =
                    readStream(
                            inputStream
                    );

            if (response == null ||
                    response.trim().isEmpty()) {

                return null;
            }

            JSONObject root =
                    new JSONObject(
                            response
                    );

            String status =
                    root.optString(
                            "status",
                            ""
                    );

            if (!"ok".equalsIgnoreCase(
                    status
            )) {

                return null;
            }

            JSONArray results =
                    root.optJSONArray(
                            "results"
                    );

            if (results == null ||
                    results.length() == 0) {

                return null;
            }

            /*
             * Find the result corresponding to
             * our AcoustID track.
             */
            JSONObject bestResult =
                    null;

            for (int i = 0;
                    i < results.length();
                    i++) {

                JSONObject result =
                        results.optJSONObject(
                                i
                        );

                if (result == null) {
                    continue;
                }

                String resultId =
                        clean(
                                result.optString(
                                        "id",
                                        null
                                )
                        );

                if (acoustId.equals(
                        resultId
                )) {

                    bestResult =
                            result;

                    break;
                }

                if (bestResult == null) {

                    bestResult =
                            result;
                }
            }

            if (bestResult == null) {

                return null;
            }

            double resultScore =
                    bestResult.optDouble(
                            "score",
                            score
                    );

            JSONArray recordings =
                    bestResult.optJSONArray(
                            "recordings"
                    );

            /*
             * If recordings[] is still missing,
             * return the AcoustID match rather than
             * losing the successful fingerprint match.
             */
            if (recordings == null ||
                    recordings.length() == 0) {

                return new AcoustIdMatch(
                        acoustId,
                        resultScore,
                        null,
                        null,
                        null,
                        null,
                        0,
                        response
                );
            }

            /*
             * Use the first MusicBrainz recording
             * associated with this AcoustID.
             */
            JSONObject recording =
                    recordings.optJSONObject(
                            0
                    );

            if (recording == null) {

                return new AcoustIdMatch(
                        acoustId,
                        resultScore,
                        null,
                        null,
                        null,
                        null,
                        0,
                        response
                );
            }

            String recordingId =
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
                    extractArtists(
                            recording
                    );

            String album =
                    extractAlbum(
                            recording
                    );

            long duration =
                    recording.optLong(
                            "duration",
                            0
                    );

            return new AcoustIdMatch(
                    acoustId,
                    resultScore,
                    recordingId,
                    title,
                    artist,
                    album,
                    duration,
                    response
            );

        } catch (Exception ignored) {

            return null;

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    private boolean hasRecordingMetadata(
            AcoustIdResult result) {

        if (result == null ||
                result.getMatches() == null) {

            return false;
        }

        for (AcoustIdMatch match :
                result.getMatches()) {

            if (match == null) {
                continue;
            }

            String recordingId =
                    match.getRecordingId();

            if (recordingId != null &&
                    !recordingId.trim().isEmpty()) {

                return true;
            }
        }

        return false;
    }

    private AcoustIdResult parseResponse(
            String response) {

        if (response == null ||
                response.trim().isEmpty()) {

            return new AcoustIdResult(
                    false,
                    "Empty AcoustID response.",
                    response,
                    new ArrayList<AcoustIdMatch>()
            );
        }

        try {

            JSONObject root =
                    new JSONObject(
                            response
                    );

            String status =
                    root.optString(
                            "status",
                            ""
                    );

            if (!"ok".equalsIgnoreCase(
                    status
            )) {

                return new AcoustIdResult(
                        false,
                        "AcoustID returned status: "
                                + status,
                        response,
                        new ArrayList<AcoustIdMatch>()
                );
            }

            JSONArray results =
                    root.optJSONArray(
                            "results"
                    );

            ArrayList<AcoustIdMatch> matches =
                    new ArrayList<>();

            if (results == null ||
                    results.length() == 0) {

                return new AcoustIdResult(
                        true,
                        "No AcoustID matches found.",
                        response,
                        matches
                );
            }

            for (int i = 0;
                    i < results.length();
                    i++) {

                JSONObject result =
                        results.optJSONObject(
                                i
                        );

                if (result == null) {
                    continue;
                }

                String acoustId =
                        clean(
                                result.optString(
                                        "id",
                                        null
                                )
                        );

                double score =
                        result.optDouble(
                                "score",
                                0.0
                        );

                JSONArray recordings =
                        result.optJSONArray(
                                "recordings"
                        );

                /*
                 * No recording metadata yet.
                 *
                 * Keep the AcoustID Track ID so
                 * lookupByTrackId() can resolve it.
                 */
                if (recordings == null ||
                        recordings.length() == 0) {

                    matches.add(
                            new AcoustIdMatch(
                                    acoustId,
                                    score,
                                    null,
                                    null,
                                    null,
                                    null,
                                    0,
                                    null
                            )
                    );

                    continue;
                }

                for (int j = 0;
                        j < recordings.length();
                        j++) {

                    JSONObject recording =
                            recordings.optJSONObject(
                                    j
                            );

                    if (recording == null) {
                        continue;
                    }

                    String recordingId =
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
                            extractArtists(
                                    recording
                            );

                    String album =
                            extractAlbum(
                                    recording
                            );

                    long duration =
                            recording.optLong(
                                    "duration",
                                    0
                            );

                    matches.add(
                            new AcoustIdMatch(
                                    acoustId,
                                    score,
                                    recordingId,
                                    title,
                                    artist,
                                    album,
                                    duration,
                                    response
                            )
                    );
                }
            }

            return new AcoustIdResult(
                    true,
                    "AcoustID lookup completed.",
                    response,
                    matches
            );

        } catch (Exception e) {

            String error =
                    e.getMessage();

            if (error == null ||
                    error.trim().isEmpty()) {

                error =
                        "Invalid AcoustID response.";
            }

            return new AcoustIdResult(
                    false,
                    error,
                    response,
                    new ArrayList<AcoustIdMatch>()
            );
        }
    }

    private String extractArtists(
            JSONObject recording) {

        JSONArray artists =
                recording.optJSONArray(
                        "artists"
                );

        if (artists == null ||
                artists.length() == 0) {

            return null;
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
                i < artists.length();
                i++) {

            JSONObject artist =
                    artists.optJSONObject(
                            i
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
        }

        if (builder.length() == 0) {
            return null;
        }

        return builder.toString();
    }

    private String extractAlbum(
            JSONObject recording) {

        JSONArray releaseGroups =
                recording.optJSONArray(
                        "releasegroups"
                );

        if (releaseGroups == null ||
                releaseGroups.length() == 0) {

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

    private String clean(
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

    private String readStream(
            InputStream inputStream) {

        if (inputStream == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        try {

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

        } catch (Exception ignored) {
        }

        return builder.toString();
    }

    public static class AcoustIdMatch {

        private final String acoustId;
        private final double score;
        private final String recordingId;
        private final String title;
        private final String artist;
        private final String album;
        private final long duration;
        private final String rawTrackResponse;

        public AcoustIdMatch(
                String acoustId,
                double score,
                String recordingId,
                String title,
                String artist,
                String album,
                long duration) {

            this(
                    acoustId,
                    score,
                    recordingId,
                    title,
                    artist,
                    album,
                    duration,
                    null
            );
        }

        public AcoustIdMatch(
                String acoustId,
                double score,
                String recordingId,
                String title,
                String artist,
                String album,
                long duration,
                String rawTrackResponse) {

            this.acoustId =
                    acoustId;

            this.score =
                    score;

            this.recordingId =
                    recordingId;

            this.title =
                    title;

            this.artist =
                    artist;

            this.album =
                    album;

            this.duration =
                    duration;

            this.rawTrackResponse =
                    rawTrackResponse;
        }

        public String getAcoustId() {
            return acoustId;
        }

        public double getScore() {
            return score;
        }

        public String getRecordingId() {
            return recordingId;
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

        public long getDuration() {
            return duration;
        }

        public String getRawTrackResponse() {
            return rawTrackResponse;
        }
    }

    public static class AcoustIdResult {

        private final boolean success;
        private final String message;
        private final String rawResponse;
        private final ArrayList<AcoustIdMatch> matches;

        public AcoustIdResult(
                boolean success,
                String message) {

            this(
                    success,
                    message,
                    null,
                    new ArrayList<AcoustIdMatch>()
            );
        }

        public AcoustIdResult(
                boolean success,
                String message,
                ArrayList<AcoustIdMatch> matches) {

            this(
                    success,
                    message,
                    null,
                    matches
            );
        }

        public AcoustIdResult(
                boolean success,
                String message,
                String rawResponse,
                ArrayList<AcoustIdMatch> matches) {

            this.success =
                    success;

            this.message =
                    message;

            this.rawResponse =
                    rawResponse;

            this.matches =
                    matches != null
                            ? matches
                            : new ArrayList<AcoustIdMatch>();
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public ArrayList<AcoustIdMatch>
                getMatches() {

            return matches;
        }
    }
}