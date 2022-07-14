package com.soywiz.korau.mod

import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.file.VfsFile
import kotlin.math.min

abstract class BaseModuleTracker {
    var samplerate = 44100
    var playing = false
    var endofsong = false

    abstract fun initialize()
    abstract fun parse(buffer: Uint8Buffer): Boolean
    abstract fun mix(bufs: Array<FloatArray>, buflen: Int = bufs[0].size)

    fun parseAndInit(buffer: Uint8Buffer) {
        parse(buffer)
        initialize()
        playing = true
    }

    var totalLengthInSamples: Long? = null

    suspend fun createSoundFromFile(file: VfsFile, soundProvider: NativeSoundProvider = nativeSoundProvider): Sound {
        parseAndInit(file.readBytes().toUint8Buffer())
        return createSound(soundProvider)
    }

    suspend fun createSound(soundProvider: NativeSoundProvider = nativeSoundProvider): Sound {
        return soundProvider.createStreamingSound(createAudioStream())
    }

    fun createAudioStream(): AudioStream {
        playing = true
        var fch = Array(2) { FloatArray(1024) }
        return object : AudioStream(samplerate, 2) {
            override val finished: Boolean get() = endofsong

            // @TODO: we should figure out how to compute the length in samples/time
            override val totalLengthInSamples: Long?
                get() = this@BaseModuleTracker.totalLengthInSamples

            var _currentPositionInSamples: Long = 0L

            private fun skipUntil(newPosition: Long) {
                while (_currentPositionInSamples < newPosition) {
                    val available = newPosition - _currentPositionInSamples
                    val skip = min(available.toInt(), fch[0].size)
                    mix(fch, skip)
                    _currentPositionInSamples += skip
                }
            }

            override var currentPositionInSamples: Long
                get() = _currentPositionInSamples
                set(value) {
                    if (_currentPositionInSamples == value) return
                    if (value > _currentPositionInSamples) {
                        skipUntil(value)
                    } else {
                        //if (value != 0L) error("only supported rewind in MOD value=$value")
                        _currentPositionInSamples = 0L
                        initialize()
                        if (value != 0L) {
                            println("SLOW SEEK")
                            skipUntil(value)
                        }
                    }
                }

            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                if (fch[0].size < length) fch = Array(2) { FloatArray(length) }
                mix(fch, length)
                _currentPositionInSamples += length
                val l = fch[0]
                val r = fch[1]
                for (n in 0 until length) out.setFloatStereo(offset + n, l[n], r[n])
                return length
            }

            override suspend fun clone(): AudioStream {
                return createAudioStream()
            }

        }
    }
}
