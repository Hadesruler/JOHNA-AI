# JOHNA AI v1.0.15 — Ethereal presence and foreground wake fixes

Based on the full v1.0.14.1 source project, preserving existing weather, apps, sound, volume and LAN functions.

## Visual changes
- Replaces the globe-like center with a soft, organic, luminous energy presence. Canvas-generated animated wisps, glow and particles; **not true 3D geometry**.
- Replaces heavy opaque tile dock with a 62 dp translucent floating navigation capsule.
- Uses calm atmospheric gradients and floating particles in place of the static busy background image.
- Reduces strong borders and keeps animated panel transitions and live weather.

## Voice / wake fixes
- Adds recognition of `Hey Johna`, `Τζόνα`, plus common Greek phonetic variants.
- Handles Android SpeechRecognizer error 11 (`ERROR_SERVER_DISCONNECTED`) with bounded delayed reconnect, rather than immediately switching off wake.
- Resumes foreground wake after returning to the app; stops listening when app goes to background.
- Wake remains **experimental foreground speech recognition**, not a guaranteed on-device always-on hotword engine.

## Validation
- Source static checks, ZIP integrity, XML parsing and command parser JVM checks if available.
- Full Gradle Android compilation and Play Protect evaluation must happen in GitHub Actions / tablet.
- This package does not include signing certificates or credentials. CI still expects the existing signing secrets.

No email is sent, no permissions are newly requested, no smart-home control issued.
