package com.urfavxbf.kanade.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

internal fun accessibleAccent(color: Color, backgrounds: List<Color>, dark: Boolean): Color {
    if (backgrounds.all { contrastRatio(color, it) >= 4.5f }) return color
    val target = if (dark) Color.White else Color.Black
    var low = 0f
    var high = 1f
    repeat(20) {
        val middle = (low + high) / 2f
        if (backgrounds.all { contrastRatio(lerp(color, target, middle), it) >= 4.5f }) high = middle
        else low = middle
    }
    return lerp(color, target, high)
}
