package com.youssef.prayeroverlay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private TextView status;
    private TextView nextPrayer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(buildUi());
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
        if (Settings.canDrawOverlays(this) && PrayerScheduler.canScheduleExactAlarms(this)) {
            PrayerScheduler.scheduleUpcoming(this);
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = dp(24); box.setPadding(p, dp(32), p, dp(32));

        TextView title = tv("تذكير الصلاة - الرياض", 28, true);
        title.setGravity(Gravity.CENTER); box.addView(title, lp());
        TextView desc = tv("قبل الصلاة بثلاث دقائق تظهر شاشة بيضاء فوق التطبيقات مكتوب عليها: ما عند الله خير وأبقى. وتصدر نغمة هادئة لمدة 10 ثوانٍ في بداية كل دقيقة من الدقائق الثلاث.", 16, false);
        desc.setGravity(Gravity.CENTER); desc.setPadding(0,dp(14),0,dp(18)); box.addView(desc, lp());
        TextView method = tv("المدينة: الرياض • طريقة الحساب: أم القرى • التوقيت: Asia/Riyadh", 14, false);
        method.setTextColor(Color.DKGRAY); method.setGravity(Gravity.CENTER); box.addView(method, lp());

        status = tv("", 17, true); status.setGravity(Gravity.CENTER); status.setPadding(0,dp(22),0,dp(8)); box.addView(status, lp());
        nextPrayer = tv("", 18, true); nextPrayer.setGravity(Gravity.CENTER); nextPrayer.setPadding(0,0,0,dp(18)); box.addView(nextPrayer, lp());

        Button overlay = btn("1) السماح بالظهور فوق التطبيقات");
        overlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) { toast("الصلاحية مفعلة بالفعل"); return; }
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        }); box.addView(overlay, blp());

        Button exact = btn("2) السماح بالمنبّهات الدقيقة");
        exact.setOnClickListener(v -> {
            if (PrayerScheduler.canScheduleExactAlarms(this)) { toast("الصلاحية مفعلة بالفعل"); return; }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            }
        }); box.addView(exact, blp());

        if (Build.VERSION.SDK_INT >= 33) {
            Button notify = btn("3) السماح بالإشعارات");
            notify.setOnClickListener(v -> {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                else toast("الإشعارات مفعلة بالفعل");
            }); box.addView(notify, blp());
        }

        Button activate = btn("تفعيل وجدولة التذكيرات");
        activate.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) { toast("فعّل صلاحية الظهور فوق التطبيقات أولاً"); return; }
            if (!PrayerScheduler.canScheduleExactAlarms(this)) { toast("فعّل المنبهات الدقيقة أولاً"); return; }
            PrayerScheduler.scheduleUpcoming(this); refresh(); toast("تم تفعيل وجدولة التذكيرات");
        }); box.addView(activate, blp());

        Button test = btn("اختبار الشاشة والنغمة الآن - 15 ثانية");
        test.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) { toast("فعّل صلاحية الظهور فوق التطبيقات أولاً"); return; }
            Intent i = new Intent(this, PrayerOverlayService.class)
                    .putExtra(PrayerOverlayService.EXTRA_PRAYER_NAME,"اختبار")
                    .putExtra(PrayerOverlayService.EXTRA_PRAYER_TIME,System.currentTimeMillis()+15_000L)
                    .putExtra(PrayerOverlayService.EXTRA_TEST_MODE,true);
            try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
            catch (Exception e) { toast("تعذر تشغيل الاختبار"); }
        }); box.addView(test, blp());

        TextView hint = tv("ملاحظة: يمكنك لمس الشاشة البيضاء لإغلاقها. التطبيق لا يحتاج GPS ولا إنترنت بعد التثبيت.", 13, false);
        hint.setTextColor(Color.GRAY); hint.setGravity(Gravity.CENTER); hint.setPadding(0,dp(14),0,0); box.addView(hint, lp());
        scroll.addView(box); return scroll;
    }

    private void refresh() {
        boolean ok = Settings.canDrawOverlays(this) && PrayerScheduler.canScheduleExactAlarms(this);
        status.setText(ok ? "الحالة: جاهز للعمل ✓" : "الحالة: يلزم منح الصلاحيات المطلوبة");
        status.setTextColor(ok ? Color.rgb(20,110,55) : Color.rgb(160,45,45));
        PrayerScheduler.NextPrayer n = PrayerScheduler.findNextPrayer();
        if (n != null) {
            SimpleDateFormat f = new SimpleDateFormat("hh:mm a", new Locale("ar","SA"));
            f.setTimeZone(TimeZone.getTimeZone("Asia/Riyadh"));
            nextPrayer.setText("الصلاة القادمة: " + n.name + " • " + f.format(new Date(n.timeMillis)));
        }
    }

    private TextView tv(String s, float sp, boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.BLACK); if(bold)t.setTypeface(t.getTypeface(),1); return t; }
    private Button btn(String s) { Button b=new Button(this); b.setText(s); b.setTextSize(16); return b; }
    private LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1,-2); }
    private LinearLayout.LayoutParams blp() { LinearLayout.LayoutParams p=lp(); p.setMargins(0,dp(5),0,dp(5)); return p; }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
