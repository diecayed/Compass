// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.compass.app.R
import com.compass.app.features.compass.rememberDialColors
import com.compass.app.utils.HapticEvent
import com.compass.app.utils.HapticFeedbackPlayer
import com.compass.app.utils.KeepScreenOn
import kotlin.math.abs
import kotlin.math.hypot

private const val BUBBLE_RATE_PER_SECOND = 4f

/** Green for "level": the iOS-style green on dark backgrounds, a deeper one on light backgrounds. */
@Composable
private fun levelGreen(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF34C759)
    else Color(0xFF1E9E46)

@Composable
fun LevelScreen(
    modifier: Modifier = Modifier,
    viewModel: LevelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calibrationEvent by viewModel.calibrationEvents.collectAsStateWithLifecycle()
    var showCalibrateDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    KeepScreenOn()

    LaunchedEffect(calibrationEvent) {
        val message = when (calibrationEvent) {
            LevelViewModel.CalibrationEvent.Saved -> R.string.level_calibrate_saved
            LevelViewModel.CalibrationEvent.Reset -> R.string.level_calibrate_restored
            null -> return@LaunchedEffect
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.calibrationEventConsumed()
    }

    if (!state.supported) {
        Text(
            text = stringResource(R.string.level_not_supported),
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = rememberDialColors().muted,
        )
        return
    }

    val dial = rememberDialColors()

    // One buzz the moment the level turns green.
    val view = LocalView.current
    val trigger = remember { LevelHapticTrigger(initiallyLevel = state.isLevel) }
    LaunchedEffect(state.isLevel, state.tilt) {
        if (trigger.update(state.isLevel, state.tilt)) {
            HapticFeedbackPlayer.play(view, state.hapticStrength, HapticEvent.SUCCESS)
        }
    }
    val accent by animateColorAsState(
        targetValue = if (state.isLevel) levelGreen() else dial.ink,
        animationSpec = tween(durationMillis = 250),
        label = "levelAccent",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight

        val actions = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietAction(
                    text = stringResource(R.string.level_lock),
                    active = state.locked,
                    ink = dial.ink,
                    muted = dial.muted,
                    onClick = viewModel::toggleLock,
                )
                QuietAction(
                    text = stringResource(R.string.level_calibrate),
                    active = false,
                    ink = dial.ink,
                    muted = dial.muted,
                    onClick = { showCalibrateDialog = true },
                )
            }
        }

        if (landscape) {
            // Instrument on the left, readout and actions on the right.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelInstrument(
                    state = state,
                    accent = accent,
                    muted = dial.muted,
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Readout(state = state, accent = accent, muted = dial.muted, compact = true)
                    Spacer(Modifier.height(12.dp))
                    actions()
                    ModeTitle(state = state, muted = dial.muted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LevelInstrument(
                    state = state,
                    accent = accent,
                    muted = dial.muted,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Readout(state = state, accent = accent, muted = dial.muted)
                Spacer(Modifier.height(12.dp))
                actions()
                ModeTitle(
                    state = state,
                    muted = dial.muted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
            }
        }
    }

    if (showCalibrateDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrateDialog = false },
            title = { Text(stringResource(R.string.level_calibrate_title)) },
            text = { Text(stringResource(R.string.level_calibrate_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.calibrate()
                    showCalibrateDialog = false
                }) { Text(stringResource(R.string.level_calibrate)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.resetCalibration()
                        showCalibrateDialog = false
                    }) { Text(stringResource(R.string.level_reset)) }
                    TextButton(onClick = { showCalibrateDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }
}

/** Name of the current mode. It stays put and does not turn with the readout. */
@Composable
private fun ModeTitle(state: LevelUiState, muted: Color, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            when (state.orientation) {
                DeviceOrientation.LANDING -> R.string.level_mode_flat
                DeviceOrientation.TOP, DeviceOrientation.BOTTOM -> R.string.level_mode_horizontal
                DeviceOrientation.LEFT, DeviceOrientation.RIGHT -> R.string.level_mode_vertical
            }
        ),
        color = muted,
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

/** The big angle and, when lying flat in precise mode, the two axes. Turns to stay parallel to the ground. */
@Composable
private fun Readout(state: LevelUiState, accent: Color, muted: Color, compact: Boolean = false) {
    // Keeps the readout parallel to the ground: it turns against the phone's tilt.
    val rotation = rememberUprightRotation(state.uprightRotation)

    // The numbers change at a calm rhythm instead of with every sensor reading.
    val precise = state.highPrecision
    val tilt = rememberStableValue(state.tilt, precise)
    val x = rememberStableValue(state.angle2, precise)
    val y = rememberStableValue(state.angle1, precise)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { rotationZ = rotation },
    ) {
        Text(
            text = formatLevelAngle(tilt, precise),
            color = accent,
            fontSize = if (compact) 56.sp else 72.sp,
            fontWeight = FontWeight.Light,
        )
        // X and Y only show in precise mode. The space stays reserved so the layout does not jump.
        Text(
            text = if (precise && state.orientation == DeviceOrientation.LANDING) {
                "X ${formatLevelAngle(x, precise)}     " +
                        "Y ${formatLevelAngle(y, precise)}"
            } else {
                " "
            },
            color = muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun QuietAction(
    text: String,
    active: Boolean,
    ink: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (active) ink else muted,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) ink else Color.Transparent)
        )
    }
}

@Composable
private fun LevelInstrument(
    state: LevelUiState,
    accent: Color,
    muted: Color,
    modifier: Modifier = Modifier,
) {
    val currentState by rememberUpdatedState(state)
    var posX by remember { mutableFloatStateOf(0f) }
    var posY by remember { mutableFloatStateOf(0f) }

    // Bubble physics: ease the bubble towards its target at a fixed rate (the "viscosity").
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    val s = currentState
                    var targetX: Float
                    var targetY = 0f
                    when (s.orientation) {
                        DeviceOrientation.LANDING -> {
                            targetX = 2 * s.bubbleX
                            targetY = 2 * s.bubbleY
                            val r = hypot(targetX, targetY)
                            if (r > 1f) {
                                targetX /= r
                                targetY /= r
                            }
                        }

                        DeviceOrientation.TOP, DeviceOrientation.BOTTOM ->
                            targetX = (s.orientation.reverse * 2 * s.bubbleX).coerceIn(-1f, 1f)

                        DeviceOrientation.LEFT, DeviceOrientation.RIGHT ->
                            targetX = (s.orientation.reverse * 2 * s.bubbleY).coerceIn(-1f, 1f)
                    }
                    val k = (BUBBLE_RATE_PER_SECOND * dt).coerceAtMost(1f)
                    posX += (targetX - posX) * k
                    posY += (targetY - posY) * k
                }
                last = now
            }
        }
    }

    Canvas(modifier = modifier) {
        if (state.orientation == DeviceOrientation.LANDING) {
            drawFlatLevel(posX, posY, muted, accent)
        } else {
            drawScaleLevel(state.orientation, posX, muted, accent)
        }
    }
}

