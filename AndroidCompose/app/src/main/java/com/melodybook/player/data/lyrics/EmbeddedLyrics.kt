package com.melodybook.player.data.lyrics

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * 从音频文件里读内嵌歌词，纯 JVM 实现，不依赖第三方库。
 * 支持：MP3 的 ID3v2 USLT 帧、FLAC 的 Vorbis Comment（LYRICS / UNSYNCEDLYRICS）。
 * 读取有大小上限，异常一律返回 null，不会让播放页崩溃。
 */
object EmbeddedLyrics {

    private const val MAX_TAG_BYTES = 4 * 1024 * 1024

    fun read(format: String, open: () -> InputStream?): String? = try {
        when (format) {
            "mp3" -> open()?.use { readId3(it) }
            "flac" -> open()?.use { readFlac(it) }
            else -> null
        }
    } catch (e: IOException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    // ---------------------------------------------------------------- ID3v2

    internal fun readId3(input: InputStream): String? {
        val header = ByteArray(10)
        if (!readFully(input, header) || header[0] != 'I'.code.toByte() ||
            header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()
        ) {
            return null
        }
        val version = header[3].toInt()
        val flags = header[5].toInt()
        val size = syncSafe(header, 6)
        if (version !in 2..4 || size <= 0 || size > MAX_TAG_BYTES) return null

        var tag = ByteArray(size)
        if (!readFully(input, tag)) return null
        // 标签级的 unsynchronisation（v2.3 常见）：FF 00 -> FF
        if (flags and 0x80 != 0 && version < 4) tag = deUnsync(tag)

        var pos = 0
        // 跳过扩展头
        if (flags and 0x40 != 0 && version >= 3) {
            val ext = if (version == 4) syncSafe(tag, 0) else int32(tag, 0) + 4
            pos = ext.coerceIn(0, tag.size)
        }

        val idLen = if (version == 2) 3 else 4
        val headerLen = if (version == 2) 6 else 10
        val target = if (version == 2) "ULT" else "USLT"

        while (pos + headerLen <= tag.size) {
            if (tag[pos] == 0.toByte()) break // padding
            val id = String(tag, pos, idLen, Charsets.ISO_8859_1)
            val frameSize = when (version) {
                2 -> ((tag[pos + 3].toInt() and 0xFF) shl 16) or
                    ((tag[pos + 4].toInt() and 0xFF) shl 8) or (tag[pos + 5].toInt() and 0xFF)
                3 -> int32(tag, pos + 4)
                else -> syncSafe(tag, pos + 4)
            }
            val frameFlags = if (version == 2) 0 else (tag[pos + 9].toInt() and 0xFF)
            val start = pos + headerLen
            if (frameSize <= 0 || start + frameSize > tag.size) break
            if (id == target) {
                var body = tag.copyOfRange(start, start + frameSize)
                // v2.4 帧级 unsynchronisation / 数据长度指示
                if (version == 4) {
                    if (frameFlags and 0x01 != 0 && body.size >= 4) body = body.copyOfRange(4, body.size)
                    if (frameFlags and 0x02 != 0) body = deUnsync(body)
                }
                parseUslt(body)?.let { return it }
            }
            pos = start + frameSize
        }
        return null
    }

    /** USLT: 编码(1) 语言(3) 描述(以 0 结尾) 歌词 */
    private fun parseUslt(body: ByteArray): String? {
        if (body.size < 5) return null
        val enc = body[0].toInt()
        var i = 4
        val wide = enc == 1 || enc == 2
        // 跳过描述
        if (wide) {
            while (i + 1 < body.size && !(body[i] == 0.toByte() && body[i + 1] == 0.toByte())) i += 2
            i += 2
        } else {
            while (i < body.size && body[i] != 0.toByte()) i++
            i += 1
        }
        if (i >= body.size) return null
        val text = decodeId3Text(enc, body.copyOfRange(i, body.size))
        // 去掉结尾的 0 字节（Char(0)，避免在源码里写不可见字符）
        return text.trim { it.isWhitespace() || it.code == 0 }.ifBlank { null }
    }

    private fun decodeId3Text(enc: Int, bytes: ByteArray): String = when (enc) {
        // 规范上 0 是 Latin-1，但国内很多标签实际写的是 GBK，按自动识别处理
        0 -> TextDecoding.decode(bytes)
        1 -> when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            else -> String(bytes, Charsets.UTF_16LE)
        }
        2 -> String(bytes, Charsets.UTF_16BE)
        else -> String(bytes, Charsets.UTF_8)
    }

    // ---------------------------------------------------------------- FLAC

    internal fun readFlac(input: InputStream): String? {
        val data = DataInputStream(input)
        val magic = ByteArray(4)
        if (!readFully(data, magic) || String(magic, Charsets.ISO_8859_1) != "fLaC") return null
        var total = 0
        while (true) {
            val head = data.readInt()
            val last = head ushr 31 == 1
            val type = (head ushr 24) and 0x7F
            val len = head and 0xFFFFFF
            total += len
            if (total > MAX_TAG_BYTES * 4) return null
            if (type == 4) {
                if (len > MAX_TAG_BYTES) return null
                val block = ByteArray(len)
                if (!readFully(data, block)) return null
                return parseVorbisComment(block)
            }
            skipFully(data, len.toLong())
            if (last) return null
        }
    }

    private fun parseVorbisComment(b: ByteArray): String? {
        var p = 0
        fun le32(): Int {
            val v = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or
                ((b[p + 2].toInt() and 0xFF) shl 16) or ((b[p + 3].toInt() and 0xFF) shl 24)
            p += 4
            return v
        }
        if (b.size < 8) return null
        val vendorLen = le32()
        p += vendorLen
        if (p + 4 > b.size) return null
        val count = le32()
        var unsynced: String? = null
        repeat(count) {
            if (p + 4 > b.size) return unsynced
            val len = le32()
            if (len < 0 || p + len > b.size) return unsynced
            val entry = String(b, p, len, Charsets.UTF_8)
            p += len
            val eq = entry.indexOf('=')
            if (eq > 0) {
                val key = entry.substring(0, eq).uppercase()
                val value = entry.substring(eq + 1)
                if (key == "LYRICS" && value.isNotBlank()) return value
                if (key == "UNSYNCEDLYRICS" && value.isNotBlank()) unsynced = value
            }
        }
        return unsynced
    }

    // ---------------------------------------------------------------- 工具

    private fun syncSafe(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0x7F) shl 21) or ((b[o + 1].toInt() and 0x7F) shl 14) or
            ((b[o + 2].toInt() and 0x7F) shl 7) or (b[o + 3].toInt() and 0x7F)

    private fun int32(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
            ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)

    private fun deUnsync(src: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream(src.size)
        var i = 0
        while (i < src.size) {
            out.write(src[i].toInt())
            if (src[i] == 0xFF.toByte() && i + 1 < src.size && src[i + 1] == 0.toByte()) i++
            i++
        }
        return out.toByteArray()
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) return false
            off += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val n = input.skip(left)
            if (n <= 0) {
                if (input.read() < 0) throw IOException("EOF")
                left--
            } else {
                left -= n
            }
        }
    }
}
