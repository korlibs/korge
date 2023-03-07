package com.soywiz.korau.sound

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korau.sound.backends.*
import com.soywiz.korau.sound.impl.jna.*
import com.soywiz.korio.time.*

private val logger = Logger("NativeSoundProviderJvm")

private val dummyNativeSoundProvider by lazy { DummyNativeSoundProvider() }

private val nativeSoundProviderDeferred: NativeSoundProvider by lazy {
    try {
        traceTime("SoundProvider") {
            when {
                Platform.isLinux -> alsaNativeSoundProvider
                Platform.isApple -> jvmCoreAudioNativeSoundProvider
                Platform.isWindows -> jvmWaveOutNativeSoundProvider
                else -> JnaOpenALNativeSoundProvider()
            } ?: dummyNativeSoundProvider
        }
    } catch (e: UnsatisfiedLinkError) {
        dummyNativeSoundProvider
    } catch (e: OpenALException) {
        logger.error { "OpenALException: ${e.message}" }
        dummyNativeSoundProvider
    } catch (e: Throwable) {
        e.printStackTrace()
        dummyNativeSoundProvider
    }
}

actual val nativeSoundProvider: NativeSoundProvider by lazy {
    Thread { nativeSoundProviderDeferred }.apply { isDaemon = true }.start()
    LazyNativeSoundProvider { nativeSoundProviderDeferred }
}
//actual val nativeSoundProvider: NativeSoundProvider by lazy { JogampNativeSoundProvider() }
//actual val nativeSoundProvider: NativeSoundProvider by lazy { AwtNativeSoundProvider() }
