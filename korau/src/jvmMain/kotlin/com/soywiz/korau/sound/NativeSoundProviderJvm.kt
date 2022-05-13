package com.soywiz.korau.sound

import com.soywiz.klogger.Console
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.OGG
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.mp3.MP3Decoder
import com.soywiz.korau.sound.impl.jna.JnaOpenALNativeSoundProvider
import com.soywiz.korau.sound.impl.jna.OpenALException
import com.soywiz.korio.time.traceTime
import java.util.*

internal val nativeAudioFormats = AudioFormats(WAV, MP3Decoder, OGG) + AudioFormats(try {
        ServiceLoader.load(AudioFormat::class.java).toList()
    } catch (e: Throwable) {
        e.printStackTrace()
        listOf<AudioFormat>()
    })

private val dummyNativeSoundProvider by lazy { DummyNativeSoundProvider(nativeAudioFormats) }

private val nativeSoundProviderDeferred by lazy {
    try {
        traceTime("OpenAL") {
            JnaOpenALNativeSoundProvider()
        }
    } catch (e: UnsatisfiedLinkError) {
        dummyNativeSoundProvider
    } catch (e: OpenALException) {
        Console.error("OpenALException", e.message)
        dummyNativeSoundProvider
    } catch (e: Throwable) {
        e.printStackTrace()
        dummyNativeSoundProvider
    }
}

actual val nativeSoundProvider: NativeSoundProvider = LazyNativeSoundProvider(prepareInit = {
    //println("nativeSoundProvider")
    Thread { nativeSoundProviderDeferred }.apply { isDaemon = true }.start()
}) {
    nativeSoundProviderDeferred
}
//actual val nativeSoundProvider: NativeSoundProvider by lazy { JogampNativeSoundProvider() }
//actual val nativeSoundProvider: NativeSoundProvider by lazy { AwtNativeSoundProvider() }
