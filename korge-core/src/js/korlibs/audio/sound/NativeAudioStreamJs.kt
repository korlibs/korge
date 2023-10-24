package korlibs.audio.sound

import korlibs.audio.internal.*
import korlibs.io.lang.*
import korlibs.platform.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider by lazy {
    if (Platform.isJsBrowser) {
        HtmlNativeSoundProvider()
    } else {
        DummyNativeSoundProvider
    }
}

class JsNewPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    nchannels: Int,
    freq: Int,
    gen: (AudioSamplesInterleaved) -> Unit
) : NewPlatformAudioOutput(
    coroutineContext, nchannels, freq, gen
) {
	init {
		nativeSoundProvider // Ensure it is created
	}

	var missingDataCount = 0
	var nodeRunning = false
	var node: ScriptProcessorNode? = null

	private var startPromise: Cancellable? = null

	override fun internalStart() {
		if (nodeRunning) return
		startPromise = HtmlSimpleSound.callOnUnlocked {
			node = HtmlSimpleSound.ctx?.createScriptProcessor(1024, channels, 2)
			node?.onaudioprocess = { e ->
                val nchannels = e.outputBuffer.numberOfChannels
                val outChannels = Array(nchannels) { e.outputBuffer.getChannelData(it) }
                val nsamples = e.outputBuffer.getChannelData(0).size
                val samples = AudioSamplesInterleaved(nchannels, nsamples)
                genSafe(samples)
                for (ch in 0 until nchannels) {
                    val outCh = outChannels[ch]
                    for (n in 0 until nsamples) {
                        outCh[n] = SampleConvert.shortToFloat(samples[ch, n])
                    }
                }

            }
			if (HtmlSimpleSound.ctx != null) this.node?.connect(HtmlSimpleSound.ctx.destination)
		}
        nodeRunning = true
		missingDataCount = 0
	}

	override fun internalStop() {
		if (!nodeRunning) return
		startPromise?.cancel()
		this.node?.disconnect()
		nodeRunning = false
	}
}
