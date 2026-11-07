# JOHNA AI v1.0.14.1 — Kotlin JVM compilation fix

This archive is the complete v1.0.14 source project, updated with a narrowly scoped fix for three `:app:compileDebugKotlin` errors.

- `volumeLevel` generates the JVM getter `getVolumeLevel()`, so the manually declared method was renamed to `readCurrentMediaVolumeLevel()`.
- `wakeEnabled` generates the JVM setter `setWakeEnabled(Boolean)`, so the manually declared method was renamed to `configureWakeListening(Boolean)`.
- The synthetic `access$setWakeEnabled` clash disappears with that rename.
- All five media volume references and both wake-control references in MainActivity were updated.
- New build identifier: versionCode 115; versionName 1.0.14.1.

All existing Kotlin/Compose functionality and resources were preserved. No signing keystores or secrets are contained here.

Checks passed: changed-symbol counts, source references, unchanged files, archive CRC. Full Android SDK compilation and on-device install **not run** in this environment.

The workflow requires `JOHNA_SIGNING_KEYSTORE_B64` and `JOHNA_SIGNING_PASSWORD` as GitHub Actions secrets, with the existing stable signing certificate. Do not put these secrets in the repository.

Upload all extracted project contents to the existing repository with the same paths, commit, then check GitHub Actions. Do not upload the ZIP itself as a GitHub source file.
