package com.soywiz.korau.sound

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import platform.windows.*
import kotlin.math.*
import kotlin.native.concurrent.*

private class ConcurrentDeque<T : Any> {
    private val items = AtomicReference<List<T>>(emptyList<T>().freeze())

    init {
        this.freeze()
    }

    val size get() = items.value.size

    fun add(item: T) {
        do {
            val oldList = this.items.value
            val newList = (oldList + item).freeze()
        } while (!this.items.compareAndSet(oldList, newList))
    }

    //val length get() = items.value.size

    fun consume(): T? {
        while (true) {
            val oldList = this.items.value
            if (oldList.isEmpty()) return null
            val lastItem = oldList.first()
            val newList = oldList.subList(1, oldList.size).freeze()
            if (this.items.compareAndSet(oldList, newList)) return lastItem
        }
    }
}

private interface WaveOutPart

private object WaveOutEnd : WaveOutPart

private class WaveOutData(val data: ShortArray) : WaveOutPart {
    init {
        this.freeze()
    }
}
private class WaveOutSetVolume(val volume: Double) : WaveOutPart {
    init {
        this.freeze()
    }
}
class WaveOutProcess(val freq: Int, val nchannels: Int) {
    private val sPosition = AtomicLong(0L)
    private val sLength = AtomicLong(0L)
    private val completed = AtomicLong(0L)
    private val deque = ConcurrentDeque<WaveOutPart>()
    private val info = AtomicReference<Future<Unit>?>(null)

    val position get() = sPosition.value
    val length get() = sLength.value
    val isCompleted get() = completed.value != 0L

    init {
        freeze()
    }

    val pendingCommands get() = deque.size

    fun addData(data: ShortArray) {
        sLength.addAndGet(data.size / nchannels)
        deque.add(WaveOutData(data))
    }

    fun setVolume(volume: Double) {
        deque.add(WaveOutSetVolume(volume))
    }

    fun stop() {
        deque.add(WaveOutEnd)
    }

    fun stopAndWait() {
        stop()
        info?.value?.consume {  }
    }

    fun start(_worker: Worker): WaveOutProcess {
        val _info = this
        _info.info.value = _worker.execute(TransferMode.SAFE, { _info }) { info ->
            memScoped {
                val format = alloc<WAVEFORMATEX>().apply {
                    this.wFormatTag = WAVE_FORMAT_PCM.convert()
                    this.nChannels = info.nchannels.convert() // 2?
                    this.nSamplesPerSec = info.freq.convert() // 44100?
                    this.wBitsPerSample = Short.SIZE_BITS.convert() // 16
                    this.nBlockAlign = (info.nchannels * Short.SIZE_BYTES).convert()
                    this.nAvgBytesPerSec = this.nSamplesPerSec * this.nBlockAlign
                    this.cbSize = sizeOf<WAVEFORMATEX>().convert()
                    //this.cbSize = 0.convert()
                }
                val hWaveOut = alloc<HWAVEOUTVar>()

                waveOutOpen(hWaveOut.ptr, WAVE_MAPPER, format.ptr, 0.convert(), 0.convert(), CALLBACK_NULL)
                val pendingChunks = ArrayDeque<WaveOutChunk>()

                fun updatePosition() {
                    // Update position
                    memScoped {
                        val time = alloc<MMTIME>()
                        time.wType = TIME_BYTES.convert()
                        waveOutGetPosition(hWaveOut!!.value, time.ptr, sizeOf<MMTIME>().convert())
                        //info.position.value = time.u.cb.toLong() / Short.SIZE_BYTES / info.nchannels
                        info.sPosition.value = time.u.cb.toLong() / info.nchannels
                    }
                }

                fun clearCompletedChunks() {
                    updatePosition()
                    while (pendingChunks.isNotEmpty() && pendingChunks.first().completed) {
                        val chunk = pendingChunks.removeFirst()
                        waveOutUnprepareHeader(hWaveOut.value, chunk.hdr.ptr, sizeOf<WAVEHDR>().convert())
                        chunk.dispose()
                    }
                }

                try {
                    process@while (true) {
                        Sleep(5.convert())
                        updatePosition()
                        while (true) {
                            val it = info.deque.consume() ?: break
                            //println("CONSUME: $item")
                            when (it) {
                                is WaveOutEnd -> break@process
                                is WaveOutData -> {
                                    val chunk = WaveOutChunk(it.data)
                                    //info.sLength.addAndGet(chunk.data.size / info.nchannels)
                                    pendingChunks.add(chunk)
                                    waveOutPrepareHeader(hWaveOut.value, chunk.hdr.ptr, sizeOf<WAVEHDR>().convert())
                                    waveOutWrite(hWaveOut.value, chunk.hdr.ptr, sizeOf<WAVEHDR>().convert())
                                    clearCompletedChunks()
                                }
                                is WaveOutSetVolume -> {
                                    waveOutSetVolume(hWaveOut.value, (it.volume.clamp01() * 0xFFFF).toInt().convert())
                                }
                            }

                        }
                    }
                } finally {
                    //println("finalizing...")
                    while (pendingChunks.isNotEmpty()) {
                        Sleep(5.convert())
                        clearCompletedChunks()
                    }
                    waveOutReset(hWaveOut.value)
                    waveOutClose(hWaveOut.value)
                    info.completed.value = 1L
                }
            }
        }
        return _info
    }
}

private class WaveOutChunk(val data: ShortArray) {
    val scope = Arena()
    val dataPin = data.pin()
    val hdr = scope.alloc<WAVEHDR>().apply {
        //println(samplesInterleaved.data.toList())
        this.lpData = dataPin.startAddressOf.reinterpret()
        this.dwBufferLength = (data.size * Short.SIZE_BYTES).convert()
        this.dwFlags = 0.convert()
    }
    val completed: Boolean get() = (hdr.dwFlags.toInt() and WHDR_DONE.toInt()) != 0

    fun dispose() {
        dataPin.unpin()
        scope.clear()
    }
}
