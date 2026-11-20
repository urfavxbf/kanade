package com.urfavxbf.kanade.playback

internal fun onlineBoostGain(mediaId: String?, requestedGain: Int): Int =
    if (mediaId?.startsWith("youtube:") == true) requestedGain.coerceIn(0, 600) else 0
