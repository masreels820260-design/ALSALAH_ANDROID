package com.mas.alsalah.data

import android.content.Context
import com.mas.alsalah.domain.prayer.CalcMethod
import com.mas.alsalah.domain.prayer.CalcParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

const val SOUND_TAKBEER = 0
const val SOUND_TONE = 1
const val SOUND_SILENT = 2

data class PrayerNotif(
    val preEnabled: Boolean = true,
    val preMinutes: Int = 15,
    val atEnabled: Boolean = true,
    val sound: Int = SOUND_TAKBEER,
    val vibrate: Boolean = true
)

data class Settings(
    val theme: Int = 0,               // 0 النظام، 1 فاتح، 2 داكن
    val use24: Boolean = false,
    val arabicDigits: Boolean = true,
    val zoneMode: Int = 0,            // 0 توقيت الجهاز التلقائي، 1 القاهرة دائمًا
    val lat: Double = 30.0444,
    val lon: Double = 31.2357,
    val placeName: String = "القاهرة",
    val locMode: Int = 0,             // 0 يدوي (محافظة)، 1 GPS، 2 إحداثيات مخصصة
    val govId: Int = 0,
    val method: Int = 0,
    val asr: Int = 1,                 // 1 شافعي، 2 حنفي
    val corr: List<Int> = List(6) { 0 },
    val notif: List<PrayerNotif> = List(5) { PrayerNotif() },
    val volume: Float = 1f,
    val haptics: Boolean = true,
    val hijriAdjust: Int = 0
)

fun Settings.params(): CalcParams =
    CalcParams.of(CalcMethod.entries[method.coerceIn(0, CalcMethod.entries.size - 1)], asr, corr)

fun Settings.zone(): ZoneId = if (zoneMode == 1) ZoneId.of("Africa/Cairo") else ZoneId.systemDefault()

class SettingsStore private constructor(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("alsalah_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state

    @Synchronized
    fun update(block: (Settings) -> Settings) {
        val n = block(_state.value)
        _state.value = n
        save(n)
    }

    @Synchronized
    fun reset() {
        sp.edit().clear().apply()
        _state.value = Settings()
    }

    private fun load(): Settings {
        val d = Settings()
        fun dbl(k: String, def: Double) = if (sp.contains(k)) Double.fromBits(sp.getLong(k, 0L)) else def
        return Settings(
            theme = sp.getInt("theme", d.theme),
            use24 = sp.getBoolean("use24", d.use24),
            arabicDigits = sp.getBoolean("digits", d.arabicDigits),
            zoneMode = sp.getInt("zoneMode", d.zoneMode),
            lat = dbl("lat", d.lat),
            lon = dbl("lon", d.lon),
            placeName = sp.getString("place", d.placeName) ?: d.placeName,
            locMode = sp.getInt("locMode", d.locMode),
            govId = sp.getInt("govId", d.govId),
            method = sp.getInt("method", d.method),
            asr = sp.getInt("asr", d.asr),
            corr = List(6) { sp.getInt("corr$it", 0) },
            notif = List(5) {
                val n = PrayerNotif()
                PrayerNotif(
                    preEnabled = sp.getBoolean("n${it}_pre", n.preEnabled),
                    preMinutes = sp.getInt("n${it}_lead", n.preMinutes),
                    atEnabled = sp.getBoolean("n${it}_at", n.atEnabled),
                    sound = sp.getInt("n${it}_snd", n.sound),
                    vibrate = sp.getBoolean("n${it}_vib", n.vibrate)
                )
            },
            volume = sp.getFloat("volume", d.volume),
            haptics = sp.getBoolean("haptics", d.haptics),
            hijriAdjust = sp.getInt("hijriAdj", d.hijriAdjust)
        )
    }

    private fun save(s: Settings) {
        val e = sp.edit()
        e.putInt("theme", s.theme).putBoolean("use24", s.use24).putBoolean("digits", s.arabicDigits)
            .putInt("zoneMode", s.zoneMode)
            .putLong("lat", s.lat.toRawBits()).putLong("lon", s.lon.toRawBits())
            .putString("place", s.placeName).putInt("locMode", s.locMode).putInt("govId", s.govId)
            .putInt("method", s.method).putInt("asr", s.asr)
            .putFloat("volume", s.volume).putBoolean("haptics", s.haptics).putInt("hijriAdj", s.hijriAdjust)
        s.corr.forEachIndexed { i, v -> e.putInt("corr$i", v) }
        s.notif.forEachIndexed { i, n ->
            e.putBoolean("n${i}_pre", n.preEnabled).putInt("n${i}_lead", n.preMinutes)
                .putBoolean("n${i}_at", n.atEnabled).putInt("n${i}_snd", n.sound).putBoolean("n${i}_vib", n.vibrate)
        }
        e.apply()
    }

    companion object {
        @Volatile private var inst: SettingsStore? = null
        fun get(ctx: Context): SettingsStore =
            inst ?: synchronized(this) { inst ?: SettingsStore(ctx).also { inst = it } }
    }
}
