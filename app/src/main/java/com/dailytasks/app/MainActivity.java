package com.dailytasks.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 71;
    private static final String CHANNEL_ID = "daily_tasks_channel";
    private static final int TIMER_NOTIFICATION_ID = 4101;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        getWindow().setStatusBarColor(android.graphics.Color.rgb(247,247,244));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(247,247,244));
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        webView = new WebView(this);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تنبيهات المهام", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("تنبيهات انتهاء وقت المهام وإنجازها");
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    public static void cancelScheduledTimer(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TimerNotificationReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, TIMER_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (manager != null) manager.cancel(pending);
        TimerProgressService.stop(context);
    }

    @Override public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("(function(){var ids=['calendarModal','finishModal','editTaskModal','noteModal','deleteModal'];for(var i=0;i<ids.length;i++){var e=document.getElementById(ids[i]);if(e&&e.classList.contains('show')){e.classList.remove('show');return 'modal'};}return 'none'})()", value -> {
                if ("\"none\"".equals(value)) finish();
            });
        } else super.onBackPressed();
    }

    public class AndroidBridge {
        private final Context context;
        AndroidBridge(Context context) { this.context = context.getApplicationContext(); }

        @JavascriptInterface public void showNotification(String title, String body) {
            cancelScheduledTimer(context);
            TimerNotificationReceiver.show(context, title, body, TIMER_NOTIFICATION_ID);
        }

        @JavascriptInterface public void scheduleTimer(String title, long millis, long elapsedMillis) {
            cancelScheduledTimer(context);
            TimerProgressService.start(context, title, millis, elapsedMillis);
            Intent intent = new Intent(context, TimerNotificationReceiver.class);
            intent.putExtra("title", "Daily Tasks — انتهى الوقت");
            intent.putExtra("body", "خلص وقت: " + title);
            PendingIntent pending = PendingIntent.getBroadcast(context, TIMER_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (manager != null) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + Math.max(1000L, millis), pending);
        }

        @JavascriptInterface public void cancelTimer() { cancelScheduledTimer(context); }
    }
}







