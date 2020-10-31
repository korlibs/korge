package com.soywiz.korau.mod

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import kotlin.math.*
import kotlin.random.Random

/*
  fast tracker 2 module player for web audio api
  (c) 2015-2017 firehawk/tda  (firehawk@haxor.fi)
  reading material:
  - ftp://ftp.modland.com/pub/documents/format_documentation/FastTracker%202%20v2.04%20(.xm).html
  - http://sid.ethz.ch/debian/milkytracker/milkytracker-0.90.85%2Bdfsg/resources/reference/xm-form.txt
  - ftp://ftp.modland.com/pub/documents/format_documentation/Tracker%20differences%20for%20Coders.txt
  - http://wiki.openmpt.org/Manual:_Compatible_Playback
  greets to guru, alfred and ccr for their work figuring out the .xm format. :)
*/

suspend fun VfsFile.readXM() = Fasttracker().apply { parse(UByteArrayInt(readAll())) }

@Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate", "FunctionName")
class Fasttracker {
	companion object {
		const val MAX_CHANNELS = 64

		// amiga period value table
		val periodtable = floatArrayOf(
			//ft -8     -7     -6     -5     -4     -3     -2     -1
			//    0      1      2      3      4      5      6      7
			907f, 900f, 894f, 887f, 881f, 875f, 868f, 862f,  // B-3
			856f, 850f, 844f, 838f, 832f, 826f, 820f, 814f,  // C-4
			808f, 802f, 796f, 791f, 785f, 779f, 774f, 768f,  // C#4
			762f, 757f, 752f, 746f, 741f, 736f, 730f, 725f,  // D-4
			720f, 715f, 709f, 704f, 699f, 694f, 689f, 684f,  // D#4
			678f, 675f, 670f, 665f, 660f, 655f, 651f, 646f,  // E-4
			640f, 636f, 632f, 628f, 623f, 619f, 614f, 610f,  // F-4
			604f, 601f, 597f, 592f, 588f, 584f, 580f, 575f,  // F#4
			570f, 567f, 563f, 559f, 555f, 551f, 547f, 543f,  // G-4
			538f, 535f, 532f, 528f, 524f, 520f, 516f, 513f,  // G#4
			508f, 505f, 502f, 498f, 494f, 491f, 487f, 484f,  // A-4
			480f, 477f, 474f, 470f, 467f, 463f, 460f, 457f,  // A#4
			453f, 450f, 447f, 445f, 442f, 439f, 436f, 433f,  // B-4
			428f
		)

		// calc tables for vibrato waveforms
		val vibratotable = Array(4) { t ->
			FloatArray(64) { i ->
				when (t) {
					0 -> (127f * sin(PI * 2 * (i / 64))).toFloat()
					1 -> (127f - 4f * i)
					2 -> if (i < 32) 127f else -127f
					3 -> ((1 - 2 * Random.nextDouble()) * 127).toFloat()
					else -> invalidOp
				}
			}
		}
	}

	var playing = false
	var paused = false
	var repeat = false

	var filter = false

	var syncqueue = IntDeque()

	var samplerate = 44100
	var ramplen = 64.0

	var mixval = 8.0


	val pan = FloatArray(MAX_CHANNELS) { 0.5f }
	val finalpan = FloatArray(MAX_CHANNELS) { 0.5f }

	val voleffects_t0 = arrayOf(
		this::effect_vol_t0_f0, this::effect_vol_t0_60, this::effect_vol_t0_70, this::effect_vol_t0_80,
		this::effect_vol_t0_90, this::effect_vol_t0_a0, this::effect_vol_t0_b0, this::effect_vol_t0_c0,
		this::effect_vol_t0_d0, this::effect_vol_t0_e0
	)
	// volume column effect jumptable for 0x50..0xef
	val voleffects_t1 = arrayOf(
		this::effect_vol_t1_f0, this::effect_vol_t1_60, this::effect_vol_t1_70, this::effect_vol_t1_80,
		this::effect_vol_t1_90, this::effect_vol_t1_a0, this::effect_vol_t1_b0, this::effect_vol_t1_c0,
		this::effect_vol_t1_d0, this::effect_vol_t1_e0
	)
	// effect jumptables for tick 0 and ticks 1..f
	val effects_t0 = arrayOf(
		this::effect_t0_0,
		this::effect_t0_1,
		this::effect_t0_2,
		this::effect_t0_3,
		this::effect_t0_4,
		this::effect_t0_5,
		this::effect_t0_6,
		this::effect_t0_7,
		this::effect_t0_8,
		this::effect_t0_9,
		this::effect_t0_a,
		this::effect_t0_b,
		this::effect_t0_c,
		this::effect_t0_d,
		this::effect_t0_e,
		this::effect_t0_f,
		this::effect_t0_g,
		this::effect_t0_h,
		this::effect_t0_i,
		this::effect_t0_j,
		this::effect_t0_k,
		this::effect_t0_l,
		this::effect_t0_m,
		this::effect_t0_n,
		this::effect_t0_o,
		this::effect_t0_p,
		this::effect_t0_q,
		this::effect_t0_r,
		this::effect_t0_s,
		this::effect_t0_t,
		this::effect_t0_u,
		this::effect_t0_v,
		this::effect_t0_w,
		this::effect_t0_x,
		this::effect_t0_y,
		this::effect_t0_z
	)
	val effects_t0_e = arrayOf(
		this::effect_t0_e0,
		this::effect_t0_e1,
		this::effect_t0_e2,
		this::effect_t0_e3,
		this::effect_t0_e4,
		this::effect_t0_e5,
		this::effect_t0_e6,
		this::effect_t0_e7,
		this::effect_t0_e8,
		this::effect_t0_e9,
		this::effect_t0_ea,
		this::effect_t0_eb,
		this::effect_t0_ec,
		this::effect_t0_ed,
		this::effect_t0_ee,
		this::effect_t0_ef
	)
	val effects_t1 = arrayOf(
		this::effect_t1_0,
		this::effect_t1_1,
		this::effect_t1_2,
		this::effect_t1_3,
		this::effect_t1_4,
		this::effect_t1_5,
		this::effect_t1_6,
		this::effect_t1_7,
		this::effect_t1_8,
		this::effect_t1_9,
		this::effect_t1_a,
		this::effect_t1_b,
		this::effect_t1_c,
		this::effect_t1_d,
		this::effect_t1_e,
		this::effect_t1_f,
		this::effect_t1_g,
		this::effect_t1_h,
		this::effect_t1_i,
		this::effect_t1_j,
		this::effect_t1_k,
		this::effect_t1_l,
		this::effect_t1_m,
		this::effect_t1_n,
		this::effect_t1_o,
		this::effect_t1_p,
		this::effect_t1_q,
		this::effect_t1_r,
		this::effect_t1_s,
		this::effect_t1_t,
		this::effect_t1_u,
		this::effect_t1_v,
		this::effect_t1_w,
		this::effect_t1_x,
		this::effect_t1_y,
		this::effect_t1_z
	)
	val effects_t1_e = arrayOf(
		this::effect_t1_e0,
		this::effect_t1_e1,
		this::effect_t1_e2,
		this::effect_t1_e3,
		this::effect_t1_e4,
		this::effect_t1_e5,
		this::effect_t1_e6,
		this::effect_t1_e7,
		this::effect_t1_e8,
		this::effect_t1_e9,
		this::effect_t1_ea,
		this::effect_t1_eb,
		this::effect_t1_ec,
		this::effect_t1_ed,
		this::effect_t1_ee,
		this::effect_t1_ef
	)
	var title = ""
	var signature = ""
	var trackerversion = 0x0104

	var songlen = 1
	var repeatpos = 0

	var channels = 0
	var patterns = 0
	var instruments = 32

	var amigaperiods = 0

	var initSpeed = 6
	var initBPM = 125

	init {
		this.clearsong()
		this.initialize()
	}

	var pattern = Array<UByteArrayInt>(0) { UByteArrayInt(0) }
	var patternlen = IntArray(0)
	var chvu = DoubleArray(2)

