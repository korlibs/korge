package korlibs.audio.sound

import korlibs.audio.format.*
import korlibs.audio.internal.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.time.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider by lazy {
    if (Platform.isJsBrowser) {
        HtmlNativeSoundProvider()
    } else {
        DummyNativeSoundProvider
    }
}

class HtmlNativeSoundProvider : NativeSoundProviderNew() {
    init {
        HtmlSimpleSound.ensureUnlockStart()
    }

    override fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput {
        return JsNewPlatformAudioOutput(coroutineContext, channels, frequency, gen)
    }

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound =
        AudioBufferSound(AudioBufferOrHTMLMediaElement(HtmlSimpleSound.loadSound(data)), "#bytes", coroutineContext, name)

    override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean, props: AudioDecodingProps): Sound = when (vfs) {
        is LocalVfs, is UrlVfs -> {
            //println("createSound[1]")
            val url = when (vfs) {
                is LocalVfs -> path
                is UrlVfs -> vfs.getFullUrl(path)
                else -> invalidOp
            }
            if (streaming) {
                AudioBufferSound(AudioBufferOrHTMLMediaElement(HtmlSimpleSound.loadSoundBuffer(url)), url, coroutineContext)
                //HtmlElementAudio(url)
            } else {
                AudioBufferSound(AudioBufferOrHTMLMediaElement(HtmlSimpleSound.loadSound(url)), url, coroutineContext)
            }
        }
        else -> {
            //println("createSound[2]")
            super.createSound(vfs, path)
        }
    }
}

class JsNewPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    nchannels: Int,
    frequency: Int,
    gen: (AudioSamplesInterleaved) -> Unit
) : NewPlatformAudioOutput(
    coroutineContext, nchannels, frequency, gen
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
            val ctx = HtmlSimpleSound.ctx
            if (ctx != null) {
                val bufferSize = 1024
                val scale = (frequency / ctx.sampleRate).toFloat()
                val samples = AudioSamplesInterleaved(channels, (bufferSize * scale).toInt())
                node = ctx.createScriptProcessor(bufferSize, channels, channels)
                //Console.log("sampleRate", ctx.sampleRate, "bufferSize", bufferSize, "totalSamples", samples.totalSamples, "scale", scale)
                node?.onaudioprocess = { e ->
                    genSafe(samples)
                    val separated = samples.separated()
                    for (ch in 0 until channels) {
                        val outCh = e.outputBuffer.getChannelData(ch)
                        val data = separated[ch]
                        for (n in 0 until bufferSize) {
                            outCh[n] = SampleConvert.shortToFloat(data.getSampled(n * scale))
                        }
                    }

                }
                this.node?.connect(ctx.destination)
            }
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

class AudioBufferSound(
    val buffer: AudioBufferOrHTMLMediaElement,
    val url: String,
    coroutineContext: CoroutineContext,
    override val name: String = "unknown"
) : Sound(coroutineContext) {
	override val length: TimeSpan = ((buffer.duration) ?: 0.0).seconds

    override val nchannels: Int get() = buffer.numberOfChannels ?: 1

    override suspend fun decode(maxSamples: Int): AudioData {
        if (this.buffer.isNull) return AudioData.DUMMY
        val buffer = this.buffer.audioBuffer
            ?: return AudioBufferSound(AudioBufferOrHTMLMediaElement(HtmlSimpleSound.loadSound(url)), url, defaultCoroutineContext).decode()
        val nchannels = buffer.numberOfChannels
        val nsamples = buffer.length
        val data = AudioSamples(nchannels, nsamples)
        for (c in 0 until nchannels) {
            var m = 0
            val channelF = buffer.getChannelData(c)
            for (n in 0 until nsamples) {
                data[c][m++] = SampleConvert.floatToShort(channelF[n])
            }
        }
        return AudioData(buffer.sampleRate, data)
    }

	override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val channel = if (buffer.isNotNull) HtmlSimpleSound.playSound(buffer, params, coroutineContext) else null
        HtmlSimpleSound.callOnUnlocked {
            channel?.play()
        }

		return object : SoundChannel(this) {

			override var volume: Double
				get() = channel?.volume ?: 1.0
				set(value) { channel?.volume = value}
			override var pitch: Double
				get() = channel?.pitch ?: 1.0
				set(value) { channel?.pitch = value }
			override var panning: Double
				get() = channel?.panning ?: 0.0
				set(value) { channel?.panning = value }
			override var current: TimeSpan
                get() = channel?.currentTime ?: 0.seconds
                set(value) { channel?.currentTime = value }
			override val total: TimeSpan = buffer?.duration?.seconds ?: 0.seconds
            override val state: SoundChannelState get() = when {
                channel?.pausedAt != null -> SoundChannelState.PAUSED
                channel?.playing ?: (current < total) -> SoundChannelState.PLAYING
                else -> SoundChannelState.STOPPED
            }

            override fun pause() {
                channel?.pause()
            }

            override fun resume() {
                channel?.resume()
            }

            override fun stop() {
                channel?.stop()
            }
		}.also {
            //it.current = params.startTime
            it.copySoundPropsFrom(params)
        }
	}
}
