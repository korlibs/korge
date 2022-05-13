package com.soywiz.korau.sound

import com.soywiz.klock.milliseconds
import com.soywiz.kmem.clamp
import com.soywiz.kmem.clamp01
import com.soywiz.korio.async.delay
import com.soywiz.korio.lang.Disposable
import kotlin.coroutines.CoroutineContext

open class PlatformAudioOutput(
    val coroutineContext: CoroutineContext,
    val frequency: Int
) : Disposable, SoundProps {
	open val availableSamples: Int = 0
    override var pitch: Double = 1.0
    override var volume: Double = 1.0
    override var panning: Double = 0.0
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

open class DequeBasedPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    frequency: Int
) : PlatformAudioOutput(coroutineContext, frequency) {
    companion object {
        const val nchannels = 2
    }
    private val deque = AudioSamplesDeque(nchannels)

    override var pitch: Double = 1.0 ; set(value) { field = value; updateProps() }
    override var volume: Double = 1.0 ; set(value) { field = value; updateProps() }
    override var panning: Double = 0.0 ; set(value) { field = value; updateProps() }

    var volumes = FloatArray(2) { 1f }

    private fun updateProps() {
        val rratio = ((panning + 1.0) / 2.0).toFloat().clamp01()
        val lratio = 1f - rratio
        volumes[0] = (volume * lratio).toFloat()
        volumes[1] = (volume * rratio).toFloat()
    }

    protected val availableRead get() = deque.availableRead
    protected fun readFloat(channel: Int): Float = if (deque.availableRead >= 1) (deque.readFloat(channel) * volumes[channel]).clamp(-1f, +1f) else 0f
    protected fun readShort(channel: Int): Short = (readFloat(channel) * Short.MAX_VALUE).toInt().toShort()

    final override val availableSamples: Int get() = deque.availableRead
    final override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        deque.write(samples, offset, size)
    }
}
