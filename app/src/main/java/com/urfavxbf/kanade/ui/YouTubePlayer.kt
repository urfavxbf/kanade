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
        view?.evaluateJavascript("window.setKanadeForeground?.(true)", null)
        onPauseOrDispose {
            view?.evaluateJavascript("window.setKanadeForeground?.(false)", null)
            view?.onPause()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 304.dp).aspectRatio(1.35f),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                        request.isForMainFrame || request.url.scheme !in setOf("https", "about")
                }
                val origin = "https://${context.packageName}"
                val template = context.assets.open("youtube-player.html").bufferedReader().use { it.readText() }
                require(ids.isNotEmpty() && ids.all { it.matches(Regex("[A-Za-z0-9_-]{11}")) })
                val html = template.replace("__VIDEO_IDS__", JSONArray(ids).toString())
                loadDataWithBaseURL("$origin/", html, "text/html", "UTF-8", null)
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
