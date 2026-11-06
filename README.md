# JOHNA AI v0.4 — Futuristic Dashboard

Neon magenta/cyan Compose dashboard with animated central audio waveform orb (no avatar), responsive tablet layout, live Open-Meteo Athens weather, Android notification access, agent status panels, device placeholders, calendar, conversation history, and existing Google Greek speech/TTS.

## Limitations
This is an Android source project, not a prebuilt APK. Not compiled/tested in this environment. AI agent tiles are informational, not connected autonomous agents. Local LLM and 'Hey Johna' wake-word detection are not implemented. Device connection tiles are placeholders. The calendar is a simple day grid, not linked to Google Calendar.

## Build
Push project to GitHub and use `.github/workflows/build-apk.yml` to build debug APK; check Actions logs and artifacts.

JOHNA v0.5: Gemini API settings in-app, Android Keystore encrypted API key, hidden local conversation memory. No API key is bundled.

JOHNA v0.5.3: Gemini 3.8 Flash with retries and fallback to 3.1 Flash Lite and 3.5 Flash Lite.
