package com.urfavxbf.kanade.ui.youtube;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.urfavxbf.kanade.R;

import java.lang.ref.WeakReference;

public final class YouTubePlaybackManager {
    private static final String APP_ORIGIN = "https://com.urfavxbf.kanade";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WebView player;
    private static String videoId = "";
    private static String title = "";
    private static String channel = "";
    private static String thumbnailUrl = "";
    private static boolean playing;
    private static boolean audioOnly;

    private static WeakReference<View> miniRoot = new WeakReference<>(null);
    private static WeakReference<ImageView> miniThumb = new WeakReference<>(null);
    private static WeakReference<TextView> miniTitle = new WeakReference<>(null);
    private static WeakReference<TextView> miniArtist = new WeakReference<>(null);
    private static WeakReference<ImageButton> miniPlayPause = new WeakReference<>(null);

    private YouTubePlaybackManager() {
    }

    public static boolean isActive() {
        return player != null && !videoId.isEmpty();
    }

    public static boolean isPlaying() {
        return playing;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public static void play(Context context, String id, String songTitle, String songChannel,
            String thumb, boolean compact) {
        if (context == null || TextUtils.isEmpty(id)) {
            return;
        }
        ensurePlayer(context.getApplicationContext());
        videoId = id;
        title = TextUtils.isEmpty(songTitle) ? "YouTube video" : songTitle;
        channel = TextUtils.isEmpty(songChannel) ? "YouTube" : songChannel;
        thumbnailUrl = thumb == null ? "" : thumb;
        playing = false;
        audioOnly = compact;
        updateMini();

        String escapedId = escapeJs(id);
        String html = "<!doctype html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body,#player{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden;}"
                + "iframe{border:0;width:100%;height:100%;display:block;}</style></head><body>"
                + "<div id=\"player\"></div><script>var player;"
                + "function onYouTubeIframeAPIReady(){player=new YT.Player('player',{height:'100%',width:'100%',"
                + "videoId:'" + escapedId + "',playerVars:{playsinline:1,rel:0,controls:1,enablejsapi:1,"
                + "origin:'https://com.urfavxbf.kanade'},events:{onReady:onReady,onStateChange:onState,onError:onError,"
                + "onAutoplayBlocked:onBlocked}});}"
                + "function onReady(){KanadePlayer.ready();}"
                + "function onState(e){KanadePlayer.state(e.data);}"
                + "function onError(e){KanadePlayer.error(e.data);}"
                + "function onBlocked(){KanadePlayer.blocked();}"
                + "function playYT(){if(player)player.playVideo();}"
                + "function pauseYT(){if(player)player.pauseVideo();}"
                + "</script><script src=\"https://www.youtube.com/iframe_api\"></script></body></html>";
        player.setAlpha(compact ? 0f : 1f);
        player.loadDataWithBaseURL(APP_ORIGIN + "/", html, "text/html", "UTF-8", APP_ORIGIN + "/");
    }

    public static void attachTo(View host) {
        if (host == null || player == null || !(host instanceof android.view.ViewGroup)) {
            return;
        }
        android.view.ViewGroup group = (android.view.ViewGroup) host;
        if (player.getParent() != group) {
            detachFromParent();
            group.addView(player, new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        }
        player.setAlpha(audioOnly ? 0f : 1f);
    }

    public static void moveToGlobalHost(View host) {
        if (host == null || player == null || !(host instanceof android.view.ViewGroup)) {
            return;
        }
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
    }

    public static void toggle() {
        if (player == null || videoId.isEmpty()) {
            return;
        }
        player.evaluateJavascript(playing ? "pauseYT();" : "playYT();", null);
    }

    public static void setAudioOnly(boolean enabled) {
        audioOnly = enabled;
        if (player != null) {
            player.setAlpha(enabled ? 0f : 1f);
        }
        updateMini();
    }

    public static void showVideo() {
        audioOnly = false;
        if (player != null) {
            player.setAlpha(1f);
        }
        updateMini();
    }

    public static void stop() {
        if (player != null) {
            player.evaluateJavascript("pauseYT();", null);
        }
        playing = false;
        updateMini();
    }

    private static void ensurePlayer(Context context) {
        if (player != null) {
            return;
        }
        player = new YouTubeWebView(context);
        WebSettings settings = player.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        player.setBackgroundColor(Color.BLACK);
        player.addJavascriptInterface(new Bridge(), "KanadePlayer");
        player.setWebViewClient(new WebViewClient());
    }

    private static void detachFromParent() {
        if (player != null && player.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) player.getParent()).removeView(player);
        }
    }

    private static String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void updateMini() {
        MAIN.post(() -> {
            View root = miniRoot.get();
            TextView titleView = miniTitle.get();
            TextView artistView = miniArtist.get();
            ImageButton playPause = miniPlayPause.get();
            if (root == null) {
                return;
            }
            root.setVisibility(isActive() ? View.VISIBLE : View.GONE);
            if (titleView != null) titleView.setText(title);
            if (artistView != null) artistView.setText(audioOnly ? channel + " • Audio only" : channel);
            if (playPause != null) {
                playPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
                playPause.setContentDescription(playing ? "Pause YouTube playback" : "Play YouTube playback");
            }
        });
    }

    private static final class Bridge {
        @JavascriptInterface
        public void ready() {
            MAIN.post(() -> {
                if (player != null) player.evaluateJavascript("playYT();", null);
            });
        }

        @JavascriptInterface
        public void state(int state) {
            MAIN.post(() -> {
                playing = state == 1 || state == 3;
                updateMini();
            });
        }

        @JavascriptInterface
        public void error(int code) {
            MAIN.post(() -> {
                playing = false;
                updateMini();
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
