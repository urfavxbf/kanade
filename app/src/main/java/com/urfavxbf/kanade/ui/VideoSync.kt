package com.urfavxbf.kanade.ui

import com.urfavxbf.kanade.playback.PlaybackState

internal fun resolvedVideoPlayback(requestedId: String, latest: PlaybackState): PlaybackState? =
    latest.takeIf { it.mediaId == requestedId }
