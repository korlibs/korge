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

object Win32NativeSoundProvider : NativeSoundProvider(), Disposable {
    //override val audioFormats: AudioFormats = AudioFormats(WAV, NativeMp3DecoderFormat, NativeOggVorbisDecoderFormat)
    //override val audioFormats: AudioFormats = AudioFormats(WAV, NativeMp3DecoderAudioFormat, PureJavaMp3DecoderAudioFormat, NativeOggVorbisDecoderFormat)
    override val audioFormats: AudioFormats = AudioFormats(WAV, MP3Decoder, NativeOggVorbisDecoderFormat)

    val workerPool get() = Win32NativeSoundProvider_workerPool

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        Win32PlatformAudioOutput(Win32NativeSoundProvider, coroutineContext, freq)

    override fun dispose() {
        while (workerPool.itemsInPool > 0) {
            workerPool.alloc().requestTermination()
        }
    }
}

class Win32PlatformAudioOutput(
    val provider: Win32NativeSoundProvider,
    coroutineContext: CoroutineContext,
    val freq: Int
) : PlatformAudioOutput(coroutineContext, freq) {
    private var process: WaveOutProcess = WaveOutProcess(freq, nchannels = 2)

    override val availableSamples: Int get() = (process.length - process.position).toInt()
        //.also { println("Win32PlatformAudioOutput.availableSamples. length=${process.length}, position=${process.position}, value=$it") }

    override var pitch: Double = 1.0
    override var volume: Double = 1.0
    override var panning: Double = 0.0

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        // More than 1 second queued, let's wait a bit
        if (availableSamples > freq) {
            delay(200.milliseconds)
        }

        // @TODO: All this things could be done at worker level
        process.addData(samples.copyOfRange(offset, offset + size).interleaved().applyProps(pitch, panning, volume).ensureTwoChannels().data)
        //process.addData(samples.data[0].copyOfRange(offset, offset + size))
    }

    var worker: Worker? = null

    override fun start() {
        if (worker != null) return
        worker = provider.workerPool.alloc()
        //println("Win32PlatformAudioOutput.START WORKER: $worker")
        process.start(worker!!)
    }

    override suspend fun wait() {
        while (!process.isCompleted) {
            delay(10.milliseconds)
            //println("WAITING...: process.isCompleted=${process.isCompleted}")
        }
    }

    override fun stop() {
        //println("Win32PlatformAudioOutput.STOP WORKER: $worker")
        val worker = this.worker ?: return
        process.stop()
        launchImmediately(coroutineContext) {
            try {
                wait()
            } catch (e: CancellationException) {
                // Do nothing
            } catch (e: Throwable) {
                Console.error("Error in Win32PlatformAudioOutput.stop:")
                e.printStackTrace()
            } finally {
                provider.workerPool.free(worker)
            }
        }
    }
}

