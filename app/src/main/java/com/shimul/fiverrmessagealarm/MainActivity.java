package com.shimul.fiverrmessagealarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Ringtone;
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
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";

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
        // If a previous alarm run was killed before it could restore the volume, fix it now.
        NightWatchAlarmService.restoreStaleAlarmVolume(this);
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

        TextView title = text("Night Watch v3.0.1", 26, Color.rgb(28, 30, 33));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text(
                "24-hour local alarm watch for Fiverr and Upwork notifications (WhatsApp optional). "
                        + "No marketplace login or external server is used.",
                15, Color.DKGRAY);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        root.addView(subtitle);

        root.addView(toggle("24-hour Night Watch enabled", 17,
                AppPrefs.isEnabled(this), AppPrefs.ENABLED), matchWrap());
        root.addView(toggle("Watch Fiverr notifications", 16,
                AppPrefs.isFiverrEnabled(this), AppPrefs.FIVERR_ENABLED), matchWrap());
        root.addView(toggle("Watch Upwork notifications", 16,
                AppPrefs.isUpworkEnabled(this), AppPrefs.UPWORK_ENABLED), matchWrap());
        root.addView(toggle("Watch WhatsApp / WhatsApp Business (off by default - alarms on every chat)", 16,
                AppPrefs.isWhatsappEnabled(this), AppPrefs.WHATSAPP_ENABLED), matchWrap());
        root.addView(toggle("Temporarily use maximum alarm volume", 16,
                AppPrefs.useMaxVolume(this), AppPrefs.MAX_VOLUME), matchWrap());

        addSectionLabel(root, "Alarm cycle");
        root.addView(text(
                "2 minutes ringing -> 1 minute pause -> repeated up to 3 times. Stop cancels the entire sequence.",
                15, Color.DKGRAY));

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
        dndButton.setOnClickListener(v -> safeStart(
                new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                "Open Do Not Disturb access in Settings."));
        root.addView(dndButton, buttonParams());

        Button batteryButton = button("Open battery optimization settings");
        batteryButton.setOnClickListener(v -> safeStart(
                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Open battery settings manually."));
        root.addView(batteryButton, buttonParams());

        addSectionLabel(root, "Alarm sound");
        selectedRingtoneText = text("", 14, Color.DKGRAY);
        updateRingtoneText();
        root.addView(selectedRingtoneText);
        Button ringtoneButton = button("Select alarm tone");
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
                "WhatsApp", installedWhatsappPackage(), "Test Contact", "This is a local WhatsApp test message."));
        root.addView(testWhatsapp, buttonParams());

        TextView note = text(
                "Privacy: client details stay hidden on the lock screen and never leave the phone. "
                        + "Keep marketplace notifications enabled, allow auto-start if available, and exclude "
                        + "Night Watch and the marketplace apps from battery optimization.",
                13, Color.GRAY);
        note.setPadding(0, dp(22), 0, 0);
        root.addView(note);
        return scroll;
    }

    // ---------------------------------------------------------------- actions

    private CheckBox toggle(String label, int sp, boolean checked, String prefKey) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(sp);
        box.setChecked(checked);
        box.setOnCheckedChangeListener((button, isChecked) ->
                AppPrefs.get(this).edit().putBoolean(prefKey, isChecked).apply());
        return box;
    }

    private void startTest(String platform, String packageName, String title, String message) {
        Intent intent = NightWatchAlarmService.newIntent(this, platform, packageName, title, message);
        try {
            startForegroundService(intent);
        } catch (RuntimeException e) {
            NightWatchAlarmService.showFallbackNotification(this, platform, packageName, title, message);
        }
    }

    /** Prefer whichever WhatsApp flavour is actually installed (declared in <queries>). */
    private String installedWhatsappPackage() {
        PackageManager pm = getPackageManager();
        if (pm.getLaunchIntentForPackage(WHATSAPP_PACKAGE) != null) return WHATSAPP_PACKAGE;
        if (pm.getLaunchIntentForPackage(WHATSAPP_BUSINESS_PACKAGE) != null) return WHATSAPP_BUSINESS_PACKAGE;
        return WHATSAPP_PACKAGE;
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        String saved = AppPrefs.getRingtoneUri(this);
        Uri existing = null;
        if (saved != null && !saved.isEmpty() && !AppPrefs.RINGTONE_SILENT.equals(saved)) {
            try {
                existing = Uri.parse(saved);
            } catch (RuntimeException ignored) {
            }
        }
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        safeStartForResult(intent, REQUEST_RINGTONE, "No ringtone picker available on this device.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_RINGTONE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            // Picker returns null for "Silent". Honour it instead of falling back to default.
            AppPrefs.setRingtoneUri(this, AppPrefs.RINGTONE_SILENT);
        } else {
            // Keep the grant alive for the service so a custom file keeps working after reboot.
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // System ringtones are world-readable and do not need (or support) this.
            }
            AppPrefs.setRingtoneUri(this, uri.toString());
        }
        updateRingtoneText();
    }

    private void updateRingtoneText() {
        if (selectedRingtoneText == null) return;
        String uriStr = AppPrefs.getRingtoneUri(this);
        if (uriStr == null || uriStr.isEmpty()) {
            selectedRingtoneText.setText("Current: default alarm tone");
            return;
        }
        if (AppPrefs.RINGTONE_SILENT.equals(uriStr)) {
            selectedRingtoneText.setText("Current: Silent (vibration only)");
            return;
        }
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(uriStr));
            String name = ringtone == null ? null : ringtone.getTitle(this);
            selectedRingtoneText.setText("Current: " + (name == null ? "custom sound" : name));
        } catch (Exception e) {
            selectedRingtoneText.setText("Current: custom sound (may be unavailable - default will be used)");
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
        accessStatus.setText(listener ? "OK  Notification access is enabled" : "!  Notification access is not enabled");
        accessStatus.setTextColor(listener ? Color.rgb(20, 120, 65) : Color.rgb(180, 75, 20));

        boolean canFullScreen = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            canFullScreen = manager == null || manager.canUseFullScreenIntent();
        }
        fullScreenStatus.setText(canFullScreen
                ? "OK  Full-screen alarms are allowed"
                : "!  Full-screen alarm access is not allowed");
        fullScreenStatus.setTextColor(canFullScreen ? Color.rgb(20, 120, 65) : Color.rgb(180, 75, 20));
    }

    private boolean isNotificationListenerEnabled() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && Build.VERSION.SDK_INT >= 27) {
            return manager.isNotificationListenerAccessGranted(
                    new ComponentName(this, FiverrNotificationService.class));
        }
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null || enabled.trim().isEmpty()) return false;
        for (String value : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (component != null && getPackageName().equals(component.getPackageName())) return true;
        }
        return false;
    }

    private void openNotificationAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            new ComponentName(this, FiverrNotificationService.class).flattenToString());
            try {
                startActivity(detail);
                return;
            } catch (Exception ignored) {
            }
        }
        safeStart(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                "Open Settings > Notifications > Notification access.");
    }

    private void openFullScreenAccess() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName())));
                return;
            } catch (Exception ignored) {
            }
        }
        safeStart(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
                "Open this app's notification settings manually.");
    }

    private void safeStart(Intent intent, String failureMessage) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void safeStartForResult(Intent intent, int code, String failureMessage) {
        try {
            startActivityForResult(intent, code);
        } catch (Exception e) {
            Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- ui helpers

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
