# JOHNA AI v0.2

Android Kotlin/Jetpack Compose prototype for Redmi Pad 2.

## Working features
- Google SpeechRecognizer in Greek with partial transcription.
- Greek Android text-to-speech.
- Animated purple orb and dashboard with explicit integration status.
- Conversation history in current session.
- Optional automatic re-listening after TTS finishes, while app remains in foreground; error retry capped at 3.
- Manual microphone and stop controls.

## Not yet implemented
- Local LLM (Qwen), native inference library and GGUF model installation.
- Local wake word ("Hey Johna") and background foreground service.
- TV, Bambu A1, smart home and web integrations.

Google speech recognition may require internet and does not incur an OpenAI API charge. Continuous listening in the foreground is not equivalent to an always-on wake word and may capture TV audio. Test with headphones or low TV volume. History is not saved across app restarts.
