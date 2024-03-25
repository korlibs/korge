package korlibs.audio.sound

import korlibs.audio.format.*
import korlibs.datastructure.*
import korlibs.datastructure.pauseable.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.coroutineContext as coroutineContextKt

expect val nativeSoundProvider: NativeSoundProvider

open class LazyNativeSoundProvider(val gen: () -> NativeSoundProvider) : NativeSoundProvider() {
    val parent by lazy { gen() }

    override val target: String get() = parent.target

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = parent.createPlatformAudioOutput(coroutineContext, freq)
    override fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput =
        parent.createNewPlatformAudioOutput(coroutineContext, channels, frequency, gen)

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound =
        parent.createSound(data, streaming, props, name)

    override val audioFormats: AudioFormats get() = parent.audioFormats

    override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean, props: AudioDecodingProps): Sound =
        parent.createSound(vfs, path, streaming, props)

    override suspend fun createNonStreamingSound(data: AudioData, name: String): Sound = parent.createNonStreamingSound(data, name)

    override suspend fun createSound(data: AudioData, formats: AudioFormats, streaming: Boolean, name: String): Sound =
        parent.createSound(data, formats, streaming, name)

    override fun dispose() = parent.dispose()
}

open class NativeSoundProviderNew : NativeSoundProvider() {
    final override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        PlatformAudioOutputBasedOnNew(this, coroutineContext, freq)
}

open class NativeSoundProvider() : Disposable, Pauseable {
	open val target: String = "unknown"

    override var paused: Boolean = false

    open var listenerPosition: Vector3 = Vector3.ZERO
    open var listenerOrientationAt: Vector3 = Vector3.FORWARD // Look vector
    open var listenerOrientationUp: Vector3 = Vector3.UP
    // @TODO: Should this be estimated automatically from position samples?
    open var listenerSpeed: Vector3 = Vector3.ZERO

    @Deprecated("")
    open fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int = 44100): PlatformAudioOutput = PlatformAudioOutput(coroutineContext, freq)
    @Deprecated("")
    suspend fun createPlatformAudioOutput(freq: Int = 44100): PlatformAudioOutput = createPlatformAudioOutput(coroutineContextKt, freq)

    open fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int = 44100, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput {
        //println("createNewPlatformAudioOutput: ${this::class}")
        return NewPlatformAudioOutput(coroutineContext, channels, frequency, gen)
    }

    suspend fun createNewPlatformAudioOutput(nchannels: Int, freq: Int = 44100, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput = createNewPlatformAudioOutput(coroutineContextKt, nchannels, freq, gen)

    open suspend fun createSound(data: ByteArray, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT, name: String = "Unknown"): Sound {
        val format = props.formats ?: audioFormats
        val stream = format.decodeStreamOrError(data.openAsync(), props)
        return if (streaming) {
            createStreamingSound(stream, closeStream = true, name = name)
        } else {
            createNonStreamingSound(stream.toData(), name = name)
        }
	}

    open val audioFormats: AudioFormats by lazy { defaultAudioFormats }
    //open val audioFormats: AudioFormats = AudioFormats(WAV, MP3Decoder, OGG)

    open suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound {
        //println("createSound.coroutineContext: $coroutineContextKt")
        return if (streaming) {
            //val stream = vfs.file(path).open()
            //createStreamingSound(audioFormats.decodeStreamOrError(stream, props)) {
            val vfsFile = vfs.file(path)
            val stream: AsyncStream = if (props.readInMemory) vfsFile.readAll().openAsync() else vfsFile.open()

            createStreamingSound((props.formats ?: audioFormats).decodeStreamOrError(stream, props), name = vfsFile.baseName) {
                stream.close()
            }
        } else {
            createSound(vfs.file(path).read(), streaming, props, name = vfs[path].baseName)
        }
    }

	suspend fun createSound(file: FinalVfsFile, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound = createSound(file.vfs, file.path, streaming, props)
	suspend fun createSound(file: VfsFile, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound = createSound(file.getUnderlyingUnscapedFile(), streaming, props)

    open suspend fun createNonStreamingSound(
        data: AudioData,
        name: String = "Unknown"
    //): Sound = createStreamingSound(data.toStream(), true, name)
    ): Sound = SoundAudioData(coroutineContextKt, data, this, true, name)

    open suspend fun createSound(
		data: AudioData,
		formats: AudioFormats = defaultAudioFormats,
		streaming: Boolean = false,
        name: String = "Unknown"
	): Sound = createSound(WAV.encodeToByteArray(data), streaming, name = name)

    suspend fun createStreamingSound(stream: AudioStream, closeStream: Boolean = false, name: String = "Unknown", onComplete: (suspend () -> Unit)? = null): Sound =
        SoundAudioStream(kotlin.coroutines.coroutineContext, stream, this, closeStream, name, onComplete)

    suspend fun playAndWait(stream: AudioStream, params: PlaybackParameters = PlaybackParameters.DEFAULT) = createStreamingSound(stream).playAndWait(params)

    override fun dispose() {
    }
}

open class LogNativeSoundProvider : NativeSoundProvider() {
    data class AddInfo(val samples: AudioSamples, val offset: Int, val size: Int)
    val onBeforeAdd = Signal<AddInfo>()
    val onAfterAdd = Signal<AddInfo>()

    inner class PlatformLogAudioOutput(
        coroutineContext: CoroutineContext, frequency: Int
    ) : PlatformAudioOutput(coroutineContext, frequency) {
        val data = AudioSamplesDeque(2)
        override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
            val out = samples.clone()
            out.scaleVolume(volume)
            val addInfo = AddInfo(out, offset, size)
            onBeforeAdd(addInfo)
            data.write(out, offset, size)
            onAfterAdd(addInfo)
        }
        fun consumeToData(): AudioData = data.consumeToData(frequency)
        fun toData(): AudioData = data.toData(frequency)
    }

    val streams = arrayListOf<PlatformLogAudioOutput>()

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        return PlatformLogAudioOutput(coroutineContext, freq).also { streams.add(it) }
    }
}

