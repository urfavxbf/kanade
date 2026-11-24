package com.urfavxbf.kanade.data

import org.schabi.newpipe.extractor.Image
import java.net.URI

internal fun List<Image>.largestArtwork(): String? =
    filter { it.url.isNotBlank() }.maxByOrNull {
        it.width.coerceAtLeast(1).toLong() * it.height.coerceAtLeast(1)
    }?.url

internal fun coverRequestSize(pixels: Int): Int = when {
    pixels <= 320 -> 320
    pixels <= 640 -> 640
    pixels <= 1280 -> 1280
    else -> 1920
}

private val googleHosts = setOf("lh3.googleusercontent.com", "yt3.googleusercontent.com", "lh3.ggpht.com", "yt3.ggpht.com")
private val squareSize = Regex("""=s(\d+)(?=-|$)""")
private val pairedSize = Regex("""=w(\d+)-h(\d+)(?=-|$)""")
private val videoCover = Regex("""/vi/([A-Za-z0-9_-]{11})/(default|mqdefault|hqdefault|sddefault|maxresdefault)\.jpg""")

/** Try a larger rendition of the same asset; keep the provider URL as the last choice. */
internal fun coverVariants(url: String?, target: Int): List<String> {
    if (url.isNullOrBlank()) return emptyList()
    val original = listOf(url)
    val uri = runCatching { URI(url) }.getOrNull() ?: return original
    if (uri.scheme != "https") return original
    val path = uri.rawPath ?: return original
    fun replacePath(newPath: String): String {
        val offset = "https://".length + uri.rawAuthority.length
        return url.replaceRange(offset, offset + path.length, newPath)
    }
    if (uri.host in googleHosts) {
        val pair = pairedSize.find(path)
        if (pair != null) {
            val w = pair.groupValues[1].toIntOrNull() ?: return original
            val h = pair.groupValues[2].toIntOrNull() ?: return original
            if (minOf(w, h) <= 0 || minOf(w, h) >= target) return original
            val scale = target.toDouble() / minOf(w, h)
            val size = "=w${(w * scale).toInt()}-h${(h * scale).toInt()}"
            return listOf(replacePath(path.replaceRange(pair.range, size)), url)
        }
        val square = squareSize.find(path) ?: return original
        if ((square.groupValues[1].toIntOrNull() ?: target) >= target) return original
        return listOf(replacePath(path.replaceRange(square.range, "=s$target")), url)
    }
    if (uri.host == "coverartarchive.org" && target > 500 && path.endsWith("/front-500")) {
        return listOf(replacePath(path.removeSuffix("500") + "1200"), url)
    }
    if (uri.host == "i.ytimg.com" && videoCover.matches(path)) {
        val sizes = listOf("default", "mqdefault", "hqdefault", "sddefault", "maxresdefault")
        val source = sizes.indexOf(path.substringAfterLast('/').removeSuffix(".jpg"))
        val preferred = when {
            target > 640 -> 4
            target > 480 -> 3
            else -> 2
        }
        return (preferred downTo (source + 1)).map { index ->
            replacePath(path.substringBeforeLast('/') + "/" + sizes[index] + ".jpg")
        } + url
    }
    return original
}
