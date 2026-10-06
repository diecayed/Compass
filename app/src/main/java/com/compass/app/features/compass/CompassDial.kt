// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.compass

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringArrayResource
import androidx.core.graphics.withRotation
import com.compass.app.R
import com.compass.app.utils.Azimuth

internal val DialNorth = Color(0xFFFF3B30)

/** Colours of the dial, taken from the active app theme so it follows light, dark and AMOLED. */
@Immutable
data class DialColors(val ink: Color, val muted: Color, val north: Color)

@Composable
fun rememberDialColors(): DialColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme.onBackground, scheme.onSurfaceVariant) {
        DialColors(ink = scheme.onBackground, muted = scheme.onSurfaceVariant, north = DialNorth)
    }
}

/** Where the cross arms stop, as a fraction of the dial radius. */
private const val CROSS_ARM_END = 0.50f

/** Radius (fraction of the ring) of the 0 / 90 / 180 / 270 numbers just inside N, E, S, W. */
private const val CARDINAL_DEGREE_RADIUS = 0.585f

/**
 * Compass dial in the style of Apple's Compass: a ring of fine ticks with degree numbers and
 * cardinal letters (north in red) that rotates under a fixed heading marker, with a crosshair
 * in the middle.
 */
@Composable
fun CompassDial(
    azimuth: Azimuth,
    modifier: Modifier = Modifier,
    colors: DialColors = rememberDialColors(),
    cardinalLetters: List<String> = stringArrayResource(R.array.cardinal_letters).toList(),
) {
    val targetRotation = azimuth.roundedDegrees

    // Take the shortest way around when the heading wraps past north.
    var previousRotation by remember { mutableFloatStateOf(targetRotation) }
    var adjustedRotation by remember { mutableFloatStateOf(targetRotation) }

    LaunchedEffect(targetRotation) {
        val diff = targetRotation - previousRotation
        adjustedRotation += when {
            diff > 180 -> diff - 360
            diff < -180 -> diff + 360
            else -> diff
        }
        previousRotation = targetRotation
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -adjustedRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "CompassDialRotation",
    )

    Box(modifier = modifier) {
        // The ring is drawn once and rotated on the GPU.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = animatedRotation }
        ) { drawRing(colors, cardinalLetters) }

        Canvas(modifier = Modifier.fillMaxSize()) { drawFixedOverlay(colors) }
    }
}

private fun DrawScope.drawRing(colors: DialColors, cardinalLetters: List<String>) {
    val half = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = half * 0.94f

    // Ticks every 2 degrees; stronger every 10, thicker every 30, thickest at the cardinals.
    for (deg in 0 until 360 step 2) {
        val cardinal = deg % 90 == 0
        val thirty = deg % 30 == 0
        val ten = deg % 10 == 0
        val length = when {
            cardinal || thirty -> outer * 0.09f
            ten -> outer * 0.07f
            else -> outer * 0.04f
        }
        val width = when {
            cardinal -> outer * 0.016f
            thirty -> outer * 0.013f
            ten -> outer * 0.006f
            else -> outer * 0.004f
        }
        val color = when {
            deg == 0 -> colors.north
            ten -> colors.ink
            else -> colors.muted
        }
        rotate(deg.toFloat(), center) {
            drawLine(
                color = color,
                start = Offset(center.x, center.y - outer),
                end = Offset(center.x, center.y - outer + length),
                strokeWidth = width,
            )
        }
    }

    val numberPaint = Paint().apply {
        isAntiAlias = true
        color = colors.muted.toArgb()
        textSize = outer * 0.075f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val letterPaint = Paint().apply {
        isAntiAlias = true
        textSize = outer * 0.15f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val cardinalDegreePaint = Paint(numberPaint).apply { textSize = outer * 0.065f }
    val numberRadius = outer * 0.76f
    val letterRadius = outer * 0.73f

    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas
        for (deg in 0 until 360 step 30) {
            val isCardinal = deg % 90 == 0
            val paint = if (isCardinal) letterPaint.apply {
                color = (if (deg == 0) colors.north else colors.ink).toArgb()
            } else numberPaint
            val label = if (isCardinal) cardinalLetters[deg / 90] else deg.toString()
            val radius = if (isCardinal) letterRadius else numberRadius
            canvas.withRotation(deg.toFloat(), center.x, center.y) {
                canvas.drawText(label, center.x, center.y - radius + paint.textSize * 0.35f, paint)
                if (isCardinal) {
                    canvas.drawText(
                        deg.toString(),
                        center.x,
                        center.y - outer * CARDINAL_DEGREE_RADIUS + cardinalDegreePaint.textSize * 0.35f,
                        cardinalDegreePaint
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFixedOverlay(colors: DialColors) {
    val half = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = half * 0.94f

    // heading marker: a straight line across the ring at the top
    drawLine(
        color = colors.ink,
        start = Offset(center.x, center.y - half * 0.995f),
        end = Offset(center.x, center.y - outer * 0.88f),
        strokeWidth = half * 0.022f,
        cap = StrokeCap.Round,
    )

    // crosshair: four equal, thin arms. They stop short of the degree numbers on the dial.
    val gap = half * 0.10f
    val arm = half * CROSS_ARM_END
    val crossColor = colors.muted
    val crossWidth = half * 0.006f
    drawLine(crossColor, Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), crossWidth)
    drawLine(crossColor, Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), crossWidth)
    drawLine(crossColor, Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), crossWidth)
    drawLine(crossColor, Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), crossWidth)
    drawCircle(colors.ink, radius = half * 0.075f, center = center, style = Stroke(width = half * 0.012f))
}
