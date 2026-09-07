package com.shimul.fiverrmessagealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the "Stop all alarms" notification action. Not exported (manifest). */
public class AlarmActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && NightWatchAlarmService.ACTION_STOP.equals(intent.getAction())) {
            context.stopService(new Intent(context, NightWatchAlarmService.class));
        }
    }
}
