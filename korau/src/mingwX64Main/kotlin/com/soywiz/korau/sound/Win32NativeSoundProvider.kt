package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.windows.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider = Win32NativeSoundProvider

object Win32NativeSoundProvider : NativeSoundProvider() {
    //override val audioFormats: AudioFormats = AudioFormats(WAV, NativeMp3DecoderFormat, NativeOggVorbisDecoderFormat)
    //override val audioFormats: AudioFormats = AudioFormats(WAV, NativeMp3DecoderAudioFormat, PureJavaMp3DecoderAudioFormat, NativeOggVorbisDecoderFormat)
    override val audioFormats: AudioFormats = AudioFormats(WAV, MP3Decoder, NativeOggVorbisDecoderFormat)

    class WinAudioChunk(samples: AudioSamples, val speed: Double, val panning: Double) {
        val samplesInterleaved = samples.interleaved().applyProps(speed, panning, 1.0).ensureTwoChannels()
        val samplesPin = samplesInterleaved.data.pin()
        val scope = Arena()
        val hdr = scope.alloc<WAVEHDR>().apply {
            this.lpData = samplesPin.addressOf(0).reinterpret()
            this.dwBufferLength = (samplesInterleaved.data.size * 2).convert()
            this.dwFlags = 0.convert()
        }

        val completed: Boolean get() = (hdr.dwFlags.toInt() and WHDR_DONE.toInt()) != 0
        val totalSamples get() = samplesInterleaved.totalSamples

        fun dispose() {
            samplesPin.unpin()
            scope.clear()
        }
    }

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        val nchannels = 2

        return object : PlatformAudioOutput(coroutineContext, freq) {
            val scope = Arena()
            var hWaveOut: CPointerVarOf<CPointer<HWAVEOUT__>>? = null

            private var emitedSamples: Long = 0
            private val currentSamples: Long get() {
                if (hWaveOut == null) return 0
                return memScoped {
                    val time = alloc<MMTIME>()
                    time.wType = TIME_BYTES.convert()
                    waveOutGetPosition(hWaveOut!!.value, time.ptr, MMTIME.size.convert())
                    time.u.cb.toLong() / 2 / nchannels
                }
            }

            override val availableSamples: Int get() = (emitedSamples - currentSamples).toInt()

            override var pitch: Double = 1.0
            override var volume: Double = 1.0
                set(value) = run { field = value }.also { hWaveOut?.let { waveOutSetVolume(it.value, (value.clamp01() * 0xFFFF).toInt().convert()) } }
            override var panning: Double = 0.0

            private val chunksDeque = Deque<WinAudioChunk>()

            private fun cleanup() {
                while (chunksDeque.isNotEmpty() && chunksDeque.first.completed) {
                    chunksDeque.removeFirst().dispose()
                }
            }

            override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
                //println("add.[1] $currentSamples/$emitedSamples -- $availableSamples")
                cleanup()
                //println("add.[2]")
                while (chunksDeque.size > 10) {
                    cleanup()
                    delay(1L)
                }
                //println("add.[3]")
                val chunk = WinAudioChunk(samples, pitch, panning)
                chunksDeque.add(chunk)
                val resPrepare = waveOutPrepareHeader(hWaveOut!!.value, chunk.hdr.ptr, WAVEHDR.size.convert())
                val resOut = waveOutWrite(hWaveOut!!.value, chunk.hdr.ptr, WAVEHDR.size.convert())
                //println("add.[4]")
                emitedSamples += chunk.totalSamples
            }

            override fun start() {
                val format = scope.alloc<WAVEFORMATEX>().apply {
                    this.cbSize = WAVEFORMATEX.size.convert()
                    this.wFormatTag = WAVE_FORMAT_PCM.convert()
                    this.nSamplesPerSec = freq.convert()
                    this.nChannels = nchannels.convert()
                    this.nBlockAlign = (nchannels * 2).convert()
                    this.wBitsPerSample = 16.convert()
                }
                hWaveOut = scope.alloc<HWAVEOUTVar>()
                val res = waveOutOpen(hWaveOut!!.ptr, WAVE_MAPPER, format.ptr, 0.convert(), 0.convert(), CALLBACK_NULL)
            }

