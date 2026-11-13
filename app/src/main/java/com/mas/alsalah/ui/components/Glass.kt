package com.mas.alsalah.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mas.alsalah.ui.theme.*
import com.mas.alsalah.utils.Fmt

@Composable
fun Modifier.glass(shape: Shape = CardShape, strong: Boolean = false): Modifier {
    val g = LocalGlass.current
    return this
        .background(if (strong) g.fillStrong else g.fill, shape)
        .border(1.dp, g.border, shape)
}

@Composable
fun GText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val g = LocalGlass.current
    val ar = LocalArabicDigits.current
    Text(
        text = Fmt.digits(text, ar),
        modifier = modifier,
        color = if (color == Color.Unspecified) g.text else color,
        fontSize = size,
        fontWeight = weight,
        textAlign = align,
        maxLines = maxLines,
        lineHeight = lineHeight,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = modifier.glass(CardShape, strong)
    if (onClick != null) m = m.clip(CardShape).clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    prominent: Boolean = false,
    enabled: Boolean = true
) {
    val g = LocalGlass.current
    val base = if (prominent) modifier.clip(PillShape).background(g.accent) else modifier.glass(PillShape, true).clip(PillShape)
    Row(
        base.alpha(if (enabled) 1f else 0.4f).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (prominent) g.onAccent else g.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        GText(text, size = 15.sp, weight = FontWeight.SemiBold, color = if (prominent) g.onAccent else g.text)
    }
}

@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    val m = if (selected) Modifier.clip(PillShape).background(g.accent) else Modifier.glass(PillShape).clip(PillShape)
    Box(m.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        GText(text, size = 14.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) g.onAccent else g.text)
    }
}

@Composable
fun GlassSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Row(modifier.fillMaxWidth().glass(PillShape).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(PillShape)
                    .background(if (sel) g.accent else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { GText(o, size = 14.sp, weight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) g.onAccent else g.text) }
        }
    }
}

@Composable
fun GlassDivider() {
    val g = LocalGlass.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(g.divider))
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            GText(title, size = 16.sp)
            if (subtitle != null) GText(subtitle, size = 12.sp, color = g.sub)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = g.accent, checkedThumbColor = g.onAccent,
                uncheckedTrackColor = g.divider, uncheckedThumbColor = g.sub, uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingRow(title: String, value: String? = null, subtitle: String? = null, onClick: (() -> Unit)? = null, chevron: Boolean = onClick != null) {
    val g = LocalGlass.current
    val m = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(m.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            GText(title, size = 16.sp)
            if (subtitle != null) GText(subtitle, size = 12.sp, color = g.sub)
        }
        if (value != null) { GText(value, size = 14.sp, color = g.sub); Spacer(Modifier.width(6.dp)) }
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = g.sub, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = g.text) }
        else Spacer(Modifier.width(12.dp))
        GText(title, Modifier.weight(1f), size = 22.sp, weight = FontWeight.Bold)
        trailing?.invoke()
    }
}

@Composable
fun SectionTitle(text: String) {
    val g = LocalGlass.current
    GText(text, Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp, top = 4.dp), size = 13.sp, weight = FontWeight.SemiBold, color = g.sub)
}

@Composable
fun GlassField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false) {
    val g = LocalGlass.current
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        textStyle = TextStyle(color = g.text, fontSize = 16.sp),
        cursorBrush = SolidColor(g.accent),
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
        modifier = modifier.fillMaxWidth().glass(PillShape).padding(horizontal = 16.dp, vertical = 13.dp),
        decorationBox = { inner -> Box { if (value.isEmpty()) GText(hint, color = g.sub); inner() } }
    )
}

@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    title: String,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val g = LocalGlass.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(DialogShape).background(g.solid).border(1.dp, g.border, DialogShape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GText(title, size = 19.sp, weight = FontWeight.Bold)
            content()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (confirmText != null && onConfirm != null) GlassButton(confirmText, onConfirm, Modifier.weight(1f), prominent = true)
                GlassButton(if (confirmText != null) "إلغاء" else "إغلاق", onDismiss, Modifier.weight(1f))
            }
        }
    }
}

/** ورقة سفلية زجاجية (قائمة اختيار). */
@Composable
fun GlassSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGlass.current
    val none = remember { MutableInteractionSource() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0x66000000)).clickable(interactionSource = none, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sh = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            Column(
                Modifier.fillMaxWidth().clip(sh).background(g.solid).border(1.dp, g.border, sh)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GText(title, size = 18.sp, weight = FontWeight.Bold)
                content()
            }
        }
    }
}

@Composable
fun PickerSheet(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val g = LocalGlass.current
    GlassSheet(onDismiss, title) {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            itemsIndexed(options) { i, o ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(i); onDismiss() }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GText(o, Modifier.weight(1f), size = 16.sp, color = if (i == selected) g.accent else g.text)
                    if (i == selected) Icon(Icons.Default.Check, null, tint = g.accent)
                }
            }
        }
    }
}

@Composable
fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit, min: Int, max: Int, suffix: String = "") {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        GText(label, Modifier.weight(1f), size = 16.sp)
        StepBtn("−") { if (value > min) onChange(value - 1) }
        GText((if (value > 0) "+" else "") + value + suffix, Modifier.widthIn(min = 64.dp), size = 15.sp, color = g.text, align = TextAlign.Center)
        StepBtn("+") { if (value < max) onChange(value + 1) }
    }
}

@Composable
private fun StepBtn(t: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).glass(CircleShape, true).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        GText(t, size = 20.sp, weight = FontWeight.SemiBold)
    }
}
