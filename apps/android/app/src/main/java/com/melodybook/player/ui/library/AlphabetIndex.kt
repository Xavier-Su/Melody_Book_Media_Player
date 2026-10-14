package com.melodybook.player.ui.library

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.melodybook.player.data.Song
import com.melodybook.player.data.SongSearch
import com.melodybook.player.ui.theme.MelodyTokens

/**
 * 右侧字母快速索引（按歌名排序时显示）。
 * 只显示曲库里实际存在的首字母，点击或拖动跳到对应位置。
 */
@Composable
fun AlphabetIndex(
    songs: List<Song>,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 每个字母第一次出现的位置
    val firstIndex: List<Pair<Char, Int>> = remember(songs) {
        val seen = LinkedHashMap<Char, Int>()
        songs.forEachIndexed { i, s -> seen.putIfAbsent(SongSearch.indexLetter(s), i) }
        seen.entries.sortedBy { if (it.key == '#') '~' else it.key }.map { it.key to it.value }
    }
    if (firstIndex.size < 2) return

    var heightPx by remember { mutableIntStateOf(1) }
    fun jumpAt(y: Float) {
        val slot = (y / heightPx * firstIndex.size).toInt().coerceIn(0, firstIndex.lastIndex)
        onJump(firstIndex[slot].second)
    }

    Column(
        modifier = modifier
            .width(MelodyTokens.sizes.indexGutter)
            .padding(vertical = MelodyTokens.spacing.sm)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(firstIndex) { detectTapGestures { jumpAt(it.y) } }
            .pointerInput(firstIndex) {
                detectVerticalDragGestures { change, _ -> jumpAt(change.position.y) }
            }
            // 读屏用户使用列表本身导航，这里不暴露一堆单字母
            .clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        firstIndex.forEach { (letter, _) ->
            Text(
                letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = MelodyTokens.sizes.strokeThin),
            )
        }
    }
}
