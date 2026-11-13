package com.mas.alsalah.domain.location

import kotlin.math.*

/**
 * المحافظات المصرية الـ 27 بإحداثيات عاصمة كل محافظة (تقريبية على مستوى المدينة).
 * المراكز والقرى غير مضمّنة لعدم توفر مجموعة بيانات موثّقة دون اتصال؛ استخدم GPS أو الإحداثيات اليدوية لها.
 */
data class Gov(val id: Int, val name: String, val city: String, val lat: Double, val lon: Double) {
    val label: String get() = if (city == name) name else "$name ($city)"
}

object EgyptLocations {
    val governorates: List<Gov> = listOf(
        Gov(0, "القاهرة", "القاهرة", 30.0444, 31.2357),
        Gov(1, "الجيزة", "الجيزة", 30.0131, 31.2089),
        Gov(2, "الإسكندرية", "الإسكندرية", 31.2001, 29.9187),
        Gov(3, "القليوبية", "بنها", 30.4660, 31.1850),
        Gov(4, "بورسعيد", "بورسعيد", 31.2653, 32.3019),
        Gov(5, "السويس", "السويس", 29.9668, 32.5498),
        Gov(6, "دمياط", "دمياط", 31.4165, 31.8133),
        Gov(7, "الدقهلية", "المنصورة", 31.0409, 31.3785),
        Gov(8, "الشرقية", "الزقازيق", 30.5877, 31.5020),
        Gov(9, "الغربية", "طنطا", 30.7865, 31.0004),
        Gov(10, "المنوفية", "شبين الكوم", 30.5972, 30.9876),
        Gov(11, "كفر الشيخ", "كفر الشيخ", 31.1107, 30.9388),
        Gov(12, "البحيرة", "دمنهور", 31.0341, 30.4682),
        Gov(13, "الإسماعيلية", "الإسماعيلية", 30.5965, 32.2715),
        Gov(14, "بني سويف", "بني سويف", 29.0661, 31.0994),
        Gov(15, "الفيوم", "الفيوم", 29.3084, 30.8428),
        Gov(16, "المنيا", "المنيا", 28.1099, 30.7503),
        Gov(17, "أسيوط", "أسيوط", 27.1809, 31.1837),
        Gov(18, "سوهاج", "سوهاج", 26.5569, 31.6948),
        Gov(19, "قنا", "قنا", 26.1551, 32.7160),
        Gov(20, "الأقصر", "الأقصر", 25.6872, 32.6396),
        Gov(21, "أسوان", "أسوان", 24.0889, 32.8998),
        Gov(22, "البحر الأحمر", "الغردقة", 27.2579, 33.8116),
        Gov(23, "الوادي الجديد", "الخارجة", 25.4390, 30.5586),
        Gov(24, "مطروح", "مرسى مطروح", 31.3543, 27.2373),
        Gov(25, "شمال سيناء", "العريش", 31.1316, 33.7984),
        Gov(26, "جنوب سيناء", "الطور", 28.2417, 33.6222)
    )

    fun byId(id: Int): Gov? = governorates.firstOrNull { it.id == id }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    fun nearest(lat: Double, lon: Double): Pair<Gov, Double> =
        governorates.map { it to distanceKm(lat, lon, it.lat, it.lon) }.minBy { it.second }
}
