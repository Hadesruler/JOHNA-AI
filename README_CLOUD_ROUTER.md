# JOHNA v1.0 Cloud AI Router

Cloud fallback order: Groq (`llama-3.3-70b-versatile`) -> OpenRouter (`openrouter/free`) -> Cerebras (`llama3.1-8b`). Providers are skipped if no API key is configured. All providers have usage limits and model availability may change.

Open the AI settings in the app and enter your keys. The credentials are stored using Android Keystore AES-GCM, not committed to the repository. The new version replaces the previous Termux Qwen backend. Do not commit API keys.

**Important:** This source update has not been compiled or tested on an Android device. Check model availability and provider error responses in GitHub Actions and on device. The current settings screen replaces all keys when saving; enter every key you want to keep.
