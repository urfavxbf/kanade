package com.urfavxbf.kanade.lyrics

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offset = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<LyricLine> {
        val adjustment = offset.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return buildList {
            for (line in content.removePrefix("\uFEFF").lineSequence()) {
                val stamps = timestamp.findAll(line).toList()
                val words = line.replace(timestamp, "").trim()
                if (stamps.isEmpty() || words.isEmpty()) continue
                for (match in stamps) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: continue
                    val seconds = match.groupValues[2].toLongOrNull() ?: continue
                    if (seconds >= 60) continue
                    val frac = match.groupValues[3].let { raw ->
                        when (raw.length) {
                            1 -> (raw.toLongOrNull() ?: 0L) * 100
                            2 -> (raw.toLongOrNull() ?: 0L) * 10
                            else -> raw.toLongOrNull() ?: 0L
                        }
                    }
                    val time = (minutes * 60_000 + seconds * 1_000 + frac + adjustment).coerceAtLeast(0)
                    add(LyricLine(time, words))
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
        return found
    }
}
