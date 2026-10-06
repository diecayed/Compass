# Contributing to Compass

Thank you for your interest in contributing. Bug fixes, new features, documentation and
translations are all welcome.

## Reporting bugs and requesting features

Open an issue and use the bug or feature template. Check first that it has not been reported
already.

## Code contributions

- Fork the repository and create a branch for your change.
- Run `./gradlew testDebugUnitTest lintDebug` before opening a pull request.
- Describe what you changed and why in the pull request.
- For larger changes, open an issue first so we can talk about it.

## Translations

Strings live in `app/src/main/res/values*/strings.xml`. A new language needs its own `values-xx`
folder, an entry in `LanguageManager`, and its code added to `localeFilters` in
`app/build.gradle.kts`.

## License

Contributions are licensed under GPL-3.0-or-later, like the rest of the project.
