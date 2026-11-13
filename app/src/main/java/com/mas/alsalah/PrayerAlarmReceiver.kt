package com.mas.alsalah

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: "الصلاة"
        val before = intent.getBooleanExtra("before", false)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (before) "prayer_before" else "prayer_now"
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, if (before) "تنبيهات قبل الصلاة" else "دخول وقت الصلاة", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "تنبيهات تطبيق الصلاة"
            manager.createNotificationChannel(channel)
        }
        val title = if (before) "اقتربت صلاة $prayer" else "صلاة $prayer الآن"
        val body = if (before) "استعد للصلاة" else "الله أكبر الله أكبر"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
        manager.notify((prayer + before).hashCode(), notification)

        if (!before) speakTakbeer(context, prayer)

        // Refill the rolling schedule so future days remain covered.
        val prefs = context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 30.0444f).toDouble()
        val lon = prefs.getFloat("lon", 31.2357f).toDouble()
        val lead = prefs.getInt("lead", 15)
        val enabled = BooleanArray(5) { prefs.getBoolean("enabled_$it", true) }
        AlarmScheduler.schedule(context, lat, lon, lead, enabled)
    }

    private fun speakTakbeer(context: Context, prayer: String) {
        val pending = goAsync()
        val finished = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        fun finish() {
            if (finished.compareAndSet(false, true)) pending.finish()
        }
        // Never keep the broadcast alive indefinitely if the device TTS engine hangs.
        handler.postDelayed({ finish() }, 15_000)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                runCatching { tts.shutdown() }
                finish()
                return@TextToSpeech
            }
            val language = runCatching { tts.setLanguage(Locale("ar")) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (language < TextToSpeech.LANG_AVAILABLE) {
                tts.shutdown()
                finish()
                return@TextToSpeech
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { tts.shutdown(); finish() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { tts.shutdown(); finish() }
            })
            val result = tts.speak("الله أكبر الله أكبر", TextToSpeech.QUEUE_FLUSH, null, "takbeer_$prayer")
            if (result == TextToSpeech.ERROR) { tts.shutdown(); finish() }
        }
    }
}
