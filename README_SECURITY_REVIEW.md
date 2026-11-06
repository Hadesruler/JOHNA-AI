# JOHNA AI v1.0.9 — reduced Android permissions, assessment build

Only two changes relative to the last provided v1.0.9 stable-signing source:

1. Removed AndroidManifest.xml GET_ACCOUNTS. Google Sign-In via Play services remains in source. **GoogleAuthUtil.getToken behavior must be retested on the tablet; removing this permission may affect some devices.**
2. Set usesCleartextTraffic=false; reviewed app endpoints use HTTPS. Test all network flows.

Preserved NotificationListenerService because it is an opt-in feature and deleting it would break notifications. This is not proof that Play Protect will allow installation. The Play Protect warning states a risk involving sensitive data; Google does not disclose its exact detection logic. APK signing mismatch and Play Protect block are different failure paths.

The CI uses two GitHub secrets JOHNA_SIGNING_KEYSTORE_B64 and JOHNA_SIGNING_PASSWORD; it verifies SHA-1 92:9F:D8:F1:73:3A:A9:C4:C4:35:C3:55:9E:A7:43:60:81:9A:EF:11 and fails if missing. Do not put the keystore in a public repository.

IMPORTANT: Earlier installed app may use a different certificate. This will not upgrade it in place, regardless of the Play Protect outcome. Do not uninstall before backing up settings/data. No APK build or on-device Play Protect test was performed here.

Before distribution, inspect the Google account's independent critical security alert in the official Google Account security page, review authorized access, and verify the APK's code and permissions. Do not bypass or disable Play Protect.
