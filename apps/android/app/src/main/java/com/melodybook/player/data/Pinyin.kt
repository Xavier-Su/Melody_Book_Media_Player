package com.melodybook.player.data

import android.icu.text.Transliterator
import android.os.Build

/** 汉字转拼音。Android 10+ 使用系统自带的 ICU，无需额外依赖；更老的系统退化为原文小写。 */
object Pinyin {

    private val transliterator: Transliterator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower") }.getOrNull()
        } else {
            null
        }
    }

    /** 返回 (全拼, 首字母)。 */
    fun of(text: String): Pair<String, String> {
        val latin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            transliterator?.let { t -> synchronized(t) { t.transliterate(text) } }
        } else {
            null
        }
        return PinyinText.split(latin ?: text.lowercase())
    }
}

/** 与 Android 无关的纯文本处理，单独拆出来方便单元测试。 */
object PinyinText {
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    /** 输入 "zhou jie lun - qing hua ci"，输出 ("zhoujielunqinghuaci", "zjlqhc")。 */
    fun split(latin: String): Pair<String, String> {
        val tokens = latin.lowercase().split(separators).filter { it.isNotEmpty() }
        val full = tokens.joinToString("")
        val initials = tokens.joinToString("") { it.first().toString() }
        return full to initials
    }
}
