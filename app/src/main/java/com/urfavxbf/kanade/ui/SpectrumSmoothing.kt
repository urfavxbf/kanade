package com.urfavxbf.kanade.ui

import kotlin.math.exp

/** Frame-rate-independent interpolation with faster attack and gentler release. */
internal fun smoothSpectrum(previous: FloatArray, target: FloatArray, seconds: Float): FloatArray {
    val elapsed = seconds.coerceIn(0f, 0.1f)
    return FloatArray(previous.size) { i ->
        val goal = target.getOrElse(i) { 0f }.coerceIn(0f, 1f)
        val rate = if (goal > previous[i]) 24f else 12f
        val amount = 1f - exp(-rate * elapsed)
        previous[i] + (goal - previous[i]) * amount
    }
}
