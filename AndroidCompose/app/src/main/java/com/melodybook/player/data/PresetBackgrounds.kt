package com.melodybook.player.data

import android.content.Context

/**
 * 预置背景图。图片在构建时从旧工程复制到 assets/backgrounds/（见 app/build.gradle.kts），
 * 这里按文件列出，所以增删图片不需要改代码。名称沿用旧版的皮肤名。
 */
object PresetBackgrounds {
    private const val DIR = "backgrounds"

    private val names = mapOf(
        "bg.jpg" to "璀璨", "bg1.jpg" to "奶茶", "bg2.jpg" to "白猫", "bg3.jpg" to "绿意",
        "bg4.jpg" to "橙沙", "bg5.jpg" to "画盘", "bg6.jpg" to "奶糖", "bg7.jpg" to "静谧",
        "bg8.jpg" to "粉黛", "bg9.jpg" to "千山", "bg10.jpg" to "梨落", "bg11.jpg" to "云端",
        "bg12.jpg" to "秋意", "bg13.jpg" to "眠梦", "bg14.jpg" to "雪松", "bg15.jpg" to "曲径",
        "bg16.jpg" to "画境", "bg17.jpg" to "雪晶", "bg18.jpg" to "饼干", "bg19.jpg" to "绿缀",
        "bg20.jpg" to "嘻嘻",
    )

    data class Preset(val file: String, val name: String) {
        val url: String get() = assetUrl(file)
    }

    fun list(context: Context): List<Preset> {
        val files = runCatching { context.assets.list(DIR)?.toList() }.getOrNull().orEmpty()
        return files
            .filter { it.endsWith(".jpg", true) || it.endsWith(".png", true) || it.endsWith(".webp", true) }
            .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: -1 }
            .map { Preset(it, names[it] ?: it.substringBeforeLast('.')) }
    }

    /** Coil 支持 file:///android_asset/ 地址。 */
    fun assetUrl(file: String): String = "file:///android_asset/$DIR/$file"
}
