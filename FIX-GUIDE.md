# How to apply the Night Watch v3.0.1 fixes

Everything in this `night-watch-fix/` folder mirrors the repo layout. Copy the
files over the originals in your local clone of
`monthchatgpt54-blip/fiverr-message-alarm`, then follow steps 2-4.

## 1. Files changed

| File | What changed |
|---|---|
| `.github/workflows/android.yml` | `assembleRelease`, keystore from secrets, actions pinned to SHAs, `permissions: contents: read`, apksigner/aapt2 gate that fails on debug-signed or debuggable APKs |
| `app/build.gradle` | `signingConfigs.release` from env vars, `shrinkResources true`, versionCode 5 / versionName 3.0.1, **all dependencies removed**, debug build gets `.debug` suffix so it can coexist with the release install |
| `app/proguard-rules.pro` | keep line numbers for readable crash logs |
| `AppPrefs.java` | WhatsApp default `false`; `SAVED_ALARM_VOLUME`; `RINGTONE_SILENT` sentinel |
| `FiverrNotificationService.java` | `isRealMessage()` filter (ongoing / foreground-service / call / progress / status / system / transport / error); `startForegroundService` always (minSdk 26) |
| `NightWatchAlarmService.java` | sound: custom -> default alarm -> default notification, never silent by accident; Silent honoured; volume persisted **before** raise and restored on next start; `SequenceListener` so AlarmActivity closes; `EXTRA_FALLBACK`; wake-lock timeout covers full sequence; dead `ACTION_STOP` branch removed |
| `AlarmActivity.java` | implements `SequenceListener`; auto-closes when service ended (except fallback path); null-safe package launch |
| `MainActivity.java` | restores stale volume on resume; ringtone picker shows Default + Silent, persists URI grant; WhatsApp test uses installed flavour; all `startActivity` wrapped; uses `isNotificationListenerAccessGranted()` |
| `AlarmActionReceiver.java` | unchanged (included for completeness) |
| `README.md` | rewritten to describe what is actually shipped; changelog |

Unchanged: `AndroidManifest.xml`, `res/**`, `settings.gradle`, `gradle.properties`, `build.gradle` (root), `gradle/**`.

## 2. Create a release keystore (one time, on your PC - NOT in the repo)

```bash
keytool -genkeypair -v \
  -keystore nightwatch-release.jks \
  -alias nightwatch \
  -keyalg RSA -keysize 4096 -validity 10000
```

Remember the two passwords. **Back this file up** - if you lose it, users must
uninstall/reinstall to get future updates. It is already covered by `.gitignore`
(`*.jks`), never commit it.

## 3. Add 4 GitHub secrets

Repo -> Settings -> Secrets and variables -> Actions -> New repository secret:

| Secret name | Value |
|---|---|
| `RELEASE_KEYSTORE_B64` | output of `base64 -w0 nightwatch-release.jks` (Linux/Git-Bash) or `[Convert]::ToBase64String([IO.File]::ReadAllBytes("nightwatch-release.jks"))` (PowerShell) |
| `RELEASE_KEYSTORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | `nightwatch` |
| `RELEASE_KEY_PASSWORD` | key password |

The workflow deliberately **fails** if `RELEASE_KEYSTORE_B64` is missing, so a
debug-signed APK cannot slip out again.

## 4. Commit and push

```bash
git add -A
git commit -m "v3.0.1: signed release build, WhatsApp opt-in, notification filter, sound/volume fixes"
git push origin main
```

Download `NightWatch-release-apk` from the Actions run. The log of the
"Verify APK" step will print the certificate - it must NOT say `Android Debug`.

## 5. Installing over v3.0.0

v3.0.0 was signed with a throw-away debug key, so Android will refuse the
upgrade. Once only:

```
adb uninstall com.shimul.fiverrmessagealarm     # or uninstall from Settings
adb install app-release.apk
```

Re-enable notification access afterwards. From v3.0.1 on, updates install
normally as long as the same keystore is used.

## 6. Optional follow-ups (not done here)

- `foregroundServiceType="mediaPlayback"` is fine for sideloading but would be
  rejected on Play Store; switch to `specialUse` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
  if you ever publish.
- Fiverr sends order/promo notifications too. If they become noisy, add a
  "messages only" toggle that checks `EXTRA_TITLE`/`EXTRA_TEXT` for "message".