	class Sample(
		var bits: Int = 8,
		var stereo: Int = 0,
		var bps: Int = 1,
		var length: Int = 0,
		var loopstart: Int = 0,
		var looplength: Int = 0,
		var loopend: Int = 0,
		var looptype: Int = 0,
		var volume: Int = 64,
		var finetune: Int = 0,
		var relativenote: Int = 0,
		var panning: Int = 128,
		var name: String = "",
		var data: FloatArray = FloatArray(0)
	)

	class Instrument(
		var name: String = "",
		var sample: Array<Sample> = arrayOf()
	) {
		var samples = 0
		var samplemap = UByteArrayInt(96)
		var volenv = FloatArray(325)
		var panenv = FloatArray(325)
		var voltype = 0
		var pantype = 0
		var pansustain = 0
		var panloopstart = 0
		var panloopend = 0
		var panenvlen = 0

		var volsustain = 0.0
		var volenvlen = 0
		var volloopstart = 0
		var volloopend = 0

		var vibratotype = 0
		var vibratosweep = 0
		var vibratodepth = 0
		var vibratorate = 0
		var volfadeout = 0
	}

	var instrument = Array(instruments) { Instrument() }
	var patterntable = UByteArrayInt(256)

	// clear song data
	fun clearsong() {
		title = ""
		signature = ""
		trackerversion = 0x0104

		songlen = 1
		repeatpos = 0

		channels = 0
		patterns = 0
		instruments = 32

		amigaperiods = 0

		initSpeed = 6
		initBPM = 125

		pattern = Array<UByteArrayInt>(0) { UByteArrayInt(0) }

		patterntable = UByteArrayInt(256)

		this.instrument = Array(this.instruments) { Instrument() }

		this.chvu = DoubleArray(2)
	}

	class Channel {
		var instrument = 0
		var sampleindex = 0

		var note = 36
		var command = 0
		var data = 0
		var samplepos = 0.0
		var samplespeed = 0.0
		var flags = 0
		var noteon = false

		var volslide = 0
		var slidespeed = 0
		var slideto = 0.0
		var slidetospeed = 0
		var arpeggio = 0

		var period = 640.0
		var frequency = 8363

		var volume = 64
		var voiceperiod = 0.0
		var voicevolume = 0
		var finalvolume = 0.0

		var semitone = 12
		var vibratospeed = 0
		var vibratodepth = 0
		var vibratopos = 0
		var vibratowave = 0

		var volenvpos = 0
		var panenvpos = 0
		var fadeoutpos = 0

		var playdir = 1

		// interpolation/ramps
		var volramp = 0.0
		var volrampfrom = 0.0
		var trigramp = 0.0
		var trigrampfrom = 0.0
		var currentsample = 0.0
		var lastsample = 0.0
		var oldfinalvolume = 0.0

		var slidedownspeed = 0.0
		var slideupspeed = 0.0
	}

	var tick = -1
	var position = 0
	var row = 0
	var flags = 0

	var volume = 64

	var speed = this.initSpeed
	var bpm = this.initBPM
	var stt = 0 //this.samplerate/(this.bpm*0.4);
	var breakrow = 0
	var patternjump = 0
	var patterndelay = 0
	var patternwait = 0
	var endofsong = false
	var looprow = 0
	var loopstart = 0
	var loopcount = 0

	var globalvolslide = 0

	var channel = arrayOf<Channel>()

	// initialize all player variables to defaults prior to starting playback
	fun initialize() {
		this.syncqueue = IntDeque()

		this.tick = -1
		this.position = 0
		this.row = 0
		this.flags = 0

		this.volume = 64
		if (this.initSpeed != 0) this.speed = this.initSpeed
		if (this.initBPM != 0) this.bpm = this.initBPM
		this.stt = 0 //this.samplerate/(this.bpm*0.4);
		this.breakrow = 0
		this.patternjump = 0
		this.patterndelay = 0
		this.patternwait = 0
		this.endofsong = false
		this.looprow = 0
		this.loopstart = 0
		this.loopcount = 0

		this.globalvolslide = 0

		this.channel = Array<Channel>(this.channels) {
			Channel().apply {
				this.instrument = 0
				this.sampleindex = 0

				this.note = 36
				this.command = 0
				this.data = 0
				this.samplepos = 0.0
				this.samplespeed = 0.0
				this.flags = 0
				this.noteon = false

				this.volslide = 0
				this.slidespeed = 0
				this.slideto = 0.0
				this.slidetospeed = 0
				this.arpeggio = 0

				this.period = 640.0
				this.frequency = 8363

				this.volume = 64
				this.voiceperiod = 0.0
				this.voicevolume = 0
				this.finalvolume = 0.0

				this.semitone = 12
				this.vibratospeed = 0
				this.vibratodepth = 0
				this.vibratopos = 0
				this.vibratowave = 0

				this.volenvpos = 0
				this.panenvpos = 0
				this.fadeoutpos = 0

				this.playdir = 1

				// interpolation/ramps
				this.volramp = 0.0
				this.volrampfrom = 0.0
				this.trigramp = 0.0
				this.trigrampfrom = 0.0
				this.currentsample = 0.0
				this.lastsample = 0.0
				this.oldfinalvolume = 0.0
			}
		}
	}


