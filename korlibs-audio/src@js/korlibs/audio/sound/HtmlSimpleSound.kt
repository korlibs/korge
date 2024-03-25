package korlibs.audio.sound

import korlibs.time.DateTime
import korlibs.time.TimeSpan
import korlibs.time.seconds
import korlibs.logger.Logger
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.uniVfs
import korlibs.io.lang.Cancellable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int8Array
import org.w3c.dom.Audio
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.events.Event
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val AudioBuffer.durationOrNull: Double? get() = duration.takeIf { !it.isNaN() }
val HTMLMediaElement.durationOrNull: Double? get() = duration.takeIf { !it.isNaN() }

class AudioBufferOrHTMLMediaElement(
    val audioBuffer: AudioBuffer?,
    val htmlAudioElement: HTMLAudioElement?,
) {
    constructor(audioBuffer: AudioBuffer?) : this(audioBuffer, null)
    constructor(htmlMediaElement: HTMLAudioElement?) : this(null, htmlMediaElement)

    val isNull get() = audioBuffer == null && htmlAudioElement == null
    val isNotNull get() = !isNull

    val duration: Double? get() = when {
        audioBuffer != null -> audioBuffer.durationOrNull
        htmlAudioElement != null -> htmlAudioElement.durationOrNull
        else -> null
    }
    val numberOfChannels: Int get() = audioBuffer?.numberOfChannels ?: 1
}

fun createAudioElement(
    src: String,
    currentTime: Double = 0.0,
    autoplay: Boolean = false,
    crossOrigin: String? = "anonymous"
): Audio {
    val out = Audio(src)
    out.crossOrigin = crossOrigin
    out.currentTime = currentTime
    out.autoplay = autoplay
    out.pause()
    return out
}

fun HTMLAudioElement.clone(): Audio =
    createAudioElement(this.src, this.currentTime, this.autoplay, this.crossOrigin)

object HtmlSimpleSound {
    private val logger = Logger("HtmlSimpleSound")

	val ctx: BaseAudioContext? = try {
		when {
			jsTypeOf(window.asDynamic().AudioContext) != "undefined" -> AudioContext()
			jsTypeOf(window.asDynamic().webkitAudioContext) != "undefined" -> webkitAudioContext()
			else -> null
		}.also {
            (window.asDynamic()).globalAudioContext = it
        }
	} catch (e: Throwable) {
        logger.error { e }
		null
	}

	val available get() = ctx != null
	var unlocked = false
	private val unlockDeferred = CompletableDeferred<Unit>(Job())
	val unlock = unlockDeferred as Deferred<Unit>

