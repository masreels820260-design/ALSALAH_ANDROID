package com.mas.alsalah

import com.mas.alsalah.domain.location.EgyptLocations
import com.mas.alsalah.domain.prayer.*
import com.mas.alsalah.domain.qibla.Qibla
import com.mas.alsalah.notifications.AlarmCodes
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * اختبارات سلامة منطقية (sanity) للحساب الفلكي — وليست مطابقة رسمية لجداول الهيئة المصرية.
 * للمطابقة الرسمية أضف صفوفًا من جدول الهيئة لمدينة وتاريخ وقارنها بتسامح دقيقة أو دقيقتين.
 */
class PrayerEngineTest {
    private val cairo = ZoneId.of("Africa/Cairo")
    private val egypt = CalcParams.of(CalcMethod.EGYPT, 1, List(6) { 0 })

    private fun minutes(ms: Long, zone: ZoneId): Int {
        val t = Instant.ofEpochMilli(ms).atZone(zone)
        return t.hour * 60 + t.minute
    }
    private fun hm(h: Int, m: Int) = h * 60 + m

    @Test fun cairoNovemberRanges() {
        val d = PrayerEngine.compute(LocalDate.of(2026, 11, 13), 30.0444, 31.2357, cairo, egypt)
        assertTrue(minutes(d.at(Prayer.FAJR), cairo) in hm(4, 40)..hm(5, 0))
        assertTrue(minutes(d.at(Prayer.SUNRISE), cairo) in hm(6, 10)..hm(6, 25))
        assertTrue(minutes(d.at(Prayer.DHUHR), cairo) in hm(11, 35)..hm(11, 45))
        assertTrue(minutes(d.at(Prayer.MAGHRIB), cairo) in hm(16, 55)..hm(17, 8))
    }

    @Test fun cairoSummerUsesDst() {
        val d = PrayerEngine.compute(LocalDate.of(2026, 6, 21), 30.0444, 31.2357, cairo, egypt)
        assertTrue(minutes(d.at(Prayer.DHUHR), cairo) in hm(12, 50)..hm(13, 5))
    }

    @Test fun dstJumpIsOneHour() {
        val a = PrayerEngine.compute(LocalDate.of(2026, 4, 23), 30.0444, 31.2357, cairo, egypt)
        val b = PrayerEngine.compute(LocalDate.of(2026, 4, 26), 30.0444, 31.2357, cairo, egypt)
        val diff = minutes(b.at(Prayer.DHUHR), cairo) - minutes(a.at(Prayer.DHUHR), cairo)
        assertTrue("diff=$diff", diff in 55..65)
    }

    @Test fun orderedForAllGovernoratesAllMonths() {
        for (g in EgyptLocations.governorates) for (m in 1..12) {
            val d = PrayerEngine.compute(LocalDate.of(2026, m, 15), g.lat, g.lon, cairo, egypt)
            val t = Prayer.entries.map { d.at(it) }
            assertTrue("${g.name} $m has missing", t.all { it > 0 })
            assertEquals("${g.name} $m order", t.sorted(), t)
        }
    }

    @Test fun hanafiAsrIsLater() {
        val s = PrayerEngine.compute(LocalDate.of(2026, 11, 13), 30.0, 31.2, cairo, egypt)
        val h = PrayerEngine.compute(LocalDate.of(2026, 11, 13), 30.0, 31.2, cairo, CalcParams.of(CalcMethod.EGYPT, 2, List(6) { 0 }))
        assertTrue(h.at(Prayer.ASR) > s.at(Prayer.ASR))
    }

    @Test fun correctionShiftsExactly() {
        val base = PrayerEngine.compute(LocalDate.of(2026, 11, 13), 30.0, 31.2, cairo, egypt)
        val corr = PrayerEngine.compute(LocalDate.of(2026, 11, 13), 30.0, 31.2, cairo, CalcParams.of(CalcMethod.EGYPT, 1, listOf(0, 0, 5, 0, 0, 0)))
        assertEquals(5 * 60_000L, corr.at(Prayer.DHUHR) - base.at(Prayer.DHUHR))
    }

    @Test fun timelineCrossesMidnight() {
        val day = LocalDate.of(2026, 11, 13)
        val days = PrayerTimeline.window(day, 30.0444, 31.2357, cairo, egypt)
        val events = PrayerTimeline.events(days)
        val isha = days[1].at(Prayer.ISHA)
        val info = PrayerTimeline.nextPrevious(events, isha + 60_000)!!
        assertEquals(Prayer.FAJR, info.next.prayer)
        assertEquals(days[2].at(Prayer.FAJR), info.next.millis)
        assertEquals(Prayer.ISHA, info.previous!!.prayer)
        val atFajr = PrayerTimeline.nextPrevious(events, days[1].at(Prayer.FAJR) - 1)!!
        assertEquals(Prayer.FAJR, atFajr.next.prayer)
    }

    @Test fun qiblaBearings() {
        assertEquals(136.2, Qibla.bearing(30.0444, 31.2357), 1.0)
        assertEquals(118.9, Qibla.bearing(51.5074, -0.1278), 1.0)
        assertEquals(0.0, Math.abs(Qibla.delta(10.0, 370.0)), 1e-9)
    }

    @Test fun alarmCodesAreUnique() {
        val seen = HashSet<Int>()
        for (day in 20400L..20420L) for (i in 0..4) for (t in 0..1) assertTrue(seen.add(AlarmCodes.code(day, i, t)))
        assertFalse(seen.contains(AlarmCodes.WIDGET_CODE))
    }

    @Test fun twentySevenGovernorates() {
        assertEquals(27, EgyptLocations.governorates.size)
        assertEquals(27, EgyptLocations.governorates.map { it.id }.toSet().size)
    }
}
