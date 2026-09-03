package com.shimul.fiverrmessagealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && NightWatchAlarmService.ACTION_STOP.equals(intent.getAction())) {
            context.stopService(new Intent(context, NightWatchAlarmService.class));
        }
    }
}
