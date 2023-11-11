---
permalink: /audio/
group: reference
layout: default
title: "Audio"
fa-icon: fa-headphones
priority: 70
artifact: 'com.soywiz.korge:korge-core'
package: korlibs.audio
---

<img alt="KorAU" src="/i/logos/korau.svg" width="128" height="128" style="float: left;margin-right:1em;" />

KorAU, Kotlin cORoutines AUdio - Audio playing, and sound file decoding

It supports playing sounds, creating dynamic audio streams and decoding audio file formats: wav, mp3 and ogg.

## AudioFormat

```kotlin
suspend fun VfsFile.readSoundInfo(formats: AudioFormats = defaultAudioFormats): AudioFormat.Info
```

```kotlin
// Just Sound Information by default
object MP3 : AudioFormat("mp3")
object OGG : AudioFormat("ogg")

// Decoding and Encoding
object WAV : AudioFormat("wav")

open class AudioFormat(vararg exts: String) {
	val extensions: Set<String>

	data class Info(var duration: TimeSpan = 0.seconds, var channels: Int = 2) : Extra by Extra.Mixin()

	open suspend fun tryReadInfo(data: AsyncStream): Info?
	open suspend fun decodeStream(data: AsyncStream): AudioStream?
	suspend fun decode(data: AsyncStream): AudioData?
	suspend fun decode(data: ByteArray): AudioData?
	open suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String): Unit

	suspend fun encodeToByteArray(data: AudioData, filename: String = "out.wav", format: AudioFormat = this): ByteArray
}

open class InvalidAudioFormatException(message: String) : RuntimeException(message)
fun invalidAudioFormat(message: String = "invalid audio format"): Nothing

val defaultAudioFormats = AudioFormats().apply { registerStandard() }

class AudioFormats : AudioFormat() {
	val formats = linkedSetOf<AudioFormat>()
	fun register(vararg formats: AudioFormat): AudioFormats = this.apply { this.formats += formats }
	fun register(formats: Iterable<AudioFormat>): AudioFormats = this.apply { this.formats += formats }
}

fun AudioFormats.registerStandard(): AudioFormats = this.apply { register(WAV, OGG, MP3) }
```

## AudioData

```kotlin
class AudioData(val rate: Int, val samples: AudioSamples) {
    companion object {
        val DUMMY: AudioData = AudioData(44100, AudioSamples(2, 0))
    }

    val samplesInterleaved: AudioSamplesInterleaved by lazy { samples.interleaved() }

    val channels: Int
    val totalSamples: Int
    val totalTime: TimeSpan
    fun timeAtSample(sample: Int): TimeSpan

    operator fun get(channel: Int): ShortArray
    operator fun get(channel: Int, sample: Int): Short
    operator fun set(channel: Int, sample: Int, value: Short): Unit
}

enum class AudioConversionQuality { FAST }
fun AudioData.withRate(rate: Int): AudioData
fun AudioData.toStream(): AudioStream = object : AudioStream(rate, channels) {
    var cursor = 0
}

suspend fun AudioData.toNativeSound(): NativeSound
suspend fun AudioData.playAndWait(): Unit
suspend fun VfsFile.readAudioData(formats: AudioFormats = defaultAudioFormats): AudioData
```

## AudioSamples

```kotlin
interface IAudioSamples {
    val channels: Int
    val totalSamples: Int
    val size get() = totalSamples
    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0
    operator fun get(channel: Int, sample: Int): Short
    operator fun set(channel: Int, sample: Int, value: Short): Unit
    fun getFloat(channel: Int, sample: Int): Float
    fun setFloat(channel: Int, sample: Int, value: Float)
}

class AudioSamples(override val channels: Int, override val totalSamples: Int) : IAudioSamples {
    val data = Array(channels) { ShortArray(totalSamples) }
    operator fun get(channel: Int): ShortArray = data[channel]
}

class AudioSamplesInterleaved(override val channels: Int, override val totalSamples: Int) : IAudioSamples {
    val data = ShortArray(totalSamples * channels)
}

fun AudioSamples.copyOfRange(start: Int, end: Int): AudioSamples
fun IAudioSamples.interleaved(out: AudioSamplesInterleaved = AudioSamplesInterleaved(channels, totalSamples)): AudioSamplesInterleaved
fun IAudioSamples.separated(out: AudioSamples = AudioSamples(channels, totalSamples)): AudioSamples
```

