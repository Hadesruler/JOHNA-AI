# JOHNA AI v1.0.10 SAFE — SOURCE ONLY

## Purpose
Safety-focused diagnostic build responding to the *Google Play Protect* hard block on v1.0.9. This does NOT prove Play Protect will allow installation; only an actual build/install scan can establish that.

## Deliberate changes vs v1.0.9
- Removed embedded Google Sign-In and direct Gmail/Calendar API fetching, plus the Play Services Auth dependency.
- Disabled NotificationListenerService declaration and the listener code; no notification access is requested.
- Kept microphone, HTTPS Internet, Groq, weather, voice commands, glass UI, and orb.
- Mail voice command opens official Gmail; Calendar command opens official device Calendar via Android intent. Embedded Mail/Calendar views do NOT show live private data in this diagnostic build.
- Removed GET_ACCOUNTS (already removed previously). Cleartext traffic remains disabled.
- Set versionCode=110. No data read, mail sent, or calendar modified.

## Signing
The GitHub workflow expects JOHNA_SIGNING_KEYSTORE_B64 and JOHNA_SIGNING_PASSWORD, matching the agreed private certificate SHA1 92:9F:D8:F1:73:3A:A9:C4:C4:35:C3:55:9E:A7:43:60:81:9A:EF:11. Do not upload the keystore into this public repo. Without these secrets CI deliberately fails rather than silently producing yet another differently signed APK.

## Limits
No Android build or real Play Protect installation test was possible in this workspace. Do not bypass Google Play Protect. An existing installation with a different certificate cannot be updated in place by this key. If blocking persists, submit the app for Google Play Protect review and examine actual scan findings.
