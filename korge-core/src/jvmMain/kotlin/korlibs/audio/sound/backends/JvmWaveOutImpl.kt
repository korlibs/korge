package korlibs.audio.sound.backends

import korlibs.datastructure.thread.*
import korlibs.memory.*
import korlibs.memory.dyn.*
import korlibs.time.*
import korlibs.audio.sound.*
import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Memory
import kotlinx.coroutines.*
import kotlin.coroutines.*


val jvmWaveOutNativeSoundProvider: NativeSoundProvider? by lazy {
    JvmWaveOutNativeSoundProvider()
}

class JvmWaveOutNativeSoundProvider : NativeSoundProvider() {
    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        JvmWaveOutPlatformAudioOutput(this, coroutineContext, freq)
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

class JvmWaveOutPlatformAudioOutput(
    val provider: JvmWaveOutNativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    val samplesLock = korlibs.datastructure.lock.NonRecursiveLock()
    var nativeThread: NativeThread? = null
    var running = false
    var totalEmittedSamples = 0L

    override suspend fun wait() {
        // @TODO: Get samples not reproduced
        //println("WAITING...")
        for (n in 0 until 1000) {
            var currentPositionInSamples: Long = 0L
            var totalEmittedSamples: Long = 0L
            var availableRead = 0
            samplesLock {
                currentPositionInSamples = WINMM.waveOutGetPositionInSamples(handle)
                availableRead = this.availableRead
                totalEmittedSamples = this.totalEmittedSamples
            }
            //println("availableRead=$availableRead, waveOutGetPosition=$currentPositionInSamples, totalEmittedSamples=$totalEmittedSamples")
            if (availableRead <= 0 && currentPositionInSamples >= totalEmittedSamples) break
            korlibs.io.async.delay(1.milliseconds)
        }
        //println("DONE WAITING")
    }

    private var handle: Pointer? = null
    private var headers = emptyArray<WaveHeader>()

    override fun start() {
        //println("TRYING TO START")
        if (running) return
        //println("STARTED")
        running = true
        nativeThread = korlibs.datastructure.thread.NativeThread {
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

            headers = Array(3) { WaveHeader(it, handle, 1024, nchannels) }

            try {
                while (running || availableRead > 0) {
                    for (header in headers) {
                        if (!header.hdr.isInQueue) {
                            //println("Sending running=$running, availableRead=$availableRead, header=${header}")
                            if (availableRead > 0) {
                                samplesLock {
                                    totalEmittedSamples += header.totalSamples
                                    readShortsFully(header.data)
                                }
                                header.prepareAndWrite()
                            }
                        }
                        Thread.sleep(1L)
                    }
                }
            } finally {
                runBlocking {
                    wait()
                }
                WINMM.waveOutReset(handle)
                WINMM.waveOutClose(handle)
                handle = null
                //println("CLOSED")
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        //println("STOPPING")
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
        val hdr = WAVEHDR(Memory(WAVEHDR().size.toLong()).kpointer).also { hdr ->
            hdr.lpData = KPointerT(dataMem.kpointer)
            hdr.dwBufferLength = totalBytes
            hdr.dwFlags = 0
        }

        fun prepareAndWrite(totalSamples: Int = this.totalSamples) {
            //println(data[0].toList())

            hdr.dwBufferLength = (totalSamples * nchannels * Short.SIZE_BYTES)

            for (ch in 0 until nchannels) {
                for (n in 0 until totalSamples) {
                    dataMem.setShort(((n * nchannels + ch) * Short.SIZE_BYTES).toLong(), data[ch][n])
                }
            }
            //if (hdr.isPrepared) dispose()
            if (!hdr.isPrepared) {
                //println("-> prepare")
                WINMM.waveOutPrepareHeader(handle, hdr.pointer?.ptr, hdr.size)
            }
            WINMM.waveOutWrite(handle, hdr.pointer?.ptr, hdr.size)
        }

        fun dispose() {
            WINMM.waveOutUnprepareHeader(handle, hdr.pointer?.ptr, hdr.size)
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

internal typealias LPMMTIME = Pointer
internal class MMTIME(pointer: KPointer? = null) : KStructure(pointer) {
    var wType by int()
    var values by int()
}

internal object WINMM {
    @JvmStatic external fun waveOutOpen(phwo: LPHWAVEOUT?, uDeviceID: Int, pwfx: LPCWAVEFORMATEX?, dwCallback: Callback?, dwInstance: Pointer?, fdwOpen: Int): Int
    @JvmStatic external fun waveOutClose(hwo: HWAVEOUT?): Int
    @JvmStatic external fun waveOutReset(hwo: HWAVEOUT?): Int
    @JvmStatic external fun waveOutPrepareHeader(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int
    @JvmStatic external fun waveOutWrite(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int
    @JvmStatic external fun waveOutUnprepareHeader(hwo: HWAVEOUT?, pwh: LPWAVEHDR?, cbwh: Int): Int
    @JvmStatic external fun waveOutGetPosition(hwo: HWAVEOUT?, pmmt: LPMMTIME?, cbmmt: Int): Int
    fun waveOutGetPositionInSamples(hwo: HWAVEOUT?): Long {
        val mem = Memory(16L).also { it.clear() }
        mem.setInt(0L, TIME_SAMPLES)
        val res = waveOutGetPosition(hwo, mem, 16)
        val wType = mem.getInt(0L)
        val value = mem.getInt(4L)
        //println("waveOutGetPosition: res=$res, wType=$wType, value=$value")
        return value.toLong()
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

    init {
        Native.register("winmm.dll")
    }
}
