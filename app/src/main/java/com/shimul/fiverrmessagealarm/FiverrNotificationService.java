package com.shimul.fiverrmessagealarm;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.Map;

public class FiverrNotificationService extends NotificationListenerService {
    private static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static final String UPWORK_PACKAGE = "com.upwork.android.apps.main";
    private static final long DUPLICATE_WINDOW_MS = 15_000L;
    private final Map<String, Long> recentNotifications = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (!AppPrefs.isEnabled(this)) return;

        String packageName = sbn.getPackageName();
        String platform;
        if (FIVERR_PACKAGE.equals(packageName) && AppPrefs.isFiverrEnabled(this)) {
            platform = "Fiverr";
        } else if (UPWORK_PACKAGE.equals(packageName) && AppPrefs.isUpworkEnabled(this)) {
            platform = "Upwork";
        } else {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        long now = System.currentTimeMillis();
        String key = packageName + ":" + (sbn.getKey() == null ? sbn.getId() : sbn.getKey());
        Long previous = recentNotifications.get(key);
        if (previous != null && now - previous < DUPLICATE_WINDOW_MS) return;
        recentNotifications.put(key, now);

        Bundle extras = notification.extras;
        String conversation = value(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE), "");
        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE), "");
        if (!conversation.isEmpty()) title = conversation;
        if (title.isEmpty() || platform.equalsIgnoreCase(title)) title = "New " + platform + " message";
        String text = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT), "");
        if (text.isEmpty()) text = value(extras.getCharSequence(Notification.EXTRA_TEXT),
                "Open " + platform + " to view the message.");

        Intent intent = NightWatchAlarmService.newIntent(this, platform, packageName, title, text);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        } catch (RuntimeException restricted) {
            NightWatchAlarmService.showFallbackNotification(this, platform, packageName, title, text);
        }
    }

    private String value(CharSequence input, String fallback) {
        if (input == null) return fallback;
        String result = input.toString().trim();
        return result.isEmpty() ? fallback : result;
    }
}
