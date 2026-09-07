package com.shimul.fiverrmessagealarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 101;
    private static final int REQUEST_RINGTONE = 102;
    private TextView accessStatus;
    private TextView fullScreenStatus;
    private TextView selectedRingtoneText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NightWatchAlarmService.createChannel(this);
        setContentView(buildScreen());
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Night Watch v2.1 Security", 26, Color.rgb(28, 30, 33));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text(
                "24-hour local alarm watch for Fiverr and Upwork notifications. No marketplace login or external server is used.",
                15, Color.DKGRAY);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        root.addView(subtitle);

        CheckBox enabled = new CheckBox(this);
        enabled.setText("24-hour Night Watch enabled");
        enabled.setTextSize(17);
        enabled.setChecked(AppPrefs.isEnabled(this));
        enabled.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.ENABLED, checked).apply());
        root.addView(enabled, matchWrap());

        CheckBox fiverr = new CheckBox(this);
        fiverr.setText("Watch Fiverr notifications");
        fiverr.setTextSize(16);
        fiverr.setChecked(AppPrefs.isFiverrEnabled(this));
        fiverr.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.FIVERR_ENABLED, checked).apply());
        root.addView(fiverr, matchWrap());

        CheckBox upwork = new CheckBox(this);
        upwork.setText("Watch Upwork notifications");
        upwork.setTextSize(16);
        upwork.setChecked(AppPrefs.isUpworkEnabled(this));
        upwork.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.UPWORK_ENABLED, checked).apply());
        root.addView(upwork, matchWrap());

        CheckBox whatsapp = new CheckBox(this);
        whatsapp.setText("Watch WhatsApp notifications");
        whatsapp.setTextSize(16);
        whatsapp.setChecked(AppPrefs.isWhatsappEnabled(this));
        whatsapp.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.WHATSAPP_ENABLED, checked).apply());
        root.addView(whatsapp, matchWrap());

        CheckBox maxVolume = new CheckBox(this);
        maxVolume.setText("Temporarily use maximum alarm volume");
        maxVolume.setTextSize(16);
        maxVolume.setChecked(AppPrefs.useMaxVolume(this));
        maxVolume.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.MAX_VOLUME, checked).apply());
        root.addView(maxVolume, matchWrap());

        addSectionLabel(root, "Alarm cycle");
        TextView cycle = text(
                "2 minutes ringing → 1 minute pause → repeated up to 3 times. Stop cancels the entire sequence.",
                15, Color.DKGRAY);
        root.addView(cycle);

        addSectionLabel(root, "Required access");
        accessStatus = text("", 14, Color.DKGRAY);
        root.addView(accessStatus);
        Button accessButton = button("Enable notification access");
        accessButton.setOnClickListener(v -> openNotificationAccess());
        root.addView(accessButton, buttonParams());

        fullScreenStatus = text("", 14, Color.DKGRAY);
        fullScreenStatus.setPadding(0, dp(10), 0, 0);
        root.addView(fullScreenStatus);
        Button fullScreenButton = button("Allow full-screen alarms");
        fullScreenButton.setOnClickListener(v -> openFullScreenAccess());
        root.addView(fullScreenButton, buttonParams());

        Button dndButton = button("Allow alarm during Do Not Disturb");
        dndButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Open Do Not Disturb access in Settings.", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(dndButton, buttonParams());

        Button batteryButton = button("Open battery optimization settings");
        batteryButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        root.addView(batteryButton, buttonParams());

        addSectionLabel(root, "Alarm Sound");
        selectedRingtoneText = text("Default Ringtone", 14, Color.DKGRAY);
        updateRingtoneText();
        root.addView(selectedRingtoneText);
        Button ringtoneButton = button("Select Custom Alarm Ringtone/Music");
        ringtoneButton.setOnClickListener(v -> openRingtonePicker());
        root.addView(ringtoneButton, buttonParams());

        addSectionLabel(root, "Test");
        Button testFiverr = button("Test Fiverr alarm");
        testFiverr.setTextColor(Color.WHITE);
        testFiverr.setBackgroundColor(Color.rgb(29, 137, 79));
        testFiverr.setOnClickListener(v -> startTest(
                "Fiverr", "com.fiverr.fiverr", "Test Client", "This is a local Fiverr test message."));
        root.addView(testFiverr, buttonParams());

        Button testUpwork = button("Test Upwork alarm");
        testUpwork.setOnClickListener(v -> startTest(
                "Upwork", "com.upwork.android.apps.main", "Test Client", "This is a local Upwork test message."));
        root.addView(testUpwork, buttonParams());

        Button testWhatsapp = button("Test WhatsApp alarm");
        testWhatsapp.setOnClickListener(v -> startTest(
                "WhatsApp", "com.whatsapp", "Test Contact", "This is a local WhatsApp test message."));
        root.addView(testWhatsapp, buttonParams());

        TextView note = text(
                "Security: client details stay hidden on the lock screen. Keep Fiverr and Upwork notifications enabled, allow auto-start if available, and exclude all three apps from battery optimization.",
                13, Color.GRAY);
        note.setPadding(0, dp(22), 0, 0);
        root.addView(note);
        return scroll;
    }

    private void startTest(String platform, String packageName, String title, String message) {
        Intent intent = NightWatchAlarmService.newIntent(
                this, platform, packageName, title, message);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, 
                AppPrefs.getRingtoneUri(this) != null ? Uri.parse(AppPrefs.getRingtoneUri(this)) : null);
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                AppPrefs.setRingtoneUri(this, uri.toString());
            } else {
                AppPrefs.setRingtoneUri(this, "");
            }
            updateRingtoneText();
        }
    }

    private void updateRingtoneText() {
        if (selectedRingtoneText == null) return;
        String uriStr = AppPrefs.getRingtoneUri(this);
        if (uriStr == null || uriStr.isEmpty()) {
            selectedRingtoneText.setText("Current: Default Alarm Tone");
        } else {
            try {
                Uri uri = Uri.parse(uriStr);
                String title = RingtoneManager.getRingtone(this, uri).getTitle(this);
                selectedRingtoneText.setText("Current: " + title);
            } catch (Exception e) {
                selectedRingtoneText.setText("Current: Custom Sound");
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void refreshPermissionStatus() {
        if (accessStatus == null || fullScreenStatus == null) return;
        boolean listener = isNotificationListenerEnabled();
        accessStatus.setText(listener ? "✓ Notification access is enabled" : "! Notification access is not enabled");
        accessStatus.setTextColor(listener ? Color.rgb(20, 120, 65) : Color.rgb(180, 75, 20));

        boolean canFullScreen = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            canFullScreen = manager.canUseFullScreenIntent();
        }
        fullScreenStatus.setText(canFullScreen
                ? "✓ Full-screen alarms are allowed"
                : "! Full-screen alarm access is not allowed");
        fullScreenStatus.setTextColor(canFullScreen ? Color.rgb(20, 120, 65) : Color.rgb(180, 75, 20));
    }

    private boolean isNotificationListenerEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        if (enabled == null || enabled.trim().isEmpty()) return false;
        String[] components = enabled.split(":");
        for (String value : components) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (component != null && getPackageName().equals(component.getPackageName())) return true;
        }
        return false;
    }

    private void openNotificationAccess() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                detail.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(this, FiverrNotificationService.class).flattenToString());
                startActivity(detail);
            } else {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    private void openFullScreenAccess() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }
        Intent settings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(settings);
    }

    private void addSectionLabel(LinearLayout root, String value) {
        TextView label = text(value, 17, Color.rgb(35, 37, 40));
        label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        label.setPadding(0, dp(24), 0, dp(8));
        root.addView(label);
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setFilterTouchesWhenObscured(true);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(6);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
