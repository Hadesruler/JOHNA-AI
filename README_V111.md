# JOHNA AI v1.0.11 — Orb + foreground wake + Smart Home handoff

This is **SOURCE ONLY**, not an APK. It has NOT been compiled on Android or cleared by Play Protect.

Changes:
- True projected rotating sphere plus luminous tilted hollow orbital ring, not a spinning 2D image.
- Experimental **foreground-only** phrase activation `Johna`/`Τζόνα` while the Wake button is enabled. Uses system SpeechRecognizer (NOT an offline always-on wake word model). May stop on Android/HyperOS speech-service limits, and requires microphone permission. Does not listen in background.
- Reduced Groq timeouts (2.5 s connect, 7 s read each) and two models. Network/streaming may still exceed target latency.
- Smart Home button/voice route opens official Google Home or Home Assistant app. Discovery of local mDNS services remains. No direct power/lock automation has been enabled.
- No Google Account / Notification Listener permissions.

Build environment was unavailable in the authoring container; use GitHub Actions after configuring signing secrets. Keep Play Protect ON and avoid installing if it flags the app.

To support real always-on low-power wake and actual smart device control, next phase needs a dedicated local wake-word runtime and an authorized Home Assistant/Matter integration plus on-device reliability testing.
