package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

actual val nativeSoundProvider: NativeSoundProvider = Win32NativeSoundProvider

@ThreadLocal
private val Win32NativeSoundProvider_workerPool = Pool<Worker> {
    Worker.start(name = "Win32NativeSoundProvider$it")
}

@ThreadLocal
private val Win32NativeSoundProvider_WaveOutProcess = Pool<WaveOutProcess> {
    WaveOutProcess(44100, 2).start(Win32NativeSoundProvider_workerPool.alloc())
}


object Win32NativeSoundProvider : NativeSoundProvider(), Disposable {
    override val audioFormats: AudioFormats = AudioFormats(WAV, *knNativeAudioFormats.toTypedArray(), com.soywiz.korau.format.mp3.MP3Decoder)

    //val workerPool get() = Win32NativeSoundProvider_workerPool
    val workerPool get() = Win32NativeSoundProvider_WaveOutProcess

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        Win32PlatformAudioOutput(Win32NativeSoundProvider, coroutineContext, freq)

    override fun dispose() {
        while (Win32NativeSoundProvider_workerPool.itemsInPool > 0) {
            Win32NativeSoundProvider_workerPool.alloc().requestTermination()
        }
    }
}

class Win32PlatformAudioOutput(
    val provider: Win32NativeSoundProvider,
    coroutineContext: CoroutineContext,
    val freq: Int
) : PlatformAudioOutput(coroutineContext, freq) {
    private var process: WaveOutProcess? = null

    override val availableSamples: Int get() = if (process != null) (process!!.length - process!!.position).toInt() else 0
        //.also { println("Win32PlatformAudioOutput.availableSamples. length=${process.length}, position=${process.position}, value=$it") }

    override var pitch: Double = 1.0
        set(value) {
            field = value
            process?.pitch?.value = value
        }
    override var volume: Double = 1.0
        set(value) {
            field = value
            process?.volume?.value = value
        }
    override var panning: Double = 0.0
        set(value) {
            field = value
            process?.panning?.value = value
        }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        // More than 1 second queued, let's wait a bit
        if (process == null || availableSamples > freq) {
            delay(200.milliseconds)
        }

        process!!.addData(samples, offset, size, freq)
    }

    override fun start() {
        process = provider.workerPool.alloc()
            .also { it.reopen(freq) }
        process!!.volume.value = volume
        process!!.pitch.value = pitch
        process!!.panning.value = panning
        //println("Win32PlatformAudioOutput.START WORKER: $worker")
    }

    override suspend fun wait() {
        //while (!process.isCompleted) {
        while (availableSamples > 0) {
        //while (process?.pendingAudio == true) {
            delay(10.milliseconds)
            //println("WAITING...: process.isCompleted=${process.isCompleted}")
        }
    }

    override fun stop() {
        //println("Win32PlatformAudioOutput.STOP WORKER: $worker")
        //process.stop()
        val process = this.process
        this.process = null
        if (process != null) {
            launchImmediately(coroutineContext) {
                try {
                    wait()
                } catch (e: CancellationException) {
                    // Do nothing
                } catch (e: Throwable) {
                    Console.error("Error in Win32PlatformAudioOutput.stop:")
                    e.printStackTrace()
                } finally {
                    provider.workerPool.free(process)
                }
            }
        }
    }
}

