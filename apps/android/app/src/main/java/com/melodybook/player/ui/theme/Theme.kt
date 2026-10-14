package com.melodybook.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodybook.player.data.DarkMode

/**
 * 设计令牌：间距统一走 4dp 栅格，页面里不再出现零散的 5dp、8dp、15dp。
 */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/** 尺寸令牌：图标、封面、点击区域。点击区域至少 48dp（无障碍要求）。 */
@Immutable
data class Sizes(
    val touchTarget: Dp = 48.dp,
    val iconSm: Dp = 20.dp,
    val icon: Dp = 24.dp,
    val listArtwork: Dp = 48.dp,
    val miniPlayerArtwork: Dp = 44.dp,
    val playButton: Dp = 72.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalSizes = staticCompositionLocalOf { Sizes() }

private val MelodyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 字体层级：只用这几级，页面里不直接写 sp。 */
private val MelodyTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun MelodyTheme(
    skinId: String,
    darkMode: DarkMode,
    content: @Composable () -> Unit,
) {
    val dark = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = if (skinId == Skins.DYNAMIC_ID && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        Skins.byId(skinId).colorScheme(dark)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalSizes provides Sizes(),
        LocalIsDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MelodyTypography,
            shapes = MelodyShapes,
            content = content,
        )
    }
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }

object MelodyTokens {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
    val sizes: Sizes
        @Composable get() = LocalSizes.current
}
