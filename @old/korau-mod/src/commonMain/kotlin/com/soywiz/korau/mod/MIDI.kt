package com.soywiz.korau.mod

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

// http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html

fun SyncStream.readMidi(): Midi {
	var format = 0
	var ntrks = 0
	var division = 0
	val tracks = arrayListOf<Midi.Track>()

	while (!eof) {
		val chunkType = readStringz(4)
		val headerLen = readS32BE()
		val headerStream = readStream(headerLen)
		headerStream.run {
			when (chunkType) {
				"MThd" -> {
					// HEADER
					format = readU16BE() // 0=SINGLE, 1=MULTIPLE, 2=PATTERN
					ntrks = readU16BE()
					division = readU16BE()
					//if (format != 0) TODO("Just supported single track MIDI files")
					println("format=$format, ntrks=$ntrks, division=$division")
					if (division hasBit 15) {
						val ticksPerFrame = division.extract(0, 8)
						val smpte = division.extract(8, 7)
						println("ticksPerFrame=$ticksPerFrame, smpte=$smpte")
					} else {
						val ticksPerQuarterNote = division.extract(0, 15)
						println("ticksPerQuarterNote=$ticksPerQuarterNote")
					}
				}
				"MTrk" -> {
					// TRACK
					tracks += Midi.Track(tracks.size, MidiReader().readEvents(this))
				}
				else -> error("Not a MIDI file")
			}
		}
	}

	return Midi(tracks)
}

class MidiReader {
	//var time = 0
	fun SyncStream.readVar() = readU_VL()

	fun readEvents(s: SyncStream) = mapWhile({ !s.eof }) { s.readMidiEvent() }.toList()

	private object META {
		val SEQUENCE_NUM = 0
		val TEXT = 1
		val COPYRIGHT = 2
		val SEQUENCE_NAME = 3
		val INSTRUMENT_NAME = 4
		val LYRIC = 5
		val MARKER = 6
		val CUE_POINT = 7
		val PROGRAM_NAME = 8
		val DEVICE_NAME = 9
		val MIDI_CHANNEL_PREFIX = 0x20
		val MIDI_PORT = 0x21
		val END_OF_TRACK = 0x2f
		val TEMPO = 0x51
		val SMPTE_OFFSET = 0x54
		val TIME_SIGNATURE = 0x58
		val KEY_SIGNATURE = 0x59
		val SEQUENCER_EVENT = 0x7f
	}

	fun SyncStream.readMidiSystemEvent(deltaTime: Int): Midi.Event {
		val mtype = readU8()
		val mlen = readVar()
		val mmessage = readStream(mlen)
		return mmessage.run {
			val info = when (mtype) {
				META.SEQUENCE_NUM -> {
					val seqNum = readU16BE()
					Midi.MetaEvent.SequenceNum(seqNum)
				}
				META.TEXT,
				META.COPYRIGHT,
				META.SEQUENCE_NAME,
				META.INSTRUMENT_NAME,
				META.LYRIC,
				META.MARKER,
				META.CUE_POINT,
				META.PROGRAM_NAME,
				META.DEVICE_NAME -> {
					Midi.MetaEvent.MString(mtype, readAvailable().toString(UTF8))
				}
				META.MIDI_CHANNEL_PREFIX -> {
					val channelPrefix = readU8()
					Midi.MetaEvent.ChannelPrefix(channelPrefix)
				}
				META.MIDI_PORT -> {
					val midiPort = readU8()
					Midi.MetaEvent.MidiPort(midiPort)
				}
				META.END_OF_TRACK -> {
					Midi.MetaEvent.EndOfTrack
				}
				META.TEMPO -> {
					val usecPerQuarterNote = readU24BE()
					Midi.MetaEvent.Tempo(usecPerQuarterNote)
				}
				META.SMPTE_OFFSET -> {
					val hours = readU8()
					val mins = readU8()
					val secs = readU8()
					val fps = readU8()
					val fracFrames = readU8()
					Midi.MetaEvent.SmpteOffset(hours, mins, secs, fps, fracFrames)
				}
				META.TIME_SIGNATURE -> {
					val numerator = readU8()
					val denominator = readU8()
					val clocksPerClick = readU8()
					val m32ndPer4th = readU8()
					Midi.MetaEvent.TimeSignature(numerator, denominator, clocksPerClick, m32ndPer4th)
				}
				META.KEY_SIGNATURE -> {
					val flatsSharps = readU8()
					val majorMinor = readU8()
					Midi.MetaEvent.KeySignature(flatsSharps, majorMinor)
				}
				META.SEQUENCER_EVENT -> {
					Midi.MetaEvent.SequencerEvent(mmessage.readAvailable())
				}
				else -> {
					Midi.MetaEvent.Other(mtype, mmessage.readAvailable())
				}
			}
			Midi.MetaEvent(deltaTime, info)
		}
	}

