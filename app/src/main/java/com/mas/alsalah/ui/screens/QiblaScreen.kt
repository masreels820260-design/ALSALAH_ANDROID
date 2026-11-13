package com.mas.alsalah.ui.screens

import android.content.Context
import android.graphics.Paint
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mas.alsalah.data.Settings
import com.mas.alsalah.domain.location.EgyptLocations
import com.mas.alsalah.domain.qibla.Qibla
import com.mas.alsalah.ui.components.*
import com.mas.alsalah.ui.theme.LocalGlass
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(s: Settings) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val bearing = remember(s.lat, s.lon) { Qibla.bearing(s.lat, s.lon).toFloat() }
    val decl = remember(s.lat, s.lon) { GeomagneticField(s.lat.toFloat(), s.lon.toFloat(), 0f, System.currentTimeMillis()).declination }
    var heading by remember { mutableFloatStateOf(0f) }
    var manual by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    // يسجّل المستشعر فقط أثناء ظهور الشاشة ويُلغى فورًا عند الخروج
    LifecycleResumeEffect(sensor) {
        val rm = FloatArray(9)
        val o = FloatArray(3)
        var cur = Float.NaN
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rm, e.values)
                SensorManager.getOrientation(rm, o)
                val az = (Math.toDegrees(o[0].toDouble()).toFloat() + decl + 360f) % 360f
                cur = if (cur.isNaN()) az else (cur + (((az - cur + 540f) % 360f) - 180f) * 0.18f + 360f) % 360f
                if (abs(Qibla.delta(cur.toDouble(), heading.toDouble())) > 0.7) heading = cur
            }
            override fun onAccuracyChanged(sensor: Sensor?, a: Int) { accuracy = a }
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { sm.unregisterListener(l) }
    }

    val hasSensor = sensor != null
    val hdg = if (hasSensor) heading else manual
    val diff = Qibla.delta(bearing.toDouble(), hdg.toDouble())
    val aligned = abs(diff) < 3.0
    LaunchedEffect(aligned) { if (aligned) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    val paint = remember(g.text, density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = g.text.toArgb(); textAlign = Paint.Align.CENTER; textSize = 18f * density.density; isFakeBoldText = true
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TopBar("القبلة")
        GText(s.placeName, Modifier.fillMaxWidth(), size = 14.sp, color = g.sub, align = TextAlign.Center)

        Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(300.dp)) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = -(if (hasSensor) heading else manual) }) {
                    val r = size.minDimension / 2
                    drawCircle(g.divider, radius = r)
                    drawCircle(g.sub, radius = r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    for (i in 0 until 72) {
                        val len = if (i % 6 == 0) 16.dp.toPx() else if (i % 2 == 0) 10.dp.toPx() else 6.dp.toPx()
                        rotate(i * 5f, center) {
                            drawLine(g.sub, Offset(center.x, center.y - r + 4.dp.toPx()), Offset(center.x, center.y - r + 4.dp.toPx() + len), 1.5.dp.toPx())
                        }
                    }
                    val lr = r - 46.dp.toPx()
                    val cv = drawContext.canvas.nativeCanvas
                    cv.drawText("N", center.x, center.y - lr + 6.dp.toPx(), paint)
                    cv.drawText("E", center.x + lr, center.y + 6.dp.toPx(), paint)
                    cv.drawText("S", center.x, center.y + lr + 6.dp.toPx(), paint)
                    cv.drawText("W", center.x - lr, center.y + 6.dp.toPx(), paint)
                    rotate(bearing, center) {
                        val p = Offset(center.x, center.y - r + 40.dp.toPx())
                        drawLine(g.accent, center, p, 3.dp.toPx())
                        drawCircle(g.accent, 13.dp.toPx(), p)
                        drawCircle(g.bg, 6.dp.toPx(), p)
                    }
                    drawCircle(g.accent, 6.dp.toPx(), center)
                }
                Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(center.x, -6.dp.toPx())
                        lineTo(center.x - 10.dp.toPx(), 14.dp.toPx())
                        lineTo(center.x + 10.dp.toPx(), 14.dp.toPx())
                        close()
                    }
                    drawPath(path, if (aligned) g.good else g.text)
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth(), strong = true) {
            GText(
                if (aligned) "أنت متجه نحو القبلة" else "أدر الهاتف حتى تنطبق علامة القبلة على المؤشر العلوي",
                Modifier.fillMaxWidth(), size = 16.sp, weight = FontWeight.SemiBold,
                color = if (aligned) g.good else g.text, align = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Info("اتجاه القبلة", "${bearing.roundToInt()}°", Modifier.weight(1f))
                Info("اتجاه الهاتف", "${hdg.roundToInt()}°", Modifier.weight(1f))
                Info("المسافة", "${EgyptLocations.distanceKm(s.lat, s.lon, Qibla.KAABA_LAT, Qibla.KAABA_LON).roundToInt()} كم", Modifier.weight(1f))
            }
        }

        if (!hasSensor) {
            GlassCard(Modifier.fillMaxWidth()) {
                GText("جهازك لا يحتوي على مستشعر بوصلة", size = 15.sp, weight = FontWeight.SemiBold)
                GText("زاوية القبلة من الشمال الحقيقي ${bearing.roundToInt()}°. حدّد يدويًا الاتجاه الذي يواجهه رأس هاتفك (بوصلة خارجية أو موضع الشمس) وسيدور الاتجاه المقابل.", size = 13.sp, color = g.sub)
                Slider(
                    value = manual, onValueChange = { manual = it }, valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = g.accent, activeTrackColor = g.accent, inactiveTrackColor = g.divider)
                )
            }
        } else if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            GlassCard(Modifier.fillMaxWidth()) {
                GText("المستشعر يحتاج إلى معايرة", size = 15.sp, weight = FontWeight.SemiBold)
                GText("حرّك الهاتف في الهواء على شكل رقم ٨ عدة مرات، وابتعد عن المعادن والمغناطيس والأجهزة الكهربائية.", size = 13.sp, color = g.sub)
            }
        }
        GText("ضع الهاتف أفقيًا ومسطحًا للحصول على أفضل دقة. الاتجاه مصحّح بالانحراف المغناطيسي للموقع.", size = 12.sp, color = g.sub)
    }
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier) {
    val g = LocalGlass.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GText(label, size = 12.sp, color = g.sub)
        GText(value, size = 18.sp, weight = FontWeight.Bold)
    }
}
