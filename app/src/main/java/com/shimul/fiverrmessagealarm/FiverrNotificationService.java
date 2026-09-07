package com.shimul.fiverrmessagealarm;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FiverrNotificationService extends NotificationListenerService {
    private static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static final String UPWORK_PACKAGE = "com.upwork.android.apps.main";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";
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
        } else if ((WHATSAPP_PACKAGE.equals(packageName) || WHATSAPP_BUSINESS_PACKAGE.equals(packageName))
                && AppPrefs.isWhatsappEnabled(this)) {
            platform = "WhatsApp";
        } else {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || !isRealMessage(sbn, notification)) return;

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
            startForegroundService(intent);
        } catch (RuntimeException restricted) {
            // ForegroundServiceStartNotAllowedException (API 31+) or any OEM restriction.
            NightWatchAlarmService.showFallbackNotification(this, platform, packageName, title, text);
        }
    }

    /**
     * Drop everything that is not an actual incoming message:
     *  - group summaries (the real child notification follows separately)
     *  - ongoing / foreground-service banners ("WhatsApp Web is active", "Backing up…",
     *    "Checking for new messages", music/transport controls)
     *  - calls, progress bars, status and system notices
     * Without this filter a single WhatsApp backup notification starts a 9-minute alarm.
     */
    private static boolean isRealMessage(StatusBarNotification sbn, Notification n) {
        int flags = n.flags;
        if ((flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
        if ((flags & Notification.FLAG_ONGOING_EVENT) != 0) return false;
        if ((flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) return false;
        if (sbn.isOngoing()) return false;

        String category = n.category;
        if (category == null) return true; // Fiverr / Upwork often leave it unset - allow.
        switch (category) {
            case Notification.CATEGORY_SERVICE:
            case Notification.CATEGORY_PROGRESS:
            case Notification.CATEGORY_CALL:
            case Notification.CATEGORY_STATUS:
            case Notification.CATEGORY_SYSTEM:
            case Notification.CATEGORY_TRANSPORT:
            case Notification.CATEGORY_ERROR:
                return false;
            default:
                return true;
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
