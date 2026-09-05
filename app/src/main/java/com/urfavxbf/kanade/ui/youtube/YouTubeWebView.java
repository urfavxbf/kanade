package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebView host for the in-app YouTube player.
 *
 * YouTube documents a direct-embed WebView configuration where the player is
 * loaded from the embed URL with an explicit HTTPS Referer header. This class
 * keeps the existing manager API intact while translating its generated player
 * request into that direct-embed configuration.
 */
public class YouTubeWebView extends WebView {
    private static final String APP_ID = "com.urfavxbf.kanade";
    private static final String APP_REFERRER = "https://" + APP_ID;
    private static final String YOUTUBE_EMBED = "https://www.youtube.com/embed/";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("videoId:'([^']+)'");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public YouTubeWebView(Context context) {
        super(context);
        configureClient();
    }

    public YouTubeWebView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        configureClient();
    }

    public YouTubeWebView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        configureClient();
    }

    @Override
    public void loadDataWithBaseURL(
            @Nullable String baseUrl,
            String data,
            @Nullable String mimeType,
            @Nullable String encoding,
            @Nullable String historyUrl) {
        String videoId = extractVideoId(data);
        if (videoId.isEmpty()) {
            super.loadDataWithBaseURL(
                    APP_REFERRER + "/",
                    data == null ? "" : data,
                    mimeType,
                    encoding,
                    APP_REFERRER + "/");
            return;
        }

        String embedUrl = YOUTUBE_EMBED + Uri.encode(videoId)
                + "?autoplay=1"
                + "&playsinline=1"
                + "&rel=0"
                + "&controls=0"
                + "&enablejsapi=1"
                + "&origin=" + Uri.encode(APP_REFERRER)
                + "&widget_referrer=" + Uri.encode(APP_REFERRER);

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", APP_REFERRER);
        super.loadUrl(embedUrl, headers);
    }

    private void configureClient() {
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith(YOUTUBE_EMBED)) {
                    injectDirectPlayerBridge();
                }
            }
        });
    }

    private void injectDirectPlayerBridge() {
        String script = "javascript:(function(){"
                + "window.playYT=function(){var v=document.querySelector('video');if(v){var p=v.play();if(p&&p.catch){p.catch(function(){});}}};"
                + "window.pauseYT=function(){var v=document.querySelector('video');if(v){v.pause();}};"
                + "window.seekYT=function(s){var v=document.querySelector('video');if(v){try{v.currentTime=Math.max(0,Number(s)||0);}catch(e){}}};"
                + "window.reportProgress=function(){var v=document.querySelector('video');if(v&&window.KanadePlayer){window.KanadePlayer.progress(v.currentTime||0,v.duration||0);}};"
                + "window.__kanadeBoundVideo=window.__kanadeBoundVideo||null;"
                + "window.__kanadeBind=function(){"
                + "var v=document.querySelector('video');"
                + "if(!v){return false;}"
                + "if(window.__kanadeBoundVideo===v){return true;}"
                + "window.__kanadeBoundVideo=v;"
                + "v.addEventListener('play',function(){if(window.KanadePlayer){window.KanadePlayer.state(1);}});"
                + "v.addEventListener('playing',function(){if(window.KanadePlayer){window.KanadePlayer.state(1);}});"
                + "v.addEventListener('pause',function(){if(window.KanadePlayer){window.KanadePlayer.state(2);}});"
                + "v.addEventListener('waiting',function(){if(window.KanadePlayer){window.KanadePlayer.state(3);}});"
                + "v.addEventListener('ended',function(){if(window.KanadePlayer){window.KanadePlayer.state(0);}});"
                + "v.addEventListener('loadedmetadata',function(){if(window.KanadePlayer){window.KanadePlayer.ready();}});"
                + "if(window.KanadePlayer){window.KanadePlayer.ready();}"
                + "return true;};"
                + "window.__kanadeTries=0;"
                + "window.__kanadeTimer=setInterval(function(){"
                + "if(window.__kanadeBind()||++window.__kanadeTries>100){clearInterval(window.__kanadeTimer);}"
                + "},100);"
                + "})();";
        evaluateJavascript(script, null);
    }

    private static String extractVideoId(String data) {
        if (data == null || data.isEmpty()) return "";
        Matcher matcher = VIDEO_ID_PATTERN.matcher(data);
        if (!matcher.find()) return "";
        return matcher.group(1) == null ? "" : matcher.group(1).trim();
    }
}
