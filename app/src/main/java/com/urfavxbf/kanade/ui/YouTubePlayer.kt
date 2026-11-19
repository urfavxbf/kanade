package com.urfavxbf.kanade.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.json.JSONArray

/** YouTube owns transport, ads, captions and advancement through the selected mix. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YouTubePlayer(ids: List<String>) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    LifecycleResumeEffect(webView) {
        val view = webView
        view?.onResume()
        onPauseOrDispose {
            view?.evaluateJavascript("window.pauseKanadeVideo?.()", null)
            view?.onPause()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).aspectRatio(16f / 9),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                        request.isForMainFrame
                }
                val origin = "https://${context.packageName}"
                loadDataWithBaseURL(origin, playerDocument(ids, origin), "text/html", "UTF-8", null)
                webView = this
            }
        },
        onReset = null,
        onRelease = { view ->
            webView = null
            view.stopLoading()
            view.loadUrl("about:blank")
            view.destroy()
        },
    )
}

private fun playerDocument(ids: List<String>, origin: String): String {
    require(ids.isNotEmpty() && ids.all { it.matches(Regex("[A-Za-z0-9_-]{11}")) })
    return """<!doctype html><html lang="en"><head>
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
          *{box-sizing:border-box}html,body,#player{width:100%;height:100%;margin:0;border:0}
          body{background:#141513;color:#faf4e8;font:16px sans-serif}
          #message{padding:16px;margin:0}#player{display:block}
        </style></head><body>
        <div id="player"><p id="message" role="status">Loading YouTube…</p></div>
        <script>
          const videos = ${JSONArray(ids)};
          let player;
          function unavailable() {
            if(player && player.destroy) player.destroy();
            document.body.textContent = "Video unavailable here. Open it on YouTube or choose another result.";
          }
          const timeout = setTimeout(unavailable, 18000);
          window.pauseKanadeVideo = () => { if(player && player.pauseVideo) player.pauseVideo(); };
          document.addEventListener("visibilitychange", () => { if(document.hidden) window.pauseKanadeVideo(); });
          function onYouTubeIframeAPIReady() {
            if(!document.getElementById("player")) return;
            player = new YT.Player("player", {
              width:"100%", height:"100%", videoId:videos[0],
              playerVars:{playsinline:1, origin:"$origin", controls:1, playlist:videos.slice(1).join(",")},
              events:{
                onReady:() => clearTimeout(timeout),
                onError:() => { clearTimeout(timeout); unavailable(); }
              }
            });
          }
        </script><script src="https://www.youtube.com/iframe_api"></script></body></html>"""
}