	// parse the module from local buffer
	fun parse(buffer: UByteArrayInt): Boolean {
		var j: Int
		var k: Int
		var c: Int
		var offset: Int
		var datalen: Int
		var hdrlen: Int

		//if (buffer != null) return false

		// check xm signature, type and tracker version
		for (i in 0 until 17) this.signature += buffer[i].toChar()
		if (this.signature != "Extended Module: ") return false
		if (buffer[37] != 0x1a) return false
		this.signature = "X.M."
		this.trackerversion = le_word(buffer, 58)
		if (this.trackerversion < 0x0104) return false // older versions not currently supported

		// song title
		run {
			var i = 0
			while (buffer[i] != 0 && i < 20) this.title += dos2utf(buffer[17 + i++])
		}

		offset = 60
		hdrlen = le_dword(buffer, offset)
		this.songlen = le_word(buffer, offset + 4)
		this.repeatpos = le_word(buffer, offset + 6)
		this.channels = le_word(buffer, offset + 8)
		this.patterns = le_word(buffer, offset + 10)
		this.instruments = le_word(buffer, offset + 12)

		this.amigaperiods = if (le_word(buffer, offset + 14) == 0) 1 else 0

		this.initSpeed = le_word(buffer, offset + 16)
		this.initBPM = le_word(buffer, offset + 18)

		var maxpatt = 0
		for (n in 0 until 256) {
			this.patterntable[n] = buffer[offset + 20 + n]
			if (this.patterntable[n] > maxpatt) maxpatt = this.patterntable[n]
		}
		maxpatt++

		// allocate arrays for pattern data
		this.pattern = Array(maxpatt) { UByteArrayInt(0) }
		this.patternlen = IntArray(maxpatt)

		for (n in 0 until maxpatt) {
			// initialize the pattern to defaults prior to unpacking
			this.patternlen[n] = 64
			this.pattern[n] = UByteArrayInt(this.channels * this.patternlen[n] * 5)
			val pat = this.pattern[n]
			for (row in 0 until this.patternlen[n]) for (ch in 0 until this.channels) {
				pat[row * this.channels * 5 + ch * 5 + 0] = 255 // note (255=no note)
				pat[row * this.channels * 5 + ch * 5 + 1] = 0 // instrument
				pat[row * this.channels * 5 + ch * 5 + 2] = 255 // volume
				pat[row * this.channels * 5 + ch * 5 + 3] = 255 // command
				pat[row * this.channels * 5 + ch * 5 + 4] = 0 // parameter
			}
		}

		// load and unpack patterns
		offset += hdrlen // initial offset for patterns
		for (i in 0 until this.patterns) {
			this.patternlen[i] = le_word(buffer, offset + 5)
			this.pattern[i] = UByteArrayInt(this.channels * this.patternlen[i] * 5)

			// initialize pattern to defaults prior to unpacking
			val pat = this.pattern[i]
			for (n in 0 until (this.patternlen[i] * this.channels)) {
				pat[n * 5 + 0] = 0 // note
				pat[n * 5 + 1] = 0 // instrument
				pat[n * 5 + 2] = 0 // volume
				pat[n * 5 + 3] = 0 // command
				pat[n * 5 + 4] = 0 // parameter
			}

			datalen = le_word(buffer, offset + 7)
			offset += le_dword(buffer, offset) // jump over header
			j = 0
			k = 0
			while (j < datalen) {
				c = buffer[offset + j++]
				if ((c and 128) != 0) {
					// first byte is a bitmask
					if ((c and 1) != 0) pat[k + 0] = buffer[offset + j++]
					if ((c and 2) != 0) pat[k + 1] = buffer[offset + j++]
					if ((c and 4) != 0) pat[k + 2] = buffer[offset + j++]
					if ((c and 8) != 0) pat[k + 3] = buffer[offset + j++]
					if ((c and 16) != 0) pat[k + 4] = buffer[offset + j++]
				} else {
					// first byte is note -> all columns present sequentially
					pat[k + 0] = c
					pat[k + 1] = buffer[offset + j++]
					pat[k + 2] = buffer[offset + j++]
					pat[k + 3] = buffer[offset + j++]
					pat[k + 4] = buffer[offset + j++]
				}
				k += 5
			}

			for (n in 0 until (this.patternlen[i] * this.channels * 5) step 5) {
				// remap note to st3-style, 255=no note, 254=note off
				pat[n + 0] = when {
					pat[n + 0] >= 97 -> 254
					pat[n + 0] == 0 -> 255
					else -> pat[n + 0] - 1
				}

				// command 255=no command
				if (pat[n + 3] == 0 && pat[n + 4] == 0) pat[n + 3] = 255

				// remap volume column setvol to 0x00..0x40, tone porta to 0x50..0x5f and 0xff for nop
				when {
					pat[n + 2] < 0x10 -> pat[n + 2] = 0xff
					pat[n + 2] in 0x10..0x50 -> pat[n + 2] -= 0x10
					pat[n + 2] >= 0xf0 -> pat[n + 2] -= 0xa0
				}
			}

			// unpack next pattern
			offset += j
		}
		this.patterns = maxpatt

		// instruments
		this.instrument = Array(this.instruments) { Instrument() }
		for (i in 0 until this.instruments) {
			hdrlen = le_dword(buffer, offset)
			val ins = this.instrument[i]
			ins.sample = arrayOf()
			ins.name = ""
			j = 0
			while ((buffer[offset + 4 + j] != 0) && j < 22) ins.name += dos2utf(buffer[offset + 4 + j++])
			ins.samples = le_word(buffer, offset + 27)

			// initialize to defaults
			ins.samplemap = UByteArrayInt(96)
			for (n in 0 until 96) ins.samplemap[n] = 0
			ins.volenv = FloatArray(325)
			ins.panenv = FloatArray(325)
			ins.voltype = 0
			ins.pantype = 0
			ins.sample = (0 until ins.samples).map {
				Sample(
					bits = 8, stereo = 0, bps = 1,
					length = 0, loopstart = 0, looplength = 0, loopend = 0, looptype = 0,
					volume = 64, finetune = 0, relativenote = 0, panning = 128, name = "",
					data = FloatArray(0)
				)
			}.toTypedArray()

			if (ins.samples != 0) {
				val smphdrlen = le_dword(buffer, offset + 29)

				for (n in 0 until 96) ins.samplemap[n] = buffer[offset + 33 + n]

				// envelope points. the xm specs say 48 bytes per envelope, but while that may
				// technically be correct, what they don't say is that it means 12 pairs of
				// little endian words. first word is the x coordinate, second is y. point
				// 0 always has x=0.
				val tmp_volenv = Array(12) { IntArray(0) }
				val tmp_panenv = Array(12) { IntArray(0) }
				for (n in 0 until 12) {
					tmp_volenv[n] =
							intArrayOf(le_word(buffer, offset + 129 + n * 4), le_word(buffer, offset + 129 + n * 4 + 2))
					tmp_panenv[n] =
							intArrayOf(le_word(buffer, offset + 177 + n * 4), le_word(buffer, offset + 177 + n * 4 + 2))
				}

				// are envelopes enabled?
				ins.voltype = buffer[offset + 233] // 1=enabled, 2=sustain, 4=loop
				ins.pantype = buffer[offset + 234]

				// pre-interpolate the envelopes to arrays of [0..1] float32 values which
				// are stepped through at a rate of one per tick. max tick count is 0x0144.

				// volume envelope
				for (n in 0 until 325) ins.volenv[n] = 1f
				if ((ins.voltype and 1) != 0) {
					for (n in 0 until 325) {
						var p = 1
						var delta: Int
						while (tmp_volenv[p][0] < n && p < 11) p++
						delta = if (tmp_volenv[p][0] == tmp_volenv[p - 1][0]) {
							0
						} else {
							(tmp_volenv[p][1] - tmp_volenv[p - 1][1]) / (tmp_volenv[p][0] - tmp_volenv[p - 1][0])
						}
						ins.volenv[n] =
								((tmp_volenv[p - 1][1] + delta * (n - tmp_volenv[p - 1][0])) / 64.0).toFloat()
					}
					ins.volenvlen = tmp_volenv[max(0, buffer[offset + 225] - 1)][0]
					ins.volsustain = tmp_volenv[buffer[offset + 227]][0].toDouble()
					ins.volloopstart = tmp_volenv[buffer[offset + 228]][0]
					ins.volloopend = tmp_volenv[buffer[offset + 229]][0]
				}

				// pan envelope
				for (n in 0 until 325) ins.panenv[n] = 0.5f
				if ((ins.pantype and 1) != 0) {
					for (n in 0 until 325) {
						var p = 1
						while (tmp_panenv[p][0] < n && p < 11) p++
						val delta =
							if (tmp_panenv[p][0] == tmp_panenv[p - 1][0]) 0 else (tmp_panenv[p][1] - tmp_panenv[p - 1][1]) / (tmp_panenv[p][0] - tmp_panenv[p - 1][0])
						ins.panenv[n] = ((tmp_panenv[p - 1][1] + delta * (n - tmp_panenv[p - 1][0])) / 64.0).toFloat()
					}
					ins.panenvlen = tmp_panenv[max(0, buffer[offset + 226] - 1)][0]
					ins.pansustain = tmp_panenv[buffer[offset + 230]][0]
					ins.panloopstart = tmp_panenv[buffer[offset + 231]][0]
					ins.panloopend = tmp_panenv[buffer[offset + 232]][0]
				}

				// vibrato
				ins.vibratotype = buffer[offset + 235]
				ins.vibratosweep = buffer[offset + 236]
				ins.vibratodepth = buffer[offset + 237]
				ins.vibratorate = buffer[offset + 238]

				// volume fade out
				ins.volfadeout = le_word(buffer, offset + 239)

				// sample headers
				offset += hdrlen
				ins.sample = Array(ins.samples) { Sample() }
				for (n in 0 until ins.samples) {
					datalen = le_dword(buffer, offset + 0)

					ins.sample[n] = Sample()
					val sample = ins.sample[n]
					sample.bits = if ((buffer[offset + 14] and 16) != 0) 16 else 8
					sample.stereo = 0
					sample.bps = if (sample.bits == 16) 2 else 1 // bytes per sample

					// sample length and loop points are in BYTES even for 16-bit samples!
					sample.length = datalen / sample.bps
					sample.loopstart = le_dword(buffer, offset + 4) / sample.bps
					sample.looplength = le_dword(buffer, offset + 8) / sample.bps
					sample.loopend = sample.loopstart + sample.looplength
					sample.looptype = buffer[offset + 14] and 0x03
					sample.volume = buffer[offset + 12]

					// finetune and seminote tuning
					sample.finetune =
							(if (buffer[offset + 13] < 128) buffer[offset + 13] else buffer[offset + 13] - 256)
					sample.relativenote =
							(if (buffer[offset + 16] < 128) buffer[offset + 16] else buffer[offset + 16] - 256)

					sample.panning = buffer[offset + 15]

					k = 0; sample.name = ""
					while ((buffer[offset + 18 + k] != 0) && k < 22) sample.name += dos2utf(buffer[offset + 18 + k++])

					offset += smphdrlen
				}

				// sample data (convert to signed float32)
				for (n in 0 until ins.samples) {
					ins.sample[n].data = FloatArray(ins.sample[n].length)
					c = 0
					if (ins.sample[n].bits == 16) {
						for (m in 0 until ins.sample[n].length) {
							c += s_le_word(buffer, offset + m * 2)
							if (c < -32768) c += 65536
							if (c > 32767) c -= 65536
							ins.sample[n].data[m] = (c / 32768.0).toFloat()
						}
					} else {
						for (n in 0 until ins.sample[n].length) {
							c += s_byte(buffer, offset + n)
							if (c < -128) c += 256
							if (c > 127) c -= 256
							ins.sample[n].data[n] = (c / 128.0).toFloat()
						}
					}
					offset += ins.sample[n].length * ins.sample[n].bps
				}
			} else {
				offset += hdrlen
			}
		}

		this.mixval = 4.0 - 2.0 * (this.channels / 32.0)
		this.chvu = DoubleArray(this.channels) { 0.0 }

		return true
	}


