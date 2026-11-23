package com.urfavxbf.kanade.lyrics

data class LyricSegment(val timeMs: Long, val start: Int, val end: Int)
data class LyricLine(val timeMs: Long, val text: String, val segments: List<LyricSegment> = emptyList(), val endMs: Long? = null)

object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):([0-5]\d)(?:[.:](\d{1,3}))?]""")
    private val inline = Regex("""<(\d{1,3}):([0-5]\d)(?:[.:](\d{1,3}))?>""")
    private val offset = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    private fun time(match: MatchResult): Long =
        match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1000 +
            match.groupValues[3].padEnd(3, '0').toLong()

    internal fun timedLine(start: Long, body: String, markers: List<Pair<IntRange, Long>>, end: Long? = null): LyricLine {
        val text = StringBuilder()
        val segments = mutableListOf<LyricSegment>()
        var cursor = 0
        var time = start
        fun append(until: Int) {
            val from = text.length
            text.append(body, cursor, until)
            if (text.length > from && markers.isNotEmpty()) segments += LyricSegment(time, from, text.length)
        }
        for ((range, at) in markers) {
            append(range.first)
            cursor = range.last + 1
            time = at.coerceAtLeast(time)
        }
        append(body.length)
        return LyricLine(start, text.toString(), segments, end)
    }

    fun parse(content: String): List<LyricLine> {
        val adjustment = offset.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return buildList {
            for (line in content.removePrefix("\uFEFF").lineSequence()) {
                val stamps = timestamp.findAll(line).toList()
                val first = stamps.firstOrNull() ?: continue
                val body = line.replace(timestamp, "").trim()
                for (stamp in stamps) {
                    val start = time(stamp)
                    val markers = inline.findAll(body).map {
                        it.range to (time(it) + start - time(first) + adjustment).coerceAtLeast(0)
                    }.toList()
                    val parsed = timedLine((start + adjustment).coerceAtLeast(0), body, markers)
                    if (parsed.text.isNotBlank()) add(parsed)
                }
            }
        }.sortedBy { it.timeMs }
    }

    fun current(lines: List<LyricLine>, positionMs: Long): Int {
        var left = 0
        var right = lines.lastIndex
        var found = -1
        while (left <= right) {
            val mid = (left + right) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                found = mid
                left = mid + 1
            } else right = mid - 1
        }
        return if (found >= 0 && (lines[found].endMs ?: Long.MAX_VALUE) <= positionMs) -1 else found
    }
}
