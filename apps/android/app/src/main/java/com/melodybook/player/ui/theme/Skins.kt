package com.melodybook.player.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 皮肤 = 一个主色 + 一个点缀色，其余颜色全部由规则推导。
 *
 * 旧版的“皮肤”是 21 张背景图，文字颜色固定，换到深色图时文字就看不清。
 * 这里把皮肤变成配色方案：所有组件都从同一套 ColorScheme 取色，
 * 浅色 / 深色模式各自保证对比度，风格天然统一。
 * 名字沿用旧版的皮肤名。
 */
data class Skin(
    val id: String,
    val name: String,
    val primary: Color,
    val accent: Color,
)

object Skins {
    /** 特殊皮肤：Android 12+ 跟随壁纸取色（Material You）。 */
    const val DYNAMIC_ID = "dynamic"

    val all: List<Skin> = listOf(
        Skin("default", "绿意", Color(0xFF00A88A), Color(0xFF5B8DEF)),
        Skin("milktea", "奶茶", Color(0xFFA0704C), Color(0xFFD9A273)),
        Skin("cat", "白猫", Color(0xFF5F6B7A), Color(0xFF9AA8B8)),
        Skin("orange", "橙沙", Color(0xFFE07A2E), Color(0xFFF2B33D)),
        Skin("pink", "粉黛", Color(0xFFD65A8A), Color(0xFFF29BB5)),
        Skin("calm", "静谧", Color(0xFF3F6FB5), Color(0xFF79A7E0)),
        Skin("mountain", "千山", Color(0xFF3E7D5A), Color(0xFF8DBF7A)),
        Skin("cloud", "云端", Color(0xFF4E9CC9), Color(0xFFA5D3EE)),
        Skin("autumn", "秋意", Color(0xFFB5542E), Color(0xFFD9A441)),
        Skin("dream", "眠梦", Color(0xFF7059C2), Color(0xFFB39DFA)),
        Skin("pine", "雪松", Color(0xFF2F7F7A), Color(0xFF7FC4BB)),
        Skin("candy", "奶糖", Color(0xFFC77D9B), Color(0xFFE8C07D)),
    )

    fun byId(id: String): Skin = all.firstOrNull { it.id == id } ?: all.first()
}

/** 由两个种子色推导出完整的 Material 3 配色。 */
fun Skin.colorScheme(dark: Boolean): ColorScheme =
    if (dark) darkSchemeFrom(primary, accent) else lightSchemeFrom(primary, accent)

private fun Color.mix(other: Color, fraction: Float) = lerp(this, other, fraction)

private fun lightSchemeFrom(p: Color, a: Color): ColorScheme {
    val white = Color.White
    val ink = Color(0xFF15191A)
    return lightColorScheme(
        primary = p,
        onPrimary = white,
        primaryContainer = p.mix(white, 0.82f),
        onPrimaryContainer = p.mix(ink, 0.6f),
        secondary = p.mix(ink, 0.25f),
        onSecondary = white,
        secondaryContainer = p.mix(white, 0.88f),
        onSecondaryContainer = p.mix(ink, 0.65f),
        tertiary = a,
        onTertiary = white,
        tertiaryContainer = a.mix(white, 0.8f),
        onTertiaryContainer = a.mix(ink, 0.6f),
        background = p.mix(white, 0.985f),
        onBackground = ink,
        surface = p.mix(white, 0.985f),
        onSurface = ink,
        surfaceVariant = p.mix(white, 0.93f),
        onSurfaceVariant = p.mix(Color(0xFF444B4D), 0.85f),
        surfaceContainerLowest = white,
        surfaceContainerLow = p.mix(white, 0.975f),
        surfaceContainer = p.mix(white, 0.96f),
        surfaceContainerHigh = p.mix(white, 0.94f),
        surfaceContainerHighest = p.mix(white, 0.92f),
        outline = p.mix(Color(0xFF747B7D), 0.85f),
        outlineVariant = p.mix(white, 0.8f),
    )
}

private fun darkSchemeFrom(p: Color, a: Color): ColorScheme {
    val bg = Color(0xFF101415)
    val light = Color(0xFFE6EBEC)
    val pLight = p.mix(Color.White, 0.35f)
    return darkColorScheme(
        primary = pLight,
        onPrimary = p.mix(Color.Black, 0.65f),
        primaryContainer = p.mix(bg, 0.55f),
        onPrimaryContainer = p.mix(Color.White, 0.8f),
        secondary = pLight.mix(light, 0.4f),
        onSecondary = p.mix(Color.Black, 0.7f),
        secondaryContainer = p.mix(bg, 0.7f),
        onSecondaryContainer = p.mix(Color.White, 0.82f),
        tertiary = a.mix(Color.White, 0.3f),
        onTertiary = a.mix(Color.Black, 0.7f),
        tertiaryContainer = a.mix(bg, 0.6f),
        onTertiaryContainer = a.mix(Color.White, 0.8f),
        background = p.mix(bg, 0.97f),
        onBackground = light,
        surface = p.mix(bg, 0.97f),
        onSurface = light,
        surfaceVariant = p.mix(bg, 0.86f),
        onSurfaceVariant = p.mix(Color(0xFFC0C8CA), 0.85f),
        surfaceContainerLowest = Color(0xFF0B0E0F),
        surfaceContainerLow = p.mix(bg, 0.955f),
        surfaceContainer = p.mix(bg, 0.94f),
        surfaceContainerHigh = p.mix(bg, 0.915f),
        surfaceContainerHighest = p.mix(bg, 0.89f),
        outline = p.mix(Color(0xFF8A9294), 0.85f),
        outlineVariant = p.mix(bg, 0.7f),
    )
}
