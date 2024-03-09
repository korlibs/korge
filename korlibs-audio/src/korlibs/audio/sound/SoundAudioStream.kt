package korlibs.audio.sound

import korlibs.datastructure.*
import korlibs.io.async.*
import korlibs.io.concurrent.*
import korlibs.io.lang.*
import korlibs.platform.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class SoundAudioData(
    coroutineContext: CoroutineContext,
    val audioData: AudioData,
    var soundProvider: NativeSoundProvider,
    val closeStream: Boolean = false,
    override val name: String = "Unknown",
    val onComplete: (suspend () -> Unit)? = null
) : Sound(coroutineContext) {
    override suspend fun decode(maxSamples: Int): AudioData = audioData

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        var pos = 0
        var paused = false
        var times = params.times
        var nas: NewPlatformAudioOutput? = null
        nas = soundProvider.createNewPlatformAudioOutput(coroutineContext, audioData.channels, audioData.rate) { it ->
            if (paused) {
                // @TODO: paused should not even call this right?
                for (ch in 0 until it.channels) {
                    audioData[ch].fill(0)
                }
                return@createNewPlatformAudioOutput
            }
            loop@for (ch in 0 until it.channels) {
                val audioDataCh = audioData[ch]
                for (n in 0 until it.totalSamples) {
                    val audioDataPos = pos + n
                    val sample = if (audioDataPos < audioDataCh.size) audioDataCh[audioDataPos] else 0
                    it[ch, n] = sample
                }
            }
            pos += it.totalSamples
            if (pos >= audioData.totalSamples) {
                pos = 0
                times = times.oneLess

                if (times == PlaybackTimes.ZERO) {
                    nas?.stop()
                }
            }
        }
        nas.copySoundPropsFromCombined(params, this)
        nas.start()
        return object : SoundChannel(this) {
            override var volume: Double by nas::volume
            override var pitch: Double by nas::pitch
            override var panning: Double by nas::panning
            override var current: TimeSpan
                get() = audioData.timeAtSample(pos)
                set(value) {
                    pos = audioData.sampleAtTime(value)
                }
            override val total: TimeSpan get() = audioData.totalTime
            override val state: SoundChannelState get() = when {
                paused -> SoundChannelState.PAUSED
                playing -> SoundChannelState.PLAYING
                else -> SoundChannelState.STOPPED
            }
            override fun pause() { nas.paused = true }
            override fun resume() { nas.paused = false }
            override fun stop() { nas.stop() }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
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
    override suspend fun decode(maxSamples: Int): AudioData = stream.toData(maxSamples)
    override suspend fun toStream(): AudioStream = stream.clone()
    override val nchannels: Int get() = stream.channels

    companion object {
        private val ID_POOL = ConcurrentPool<Int> { it }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val nas: PlatformAudioOutput = soundProvider.createPlatformAudioOutput(coroutineContext, stream.rate)
        nas.copySoundPropsFromCombined(params, this)
        var playing = true
        var paused = false
        var newStream: AudioStream? = null
        val channelId = ID_POOL.alloc()
        val dispatcherName = "SoundChannel-SoundAudioStream-$channelId"
        //println("dispatcher[a]=$dispatcher, thread=${currentThreadName}:${currentThreadId}")
        val job = launchAsap(coroutineContext) {
            val dispatcher = when {
                //Platform.runtime.isNative -> Dispatchers.createRedirectedDispatcher(dispatcherName, coroutineContext[CoroutineDispatcher.Key] ?: Dispatchers.Default)
                Platform.runtime.isNative -> null // @TODO: In MacOS audio is not working. Check why.
                else -> Dispatchers.createSingleThreadedDispatcher(dispatcherName)
            }
            try {
                withContext(dispatcher ?: EmptyCoroutineContext) {
                    //println("dispatcher[b]=$dispatcher, thread=${currentThreadName}:${currentThreadId}")
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
                        nas.stop()
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
            } finally {
                ID_POOL.free(channelId)
                when (dispatcher) {
                    is CloseableCoroutineDispatcher -> dispatcher.close()
                    is Closeable -> dispatcher.close()
                }
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
