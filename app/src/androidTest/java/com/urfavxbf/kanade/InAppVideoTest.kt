package com.urfavxbf.kanade

import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InAppVideoTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun starterMixOpensEmbeddedPlayerAndKeepsNavigationInKanade() {
        compose.onNodeWithText("Your music").assertExists()
        compose.onNodeWithText("YouTube settings").assertDoesNotExist()
        compose.onNodeWithText("Discover").performClick()
        compose.onNodeWithText("Discover songs").assertExists()
        compose.onNodeWithText("Music videos").performClick()
        compose.onNodeWithText("Play mix", substring = true).performClick()
        compose.onNodeWithText("Agree and continue").performClick()
        compose.onNodeWithText("YouTube player").assertExists()
        lateinit var player: WebView
        onView(isAssignableFrom(WebView::class.java)).check { view, error ->
            if (error != null) throw error
            player = view as WebView
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun evaluate(script: String): String {
            val result = LinkedBlockingQueue<String>()
            instrumentation.runOnMainSync { player.evaluateJavascript(script) { result.offer(it ?: "null") } }
            return result.poll(5, TimeUnit.SECONDS) ?: error("WebView did not respond")
        }
        compose.waitUntil(15000) { evaluate("typeof selectVideo") == "\"function\"" }
        assertEquals("\"https://com.urfavxbf.kanade\"", evaluate("location.origin"))
        assertEquals("3", evaluate("videos.length"))
        assertTrue(evaluate("document.querySelector('script[src]').src").contains("youtube.com/iframe_api"))

        // A top-level watch link must not replace the embedded player or launch an external app.
        evaluate("location.href = 'https://www.youtube.com/watch?v=5NV6Rdv1a3I'")
        instrumentation.waitForIdleSync()
        compose.onNodeWithText("YouTube player").assertExists()
        assertEquals("\"https://com.urfavxbf.kanade\"", evaluate("location.origin"))
    }
}