## AudioSamplesDeque

```kotlin
class AudioSamplesDeque(val channels: Int) {
    val buffer: Array<ShortArrayDeque>
    val availableRead: Int
    val availableReadMax: Int

    fun read(channel: Int): Short
    fun write(channel: Int, sample: Short)   
    fun write(samples: AudioSamples, offset: Int = 0, len: Int = samples.size - offset)
    fun write(samples: AudioSamplesInterleaved, offset: Int = 0, len: Int = samples.size - offset)
    fun write(samples: IAudioSamples, offset: Int = 0, len: Int = samples.size - offset)
    fun write(channel: Int, data: ShortArray, offset: Int = 0, len: Int = data.size - offset)
    fun write(channel: Int, data: FloatArray, offset: Int = 0, len: Int = data.size - offset)
    fun writeInterleaved(data: ShortArray, offset: Int, len: Int = data.size - offset, channels: Int
    fun read(out: AudioSamples, offset: Int = 0, len: Int = out.totalSamples - offset): Int
    fun read(out: AudioSamplesInterleaved, offset: Int = 0, len: Int = out.totalSamples - offset): Int
    fun read(out: IAudioSamples, offset: Int = 0, len: Int = out.totalSamples - offset): Int
}
```

## AudioStream

```kotlin
open class AudioStream(val rate: Int, val channels: Int) : Closeable {
    open val finished: Boolean
    val totalLengthInSamples: Long?
    val totalLength: TimeSpan
    open suspend fun read(out: AudioSamples, offset: Int, length: Int): Int = 0

    companion object {
        fun generator(rate: Int, channels: Int, generateChunk: suspend AudioSamplesDeque.(step: Int) -> Boolean): AudioStream
    }
}

suspend fun AudioStream.toData(maxSamples: Int = Int.MAX_VALUE): AudioData
suspend fun AudioStream.playAndWait(bufferSeconds: Double = 0.1)
suspend fun VfsFile.readAudioStream(formats: AudioFormats = defaultAudioFormats)
suspend fun VfsFile.writeAudio(data: AudioData, formats: AudioFormats = defaultAudioFormats)
```

## AudioTone

```kotlin
object AudioTone {
    fun generate(length: TimeSpan, freq: Double, rate: Int = 44100): AudioData
}
```

## NativeSound

```kotlin
expect val nativeSoundProvider: NativeSoundProvider

open class NativeSoundProvider {
	open val target: String = "unknown"
	open fun initOnce()
    open fun createAudioStream(freq: Int = 44100): PlatformAudioOutput
	protected open fun init(): Unit
	open suspend fun createSound(data: ByteArray, streaming: Boolean = false): NativeSound
	open suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean = false): NativeSound
	suspend fun createSound(file: FinalVfsFile, streaming: Boolean = false): NativeSound
	suspend fun createSound(file: VfsFile, streaming: Boolean = false): NativeSound
	open suspend fun createSound(data: AudioData, formats: AudioFormats = defaultAudioFormats, streaming: Boolean = false): NativeSound
	suspend fun playAndWait(stream: AudioStream, bufferSeconds: Double = 0.1)
}

abstract class NativeSoundChannel(val sound: NativeSound) {
	open var volume: Double
	open var pitch: Double
	open val current: TimeSpan
	open val total: TimeSpan
	open val playing: Boolean
	abstract fun stop()
}

suspend fun NativeSoundChannel.await(progress: NativeSoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> })

abstract class NativeSound {
	open val length: TimeSpan = 0.seconds
	abstract suspend fun decode(): AudioData
	abstract fun play(): NativeSoundChannel
}

suspend fun NativeSound.toData(): AudioData
suspend fun NativeSound.toStream(): AudioStream

suspend fun NativeSound.playAndWait(progress: NativeSoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }): Unit

suspend fun VfsFile.readNativeSound(streaming: Boolean = false): NativeSound
suspend fun VfsFile.readNativeSoundOptimized(streaming: Boolean = false): NativeSound
```

## SoundUtils

```kotlin
object SoundUtils {
	fun convertS16ToF32(channels: Int, input: ShortArray, leftVolume: Int, rightVolume: Int): FloatArray
```

