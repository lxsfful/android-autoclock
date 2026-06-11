# android-autoclock

**Version:** 1.1 | **Platform:** Android | **Stack:** Kotlin, AndroidX, Gradle 8.4, Android Gradle Plugin 8.2.2

## What

Android/Kotlin sample app for authorized, user-configured AccessibilityService automation. It schedules gesture sequences, checks a placeholder target app for a success message, stores local history, and can send optional SMTP notifications.

## Quick Start

```bash
./setup.sh                 # First-time local checks
./gradlew testDebugUnitTest # Run unit tests
./gradlew assembleDebug    # Build debug APK
```

## Commands

```bash
# Development
./setup.sh                 # Check local prerequisites
./gradlew assembleDebug    # Build debug APK (auto-signed)
./gradlew assembleRelease  # Build release APK (unsigned)
./gradlew lintDebug        # Run Android lint
./gradlew clean            # Remove build outputs

# Testing
./gradlew testDebugUnitTest # Run JVM unit tests
```

## Architecture

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt                  # UI, permissions, settings, history, email test
├── AutoClockService.kt              # Accessibility gestures and target-window text checks
├── AlarmScheduler.kt                # Exact-alarm scheduling
├── AlarmReceiver.kt / BootReceiver.kt # Trigger sequence and reschedule after boot
├── CoordinatePickerActivity.kt      # Full-screen coordinate selection
├── UsageGuideActivity.kt            # Built-in usage guide
├── Prefs.kt                         # SharedPreferences and encrypted SMTP password
├── ClockSuccessDetector.kt          # Target package and success-text matching
├── ClockHistory.kt / ClockRecord.kt # Local history model and serialization
├── EmailSender.kt / ClockEmailContent.kt # Optional email notifications
├── TimeFormatter.kt                 # Time formatting utilities
├── CoordinateMapper.kt              # Coordinate ratio mapping
└── ClockAccessibilitySnapshot.kt    # Accessibility event snapshot model
```

`MainActivity` writes configuration to `Prefs`. `AlarmScheduler` schedules `AlarmReceiver`, which invokes the active `AutoClockService`; the service performs gestures, checks target text through `ClockSuccessDetector`, waits 2s, kills the target app to prevent popup residue, stores history, and optionally sends email.

## Key Files

```text
README.md                                                   # Public overview and release checklist
CLAUDE.md                                                   # This file
app/build.gradle                                            # Android/Kotlin dependencies and SDK levels
app/src/main/AndroidManifest.xml                            # Permissions and components
app/src/main/res/xml/accessibility_service_config.xml        # Accessibility package filter and disclosure resource
app/src/main/res/values/strings.xml                         # App and service labels/disclosure
app/src/main/res/values/colors.xml                           # Fresh blue-green color scheme
app/src/main/res/values/themes.xml                           # Material Light theme
app/src/main/java/com/autoclock/ClockSuccessDetector.kt      # Replace target package and success text here
app/src/main/java/com/autoclock/AutoClockService.kt          # AccessibilityService behavior
app/src/main/java/com/autoclock/Prefs.kt                     # Local settings and credential encryption
app/src/main/java/com/autoclock/UsageGuideActivity.kt        # Usage guide with 7-section instructions
.env.example                                                # Placeholder configuration reference only
```

## Configuration

Runtime configuration is mostly in-app. Placeholder values are documented in `.env.example` for release review.

| Variable | Required | Description |
| --- | --- | --- |
| TARGET_APP_PACKAGE | Yes | Placeholder package to copy into source/XML for an authorized target app. |
| TARGET_SUCCESS_TEXT | Yes | Placeholder success text to copy into `ClockSuccessDetector.kt`. |
| SMTP_HOST | No | Optional notification SMTP server. |
| SMTP_PORT | No | Optional notification SMTP port. |
| SENDER_EMAIL | No | Optional notification sender. |
| RECIPIENT_EMAIL | No | Optional notification recipient. |
| SMTP_APP_PASSWORD | No | Optional SMTP app password; enter in app, never commit. |

## Key Behaviors

- **Success detection**: Polls the active window for the configured success text after tapping the clock button.
- **Post-success cleanup**: After detecting success, waits 2 seconds then force-stops the target app to prevent residual popups from interfering with the next run.
- **Email notifications**: Sends optional SMTP emails on success, failure, or test.
- **Secure storage**: SMTP passwords are encrypted with Android Keystore-backed AES-GCM.

## Rules

- Public docs must avoid private paths, emails, device IDs, organization-specific app names, and misuse framing.
- Preserve MIT license, AccessibilityService disclosure, target-package scoping, and `FLAG_SECURE` behavior.
- Do not change application behavior unless required for privacy or security.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
