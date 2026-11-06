# JOHNA AI v1.0.8 — 3D Sphere + One-shot voice + App launch + Gmail preview

- Procedural shaded 3D sphere with front-hemisphere projected latitude/longitude geometry. Rotates only while TTS is speaking.
- Panels enter horizontally and automatically close after 10 seconds unless changed.
- One-shot microphone listening, no repeated continuous recognition loop. True always-on wake word is not implemented (requires separate always-on/hotword engine and Android background constraints).
- Local voice commands open Chrome and camera via Android intents.
- Mail voice command opens internal mail panel showing **recent Gmail Android notification previews only** when Notification Access is enabled. Button opens native Gmail app. No inbox API or OAuth access is claimed.
- To read Gmail inbox fully in-app, implement Google OAuth with Gmail readonly scope and an authorized Android OAuth client for the app signature; user approval required. Never request mailbox password or paste tokens into source.

Source only; needs Android compile and real-device test.