open class DummyNativeSoundProvider : NativeSoundProvider() {
    companion object : DummyNativeSoundProvider()
}

class DummySoundChannel(sound: Sound, val data: AudioData? = null) : SoundChannel(sound) {
	private var timeStart = DateTime.now()
	override var current: TimeSpan
        get() = DateTime.now() - timeStart
        set(value) = Unit
	override val total: TimeSpan get() = data?.totalTime ?: 0.seconds

	override fun stop() {
		timeStart = DateTime.now() + total
	}
}

interface ReadonlySoundProps {
    val volume: Double
    val pitch: Double
    val panning: Double
    val position: Vector3
}

interface SoundProps : ReadonlySoundProps {
    override var volume: Double
    override var pitch: Double
    override var panning: Double
    override var position: Vector3
}

object DummySoundProps : SoundProps {
    override var volume: Double
        get() = 1.0
        set(v) = Unit
    override var pitch: Double
        get() = 1.0
        set(v) = Unit
    override var panning: Double
        get() = 0.0
        set(v) = Unit
    override var position: Vector3 = Vector3.ZERO
}

fun SoundProps.copySoundPropsFrom(other: ReadonlySoundProps) {
    this.volume = other.volume
    this.pitch = other.pitch
    this.panning = other.panning
}

fun SoundProps.volumeForChannel(channel: Int): Double {
    return when (channel) {
        0 -> panning.convertRangeClamped(-1.0, 0.0, 0.0, 1.0)
        else -> 1.0 - panning.convertRangeClamped(0.0, 1.0, 0.0, 1.0)
    }
}

fun SoundProps.applyPropsTo(samples: AudioSamplesInterleaved) {
    for (ch in 0 until samples.channels) {
        val volume01 = volumeForChannel(ch)
        for (n in 0 until samples.totalSamples) {
            var sample = samples[ch, n]
            sample = (sample * volume01).toInt().toShort()
            samples[ch, n] = sample
        }
    }
}

fun SoundProps.applyPropsTo(samples: AudioSamples) {
    for (ch in 0 until samples.channels) {
        val volume01 = volumeForChannel(ch)
        for (n in 0 until samples.totalSamples) {
            var sample = samples[ch, n]
            sample = (sample * volume01).toInt().toShort()
            samples[ch, n] = sample
        }
    }
}

fun SoundProps.copySoundPropsFromCombined(l: ReadonlySoundProps, r: ReadonlySoundProps) {
    this.volume = l.volume * r.volume
    this.pitch = l.pitch * r.pitch
    //this.panning = l.panning + r.panning
    this.panning = r.panning
}

class SoundChannelGroup(volume: Double = 1.0, pitch: Double = 1.0, panning: Double = 0.0) : SoundChannelBase, SoundChannelPlay, Extra by Extra.Mixin() {
    private val channels = arrayListOf<SoundChannelBase>()

    override val state: SoundChannelState get() = when {
        channels.any { it.playing } -> SoundChannelState.PLAYING
        channels.any { it.paused } -> SoundChannelState.PAUSED
        else -> SoundChannelState.STOPPED
    }

