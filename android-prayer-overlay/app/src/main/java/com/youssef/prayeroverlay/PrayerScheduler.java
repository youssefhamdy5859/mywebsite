package com.youssef.prayeroverlay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public final class PrayerScheduler {
    private PrayerScheduler() {}
    public static final ZoneId RIYADH_ZONE = ZoneId.of("Asia/Riyadh");
    private static final long THREE_MINUTES_MS = 180000L;
    private static final int DAYS = 8;

    public static boolean canScheduleExactAlarms(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        return m != null && m.canScheduleExactAlarms();
    }

    public static void scheduleUpcoming(Context c) {
        if (!canScheduleExactAlarms(c)) return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am==null) return;
        long now=System.currentTimeMillis();
        LocalDate today=ZonedDateTime.now(RIYADH_ZONE).toLocalDate();

        for(int d=0; d<DAYS; d++) {
            LocalDate date=today.plusDays(d);
            List<PrayerTimesCalculator.PrayerMoment> prayers=PrayerTimesCalculator.getPrayerMoments(date);
            for(int i=0;i<prayers.size();i++) {
                PrayerTimesCalculator.PrayerMoment p=prayers.get(i);
                long pt=p.time.getTime(), trigger=pt-THREE_MINUTES_MS;
                if(trigger<=now+1500L) continue;
                Intent in=new Intent(c,PrayerAlarmReceiver.class)
                        .setAction("com.youssef.prayeroverlay.PRAYER_"+date.toString()+"_"+i)
                        .putExtra(PrayerOverlayService.EXTRA_PRAYER_NAME,p.name)
                        .putExtra(PrayerOverlayService.EXTRA_PRAYER_TIME,pt);
                PendingIntent pi=PendingIntent.getBroadcast(c, requestCode(date,i), in,
                        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                try {
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);
                    else am.setExact(AlarmManager.RTC_WAKEUP,trigger,pi);
                } catch(SecurityException e) { return; }
            }
        }
    }

    public static NextPrayer findNextPrayer() {
        long now=System.currentTimeMillis();
        LocalDate today=ZonedDateTime.now(RIYADH_ZONE).toLocalDate();
        for(int d=0; d<=1; d++)
            for(PrayerTimesCalculator.PrayerMoment p:PrayerTimesCalculator.getPrayerMoments(today.plusDays(d)))
                if(p.time.getTime()>now) return new NextPrayer(p.name,p.time.getTime());
        return null;
    }

    private static int requestCode(LocalDate d,int i) {
        int ymd=d.getYear()*10000+d.getMonthValue()*100+d.getDayOfMonth();
        return ymd*10+i;
    }
    public static final class NextPrayer {
        public final String name; public final long timeMillis;
        public NextPrayer(String n,long t){name=n;timeMillis=t;}
    }
}
