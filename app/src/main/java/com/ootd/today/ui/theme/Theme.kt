package com.ootd.today.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 品牌扩展色板。
 *
 * Material 3 的 [androidx.compose.material3.ColorScheme] 只有 primary / secondary /
 * tertiary 三个强调色，不足以承载"撞色"设计（我们需要青、粉、黄、橙四个强调色同时出现，
 * 且各自要有对应的 container / onContainer）。因此用独立的扩展色板承载，
 * 通过 [LocalOotdColors] 暴露给 Composable。
 */
data class OotdExtendedColors(
    /** 荧光青：次级强调、进度、成功态。 */
    val cyan: Color,
    val cyanContainer: Color,
    val onCyanContainer: Color,
    /** 玫粉：喜爱、打卡、情绪强调。 */
    val pink: Color,
    val pinkContainer: Color,
    val onPinkContainer: Color,
    /** 柠檬黄：高亮标记、"今日"角标。 */
    val yellow: Color,
    val yellowContainer: Color,
    val onYellowContainer: Color,
    /** 珊瑚橙：预警、高温、热度。 */
    val orange: Color,
    /** 7 大品类专属色，用于衣橱筛选与统计图表。 */
    val categoryTop: Color,
    val categoryBottom: Color,
    val categoryOnePiece: Color,
    val categoryOuterwear: Color,
    val categoryShoes: Color,
    val categoryBag: Color,
    val categoryAccessory: Color,
    /** 卡片描边（撞色风格中常见的细边线）。 */
    val cardBorder: Color,
    /**
     * 品牌主渐变（天气卡、中间大按钮）。
     *
     * 深色模式必须用**深色版**，否则浅色渐变上放白字会对比度不足，
     * 这是深色模式走查最容易漏掉的一点。
     */
    val brandGradientStart: Color,
    val brandGradientEnd: Color,
    /** 统计概览卡渐变，同样深色模式用深色版。 */
    val statGradientStart: Color,
    val statGradientEnd: Color,
    /** 排名文字色（金/银/铜），浅色模式用深色版保证白底可读。 */
    val rankFirst: Color,
    val rankSecond: Color,
    val rankThird: Color,
    /** 卡片背景上可用的强调文字色（如单次穿着成本）。 */
    val accentText: Color
)

private val LightExtendedColors = OotdExtendedColors(
    cyan = NeonCyan,
    cyanContainer = CyanContainer,
    onCyanContainer = OnCyanContainer,
    pink = HotPink,
    pinkContainer = PinkContainer,
    onPinkContainer = OnPinkContainer,
    yellow = LemonYellow,
    yellowContainer = YellowContainer,
    onYellowContainer = OnYellowContainer,
    orange = CoralOrange,
    categoryTop = CatTop,
    categoryBottom = CatBottom,
    categoryOnePiece = CatOnePiece,
    categoryOuterwear = CatOuterwear,
    categoryShoes = CatShoes,
    categoryBag = CatBag,
    categoryAccessory = CatAccessory,
    cardBorder = OutlineVariantLight,
    brandGradientStart = ElectricViolet,
    brandGradientEnd = Color(0xFF566A46),
    statGradientStart = Color(0xFF344E41),
    statGradientEnd = ElectricViolet,
    rankFirst = Color(0xFF9A7400),
    rankSecond = Color(0xFF00796B),
    rankThird = Color(0xFFC2185B),
    accentText = Color(0xFF00796B)
)

private val DarkExtendedColors = OotdExtendedColors(
    cyan = NeonCyanDark,
    cyanContainer = Color(0xFF294635),
    onCyanContainer = Color(0xFFC8FFF5),
    pink = HotPinkDark,
    pinkContainer = Color(0xFF604334),
    onPinkContainer = Color(0xFFFFD9E7),
    yellow = LemonYellowDark,
    yellowContainer = Color(0xFF5C4300),
    onYellowContainer = Color(0xFFFFEFC2),
    orange = CoralOrangeDark,
    categoryTop = ElectricVioletDark,
    categoryBottom = NeonCyanDark,
    categoryOnePiece = HotPinkDark,
    categoryOuterwear = CoralOrangeDark,
    categoryShoes = LemonYellowDark,
    categoryBag = Color(0xFFB3C4A2),
    categoryAccessory = Color(0xFFAACDC1),
    cardBorder = OutlineVariantDark,
    // Dark-mode gradients use deep colors to ensure white text is readable.
    brandGradientStart = Color(0xFF304C39),
    brandGradientEnd = Color(0xFF3E5535),
    statGradientStart = Color(0xFF294635),
    statGradientEnd = Color(0xFF304C39),
    rankFirst = LemonYellowDark,
    rankSecond = NeonCyanDark,
    rankThird = HotPinkDark,
    accentText = NeonCyanDark
)

val LocalOotdColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColors = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = VioletContainer,
    onPrimaryContainer = OnVioletContainer,
    inversePrimary = ElectricVioletDark,
    secondary = NeonCyan,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = HotPink,
    onTertiary = Color.White,
    tertiaryContainer = PinkContainer,
    onTertiaryContainer = OnPinkContainer,
    background = Ivory,
    onBackground = Color(0xFF263329),
    surface = SurfaceLight,
    onSurface = Color(0xFF263329),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF71796B),
    surfaceTint = ElectricViolet,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerLow = Color(0xFFF2F3EB),
    surfaceContainerHigh = SurfaceVariantLight,
    inverseSurface = Color(0xFF2B3B30),
    inverseOnSurface = Color(0xFFF4F2FA),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = ElectricVioletDark,
    onPrimary = Color(0xFF1C331D),
    primaryContainer = Color(0xFF304C39),
    onPrimaryContainer = VioletContainer,
    inversePrimary = ElectricViolet,
    secondary = NeonCyanDark,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF294635),
    onSecondaryContainer = CyanContainer,
    tertiary = HotPinkDark,
    onTertiary = Color(0xFF35251D),
    tertiaryContainer = Color(0xFF3E5535),
    onTertiaryContainer = PinkContainer,
    background = InkBlack,
    onBackground = Color(0xFFE5EADD),
    surface = SurfaceDark,
    onSurface = Color(0xFFE5EADD),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC3CDBB),
    surfaceTint = ElectricVioletDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    inverseSurface = Color(0xFFE5EADD),
    inverseOnSurface = Color(0xFF2B3B30),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRedDark,
    onError = Color(0xFF690009),
    errorContainer = Color(0xFF930014),
    onErrorContainer = ErrorContainer,
    scrim = Color.Black
)

/**
 * 「今日穿搭」应用主题。
 *
 * @param darkTheme 是否使用深色配色，默认跟随系统。
 * @param content 主题作用域内的内容。
 *
 * 注意：这里**刻意不启用** Material You 动态取色（`dynamicColor`）。
 * 动态取色会从壁纸提取颜色，直接冲垮我们精心设计的撞色品牌识别，
 * 对一款强调视觉调性的时尚类 App 来说弊大于利。
 */
@Composable
fun OotdTodayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏/导航栏透明，由 Compose 内容延伸至边缘（edge-to-edge）。
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalOotdColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OotdTypography,
            shapes = OotdShapes,
            content = content
        )
    }
}

/**
 * 访问品牌扩展色板，用法与 `MaterialTheme.colorScheme` 一致：
 * `OotdColors.pink`、`OotdColors.categoryTop` 等。
 */
object OotdColors {
    val current: OotdExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOotdColors.current
}
