package com.urfavxbf.kanade

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.urfavxbf.kanade.ui.DEFAULT_APP_FONT
import com.urfavxbf.kanade.ui.LocalAppFont
import com.urfavxbf.kanade.ui.appFonts
import com.urfavxbf.kanade.ui.KanadeScreen
import com.urfavxbf.kanade.ui.KanadeViewModel

class MainActivity : ComponentActivity() {
    private val model: KanadeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent {
            val themeMode by model.themeMode.collectAsState()
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val font by model.appFont.collectAsState()
            CompositionLocalProvider(LocalAppFont provides (appFonts[font] ?: appFonts.getValue(DEFAULT_APP_FONT))) {
                AccountEntry(this, darkTheme) { account, action, onAction ->
                    KanadeScreen(model, darkTheme, themeMode, account, action, onAction)
                }
            }
        }
    }
}
