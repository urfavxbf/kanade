package com.urfavxbf.kanade.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal enum class ControlStyle(val key: String, val label: String) {
    GLASS("glass", "Glass"),
    SOFT_RAISED("soft_raised", "Soft Raised");

    companion object {
        fun fromStored(key: String?) = entries.firstOrNull { it.key == key } ?: GLASS
    }
}

internal val LocalControlStyle = staticCompositionLocalOf { ControlStyle.GLASS }

internal data class RaisedColors(val face: Color, val highlight: Color, val shadow: Color, val accent: Color)

internal val DarkRaised = RaisedColors(Color(0xFF302F50), Color(0xFF48476C), Color(0xFF19182F), Color(0xFFC0B9FF))
internal val LightRaised = RaisedColors(Color(0xFFE7E6F1), Color.White, Color(0xFFAFADC3), Color(0xFF5148A4))
internal val LocalRaisedColors = staticCompositionLocalOf { DarkRaised }

@Composable
internal fun Modifier.softRaised(shape: Shape, pressed: Boolean = false): Modifier {
    val colors = LocalRaisedColors.current
    val light = Shadow(radius = 5.dp, color = colors.highlight, offset = DpOffset((-2).dp, (-2).dp))
    val dark = Shadow(radius = 6.dp, color = colors.shadow, offset = DpOffset(3.dp, 3.dp))
    return if (pressed) {
        clip(shape).background(colors.face).innerShadow(shape, dark).innerShadow(shape, light)
    } else {
        dropShadow(shape, light).dropShadow(shape, dark).clip(shape).background(colors.face)
    }
}
