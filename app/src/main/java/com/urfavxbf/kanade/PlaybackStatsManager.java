package com.urfavxbf.kanade;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class PlaybackStatsManager {

    private static final String PREF_NAME = "kanade_playback_stats";
    private static final String KEY_SONGS = "songs";
    private static final String KEY_DAYS = "days";
    private static final String FIELD_URI = "uri";
    private static final String FIELD_ARTIST = "artist";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_PLAY_COUNT = "playCount";
    private static final String FIELD_LAST_PLAYED = "lastPlayed";
    private static final String FIELD_DAY = "day";
    private static final String FIELD_MINUTES = "minutes";

    private final SharedPreferences preferences;

    public PlaybackStatsManager(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void recordPlay(AudioFile song) {
        if (song == null || isEmpty(song.getUri())) return;

        long now = System.currentTimeMillis();
        Map<String, SongStat> stats = readSongStats();
        SongStat stat = stats.get(song.getUri());
        if (stat == null) {
            stat = new SongStat(song.getUri(), clean(song.getTitle()), clean(song.getArtist()), 0L, 0L);
        }
        stat.title = clean(song.getTitle());
        stat.artist = clean(song.getArtist());
        stat.playCount++;
        stat.lastPlayed = now;
        stats.put(song.getUri(), stat);
        writeSongStats(stats);
    }

    public synchronized void recordUsageMinute(long timestamp) {
        recordUsageMinutes(timestamp, 1L);
    }

    public synchronized SongStat getSongStat(String uri) {
        if (isEmpty(uri)) return null;
        return readSongStats().get(uri);
    }

    public synchronized ArrayList<SongStat> getMostPlayed(int limit) {
        return sortedSongs(limit, true);
    }

    public synchronized ArrayList<SongStat> getLeastPlayed(int limit) {
        ArrayList<SongStat> songs = new ArrayList<>(readSongStats().values());
        Collections.sort(songs, (a, b) -> {
            int count = Long.compare(a.playCount, b.playCount);
            return count != 0 ? count : Long.compare(b.lastPlayed, a.lastPlayed);
        });
        return limit(songs, limit);
    }

    public synchronized ArrayList<SongStat> getRecentlyPlayed(int limit) {
        ArrayList<SongStat> songs = new ArrayList<>(readSongStats().values());
        Collections.sort(songs, (a, b) -> Long.compare(b.lastPlayed, a.lastPlayed));
        return limit(songs, limit);
    }

    public synchronized ArrayList<UsagePoint> getUsageLast7Days() {
        Map<String, Long> values = readUsage();
        ArrayList<UsagePoint> result = new ArrayList<>();
        long dayMs = 86_400_000L;
        long today = startOfDay(System.currentTimeMillis());
        for (int i = 6; i >= 0; i--) {
            long day = today - (i * dayMs);
            String key = String.valueOf(day);
            result.add(new UsagePoint(day, values.containsKey(key) ? values.get(key) : 0L));
        }
        return result;
    }

    public synchronized ArrayList<ArtistStat> getTopArtists(int limit) {
        HashMap<String, ArtistStat> artists = new HashMap<>();
        for (SongStat song : readSongStats().values()) {
            String artist = clean(song.artist);
            if (artist.isEmpty()) continue;
            String key = artist.toLowerCase(java.util.Locale.ROOT);
            ArtistStat stat = artists.get(key);
            if (stat == null) {
                stat = new ArtistStat(artist, 0L);
                artists.put(key, stat);
            }
            stat.playCount += song.playCount;
        }
        ArrayList<ArtistStat> result = new ArrayList<>(artists.values());
        Collections.sort(result, (a, b) -> Long.compare(b.playCount, a.playCount));
        return limit(result, limit);
    }

    private ArrayList<SongStat> sortedSongs(int limit, boolean descending) {
        ArrayList<SongStat> songs = new ArrayList<>(readSongStats().values());
        Comparator<SongStat> comparator = (a, b) -> Long.compare(a.playCount, b.playCount);
        if (descending) comparator = comparator.reversed();
        final Comparator<SongStat> finalComparator = comparator;
        Collections.sort(songs, (a, b) -> {
            int result = finalComparator.compare(a, b);
            return result != 0 ? result : Long.compare(b.lastPlayed, a.lastPlayed);
        });
        return limit(songs, limit);
    }

    private void recordUsageMinutes(long timestamp, long minutes) {
        Map<String, Long> usage = readUsage();
        String key = String.valueOf(startOfDay(timestamp));
        long current = usage.containsKey(key) ? usage.get(key) : 0L;
        usage.put(key, current + Math.max(0L, minutes));
        long cutoff = startOfDay(timestamp) - (30L * 86_400_000L);
        usage.entrySet().removeIf(entry -> parseLong(entry.getKey()) < cutoff);

        JSONArray array = new JSONArray();
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            JSONObject object = new JSONObject();
            putJson(object, FIELD_DAY, entry.getKey());
            putJson(object, FIELD_MINUTES, entry.getValue());
            array.put(object);
        }
        preferences.edit().putString(KEY_DAYS, array.toString()).apply();
    }

    private Map<String, SongStat> readSongStats() {
        HashMap<String, SongStat> result = new HashMap<>();
        String json = preferences.getString(KEY_SONGS, null);
        if (isEmpty(json)) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String uri = object.optString(FIELD_URI, "");
                if (uri.isEmpty()) continue;
                result.put(uri, new SongStat(uri, clean(object.optString(FIELD_TITLE, "")), clean(object.optString(FIELD_ARTIST, "")), Math.max(0L, object.optLong(FIELD_PLAY_COUNT, 0L)), Math.max(0L, object.optLong(FIELD_LAST_PLAYED, 0L))));
            }
        } catch (Exception ignored) {
            result.clear();
        }
        return result;
    }

    private void writeSongStats(Map<String, SongStat> stats) {
        JSONArray array = new JSONArray();
        for (SongStat stat : stats.values()) {
            JSONObject object = new JSONObject();
            putJson(object, FIELD_URI, stat.uri);
            putJson(object, FIELD_TITLE, stat.title);
            putJson(object, FIELD_ARTIST, stat.artist);
            putJson(object, FIELD_PLAY_COUNT, stat.playCount);
            putJson(object, FIELD_LAST_PLAYED, stat.lastPlayed);
            array.put(object);
        }
        preferences.edit().putString(KEY_SONGS, array.toString()).apply();
    }

    private void putJson(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException ignored) {
            // JSONObject.put() is checked in this Android JSON API; an invalid value is skipped.
        }
    }

    private Map<String, Long> readUsage() {
        HashMap<String, Long> result = new HashMap<>();
        String json = preferences.getString(KEY_DAYS, null);
        if (isEmpty(json)) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String day = object.optString(FIELD_DAY, "");
                if (!day.isEmpty()) result.put(day, Math.max(0L, object.optLong(FIELD_MINUTES, 0L)));
            }
        } catch (Exception ignored) {
            result.clear();
        }
        return result;
    }

    private long startOfDay(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long parseLong(String value) {
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return Long.MAX_VALUE; }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private boolean isEmpty(String value) { return value == null || value.trim().isEmpty(); }

    private <T> ArrayList<T> limit(ArrayList<T> values, int limit) {
        if (limit <= 0 || values.size() <= limit) return values;
        return new ArrayList<>(values.subList(0, limit));
    }

    public static final class SongStat {
        public final String uri;
        public String title;
        public String artist;
        public long playCount;
        public long lastPlayed;
        public SongStat(String uri, String title, String artist, long playCount, long lastPlayed) {
            this.uri = uri; this.title = title; this.artist = artist; this.playCount = playCount; this.lastPlayed = lastPlayed;
        }
    }

    public static final class ArtistStat {
        public final String artist;
        public long playCount;
        public ArtistStat(String artist, long playCount) { this.artist = artist; this.playCount = playCount; }
    }

    public static final class UsagePoint {
        public final long day;
        public final long minutes;
        public UsagePoint(long day, long minutes) { this.day = day; this.minutes = minutes; }
    }
}
