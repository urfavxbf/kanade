package com.urfavxbf.kanade.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Videos selected during this session, independent of the native audio queue. */
class VideoQueue {
    private val entries = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val videos = entries.asStateFlow()

    fun add(video: YouTubeVideo) {
        entries.update { list -> if (list.any { it.id == video.id }) list else list + video }
    }

    fun move(id: String, offset: Int) {
        entries.update { list ->
            val from = list.indexOfFirst { it.id == id }
            val to = from + offset
            if (from < 0 || to !in list.indices) list
            else list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun remove(id: String) { entries.update { list -> list.filterNot { it.id == id } } }
    fun clear() { entries.value = emptyList() }
}
