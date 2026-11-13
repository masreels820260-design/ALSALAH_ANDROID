package com.mas.alsalah.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mas.alsalah.data.LocalData
import com.mas.alsalah.data.Settings
import com.mas.alsalah.ui.MainViewModel
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.ui.theme.PillShape
import java.time.Instant
import java.time.ZoneId

@Composable
fun TasbeehScreen(vm: MainViewModel, s: Settings) {
    val g = LocalGlass.current
    val view = LocalView.current
    val d = vm.data
    var dlg by remember { mutableIntStateOf(0) } // 1 مخصص، 2 هدف، 3 هدف يومي، 4 جلسات
    var custom by remember { mutableStateOf("") }

    LaunchedEffect(d.count) {
        if (s.haptics && d.count > 0 && d.count % d.target == 0) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TopBar("المسبحة") {
            IconButton(onClick = { dlg = 4 }) { Icon(Icons.Default.History, "الجلسات", tint = g.text) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LocalData.DEFAULT_TASBEEH + d.customDhikr.toList()) { t -> GlassChip(t, d.dhikr == t) { d.selectDhikr(t) } }
            item { GlassChip("+ ذكر مخصص", false) { custom = ""; dlg = 1 } }
        }

        Box(Modifier.fillMaxWidth().height(290.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                val progress = (d.count.toFloat() / d.target).coerceIn(0f, 1f)
                Canvas(Modifier.fillMaxSize()) {
                    val w = 10.dp.toPx()
                    val tl = Offset(w / 2, w / 2)
                    val sz = Size(size.width - w, size.height - w)
                    drawArc(g.divider, 0f, 360f, false, tl, sz, style = Stroke(w))
                    drawArc(g.accent, -90f, 360f * progress, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
                }
                Box(
                    Modifier.size(232.dp).glass(CircleShape, true).clip(CircleShape).clickable {
                        d.tap()
                        if (s.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GText("${d.count}", size = 68.sp, weight = FontWeight.Bold)
                        GText(d.dhikr, size = 18.sp, weight = FontWeight.SemiBold, color = g.accent, align = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp), maxLines = 2)
                        GText("الهدف ${d.target}", size = 12.sp, color = g.sub)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            GlassButton("تراجع", { d.undo() }, icon = Icons.AutoMirrored.Filled.Undo, enabled = d.count > 0)
            GlassButton("إعادة", { d.resetSession() }, icon = Icons.Default.Refresh, enabled = d.count > 0)
            GlassButton("الهدف", { dlg = 2 }, icon = Icons.Default.Flag)
        }

        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = { dlg = 3 }) {
            Row {
                GText("إجمالي اليوم", Modifier.weight(1f), size = 15.sp, weight = FontWeight.SemiBold)
                GText("${d.todayTotal} / ${d.dailyGoal}", size = 15.sp, color = g.accent, weight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(PillShape).background(g.divider)) {
                Box(Modifier.fillMaxWidth((d.todayTotal.toFloat() / d.dailyGoal).coerceIn(0f, 1f)).fillMaxHeight().clip(PillShape).background(g.accent))
            }
            GText("اضغط لتعديل الهدف اليومي", size = 11.sp, color = g.sub, modifier = Modifier.padding(top = 6.dp))
        }
    }

    when (dlg) {
        1 -> GlassDialog({ dlg = 0 }, "ذكر مخصص", "إضافة", { d.addCustom(custom); dlg = 0 }) {
            GlassField(custom, { custom = it.take(60) }, "اكتب الذكر")
        }
        2 -> NumberDialog("الهدف لكل دورة", d.target, 1, 100000, { dlg = 0 }) { d.setTarget(it) }
        3 -> NumberDialog("الهدف اليومي", d.dailyGoal, 1, 1000000, { dlg = 0 }) { d.setDailyGoal(it) }
        4 -> GlassDialog({ dlg = 0 }, "الجلسات المحفوظة") {
            if (d.sessions.isEmpty()) GText("لا توجد جلسات بعد. تُحفظ الجلسة عند الضغط على «إعادة» أو تبديل الذكر.", size = 14.sp, color = g.sub)
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(d.sessions.toList()) { ss ->
                    val t = Instant.ofEpochMilli(ss.time).atZone(ZoneId.systemDefault())
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        GText(ss.dhikr, Modifier.weight(1f), size = 15.sp)
                        GText("${ss.count}", size = 15.sp, weight = FontWeight.Bold, color = g.accent)
                        Spacer(Modifier.width(10.dp))
                        GText("${t.dayOfMonth}/${t.monthValue} ${String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)}", size = 12.sp, color = g.sub)
                    }
                    GlassDivider()
                }
            }
        }
    }
}
