package com.urfavxbf.kanade.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.shapes.PrismalCapsule

/**
 * The physical play/pause control is the glass surface, not a glass parent panel.
 *
 * Prismal samples the Now Playing backdrop, refracts its edges on Android 13+,
 * and automatically degrades to a frosted surface on older Android versions.
 * Playback state and clicks remain owned by the caller.
 */
@Composable
internal fun LiquidGlassPlayPauseButton(
    playing: Boolean,
    backdrop: PrismalBackdrop,
    accent: Color,
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        blurRadius = 5.dp,
        refractionHeight = 11.dp,
        refractionAmount = 21.dp,
        chromaticAberration = 0.18f,
        saturation = 1.35f,
        tint = accent,
        tintAlpha = 0.13f,
        isInteractive = !reduceMotion,
        pressLift = 2.dp,
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
