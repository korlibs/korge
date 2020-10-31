package com.soywiz.korau.mod

import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.concurrent.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.math.*
import kotlin.random.Random

suspend fun VfsFile.readMod() = MOD().apply { read(readAll().openSync()) }

// http://www.fileformat.info/format/mod/corion.htm
// https://eblong.com/zarf/blorb/mod-spec.txt
// https://github.com/jhalme/webaudio-mod-player/blob/master/js/pt.js
class MOD {
	fun read(s: SyncStream) = s.readMod()

	var nchannels = 4
	var instruments = listOf<Instrument>(); private set
	var songLenInPatterns = 0
	lateinit var patterns: Patterns; private set

	companion object {
		val BASE_PERIODS = intArrayOf(
			856, 808, 762, 720, 678, 640, 604, 570, 538, 508, 480, 453,
			428, 404, 381, 360, 339, 320, 302, 285, 269, 254, 240, 226,
			214, 202, 190, 180, 170, 160, 151, 143, 135, 127, 120, 113
		)
		val FINE_TINE = DoubleArray(16) { 2.0.pow((it - 8) / 12 / 8) }
		val VIBRATO_TABLES = Array(4) { vibrato ->
			DoubleArray(64) { i ->
				when (vibrato) {
					0 -> 127 * sin(PI * 2 * (i / 64))
					1 -> (127 - 4 * i).toDouble()
					2 -> if (i < 32) 127.0 else -127.0
					3 -> (1 - 2 * Random.nextDouble()) * 127
					else -> invalidOp
				}
			}
		}
	}

	fun SyncStream.readMod() {
		val ninstruments = 31
		//if (sliceWithStart(1080).readStringz(4) in listOf("M.K", "4CHN", "6CHN", "8CHN", "FLT4", "FLT8")) 31 else 15
		val title = readStringz(20).trim()
		instruments = (0 until ninstruments).map {
			Instrument(
				name = readStringz(22).trim(),
				sampleLen = readU16BE(),
				finetune = readU8(),
				volume = readU8(),
				loopStart = readU16BE(),
				loopLen = readU16BE()
			)
		}
		for (instrument in instruments) {
			println(instrument)
		}
		position = 950
		songLenInPatterns = readU8()
		val restartByteForSongLooping = readU8()
		val patternPlaySequences = readUByteArray(128)
		val id = readStringz(4)
		nchannels = id.filter { it.isNumeric }.toInt()

		patterns = Patterns((0 until songLenInPatterns).map { pattern ->
			Pattern((0 until 64).map { row ->
				Row((0 until nchannels).map { channel -> Note(pattern, row, channel, readS32BE()) })
			})
		})
		for (i in instruments) {
			i.data = readShortArrayBE(i.sampleLen)
		}
		//println(id)
		//println(available)
		//println(available)
	}

	data class Patterns(val pattern: List<Pattern>)
	data class Pattern(val rows: List<Row>)
	data class Row(val notes: List<Note>)

	class Note(val pattern: Int, val row: Int, val channel: Int, val data: Int) {
		val reffect = data.extract(0, 12)
		val instrumentLow = data.extract(12, 4)
		val period = data.extract(16, 11)
		val instrumentHigh = data.extract(28, 3)
		val instrument = instrumentLow or (instrumentHigh shl 4)
		val effect get() = reffect.extract(8, 4)
		val effectData get() = reffect.extract(0, 8)
		override fun toString(): String = "Note(%d, %d, %03X)".format(instrument, period, reffect)
	}

	data class Instrument(
		val name: String,
		val sampleLen: Int,
		val finetune: Int,
		val volume: Int,
		val loopStart: Int,
		val loopLen: Int
	) {
		val loopEnd = loopStart + loopLen

		companion object {
			val dummy = Instrument("", 0, 0, 0, 0, 0)
		}

		var data = shortArrayOf()
	}

	fun interpreter() = Interpreter(this)

