package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.Nullable;

/**
 * WebView configured with a legitimate third-party embed base URL.
 *
 * YouTube rejects an iframe hosted by a page that identifies itself as
 * youtube.com with Error 152-4. The player HTML is therefore loaded under a
 * normal third-party HTTPS origin while the iframe itself continues to use the
 * official YouTube IFrame Player API.
 */
public class YouTubeWebView extends WebView {
    private static final String EMBED_BASE = "https://screentinker.com";

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
        super.loadDataWithBaseURL(
                EMBED_BASE + "/",
                safeData,
                mimeType,
                encoding,
                EMBED_BASE + "/");
    }
}
