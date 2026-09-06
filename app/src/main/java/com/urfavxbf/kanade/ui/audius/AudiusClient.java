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
import java.util.ArrayList;

public final class AudiusClient {

    private static final String API_BASE = "https://api.audius.co/v1";

    private AudiusClient() {
    }

    public static ArrayList<Track> searchTracks(String query) throws Exception {
        String endpoint = API_BASE + "/tracks/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&limit=20&sort_method=relevant";

        String apiKey = BuildConfig.AUDIUS_API_KEY;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            endpoint += "&api_key="
                    + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8.name());
        }

        JSONObject response = getJson(endpoint);
        JSONArray data = response.optJSONArray("data");
        ArrayList<Track> tracks = new ArrayList<>();

        if (data == null) {
            return tracks;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String id = item.optString("id", "").trim();
            String title = item.optString("title", "").trim();
            JSONObject user = item.optJSONObject("user");
            String artist = user == null ? "Unknown artist" : user.optString("name", "Unknown artist").trim();
            if (artist.isEmpty()) {
                artist = "Unknown artist";
            }

            String artwork = extractArtwork(item.optJSONObject("artwork"));
            long duration = item.optLong("duration", 0L);
            boolean streamable = item.optBoolean("isStreamable", true);

            if (!id.isEmpty() && !title.isEmpty() && streamable) {
                tracks.add(new Track(id, title, artist, artwork, duration));
            }
        }

        return tracks;
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
