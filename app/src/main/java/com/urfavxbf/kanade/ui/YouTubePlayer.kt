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

/** The official foreground player owns transport, ads, captions and playlist advancement. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YouTubePlayer(ids: List<String>) {
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }
    if (!resumed || ids.isEmpty()) return
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
                val playlist = ids.drop(1).joinToString(",")
                val url = "https://www.youtube.com/embed/${ids.first()}?playsinline=1&amp;rel=0&amp;playlist=$playlist"
                val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                    <meta name="referrer" content="strict-origin-when-cross-origin">
                    <style>*{margin:0}html,body,iframe{width:100%;height:100%;border:0;background:#000;display:block}</style>
                    </head><body><iframe src="$url" title="YouTube video player"
                    allow="encrypted-media; autoplay; picture-in-picture"></iframe></body></html>"""
                loadDataWithBaseURL(origin, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { view -> view.stopLoading(); view.loadUrl("about:blank"); view.destroy() },
    )
}
