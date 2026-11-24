package com.urfavxbf.kanade.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Sample a short PCM window at most 20 times a second while the seek bar is visible. */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal object LiveSpectrum : TeeAudioProcessor.AudioBufferSink {
    val bands = MutableStateFlow(FloatArray(12))
    @Volatile var visible = false
    private var rate = 0
    private var channels = 0
    private var nextFrame = 0L
    private val window = DoubleArray(512) { 0.5 - 0.5 * cos(2 * PI * it / 511) }
    private val samples = DoubleArray(window.size)
    private val coefficients = DoubleArray(12)
    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        rate = if (encoding == C.ENCODING_PCM_16BIT) sampleRateHz else 0
        channels = channelCount
        if (rate > 0) for (band in coefficients.indices) {
            val frequency = min(rate * 0.44, 90.0 * 1.49.pow(band))
            coefficients[band] = 2 * cos(2 * PI * frequency / rate)
        }
        nextFrame = 0
        bands.value = FloatArray(12)
    }
    override fun handleBuffer(buffer: ByteBuffer) {
        if (!visible || rate <= 0 || channels <= 0) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now < nextFrame) return
        val data = buffer.duplicate().order(ByteOrder.nativeOrder())
        val count = min(samples.size, data.remaining() / (2 * channels))
        if (count < 32) return
        nextFrame = now + 50
        for (i in 0 until count) {
            var total = 0
            repeat(channels) { total += data.short.toInt() }
            samples[i] = total / (32768.0 * channels) * window[i * 511 / (count - 1)]
        }
        bands.value = FloatArray(12) { band ->
            val coefficient = coefficients[band]
            var a = 0.0
            var b = 0.0
            for (i in 0 until count) {
                val next = samples[i] + coefficient * a - b
                b = a
                a = next
            }
            val magnitude = sqrt(max(0.0, a*a + b*b - coefficient*a*b)) / count
            (ln(1 + magnitude * 120) / ln(15.0)).toFloat().coerceIn(0f, 1f)
        }
    }
}

