package com.melodybook.player.ui.nowplaying

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.melodybook.player.data.SongMetadata
import com.melodybook.player.playback.PlayerUiState
import com.melodybook.player.ui.LyricsUiState
import com.melodybook.player.ui.components.icon
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.launch

private const val PAGE_DISC = 0
private const val PAGE_LYRICS = 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    position: State<Long>,
    lyrics: LyricsUiState,
    sleepTimerLabel: String?,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlayMode: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onNoLyrics: () -> Unit,
    onPickLyricsFolder: () -> Unit,
    spectrumEnabled: Boolean,
    onToggleSpectrum: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    val sizes = MelodyTokens.sizes
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val pager = rememberPagerState(initialPage = PAGE_DISC) { 2 }
    var showAlbumArt by rememberSaveable { mutableStateOf(false) }

    // 返回键：先关大图，再回唱片页，最后才收起播放页
    BackHandler(enabled = !showAlbumArt) {
        if (pager.currentPage == PAGE_LYRICS) scope.launch { pager.animateScrollToPage(PAGE_DISC) }
        else onCollapse()
    }

    fun openLyrics() {
        when (lyrics) {
            is LyricsUiState.Missing -> onNoLyrics()
            else -> scope.launch { pager.animateScrollToPage(PAGE_LYRICS) }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            // 之前的穿透问题：这一层根节点没有任何触摸处理，点击落在空白处（比如唱片周围）时，
            // Compose 会继续往下找，结果命中了下层的歌曲列表。
            // 只要根节点挂一个触摸监听，命中测试就停在这一层。这里只监听不消费，
            // 否则会把子控件（按钮、唱片、进度条）的点击判定一起取消掉。
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            },
    ) {
        Backdrop(state)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "收起播放页")
                }
                PageDots(
                    current = pager.currentPage,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleSpectrum) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = if (spectrumEnabled) "关闭频谱" else "开启频谱",
                        tint = if (spectrumEnabled) scheme.primary else scheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    PAGE_DISC -> DiscPage(
                        state = state,
                        onTap = ::openLyrics,
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAlbumArt = true
                        },
                    )
                    else -> LyricsPage(
                        state = lyrics,
                        position = position,
                        onSeek = onSeek,
                        onPickLyricsFolder = onPickLyricsFolder,
                    )
                }
            }

            Column(
                Modifier.padding(horizontal = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    state.title.ifEmpty { "未在播放" },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    state.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (spectrumEnabled) {
                    Spacer(Modifier.height(spacing.md))
                    SpectrumView(isPlaying = state.isPlaying)
                }

                Spacer(Modifier.height(spacing.sm))
                SeekBar(state.durationMs, position, onSeek)

                Spacer(Modifier.height(spacing.md))
                Controls(state, sleepTimerLabel, onTogglePlay, onPrevious, onNext, onCyclePlayMode, onOpenSleepTimer)
                Box(Modifier.height(spacing.xl), contentAlignment = Alignment.Center) {
                    if (sleepTimerLabel != null) {
                        Text(sleepTimerLabel, style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                    }
                }
                Spacer(Modifier.height(spacing.sm))
            }
        }

        AnimatedVisibility(visible = showAlbumArt, enter = fadeIn(), exit = fadeOut()) {
            AlbumArtViewer(
                artworkUri = state.artworkUri,
                title = state.title,
                album = state.album,
                onDismiss = { showAlbumArt = false },
            )
        }
    }
}

/** 背景：封面高斯模糊（Android 12+），上面盖一层统一的表面色，保证文字对比度。 */
@Composable
private fun Backdrop(state: PlayerUiState) {
    if (state.artworkUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AsyncImage(
            model = state.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(MelodyTokens.sizes.backdropBlur)
                .alpha(0.5f),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscPage(state: PlayerUiState, onTap: () -> Unit, onLongPress: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SpinningDisc(
            artworkUri = state.artworkUri,
            isPlaying = state.isPlaying,
            modifier = Modifier
                .widthIn(max = MelodyTokens.sizes.discMax)
                .padding(horizontal = MelodyTokens.spacing.xxl)
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "查看歌词",
                    onLongClickLabel = "查看专辑图片",
                    onClick = onTap,
                    onLongClick = onLongPress,
                ),
        )
    }
}

/** 顶部的两个小圆点，提示可以左右滑动切换唱片 / 歌词。 */
@Composable
private fun PageDots(current: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val sizes = MelodyTokens.sizes
    Row(
        modifier.semantics { contentDescription = if (current == PAGE_DISC) "唱片页，右滑看歌词" else "歌词页" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(2) { i ->
            Box(
                Modifier
                    .padding(horizontal = MelodyTokens.spacing.xs)
                    .size(if (i == current) sizes.dotActive else sizes.dotInactive)
                    .background(
                        if (i == current) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    sleepTimerLabel: String?,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sizes = MelodyTokens.sizes
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCyclePlayMode) {
            Icon(state.playMode.icon, contentDescription = "播放模式：${state.playMode.label}")
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(sizes.controlButton)) {
            Icon(Icons.Rounded.SkipPrevious, "上一首", Modifier.size(sizes.controlIcon))
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(sizes.playButton),
        ) {
            Icon(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "暂停" else "播放",
                modifier = Modifier.size(sizes.playIcon),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(sizes.controlButton)) {
            Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(sizes.controlIcon))
        }
        IconButton(onClick = onOpenSleepTimer) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = sleepTimerLabel?.let { "定时关闭：$it" } ?: "定时关闭",
                tint = if (sleepTimerLabel != null) scheme.primary else LocalContentColor.current,
            )
        }
    }
}
