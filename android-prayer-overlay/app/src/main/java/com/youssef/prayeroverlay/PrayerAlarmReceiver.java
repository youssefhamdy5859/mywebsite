package com.youssef.prayeroverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PrayerScheduler.scheduleUpcoming(context);
        if (!Settings.canDrawOverlays(context)) return;
        Intent s = new Intent(context, PrayerOverlayService.class)
                .putExtra(PrayerOverlayService.EXTRA_PRAYER_NAME, intent.getStringExtra(PrayerOverlayService.EXTRA_PRAYER_NAME))
                .putExtra(PrayerOverlayService.EXTRA_PRAYER_TIME, intent.getLongExtra(PrayerOverlayService.EXTRA_PRAYER_TIME,0L));
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(s);
            else context.startService(s);
        } catch (Exception ignored) {}
    }
}