    override var volume: Double = 1.0
        set(value) {
            field = value
            all { it.volume = value }
        }
    override var pitch: Double = 1.0
        set(value) {
            field = value
            all { it.pitch = value }
        }
    override var panning: Double = 0.0
        set(value) {
            field = value
            all { it.panning = value }
        }
    override var position: Vector3 = Vector3.ZERO
        set(value) {
            field = value
            all { it.position = value }
        }

    init {
        this.volume = volume
        this.pitch = pitch
        this.panning = panning
    }

    @Suppress("DEPRECATION")
    fun register(channel: SoundChannelBase, coroutineContext: CoroutineContext) {
        add(channel)
        channel.onCompleted(coroutineContext) { remove(channel) }
    }

    @Deprecated("Use register instead of play")
    fun add(channel: SoundChannelBase) {
        channels.add(channel)
        setProps(channel)
    }
    @Deprecated("Use register instead of play")
    fun remove(channel: SoundChannelBase) {
        channels.remove(channel)
    }

    private fun setProps(channel: SoundChannelBase) {
        channel.volume = this.volume
        channel.pitch = this.pitch
        channel.panning = this.panning
        channel.position = this.position
    }

    @PublishedApi
    internal fun prune() {
        channels.removeAll { !it.playing }
    }

    private inline fun all(callback: (SoundChannelBase) -> Unit) {
        for (channel in channels) callback(channel)
        prune()
    }

    override fun reset() = all { it.reset() }
    override fun stop() = all { it.stop() }
    override fun resume() = all { it.resume() }
    override fun pause() = all { it.pause() }

    override fun play(coroutineContext: CoroutineContext, sound: Sound, params: PlaybackParameters): SoundChannel {
        return sound.play(
            coroutineContext,
            params.copy(
                volume = this.volume * params.volume,
                pitch = this.pitch * params.pitch,
                panning = this.panning * params.panning,
            )
        ).also { register(it, coroutineContext) }
    }
}

enum class SoundChannelState {
    INITIAL, PAUSED, PLAYING, STOPPED;

    val playing get() = this == PLAYING
    val paused get() = this == PAUSED
    val playingOrPaused get() = this == PAUSED || this == PLAYING
}

interface SoundChannelBase : SoundProps, Extra {
    val state: SoundChannelState
    fun reset(): Unit
    fun stop(): Unit
    fun resume(): Unit
    fun pause(): Unit

    fun onCompleted(coroutineContext: CoroutineContext, block: () -> Unit) {
        var blockOnce: (() -> Unit)? = null
        blockOnce = {
            blockOnce = null
            block()
        }

        coroutineContext.onCancel {
            blockOnce?.invoke()
        }
        coroutineContext.launchUnscoped {
            try {
                while (state.playing) delay(10.milliseconds)
            } finally {
                blockOnce?.invoke()
            }
        }
    }
}

suspend fun SoundChannelBase.await() {
    while (playingOrPaused) delay(1.milliseconds)
}

val SoundChannelBase.playing: Boolean get() = state.playing
val SoundChannelBase.paused: Boolean get() = state.paused
val SoundChannelBase.playingOrPaused: Boolean get() = state.playingOrPaused

@Deprecated("Use channel.play() instead")
fun <T : SoundChannelBase> T.attachTo(group: SoundChannelGroup): T = this.apply { group.add(this) }

abstract class SoundChannel(val sound: Sound) : SoundChannelBase, Extra by Extra.Mixin() {
	private var startTime = DateTime.now()
	override var volume = 1.0
	override var pitch = 1.0
	override var panning = 0.0 // -1.0 left, +1.0 right
    override var position: Vector3 = Vector3.ZERO
    // @TODO: Rename to position
	open var current: TimeSpan
        get() = DateTime.now() - startTime
        set(value) { startTime = DateTime.now() - value }
	open val total: TimeSpan get() = sound.length
    override val state: SoundChannelState get() = when {
        current < total -> SoundChannelState.PLAYING
        else -> SoundChannelState.STOPPED
    }
    final override fun reset() { current = 0.seconds }
	abstract override fun stop(): Unit

    override fun pause(): Unit = unsupported()
    override fun resume(): Unit = unsupported()
    fun togglePaused(): Unit = if (paused) resume() else pause()
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun SoundChannel.await(progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }) {
	try {
		while (playingOrPaused) {
			if (!paused) progress(current, total)
			delay(4.milliseconds)
		}
		progress(total, total)
	} catch (e: CancellationException) {
		stop()
	}
}

