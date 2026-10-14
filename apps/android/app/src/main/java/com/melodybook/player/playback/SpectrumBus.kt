package com.melodybook.player.playback

/**
 * 音频处理器（在播放线程）和频谱界面（在主线程）之间的传话筒。
 * 播放服务和界面在同一个进程，所以直接用进程内单例，不需要 IPC。
 *
 * 每一帧带一个“该被听到的时间”。界面按当前时间取对应的帧，而不是取最新的帧，
 * 这样音频缓冲区提前处理的那几百毫秒不会让画面跑在声音前面。
 */
object SpectrumBus {
    private class Frame(val timeNs: Long, val levels: FloatArray)

    private val lock = Any()
    private val frames = ArrayDeque<Frame>()

    /** 频谱界面可见且开关打开时才为 true；false 时处理器完全跳过分析，不耗 CPU。 */
    @Volatile
    var active: Boolean = false
        private set

    fun setActive(value: Boolean) {
        active = value
        if (!value) clear()
    }

    fun publish(timeNs: Long, levels: FloatArray) {
        synchronized(lock) {
            frames.addLast(Frame(timeNs, levels))
            while (frames.size > MAX_FRAMES) frames.removeFirst()
        }
    }

    /** 取时间不晚于 [targetNs] 的最新一帧，并丢弃更早的帧。还没有这样的帧时返回 null。 */
    fun frameAt(targetNs: Long): FloatArray? = synchronized(lock) {
        var found: Frame? = null
        while (frames.isNotEmpty() && frames.first().timeNs <= targetNs) {
            found = frames.removeFirst()
        }
        // 把取出的帧放回队首，下一次没有更新的帧时还能继续用它
        found?.let { frames.addFirst(it) }
        found?.levels
    }

    fun clear() {
        synchronized(lock) { frames.clear() }
    }

    /** 约 3 秒的帧数（采样率 44.1kHz、每跳 512 个采样时约 86 帧/秒）。 */
    private const val MAX_FRAMES = 256
}