	// calculate period value for note
	fun calcperiod(note: Int, finetune: Double): Double {
		if (this.amigaperiods != 0) {
			val ft: Double = (finetune / 16.0).toInt().toDouble() // = -8 .. 7
			val p1 = Fasttracker.periodtable[8 + (note % 12) * 8 + ft.toInt()]
			val p2 = Fasttracker.periodtable[8 + (note % 12) * 8 + ft.toInt() + 1]
			val ft2 = (finetune / 16.0) - ft
			return ((1.0 - ft2) * p1 + ft2 * p2) * (16.0 / 2.0.pow((note / 12) - 1))
		} else {
			return 7680.0 - note * 64.0 - finetune / 2
		}
	}


	// advance player by a tick
	fun advance() {
		this.stt = (((125.0 / this.bpm) * (1 / 50.0) * this.samplerate).toFloat()).toInt() // 50Hz

		// advance player
		this.tick++
		this.flags = this.flags or 1

		// new row on this tick?
		if (this.tick >= this.speed) {
			if (this.patterndelay != 0) { // delay pattern
				if (this.tick < ((this.patternwait + 1) * this.speed)) {
					this.patternwait++
				} else {
					this.row++; this.tick = 0; this.flags = this.flags or 2; this.patterndelay = 0
				}
			} else {
				if ((this.flags and (16 + 32 + 64)) != 0) {
					if ((this.flags and 64) != 0) {
						// loop pattern?
						this.row = this.looprow
						this.flags = this.flags and 0xa1
						this.flags = this.flags or 2
					} else {
						if ((this.flags and 16) != 0) {
							// pattern jump/break?
							this.position = this.patternjump
							this.row = this.breakrow
							this.patternjump = 0
							this.breakrow = 0
							this.flags = this.flags and 0xe1
							this.flags = this.flags or 2
						}
					}
					this.tick = 0
				} else {
					this.row++; this.tick = 0; this.flags = this.flags or 2
				}
			}
		}

		// step to new pattern?
		if (this.row >= this.patternlen[this.patterntable[this.position]]) {
			this.position++
			this.row = 0
			this.flags = this.flags or 4
		}

		// end of song?
		if (this.position >= this.songlen) {
			if (this.repeat) {
				this.position = 0
			} else {
				this.endofsong = true
			}
			return
		}
	}


	// process one channel on a row in pattern p, pp is an offset to pattern data
	fun process_note(p: Int, ch: Int) {
		var s: Int
		val v: Int
		val pp: Int = this.row * 5 * this.channels + ch * 5
		val pv: Double

		val n = this.pattern[p][pp]
		var i = this.pattern[p][pp + 1]
		val chan = this.channel[ch]
		if ((i != 0) && i <= this.instrument.size) {
			chan.instrument = i - 1

			if (this.instrument[i - 1].samples != 0) {
				s = this.instrument[i - 1].samplemap[chan.note]
				chan.sampleindex = s
				chan.volume = this.instrument[i - 1].sample[s].volume
				chan.playdir = 1 // fixes crash in respirator.xm pos 0x12

				// set pan from sample
				this.pan[ch] = (this.instrument[i - 1].sample[s].panning / 255.0).toFloat()
			}
			chan.voicevolume = chan.volume
		}
		i = chan.instrument

		if (n < 254) {
			// look up the sample
			s = this.instrument[i].samplemap[n]
			chan.sampleindex = s

			val rn = n + this.instrument[i].sample[s].relativenote

			// calc period for note
			pv = this.calcperiod(rn, this.instrument[i].sample[s].finetune.toDouble())

			if (chan.noteon) {
				// retrig note, except if command=0x03 (porta to note) or 0x05 (porta+volslide)
				if ((chan.command != 0x03) && (chan.command != 0x05)) {
					chan.note = n
					chan.period = pv
					chan.voiceperiod = chan.period
					chan.flags = chan.flags or 3 // force sample speed recalc
					chan.trigramp = 0.0
					chan.trigrampfrom = chan.currentsample
					chan.samplepos = 0.0
					chan.playdir = 1
					if (chan.vibratowave > 3) chan.vibratopos = 0
					chan.noteon = true
					chan.fadeoutpos = 65535
					chan.volenvpos = 0
					chan.panenvpos = 0
				}
			} else {
				// note is off, restart but don't set period if slide command
				if (this.pattern[p][pp + 1] != 0) { // instrument set on row?
					chan.samplepos = 0.0
					chan.playdir = 1
					if (chan.vibratowave > 3) chan.vibratopos = 0
					chan.noteon = true
					chan.fadeoutpos = 65535
					chan.volenvpos = 0
					chan.panenvpos = 0
					chan.trigramp = 0.0
					chan.trigrampfrom = chan.currentsample
				}
				if ((chan.command != 0x03) && (chan.command != 0x05)) {
					chan.note = n
					chan.period = pv
					chan.voiceperiod = chan.period
					chan.flags = chan.flags or 3 // force sample speed recalc
				}
			}
			// in either case, set the slide to note target to note period
			chan.slideto = pv
		} else if (n == 254) {
			chan.noteon = false // note off
			if ((this.instrument[i].voltype and 1) == 0) chan.voicevolume = 0
		}

		if (this.pattern[p][pp + 2] != 255) {
			v = this.pattern[p][pp + 2]
			if (v <= 0x40) {
				chan.volume = v
				chan.voicevolume = chan.volume
			}
		}
	}


