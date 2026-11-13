package com.mas.alsalah.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mas.alsalah.MainActivity
import com.mas.alsalah.R
import com.mas.alsalah.data.SOUND_SILENT
import com.mas.alsalah.data.SOUND_TAKBEER
import com.mas.alsalah.data.SOUND_TONE
import com.mas.alsalah.data.SettingsStore
import com.mas.alsalah.domain.prayer.Prayer
import com.mas.alsalah.services.TakbeerService
import com.mas.alsalah.utils.Fmt

object NotificationHelper {
    const val TAKBEER_TEXT = "الله أكبر، الله أكبر"

    fun channelId(pre: Boolean, sound: Boolean, vib: Boolean) =
        "${if (pre) "pre" else "now"}_${if (sound) "snd" else "mute"}_${if (vib) "vib" else "novib"}_v1"

    fun notifId(idx: Int, type: Int) = 1000 + idx * 2 + type

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        for (pre in listOf(true, false)) for (snd in listOf(true, false)) for (vib in listOf(true, false)) {
            val name = (if (pre) "قبل الصلاة" else "وقت الصلاة") + " — " +
                (if (snd) "بنغمة" else "بدون نغمة") + " — " + (if (vib) "مع اهتزاز" else "بدون اهتزاز")
            val ch = NotificationChannel(channelId(pre, snd, vib), name, NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(vib)
            if (snd) ch.setSound(uri, attrs) else ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
    }

    /** type: 0 قبل الصلاة، 1 وقت الصلاة. forceTone: عند فشل تشغيل خدمة التكبير. */
    fun build(ctx: Context, idx: Int, type: Int, forceTone: Boolean = false): Notification {
        val s = SettingsStore.get(ctx).state.value
        val i = idx.coerceIn(0, 4)
        val n = s.notif[i]
        val pr = Prayer.FIVE[i]
        val pre = type == 0
        val title = if (pre) "اقتربت صلاة ${pr.ar}" else "صلاة ${pr.ar} الآن"
        val body = if (pre) "متبقي ${n.preMinutes} دقيقة على صلاة ${pr.ar}" else TAKBEER_TEXT
        val soundOn = if (pre) n.sound != SOUND_SILENT else (n.sound == SOUND_TONE || forceTone)
        val open = PendingIntent.getActivity(
            ctx, 77, Intent(ctx, MainActivity::class.java).putExtra("route", "home").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, channelId(pre, soundOn, n.vibrate))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(Fmt.digits(title, s.arabicDigits))
            .setContentText(Fmt.digits(body, s.arabicDigits))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
    }

    fun fire(ctx: Context, idx: Int, type: Int, test: Boolean = false) {
        ensureChannels(ctx)
        val s = SettingsStore.get(ctx).state.value
        val i = idx.coerceIn(0, 4)
        if (type == 1 && s.notif[i].sound == SOUND_TAKBEER) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, TakbeerService::class.java).putExtra(AlarmScheduler.EXTRA_PRAYER, i))
                return
            } catch (e: Exception) {
                post(ctx, i, type, build(ctx, i, type, forceTone = true)); return
            }
        }
        post(ctx, i, type, build(ctx, i, type))
    }

    private fun post(ctx: Context, idx: Int, type: Int, n: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try { NotificationManagerCompat.from(ctx).notify(notifId(idx, type), n) } catch (_: SecurityException) {}
    }
}