	class SimpleSoundChannel(
		val buffer: AudioBufferOrHTMLMediaElement,
		val ctx: BaseAudioContext?,
        val params: PlaybackParameters,
        val coroutineContext: CoroutineContext
	) {
        var gainNode: GainNode? = null
        var pannerNode: PannerNode? = null
        var sourceNode: AudioScheduledSourceNode? = null
        var realHtmlAudioElement: HTMLAudioElement? = null

        fun createNode(startTime: TimeSpan) {
            realHtmlAudioElement?.pause()
            sourceNode?.disconnect()

            val htmlAudioElement = buffer.htmlAudioElement
            val audioBuffer = buffer.audioBuffer

            ctx?.destination?.apply {
                pannerNode = panner {
                    gainNode = gain {
                        when {
                            htmlAudioElement != null -> {
                                realHtmlAudioElement = htmlAudioElement.clone()
                                sourceNode = source(realHtmlAudioElement!!)
                            }
                            audioBuffer != null -> {
                                sourceNode = source(audioBuffer)
                            }
                        }
                    }
                }
                updateNodes()
            }
            val realHtmlAudioElement = this.realHtmlAudioElement
            if (realHtmlAudioElement != null) {
                realHtmlAudioElement.currentTime = startTime.seconds
                realHtmlAudioElement.play()
            } else {
                sourceNode?.start(0.0, startTime.seconds)
            }
        }

        var startedAt = DateTime.now()
        var times = params.times

        fun createJobAt(startTime: TimeSpan): Job {
            if (coroutineContext.job.isCompleted) {
                logger.warn { "Sound won't play because coroutineContext.job is completed" }
            }
            startedAt = DateTime.now()
            var startTime = startTime
            ctx?.resume()
            return launchImmediately(coroutineContext) {
                try {
                    while (times.hasMore) {
                        //println("TIMES: $times, startTime=$startTime, buffer.duration.seconds=${buffer.duration.seconds}")
                        startedAt = DateTime.now()
                        createNode(startTime)
                        startTime = 0.seconds
                        val deferred = CompletableDeferred<Unit>()
                        //println("sourceNode: $sourceNode, ctx?.state=${ctx?.state}, buffer.duration=${buffer.duration}")
                        if (sourceNode == null || ctx?.state != "running") {
                            window.setTimeout(
                                { deferred.complete(Unit) },
                                ((buffer.unsafeCast<HTMLMediaElement>().durationOrNull ?: 0.0) * 1000).toInt()
                            )
                        } else {
                            sourceNode?.onended = {
                                deferred.complete(Unit)
                            }
                        }
                        //println("awaiting sound")
                        deferred.await()
                        times = times.oneLess
                        //println("sound awaited")
                        if (!times.hasMore) break
                    }
                } catch (e: CancellationException) {
                    params.onCancel?.invoke()
                } finally {
                    running = false
                    val realHtmlAudioElement = this.realHtmlAudioElement
                    if (realHtmlAudioElement != null) {
                        realHtmlAudioElement.pause()
                        realHtmlAudioElement.currentTime = 0.0
                        //sourceNode?.stop()
                    } else {
                        sourceNode?.stop()
                    }
                    gainNode = null
                    pannerNode = null
                    sourceNode = null
                    params.onFinish?.invoke()
                }
            }
        }

		var currentTime: TimeSpan
            get() = DateTime.now() - startedAt
            set(value) {
                job?.cancel()
                job = createJobAt(value)
            }
        var volume: Double = params.volume
            set(value) {
                field = value
                updateVolume()
            }
        var pitch: Double = params.pitch
            set(value) {
                field = value
                updatePitch()
            }
        var panning: Double = params.panning
            set(value) {
                field = value
                updatePanning()
            }



        private var running = true
        var pausedAt: TimeSpan? = null

        fun updateNodes() {
            updateVolume()
            updatePitch()
            updatePanning()
        }

        fun updateVolume() {
            gainNode?.gain?.value = volume
        }

        fun updatePitch() {
        }

        fun updatePanning() {
            pannerNode?.setPosition(panning, 0.0, 0.0)
            pannerNode?.setOrientation(0.0, 1.0, 0.0)
        }

		//val playing get() = running && currentTime < buffer.duration
        val playing: Boolean
            get() = running.also {
            //println("playing: $running")
        }

        fun pause() {
            this.pausedAt = currentTime
            if (realHtmlAudioElement != null) {
                realHtmlAudioElement?.pause()
            } else {
                stop()
            }
        }

        fun resume() {
            if (realHtmlAudioElement != null) {
                realHtmlAudioElement?.play()
            } else {
                this.pausedAt?.let { currentTime = it }
            }
            this.pausedAt = null
        }

        fun stop() {
            job?.cancel()
		}

        fun play() {
            if (job != null && realHtmlAudioElement != null) {
                realHtmlAudioElement?.play()
            } else {
                stop()
                job = createJobAt(params.startTime)
            }
        }

        var job: Job? = null
    }

	fun AudioNode.panner(callback: PannerNode.() -> Unit = {}): PannerNode? {
		val ctx = ctx ?: return null
		val node = kotlin.runCatching { ctx.createPanner() }.getOrNull() ?: return null
		callback(node)
		node.connect(this)
		return node
	}

	fun AudioNode.gain(callback: GainNode.() -> Unit = {}): GainNode? {
		val ctx = ctx ?: return null
		val node = ctx.createGain()
		callback(node)
		node.connect(this)
		return node
	}