## PlatformAudioOutput

```kotlin
open class PlatformAudioOutput(freq: Int) {
	open val availableSamples: Int = 0
	open suspend fun add(samples: AudioSamples, offset: Int = 0, size: Int = samples.totalSamples) = Unit
	suspend fun add(data: AudioData) = add(data.samples, 0, data.totalSamples)
	open fun start() = Unit
	open fun stop() = Unit
}
```



<img src="/i/logos/korau.avif" style="float:left;" />

Korge uses **[KorAU](https://github.com/soywiz/korau)** for audio loading,
and playback.

This library is able to load MP3, OGG and WAV files in a portable way,
and it is capable of using native audio loaders.

<div style="clear: both;"></div>

## Loading sounds and music files

For games, the recommended way of loading sounds and music files is to use the `NativeSound` facility.
Typically the `VfsFile.readSound` and `VfsFile.readMusic()` methods:

```kotlin
val sound = resourcesVfs["sound.mp3"].readSound()
val music = resourcesVfs["music.mp3"].readMusic()
```

The difference between Sound and Music is that a `Sound` is fully decoded and stored at memory, while
the `Music` file is being streamed and decoded on the fly while playing.

## Playing sounds

To play a sound, you just have to call the play method:

```kotlin
val channel = sound.play()
val channel = sound.play(3.times)
val channel = sound.play(infiniteTimes)
```

It returns a channel, that you can wait for:

```kotlin
channel.await()
```

Optionally you listen to a callback that executes several times during the play:

```kotlin
channel.await { current: TimeSpan, total: TimeSpan -> // this: NativeSoundChannel
    println("$current/$total")
}
```

Full API:

```kotlin
class NativeSound {
	val length: TimeSpan
	suspend fun decode(): AudioData // Not supported on Android yet
	fun play(): NativeSoundChannel
}

class NativeSoundChannel {
    // Mutable
	var volume: Double
	var pitch: Double // Only implemented on OpenAL for now

    val sound: NativeSound
	val current: TimeSpan
	val total: TimeSpan
	val playing: Boolean
	fun stop(): Unit
    suspend fun await(progress: NativeSoundChannel.(current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> })
}
```

## Audio Streams

KorAU also supports dynamic audio generation:

```kotlin
// You have first to create a PlatformAudioOutput:
val audioOutput: PlatformAudioOutput = nativeSoundProvider.createAudioStream(freq = 44100)
audioOutput.start()

while (true) {
    // Then you have to add samples. This function suspends, and resumes when it needs more data so it can play the data continuously. 
    // Here you can do raw manipulation, DSP, manual effects and other stuff.
    audioOutput.add(...)
}

audioOutput.start(stop)
```

The `PlatformAudioOutput` API:

```kotlin
class PlatformAudioOutput {
	val availableSamples: Int
	suspend fun add(samples: AudioSamples, offset: Int = 0, size: Int = samples.totalSamples)
	suspend fun add(data: AudioData)
}
```


## Platform Format support and considerations

All the targets support `WAV` and `MP3` files by default.

### JS

JavaScript uses the WebAudio API. Almost all the browsers limit this API after a first interaction.
So you won't be able to play sounds before the user clicks or taps.

The most widely format supported in the browsers via JavaScript is MP3. You can check the compatibility tables here:

* <https://caniuse.com/#feat=mp3>
* <https://caniuse.com/#feat=vorbis>
* <https://caniuse.com/#feat=opus>

### Native (Desktop and iOS)

Native targets use OpenAL as audio backend except for Windows that uses the standard waveout API.

On native platforms there is WAV, OGG and MP3 support out of the box. Using public these domain libraries:

* MP3: [minimp3](https://github.com/lieff/minimp3)
* OGG Vorbis: [stb_vorbis](https://github.com/nothings/stb/blob/master/stb_vorbis.c)

On linux you need to install the OpenAL development libraries:
```shell script
sudo apt-get install -y libopenal-dev
```
{:.note}

### Android

On Android KorAU uses the Android's MediaPlayer:

* List of supported formats: <https://developer.android.com/guide/topics/media/media-formats>

### JVM

On the JVM KorAU uses `javax.sound.sampled` APIs.

This API supports WAV and MP3 files by default.
