// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.settings

import android.app.Activity
import android.content.res.Configuration
import android.hardware.SensorManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.compass.app.R
import com.compass.app.core.sensors.SensorViewModel
import com.compass.app.ui.components.LocalBottomBarInset
import com.compass.app.ui.theme.CompassTheme
import com.compass.app.ui.theme.ThemeConfig
import com.compass.app.utils.HapticEvent
import com.compass.app.utils.HapticFeedbackPlayer
import com.compass.app.utils.HapticStrength
import com.compass.app.utils.LanguageManager
import com.compass.app.utils.Links
import com.compass.app.utils.Links.LICENSE_PAGE

@Composable
fun SettingsScreen(
    sensorViewModel: SensorViewModel,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var languageTag by remember { mutableStateOf(LanguageManager.savedTag(context)) }
    val view = LocalView.current
    val sensorAccuracy by sensorViewModel.accuracy.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        accuracy = CompassAccuracy.fromSensorStatus(sensorAccuracy),
        onTrueDarkStateChange = viewModel::setTrueDarkState,
        onTrueNorthStateChange = viewModel::setTrueNorthState,
        onHighPrecisionStateChange = viewModel::setHighPrecisionState,
        onThemeOptionClicked = viewModel::setTheme,
        languageTag = languageTag,
        hapticStrength = uiState.hapticStrength,
        onHapticStrengthSelected = { strength ->
            viewModel.setHapticStrength(strength)
            // let the user feel the strength they just picked
            HapticFeedbackPlayer.play(view, strength, HapticEvent.CONFIRM)
        },
        onLanguageSelected = { tag ->
            if (tag != languageTag) {
                LanguageManager.save(context, tag)
                languageTag = tag
                // restart the activity so every screen picks up the new language
                (context as? Activity)?.recreate()
            }
        },
        onLicensesClicked = {
            uriHandler.openUri(LICENSE_PAGE)
        },
        onLinkClicked = { uriHandler.openUri(it) })
}

// region One UI style

/** Colours and sizes of the settings screen, in the style of Samsung's One UI. */
@Immutable
private data class OneUiStyle(
    val background: Color,
    val card: Color,
    val divider: Color,
    val title: Color,
    val summary: Color,
    val accent: Color,
    val switchOffTrack: Color,
    val switchOffStroke: Color,
)

private val LocalOneUi = staticCompositionLocalOf<OneUiStyle> { error("OneUiStyle not provided") }

private val OneUiBlueLight = Color(0xFF0381FE)
private val OneUiBlueDark = Color(0xFF3E91FF)
private val CardShape = RoundedCornerShape(26.dp)

// One UI spacing: cards sit 12dp from the screen edge, and rows are padded 24dp inside them.
private val SideMargin = 12.dp
private val RowSidePadding = 24.dp
private val RowStartPadding = 16.dp
private val IconBadgeSize = 40.dp
private val IconTextGap = 16.dp

@Composable
private fun rememberOneUiStyle(): OneUiStyle {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            OneUiStyle(
                background = Color.Black,
                card = Color(0xFF1C1C1E),
                divider = Color(0xFF38383A),
                title = Color(0xFFF2F2F7),
                summary = Color(0xFF9A9AA0),
                accent = OneUiBlueDark,
                switchOffTrack = Color(0xFF3A3A3C),
                switchOffStroke = Color(0xFF8F8F8F),
            )
        } else {
            OneUiStyle(
                background = Color(0xFFF5F5F7),
                card = Color.White,
                divider = Color(0xFFE5E5EA),
                title = Color(0xFF1C1C1E),
                summary = Color(0xFF7C7C82),
                accent = OneUiBlueLight,
                switchOffTrack = Color(0xFFE3E3E8),
                switchOffStroke = Color(0xFF8C8C8C),
            )
        }
    }
}

private val BadgeBlue = Color(0xFF3E91FF)
private val BadgeOrange = Color(0xFFFF9500)
private val BadgeIndigo = Color(0xFF5E5CE6)
private val BadgeGraphite = Color(0xFF636366)
private val BadgeTeal = Color(0xFF30B0C7)
private val BadgePink = Color(0xFFFF375F)
private val BadgeGray = Color(0xFF8E8E93)
private val BadgeRed = Color(0xFFFF3B30)
private val StatusGreen = Color(0xFF34C759)

