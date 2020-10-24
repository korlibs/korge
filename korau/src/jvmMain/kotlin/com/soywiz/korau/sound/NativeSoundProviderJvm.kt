package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import com.soywiz.korau.sound.impl.jna.*
import java.util.*

internal val nativeAudioFormats = AudioFormats(WAV, MP3Decoder, OGG) + AudioFormats(try {
        ServiceLoader.load(AudioFormat::class.java).toList()
    } catch (e: Throwable) {
        e.printStackTrace()
        listOf<AudioFormat>()
    })

actual val nativeSoundProvider: NativeSoundProvider by lazy { JnaOpenALNativeSoundProvider() }
//actual val nativeSoundProvider: NativeSoundProvider by lazy { JogampNativeSoundProvider() }
//actual val nativeSoundProvider: NativeSoundProvider by lazy { AwtNativeSoundProvider() }
