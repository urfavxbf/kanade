package com.urfavxbf.kanade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.urfavxbf.kanade.playback.LiveSpectrum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive

internal const val DEFAULT_SPECTRUM_BARS = 36
internal val spectrumBarOptions = listOf(0, 12, 24, DEFAULT_SPECTRUM_BARS, 48, 60)

@Composable
internal fun SpectrumBar(art: String?, progress: Float, playing: Boolean, reduced: Boolean, fallback: Color, bars: Int) {
    val context = LocalContext.current
    var tint by remember(art, fallback) { mutableStateOf(fallback) }
    LaunchedEffect(art, fallback) {
        if (art.isNullOrEmpty()) return@LaunchedEffect
        try {
            val request = ImageRequest.Builder(context).data(art).size(24, 24).allowHardware(false).build()
            val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image ?: return@LaunchedEffect
            val bitmap = image.toBitmap()
            val bins = IntArray(512)
            val hues = IntArray(512)
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) < 128) continue
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                val index = (r / 32) * 64 + (g / 32) * 8 + b / 32
                bins[index] += 1 + (maxOf(r,g,b) - minOf(r,g,b)) / 40
                hues[index] = pixel
            }
            val best = bins.indices.maxByOrNull { bins[it] } ?: return@LaunchedEffect
            if (bins[best] > 0) tint = Color(hues[best])
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { tint = fallback }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, reduced, playing) {
        fun update() { LiveSpectrum.visible = playing && !reduced && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        val observer = LifecycleEventObserver { _, _ -> update() }
        owner.lifecycle.addObserver(observer)
        update()
        onDispose { owner.lifecycle.removeObserver(observer); LiveSpectrum.visible = false }
    }
    val displayed = remember { mutableStateOf(FloatArray(12)) }
    LaunchedEffect(owner, playing, reduced) {
        if (!playing || reduced) {
            displayed.value = FloatArray(12)
            return@LaunchedEffect
        }
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                var lastFrame = withFrameNanos { it }
                while (isActive) {
                    val now = withFrameNanos { it }
                    displayed.value = smoothSpectrum(displayed.value, LiveSpectrum.bands.value,
                        ((now - lastFrame) / 1_000_000_000.0).toFloat())
                    lastFrame = now
                }
            } finally {
                displayed.value = FloatArray(12)
            }
        }
    }
    val accent = if (tint.luminance() < 0.35f) lerp(tint, Color.White, 0.55f) else tint
    Canvas(Modifier.fillMaxWidth().height(30.dp)) {
        val levels = displayed.value
        val step = size.width / bars
        for (i in 0 until bars) {
            val band = i * (levels.size - 1f) / (bars - 1)
            val first = band.toInt()
            val level = if (reduced || !playing) 0f else levels[first] +
                (levels[(first+1).coerceAtMost(levels.lastIndex)] - levels[first]) * (band - first)
            val h = 2.dp.toPx() + level * (size.height / 2 - 2.dp.toPx())
            val x = (i + 0.5f) * step
            drawLine(if (x <= progress * size.width) accent else Color.White.copy(alpha = 0.48f),
                Offset(x, size.height / 2 - h), Offset(x, size.height / 2 + h), step * 0.58f, StrokeCap.Round)
        }
    }
}

