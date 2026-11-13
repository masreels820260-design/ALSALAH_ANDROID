package com.mas.alsalah.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mas.alsalah.data.Settings
import com.mas.alsalah.data.content.DuaData
import com.mas.alsalah.data.zone
import com.mas.alsalah.domain.calendar.HijriCalendar
import com.mas.alsalah.ui.MainViewModel
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.utils.Fmt
import com.mas.alsalah.utils.copyText
import com.mas.alsalah.utils.shareText
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit

@Composable
fun MoreScreen(navigate: (String) -> Unit) {
    val g = LocalGlass.current
    val rows = listOf(
        Triple("التقويم الهجري", "calendar", Icons.Default.CalendarMonth),
        Triple("دعاء اليوم", "dua", Icons.Default.AutoAwesome),
        Triple("الإعدادات", "settings", Icons.Default.Settings),
        Triple("حول التطبيق", "about", Icons.Default.Info),
        Triple("الخصوصية", "privacy", Icons.Default.PrivacyTip)
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar("المزيد")
        rows.forEach { (title, route, icon) ->
            GlassCard(Modifier.fillMaxWidth(), onClick = { navigate(route) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = g.accent)
                    Spacer(Modifier.width(14.dp))
                    GText(title, Modifier.weight(1f), size = 17.sp, weight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = g.sub)
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(s: Settings, vm: MainViewModel, onBack: () -> Unit) {
    val g = LocalGlass.current
    val zone = remember(s.zoneMode) { s.zone() }
    val today = remember(zone) { LocalDate.now(zone) }
    val adj = s.hijriAdjust
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val baseH = remember(today, adj) { HijriCalendar.hijri(today, adj) }
    val first = remember(baseH, offset) {
        HijrahDate.of(HijriCalendar.year(baseH), HijriCalendar.month(baseH), 1).plus(offset.toLong(), ChronoUnit.MONTHS)
    }
    val month = HijriCalendar.month(first)
    val year = HijriCalendar.year(first)
    val len = first.lengthOfMonth()
    val startG = remember(first, adj) { LocalDate.from(first).minusDays(adj.toLong()) }
    val lead = (startG.dayOfWeek.value + 1) % 7 // السبت أول الأسبوع
    val upcoming = remember(today, adj) { HijriCalendar.upcoming(today, adj, 6) }
    val days = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
    val todayH = HijriCalendar.format(today, adj)

    Column(Modifier.fillMaxSize()) {
        TopBar("التقويم الهجري", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(Modifier.fillMaxWidth(), strong = true) {
                GText("اليوم", size = 13.sp, color = g.sub)
                GText(todayH, size = 22.sp, weight = FontWeight.Bold)
                GText(Fmt.date(today), size = 14.sp, color = g.sub)
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset--; selected = 0 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق", tint = g.text) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GText("${HijriCalendar.MONTHS[month - 1]} $year هـ", size = 18.sp, weight = FontWeight.Bold)
                        GText("${Fmt.MONTHS[startG.monthValue - 1]} ${startG.year}", size = 12.sp, color = g.sub)
                    }
                    IconButton(onClick = { offset++; selected = 0 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي", tint = g.text) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) { days.forEach { GText(it, Modifier.weight(1f), size = 12.sp, color = g.sub, align = TextAlign.Center) } }
                val rows = (lead + len + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        for (c in 0 until 7) {
                            val k = r * 7 + c - lead + 1
                            if (k < 1 || k > len) { Spacer(Modifier.weight(1f)) } else {
                                val gd = startG.plusDays((k - 1).toLong())
                                val isToday = gd == today
                                val mark = HijriCalendar.occasionsOn(month, k).isNotEmpty() || HijriCalendar.isWhiteDay(k)
                                Column(
                                    Modifier.weight(1f).padding(1.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isToday) g.accent else if (selected == k) g.accent.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable { selected = k }.padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    GText("$k", size = 16.sp, weight = FontWeight.SemiBold, color = if (isToday) g.onAccent else g.text)
                                    GText("${gd.dayOfMonth}", size = 10.sp, color = if (isToday) g.onAccent else g.sub)
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (mark) (if (isToday) g.onAccent else g.accent) else Color.Transparent))
                                }
                            }
                        }
                    }
                }
            }
            if (selected in 1..len) {
                val gd = startG.plusDays((selected - 1).toLong())
                val occ = HijriCalendar.occasionsOn(month, selected)
                GlassCard(Modifier.fillMaxWidth()) {
                    GText("$selected ${HijriCalendar.MONTHS[month - 1]} $year هـ — ${Fmt.date(gd)}", size = 15.sp, weight = FontWeight.SemiBold)
                    occ.forEach { GText("• ${it.name}: ${it.note}", size = 13.sp, color = g.sub, modifier = Modifier.padding(top = 6.dp)) }
                    if (HijriCalendar.isWhiteDay(selected)) GText("• من الأيام البيض (١٣ و١٤ و١٥) ويُستحب صيامها.", size = 13.sp, color = g.sub, modifier = Modifier.padding(top = 6.dp))
                }
            }
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                StepperRow("تعديل التاريخ الهجري", s.hijriAdjust, { v -> vm.update { it.copy(hijriAdjust = v) } }, -2, 2, " يوم")
            }
            SectionTitle("المناسبات القادمة")
            GlassCard(Modifier.fillMaxWidth()) {
                upcoming.forEachIndexed { i, u ->
                    if (i > 0) { Spacer(Modifier.height(8.dp)); GlassDivider(); Spacer(Modifier.height(8.dp)) }
                    Row {
                        GText(u.name, Modifier.weight(1f), size = 15.sp, weight = FontWeight.SemiBold)
                        GText(if (u.daysAway == 0L) "اليوم" else "بعد ${u.daysAway} يوم", size = 13.sp, color = g.accent)
                    }
                    GText(Fmt.date(u.date), size = 12.sp, color = g.sub)
                    GText(u.note, size = 12.sp, color = g.sub)
                }
            }
            GText("التواريخ محسوبة حسابيًا (تقويم أم القرى) وقد تختلف يومًا عن الإعلان الرسمي في مصر. المناسبات المرتبطة بالرؤية (رمضان والعيدان) غير مؤكدة حتى يعلنها المرجع الرسمي، ويمكنك التعديل بين -٢ و+٢ يوم.", size = 12.sp, color = g.sub, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun DuaScreen(vm: MainViewModel, s: Settings, onBack: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val zone = remember(s.zoneMode) { s.zone() }
    val dayIdx = remember(zone) { LocalDate.now(zone).dayOfYear }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val n = DuaData.ALL.size
    val i = ((dayIdx + offset) % n + n) % n
    val dua = DuaData.ALL[i]
    val fav = vm.data.isFav(dua.id)

    Column(Modifier.fillMaxSize()) {
        TopBar(if (offset == 0) "دعاء اليوم" else "الأدعية", onBack) {
            IconButton(onClick = { vm.data.toggleFav(dua.id) }) {
                Icon(if (fav) Icons.Default.Star else Icons.Default.StarBorder, "مفضلة", tint = if (fav) g.accent else g.text)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.fillMaxWidth(), strong = true, padding = 22.dp) {
                GText(dua.text, Modifier.fillMaxWidth(), size = 24.sp, lineHeight = 42.sp, align = TextAlign.Center)
            }
            GText("المصدر: ${dua.source}", Modifier.fillMaxWidth(), size = 13.sp, color = g.sub, align = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("السابق", { offset-- }, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight)
                GlassButton("التالي", { offset++ }, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("نسخ", { copyText(ctx, dua.text); vm.toast("تم النسخ") }, icon = Icons.Default.ContentCopy)
                GlassButton("مشاركة", { shareText(ctx, dua.text + "\n\n" + dua.source) }, icon = Icons.Default.Share)
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "-" }
    InfoPage("حول التطبيق", onBack) {
        GText("الصلاة — AL-SALAH", size = 22.sp, weight = FontWeight.Bold)
        GText("من MAS • الإصدار $ver", size = 13.sp, color = g.sub)
        GText("تطبيق مواقيت الصلاة لجميع محافظات مصر، يعمل بالكامل دون إنترنت: مواقيت محسوبة فلكيًا من إحداثيات موقعك، تنبيهات قبل الصلاة وعند دخولها بالتكبير، قبلة بالبوصلة، أذكار، مسبحة، تقويم هجري، ودعاء اليوم.", size = 15.sp, lineHeight = 26.sp)
        GText("طريقة الحساب الافتراضية: الهيئة المصرية العامة للمساحة (الفجر ١٩٫٥° والعشاء ١٧٫٥°). المواقيت حسابية؛ طابقها مع التقويم الرسمي في بلدك ويمكنك التصحيح اليدوي لكل صلاة من الإعدادات.", size = 14.sp, color = g.sub, lineHeight = 24.sp)
        GText("تنبيه: نصوص الأذكار والأدعية مأخوذة من مصادرها المعروفة مع ذكر المصدر، وتحتاج مراجعة أهل العلم قبل الاعتماد عليها في النشر العام.", size = 14.sp, color = g.sub, lineHeight = 24.sp)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val g = LocalGlass.current
    InfoPage("الخصوصية", onBack) {
        listOf(
            "لا يوجد تسجيل حساب ولا خادم خاص بالتطبيق.",
            "كل الإعدادات والتقدّم (الأذكار، المسبحة، المفضلة) تُحفظ على جهازك فقط.",
            "لا توجد أدوات تحليلات ولا إعلانات ولا تتبع.",
            "الموقع يُطلب فقط عند الضغط على «تحديد موقعي تلقائيًا» لمرة واحدة، وتُحفظ الإحداثيات محليًا لحساب المواقيت دون إنترنت. لا يوجد تتبع مستمر.",
            "عند استخدام GPS قد تتعامل خدمات Google Play (إن وُجدت على جهازك) مع طلب الموقع وفق سياساتها.",
            "الأذونات: الإشعارات، المنبّه الدقيق (لإطلاق التنبيه في وقته)، الاهتزاز، خدمة تشغيل الصوت في المقدمة (للتكبير)، والموقع عند الطلب."
        ).forEach { GText("• $it", size = 15.sp, lineHeight = 26.sp) }
        GText("يمكنك مسح كل بيانات التطبيق من إعدادات الأندرويد أو من «إعادة ضبط التفضيلات».", size = 13.sp, color = g.sub)
    }
}

@Composable
private fun InfoPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(Modifier.fillMaxWidth(), padding = 20.dp) { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
        }
    }
}
