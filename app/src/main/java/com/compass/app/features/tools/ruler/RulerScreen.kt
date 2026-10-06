// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.ruler

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.withTranslation
import androidx.core.os.ConfigurationCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.compass.app.R
import com.compass.app.features.compass.DialColors
import com.compass.app.features.compass.rememberDialColors
import com.compass.app.utils.KeepScreenOn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FINE_DEFAULT = 100
private const val FINE_MAX = 200
private const val COARSE_DEFAULT = 2000
private const val COARSE_MAX = 6000
private const val MM_PER_INCH = 25.4f

data class RulerCalibration(val fine: Int = FINE_DEFAULT, val coarse: Int = COARSE_DEFAULT)

@HiltViewModel
class RulerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ruler_prefs", Context.MODE_PRIVATE)

    private val _calibration = MutableStateFlow(
        RulerCalibration(
            fine = prefs.getInt("fine", FINE_DEFAULT),
            coarse = prefs.getInt("coarse", COARSE_DEFAULT),
        )
    )
    val calibration: StateFlow<RulerCalibration> = _calibration.asStateFlow()

    fun setFine(value: Int) = update(_calibration.value.copy(fine = value))

    fun setCoarse(value: Int) = update(_calibration.value.copy(coarse = value))

    fun reset() = update(RulerCalibration())

    private fun update(calibration: RulerCalibration) {
        _calibration.value = calibration
        prefs.edit {
            putInt("fine", calibration.fine)
            putInt("coarse", calibration.coarse)
        }
    }
}

