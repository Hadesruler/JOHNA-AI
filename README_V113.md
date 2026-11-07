# JOHNA AI v1.0.13 — Living Dashboard (source preview)

Changes made on top of v1.0.12 source:

- AmbientLivingBackdrop.kt: subtle, animated vector light field, drifting points, dim orbit rings, gentle light breathing. Runs behind dashboard UI without additional permissions, network requests, or assets.
- Voice-reactive ambience: backdrop brightens while speaking and the sphere displays outward speech-energy ripples; listening increases intensity of particle lights.
- Settings: saved "Ζωντανό φόντο" switch to turn ambient animation off on devices where power usage matters.
- Settings dialog now uses dark navy surface to match the HUD.
- Header updated to v1.0.13 and Android versionCode 113.

This is *not* an Android system live wallpaper, not a true real-time audio amplitude visualizer, and not a compiled or on-device-tested APK. Speaking/listening visuals currently follow the app's existing TTS/STT state. Always-on wake word remains experimental from v1.0.12. Smart home control still requires approved third-party APIs and pairing.

Build through existing Android CI only after correctly configuring signing secrets; do not bypass Play Protect. The source ZIP excludes private signing keys and API keys.