@Composable
fun SettingsScreen(
    uiState: SettingsViewModel.SettingsUiState,
    accuracy: CompassAccuracy,
    onTrueDarkStateChange: (Boolean) -> Unit,
    onTrueNorthStateChange: (Boolean) -> Unit,
    onHighPrecisionStateChange: (Boolean) -> Unit,
    onThemeOptionClicked: (String) -> Unit,
    languageTag: String,
    onLanguageSelected: (String) -> Unit,
    hapticStrength: HapticStrength,
    onHapticStrengthSelected: (HapticStrength) -> Unit,
    onLicensesClicked: () -> Unit,
    onLinkClicked: (String) -> Unit
) {
    val style = rememberOneUiStyle()
    var isThemeDialogVisible by remember { mutableStateOf(false) }
    var isLanguageDialogVisible by remember { mutableStateOf(false) }
    var isHapticDialogVisible by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalOneUi provides style) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(style.background)
        ) {
            SettingsList(
                uiState = uiState,
                accuracy = accuracy,
                onTrueDarkStateChange = onTrueDarkStateChange,
                onTrueNorthStateChange = onTrueNorthStateChange,
                onHighPrecisionStateChange = onHighPrecisionStateChange,
                languageTag = languageTag,
                hapticStrength = hapticStrength,
                onThemeItemClicked = { isThemeDialogVisible = true },
                onLanguageItemClicked = { isLanguageDialogVisible = true },
                onHapticItemClicked = { isHapticDialogVisible = true },
                onLicensesClicked = onLicensesClicked,
                onLinkClicked = onLinkClicked,
            )
        }

        LanguageDialog(
            isDialogVisible = isLanguageDialogVisible,
            onDismissRequest = { isLanguageDialogVisible = false },
            currentTag = languageTag,
            onLanguageClicked = onLanguageSelected,
        )
        HapticStrengthDialog(
            isDialogVisible = isHapticDialogVisible,
            onDismissRequest = { isHapticDialogVisible = false },
            current = hapticStrength,
            onStrengthClicked = onHapticStrengthSelected,
        )
        ThemeDialog(
            isDialogVisible = isThemeDialogVisible,
            onDismissRequest = { isThemeDialogVisible = false },
            currentSelection = uiState.theme,
            options = uiState.themeDialogOptions,
            onOptionClicked = onThemeOptionClicked,
        )
    }
}