    fun AudioNode.sourceAny(buffer: AudioBufferOrHTMLMediaElement, callback: AudioScheduledSourceNode.() -> Unit = {}): AudioScheduledSourceNode? {
        val audioBuffer = buffer.audioBuffer
        val htmlAudioElement = buffer.htmlAudioElement
        return when {
            audioBuffer != null -> source(audioBuffer) { callback() }
            htmlAudioElement != null -> source(htmlAudioElement) { callback() }
            else -> error("Unexpected buffer $buffer")
        }
    }

	fun AudioNode.source(buffer: AudioBuffer, callback: AudioBufferSourceNode.() -> Unit = {}): AudioBufferSourceNode? {
		val ctx = ctx ?: return null
		val node = ctx.createBufferSource()
		node.buffer = buffer
		callback(node)
		node.connect(this)
		return node
	}

    fun AudioNode.source(buffer: HTMLAudioElement, callback: MediaElementAudioSourceNode.() -> Unit = {}): MediaElementAudioSourceNode? {
        val ctx = ctx ?: return null
        val node = ctx.createMediaElementSource(buffer)
        callback(node)
        node.connect(this)
        return node
    }

    fun playSound(buffer: AudioBufferOrHTMLMediaElement, params: PlaybackParameters, coroutineContext: CoroutineContext): SimpleSoundChannel? {
        return ctx?.let { SimpleSoundChannel(buffer, it, params, coroutineContext) }
    }

    fun stopSound(channel: AudioBufferSourceNode?) {
		channel?.disconnect(0)
		channel?.stop(0.0)
	}

    fun ensureUnlockStart() {
        unlock
    }

	suspend fun waitUnlocked(): BaseAudioContext? {
        if (!unlock.isCompleted) {
            logger.warn { "Waiting for key or mouse down to start sound..." }
        }
		unlock.await()
		return ctx
	}

	fun callOnUnlocked(callback: (Unit) -> Unit): Cancellable {
		var cancelled = false
		unlock.invokeOnCompletion { if (!cancelled) callback(Unit) }
		return Cancellable { cancelled = true }
	}

	suspend fun loadSound(data: ArrayBuffer, url: String): AudioBuffer? {
		if (ctx == null) return null
		return suspendCoroutine<AudioBuffer> { c ->
			ctx.decodeAudioData(
				data,
				{ data -> c.resume(data) },
				{ c.resumeWithException(Exception("error decoding $url")) }
			)
		}
	}

	fun loadSoundBuffer(url: String): HTMLAudioElement? {
		if (ctx == null) return null
		return createAudioElement(url)
	}

    /*
	suspend fun playSoundBuffer(buffer: HTMLAudioElement?) {
		if (ctx != null) {
			buffer?.audio?.play()
			buffer?.node?.connect(ctx.destination)
		}
	}

	suspend fun stopSoundBuffer(buffer: HTMLAudioElement?) {
		if (ctx != null) {
			buffer?.audio?.pause()
			buffer?.audio?.currentTime = 0.0
			buffer?.node?.disconnect(ctx.destination)
		}
	}
    */

	suspend fun loadSound(data: ByteArray): AudioBuffer? = loadSound(data.unsafeCast<Int8Array>().buffer, "ByteArray")

	suspend fun loadSound(url: String): AudioBuffer? = loadSound(url.uniVfs.readBytes())

