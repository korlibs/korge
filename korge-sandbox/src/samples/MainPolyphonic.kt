package samples

import korlibs.audio.sound.*
import korlibs.io.concurrent.atomic.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.*
import kotlinx.atomicfu.*
import kotlin.math.*

class MainPolyphonic : Scene() {
    // https://github.com/pspdev/pspsdk/blob/master/src/samples/audio/polyphonic/main.c
    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(adjustSize = false) {
            text("Polyphonic sample by Shine")
            text("")
            text("Soundtrack of the movie")
            text("\"Le fabuleux destin d'Amelie Poulain\"")
            text("by Yann Tiersen")
        }

        channelStates[0].noteIndex.value = 0; nextNote(0)
        channelStates[1].noteIndex.value = 0; nextNote(1)

        for (nchannel in 0 until 2) {
            val stream2 = nativeSoundProvider.createNewPlatformAudioOutput(1, 44100) { samples ->
                audioOutCallback(nchannel, samples.data, samples.data.size)
                samples.scaleVolume(.05f)
            }
            stream2.start()
        }
    }

    companion object {
        const val SAMPLE_COUNT = 0x1000
        val SAMPLE = FloatArray(SAMPLE_COUNT).also { SAMPLE ->
            val maxAt = SAMPLE_COUNT / 16
            for (i in 0 until SAMPLE_COUNT) {
                SAMPLE[i] = when {
                    i < maxAt -> (i.toFloat() / maxAt.toFloat() * 2f - 1f)
                    else -> (1f - (i - maxAt).toFloat() / (SAMPLE_COUNT - maxAt).toFloat() * 2f)
                }
            }
        }

        const val SAMPLE_RATE = 44100

        val OCTAVES = Array(6) { FloatArray(12) }.also { OCTAVES ->
            var base = 40.0f
            for (element in OCTAVES) {
                createPitches(base, element)
                base *= 2f
            }
        }

        data class Note_t(val note: Int, val octave: Int, val duration: Int)
        data class ChannelState_t(
            val currentNote: AtomicRef<Note_t> = atomic(Note_t(0, 0, 0)),
            val noteIndex: AtomicInt = atomic(0),
            val currentTime: AtomicInt = atomic(0),
            val currentsampleIndex: AtomicRef<Float> = atomic(0f),
            val currentsampleIncrement: AtomicRef<Float> = atomic(0f)
        )

        val channelStates = Array(2) { ChannelState_t() }

        // "S" means "#"
        const val NOTE_END = -2
        const val NOTE_PAUSE = -1
        const val NOTE_C = 0
        const val NOTE_CS = 1
        const val NOTE_D = 2
        const val NOTE_DS = 3
        const val NOTE_E = 4
        const val NOTE_F = 5
        const val NOTE_FS = 6
        const val NOTE_G = 7
        const val NOTE_GS = 8
        const val NOTE_A = 9
        const val NOTE_AS = 10
        const val NOTE_B = 11

        fun EIGHT_NOTE(note: Int, octave: Int, duration: Int) = Note_t(note, octave, SAMPLE_RATE * duration / 8)

        val channel0 = arrayOf<Note_t>(
            EIGHT_NOTE(NOTE_D, 4, 7),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_F, 4, 1),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_F, 4, 1),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_A, 3, 9),
            EIGHT_NOTE(NOTE_B, 3, 2),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_D, 4, 7),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_F, 4, 1),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_F, 4, 1),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_A, 3, 9),
            EIGHT_NOTE(NOTE_G, 3, 3),
            EIGHT_NOTE(NOTE_A, 3, 7),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_E, 3, 9),
            EIGHT_NOTE(NOTE_F, 3, 2),
            EIGHT_NOTE(NOTE_G, 3, 1),
            EIGHT_NOTE(NOTE_A, 3, 7),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_E, 3, 12),
            EIGHT_NOTE(NOTE_D, 4, 9),
            EIGHT_NOTE(NOTE_C, 4, 3),
            EIGHT_NOTE(NOTE_B, 3, 6),
            EIGHT_NOTE(NOTE_A, 3, 6),
            EIGHT_NOTE(NOTE_D, 4, 7),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_E, 4, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 6),
            EIGHT_NOTE(NOTE_A, 3, 6),
            EIGHT_NOTE(NOTE_C, 4, 9),
            EIGHT_NOTE(NOTE_B, 3, 3),
            EIGHT_NOTE(NOTE_E, 3, 12),
            EIGHT_NOTE(NOTE_C, 4, 7),
            EIGHT_NOTE(NOTE_D, 4, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_C, 4, 1),
            EIGHT_NOTE(NOTE_B, 3, 1),
            EIGHT_NOTE(NOTE_E, 3, 12),
            Note_t(NOTE_END, 0, 0)
        )

        val channel1 = arrayOf<Note_t>(
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_C, 3, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_C, 3, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_D, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_B, 1, 1),
            EIGHT_NOTE(NOTE_A, 1, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_A, 0, 1),
            EIGHT_NOTE(NOTE_E, 1, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_C, 3, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_C, 3, 1),
            EIGHT_NOTE(NOTE_A, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_F, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_F, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_G, 2, 1),
            EIGHT_NOTE(NOTE_E, 2, 1),
            EIGHT_NOTE(NOTE_D, 2, 1),
            EIGHT_NOTE(NOTE_C, 2, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            EIGHT_NOTE(NOTE_C, 1, 1),
            EIGHT_NOTE(NOTE_G, 1, 1),
            Note_t(NOTE_END, 0, 0)
        )

        val channels = arrayOf(channel0, channel1)

        fun nextNote(channel: Int) {
            val state = channelStates[channel]
            state.currentNote.value = channels[channel][state.noteIndex.value]
            state.currentTime.value = 0
            state.currentsampleIndex.value = 0f
            val note = state.currentNote.value.note
            if (note == NOTE_PAUSE) {
                state.currentsampleIncrement.value = 0f
            } else {
                state.currentsampleIncrement.value = OCTAVES[state.currentNote.value.octave][note] * (SAMPLE_COUNT.toFloat()) / (SAMPLE_RATE.toFloat())
            }

            state.noteIndex.incrementAndGet()
            if (channels[channel][state.noteIndex.value].note == NOTE_END) state.noteIndex.value = 0
        }

        // calculate current value of attack/delay/sustain/release envelope
        fun adsr(time: Float, duration: Float): Float {
            if (time < 0.0f) return 0.0f
            val attack = 0.004f
            val decay = 0.02f
            val sustain = 0.5f
            val release = 0.08f
            var time = time
            var duration = duration
            duration -= attack + decay + release
            if (time < attack) return time / attack
            time -= attack
            if (time < decay) return (decay - time) / decay * (1.0f - sustain) + sustain
            time -= decay
            if (time < duration) return sustain
            time -= duration
            if (time < release) return (release - time) / release * sustain
            return 0.0f
        }

        fun audioOutCallback(channel: Int, buf: ShortArray, reqn: Int = buf.size, bufn: Int = 0, nchannels: Int = 1) {
            val state = channelStates[channel]
            var bufn = bufn
            for (i in 0 until reqn) {
                val time = (state.currentTime.value.toFloat()) / (SAMPLE_RATE.toFloat())
                if (state.currentTime.getAndIncrement() == state.currentNote.value.duration) {
                    nextNote(channel)
                }
                var value: Float
                if (state.currentsampleIncrement.value == 0.0f) {
                    value = 0.0f
                } else {
                    value = SAMPLE[state.currentsampleIndex.value.toInt()] * adsr(time, (state.currentNote.value.duration.toFloat()) / (SAMPLE_RATE.toFloat()))
                    value *= 0x7000f
                    state.currentsampleIndex.addAndGetMod(state.currentsampleIncrement.value, SAMPLE_COUNT.toFloat())
                }
                val rvalue = value.clamp(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toInt().toFloat()).toInt().toShort()
                //for (n in 0 until nchannels) buf[bufn++] = value.toShort()
                buf[bufn++] = rvalue
                //buf[bufn++] = rvalue
            }
        }

        fun createPitches(base: Float, target: FloatArray) {
            var base = base
            val CONST = 2.0.pow(1.0 / 12.0).toFloat() // 2^(1/12)
            for (i in 0 until 12) {
                target[i] = base
                base *= CONST
            }
        }
    }
}

private fun AtomicRef<Float>.addAndGetMod(delta: Float, modulo: Float): Float = updateAndGet { (it + delta) % modulo }
private fun AtomicRef<Double>.addAndGetMod(delta: Double, modulo: Double): Double = updateAndGet { (it + delta) % modulo }
