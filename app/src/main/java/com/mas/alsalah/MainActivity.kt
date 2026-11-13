package com.mas.alsalah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mas.alsalah.notifications.NotificationHelper
import com.mas.alsalah.ui.AppActions
import com.mas.alsalah.ui.AppRoot
import com.mas.alsalah.ui.MainViewModel
import com.mas.alsalah.ui.theme.AlSalahTheme
import com.mas.alsalah.ui.theme.DarkGlass
import com.mas.alsalah.ui.theme.LightGlass
import com.mas.alsalah.utils.Perms

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val gpsPerm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) vm.fetchLocation()
        else vm.toast("لم يتم منح إذن الموقع. يمكنك اختيار المحافظة يدويًا.")
    }
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) vm.toast("الإشعارات متوقفة؛ لن تصلك تنبيهات الصلاة")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        handleIntent(intent)
        askNotificationsOnce()

        val actions = AppActions(
            requestGps = {
                if (Perms.locationOk(this)) vm.fetchLocation()
                else gpsPerm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            },
            requestNotif = {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            },
            openExactAlarm = {
                if (Build.VERSION.SDK_INT >= 31)
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            },
            openBattery = {
                runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }
        )

        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            val dark = when (s.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            val view = LocalView.current
            SideEffect {
                val g = if (dark) DarkGlass else LightGlass
                window.setBackgroundDrawable(ColorDrawable(g.bg.toArgb()))
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            AlSalahTheme(dark = dark, arabicDigits = s.arabicDigits) { AppRoot(vm, actions) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        i?.getStringExtra("route")?.let { vm.pendingRoute.value = it }
    }

    private fun askNotificationsOnce() {
        val sp = getSharedPreferences("alsalah_data", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !sp.getBoolean("asked_notif", false) && !Perms.notificationsOk(this)) {
            sp.edit().putBoolean("asked_notif", true).apply()
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