	fun SyncStream.readMidiEvent(): Midi.Event {
		val deltaTime = readVar()
		val status = readU8()
		when (status) {
			0xFF -> {
				return readMidiSystemEvent(deltaTime)
			}
			0xF0 -> {
				return Midi.SysexEvent(deltaTime, readBytesExact(readVar()))
			}
			else -> {
				val sstatus = status and 0xF0
				val channel = status and 0x0F
				val info = when (sstatus) {
					0x80, 0x90 -> { // note_off_event, note_on_event
						val on = sstatus == 0x90
						val note = readU8()
						val velocity = readU8()
						Midi.ChannelEvent.NoteOnOff(on, note, velocity)
					}
					0xA0 -> { // note_pressure_event
						val note = readU8()
						val pressure = readU8()
						Midi.ChannelEvent.NotePressure(note, pressure)
					}
					0xB0 -> { // controller_event
						val controller = readU8()
						val value = readU8()
						Midi.ChannelEvent.Controller(controller, value)
					}
					0xC0 -> { // program_event
						val program = readU8()
						Midi.ChannelEvent.Program(program)
					}
					0xD0 -> { // channel_pressure_event
						val pressure = readU8()
						Midi.ChannelEvent.Pressure(pressure)
					}
					0xE0 -> { // pitch_bend_event
						val lsb = readU8()
						val msb = readU8()
						Midi.ChannelEvent.PitchBend(lsb, msb)
					}
					else -> TODO("Illegal")
				}
				return Midi.ChannelEvent(deltaTime, channel, info)
			}
		}
	}
}

data class Midi(val tracks: List<Track>) {
	val nchannels = tracks.map { it.nchannels }.max() ?: 0

	data class Track(val index: Int, val events: List<Event>) {
		val nchannels = events.filterIsInstance<ChannelEvent>().map { it.channel + 1 }.max() ?: 0
	}

	interface Event {
		val deltaTime: Int
	}

	data class ChannelEvent(override val deltaTime: Int, val channel: Int, val info: Info) :
		Event {
		interface Info
		data class NoteOnOff(val on: Boolean, val note: Int, val velocity: Int) : Info
		data class NotePressure(val note: Int, val pressure: Int) : Info
		data class Controller(val controller: Int, val value: Int) : Info
		data class Program(val program: Int) : Info
		data class Pressure(val pressure: Int) : Info
		data class PitchBend(val lsb: Int, val msb: Int) : Info
	}

	data class MetaEvent(override val deltaTime: Int, val info: Info) : Event {
		interface Info
		data class MString(val kind: Int, val value: String) : Info
		data class SequenceNum(val seqNum: Int) : Info
		data class ChannelPrefix(val channelPrefix: Int) : Info
		data class MidiPort(val midiPort: Int) : Info
		data class Tempo(val usecPerQuarterNote: Int) : Info {
			val bpm = 60000000 / usecPerQuarterNote
		}

		data class SmpteOffset(val hours: Int, val mins: Int, val secs: Int, val fps: Int, val fracFrames: Int) :
			Info

		data class TimeSignature(
			val numerator: Int,
			val denominator: Int,
			val clocksPerClick: Int,
			val m32ndPer4th: Int
		) : Info

		data class KeySignature(val flatsSharps: Int, val majorMinor: Int) : Info
		data class SequencerEvent(val data: ByteArray) : Info
		data class Other(val mtype: Int, val data: ByteArray) : Info

		object EndOfTrack : Info
	}

	//data class MetaStringEvent(override val deltaTime: Int, val kind: Int, val value: String) : Event
	data class SysexEvent(override val deltaTime: Int, val data: ByteArray) : Event

	data class Instrument(val index: Int, val name: String, val family: Family? = null) {
		enum class Family {
			Piano, ChromaticPercussion,
			Organ, Guitar, Bass, Strings,
			Ensemble, Brass, Reed, Pipe, SynthLead,
			SynthPad, SynthEffects, Ethnic, Percussive,
			SoundEffects
		}

