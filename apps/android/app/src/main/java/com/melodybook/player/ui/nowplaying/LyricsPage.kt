package com.melodybook.player.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.melodybook.player.data.lyrics.LrcParser
import com.melodybook.player.data.lyrics.LyricLine
import com.melodybook.player.data.lyrics.Lyrics
import com.melodybook.player.ui.LyricsUiState
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.delay

/**
 * 歌词页。
 * 带时间轴的歌词跟随播放滚动，当前行居中放大高亮；
 * 用户手动滑动后暂停自动滚动 3 秒；点某一行可以跳到那个时间点。
 */
@Composable
fun LyricsPage(
    state: LyricsUiState,
    position: State<Long>,
    onSeek: (Long) -> Unit,
    onPickLyricsFolder: () -> Unit,
    /** 设置里开了在线歌词才为 true：此时显示“搜索在线歌词”入口。 */
    onlineLyrics: Boolean,
    onSearchLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            LyricsUiState.None -> Unit
            is LyricsUiState.Loading -> CircularProgressIndicator()
            is LyricsUiState.Missing -> NoLyrics(onPickLyricsFolder, onlineLyrics, onSearchLyrics)
            is LyricsUiState.Loaded -> {
                when (val l = state.lyrics) {
                    is Lyrics.Synced -> SyncedLyrics(l.lines, position, onSeek)
                    is Lyrics.Plain -> PlainLyrics(l.lines)
                }
                // 自动匹配错了，可以自己搜
                if (onlineLyrics) {
                    IconButton(onClick = onSearchLyrics, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索其它歌词")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoLyrics(onPickLyricsFolder: () -> Unit, onlineLyrics: Boolean, onSearchLyrics: () -> Unit) {
    val spacing = MelodyTokens.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(spacing.xl),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Notes,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MelodyTokens.sizes.touchTarget),
        )
        Spacer(Modifier.height(spacing.md))
        Text("这首歌没有歌词", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(spacing.sm))
        Text(
            "支持歌曲内嵌歌词，以及与歌曲同名的 .lrc 文件。\n把 .lrc 放在一个文件夹里并在这里选择它，查找最可靠。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.lg))
        if (onlineLyrics) {
            Button(onClick = onSearchLyrics) { Text("搜索在线歌词") }
            Spacer(Modifier.height(spacing.sm))
        }
        OutlinedButton(onClick = onPickLyricsFolder) { Text("选择歌词文件夹") }
    }
}

@Composable
private fun PlainLyrics(lines: List<String>) {
    val spacing = MelodyTokens.spacing
    LazyColumn(
        contentPadding = PaddingValues(vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(lines) { _, line ->
            Text(
                line.ifEmpty { " " },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, position: State<Long>, onSeek: (Long) -> Unit) {
    val spacing = MelodyTokens.spacing
    val listState = rememberLazyListState()
    // 只有“当前行”变了才重组，进度每 500ms 变一次不会让整页刷新
    val active by remember(lines) { derivedStateOf { LrcParser.activeIndex(lines, position.value) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val half = maxHeight / 2

        // 用户手指拖动列表时暂停自动滚动，松手 3 秒后再回到当前行
        val dragged by listState.interactionSource.collectIsDraggedAsState()
        var lastUserScrollAt by remember { mutableLongStateOf(0L) }
        LaunchedEffect(dragged) {
            if (dragged) lastUserScrollAt = Long.MAX_VALUE
            else if (lastUserScrollAt != 0L) lastUserScrollAt = System.currentTimeMillis()
        }
        LaunchedEffect(active, lastUserScrollAt) {
            if (active < 0 || dragged) return@LaunchedEffect
            val sinceUser = System.currentTimeMillis() - lastUserScrollAt
            if (sinceUser in 0 until 3_000) delay(3_000 - sinceUser)
            // 列表顶部留了半屏空白，所以滚到当前行即可让它大致位于正中
            listState.animateScrollToItem(active)
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = half - MelodyTokens.sizes.touchTarget, bottom = half),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(lines, key = { i, l -> "$i-${l.timeMs}" }) { i, line ->
                LyricRow(line, isActive = i == active, onClick = { onSeek(line.timeMs) })
            }
        }
    }
}

@Composable
private fun LyricRow(line: LyricLine, isActive: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        if (isActive) scheme.primary else scheme.onSurface.copy(alpha = 0.55f),
        label = "lyricColor",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "从这句开始播放", onClick = onClick)
            .padding(horizontal = MelodyTokens.spacing.xl, vertical = MelodyTokens.spacing.xs),
    ) {
        Text(
            line.text.ifEmpty { "♪" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center,
        )
        line.translation?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = color.alpha * 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
