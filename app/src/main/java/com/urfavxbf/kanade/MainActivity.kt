package com.urfavxbf.kanade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.urfavxbf.kanade.ui.KanadeScreen
import com.urfavxbf.kanade.ui.KanadeViewModel

class MainActivity : ComponentActivity() {
    private val model: KanadeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(11, 12, 18)
        window.navigationBarColor = android.graphics.Color.rgb(11, 12, 18)
        setContent { KanadeScreen(model) }
    }
}
