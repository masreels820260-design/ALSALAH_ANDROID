package com.mas.alsalah.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mas.alsalah.ui.components.GlassDialog
import com.mas.alsalah.ui.components.GlassField

@Composable
fun NumberDialog(title: String, initial: Int, min: Int, max: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var t by remember { mutableStateOf(initial.toString()) }
    GlassDialog(onDismiss, title, "حفظ", {
        t.toIntOrNull()?.let { onConfirm(it.coerceIn(min, max)); onDismiss() }
    }) {
        GlassField(t, { v -> t = v.filter { it.isDigit() }.take(7) }, "اكتب رقمًا", number = true)
    }
}
