package com.mas.alsalah.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.LocalContext
import com.mas.alsalah.data.*
import com.mas.alsalah.domain.location.EgyptLocations
import com.mas.alsalah.domain.prayer.CalcMethod
import com.mas.alsalah.domain.prayer.Prayer
import com.mas.alsalah.domain.prayer.PrayerTimeline
import com.mas.alsalah.ui.AppActions
import com.mas.alsalah.ui.MainViewModel
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.utils.Perms
import java.time.LocalDate

private val PRESET_LEADS = listOf(5, 10, 15, 20, 25, 30)
private val SOUND_NAMES = listOf("تكبير", "نغمة", "صامت")

@Composable
fun SettingsScreen(vm: MainViewModel, s: Settings, actions: AppActions, navigate: (String) -> Unit, onBack: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val notifOk = remember(tick) { Perms.notificationsOk(ctx) }
    val exactOk = remember(tick) { Perms.exactOk(ctx) }
    var picker by remember { mutableIntStateOf(0) }  // 1 طريقة الحساب
    var showReset by remember { mutableStateOf(false) }
    val nextIdx = remember {
        val z = s.zone()
        val d = PrayerTimeline.window(LocalDate.now(z), s.lat, s.lon, z, s.params())
        PrayerTimeline.nextPrevious(PrayerTimeline.events(d), System.currentTimeMillis())?.next?.prayer?.notifIndex ?: 0
    }
    var vol by remember(s.volume) { mutableFloatStateOf(s.volume) }

    Column(Modifier.fillMaxSize()) {
        TopBar("الإعدادات", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            SectionTitle("المظهر")
            GlassCard(Modifier.fillMaxWidth()) {
                GlassSegmented(listOf("النظام", "فاتح", "داكن"), s.theme, { v -> vm.update { it.copy(theme = v) } })
            }

            SectionTitle("الوقت")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                Column(Modifier.padding(16.dp)) {
                    GText("نظام الساعة", size = 14.sp, color = g.sub)
                    Spacer(Modifier.height(8.dp))
                    GlassSegmented(listOf("١٢ ساعة", "٢٤ ساعة"), if (s.use24) 1 else 0, { v -> vm.update { it.copy(use24 = v == 1) } })
                    Spacer(Modifier.height(12.dp))
                    GText("الأرقام", size = 14.sp, color = g.sub)
                    Spacer(Modifier.height(8.dp))
                    GlassSegmented(listOf("هندية ١٢٣", "إنجليزية 123"), if (s.arabicDigits) 0 else 1, { v -> vm.update { it.copy(arabicDigits = v == 0) } })
                }
                GlassDivider()
                SwitchRow(
                    "المنطقة الزمنية تلقائيًا", s.zoneMode == 0, { v -> vm.update { it.copy(zoneMode = if (v) 0 else 1) } },
                    subtitle = if (s.zoneMode == 0) "يتبع توقيت جهازك، ويدعم التوقيت الصيفي تلقائيًا" else "توقيت القاهرة دائمًا (Africa/Cairo)"
                )
            }

            SectionTitle("الموقع")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingRow("الموقع الحالي", s.placeName, subtitle = when (s.locMode) { 1 -> "تحديد تلقائي (GPS)"; 2 -> "إحداثيات مخصصة"; else -> "اختيار يدوي" }, onClick = { navigate("location") })
            }

            SectionTitle("حساب المواقيت")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingRow("طريقة الحساب", CalcMethod.entries[s.method.coerceIn(0, CalcMethod.entries.size - 1)].ar, onClick = { picker = 1 })
                GlassDivider()
                Column(Modifier.padding(16.dp)) {
                    GText("حساب العصر", size = 14.sp, color = g.sub)
                    Spacer(Modifier.height(8.dp))
                    GlassSegmented(listOf("شافعي/مالكي/حنبلي", "حنفي"), if (s.asr == 2) 1 else 0, { v -> vm.update { it.copy(asr = if (v == 1) 2 else 1) } })
                }
                GlassDivider()
                Prayer.entries.forEach { p ->
                    StepperRow("تصحيح ${p.ar}", s.corr[p.ordinal], { v -> vm.update { st -> st.copy(corr = st.corr.mapIndexed { i, x -> if (i == p.ordinal) v else x }) } }, -30, 30, " د")
                }
                Row(Modifier.padding(16.dp)) {
                    GlassButton("استعادة الافتراضي", {
                        vm.update { it.copy(method = 0, asr = 1, corr = List(6) { 0 }) }
                    }, icon = Icons.Default.Refresh)
                }
            }

            SectionTitle("الإشعارات")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                Prayer.FIVE.forEachIndexed { i, p ->
                    val n = s.notif[i]
                    val summary = buildString {
                        append(if (n.preEnabled) "قبل ${n.preMinutes} د" else "بلا تنبيه مسبق")
                        append(" • ")
                        append(if (n.atEnabled) SOUND_NAMES[n.sound.coerceIn(0, 2)] else "عند الوقت: متوقف")
                    }
                    SettingRow(p.ar, subtitle = summary, onClick = { navigate("prayer:$i") })
                    if (i < 4) GlassDivider()
                }
                GlassDivider()
                Column(Modifier.padding(16.dp)) {
                    GText("مستوى صوت التكبير", size = 14.sp, color = g.sub)
                    Slider(
                        value = vol, onValueChange = { vol = it }, valueRange = 0.1f..1f,
                        onValueChangeFinished = { vm.update { it.copy(volume = vol) } },
                        colors = SliderDefaults.colors(thumbColor = g.accent, activeTrackColor = g.accent, inactiveTrackColor = g.divider)
                    )
                    GText("يتحكم في صوت نطق التكبير فقط ضمن مستوى صوت المنبّه في جهازك. لا يتجاوز التطبيق وضع الصامت أو عدم الإزعاج.", size = 12.sp, color = g.sub)
                    Spacer(Modifier.height(10.dp))
                    GlassButton("اختبار الإشعار والصوت", {
                        if (!notifOk) actions.requestNotif()
                        vm.test(nextIdx)
                    }, Modifier.fillMaxWidth(), icon = Icons.Default.NotificationsActive, prominent = true)
                }
            }

            SectionTitle("أذونات وموثوقية التنبيهات")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingRow("الإشعارات", if (notifOk) "مفعّلة" else "متوقفة", onClick = actions.requestNotif)
                GlassDivider()
                SettingRow("المنبّه الدقيق", if (exactOk) "مسموح" else "غير مسموح", subtitle = if (exactOk) null else "بدونه قد يتأخر التنبيه بضع دقائق", onClick = actions.openExactAlarm)
                GlassDivider()
                SettingRow("تحسين البطارية", subtitle = "على بعض الأجهزة (شاومي، هواوي، أوبو، فيفو، سامسونج) قد يوقف النظام التطبيق في الخلفية. اسمح للتطبيق بالعمل دون قيود البطارية وفعّل «التشغيل التلقائي» إن وُجد.", onClick = actions.openBattery)
            }

            SectionTitle("عام")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SwitchRow("اهتزاز عند اللمس (المسبحة والأذكار)", s.haptics, { v -> vm.update { it.copy(haptics = v) } })
                GlassDivider()
                SettingRow("اللغة", "العربية", subtitle = "التطبيق متاح بالعربية فقط حاليًا")
                GlassDivider()
                SettingRow("الخصوصية", onClick = { navigate("privacy") })
                GlassDivider()
                SettingRow("حول التطبيق", onClick = { navigate("about") })
                GlassDivider()
                SettingRow("إعادة ضبط التفضيلات", onClick = { showReset = true })
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (picker == 1) PickerSheet("طريقة الحساب", CalcMethod.entries.map { it.ar }, s.method, { v -> vm.update { it.copy(method = v) } }, { picker = 0 })
    if (showReset) GlassDialog({ showReset = false }, "إعادة ضبط التفضيلات", "إعادة ضبط", { vm.resetAll(); showReset = false }) {
        GText("ستعود كل الإعدادات (المظهر، الموقع، التنبيهات، التصحيحات) إلى الوضع الافتراضي. لن تُحذف بيانات الأذكار والمسبحة.", size = 14.sp, color = g.sub)
    }
}

