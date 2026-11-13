package com.mas.alsalah.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.mas.alsalah.MainActivity
import com.mas.alsalah.R
import com.mas.alsalah.data.params
import com.mas.alsalah.data.zone
import com.mas.alsalah.data.SettingsStore
import com.mas.alsalah.domain.prayer.Prayer
import com.mas.alsalah.domain.prayer.PrayerTimeline
import com.mas.alsalah.notifications.AlarmScheduler
import com.mas.alsalah.utils.Fmt
import java.time.Instant

/** تحديث الـ widgets: العد التنازلي يعمل بـ Chronometer على مستوى النظام بلا أي تحديث كل ثانية. */
object WidgetUpdater {
    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val small = mgr.getAppWidgetIds(ComponentName(ctx, SmallWidgetProvider::class.java))
        val medium = mgr.getAppWidgetIds(ComponentName(ctx, MediumWidgetProvider::class.java))
        val large = mgr.getAppWidgetIds(ComponentName(ctx, LargeWidgetProvider::class.java))
        if (small.isEmpty() && medium.isEmpty() && large.isEmpty()) return

        val s = SettingsStore.get(ctx).state.value
        val zone = s.zone()
        val now = System.currentTimeMillis()
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = PrayerTimeline.window(day, s.lat, s.lon, zone, s.params())
        val info = PrayerTimeline.nextPrevious(PrayerTimeline.events(days), now) ?: return
        val next = info.next
        val remain = next.millis - now
        val textC = ContextCompat.getColor(ctx, R.color.widget_text)
        val accent = ContextCompat.getColor(ctx, R.color.widget_accent)
        fun d(t: String) = Fmt.digits(t, s.arabicDigits)
        val name = "صلاة ${next.prayer.ar}"
        val time = d(Fmt.time(next.millis, zone, s.use24))

        fun countdown(v: RemoteViews) {
            v.setChronometerCountDown(R.id.w_count, true)
            v.setChronometer(R.id.w_count, SystemClock.elapsedRealtime() + remain, null, true)
        }
        fun click(v: RemoteViews, code: Int, route: String) {
            val pi = PendingIntent.getActivity(
                ctx, 900 + code, Intent(ctx, MainActivity::class.java).putExtra("route", route).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.widget_root, pi)
        }

        if (small.isNotEmpty()) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_small)
            v.setTextViewText(R.id.w_label, "الصلاة القادمة"); v.setTextViewText(R.id.w_name, name); v.setTextViewText(R.id.w_time, time)
            countdown(v); click(v, 1, "home")
            mgr.updateAppWidget(small, v)
        }
        if (medium.isNotEmpty()) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
            v.setTextViewText(R.id.w_label, "الصلاة القادمة"); v.setTextViewText(R.id.w_name, name); v.setTextViewText(R.id.w_time, time)
            v.setTextViewText(R.id.w_place, s.placeName)
            countdown(v); click(v, 2, "location")
            mgr.updateAppWidget(medium, v)
        }
        if (large.isNotEmpty()) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_large)
            v.setTextViewText(R.id.w_label, "الصلاة القادمة — ${s.placeName}")
            v.setTextViewText(R.id.w_name, name)
            countdown(v); click(v, 3, "home")
            val today = days[1]
            val nameIds = intArrayOf(R.id.r0n, R.id.r1n, R.id.r2n, R.id.r3n, R.id.r4n, R.id.r5n)
            val timeIds = intArrayOf(R.id.r0t, R.id.r1t, R.id.r2t, R.id.r3t, R.id.r4t, R.id.r5t)
            Prayer.entries.forEachIndexed { i, p ->
                val isNext = p == next.prayer
                val ms = if (isNext) next.millis else today.at(p)
                val c = if (isNext) accent else textC
                v.setTextViewText(nameIds[i], p.ar); v.setTextViewText(timeIds[i], d(Fmt.time(ms, zone, s.use24)))
                v.setTextColor(nameIds[i], c); v.setTextColor(timeIds[i], c)
            }
            mgr.updateAppWidget(large, v)
        }
    }
}

open class BaseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AlarmScheduler.rescheduleAll(context)
    }
    override fun onEnabled(context: Context) { AlarmScheduler.rescheduleAll(context) }
}
class SmallWidgetProvider : BaseWidgetProvider()
class MediumWidgetProvider : BaseWidgetProvider()
class LargeWidgetProvider : BaseWidgetProvider()
