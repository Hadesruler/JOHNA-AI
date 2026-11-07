# JOHNA AI v1.0.16 — Holographic Command Centre

**Full Android Studio/Gradle Kotlin/Jetpack Compose source**, based directly on JOHNA v1.0.15. No partial legacy patch.

## New interface

- Native responsive three-column landscape dashboard influenced by the target reference image.
- No bottom navigation dock. Left: Open-Meteo weather with animated sky, calendar, apps. Centre: large custom energy orb, listening/speaking visualization, natural-language status, action chips. Right: real notification state and LAN smart-home discovery.
- Material icons throughout, including real installed app icons via Android PackageManager in the Apps panel.
- Dark navy/glass surfaces, cyan/violet outlines, animated stars/aurora, mountain silhouette, breathing hologram platform and revolving *hollow* orbit ring. Orb is vector Canvas animation, **not actual volumetric 3D geometry**.
- Main panels retain sliding transitions and sound effects. Android Back closes an expanded panel or settings instead of exiting the app.
- Settings retain sound toggle and animation toggle; buttons are connected to actual Android handlers.
- Android Apps Hub filters the actual launcher apps and launches them via PackageManager. Each external app runs in its own task/UI; JOHNA does not embed/force-stop arbitrary APKs.

## Voice and limitations

- Greek / English wake phrase parser still recognizes `Johna`, `Hey Johna`, `Τζόνα`, etc.
- Uses Android's **on-device speech recognizer where the OS exposes one** (Android 12+), else the platform recognizer.
- Android recognizer error 11 now trips a circuit breaker after three disconnects within one minute, preventing an endless restart loop. The manual `Μίλα` button remains available.
- This **does NOT implement persistent low-power Gemini-style hotword recognition**. That needs a dedicated on-device keyword spotting engine plus a compatible model and battery/microphone lifecycle work. A wake phrase is recognized only while Android SpeechRecognizer is actually listening in the foreground.
- Commands `κλείσε Gmail`, `κλείσε Chrome`, `κλείσε εφαρμογή` return the *JOHNA dashboard* when JOHNA is active. Android does not permit standard third-party apps to force-stop other apps. While Gmail/Chrome is on screen JOHNA does not record background voice; use Android Back to return.
- `Volume +2/-2`, `άνοιξε Gmail`, `άνοιξε καιρό`, etc. retain the on-device command router; ordinary questions use configured Groq.

## Connections

- Devices panel uses NSD LAN discovery. A discovered service is **not** an authorized connected device.
- Smart-home toggles are intentionally NOT fake controls: physical lights, cameras, thermostats require a controller + consent (Matter/Home Assistant/Google Home APIs).
- Notifications and Google Calendar are displayed **only** if their underlying permissions/integrations supply real data. The browser preview has explicitly marked fictitious demo data.

## Desktop preview

Open `preview/index.html` in a browser. All front-end assets are local CSS/HTML; interactive demo panel transitions and audio-state illustrations are only visual preview. `docs/johna_target_reference.png` is the design reference image, not a substitute for functional Android UI.

## Validation and build

- 14 pure Kotlin parser checks passed under `kotlinc` / JDK 21 (`tests/RouterSmoke.kt`).
- Desktop HTML preview: 5 interactive browser checks passed (panels, voice-state UI, no JS errors).
- Android XML and source/ZIP integrity are checked.
- **Not compiled as an Android APK in this environment**: Android SDK/Gradle distribution not available. Build with existing GitHub Actions workflow and inspect compiler output before installing on Redmi Pad 2.
- CI workflow retains the stable signing secrets and SHA-1 verification from v1.0.15. Do not commit keystore, Groq key or tokens.

`versionCode 117` / `versionName 1.0.16`.