@Composable
fun PrayerSettingsScreen(idx: Int, s: Settings, vm: MainViewModel, onBack: () -> Unit) {
    val g = LocalGlass.current
    val i = idx.coerceIn(0, 4)
    val n = s.notif[i]
    val pr = Prayer.FIVE[i]
    var customDlg by remember { mutableStateOf(false) }
    fun upd(f: (PrayerNotif) -> PrayerNotif) = vm.update { st -> st.copy(notif = st.notif.mapIndexed { k, x -> if (k == i) f(x) else x }) }

    Column(Modifier.fillMaxSize()) {
        TopBar("إشعارات ${pr.ar}", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("قبل الصلاة")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SwitchRow("تنبيه قبل الصلاة", n.preEnabled, { v -> upd { it.copy(preEnabled = v) } }, subtitle = "«اقتربت صلاة ${pr.ar}»")
                if (n.preEnabled) {
                    GlassDivider()
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRESET_LEADS.forEach { m -> GlassChip("$m د", n.preMinutes == m) { upd { it.copy(preMinutes = m) } } }
                        GlassChip(if (n.preMinutes in PRESET_LEADS) "مخصص" else "مخصص: ${n.preMinutes} د", n.preMinutes !in PRESET_LEADS) { customDlg = true }
                    }
                }
            }
            SectionTitle("عند دخول الوقت")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SwitchRow("تنبيه عند دخول الوقت", n.atEnabled, { v -> upd { it.copy(atEnabled = v) } }, subtitle = "«صلاة ${pr.ar} الآن»")
                if (n.atEnabled) {
                    GlassDivider()
                    Column(Modifier.padding(16.dp)) {
                        GText("الصوت", size = 14.sp, color = g.sub)
                        Spacer(Modifier.height(8.dp))
                        GlassSegmented(SOUND_NAMES, n.sound.coerceIn(0, 2), { v -> upd { it.copy(sound = v) } })
                        if (n.sound == SOUND_TAKBEER) GText("يُنطق «الله أكبر، الله أكبر» بصوت محرك النطق العربي في جهازك (لا يُشغَّل الأذان كاملًا). إن لم تتوفر العربية في جهازك تُستخدم نغمة الإشعار بدلًا منه.", size = 12.sp, color = g.sub, modifier = Modifier.padding(top = 8.dp))
                    }
                    GlassDivider()
                    SwitchRow("الاهتزاز", n.vibrate, { v -> upd { it.copy(vibrate = v) } })
                    GlassDivider()
                    Box(Modifier.padding(16.dp)) {
                        GlassButton("تجربة الصوت والإشعار", { vm.test(i) }, Modifier.fillMaxWidth(), icon = Icons.Default.NotificationsActive)
                    }
                }
            }
            GText("بعد تغيير الصوت أو الاهتزاز قد يحتفظ أندرويد بإعدادات قناة الإشعار القديمة؛ يمكنك ضبطها من إعدادات إشعارات التطبيق في النظام.", size = 12.sp, color = g.sub, modifier = Modifier.padding(6.dp))
        }
    }
    if (customDlg) NumberDialog("المدة قبل الصلاة (بالدقائق)", n.preMinutes, 1, 180, { customDlg = false }) { v -> upd { it.copy(preMinutes = v, preEnabled = true) } }
}

