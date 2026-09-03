package com.shimul.fiverrmessagealarm;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FiverrNotificationService extends NotificationListenerService {
    private static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static final String UPWORK_PACKAGE = "com.upwork.android.apps.main";
    private static final long DUPLICATE_WINDOW_MS = 30_000L;
    private static final long ENTRY_TTL_MS = 10 * 60_000L;
    private static final int MAX_RECENT_ENTRIES = 128;
    private final Map<String, Long> recentNotifications = new LinkedHashMap<>();

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
        pruneRecentNotifications(now);
        String key = packageName + ":" + (sbn.getKey() == null ? sbn.getId() : sbn.getKey());
        Long previous = recentNotifications.get(key);
        if (previous != null && now - previous < DUPLICATE_WINDOW_MS) return;
        recentNotifications.put(key, now);

        Bundle extras = notification.extras;
        String conversation = value(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE), "", 160);
        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE), "", 160);
        if (!conversation.isEmpty()) title = conversation;
        if (title.isEmpty() || platform.equalsIgnoreCase(title)) title = "New " + platform + " message";
        String text = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT), "", 1200);
        if (text.isEmpty()) text = value(extras.getCharSequence(Notification.EXTRA_TEXT),
                "Open " + platform + " to view the message.", 1200);

        Intent intent = NightWatchAlarmService.newIntent(this, platform, packageName, title, text);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        } catch (RuntimeException restricted) {
            NightWatchAlarmService.showFallbackNotification(this, platform, packageName, title, text);
        }
    }

    private void pruneRecentNotifications(long now) {
        Iterator<Map.Entry<String, Long>> iterator = recentNotifications.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > ENTRY_TTL_MS) iterator.remove();
        }
        while (recentNotifications.size() >= MAX_RECENT_ENTRIES) {
            Iterator<String> keys = recentNotifications.keySet().iterator();
            if (!keys.hasNext()) break;
            keys.next();
            keys.remove();
        }
    }

    private String value(CharSequence input, String fallback, int maxLength) {
        if (input == null) return fallback;
        String raw = input.toString();
        StringBuilder clean = new StringBuilder(Math.min(raw.length(), maxLength));
        for (int i = 0; i < raw.length() && clean.length() < maxLength; i++) {
            char value = raw.charAt(i);
            int type = Character.getType(value);
            if (type == Character.FORMAT) continue;
            if (Character.isISOControl(value) && value != '\n' && value != '\t') {
                clean.append(' ');
            } else {
                clean.append(value);
            }
        }
        String result = clean.toString().trim();
        return result.isEmpty() ? fallback : result;
    }
}