/** An outer ring, a target circle and the bubble: nothing else. */
private fun DrawScope.drawFlatLevel(posX: Float, posY: Float, muted: Color, accent: Color) {
    // the outer ring is where the bubble stops
    val field = minOf(size.width, size.height) / 2f * 0.96f
    val center = Offset(size.width / 2f, size.height / 2f)
    val hair = field * 0.006f

    drawCircle(muted.copy(alpha = 0.4f), field, center, style = Stroke(hair))

    // target and bubble have the same size, so the bubble sits exactly inside the target when level
    val bubbleRadius = field * 0.155f
    drawCircle(accent.copy(alpha = 0.7f), bubbleRadius, center, style = Stroke(hair * 2f))

    val travel = field - bubbleRadius
    val bubble = Offset(center.x + posX * travel, center.y + posY * travel)
    drawCircle(accent.copy(alpha = 0.16f), bubbleRadius, bubble)
    drawCircle(accent, bubbleRadius, bubble, style = Stroke(hair * 2.4f))
    drawCircle(accent, field * 0.016f, bubble)
}

/** A scale of ticks with two target marks and a bubble sliding along it. */
private fun DrawScope.drawScaleLevel(
    orientation: DeviceOrientation,
    posX: Float,
    muted: Color,
    accent: Color,
) {
    val sideways = abs(orientation.rotation) == 90
    // the scale runs along the screen's long side when the device is on its side
    val length = if (sideways) size.height else size.width
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = length / 2f * 0.92f
    // the scale never grows taller than the space across it, which is small in landscape
    val across = if (sideways) size.width else size.height
    val unit = minOf(half * 0.04f, 7.dp.toPx(), across * 0.08f)
    val hair = 1.dp.toPx()

    rotate(orientation.rotation.toFloat(), center) {
        drawLine(
            color = muted.copy(alpha = 0.25f),
            start = Offset(center.x - half, center.y),
            end = Offset(center.x + half, center.y),
            strokeWidth = hair,
        )
        for (k in -10..10) {
            val x = center.x + k / 10f * half
            val tick = when {
                k == 0 -> unit * 2.6f
                k % 5 == 0 -> unit * 1.7f
                else -> unit
            }
            drawLine(
                color = muted.copy(alpha = if (k % 5 == 0) 0.85f else 0.4f),
                start = Offset(x, center.y - tick),
                end = Offset(x, center.y + tick),
                strokeWidth = if (k % 5 == 0) hair * 1.6f else hair,
                cap = StrokeCap.Round,
            )
        }

        // two target marks the bubble should rest between
        val bubbleRadius = minOf(half * 0.06f, 20.dp.toPx(), across * 0.22f)
        val mark = bubbleRadius * 1.35f
        for (side in listOf(-1f, 1f)) {
            drawLine(
                color = accent.copy(alpha = 0.7f),
                start = Offset(center.x + side * mark, center.y - unit * 4.2f),
                end = Offset(center.x + side * mark, center.y + unit * 4.2f),
                strokeWidth = hair * 1.6f,
                cap = StrokeCap.Round,
            )
        }

        val travel = half - bubbleRadius
        val bubble = Offset(center.x + posX * travel, center.y)
        drawCircle(accent.copy(alpha = 0.16f), bubbleRadius, bubble)
        drawCircle(accent, bubbleRadius, bubble, style = Stroke(hair * 1.8f))
        drawCircle(accent, bubbleRadius * 0.12f, bubble)
    }
}

/**
 * Smooths [target] (degrees) and always turns the short way round, so the readout does not spin
 * a full circle when the angle wraps from 179 to -179.
 */
@Composable
private fun rememberUprightRotation(target: Float): Float {
    var previous by remember { mutableFloatStateOf(target) }
    var unwrapped by remember { mutableFloatStateOf(target) }

    LaunchedEffect(target) {
        unwrapped += ((target - previous + 540f) % 360f) - 180f
        previous = target
    }

    val animated by animateFloatAsState(
        targetValue = unwrapped,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "uprightRotation",
    )
    return animated
}
