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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
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
            ambientColor = WarmShadow.copy(alpha = 0.34f),
            spotColor = WarmShadow.copy(alpha = 0.40f),
        )
        .then(
            if (backdrop == null) Modifier
            else Modifier.liquid(backdrop) {
                this.shape = shape
                refraction = 0.48f
                curve = 0.48f
                edge = 0.17f
                dispersion = 0.045f
                frost = 0.6.dp
                saturation = 1.06f
                tint = WarmGlass.copy(alpha = 0.045f)
            }
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFCF7).copy(alpha = 0.10f),
                    WarmGlass.copy(alpha = 0.025f),
                    Color(0xFFB7A89A).copy(alpha = 0.035f),
                )
            )
        )
        .border(
            width = 1.25.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.84f),
                    Color.White.copy(alpha = 0.10f),
                    Color(0xFFFFE9C9).copy(alpha = 0.68f),
                )
            ),
            shape = shape,
        )
        .drawWithContent {
            drawContent()
            val inset = 2.dp.toPx()
            val corner = (radius.toPx() - inset).coerceAtLeast(0f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.Transparent,
                        Color(0xFFFFDDB4).copy(alpha = 0.24f))
                ),
                topLeft = Offset(inset, inset),
                size = Size(
                    (size.width - inset * 2).coerceAtLeast(0f),
                    (size.height - inset * 2).coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius(corner),
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
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
    backdrop: LiquidState? = null,
    contentColor: Color = Color(0xFFF9F3EC),
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
            .warmRaisedGlass(backdrop, radius = 16.dp, elevation = if (active) 5.dp else 2.dp)
            .background(if (active) Color(0xFFFFF3E3).copy(alpha = 0.12f) else Color.Transparent)
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
            color = contentColor,
            selected = active,
            size = 22.dp,
            bold = false,
        )
    }
}
