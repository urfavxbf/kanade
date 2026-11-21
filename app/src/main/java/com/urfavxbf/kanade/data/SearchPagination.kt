package com.urfavxbf.kanade.data

import com.urfavxbf.kanade.model.Track
import org.schabi.newpipe.extractor.Page

internal object SearchPagination {
    fun merge(existing: List<Track>, incoming: List<Track>): List<Track> =
        (existing + incoming).distinctBy { it.id }

    fun sameCursor(first: Page?, second: Page?): Boolean {
        if (first == null || second == null) return false
        return first.url == second.url && first.id == second.id && first.ids == second.ids &&
            first.cookies == second.cookies && first.body.contentEquals(second.body)
    }
}
