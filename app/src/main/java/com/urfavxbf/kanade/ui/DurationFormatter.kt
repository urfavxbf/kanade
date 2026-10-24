package com.urfavxbf.kanade.ui

object DurationFormatter {
    fun format(milliseconds: Long): String {
        val total = (milliseconds.coerceAtLeast(0) / 1_000)
        val seconds = total % 60
        val minutes = (total / 60) % 60
        val hours = total / 3_600
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }
}
