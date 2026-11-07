package com.app_health.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AppColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppPrimaryLight,
    background = AppBackground,
    surface = AppSurface,
    surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFFF0F5F4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF3F8F6),
    surfaceContainerHighest = Color(0xFFEAF5F0),
    primaryContainer = Color(0xFFE1F7EF),
    onPrimaryContainer = Color(0xFF086651),
    secondaryContainer = Color(0xFFE9F7F2),
    onSecondaryContainer = Color(0xFF126651),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFAFBCB8),
    outlineVariant = Color(0xFFE6ECEA),
    error = Color(0xFFCE4254),
    errorContainer = Color(0xFFFFEDF0),
    onErrorContainer = Color(0xFF822A39),
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AppHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Tắt dynamic color để ứng dụng ưu tiên hiển thị màu Mint
    content: @Composable () -> Unit
) {
    // Tạm thời áp dụng chung 1 bảng màu sáng (Mint) cho toàn bộ app
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
