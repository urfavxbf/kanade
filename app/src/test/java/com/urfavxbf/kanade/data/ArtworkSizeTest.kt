package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.extractor.Image

class ArtworkSizeTest {
    @Test fun requestsLargerArtFromSavedMusicSearchThumbnail() {
        val url = "https://yt3.googleusercontent.com/album=w120-h120-l90-rj"
        assertEquals(listOf(url.replace("120", "640"), url), coverVariants(url, coverRequestSize(432)))
        assertEquals(listOf(url.replace("120", "1280"), url), coverVariants(url, coverRequestSize(1080)))
    }

    @Test fun preservesArtworkIdentityAndOriginalUrl() {
        val url = "https://lh3.googleusercontent.com/art=s192-c?token=abc"
        assertEquals(listOf(url.replace("s192", "s640"), url), coverVariants(url, 640))
        val wide = "https://lh3.googleusercontent.com/id=w240-h120-p-rj"
        assertEquals(wide.replace("240-h120", "1280-h640"), coverVariants(wide, 640).first())
    }

    @Test fun leavesLocalUnknownAndAlreadyLargeImagesAlone() {
        for (url in listOf("content://media/external/audio/albumart/4",
            "https://example.com/image=w60-h60", "https://lh3.googleusercontent.com/id=s1200")) {
            assertEquals(listOf(url), coverVariants(url, 640))
        }
    }

    @Test fun triesLargeArchiveAndVideoImagesBeforeTheirOriginals() {
        val url = "https://coverartarchive.org/release/id/front-500"
        assertEquals(listOf(url.replace("500", "1200"), url), coverVariants(url, 1280))
        val video = "https://i.ytimg.com/vi/123456789ab/hqdefault.jpg"
        assertEquals(listOf(video.replace("hqdefault", "maxresdefault"), video.replace("hqdefault", "sddefault"), video),
            coverVariants(video, 1280))
    }

    @Test fun selectsByImageDimensionsNotResponseOrder() {
        fun image(url: String, pixels: Int) = Image(url, pixels, pixels, Image.ResolutionLevel.UNKNOWN)
        assertEquals("large", listOf(image("large", 544), image("small", 120)).largestArtwork())
    }
}