interface SoundChannelPlay {
    fun play(coroutineContext: CoroutineContext, sound: Sound, params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel
    fun play(coroutineContext: CoroutineContext, sound: Sound, times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, sound, PlaybackParameters(times, startTime))
    fun playForever(coroutineContext: CoroutineContext, sound: Sound, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, sound, infinitePlaybackTimes, startTime)
    suspend fun play(sound: Sound, params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel = play(coroutineContextKt, sound, params)
    suspend fun play(sound: Sound, times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContextKt, sound, times, startTime)
    suspend fun playForever(sound: Sound, startTime: TimeSpan = 0.seconds): SoundChannel = playForever(coroutineContextKt, sound, startTime)
    suspend fun playAndWait(sound: Sound, params: PlaybackParameters, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(sound, params).await(progress)
    suspend fun playAndWait(sound: Sound, times: PlaybackTimes = 1.playbackTimes, startTime: TimeSpan = 0.seconds, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(sound, times, startTime).await(progress)
}

interface SoundPlay {
    fun play(coroutineContext: CoroutineContext, params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel
    fun play(coroutineContext: CoroutineContext, times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, PlaybackParameters(times, startTime))
    fun playForever(coroutineContext: CoroutineContext, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, infinitePlaybackTimes, startTime)
    suspend fun play(params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel = play(coroutineContextKt, params)
    suspend fun play(times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContextKt, times, startTime)
    suspend fun playForever(startTime: TimeSpan = 0.seconds): SoundChannel = playForever(coroutineContextKt, startTime)
    suspend fun playAndWait(params: PlaybackParameters, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(params).await(progress)
    suspend fun playAndWait(times: PlaybackTimes = 1.playbackTimes, startTime: TimeSpan = 0.seconds, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(times, startTime).await(progress)
}

abstract class Sound(val creationCoroutineContext: CoroutineContext) : SoundProps, SoundPlay, AudioStreamable {
    var defaultCoroutineContext = creationCoroutineContext

    open val name: String = "UnknownNativeSound"
    override var volume: Double = 1.0
    override var panning: Double = 0.0
    override var pitch: Double = 1.0
    override var position: Vector3 = Vector3.ZERO
	open val length: TimeSpan = 0.seconds
    open val nchannels: Int get() = 1

    fun playNoCancel(times: PlaybackTimes = PlaybackTimes.ONE, startTime: TimeSpan = 0.seconds): SoundChannel = play(creationCoroutineContext + SupervisorJob(), times, startTime)
    fun playNoCancelForever(startTime: TimeSpan = 0.seconds): SoundChannel = play(creationCoroutineContext + SupervisorJob(), infinitePlaybackTimes, startTime)

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel = TODO()

    abstract suspend fun decode(maxSamples: Int = DEFAULT_MAX_SAMPLES): AudioData
    suspend fun toAudioData(maxSamples: Int = DEFAULT_MAX_SAMPLES): AudioData = decode(maxSamples)
    override suspend fun toStream(): AudioStream = decode().toStream()
    override fun toString(): String = "NativeSound('$name')"
}

data class PlaybackParameters(
    val times: PlaybackTimes = 1.playbackTimes,
    val startTime: TimeSpan = 0.seconds,
    val bufferTime: TimeSpan = 0.16.seconds,
    override val volume: Double = 1.0,
    override val pitch: Double = 1.0,
    override val panning: Double = 0.0,
    override val position: Vector3 = Vector3.ZERO,
    val onCancel: (() -> Unit)? = null,
    val onFinish: (() -> Unit)? = null,
) : ReadonlySoundProps {
    companion object {
        val DEFAULT = PlaybackParameters(1.playbackTimes, 0.seconds)
    }
}

val infinitePlaybackTimes get() = PlaybackTimes.INFINITE
inline val Int.playbackTimes get() = PlaybackTimes(this)

inline class PlaybackTimes(val count: Int) {
    companion object {
        val ZERO = PlaybackTimes(0)
        val ONE = PlaybackTimes(1)
        val INFINITE = PlaybackTimes(-1)
    }
    val hasMore get() = this != ZERO
    val oneLess get() = if (this == INFINITE) INFINITE else PlaybackTimes(count - 1)
    override fun toString(): String = if (count >= 0) "$count times" else "Infinite times"
}

suspend fun VfsFile.readSound(props: AudioDecodingProps = AudioDecodingProps.DEFAULT, streaming: Boolean = false): Sound = nativeSoundProvider.createSound(this, streaming, props)
suspend fun ByteArray.readSound(props: AudioDecodingProps = AudioDecodingProps.DEFAULT, streaming: Boolean = false): Sound = nativeSoundProvider.createSound(this, streaming, props)

suspend fun ByteArray.readMusic(props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound = readSound(streaming = true, props = props)
suspend fun VfsFile.readMusic(props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound = readSound(streaming = true, props = props)
