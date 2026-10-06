package com.youssef.prayeroverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemEventReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PrayerScheduler.scheduleUpcoming(context);
    }
}
