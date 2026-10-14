package com.melodybook.player.ui.nowplaying

import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.melodybook.player.ui.theme.CoverColors
import com.melodybook.player.ui.theme.LocalIsDarkTheme
import com.melodybook.player.ui.theme.Skin
import com.melodybook.player.ui.theme.colorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 封面取出的两个种子色，和皮肤一样：一个主色 + 一个点缀色。 */
private data class AlbumSeeds(val primary: Color, val accent: Color)

/** 取色用的小图边长。只看颜色分布，64px（4096 个像素）足够，也快。 */
private const val SAMPLE_PX = 64

/** 换歌时颜色过渡的时长，避免整页一下子跳色。 */
private const val COLOR_FADE_MS = 600

/** 最近几首歌的取色结果，来回切歌不用重新解码图片。 */
private val seedCache = LruCache<Uri, AlbumSeeds>(32)

/**
 * 播放页专用的配色：有封面时按封面取色，生成一套完整的 ColorScheme，
 * 按钮、进度条、歌词高亮都跟着封面走；没有封面或取色失败时沿用当前皮肤。
 *
 * 推导规则和皮肤完全一样（[Skin.colorScheme]），所以浅色 / 深色模式的对比度规则不变。
 */
@Composable
fun rememberAlbumColorScheme(artworkUri: Uri?): ColorScheme {
    val fallback = MaterialTheme.colorScheme
    val dark = LocalIsDarkTheme.current
    val context = LocalContext.current

    val seeds by produceState(artworkUri?.let(seedCache::get), artworkUri) {
        value = artworkUri?.let { uri -> seedCache.get(uri) ?: extractSeeds(context, uri)?.also { seedCache.put(uri, it) } }
    }

    // 没有种子色时动画回到皮肤的主色，过渡同样平滑
    val primary by animateColorAsState(seeds?.primary ?: fallback.primary, tween(COLOR_FADE_MS), label = "albumPrimary")
    val accent by animateColorAsState(seeds?.accent ?: fallback.tertiary, tween(COLOR_FADE_MS), label = "albumAccent")

    if (seeds == null && primary == fallback.primary) return fallback
    return remember(primary, accent, dark) { Skin("album", "", primary, accent).colorScheme(dark) }
}

private suspend fun extractSeeds(context: android.content.Context, uri: Uri): AlbumSeeds? {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .size(SAMPLE_PX)
        // 要读像素，硬件位图读不了
        .allowHardware(false)
        .build()
    val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
    return withContext(Dispatchers.Default) {
        val bitmap = drawable.toBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        CoverColors.extract(pixels)?.let { AlbumSeeds(Color(it.primary), Color(it.accent)) }
    }
}
