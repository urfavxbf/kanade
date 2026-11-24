package com.urfavxbf.kanade.lyrics

/** WebVTT from the selected video's caption track, including optional inline word timestamps. */
internal object VideoCaptions {
    private val time = """(?:\d{2,}:)?\d{2}:\d{2}\.\d{3}"""
    private val cue = Regex("""^($time)\s+-->\s+($time)(?:\s.*)?$""")
    private val marker = Regex("""<($time)>""")
    private val markup = Regex("""</?[a-zA-Z][^>]*>""")

    private fun millis(value: String): Long {
        val parts = value.split(':', '.').map(String::toLong)
        return parts.dropLast(1).fold(0L) { total, part -> total * 60 + part } * 1000 + parts.last()
    }

    fun parse(input: String): List<LyricLine> = buildList {
        for (block in input.replace("\r", "").split(Regex("""\n\s*\n"""))) {
            val lines = block.lines()
            val index = lines.indexOfFirst { cue.matches(it) }
            if (index < 0) continue
            val match = cue.matchEntire(lines[index]) ?: continue
            val start = millis(match.groupValues[1])
            val end = millis(match.groupValues[2])
            val body = lines.drop(index + 1).joinToString(" ").replace(markup, "")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim()
            if (end <= start || body.isEmpty()) continue
            val markers = marker.findAll(body).map { it.range to millis(it.groupValues[1]) }.toList()
            val line = LrcParser.timedLine(start, body, markers, end)
            if (line.text.isNotBlank()) add(line)
        }
    }.sortedBy { it.timeMs }
}
