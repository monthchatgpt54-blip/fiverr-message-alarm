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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class AlarmService extends Service {
    static final String CHANNEL_ID = "fiverr_message_alarm_v1";
    static final String ACTION_STOP = "com.shimul.fiverrmessagealarm.STOP";
    static final String EXTRA_TITLE = "alarm_title";
    static final String EXTRA_TEXT = "alarm_text";
    private static final int NOTIFICATION_ID = 3701;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private int originalAlarmVolume = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = this::stopSelf;

    static Intent newIntent(Context context, String title, String text) {
        return new Intent(context, AlarmService.class)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text);
    }

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Fiverr message alarms", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Alarm shown when Fiverr posts a notification");
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
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

        String title = intent == null ? "New Fiverr notification" :
                intent.getStringExtra(EXTRA_TITLE);
        String text = intent == null ? "Open Fiverr to view it." :
                intent.getStringExtra(EXTRA_TEXT);
        if (title == null || title.trim().isEmpty()) title = "New Fiverr notification";
        if (text == null || text.trim().isEmpty()) text = "Open Fiverr to view it.";

        Notification notification = buildNotification(title, text);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startAlarmSound();
        startVibration();
        holdWakeLock();

        handler.removeCallbacks(timeout);
        int duration = AppPrefs.durationSeconds(this);
        if (duration > 0) handler.postDelayed(timeout, duration * 1000L);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String title, String text) {
        Intent alarmScreen = new Intent(this, AlarmActivity.class)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(this, 20, alarmScreen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, AlarmActionReceiver.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getBroadcast(this, 21, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent launch = getPackageManager().getLaunchIntentForPackage("com.fiverr.fiverr");
        if (launch == null) launch = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 22, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stop", stop).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_view, "Open Fiverr", open).build())
                .build();
    }

    private void startAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) return;
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null && AppPrefs.useMaxVolume(this)) {
            try {
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            } catch (SecurityException ignored) {
                originalAlarmVolume = -1;
            }
        }

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone == null) return;
        ringtone.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        if (Build.VERSION.SDK_INT >= 28) ringtone.setLooping(true);
        ringtone.play();
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 700, 350, 700, 800};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
    }

    private void holdWakeLock() {
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        if (manager == null) return;
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getPackageName() + ":fiverr-alarm");
        wakeLock.acquire(10 * 60 * 1000L);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(timeout);
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (vibrator != null) vibrator.cancel();
        if (audioManager != null && originalAlarmVolume >= 0) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0);
            } catch (SecurityException ignored) {}
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
