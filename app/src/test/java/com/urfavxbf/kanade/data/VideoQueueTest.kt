package com.urfavxbf.kanade.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoQueueTest {
    @Test fun preservesUserOrderAcrossMovesDuplicatesAndRemoval() {
        val queue = VideoQueue()
        val a = YouTubeVideo("aaaaaaaaaaa", "A", "", "")
        val b = YouTubeVideo("bbbbbbbbbbb", "B", "", "")
        queue.add(a)
        queue.add(b)
        queue.add(a)
        queue.move(b.id, -1)
        queue.move(b.id, -1)
        queue.move("missing", 1)
        assertEquals(listOf(b, a), queue.videos.value)
        queue.remove(b.id)
        assertEquals(listOf(a), queue.videos.value)
        queue.clear()
        assertEquals(emptyList<YouTubeVideo>(), queue.videos.value)
    }
}
