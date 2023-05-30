---
layout: default
title: "Audio"
title_prefix: KorGE
fa-icon: fa-headphones
priority: 36
---

<img src="/i/logos/korau.png" style="float:left;" />

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
