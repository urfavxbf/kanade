package com.urfavxbf.kanade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Consistent 24-unit icons; active states use the same silhouette with a heavier stroke. */
internal enum class KanadeIcon {
    NAV_HOME, NAV_LIBRARY, NAV_SEARCH, NAV_PLAYLISTS,
    HOME, MORE, LIBRARY, ONLINE, PLAYLIST, INSIGHTS, MUSIC, SEARCH, REFRESH, BACK, FORWARD,
    FAVORITE, PLAY_NEXT, ADD, REMOVE, EDIT, PREVIOUS, PLAY, PAUSE, NEXT,
    SHUFFLE, REPEAT, REPEAT_ONE, QUEUE, LYRICS, AUDIO, CLOSE, UP, DOWN
}

@Composable
internal fun KanadeGlyph(icon: KanadeIcon, color: Color, selected: Boolean = false, size: Dp = 22.dp, bold: Boolean = false) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val width = if (bold) 3.2f else if (selected) 2.9f else 1.8f
            val stroke = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                drawLine(color, Offset(x1, y1), Offset(x2, y2), width, cap = StrokeCap.Round)
            fun circle(x: Float, y: Float, radius: Float) = drawCircle(color, radius, Offset(x, y), style = stroke)
            fun box(x: Float, y: Float, w: Float, h: Float, radius: Float = 3f) =
                drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(radius), style = stroke)
            fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = stroke)
            fun triangle(base: Float, tip: Float) {
                drawPath(Path().apply {
                    moveTo(base, 3.5f)
                    lineTo(tip, 12f)
                    lineTo(base, 20.5f)
                    close()
                }, color)
            }
            when (icon) {
                KanadeIcon.NAV_HOME -> {
                    fun tile(x: Float, y: Float, height: Float) =
                        drawRoundRect(color, Offset(x, y), Size(8f, height), CornerRadius(2f))
                    tile(2.5f, 3f, 10f)
                    tile(13f, 3f, 6f)
                    tile(2.5f, 15f, 6f)
                    tile(13f, 11f, 10f)
                }
                KanadeIcon.NAV_LIBRARY -> {
                    line(4f, 5f, 4f, 20f)
                    drawRoundRect(color, Offset(7f, 4f), Size(5f, 17f), CornerRadius(1.5f))
                    path { moveTo(15f, 6f); lineTo(18f, 5f); lineTo(22f, 19f); lineTo(19f, 20f); close() }
                }
                KanadeIcon.NAV_SEARCH -> {
                    circle(10f, 10f, 7f)
                    line(7f, 10f, 7f, 11f)
                    line(10f, 8f, 10f, 12f)
                    line(13f, 9f, 13f, 11f)
                    line(15.5f, 15.5f, 21f, 21f)
                }
                KanadeIcon.NAV_PLAYLISTS -> {
                    line(3f, 5f, 16f, 5f)
                    line(3f, 10f, 13f, 10f)
                    line(3f, 15f, 9f, 15f)
                    drawPath(Path().apply {
                        moveTo(14f, 11f)
                        lineTo(22f, 16f)
                        lineTo(14f, 21f)
                        close()
                    }, color)
                }
                KanadeIcon.HOME -> path { moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); lineTo(20f, 11f); lineTo(20f, 21f); lineTo(4f, 21f); lineTo(4f, 11f); close() }
                KanadeIcon.MORE -> for (y in listOf(5f, 12f, 19f)) drawCircle(color, 1.7f, Offset(12f, y))
                KanadeIcon.LIBRARY -> {
                    path { moveTo(14f, 3f); lineTo(6f, 3f); quadraticTo(3f, 3f, 3f, 6f); lineTo(3f, 16f); quadraticTo(3f, 19f, 6f, 19f) }
                    box(7f, 7f, 14f, 14f)
                    line(15f, 10f, 15f, 16f); line(15f, 10f, 18f, 9f); line(18f, 9f, 18f, 15f)
                    drawCircle(color, 1.35f, Offset(13.7f, 16f)); drawCircle(color, 1.35f, Offset(16.7f, 15f))
                }
                KanadeIcon.ONLINE -> { circle(12f, 12f, 8.5f); line(3.5f, 12f, 20.5f, 12f); path { moveTo(12f, 3.5f); cubicTo(7f, 8f, 7f, 16f, 12f, 20.5f); cubicTo(17f, 16f, 17f, 8f, 12f, 3.5f) } }
                KanadeIcon.PLAYLIST -> { line(4f, 6f, 17f, 6f); line(4f, 10f, 15f, 10f); line(4f, 14f, 12f, 14f); line(17f, 13f, 17f, 19f); line(17f, 13f, 20f, 12f); drawCircle(color, 1.6f, Offset(15.5f, 19f)) }
                KanadeIcon.INSIGHTS -> { line(5f, 20f, 5f, 13f); line(12f, 20f, 12f, 8f); line(19f, 20f, 19f, 4f) }
                KanadeIcon.MUSIC -> { line(10f, 6f, 10f, 17f); line(10f, 6f, 18f, 4f); line(18f, 4f, 18f, 15f); circle(7.5f, 18f, 2.3f); circle(15.5f, 16f, 2.3f) }
                KanadeIcon.SEARCH -> { circle(10.5f, 10.5f, 6f); line(15f, 15f, 20f, 20f) }
                KanadeIcon.REFRESH -> { path { moveTo(19f, 9f); cubicTo(17f, 4f, 10f, 3f, 6f, 7f); cubicTo(2f, 11f, 4f, 18f, 9f, 20f); cubicTo(13f, 21f, 17f, 19f, 19f, 16f) }; line(19f, 5f, 19f, 9f); line(19f, 9f, 15f, 9f) }
                KanadeIcon.BACK -> { line(19f, 12f, 5f, 12f); line(5f, 12f, 10f, 7f); line(5f, 12f, 10f, 17f) }
                KanadeIcon.FORWARD -> { line(9f, 6f, 15f, 12f); line(15f, 12f, 9f, 18f) }
                KanadeIcon.FAVORITE -> path { moveTo(12f, 20f); cubicTo(10f, 18f, 3.5f, 14f, 3.5f, 9f); cubicTo(3.5f, 4f, 9f, 3f, 12f, 7f); cubicTo(15f, 3f, 20.5f, 4f, 20.5f, 9f); cubicTo(20.5f, 14f, 14f, 18f, 12f, 20f); close() }
                KanadeIcon.PLAY_NEXT -> { line(4f, 6f, 15f, 6f); line(4f, 10f, 15f, 10f); line(4f, 14f, 11f, 14f); line(16f, 14f, 20f, 17f); line(20f, 17f, 16f, 20f) }
                KanadeIcon.ADD -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
                KanadeIcon.REMOVE -> line(5f, 12f, 19f, 12f)
                KanadeIcon.EDIT -> { path { moveTo(5f, 19f); lineTo(6f, 15f); lineTo(16f, 5f); quadraticTo(18f, 3f, 20f, 5f); quadraticTo(22f, 7f, 20f, 9f); lineTo(10f, 19f); close() }; line(15f, 6f, 19f, 10f) }
                KanadeIcon.PREVIOUS -> if (bold) { triangle(20f, 7f); line(4f, 5f, 4f, 19f) } else { line(5f, 5f, 5f, 19f); path { moveTo(19f, 5f); lineTo(8f, 12f); lineTo(19f, 19f); close() } }
                KanadeIcon.NEXT -> if (bold) { triangle(4f, 17f); line(20f, 5f, 20f, 19f) } else { line(19f, 5f, 19f, 19f); path { moveTo(5f, 5f); lineTo(16f, 12f); lineTo(5f, 19f); close() } }
                KanadeIcon.PLAY -> if (bold) triangle(6f, 21f) else path { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }
                KanadeIcon.PAUSE -> if (bold) {
                    drawRoundRect(color, Offset(5f, 4f), Size(5f, 16f), CornerRadius(1f))
                    drawRoundRect(color, Offset(14f, 4f), Size(5f, 16f), CornerRadius(1f))
                } else { line(9f, 5f, 9f, 19f); line(15f, 5f, 15f, 19f) }
                KanadeIcon.SHUFFLE -> { path { moveTo(4f, 6f); lineTo(7f, 6f); cubicTo(12f, 6f, 12f, 18f, 17f, 18f); lineTo(20f, 18f) }; path { moveTo(4f, 18f); lineTo(7f, 18f); cubicTo(9f, 18f, 10f, 16f, 11f, 14f) }; path { moveTo(13f, 10f); cubicTo(14f, 8f, 15f, 6f, 17f, 6f); lineTo(20f, 6f) }; line(17f, 3f, 20f, 6f); line(20f, 6f, 17f, 9f); line(17f, 15f, 20f, 18f); line(20f, 18f, 17f, 21f) }
                KanadeIcon.REPEAT, KanadeIcon.REPEAT_ONE -> { path { moveTo(4f, 9f); quadraticTo(4f, 5f, 8f, 5f); lineTo(19f, 5f); lineTo(16f, 2f); moveTo(19f, 5f); lineTo(16f, 8f); moveTo(20f, 15f); quadraticTo(20f, 19f, 16f, 19f); lineTo(5f, 19f); lineTo(8f, 22f); moveTo(5f, 19f); lineTo(8f, 16f) }; if (icon == KanadeIcon.REPEAT_ONE) { line(12f, 10f, 12f, 15f); line(10.5f, 11f, 12f, 10f) } }
                KanadeIcon.QUEUE -> { line(4f, 6f, 20f, 6f); line(4f, 11f, 20f, 11f); line(4f, 16f, 14f, 16f); line(17f, 15f, 20f, 18f); line(20f, 18f, 17f, 21f) }
                KanadeIcon.LYRICS -> if (bold) { line(4f, 6f, 20f, 6f); line(4f, 12f, 16f, 12f); line(4f, 18f, 12f, 18f) } else { box(4f, 3f, 16f, 18f); line(8f, 8f, 16f, 8f); line(8f, 12f, 16f, 12f); line(8f, 16f, 13f, 16f) }
                KanadeIcon.AUDIO -> if (bold) { line(5f, 9f, 5f, 15f); line(12f, 4f, 12f, 20f); line(19f, 7f, 19f, 17f) } else { line(6f, 4f, 6f, 20f); line(12f, 4f, 12f, 20f); line(18f, 4f, 18f, 20f); drawCircle(color, 2.3f, Offset(6f, 8f)); drawCircle(color, 2.3f, Offset(12f, 15f)); drawCircle(color, 2.3f, Offset(18f, 10f)) }
                KanadeIcon.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
                KanadeIcon.UP -> { line(12f, 19f, 12f, 5f); line(12f, 5f, 7f, 10f); line(12f, 5f, 17f, 10f) }
                KanadeIcon.DOWN -> { line(12f, 5f, 12f, 19f); line(12f, 19f, 7f, 14f); line(12f, 19f, 17f, 14f) }
            }
        }
    }
}
