package com.youssef.prayeroverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PrayerOverlayService extends Service {
    public static final String EXTRA_PRAYER_NAME="prayer_name";
    public static final String EXTRA_PRAYER_TIME="prayer_time";
    public static final String EXTRA_TEST_MODE="test_mode";
    private static final String CHANNEL_ID="prayer_overlay_active";
    private static final int NOTIFICATION_ID=1001;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private View overlay;
    private PowerManager.WakeLock wakeLock;
    private AudioTrack audioTrack;
    private Thread toneThread;

    @Override public void onCreate(){
        super.onCreate();
        createChannel();
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        clearSession();
        String name=intent!=null?intent.getStringExtra(EXTRA_PRAYER_NAME):"الصلاة";
        long prayerTime=intent!=null?intent.getLongExtra(EXTRA_PRAYER_TIME,System.currentTimeMillis()+180000L):System.currentTimeMillis()+180000L;
        boolean test=intent!=null&&intent.getBooleanExtra(EXTRA_TEST_MODE,false);
        startForeground(NOTIFICATION_ID,notification(name==null?"الصلاة":name));
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}

        long now=System.currentTimeMillis();
        long duration=Math.max(1000L,prayerTime-now);
        if(test) duration=Math.min(duration,15000L);
        acquireWakeLock(duration+5000L);
        showOverlay();
        playTone();
        if(!test){
            long d2=(prayerTime-120000L)-now;
            long d1=(prayerTime-60000L)-now;
            if(d2>0) handler.postDelayed(this::playTone,d2);
            if(d1>0) handler.postDelayed(this::playTone,d1);
        }
        handler.postDelayed(this::stopSelf,duration);
        return START_NOT_STICKY;
    }

    private void showOverlay(){
        if(overlay!=null||wm==null)return;
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setOnClickListener(v->stopSelf());
        TextView text=new TextView(this);
        text.setText("ما عند الله خير وأبقى");
        text.setTextColor(Color.BLACK);
        text.setTextSize(32f);
        text.setGravity(Gravity.CENTER);
        text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        text.setTypeface(text.getTypeface(),android.graphics.Typeface.BOLD);
        text.setPadding(32,32,32,32);
        root.addView(text,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));

        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
                -1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE);
        p.gravity=Gravity.TOP|Gravity.START;
        overlay=root;
        try{wm.addView(overlay,p);}catch(Exception e){overlay=null;}
    }

    private synchronized void playTone(){
        stopTone();
        toneThread=new Thread(()->{
            final int rate=22050,total=rate*10;
            short[] pcm=new short[total];
            for(int i=0;i<total;i++){
                double sec=i/(double)rate;
                double phase=sec%2.0;
                double env=phase<0.65?Math.sin(Math.PI*phase/0.65):0.0;
                double freq=((int)(sec/2.0)%2==0)?523.25:659.25;
                double value=Math.sin(2.0*Math.PI*freq*sec)*env*0.12;
                pcm[i]=(short)(value*Short.MAX_VALUE);
            }
            try{
                AudioAttributes attrs=new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                AudioFormat format=new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
                AudioTrack track=new AudioTrack(attrs,format,pcm.length*2,AudioTrack.MODE_STATIC,AudioTrack.AUDIO_SESSION_ID_GENERATE);
                synchronized(PrayerOverlayService.this){audioTrack=track;}
                track.write(pcm,0,pcm.length);
                track.setVolume(0.45f);
                track.play();
                Thread.sleep(10000L);
            }catch(Exception ignored){}finally{
                synchronized(PrayerOverlayService.this){
                    if(audioTrack!=null){
                        try{audioTrack.stop();}catch(Exception ignored){}
                        try{audioTrack.release();}catch(Exception ignored){}
                        audioTrack=null;
                    }
                    toneThread=null;
                }
            }
        },"PrayerGentleTone");
        toneThread.start();
    }

    private Notification notification(String prayerName){
        PendingIntent pi=PendingIntent.getActivity(this,11,new Intent(this,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("تذكير الصلاة")
                .setContentText("باقٍ أقل من 3 دقائق على صلاة "+prayerName)
                .setContentIntent(pi).setOngoing(true).setCategory(Notification.CATEGORY_REMINDER).build();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CHANNEL_ID,"تذكير الصلاة النشط",NotificationManager.IMPORTANCE_LOW);
            c.setSound(null,null);
            c.setDescription("إشعار يظهر أثناء شاشة التذكير قبل الصلاة");
            NotificationManager m=getSystemService(NotificationManager.class);
            if(m!=null)m.createNotificationChannel(c);
        }
    }

    private void acquireWakeLock(long timeout){
        releaseWakeLock();
        PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
        if(pm==null)return;
        wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PrayerOverlay:ThreeMinuteWindow");
        wakeLock.setReferenceCounted(false);
        try{wakeLock.acquire(timeout);}catch(Exception ignored){}
    }

    private synchronized void stopTone(){
        if(audioTrack!=null){
            try{audioTrack.stop();}catch(Exception ignored){}
            try{audioTrack.release();}catch(Exception ignored){}
            audioTrack=null;
        }
        if(toneThread!=null){toneThread.interrupt();toneThread=null;}
    }
    private void removeOverlay(){
        if(overlay!=null&&wm!=null){try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;}
    }
    private void releaseWakeLock(){
        if(wakeLock!=null){try{if(wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}wakeLock=null;}
    }
    private void clearSession(){
        handler.removeCallbacksAndMessages(null);
        stopTone(); removeOverlay(); releaseWakeLock();
    }
    @Override public void onDestroy(){
        clearSession();
        try{stopForeground(STOP_FOREGROUND_REMOVE);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
