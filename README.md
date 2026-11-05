# JOHNA AI v0.1 — Android prototype

Prototype for Redmi Pad 2. Purple/magenta orb, tap-to-talk Greek speech recognition and Greek text-to-speech (installed Android voice permitting). It is NOT yet always-on, an LLM assistant, or a smart-home controller.

## Build APK using GitHub Actions (no PC required)
1. Create a new GitHub repository and upload the *contents* of this folder (including `.github/workflows/build-apk.yml`) to its root. GitHub mobile web file upload may hide dot-folders; use the repository's web editor or a Git client if necessary.
2. Open **Actions** > **Build JOHNA Android APK** > **Run workflow** (or push to main).
3. Wait for the build to succeed; download artifact **JOHNA_AI_v0.1_debug_APK** from the run summary.
4. Extract the artifact ZIP and install `app-debug.apk` on the Android tablet. Allow installation from that source when Android asks.

This project has not been compiled or device-tested in this environment. Check Actions build logs if it fails. Debug APK is automatically signed for testing, not production distribution.
