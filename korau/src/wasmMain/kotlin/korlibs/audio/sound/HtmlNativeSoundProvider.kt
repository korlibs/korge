package korlibs.audio.sound

import korlibs.time.TimeSpan
import korlibs.time.seconds
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.internal.SampleConvert
import korlibs.io.file.Vfs
import korlibs.io.file.std.LocalVfs
import korlibs.io.file.std.UrlVfs
import korlibs.io.lang.invalidOp
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.*
import org.w3c.dom.HTMLAudioElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class HtmlNativeSoundProvider : NativeSoundProvider() {
    init {
        HtmlSimpleSound.ensureUnlockStart()
    }

	override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = JsPlatformAudioOutput(coroutineContext, freq)

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

class HtmlElementAudio(
    val audio: HTMLAudioElement,
    coroutineContext: CoroutineContext,
) : Sound(coroutineContext) {
    override val length: TimeSpan get() = audio.duration.seconds

    override suspend fun decode(maxSamples: Int): AudioData =
        AudioBufferSound(AudioBufferOrHTMLMediaElement(HtmlSimpleSound.loadSound(audio.src)), audio.src, defaultCoroutineContext).decode()

    companion object {
        suspend operator fun invoke(url: String): HtmlElementAudio {
            val audio = createAudioElement(url)
            val promise = CompletableDeferred<Unit>()
            audio.oncanplay = {
                promise.complete(Unit)
                null
            }
            audio.oncanplaythrough = {
                promise.complete(Unit)
                null
            }
            promise.await()
            //HtmlSimpleSound.waitUnlocked()
            return HtmlElementAudio(audio, coroutineContext)
        }
    }

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val audioCopy = audio.clone()
        audioCopy.volume = params.volume
        HtmlSimpleSound.callOnUnlocked {
            audioCopy.play()
        }
        audioCopy.oncancel = {
            params.onCancel?.invoke()
            null
        }
        audioCopy.onended = {
            params.onFinish?.invoke()
            null
        }
        return object : SoundChannel(this@HtmlElementAudio) {
            override var volume: Double
                get() = audioCopy.volume
                set(value) {
                    audioCopy.volume = value
                }

            override var pitch: Double
                get() = 1.0
                set(value) {}

            override var panning: Double
                get() = 0.0
                set(value) {}

            override val total: TimeSpan get() = audioCopy.duration.seconds
            override var current: TimeSpan
                get() = audioCopy.currentTime.seconds
                set(value) { audioCopy.currentTime = value.seconds }

            override val state: SoundChannelState get() = when {
                audioCopy.paused -> SoundChannelState.PAUSED
                audioCopy.ended -> SoundChannelState.STOPPED
                else -> SoundChannelState.PLAYING
            }

            override fun pause() {
                audioCopy.pause()
            }

            override fun resume() {
                audioCopy.play()
            }

            override fun stop() {
                audioCopy.pause()
                current = 0.seconds
            }
        }
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
            //println("decode:$c: ${data.channels}")
            //println("decode:$c: ${data[c].size}")
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
