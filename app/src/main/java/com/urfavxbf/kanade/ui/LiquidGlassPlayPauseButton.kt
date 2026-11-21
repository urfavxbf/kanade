package com.urfavxbf.kanade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.shapes.PrismalCapsule

/**
 * Native Compose adaptation of the lens-style button at
 * https://github.com/kucukkanat/liquid-glass (MIT).
 *
 * The BUTTON is the glass surface, not a glass panel around a plain button.
 * PrismalAGSL replaces the web kit's SVG feDisplacementMap with native AGSL
 * refraction; a bright rim, white tint, and specular sweep match its styling.
 *
 * The actual optical sampling depends on the backdrop behind the control,
 * and older Android versions get the renderer's frosted-glass fallback.
 */
@Composable
internal fun LiquidGlassPlayPauseButton(
    playing: Boolean,
    backdrop: PrismalBackdrop,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
) {
    PrismalGlassButton(
        onClick = onToggle,
        backdrop = backdrop,
        modifier = Modifier
            .size(64.dp)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        shape = { PrismalCapsule() },
        height = null,
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.Center,
        blurRadius = 2.dp,
        // Equivalent to the web kit's curved-lens edge and ~40px displacement.
        refractionHeight = 24.dp,
        refractionAmount = 40.dp,
        chromaticAberration = 0.10f,
        saturation = 1.7f,
        tint = Color.White,
        tintAlpha = 0.10f,
        isInteractive = !reduceMotion,
        pressLift = 2.dp,
        onDrawSurface = {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    )
                )
            )
        },
    ) {
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
