package com.mas.alsalah.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mas.alsalah.data.Settings
import com.mas.alsalah.data.content.AdhkarData
import com.mas.alsalah.ui.MainViewModel
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.utils.copyText
import com.mas.alsalah.utils.shareText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun catName(c: Int) = if (c == AdhkarData.FAVORITES) "المفضلة" else AdhkarData.CATEGORIES[c]

@Composable
fun AdhkarScreen(vm: MainViewModel, s: Settings) {
    var cat by rememberSaveable { mutableIntStateOf(-1) }
    BackHandler(enabled = cat >= 0) { cat = -1 }
    if (cat < 0) AdhkarCategories(vm) { cat = it } else AdhkarViewer(vm, s, cat) { cat = -1 }
}

@Composable
private fun AdhkarCategories(vm: MainViewModel, onOpen: (Int) -> Unit) {
    val g = LocalGlass.current
    val d = vm.data
    Column(Modifier.fillMaxSize()) {
        TopBar("الأذكار")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed((0..AdhkarData.FAVORITES).toList()) { _, c ->
                val items = AdhkarData.inCategory(c, d.favorites)
                val done = items.count { d.adhkarCount(it.id) >= it.repeat }
                GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen(c) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            GText(catName(c), size = 18.sp, weight = FontWeight.SemiBold)
                            GText(
                                if (items.isEmpty()) "لا توجد عناصر بعد" else "${items.size} ذكر • اكتمل $done",
                                size = 13.sp, color = g.sub
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = g.sub)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarViewer(vm: MainViewModel, s: Settings, cat: Int, onBack: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val d = vm.data
    val items = AdhkarData.inCategory(cat, d.favorites)
    var idx by rememberSaveable(cat) { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            TopBar(catName(cat), onBack)
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.StarBorder, null, tint = g.sub, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                GText("لم تضف أي ذكر للمفضلة بعد. اضغط على النجمة داخل أي ذكر لإضافته.", align = TextAlign.Center, color = g.sub)
            }
            return@Column
        }
        val i = idx.coerceIn(0, items.lastIndex)
        val item = items[i]
        val count = d.adhkarCount(item.id)
        val remaining = (item.repeat - count).coerceAtLeast(0)
        val fav = d.isFav(item.id)

        TopBar("${catName(cat)}  (${i + 1}/${items.size})", onBack) {
            IconButton(onClick = { d.toggleFav(item.id) }) {
                Icon(if (fav) Icons.Default.Star else Icons.Default.StarBorder, "مفضلة", tint = if (fav) g.accent else g.text)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.fillMaxWidth(), strong = true, padding = 20.dp) {
                GText(item.text, Modifier.fillMaxWidth(), size = 22.sp, lineHeight = 40.sp, align = TextAlign.Center)
            }
            GText("المصدر: ${item.source}", Modifier.fillMaxWidth(), size = 13.sp, color = g.sub, align = TextAlign.Center)
            if (item.note != null) GText(item.note, Modifier.fillMaxWidth(), size = 13.sp, color = g.sub, align = TextAlign.Center)
        }

        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(112.dp).glass(CircleShape, true).clip(CircleShape).clickable {
                    val done = d.adhkarTap(item.id, item.repeat)
                    if (s.haptics) view.performHapticFeedback(if (done) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CLOCK_TICK)
                    if (done) scope.launch { delay(450); if (idx < items.lastIndex) idx++ }
                },
                contentAlignment = Alignment.Center
            ) {
                if (remaining == 0) Icon(Icons.Default.Check, "اكتمل", tint = g.good, modifier = Modifier.size(44.dp))
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GText("$remaining", size = 34.sp, weight = FontWeight.Bold)
                    GText("متبقي", size = 12.sp, color = g.sub)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("السابق", { if (idx > 0) idx-- }, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, enabled = i > 0)
                GlassButton("إعادة", { d.adhkarReset(item.id) }, icon = Icons.Default.Refresh)
                GlassButton("التالي", { if (idx < items.lastIndex) idx++ }, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, enabled = i < items.lastIndex, prominent = remaining == 0)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("نسخ", { copyText(ctx, item.text); vm.toast("تم النسخ") }, icon = Icons.Default.ContentCopy)
                GlassButton("مشاركة", { shareText(ctx, item.text + "\n\n" + item.source) }, icon = Icons.Default.Share)
            }
        }
    }
}