            override fun stop() {
                hWaveOut?.let { waveOutReset(it.value) }
                hWaveOut?.let { waveOutClose(it.value) }
                cleanup()
                scope.clear()
            }
        }
    }
}

/*
class Win32NativeSoundNoStream(val coroutineContext: CoroutineContext, val data: AudioData?) : NativeSound() {
    override suspend fun decode(): AudioData = data ?: AudioData.DUMMY

    override fun play(params: PlaybackParameters): NativeSoundChannel {
        val data = data ?: return DummyNativeSoundChannel(this)
        val scope = Arena()
        val hWaveOut = scope.alloc<HWAVEOUTVar>()
        val samplesPin = data.samplesInterleaved.data.pin()
        val hdr = scope.alloc<WAVEHDR>().apply {
            this.lpData = samplesPin.addressOf(0).reinterpret()
            this.dwBufferLength = (data.samples.size * 2).convert()
            this.dwFlags = 0.convert()

            //this.dwBytesRecorded = 0.convert()
            //this.dwUser = 0.convert()
            //this.dwLoops = 0.convert()
            //this.lpNext = 0.convert()
        }
        memScoped {
            val format = alloc<WAVEFORMATEX>().apply {
                this.cbSize = WAVEFORMATEX.size.convert()
                this.wFormatTag = WAVE_FORMAT_PCM.convert()
                this.nSamplesPerSec =  data.rate.convert()
                this.nChannels = data.channels.convert()
                this.nBlockAlign = (data.channels * 2).convert()
                this.wBitsPerSample = 16.convert()
            }
            val res = waveOutOpen(hWaveOut.ptr, WAVE_MAPPER, format.ptr, 0.convert(), 0.convert(), CALLBACK_NULL)
            //println(res)
            val resPrepare = waveOutPrepareHeader(hWaveOut.value, hdr.ptr, WAVEHDR.size.convert())
            //println(resPrepare)
            val resOut = waveOutWrite(hWaveOut.value, hdr.ptr, WAVEHDR.size.convert())
            //println(resOut)
        }
        var stopped = false
        val channel = object : NativeSoundChannel(this) {
            override var pitch: Double = 1.0
                set(value) {
                    field = value
                    val intPart = value.toInt()
                    val divPart = field % 1.0
                    waveOutSetPitch(hWaveOut.value, ((intPart shl 16) or (divPart * 0xFFFF).toInt()).convert())
                }
            override var volume: Double = 1.0
                set(value) {
                    field = value
                    waveOutSetVolume(hWaveOut.value, (value.clamp(0.0, 1.0) * 0xFFFF).toInt().convert())
                }

            val currentSamples: Int
                get() = memScoped {
                    val time = alloc<mmtime_tag>().apply {
                        wType = TIME_SAMPLES.convert()
                    }
                    waveOutGetPosition(hWaveOut.value, time.ptr, MMTIME.size.convert())
                    time.u.sample.toInt()
                }

            override var current: TimeSpan
                get() = (currentSamples.toDouble() / data.rate).seconds
                set(value) = seekingNotSupported()
            override val total: TimeSpan get() = data.totalTime
            override val playing: Boolean
                get() = !stopped && super.playing

            override fun stop() {
                if (!stopped) {
                    //println("stop")
                    stopped = true
                    waveOutReset(hWaveOut.value)
                    val res = waveOutClose(hWaveOut.value)
                    waveOutUnprepareHeader(hWaveOut.value, hdr.ptr, WAVEHDR.size.convert())
                    //println(res)
                    scope.clear()
                    samplesPin.unpin()
                }
            }
        }.also {
            it.copySoundPropsFrom(params)
        }
        launchImmediately(coroutineContext[ContinuationInterceptor.Key] ?: coroutineContext) {
            try {
                while (channel.playing) {
                    //println("${channel.current}/${channel.total}")
                    delay(1L)
                }
            } finally {
                channel.stop()
            }
        }
        return channel
    }
}
*/