@Composable
private fun SettingsList(
    uiState: SettingsViewModel.SettingsUiState,
    accuracy: CompassAccuracy,
    onTrueDarkStateChange: (Boolean) -> Unit,
    onTrueNorthStateChange: (Boolean) -> Unit,
    onHighPrecisionStateChange: (Boolean) -> Unit,
    languageTag: String,
    hapticStrength: HapticStrength,
    onThemeItemClicked: () -> Unit,
    onLanguageItemClicked: () -> Unit,
    onHapticItemClicked: () -> Unit,
    onLicensesClicked: () -> Unit,
    onLinkClicked: (String) -> Unit,
) {
    val style = LocalOneUi.current

    LazyColumn(
        state = rememberLazyListState(),
        // the list ends under the status bar, so scrolling text never runs into the clock
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = SideMargin,
            end = SideMargin,
            bottom = LocalBottomBarInset.current + 16.dp,
        ),
    ) {
        // The big title, with plenty of room above it. It scrolls away with the list.
        item(key = "__title") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    color = style.title,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        item(key = "__compass") {
            SettingsSection(title = stringResource(R.string.compass)) {
                SettingsRow(
                    icon = R.drawable.ic_true_north,
                    badge = BadgeBlue,
                    title = stringResource(R.string.true_north),
                    summary = stringResource(R.string.tn_desc),
                    onClick = { onTrueNorthStateChange(!uiState.isTrueNorthEnabled) },
                    trailing = {
                        OneUiSwitch(uiState.isTrueNorthEnabled, onTrueNorthStateChange)
                    },
                )
                AccuracyRow(accuracy)
            }
        }

        item(key = "__tools") {
            SettingsSection(title = stringResource(R.string.tools)) {
                SettingsRow(
                    icon = R.drawable.ic_level,
                    badge = BadgeOrange,
                    title = stringResource(R.string.precision_title),
                    summary = stringResource(R.string.precision_desc),
                    divider = false,
                    onClick = { onHighPrecisionStateChange(!uiState.isHighPrecisionEnabled) },
                    trailing = {
                        OneUiSwitch(uiState.isHighPrecisionEnabled, onHighPrecisionStateChange)
                    },
                )
            }
        }

        item(key = "__display") {
            SettingsSection(title = stringResource(R.string.display)) {
                SettingsRow(
                    icon = R.drawable.ic_theme,
                    badge = BadgeIndigo,
                    title = stringResource(R.string.theme),
                    summary = getThemeName(option = uiState.theme),
                    onClick = onThemeItemClicked,
                )
                AnimatedVisibility(visible = shouldShowTrueDarkSwitch(uiState.theme)) {
                    SettingsRow(
                        icon = R.drawable.ic_dark_mode,
                        badge = BadgeGraphite,
                        title = stringResource(R.string.amoled_dark),
                        summary = stringResource(R.string.true_black_theme),
                        onClick = { onTrueDarkStateChange(!uiState.isTrueDarkThemeEnabled) },
                        trailing = {
                            OneUiSwitch(uiState.isTrueDarkThemeEnabled, onTrueDarkStateChange)
                        },
                    )
                }
                SettingsRow(
                    icon = R.drawable.ic_language,
                    badge = BadgeTeal,
                    title = stringResource(R.string.language),
                    summary = languageDisplayName(languageTag),
                    divider = false,
                    onClick = onLanguageItemClicked,
                )
            }
        }

        item(key = "__feedback") {
            SettingsSection(title = stringResource(R.string.feedback)) {
                SettingsRow(
                    icon = R.drawable.ic_vibration,
                    badge = BadgePink,
                    title = stringResource(R.string.haptic_title),
                    summary = hapticStrengthName(hapticStrength),
                    divider = false,
                    onClick = onHapticItemClicked,
                )
            }
        }

        item(key = "__about") {
            SettingsSection(title = stringResource(R.string.settings_about)) {
                SettingsRow(
                    icon = R.drawable.ic_license,
                    badge = BadgeGray,
                    title = stringResource(R.string.licenses),
                    summary = stringResource(R.string.app_license),
                    onClick = onLicensesClicked,
                )
                SettingsRow(
                    icon = R.drawable.ic_github,
                    badge = BadgeGraphite,
                    title = stringResource(R.string.made_by),
                    summary = "diecayed",
                    onClick = { onLinkClicked(Links.GITHUB_PROFILE) },
                )
                // The donation page is not ready yet, so this row does nothing for now.
                SettingsRow(
                    icon = R.drawable.ic_favorite,
                    badge = BadgeRed,
                    title = stringResource(R.string.support_title),
                    summary = stringResource(R.string.support_desc),
                    divider = false,
                    onClick = {},
                )
            }
        }

        item(key = "__openSource") {
            Text(
                text = stringResource(R.string.built_with_open_source),
                color = style.summary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = RowStartPadding, top = 22.dp, bottom = 8.dp),
            )
            // the two projects this app is built on, side by side
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RepoButton(
                    label = "MBCompass",
                    modifier = Modifier.weight(1f),
                    onClick = { onLinkClicked(Links.BASE_COMPASS_REPO) },
                )
                RepoButton(
                    label = "Level",
                    modifier = Modifier.weight(1f),
                    onClick = { onLinkClicked(Links.BASE_LEVEL_REPO) },
                )
            }
        }
    }
}

