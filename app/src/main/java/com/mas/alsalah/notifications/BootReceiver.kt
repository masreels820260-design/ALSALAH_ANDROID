package com.mas.alsalah.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** إعادة الجدولة بعد: إعادة التشغيل، تغيير الوقت/التاريخ/المنطقة، تحديث التطبيق، تغيّر إذن المنبّه الدقيق. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.ensureChannels(context)
        AlarmScheduler.rescheduleAll(context)
    }
}
