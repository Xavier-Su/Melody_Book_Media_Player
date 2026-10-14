package com.melodybook.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.melodybook.player.appContainer
import com.melodybook.player.data.AppSettings
import com.melodybook.player.data.BackgroundMode
import com.melodybook.player.data.DarkMode
import com.melodybook.player.data.PresetBackgrounds
import com.melodybook.player.data.lyrics.Lyrics
import com.melodybook.player.data.ScanFilter
import com.melodybook.player.data.Pinyin
import com.melodybook.player.data.Song
import com.melodybook.player.data.SongGroup
import com.melodybook.player.data.SongGroups
import com.melodybook.player.data.SongSearch
import com.melodybook.player.data.SortOrder
import com.melodybook.player.playback.PlayerConnection
import com.melodybook.player.playback.SleepTimer
import com.melodybook.player.ui.theme.Skins
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

data class LibraryUiState(
    /** 数据库还没读完时为 false，用于区分“加载中”和“空曲库”。 */
    val loaded: Boolean = false,
    val totalCount: Int = 0,
    val songs: List<Song> = emptyList(),
    /** 按歌手 / 专辑归类的结果，和 [songs] 一样已经过搜索过滤。 */
    val artists: List<SongGroup> = emptyList(),
    val albums: List<SongGroup> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.TITLE,
)

sealed interface LyricsUiState {
    data object None : LyricsUiState
    data class Loading(val songId: Long) : LyricsUiState
    data class Loaded(val songId: Long, val lyrics: Lyrics, val source: String?) : LyricsUiState
    data class Missing(val songId: Long) : LyricsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app.appContainer
    private val library = container.library
    private val settingsRepo = container.settings
    private val lyricsRepo = container.lyrics
    val sleepTimer: SleepTimer = container.sleepTimer

    val player = PlayerConnection(app, viewModelScope, library, settingsRepo)

