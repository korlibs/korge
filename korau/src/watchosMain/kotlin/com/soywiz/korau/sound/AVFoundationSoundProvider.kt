package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider get() = AVFOUNDATION_NATIVE_SOUND_PROVIDER
val AVFOUNDATION_NATIVE_SOUND_PROVIDER: AVFoundationNativeSoundProvider by lazy { AVFoundationNativeSoundProvider() }

class AVFoundationNativeSoundProvider : NativeSoundProvider() {
    init {
        //AVAudioSession()
    }

    override val audioFormats: AudioFormats = AudioFormats(WAV, com.soywiz.korau.format.mp3.MP3Decoder, NativeOggVorbisDecoderFormat)

    // @TODO: DUMMY! We should use AVFoundation here? But it doesn't seem to work: https://github.com/JetBrains/kotlin-native/issues/3984
    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = PlatformAudioOutput(coroutineContext, freq)
}
