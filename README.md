# Night Watch v3.0.1

Personal Android app that listens locally for notifications from **Fiverr**
(`com.fiverr.fiverr`) and **Upwork** (`com.upwork.android.apps.main`), and
optionally **WhatsApp / WhatsApp Business** (off by default). It shows the
notification title/client name and preview, then starts a repeated alarm:
2 minutes ringing, 1 minute pause, up to 3 times. Stop cancels everything.

## Privacy

- No marketplace credentials are requested or stored.
- **No `INTERNET` permission** is declared - the app cannot send data anywhere.
- Notification content is kept in memory only while an alarm is running. The
  only things saved to disk are the on/off toggles, the chosen alarm tone and,
  while an alarm runs, the previous alarm-volume level.
- Client names and message previews are hidden while the phone is locked.
- Backup, device transfer and cleartext network traffic are disabled.
- WhatsApp watching is **opt-in**. When enabled, sender name and message
  preview of *any* WhatsApp chat are shown on the alarm screen (locally only).

## Security

- CI builds `assembleRelease`: R8-minified, `debuggable=false`, signed with a
  user-owned release key stored only as a GitHub secret. The workflow fails if
  the produced APK is debug-signed or debuggable, so a debug build can never be
  published by accident.
- GitHub Actions are pinned to commit SHAs and run with `contents: read` only.
- Internal alarm components are not exported. The notification listener is
  protected by Android's signature-level bind permission.
- Pending intents are immutable, notification text is length-limited and
  control/format characters are stripped.
- Ongoing / service / call / progress notifications are ignored, so a
  "Backing up..." or "WhatsApp Web is active" banner cannot trigger an alarm.
- Repeated notifications cannot restart an active alarm sequence; the
  de-duplication cache is bounded (128 entries, 10 min).
- No third-party libraries. APK is a few hundred KB.

## First-time setup

1. Install the APK and open **Night Watch**.
2. Allow app notifications.
3. Tap **Enable notification access** and enable this app.
4. On Android 14+, tap **Allow full-screen alarms** and enable access.
5. Exclude Fiverr, Upwork and Night Watch from battery optimization.
6. Enable auto-start for Night Watch if the phone provides that setting.
7. Run the test buttons.

If the release signing key changes, Android will refuse to update over an
existing install (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Keep the keystore safe.

## Building a release locally

```bash
export NW_KEYSTORE_FILE=/path/to/release.jks
export NW_KEYSTORE_PASSWORD=...
export NW_KEY_ALIAS=nightwatch
export NW_KEY_PASSWORD=...
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

## Changelog

### 3.0.1
- CI now produces a signed, minified, non-debuggable release APK (v3.0.0 was an
  unminified debug build signed with the throw-away Android Debug key).
- WhatsApp watching defaults to **off**.
- Ongoing / foreground-service / call / progress / status notifications no
  longer trigger the alarm.
- Custom alarm tone: falls back to the default alarm if the chosen file cannot
  be played (previously the alarm went silent). "Silent" is now honoured.
- Alarm volume is persisted before being raised and restored on next launch if
  the process was killed mid-sequence.
- Alarm screen closes automatically when the sequence finishes or Stop is
  pressed from the notification.
- Removed unused appcompat / material / constraintlayout dependencies (-5 MB).
- Workflow: actions pinned to SHAs, `permissions: contents: read`, APK
  signature and debuggable flag verified before upload.
