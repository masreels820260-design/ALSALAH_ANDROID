package com.mas.alsalah

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

object AlarmScheduler {
    fun schedule(context: Context, lat: Double, lon: Double, leadMinutes: Int, enabled: BooleanArray) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val names = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

        // Keep a rolling three-day window. Each date uses its own UTC offset so DST changes are respected.
        (0..2).forEach { dayOffset ->
            val date = LocalDate.now(zone).plusDays(dayOffset.toLong())
            val noonInstant = date.atTime(12, 0).atZone(zone).toInstant()
            val offsetHours = zone.rules.getOffset(noonInstant).totalSeconds / 3600.0
            val times = PrayerTimes.calculate(date, lat, lon, offsetHours)
            val values = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
            values.forEachIndexed { index, decimalHours ->
                if (!enabled.getOrElse(index) { true }) return@forEachIndexed
                val minuteOfDay = PrayerTimes.minutes(decimalHours)
                if (minuteOfDay < 0) return@forEachIndexed
                val prayerAt = date.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()
                if (prayerAt <= now) return@forEachIndexed
                val dayKey = date.dayOfYear
                scheduleOne(context, manager, prayerAt - leadMinutes.coerceIn(0, 120) * 60_000L,
                    names[index], true, 10_000 + dayKey * 20 + index)
                scheduleOne(context, manager, prayerAt, names[index], false, 20_000 + dayKey * 20 + index)
            }
        }
    }

    private fun scheduleOne(context: Context, manager: AlarmManager, at: Long, prayer: String, before: Boolean, requestCode: Int) {
        if (at <= System.currentTimeMillis()) return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer", prayer)
            putExtra("before", before)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
}
