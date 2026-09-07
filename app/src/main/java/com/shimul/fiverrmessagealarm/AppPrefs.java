package com.shimul.fiverrmessagealarm;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    static final String PREFS = "night_watch_settings";
    static final String ENABLED = "enabled";
    static final String MAX_VOLUME = "max_volume";
    static final String FIVERR_ENABLED = "fiverr_enabled";
    static final String UPWORK_ENABLED = "upwork_enabled";
    static final String WHATSAPP_ENABLED = "whatsapp_enabled";
    static final String RINGTONE_URI = "ringtone_uri";
    static final int RING_SECONDS = 120;
    static final int PAUSE_SECONDS = 60;
    static final int REPEAT_COUNT = 3;

    private AppPrefs() {}

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context context) {
        return get(context).getBoolean(ENABLED, true);
    }

    static boolean useMaxVolume(Context context) {
        return get(context).getBoolean(MAX_VOLUME, true);
    }

    static boolean isFiverrEnabled(Context context) {
        return get(context).getBoolean(FIVERR_ENABLED, true);
    }

    static boolean isUpworkEnabled(Context context) {
        return get(context).getBoolean(UPWORK_ENABLED, true);
    }

    static boolean isWhatsappEnabled(Context context) {
        return get(context).getBoolean(WHATSAPP_ENABLED, true);
    }

    static String getRingtoneUri(Context context) {
        return get(context).getString(RINGTONE_URI, null);
    }

    static void setRingtoneUri(Context context, String uri) {
        get(context).edit().putString(RINGTONE_URI, uri).apply();
    }

    // Kept only so the v1 class remains source-compatible; v2 uses fixed cycles above.
    static int durationSeconds(Context context) {
        return 60;
    }
}
