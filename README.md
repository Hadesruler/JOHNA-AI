# JOHNA AI v0.7 — Animated Smart Home Dashboard

Android/Jetpack Compose source project, based on v0.6.

## Added
- Four persistent color themes (Cyber Purple, Midnight Blue, Arctic White, Amber Tech), selectable from THEMES in the header.
- Animated status tiles with custom PNG assets for AI, Wi-Fi, Home and Voice; extra PNGs for lights, camera, climate and power.
- Animated neon borders on cards, live orb and existing voice controls.
- Existing local network mDNS discovery panel, voice conversation, encrypted API key, weather and notifications preserved.
- Gemini Flash Lite preferred; fallback retained.

## Important limitations
- No APK build was run here. GitHub Actions must compile and test.
- Discovery is not pairing or controlling devices. Vendor-specific adapters are needed.
- Some themes retain light text on dark widget cards; full per-widget theme polish is future work.
- No API key is embedded. Keep your key in the app's encrypted storage; do not uninstall/clear app data if you want to retain it.
