package com.shimul.fiverrmessagealarm;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    static final String PREFS = "fiverr_alarm_settings";
    static final String ENABLED = "enabled";
    static final String MAX_VOLUME = "max_volume";
    static final String DURATION = "duration_seconds";

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

    static int durationSeconds(Context context) {
        return get(context).getInt(DURATION, 60);
    }
}
