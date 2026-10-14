package com.melodybook.player.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 插在 ExoPlayer 音频链里的“旁听”处理器：声音原样放行，只是顺便算一份频谱。
 *
 * 不用 android.media.audiofx.Visualizer，是因为它需要录音权限，
 * 一个本地播放器去申请“录音”很难让人放心。直接在 PCM 上做分析不需要任何权限。
 */
@OptIn(UnstableApi::class)
class SpectrumProcessor : BaseAudioProcessor() {

    private var analyzer: SpectrumAnalyzer? = null
    private var encoding = C.ENCODING_INVALID
    private var channels = 1
    private var hopNs = 0L
    private var lastStampNs = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val supported = encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT
        analyzer = if (supported) SpectrumAnalyzer(inputAudioFormat.sampleRate) else null
        hopNs = SpectrumAnalyzer.HOP * 1_000_000_000L / inputAudioFormat.sampleRate.coerceAtLeast(1)
        lastStampNs = 0L
        // 格式不变，其他编码也照样放行，只是不分析
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val a = analyzer
        if (a != null && SpectrumBus.active) {
            // duplicate 出来读，不动原缓冲区的位置
            analyze(a, inputBuffer.duplicate().order(ByteOrder.nativeOrder()))
        }
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    private fun analyze(a: SpectrumAnalyzer, buf: ByteBuffer) {
        val isFloat = encoding == C.ENCODING_PCM_FLOAT
        val frameBytes = (if (isFloat) 4 else 2) * channels
        while (buf.remaining() >= frameBytes) {
            var sum = 0f
            repeat(channels) {
                sum += if (isFloat) buf.getFloat() else buf.getShort() / 32768f
            }
            val levels = a.push(sum / channels) ?: continue
            // 开头缓冲区会一口气灌进来，所以每帧至少比上一帧晚一跳，
            // 这样帧的时间间隔和真实播放节奏一致，而不是挤在同一毫秒
            lastStampNs = maxOf(System.nanoTime(), lastStampNs + hopNs)
            SpectrumBus.publish(lastStampNs, levels)
        }
    }

    override fun onFlush() {
        analyzer?.reset()
        lastStampNs = 0L
        SpectrumBus.clear()
    }

    override fun onReset() {
        analyzer = null
        lastStampNs = 0L
    }
}
