package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.Nullable;

/**
 * WebView configured with the application identity required by YouTube's embedded player.
 * YouTube requires a fully-qualified HTTPS app-id referrer for WebView integrations.
 */
public class YouTubeWebView extends WebView {
    private static final String APP_ORIGIN = "https://com.urfavxbf.kanade";
    private static final String NOCOOKIE_HOST = "https://www.youtube-nocookie.com";

    public YouTubeWebView(Context context) {
        super(context);
    }

    public YouTubeWebView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public YouTubeWebView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void loadDataWithBaseURL(
            @Nullable String baseUrl,
            String data,
            @Nullable String mimeType,
            @Nullable String encoding,
            @Nullable String historyUrl) {
        String safeData = data == null ? "" : data;
        String correctedData = safeData
                .replace(
                        "origin=https%3A%2F%2Fwww.youtube.com",
                        "origin=https%3A%2F%2Fcom.urfavxbf.kanade")
                .replace(
                        "origin:'https://www.youtube.com'",
                        "origin:'https://com.urfavxbf.kanade'")
                .replace(
                        "player=new YT.Player('player',{",
                        "player=new YT.Player('player',{host:'" + NOCOOKIE_HOST + "',");
        super.loadDataWithBaseURL(
                APP_ORIGIN + "/",
                correctedData,
                mimeType,
                encoding,
                APP_ORIGIN + "/");
    }
}
