# Credits

Compass is built on top of two open-source projects. Both are licensed under the GNU General
Public License v3.0 or later, so Compass as a whole is distributed under GPL-3.0-or-later as
well. See [LICENSE](LICENSE).

## MBCompass

- Source: https://github.com/CompassMB/MBCompass
- Author: Mubarak Basha and contributors
- License: GPL-3.0-or-later
- Used for: the base of the app, including sensor and location handling, settings, theming and
  navigation.
- Artwork: the figure-eight calibration icon (`ic_calibration_figure_eight.xml`) is by Mubarak
  Basha and licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

## Level

- Source: https://github.com/woheller69/Level
- Author: woheller69, based on https://github.com/avianey/Level by Antoine Vianey
  (Copyright (C) 2014)
- License: GPL-3.0-or-later
- Used for: the bubble level (flat, horizontal and vertical modes), calibration and angle logic,
  ported from Java to Kotlin and Jetpack Compose in `features/tools/level/`.
- The ruler comes from Privacy Friendly Ruler
  (https://github.com/SecUSo/privacy-friendly-ruler, SecUSo group, GPL-3.0), ported to Compose
  in `features/tools/ruler/`.

## Notice about source files

The copyright notices of these projects are kept here and in the app's repository links instead
of in every source file. If you reuse code from Compass, keep this file with it.

## Design references

The settings screen and the bottom menu are styled after Samsung's One UI. Measurements such as
the switch size and the floating bar's height and colours were taken from the open-source One UI
libraries. No code was copied from them:

- https://github.com/tribalfs/sesl-androidx
- https://github.com/Revilo-Dev/oneui-design-9.0
