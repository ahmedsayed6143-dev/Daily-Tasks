package com.dailytasks.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class TimerProgressService extends Service {
    public static final String ACTION_START = "com.dailytasks.app.timer.START";
    public static final String ACTION_STOP = "com.dailytasks.app.timer.STOP";
    private static final String CHANNEL_ID = "daily_tasks_progress";
    private static final String PREFS = "daily_tasks_timer";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DEADLINE = "deadline";
    private static final String KEY_STARTED = "startedAt";
    private static final int PROGRESS_NOTIFICATION_ID = 4102;
    private static final int FINISHED_NOTIFICATION_ID = 4101;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String taskTitle;
    private long deadlineAt;
    private boolean finished;

    public static void start(Context context, String title, long durationMs, long elapsedMs) {
        if (durationMs <= 0) return;
        long deadline = System.currentTimeMillis() + durationMs;
        Intent intent = new Intent(context, TimerProgressService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(KEY_TITLE, title);
        intent.putExtra(KEY_DEADLINE, deadline);
        intent.putExtra(KEY_STARTED, deadline - durationMs - Math.max(0L, elapsedMs));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, TimerProgressService.class);
        intent.setAction(ACTION_STOP);
        context.stopService(intent);
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            taskTitle = intent.getStringExtra(KEY_TITLE);
            deadlineAt = intent.getLongExtra(KEY_DEADLINE, 0L);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(KEY_STARTED, intent.getLongExtra(KEY_STARTED, deadlineAt)).apply();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_TITLE, taskTitle).putLong(KEY_DEADLINE, deadlineAt).apply();
        } else if (intent == null) {
            android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            taskTitle = prefs.getString(KEY_TITLE, null);
            deadlineAt = prefs.getLong(KEY_DEADLINE, 0L);
        }
        if (taskTitle == null || deadlineAt <= System.currentTimeMillis()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        finished = false;
        startForeground(PROGRESS_NOTIFICATION_ID, buildProgressNotification());
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (taskTitle == null || finished) return;
            long remaining = deadlineAt - System.currentTimeMillis();
            if (remaining <= 0) {
                finishTimer();
                return;
            }
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(PROGRESS_NOTIFICATION_ID, buildProgressNotification());
            handler.postDelayed(this, 1000L);
        }
    };

    private Notification buildProgressNotification() {
        long total = Math.max(1L, deadlineAt - getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_STARTED, deadlineAt - 1L));
        // The deadline is the source of truth; the system chronometer keeps counting while the device sleeps.
        long remaining = Math.max(0L, deadlineAt - System.currentTimeMillis());
        int progress = (int) Math.max(0L, Math.min(1000L, 1000L - (remaining * 1000L / total)));
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 4103, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("مهمة شغالة: " + taskTitle)
                .setContentText("الوقت المتبقي للمهمة")
                .setProgress(1000, progress, false)
                .setWhen(deadlineAt)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setContentIntent(content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) builder.setChronometerCountDown(true);
        return builder.build();
    }

    private void finishTimer() {
        if (finished) return;
        finished = true;
        handler.removeCallbacks(tick);
        MainActivity.cancelScheduledTimer(this);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
        TimerNotificationReceiver.show(this, "Daily Tasks — انتهى الوقت", "خلص وقت: " + taskTitle, FINISHED_NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تقدم المؤقت", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("يعرض تقدم المهمة والوقت المتبقي أثناء تشغيل المؤقت");
            channel.setSound(null, null);
            channel.enableVibration(false);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        if (!finished && (taskTitle == null || deadlineAt <= System.currentTimeMillis())) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}









