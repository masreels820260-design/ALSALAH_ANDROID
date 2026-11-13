package com.mas.alsalah.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getIntExtra(AlarmScheduler.EXTRA_TYPE, 2)
        val idx = intent.getIntExtra(AlarmScheduler.EXTRA_PRAYER, 0)
        if (type == 0 || type == 1) NotificationHelper.fire(context, idx, type)
        AlarmScheduler.rescheduleAll(context) // جدولة متدحرجة + تحديث الـ widgets
    }
}
