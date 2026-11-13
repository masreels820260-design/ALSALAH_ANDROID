package com.mas.alsalah.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mas.alsalah.data.Settings
import com.mas.alsalah.data.params
import com.mas.alsalah.data.zone
import com.mas.alsalah.domain.calendar.HijriCalendar
import com.mas.alsalah.domain.prayer.*
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.rememberNowState
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.ui.theme.PillShape
import com.mas.alsalah.utils.Fmt
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(s: Settings, navigate: (String) -> Unit) {
    val zone = remember(s.zoneMode) { s.zone() }
    val params = remember(s.method, s.asr, s.corr) { s.params() }
    val now = rememberNowState()
    val day by remember(zone) { derivedStateOf { Instant.ofEpochMilli(now.longValue).atZone(zone).toLocalDate() } }
    val days = remember(day, s.lat, s.lon, zone, params) { PrayerTimeline.window(day, s.lat, s.lon, zone, params) }
    val events = remember(days) { PrayerTimeline.events(days) }
    val next = remember(events) { derivedStateOf { PrayerTimeline.nextPrevious(events, now.longValue) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeHeader(s, Fmt.date(day), HijriCalendar.format(day, s.hijriAdjust), navigate) }
        item { CountdownPanel(next, now, zone, s.use24) }
        item { PrayerTable(days[1], next, zone, s, navigate) }
        item { DailySummary(days, zone, s.use24) }
        item { Shortcuts(navigate) }
    }
}

@Composable
private fun HomeHeader(s: Settings, gDate: String, hDate: String, navigate: (String) -> Unit) {
    val g = LocalGlass.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(g.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NightsStay, null, tint = g.onAccent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                GText("الصلاة", size = 24.sp, weight = FontWeight.Bold)
                GText("MAS", size = 11.sp, color = g.sub)
            }
            IconButton(onClick = { navigate("settings") }) { Icon(Icons.Default.Settings, "الإعدادات", tint = g.text) }
        }
        Row(Modifier.clip(PillShape).clickable { navigate("location") }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = g.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            GText(s.placeName, size = 15.sp, weight = FontWeight.SemiBold)
        }
        GText(gDate, size = 15.sp)
        GText(hDate, size = 14.sp, color = g.sub)
    }
}

@Composable
private fun CountdownPanel(next: State<NextInfo?>, now: MutableLongState, zone: ZoneId, use24: Boolean) {
    val g = LocalGlass.current
    val info = next.value
    GlassCard(Modifier.fillMaxWidth(), strong = true, padding = 20.dp) {
        if (info == null) {
            GText("تعذّر حساب المواقيت لهذا الموقع", color = g.sub)
        } else {
            val n = info.next
            GText("الصلاة القادمة", size = 14.sp, color = g.sub)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GText(n.prayer.ar, Modifier.weight(1f), size = 34.sp, weight = FontWeight.Bold)
                GText(Fmt.time(n.millis, zone, use24), size = 22.sp, weight = FontWeight.SemiBold, color = g.accent)
            }
            Spacer(Modifier.height(10.dp))
            val remain = n.millis - now.longValue
            GText(Fmt.countdown(remain), Modifier.fillMaxWidth(), size = 48.sp, weight = FontWeight.Bold, align = TextAlign.Center)
            GText("متبقي على صلاة ${n.prayer.ar}", Modifier.fillMaxWidth(), size = 14.sp, color = g.sub, align = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            val prev = info.previous?.millis ?: (n.millis - 6 * 3600_000L)
            val frac = ((now.longValue - prev).toFloat() / (n.millis - prev).toFloat()).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(PillShape).background(g.divider)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(PillShape).background(g.accent))
            }
        }
    }
}

@Composable
private fun PrayerTable(today: DayTimes, next: State<NextInfo?>, zone: ZoneId, s: Settings, navigate: (String) -> Unit) {
    val g = LocalGlass.current
    val nx = next.value?.next
    GlassCard(Modifier.fillMaxWidth(), padding = 8.dp) {
        Prayer.entries.forEach { pr ->
            val isNext = nx?.prayer == pr
            val ms = if (isNext && nx != null) nx.millis else today.at(pr)
            val tint = if (isNext) g.accent else g.text
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (isNext) g.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GText(pr.ar, Modifier.weight(1f), size = 17.sp, weight = if (isNext) FontWeight.Bold else FontWeight.Normal, color = tint)
                GText(Fmt.time(ms, zone, s.use24), size = 17.sp, weight = FontWeight.SemiBold, color = tint)
                Spacer(Modifier.width(10.dp))
                if (pr.isPrayer) {
                    val n = s.notif[pr.notifIndex]
                    val on = n.atEnabled || n.preEnabled
                    IconButton(onClick = { navigate("prayer:${pr.notifIndex}") }, modifier = Modifier.size(28.dp)) {
                        Icon(if (on) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, "إشعارات ${pr.ar}",
                            tint = if (on) g.accent else g.sub, modifier = Modifier.size(20.dp))
                    }
                } else Spacer(Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun DailySummary(days: List<DayTimes>, zone: ZoneId, use24: Boolean) {
    val g = LocalGlass.current
    val t = days[1]; val tm = days[2]
    val maghrib = t.at(Prayer.MAGHRIB); val sunrise = t.at(Prayer.SUNRISE); val fajrNext = tm.at(Prayer.FAJR)
    val ok = maghrib > 0 && sunrise > 0 && fajrNext > 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("ملخص اليوم")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("طول النهار", if (ok) Fmt.duration(maghrib - sunrise) else "--", Modifier.weight(1f))
            SummaryTile("منتصف الليل", if (ok) Fmt.time((maghrib + fajrNext) / 2, zone, use24) else "--", Modifier.weight(1f))
            SummaryTile("الثلث الأخير", if (ok) Fmt.time(fajrNext - (fajrNext - maghrib) / 3, zone, use24) else "--", Modifier.weight(1f))
        }
        GText("منتصف الليل والثلث الأخير محسوبان بين المغرب والفجر التالي.", size = 12.sp, color = g.sub, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
private fun SummaryTile(title: String, value: String, modifier: Modifier) {
    val g = LocalGlass.current
    GlassCard(modifier, padding = 12.dp) {
        GText(title, size = 12.sp, color = g.sub)
        Spacer(Modifier.height(4.dp))
        GText(value, size = 16.sp, weight = FontWeight.SemiBold)
    }
}

private class Shortcut(val label: String, val route: String, val icon: ImageVector)

@Composable
private fun Shortcuts(navigate: (String) -> Unit) {
    val g = LocalGlass.current
    val items = remember {
        listOf(
            Shortcut("القبلة", "qibla", Icons.Default.Explore),
            Shortcut("الأذكار", "adhkar", Icons.AutoMirrored.Filled.MenuBook),
            Shortcut("المسبحة", "tasbeeh", Icons.Default.RadioButtonChecked),
            Shortcut("دعاء اليوم", "dua", Icons.Default.AutoAwesome),
            Shortcut("التقويم الهجري", "calendar", Icons.Default.CalendarMonth)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("اختصارات")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { sc ->
                GlassCard(Modifier.width(104.dp), onClick = { navigate(sc.route) }, padding = 12.dp) {
                    Icon(sc.icon, null, tint = g.accent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(8.dp))
                    GText(sc.label, size = 13.sp, weight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}
