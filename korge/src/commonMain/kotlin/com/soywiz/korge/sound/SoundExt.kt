package com.soywiz.korge.sound

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlin.coroutines.cancellation.*

val DEFAULT_FADE_TIME get() = 0.5.seconds
val DEFAULT_FADE_EASING get() = Easing.LINEAR

private val SoundChannel.fadeThread by extraProperty { AsyncThread() }
private var SoundChannel.changing by extraProperty { false }
private inline fun <T> SoundChannel.changing(block: () -> T): T {
    changing = true
    try {
        return block()
    } finally {
        changing = false
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun SoundChannel.fadeTo(volume: Double, time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeThread.cancelAndQueue {
    changing {
        val start = DateTime.now()
        val startVolume = this.volume
        val endVolume = volume
        while (true) {
            val now = DateTime.now()
            val elapsed = now - start
            val ratio = (elapsed / time).clamp01()
            this.volume = easing(ratio).interpolate(startVolume, endVolume)
            if (ratio >= 1.0) break
            delay(1.milliseconds)
        }
    }
}

suspend fun SoundChannel.fadeOut(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeTo(0.0, time, easing)
suspend fun SoundChannel.fadeIn(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeTo(1.0, time, easing)

suspend fun SoundChannel.fadeOutPause(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    fadeOut(time, easing)
    pause()
}

suspend fun SoundChannel.fadeInResume(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    resume()
    fadeIn(time, easing)
}

val SoundChannel.pausedOrPausing get() = paused || (!paused && changing)

suspend fun SoundChannel.togglePausedFaded(enable: Boolean? = null, time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    if (enable ?: pausedOrPausing) {
        //println("RESUME")
        fadeInResume(time, easing)
    } else {
        //println("PAUSE")
        fadeOutPause(time, easing)
    }
}
