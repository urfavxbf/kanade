package com.urfavxbf.kanade.ui.youtube;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.urfavxbf.kanade.BuildConfig;
import com.urfavxbf.kanade.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class YouTubePlaybackManager {
    public static final String ACTION_STATE_CHANGED = "com.urfavxbf.kanade.YOUTUBE_STATE_CHANGED";
    public static final String EXTRA_VIDEO_ID = "youtube_video_id";
    public static final String EXTRA_TITLE = "youtube_title";
    public static final String EXTRA_CHANNEL = "youtube_channel";
    public static final String EXTRA_THUMBNAIL = "youtube_thumbnail";
    public static final String EXTRA_PLAYING = "youtube_playing";
    public static final String EXTRA_AUDIO_ONLY = "youtube_audio_only";
    public static final String EXTRA_RADIO = "youtube_radio";
    public static final String EXTRA_POSITION = "youtube_position";
    public static final String EXTRA_DURATION = "youtube_duration";

    private static final String APP_ORIGIN = "https://com.urfavxbf.kanade";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kanade-YouTube-Images");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService RADIO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kanade-YouTube-Radio");
        thread.setDaemon(true);
        return thread;
    });
    private static final Runnable PROGRESS_POLL = new Runnable() {
        @Override
        public void run() {
            if (player != null && !videoId.isEmpty()) {
                player.evaluateJavascript("reportProgress();", null);
                MAIN.postDelayed(this, 500L);
            }
        }
    };

    private static Context appContext;
    private static WebView player;
    private static String videoId = "";
    private static String title = "";
    private static String channel = "";
    private static String thumbnailUrl = "";
    private static boolean playing;
    private static boolean audioOnly;
    private static boolean radio;
    private static boolean radioLoading;
    private static double positionSeconds;
    private static double durationSeconds;

    private static final ArrayList<QueueItem> queue = new ArrayList<>();
    private static int queueIndex = -1;

    private static WeakReference<View> miniRoot = new WeakReference<>(null);
    private static WeakReference<ImageView> miniThumb = new WeakReference<>(null);
    private static WeakReference<TextView> miniTitle = new WeakReference<>(null);
    private static WeakReference<TextView> miniArtist = new WeakReference<>(null);
    private static WeakReference<ImageButton> miniPlayPause = new WeakReference<>(null);
    private static WeakReference<View> fullPlayerAlbumCard = new WeakReference<>(null);

    private YouTubePlaybackManager() {
    }

    public static boolean isActive() { return player != null && !videoId.isEmpty(); }
    public static boolean isPlaying() { return playing; }
    public static boolean isAudioOnly() { return audioOnly; }
    public static boolean isRadioEnabled() { return radio; }
    public static boolean isRadioLoading() { return radioLoading; }
    public static String getVideoId() { return videoId; }
    public static String getTitle() { return title; }
    public static String getChannel() { return channel; }
    public static String getThumbnailUrl() { return thumbnailUrl; }
    public static double getPositionSeconds() { return positionSeconds; }
    public static double getDurationSeconds() { return durationSeconds; }
    public static List<QueueItem> getQueue() { return Collections.unmodifiableList(new ArrayList<>(queue)); }

    public static boolean isInMixQueue(String id) {
        if (TextUtils.isEmpty(id) || !radio) return false;
        return findQueueIndex(id) >= 0;
    }

    public static void addToQueue(String id, String songTitle, String songChannel, String thumb) {
        if (TextUtils.isEmpty(id) || findQueueIndex(id) >= 0) return;
        queue.add(new QueueItem(id, songTitle, songChannel, thumb));
        if (queueIndex < 0) queueIndex = 0;
        broadcastState();
    }

    public static void clearQueue() {
        queue.clear();
        queueIndex = -1;
        broadcastState();
    }

    public static void setRadioEnabled(boolean enabled) {
        if (radio == enabled) {
            broadcastState();
            return;
        }
        radio = enabled;
        if (enabled && isActive() && !hasUpcomingItem() && !radioLoading) {
            fetchRadioAndPlay();
        } else {
            broadcastState();
        }
    }

    public static void playQueueItem(int index) {
        if (index < 0 || index >= queue.size()) return;
        QueueItem item = queue.get(index);
        queueIndex = index;
        play(appContext, item.videoId, item.title, item.channel, item.thumbnailUrl, audioOnly);
    }

    public static void next() {
        if (queueIndex >= 0 && queueIndex + 1 < queue.size()) {
            playQueueItem(queueIndex + 1);
            return;
        }
        if (radio && !radioLoading) {
            fetchRadioAndPlay();
            return;
        }
        playing = false;
        updateMini();
        broadcastState();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public static void play(Context context, String id, String songTitle, String songChannel,
            String thumb, boolean compact) {
        if (context == null || TextUtils.isEmpty(id)) return;
        ensurePlayer(context.getApplicationContext());
        videoId = id;
        title = TextUtils.isEmpty(songTitle) ? "YouTube video" : songTitle;
        channel = TextUtils.isEmpty(songChannel) ? "YouTube" : songChannel;
        thumbnailUrl = TextUtils.isEmpty(thumb)
                ? "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg" : thumb;
        positionSeconds = 0d;
        durationSeconds = 0d;
        playing = false;
        audioOnly = compact;

        int existingIndex = findQueueIndex(id);
        if (existingIndex >= 0) {
            queueIndex = existingIndex;
            QueueItem existing = queue.get(existingIndex);
            if (TextUtils.isEmpty(existing.thumbnailUrl) && !thumbnailUrl.isEmpty()) {
                queue.set(existingIndex, new QueueItem(id, title, channel, thumbnailUrl));
            }
        } else {
            queue.add(new QueueItem(id, title, channel, thumbnailUrl));
            queueIndex = queue.size() - 1;
        }

        updateMini();
        loadThumbnailIntoMini(thumbnailUrl);
        MAIN.removeCallbacks(PROGRESS_POLL);

        String escapedId = escapeJs(id);
        String html = "<!doctype html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">"
                + "<style>html,body,#player{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden;}iframe{border:0;width:100%;height:100%;display:block;background:#000;}</style></head><body>"
                + "<div id=\"player\"></div><script>var player;"
                + "function onYouTubeIframeAPIReady(){player=new YT.Player('player',{height:'100%',width:'100%',videoId:'" + escapedId + "',"
                + "playerVars:{playsinline:1,rel:0,controls:0,enablejsapi:1,modestbranding:1,iv_load_policy:3,origin:'https://com.urfavxbf.kanade'},"
                + "events:{onReady:onReady,onStateChange:onState,onError:onError,onAutoplayBlocked:onBlocked}});}"
                + "function onReady(){KanadePlayer.ready();}function onState(e){KanadePlayer.state(e.data);}"
                + "function onError(e){KanadePlayer.error(e.data);}function onBlocked(){KanadePlayer.blocked();}"
                + "function playYT(){if(player)player.playVideo();}function pauseYT(){if(player)player.pauseVideo();}"
                + "function seekYT(seconds){if(player)player.seekTo(seconds,true);}"
                + "function reportProgress(){if(player)KanadePlayer.progress(player.getCurrentTime()||0,player.getDuration()||0);}"
                + "</script><script src=\"https://www.youtube.com/iframe_api\"></script></body></html>";
        player.setAlpha(compact ? 0f : 1f);
        player.loadDataWithBaseURL(APP_ORIGIN + "/", html, "text/html", "UTF-8", APP_ORIGIN + "/");
    }

    public static void attachTo(View host) {
        if (host == null || player == null || !(host instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup group = (android.view.ViewGroup) host;
        if (player.getParent() != group) {
            detachFromParent();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            group.addView(player, 0, params);
        } else if (group.indexOfChild(player) != 0) {
            group.removeView(player);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            group.addView(player, 0, params);
        }

        View albumCard = group.findViewById(R.id.fullPlayerAlbumCard);
        if (albumCard != null) {
            fullPlayerAlbumCard = new WeakReference<>(albumCard);
            albumCard.setVisibility(View.GONE);
        }

        player.setAlpha(audioOnly ? 0f : 1f);
        player.post(() -> resizeVideoPlayer(group));
    }

    public static void detachFromHost() {
        detachFromParent();
        fullPlayerAlbumCard.clear();
    }

    private static void resizeVideoPlayer(android.view.ViewGroup group) {
        if (player == null || player.getParent() != group || group.getWidth() <= 0 || group.getHeight() <= 0) return;

        int containerWidth = group.getWidth();
        int containerHeight = group.getHeight();
        float containerRatio = containerWidth / (float) containerHeight;

        int width;
        int height;
        if (containerRatio > 16f / 9f) {
            height = containerHeight;
            width = Math.round(height * 16f / 9f);
        } else {
            width = containerWidth;
            height = Math.round(width * 9f / 16f);
        }

        if (width <= 0 || height <= 0) return;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
        player.setLayoutParams(params);
    }

    public static void moveToGlobalHost(View host) {
        if (host == null || player == null || !(host instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup group = (android.view.ViewGroup) host;
        if (player.getParent() != group) {
            detachFromParent();
            group.addView(player, new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        }
        player.setAlpha(0f);
    }

    public static void setMiniViews(View root, ImageView thumb, TextView titleView,
            TextView artistView, ImageButton playPause) {
        miniRoot = new WeakReference<>(root);
        miniThumb = new WeakReference<>(thumb);
        miniTitle = new WeakReference<>(titleView);
        miniArtist = new WeakReference<>(artistView);
        miniPlayPause = new WeakReference<>(playPause);
        updateMini();
        loadThumbnailIntoMini(thumbnailUrl);
    }

    public static void toggle() {
        if (player == null || videoId.isEmpty()) return;
        player.evaluateJavascript(playing ? "pauseYT();" : "playYT();", null);
    }

    public static void seekTo(double seconds) {
        if (player == null || videoId.isEmpty() || Double.isNaN(seconds)) return;
        double target = Math.max(0d, Math.min(seconds, durationSeconds > 0d ? durationSeconds : seconds));
        positionSeconds = target;
        player.evaluateJavascript("seekYT(" + target + ");", null);
        broadcastState();
    }

    public static void setAudioOnly(boolean enabled) {
        audioOnly = enabled;
        if (player != null) player.setAlpha(enabled ? 0f : 1f);
        updateMini();
        broadcastState();
    }

    public static void showVideo() {
        setAudioOnly(false);
    }

    public static void stop() {
        if (player != null) player.evaluateJavascript("pauseYT();", null);
        playing = false;
        MAIN.removeCallbacks(PROGRESS_POLL);
        View albumCard = fullPlayerAlbumCard.get();
        if (albumCard != null) {
            albumCard.setVisibility(View.VISIBLE);
        }
        updateMini();
        broadcastState();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private static void ensurePlayer(Context context) {
        appContext = context;
        if (player != null) return;
        player = new YouTubeWebView(context);
        WebSettings settings = player.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        player.setBackgroundColor(Color.BLACK);
        player.setOverScrollMode(View.OVER_SCROLL_NEVER);
        player.addJavascriptInterface(new Bridge(), "KanadePlayer");
        player.setWebViewClient(new WebViewClient());
    }

    private static int findQueueIndex(String id) {
        for (int i = 0; i < queue.size(); i++) {
            if (id.equals(queue.get(i).videoId)) return i;
        }
        return -1;
    }

    private static boolean hasUpcomingItem() {
        return queueIndex >= 0 && queueIndex + 1 < queue.size();
    }

    private static void detachFromParent() {
        if (player != null && player.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) player.getParent()).removeView(player);
        }
    }

    private static String escapeJs(String value) {
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void updateMini() {
        MAIN.post(() -> {
            View root = miniRoot.get();
            ImageView thumb = miniThumb.get();
            TextView titleView = miniTitle.get();
            TextView artistView = miniArtist.get();
            ImageButton playPause = miniPlayPause.get();
            if (root == null) return;
            root.setVisibility(isActive() ? View.VISIBLE : View.GONE);
            if (titleView != null) titleView.setText(title);
            if (artistView != null) {
                artistView.setText(audioOnly ? channel + " • Audio only" : channel);
            }
            if (playPause != null) {
                playPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
                playPause.setContentDescription(playing
                        ? "Pause YouTube playback" : "Play YouTube playback");
            }
            if (thumb != null && thumbnailUrl.isEmpty()) {
                thumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        });
    }

    private static void loadThumbnailIntoMini(String url) {
        if (TextUtils.isEmpty(url)) return;
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap == null) return;
            MAIN.post(() -> {
                ImageView thumb = miniThumb.get();
                if (thumb != null && url.equals(thumbnailUrl)) thumb.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap downloadBitmap(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void fetchRadioAndPlay() {
        if (appContext == null || TextUtils.isEmpty(BuildConfig.YOUTUBE_API_KEY)
                || TextUtils.isEmpty(videoId)) {
            radioLoading = false;
            playing = false;
            updateMini();
            broadcastState();
            return;
        }

        radioLoading = true;
        broadcastState();
        final String seedVideoId = videoId;
        final String seedTitle = title;
        final String seedChannel = channel;

        RADIO_EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String query = URLEncoder.encode(seedTitle + " " + seedChannel, "UTF-8");
                String endpoint = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video"
                        + "&videoCategoryId=10&videoEmbeddable=true&videoSyndicated=true&maxResults=10&q="
                        + query + "&key=" + URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY, "UTF-8");
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                if (stream == null) throw new IllegalStateException("No YouTube radio response");
                String response = readResponse(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException("YouTube radio failed");
                }

                JSONArray items = new JSONObject(response).optJSONArray("items");
                ArrayList<QueueItem> additions = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        JSONObject id = item.optJSONObject("id");
                        JSONObject snippet = item.optJSONObject("snippet");
                        if (id == null || snippet == null) continue;
                        String idValue = id.optString("videoId", "").trim();
                        String itemTitle = snippet.optString("title", "").trim();
                        String itemChannel = snippet.optString("channelTitle", "").trim();
                        JSONObject thumbs = snippet.optJSONObject("thumbnails");
                        String thumb = "";
                        if (thumbs != null) {
                            JSONObject high = thumbs.optJSONObject("high");
                            JSONObject medium = thumbs.optJSONObject("medium");
                            if (high != null) thumb = high.optString("url", "").trim();
                            if (TextUtils.isEmpty(thumb) && medium != null) {
                                thumb = medium.optString("url", "").trim();
                            }
                        }
                        if (!TextUtils.isEmpty(idValue)
                                && !idValue.equals(seedVideoId)
                                && findQueueIndex(idValue) < 0) {
                            additions.add(new QueueItem(idValue, itemTitle, itemChannel, thumb));
                        }
                    }
                }

                MAIN.post(() -> {
                    radioLoading = false;
                    if (!radio) {
                        broadcastState();
                        return;
                    }
                    for (QueueItem item : additions) {
                        if (findQueueIndex(item.videoId) < 0) queue.add(item);
                    }
                    if (queueIndex + 1 < queue.size()) {
                        playQueueItem(queueIndex + 1);
                    } else {
                        playing = false;
                        updateMini();
                        broadcastState();
                    }
                });
            } catch (Exception ignored) {
                MAIN.post(() -> {
                    radioLoading = false;
                    playing = false;
                    updateMini();
                    broadcastState();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static String readResponse(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static void broadcastState() {
        if (appContext == null) return;
        Intent intent = new Intent(ACTION_STATE_CHANGED)
                .setPackage(appContext.getPackageName())
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_CHANNEL, channel)
                .putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                .putExtra(EXTRA_PLAYING, playing)
                .putExtra(EXTRA_AUDIO_ONLY, audioOnly)
                .putExtra(EXTRA_RADIO, radio)
                .putExtra(EXTRA_POSITION, positionSeconds)
                .putExtra(EXTRA_DURATION, durationSeconds);
        appContext.sendBroadcast(intent);
    }

    public static final class QueueItem {
        public final String videoId;
        public final String title;
        public final String channel;
        public final String thumbnailUrl;

        public QueueItem(String videoId, String title, String channel, String thumbnailUrl) {
            this.videoId = videoId;
            this.title = TextUtils.isEmpty(title) ? "YouTube video" : title;
            this.channel = TextUtils.isEmpty(channel) ? "YouTube" : channel;
            this.thumbnailUrl = TextUtils.isEmpty(thumbnailUrl)
                    ? "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg" : thumbnailUrl;
        }
    }

    private static final class Bridge {
        @JavascriptInterface
        public void ready() {
            MAIN.post(() -> {
                if (player != null) player.evaluateJavascript("playYT();", null);
                playing = true;
                MAIN.removeCallbacks(PROGRESS_POLL);
                MAIN.post(PROGRESS_POLL);
                updateMini();
                broadcastState();
            });
        }

        @JavascriptInterface
        public void state(int state) {
            MAIN.post(() -> {
                playing = state == 1 || state == 3;
                if (playing) {
                    MAIN.removeCallbacks(PROGRESS_POLL);
                    MAIN.post(PROGRESS_POLL);
                }
                if (state == 0) {
                    MAIN.removeCallbacks(PROGRESS_POLL);
                    next();
                    return;
                }
                if (!playing) MAIN.removeCallbacks(PROGRESS_POLL);
                updateMini();
                broadcastState();
            });
        }

        @JavascriptInterface
        public void progress(double position, double duration) {
            MAIN.post(() -> {
                positionSeconds = Math.max(0d, position);
                durationSeconds = Math.max(0d, duration);
                broadcastState();
            });
        }

        @JavascriptInterface
        public void error(int code) {
            MAIN.post(() -> {
                playing = false;
                MAIN.removeCallbacks(PROGRESS_POLL);
                updateMini();
                broadcastState();
            });
        }

        @JavascriptInterface
        public void blocked() {
            MAIN.post(() -> {
                if (player != null) player.evaluateJavascript("playYT();", null);
            });
        }
    }
}