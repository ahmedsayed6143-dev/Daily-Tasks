package com.dailytasks.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "daily_tasks_channel";
    @Override public void onReceive(Context context, Intent intent) {
        MainActivity.cancelScheduledTimer(context);
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        show(context, title == null ? "Daily Tasks — انتهى الوقت" : title, body == null ? "خلص وقت المهمة" : body, 4101);
    }

    public static void show(Context context, String title, String body, int id) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تنبيهات المهام", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("تنبيهات انتهاء وقت المهام وإنجازها");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 4102, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder = new Notification.Builder(context, CHANNEL_ID);
        else builder = new Notification.Builder(context);
        Notification notification = builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true).setContentIntent(content).build();
        manager.notify(id, notification);
    }
}


