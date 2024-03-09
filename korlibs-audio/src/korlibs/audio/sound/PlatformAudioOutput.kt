package korlibs.audio.sound

import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

open class NewPlatformAudioOutput(
    val coroutineContext: CoroutineContext,
    val channels: Int,
    val frequency: Int,
    private val gen: (AudioSamplesInterleaved) -> Unit,
) : Disposable, SoundProps {
    var onCancel: Cancellable? = null
    var paused: Boolean = false

    private val lock = Lock()
    fun genSafe(buffer: AudioSamplesInterleaved) {
        lock {
            try {
                gen(buffer)
                applyPropsTo(buffer)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override var pitch: Double = 1.0
    override var volume: Double = 1.0
    override var panning: Double = 0.0
    override var position: Vector3 = Vector3.ZERO

    protected open fun internalStart() = Unit
    protected open fun internalStop() = Unit

    fun start() {
        stop()
        onCancel = coroutineContext.onCancel { stop() }
        internalStart()
    }
    fun stop() {
        onCancel?.cancel()
        onCancel = null
        internalStop()
    }
    final override fun dispose() = stop()
}

open class PlatformAudioOutputBasedOnNew(
    val soundProvider: NativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int,
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    init{
        println("PlatformAudioOutputBasedOnNew[$frequency] = $soundProvider")
    }

    val new = soundProvider.createNewPlatformAudioOutput(coroutineContext, 2, frequency) { buffer ->
        //println("availableRead=$availableRead")
        //if (availableRead >= buffer.data.size) {
        readSamplesInterleaved(buffer, fully = true)
        //}
    }

    override fun start() {
        new.start()
    }

    override fun stop() {
        new.stop()
    }
}

open class PlatformAudioOutput(
    val coroutineContext: CoroutineContext,
    val frequency: Int
) : Disposable, SoundProps {
	open val availableSamples: Int = 0
    override var pitch: Double = 1.0
    override var volume: Double = 1.0
    override var panning: Double = 0.0
    override var position: Vector3 = Vector3.ZERO
	open suspend fun add(samples: AudioSamples, offset: Int = 0, size: Int = samples.totalSamples) {
        delay(100.milliseconds)
    }
	suspend fun add(data: AudioData) = add(data.samples, 0, data.totalSamples)
	open fun start() = Unit
    //open fun pause() = unsupported()
	open fun stop() = Unit
    // @TODO: We should week stop or dispose, but maybe not both

    open suspend fun wait() {
        while (availableSamples > 0) {
            delay(10.milliseconds)
        }
    }

    final override fun dispose() = stop()
}

abstract class ThreadBasedPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    frequency: Int,
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    var nativeThread: NativeThread? = null
    var running = false

    val totalPendingSamples: Long get() = availableSamples.toLong() + samplesPendingToPlay
    protected abstract val samplesPendingToPlay: Long
    protected open val chunkSize: Int get() = 1024
    protected abstract fun open(frequency: Int, channels: Int)
    protected abstract fun write(data: AudioSamples, offset: Int, count: Int): Int
    protected abstract fun close()

    final override suspend fun wait() {
        while (totalPendingSamples > 0) {
            delay(1.milliseconds)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    protected fun writeFully(data: AudioSamples, count: Int) {
        var offset = 0
        var pending = count
        var nonAdvancingCount = 0
        while (pending > 0 && nonAdvancingCount < 10) {
            val written = write(data, offset, pending)
            if (written < 0) {
                println("ThreadBasedPlatformAudioOutput.notFullyWritten: offset=$offset, pending=$pending, written=$written")
                break
            }
            if (written == 0) {
                println("ThreadBasedPlatformAudioOutput.nothingWritten: offset=$offset, pending=$pending, written=$written")
                blockingSleep(1.milliseconds)
                nonAdvancingCount++
            }
            offset += written
            pending -= written
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    final override fun start() {
        if (running) return
        running = true
        var opened = false
        // @TODO: Use a thread pool to reuse audio threads?
        nativeThread = NativeThread {
            val temp = AudioSamples(nchannels, chunkSize)

            try {
                while (running) {
                    val totalRead = readSamples(temp)
                    if (totalRead == 0) {
                        blockingSleep(1.milliseconds)
                        continue
                    }
                    if (!opened) {
                        opened = true
                        open(frequency, nchannels)
                    }

                    writeFully(temp, totalRead)
                }
            } finally {
                if (opened) {
                    opened = false
                    close()
                }
                running = false
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    final override fun stop() {
        running = false
    }
}

open class DequeBasedPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    frequency: Int
) : PlatformAudioOutput(coroutineContext, frequency) {
    companion object {
        const val nchannels = 2
    }
    private val lock = NonRecursiveLock()
    private val deque = AudioSamplesDeque(nchannels)

    override var pitch: Double = 1.0 ; set(value) { field = value; updateProps() }
    override var volume: Double = 1.0 ; set(value) { field = value; updateProps() }
    override var panning: Double = 0.0 ; set(value) { field = value; updateProps() }
    override var position: Vector3 = Vector3.ZERO; set(value) { field = value; updateProps() }

    var volumes = FloatArray(2) { 1f }

    private fun updateProps() {
        val rratio = ((panning + 1.0) / 2.0).toFloat().clamp01()
        val lratio = 1f - rratio
        volumes[0] = (volume * lratio).toFloat()
        volumes[1] = (volume * rratio).toFloat()
    }

    protected val availableRead get() = lock { deque.availableRead }
    private fun _readFloat(channel: Int): Float = if (deque.availableRead >= 1) (deque.readFloat(channel) * volumes[channel]).clamp(-1f, +1f) else 0f
    private fun _readShort(channel: Int): Short = if (deque.availableRead >= 1) (deque.read(channel) * volumes[channel]).toInt().toShortClamped() else 0
    protected fun readFloat(channel: Int): Float = lock { _readFloat(channel) }
    protected fun readShort(channel: Int): Short = lock { _readShort(channel) }

    protected fun readShortsInterleaved(out: AudioSamplesInterleaved): Int {
        lock {
            val readCount = minOf(availableRead, out.totalSamples)
            //val readCount = out.totalSamples
            for (n in 0 until readCount) {
                for (ch in 0 until nchannels) {
                    out[ch, n] = _readShort(ch)
                }
            }
            return readCount
        }
    }

    protected fun readFloats(channel: Int, out: FloatArray, offset: Int = 0, count: Int = out.size - offset) {
        lock {
            for (n in 0 until count) out[offset + n] = _readFloat(channel)
        }
    }
    protected fun readShorts(channel: Int, out: ShortArray, offset: Int = 0, count: Int = out.size - offset) {
        lock {
            for (n in 0 until count) out[offset + n] = _readShort(channel)
        }
    }

    protected fun readShortsPartial(out: Array<ShortArray>, offset: Int = 0, count: Int = out[0].size - offset, nchannels: Int = out.size): Int {
        return _readShorts(out, offset, count, nchannels, fully = false)
    }

    protected fun readShortsFully(out: Array<ShortArray>, offset: Int = 0, count: Int = out[0].size - offset, nchannels: Int = out.size): Int {
        return _readShorts(out, offset, count, nchannels, fully = true)
    }

    protected fun readShorts(out: Array<ShortArray>, offset: Int = 0, count: Int = out[0].size - offset, nchannels: Int = out.size) {
        _readShorts(out, offset, count, nchannels, fully = true)
    }

    protected fun _readShorts(out: Array<ShortArray>, offset: Int = 0, count: Int = out[0].size - offset, nchannels: Int = out.size, fully: Boolean): Int {
        lock {
            val totalRead = if (fully) count else minOf(availableRead, count)

            for (n in 0 until totalRead) {
                for (ch in 0 until nchannels) {
                    out[ch][offset + n] = _readShort(ch)
                }
            }

            return totalRead
        }
    }

    protected fun readSamplesInterleaved(out: IAudioSamples, offset: Int = 0, count: Int = out.totalSamples - offset, nchannels: Int = out.channels, fully: Boolean): Int {
        lock {
            val totalRead = if (fully) count else minOf(availableRead, count)

            for (n in 0 until totalRead) {
                for (ch in 0 until nchannels) {
                    out[ch, offset + n] = _readShort(ch)
                }
            }

            return totalRead
        }
    }

    protected fun readSamples(samples: AudioSamples, offset: Int = 0, count: Int = samples.totalSamples - offset, fully: Boolean = false): Int {
        return _readShorts(samples.data, offset, count, fully = fully)
    }

    override val availableSamples: Int get() = lock { deque.availableRead }
    final override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        while (deque.availableRead >= 1024 * 16) delay(1.milliseconds)
        lock { deque.write(samples, offset, size) }
    }
}
