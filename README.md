# Compass

A free, open-source compass with a bubble level and a ruler for Android. No ads, no in-app
purchases and no tracking. Built with Kotlin and Jetpack Compose.

## Features

**Compass**
- True north or magnetic north, with the magnetic declination worked out on the device
- Coordinates, elevation and magnetic field strength
- A dial inspired by Apple's Compass, with a haptic tick at every mark
- Sensor accuracy indicator and a calibration guide

**Tools** (long-press the middle tab to switch between them)
- Bubble level for flat, horizontal and vertical surfaces, with a lock button and calibration
- Ruler in centimetres and inches, with on-screen calibration

**App**
- Light, dark and AMOLED themes
- English, Deutsch, Nederlands, Türkçe and 日本語, switchable inside the app
- Adjustable haptic strength and measurement precision
- The controls turn to stay readable when the phone is held sideways or upside down
- Android 6.0 and above

## Building

You need JDK 21 and the Android SDK (platform 37).

```
./gradlew assembleDebug
```

Run the checks with:

```
./gradlew testDebugUnitTest lintDebug
```

## Permissions

- **Location** (optional): used only for true north, coordinates and elevation. It never leaves
  your phone.
- **Vibrate**: haptic feedback.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Compass is free software, licensed under the GNU General Public License v3.0 or later. See
[LICENSE](LICENSE).

It is built on top of other open-source projects. See [CREDITS.md](CREDITS.md).
