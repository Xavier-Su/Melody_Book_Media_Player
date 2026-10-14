package com.melodybook.player.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 从封面像素里挑两个种子色（主色 + 点缀色），交给 [Skin.colorScheme] 推导整套配色。
 *
 * 不用 androidx.palette：这里只需要“最有代表性的两个颜色”，按色相分桶统计就够了，
 * 纯 Kotlin 实现也方便在 JVM 单元测试里验证。
 *
 * 规则：
 * - 每个像素按色相落进 [HUE_BUCKETS] 个桶里，权重 = 饱和度，越鲜艳的像素越能代表封面的“颜色”；
 * - 太暗、太亮、太灰的像素只算进“灰色”计数，不参与选色；
 * - 主色取权重最大的桶的平均色，点缀色取色相相差足够远的第二大桶，没有就用主色；
 * - 整张图几乎都是灰色（黑白封面）时返回 null，沿用皮肤配色。
 */
object CoverColors {
    data class Seeds(val primary: Int, val accent: Int)

    private const val HUE_BUCKETS = 24
    private const val MIN_SATURATION = 0.12f
    private const val MIN_LIGHTNESS = 0.08f
    private const val MAX_LIGHTNESS = 0.94f

    /** 有颜色的像素占比低于这个值，就当作黑白封面。 */
    private const val MIN_COLORFUL_SHARE = 0.05f

    /** 点缀色和主色的色相至少相差几个桶（每桶 15°）。 */
    private const val ACCENT_MIN_DISTANCE = 3

    /** 点缀色的桶权重至少是主色的多少，太少的颜色只是噪点。 */
    private const val ACCENT_MIN_WEIGHT = 0.15f

    fun extract(pixels: IntArray): Seeds? {
        if (pixels.isEmpty()) return null
        val weight = FloatArray(HUE_BUCKETS)
        val sumR = FloatArray(HUE_BUCKETS)
        val sumG = FloatArray(HUE_BUCKETS)
        val sumB = FloatArray(HUE_BUCKETS)
        val hsl = FloatArray(3)
        var colorful = 0

        for (p in pixels) {
            if ((p ushr 24) < 0x80) continue // 透明像素
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            rgbToHsl(r, g, b, hsl)
            if (hsl[1] < MIN_SATURATION || hsl[2] < MIN_LIGHTNESS || hsl[2] > MAX_LIGHTNESS) continue
            colorful++
            val bucket = ((hsl[0] / 360f) * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val w = hsl[1]
            weight[bucket] += w
            sumR[bucket] += r * w
            sumG[bucket] += g * w
            sumB[bucket] += b * w
        }
        if (colorful < pixels.size * MIN_COLORFUL_SHARE) return null

        val main = weight.indices.maxBy { weight[it] }
        val accentBucket = weight.indices
            .filter { hueDistance(it, main) >= ACCENT_MIN_DISTANCE && weight[it] >= weight[main] * ACCENT_MIN_WEIGHT }
            .maxByOrNull { weight[it] }

        fun average(i: Int): Int {
            val w = weight[i]
            return rgb((sumR[i] / w).toInt(), (sumG[i] / w).toInt(), (sumB[i] / w).toInt())
        }
        val primary = normalize(average(main))
        return Seeds(primary, accentBucket?.let { normalize(average(it)) } ?: primary)
    }

    /**
     * 封面颜色可能很暗、很亮或偏灰，直接当主色会让按钮看不清或发灰。
     * 把亮度压到中间区间，饱和度给个下限，保证推导出的配色可读。
     */
    internal fun normalize(color: Int): Int {
        val hsl = FloatArray(3)
        rgbToHsl((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF, hsl)
        hsl[1] = hsl[1].coerceAtLeast(0.3f)
        hsl[2] = hsl[2].coerceIn(0.35f, 0.6f)
        return hslToRgb(hsl)
    }

    private fun hueDistance(a: Int, b: Int): Int {
        val d = abs(a - b)
        return min(d, HUE_BUCKETS - d)
    }

    private fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    internal fun rgbToHsl(r: Int, g: Int, b: Int, out: FloatArray) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) {
            out[0] = 0f
            out[1] = 0f
        } else {
            out[1] = d / (1f - abs(2f * l - 1f))
            out[0] = when (mx) {
                rf -> 60f * (((gf - bf) / d) % 6f)
                gf -> 60f * ((bf - rf) / d + 2f)
                else -> 60f * ((rf - gf) / d + 4f)
            }.let { if (it < 0f) it + 360f else it }
        }
        out[2] = l
    }

    internal fun hslToRgb(hsl: FloatArray): Int {
        val (h, s, l) = Triple(hsl[0], hsl[1], hsl[2])
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return rgb(((r + m) * 255f + 0.5f).toInt(), ((g + m) * 255f + 0.5f).toInt(), ((b + m) * 255f + 0.5f).toInt())
    }
}
