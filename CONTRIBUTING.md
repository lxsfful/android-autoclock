# Contributing

Thanks for helping improve android-autoclock. This project is an Android/Kotlin AccessibilityService automation sample for authorized personal automation and learning.

## Ground rules

- Keep the project generic and safe for public release.
- Do not add private paths, personal emails, device IDs, screenshots with sensitive content, service-specific app names, or credentials.
- Do not frame contributions around policy evasion, misrepresentation, or prohibited automation.
- Preserve the MIT license, AccessibilityService disclosure, target-package scoping, credential-storage protections, and `FLAG_SECURE` behavior unless a security fix requires a documented change.

## Development setup

```bash
./setup.sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Requirements:

- JDK 17+
- Android Studio or Android SDK command-line tools
- Android SDK Platform 34
- `local.properties` with your local `sdk.dir`

## Branch and pull request workflow

1. Open an issue for larger changes before implementation.
2. Create a focused branch, for example `docs/readme-release-checklist`.
3. Keep pull requests small and explain user-visible behavior changes.
4. Include test results in the PR description.
5. Update README, `CLAUDE.md`, and disclosure text when behavior or permissions change.

## Code style

- Use official Kotlin style (`kotlin.code.style=official`).
- Prefer `val` over `var` where practical.
- Keep functions focused and handle errors explicitly.
- Validate user input at UI and system boundaries.
- Avoid logging sensitive text, credentials, package names from private deployments, or email addresses.

## Testing

Run at minimum:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Add or update unit tests for scheduling, parsing, coordinate mapping, success detection, history serialization, and email content when those areas change.

## Accessibility and privacy review

Before submitting changes that affect automation, permissions, storage, or networking, verify:

- Accessibility events remain scoped to the configured target package.
- Window text is not stored or logged unnecessarily.
- SMTP credentials are not committed and remain protected by platform secure storage.
- UI disclosure text accurately describes what the app can do.
- `FLAG_SECURE` remains enabled on screens that show sensitive configuration or history.

## Using Claude Code

This repository includes `CLAUDE.md` for Claude Code. Start Claude Code from the repository root and ask it to inspect existing files before editing.

```bash
claude
```

## Reporting issues

Use the GitHub issue templates for bugs and feature requests. For suspected vulnerabilities or privacy issues, use `SECURITY.md` instead of opening a public issue.