	// advance player and all channels by a tick
	private fun process_tick() {
		// advance global player state by a tick
		this.advance()

		// advance all channels by a tick
		for (ch in 0 until this.channels) {

			// calculate playback position
			val p = this.patterntable[this.position]
			val pp = this.row * 5 * this.channels + ch * 5

			// save old volume if ramping is needed
			val chan = this.channel[ch]
			chan.oldfinalvolume = chan.finalvolume

			if ((this.flags and 2) != 0) {
				// new row on this tick?
				chan.command = this.pattern[p][pp + 3]
				chan.data = this.pattern[p][pp + 4]
				if (!(chan.command == 0x0e && (chan.data and 0xf0) == 0xd0)) {
					// note delay?
					this.process_note(p, ch)
				}
			}
			val i = chan.instrument
			@Suppress("UNUSED_VARIABLE")
			var si = chan.sampleindex

			// kill empty instruments
			val inst = this.instrument[i]
			if (chan.noteon && inst.samples == 0) {
				chan.noteon = false
			}

			// effects
			val v = this.pattern[p][pp + 2]
			if (v in 0x50..239) {
				if (this.tick == 0) this.voleffects_t0[(v shr 4) - 5](ch, v and 0x0f)
				else this.voleffects_t1[(v shr 4) - 5](ch, v and 0x0f)
			}
			if (chan.command < 36) {
				if (this.tick == 0) {
					// process only on tick 0
					this.effects_t0[chan.command](ch)
				} else {
					this.effects_t1[chan.command](ch)
				}
			}

			// recalc sample speed if voiceperiod has changed
			if ((((chan.flags and 1) != 0) || ((this.flags and 2) != 0)) && chan.voiceperiod != 0.0) {
				val f: Double = if (this.amigaperiods != 0) {
					8287.137 * 1712.0 / chan.voiceperiod
				} else {
					8287.137 * 2.0.pow((4608.0 - chan.voiceperiod) / 768.0)
				}
				chan.samplespeed = f / this.samplerate
			}

			// advance vibrato on each new tick
			chan.vibratopos += chan.vibratospeed
			chan.vibratopos = chan.vibratopos and 0x3f

			// advance volume envelope, if enabled (also fadeout)
			if ((inst.voltype and 1) != 0) {
				chan.volenvpos++

				if ((chan.noteon) && ((inst.voltype and 2) != 0) && chan.volenvpos >= inst.volsustain) chan.volenvpos =
						inst.volsustain.toInt()
				if (((inst.voltype and 4) != 0) && chan.volenvpos >= inst.volloopend) chan.volenvpos = inst.volloopstart
				if (chan.volenvpos >= inst.volenvlen) chan.volenvpos = inst.volenvlen
				if (chan.volenvpos > 324) chan.volenvpos = 324

				// fadeout if note is off
				if (!chan.noteon && chan.fadeoutpos != 0) {
					chan.fadeoutpos -= inst.volfadeout
					if (chan.fadeoutpos < 0) chan.fadeoutpos = 0
				}
			}

			// advance pan envelope, if enabled
			if ((inst.pantype and 1) != 0) {
				chan.panenvpos++

				if (chan.noteon && ((inst.pantype and 2) != 0) && chan.panenvpos >= inst.pansustain)
					chan.panenvpos = inst.pansustain

				if (((inst.pantype and 4) != 0) && chan.panenvpos >= inst.panloopend)
					chan.panenvpos = inst.panloopstart

				if (chan.panenvpos >= inst.panenvlen) chan.panenvpos = inst.panenvlen

				if (chan.panenvpos > 324) chan.panenvpos = 324
			}

			// calc final volume for channel
			chan.finalvolume = chan.voicevolume * inst.volenv[chan.volenvpos] * chan.fadeoutpos / 65536.0

			// calc final panning for channel

			// Kolin.JS bug:
			//var tmp$_7 = this.finalpan;
			//var tmp$_8 = this.pan[ch];
			//var tmp$_9 = this.instrument[i].panenv[this.channel[ch].panenvpos] - 0.5;
			//var x_0 = this.pan[ch] - 0.5;
			//tmp$_7[ch] = tmp$_8 + tmp$_9 * (0.5 * Math_0.abs(x_0)) * 2.0;
			//if (this.channel[ch].oldfinalvolume !== this.channel[ch].finalvolume) {
			//    this.channel[ch].volrampfrom = this.channel[ch].oldfinalvolume;
			//    this.channel[ch].volramp = 0.0;
			//}

			val panch = this.pan[ch]
			this.finalpan[ch] = (panch + (inst.panenv[chan.panenvpos] - 0.5) * (0.5 * abs(panch - 0.5)) * 2.0).toFloat()

			// setup volramp if voice volume changed
			if (chan.oldfinalvolume != chan.finalvolume) {
				chan.volrampfrom = chan.oldfinalvolume
				chan.volramp = 0.0
			}

			// clear channel flags
			chan.flags = 0
		}

		// clear global flags after all channels are processed
		this.flags = this.flags and 0x70
	}

	private var fl = 0.0
	private var fr = 0.0
	private var fs = 0.0

	// mix a buffer of audio for an audio processing event
	fun mix(lbuf: DoubleArray, rbuf: DoubleArray, buflen: Int) {
		// return a buffer of silence if not playing
		if (this.paused || this.endofsong || !this.playing) {
			for (s in 0 until buflen) {
				lbuf[s] = 0.0
				rbuf[s] = 0.0
				for (ch in 0 until this.chvu.size) this.chvu[ch] = 0.0
			}
			return
		}

		// fill audiobuffer
		val t = this.volume / 64.0
		for (s in 0 until buflen) {
			var outL = 0.0
			var outR = 0.0

			// if STT has run out, step player forward by tick
			if (this.stt <= 0) this.process_tick()

			// mix channels
			for (ch in 0 until this.channel.size) {
				fl = 0.0
				fr = 0.0
				fs = 0.0
				val chh = this.channel[ch]
				val si = chh.sampleindex
				val ins = this.instrument[chh.instrument % this.instrument.size]

				// add channel output to left/right master outputs
				if (chh.noteon || ((ins.voltype and 1) != 0) && (!chh.noteon) && (chh.fadeoutpos != 0) || (!chh.noteon) && chh.volramp < 1.0) {
					val smpl = ins.sample[si]
					if (smpl.length > chh.samplepos) {
						handle2LenPos(chh, smpl, ch)
					}
					outL += fl
					outR += fr

					val oldpos = chh.samplepos
					chh.samplepos += (chh.playdir * chh.samplespeed)
					if (chh.samplepos.compareTo(oldpos) == chh.playdir) chh.lastsample = fs
					handleLoop(chh, smpl)

				} else {
					chh.currentsample = 0.0 // note is completely off
				}
				//this.chvu[ch] = max(this.chvu[ch], abs(fl + fr)) // @TODO: USED?
			}

			// done - store to output buffer
			lbuf[s] = outL * t
			rbuf[s] = outR * t
			this.stt--
		}
	}

	private fun handle2LenPos(chh: Channel, smpl: Sample, ch: Int) {
		fl = chh.lastsample

		// interpolate towards current sample
		val _v1 = chh.samplepos
		fs = smpl.data[_v1.toInt()].toDouble()
		val _v1a = chh.samplepos - _v1
		val _v1b = if (chh.playdir < 0) (1.0 - _v1a) else _v1a
		fl = _v1b * fs + (1.0 - _v1b) * fl

		// smooth out discontinuities from retrig and sample offset
		val _v2 = chh.trigramp
		fl = _v2 * fl + (1.0 - _v2) * chh.trigrampfrom
		val _v2a = _v2 + (1.0 / 128.0)
		chh.trigramp = if (_v2a <= 1.0) _v2a else 1.0
		chh.currentsample = fl

		// ramp volume changes over 64 samples to avoid clicks
		fr = fl * (chh.finalvolume / 64.0)
		val _v3 = chh.volramp
		fl = _v3 * fr + (1.0 - _v3) * (fl * (chh.volrampfrom / 64.0))
		val _v3a = _v3 + (1.0 / 64.0)
		chh.volramp = if (_v3a <= 1.0) _v3a else 1.0

		// pan samples, if envelope is disabled panvenv is always 0.5
		val _v4 = this.finalpan[ch].toDouble()
		fr = fl * _v4
		fl *= 1.0 - _v4
	}

	private fun handleLoop(chh: Channel, smpl: Sample) {
		when (smpl.looptype) {
			2 -> { // pingpong loop
				if (chh.playdir == -1) {
					// bounce off from start?
					if (chh.samplepos <= smpl.loopstart) {
						chh.samplepos += (smpl.loopstart - chh.samplepos)
						chh.playdir = 1
						chh.lastsample = chh.currentsample
					}
				} else {
					// bounce off from end?
					if (chh.samplepos >= smpl.loopend) {
						chh.samplepos -= (chh.samplepos - smpl.loopend)
						chh.playdir = -1
						chh.lastsample = chh.currentsample
					}
				}
			}
			0 -> { // no loop
				if (chh.samplepos >= smpl.length) {
					chh.noteon = false
				}
			}
			else -> { // normal loop
				if (chh.samplepos >= smpl.loopend) {
					chh.samplepos -= smpl.looplength
					chh.lastsample = chh.currentsample
				}
			}
		}
	}

