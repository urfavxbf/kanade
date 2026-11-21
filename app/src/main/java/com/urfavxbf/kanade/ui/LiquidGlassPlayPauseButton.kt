package com.urfavxbf.kanade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Native Kotlin/Compose adaptation of the *button* from the MIT-licensed
 * https://github.com/kucukkanat/liquid-glass web kit.
 *
 * The glass lens is the play/pause control itself, NOT a container or panel.
 * Liquid's AGSL optical refraction uses the Now Playing backdrop; on API <33
 * it automatically degrades to supported frosted/tinted effects.
 */
@Composable
internal fun LiquidGlassPlayPauseButton(
    playing: Boolean,
    backdrop: LiquidState,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.95f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.65f, stiffness = 620f),
        label = "Glass button press",
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .liquid(backdrop) {
                shape = CircleShape
                refraction = 0.5f
                curve = 0.5f
                edge = 0.15f
                dispersion = 0.14f
                frost = 2.dp
                saturation = 1.7f
                tint = Color.White.copy(alpha = 0.10f)
            }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        // Diagonal specular sweep and translucent body, matching the web reference.
        Box(
            Modifier.matchParentSize().clip(CircleShape)
                .background(Brush.linearGradient(listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.035f),
                    Color.Transparent,
                )))
        )
        Crossfade(
            targetState = playing,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 160),
            label = "Play or pause",
        ) { isPlaying ->
            KanadeGlyph(
                icon = if (isPlaying) KanadeIcon.PAUSE else KanadeIcon.PLAY,
                color = Color.White,
                size = 30.dp,
                bold = true,
            )
        }
    }
}
