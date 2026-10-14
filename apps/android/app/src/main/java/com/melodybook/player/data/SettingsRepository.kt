package com.melodybook.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class DarkMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }

enum class PlayMode(val label: String) { LIST_LOOP("列表循环"), SINGLE_LOOP("单曲循环"), SHUFFLE("随机播放") }

enum class BackgroundMode(val label: String) { NONE("不用"), PRESET("预置图片"), GALLERY("相册图片") }

data class AppSettings(
    val skinId: String = "default",
    val randomSkinOnLaunch: Boolean = false,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val backgroundMode: BackgroundMode = BackgroundMode.NONE,
    /** 预置背景图的文件名，例如 "bg3.jpg"。 */
    val presetBackground: String = "bg1.jpg",
    /** 用户从相册选的背景图（content:// Uri）。 */
    val wallpaperUri: String? = null,
    /** 背景图浓度 0~1：越大图片越清晰，越小遮罩越厚。 */
    val backgroundStrength: Float = DEFAULT_BG_STRENGTH,
    /** 预置模式下每次启动随机换一张。 */
    val randomPresetOnLaunch: Boolean = false,
    /** 用户授权的歌词文件夹（SAF tree Uri）。 */
    val lyricsFolderUri: String? = null,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val scanFilter: ScanFilter = ScanFilter.ALL,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
    /** 播放页是否显示频谱。 */
    val spectrumEnabled: Boolean = true,
) {
    /** 当前实际要显示的背景图（Coil 可直接加载的地址），null 表示不显示。 */
    val backgroundModel: String?
        get() = when (backgroundMode) {
            BackgroundMode.NONE -> null
            BackgroundMode.PRESET -> PresetBackgrounds.assetUrl(presetBackground)
            BackgroundMode.GALLERY -> wallpaperUri
        }

    companion object {
        const val DEFAULT_BG_STRENGTH = 0.35f
    }
}

/** 上次播放现场，用于下次启动时“接着听”。 */
data class PlaybackSnapshot(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val skin = stringPreferencesKey("skin")
        val randomSkin = booleanPreferencesKey("random_skin")
        val darkMode = stringPreferencesKey("dark_mode")
        val wallpaper = stringPreferencesKey("wallpaper")
        val bgMode = stringPreferencesKey("bg_mode")
        val bgPreset = stringPreferencesKey("bg_preset")
        val bgStrength = floatPreferencesKey("bg_strength")
        val bgRandom = booleanPreferencesKey("bg_random")
        val lyricsFolder = stringPreferencesKey("lyrics_folder")
        val sort = stringPreferencesKey("sort")
        val scanFilter = stringPreferencesKey("scan_filter")
        val playMode = stringPreferencesKey("play_mode")
        val spectrum = booleanPreferencesKey("spectrum")
        val queue = stringPreferencesKey("queue")
        val queueIndex = intPreferencesKey("queue_index")
        val position = longPreferencesKey("position")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            skinId = p[Keys.skin] ?: "default",
            randomSkinOnLaunch = p[Keys.randomSkin] ?: false,
            darkMode = enumOr(p[Keys.darkMode], DarkMode.SYSTEM),
            // 老版本只有 wallpaper，没有 bgMode：有相册图就当作相册模式
            backgroundMode = enumOr(
                p[Keys.bgMode],
                if (p[Keys.wallpaper] != null) BackgroundMode.GALLERY else BackgroundMode.NONE,
            ),
            presetBackground = p[Keys.bgPreset] ?: "bg1.jpg",
            wallpaperUri = p[Keys.wallpaper],
            backgroundStrength = p[Keys.bgStrength] ?: AppSettings.DEFAULT_BG_STRENGTH,
            randomPresetOnLaunch = p[Keys.bgRandom] ?: false,
            lyricsFolderUri = p[Keys.lyricsFolder],
            sortOrder = enumOr(p[Keys.sort], SortOrder.TITLE),
            scanFilter = enumOr(p[Keys.scanFilter], ScanFilter.ALL),
            playMode = enumOr(p[Keys.playMode], PlayMode.LIST_LOOP),
            spectrumEnabled = p[Keys.spectrum] ?: true,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setSkin(id: String) = store.edit { it[Keys.skin] = id }
    suspend fun setRandomSkin(enabled: Boolean) = store.edit { it[Keys.randomSkin] = enabled }
    suspend fun setDarkMode(mode: DarkMode) = store.edit { it[Keys.darkMode] = mode.name }
    suspend fun setSortOrder(order: SortOrder) = store.edit { it[Keys.sort] = order.name }
    suspend fun setScanFilter(filter: ScanFilter) = store.edit { it[Keys.scanFilter] = filter.name }
    suspend fun setPlayMode(mode: PlayMode) = store.edit { it[Keys.playMode] = mode.name }
    suspend fun setSpectrum(enabled: Boolean) = store.edit { it[Keys.spectrum] = enabled }

    /** 选了相册图片就自动切到“相册图片”模式；清除时如果正在用相册图则关掉背景。 */
    suspend fun setWallpaper(uri: String?) = store.edit {
        if (uri == null) {
            it.remove(Keys.wallpaper)
            if (it[Keys.bgMode] == BackgroundMode.GALLERY.name) it[Keys.bgMode] = BackgroundMode.NONE.name
        } else {
            it[Keys.wallpaper] = uri
            it[Keys.bgMode] = BackgroundMode.GALLERY.name
        }
    }

    suspend fun setBackgroundMode(mode: BackgroundMode) = store.edit { it[Keys.bgMode] = mode.name }

    suspend fun setPresetBackground(file: String) = store.edit {
        it[Keys.bgPreset] = file
        it[Keys.bgMode] = BackgroundMode.PRESET.name
    }

    suspend fun setBackgroundStrength(value: Float) = store.edit {
        it[Keys.bgStrength] = value.coerceIn(0f, 1f)
    }

    suspend fun setRandomPreset(enabled: Boolean) = store.edit { it[Keys.bgRandom] = enabled }

    suspend fun setLyricsFolder(uri: String?) = store.edit {
        if (uri == null) it.remove(Keys.lyricsFolder) else it[Keys.lyricsFolder] = uri
    }

    suspend fun saveSnapshot(snapshot: PlaybackSnapshot) = store.edit {
        it[Keys.queue] = snapshot.queueIds.joinToString(",")
        it[Keys.queueIndex] = snapshot.index
        it[Keys.position] = snapshot.positionMs
    }

    suspend fun loadSnapshot(): PlaybackSnapshot? {
        val p = store.data.first()
        val ids = p[Keys.queue]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (ids.isEmpty()) return null
        return PlaybackSnapshot(
            queueIds = ids,
            index = (p[Keys.queueIndex] ?: 0).coerceIn(0, ids.lastIndex),
            positionMs = p[Keys.position] ?: 0L,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback
}
