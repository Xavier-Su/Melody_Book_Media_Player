package com.melodybook.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.melodybook.player.data.Song
import com.melodybook.player.data.SongMetadata
import com.melodybook.player.data.artworkUri
import com.melodybook.player.ui.theme.MelodyTokens

data class SongAction(val label: String, val onClick: () -> Unit)

/**
 * 列表行：封面 / 歌名 / 歌手·专辑 / 时长 / 更多。
 * 当前播放的歌曲用主题色高亮并显示动态图标；高亮按歌曲 id 判断，
 * 不会像旧版那样在搜索过滤后高亮到别的歌上。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    actions: List<SongAction>,
    modifier: Modifier = Modifier,
    /** 右侧显示字母索引时需要让出位置，避免盖住“更多”按钮。 */
    endPadding: androidx.compose.ui.unit.Dp = MelodyTokens.spacing.xs,
) {
    val spacing = MelodyTokens.spacing
    val sizes = MelodyTokens.sizes
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 背景放在 clickable 前面，按压水波纹才会画在高亮色之上
            .background(if (isCurrent) scheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = { menuOpen = true },
                onLongClickLabel = "更多操作",
            )
            .padding(start = spacing.lg, end = endPadding, top = spacing.sm, bottom = spacing.sm)
            .semantics { if (isCurrent) stateDescription = if (isPlaying) "正在播放" else "已暂停" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Artwork(song.artworkUri, sizes.listArtwork, MaterialTheme.shapes.small)
            if (isCurrent) {
                Box(
                    Modifier
                        .size(sizes.listArtwork)
                        .background(scheme.scrim.copy(alpha = 0.35f), MaterialTheme.shapes.small),
                )
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(sizes.icon),
                )
            }
        }
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) scheme.primary else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(spacing.sm))
        Text(
            text = SongMetadata.formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "${song.title} 的更多操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            menuOpen = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}
