package com.urfavxbf.kanade.ui.audius;

import com.urfavxbf.kanade.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AudiusClient {

    private static final String API_BASE = "https://api.audius.co/v1";
    private static final int SEARCH_LIMIT = 50;
    private static final int MAX_RESULTS = 40;

    private AudiusClient() {
    }

    public static ArrayList<Track> searchTracks(String query) throws Exception {
        String originalQuery = query == null ? "" : query.trim();
        if (originalQuery.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = normalize(originalQuery);
        String[] queryTokens = tokens(normalizedQuery);
        Map<String, ScoredTrack> candidates = new HashMap<>();

        addSearchResults(candidates, originalQuery, queryTokens, 0);

        if (candidates.size() < 8 && queryTokens.length > 1) {
            for (String token : queryTokens) {
                if (token.length() < 2) {
                    continue;
                }
                addSearchResults(candidates, token, queryTokens, 1);
            }
        }

        ArrayList<ScoredTrack> scored = new ArrayList<>(candidates.values());
        Collections.sort(scored, new Comparator<ScoredTrack>() {
            @Override
            public int compare(ScoredTrack left, ScoredTrack right) {
                int score = Double.compare(right.score, left.score);
                if (score != 0) {
                    return score;
                }
                return Integer.compare(left.apiPosition, right.apiPosition);
            }
        });

        ArrayList<Track> tracks = new ArrayList<>();
        for (int i = 0; i < scored.size() && tracks.size() < MAX_RESULTS; i++) {
            ScoredTrack result = scored.get(i);
            if (result.score >= 18.0) {
                tracks.add(result.track);
            }
        }

        if (tracks.isEmpty()) {
            for (int i = 0; i < scored.size() && tracks.size() < MAX_RESULTS; i++) {
                tracks.add(scored.get(i).track);
            }
        }

        return tracks;
    }

    private static void addSearchResults(
            Map<String, ScoredTrack> candidates,
            String query,
            String[] originalTokens,
            int searchPass) throws Exception {
        String endpoint = API_BASE + "/tracks/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&limit=" + SEARCH_LIMIT
                + "&offset=0&sort_method=relevant";

        String apiKey = BuildConfig.AUDIUS_API_KEY;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            endpoint += "&api_key="
                    + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8.name());
        }

        JSONObject response = getJson(endpoint);
        JSONArray data = response.optJSONArray("data");
        if (data == null) {
            return;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) {
                continue;
            }

            Track track = parseTrack(item);
            if (track == null) {
                continue;
            }

            double score = scoreTrack(track, normalize(query), originalTokens, i, searchPass);
            ScoredTrack existing = candidates.get(track.id);
            if (existing == null || score > existing.score) {
                candidates.put(track.id, new ScoredTrack(track, score, i));
            }
        }
    }

    private static Track parseTrack(JSONObject item) {
        String id = item.optString("id", "").trim();
        String title = item.optString("title", "").trim();
        JSONObject user = item.optJSONObject("user");
        String artist = user == null
                ? "Unknown artist"
                : user.optString("name", "Unknown artist").trim();

        if (artist.isEmpty()) {
            artist = "Unknown artist";
        }

        String artwork = extractArtwork(item.optJSONObject("artwork"));
        long duration = item.optLong("duration", 0L);
        boolean streamable = item.optBoolean("isStreamable", true);

        if (item.has("isStreamable") && item.opt("isStreamable") instanceof String) {
            streamable = Boolean.parseBoolean(item.optString("isStreamable", "true"));
        }

        if (id.isEmpty() || title.isEmpty() || !streamable) {
            return null;
        }

        return new Track(id, title, artist, artwork, duration);
    }

    private static double scoreTrack(
            Track track,
            String query,
            String[] queryTokens,
            int apiPosition,
            int searchPass) {
        String title = normalize(track.title);
        String artist = normalize(track.artist);
        String combined = title + " " + artist;
        double score = searchPass == 0 ? 10.0 : 0.0;

        if (title.equals(query)) {
            score += 100.0;
        } else if (title.startsWith(query)) {
            score += 78.0;
        } else if (title.contains(query)) {
            score += 62.0;
        }

        if (artist.equals(query)) {
            score += 72.0;
        } else if (artist.startsWith(query)) {
            score += 48.0;
        } else if (artist.contains(query)) {
            score += 36.0;
        }

        int matchedTokens = 0;
        for (String token : queryTokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (title.equals(token)) {
                score += 32.0;
                matchedTokens++;
            } else if (title.startsWith(token)) {
                score += 24.0;
                matchedTokens++;
            } else if (title.contains(token)) {
                score += 18.0;
                matchedTokens++;
            } else if (artist.contains(token)) {
                score += 14.0;
                matchedTokens++;
            } else if (combined.contains(token)) {
                score += 7.0;
                matchedTokens++;
            }
        }

        if (queryTokens.length > 0 && matchedTokens == queryTokens.length) {
            score += 30.0;
        } else if (matchedTokens == 0) {
            score -= 30.0;
        }

        score -= Math.min(apiPosition, 30) * 0.15;
        return score;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private static String[] tokens(String value) {
        if (value == null || value.isEmpty()) {
            return new String[0];
        }

        String[] raw = value.split("\\s+");
        ArrayList<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String token : raw) {
            if (token.length() >= 2 && seen.add(token)) {
                result.add(token);
            }
        }
        return result.toArray(new String[0]);
    }

    public static String resolveStreamUrl(String trackId) throws Exception {
        String endpoint = API_BASE + "/tracks/"
                + URLEncoder.encode(trackId, StandardCharsets.UTF_8.name())
                + "/stream?no_redirect=true";

        String apiKey = BuildConfig.AUDIUS_API_KEY;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            endpoint += "&api_key="
                    + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8.name());
        }

        JSONObject response = getJson(endpoint);
        JSONObject data = response.optJSONObject("data");
        if (data != null) {
            String url = data.optString("url", "").trim();
            if (!url.isEmpty()) {
                return url;
            }
        }

        String directUrl = response.optString("data", "").trim();
        if (!directUrl.isEmpty() && directUrl.startsWith("http")) {
            return directUrl;
        }

        throw new IllegalStateException("Audius did not return a playable stream URL");
    }

    private static String extractArtwork(JSONObject artwork) {
        if (artwork == null) {
            return "";
        }

        String[] keys = {"_480x480", "480x480", "_1000x1000", "1000x1000", "_150x150", "150x150"};
        for (String key : keys) {
            String url = artwork.optString(key, "").trim();
            if (!url.isEmpty()) {
                return url;
            }
        }
        return "";
    }

    private static JSONObject getJson(String endpoint) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                throw new IllegalStateException("Audius returned no response");
            }

            String response = readResponse(stream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Audius request failed (HTTP " + responseCode + "): "
                        + extractErrorMessage(response));
            }

            return new JSONObject(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String extractErrorMessage(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject error = json.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "").trim();
                if (!message.isEmpty()) {
                    return message;
                }
            }
            String message = json.optString("message", "").trim();
            return message.isEmpty() ? "Unknown error" : message;
        } catch (Exception ignored) {
            return response == null || response.trim().isEmpty() ? "Unknown error" : response;
        }
    }

    private static String readResponse(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static final class ScoredTrack {
        final Track track;
        final double score;
        final int apiPosition;

        ScoredTrack(Track track, double score, int apiPosition) {
            this.track = track;
            this.score = score;
            this.apiPosition = apiPosition;
        }
    }

    public static final class Track {
        public final String id;
        public final String title;
        public final String artist;
        public final String artworkUrl;
        public final long durationSeconds;

        public Track(String id, String title, String artist, String artworkUrl, long durationSeconds) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.artworkUrl = artworkUrl;
            this.durationSeconds = durationSeconds;
        }
    }
}
