package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Premium Organic Claymorphism Card
 * Features soft double-depth: extruded ambient shadow + subtle specular top bevel highlight.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    backgroundColor: Color = Color.Unspecified,
    elevation: Dp = 6.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else if (isDark) Color(0xFF141414) else Color(0xFFFFFFFF)
    val ambientBorder = border ?: BorderStroke(
        width = 1.5.dp,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(ClayHighlightDarkTheme, Color.Transparent)
            } else {
                listOf(Color.White.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.05f))
            }
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark,
                spotColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark
            )
            .clip(shape)
            .background(resolvedBg)
            .border(ambientBorder, shape)
    ) {
        CompositionLocalProvider(LocalContentColor provides if (isDark) Color.White else Color.Black) {
            Column(content = content)
        }
    }
}

/**
 * AI Clay Card with subtle Cyan-Lime specular glow
 */
@Composable
fun ClayAICard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    backgroundColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else if (isDark) Color(0xFF141414) else Color(0xFFFFFFFF)
    val glowBorder = BorderStroke(
        width = 2.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                ClayAIGlow.copy(alpha = 0.8f),
                ClayBrightLime.copy(alpha = 0.4f),
                Color.Transparent
            )
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = ClayAIGlow.copy(alpha = 0.25f),
                spotColor = ClayPrimary.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(resolvedBg)
            .border(glowBorder, shape)
    ) {
        CompositionLocalProvider(LocalContentColor provides if (isDark) Color.White else Color.Black) {
            Column(content = content)
        }
    }
}

/**
 * Tactile Claymorphism Primary Button
 * Has physical tactile press feedback (inward bounce and lower elevation on touch).
 */
@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = if (isSystemInDarkTheme()) ClayPrimaryDark else ClayPrimary,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "clay_press_scale"
    )

    val currentElevation = if (isPressed) 2.dp else 7.dp

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = currentElevation,
                shape = shape,
                ambientColor = ClayShadowPressed,
                spotColor = ClayShadowPressed
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor
                    )
                )
            )
            .border(
                BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

/**
 * Secondary Organic Clay Button (soft mint surface, dark green text)
 */
@Composable
fun ClaySecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val resolvedContentColor = if (contentColor != Color.Unspecified) contentColor else if (isDark) Color.White else Color.Black

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "clay_secondary_scale"
    )

    val currentElevation = if (isPressed) 1.5.dp else 4.dp

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = currentElevation,
                shape = shape,
                ambientColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark,
                spotColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark
            )
            .clip(shape)
            .background(resolvedBg)
            .border(
                BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(ClayHighlightDarkTheme, Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.05f))
                        }
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
                content()
            }
        }
    }
}

/**
 * Clay Icon Button
 */
@Composable
fun ClayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
    val resolvedBg = if (backgroundColor != Color.Unspecified) backgroundColor else if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "icon_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .size(48.dp)
            .shadow(3.dp, shape, ambientColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark, spotColor = if (isDark) ClayShadowDarkTheme else ClayShadowDark)
            .clip(shape)
            .background(resolvedBg)
            .border(
                BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)),
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (isDark) Color.White else Color.Black) {
            content()
        }
    }
}

/**
 * Inset Clay Progress Bar
 */
@Composable
fun ClayProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = ClayPrimary,
    trackColor: Color = Color.Unspecified
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
    val resolvedTrackColor = if (trackColor != Color.Unspecified) trackColor else if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(shape)
            .background(resolvedTrackColor)
            .border(
                BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                shape
            )
    ) {
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                fillColor,
                                ClayBrightLime.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        shape
                    )
            )
        }
    }
}
