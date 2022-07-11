package com.soywiz.korau.sound

import com.soywiz.kds.Extra
import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korio.async.delay
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.file.FinalVfsFile
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.lang.Disposable
import com.soywiz.korio.lang.unsupported
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.native.concurrent.ThreadLocal
import kotlin.coroutines.coroutineContext as coroutineContextKt

@ThreadLocal
expect val nativeSoundProvider: NativeSoundProvider

open class LazyNativeSoundProvider(val prepareInit: () -> Unit = {}, val gen: () -> NativeSoundProvider) : NativeSoundProvider() {
    val parent by lazy { gen().also { it.initOnce() } }

    override val target: String get() = parent.target

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = parent.createAudioStream(coroutineContext, freq)

    override fun init() = prepareInit()

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound =
        parent.createSound(data, streaming, props, name)

    override val audioFormats: AudioFormats
        get() = parent.audioFormats

    override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean, props: AudioDecodingProps): Sound =
        parent.createSound(vfs, path, streaming, props)

    override suspend fun createNonStreamingSound(data: AudioData, name: String): Sound = parent.createNonStreamingSound(data, name)

    override suspend fun createSound(data: AudioData, formats: AudioFormats, streaming: Boolean, name: String): Sound =
        parent.createSound(data, formats, streaming, name)

    override fun dispose() = parent.dispose()
}

open class NativeSoundProvider : Disposable {
	open val target: String = "unknown"

	private var initialized = korAtomic(false)

	fun initOnce() {
		if (!initialized.value) {
			initialized.value = true
			init()
		}
	}

	open fun createAudioStream(coroutineContext: CoroutineContext, freq: Int = 44100): PlatformAudioOutput = PlatformAudioOutput(coroutineContext, freq)

    suspend fun createAudioStream(freq: Int = 44100): PlatformAudioOutput = createAudioStream(coroutineContextKt, freq)

	protected open fun init(): Unit = Unit

	open suspend fun createSound(data: ByteArray, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT, name: String = "Unknown"): Sound {
        val format = if (props.formats != null) AudioFormats(listOf(props.formats, *audioFormats.formats.toTypedArray()).filterNotNull()) else audioFormats
        val stream = format.decodeStreamOrError(data.openAsync(), props)
        return if (streaming) {
            createStreamingSound(stream, closeStream = true, name = name)
        } else {
            createNonStreamingSound(stream.toData(), name = name)
        }
	}

    open val audioFormats: AudioFormats = AudioFormats(WAV)
    //open val audioFormats: AudioFormats = AudioFormats(WAV, MP3Decoder, OGG)

    open suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean = false, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Sound {
        return if (streaming) {
            //val stream = vfs.file(path).open()
            //createStreamingSound(audioFormats.decodeStreamOrError(stream, props)) {
            val vfsFile = vfs.file(path)
            val stream: AsyncStream = if (props.readInMemory) vfsFile.readAll().openAsync() else vfsFile.open()
            createStreamingSound(audioFormats.decodeStreamOrError(stream, props), name = vfsFile.baseName) {
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
    ): Sound = createStreamingSound(data.toStream(), true, name)

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

open class LogNativeSoundProvider(
    override val audioFormats: AudioFormats
) : NativeSoundProvider() {
    class PlatformLogAudioOutput(
        coroutineContext: CoroutineContext, frequency: Int
    ) : PlatformAudioOutput(coroutineContext, frequency) {
        val data = AudioSamplesDeque(2)
        override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
            data.write(samples, offset, size)
        }
    }

    val streams = arrayListOf<PlatformLogAudioOutput>()

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        return PlatformLogAudioOutput(coroutineContext, freq).also { streams.add(it) }
    }
}

open class DummyNativeSoundProvider(
    override val audioFormats: AudioFormats
) : NativeSoundProvider() {
    companion object : DummyNativeSoundProvider(AudioFormats(WAV))
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
}

interface SoundProps : ReadonlySoundProps {
    override var volume: Double
    override var pitch: Double
    override var panning: Double
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
}

fun SoundProps.copySoundPropsFrom(other: ReadonlySoundProps) {
    this.volume = other.volume
    this.pitch = other.pitch
    this.panning = other.panning
}

fun SoundProps.copySoundPropsFromCombined(l: ReadonlySoundProps, r: ReadonlySoundProps) {
    this.volume = l.volume * r.volume
    this.pitch = l.pitch * r.pitch
    //this.panning = l.panning + r.panning
    this.panning = r.panning
}

class SoundChannelGroup(volume: Double = 1.0, pitch: Double = 1.0, panning: Double = 0.0) : SoundChannelBase {
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

