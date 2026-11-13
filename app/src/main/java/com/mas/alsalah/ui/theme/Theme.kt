package com.mas.alsalah.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** ألوان نظام «الزجاج السائل». الفاتح أبيض فعلي والداكن أسود فعلي. الفرش تُنشأ مرة واحدة. */
@Immutable
class GlassColors(
    val dark: Boolean,
    val bg: Color,
    val text: Color,
    val sub: Color,
    val accent: Color,
    val onAccent: Color,
    val solid: Color,
    val divider: Color,
    val good: Color,
    val fill: Brush,
    val fillStrong: Brush,
    val border: Brush
)

val LightGlass = GlassColors(
    dark = false, bg = Color(0xFFFFFFFF), text = Color(0xFF111111), sub = Color(0xFF6B6B70),
    accent = Color(0xFF0A63D6), onAccent = Color.White, solid = Color(0xFFFFFFFF),
    divider = Color(0x1A000000), good = Color(0xFF1B8A3A),
    fill = Brush.verticalGradient(listOf(Color(0xCCF8F9FB), Color(0xCCF1F3F6))),
    fillStrong = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF4F6F9))),
    border = Brush.verticalGradient(listOf(Color(0xFFE3E6EB), Color(0xFFCFD3DA)))
)

val DarkGlass = GlassColors(
    dark = true, bg = Color(0xFF000000), text = Color(0xFFFFFFFF), sub = Color(0xFF9A9AA0),
    accent = Color(0xFF4DA3FF), onAccent = Color(0xFF00172E), solid = Color(0xFF1C1C1E),
    divider = Color(0x26FFFFFF), good = Color(0xFF55D678),
    fill = Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x14FFFFFF))),
    fillStrong = Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x1FFFFFFF))),
    border = Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x1FFFFFFF)))
)

val LocalGlass = staticCompositionLocalOf { LightGlass }
val LocalArabicDigits = compositionLocalOf { true }

val CardShape = RoundedCornerShape(24.dp)
val PillShape = RoundedCornerShape(50)
val DialogShape = RoundedCornerShape(28.dp)

@Composable
fun AlSalahTheme(dark: Boolean, arabicDigits: Boolean, content: @Composable () -> Unit) {
    val g = if (dark) DarkGlass else LightGlass
    val scheme = if (dark) darkColorScheme(
        primary = g.accent, onPrimary = g.onAccent, background = g.bg, onBackground = g.text,
        surface = g.bg, onSurface = g.text
    ) else lightColorScheme(
        primary = g.accent, onPrimary = g.onAccent, background = g.bg, onBackground = g.text,
        surface = g.bg, onSurface = g.text
    )
    CompositionLocalProvider(
        LocalGlass provides g,
        LocalArabicDigits provides arabicDigits,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
