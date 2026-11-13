package com.mas.alsalah.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class TasbeehSession(val dhikr: String, val count: Int, val time: Long)

/** بيانات المستخدم المحلية: تقدّم الأذكار، المفضلة، المسبحة. تُحفظ محليًا فقط. */
class LocalData(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("alsalah_data", Context.MODE_PRIVATE)

    val progress = mutableStateMapOf<String, Int>()
    val favorites = mutableStateListOf<String>()

    var dhikr by mutableStateOf(DEFAULT_DHIKR)
        private set
    var count by mutableIntStateOf(0)
        private set
    var target by mutableIntStateOf(33)
        private set
    var dailyGoal by mutableIntStateOf(300)
        private set
    var todayTotal by mutableIntStateOf(0)
        private set
    val customDhikr = mutableStateListOf<String>()
    val sessions = mutableStateListOf<TasbeehSession>()

    private var today = LocalDate.now().toString()

    init {
        val saved = sp.getString("day", null)
        if (saved != today) {
            sp.edit().putString("day", today).remove("progress").apply()
        } else {
            sp.getString("progress", "")?.split(";")?.forEach {
                val p = it.split("=")
                if (p.size == 2) p[1].toIntOrNull()?.let { v -> progress[p[0]] = v }
            }
        }
        sp.getString("favs", "")?.split(";")?.filter { it.isNotBlank() }?.let { favorites.addAll(it) }
        dhikr = sp.getString("tas_dhikr", DEFAULT_DHIKR) ?: DEFAULT_DHIKR
        count = sp.getInt("tas_count", 0)
        target = sp.getInt("tas_target", 33)
        dailyGoal = sp.getInt("tas_goal", 300)
        todayTotal = sp.getInt("tas_total_$today", 0)
        sp.getString("tas_custom", "")?.split(";")?.filter { it.isNotBlank() }?.let { customDhikr.addAll(it) }
        runCatching {
            val arr = JSONArray(sp.getString("tas_sessions", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                sessions.add(TasbeehSession(o.getString("d"), o.getInt("c"), o.getLong("t")))
            }
        }
    }

    private fun rollDay() {
        val now = LocalDate.now().toString()
        if (now != today) {
            today = now
            progress.clear()
            todayTotal = 0
            sp.edit().putString("day", today).remove("progress").apply()
        }
    }

    // ---- الأذكار ----
    fun adhkarCount(id: String): Int { rollDay(); return progress[id] ?: 0 }

    /** يزيد العداد ويعيد true إذا اكتمل الذكر بهذه الضغطة. */
    fun adhkarTap(id: String, repeat: Int): Boolean {
        rollDay()
        val cur = progress[id] ?: 0
        if (cur >= repeat) return false
        progress[id] = cur + 1
        saveProgress()
        return cur + 1 >= repeat
    }

    fun adhkarReset(id: String) { progress.remove(id); saveProgress() }

    private fun saveProgress() {
        sp.edit().putString("progress", progress.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    fun isFav(id: String) = favorites.contains(id)
    fun toggleFav(id: String) {
        if (!favorites.remove(id)) favorites.add(id)
        sp.edit().putString("favs", favorites.joinToString(";")).apply()
    }

    // ---- المسبحة ----
    fun tap() {
        rollDay()
        count++; todayTotal++
        sp.edit().putInt("tas_count", count).putInt("tas_total_$today", todayTotal).apply()
    }

    fun undo() {
        if (count <= 0) return
        count--; if (todayTotal > 0) todayTotal--
        sp.edit().putInt("tas_count", count).putInt("tas_total_$today", todayTotal).apply()
    }

    /** يحفظ الجلسة الحالية (إن لم تكن صفرًا) ويصفّر العداد. */
    fun resetSession() {
        if (count > 0) {
            sessions.add(0, TasbeehSession(dhikr, count, System.currentTimeMillis()))
            while (sessions.size > 50) sessions.removeAt(sessions.lastIndex)
            val arr = JSONArray()
            sessions.forEach { arr.put(JSONObject().put("d", it.dhikr).put("c", it.count).put("t", it.time)) }
            sp.edit().putString("tas_sessions", arr.toString()).apply()
        }
        count = 0
        sp.edit().putInt("tas_count", 0).apply()
    }

    fun selectDhikr(d: String) {
        if (d == dhikr) return
        resetSession(); dhikr = d
        sp.edit().putString("tas_dhikr", d).apply()
    }

    fun addCustom(d: String) {
        val t = d.trim().replace(";", " ")
        if (t.isEmpty()) return
        if (!customDhikr.contains(t) && !DEFAULT_TASBEEH.contains(t)) customDhikr.add(t)
        sp.edit().putString("tas_custom", customDhikr.joinToString(";")).apply()
        selectDhikr(t)
    }

    fun setTarget(v: Int) { target = v.coerceIn(1, 100000); sp.edit().putInt("tas_target", target).apply() }
    fun setDailyGoal(v: Int) { dailyGoal = v.coerceIn(1, 1000000); sp.edit().putInt("tas_goal", dailyGoal).apply() }

    companion object {
        const val DEFAULT_DHIKR = "سبحان الله"
        val DEFAULT_TASBEEH = listOf("سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله", "أستغفر الله")
    }
}
