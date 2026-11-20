package com.urfavxbf.kanade.ui

internal fun folderVisible(folder: String, include: Set<String>, exclude: Set<String>): Boolean {
    val path = folder.trimEnd('/')
    fun matches(parent: String): Boolean {
        val root = parent.trimEnd('/')
        return path == root || path.startsWith("$root/")
    }
    return (include.isEmpty() || include.any(::matches)) && exclude.none(::matches)
}

internal fun lyricClock(position: Long, delay: Long): Long = position - delay
internal fun lyricSeek(timestamp: Long, delay: Long): Long = (timestamp + delay).coerceAtLeast(0)
