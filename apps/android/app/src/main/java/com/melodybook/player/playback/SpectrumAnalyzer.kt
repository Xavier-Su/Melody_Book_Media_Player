package com.melodybook.player.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 频谱分析：把单声道采样送进来，每攒够 [HOP] 个新采样就用最近 [SIZE] 个做一次 FFT，
 * 折成 [BANDS] 个对数间隔的频段，输出 0~1 的强度。
 *
 * 纯 Kotlin，不依赖 Android，可以直接单元测试。
 */
class SpectrumAnalyzer(sampleRate: Int) {

    private val ring = FloatArray(SIZE)
    private var write = 0
    private var filled = 0
    private var sinceLast = 0

    private val re = FloatArray(SIZE)
    private val im = FloatArray(SIZE)
    private val window = FloatArray(SIZE) { (0.5 - 0.5 * cos(2.0 * PI * it / (SIZE - 1))).toFloat() }
    private val cosTable = FloatArray(SIZE / 2) { cos(2.0 * PI * it / SIZE).toFloat() }
    private val sinTable = FloatArray(SIZE / 2) { sin(2.0 * PI * it / SIZE).toFloat() }
    private val reversed = IntArray(SIZE) { Integer.reverse(it) ushr (32 - LOG2_SIZE) }

    private val bandLo = IntArray(BANDS)
    private val bandHi = IntArray(BANDS)

    init {
        val fMax = min(F_MAX, sampleRate / 2f - 1f)
        val ratio = fMax / F_MIN
        for (b in 0 until BANDS) {
            val f0 = F_MIN * ratio.pow(b / BANDS.toFloat())
            val f1 = F_MIN * ratio.pow((b + 1) / BANDS.toFloat())
            val lo = (f0 * SIZE / sampleRate).toInt().coerceIn(1, SIZE / 2 - 1)
            val hi = kotlin.math.ceil(f1 * SIZE / sampleRate).toInt().coerceIn(lo + 1, SIZE / 2)
            bandLo[b] = lo
            bandHi[b] = hi
        }
    }

    fun reset() {
        ring.fill(0f)
        write = 0
        filled = 0
        sinceLast = 0
    }

    /** 送入一个采样（-1..1）。攒够一跳时返回一帧 [BANDS] 个强度，否则返回 null。 */
    fun push(sample: Float): FloatArray? {
        ring[write] = sample
        write = (write + 1) % SIZE
        if (filled < SIZE) filled++
        if (++sinceLast < HOP || filled < SIZE) return null
        sinceLast = 0
        return analyze()
    }

    private fun analyze(): FloatArray {
        for (i in 0 until SIZE) {
            re[i] = ring[(write + i) % SIZE] * window[i]
            im[i] = 0f
        }
        fft()

        val out = FloatArray(BANDS)
        for (b in 0 until BANDS) {
            var peak = 0f
            for (k in bandLo[b] until bandHi[b]) {
                val mag = sqrt(re[k] * re[k] + im[k] * im[k])
                if (mag > peak) peak = mag
            }
            // 汉宁窗下，幅度 A 的正弦在峰值处的模为 A * SIZE / 4
            val db = 20f * log10(peak / (SIZE / 4f) + 1e-9f) + TILT_DB * b / (BANDS - 1)
            out[b] = ((db - FLOOR_DB) / RANGE_DB).coerceIn(0f, 1f)
        }
        return out
    }

    private fun fft() {
        for (i in 0 until SIZE) {
            val j = reversed[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= SIZE) {
            val half = len / 2
            val step = SIZE / len
            var i = 0
            while (i < SIZE) {
                var k = 0
                for (j in 0 until half) {
                    val wr = cosTable[k]
                    val wi = -sinTable[k]
                    val a = i + j
                    val b = a + half
                    val tr = re[b] * wr - im[b] * wi
                    val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr
                    im[b] = im[a] - ti
                    re[a] += tr
                    im[a] += ti
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
    }

    companion object {
        const val BANDS = 24
        const val SIZE = 2048
        const val HOP = 512
        private const val LOG2_SIZE = 11
        private const val F_MIN = 50f
        private const val F_MAX = 16_000f

        /** 低于这个分贝当作 0，高于 FLOOR_DB + RANGE_DB 当作 1。 */
        private const val FLOOR_DB = -70f
        private const val RANGE_DB = 60f

        /** 音乐的高频能量天然比低频小，从低到高最多补 12dB，让右边的条不至于一直趴着。 */
        private const val TILT_DB = 12f
    }
}
