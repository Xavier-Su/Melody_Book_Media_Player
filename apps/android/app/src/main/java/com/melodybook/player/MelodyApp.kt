package com.melodybook.player

import android.app.Application
import android.content.Context
import com.melodybook.player.data.LibraryRepository
import com.melodybook.player.data.MediaScanner
import com.melodybook.player.data.MelodyDatabase
import com.melodybook.player.data.SettingsRepository
import com.melodybook.player.data.lyrics.LyricsRepository
import com.melodybook.player.playback.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 手写的轻量依赖容器。项目规模不大，暂时不引入 Hilt。 */
class AppContainer(context: Context) {
    /** 生命周期与进程一致的协程作用域，用于“必须写完”的小任务（如保存播放进度）。 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = MelodyDatabase.create(context)
    val settings = SettingsRepository(context)
    val library = LibraryRepository(database.songDao(), MediaScanner(context.contentResolver))
    val sleepTimer = SleepTimer()
    val lyrics = LyricsRepository(context)
}

class MelodyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MelodyApp).container
