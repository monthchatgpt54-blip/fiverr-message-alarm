package com.shimul.fiverrmessagealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import java.io.IOException;

public class NightWatchAlarmService extends Service {
    static final String CHANNEL_ID = "night_watch_alarm_v2";
    private static final String FALLBACK_CHANNEL_ID = "night_watch_fallback_v2";
    static final String ACTION_STOP = "com.shimul.fiverrmessagealarm.STOP_V2";
    static final String EXTRA_PLATFORM = "alarm_platform";
    static final String EXTRA_PACKAGE = "alarm_package";
    static final String EXTRA_TITLE = "alarm_title";
    static final String EXTRA_TEXT = "alarm_text";
    private static final int NOTIFICATION_ID = 3702;
    private static final int FALLBACK_NOTIFICATION_ID = 3703;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishRingPhase = this::finishRingPhase;
    private final Runnable startNextCycle = this::startNextCycle;
    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private int originalAlarmVolume = -1;
    private int currentCycle = 1;
    private boolean sequenceActive;
    private boolean paused;
    private String platform = "Night Watch";
    private String targetPackage = "";
    private String title = "New client message";
    private String message = "Open the marketplace app to view it.";

    static Intent newIntent(Context context, String platform, String packageName,
                            String title, String text) {
        return new Intent(context, NightWatchAlarmService.class)
                .putExtra(EXTRA_PLATFORM, platform)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text);
    }

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel alarmChannel = new NotificationChannel(
                CHANNEL_ID, "Night Watch alarms", NotificationManager.IMPORTANCE_HIGH);
        alarmChannel.setDescription("Repeated alarm for Fiverr and Upwork notifications");
        alarmChannel.enableVibration(true);
        alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        alarmChannel.setSound(null, null);
        manager.createNotificationChannel(alarmChannel);

        NotificationChannel fallbackChannel = new NotificationChannel(
                FALLBACK_CHANNEL_ID, "Night Watch fallback alerts", NotificationManager.IMPORTANCE_HIGH);
        fallbackChannel.setDescription("Backup alert if Android blocks the repeating alarm service");
        fallbackChannel.enableVibration(true);
        fallbackChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        fallbackChannel.setSound(defaultAlarmUri(), alarmAttributes());
        manager.createNotificationChannel(fallbackChannel);
    }

    static void showFallbackNotification(Context context, String platform, String packageName,
                                         String title, String text) {
        createChannel(context);
        Intent screen = alarmScreenIntent(context, platform, packageName, title, text);
        PendingIntent fullScreen = PendingIntent.getActivity(context, 40, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification publicVersion = new Notification.Builder(context, FALLBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_night_watch_notification)
                .setContentTitle(platform + " message")
                .setContentText("Unlock to view client details")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        Notification notification = new Notification.Builder(context, FALLBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_night_watch_notification)
                .setContentTitle(platform + ": " + title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setAutoCancel(true)
                .build();
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(FALLBACK_NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        platform = safe(intent, EXTRA_PLATFORM, "Night Watch");
        targetPackage = safe(intent, EXTRA_PACKAGE, "");
        title = safe(intent, EXTRA_TITLE, "New " + platform + " message");
        message = safe(intent, EXTRA_TEXT, "Open " + platform + " to view the message.");
        if (sequenceActive) {
            updateNotification(paused);
            return START_NOT_STICKY;
        }
        sequenceActive = true;
        currentCycle = 1;
        cancelScheduledPhases();
        stopAlertMedia();

        Notification notification = buildNotification(false);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        holdWakeLock();
        startRingPhase();
        return START_NOT_STICKY;
    }

    private void startRingPhase() {
        paused = false;
        startAlarmSound();
        startVibration();
        updateNotification(false);
        handler.postDelayed(finishRingPhase, AppPrefs.RING_SECONDS * 1000L);
    }

    private void finishRingPhase() {
        stopAlertMedia();
        if (currentCycle >= AppPrefs.REPEAT_COUNT) {
            stopSelf();
            return;
        }
        paused = true;
        updateNotification(true);
        handler.postDelayed(startNextCycle, AppPrefs.PAUSE_SECONDS * 1000L);
    }

    private void startNextCycle() {
        currentCycle++;
        startRingPhase();
    }

    private void updateNotification(boolean paused) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(paused));
    }

    private Notification buildNotification(boolean paused) {
        Intent screen = alarmScreenIntent(this, platform, targetPackage, title, message);
        PendingIntent fullScreen = PendingIntent.getActivity(this, 20, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, AlarmActionReceiver.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getBroadcast(this, 21, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent launch = getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launch == null) launch = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 22, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String phase = paused
                ? "Paused - alarm " + (currentCycle + 1) + " of " + AppPrefs.REPEAT_COUNT + " in 1 minute"
                : "Alarm " + currentCycle + " of " + AppPrefs.REPEAT_COUNT + " - rings for 2 minutes";

        Notification publicVersion = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_night_watch_notification)
                .setContentTitle(platform + " message alarm")
                .setContentText("Unlock to view client details")
                .setSubText(phase)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .build();

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_night_watch_notification)
                .setContentTitle(platform + ": " + title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setSubText(phase)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, !paused)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stop all alarms", stop).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_view, "Open " + platform, open).build())
                .build();
    }

    private static Intent alarmScreenIntent(Context context, String platform, String packageName,
                                            String title, String text) {
        return new Intent(context, AlarmActivity.class)
                .putExtra(EXTRA_PLATFORM, platform)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    private void startAlarmSound() {
        applyMaxAlarmVolume();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(alarmAttributes());
            player.setDataSource(this, defaultAlarmUri());
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (IOException | IllegalStateException | SecurityException error) {
            if (player != null) {
                player.release();
                player = null;
            }
        }
    }

    private void applyMaxAlarmVolume() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null || !AppPrefs.useMaxVolume(this)) return;
        try {
            if (originalAlarmVolume < 0) {
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            }
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
        } catch (SecurityException ignored) {
            originalAlarmVolume = -1;
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 700, 350, 700, 800};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
    }

    private void stopAlertMedia() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) vibrator.cancel();
    }

    private void holdWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        if (manager == null) return;
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getPackageName() + ":night-watch-alarm");
        wakeLock.acquire(10 * 60 * 1000L);
    }

    private void cancelScheduledPhases() {
        handler.removeCallbacks(finishRingPhase);
        handler.removeCallbacks(startNextCycle);
    }

    private void restoreAlarmVolume() {
        if (audioManager == null || originalAlarmVolume < 0) return;
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0);
        } catch (SecurityException ignored) {
        }
        originalAlarmVolume = -1;
    }

    private static Uri defaultAlarmUri() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return uri == null ? Settings.System.DEFAULT_NOTIFICATION_URI : uri;
    }

    private static AudioAttributes alarmAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    private String safe(Intent intent, String key, String fallback) {
        if (intent == null) return fallback;
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    public void onDestroy() {
        sequenceActive = false;
        paused = false;
        cancelScheduledPhases();
        stopAlertMedia();
        restoreAlarmVolume();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
