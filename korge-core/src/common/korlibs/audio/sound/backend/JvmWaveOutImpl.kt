package korlibs.audio.sound.backend

import korlibs.audio.sound.*
import korlibs.datastructure.thread.*
import korlibs.ffi.*
import korlibs.io.lang.*
import korlibs.memory.*
import kotlin.coroutines.*

val jvmWaveOutNativeSoundProvider: NativeSoundProvider? by lazy {
    JvmWaveOutNativeSoundProvider()
}

class JvmWaveOutNativeSoundProvider : NativeSoundProviderNew() {
    override fun createNewPlatformAudioOutput(
        coroutineContext: CoroutineContext,
        channels: Int,
        frequency: Int,
        gen: (AudioSamplesInterleaved) -> Unit
    ): NewPlatformAudioOutput = JvmWaveOutNewPlatformAudioOutput(coroutineContext, channels, frequency, gen)
}

/*
class JvmWaveOutPlatformAudioOutputNew(
    val provider: JvmWaveOutNativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int
) : ThreadBasedPlatformAudioOutput(coroutineContext, frequency) {
    override val samplesPendingToPlay: Long get() = TODO("Not yet implemented")
    override val chunkSize: Int get() = 1024

    private var handle: Long = 0L
    private var headers = emptyArray<JvmWaveOutPlatformAudioOutput.WaveHeader>()
    override fun open(frequency: Int, channels: Int) {
        val handlePtr = Memory(8L)
        val freq = frequency
        val blockAlign = (nchannels * Short.SIZE_BYTES)
        val format = WAVEFORMATEX(Memory(WAVEFORMATEX().size.toLong()).also { it.clear() }.kpointer).also { format ->
            format.wFormatTag = WINMM.WAVE_FORMAT_PCM.toShort()
            format.nChannels = nchannels.toShort() // 2?
            format.nSamplesPerSec = freq.toInt()
            format.wBitsPerSample = Short.SIZE_BITS.toShort() // 16
            format.nBlockAlign = ((nchannels * Short.SIZE_BYTES).toShort())
            format.nAvgBytesPerSec = freq * blockAlign
            format.cbSize = format.size.toShort()
        }
        WINMM.waveOutOpen(handlePtr, WINMM.WAVE_MAPPER, format.pointer?.ptr, null, null, 0).also {
            if (it != 0) println("WINMM.waveOutOpen: $it")
        }
        handle = handlePtr.getPointer(0L)
        //println("handle=$handle")

        headers = Array(3) { JvmWaveOutPlatformAudioOutput.WaveHeader(it, handle, chunkSize, nchannels) }
    }

    override fun write(data: AudioSamples, offset: Int, count: Int): Int {
        header.prepareAndWrite()
    }

    override fun close() {
        for (header in headers) {
            while (!header.hdr.isInQueue) Thread.sleep(10L)
            header.dispose()
        }
        WINMM.waveOutReset(handle)
        WINMM.waveOutClose(handle)

    }
}
*/

class JvmWaveOutNewPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    nchannels: Int,
    freq: Int,
    gen: (AudioSamplesInterleaved) -> Unit
) : NewPlatformAudioOutput(coroutineContext, nchannels, freq, gen) {
    val samplesLock = korlibs.datastructure.lock.NonRecursiveLock()
    var nativeThread: NativeThread? = null
    var running = false
    var totalEmittedSamples = 0L

    //override suspend fun wait() {
    //    // @TODO: Get samples not reproduced
    //    //println("WAITING...")
    //    for (n in 0 until 1000) {
    //        var currentPositionInSamples: Long = 0L
    //        var totalEmittedSamples: Long = 0L
    //        var availableRead = 0
    //        samplesLock {
    //            currentPositionInSamples = WINMM.waveOutGetPositionInSamples(handle)
    //            availableRead = this.availableRead
    //            totalEmittedSamples = this.totalEmittedSamples
    //        }
    //        //println("availableRead=$availableRead, waveOutGetPosition=$currentPositionInSamples, totalEmittedSamples=$totalEmittedSamples")
    //        if (availableRead <= 0 && currentPositionInSamples >= totalEmittedSamples) break
    //        delay(1.milliseconds)
    //    }
    //    //println("DONE WAITING")
    //}

    private var handle: FFIPointer? = null
    private var headers = emptyArray<WaveHeader>()

    override fun internalStart() {
        //println("TRYING TO START")
        if (running) return
        //println("STARTED")
        running = true
        nativeThread = korlibs.datastructure.thread.NativeThread {
            ffiScoped {
                val arena = this
                val handlePtr = allocBytes(8).typed<FFIPointer?>()
                val freq = frequency
                val blockAlign = (channels * Short.SIZE_BYTES)
                val format = WAVEFORMATEX(allocBytes(WAVEFORMATEX().size)).also { format ->
                    format.wFormatTag = WINMM.WAVE_FORMAT_PCM.toShort()
                    format.nChannels = channels.toShort() // 2?
                    format.nSamplesPerSec = freq.toInt()
                    format.wBitsPerSample = Short.SIZE_BITS.toShort() // 16
                    format.nBlockAlign = ((channels * Short.SIZE_BYTES).toShort())
                    format.nAvgBytesPerSec = freq * blockAlign
                    format.cbSize = format.size.toShort()
                }
                WINMM.waveOutOpen(handlePtr.pointer, WINMM.WAVE_MAPPER, format.ptr, null, null, 0).also {
                    if (it != 0) println("WINMM.waveOutOpen: $it")
                }
                handle = handlePtr[0]
                //println("handle=$handle")

                headers = Array(4) { WaveHeader(it, handle, 1024, channels, arena) }

                try {
                    while (running) {
                        var queued = 0
                        for (header in headers) {
                            if (!header.hdr.isInQueue) {
                                genSafe(header.samples)
                                header.prepareAndWrite()
                                queued++
                                //println("Sending running=$running, availableRead=$availableRead, header=${header}")
                            }
                        }
                        if (queued == 0) Thread_sleep(1L)
                    }
                } finally {
                    for (header in headers) header.dispose()
                    //runBlockingNoJs {
                    //    wait()
                    //}
                    WINMM.waveOutReset(handle)
                    WINMM.waveOutClose(handle)
                    handle = null
                    //println("CLOSED")
                }
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun internalStop() {
        running = false
        //println("STOPPING")
    }
}


private class WaveHeader(
    val id: Int,
    val handle: FFIPointer?,
    val totalSamples: Int,
    val channels: Int,
    val arena: FFIArena,
) {
    val samples = AudioSamplesInterleaved(channels, totalSamples)

    val totalBytes = (totalSamples * channels * Short.SIZE_BYTES)
    val dataMem = arena.allocBytes(totalBytes).typed<Short>()
    val hdr = WAVEHDR(arena.allocBytes(WAVEHDR().size)).also { hdr ->
        hdr.lpData = dataMem.reinterpret()
        hdr.dwBufferLength = totalBytes
        hdr.dwFlags = 0
    }

    fun prepareAndWrite(totalSamples: Int = this.totalSamples) {
        //println(data[0].toList())

        val channels = this.channels
        hdr.dwBufferLength = (totalSamples * channels * Short.SIZE_BYTES)

        val samplesData = samples.data
        for (n in 0 until channels * totalSamples) {
            dataMem[n] = samplesData[n]
        }
        //if (hdr.isPrepared) dispose()
        if (!hdr.isPrepared) {
            //println("-> prepare")
            WINMM.waveOutPrepareHeader(handle, hdr.ptr, hdr.size)
        }
        WINMM.waveOutWrite(handle, hdr.ptr, hdr.size)
    }

    fun dispose() {
        WINMM.waveOutUnprepareHeader(handle, hdr.ptr, hdr.size)
    }

    override fun toString(): String = "WaveHeader(id=$id, totalSamples=$totalSamples, nchannels=$channels, hdr=$hdr)"
}

internal typealias LPHWAVEOUT = FFIPointer
internal typealias HWAVEOUT = FFIPointer
internal typealias LPCWAVEFORMATEX = FFIPointer
internal typealias LPWAVEHDR = FFIPointer

internal class WAVEHDR(pointer: FFIPointer? = null) : FFIStructure(pointer) {
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

internal class WAVEFORMATEX(pointer: FFIPointer? = null) : FFIStructure(pointer) {
    var wFormatTag by short()
    var nChannels by short()
    var nSamplesPerSec by int()
    var nAvgBytesPerSec by int()
    var nBlockAlign by short()
    var wBitsPerSample by short()
    var cbSize by short()
}

internal typealias LPMMTIME = FFIPointer?
internal class MMTIME(pointer: FFIPointer? = null) : FFIStructure(pointer) {
    var wType by int()
    var values by int()
}

internal object WINMM : FFILib("winmm.dll") {
    //val waveOutOpen by func<(phwo: LPHWAVEOUT?, uDeviceID: Int, pwfx: LPCWAVEFORMATEX?, dwCallback: Callback?, dwInstance: Pointer?, fdwOpen: Int) -> Int>()
    val waveOutOpen by func<(phwo: LPHWAVEOUT?, uDeviceID: Int, pwfx: LPCWAVEFORMATEX?, dwCallback: FFIPointer?, dwInstance: FFIPointer?, fdwOpen: Int) -> Int>()
    val waveOutClose by func<(hwo: HWAVEOUT?) -> Int>()
    val waveOutReset by func<(hwo: HWAVEOUT?) -> Int>()
    val waveOutPrepareHeader by func<(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int) -> Int>()
    val waveOutWrite by func<(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int) -> Int>()
    val waveOutUnprepareHeader by func<(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int) -> Int>()
    val waveOutGetPosition by func<(hwo: HWAVEOUT?, pmmt: LPMMTIME?, cbmmt: Int) -> Int>()

    fun waveOutGetPositionInSamples(hwo: HWAVEOUT?): Long {
        ffiScoped {
            val mem = allocBytes(16).typed<Int>()
            mem[0] = TIME_SAMPLES
            val res = waveOutGetPosition(hwo, mem.pointer, 16)
            val wType = mem[0]
            val value = mem[1]
            //println("waveOutGetPosition: res=$res, wType=$wType, value=$value")
            return value.toLong()
        }
    }

    const val WAVE_MAPPER = -1
    const val WAVE_FORMAT_PCM = 1

    const val WHDR_DONE = 0x00000001
    const val WHDR_PREPARED = 0x00000002
    const val WHDR_BEGINLOOP = 0x00000004
    const val WHDR_ENDLOOP = 0x00000008
    const val WHDR_INQUEUE = 0x00000010

    const val TIME_MS = 1
    const val TIME_SAMPLES = 2
    const val TIME_BYTES = 4
    const val TIME_SMPTE = 8
    const val TIME_MIDI =16
    const val TIME_TICKS =32
}
