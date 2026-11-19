package com.urfavxbf.kanade

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class MusicPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun fileFromHomePlaysInNativePlayer() {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val title = "Kanade playback check"
        val uri = requireNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }))
        Intents.init()
        try {
            // A local PCM fixture exercises the real Media3 pipeline without a network dependency.
            val pcm = ByteArray(8000 * 2 * 20)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVEfmt ".toByteArray())
                putInt(16); putShort(1); putShort(1); putInt(8000); putInt(16000)
                putShort(2); putShort(16); put("data".toByteArray()); putInt(pcm.size)
            }
            requireNotNull(resolver.openOutputStream(uri)).use { it.write(header.array()); it.write(pcm) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))
            compose.onNodeWithText("Open audio file").performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Now playing").performClick()
            onView(withContentDescription("Playback position")).check(matches(isDisplayed()))
            compose.onNode(hasText("Pause") and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil(5000) {
                compose.onAllNodes(hasText("Play") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            Intents.release()
            resolver.delete(uri, null, null)
        }
    }
}