    init {
        this.volume = volume
        this.pitch = pitch
        this.panning = panning
    }

    fun add(channel: SoundChannelBase) {
        channels.add(channel)
        setProps(channel)
    }
    fun remove(channel: SoundChannelBase) {
        channels.remove(channel)
    }

    private fun setProps(channel: SoundChannelBase) {
        channel.volume = this.volume
        channel.pitch = this.pitch
        channel.panning = this.panning
    }

    @PublishedApi
    internal fun prune() {
        channels.removeAll { !it.playing }
    }

    private inline fun all(callback: (SoundChannelBase) -> Unit) {
        for (channel in channels) callback(channel)
        prune()
    }

    override fun reset(): Unit = all { it.reset() }
    override fun stop(): Unit = all { it.stop() }
}

enum class SoundChannelState {
    INITIAL, PAUSED, PLAYING, STOPPED;

    val playing get() = this == PLAYING
    val paused get() = this == PAUSED
    val playingOrPaused get() = this == PAUSED || this == PLAYING
}


interface SoundChannelBase : SoundProps {
    val state: SoundChannelState
    fun reset(): Unit
    fun stop(): Unit
}

suspend fun SoundChannelBase.await() {
    while (playingOrPaused) delay(1.milliseconds)
}

val SoundChannelBase.playing get() = state.playing
val SoundChannelBase.paused get() = state.paused
val SoundChannelBase.playingOrPaused get() = state.playingOrPaused

fun <T : SoundChannelBase> T.attachTo(group: SoundChannelGroup): T = this.apply { group.add(this) }

abstract class SoundChannel(val sound: Sound) : SoundChannelBase, Extra by Extra.Mixin() {
	private var startTime = DateTime.now()
	override var volume = 1.0
	override var pitch = 1.0
	override var panning = 0.0 // -1.0 left, +1.0 right
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

    open fun pause(): Unit = unsupported()
    open fun resume(): Unit = unsupported()
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

abstract class Sound(val creationCoroutineContext: CoroutineContext) : SoundProps, AudioStreamable {
    var defaultCoroutineContext = creationCoroutineContext
    @Deprecated("Use defaultCoroutineContext instead", ReplaceWith("defaultCoroutineContext"))
    val coroutineContext: CoroutineContext get() = defaultCoroutineContext

    open val name: String = "UnknownNativeSound"
    override var volume: Double = 1.0
    override var panning: Double = 0.0
    override var pitch: Double = 1.0
	open val length: TimeSpan = 0.seconds
    open val nchannels: Int get() = 1

    fun playNoCancel(times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(creationCoroutineContext + SupervisorJob(), times, startTime)
    fun playNoCancelForever(startTime: TimeSpan = 0.seconds): SoundChannel = play(creationCoroutineContext + SupervisorJob(), infinitePlaybackTimes, startTime)

    open fun play(coroutineContext: CoroutineContext, params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel = TODO()
    fun play(coroutineContext: CoroutineContext, times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, PlaybackParameters(times, startTime))
    fun playForever(coroutineContext: CoroutineContext, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContext, infinitePlaybackTimes, startTime)

    suspend fun play(params: PlaybackParameters = PlaybackParameters.DEFAULT): SoundChannel = play(coroutineContextKt, params)
    suspend fun play(times: PlaybackTimes, startTime: TimeSpan = 0.seconds): SoundChannel = play(coroutineContextKt, times, startTime)
    suspend fun playForever(startTime: TimeSpan = 0.seconds): SoundChannel = playForever(coroutineContextKt, startTime)

    suspend fun playAndWait(params: PlaybackParameters, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(params).await(progress)
    suspend fun playAndWait(times: PlaybackTimes = 1.playbackTimes, startTime: TimeSpan = 0.seconds, progress: SoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit = play(times, startTime).await(progress)

    abstract suspend fun decode(): AudioData
    suspend fun toData(): AudioData = decode()
    override suspend fun toStream(): AudioStream = decode().toStream()
    override fun toString(): String = "NativeSound('$name')"
}

data class PlaybackParameters(
    val times: PlaybackTimes = 1.playbackTimes,
    val startTime: TimeSpan = 0.seconds,
    val bufferTime: TimeSpan = 0.1.seconds,
    override val volume: Double = 1.0,
    override val pitch: Double = 1.0,
    override val panning: Double = 0.0,
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
