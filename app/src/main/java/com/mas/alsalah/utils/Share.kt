package com.mas.alsalah.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("text", text))
}

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
