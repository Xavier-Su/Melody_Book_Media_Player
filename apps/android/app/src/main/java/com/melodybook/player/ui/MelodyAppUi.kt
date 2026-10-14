package com.melodybook.player.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.melodybook.player.data.AppSettings
import com.melodybook.player.data.ScanFilter
import com.melodybook.player.data.SongMetadata
import com.melodybook.player.data.card.cardShareText
import com.melodybook.player.ui.card.DrawCardDialog
import com.melodybook.player.playback.SleepTimer
import com.melodybook.player.ui.components.MiniPlayer
import com.melodybook.player.ui.library.LibraryScreen
import com.melodybook.player.ui.nowplaying.NowPlayingScreen
import com.melodybook.player.ui.sheets.AppearanceSheet
import com.melodybook.player.ui.sheets.LyricsSearchSheet
import com.melodybook.player.ui.sheets.OnlineSheet
import com.melodybook.player.ui.sheets.OrbitSheet
import com.melodybook.player.ui.sheets.ScanSheet
import com.melodybook.player.ui.sheets.SleepTimerSheet
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.delay

private enum class Sheet { NONE, SCAN, APPEARANCE, SLEEP, ONLINE, ORBIT }

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun videoPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun MelodyAppUi(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val library by vm.libraryState.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val timerState by vm.sleepTimer.state.collectAsStateWithLifecycle()
    val queryText by vm.queryText.collectAsStateWithLifecycle()
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val weatherText by vm.weatherText.collectAsStateWithLifecycle()
    val lyricsSearch by vm.lyricsSearch.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()

    // 播放进度只在界面可见时轮询
    val positionFlow = remember(vm) { vm.player.positionFlow() }
    val position = positionFlow.collectAsStateWithLifecycle(initialValue = 0L)

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // ---------------------------------------------------------------- 权限
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    var hasPermission by remember { mutableStateOf(granted(audioPermission())) }
    // 用户可能去系统设置里改了权限，回到前台时重新检查
    LifecycleResumeEffect(Unit) {
        val now = granted(audioPermission())
        if (now && !hasPermission) vm.onPermissionGranted()
        hasPermission = now
        onPauseOrDispose { }
    }

    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val ok = result[audioPermission()] == true
        hasPermission = ok
        if (ok) {
            vm.onPermissionGranted()
        } else {
            // 系统允许再问一次时 shouldShowRationale 为 true；为 false 说明用户选了“不再询问”
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission())
        }
    }
    fun requestPermission() {
        if (permanentlyDenied) {
            // 被拒绝过后系统不再弹窗，引导去设置页
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )
            return
        }
        val perms = buildList {
            add(audioPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
    // 只在首次进入时自动弹一次，旋转屏幕等重建不再重复弹
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (hasPermission) {
            vm.onPermissionGranted()
        } else if (!askedOnce) {
            askedOnce = true
            requestPermission()
        }
    }

    // “MP4 听声”需要额外的视频权限
    var pendingScan by remember { mutableStateOf<Pair<ScanFilter, Boolean>?>(null) }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val p = pendingScan
        pendingScan = null
        if (ok && p != null) vm.scan(p.first, p.second)
    }
    fun startScan(filter: ScanFilter, restore: Boolean) {
        if (filter.includeVideo && !granted(videoPermission())) {
            pendingScan = filter to restore
            videoLauncher.launch(videoPermission())
        } else {
            vm.scan(filter, restore)
        }
    }

    // ---------------------------------------------------------------- 背景图
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // 长期持有读取权限，下次启动仍可显示
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.setWallpaper(uri.toString())
        }
    }

    // ---------------------------------------------------------------- 歌词文件夹
    // 用系统文件选择器授权一个文件夹，不需要“所有文件访问”这类敏感权限
    val lyricsFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            settings.lyricsFolderUri?.let { old ->
                if (old != uri.toString()) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(old),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
            }
            vm.setLyricsFolder(uri.toString())
            vm.showMessage("已设置歌词文件夹")
        }
    }

    // ---------------------------------------------------------------- 定时关闭剩余时间
    val timerLabel by produceState<String?>(null, timerState) {
        while (true) {
            value = when (val s = timerState) {
                SleepTimer.State.Off -> null
                SleepTimer.State.AfterCurrent -> "播完这首后停止"
                is SleepTimer.State.Counting ->
                    "${SongMetadata.formatDuration(vm.sleepTimer.remainingMs(s))} 后停止"
            }
            if (timerState !is SleepTimer.State.Counting) break
            delay(1000)
        }
    }

    var sheet by rememberSaveable { mutableStateOf(Sheet.NONE) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val miniPlayerHeight = if (player.hasMedia) MelodyTokens.sizes.miniPlayerHeight else 0.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val bgModel = settings.backgroundModel
        if (bgModel != null) {
            AsyncImage(
                model = bgModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 遮罩：浓度越高遮罩越薄。上限 0.85，保证最清晰时文字也还看得清
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 1f - settings.backgroundStrength.coerceIn(0f, 0.85f),
                        ),
                    ),
            )
        }

        Box(
            Modifier
                .statusBarsPadding()
                // 播放页盖住列表时，读屏软件也不应再读到下层的列表
                .then(if (expanded && player.hasMedia) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            LibraryScreen(
                state = library,
                queryText = queryText,
                player = player,
                scanning = scanning,
                hasPermission = hasPermission,
                sleepTimerActive = timerState !is SleepTimer.State.Off,
                transparentBackground = bgModel != null,
                bottomPadding = miniPlayerHeight,
                onRequestPermission = ::requestPermission,
                onQueryChange = vm::setQuery,
                onSortChange = vm::setSortOrder,
                onPlay = vm::play,
                onPlayIn = vm::playIn,
                onPlayNext = vm::playNext,
                onHide = { vm.hide(it) },
                onShuffleAll = vm::shuffleAll,
                onOpenScan = { sheet = Sheet.SCAN },
                onOpenAppearance = { sheet = Sheet.APPEARANCE },
                onOpenSleepTimer = { sheet = Sheet.SLEEP },
                weatherText = weatherText,
                onlineActive = settings.onlineLyrics || settings.weatherEnabled || settings.cardEnabled,
                onOpenOnline = { sheet = Sheet.ONLINE },
                cardEnabled = settings.cardEnabled,
                onOpenCard = vm::openCard,
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = player.hasMedia && !expanded,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                MiniPlayer(
                    state = player,
                    position = position,
                    onExpand = { expanded = true },
                    onTogglePlay = vm.player::togglePlayPause,
                    onNext = vm.player::next,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && player.hasMedia,
            enter = slideInVertically(tween(320)) { it },
            exit = slideOutVertically(tween(260)) { it },
        ) {
            NowPlayingScreen(
                state = player,
                position = position,
                lyrics = lyrics,
                sleepTimerLabel = timerLabel,
                onNoLyrics = {
                    vm.showMessage(
                        if (lyrics is LyricsUiState.Loading) "正在查找歌词…" else "这首歌没有歌词，右滑可设置歌词文件夹",
                    )
                },
                onPickLyricsFolder = { lyricsFolderPicker.launch(null) },
                onlineLyrics = settings.onlineLyrics,
                onSearchLyrics = vm::openLyricsSearch,
                spectrumEnabled = settings.spectrumEnabled,
                onToggleSpectrum = { vm.setSpectrum(!settings.spectrumEnabled) },
                orbit = settings.orbit,
                orbitClock = vm.orbitClock,
                onOpenOrbit = { sheet = Sheet.ORBIT },
                onCollapse = { expanded = false },
                onTogglePlay = vm.player::togglePlayPause,
                onPrevious = vm.player::previous,
                onNext = vm.player::next,
                onSeek = vm.player::seekTo,
                onCyclePlayMode = vm::cyclePlayMode,
                onOpenSleepTimer = { sheet = Sheet.SLEEP },
            )
        }

        // 放在最上层，播放页展开时提示也看得到
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (expanded || !player.hasMedia) 0.dp else miniPlayerHeight),
        )
    }

    when (sheet) {
        Sheet.NONE -> Unit
        Sheet.SCAN -> ScanSheet(
            current = settings.scanFilter,
            onScan = ::startScan,
            onDismiss = { sheet = Sheet.NONE },
        )
        Sheet.APPEARANCE -> AppearanceSheet(
            settings = settings,
            presets = vm.presetBackgrounds,
            supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onSkin = vm::setSkin,
            onDarkMode = vm::setDarkMode,
            onRandomSkin = vm::setRandomSkin,
            onBackgroundMode = vm::setBackgroundMode,
            onPreset = vm::setPresetBackground,
            onStrength = vm::setBackgroundStrength,
            onRandomPreset = vm::setRandomPreset,
            onPickLyricsFolder = { lyricsFolderPicker.launch(null) },
            onClearLyricsFolder = {
                settings.lyricsFolderUri?.let { old ->
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(old),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
                vm.setLyricsFolder(null)
            },
            onPickWallpaper = {
                wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClearWallpaper = {
                settings.wallpaperUri?.let { old ->
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(old),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
                vm.setWallpaper(null)
            },
            onDismiss = { sheet = Sheet.NONE },
        )
        Sheet.ONLINE -> OnlineSheet(
            settings = settings,
            onOnlineLyrics = vm::setOnlineLyrics,
            onWeather = vm::setWeatherEnabled,
            onCity = vm::setWeatherCity,
            onCard = vm::setCardEnabled,
            onCardCategory = vm::setCardCategory,
            onCardZodiac = vm::setCardZodiac,
            onDismiss = { sheet = Sheet.NONE },
        )
        Sheet.ORBIT -> OrbitSheet(
            config = settings.orbit,
            previewRunning = vm.orbitClock.running,
            onConfig = vm::setOrbit,
            onTogglePreview = vm.orbitClock::toggle,
            onResetPreview = vm.orbitClock::reset,
            onDismiss = { sheet = Sheet.NONE },
        )
        Sheet.SLEEP -> SleepTimerSheet(
            state = timerState,
            remainingLabel = timerLabel,
            onPick = vm::startSleepTimer,
            onAfterCurrent = vm::stopAfterCurrent,
            onCancel = vm::cancelSleepTimer,
            onDismiss = { sheet = Sheet.NONE },
        )
    }

    card?.let { c ->
        DrawCardDialog(
            state = c,
            onKind = vm::setCardKind,
            onRedraw = vm::drawCard,
            onShare = {
                c.content?.let { content ->
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, cardShareText(content, c.lunar))
                    context.startActivity(Intent.createChooser(send, "分享卡片"))
                }
            },
            onDismiss = vm::closeCard,
        )
    }

    lyricsSearch?.let { s ->
        LyricsSearchSheet(
            state = s,
            onSearch = vm::searchLyrics,
            onPick = vm::applyLyrics,
            onDismiss = vm::closeLyricsSearch,
        )
    }
}