/** A small grey label above one big rounded card that holds a group of rows. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalOneUi.current
    Text(
        text = title,
        color = style.summary,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = RowStartPadding, top = 22.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(style.card),
        content = content,
    )
}

/** One row: a coloured round icon, a title with a grey summary below, and optionally a control. */
@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    badge: Color,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryContent: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val style = LocalOneUi.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = RowStartPadding, top = 14.dp, end = RowSidePadding, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon = icon, color = badge)
            Spacer(Modifier.width(IconTextGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = style.title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                )
                if (summaryContent != null) {
                    summaryContent()
                } else if (summary != null) {
                    Text(
                        text = summary,
                        color = style.summary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        if (divider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = RowStartPadding + IconBadgeSize + IconTextGap, end = RowSidePadding),
                thickness = 0.5.dp,
                color = style.divider,
            )
        }
    }
}

@Composable
private fun IconBadge(@DrawableRes icon: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(IconBadgeSize)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The One UI switch, drawn to Samsung's own measurements (from the open-source SESL library):
 * a slim 32 x 20 dp pill track and a 16 dp round thumb, 2 dp from the edge. On, the track is the
 * One UI blue and the thumb is plain white. Off, the track is a light grey and the thumb gets a
 * thin grey outline. The thumb slides over 300 ms.
 */
@Composable
private fun OneUiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val style = LocalOneUi.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "oneUiSwitch",
    )

    Box(
        modifier = Modifier
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            // a bigger touch area around the slim switch
            .padding(horizontal = 6.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = SwitchWidth, height = SwitchHeight)
                .clip(RoundedCornerShape(SwitchHeight / 2))
                .background(lerp(style.switchOffTrack, style.accent, progress)),
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = lerp(SwitchThumbInset, SwitchWidth - SwitchThumbInset - SwitchThumbSize, progress).roundToPx(),
                            y = SwitchThumbInset.roundToPx(),
                        )
                    }
                    .size(SwitchThumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    // the outline is only there while the switch is off
                    .border(1.dp, style.switchOffStroke.copy(alpha = 1f - progress), CircleShape),
            )
        }
    }
}

private val SwitchWidth = 32.dp
private val SwitchHeight = 20.dp
private val SwitchThumbSize = 16.dp
private val SwitchThumbInset = 2.dp

@Composable
private fun RepoButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val style = LocalOneUi.current
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.card,
            contentColor = style.title,
        ),
        elevation = null,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, fontSize = 15.sp)
    }
}

// endregion

/** How trustworthy the compass sensor currently is. */
enum class CompassAccuracy {
    ACCURATE, NEEDS_CALIBRATION, UNKNOWN;

    companion object {
        /** High and medium are good enough for a compass; low, unreliable and no contact are not. */
        fun fromSensorStatus(status: Int?): CompassAccuracy = when (status) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> ACCURATE

            SensorManager.SENSOR_STATUS_ACCURACY_LOW,
            SensorManager.SENSOR_STATUS_UNRELIABLE,
            SensorManager.SENSOR_STATUS_NO_CONTACT -> NEEDS_CALIBRATION

            else -> UNKNOWN
        }
    }
}

/** Shows how accurate the compass is right now, with the hint for how to calibrate it. */
@Composable
private fun AccuracyRow(accuracy: CompassAccuracy) {
    val style = LocalOneUi.current
    val (label, color, icon) = when (accuracy) {
        CompassAccuracy.ACCURATE -> Triple(
            stringResource(R.string.compass_accuracy_accurate), StatusGreen, R.drawable.ic_signal_full
        )

        CompassAccuracy.NEEDS_CALIBRATION -> Triple(
            stringResource(R.string.compass_accuracy_needs_calibration), BadgeRed, R.drawable.ic_signal_low
        )

        CompassAccuracy.UNKNOWN -> Triple(
            stringResource(R.string.compass_accuracy_checking), BadgeGray, R.drawable.ic_question_mark
        )
    }

    SettingsRow(
        icon = icon,
        badge = color,
        title = stringResource(R.string.compass_accuracy),
        divider = false,
        summaryContent = {
            Text(
                text = label,
                color = color,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = stringResource(R.string.compass_calibration_hint),
                color = style.summary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        },
    )
}

@Composable
fun shouldShowTrueDarkSwitch(theme: String): Boolean {
    return when (theme) {
        ThemeConfig.FOLLOW_SYSTEM.prefName -> isSystemInDarkTheme()
        ThemeConfig.DARK.prefName -> true
        else -> false
    }
}

// region dialogs

/** A rounded One UI dialog: centred title, the content, and one full-width blue button. */
@Composable
private fun OneUiDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val style = LocalOneUi.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = style.card,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = style.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        },
        text = content,
        confirmButton = {
            TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dismiss),
                    color = style.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}

