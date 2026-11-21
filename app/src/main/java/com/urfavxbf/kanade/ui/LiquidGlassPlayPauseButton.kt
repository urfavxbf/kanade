package com.urfavxbf.kanade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

/**
 * Native Compose adaptation of the circular lens button in:
 * https://github.com/kucukkanat/liquid-glass (MIT).
 *
 * The BUTTON ITSELF refracts the Now Playing backdrop; there is no parent glass
 * container. Vitro's GPU shader replaces the web kit's SVG displacement filter.
 * The web reference's glass tint, curved edge, chromatic fringe and highlights
 * are recreated on Android (with library-provided older-device fallback).
 */
@Composable
internal fun LiquidGlassPlayPauseButton(
    playing: Boolean,
    backdrop: GlassBackdrop,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
) {
    // A single interaction source drives both the glass press state and button
    // semantics, including accessibility/keyboard actions.
    val source = remember { MutableInteractionSource() }
    val glass = remember(reduceMotion) {
        Glass.Clear.copy(
            refractionHeight = 22.dp,
            refractionAmount = 40.dp,
            dispersion = 0.10f,
            blurRadius = 2.dp,
            saturation = 1.7f,
            brightness = 0.15f,
            tint = Color.White.copy(alpha = 0.10f),
            highlightIntensity = 1.1f,
            isInteractive = !reduceMotion,
        )
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .glassEffect(backdrop = backdrop, glass = glass, shape = CircleShape, interactionSource = source)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        // A subtle diagonal sheen mimics the original web kit's specular layer.
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.035f),
                            Color.Transparent,
                        )
                    )
                )
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
