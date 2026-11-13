package com.mas.alsalah.domain.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class Occasion(val month: Int, val day: Int, val name: String, val note: String)
data class UpcomingOccasion(val date: LocalDate, val name: String, val note: String, val daysAway: Long)

/** التقويم الهجري الحسابي (أم القرى) مع تعديل يدوي ±2 يوم. */
object HijriCalendar {
    val MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    val OCCASIONS = listOf(
        Occasion(1, 1, "رأس السنة الهجرية", "بداية العام الهجري الجديد."),
        Occasion(1, 10, "يوم عاشوراء", "يُستحب صيامه، ويُستحب صيام يوم قبله أو بعده."),
        Occasion(3, 12, "المولد النبوي", "على المشهور، وتحديد اليوم محل خلاف بين أهل العلم."),
        Occasion(7, 27, "ذكرى الإسراء والمعراج", "على المشهور، ولم يثبت تحديد ليلتها بدليل صحيح."),
        Occasion(9, 1, "بداية شهر رمضان", "يتحدد فعليًا بالرؤية الشرعية بحسب إعلان دار الإفتاء."),
        Occasion(9, 21, "بداية العشر الأواخر من رمضان", "تُلتمس ليلة القدر في الوتر من العشر الأواخر."),
        Occasion(10, 1, "عيد الفطر", "يتحدد بالرؤية الشرعية."),
        Occasion(12, 9, "يوم عرفة", "يُستحب صيامه لغير الحاج."),
        Occasion(12, 10, "عيد الأضحى", "يتحدد بالرؤية الشرعية."),
        Occasion(12, 11, "أيام التشريق", "أيام التشريق: ١١ و١٢ و١٣ من ذي الحجة.")
    )

    fun hijri(date: LocalDate, adjust: Int): HijrahDate = HijrahDate.from(date.plusDays(adjust.toLong()))

    fun year(h: HijrahDate) = h.get(ChronoField.YEAR_OF_ERA)
    fun month(h: HijrahDate) = h.get(ChronoField.MONTH_OF_YEAR)
    fun day(h: HijrahDate) = h.get(ChronoField.DAY_OF_MONTH)

    fun format(date: LocalDate, adjust: Int): String {
        val h = hijri(date, adjust)
        return "${day(h)} ${MONTHS[month(h) - 1]} ${year(h)} هـ"
    }

    fun occasionsOn(month: Int, day: Int): List<Occasion> =
        OCCASIONS.filter { it.month == month && it.day == day }

    fun isWhiteDay(day: Int) = day in 13..15

    fun upcoming(from: LocalDate, adjust: Int, limit: Int): List<UpcomingOccasion> {
        val out = ArrayList<UpcomingOccasion>()
        for (i in 0..420) {
            val date = from.plusDays(i.toLong())
            val h = hijri(date, adjust)
            for (o in occasionsOn(month(h), day(h))) {
                out += UpcomingOccasion(date, o.name, o.note, i.toLong())
            }
            if (out.size >= limit) break
        }
        return out
    }
}