	//
	// volume column effect functions
	//
	private fun effect_vol_t0_60(ch: Int, data: Int) { // 60-6f vol slide down
	}

	private fun effect_vol_t0_70(ch: Int, data: Int) { // 70-7f vol slide up
	}

	private fun effect_vol_t0_80(ch: Int, data: Int) { // 80-8f fine vol slide down
		this.channel[ch].voicevolume -= data
		if (this.channel[ch].voicevolume < 0) this.channel[ch].voicevolume = 0
	}

	private fun effect_vol_t0_90(ch: Int, data: Int) { // 90-9f fine vol slide up
		this.channel[ch].voicevolume += data
		if (this.channel[ch].voicevolume > 64) this.channel[ch].voicevolume = 64
	}

	private fun effect_vol_t0_a0(ch: Int, data: Int) { // a0-af set vibrato speed
		this.channel[ch].vibratospeed = data
	}

	private fun effect_vol_t0_b0(ch: Int, data: Int) { // b0-bf vibrato
		if (data != 0) this.channel[ch].vibratodepth = data
		this.effect_t1_4(ch)
	}

	private fun effect_vol_t0_c0(ch: Int, data: Int) { // c0-cf set panning
		this.pan[ch] = ((data and 0x0f) / 15.0).toFloat()
	}

	private fun effect_vol_t0_d0(ch: Int, data: Int) { // d0-df panning slide left
	}

	private fun effect_vol_t0_e0(ch: Int, data: Int) { // e0-ef panning slide right
	}

	private fun effect_vol_t0_f0(ch: Int, data: Int) { // f0-ff tone porta
//  if (data) this.channel[ch].slidetospeed=data;
//  if (!this.amigaperiods) this.channel[ch].slidetospeed*=4;
	}

	//////
	private fun effect_vol_t1_60(ch: Int, data: Int) { // 60-6f vol slide down
		this.channel[ch].voicevolume -= data
		if (this.channel[ch].voicevolume < 0) this.channel[ch].voicevolume = 0
	}

	private fun effect_vol_t1_70(ch: Int, data: Int) { // 70-7f vol slide up
		this.channel[ch].voicevolume += data
		if (this.channel[ch].voicevolume > 64) this.channel[ch].voicevolume = 64
	}

	private fun effect_vol_t1_80(ch: Int, data: Int) = Unit // 80-8f fine vol slide down
	private fun effect_vol_t1_90(ch: Int, data: Int) = Unit // 90-9f fine vol slide up
	private fun effect_vol_t1_a0(ch: Int, data: Int) = Unit // a0-af set vibrato speed
	private fun effect_vol_t1_b0(ch: Int, data: Int) =
		this.effect_t1_4(ch) // b0-bf vibrato // same as effect column vibrato on ticks 1+

	private fun effect_vol_t1_c0(ch: Int, data: Int) = Unit // c0-cf set panning
	private fun effect_vol_t1_d0(ch: Int, data: Int) = Unit // d0-df panning slide left
	private fun effect_vol_t1_e0(ch: Int, data: Int) = Unit // e0-ef panning slide right
	private fun effect_vol_t1_f0(ch: Int, data: Int) = Unit // f0-ff tone porta //  this.effect_t1_3(mod, ch);


	//
	// tick 0 effect functions
	//
	private fun effect_t0_0(ch: Int) = run { this.channel[ch].arpeggio = this.channel[ch].data }// 0 arpeggio

	private fun effect_t0_1(ch: Int) { // 1 slide up
		if (this.channel[ch].data != 0) this.channel[ch].slideupspeed = (this.channel[ch].data * 4).toDouble()
	}

	private fun effect_t0_2(ch: Int) { // 2 slide down
		if (this.channel[ch].data != 0) this.channel[ch].slidedownspeed = (this.channel[ch].data * 4).toDouble()
	}

	private fun effect_t0_3(ch: Int) { // 3 slide to note
		if (this.channel[ch].data != 0) this.channel[ch].slidetospeed = this.channel[ch].data * 4
	}

	private fun effect_t0_4(ch: Int) { // 4 vibrato
		if ((this.channel[ch].data and 0x0f) != 0 && (this.channel[ch].data and 0xf0) != 0) {
			this.channel[ch].vibratodepth = (this.channel[ch].data and 0x0f)
			this.channel[ch].vibratospeed = (this.channel[ch].data and 0xf0) shr 4
		}
		this.effect_t1_4(ch)
	}

	private fun effect_t0_5(ch: Int) = this.effect_t0_a(ch) // 5
	private fun effect_t0_6(ch: Int) = this.effect_t0_a(ch) // 6
	private fun effect_t0_7(ch: Int) = Unit // 7

	private fun effect_t0_8(ch: Int) { // 8 set panning
		this.pan[ch] = (this.channel[ch].data / 255.0).toFloat()
	}

	private fun effect_t0_9(ch: Int) { // 9 set sample offset
		this.channel[ch].samplepos = (this.channel[ch].data * 256).toDouble()
		this.channel[ch].playdir = 1

		this.channel[ch].trigramp = 0.0
		this.channel[ch].trigrampfrom = this.channel[ch].currentsample
	}

	private fun effect_t0_a(ch: Int) { // a volume slide
		// this behavior differs from protracker!! A00 will slide using previous non-zero parameter.
		if (this.channel[ch].data != 0) this.channel[ch].volslide = this.channel[ch].data
	}

	private fun effect_t0_b(ch: Int) { // b pattern jump
		this.breakrow = 0
		this.patternjump = this.channel[ch].data
		this.flags = this.flags or 16
	}

	private fun effect_t0_c(ch: Int) { // c set volume
		this.channel[ch].voicevolume = this.channel[ch].data
		if (this.channel[ch].voicevolume < 0) this.channel[ch].voicevolume = 0
		if (this.channel[ch].voicevolume > 64) this.channel[ch].voicevolume = 64
	}

	private fun effect_t0_d(ch: Int) { // d pattern break
		this.breakrow = ((this.channel[ch].data and 0xf0) shr 4) * 10 + (this.channel[ch].data and 0x0f)
		if (0 == (this.flags and 16)) this.patternjump = this.position + 1
		this.flags = this.flags or 16
	}

	private fun effect_t0_e(ch: Int) { // e
		val i = (this.channel[ch].data and 0xf0) shr 4
		this.effects_t0_e[i](ch)
	}

	private fun effect_t0_f(ch: Int) { // f set speed
		when {
			this.channel[ch].data > 32 -> this.bpm = this.channel[ch].data
			this.channel[ch].data != 0 -> this.speed = this.channel[ch].data
		}
	}

	private fun effect_t0_g(ch: Int) { // g set global volume
		if (this.channel[ch].data <= 0x40) this.volume = this.channel[ch].data
	}

	private fun effect_t0_h(ch: Int) { // h global volume slide
		if (this.channel[ch].data != 0) this.globalvolslide = this.channel[ch].data
	}

	private fun effect_t0_i(ch: Int) = Unit // i
	private fun effect_t0_j(ch: Int) = Unit // j

	private fun effect_t0_k(ch: Int) { // k key off
		this.channel[ch].noteon = false
		if ((this.instrument[this.channel[ch].instrument].voltype and 1) == 0) this.channel[ch].voicevolume = 0
	}

	private fun effect_t0_l(ch: Int) { // l set envelope position
		this.channel[ch].volenvpos = this.channel[ch].data
		this.channel[ch].panenvpos = this.channel[ch].data
	}

