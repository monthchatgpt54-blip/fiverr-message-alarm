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
    /** Alarm-stream volume saved before the service raised it to max. -1 = nothing saved. */
    static final String SAVED_ALARM_VOLUME = "saved_alarm_volume";
    /** Sentinel stored in RINGTONE_URI when the user explicitly picked "Silent". */
    static final String RINGTONE_SILENT = "silent";

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

    /**
     * OFF by default. WhatsApp is a personal messenger, not a marketplace; the user must
     * opt in explicitly so upgrading from v2.x does not silently start alarming on every
     * WhatsApp notification.
     */
    static boolean isWhatsappEnabled(Context context) {
        return get(context).getBoolean(WHATSAPP_ENABLED, false);
    }

    static String getRingtoneUri(Context context) {
        return get(context).getString(RINGTONE_URI, null);
    }

    static void setRingtoneUri(Context context, String uri) {
        get(context).edit().putString(RINGTONE_URI, uri).apply();
    }

    static boolean isRingtoneSilent(Context context) {
        return RINGTONE_SILENT.equals(getRingtoneUri(context));
    }

    static int getSavedAlarmVolume(Context context) {
        return get(context).getInt(SAVED_ALARM_VOLUME, -1);
    }

    static void setSavedAlarmVolume(Context context, int volume) {
        // commit() (synchronous) on purpose: this must hit disk before the process can be killed.
        get(context).edit().putInt(SAVED_ALARM_VOLUME, volume).commit();
    }

    static void clearSavedAlarmVolume(Context context) {
        get(context).edit().remove(SAVED_ALARM_VOLUME).commit();
    }
}
