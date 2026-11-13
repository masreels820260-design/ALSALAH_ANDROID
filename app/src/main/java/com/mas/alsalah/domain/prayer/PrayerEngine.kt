package com.mas.alsalah.domain.prayer

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

/** الصلوات بالترتيب الزمني. notifIndex = ترتيب الصلاة في إعدادات الإشعارات (الشروق = -1). */
enum class Prayer(val ar: String, val isPrayer: Boolean, val notifIndex: Int) {
    FAJR("الفجر", true, 0),
    SUNRISE("الشروق", false, -1),
    DHUHR("الظهر", true, 1),
    ASR("العصر", true, 2),
    MAGHRIB("المغرب", true, 3),
    ISHA("العشاء", true, 4);

    companion object {
        /** الصلوات الخمس بترتيب فهرس الإشعارات. */
        val FIVE: List<Prayer> = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

/** طرق الحساب: زاويتا الفجر والعشاء. الطريقة الأولى هي الهيئة المصرية العامة للمساحة. */
enum class CalcMethod(val ar: String, val fajr: Double, val isha: Double) {
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0),
    KARACHI("جامعة العلوم الإسلامية - كراتشي", 18.0, 18.0),
    ISNA("أمريكا الشمالية (ISNA)", 15.0, 15.0)
}

/** corr: تصحيح يدوي بالدقائق بترتيب Prayer.entries (6 قيم). asrFactor: 1 شافعي، 2 حنفي. */
class CalcParams(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val asrFactor: Int = 1,
    val corr: List<Int> = List(6) { 0 },
    val sunriseAltitude: Double = -0.833
) {
    companion object {
        fun of(method: CalcMethod, asrFactor: Int, corr: List<Int>) =
            CalcParams(method.fajr, method.isha, asrFactor, corr)
    }
}

/** أوقات يوم واحد بالميلي ثانية (UTC epoch). القيمة -1 تعني أنها غير قابلة للحساب. */
class DayTimes(val date: LocalDate, private val t: LongArray) {
    fun at(p: Prayer): Long = t[p.ordinal]
}

object PrayerEngine {

    fun compute(date: LocalDate, lat: Double, lon: Double, zone: ZoneId, p: CalcParams): DayTimes {
        val noon = date.atTime(12, 0).atZone(zone)
        val offsetSec = zone.rules.getOffset(noon.toInstant()).totalSeconds
        val tz = offsetSec / 3600.0

        // اليوليانية عند الظهر الشمسي المحلي التقريبي
        val jd = date.toEpochDay() + 2440588.0 - lon / 360.0
        val d = jd - 2451545.0

        val g = norm360(357.529 + 0.98560028 * d)
        val q = norm360(280.459 + 0.98564736 * d)
        val gr = Math.toRadians(g)
        val l = norm360(q + 1.915 * sin(gr) + 0.020 * sin(2 * gr))
        val lr = Math.toRadians(l)
        val e = 23.439 - 0.00000036 * d
        val er = Math.toRadians(e)
        var ra = Math.toDegrees(atan2(cos(er) * sin(lr), cos(lr)))
        ra = norm360(ra) / 15.0
        var eqt = q / 15.0 - ra
        if (eqt > 12.0) eqt -= 24.0 else if (eqt < -12.0) eqt += 24.0
        val decl = asin(sin(er) * sin(lr))

        val noonH = 12.0 + tz - lon / 15.0 - eqt
        val latR = Math.toRadians(lat)

        fun hourAngle(alt: Double): Double {
            val x = (sin(Math.toRadians(alt)) - sin(latR) * sin(decl)) / (cos(latR) * cos(decl))
            return if (x < -1.0 || x > 1.0) Double.NaN else Math.toDegrees(acos(x)) / 15.0
        }

        val asrAlt = Math.toDegrees(atan(1.0 / (p.asrFactor + tan(abs(latR - decl)))))
        val base = date.toEpochDay() * 86400L - offsetSec

        fun ms(h: Double, c: Int): Long =
            if (h.isNaN()) -1L else (base + Math.round(h * 60.0) * 60L + c * 60L) * 1000L

        val out = LongArray(6)
        out[Prayer.FAJR.ordinal] = ms(noonH - hourAngle(-p.fajrAngle), p.corr.getOrElse(0) { 0 })
        out[Prayer.SUNRISE.ordinal] = ms(noonH - hourAngle(p.sunriseAltitude), p.corr.getOrElse(1) { 0 })
        out[Prayer.DHUHR.ordinal] = ms(noonH, p.corr.getOrElse(2) { 0 })
        out[Prayer.ASR.ordinal] = ms(noonH + hourAngle(asrAlt), p.corr.getOrElse(3) { 0 })
        out[Prayer.MAGHRIB.ordinal] = ms(noonH + hourAngle(p.sunriseAltitude), p.corr.getOrElse(4) { 0 })
        out[Prayer.ISHA.ordinal] = ms(noonH + hourAngle(-p.ishaAngle), p.corr.getOrElse(5) { 0 })
        return DayTimes(date, out)
    }

    private fun norm360(v: Double): Double {
        val r = v % 360.0
        return if (r < 0) r + 360.0 else r
    }
}

data class PrayerEvent(val prayer: Prayer, val millis: Long)
data class NextInfo(val next: PrayerEvent, val previous: PrayerEvent?)

object PrayerTimeline {
    /** أمس واليوم وغدًا وبعد غد، لمعالجة منتصف الليل بشكل صحيح. */
    fun window(center: LocalDate, lat: Double, lon: Double, zone: ZoneId, p: CalcParams): List<DayTimes> =
        (-1..2).map { PrayerEngine.compute(center.plusDays(it.toLong()), lat, lon, zone, p) }

    fun events(days: List<DayTimes>): List<PrayerEvent> =
        days.flatMap { d -> Prayer.FIVE.mapNotNull { pr -> d.at(pr).takeIf { it >= 0 }?.let { PrayerEvent(pr, it) } } }
            .sortedBy { it.millis }

    fun nextPrevious(events: List<PrayerEvent>, now: Long): NextInfo? {
        val i = events.indexOfFirst { it.millis > now }
        if (i < 0) return null
        return NextInfo(events[i], events.getOrNull(i - 1))
    }
}
