// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * One UI text is set tight, with no extra tracking, which is the main thing that makes Material's
 * default type scale look loose next to it. Sizes follow One UI's list type: 17sp titles over 14sp
 * summaries. The font is the system font, which on a Samsung phone is Samsung's own One UI Sans.
 */
private fun TextStyle.oneUi(
    size: Float? = null,
    lineHeight: Float? = null,
    weight: FontWeight? = null,
): TextStyle = copy(
    fontFamily = FontFamily.Default,
    fontSize = size?.sp ?: fontSize,
    lineHeight = lineHeight?.sp ?: this.lineHeight,
    fontWeight = weight ?: fontWeight,
    letterSpacing = 0.sp,
)

private val Material = Typography()

val Typography = Typography(
    displayLarge = Material.displayLarge.oneUi(),
    displayMedium = Material.displayMedium.oneUi(),
    displaySmall = Material.displaySmall.oneUi(),
    headlineLarge = Material.headlineLarge.oneUi(),
    headlineMedium = Material.headlineMedium.oneUi(),
    headlineSmall = Material.headlineSmall.oneUi(),
    titleLarge = Material.titleLarge.oneUi(size = 22f, lineHeight = 28f, weight = FontWeight.Normal),
    titleMedium = Material.titleMedium.oneUi(size = 17f, lineHeight = 24f, weight = FontWeight.Medium),
    titleSmall = Material.titleSmall.oneUi(size = 14f, lineHeight = 20f, weight = FontWeight.Medium),
    bodyLarge = Material.bodyLarge.oneUi(size = 17f, lineHeight = 24f, weight = FontWeight.Normal),
    bodyMedium = Material.bodyMedium.oneUi(size = 14f, lineHeight = 20f, weight = FontWeight.Normal),
    bodySmall = Material.bodySmall.oneUi(size = 12f, lineHeight = 16f, weight = FontWeight.Normal),
    labelLarge = Material.labelLarge.oneUi(size = 14f, lineHeight = 20f, weight = FontWeight.Medium),
    labelMedium = Material.labelMedium.oneUi(size = 12f, lineHeight = 16f, weight = FontWeight.Medium),
    labelSmall = Material.labelSmall.oneUi(size = 11f, lineHeight = 16f, weight = FontWeight.Medium),
)