		companion object {
			val instruments = listOf(
				Instrument(1, "Acoustic Grand Piano", Family.Piano),
				Instrument(2, "Bright Acoustic Piano", Family.Piano),
				Instrument(3, "Electric Grand Piano", Family.Piano),
				Instrument(4, "Honky-tonk Piano", Family.Piano),
				Instrument(5, "Electric Piano 1 (Rhodes Piano)", Family.Piano),
				Instrument(6, "Electric Piano 2 (Chorused Piano)", Family.Piano),
				Instrument(7, "Harpsichord", Family.Piano),
				Instrument(8, "Clavinet", Family.Piano),
				Instrument(9, "Celesta", Family.ChromaticPercussion),
				Instrument(10, "Glockenspiel", Family.ChromaticPercussion),
				Instrument(11, "Music Box", Family.ChromaticPercussion),
				Instrument(12, "Vibraphone", Family.ChromaticPercussion),
				Instrument(13, "Marimba", Family.ChromaticPercussion),
				Instrument(14, "Xylophone", Family.ChromaticPercussion),
				Instrument(15, "Tubular Bells", Family.ChromaticPercussion),
				Instrument(16, "Dulcimer (Santur)", Family.ChromaticPercussion),
				Instrument(17, "Drawbar Organ (Hammond)", Family.Organ),
				Instrument(18, "Percussive Organ", Family.Organ),
				Instrument(19, "Rock Organ", Family.Organ),
				Instrument(20, "Church Organ", Family.Organ),
				Instrument(21, "Reed Organ", Family.Organ),
				Instrument(22, "Accordion (French)", Family.Organ),
				Instrument(23, "Harmonica", Family.Organ),
				Instrument(24, "Tango Accordion (Band neon)", Family.Organ),
				Instrument(25, "Acoustic Guitar (nylon)", Family.Guitar),
				Instrument(26, "Acoustic Guitar (steel)", Family.Guitar),
				Instrument(27, "Electric Guitar (jazz)", Family.Guitar),
				Instrument(28, "Electric Guitar (clean)", Family.Guitar),
				Instrument(29, "Electric Guitar (muted)", Family.Guitar),
				Instrument(30, "Overdriven Guitar", Family.Guitar),
				Instrument(31, "Distortion Guitar", Family.Guitar),
				Instrument(32, "Guitar harmonics", Family.Guitar),
				Instrument(33, "Acoustic Bass", Family.Bass),
				Instrument(34, "Electric Bass (fingered)", Family.Bass),
				Instrument(35, "Electric Bass (picked)", Family.Bass),
				Instrument(36, "Fretless Bass", Family.Bass),
				Instrument(37, "Slap Bass 1", Family.Bass),
				Instrument(38, "Slap Bass 2", Family.Bass),
				Instrument(39, "Synth Bass 1", Family.Bass),
				Instrument(40, "Synth Bass 2", Family.Bass),
				Instrument(41, "Violin", Family.Strings),
				Instrument(42, "Viola", Family.Strings),
				Instrument(43, "Cello", Family.Strings),
				Instrument(44, "Contrabass", Family.Strings),
				Instrument(45, "Tremolo Strings", Family.Strings),
				Instrument(46, "Pizzicato Strings", Family.Strings),
				Instrument(47, "Orchestral Harp", Family.Strings),
				Instrument(48, "Timpani", Family.Strings),
				Instrument(49, "String Ensemble 1 (strings)", Family.Ensemble),
				Instrument(50, "String Ensemble 2 (slow strings)", Family.Ensemble),
				Instrument(51, "SynthStrings 1", Family.Ensemble),
				Instrument(52, "SynthStrings 2", Family.Ensemble),
				Instrument(53, "Choir Aahs", Family.Ensemble),
				Instrument(54, "Voice Oohs", Family.Ensemble),
				Instrument(55, "Synth Voice", Family.Ensemble),
				Instrument(56, "Orchestra Hit", Family.Ensemble),
				Instrument(57, "Trumpet", Family.Brass),
				Instrument(58, "Trombone", Family.Brass),
				Instrument(59, "Tuba", Family.Brass),
				Instrument(60, "Muted Trumpet", Family.Brass),
				Instrument(61, "French Horn", Family.Brass),
				Instrument(62, "Brass Section", Family.Brass),
				Instrument(63, "SynthBrass 1", Family.Brass),
				Instrument(64, "SynthBrass 2", Family.Brass),
				Instrument(65, "Soprano Sax", Family.Reed),
				Instrument(66, "Alto Sax", Family.Reed),
				Instrument(67, "Tenor Sax", Family.Reed),
				Instrument(68, "Baritone Sax", Family.Reed),
				Instrument(69, "Oboe", Family.Reed),
				Instrument(70, "English Horn", Family.Reed),
				Instrument(71, "Bassoon", Family.Reed),
				Instrument(72, "Clarinet", Family.Reed),
				Instrument(73, "Piccolo", Family.Pipe),
				Instrument(74, "Flute", Family.Pipe),
				Instrument(75, "Recorder", Family.Pipe),
				Instrument(76, "Pan Flute", Family.Pipe),
				Instrument(77, "Blown Bottle", Family.Pipe),
				Instrument(78, "Shakuhachi", Family.Pipe),
				Instrument(79, "Whistle", Family.Pipe),
				Instrument(80, "Ocarina", Family.Pipe),
				Instrument(81, "Lead 1 (square wave)", Family.SynthLead),
				Instrument(82, "Lead 2 (sawtooth wave)", Family.SynthLead),
				Instrument(83, "Lead 3 (calliope)", Family.SynthLead),
				Instrument(84, "Lead 4 (chiffer)", Family.SynthLead),
				Instrument(85, "Lead 5 (charang)", Family.SynthLead),
				Instrument(86, "Lead 6 (voice solo)", Family.SynthLead),
				Instrument(87, "Lead 7 (fifths)", Family.SynthLead),
				Instrument(88, "Lead 8 (bass + lead)", Family.SynthLead),
				Instrument(89, "Pad 1 (new age Fantasia)", Family.SynthPad),
				Instrument(90, "Pad 2 (warm)", Family.SynthPad),
				Instrument(91, "Pad 3 (polysynth)", Family.SynthPad),
				Instrument(92, "Pad 4 (choir space voice)", Family.SynthPad),
				Instrument(93, "Pad 5 (bowed glass)", Family.SynthPad),
				Instrument(94, "Pad 6 (metallic pro)", Family.SynthPad),
				Instrument(95, "Pad 7 (halo)", Family.SynthPad),
				Instrument(96, "Pad 8 (sweep)", Family.SynthPad),
				Instrument(97, "FX 1 (rain)", Family.SynthEffects),
				Instrument(98, "FX 2 (soundtrack)", Family.SynthEffects),
				Instrument(99, "FX 3 (crystal)", Family.SynthEffects),
				Instrument(100, "FX 4 (atmosphere)", Family.SynthEffects),
				Instrument(101, "FX 5 (brightness)", Family.SynthEffects),
				Instrument(102, "FX 6 (goblins)", Family.SynthEffects),
				Instrument(103, "FX 7 (echoes, drops)", Family.SynthEffects),
				Instrument(104, "FX 8 (sci-fi, star theme)", Family.SynthEffects),
				Instrument(105, "Sitar", Family.Ethnic),
				Instrument(106, "Banjo", Family.Ethnic),
				Instrument(107, "Shamisen", Family.Ethnic),
				Instrument(108, "Koto", Family.Ethnic),
				Instrument(109, "Kalimba", Family.Ethnic),
				Instrument(110, "Bag pipe", Family.Ethnic),
				Instrument(111, "Fiddle", Family.Ethnic),
				Instrument(112, "Shanai", Family.Ethnic),
				Instrument(113, "Tinkle Bell", Family.Percussive),
				Instrument(114, "Agogo", Family.Percussive),
				Instrument(115, "Steel Drums", Family.Percussive),
				Instrument(116, "Woodblock", Family.Percussive),
				Instrument(117, "Taiko Drum", Family.Percussive),
				Instrument(118, "Melodic Tom", Family.Percussive),
				Instrument(119, "Synth Drum", Family.Percussive),
				Instrument(120, "Reverse Cymbal", Family.Percussive),
				Instrument(121, "Guitar Fret Noise", Family.SoundEffects),
				Instrument(122, "Breath Noise", Family.SoundEffects),
				Instrument(123, "Seashore", Family.SoundEffects),
				Instrument(124, "Bird Tweet", Family.SoundEffects),
				Instrument(125, "Telephone Ring", Family.SoundEffects),
				Instrument(126, "Helicopter", Family.SoundEffects),
				Instrument(127, "Applause", Family.SoundEffects),
				Instrument(128, "Gunshot", Family.SoundEffects)
			)
		}
	}

	object Notes {
		private val NOTE_STRS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
		fun getName(note: Int) = NOTE_STRS[note % NOTE_STRS.size]
		fun getOctave(note: Int) = (note / 12)
		fun toString(note: Int): String {
			return "${getName(note)}${getOctave(note)}"
		}
	}
}

private infix fun Int.hasBit(bit: Int): Boolean = (this and (1 shl bit)) != 0