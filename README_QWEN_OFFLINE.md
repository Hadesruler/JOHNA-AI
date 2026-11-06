# JOHNA v0.9 — Qwen3 1.7B offline via Termux

This is an Android client integration, NOT an embedded model. The Qwen GGUF weights are NOT bundled. Run a llama.cpp `llama-server` in Termux listening on 127.0.0.1:8080 with an appropriate Qwen3-1.7B Q4 GGUF file.

Example after building llama.cpp and downloading a verified Qwen3-1.7B GGUF:

```sh
./build/bin/llama-server -m /path/to/qwen3-1.7b-q4.gguf --host 127.0.0.1 --port 8080 -c 2048 -t 4
```

The app calls the OpenAI-compatible local endpoint `/v1/chat/completions` with no cloud API key. Android's INTERNET permission is used for loopback traffic; `usesCleartextTraffic=true` enables HTTP to localhost (and globally, so narrow this for production).

Speech recognition currently uses Android SpeechRecognizer and may require network depending on the installed speech engine. Install/download offline Greek speech recognition if supported; Android TTS likewise needs a locally installed Greek voice. No APK build or device performance test has been completed.
