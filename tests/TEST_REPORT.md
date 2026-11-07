# Test report — JOHNA AI 1.0.16

## Confirmed
- Pure Kotlin parser smoke tests: 14/14 pass (JDK 21 `kotlinc` and `java`). See `RouterSmoke.kt`.
- Browser preview interaction: 5/5 checks pass (Chromium/Playwright), with no JavaScript exceptions.
- `tests/preflight.py`: 17 static assertions pass, including manifest XML parse, source feature wiring, version, on-device recognizer preference, back-navigation and volume callbacks.
- Source ZIP integrity to be checked after archive assembly.

## Not confirmed
- `:app:assembleDebug` **not run** (Android SDK/Gradle Android dependencies not installed in this container).
- No Redmi Pad 2 installation, microphone/Groq call, Android wake, smart-home, Play Protect or real device validation.
- No full Kotlin/Compose typecheck. A real GitHub Actions build may expose compile errors; report the build log if it does.

Note: The desktop preview uses **synthetic example** weather, Gmail, notification, calendar and smart-home data; real Android UI uses the app's source data and honest no-connection states.
