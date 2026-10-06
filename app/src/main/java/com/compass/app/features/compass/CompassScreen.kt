// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.compass

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compass.app.MainViewModel
import com.compass.app.R
import com.compass.app.core.location.AndroidLocationManager
import com.compass.app.core.sensors.AndroidSensorEventListener
import com.compass.app.core.sensors.SensorViewModel
import com.compass.app.features.settings.SettingsViewModel
import com.compass.app.ui.components.FloatingBarMetrics
import com.compass.app.ui.components.LocalBottomBarInset
import com.compass.app.ui.components.ScreenTurn
import com.compass.app.ui.components.rememberAutoRotateEnabled
import com.compass.app.ui.components.rememberScreenTurn
import com.compass.app.utils.Azimuth
import com.compass.app.utils.HapticEvent
import com.compass.app.utils.HapticFeedbackPlayer
import com.compass.app.utils.HapticStrength
import com.compass.app.utils.KeepScreenOn
import kotlin.math.roundToInt

@Composable
fun CompassScreen(
    sensorViewModel: SensorViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
) {
    val context = LocalContext.current

    var sensorEventListener by remember { mutableStateOf<AndroidSensorEventListener?>(null) }

    val dialogState by sensorViewModel.accuracyDialog.collectAsStateWithLifecycle()

    val androidLocationManager = remember {
        AndroidLocationManager(context) { location ->
            sensorViewModel.provideLocation(location)
        }
    }

    LaunchedEffect(Unit) {
        sensorEventListener = AndroidSensorEventListener(
            context = context,
            sensorViewModel = sensorViewModel,
            onAccuracyUpdate = { accuracy ->
                sensorViewModel.updateSensorAccuracy(accuracy)
            },
        )
    }

    // Show AlertDialog based on dialogState
    if (dialogState.show && dialogState.accuracyForDialog != null) {
        ShowAccuracyAlertDialog(
            context = context, accuracy = dialogState.accuracyForDialog!!, onDismiss = {
                sensorViewModel.dismissAccuracyDialog()
            })
    }

    KeepScreenOn()
    var magneticStrength by remember { mutableFloatStateOf(0F) }

    val azimuthSensor by mainViewModel.azimuth.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // Coordinates and elevation need the user's location.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            @SuppressLint("MissingPermission")
            androidLocationManager.registerLocationListener()
        }
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context) && !askedForLocationThisSession) {
            askedForLocationThisSession = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    sensorEventListener?.let { listener ->
        RegisterListener(
            lifecycleOwner = LocalLifecycleOwner.current,
            listener = listener,
            androidLocationManager = androidLocationManager,
            sensorViewModel = sensorViewModel,
            settingsUiState = settingsState,
            mainViewModel = mainViewModel,
            mStrength = { magneticStrength = it })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(bottom = LocalBottomBarInset.current)
    ) {
        Compass(
            modifier = Modifier.weight(1f),
            sensorViewModel = sensorViewModel,
            degreeIn = azimuthSensor,
            androidLocationManager = androidLocationManager,
            magneticStrength = magneticStrength,
            hapticStrength = settingsState.hapticStrength
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Compass(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    sensorViewModel: SensorViewModel = viewModel(),
    androidLocationManager: AndroidLocationManager,
    degreeIn: Azimuth,
    magneticStrength: Float,
    hapticStrength: HapticStrength,
) {
    val view = LocalView.current
    val azimuthState by viewModel.azimuth.collectAsStateWithLifecycle()
    val strength by viewModel.strength.collectAsStateWithLifecycle()

    val location by sensorViewModel.location.collectAsStateWithLifecycle()
    val trueNorthEnabled by sensorViewModel.trueNorthEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(
        degreeIn, magneticStrength
    ) {
        viewModel.updateAzimuth(degreeIn)
        viewModel.updateMagneticStrength(magneticStrength)
    }

    val degree by remember {
        derivedStateOf { azimuthState.roundedDegrees }
    }

    val abbreviations = stringArrayResource(R.array.direction_abbreviations)
    val hemisphereFirst = LocalResources.current.getBoolean(R.bool.coordinates_hemisphere_first)
    val cardinalNames = stringArrayResource(
        if (hemisphereFirst) R.array.coordinate_hemisphere_names else R.array.cardinal_letters
    ).toList()
    val abbreviation by remember(abbreviations) {
        derivedStateOf { abbreviations[((degree + 22.5f) / 45f).toInt() % 8] }
    }

    val strengthRounded by remember {
        derivedStateOf { strength.roundToInt() }
    }

    val context = LocalContext.current
    val dial = rememberDialColors()

    var isLoadingLocation by remember { mutableStateOf(false) }
    LaunchedEffect(location) {
        if (location != null) isLoadingLocation = false
    }
    LaunchedEffect(trueNorthEnabled) {
        if (!trueNorthEnabled) isLoadingLocation = false
    }

    val needsReload = trueNorthEnabled && location == null

    val northLabel = when {
        trueNorthEnabled && location != null -> stringResource(R.string.true_north)
        needsReload -> stringResource(R.string.reload_location)
        else -> stringResource(R.string.magnetic_north)
    }

    HeadingHaptics(headingDegrees = degreeIn.degrees, strength = hapticStrength)

    CompassLayout(
        turn = rememberScreenTurn(frozen = !rememberAutoRotateEnabled()),
        modifier = modifier.fillMaxSize(),
        dial = {
            CompassDial(
                azimuth = degreeIn,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )
        },
    ) {
        // everything under the dial; it turns with the phone so it reads upright held sideways too
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${degree.roundToInt()}° $abbreviation",
                    color = dial.ink,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                )
            }

            // coordinates and elevation, right under the heading
            val currentLocation = location
            if (currentLocation != null) {
                val coordinates = formatCoordinates(currentLocation, cardinalNames, hemisphereFirst)
                val copyLabel = stringResource(R.string.copy_coordinates)
                val copiedMessage = stringResource(R.string.coordinates_copied)
                Text(
                    text = coordinates,
                    color = dial.ink,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = {},
                            onLongClickLabel = copyLabel,
                            hapticFeedbackEnabled = false,
                            onLongClick = {
                                if (copyCoordinates(context, coordinates)) {
                                    HapticFeedbackPlayer.play(view, hapticStrength, HapticEvent.SUCCESS)
                                    // Android 13 and up shows its own "copied" message
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                formatElevation(currentLocation)?.let { elevation ->
                    Text(
                        text = stringResource(R.string.elevation_value, elevation),
                        color = dial.muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = needsReload) {
                            isLoadingLocation = true
                            handleLocationRequest(context, androidLocationManager)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = dial.muted
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = northLabel,
                        color = if (needsReload) dial.north else dial.muted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.magnetic_field_value, strengthRounded),
                    color = dial.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * The dial and the readings under it. The dial never changes size or place. The readings sit under
 * the dial held upright and slide to the middle of the gap between the dial and the bottom bar,
 * turning as they go, when the phone is held sideways.
 */
@Composable
private fun CompassLayout(
    turn: ScreenTurn,
    modifier: Modifier = Modifier,
    dial: @Composable () -> Unit,
    readings: @Composable () -> Unit,
) {
    val slide by animateFloatAsState(
        targetValue = if (turn.sideways) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "readingsSlide",
    )
    Layout(
        content = {
            Box { dial() }
            Box { readings() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val bottomPadding = 16.dp.roundToPx()

        // the dial is sized from the readings' upright height, so it is the same whichever way the phone is held
        val readingsPlaceable = measurables[1].measure(Constraints(maxWidth = width))
        val room = (height - readingsPlaceable.height - bottomPadding).coerceAtLeast(0)
        val side = minOf(width, room)
        val dialPlaceable = measurables[0].measure(Constraints.fixed(side, side))
        val dialTop = (room - side) / 2

        layout(width, height) {
            dialPlaceable.place((width - side) / 2, dialTop)

            val uprightCentre = height - bottomPadding - readingsPlaceable.height / 2f
            val barTop = height + FloatingBarMetrics.ContentGap.roundToPx()
            val sidewaysCentre = (dialTop + side + barTop) / 2f
            val centre = uprightCentre + (sidewaysCentre - uprightCentre) * slide

            readingsPlaceable.placeWithLayer(
                x = (width - readingsPlaceable.width) / 2,
                y = (centre - readingsPlaceable.height / 2f).roundToInt(),
            ) { rotationZ = turn.angle }
        }
    }
}

@SuppressLint("MissingPermission") // guarded by hasLocationPermission
private fun handleLocationRequest(
    context: Context,
    androidLocationManager: AndroidLocationManager
) {
    if (hasLocationPermission(context)) {
        androidLocationManager.registerLocationListener()

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager

        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            locationRequestDialog(
                title = R.string.location_disabled,
                message = R.string.location_disabled_rationale,
                actionIntent = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                context = context
            )
        }
    } else {
        // No location permission, show AlertDialog
        locationRequestDialog(
            title = R.string.permission_required,
            message = R.string.permission_rationale,
            actionIntent = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            context = context
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun RegisterListener(
    lifecycleOwner: LifecycleOwner,
    androidLocationManager: AndroidLocationManager,
    sensorViewModel: SensorViewModel,
    settingsUiState: SettingsViewModel.SettingsUiState,
    listener: AndroidSensorEventListener,
    mainViewModel: MainViewModel,
    mStrength: (Float) -> Unit,
) {
    val context = LocalContext.current
    val trueNorthState = settingsUiState.isTrueNorthEnabled

    LaunchedEffect(trueNorthState) {
        sensorViewModel.setTrueNorthState(trueNorthState)
    }

    val location by sensorViewModel.location.collectAsStateWithLifecycle()
    LaunchedEffect(trueNorthState, location) {
        if (trueNorthState && location == null && hasLocationPermission(context)) {
            androidLocationManager.registerLocationListener()
        }
    }

    DisposableEffect(listener, lifecycleOwner) {
        val azimuthListener = object : AndroidSensorEventListener.AzimuthValueListener {
            override fun onAzimuthValueChange(degree: Azimuth) {
                mainViewModel.updateAzimuth(degree)
            }

            override fun onMagneticStrengthChange(strengthInUt: Float) = mStrength(strengthInUt)
        }
        listener.setAzimuthListener(azimuthListener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    listener.registerSensor()
                    // refresh the position for the coordinates readout
                    if (hasLocationPermission(context)) androidLocationManager.registerLocationListener()
                }
                Lifecycle.Event.ON_PAUSE -> listener.unregisterSensorListener()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.unregisterSensorListener()
        }
    }
}

private fun locationRequestDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    actionIntent: String,
    context: Context
) {
    AlertDialog.Builder(context)
        .setTitle(title)
        .setIcon(R.drawable.ic_error)
        .setMessage(context.getString(message))
        .setPositiveButton(R.string.settings) { _, _ ->
            val intent = if (actionIntent == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                Intent(actionIntent, Uri.fromParts("package", context.packageName, null))
            } else {
                Intent(actionIntent)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        .setNegativeButton(R.string.ok_button) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

@Composable
fun ShowAccuracyAlertDialog(context: Context, accuracy: Int, onDismiss: () -> Unit) {
    val accuracyString = when (accuracy) {
        SensorManager.SENSOR_STATUS_UNRELIABLE -> context.getString(R.string.accuracy_unreliable)
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> context.getString(R.string.accuracy_low)
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> context.getString(R.string.accuracy_medium)
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> context.getString(R.string.accuracy_high)
        else -> context.getString(R.string.accuracy_unknown)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.calibration_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calibration_figure_eight),
                    contentDescription = stringResource(R.string.figure_8_pattern),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(context.getString(R.string.calibration_required_message, accuracyString))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok_button))
            }
        })
}
fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

// Ask for location once per app launch, not every time the tab is opened.
private var askedForLocationThisSession = false
