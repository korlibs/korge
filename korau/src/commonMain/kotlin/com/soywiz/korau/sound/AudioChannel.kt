package com.soywiz.korau.sound

import com.soywiz.klock.*

class AudioChannel {
    private var channel: SoundChannel? = null

    val state get() = channel?.state ?: SoundChannelState.INITIAL

    val playing get() = state.playingOrPaused
    val current get() = channel?.current ?: 0.seconds
    val total get() = channel?.total ?: 0.seconds

    var volume: Double = 1.0
        set(value) {
            field = value
            channel?.volume = value
        }

    var pitch: Double = 1.0
        set(value) {
            field = value
            channel?.pitch = value
        }

    var panning: Double = 0.0
        set(value) {
            field = value
            channel?.panning = value
        }

    fun volume(value: Double): AudioChannel = this.apply { volume = value }
    fun pitch(value: Double): AudioChannel = this.apply { pitch = value }
    fun panning(value: Double): AudioChannel = this.apply { panning = value }

    fun play(
        sound: Sound,
        times: PlaybackTimes = 1.playbackTimes,
        startTime: TimeSpan = 0.seconds,
    ): AudioChannel {
        stop()
        channel = sound.play(PlaybackParameters(times = times, startTime = startTime, volume = volume, pitch = pitch, panning = panning))
        return this
    }

    suspend fun play(
        sound: AudioStream,
        times: PlaybackTimes = 1.playbackTimes,
        startTime: TimeSpan = 0.seconds,
    ): AudioChannel {
        return play(nativeSoundProvider.createStreamingSound(sound, true), times, startTime)
    }

    fun stop(): AudioChannel {
        channel?.stop()
        channel = null
        return this
    }

    fun reset(): AudioChannel {
        channel?.reset()
        return this
    }
}
