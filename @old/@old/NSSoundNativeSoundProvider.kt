package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.AppKit.*
import platform.Foundation.*

actual val nativeSoundProvider: NativeSoundProvider = object : NativeSoundProvider() {
    override fun createAudioStream(freq: Int): NativeAudioStream {
        return MacAudioStream(freq)
    }

    override suspend fun createSound(data: ByteArray, streaming: Boolean): NativeSound {
        return NSSoundNativeSound(NSSound(data.toNSData()))
    }
}

class MacAudioStream(freq: Int) : NativeAudioStream(freq) {
    val engine = AVAudioEngine()
    val playerNode = AVAudioPlayerNode()
    val audioFormat = AVAudioFormat(44100.0, channels = 2.convert())

    init {
        engine.attachNode(playerNode)
        engine.connect(playerNode, engine.mainMixerNode, playerNode.outputFormatForBus(0))
    }

    override var availableSamples: Int = 0

    override suspend fun addSamples(samples: ShortArray, offset: Int, size: Int) {
        val buffer = AVAudioPCMBuffer(audioFormat, size.convert())
        val channelData = buffer.floatChannelData!!
        val channelLeft = channelData[0]!!
        val channelRight = channelData[1]!!
        val nsamples = size / 2
        for (n in 0 until nsamples) {
            val m = offset + n * 2
            channelLeft[n] = samples[m + 0].toFloat() / Short.MAX_VALUE.toFloat()
            channelRight[n] = samples[m + 1].toFloat() / Short.MAX_VALUE.toFloat()
        }

        availableSamples++
        playerNode.scheduleBuffer(buffer) {
            availableSamples--
        }

        while (availableSamples > 4) {
            delay(4.milliseconds)
        }
    }

    override fun start() {
        engine.startAndReturnError(null)
    }

    override fun stop() {
        engine.stop()
    }
}

class NSSoundNativeSound(val sound: NSSound) : NativeSound() {
    override fun play(): NativeSoundChannel {
        val ssound = sound.copy() as NSSound
        ssound.play()
        return object : NativeSoundChannel(this) {
            override var volume: Double
                get() = ssound.volume.toDouble()
                set(value) { ssound.volume = value.toFloat() }
            override val current: TimeSpan get() = ssound.currentTime.seconds

            override val total: TimeSpan get() = ssound.duration.seconds
            override val playing: Boolean get() = ssound.playing

            override fun stop() {
                ssound.stop()
            }
        }
    }
}

private fun ByteArray.toNSData(): NSData {
    val data = this
    return data.usePinned { dataPin ->
        NSData.dataWithBytes(dataPin.addressOf(0), data.size.convert())
    }
}