@Composable
private fun RadioOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val style = LocalOneUi.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = style.accent,
                unselectedColor = style.summary,
            ),
        )
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = style.title, fontSize = 17.sp)
    }
}

@Composable
private fun ThemeDialog(
    isDialogVisible: Boolean,
    onDismissRequest: () -> Unit,
    currentSelection: String,
    options: List<String>,
    onOptionClicked: (String) -> Unit,
) {
    if (!isDialogVisible) return

    OneUiDialog(title = stringResource(R.string.choose_theme), onDismissRequest = onDismissRequest) {
        Column {
            for (option in options) {
                RadioOption(
                    label = getThemeName(option = option),
                    selected = option == currentSelection,
                    onClick = {
                        onOptionClicked(option)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}

@Composable
fun getThemeName(option: String): String {
    return when (option) {
        ThemeConfig.FOLLOW_SYSTEM.prefName -> stringResource(R.string.sys_default)
        ThemeConfig.LIGHT.prefName -> stringResource(R.string.light_theme)
        ThemeConfig.DARK.prefName -> stringResource(R.string.dark_theme)
        else -> throw IllegalArgumentException("Unknown theme")
    }
}

/** Each language is written in itself; only "system default" follows the current app language. */
@Composable
private fun languageDisplayName(tag: String): String =
    LanguageManager.supportedLanguages.firstOrNull { it.tag == tag }?.nativeName
        ?: stringResource(R.string.language_system)

@Composable
private fun LanguageDialog(
    isDialogVisible: Boolean,
    onDismissRequest: () -> Unit,
    currentTag: String,
    onLanguageClicked: (String) -> Unit,
) {
    if (!isDialogVisible) return

    val options = listOf(LanguageManager.SYSTEM to stringResource(R.string.language_system)) +
            LanguageManager.supportedLanguages.map { it.tag to it.nativeName }

    OneUiDialog(title = stringResource(R.string.choose_language), onDismissRequest = onDismissRequest) {
        Column {
            for ((tag, name) in options) {
                RadioOption(
                    label = name,
                    selected = tag == currentTag,
                    onClick = {
                        onDismissRequest()
                        onLanguageClicked(tag)
                    },
                )
            }
        }
    }
}

@Composable
private fun hapticStrengthName(strength: HapticStrength): String = stringResource(
    when (strength) {
        HapticStrength.OFF -> R.string.haptic_off
        HapticStrength.SOFT -> R.string.haptic_soft
        HapticStrength.DEFAULT -> R.string.haptic_default
        HapticStrength.POWERFUL -> R.string.haptic_powerful
    }
)

@Composable
private fun HapticStrengthDialog(
    isDialogVisible: Boolean,
    onDismissRequest: () -> Unit,
    current: HapticStrength,
    onStrengthClicked: (HapticStrength) -> Unit,
) {
    if (!isDialogVisible) return

    OneUiDialog(title = stringResource(R.string.choose_haptic_strength), onDismissRequest = onDismissRequest) {
        Column {
            for (strength in HapticStrength.entries) {
                RadioOption(
                    label = hapticStrengthName(strength),
                    selected = strength == current,
                    onClick = { onStrengthClicked(strength) },
                )
            }
        }
    }
}

// endregion

@Preview(showSystemUi = false, showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreview() {
    CompassTheme(darkTheme = true, uiState = SettingsViewModel.SettingsUiState()) {
        SettingsScreen(
            uiState = SettingsViewModel.SettingsUiState(),
            accuracy = CompassAccuracy.ACCURATE,
            onTrueDarkStateChange = {},
            onTrueNorthStateChange = {},
            onHighPrecisionStateChange = {},
            onThemeOptionClicked = {},
            languageTag = LanguageManager.SYSTEM,
            onLanguageSelected = {},
            hapticStrength = HapticStrength.DEFAULT,
            onHapticStrengthSelected = {},
            onLicensesClicked = {},
            onLinkClicked = {})
    }
}
