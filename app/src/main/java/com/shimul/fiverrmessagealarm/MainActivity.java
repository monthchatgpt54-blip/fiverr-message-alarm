package com.shimul.fiverrmessagealarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 101;
    private TextView accessStatus;
    private TextView fullScreenStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmService.createChannel(this);
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

        TextView title = text("Fiverr Message Alarm", 26, Color.rgb(28, 30, 33));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text(
                "A Fiverr notification will trigger a loud alarm on this phone. No Fiverr login or external server is used.",
                15, Color.DKGRAY);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        root.addView(subtitle);

        CheckBox enabled = new CheckBox(this);
        enabled.setText("Alarm enabled");
        enabled.setTextSize(17);
        enabled.setChecked(AppPrefs.isEnabled(this));
        enabled.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.ENABLED, checked).apply());
        root.addView(enabled, matchWrap());

        CheckBox maxVolume = new CheckBox(this);
        maxVolume.setText("Temporarily use maximum alarm volume");
        maxVolume.setTextSize(16);
        maxVolume.setChecked(AppPrefs.useMaxVolume(this));
        maxVolume.setOnCheckedChangeListener((button, checked) ->
                AppPrefs.get(this).edit().putBoolean(AppPrefs.MAX_VOLUME, checked).apply());
        root.addView(maxVolume, matchWrap());

        addSectionLabel(root, "Alarm duration");
        Spinner duration = new Spinner(this);
        String[] labels = {"30 seconds", "1 minute", "5 minutes", "Until I stop it"};
        int[] values = {30, 60, 300, 0};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        duration.setAdapter(adapter);
        int saved = AppPrefs.durationSeconds(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == saved) selected = i;
        }
        duration.setSelection(selected);
        duration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppPrefs.get(MainActivity.this).edit()
                        .putInt(AppPrefs.DURATION, values[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(duration, matchWrap());

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

        addSectionLabel(root, "Test");
        Button test = button("Test alarm now");
        test.setTextColor(Color.WHITE);
        test.setBackgroundColor(Color.rgb(29, 137, 79));
        test.setOnClickListener(v -> {
            Intent intent = AlarmService.newIntent(this,
                    "Test Fiverr message", "This is a local test alarm.");
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        });
        root.addView(test, buttonParams());

        TextView note = text(
                "Important: keep Fiverr notifications enabled. This app reacts only after Android receives a Fiverr notification.",
                13, Color.GRAY);
        note.setPadding(0, dp(22), 0, 0);
        root.addView(note);
        return scroll;
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
