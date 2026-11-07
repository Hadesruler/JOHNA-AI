# JOHNA AI 1.0.14 — Living Command Center

Based **directly on the user-supplied JOHNA 1.0.13 source archive**, preserving the existing Kotlin/Jetpack Compose dashboard, weather, app launcher, smart-home discovery, and voice route.

## Implemented in source

- Energy-core orb: no latitude/longitude grid; luminous core, two tilted 3D-looking hollow orbital rings with moving light nodes. Idle rotation and distinct speaking/listening pulse, Android SpeechRecognizer RMS-driven visual energy while listening. This is a native 2D Canvas *projection* of a volumetric object, not a real 3D OpenGL/Filament mesh.
- Live weather scene: full-size weather panel with Open-Meteo hourly/7-day forecasts and an animated day/night sky with clouds, rain, snow or stars depending on the weather code, sunrise and sunset; does not invent values.
- Media volume via local voice parser: `volume up 2`, `volume down 2`, `βάλε ένταση στο 7`, `τέρμα τη φωνή`, `σίγαση`; 0–10 UI scale mapped to Android STREAM_MUSIC actual steps. No Groq request. Devices with unusual hardware step counts round to nearest.
- `άνοιξε το mail` or `άνοιξε το Gmail`: launch the official Gmail app. Gmail inbox is NOT accessible inside the app without separate OAuth and consent.
- Short UI sound effects (SoundPool): open, close, volume up, volume down, with an effects on/off switch.
- Directional panel transitions and lightly animated bottom dock, sound feedback on panel open/close, and optional ambient living background.
- Foreground experimental wake phrase STT resumes after local non-speaking commands. Not true, continuous, offline, always-on wake detection: Android's SpeechRecognizer service may stop, time out or require Internet.
- Existing authorized Smart Home controller launching and network discovery retained. No unauthorised device commands or arbitrary APK installation.

## Verified without Android SDK

- 14 **executed** Kotlin pure-parser smoke scenarios (`LocalVolumeParser.kt`), including rejecting unrelated commands. Kotlin JVM compiler tested.
- 10 Kotlin source files: balanced delimiters/comments/string checks only (not a full Kotlin type check).
- AndroidManifest.xml parsed: INTERNET, RECORD_AUDIO, ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE; no GET_ACCOUNTS / QUERY_ALL_PACKAGES, unencrypted traffic disabled.
- Four WAV files: valid 22050 Hz mono PCM, low-volume assets.
- No signing private keys in the source ZIP.

## NOT yet verified

This container has no Android SDK / Gradle installation, and cannot resolve `dl.google.com`. **No Android APK was compiled, no real-device test and no Play Protect certification** were performed here. A GitHub Actions build will type-check the Compose code. The workflow requires repository secrets `JOHNA_SIGNING_KEYSTORE_B64` and `JOHNA_SIGNING_PASSWORD`; it intentionally fails when they're missing instead of silently changing the signing certificate. This APK cannot update an already-installed app with a different signature.

After build, test with a functioning installed app: local volume commands, panel swipe/close, animated weather (day/night), sound toggle, foreground wake, voice/Groq response, and Play Protect. Keep the previously working installation until verification.
