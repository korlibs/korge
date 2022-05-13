package com.soywiz.korau.sound

import com.soywiz.kds.FloatArrayDeque
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korau.internal.SampleConvert
import com.soywiz.korau.internal.write
import com.soywiz.korio.async.delay
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import kotlinx.browser.document
import kotlin.coroutines.CoroutineContext


actual val nativeSoundProvider: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }

class JsPlatformAudioOutput(coroutineContext: CoroutineContext, val freq: Int) : PlatformAudioOutput(coroutineContext, freq) {
	val id = lastId++

	init {
		nativeSoundProvider.initOnce()
	}

	companion object {
		var lastId = 0
	}

	var missingDataCount = 0
	var nodeRunning = false
	var node: ScriptProcessorNode? = null

	private val nchannels = 2
	private val deques = Array(nchannels) { FloatArrayDeque() }

    private fun process(e: AudioProcessingEvent) {
		//val outChannels = Array(e.outputBuffer.numberOfChannels) { e.outputBuffer.getChannelData(it) }
        val outChannels = Array(e.outputBuffer.numberOfChannels) { e.outputBuffer.getChannelData(it) }
		var hasData = true

		if (!document.asDynamic().hidden) {
			for (channel in 0 until nchannels) {
				val deque = deques[channel]
				val outChannel = outChannels[channel]
				val read = deque.read(outChannel)
				if (read < outChannel.size) hasData = false
			}
		}

		if (!hasData) {
			missingDataCount++
		}

		if (missingDataCount >= 500) {
			stop()
		}
	}

	private fun ensureInit() { node }

	private var startPromise: Cancellable? = null

	override fun start() {
		if (nodeRunning) return
		startPromise = HtmlSimpleSound.callOnUnlocked {
			node = HtmlSimpleSound.ctx?.createScriptProcessor(1024, 2, 2)
			node?.onaudioprocess = { process(it) }
			if (HtmlSimpleSound.ctx != null) this.node?.connect(HtmlSimpleSound.ctx.destination)
		}
		missingDataCount = 0
		nodeRunning = true
	}

	override fun stop() {
		if (!nodeRunning) return
		startPromise?.cancel()
		this.node?.disconnect()
		nodeRunning = false
	}

	fun ensureRunning() {
		ensureInit()
		if (!nodeRunning) {
			start()
		}
	}

	var totalShorts = 0
	override val availableSamples get() = totalShorts

	override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
		//println("addSamples: $available, $size")
		//println(samples.sliceArray(offset until offset + size).toList())
		totalShorts += size
		if (!HtmlSimpleSound.available) {
			// Delay simulating consuming samples
			val sampleCount = (size / 2)
			val timeSeconds = sampleCount.toDouble() / 41_000.0
			coroutineContext.delay(timeSeconds.seconds)
		} else {
			ensureRunning()

            val schannels = samples.channels
			for (channel in 0 until nchannels) {
				val sample = samples[channel % schannels]
				val deque = deques[channel]
				for (n in 0 until size) {
					deque.write(SampleConvert.shortToFloat(sample[offset + n]))
				}
			}

			while (deques[0].availableRead > samples.totalSamples * 4) {
				coroutineContext.delay(4.milliseconds)
			}
		}
	}
}
