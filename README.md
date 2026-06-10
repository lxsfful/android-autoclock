# android-autoclock

Android/Kotlin AccessibilityService automation sample and personal tool.

This project demonstrates a user-configured, time-window-based automation flow:

1. Wake the screen and return to the launcher.
2. Tap a user-selected shortcut coordinate for a target app.
3. Wait for a configurable delay.
4. Tap a user-selected action coordinate.
5. Watch the configured target app for a generic success message.
6. Return to the launcher and optionally tap a third user-selected coordinate.

## Intended use

This repository is published as an Android AccessibilityService automation sample for personal, permitted automation and learning. It is not positioned as, and should not be used as, a workplace policy bypass, attendance fraud tool, or automation against services where automation is prohibited. Always comply with applicable laws, policies, app terms, and workplace rules.

## Configuration placeholders

Before using the sample, replace the placeholder target package and success text with values appropriate for your own permitted use case:

- `app/src/main/java/com/autoclock/ClockSuccessDetector.kt`
- `app/src/main/res/xml/accessibility_service_config.xml`

The default placeholder package is `com.example.targetapp`.

## Build

Install Android Studio or the Android SDK, create your own `local.properties` with `sdk.dir`, then run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Privacy and security

- The sample stores coordinates, time windows, and optional email settings locally on the device.
- SMTP credentials entered in the app are stored with Android Keystore-backed encryption where available.
- No real credentials, local SDK paths, device IDs, or private handoff notes are included in this repository.

## License

MIT. See `LICENSE`.
