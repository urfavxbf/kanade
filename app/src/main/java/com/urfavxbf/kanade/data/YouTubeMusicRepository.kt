package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import com.urfavxbf.kanade.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Unofficial YouTube integration. Stream URLs are resolved afresh when playback starts. */
object YouTubeMusicRepository {
    init {
        NewPipe.init(object : Downloader() {
            override fun execute(request: Request): Response {
                val connection = URL(request.url()).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = request.httpMethod()
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 20_000
                    request.headers().forEach { (name, values) ->
                        values.forEach { connection.addRequestProperty(name, it) }
                    }
                    request.dataToSend()?.let { data ->
                        connection.doOutput = true
                        connection.outputStream.use { it.write(data) }
                    }
                    val status = connection.responseCode
                    val body = (if (status >= 400) connection.errorStream else connection.inputStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    return Response(status, connection.responseMessage.orEmpty(), connection.headerFields,
                        body, connection.url.toString())
                } finally {
                    connection.disconnect()
                }
            }
        })
    }

    data class SearchPage(val tracks: List<Track>, val nextPage: Page?)

    private fun songsExtractor(query: String) = ServiceList.YouTube.getSearchExtractor(query, listOf(MUSIC_SONGS), "")

    suspend fun search(query: String): SearchPage = withContext(Dispatchers.IO) {
        val extractor = songsExtractor(query)
        extractor.fetchPage()
        parsePage(extractor.initialPage)
    }

    suspend fun searchNext(query: String, page: Page): SearchPage = withContext(Dispatchers.IO) {
        val extractor = songsExtractor(query)
        parsePage(extractor.getPage(page))
    }

    private fun parsePage(page: ListExtractor.InfoItemsPage<InfoItem>): SearchPage {
        val tracks = page.items.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
            val id = YouTubeTrackId.fromUrl(item.url) ?: return@mapNotNull null
            Track(id = "youtube:$id", title = item.name, artist = item.uploaderName ?: "Unknown artist",
                uri = "kanadeyt://track/$id", artworkUri = item.thumbnails.lastOrNull()?.url,
                durationMs = item.duration.coerceAtLeast(0) * 1_000, source = TrackSource.YOUTUBE)
        }.distinctBy { it.id }
        return SearchPage(tracks, page.nextPage?.takeIf { Page.isValid(it) })
    }

    @Throws(IOException::class)
    fun videoUrl(id: String): String {
        if (!YouTubeTrackId.isValid(id)) throw IOException("Invalid YouTube track")
        try {
            val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$id")
            extractor.fetchPage()
            val videos = (extractor.videoOnlyStreams + extractor.videoStreams).filter {
                it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.content.startsWith("https://")
            }
            return videos.firstOrNull { it.resolution == "360p" }?.content
                ?: videos.firstOrNull { it.resolution == "480p" }?.content
                ?: videos.firstOrNull()?.content ?: throw IOException("No video stream available")
        } catch (e: Exception) {
            if (e is IOException) throw e
            throw IOException("Unable to load video", e)
        }
    }

    @Throws(IOException::class)
    fun streamUrl(id: String): String {
        if (!YouTubeTrackId.isValid(id)) throw IOException("Invalid YouTube track")
        try {
            val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$id")
            extractor.fetchPage()
            return extractor.audioStreams.filter {
                it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    it.content.startsWith("https://")
            }
                .maxByOrNull { it.averageBitrate }?.content ?: throw IOException("No playable audio stream")
        } catch (e: Exception) {
            if (e is IOException) throw e
            throw IOException("Unable to load YouTube audio", e)
        }
    }
}
