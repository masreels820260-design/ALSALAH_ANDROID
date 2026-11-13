package com.mas.alsalah.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mas.alsalah.data.LocalData
import com.mas.alsalah.data.Settings
import com.mas.alsalah.data.SettingsStore
import com.mas.alsalah.domain.location.EgyptLocations
import com.mas.alsalah.notifications.AlarmScheduler
import com.mas.alsalah.notifications.NotificationHelper
import com.mas.alsalah.utils.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore.get(app)
    val settings: StateFlow<Settings> = store.state
    val data = LocalData(app)

    val pendingRoute = MutableStateFlow<String?>(null)
    val message = MutableStateFlow<String?>(null)
    val gpsBusy = MutableStateFlow(false)

    init {
        // إعادة الجدولة فقط عند تغيّر ما يؤثر على المواقيت/التنبيهات (لا عند تغيير المظهر)
        viewModelScope.launch(Dispatchers.Default) {
            store.state
                .map { listOf(it.lat, it.lon, it.method, it.asr, it.corr, it.notif, it.zoneMode, it.use24, it.arabicDigits, it.placeName) }
                .distinctUntilChanged()
                .debounce(300)
                .collect { AlarmScheduler.rescheduleAll(getApplication<Application>()) }
        }
    }

    fun update(block: (Settings) -> Settings) = store.update(block)
    fun resetAll() { store.reset(); toast("تمت إعادة الإعدادات الافتراضية") }
    fun toast(m: String) { message.value = m }
    fun consumeRoute() { pendingRoute.value = null }

    /** اختبار فعلي للإشعار والصوت المختار لهذه الصلاة (يستخدم نفس مسار التنبيه الحقيقي). */
    fun test(idx: Int) {
        val app = getApplication<Application>()
        if (!Perms.notificationsOk(app)) toast("الإشعارات غير مفعّلة للتطبيق؛ فعّلها من إعدادات النظام لسماع/رؤية الاختبار")
        NotificationHelper.fire(app, idx, 1, test = true)
    }

    // ---- GPS: طلب واحد فقط عند الضغط، بلا تتبع مستمر ----
    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        val app = getApplication<Application>()
        if (!Perms.locationOk(app)) { toast("لم يتم منح إذن الموقع"); return }
        gpsBusy.value = true
        try {
            val client = LocationServices.getFusedLocationProviderClient(app)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    if (loc != null) applyLocation(loc)
                    else client.lastLocation.addOnSuccessListener { l2 -> if (l2 != null) applyLocation(l2) else lastKnownFallback() }
                        .addOnFailureListener { lastKnownFallback() }
                }
                .addOnFailureListener { lastKnownFallback() }
        } catch (e: Exception) { lastKnownFallback() }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownFallback() {
        val app = getApplication<Application>()
        try {
            val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
            if (loc != null) applyLocation(loc) else fail()
        } catch (e: Exception) { fail() }
    }

    private fun fail() {
        gpsBusy.value = false
        toast("تعذّر تحديد الموقع. فعّل خدمة الموقع أو اختر المحافظة يدويًا.")
    }

    private fun applyLocation(l: Location) {
        val (gov, km) = EgyptLocations.nearest(l.latitude, l.longitude)
        val name = if (km < 400) "موقعي (قرب ${gov.city})" else "موقعي الحالي"
        store.update { it.copy(lat = l.latitude, lon = l.longitude, placeName = name, locMode = 1, govId = -1) }
        gpsBusy.value = false
        toast("تم تحديد موقعك وتحديث المواقيت")
    }
}
