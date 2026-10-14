package com.melodybook.player.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.melodybook.player.data.Song
import com.melodybook.player.data.SortOrder
import com.melodybook.player.playback.PlayerUiState
import com.melodybook.player.ui.LibraryUiState
import com.melodybook.player.ui.components.ListScrollbar
import com.melodybook.player.ui.components.SongAction
import com.melodybook.player.ui.components.SongRow
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    /** 输入框的实时文字（与 state.query 分开，后者是过滤完成后的查询）。 */
    queryText: String,
    player: PlayerUiState,
    scanning: Boolean,
    hasPermission: Boolean,
    sleepTimerActive: Boolean,
    transparentBackground: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRequestPermission: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onPlay: (Song) -> Unit,
    onPlayIn: (List<Song>, Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onHide: (Song) -> Unit,
    onShuffleAll: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var sortMenu by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(LibraryTab.ALL) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("音书", style = MaterialTheme.typography.titleLarge)
                    if (state.totalCount > 0) {
                        Text(
                            "${state.totalCount} 首本地音乐",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onOpenSleepTimer) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "定时关闭",
                        tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenScan, enabled = hasPermission && !scanning) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "扫描歌曲")
                }
                IconButton(onClick = onOpenAppearance) {
                    Icon(Icons.Rounded.Palette, contentDescription = "变身（更换皮肤）")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (transparentBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
            ),
        )
        if (scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        when {
            !hasPermission -> EmptyState(
                title = "需要读取音乐的权限",
                body = "音书只读取本机的音频文件，用来生成你的曲库，不会上传任何数据。",
                action = "授予权限",
                onAction = onRequestPermission,
            )
            !state.loaded -> Unit
            state.totalCount == 0 && !scanning -> EmptyState(
                title = "曲库还是空的",
                body = "扫描一下本机的音乐，支持 MP3、FLAC、WAV、M4A 等格式。",
                action = "扫描歌曲",
                onAction = onOpenScan,
            )
            else -> {
                // 搜索框 + 工具条
                OutlinedTextField(
                    value = queryText,
                    onValueChange = onQueryChange,
                    placeholder = { Text("搜索歌名、歌手、专辑，支持拼音首字母") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    LibraryTab.entries.forEach { t ->
                        FilterChip(
                            selected = tab == t,
                            onClick = {
                                tab = t
                                openKey = null
                            },
                            label = { Text(t.label) },
                        )
                    }
                }

                if (tab != LibraryTab.ALL) {
                    val groups = if (tab == LibraryTab.ARTIST) state.artists else state.albums
                    val open = openKey?.let { k -> groups.firstOrNull { it.key == k } }
                    // 搜索把打开的那一组过滤没了，就回到列表
                    LaunchedEffect(open == null, openKey) {
                        if (openKey != null && open == null) openKey = null
                    }
                    BackHandler(enabled = open != null) { openKey = null }
                    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    when {
                        groups.isEmpty() -> Text(
                            "没有找到“${state.query}”相关的${tab.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.xxl),
                        )
                        open != null -> CategoryDetail(
                            group = open,
                            player = player,
                            bottomPadding = bottomPadding + navBar,
                            onBack = { openKey = null },
                            onPlay = onPlayIn,
                            onPlayNext = onPlayNext,
                            onHide = onHide,
                            modifier = Modifier.weight(1f),
                        )
                        else -> CategoryList(
                            groups = groups,
                            bottomPadding = bottomPadding + navBar,
                            onOpen = { openKey = it.key },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.sm, end = spacing.xs, top = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = onShuffleAll,
                        enabled = state.songs.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = spacing.lg),
                        modifier = Modifier.padding(start = spacing.sm),
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, Modifier.size(MelodyTokens.sizes.iconSm))
                        Spacer(Modifier.size(spacing.sm))
                        Text("随机播放")
                    }
                    Spacer(Modifier.weight(1f))
                    // 对应旧版的“定位”按钮：滚动到正在播放的歌
                    val currentIndex = state.songs.indexOfFirst { it.id == player.currentId }
                    IconButton(
                        onClick = { scope.launch { listState.animateScrollToItem(currentIndex) } },
                        enabled = currentIndex >= 0,
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "定位到正在播放")
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "排序：${state.sortOrder.label}")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOrder.entries.forEach { o ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            o.label,
                                            color = if (o == state.sortOrder) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        sortMenu = false
                                        onSortChange(o)
                                    },
                                )
                            }
                        }
                    }
                }

                if (state.songs.isEmpty()) {
                    Text(
                        "没有找到“${state.query}”相关的歌曲",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.xxl),
                    )
                } else {
                    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val showIndex = state.sortOrder == SortOrder.TITLE && state.query.isBlank()
                    Box(Modifier.weight(1f)) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = bottomPadding + navBar + spacing.sm),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.songs, key = { it.id }, contentType = { "song" }) { song ->
                                val isCurrent = song.id == player.currentId
                                SongRow(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && player.isPlaying,
                                    onClick = { onPlay(song) },
                                    actions = listOf(
                                        SongAction("下一首播放") { onPlayNext(song) },
                                        SongAction("从列表移除") { onHide(song) },
                                    ),
                                    endPadding = if (showIndex) MelodyTokens.sizes.indexGutter else spacing.xs,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        // 字母索引在最右侧时，滚动条让到它左边
                        ListScrollbar(
                            listState = listState,
                            itemCount = state.songs.size,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    end = if (showIndex) MelodyTokens.sizes.indexGutter else 0.dp,
                                    top = spacing.sm,
                                    bottom = bottomPadding + navBar + spacing.sm,
                                ),
                        )
                        if (showIndex) {
                            AlphabetIndex(
                                songs = state.songs,
                                onJump = { idx -> scope.launch { listState.scrollToItem(idx) } },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(bottom = bottomPadding + navBar),
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

private enum class LibraryTab(val label: String) { ALL("全部"), ARTIST("歌手"), ALBUM("专辑") }

@Composable
private fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    val spacing = MelodyTokens.spacing
    // 文字靠左、没有大图标：只说明现状和下一步，不做居中的欢迎页
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(spacing.sm))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.lg))
        FilledTonalButton(onClick = onAction) { Text(action) }
    }
}
