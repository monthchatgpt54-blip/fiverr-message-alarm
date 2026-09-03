# Night Watch v2

Personal Android app that listens locally for notifications from Fiverr
(`com.fiverr.fiverr`) and Upwork (`com.upwork.android.apps.main`). It displays
the notification title/client name and preview, then starts a repeated alarm.

## Privacy

- No Fiverr credentials are requested or stored.
- No network permission is declared.
- Notification data is not transmitted or saved.
- All processing happens on the phone.

## First-time setup

1. Install the APK and open **Night Watch**.
2. Allow app notifications.
3. Tap **Enable notification access** and enable this app.
4. On Android 14+, tap **Allow full-screen alarms** and enable access.
5. Exclude Fiverr, Upwork and Night Watch from battery optimization.
6. Enable auto-start for Night Watch if the phone provides that setting.
7. Run both test buttons.

Each cycle rings for 2 minutes, pauses for 1 minute, and repeats up to 3 times.
The Stop button cancels the active alarm and every remaining repeat. To avoid
missing a client, reliable mode reacts to every notification from either
enabled marketplace; it does not attempt to read either private inbox directly.
