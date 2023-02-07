package com.soywiz.korau.sound.backends

import com.soywiz.kds.thread.*
import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korau.sound.*
import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Memory
import kotlin.coroutines.*

val jvmWaveOutNativeSoundProvider: NativeSoundProvider? by lazy {
    JvmWaveOutNativeSoundProvider()
}

class JvmWaveOutNativeSoundProvider : NativeSoundProvider() {
    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        JvmWaveOutPlatformAudioOutput(this, coroutineContext, freq)
}

class JvmWaveOutPlatformAudioOutput(
    val provider: JvmWaveOutNativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    var nativeThread: NativeThread? = null
    var running = false

    override fun start() {
        running = true
        nativeThread = NativeThread {
            val handlePtr = Memory(8L)
            val freq = frequency
            val blockAlign = (nchannels * Short.SIZE_BYTES)
            val format = WAVEFORMATEX(Memory(WAVEFORMATEX().size.toLong())).also { format ->
                format.wFormatTag = WINMM.WAVE_FORMAT_PCM.toShort()
                format.nChannels = nchannels.toShort() // 2?
                format.nSamplesPerSec = freq.toInt()
                format.wBitsPerSample = Short.SIZE_BITS.toShort() // 16
                format.nBlockAlign = ((nchannels * Short.SIZE_BYTES).toShort())
                format.nAvgBytesPerSec = freq * blockAlign
                format.cbSize = format.size.toShort()
            }
            WINMM.waveOutOpen(handlePtr, WINMM.WAVE_MAPPER, format.pointer, null, null, 0).also {
                if (it != 0) println("WINMM.waveOutOpen: $it")
            }
            val handle = handlePtr.getPointer(0L)
            //println("handle=$handle")

            val headers = Array(3) { WaveHeader(it, handle, 1024, nchannels) }

            try {
                while (true) {
                    for (header in headers) {
                        if (!header.hdr.isInQueue) {
                            //println("Sending availableRead=$availableRead, header=${header}")
                            readShorts(header.data)
                            header.prepareAndWrite()
                        }
                        Thread.sleep(1L)
                    }
                }
            } finally {
                for (header in headers) {
                    while (!header.hdr.isInQueue) Thread.sleep(10L)
                    header.dispose()
                }
                WINMM.waveOutReset(handle)
                WINMM.waveOutClose(handle)
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
        super.start()
    }

    override fun stop() {
        running = false
        super.stop()
    }

    private class WaveHeader(
        val id: Int,
        val handle: Pointer?,
        val totalSamples: Int,
        val nchannels: Int
    ) {
        val data = Array(nchannels) { ShortArray(totalSamples) }
        val totalBytes = (totalSamples * nchannels * Short.SIZE_BYTES)
        val dataMem = Memory(totalBytes.toLong())
        val hdr = WAVEHDR(Memory(WAVEHDR().size.toLong())).also { hdr ->
            hdr.lpData = KPointerT(dataMem)
            hdr.dwBufferLength = totalBytes
            hdr.dwFlags = 0
        }

        fun prepareAndWrite() {
            //println(data[0].toList())

            for (ch in 0 until nchannels) {
                for (n in 0 until totalSamples) {
                    dataMem.setShort(((n * nchannels + ch) * Short.SIZE_BYTES), data[ch][n])
                }
            }
            //if (hdr.isPrepared) dispose()
            if (!hdr.isPrepared) {
                //println("-> prepare")
                WINMM.waveOutPrepareHeader(handle, hdr.pointer, hdr.size)
            }
            WINMM.waveOutWrite(handle, hdr.pointer, hdr.size)
        }

        fun dispose() {
            WINMM.waveOutUnprepareHeader(handle, hdr.pointer, hdr.size)
        }

        override fun toString(): String = "WaveHeader(id=$id, totalSamples=$totalSamples, nchannels=$nchannels, hdr=$hdr)"
    }
}

internal typealias LPHWAVEOUT = Pointer
internal typealias HWAVEOUT = Pointer
internal typealias LPCWAVEFORMATEX = Pointer
internal typealias LPWAVEHDR = Pointer

internal class WAVEHDR(pointer: KPointer? = null) : KStructure(pointer) {
    var lpData by pointer<Byte>()
    var dwBufferLength by int()
    var dwBytesRecorded by int()
    var dwUser by pointer<Byte>()
    var dwFlags by int()
    var dwLoops by int()
    var lpNext by pointer<Byte>()
    var reserved by pointer<Byte>()

    val isDone: Boolean get() = dwFlags.hasFlags(WINMM.WHDR_DONE)
    val isPrepared: Boolean get() = dwFlags.hasFlags(WINMM.WHDR_PREPARED)
    val isBeginLoop: Boolean get() = dwFlags.hasFlags(WINMM.WHDR_BEGINLOOP)
    val isEndLoop: Boolean get() = dwFlags.hasFlags(WINMM.WHDR_ENDLOOP)
    val isInQueue: Boolean get() = dwFlags.hasFlags(WINMM.WHDR_INQUEUE)

    override fun toString(): String = "WAVEHDR(dwBufferLength=$dwBufferLength, isDone=$isDone, isPrepared=$isPrepared, isInQueue=$isInQueue, flags=$dwFlags)"
}

internal class WAVEFORMATEX(pointer: KPointer? = null) : KStructure(pointer) {
    var wFormatTag by short()
    var nChannels by short()
    var nSamplesPerSec by int()
    var nAvgBytesPerSec by int()
    var nBlockAlign by short()
    var wBitsPerSample by short()
    var cbSize by short()
}

internal object WINMM {
    @JvmStatic external fun waveOutOpen(phwo: LPHWAVEOUT?, uDeviceID: Int, pwfx: LPCWAVEFORMATEX?, dwCallback: Callback?, dwInstance: Pointer?, fdwOpen: Int): Int
    @JvmStatic external fun waveOutClose(hwo: HWAVEOUT?): Int
    @JvmStatic external fun waveOutReset(hwo: HWAVEOUT?): Int
    @JvmStatic external fun waveOutPrepareHeader(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int
    @JvmStatic external fun waveOutWrite(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int
    @JvmStatic external fun waveOutUnprepareHeader(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int

    const val WAVE_MAPPER = -1
    const val WAVE_FORMAT_PCM = 1

    const val WHDR_DONE = 0x00000001
    const val WHDR_PREPARED = 0x00000002
    const val WHDR_BEGINLOOP = 0x00000004
    const val WHDR_ENDLOOP = 0x00000008
    const val WHDR_INQUEUE = 0x00000010

    init {
        Native.register("winmm.dll")
    }
}
