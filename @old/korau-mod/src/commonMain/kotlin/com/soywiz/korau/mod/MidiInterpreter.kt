package com.soywiz.korau.mod

class MidiInterpreter(
	val midi: Midi,
	val font: SoundFont = DummySoundFont,
	val emit: (channel: Int, sample: Float) -> Unit = { channel, sample -> }
) {
	init {
		println("MIDI. NTRACKS: ${midi.tracks.size}, NCHANNELS: ${midi.nchannels}")
	}

	val tracks = midi.tracks.map { TrackInterpreter(it) }
	val channels = (0 until midi.nchannels).map { ChannelInterpreter(it) }

	fun interpret(time: Int) {
		for (track in tracks) track.interpret(time)
	}

	inner class TrackInterpreter(val track: Midi.Track) {
		init {
			println("   - TRACK. NCHANNELS: ${track.nchannels}")
		}

		var cur = 0

		var elapsedTime = 0
		var currentTime = 0

		var tempBpm = 60 // @TODO

		fun interpret(time: Int) {
			elapsedTime += time
			while (cur < track.events.size) {
				val event = track.events[cur]
				if (currentTime >= elapsedTime) {
					//println("currentTime >= elapsedTime :: $currentTime >= $elapsedTime")
					break
				}
				eval(event)
				currentTime += event.deltaTime
				cur++
			}
			//for (channel in channels) channel.increaseTimeBefore(0, currentTime)
		}

		fun eval(event: Midi.Event) {
			when (event) {
				is Midi.MetaEvent -> {
					val info = event.info
					when (info) {
						is Midi.MetaEvent.Tempo -> run { tempBpm = info.bpm }
						is Midi.MetaEvent.EndOfTrack -> Unit
						is Midi.MetaEvent.KeySignature -> Unit
						is Midi.MetaEvent.MString -> Unit
						else -> {
							println(event)
						}
					}
				}
				is Midi.ChannelEvent -> {
					val channel = channels[event.channel]
					val info = event.info
					if (event.deltaTime != 0) {
						channel.increaseTimeBefore(event.deltaTime, currentTime)
					}
					channel.beforeEvent(currentTime)
					when (info) {
						is Midi.ChannelEvent.Controller -> channel.controller(info.controller, info.value)
						is Midi.ChannelEvent.NoteOnOff -> channel.noteOnOff(info.on, info.note, info.velocity)
						is Midi.ChannelEvent.Program -> channel.program(info.program)
						is Midi.ChannelEvent.Pressure -> channel.pressure(info.pressure)
						else -> {
							println(event)
						}
					}
					channel.afterEvent(currentTime)
					if (event.deltaTime != 0) {
						channel.increaseTimeAfter(event.deltaTime, currentTime)
					}
				}
				is Midi.SysexEvent -> {
					println(event)
				}
			}
		}
	}

	inner class ChannelInterpreter(val channel0: Int) {
		val channel1 = channel0 + 1
		var lastTime = 0
		var program = 0
		var controller = 0
		var controllerValue = 0
		var note = 0
		var velocity = 0
		var pressure = 0
		var on = false

		val channelData = arrayListOf<Float>()

		fun increaseTimeBefore(deltaTime: Int, time: Int) {
			generateChunk(lastTime, time)
			lastTime = time
		}

		fun increaseTimeAfter(deltaTime: Int, time: Int) {
			//generateChunk(lastTime, time)
			//lastTime = time
		}

		fun beforeEvent(time: Int) {
		}

		fun afterEvent(time: Int) {
		}

		fun controller(controller: Int, value: Int) {
			this.controller = controller
			this.controllerValue = value
		}

		fun noteOnOff(on: Boolean, note: Int, velocity: Int) {
			this.on = on
			this.note = note
			this.velocity = velocity
			//println("CHANNEL[$index1][$on] $note -- $velocity :: ${midi.NoteInfo.toString(note)}")
		}

		fun pressure(pressure: Int) {
			this.pressure = pressure
		}

		fun program(program: Int) {
			this.program = program
		}

		fun generateChunk(from: Int, to: Int) {
			val patch = font[program]
			val instrument = Midi.Instrument.instruments[program]
			println(
				"CHUNK[$channel1][$from -> $to] on=$on, program=$program($instrument), controller=$controller, controllerValue=$controllerValue, note=$note(${Midi.Notes.toString(
					note
				)}), velocity=$velocity, pressure=$pressure"
			)
			for (n in from until to) {
				emit(channel1, patch.getSample(note, n))
			}
		}
	}
}