	class Interpreter(val mod: MOD) {
		val rate = 16574
		//val rate = 22100
		//val rate = 44100
		var tempo = 90 // BPM

		inner class ChannelInterpreter(val channel: Int) {
			val samples = ArrayList<Short>()
			val samplesLock = Lock()

			var ir: InstrumentReader = InstrumentReader(Instrument.dummy, 0)
			var period = 0
			var volume = 0
			var panning = 127
			var sampleOffset = 0

			//val rowsPerSecond get() = 60.0 / tempo.toDouble()
			val rowsPerSecond get() = 4.0
			val volumeFloat get() = volume.toDouble() / 64.0

			inner class InstrumentReader(var instrument: Instrument, var offset: Int = 0) {
				val size get() = instrument.data.size

				var sub: Int = 0

				fun read(num: Int, den: Int): Int {
					sub += num
					if (sub >= den) {
						offset += sub / den
						sub %= den
					}
					return get()
				}

				fun read(): Int = read(1, 1)

				fun get(): Int {
					if (instrument.loopStart != 0) {
						if (offset >= instrument.loopEnd) {
							offset = instrument.loopStart + ((offset - instrument.loopEnd) % instrument.loopLen)
						}
					}
					val data = instrument.data.getOrElse(offset) { 0 }.toInt()
					//print("[$offset/$size]=$data ")
					return data
				}
			}

			fun eval(note: Note) {
				if (note.data != 0) {
					//println(note)
					when (note.effect) {
						0 -> Unit
						0x8 -> run { panning = note.effectData }
						0xC -> run { volume = note.effectData }
						0xF -> run { tempo = note.effectData }
						else -> {
							//TODO("Unsupported effect ${note.effect}")
						}
					}
					if (note.period != 0) {
						ir.instrument = mod.instruments.getOrNull(note.instrument) ?: Instrument.dummy
						ir.offset = 0
						println("[${note.pattern},${note.row},${note.channel}]:  ${ir.instrument}: $note")

						period = note.period
						sampleOffset = 0
					}
				}
				if (period > 0) {
					val sampleRate = 7093789.2 / period.toDouble()

					//println("sampleRate: $sampleRate")

					samplesLock {
						//println("--------------")
						for (n in 0 until ((rate / rowsPerSecond)).toInt()) {
							val sample = ir.read(sampleRate.toInt(), 90000)
							samples.add((sample * volumeFloat).toShort())
							//samples.add((sample * volumeFloat).toShort())
							//print("$sample,")
						}
						//println()
					}
				}
			}
		}

		val channels = (0 until mod.nchannels).map { ChannelInterpreter(it) }

		var pattern = 0
		var row = 0

		fun evalNextRow() {
			val pat = mod.patterns.pattern[pattern]
			evalRow(pat.rows[row])
			row++
			if (row >= 64) {
				row = 0
				pattern++
			}
		}

		val samples = ArrayList<Short>()
		val samplesLock = Lock()

		fun evalRow(row: MOD.Row) {
			for ((index, note) in row.notes.withIndex()) {
				//if (index == 0) channels[index].eval(note)
				//if (index == 18) channels[index].eval(note)
				//if (index == 17) channels[index].eval(note)
				channels[index].eval(note)
			}

			// MIX channels
			samplesLock {
				for (n in 0 until (channels.map { it.samples.size }.max() ?: 0)) {
					var accum = 0
					var count = 0
					for (channel in channels) {
						if (channel.volume != 0) {
							//println(channel.volume)
							accum += channel.samples.getOrElse(n) { 0 }
							count++
						}
					}
					if (count == 0) {
						samples.add(0)
					} else {
						samples.add(((accum / count) * 0.3).toShort())
					}
				}
				for (channel in channels) {
					channel.samples.clear()
				}
			}
		}

		suspend fun play() {
			nativeSoundProvider.play(AudioStream.generator(rate, 1) {
				samplesLock {
					val out = samples.toShortArray()
					samples.clear()
					out
				}
			})
		}
	}
}

