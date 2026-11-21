package com.urfavxbf.kanade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

/**
 * Single clickable circular glass control, larger than the surrounding minimal
 * transport buttons. The button shape ITSELF refracts the captured backdrop;
 * there is no second glass wrapper or nested panel around its glyph.
 *
 * Lens styling adapts the original MIT-licensed design reference:
 * https://github.com/kucukkanat/liquid-glass
 */
@Composable
internal fun LiquidGlassPlayPauseButton(
    playing: Boolean,
    backdrop: LiquidState,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.95f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.66f, stiffness = 640f),
        label = "Glass play pause press",
    )
    Box(
        modifier = Modifier
            .size(62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color(0xFFCCAB8F).copy(alpha = 0.26f),
                spotColor = Color(0xFF241711).copy(alpha = 0.24f),
            )
            .liquid(backdrop) {
                shape = CircleShape
                refraction = 0.50f
                curve = 0.50f
                edge = 0.15f
                dispersion = 0.12f
                frost = 2.dp
                saturation = 1.65f
                tint = Color(0xFFF6EBDF).copy(alpha = 0.18f)
            }
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.26f),
                        Color(0xFFEADAC9).copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.025f),
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.84f),
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.42f),
                    )
                ),
                CircleShape,
            )
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onToggle,
            )
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = playing,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 160),
            label = "Play or pause",
        ) { isPlaying ->
            KanadeGlyph(
                icon = if (isPlaying) KanadeIcon.PAUSE else KanadeIcon.PLAY,
                color = Color.White,
                size = 29.dp,
                bold = true,
            )
        }
    }
}
