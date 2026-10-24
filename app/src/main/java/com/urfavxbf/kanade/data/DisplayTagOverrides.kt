package com.urfavxbf.kanade.data

import android.content.Context
import com.urfavxbf.kanade.model.Track
import org.json.JSONObject

/**
 * Non-destructive display metadata edits. Never writes to source audio files.
 * Keys are MediaStore-stable track IDs; data remains on device.
 */
class DisplayTagOverrides(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kanade_display_tags", Context.MODE_PRIVATE)

    fun apply(track: Track): Track {
        val raw = prefs.getString(track.id, null) ?: return track
        return runCatching {
            val data = JSONObject(raw)
            track.copy(
                title = data.optString("title", track.title),
                artist = data.optString("artist", track.artist),
                album = data.optString("album", track.album),
            )
        }.getOrDefault(track)
    }

    fun save(track: Track, title: String, artist: String, album: String): Track {
        val corrected = track.copy(
            title = title.trim().take(150).ifBlank { track.title },
            artist = artist.trim().take(150).ifBlank { track.artist },
            album = album.trim().take(150),
        )
        prefs.edit().putString(track.id,
            JSONObject().put("title", corrected.title)
                .put("artist", corrected.artist)
                .put("album", corrected.album).toString()
        ).apply()
        return corrected
    }
}