@Composable
fun LocationScreen(s: Settings, vm: MainViewModel, actions: AppActions, onBack: () -> Unit) {
    val g = LocalGlass.current
    val busy by vm.gpsBusy.collectAsState()
    var q by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    val list = remember(q) { EgyptLocations.governorates.filter { q.isBlank() || it.name.contains(q.trim()) || it.city.contains(q.trim()) } }

    Column(Modifier.fillMaxSize()) {
        TopBar("الموقع", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), strong = true) {
                    GText("الموقع الحالي", size = 13.sp, color = g.sub)
                    GText(s.placeName, size = 20.sp, weight = FontWeight.Bold)
                    GText("خط العرض ${"%.4f".format(java.util.Locale.US, s.lat)} • خط الطول ${"%.4f".format(java.util.Locale.US, s.lon)}", size = 12.sp, color = g.sub)
                    Spacer(Modifier.height(12.dp))
                    GlassButton(if (busy) "جارٍ التحديد..." else "تحديد موقعي تلقائيًا (GPS)", actions.requestGps, Modifier.fillMaxWidth(), icon = Icons.Default.MyLocation, prominent = true, enabled = !busy)
                    GText("يُطلب الموقع مرة واحدة فقط عند الضغط، ولا يوجد تتبع مستمر. تُحفظ آخر إحداثيات صالحة للعمل دون إنترنت.", size = 12.sp, color = g.sub, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item { GlassField(q, { q = it }, "ابحث عن محافظة أو مدينة") }
            items(list) { gov ->
                val sel = s.locMode == 0 && s.govId == gov.id
                GlassCard(Modifier.fillMaxWidth(), onClick = {
                    vm.update { it.copy(lat = gov.lat, lon = gov.lon, placeName = gov.label, locMode = 0, govId = gov.id) }
                }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GText(gov.label, Modifier.weight(1f), size = 16.sp, color = if (sel) g.accent else g.text, weight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        if (sel) Icon(Icons.Default.Check, null, tint = g.accent)
                    }
                }
            }
            item {
                GlassButton("إدخال إحداثيات يدويًا (مركز/قرية)", { custom = true }, Modifier.fillMaxWidth())
                GText("المحافظات بإحداثيات عاصمة كل محافظة (تقريبية). للمراكز والقرى استخدم GPS أو أدخل إحداثياتها الدقيقة؛ لا تُعرض مواقيت قرية على أنها موثّقة إن لم تُدخل إحداثياتها بنفسك.", size = 12.sp, color = g.sub, modifier = Modifier.padding(6.dp))
            }
        }
    }
    if (custom) CustomLocationDialog({ custom = false }) { name, la, lo ->
        vm.update { it.copy(lat = la, lon = lo, placeName = name, locMode = 2, govId = -1) }
    }
}

@Composable
private fun CustomLocationDialog(onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var la by remember { mutableStateOf("") }
    var lo by remember { mutableStateOf("") }
    val latV = la.replace(',', '.').toDoubleOrNull()
    val lonV = lo.replace(',', '.').toDoubleOrNull()
    val valid = latV != null && lonV != null && latV in -90.0..90.0 && lonV in -180.0..180.0
    GlassDialog(onDismiss, "إحداثيات مخصصة", "حفظ", { if (valid) { onSave(name.ifBlank { "موقع مخصص" }, latV!!, lonV!!); onDismiss() } }) {
        GlassField(name, { name = it.take(40) }, "اسم الموقع (اختياري)")
        GlassField(la, { la = it.take(12) }, "خط العرض (مثال 30.0444)", number = true)
        GlassField(lo, { lo = it.take(12) }, "خط الطول (مثال 31.2357)", number = true)
        if (!valid && (la.isNotEmpty() || lo.isNotEmpty())) GText("أدخل إحداثيات صحيحة.", size = 12.sp, color = LocalGlass.current.sub)
    }
}
