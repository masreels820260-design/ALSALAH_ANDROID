package com.mas.alsalah.utils

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object Fmt {
    private val AR = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val WEEKDAYS = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد") // DayOfWeek.value-1
    val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

    fun digits(s: String, arabic: Boolean): String {
        if (!arabic) return s
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') AR[c - '0'] else c)
        return sb.toString()
    }

    fun time(millis: Long, zone: ZoneId, use24: Boolean): String {
        if (millis < 0) return "--:--"
        val t = Instant.ofEpochMilli(millis).atZone(zone)
        return if (use24) String.format(Locale.US, "%02d:%02d", t.hour, t.minute)
        else {
            val h = if (t.hour % 12 == 0) 12 else t.hour % 12
            String.format(Locale.US, "%d:%02d %s", h, t.minute, if (t.hour < 12) "ص" else "م")
        }
    }

    fun countdown(ms: Long): String {
        val s = (ms.coerceAtLeast(0)) / 1000
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    fun duration(ms: Long): String {
        val m = ms.coerceAtLeast(0) / 60000
        val h = m / 60
        val r = m % 60
        return if (h > 0) "$h س $r د" else "$r د"
    }

    fun date(d: java.time.LocalDate): String =
        "${WEEKDAYS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"
}