	private fun effect_t0_m(ch: Int) = Unit // m
	private fun effect_t0_n(ch: Int) = Unit // n
	private fun effect_t0_o(ch: Int) = Unit // o
	private fun effect_t0_p(ch: Int) = Unit // p panning slide
	private fun effect_t0_q(ch: Int) = Unit // q
	private fun effect_t0_r(ch: Int) = Unit // r multi retrig note
	private fun effect_t0_s(ch: Int) = Unit // s
	private fun effect_t0_t(ch: Int) = Unit // t tremor
	private fun effect_t0_u(ch: Int) = Unit // u
	private fun effect_t0_v(ch: Int) = Unit // v
	private fun effect_t0_w(ch: Int) = Unit // w
	private fun effect_t0_x(ch: Int) = Unit // x extra fine porta up/down
	private fun effect_t0_y(ch: Int) = Unit // y
	private fun effect_t0_z(ch: Int) = Unit // z

	//
	// tick 0 effect e functions
	//
	private fun effect_t0_e0(ch: Int) = Unit // e0 filter on/off

	private fun effect_t0_e1(ch: Int) { // e1 fine slide up
		this.channel[ch].period -= this.channel[ch].data and 0x0f
		if (this.channel[ch].period < 113) this.channel[ch].period = 113.0
	}

	private fun effect_t0_e2(ch: Int) { // e2 fine slide down
		this.channel[ch].period += this.channel[ch].data and 0x0f
		if (this.channel[ch].period > 856) this.channel[ch].period = 856.0
		this.channel[ch].flags = this.channel[ch].flags or 1
	}

	private fun effect_t0_e3(ch: Int) = Unit // e3 set glissando
	private fun effect_t0_e4(ch: Int) =
		run { this.channel[ch].vibratowave = this.channel[ch].data and 0x07 } // e4 set vibrato waveform

	private fun effect_t0_e5(ch: Int) = Unit // e5 set finetune

	private fun effect_t0_e6(ch: Int) { // e6 loop pattern
		if ((this.channel[ch].data and 0x0f) != 0) {
			if (this.loopcount != 0) {
				this.loopcount--
			} else {
				this.loopcount = this.channel[ch].data and 0x0f
			}
			if (this.loopcount != 0) this.flags = this.flags or 64
		} else {
			this.looprow = this.row
		}
	}

	private fun effect_t0_e7(ch: Int) = Unit // e7
	private fun effect_t0_e8(ch: Int) = this.syncqueue.addFirst(this.channel[ch].data and 0x0f) // e8, use for syncing
	private fun effect_t0_e9(ch: Int) = Unit // e9

	private fun effect_t0_ea(ch: Int) { // ea fine volslide up
		this.channel[ch].voicevolume += this.channel[ch].data and 0x0f
		if (this.channel[ch].voicevolume > 64) this.channel[ch].voicevolume = 64
	}

	private fun effect_t0_eb(ch: Int) { // eb fine volslide down
		this.channel[ch].voicevolume -= this.channel[ch].data and 0x0f
		if (this.channel[ch].voicevolume < 0) this.channel[ch].voicevolume = 0
	}

	private fun effect_t0_ec(ch: Int) = Unit // ec

	private fun effect_t0_ed(ch: Int) { // ed delay sample
		if (this.tick == (this.channel[ch].data and 0x0f)) {
			this.process_note(this.patterntable[this.position], ch)
		}
	}

	private fun effect_t0_ee(ch: Int) { // ee delay pattern
		this.patterndelay = this.channel[ch].data and 0x0f
		this.patternwait = 0
	}

	private fun effect_t0_ef(ch: Int) = Unit // ef

	//
	// tick 1+ effect functions
	//
	private fun effect_t1_0(ch: Int) { // 0 arpeggio
		val channel = this.channel[ch]
		if (channel.data != 0) {
			val i = channel.instrument
			var apn = channel.note
			if ((this.tick % 3) == 1) apn += channel.arpeggio shr 4
			if ((this.tick % 3) == 2) apn += channel.arpeggio and 0x0f

			val s = channel.sampleindex
			channel.voiceperiod = this.calcperiod(
				apn + this.instrument[i].sample[s].relativenote,
				this.instrument[i].sample[s].finetune.toDouble()
			)
			channel.flags = channel.flags or 1
		}
	}

	private fun effect_t1_1(ch: Int) { // 1 slide up
		this.channel[ch].voiceperiod -= this.channel[ch].slideupspeed
		if (this.channel[ch].voiceperiod < 1) this.channel[ch].voiceperiod += 65535 // yeah, this is how it supposedly works in ft2...
		this.channel[ch].flags = this.channel[ch].flags or 3 // recalc speed
	}

	private fun effect_t1_2(ch: Int) { // 2 slide down
		this.channel[ch].voiceperiod += this.channel[ch].slidedownspeed
		if (this.channel[ch].voiceperiod > 7680) this.channel[ch].voiceperiod = 7680.0
		this.channel[ch].flags = this.channel[ch].flags or 3 // recalc speed
	}

	private fun effect_t1_3(ch: Int) { // 3 slide to note
		val chan = this.channel[ch]
		if (chan.voiceperiod < chan.slideto) {
			chan.voiceperiod += chan.slidetospeed
			if (chan.voiceperiod > chan.slideto) chan.voiceperiod = chan.slideto
		}
		if (chan.voiceperiod > chan.slideto) {
			chan.voiceperiod -= chan.slidetospeed
			if (chan.voiceperiod < chan.slideto) chan.voiceperiod = chan.slideto
		}
		chan.flags = chan.flags or 3 // recalc speed
	}

	private fun effect_t1_4(ch: Int) { // 4 vibrato
		val waveform = Fasttracker.vibratotable[this.channel[ch].vibratowave and 3][this.channel[ch].vibratopos] / 63.0
		val a = this.channel[ch].vibratodepth * waveform
		this.channel[ch].voiceperiod += a
		this.channel[ch].flags = this.channel[ch].flags or 1
	}

	private fun effect_t1_5(ch: Int) { // 5 volslide + slide to note
		this.effect_t1_3(ch) // slide to note
		this.effect_t1_a(ch) // volslide
	}

	private fun effect_t1_6(ch: Int) { // 6 volslide + vibrato
		this.effect_t1_4(ch) // vibrato
		this.effect_t1_a(ch) // volslide
	}

	private fun effect_t1_7(ch: Int) = Unit // 7
	private fun effect_t1_8(ch: Int) = Unit // 8 unused
	private fun effect_t1_9(ch: Int) = Unit // 9 set sample offset

	private fun effect_t1_a(ch: Int) { // a volume slide
		if ((this.channel[ch].volslide and 0x0f) == 0) {
			// y is zero, slide up
			this.channel[ch].voicevolume += (this.channel[ch].volslide shr 4)
			if (this.channel[ch].voicevolume > 64) this.channel[ch].voicevolume = 64
		}
		if ((this.channel[ch].volslide and 0xf0) == 0) {
			// x is zero, slide down
			this.channel[ch].voicevolume -= (this.channel[ch].volslide and 0x0f)
			if (this.channel[ch].voicevolume < 0) this.channel[ch].voicevolume = 0
		}
	}

	private fun effect_t1_b(ch: Int) = Unit // b pattern jump
	private fun effect_t1_c(ch: Int) = Unit // c set volume
	private fun effect_t1_d(ch: Int) = Unit // d pattern break

	private fun effect_t1_e(ch: Int) { // e
		val i = (this.channel[ch].data and 0xf0) ushr 4
		this.effects_t1_e[i](ch)
	}

	private fun effect_t1_f(ch: Int) = Unit // f
	private fun effect_t1_g(ch: Int) = Unit // g set global volume

	private fun effect_t1_h(ch: Int) { // h global volume slude
		if ((this.globalvolslide and 0x0f) == 0) {
			// y is zero, slide up
			this.volume += (this.globalvolslide shr 4)
			if (this.volume > 64) this.volume = 64
		}
		if ((this.globalvolslide and 0xf0) == 0) {
			// x is zero, slide down
			this.volume -= (this.globalvolslide and 0x0f)
			if (this.volume < 0) this.volume = 0
		}
	}

