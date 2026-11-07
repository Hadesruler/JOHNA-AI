# JOHNA AI v1.0.17 — NEBULA ORB

Complete source project based on JOHNA v1.0.16.

## Changed
- Replaced hard-edged, glossy holographic sphere with an original translucent moving nebula renderer (`JohnaSphere.kt`).
- Removed the rigid elliptical rings, sphere outline, metallic surface shading, wireframe and platform.
- Added multiple gently drifting transparent mist fields, deforming plasma wisps, light pockets and floating particles.
- The orb changes brightness while speaking/listening and responds to the microphone energy already provided by the dashboard.
- Updated versionCode to 118 and versionName to 1.0.17.

## Not changed / limitations
- Keeps the existing GUI panels, controls, weather, Gmail launcher, wake, and volume logic of v1.0.16.
- This is an animated 2D procedural nebula rendered using Compose Canvas, not a 3D GPU volume simulation.
- The earlier Android SpeechRecognizer wake implementation is not a true always-on wake-word engine and may still emit error 11.
- No Android SDK build or physical Redmi Pad 2 runtime validation was possible in this environment. Compile with `gradle --no-daemon :app:assembleDebug` using the GitHub Actions workflow. The source repository currently has no Gradle wrapper script.
