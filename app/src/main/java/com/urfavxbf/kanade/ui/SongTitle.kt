package com.urfavxbf.kanade.ui

private val videoLabel = Regex(
    """\s*[\[(]\s*(?:official\s+)?(?:music\s+video|video|audio|lyrics?|lyric\s+video|visuali[sz]er)\s*[\])]\s*$""",
    RegexOption.IGNORE_CASE,
)
private val separator = Regex("""\s+[-–—|]\s+""")

internal fun songTitle(raw: String, artist: String): String {
    val title = raw.trim().replace(videoLabel, "").trim().ifEmpty { return raw }
    val performer = artist.trim().removeSuffix(" - Topic").trim()
    for (divider in separator.findAll(title)) {
        val left = title.substring(0, divider.range.first).trim()
        val right = title.substring(divider.range.last + 1).trim()
        if (left.equals(performer, true) && right.isNotEmpty()) return right
        if (right.equals(performer, true) && left.isNotEmpty()) return left
    }
    return title
}
