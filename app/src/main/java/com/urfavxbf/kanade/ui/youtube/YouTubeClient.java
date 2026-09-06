package com.urfavxbf.kanade.ui.youtube;

import com.urfavxbf.kanade.MusicRepository;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class YouTubeClient {

    private static final Object INIT_LOCK = new Object();
    private static final long STREAM_CACHE_SAFETY_MS = 60_000L;
    private static final long UNKNOWN_EXPIRY_MS = 180_000L;
    private static final int MAX_RESULTS = 40;
    private static final int MAX_AUDIO_RESOLUTION_ATTEMPTS = 3;
    private static final long FAILURE_BACKOFF_MS = 5_000L;

    private static volatile boolean initialized;
    private static final Map<String, CachedStream> STREAM_CACHE = new HashMap<>();
    private static final Map<String, Long> RESOLUTION_FAILURES = new HashMap<>();
    private static final Map<String, Object> RESOLUTION_LOCKS = new HashMap<>();
    private static final Map<String, Track> TRACK_METADATA = new HashMap<>();

    private YouTubeClient() {
    }

    public static ArrayList<Track> searchTracks(String query) throws Exception {
        ensureInitialized();

        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        StreamingService service = NewPipe.getService("YouTube");
        ArrayList<Track> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        SearchExtractor extractor = service.getSearchExtractor(query.trim());
        extractor.fetchPage();
        collectSearchItems(extractor.getInitialPage().getItems(), results, seen);

        results.sort(Comparator.comparingInt((Track track) -> relevance(normalizedQuery, track)).reversed());

        if (results.size() < 8) {
            String[] tokens = normalizedQuery.split(" ");
            for (String token : tokens) {
                if (token.length() < 2) continue;
                try {
                    SearchExtractor fallback = service.getSearchExtractor(token);
                    fallback.fetchPage();
                    collectSearchItems(fallback.getInitialPage().getItems(), results, seen);
                } catch (Exception ignored) {
                }
            }
            results.sort(Comparator.comparingInt((Track track) -> relevance(normalizedQuery, track)).reversed());
        }

        if (results.size() > MAX_RESULTS) {
            return new ArrayList<>(results.subList(0, MAX_RESULTS));
        }
        return results;
    }

    public static String resolveStreamUrl(String videoId) throws Exception {
        ensureInitialized();

        String id = safe(videoId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Missing YouTube video id");
        }

        CachedStream cached;
        synchronized (STREAM_CACHE) {
            cached = STREAM_CACHE.get(id);
            if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
                publishTrackMetadata(id, cached.url);
                return cached.url;
            }
            STREAM_CACHE.remove(id);
        }

        Object lock;
        synchronized (RESOLUTION_LOCKS) {
            lock = RESOLUTION_LOCKS.get(id);
            if (lock == null) {
                lock = new Object();
                RESOLUTION_LOCKS.put(id, lock);
            }
        }

        synchronized (lock) {
            synchronized (STREAM_CACHE) {
                cached = STREAM_CACHE.get(id);
                if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
                    publishTrackMetadata(id, cached.url);
                    return cached.url;
                }
            }

            synchronized (RESOLUTION_FAILURES) {
                Long failedAt = RESOLUTION_FAILURES.get(id);
                if (failedAt != null && System.currentTimeMillis() - failedAt < FAILURE_BACKOFF_MS) {
                    throw new IllegalStateException("YouTube stream resolution is temporarily unavailable");
                }
            }

            Exception lastError = null;
            String watchUrl = id.startsWith("http") ? id : "https://www.youtube.com/watch?v=" + id;

            for (int attempt = 0; attempt < MAX_AUDIO_RESOLUTION_ATTEMPTS; attempt++) {
                try {
                    StreamInfo info = StreamInfo.getInfo(watchUrl);
                    publishTrackMetadata(id, info);

                    List<AudioStream> streams = info.getAudioStreams();
                    if (streams == null || streams.isEmpty()) {
                        throw new IllegalStateException("YouTube returned no playable audio streams");
                    }

                    AudioStream selected = selectBestAudioStream(streams, attempt);
                    String streamUrl = safe(selected.getContent());
                    if (streamUrl.isEmpty()) {
                        streamUrl = safe(selected.getUrl());
                    }
                    if (streamUrl.isEmpty()) {
                        throw new IllegalStateException("YouTube returned an empty audio stream URL");
                    }

                    long expiresAt = extractExpireTimestamp(streamUrl);
                    if (expiresAt <= 0) {
                        expiresAt = System.currentTimeMillis() + UNKNOWN_EXPIRY_MS;
                    }

                    synchronized (STREAM_CACHE) {
                        STREAM_CACHE.put(id, new CachedStream(streamUrl, expiresAt));
                    }
                    synchronized (RESOLUTION_FAILURES) {
                        RESOLUTION_FAILURES.remove(id);
                    }
                    publishTrackMetadata(id, streamUrl);
                    return streamUrl;
                } catch (Exception e) {
                    lastError = e;
                }
            }

            synchronized (RESOLUTION_FAILURES) {
                RESOLUTION_FAILURES.put(id, System.currentTimeMillis());
            }
            throw lastError == null
                    ? new IllegalStateException("Unable to resolve YouTube stream")
                    : lastError;
        }
    }

    public static void invalidateStream(String videoId) {
        if (videoId == null) {
            return;
        }
        synchronized (STREAM_CACHE) {
            STREAM_CACHE.remove(videoId);
        }
        synchronized (RESOLUTION_FAILURES) {
            RESOLUTION_FAILURES.remove(videoId);
        }
    }

    private static void collectSearchItems(List<InfoItem> source, ArrayList<Track> results, Set<String> seen) {
        if (source == null) return;
        for (InfoItem item : source) {
            if (!(item instanceof StreamInfoItem)) continue;
            StreamInfoItem stream = (StreamInfoItem) item;
            String url = safe(stream.getUrl());
            String title = safe(stream.getName());
            if (url.isEmpty() || title.isEmpty() || !seen.add(url)) continue;

            String artist = safe(stream.getUploaderName());
            String artwork = null;
            List<Image> thumbnails = stream.getThumbnails();
            if (thumbnails != null && !thumbnails.isEmpty()) {
                artwork = safe(thumbnails.get(thumbnails.size() - 1).getUrl());
            }
            Track track = new Track(extractVideoId(url), title, artist, artwork, url, stream.getDuration());
            results.add(track);
            synchronized (TRACK_METADATA) {
                TRACK_METADATA.put(track.id, track);
                while (TRACK_METADATA.size() > 200) {
                    String firstKey = TRACK_METADATA.keySet().iterator().next();
                    TRACK_METADATA.remove(firstKey);
                }
            }
        }
    }

    private static void publishTrackMetadata(String id, String streamUrl) {
        Track track;
        synchronized (TRACK_METADATA) {
            track = TRACK_METADATA.get(id);
        }
        if (track == null) {
            return;
        }
        MusicRepository.registerRemoteSong(
                streamUrl,
                track.title,
                track.artist,
                track.artworkUrl,
                track.durationSeconds
        );
    }

    private static void publishTrackMetadata(String id, StreamInfo info) {
        if (info == null) {
            return;
        }

        Track track;
        synchronized (TRACK_METADATA) {
            track = TRACK_METADATA.get(id);
        }

        if (track != null) {
            return;
        }

        String title = safe(info.getName());
        String artist = safe(info.getUploaderName());
        String artwork = null;
        List<Image> thumbnails = info.getThumbnails();
        if (thumbnails != null && !thumbnails.isEmpty()) {
            artwork = safe(thumbnails.get(thumbnails.size() - 1).getUrl());
        }

        track = new Track(
                id,
                title,
                artist,
                artwork,
                safe(info.getOriginalUrl()),
                info.getDuration()
        );

        synchronized (TRACK_METADATA) {
            TRACK_METADATA.put(id, track);
        }
    }

    private static AudioStream selectBestAudioStream(List<AudioStream> streams, int attempt) {
        ArrayList<AudioStream> candidates = new ArrayList<>();
        for (AudioStream stream : streams) {
            if (stream == null) continue;
            String content = safe(stream.getContent());
            if (content.isEmpty()) content = safe(stream.getUrl());
            if (!content.isEmpty()) candidates.add(stream);
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No usable YouTube audio stream found");
        }

        candidates.sort((a, b) -> Integer.compare(normalizedBitrate(b), normalizedBitrate(a)));
        int index = Math.min(attempt, candidates.size() - 1);
        return candidates.get(index);
    }

    private static int normalizedBitrate(AudioStream stream) {
        int bitrate = stream.getAverageBitrate();
        return bitrate == AudioStream.UNKNOWN_BITRATE ? -1 : bitrate;
    }

    private static int relevance(String query, Track track) {
        String title = normalize(track.title);
        String artist = normalize(track.artist);
        int score = 0;
        if (title.equals(query)) score += 1000;
        if (title.startsWith(query)) score += 700;
        if (title.contains(query)) score += 450;
        if (artist.equals(query)) score += 500;
        if (artist.contains(query)) score += 250;
        for (String word : query.split(" ")) {
            if (!word.isEmpty() && title.contains(word)) score += 50;
            if (!word.isEmpty() && artist.contains(word)) score += 30;
        }
        return score;
    }

    private static long extractExpireTimestamp(String url) {
        try {
            String marker = "expire=";
            int start = url.indexOf(marker);
            if (start < 0) return -1L;
            start += marker.length();
            int end = url.indexOf('&', start);
            String value = end < 0 ? url.substring(start) : url.substring(start, end);
            return Long.parseLong(value) * 1000L - STREAM_CACHE_SAFETY_MS;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static String extractVideoId(String url) {
        int marker = url.indexOf("v=");
        if (marker >= 0) {
            int start = marker + 2;
            int end = url.indexOf('&', start);
            return end >= 0 ? url.substring(start, end) : url.substring(start);
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash + 1 < url.length() ? url.substring(slash + 1) : url;
    }

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (INIT_LOCK) {
            if (initialized) return;
            NewPipe.init(new YouTubeDownloader());
            initialized = true;
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Track {
        public final String id;
        public final String title;
        public final String artist;
        public final String artworkUrl;
        public final String url;
        public final long durationSeconds;

        public Track(String id, String title, String artist, String artworkUrl, String url, long durationSeconds) {
            this.id = safe(id);
            this.title = safe(title);
            this.artist = safe(artist);
            this.artworkUrl = artworkUrl;
            this.url = safe(url);
            this.durationSeconds = durationSeconds;
        }
    }

    private static final class CachedStream {
        final String url;
        final long expiresAtMs;

        CachedStream(String url, long expiresAtMs) {
            this.url = url;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
