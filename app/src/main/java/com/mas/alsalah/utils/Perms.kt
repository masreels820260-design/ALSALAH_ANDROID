package com.mas.alsalah.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Perms {
    fun notificationsOk(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun exactOk(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun locationOk(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
