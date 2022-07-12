package com.soywiz.korau.sound

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchAsap
import com.soywiz.korio.async.launchImmediately
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class SoundAudioStream(
    coroutineContext: CoroutineContext,
    val stream: AudioStream,
    var soundProvider: NativeSoundProvider,
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
        val nas: PlatformAudioOutput = soundProvider.createAudioStream(coroutineContext, stream.rate)
        nas.copySoundPropsFrom(params)
        var playing = true
        var paused = false
        var newStream: AudioStream? = null
        val job = launchAsap(coroutineContext) {
            val stream = stream.clone()
            newStream = stream
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
                    stream.currentPositionInSamples = 0L
                    while (!stream.finished) {
                        //println("STREAM")
                        while (paused) {
                            delay(2.milliseconds)
                            //println("PAUSED")
                        }
                        val read = stream.read(temp, 0, temp.totalSamples)
                        nas.add(temp, 0, read)
                        while (nas.availableSamples in minBuf..minBuf * 2) {
                            delay(2.milliseconds) // 100ms of buffering, and 1s as much
                            //println("STREAM.WAIT: ${nas.availableSamples}")
                        }
                        if (nas.availableSamples !in minBuf..minBuf * 2 && !stream.finished) {
                            //println("BUSY LOOP!")
                            delay(2.milliseconds)
                        }
                    }
                    times = times.oneLess
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
                get() = newStream?.currentTime ?: 0.milliseconds
                set(value) { newStream?.currentTime = value }
            override val total: TimeSpan get() = newStream?.totalLength ?: stream.totalLength
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
