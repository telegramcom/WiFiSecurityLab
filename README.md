# WiFiSecurityLab~

## Overview

WiFiSecurityLab~ is an authorized, educational Wi-Fi security research tool for controlled laboratory environments. It is designed for cybersecurity training and security-awareness demonstrations using two Android devices.

The app creates a local laboratory hotspot, hosts an educational portal, records safe demonstration events, and presents a warning flow that explains how real phishing and rogue-network risks work without collecting real credentials.

## Safety & Ethics

- **This application is for AUTHORIZED SECURITY RESEARCH ONLY.**
- Never use it on public networks or without explicit consent from all participants.
- All credentials are synthetic demonstration values (`test_user` / `training_password`) only.
- Passwords are always redacted as `[REDACTED — SIMULATION]`.
- The app performs no real credential harvesting, man-in-the-middle activity, or traffic interception.
- Use only in a controlled lab that you own or have explicit written permission to assess.

## Lab Setup

- **Phone A (Research Controller):** Creates the `LAB-WIFI` hotspot and runs the dashboard.
- **Phone B (Test Client):** Connects to `LAB-WIFI` and accesses the educational portal.

Keep both devices under the control of the authorized research team. Disconnect the hotspot and stop the local server when the exercise is complete.

## Features

- Local-only hotspot creation (Android 8.0+)
- Embedded educational captive portal
- Synthetic credential submission with redaction
- Live event dashboard
- Rogue Wi-Fi detection scanner
- Security-awareness warning flow

## Build Instructions

1. Clone this repository.
2. Open it in Android Studio Hedgehog or newer.
3. Sync the Gradle files.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Install the debug APK on an Android 8.0+ (API 26+) device.

From a terminal with Java 17 or newer:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The GitHub Actions workflow performs the same build on every push and pull request targeting `main`.

## Permissions Required

- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET` (local server only)
- `FOREGROUND_SERVICE`

Android may also request additional system permissions depending on device version and hotspot implementation.

## Technical Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- Coroutines / Flow
- Android Wi-Fi APIs
- Local HTTP server on port `8080`

## License

MIT License — see the [LICENSE](LICENSE) file.

## Disclaimer

This tool is provided for educational purposes only. The authors assume no liability for misuse. Always comply with local laws, organizational rules, and institutional policies. Do not use this project to access, monitor, or interfere with networks or accounts without explicit authorization.