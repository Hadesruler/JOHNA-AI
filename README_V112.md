# JOHNA AI v1.0.12 - Interactive Hub preview

Changes to v1.0.11:
- Open-Meteo hourly (24h) and 7-day forecast, feels-like, humidity, wind, gusts, pressure, UV, rainfall probability, sunrise/sunset.
- Interactive Compose weather panel with selectable tabs and daily cards; panel timeout restarts upon interactions.
- Device discovery panel clarifies discovery versus authenticated control and opens official smart-home controller.
- Maintains current voice router, Groq, orb and local commands.

Limits: This is a **source build**, not a verified APK. It has not been compiled or installed on the Redmi Pad 2. Devices cannot be paired or commanded solely from mDNS. Universal third-party APK embedding is not feasible under Android app sandboxing; integration requires package intents, deep links or documented device APIs and user authorization. Google OAuth for inbox/calendar is not automatically available.

Safety: Do not bypass Play Protect. Existing signing keys/secrets are not included. Stable signing requires CI secrets and cannot guarantee Play Protect clearance.

- Interactive installed-app launcher list, search, and explicit voice open by app label (only Android-visible launchers). No QUERY_ALL_PACKAGES permission.
