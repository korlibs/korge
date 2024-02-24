package korlibs.audio.sound.fade

import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.io.async.*
import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.*

val DEFAULT_FADE_TIME get() = 0.5.seconds
val DEFAULT_FADE_EASING get() = Easing.LINEAR

private val SoundChannelBase.fadeThread by extraProperty { AsyncThread() }
private var SoundChannelBase.changing by extraProperty { false }
private inline fun <T> SoundChannelBase.changing(block: () -> T): T {
    changing = true
    try {
        return block()
    } finally {
        changing = false
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun SoundChannelBase.fadeTo(volume: Double, time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeThread.cancelAndQueue {
    changing {
        val start = DateTime.now()
        val startVolume = this.volume
        val endVolume = volume
        while (true) {
            val now = DateTime.now()
            val elapsed = now - start
            val ratio = (elapsed / time).clamp01()
            this.volume = easing(ratio).toRatio().interpolate(startVolume, endVolume)
            if (ratio >= 1.0) break
            delay(1.milliseconds)
        }
    }
}

suspend fun SoundChannelBase.fadeOut(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeTo(0.0, time, easing)
suspend fun SoundChannelBase.fadeIn(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) = fadeTo(1.0, time, easing)

suspend fun SoundChannelBase.fadeOutPause(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    fadeOut(time, easing)
    pause()
}

suspend fun SoundChannelBase.fadeInResume(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    resume()
    fadeIn(time, easing)
}

val SoundChannelBase.pausedOrPausing get() = paused || (!paused && changing)

suspend fun SoundChannelBase.togglePausedFaded(enable: Boolean? = null, time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
    if (enable ?: pausedOrPausing) {
        //println("RESUME")
        fadeInResume(time, easing)
    } else {
        //println("PAUSE")
        fadeOutPause(time, easing)
    }
}