    init {
        val _scratchBuffer = ctx?.createBuffer(1, 1, 22050)
        lateinit var unlock: (e: Event) -> Unit
        unlock = {
            // Remove the touch start listener.
            document.removeEventListener("keydown", unlock, true)
            document.removeEventListener("touchstart", unlock, true)
            document.removeEventListener("touchend", unlock, true)
            document.removeEventListener("mousedown", unlock, true)

            if (ctx != null) {
                // If already created the audio context, we try to resume it
                (window.asDynamic()).globalAudioContext.unsafeCast<BaseAudioContext?>()?.resume()

                val source = ctx.createBufferSource()

                source.buffer = _scratchBuffer
                source.connect(ctx.destination)
                source.start(0.0)
                if (jsTypeOf(ctx.asDynamic().resume) === "function") ctx.asDynamic().resume()
                source.onended = {
                    source.disconnect(0)

                    unlocked = true
                    logger.info { "Web Audio was successfully unlocked" }
                    unlockDeferred.complete(Unit)
                }
            }
        }

        document.addEventListener("keydown", unlock, true)
        document.addEventListener("touchstart", unlock, true)
        document.addEventListener("touchend", unlock, true)
        document.addEventListener("mousedown", unlock, true)
    }
}

external interface AudioParam {
	val defaultValue: Double
	val minValue: Double
	val maxValue: Double
	var value: Double
}

external interface GainNode : AudioNode {
	val gain: AudioParam
}

external interface StereoPannerNode : AudioNode {
	val pan: AudioParam
}

external interface PannerNode : AudioNode {
    fun setPosition(x: Double, y: Double, z: Double)
    fun setOrientation(x: Double, y: Double, z: Double)
}

open external class BaseAudioContext {
	fun createScriptProcessor(
		bufferSize: Int,
		numberOfInputChannels: Int,
		numberOfOutputChannels: Int
	): ScriptProcessorNode

	fun decodeAudioData(ab: ArrayBuffer, successCallback: (AudioBuffer) -> Unit, errorCallback: () -> Unit): Unit

	fun createMediaElementSource(audio: HTMLAudioElement): MediaElementAudioSourceNode
	fun createBufferSource(): AudioBufferSourceNode
	fun createGain(): GainNode
    fun createPanner(): PannerNode
	fun createStereoPanner(): StereoPannerNode
	fun createBuffer(numOfchannels: Int, length: Int, rate: Int): AudioBuffer

	var currentTime: Double
	//var listener: AudioListener
	var sampleRate: Double
	var state: String // suspended, running, closed
	val destination: AudioDestinationNode

    fun resume()
    fun suspend()
}

external class AudioContext : BaseAudioContext
external class webkitAudioContext : BaseAudioContext

external interface MediaElementAudioSourceNode : AudioScheduledSourceNode {
    val mediaElement: HTMLMediaElement
}

external interface AudioScheduledSourceNode : AudioNode {
	var onended: () -> Unit
	fun start(whn: Double = definedExternally, offset: Double = definedExternally, duration: Double = definedExternally)
	fun stop(whn: Double = definedExternally)
}

external interface AudioBufferSourceNode : AudioScheduledSourceNode {
	var buffer: AudioBuffer?
	var detune: Int
	var loop: Boolean
	var loopEnd: Double
	var loopStart: Double
	var playbackRate: Double
}

external class AudioBuffer {
	val duration: Double
	val length: Int
	val numberOfChannels: Int
	val sampleRate: Int
	fun copyFromChannel(destination: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	fun copyToChannel(source: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	//fun getChannelData(channel: Int): Float32Array
    fun getChannelData(channel: Int): FloatArray
}

external interface AudioNode {
	val channelCount: Int
	//val channelCountMode: ChannelCountMode
	//val channelInterpretation: ChannelInterpretation
	val context: AudioContext
	val numberOfInputs: Int
	val numberOfOutputs: Int
	fun connect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): AudioNode
	//fun connect(destination: AudioParam, output: Int?): Unit
	fun disconnect(output: Int? = definedExternally): Unit

	fun disconnect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): Unit
	//fun disconnect(destination: AudioParam, output: Int?): Unit
}

external interface AudioDestinationNode : AudioNode {
	val maxChannelCount: Int
}

external class AudioProcessingEvent : Event {
	val inputBuffer: AudioBuffer
	val outputBuffer: AudioBuffer
	val playbackTime: Double
}

external interface ScriptProcessorNode : AudioNode {
	var onaudioprocess: (AudioProcessingEvent) -> Unit
}
