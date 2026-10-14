package com.melodybook.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.melodybook.player.data.Song
import com.melodybook.player.data.SongGroup
import com.melodybook.player.data.artworkUri
import com.melodybook.player.playback.PlayerUiState
import com.melodybook.player.ui.components.Artwork
import com.melodybook.player.ui.components.ListScrollbar
import com.melodybook.player.ui.components.SongAction
import com.melodybook.player.ui.components.SongRow
import com.melodybook.player.ui.theme.MelodyTokens

/** 歌手 / 专辑列表：每行一组，点开看这一组里的歌。 */
@Composable
fun CategoryList(
    groups: List<SongGroup>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpen: (SongGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MelodyTokens.spacing
    val sizes = MelodyTokens.sizes
    val listState = rememberLazyListState()
    Box(modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding + spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(groups, key = { it.key }, contentType = { "group" }) { group ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "打开${group.title}") { onOpen(group) }
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 封面取这一组里第一首有封面的歌
                Artwork(
                    uri = group.songs.firstNotNullOfOrNull { it.artworkUri },
                    size = sizes.listArtwork,
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        group.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    ListScrollbar(
        listState = listState,
        itemCount = groups.size,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(top = spacing.sm, bottom = bottomPadding + spacing.sm),
    )
    }
}

/** 打开某个歌手 / 专辑后的歌曲列表。点歌时以这一组作为播放队列。 */
@Composable
fun CategoryDetail(
    group: SongGroup,
    player: PlayerUiState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onPlay: (List<Song>, Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onHide: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MelodyTokens.spacing
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.xs, end = spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回分类列表")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    group.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(onClick = { onPlay(group.songs, group.songs.first()) }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, Modifier.size(MelodyTokens.sizes.iconSm))
                Spacer(Modifier.width(spacing.sm))
                Text("播放全部")
            }
        }
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding + spacing.sm),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(group.songs, key = { it.id }, contentType = { "song" }) { song ->
                val isCurrent = song.id == player.currentId
                SongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && player.isPlaying,
                    onClick = { onPlay(group.songs, song) },
                    actions = listOf(
                        SongAction("下一首播放") { onPlayNext(song) },
                        SongAction("从列表移除") { onHide(song) },
                    ),
                )
            }
        }
        ListScrollbar(
            listState = listState,
            itemCount = group.songs.size,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = spacing.sm, bottom = bottomPadding + spacing.sm),
        )
        }
    }
}
