package com.urfavxbf.kanade.ui

import com.urfavxbf.kanade.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSyncTest {
    @Test fun useLatestPlaybackAfterVideoResolution() {
        val paused = PlaybackState(mediaId = "youtube:first", positionMs = 42000, playing = false, speed = 1.5f)
        assertEquals(paused, resolvedVideoPlayback("youtube:first", paused))
        assertNull(resolvedVideoPlayback("youtube:other", paused))
    }
}
