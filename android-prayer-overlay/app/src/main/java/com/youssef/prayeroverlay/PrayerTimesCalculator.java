package com.youssef.prayeroverlay;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class PrayerTimesCalculator {
    private PrayerTimesCalculator() {}
    public static final double RIYADH_LATITUDE = 24.7136;
    public static final double RIYADH_LONGITUDE = 46.6753;

    public static List<PrayerMoment> getPrayerMoments(LocalDate date) {
        Coordinates coordinates = new Coordinates(RIYADH_LATITUDE, RIYADH_LONGITUDE);
        DateComponents dc = new DateComponents(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        CalculationParameters params = CalculationMethod.UMM_AL_QURA.getParameters();
        params.madhab = Madhab.SHAFI;
        HijrahDate hijrah = HijrahDate.from(date);
        if (hijrah.get(ChronoField.MONTH_OF_YEAR) == 9) params.adjustments.isha = 30;

        PrayerTimes t = new PrayerTimes(coordinates, dc, params);
        List<PrayerMoment> r = new ArrayList<>(5);
        r.add(new PrayerMoment("الفجر", t.fajr));
        r.add(new PrayerMoment("الظهر", t.dhuhr));
        r.add(new PrayerMoment("العصر", t.asr));
        r.add(new PrayerMoment("المغرب", t.maghrib));
        r.add(new PrayerMoment("العشاء", t.isha));
        return r;
    }

    public static final class PrayerMoment {
        public final String name; public final Date time;
        public PrayerMoment(String name, Date time) { this.name=name; this.time=time; }
    }
}