@Composable
fun RulerScreen(
    modifier: Modifier = Modifier,
    viewModel: RulerViewModel = hiltViewModel(),
) {
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    var showCalibrate by remember { mutableStateOf(false) }

    KeepScreenOn()

    val ydpi = LocalResources.current.displayMetrics.ydpi
    // pixels per millimetre, scaled by the user's calibration
    val dpmm = (ydpi / MM_PER_INCH) *
            (1f + (calibration.fine + calibration.coarse - FINE_DEFAULT - COARSE_DEFAULT) / 5000f)

    val colors = rememberDialColors()

    Box(modifier = modifier.fillMaxSize()) {
        RulerScale(dpmm = dpmm, colors = colors, modifier = Modifier.fillMaxSize())
        MeasureLines(
            dpmm = dpmm,
            colors = colors,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = stringResource(R.string.ruler_calibrate),
            color = colors.muted,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCalibrate = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (showCalibrate) {
        AlertDialog(
            onDismissRequest = { showCalibrate = false },
            title = { Text(stringResource(R.string.ruler_calibrate)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.ruler_calibrate_message))
                    Text(stringResource(R.string.ruler_coarse))
                    Slider(
                        value = calibration.coarse.toFloat(),
                        onValueChange = { viewModel.setCoarse(it.toInt()) },
                        valueRange = 0f..COARSE_MAX.toFloat(),
                    )
                    Text(stringResource(R.string.ruler_fine))
                    Slider(
                        value = calibration.fine.toFloat(),
                        onValueChange = { viewModel.setFine(it.toInt()) },
                        valueRange = 0f..FINE_MAX.toFloat(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalibrate = false }) {
                    Text(stringResource(R.string.ok_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.level_reset)) }
            },
        )
    }
}

/** Centimetres down the left edge from the top, 1/32 inches up the right edge from the bottom. */
@Composable
private fun RulerScale(dpmm: Float, colors: DialColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val heightPx = size.height
        val widthPx = size.width
        val dpfi = dpmm * MM_PER_INCH / 32f // pixels per 1/32 inch
        val textSize = dpmm * 3.2f

        val ink = colors.ink.toArgb()
        val soft = colors.muted.toArgb()

        val numberPaint = Paint().apply {
            isAntiAlias = true
            color = ink
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        val unitPaint = Paint().apply {
            isAntiAlias = true
            color = soft
            this.textSize = textSize * 0.62f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val tickPaint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        fun tick(paint: Paint, color: Int, width: Float) {
            paint.color = color
            paint.strokeWidth = width
        }

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas

            // Millimetres, from the top, on the left edge
            var mm = 0
            while (mm < heightPx / dpmm) {
                val y = dpmm * mm
                when {
                    mm % 10 == 0 -> {
                        tick(tickPaint, ink, dpmm * 0.16f)
                        canvas.drawLine(0f, y, dpmm * 8, y, tickPaint)
                        canvas.drawText("${mm / 10}", dpmm * 8 + textSize / 3f, y + textSize * 0.35f, numberPaint)
                    }

                    mm % 5 == 0 -> {
                        tick(tickPaint, ink, dpmm * 0.12f)
                        canvas.drawLine(0f, y, dpmm * 5, y, tickPaint)
                    }

                    else -> {
                        tick(tickPaint, soft, dpmm * 0.09f)
                        canvas.drawLine(0f, y, dpmm * 3, y, tickPaint)
                    }
                }
                mm++
            }

            // 1/32 inches, from the bottom, on the right edge
            var i = 0
            while (i < heightPx / dpfi) {
                val y = heightPx - dpfi * i
                val length = when {
                    i % 32 == 0 -> 8f
                    i % 16 == 0 -> 6f
                    i % 8 == 0 -> 4f
                    i % 4 == 0 -> 3f
                    i % 2 == 0 -> 2f
                    else -> 1.5f
                }
                if (i % 8 == 0) tick(tickPaint, ink, dpmm * if (i % 32 == 0) 0.16f else 0.12f)
                else tick(tickPaint, soft, dpmm * 0.09f)
                canvas.drawLine(widthPx - dpmm * length, y, widthPx, y, tickPaint)
                if (i % 32 == 0) {
                    // inch number, running upwards next to the line
                    val label = "${i / 32}"
                    canvas.withTranslation(widthPx - dpmm * 8 - textSize / 3f, y - textSize * 0.45f) {
                        canvas.rotate(-90f)
                        canvas.drawText(label, 0f, 0f, numberPaint)
                    }
                }
                i++
            }

            // unit labels
            canvas.drawText("cm", dpmm * 8 + textSize / 3f, textSize * 2.4f, unitPaint)
            unitPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("in", widthPx - dpmm * 8 - textSize / 3f, heightPx - textSize * 1.6f, unitPaint)
        }
    }
}

/**
 * Two lines the user drags to measure anything held against the screen; the distance between them
 * is shown in the middle in both centimetres and inches.
 */
@Composable
private fun MeasureLines(
    dpmm: Float,
    colors: DialColors,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var height by remember { mutableFloatStateOf(0f) }
    var y1 by remember { mutableFloatStateOf(-1f) }
    var y2 by remember { mutableFloatStateOf(-1f) }
    var dragging by remember { mutableIntStateOf(0) }

    val background = MaterialTheme.colorScheme.background
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.ROOT

    Box(
        modifier = modifier
            .onSizeChanged {
                height = it.height.toFloat()
                if (y1 < 0f) {
                    y1 = height * 0.28f
                    y2 = height * 0.62f
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start -> dragging = if (abs(start.y - y1) <= abs(start.y - y2)) 0 else 1 },
                    onDrag = { change, drag ->
                        change.consume()
                        if (dragging == 0) y1 = (y1 + drag.y).coerceIn(0f, height)
                        else y2 = (y2 + drag.y).coerceIn(0f, height)
                    },
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (y1 < 0f) return@Canvas
            val margin = dpmm * 10f
            val stroke = 1.5.dp.toPx()
            val knob = 7.dp.toPx()
            val top = minOf(y1, y2)
            val bottom = maxOf(y1, y2)

            // a faint band between the two lines
            drawRect(
                color = accent.copy(alpha = 0.07f),
                topLeft = Offset(margin, top),
                size = androidx.compose.ui.geometry.Size(size.width - 2 * margin, bottom - top),
            )
            for (y in listOf(y1, y2)) {
                drawLine(
                    color = accent,
                    start = Offset(margin, y),
                    end = Offset(size.width - margin, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(background, knob, Offset(size.width / 2f, y))
                drawCircle(accent, knob, Offset(size.width / 2f, y), style = Stroke(stroke))
            }
        }

        if (y1 >= 0f && dpmm > 0f) {
            val millimetres = abs(y2 - y1) / dpmm
            val middle = (y1 + y2) / 2f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .offset { IntOffset(0, (middle - 44.dp.toPx()).roundToInt()) },
            ) {
                // the numbers stay out of the way of the lines when they are close together
                Text(
                    text = String.format(locale, "%.1f cm", millimetres / 10f),
                    color = colors.ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    text = String.format(locale, "%.2f in", millimetres / MM_PER_INCH),
                    color = colors.muted,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
