# Security policy

Night Watch processes notifications only on the user's Android device. It does
not request Internet, storage, contacts, SMS, microphone, camera, accessibility,
device administrator or package-install permissions.

The release signing key must never be committed to this repository. Keep its
encrypted recovery package private. Only APKs signed by that same key can update
an installed release without uninstalling it.

Before installing a build, verify the published SHA-256 digest and confirm the
APK signature. Treat any APK with a different digest or signing certificate as
untrusted.
