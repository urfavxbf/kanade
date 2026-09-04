package com.urfavxbf.kanade;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MusicBrainzArtistPhotoClient {

    private static final String MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/artist";
    private static final String USER_AGENT = "KanadeMusicPlayer/1.0.0";

    public ArtistPhoto resolve(String artistName) {
        if (artistName == null || artistName.trim().isEmpty()) return null;
        HttpURLConnection connection = null;
        try {
            String query = URLEncoder.encode("artist:\"" + artistName.trim() + "\"", StandardCharsets.UTF_8.name());
            URL url = new URL(MUSICBRAINZ_URL + "?query=" + query + "&fmt=json&limit=3&inc=url-rels");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;

            String response = read(connection.getInputStream());
            JSONArray artists = new JSONObject(response).optJSONArray("artists");
            if (artists == null) return null;

            for (int i = 0; i < artists.length(); i++) {
                JSONObject artist = artists.optJSONObject(i);
                if (artist == null) continue;
                String name = artist.optString("name", "").trim();
                if (!artistName.trim().equalsIgnoreCase(name)) continue;
                String wikidataId = extractWikidataId(artist.optJSONArray("relations"));
                if (wikidataId == null) continue;
                String imageUrl = resolveWikidataImage(wikidataId);
                if (imageUrl == null) continue;
                Bitmap bitmap = downloadBitmap(imageUrl);
                if (bitmap != null) return new ArtistPhoto(name, bitmap);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private String extractWikidataId(JSONArray relations) {
        if (relations == null) return null;
        for (int i = 0; i < relations.length(); i++) {
            JSONObject relation = relations.optJSONObject(i);
            if (relation == null) continue;
            if (!"wikidata".equalsIgnoreCase(relation.optString("type", ""))) continue;
            String url = relation.optJSONObject("url") == null ? "" : relation.optJSONObject("url").optString("resource", "");
            int slash = url.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < url.length()) return url.substring(slash + 1);
        }
        return null;
    }

    private String resolveWikidataImage(String wikidataId) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://www.wikidata.org/wiki/Special:EntityData/" + URLEncoder.encode(wikidataId, StandardCharsets.UTF_8.name()) + ".json");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            JSONObject entity = new JSONObject(read(connection.getInputStream())).optJSONObject("entities");
            if (entity == null) return null;
            JSONObject item = entity.optJSONObject(wikidataId);
            JSONObject claims = item == null ? null : item.optJSONObject("claims");
            JSONArray images = claims == null ? null : claims.optJSONArray("P18");
            if (images == null || images.length() == 0) return null;
            JSONObject claim = images.optJSONObject(0);
            JSONObject mainsnak = claim == null ? null : claim.optJSONObject("mainsnak");
            JSONObject datavalue = mainsnak == null ? null : mainsnak.optJSONObject("datavalue");
            String fileName = datavalue == null ? null : datavalue.optString("value", null);
            if (fileName == null || fileName.trim().isEmpty()) return null;
            return "https://commons.wikimedia.org/wiki/Special:Redirect/file/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception ignored) {
            return null;
        } catch (OutOfMemoryError ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String read(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        return builder.toString();
    }

    public static final class ArtistPhoto {
        public final String artistName;
        public final Bitmap bitmap;
        public ArtistPhoto(String artistName, Bitmap bitmap) { this.artistName = artistName; this.bitmap = bitmap; }
    }
}
