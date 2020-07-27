import com.soywiz.kmem.*
import com.soywiz.korau.sound.*
import com.soywiz.korge.*
import com.soywiz.korio.async.*

// https://github.com/pspdev/pspsdk/blob/master/src/samples/audio/polyphonic/main.c
suspend fun main() = Korge {
	println("Polyphonic sample by Shine")
	println()
	println("Soundtrack of the movie")
	println("\"Le fabuleux destin d'Amelie Poulain\"")
	println("by Yann Tiersen")

	val maxAt = SAMPLE_COUNT / 16
	for (i in 0 until SAMPLE_COUNT) {
		sample[i] = when {
			i < maxAt -> (i.toFloat() / maxAt.toFloat() * 2f - 1f)
			else -> (1f - (i - maxAt).toFloat() / (SAMPLE_COUNT - maxAt).toFloat() * 2f)
		}
	}
	var base = 40.0f
	for (i in 0 until OCTAVE_COUNT) {
		createPitches(base, octaves[i])
		base *= 2f
	}
	channelStates[0].noteIndex = 0; nextNote(0)
	channelStates[1].noteIndex = 0; nextNote(1)

	for (nchannel in 0 until 2) {
	//for (nchannel in 0 until 1) {
		launchImmediately {
			//AudioTone.generate(0.25.seconds, 440.0).playAndWait()
			val stream = nativeSoundProvider.createAudioStream(44100)
			stream.start()
			while (true) {
				//val samples = AudioSamples(1, 44100 * 6)
				val samples = AudioSamples(1, 4410)
				//val samples = AudioSamples(2, 44100)
				//val samples = AudioSamples(1, 44100)
				audioOutCallback(nchannel, samples.data[0], samples.data[0].size)
				for (n in 1 until samples.channels) {
					arraycopy(samples.data[0], 0, samples.data[n], 0, samples.data[0].size)
				}
				//MemorySyncStream().apply { writeShortArrayLE(samples.data[0]) }.toByteArray().writeToFile("/tmp/data.raw")
				//for (n in 0 until 44100) println(samples.data[0][n])
				stream.add(samples)
			}
		}
	}
}

const val SAMPLE_COUNT = 0x10000
val sample = FloatArray(SAMPLE_COUNT)

const val SAMPLE_RATE = 44100

const val OCTAVE_COUNT = 6

val octaves = Array(6) { FloatArray(12) }

data class Note_t(val note: Int, val octave: Int, val duration: Int)
data class ChannelState_t(
	var currentNote: Note_t = Note_t(0, 0, 0),
	var noteIndex: Int = 0,
	var currentTime: Int = 0,
	var currentsampleIndex: Float = 0f,
	var currentsampleIncrement: Float = 0f
)

val channelStates = Array(3) { ChannelState_t() }

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

var channel0 = arrayOf<Note_t>(
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

var channel1 = arrayOf<Note_t>(
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
	state.currentNote = channels[channel][state.noteIndex]
	state.currentTime = 0
	state.currentsampleIndex = 0f
	val note = state.currentNote.note
	if (note == NOTE_PAUSE) {
		state.currentsampleIncrement = 0f
	} else {
		state.currentsampleIncrement = octaves[state.currentNote.octave][note] * (SAMPLE_COUNT.toFloat()) / (SAMPLE_RATE.toFloat())
	}

	state.noteIndex++
	if (channels[channel][state.noteIndex].note == NOTE_END) state.noteIndex = 0
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
		val time = (state.currentTime.toFloat()) / (SAMPLE_RATE.toFloat())
		if (state.currentTime++ == state.currentNote.duration) {
			nextNote(channel)
		}
		var value: Float
		if (state.currentsampleIncrement == 0.0f) {
			value = 0.0f
		} else {
			value = sample[state.currentsampleIndex.toInt()] * adsr(time, (state.currentNote.duration.toFloat()) / (SAMPLE_RATE.toFloat()))
			value *= 0x7000f
			state.currentsampleIndex += state.currentsampleIncrement
			if (state.currentsampleIndex >= SAMPLE_COUNT) state.currentsampleIndex -= SAMPLE_COUNT.toFloat()
		}
		val rvalue = value.clamp(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toShort()
		//for (n in 0 until nchannels) buf[bufn++] = value.toShort()
		buf[bufn++] = rvalue
		//buf[bufn++] = rvalue
	}
}

fun createPitches(base: Float, target: FloatArray) {
	var base = base
	for (i in 0 until 12) {
		target[i] = base
		base *= 1.0594630943592952645618252949463f  // 2^(1/12)
	}
}
