package com.urfavxbf.kanade.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

private val WarmGlass = Color(0xFFF0E7DE)
private val WarmShadow = Color(0xFF130C0B)

/**
 * Shared warm, softly raised optical surface for player chrome.
 *
 * [backdrop] is optional so text fields hosted in separate dialog windows
 * receive the same visual finish without sampling a different window.
 * Sample background AND page content, but never the glass chrome itself.
 */
internal fun Modifier.warmRaisedGlass(
    backdrop: LiquidState?,
    radius: Dp,
    elevation: Dp = 8.dp,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = WarmShadow.copy(alpha = 0.24f),
            spotColor = WarmShadow.copy(alpha = 0.25f),
        )
        .then(
            if (backdrop == null) Modifier
            else Modifier.liquid(backdrop) {
                this.shape = shape
                refraction = 0.38f
                curve = 0.36f
                edge = 0.10f
                dispersion = 0.065f
                frost = 1.dp
                saturation = 1.12f
                tint = WarmGlass.copy(alpha = 0.06f)
            }
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFCF7).copy(alpha = 0.055f),
                    WarmGlass.copy(alpha = 0.025f),
                    Color(0xFFB7A89A).copy(alpha = 0.018f),
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.28f),
                )
            ),
            shape = shape,
        )
}

/**
 * One glass surface for the entire lower player area: live equalizer, elapsed
 * and total time, plus transport controls. The video/artwork remains the
 * refraction source; the controls are deliberately not recorded in it.
 *
 * Inspired by the warm-gray, raised-glass controls in the supplied reference.
 */
@Composable
internal fun GlassPlayerControls(
    backdrop: LiquidState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .warmRaisedGlass(backdrop, radius = 27.dp, elevation = 12.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        content = content,
    )
}

/**
 * Separate, minimal raised buttons, not one segmented controls strip.
 * Press feedback respects the app's Reduce Motion preference.
 */
@Composable
internal fun MinimalGlassControlButton(
    icon: KanadeIcon,
    description: String,
    active: Boolean = false,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.94f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 0.70f, stiffness = 750f),
        label = "Control press",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = pressedScale; scaleY = pressedScale }
            .shadow(
                elevation = if (active) 5.dp else 2.dp,
                shape = shape,
                ambientColor = Color(0xFFF4D8B9).copy(alpha = if (active) 0.45f else 0.10f),
                spotColor = WarmShadow.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (active) listOf(
                        Color(0xFFFFF3E3).copy(alpha = 0.18f),
                        Color(0xFFE2C7AC).copy(alpha = 0.09f),
                    ) else listOf(
                        Color(0xFFFFFAF5).copy(alpha = 0.075f),
                        Color(0xFFE7DAD0).copy(alpha = 0.025f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (active) 0.64f else 0.43f),
                        Color.White.copy(alpha = 0.09f),
                    )
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        KanadeGlyph(
            icon = icon,
            color = if (active) Color(0xFFFFE4C4) else Color(0xFFF9F3EC),
            selected = active,
            size = 22.dp,
            bold = false,
        )
    }
}
