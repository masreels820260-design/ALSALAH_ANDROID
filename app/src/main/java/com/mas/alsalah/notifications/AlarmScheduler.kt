package com.mas.alsalah.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mas.alsalah.data.SettingsStore
import com.mas.alsalah.data.params
import com.mas.alsalah.data.zone
import com.mas.alsalah.domain.prayer.Prayer
import com.mas.alsalah.domain.prayer.PrayerEngine
import com.mas.alsalah.widgets.WidgetUpdater
import java.time.LocalDate

object AlarmScheduler {
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_TYPE = "type"
    private const val HORIZON_DAYS = 3

    /**
     * يلغي ويعيد جدولة تنبيهات الأيام القادمة (٣ أيام) + منبّه تحديث الـ widgets.
     * يُستدعى عند: فتح التطبيق، تغيّر الإعدادات/الموقع، إطلاق أي تنبيه، إعادة التشغيل، تغيّر الوقت/المنطقة، تحديث التطبيق.
     */
    @Synchronized
    fun rescheduleAll(ctx: Context) {
        val s = SettingsStore.get(ctx).state.value
        val zone = s.zone()
        val params = s.params()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)

        for (d in -1..HORIZON_DAYS + 1) {
            val day = today.plusDays(d.toLong()).toEpochDay()
            for (i in 0..4) for (t in 0..1) cancel(ctx, am, AlarmCodes.code(day, i, t))
        }
        cancel(ctx, am, AlarmCodes.WIDGET_CODE)

        var nextAny = Long.MAX_VALUE
        for (d in 0..HORIZON_DAYS) {
            val date = today.plusDays(d.toLong())
            val times = PrayerEngine.compute(date, s.lat, s.lon, zone, params)
            val day = date.toEpochDay()
            for (i in 0..4) {
                val at = times.at(Prayer.FIVE[i])
                if (at < 0) continue
                if (at > now && at < nextAny) nextAny = at
                val n = s.notif[i]
                if (n.atEnabled && at > now) schedule(ctx, am, AlarmCodes.code(day, i, 1), at, i, 1)
                if (n.preEnabled && n.preMinutes > 0) {
                    val pre = at - n.preMinutes * 60_000L
                    if (pre > now) schedule(ctx, am, AlarmCodes.code(day, i, 0), pre, i, 0)
                }
            }
        }
        // منبّه يوقظ التطبيق عند أقرب صلاة لتحديث الـ widgets حتى لو كانت كل الإشعارات معطّلة
        if (nextAny != Long.MAX_VALUE) schedule(ctx, am, AlarmCodes.WIDGET_CODE, nextAny + 1500, 0, 2)
        WidgetUpdater.updateAll(ctx)
    }

    private fun intent(ctx: Context) = Intent(ctx, PrayerAlarmReceiver::class.java)

    private fun cancel(ctx: Context, am: AlarmManager, code: Int) {
        PendingIntent.getBroadcast(ctx, code, intent(ctx), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { am.cancel(it); it.cancel() }
    }

    private fun schedule(ctx: Context, am: AlarmManager, code: Int, at: Long, idx: Int, type: Int) {
        val pi = PendingIntent.getBroadcast(
            ctx, code, intent(ctx).putExtra(EXTRA_PRAYER, idx).putExtra(EXTRA_TYPE, type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
