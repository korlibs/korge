package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.*

class SoundAudioStream(
    coroutineContext: CoroutineContext,
    val stream: AudioStream,
    val closeStream: Boolean = false,
    override val name: String = "Unknown",
    val onComplete: (suspend () -> Unit)? = null
) : Sound(coroutineContext) {
    val nativeSound = this
    override val length: TimeSpan get() = stream.totalLength
    override suspend fun decode(): AudioData = stream.toData()
    override suspend fun toStream(): AudioStream = stream.clone()
    override val nchannels: Int get() = stream.channels

    @OptIn(ExperimentalStdlibApi::class)
    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val nas: PlatformAudioOutput = nativeSoundProvider.createAudioStream(coroutineContext, stream.rate)
        nas.copySoundPropsFrom(params)
        var playing = true
        var paused = false
        val job = launchImmediately(coroutineContext) {
            val stream = stream.clone()
            stream.currentTime = params.startTime
            playing = true
            //println("STREAM.START")
            var times = params.times
            try {
                val temp = AudioSamples(stream.channels, 1024)
                val nchannels = 2
                val minBuf = (stream.rate * nchannels * params.bufferTime.seconds).toInt()
                nas.start()
                while (times.hasMore) {
                    while (!stream.finished) {
                        //println("STREAM")
                        while (paused) delay(2.milliseconds)
                        val read = stream.read(temp, 0, temp.totalSamples)
                        nas.add(temp, 0, read)
                        while (nas.availableSamples in minBuf..minBuf * 2) {
                            delay(2.milliseconds) // 100ms of buffering, and 1s as much
                            //println("STREAM.WAIT: ${nas.availableSamples}")
                        }
                    }
                    times = times.oneLess
                    stream.currentPositionInSamples = 0L
                }
            } catch (e: CancellationException) {
                // Do nothing
                params.onCancel?.invoke()
            } finally {
                nas.wait()
                nas.stop()
                if (closeStream) {
                    stream.close()
                }
                playing = false
                params.onFinish?.invoke()
                onComplete?.invoke()
            }
        }
        fun close() {
            job.cancel()
        }
        return object : SoundChannel(nativeSound) {
            override var volume: Double by nas::volume
            override var pitch: Double by nas::pitch
            override var panning: Double by nas::panning
            override var current: TimeSpan
                get() = stream.currentTime
                set(value) = run { stream.currentTime = value }
            override val total: TimeSpan get() = stream.totalLength
            override val state: SoundChannelState get() = when {
                paused -> SoundChannelState.PAUSED
                playing -> SoundChannelState.PLAYING
                else -> SoundChannelState.STOPPED
            }
            override fun pause() { paused = true }
            override fun resume() { paused = false }
            override fun stop() = close()
        }
    }
}
