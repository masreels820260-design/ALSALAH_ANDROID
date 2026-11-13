package com.mas.alsalah.domain.qibla

import kotlin.math.*

object Qibla {
    const val KAABA_LAT = 21.4225
    const val KAABA_LON = 39.8262

    /** الاتجاه الأولي (بالدرجات من الشمال الحقيقي) من نقطة المستخدم إلى الكعبة. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** أصغر فرق زاوي موقّع بين اتجاهين، في المدى [-180, 180]. */
    fun delta(a: Double, b: Double): Double = ((a - b + 540.0) % 360.0) - 180.0
}
