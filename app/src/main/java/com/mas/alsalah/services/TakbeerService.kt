package com.mas.alsalah.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mas.alsalah.data.SettingsStore
import com.mas.alsalah.notifications.AlarmScheduler
import com.mas.alsalah.notifications.NotificationHelper
import java.util.Locale

/**
 * خدمة قصيرة (foreground / mediaPlayback) تنطق «الله أكبر، الله أكبر» عبر محرك النطق العربي في الجهاز،
 * وتستخدم نغمة الإشعار الافتراضية كبديل إذا لم تتوفر العربية. لا نستخدم تسجيلًا صوتيًا غير مرخّص.
 * مستوى الصوت يخص صوت النطق فقط، ولا نتجاوز وضع الصامت/عدم الإزعاج في النظام.
 */
class TakbeerService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tts != null) return START_NOT_STICKY
        val idx = intent?.getIntExtra(AlarmScheduler.EXTRA_PRAYER, 0) ?: 0
        val n = NotificationHelper.build(this, idx, 1)
        try {
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(NotificationHelper.notifId(idx, 1), n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NotificationHelper.notifId(idx, 1), n)
        } catch (e: Exception) {
            stopSelf(); return START_NOT_STICKY
        }
        handler.postDelayed({ finish() }, 15_000)
        requestFocus()
        tts = TextToSpeech(applicationContext, this)
        return START_NOT_STICKY
    }

    private fun requestFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).build()
        focus = req
        am.requestAudioFocus(req)
    }

    override fun onInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) { fallback(); return }
        val r = t.setLanguage(Locale("ar"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) { fallback(); return }
        t.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        )
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { handler.post { finish() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { handler.post { fallback() } }
        })
        val vol = SettingsStore.get(this).state.value.volume.coerceIn(0.05f, 1f)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol) }
        t.speak(NotificationHelper.TAKBEER_TEXT, TextToSpeech.QUEUE_FLUSH, params, "takbeer")
    }

    private fun fallback() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.also {
                if (Build.VERSION.SDK_INT >= 28) it.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                it.play()
            }
        } catch (_: Exception) {}
        handler.postDelayed({ finish() }, 3500)
    }

    private fun finish() {
        if (finished) return
        finished = true
        try { stopForeground(STOP_FOREGROUND_DETACH) } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        try { ringtone?.stop() } catch (_: Exception) {}
        focus?.let { (getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it) }
        super.onDestroy()
    }
}
