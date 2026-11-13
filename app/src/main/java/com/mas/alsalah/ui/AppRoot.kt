package com.mas.alsalah.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mas.alsalah.ui.components.GText
import com.mas.alsalah.ui.components.glass
import com.mas.alsalah.ui.screens.*
import com.mas.alsalah.ui.theme.LocalGlass
import com.mas.alsalah.ui.theme.PillShape
import kotlinx.coroutines.delay

class AppActions(
    val requestGps: () -> Unit,
    val requestNotif: () -> Unit,
    val openExactAlarm: () -> Unit,
    val openBattery: () -> Unit
)

private class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "الرئيسية", Icons.Default.Home),
    Tab("qibla", "القبلة", Icons.Default.Explore),
    Tab("adhkar", "الأذكار", Icons.AutoMirrored.Filled.MenuBook),
    Tab("tasbeeh", "المسبحة", Icons.Default.RadioButtonChecked),
    Tab("more", "المزيد", Icons.Default.MoreHoriz)
)

@Composable
fun AppRoot(vm: MainViewModel, actions: AppActions) {
    val g = LocalGlass.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val pending by vm.pendingRoute.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()

    val stack = rememberSaveable(saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf("home")
    }
    val route = stack.last()
    val isTab = TABS.any { it.route == route }

    fun navigate(r: String) {
        if (TABS.any { it.route == r }) { stack.clear(); stack.add(r) }
        else if (stack.last() != r) stack.add(r)
    }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else if (stack[0] != "home") stack[0] = "home" }

    BackHandler(enabled = stack.size > 1 || stack[0] != "home") { back() }
    LaunchedEffect(pending) { pending?.let { navigate(it); vm.consumeRoute() } }
    LaunchedEffect(msg) { if (msg != null) { delay(3200); vm.message.value = null } }

    Column(Modifier.fillMaxSize().background(g.bg).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when {
                route == "home" -> HomeScreen(s, ::navigate)
                route == "qibla" -> QiblaScreen(s)
                route == "adhkar" -> AdhkarScreen(vm, s)
                route == "tasbeeh" -> TasbeehScreen(vm, s)
                route == "more" -> MoreScreen(::navigate)
                route == "calendar" -> CalendarScreen(s, vm, ::back)
                route == "dua" -> DuaScreen(vm, s, ::back)
                route == "settings" -> SettingsScreen(vm, s, actions, ::navigate, ::back)
                route == "location" -> LocationScreen(s, vm, actions, ::back)
                route == "about" -> AboutScreen(::back)
                route == "privacy" -> PrivacyScreen(::back)
                route.startsWith("prayer:") -> PrayerSettingsScreen(route.substringAfter(':').toIntOrNull() ?: 0, s, vm, ::back)
                else -> HomeScreen(s, ::navigate)
            }
            if (msg != null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).glass(RoundedCornerShape(18.dp), true).background(g.solid.copy(alpha = 0.85f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp)) {
                    GText(msg ?: "", size = 14.sp)
                }
            }
        }
        if (isTab) BottomBar(route, ::navigate)
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).glass(RoundedCornerShape(28.dp), true).padding(6.dp)) {
        TABS.forEach { t ->
            val sel = t.route == current
            Column(
                Modifier.weight(1f).clip(PillShape).background(if (sel) g.accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(t.route) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(t.icon, t.label, tint = if (sel) g.accent else g.sub, modifier = Modifier.size(22.dp))
                GText(t.label, size = 11.sp, weight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) g.accent else g.sub, maxLines = 1)
            }
        }
    }
}
