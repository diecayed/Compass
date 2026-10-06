// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui.components

import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compass.app.navigation.TopLevelRoute
import com.compass.app.utils.HapticEvent
import com.compass.app.utils.HapticFeedbackPlayer
import com.compass.app.utils.HapticStrength

/**
 * How much room the floating bar needs at the bottom of the screen. Scrolling screens use it as
 * bottom padding so their last item can scroll clear of the bar, and fixed screens use it so
 * nothing sits behind the bar.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/** Measurements of the floating bar, after the One UI floating navigation bar. */
object FloatingBarMetrics {
    /** Height of the pill. */
    val Height = 60.dp

    /** Space between the pill and the bottom edge of the screen (above the system navigation bar). */
    val BottomMargin = 12.dp

    /** Space kept free between screen content and the pill. */
    val ContentGap = 12.dp

    /** Total height screens should reserve, not counting the system navigation bar. */
    val ReservedHeight: Dp = Height + BottomMargin + ContentGap

    val ItemWidth = 76.dp
    val IconSize = 24.dp
    val LabelSize = 12.sp

    /** Room for a name under its icon, upright and held sideways (where the bar's height is the limit). */
    val LabelWidth = 68.dp
    val SidewaysLabelWidth = 56.dp
}

private data class FloatingBarColors(
    val surface: Color,
    val selected: Color,
    val primary: Color,
    val secondary: Color,
    val shadow: Dp,
)

private val LightBarColors = FloatingBarColors(
    surface = Color(0xFFFFFFFF),
    selected = Color(0xFFE4E4E4),
    primary = Color(0xFF17151A),
    secondary = Color(0xFF666169),
    shadow = 5.dp,
)

private val DarkBarColors = FloatingBarColors(
    surface = Color(0xFF2E2E30),
    selected = Color(0xFF3D3D3D),
    primary = Color(0xFFF9F7FA),
    secondary = Color(0xFFD0C9D2),
    shadow = 0.dp,
)

private val PillShape = RoundedCornerShape(50)

/** Slight overshoot at the end of the highlight's slide, like One UI. */
private val OvershootEasing = Easing { fraction ->
    val tension = 0.65f
    val t = fraction - 1f
    t * t * ((tension + 1f) * t + tension) + 1f
}

/**
 * The bottom menu as a floating island: a rounded pill that hovers above the bottom edge instead
 * of covering it. Every item shows an icon over its name, and a soft highlight slides to the
 * selected one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompassBottomBar(
    destinations: List<TopLevelRoute<*>>,
    currentRoute: Any,
    navigateToRoute: (Any) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: (Any) -> Unit = {},
    hapticStrength: HapticStrength = HapticStrength.DEFAULT,
) {
    val colors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkBarColors else LightBarColors
    val selectedIndex = destinations
        .indexOfFirst { it.route::class == currentRoute::class }
        .coerceAtLeast(0)

    val highlightX by animateDpAsState(
        targetValue = FloatingBarMetrics.ItemWidth * selectedIndex,
        animationSpec = tween(durationMillis = 175, easing = OvershootEasing),
        label = "barHighlight",
    )
    val view = LocalView.current
    val turn = rememberScreenTurn()

    Box(
        modifier = modifier
            .shadow(elevation = colors.shadow, shape = PillShape)
            .clip(PillShape)
            .background(colors.surface)
            .padding(horizontal = 3.dp),
    ) {
        // the highlight under the selected item
        Box(
            modifier = Modifier
                .offset { IntOffset(highlightX.roundToPx(), 0) }
                .size(width = FloatingBarMetrics.ItemWidth, height = FloatingBarMetrics.Height)
                .padding(vertical = 1.dp)
                .clip(PillShape)
                .background(colors.selected),
        )

        Row(horizontalArrangement = Arrangement.Start) {
            destinations.forEachIndexed { index, destination ->
                BarItem(
                    label = stringResource(destination.label),
                    icon = destination.icon,
                    selected = index == selectedIndex,
                    colors = colors,
                    view = view,
                    hapticStrength = hapticStrength,
                    turn = turn,
                    onClick = { navigateToRoute(destination.route) },
                    onLongClick = { onItemLongClick(destination.route) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarItem(
    label: String,
    icon: Int,
    selected: Boolean,
    colors: FloatingBarColors,
    view: View,
    hapticStrength: HapticStrength,
    turn: ScreenTurn,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.secondary,
        animationSpec = tween(durationMillis = 175),
        label = "barItemTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = FloatingBarMetrics.ItemWidth, height = FloatingBarMetrics.Height)
            .clip(PillShape)
            .semantics { this.selected = selected }
            .combinedClickable(
                role = Role.Tab,
                hapticFeedbackEnabled = false,
                onClick = onClick,
                onLongClick = {
                    HapticFeedbackPlayer.play(view, hapticStrength, HapticEvent.CONFIRM)
                    onLongClick()
                },
            ),
    ) {
        // The item stays where it is; only what is inside it turns to read upright.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { rotationZ = turn.angle },
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(FloatingBarMetrics.IconSize),
            )
            Spacer(Modifier.height(4.dp))
            // Long names shrink to fit instead of being cut off. Held sideways there is less room.
            BasicText(
                text = label,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = FloatingBarMetrics.LabelSize),
                style = TextStyle(
                    color = tint,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.width(
                    if (turn.sideways) FloatingBarMetrics.SidewaysLabelWidth else FloatingBarMetrics.LabelWidth
                ),
            )
        }
    }
}
