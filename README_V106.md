# JOHNA AI v1.0.6 — Fast Commands & Globe Orb

- Orb rotates slowly around its vertical axis (24 s cycle) with subtle breathing.
- Live speech transcript hidden, speech recognition remains enabled.
- Greek app-panel commands handled locally without Groq.
- Groq model order: gpt-oss-20b, qwen3-32b, gpt-oss-120b.
- Short responses, lower token budget, 2.5 s connect and 6 s read timeout per attempt.
- Existing encrypted Groq key remains stored on device when upgrading.

**Build and runtime tests have not been performed.** Note: rotating a flat orb image in 3D gives a globe-like spin but is not a textured 3D sphere.
