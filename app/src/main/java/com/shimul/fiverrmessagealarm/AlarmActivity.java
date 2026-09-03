package com.shimul.fiverrmessagealarm;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        setContentView(buildScreen(getIntent()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setContentView(buildScreen(intent));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(buildScreen(getIntent()));
    }

    private LinearLayout buildScreen(Intent intent) {
        String platform = intent.getStringExtra(NightWatchAlarmService.EXTRA_PLATFORM);
        String targetPackage = intent.getStringExtra(NightWatchAlarmService.EXTRA_PACKAGE);
        String titleValue = intent.getStringExtra(NightWatchAlarmService.EXTRA_TITLE);
        String textValue = intent.getStringExtra(NightWatchAlarmService.EXTRA_TEXT);
        if (platform == null || platform.trim().isEmpty()) platform = "Night Watch";
        if (targetPackage == null) targetPackage = "";
        if (titleValue == null) titleValue = "New " + platform + " message";
        if (textValue == null) textValue = "Open " + platform + " to view it.";
        KeyguardManager keyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard != null && keyguard.isDeviceLocked()) {
            titleValue = "New " + platform + " message";
            textValue = "Unlock your phone to view the client name and message.";
        }
        final String appPackage = targetPackage;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(36), dp(28), dp(36));
        root.setBackgroundColor(Color.rgb(25, 31, 35));

        TextView badge = text(platform.toUpperCase() + " MESSAGE", 14, Color.rgb(60, 220, 130));
        root.addView(badge);

        TextView title = text(titleValue, 27, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(22), 0, dp(12));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView message = text(textValue, 17, Color.LTGRAY);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, dp(30));
        root.addView(message);

        Button stop = new Button(this);
        stop.setText("Stop alarm");
        stop.setTextSize(18);
        stop.setAllCaps(false);
        stop.setFilterTouchesWhenObscured(true);
        stop.setOnClickListener(v -> stopAndFinish());
        root.addView(stop, fullWidth());

        Button open = new Button(this);
        open.setText("Open " + platform);
        open.setTextSize(18);
        open.setAllCaps(false);
        open.setFilterTouchesWhenObscured(true);
        open.setOnClickListener(v -> {
            stopService(new Intent(this, NightWatchAlarmService.class));
            Intent launch = getPackageManager().getLaunchIntentForPackage(appPackage);
            if (launch != null) startActivity(launch);
            finish();
        });
        LinearLayout.LayoutParams openParams = fullWidth();
        openParams.topMargin = dp(12);
        root.addView(open, openParams);
        return root;
    }

    private void stopAndFinish() {
        stopService(new Intent(this, NightWatchAlarmService.class));
        finishAndRemoveTask();
    }

    @Override
    public void onBackPressed() {
        stopAndFinish();
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
