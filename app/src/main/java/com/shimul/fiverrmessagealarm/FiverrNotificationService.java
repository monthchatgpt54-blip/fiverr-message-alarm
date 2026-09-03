package com.shimul.fiverrmessagealarm;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class FiverrNotificationService extends NotificationListenerService {
    private static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static final long DUPLICATE_WINDOW_MS = 15_000L;
    private String lastKey = "";
    private long lastTriggeredAt = 0L;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !FIVERR_PACKAGE.equals(sbn.getPackageName())) return;
        if (!AppPrefs.isEnabled(this)) return;

        Notification notification = sbn.getNotification();
        if (notification == null || (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        long now = System.currentTimeMillis();
        String key = sbn.getKey();
        if (key != null && key.equals(lastKey) && now - lastTriggeredAt < DUPLICATE_WINDOW_MS) return;
        lastKey = key == null ? "" : key;
        lastTriggeredAt = now;

        Bundle extras = notification.extras;
        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE), "New Fiverr notification");
        String text = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT), "");
        if (text.isEmpty()) text = value(extras.getCharSequence(Notification.EXTRA_TEXT), "Open Fiverr to view it.");

        Intent intent = AlarmService.newIntent(this, title, text);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private String value(CharSequence input, String fallback) {
        if (input == null) return fallback;
        String result = input.toString().trim();
        return result.isEmpty() ? fallback : result;
    }
}
