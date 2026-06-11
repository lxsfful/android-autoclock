# Security Policy

## Supported versions

This project is a sample application. Security fixes are accepted for the current `main` branch unless maintainers publish a versioned release policy later.

## Reporting a vulnerability

Please do not open a public issue for vulnerabilities, privacy leaks, or unsafe automation behavior.

If GitHub private vulnerability reporting is enabled for this repository, use it. Otherwise, contact the maintainers through the repository owner's preferred private channel and include:

- affected commit or release;
- clear reproduction steps;
- expected and actual behavior;
- impact assessment;
- screenshots or logs only after removing secrets, private package names, account details, and device identifiers.

## Security expectations

- No committed credentials, API keys, SMTP app passwords, local SDK paths, or device identifiers.
- Accessibility events and window-content reads should stay scoped to the configured target package.
- Sensitive UI screens should keep `FLAG_SECURE` unless a documented privacy review approves removal.
- Optional email notification settings should remain local to the device and protected by Android Keystore-backed encryption where available.
- Network behavior should be limited to user-configured SMTP notification delivery.

## Disclosure timeline

Maintainers should acknowledge private reports within 7 days when possible, confirm severity, prepare a fix, and coordinate public disclosure after affected users have a reasonable update path.
