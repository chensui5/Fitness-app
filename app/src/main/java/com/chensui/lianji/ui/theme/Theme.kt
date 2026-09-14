package com.chensui.lianji.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* ---------- 浅色 ---------- */
val LightPrimary = Color(0xFF2E7D5B)
val LightPrimaryContainer = Color(0xFFD7EFE4)
val LightBg = Color(0xFFF4F6F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFECEFED)
val LightOnSurface = Color(0xFF1A1C1B)
val LightOnSurfaceVariant = Color(0xFF5A615D)
val LightOutline = Color(0xFFDDE2DF)

/* ---------- 深色 ---------- */
val DarkPrimary = Color(0xFF7BC9A6)
val DarkPrimaryContainer = Color(0xFF23453A)
val DarkBg = Color(0xFF111413)
val DarkSurface = Color(0xFF1B1F1D)
val DarkSurfaceVariant = Color(0xFF262B29)
val DarkOnSurface = Color(0xFFE8EBE9)
val DarkOnSurfaceVariant = Color(0xFFA8B0AC)
val DarkOutline = Color(0xFF323836)

/* ---------- 语义色 ---------- */
val CoinGold = Color(0xFFE0A32E)
val DoneGreen = Color(0xFF2E7D5B)
val RestGray = Color(0xFF8A938E)
val WarnOrange = Color(0xFFD98324)

/* ---------- 营养素与餐次配色（用于饮食页做视觉区分） ---------- */
val ProteinBlue = Color(0xFF3E86D8)
val CarbOrange = Color(0xFFE08A0C)
val FatPurple = Color(0xFF8B6BE0)
val CalorieRed = Color(0xFFD9534F)

/** 餐次颜色，浅色/深色主题下都有足够对比度 */
val MealBreakfast = Color(0xFFE8952C)
val MealLunch = Color(0xFF2E9E6B)
val MealDinner = Color(0xFF4A7DD6)
val MealSnack = Color(0xFF9B6BD6)

fun mealColor(meal: String): Color = when (meal) {
    "早餐" -> MealBreakfast
    "午餐" -> MealLunch
    "晚餐" -> MealDinner
    else -> MealSnack
}

@Composable
fun nutrientColor(kind: Int): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (kind) {
        0 -> if (dark) Color(0xFF7FB2F0) else ProteinBlue
        1 -> if (dark) Color(0xFFF0B455) else CarbOrange
        2 -> if (dark) Color(0xFFB9A3F5) else FatPurple
        else -> if (dark) Color(0xFFF09A97) else CalorieRed
    }
}

/** 完成色随主题自适应，避免深色背景下绿色发闷 */
@Composable
fun doneColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) Color(0xFF6FCF9B) else DoneGreen
}

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF113D2C),
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF10301F),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFCDEFDD),
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

/** 精简排版：层级靠字重与字号拉开，不靠装饰 */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
)

/**
 * @param mode 0 跟随系统 / 1 浅色 / 2 深色
 */
@Composable
fun LianJiTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val scheme = if (dark) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