	private fun effect_t1_i(ch: Int) = Unit // i
	private fun effect_t1_j(ch: Int) = Unit // j
	private fun effect_t1_k(ch: Int) = Unit // k key off
	private fun effect_t1_l(ch: Int) = Unit // l set envelope position
	private fun effect_t1_m(ch: Int) = Unit // m
	private fun effect_t1_n(ch: Int) = Unit // n
	private fun effect_t1_o(ch: Int) = Unit // o
	private fun effect_t1_p(ch: Int) = Unit // p panning slide
	private fun effect_t1_q(ch: Int) = Unit // q
	private fun effect_t1_r(ch: Int) = Unit // r multi retrig note
	private fun effect_t1_s(ch: Int) = Unit // s
	private fun effect_t1_t(ch: Int) = Unit // t tremor
	private fun effect_t1_u(ch: Int) = Unit // u
	private fun effect_t1_v(ch: Int) = Unit // v
	private fun effect_t1_w(ch: Int) = Unit // w
	private fun effect_t1_x(ch: Int) = Unit // x extra fine porta up/down
	private fun effect_t1_y(ch: Int) = Unit // y
	private fun effect_t1_z(ch: Int) = Unit // z

	//
	// tick 1+ effect e functions
	//
	private fun effect_t1_e0(ch: Int) = Unit // e0

	private fun effect_t1_e1(ch: Int) = Unit // e1
	private fun effect_t1_e2(ch: Int) = Unit // e2
	private fun effect_t1_e3(ch: Int) = Unit // e3
	private fun effect_t1_e4(ch: Int) = Unit // e4
	private fun effect_t1_e5(ch: Int) = Unit // e5
	private fun effect_t1_e6(ch: Int) = Unit // e6
	private fun effect_t1_e7(ch: Int) = Unit // e7
	private fun effect_t1_e8(ch: Int) = Unit // e8

	private fun effect_t1_e9(ch: Int) { // e9 retrig sample
		if (this.tick % (this.channel[ch].data and 0x0f) == 0) {
			this.channel[ch].samplepos = 0.0
			this.channel[ch].playdir = 1

			this.channel[ch].trigramp = 0.0
			this.channel[ch].trigrampfrom = this.channel[ch].currentsample

			this.channel[ch].fadeoutpos = 65535
			this.channel[ch].volenvpos = 0
			this.channel[ch].panenvpos = 0
		}
	}

	private fun effect_t1_ea(ch: Int) = Unit // ea
	private fun effect_t1_eb(ch: Int) = Unit // eb
	private fun effect_t1_ec(ch: Int) =
		run { if (this.tick == (this.channel[ch].data and 0x0f)) this.channel[ch].voicevolume = 0 }// ec cut sample

	private fun effect_t1_ed(ch: Int) = this.effect_t0_ed(ch) // ed delay sample
	private fun effect_t1_ee(ch: Int) = Unit // ee
	private fun effect_t1_ef(ch: Int) = Unit // ef

	fun play() {
		repeat = true
		endofsong = false
		paused = false
		initialize()
		flags = 1 + 2
		playing = true
		chvu = DoubleArray(channels) { 0.0 }
	}

	fun createAudioStream(): AudioStream {
		play()
		return object : AudioStream(samplerate, 2) {
			override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
				val len = length / 2

				val lbuf = DoubleArray(len)
				val rbuf = DoubleArray(len)
				mix(lbuf, rbuf, len)

				var m = offset
				for (n in 0 until len) {
					out[m++] = lbuf[n].toShortSample(mixval)
					out[m++] = rbuf[n].toShortSample(mixval)
				}

				//println(bufs.map { it.toList() })

				//return super.read(out, offset, length)
				return length
			}
		}
	}

	fun Double.toShortSample(mixval: Double): Short {
		val rr = this / mixval
		val r = (abs(rr + 0.975) - abs(rr - 0.975))
		return (r * Short.MAX_VALUE).clamp(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toShort()
	}

	fun rewind() {
		play()
	}
}

// helper functions for picking up signed, unsigned, little endian, etc from an unsigned 8-bit buffer
private fun le_word(buffer: UByteArrayInt, offset: Int): Int = buffer[offset] or (buffer[offset + 1] shl 8)

private fun le_dword(buffer: UByteArrayInt, offset: Int): Int =
	buffer[offset] or (buffer[offset + 1] shl 8) or (buffer[offset + 2] shl 16) or (buffer[offset + 3] shl 24)

private fun s_byte(buffer: UByteArrayInt, offset: Int): Int =
	if (buffer[offset] < 128) buffer[offset] else (buffer[offset] - 256)

private fun s_le_word(buffer: UByteArrayInt, offset: Int): Int =
	if (le_word(buffer, offset) < 32768) le_word(buffer, offset) else (le_word(buffer, offset) - 65536)

// convert from MS-DOS extended ASCII to Unicode
private var cs = intArrayOf(
	0x00c7,
	0x00fc,
	0x00e9,
	0x00e2,
	0x00e4,
	0x00e0,
	0x00e5,
	0x00e7,
	0x00ea,
	0x00eb,
	0x00e8,
	0x00ef,
	0x00ee,
	0x00ec,
	0x00c4,
	0x00c5,
	0x00c9,
	0x00e6,
	0x00c6,
	0x00f4,
	0x00f6,
	0x00f2,
	0x00fb,
	0x00f9,
	0x00ff,
	0x00d6,
	0x00dc,
	0x00f8,
	0x00a3,
	0x00d8,
	0x00d7,
	0x0192,
	0x00e1,
	0x00ed,
	0x00f3,
	0x00fa,
	0x00f1,
	0x00d1,
	0x00aa,
	0x00ba,
	0x00bf,
	0x00ae,
	0x00ac,
	0x00bd,
	0x00bc,
	0x00a1,
	0x00ab,
	0x00bb,
	0x2591,
	0x2592,
	0x2593,
	0x2502,
	0x2524,
	0x00c1,
	0x00c2,
	0x00c0,
	0x00a9,
	0x2563,
	0x2551,
	0x2557,
	0x255d,
	0x00a2,
	0x00a5,
	0x2510,
	0x2514,
	0x2534,
	0x252c,
	0x251c,
	0x2500,
	0x253c,
	0x00e3,
	0x00c3,
	0x255a,
	0x2554,
	0x2569,
	0x2566,
	0x2560,
	0x2550,
	0x256c,
	0x00a4,
	0x00f0,
	0x00d0,
	0x00ca,
	0x00cb,
	0x00c8,
	0x0131,
	0x00cd,
	0x00ce,
	0x00cf,
	0x2518,
	0x250c,
	0x2588,
	0x2584,
	0x00a6,
	0x00cc,
	0x2580,
	0x00d3,
	0x00df,
	0x00d4,
	0x00d2,
	0x00f5,
	0x00d5,
	0x00b5,
	0x00fe,
	0x00de,
	0x00da,
	0x00db,
	0x00d9,
	0x00fd,
	0x00dd,
	0x00af,
	0x00b4,
	0x00ad,
	0x00b1,
	0x2017,
	0x00be,
	0x00b6,
	0x00a7,
	0x00f7,
	0x00b8,
	0x00b0,
	0x00a8,
	0x00b7,
	0x00b9,
	0x00b3,
	0x00b2,
	0x25a0,
	0x00a0
)

private fun dos2utf(c: Int): Char {
	val r = c and 0xFF
	if (r < 128) return r.toChar()
	return cs[r - 128].toChar()

}

// ReferenceError: Math_0 is not defined

//fun Double.abs2() = abs(this)

// @TODO: KotlinJS bug if removed those!
private fun abs(a: Double) = if (a >= 0.0) a else -a

private fun min(a: Double, b: Double) = if (a < b) a else b
private fun max(a: Double, b: Double) = if (a > b) a else b
private fun max(a: Int, b: Int) = if (a > b) a else b
