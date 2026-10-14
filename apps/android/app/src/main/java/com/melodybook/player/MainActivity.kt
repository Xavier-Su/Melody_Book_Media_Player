package com.melodybook.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodybook.player.data.DarkMode
import com.melodybook.player.ui.MainViewModel
import com.melodybook.player.ui.MelodyAppUi
import com.melodybook.player.ui.theme.MelodyTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 系统启动页一直保持到设置读出来，避免主题从默认色闪到用户选的皮肤
        installSplashScreen().setKeepOnScreenCondition { !vm.ready.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            settings?.let { s ->
                val dark = when (s.darkMode) {
                    DarkMode.SYSTEM -> isSystemInDarkTheme()
                    DarkMode.LIGHT -> false
                    DarkMode.DARK -> true
                }
                // 状态栏/导航栏图标颜色跟随 App 自己的深浅色，而不是系统设置
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                MelodyTheme(skinId = s.skinId, darkMode = s.darkMode) {
                    MelodyAppUi(vm, s)
                }
            }
        }
    }
}