    /** null 表示设置尚未读出；启动页会一直显示到读出为止，避免主题闪一下。 */
    val settings: StateFlow<AppSettings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val launchSetupDone = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = combine(settings, launchSetupDone) { s, done -> s != null && done }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 搜索框的原始文字，同步暴露给输入框，避免输入法组字时丢字、光标乱跳。 */
    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    private val allSongs: StateFlow<List<Song>?> = library.songs
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val libraryState: StateFlow<LibraryUiState> = combine(
        allSongs,
        query,
        settings.filterNotNull().map { it.sortOrder }.distinctUntilChanged(),
    ) { songs, q, order ->
        val visible = songs?.let { SongSearch.filterAndSort(it, q, order) }.orEmpty()
        LibraryUiState(
            loaded = songs != null,
            totalCount = songs?.size ?: 0,
            songs = visible,
            artists = SongGroups.byArtist(visible),
            albums = SongGroups.byAlbum(visible) { Pinyin.of(it).first },
            query = q,
            sortOrder = order,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    /** 当前歌曲的歌词。随当前歌曲和歌词文件夹设置变化自动重新查找。 */
    val lyrics: StateFlow<LyricsUiState> = combine(
        player.state.map { it.currentId }.distinctUntilChanged(),
        settings.filterNotNull().map { it.lyricsFolderUri }.distinctUntilChanged(),
    ) { id, folder -> id to folder }
        .transformLatest { (id, folder) ->
            if (id == null) {
                emit(LyricsUiState.None)
                return@transformLatest
            }
            emit(LyricsUiState.Loading(id))
            val song = library.getById(id)
            val result = song?.let { lyricsRepo.load(it, folder) }
            emit(
                if (result?.lyrics != null) LyricsUiState.Loaded(id, result.lyrics, result.source)
                else LyricsUiState.Missing(id),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState.None)

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** 预置背景图列表（读 assets 目录，只需一次）。 */
    val presetBackgrounds: List<PresetBackgrounds.Preset> = PresetBackgrounds.list(app)

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            if (s.randomSkinOnLaunch) {
                val pick = Skins.all.filter { it.id != s.skinId }.random()
                settingsRepo.setSkin(pick.id)
                settings.first { it?.skinId == pick.id }
            }
            if (s.randomPresetOnLaunch && s.backgroundMode == BackgroundMode.PRESET && presetBackgrounds.size > 1) {
                val pick = presetBackgrounds.filter { it.file != s.presetBackground }.random()
                settingsRepo.setPresetBackground(pick.file)
                settings.first { it?.presetBackground == pick.file }
            }
            launchSetupDone.value = true
        }
    }

    override fun onCleared() {
        player.release()
    }

    // ---------------------------------------------------------------- 曲库

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSortOrder(order: SortOrder) = launchIo { settingsRepo.setSortOrder(order) }

    /** 获得权限后，如果曲库是空的就自动扫描一次。 */
    fun onPermissionGranted() = viewModelScope.launch {
        if (library.isEmpty() && !_scanning.value) {
            scan(settingsRepo.current().scanFilter, restoreHidden = false)
        }
    }

    fun scan(filter: ScanFilter, restoreHidden: Boolean) {
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            try {
                settingsRepo.setScanFilter(filter)
                val r = library.scan(filter, restoreHidden)
                _messages.send(
                    buildString {
                        append("扫描完成，共 ${r.total} 首")
                        if (r.added > 0) append("，新增 ${r.added} 首")
                        if (r.removed > 0) append("，清理 ${r.removed} 首")
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _messages.send("没有读取音乐的权限")
            } catch (e: Exception) {
                _messages.send("扫描失败：${e.message ?: e.javaClass.simpleName}")
            } finally {
                _scanning.value = false
            }
        }
    }

    fun hide(song: Song) = viewModelScope.launch {
        library.hide(song.id)
        player.removeFromQueue(song.id)
        _messages.send("已从列表移除“${song.title}”，本地文件不受影响")
    }

    // ---------------------------------------------------------------- 播放

    /** 点击列表里的歌：以当前可见列表（含搜索、排序结果）作为播放队列。 */
    fun play(song: Song) {
        val list = libraryState.value.songs
        val index = list.indexOfFirst { it.id == song.id }
        if (index >= 0) player.playQueue(list, index) else player.playQueue(listOf(song), 0)
    }

    /** 在某一组（歌手 / 专辑）里点歌：以这一组作为播放队列。 */
    fun playIn(list: List<Song>, song: Song) {
        val index = list.indexOfFirst { it.id == song.id }
        if (index >= 0) player.playQueue(list, index) else player.playQueue(listOf(song), 0)
    }

    fun playNext(song: Song) {
        if (player.playNext(song)) {
            viewModelScope.launch { _messages.send("将在下一首播放“${song.title}”") }
        }
    }

    fun shuffleAll() {
        val list = libraryState.value.songs
        if (list.isEmpty()) return
        player.setPlayMode(com.melodybook.player.data.PlayMode.SHUFFLE)
        player.playQueue(list, list.indices.random())
    }

    fun cyclePlayMode() {
        val mode = player.cyclePlayMode() ?: return
        viewModelScope.launch { _messages.send(mode.label) }
    }

    // ---------------------------------------------------------------- 定时关闭

    fun startSleepTimer(minutes: Int) {
        sleepTimer.start(minutes)
        viewModelScope.launch { _messages.send("$minutes 分钟后停止播放") }
    }

    fun stopAfterCurrent() {
        sleepTimer.stopAfterCurrent()
        viewModelScope.launch { _messages.send("播完当前歌曲后停止") }
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
        viewModelScope.launch { _messages.send("已取消定时关闭") }
    }

    // ---------------------------------------------------------------- 外观

    fun setSkin(id: String) = launchIo { settingsRepo.setSkin(id) }
    fun setDarkMode(mode: DarkMode) = launchIo { settingsRepo.setDarkMode(mode) }
    fun setRandomSkin(enabled: Boolean) = launchIo { settingsRepo.setRandomSkin(enabled) }
    fun setWallpaper(uri: String?) = launchIo { settingsRepo.setWallpaper(uri) }
    fun setBackgroundMode(mode: BackgroundMode) = launchIo { settingsRepo.setBackgroundMode(mode) }
    fun setPresetBackground(file: String) = launchIo { settingsRepo.setPresetBackground(file) }
    fun setBackgroundStrength(value: Float) = launchIo { settingsRepo.setBackgroundStrength(value) }
    fun setRandomPreset(enabled: Boolean) = launchIo { settingsRepo.setRandomPreset(enabled) }
    fun setSpectrum(enabled: Boolean) = launchIo { settingsRepo.setSpectrum(enabled) }

    // ---------------------------------------------------------------- 歌词

    fun setLyricsFolder(uri: String?) = launchIo {
        lyricsRepo.invalidate()
        settingsRepo.setLyricsFolder(uri)
    }

    fun showMessage(text: String) {
        viewModelScope.launch { _messages.send(text) }
    }

    private fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
}
