# JOHNA AI v1.0.9 — Complete source with stable signing

This bundle contains the existing v1.0.9 project and the stable signing changes. No signing keys or passwords are included.

Build requires GitHub Actions repository secrets `JOHNA_SIGNING_KEYSTORE_B64` and `JOHNA_SIGNING_PASSWORD`, corresponding to the separately held private signing kit. Never commit the private kit, .p12 or passwords to GitHub.

The signing certificate SHA-1 expected by the workflow is 92:9F:D8:F1:73:3A:A9:C4:C4:35:C3:55:9E:A7:43:60:81:9A:EF:11.

The workflow has not been executed or verified on a Redmi Pad 2. A different preinstalled certificate prevents a regular in-place update.
