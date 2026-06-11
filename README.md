# android-autoclock

Android/Kotlin sample app for authorized, user-configured AccessibilityService automation. It schedules a small gesture sequence, detects a configurable success message in a target app, records local history, and can send optional email notifications.

## Ethical and authorized use

Use this project only on devices, accounts, and apps where you have clear permission to automate. Do not use it to evade rules, misrepresent user actions, bypass controls, or automate any service whose terms or policies prohibit automation. You are responsible for complying with applicable laws, app terms, organization policies, and Android AccessibilityService disclosure requirements.

## Features

- Kotlin Android app using Gradle and AndroidX.
- AccessibilityService-driven gesture automation with user-selected screen coordinates.
- Exact-alarm scheduling for two configurable daily time windows.
- Target-app success detection using a placeholder package name and success text.
- Local run history stored in app-private preferences.
- Optional SMTP email notifications for success, failure, and test messages.
- Android Keystore-backed encryption for stored SMTP app passwords where available.
- `FLAG_SECURE` on the main configuration screen to reduce screenshot/screen-recording exposure.
- Built-in usage guide with step-by-step instructions.
- 2-second delay after success before killing target app, preventing residual popups.
- Fresh, elegant blue-green UI theme.

## Quick start

```bash
git clone https://github.com/lxsfful/android-autoclock.git
cd android-autoclock
./setup.sh
```

`setup.sh` validates Java and creates `local.properties` from `ANDROID_HOME`/`ANDROID_SDK_ROOT` when available. If it creates a placeholder instead, edit `local.properties` before running Gradle:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If you do not use `setup.sh`, install the prerequisites below and create `local.properties` yourself.

## Prerequisites

- Android Studio or Android SDK command-line tools.
- JDK 17 or newer, with `JAVA_HOME` set.
- Android SDK Platform 34.
- A local Android SDK path in `local.properties`:

```properties
sdk.dir=C:/Users/yourname/AppData/Local/Android/Sdk
```

## Configuration placeholders

This repository intentionally ships with generic placeholders. Before building for your own permitted use case, review and replace:

| Placeholder | Location | Purpose |
| --- | --- | --- |
| `com.example.targetapp` | `app/src/main/java/com/autoclock/ClockSuccessDetector.kt` | Target package checked before reading window text. |
| `com.example.targetapp` | `app/src/main/res/xml/accessibility_service_config.xml` | Package filter for accessibility events. |
| `操作成功` | `app/src/main/java/com/autoclock/ClockSuccessDetector.kt` | Success text detected in the target app. |
| SMTP host, port, sender, recipient, app password | In-app email settings; `.env.example` documents placeholder values only | Optional email notifications. |

Do not commit real credentials, private package names, local SDK paths, device identifiers, screenshots with sensitive content, or app-specific private notes.

## Build and test

```bash
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew assembleDebug          # debug APK (auto-signed)
./gradlew assembleRelease        # release APK (unsigned)
./gradlew lintDebug              # Android lint
./gradlew clean                  # remove build outputs
```

## Architecture overview

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt                 # configuration UI, permissions, history, email test
├── AutoClockService.kt             # AccessibilityService gesture sequence and target text reads
├── AlarmScheduler.kt               # exact-alarm scheduling for configured windows
├── AlarmReceiver.kt / BootReceiver.kt # alarm triggers and boot rescheduling
├── CoordinatePickerActivity.kt     # full-screen coordinate capture helper
├── UsageGuideActivity.kt           # built-in usage guide with step-by-step instructions
├── Prefs.kt                        # app-private settings and encrypted SMTP password storage
├── ClockSuccessDetector.kt         # target package and success-text matching
├── ClockHistory.kt / ClockRecord.kt # local history serialization
├── EmailSender.kt / ClockEmailContent.kt # optional SMTP notifications
├── TimeFormatter.kt                # time formatting utilities
├── CoordinateMapper.kt             # coordinate ratio mapping
└── ClockAccessibilitySnapshot.kt   # accessibility event snapshot model
```

Data flow: `MainActivity` saves user configuration into `Prefs`; `AlarmScheduler` schedules `AlarmReceiver`; `AlarmReceiver` calls the active `AutoClockService`; the service performs the configured gestures, checks the target window through `ClockSuccessDetector`, records the result, waits 2 seconds, then kills the target app to prevent residual popups, and optionally asks `EmailSender` to notify the configured recipient.

## Accessibility and privacy disclosure

This app uses Android AccessibilityService capabilities. When the user explicitly enables the service, it can:

- perform configured screen gestures, including taps and the global Home action;
- read text and content descriptions from the configured target package to detect the configured success message;
- run scheduled automation flows while the device is awake;
- use `WAKE_LOCK` and exact alarms to trigger the configured sequence;
- force-stop the target app after a successful operation to prevent popup residue.

Privacy boundaries in the current implementation:

- Accessibility event filtering is limited to the configured target package.
- Active-window text is read only after the active package matches the configured target package.
- Target-app text is used transiently for success detection and is not intentionally persisted.
- Coordinates, time windows, history, and email settings are stored locally in app-private preferences.
- SMTP app passwords are migrated away from plaintext storage and saved with Android Keystore-backed AES-GCM encryption when available.
- Email notifications are sent only to user-configured SMTP and recipient settings.
- The main configuration screen sets `FLAG_SECURE`.

Users should grant AccessibilityService access only after understanding these behaviors.

## Security notes

- The app requests powerful permissions: AccessibilityService binding, exact alarms, wake lock, boot completed, and internet for optional email.
- Keep `android:packageNames` scoped to the intended target package; avoid broad accessibility access.
- Do not log or commit secrets. `.gitignore` excludes `.env`, `local.properties`, key files, and local build output.
- Review exported components before release. Only the launcher activity and required system-bound components should be exported.
- Test release builds after any ProGuard/R8 changes.
- Report vulnerabilities privately; see `SECURITY.md`.

## Release checklist

- [ ] Replace placeholder package and success text with your authorized target configuration.
- [ ] Confirm accessibility disclosure text matches actual behavior.
- [ ] Verify no private paths, credentials, device IDs, screenshots, or service-specific names are present.
- [ ] Run `./gradlew testDebugUnitTest`.
- [ ] Run `./gradlew lintDebug`.
- [ ] Build from a clean tree with `./gradlew assembleRelease`.
- [ ] Sign the release APK with your keystore.
- [ ] Review `LICENSE`, `SECURITY.md`, and issue templates.

## License

MIT. See `LICENSE`.

## Contributing

See `CONTRIBUTING.md`.
