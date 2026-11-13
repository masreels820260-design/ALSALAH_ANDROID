package com.mas.alsalah.notifications

/** رموز طلبات PendingIntent ثابتة وفريدة لكل (يوم، صلاة، نوع). type: 0 قبل الصلاة، 1 عند الصلاة. */
object AlarmCodes {
    const val WIDGET_CODE = 1
    fun code(epochDay: Long, prayerIndex: Int, type: Int): Int = epochDay.toInt() * 16 + prayerIndex * 2 + type
}
