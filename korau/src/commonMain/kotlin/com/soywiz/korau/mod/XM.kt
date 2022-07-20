@file:Suppress(
    "MemberVisibilityCanBePrivate", "unused", "UNUSED_PARAMETER", "FunctionName",
    "PropertyName", "NAME_SHADOWING"
)

package com.soywiz.korau.mod

import com.soywiz.kds.IntDeque
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8BufferAlloc
import com.soywiz.kmem.toInt
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.file.VfsFile
import kotlin.math.pow
import kotlin.random.Random

/*
  https://github.com/electronoora/webaudio-mod-player

  (c) 2012-2021 Noora Halme et al. (see AUTHORS)

  This code is licensed under the MIT license:
  http://www.opensource.org/licenses/mit-license.php

  Fast Tracker 2 module player class

  Reading material:
  - ftp://ftp.modland.com/pub/documents/format_documentation/FastTracker%202%20v2.04%20(.xm).html
  - http://sid.ethz.ch/debian/milkytracker/milkytracker-0.90.85%2Bdfsg/resources/reference/xm-form.txt
  - ftp://ftp.modland.com/pub/documents/format_documentation/Tracker%20differences%20for%20Coders.txt
  - http://wiki.openmpt.org/Manual:_Compatible_Playback

  Greets to Guru, Alfred and CCR for their work figuring out the .xm format. :)
*/
suspend fun VfsFile.readXM(soundProvider: NativeSoundProvider = nativeSoundProvider): Sound =
    Fasttracker().createSoundFromFile(this)

class Fasttracker : BaseModuleTracker() {
    var paused = false
    var repeat = false

    var filter = false

    var syncqueue = IntDeque()

    var ramplen = 64.0

    var mixval = 8.0

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

    val pan = FloatArray(32) { 0.5f }
    val finalpan = FloatArray(32) { 0.5f }

    // calc tables for vibrato waveforms
    val vibratotable = arrayOf(
        FloatArray(64) { 127f * kotlin.math.sin(kotlin.math.PI * 2 * (it.toFloat() / 64f)).toFloat() },
        FloatArray(64) { 127f - 4f * it },
        FloatArray(64) { if (it < 32) 127f else -127f },
        FloatArray(64) { (1 - 2 * Random.nextFloat()) * 127 },
    )

    // volume column effect jumptable for 0x50..0xef
    val voleffects_t0 = arrayOf(
        ::effect_vol_t0_f0, ::effect_vol_t0_60, ::effect_vol_t0_70, ::effect_vol_t0_80, ::effect_vol_t0_90,
        ::effect_vol_t0_a0, ::effect_vol_t0_b0, ::effect_vol_t0_c0, ::effect_vol_t0_d0, ::effect_vol_t0_e0
    )
    val voleffects_t1 = arrayOf(
        ::effect_vol_t1_f0,
        ::effect_vol_t1_60, ::effect_vol_t1_70, ::effect_vol_t1_80, ::effect_vol_t1_90, ::effect_vol_t1_a0,
        ::effect_vol_t1_b0, ::effect_vol_t1_c0, ::effect_vol_t1_d0, ::effect_vol_t1_e0
    )

    // effect jumptables for tick 0 and ticks 1..f
    val effects_t0 = arrayOf(
        ::effect_t0_0, ::effect_t0_1, ::effect_t0_2, ::effect_t0_3, ::effect_t0_4, ::effect_t0_5, ::effect_t0_6,
        ::effect_t0_7, ::effect_t0_8, ::effect_t0_9, ::effect_t0_a, ::effect_t0_b, ::effect_t0_c, ::effect_t0_d,
        ::effect_t0_e, ::effect_t0_f, ::effect_t0_g, ::effect_t0_h, ::effect_t0_i, ::effect_t0_j, ::effect_t0_k,
        ::effect_t0_l, ::effect_t0_m, ::effect_t0_n, ::effect_t0_o, ::effect_t0_p, ::effect_t0_q, ::effect_t0_r,
        ::effect_t0_s, ::effect_t0_t, ::effect_t0_u, ::effect_t0_v, ::effect_t0_w, ::effect_t0_x, ::effect_t0_y,
        ::effect_t0_z
    )
    val effects_t0_e = arrayOf(
        ::effect_t0_e0, ::effect_t0_e1, ::effect_t0_e2, ::effect_t0_e3, ::effect_t0_e4, ::effect_t0_e5,
        ::effect_t0_e6, ::effect_t0_e7, ::effect_t0_e8, ::effect_t0_e9, ::effect_t0_ea, ::effect_t0_eb,
        ::effect_t0_ec, ::effect_t0_ed, ::effect_t0_ee, ::effect_t0_ef
    )
    val effects_t1 = arrayOf(
        ::effect_t1_0, ::effect_t1_1, ::effect_t1_2, ::effect_t1_3, ::effect_t1_4, ::effect_t1_5, ::effect_t1_6,
        ::effect_t1_7, ::effect_t1_8, ::effect_t1_9, ::effect_t1_a, ::effect_t1_b, ::effect_t1_c, ::effect_t1_d,
        ::effect_t1_e, ::effect_t1_f, ::effect_t1_g, ::effect_t1_h, ::effect_t1_i, ::effect_t1_j, ::effect_t1_k,
        ::effect_t1_l, ::effect_t1_m, ::effect_t1_n, ::effect_t1_o, ::effect_t1_p, ::effect_t1_q, ::effect_t1_r,
        ::effect_t1_s, ::effect_t1_t, ::effect_t1_u, ::effect_t1_v, ::effect_t1_w, ::effect_t1_x, ::effect_t1_y,
        ::effect_t1_z
    )
    val effects_t1_e = arrayOf(
        ::effect_t1_e0, ::effect_t1_e1, ::effect_t1_e2, ::effect_t1_e3, ::effect_t1_e4, ::effect_t1_e5, ::effect_t1_e6,
        ::effect_t1_e7, ::effect_t1_e8, ::effect_t1_e9, ::effect_t1_ea, ::effect_t1_eb, ::effect_t1_ec, ::effect_t1_ed,
        ::effect_t1_ee, ::effect_t1_ef
    )

    init {
        clearsong()
        initialize()
    }

    class Sample(
        var bits: Int = 0,
        var stereo: Int = 0,
        var bps: Int = 0,
        var length: Int = 0,
        var loopstart: Int = 0,
        var looplength: Int = 0,
        var loopend: Int = 0,
        var looptype: Int = 0,
        var volume: Int = 0,
        var finetune: Int = 0,
        var relativenote: Int = 0,
        var panning: Int = 0,
        var data: FloatArray = FloatArray(0),
        var name: String = "",
    ) {
    }

    class Instrument(
        var name: String = "",
        var samples: Int = 0,
        var sample: Array<Sample> = emptyArray(),
        var voltype: Int = 0,
        var pansustain: Int = 0,
        var panloopend: Int = 0,
        var panloopstart: Int = 0,
        var panenvlen: Int = 0,
        var volenv: FloatArray = FloatArray(0),
        var padenv: FloatArray = FloatArray(0),
        var panenv: FloatArray = FloatArray(0),
        var volsustain: Int = 0,
        var volloopend: Int = 0,
        var volloopstart: Int = 0,
        var volenvlen: Int = 0,
        var volfadeout: Int = 0,
        var pantype: Int = 0,
        var samplemap: Uint8Buffer = Uint8BufferAlloc(0),
        var vibratotype: Int = 0,
        var vibratosweep: Int = 0,
        var vibratodepth: Int = 0,
        var vibratorate: Int = 0,
    )

    class Channel(
        var instrument: Int = 0,
        var sampleindex: Int = 0,

        var note: Int = 36,
        var command: Int = 0,
        var data: Int = 0,
        var samplepos: Double = 0.0,
        var samplespeed: Double = 0.0,
        var flags: Int = 0,
        var noteon: Boolean = false,

        var volslide: Int = 0,
        var slidespeed: Int = 0,
        var slideto: Double = 0.0,
        var slideupspeed: Int = 0,
        var slidedownspeed: Int = 0,
        var slidetospeed: Int = 0,
        var arpeggio: Int = 0,

        var period: Double = 640.0,
        var frequency: Int = 8363,

        var volume: Int = 64,
        var voiceperiod: Double = 0.0,
        var voicevolume: Int = 0,
        var finalvolume: Double = 0.0,

        var semitone: Int = 12,
        var vibratospeed: Int = 0,
        var vibratodepth: Int = 0,
        var vibratopos: Int = 0,
        var vibratowave: Int = 0,

        var volenvpos: Int = 0,
        var panenvpos: Int = 0,
        var fadeoutpos: Int = 0,

        var playdir: Int = 1,

        // interpolation/ramps
        var volramp: Double = 0.0,
        var volrampfrom: Double = 0.0,
        var trigramp: Double = 0.0,
        var trigrampfrom: Double = 0.0,
        var currentsample: Double = 0.0,
        var lastsample: Double = 0.0,
        var oldfinalvolume: Double = 0.0,
    )

    var title: String = ""
    var signature: String = ""
    var trackerversion: Int = 0
    var songlen: Int = 0
    var repeatpos: Int = 0
    var channels: Int = 0
    var patterns: Int = 0
    var instruments: Int = 0
    var amigaperiods: Int = 0
    var initSpeed: Int = 6
    var initBPM: Int = 125
    var patterntable: Uint8Buffer = Uint8BufferAlloc(256)
    var pattern = emptyArray<Uint8Buffer>()
    var instrument = emptyArray<Instrument>()
    var chvu = FloatArray(2)

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

        patterntable = Uint8BufferAlloc(256)

        pattern = emptyArray()
        instrument = Array(instruments) { Instrument() }
        chvu = FloatArray(2)
    }

    var tick = -1
    var position = 0
    var row = 0
    var patternlen = IntArray(0)
    var flags = 0
    var volume = 64
    var speed = 0
    var bmp = 0
    var stt = 0
    var breakrow = 0
    var patternjump = 0
    var patterndelay = 0
    var patternwait = 0
    var looprow = 0
    var loopstart = 0
    var loopcount = 0
    var globalvolslide = 0
    var bpm = 0
    var channel = emptyArray<Channel>()

    // initialize all player variables to defaults prior to starting playback
    override fun initialize() {
        syncqueue = IntDeque()

        tick = -1
        position = 0
        row = 0
        flags = 0

        volume = 64
        if (initSpeed != 0) speed = initSpeed
        if (initBPM != 0) bpm = initBPM
        stt = 0 //this.samplerate/(this.bpm*0.4);
        breakrow = 0
        patternjump = 0
        patterndelay = 0
        patternwait = 0
        endofsong = false
        looprow = 0
        loopstart = 0
        loopcount = 0

        globalvolslide = 0

        channel = Array(channels) { Channel() }
    }

    // parse the module from local buffer
    override fun parse(buffer: Uint8Buffer): Boolean {
        var j: Int
        var c: Int
        var offset: Int
        var datalen: Int
        var hdrlen: Int

        // check xm signature, type and tracker version
        signature = CharArray(17) { buffer[it].toChar() }.concatToString()
        if (signature != "Extended Module: ") return false
        if (buffer[37] != 0x1a) return false
        signature = "X.M."
        trackerversion = le_word(buffer, 58)
        if (trackerversion < 0x0104) return false // older versions not currently supported

        // song title
        title += CharArray(20) { dos2utf(buffer[17 + it]) }.concatToString().trimEnd('\u0000')

        offset = 60
        hdrlen = le_dword(buffer, offset)
        songlen = le_word(buffer, offset + 4)
        repeatpos = le_word(buffer, offset + 6)
        channels = le_word(buffer, offset + 8)

        patterns = le_word(buffer, offset + 10)
        instruments = le_word(buffer, offset + 12)

        amigaperiods = (le_word(buffer, offset + 14) == 0).toInt()

        initSpeed = le_word(buffer, offset + 16)
        initBPM = le_word(buffer, offset + 18)

        var maxpatt = 0
        for (i in 0 until 256) {
            patterntable[i] = buffer[offset + 20 + i]
            if (patterntable[i] > maxpatt) maxpatt = patterntable[i]
        }
        maxpatt++

        // allocate arrays for pattern data
        pattern = Array(maxpatt) { Uint8BufferAlloc(0) }
        patternlen = IntArray(maxpatt)

        for (i in 0 until maxpatt) {
            // initialize the pattern to defaults prior to unpacking
            patternlen[i] = 64
            pattern[i] = Uint8BufferAlloc(channels * patternlen[i] * 5)
            for (row in 0 until patternlen[i]) {
                for (ch in 0 until channels) {
                    val pattern = pattern[i]
                    val index = row * channels * 5 + ch * 5
                    pattern[index + 0] = 255 // note (255=no note)
                    pattern[index + 1] = 0 // instrument
                    pattern[index + 2] = 255 // volume
                    pattern[index + 3] = 255 // command
                    pattern[index + 4] = 0 // parameter
                }
            }
        }

        // load and unpack patterns
        offset += hdrlen // initial offset for patterns
        var i = 0
        while (i < patterns) {
            patternlen[i] = le_word(buffer, offset + 5)
            pattern[i] = Uint8BufferAlloc(channels * patternlen[i] * 5)

            // initialize pattern to defaults prior to unpacking
            val pattern = pattern[i]
            for (k in 0 until (patternlen[i] * channels)) {
                pattern[k * 5 + 0] = 0 // note
                pattern[k * 5 + 1] = 0 // instrument
                pattern[k * 5 + 2] = 0 // volume
                pattern[k * 5 + 3] = 0 // command
                pattern[k * 5 + 4] = 0 // parameter
            }

            datalen = le_word(buffer, offset + 7)
            offset += le_dword(buffer, offset) // jump over header
            j = 0
            var k = 0
            while (j < datalen) {
                c = buffer[offset + j++]
                if ((c and 128) != 0) {
                    // first byte is a bitmask
                    if ((c and 1) != 0) pattern[k + 0] = buffer[offset + j++]
                    if ((c and 2) != 0) pattern[k + 1] = buffer[offset + j++]
                    if ((c and 4) != 0) pattern[k + 2] = buffer[offset + j++]
                    if ((c and 8) != 0) pattern[k + 3] = buffer[offset + j++]
                    if ((c and 16) != 0) pattern[k + 4] = buffer[offset + j++]
                } else {
                    // first byte is note -> all columns present sequentially
                    pattern[k + 0] = c
                    pattern[k + 1] = buffer[offset + j++]
                    pattern[k + 2] = buffer[offset + j++]
                    pattern[k + 3] = buffer[offset + j++]
                    pattern[k + 4] = buffer[offset + j++]
                }
                k += 5
            }

            for (k in 0 until (patternlen[i] * channels * 5) step 5) {
                // remap note to st3-style, 255=no note, 254=note off
                when {
                    pattern[k + 0] >= 97 -> pattern[k + 0] = 254
                    pattern[k + 0] == 0 -> pattern[k + 0] = 255
                    else -> pattern[k + 0]--
                }

                // command 255=no command
                if (pattern[k + 3] == 0 && pattern[k + 4] == 0) pattern[k + 3] = 255

                // remap volume column setvol to 0x00..0x40, tone porta to 0x50..0x5f and 0xff for nop
                when {
                    pattern[k + 2] < 0x10 -> pattern[k + 2] = 0xff
                    pattern[k + 2] in 0x10..0x50 -> pattern[k + 2] -= 0x10
                    pattern[k + 2] >= 0xf0 -> pattern[k + 2] -= 0xa0
                }
            }

            // unpack next pattern
            offset += j
            i++
        }
        patterns = maxpatt

        // instruments
        instrument = Array(instruments) { Instrument() }
        i = 0
        while (i < instruments) {
            hdrlen = le_dword(buffer, offset)
            val instrument = Instrument().also { instrument[i] = it }
            instrument.name = CharArray(22) { dos2utf(buffer[offset + 4 + it]) }.concatToString().trimEnd('\u0000')
            instrument.samples = le_word(buffer, offset + 27)

            // initialize to defaults
            instrument.samplemap = Uint8BufferAlloc(96)
            for (j in 0 until 96) instrument.samplemap[j] = 0
            instrument.volenv = FloatArray (325)
            instrument.panenv = FloatArray (325)
            instrument.voltype = 0
            instrument.pantype = 0
            instrument.sample = Array(instrument.samples + 1) {
                Sample(
                    bits = 8, stereo = 0, bps = 1,
                    length = 0, loopstart = 0, looplength = 0, loopend = 0, looptype = 0,
                    volume = 64, finetune = 0, relativenote = 0, panning = 128, name = "",
                    data = FloatArray(0)
                )
            }

            if (instrument.samples != 0) {
                val smphdrlen = le_dword(buffer, offset + 29)

                for (j in 0 until 96) instrument.samplemap[j] = buffer[offset + 33 + j]

                // envelope points. the xm specs say 48 bytes per envelope, but while that may
                // technically be correct, what they don't say is that it means 12 pairs of
                // little endian words. first word is the x coordinate, second is y. point
                // 0 always has x=0.
                val tmp_volenv = Array(12) { IntArray(0) }
                val tmp_panenv = Array(12) { IntArray(0) }
                for (j in 0 until 12) {
                    tmp_volenv[j] = intArrayOf(
                        le_word(buffer, offset + 129 + j * 4),
                        le_word(buffer, offset + 129 + j * 4 + 2)
                    )
                    tmp_panenv[j] = intArrayOf(
                        le_word(buffer, offset + 177 + j * 4),
                        le_word(buffer, offset + 177 + j * 4 + 2)
                    )
                }

                // are envelopes enabled?
                instrument.voltype = buffer[offset + 233] // 1=enabled, 2=sustain, 4=loop
                instrument.pantype = buffer[offset + 234]

                // pre-interpolate the envelopes to arrays of [0..1] float32 values which
                // are stepped through at a rate of one per tick. max tick count is 0x0144.

                // volume envelope
                for (j in 0 until 325) instrument.volenv[j] = 1f
                if ((instrument.voltype and 1) != 0) {
                    for (j in 0 until 325) {
                        var p = 1
                        val tmp_volenvP = tmp_volenv[p]
                        while (tmp_volenvP[0] < j && p < 11) p++
                        val delta: Double = when {
                            tmp_volenvP[0] == tmp_volenv[p - 1][0] -> 0.0
                            else -> (tmp_volenvP[1] - tmp_volenv[p - 1][1]).toDouble() / (tmp_volenvP[0] - tmp_volenv[p - 1][0]).toDouble()
                        }
                        instrument.volenv[j] = ((tmp_volenv[p - 1][1] + delta * (j - tmp_volenv[p - 1][0])) / 64.0).toFloat()
                    }
                    instrument.volenvlen = tmp_volenv[kotlin.math.max(0, buffer[offset + 225] - 1)][0]
                    instrument.volsustain = tmp_volenv[buffer[offset + 227]][0]
                    instrument.volloopstart = tmp_volenv[buffer[offset + 228]][0]
                    instrument.volloopend = tmp_volenv[buffer[offset + 229]][0]
                }

                // pan envelope
                for (j in 0 until 325) instrument.panenv[j] = 0.5.toFloat()
                if ((instrument.pantype and 1) != 0) {
                    for (j in 0 until 325) {
                        var p = 1
                        while (tmp_panenv[p][0] < j && p < 11) p++
                        val delta: Double = when {
                            tmp_panenv[p][0] == tmp_panenv[p - 1][0] -> 0.0
                            else -> (tmp_panenv[p][1] - tmp_panenv[p - 1][1]).toDouble() / (tmp_panenv[p][0] - tmp_panenv[p - 1][0]).toDouble()
                        }
                        instrument.panenv[j] = ((tmp_panenv[p - 1][1] + delta * (j - tmp_panenv[p - 1][0])) / 64.0).toFloat()
                    }
                    instrument.panenvlen = tmp_panenv[kotlin.math.max(0, buffer[offset + 226] - 1)][0]
                    instrument.pansustain = tmp_panenv[buffer[offset + 230]][0]
                    instrument.panloopstart = tmp_panenv[buffer[offset + 231]][0]
                    instrument.panloopend = tmp_panenv[buffer[offset + 232]][0]
                }

                // vibrato
                instrument.vibratotype = buffer[offset + 235]
                instrument.vibratosweep = buffer[offset + 236]
                instrument.vibratodepth = buffer[offset + 237]
                instrument.vibratorate = buffer[offset + 238]

                // volume fade out
                instrument.volfadeout = le_word(buffer, offset + 239)

                // sample headers
                offset += hdrlen
                instrument.sample = Array (instrument.samples) { Sample() }
                for (j in 0 until instrument.samples) {
                    datalen = le_dword(buffer, offset + 0)

                    val sample = instrument.sample[j]
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
                    sample.finetune = (if (buffer[offset + 13] < 128) buffer[offset + 13] else buffer[offset + 13] - 256)
                    sample.relativenote = (if (buffer[offset + 16] < 128) buffer[offset + 16] else buffer[offset + 16] - 256)

                    sample.panning = buffer[offset + 15]
                    sample.name = CharArray(22) { dos2utf(buffer[offset + 18 + it]) }.concatToString().trimEnd('\u0000')
                    offset += smphdrlen
                }

                // sample data (convert to signed float32)
                for (j in 0 until instrument.samples) {
                    val sample = instrument.sample[j]
                    sample.data = FloatArray(sample.length)
                    c = 0
                    if (sample.bits == 16) {
                        for (k in 0 until sample.length) {
                            c += s_le_word(buffer, offset + k * 2)
                            if (c < -32768) c += 65536
                            if (c > 32767) c -= 65536
                            sample.data[k] = (c / 32768.0).toFloat()
                        }
                    } else {
                        for (k in 0 until sample.length) {
                            c += s_byte(buffer, offset + k)
                            if (c < -128) c += 256
                            if (c > 127) c -= 256
                            sample.data[k] = (c / 128.0).toFloat()
                        }
                    }
                    offset += sample.length * sample.bps
                }
            } else {
                offset += hdrlen
            }
            i++
        }

        mixval = 4.0 - 2.0 * (channels / 32.0)

        chvu = FloatArray(channels) { 0.0.toFloat() }

        return true
    }

    @Deprecated("", ReplaceWith("this != 0"))
    fun Int.toBool(): Boolean = this != 0

    @Deprecated("", ReplaceWith("(this and v) != 0"))
    infix fun Int.andBool(v: Int): Boolean = (this and v) != 0
    fun pow(a: Double, b: Double): Double = a.pow(b)
    fun pow(a: Int, b: Int): Int = a.toDouble().pow(b.toDouble()).toInt() // @TODO: 2.pow(y) --> bit shifting (1 << y)

    // calculate period value for note
    fun calcperiod(note: Int, finetune: Int): Double {
        return if (amigaperiods != 0) {
            var ft = finetune / 16.0 // = -8 .. 7
            val p1 = periodtable[(8 + (note % 12) * 8 + ft).toInt()]
            val p2 = periodtable[(8 + (note % 12) * 8 + ft + 1).toInt()]
            ft = (finetune / 16.0) - ft
            ((1.0 - ft) * p1 + ft * p2) * (16.0 / pow(2.0, (note / 12.0) - 1))
        } else {
            7680.0 - note * 64.0 - finetune / 2.0
        }
    }


    // advance player by a tick
    fun advance() {
        stt = kotlin.math.floor((125.0 / bpm.toDouble()) * (1 / 50.0) * samplerate).toInt() // 50Hz

        // advance player
        tick++
        flags = flags or 1

        // new row on this tick?
        if (tick >= speed) {
            if (patterndelay != 0) { // delay pattern
                if (tick < ((patternwait + 1) * speed)) {
                    patternwait++
                } else {
                    row++; tick = 0; flags = flags or 2; patterndelay = 0
                }
            } else {
                if ((flags and (16 + 32 + 64)) != 0) {
                    if ((flags and 64) != 0) { // loop pattern?
                        row = looprow
                        flags = flags and 0xa1
                        flags = flags or 2
                    } else {
                        if ((flags and 16) != 0) { // pattern jump/break?
                            position = patternjump
                            row = breakrow
                            patternjump = 0
                            breakrow = 0
                            flags = flags and 0xe1
                            flags = flags or 2
                        }
                    }
                    tick = 0
                } else {
                    row++; tick = 0; flags = flags or 2
                }
            }
        }

        // step to new pattern?
        if (row >= patternlen[patterntable[position]]) {
            position++
            row = 0
            flags = flags or 4
        }

        // end of song?
        if (position >= songlen) {
            if (repeat) {
                position = 0
            } else {
                endofsong = true
            }
            return
        }
    }

    // process one channel on a row in pattern p, pp is an offset to pattern data
    fun process_note(p: Int, ch: Int) {
        var i: Int
        var s: Double
        val v: Int
        val pv: Double

        val pp: Int = row * 5 * channels + ch * 5
        val n: Int = pattern[p][pp]
        i = pattern[p][pp + 1]
        val channel = channel[ch]
        if (i != 0 && i <= instrument.size) {
            channel.instrument = i - 1

            if (instrument[i - 1].samples != 0) {
                s = instrument[i - 1].samplemap[channel.note].toDouble()
                channel.sampleindex = s.toInt()
                channel.volume = instrument[i - 1].sample[s.toInt()].volume
                channel.playdir = 1 // fixes crash in respirator.xm pos 0x12

                // set pan from sample
                pan[ch] = instrument[i - 1].sample[s.toInt()].panning / 255f
            }
            channel.voicevolume = channel.volume
        }
        i = channel.instrument

        if (n < 254) {
            // look up the sample
            s = instrument[i].samplemap[n].toDouble()
            channel.sampleindex = s.toInt()

            val rn = n + instrument[i].sample[s.toInt()].relativenote

            // calc period for note
            pv = calcperiod(rn, instrument[i].sample[s.toInt()].finetune)

            if (channel.noteon) {
                // retrig note, except if command=0x03 (porta to note) or 0x05 (porta+volslide)
                if ((channel.command != 0x03) && (channel.command != 0x05)) {
                    channel.note = n
                    channel.period = pv
                    channel.voiceperiod = channel.period
                    channel.flags = channel.flags or 3 // force sample speed recalc

                    channel.trigramp = 0.0
                    channel.trigrampfrom = channel.currentsample

                    channel.samplepos = 0.0
                    channel.playdir = 1
                    if (channel.vibratowave > 3) channel.vibratopos = 0

                    channel.noteon = true

                    channel.fadeoutpos = 65535
                    channel.volenvpos = 0
                    channel.panenvpos = 0
                }
            } else {
                // note is off, restart but don't set period if slide command
                if (pattern[p][pp + 1] != 0) { // instrument set on row?
                    channel.samplepos = 0.0
                    channel.playdir = 1
                    if (channel.vibratowave > 3) channel.vibratopos = 0
                    channel.noteon = true
                    channel.fadeoutpos = 65535
                    channel.volenvpos = 0
                    channel.panenvpos = 0
                    channel.trigramp = 0.0
                    channel.trigrampfrom = channel.currentsample
                }
                if ((channel.command != 0x03) && (channel.command != 0x05)) {
                    channel.note = n
                    channel.period = pv
                    channel.voiceperiod = channel.period
                    channel.flags = channel.flags or 3 // force sample speed recalc
                }
            }
            // in either case, set the slide to note target to note period
            channel.slideto = pv
        } else if (n == 254) {
            channel.noteon = false // note off
            if ((instrument[i].voltype and 1) == 0) channel.voicevolume = 0
        }

        if (pattern[p][pp + 2] != 255) {
            v = pattern[p][pp + 2]
            if (v <= 0x40) {
                channel.volume = v
                channel.voicevolume = channel.volume
            }
        }
    }


    // advance player and all channels by a tick
    fun process_tick() {
        // advance global player state by a tick
        advance()

        // advance all channels by a tick
        for (ch in 0 until channels) {

            // calculate playback position
            val p = patterntable[position]
            val pp = row * 5 * channels + ch * 5

            // save old volume if ramping is needed
            val channel = channel[ch]
            channel.oldfinalvolume = channel.finalvolume

            if ((flags and 2) != 0) { // new row on this tick?
                channel.command = pattern[p][pp + 3]
                channel.data = pattern[p][pp + 4]
                if (!(channel.command == 0x0e && (channel.data and 0xf0) == 0xd0)) { // note delay?
                    process_note(p, ch)
                }
            }
            val i = channel.instrument
            val si = channel.sampleindex

            // kill empty instruments
            val instrument = instrument[i]
            if (channel.noteon && instrument.samples == 0) {
                channel.noteon = false
            }

            // effects
            val v = pattern[p][pp + 2]
            if (v in 0x50..0xef) {
                if (tick == 0) voleffects_t0[(v shr 4) - 5](ch, v and 0x0f)
                else voleffects_t1[(v shr 4) - 5](ch, v and 0x0f)
            }
            if (channel.command < 36) {
                if (tick == 0) {
                    // process only on tick 0
                    effects_t0[channel.command](ch)
                } else {
                    effects_t1[channel.command](ch)
                }
            }

            // recalc sample speed if voiceperiod has changed
            if (((channel.flags and 1) != 0 || (flags and 2) != 0) && channel.voiceperiod != 0.0) {
                val f: Double = when {
                    amigaperiods != 0 -> 8287.137 * 1712.0 / channel.voiceperiod
                    else -> 8287.137 * pow(2.0, (4608.0 - channel.voiceperiod) / 768.0)
                }
                channel.samplespeed = f / samplerate.toDouble()
            }

            // advance vibrato on each new tick
            channel.vibratopos += channel.vibratospeed
            channel.vibratopos = channel.vibratopos and 0x3f

            // advance volume envelope, if enabled (also fadeout)
            if ((instrument.voltype and 1) != 0) {
                channel.volenvpos++

                if (channel.noteon && ((instrument.voltype and 2) != 0) && channel.volenvpos >= instrument.volsustain) {
                    channel.volenvpos = instrument.volsustain
                }

                if (((instrument.voltype and 4) != 0) && channel.volenvpos >= instrument.volloopend) {
                    channel.volenvpos = instrument.volloopstart
                }

                if (channel.volenvpos >= instrument.volenvlen) {
                    channel.volenvpos = instrument.volenvlen
                }

                if (channel.volenvpos > 324) channel.volenvpos = 324

                // fadeout if note is off
                if (!channel.noteon && channel.fadeoutpos != 0) {
                    channel.fadeoutpos -= instrument.volfadeout
                    if (channel.fadeoutpos < 0) channel.fadeoutpos = 0
                }
            }

            // advance pan envelope, if enabled
            if ((instrument.pantype and 1) != 0) {
                channel.panenvpos++

                if (channel.noteon && ((instrument.pantype and 2) != 0) && channel.panenvpos >= instrument.pansustain) {
                    channel.panenvpos = instrument.pansustain
                }

                if (((instrument.pantype and 4) != 0) && channel.panenvpos >= instrument.panloopend) {
                    channel.panenvpos = instrument.panloopstart
                }

                if (channel.panenvpos >= instrument.panenvlen) channel.panenvpos = instrument.panenvlen
                if (channel.panenvpos > 324) channel.panenvpos = 324
            }

            // calc final volume for channel
            channel.finalvolume = channel.voicevolume * instrument.volenv[channel.volenvpos] * channel.fadeoutpos / 65536.0

            // calc final panning for channel
            finalpan[ch] = (pan[ch] + (instrument.panenv[channel.panenvpos] - 0.5) * (0.5 * kotlin.math.abs(pan[ch] - 0.5)) * 2.0).toFloat()

            // setup volramp if voice volume changed
            if (channel.oldfinalvolume != channel.finalvolume) {
                channel.volrampfrom = channel.oldfinalvolume
                channel.volramp = 0.0
            }

            // clear channel flags
            channel.flags = 0
        }

        // clear global flags after all channels are processed
        flags = flags and 0x70
    }


    // mix a buffer of audio for an audio processing event
    override fun mix(bufs: Array<FloatArray>, buflen: Int) {
        val outp = FloatArray(2)

        // return a buffer of silence if not playing
        if (paused || endofsong || !playing) {
            for (s in 0 until buflen) {
                bufs[0][s] = 0f
                bufs[1][s] = 0f
                for (ch in 0 until chvu.size) chvu[ch] = 0f
            }
            return
        }

        // fill audiobuffer
        for (s in 0 until buflen) {
            outp[0] = 0f
            outp[1] = 0f

            // if STT has run out, step player forward by tick
            if (stt <= 0) process_tick()

            // mix channels
            for (ch in 0 until channels) {
                var fl = 0.0
                var fr = 0.0
                var fs = 0.0
                val channel = channel[ch]
                val i = channel.instrument
                val si = channel.sampleindex

                // add channel output to left/right master outputs
                val instrument = instrument[i]
                if (channel.noteon ||
                    (((instrument.voltype and 1) != 0) && !channel.noteon && channel.fadeoutpos != 0) ||
                    (!channel.noteon && channel.volramp < 1.0)
                ) {
                    val sample = instrument.sample[si]
                    if (sample.length > channel.samplepos) {
                        fl = channel.lastsample

                        // interpolate towards current sample
                        var f = channel.samplepos
                        fs = sample.data[f.toInt()].toDouble()
                        f = channel.samplepos - f
                        f = if (channel.playdir < 0) (1.0 - f) else f
                        fl = f * fs + (1.0 - f) * fl

                        // smooth out discontinuities from retrig and sample offset
                        f = channel.trigramp
                        fl = f * fl + (1.0 - f) * channel.trigrampfrom
                        f += 1.0 / 128.0
                        channel.trigramp = kotlin.math.min(1.0, f)
                        channel.currentsample = fl

                        // ramp volume changes over 64 samples to avoid clicks
                        fr = fl * (channel.finalvolume / 64.0)
                        f = channel.volramp
                        fl = f * fr + (1.0 - f) * (fl * (channel.volrampfrom / 64.0))
                        f += (1.0 / 64.0)
                        channel.volramp = kotlin.math.min(1.0, f)

                        // pan samples, if envelope is disabled panvenv is always 0.5
                        f = finalpan[ch].toDouble()
                        fr = fl * f
                        fl *= 1.0 - f
                    }
                    outp[0] += fl.toFloat()
                    outp[1] += fr.toFloat()

                    // advance sample position and check for loop or end
                    val oldpos = channel.samplepos
                    channel.samplepos += channel.playdir * channel.samplespeed
                    if (channel.playdir == 1) {
                        if (kotlin.math.floor(channel.samplepos) > kotlin.math.floor(oldpos)) channel.lastsample = fs
                    } else {
                        if (kotlin.math.floor(channel.samplepos) < kotlin.math.floor(oldpos)) channel.lastsample = fs
                    }

                    if (sample.looptype != 0) {
                        if (sample.looptype == 2) {
                            // pingpong loop
                            if (channel.playdir == -1) {
                                // bounce off from start?
                                if (channel.samplepos <= sample.loopstart) {
                                    channel.samplepos += (sample.loopstart - channel.samplepos)
                                    channel.playdir = 1
                                    channel.lastsample = channel.currentsample
                                }
                            } else {
                                // bounce off from end?
                                if (channel.samplepos >= sample.loopend) {
                                    channel.samplepos -= (channel.samplepos - sample.loopend)
                                    channel.playdir = -1
                                    channel.lastsample = channel.currentsample
                                }
                            }
                        } else {
                            // normal loop
                            if (channel.samplepos >= sample.loopend) {
                                channel.samplepos -= sample.looplength
                                channel.lastsample = channel.currentsample
                            }
                        }
                    } else {
                        if (channel.samplepos >= sample.length) {
                            channel.noteon = false
                        }
                    }
                } else {
                    channel.currentsample = 0.0 // note is completely off
                }
                chvu[ch] = kotlin.math.max(chvu[ch], kotlin.math.abs(fl + fr).toFloat())
            }

            // done - store to output buffer
            val t = volume / 64.0
            bufs[0][s] = (outp[0] * t).toFloat()
            bufs[1][s] = (outp[1] * t).toFloat()
            stt--
        }
    }


    //
// volume column effect functions
//
    fun effect_vol_t0_60(ch: Int, data: Int) { // 60-6f vol slide down
    }

    fun effect_vol_t0_70(ch: Int, data: Int) { // 70-7f vol slide up
    }

    fun effect_vol_t0_80(ch: Int, data: Int) { // 80-8f fine vol slide down
        val channel = channel[ch]
        channel.voicevolume -= data
        if (channel.voicevolume < 0) channel.voicevolume = 0
    }

    fun effect_vol_t0_90(ch: Int, data: Int) { // 90-9f fine vol slide up
        val channel = channel[ch]
        channel.voicevolume += data
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_vol_t0_a0(ch: Int, data: Int) { // a0-af set vibrato speed
        val channel = channel[ch]
        channel.vibratospeed = data
    }

    fun effect_vol_t0_b0(ch: Int, data: Int) { // b0-bf vibrato
        if (data != 0) {
            val channel = channel[ch]
            channel.vibratodepth = data
        }
        effect_t1_4(ch)
    }

    fun effect_vol_t0_c0(ch: Int, data: Int) { // c0-cf set panning
        pan[ch] = (data and 0x0f) / 15f
    }

    fun effect_vol_t0_d0(ch: Int, data: Int) { // d0-df panning slide left
    }

    fun effect_vol_t0_e0(ch: Int, data: Int) { // e0-ef panning slide right
    }

    fun effect_vol_t0_f0(ch: Int, data: Int) { // f0-ff tone porta
//  if (data) mod.channel[ch].slidetospeed=data;
//  if (!mod.amigaperiods) mod.channel[ch].slidetospeed*=4;
    }

    //////
    fun effect_vol_t1_60(ch: Int, data: Int) { // 60-6f vol slide down
        val channel = channel[ch]
        channel.voicevolume -= data
        if (channel.voicevolume < 0) channel.voicevolume = 0
    }

    fun effect_vol_t1_70(ch: Int, data: Int) { // 70-7f vol slide up
        val channel = channel[ch]
        channel.voicevolume += data
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_vol_t1_80(ch: Int, data: Int) { // 80-8f fine vol slide down
    }

    fun effect_vol_t1_90(ch: Int, data: Int) { // 90-9f fine vol slide up
    }

    fun effect_vol_t1_a0(ch: Int, data: Int) { // a0-af set vibrato speed
    }

    fun effect_vol_t1_b0(ch: Int, data: Int) { // b0-bf vibrato
        effect_t1_4(ch) // same as effect column vibrato on ticks 1+
    }

    fun effect_vol_t1_c0(ch: Int, data: Int) { // c0-cf set panning
    }

    fun effect_vol_t1_d0(ch: Int, data: Int) { // d0-df panning slide left
    }

    fun effect_vol_t1_e0(ch: Int, data: Int) { // e0-ef panning slide right
    }

    fun effect_vol_t1_f0(ch: Int, data: Int) { // f0-ff tone porta
//  mod.effect_t1_3(mod, ch);
    }


    //
// tick 0 effect functions
//
    fun effect_t0_0(ch: Int) { // 0 arpeggio
        val channel = channel[ch]
        channel.arpeggio = channel.data
    }

    fun effect_t0_1(ch: Int) { // 1 slide up
        val channel = channel[ch]
        if (channel.data != 0) channel.slideupspeed = channel.data * 4
    }

    fun effect_t0_2(ch: Int) { // 2 slide down
        val channel = channel[ch]
        if (channel.data != 0) channel.slidedownspeed = channel.data * 4
    }

    fun effect_t0_3(ch: Int) { // 3 slide to note
        val channel = channel[ch]
        if (channel.data != 0) channel.slidetospeed = channel.data * 4
    }

    fun effect_t0_4(ch: Int) { // 4 vibrato
        val channel = channel[ch]
        if ((channel.data and 0x0f) != 0 && (channel.data and 0xf0) != 0) {
            channel.vibratodepth = (channel.data and 0x0f)
            channel.vibratospeed = (channel.data and 0xf0) ushr 4
        }
        effect_t1_4(ch)
    }

    fun effect_t0_5(ch: Int) { // 5
        effect_t0_a(ch)
    }

    fun effect_t0_6(ch: Int) { // 6
        effect_t0_a(ch)
    }

    fun effect_t0_7(ch: Int) { // 7
    }

    fun effect_t0_8(ch: Int) { // 8 set panning
        val channel = channel[ch]
        pan[ch] = channel.data / 255f
    }

    fun effect_t0_9(ch: Int) { // 9 set sample offset
        val channel = channel[ch]
        channel.samplepos = channel.data * 256.0
        channel.playdir = 1

        channel.trigramp = 0.0
        channel.trigrampfrom = channel.currentsample
    }

    fun effect_t0_a(ch: Int) { // a volume slide
        // this behavior differs from protracker!! A00 will slide using previous non-zero parameter.
        val channel = channel[ch]
        if (channel.data != 0) channel.volslide = channel.data
    }

    fun effect_t0_b(ch: Int) { // b pattern jump
        breakrow = 0
        patternjump = channel[ch].data
        flags = flags or 16
    }

    fun effect_t0_c(ch: Int) { // c set volume
        val channel = channel[ch]
        channel.voicevolume = channel.data
        if (channel.voicevolume < 0) channel.voicevolume = 0
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_t0_d(ch: Int) { // d pattern break
        val channel = channel[ch]
        breakrow = ((channel.data and 0xf0) shr 4) * 10 + (channel.data and 0x0f)
        if ((flags and 16) == 0) patternjump = position + 1
        flags = flags or 16
    }

    fun effect_t0_e(ch: Int) { // e
        val channel = channel[ch]
        val i = (channel.data and 0xf0) shr 4
        effects_t0_e[i](ch)
    }

    fun effect_t0_f(ch: Int) { // f set speed
        val channel = channel[ch]
        if (channel.data > 32) {
            bpm = channel.data
        } else {
            if (channel.data != 0) speed = channel.data
        }
    }

    fun effect_t0_g(ch: Int) { // g set global volume
        val channel = channel[ch]
        if (channel.data <= 0x40) volume = channel.data
    }

    fun effect_t0_h(ch: Int) { // h global volume slide
        val channel = channel[ch]
        if (channel.data != 0) globalvolslide = channel.data
    }

    fun effect_t0_i(ch: Int) { // i
    }

    fun effect_t0_j(ch: Int) { // j
    }

    fun effect_t0_k(ch: Int) { // k key off
        val channel = channel[ch]
        channel.noteon = false
        if ((instrument[channel.instrument].voltype and 1) == 0) channel.voicevolume = 0
    }

    fun effect_t0_l(ch: Int) { // l set envelope position
        val channel = channel[ch]
        channel.volenvpos = channel.data
        channel.panenvpos = channel.data
    }

    fun effect_t0_m(ch: Int) { // m
    }

    fun effect_t0_n(ch: Int) { // n
    }

    fun effect_t0_o(ch: Int) { // o
    }

    fun effect_t0_p(ch: Int) { // p panning slide
    }

    fun effect_t0_q(ch: Int) { // q
    }

    fun effect_t0_r(ch: Int) { // r multi retrig note
    }

    fun effect_t0_s(ch: Int) { // s
    }

    fun effect_t0_t(ch: Int) { // t tremor
    }

    fun effect_t0_u(ch: Int) { // u
    }

    fun effect_t0_v(ch: Int) { // v
    }

    fun effect_t0_w(ch: Int) { // w
    }

    fun effect_t0_x(ch: Int) { // x extra fine porta up/down
    }

    fun effect_t0_y(ch: Int) { // y
    }

    fun effect_t0_z(ch: Int) { // z
    }


    //
// tick 0 effect e functions
//
    fun effect_t0_e0(ch: Int) { // e0 filter on/off
    }

    fun effect_t0_e1(ch: Int) { // e1 fine slide up
        val channel = channel[ch]
        channel.period -= channel.data and 0x0f
        if (channel.period < 113) channel.period = 113.toDouble()
    }

    fun effect_t0_e2(ch: Int) { // e2 fine slide down
        val channel = channel[ch]
        channel.period += channel.data and 0x0f
        if (channel.period > 856) channel.period = 856.toDouble()
        channel.flags = channel.flags or 1
    }

    fun effect_t0_e3(ch: Int) { // e3 set glissando
    }

    fun effect_t0_e4(ch: Int) { // e4 set vibrato waveform
        val channel = channel[ch]
        channel.vibratowave = channel.data and 0x07
    }

    fun effect_t0_e5(ch: Int) { // e5 set finetune
    }

    fun effect_t0_e6(ch: Int) { // e6 loop pattern
        val channel = channel[ch]
        if ((channel.data and 0x0f) != 0) {
            when {
                loopcount != 0 -> loopcount--
                else -> loopcount = channel.data and 0x0f
            }
            if (loopcount != 0) flags = flags or 64
        } else {
            looprow = row
        }
    }

    fun effect_t0_e7(ch: Int) { // e7
    }

    fun effect_t0_e8(ch: Int) { // e8, use for syncing
        val channel = channel[ch]
        syncqueue.addFirst(channel.data and 0x0f)
    }

    fun effect_t0_e9(ch: Int) { // e9
    }

    fun effect_t0_ea(ch: Int) { // ea fine volslide up
        val channel = channel[ch]
        channel.voicevolume += channel.data and 0x0f
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_t0_eb(ch: Int) { // eb fine volslide down
        val channel = channel[ch]
        channel.voicevolume -= channel.data and 0x0f
        if (channel.voicevolume < 0) channel.voicevolume = 0
    }

    fun effect_t0_ec(ch: Int) { // ec
    }

    fun effect_t0_ed(ch: Int) { // ed delay sample
        val channel = channel[ch]
        if (tick == (channel.data and 0x0f)) {
            process_note(patterntable[position], ch)
        }
    }

    fun effect_t0_ee(ch: Int) { // ee delay pattern
        val channel = channel[ch]
        patterndelay = channel.data and 0x0f
        patternwait = 0
    }

    fun effect_t0_ef(ch: Int) { // ef
    }


    //
// tick 1+ effect functions
//
    fun effect_t1_0(ch: Int) { // 0 arpeggio
        val channel = channel[ch]
        if (channel.data != 0) {
            val i = channel.instrument
            var apn = channel.note
            if ((tick % 3) == 1) apn += channel.arpeggio ushr 4
            if ((tick % 3) == 2) apn += channel.arpeggio and 0x0f

            val s = channel.sampleindex
            val instrument = instrument[i]
            val sample = instrument.sample[s]
            channel.voiceperiod = calcperiod(apn + sample.relativenote, sample.finetune)
            channel.flags = channel.flags or 1
        }
    }

    fun effect_t1_1(ch: Int) { // 1 slide up
        val channel = channel[ch]
        channel.voiceperiod -= channel.slideupspeed
        if (channel.voiceperiod < 1) channel.voiceperiod += 65535 // yeah, this is how it supposedly works in ft2...
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_2(ch: Int) { // 2 slide down
        val channel = channel[ch]
        channel.voiceperiod += channel.slidedownspeed
        if (channel.voiceperiod > 7680) channel.voiceperiod = 7680.toDouble()
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_3(ch: Int) { // 3 slide to note
        val channel = channel[ch]
        if (channel.voiceperiod < channel.slideto) {
            channel.voiceperiod += channel.slidetospeed
            if (channel.voiceperiod > channel.slideto)
                channel.voiceperiod = channel.slideto
        }
        if (channel.voiceperiod > channel.slideto) {
            channel.voiceperiod -= channel.slidetospeed
            if (channel.voiceperiod < channel.slideto)
                channel.voiceperiod = channel.slideto
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_4(ch: Int) { // 4 vibrato
        val channel = channel[ch]
        val waveform = vibratotable[channel.vibratowave and 3][channel.vibratopos] / 63.0
        val a = channel.vibratodepth * waveform
        channel.voiceperiod += a
        channel.flags = channel.flags or 1
    }

    fun effect_t1_5(ch: Int) { // 5 volslide + slide to note
        effect_t1_3(ch) // slide to note
        effect_t1_a(ch) // volslide
    }

    fun effect_t1_6(ch: Int) { // 6 volslide + vibrato
        effect_t1_4(ch) // vibrato
        effect_t1_a(ch) // volslide
    }

    fun effect_t1_7(ch: Int) { // 7
    }

    fun effect_t1_8(ch: Int) { // 8 unused
    }

    fun effect_t1_9(ch: Int) { // 9 set sample offset
    }

    fun effect_t1_a(ch: Int) { // a volume slide
        val channel = channel[ch]
        if ((channel.volslide and 0x0f) == 0) {
            // y is zero, slide up
            channel.voicevolume += (channel.volslide ushr 4)
            if (channel.voicevolume > 64) channel.voicevolume = 64
        }
        if ((channel.volslide and 0xf0) == 0) {
            // x is zero, slide down
            channel.voicevolume -= (channel.volslide and 0x0f)
            if (channel.voicevolume < 0) channel.voicevolume = 0
        }
    }

    fun effect_t1_b(ch: Int) { // b pattern jump
    }

    fun effect_t1_c(ch: Int) { // c set volume
    }

    fun effect_t1_d(ch: Int) { // d pattern break
    }

    fun effect_t1_e(ch: Int) { // e
        val channel = channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        effects_t1_e[i](ch)
    }

    fun effect_t1_f(ch: Int) { // f
    }

    fun effect_t1_g(ch: Int) { // g set global volume
    }

    fun effect_t1_h(ch: Int) { // h global volume slude
        if ((globalvolslide and 0x0f) == 0) {
            // y is zero, slide up
            volume += (globalvolslide ushr 4)
            if (volume > 64) volume = 64
        }
        if ((globalvolslide and 0xf0) == 0) {
            // x is zero, slide down
            volume -= (globalvolslide and 0x0f)
            if (volume < 0) volume = 0
        }
    }

    fun effect_t1_i(ch: Int) { // i
    }

    fun effect_t1_j(ch: Int) { // j
    }

    fun effect_t1_k(ch: Int) { // k key off
    }

    fun effect_t1_l(ch: Int) { // l set envelope position
    }

    fun effect_t1_m(ch: Int) { // m
    }

    fun effect_t1_n(ch: Int) { // n
    }

    fun effect_t1_o(ch: Int) { // o
    }

    fun effect_t1_p(ch: Int) { // p panning slide
    }

    fun effect_t1_q(ch: Int) { // q
    }

    fun effect_t1_r(ch: Int) { // r multi retrig note
    }

    fun effect_t1_s(ch: Int) { // s
    }

    fun effect_t1_t(ch: Int) { // t tremor
    }

    fun effect_t1_u(ch: Int) { // u
    }

    fun effect_t1_v(ch: Int) { // v
    }

    fun effect_t1_w(ch: Int) { // w
    }

    fun effect_t1_x(ch: Int) { // x extra fine porta up/down
    }

    fun effect_t1_y(ch: Int) { // y
    }

    fun effect_t1_z(ch: Int) { // z
    }


    //
// tick 1+ effect e functions
//
    fun effect_t1_e0(ch: Int) { // e0
    }

    fun effect_t1_e1(ch: Int) { // e1
    }

    fun effect_t1_e2(ch: Int) { // e2
    }

    fun effect_t1_e3(ch: Int) { // e3
    }

    fun effect_t1_e4(ch: Int) { // e4
    }

    fun effect_t1_e5(ch: Int) { // e5
    }

    fun effect_t1_e6(ch: Int) { // e6
    }

    fun effect_t1_e7(ch: Int) { // e7
    }

    fun effect_t1_e8(ch: Int) { // e8
    }

    fun effect_t1_e9(ch: Int) { // e9 retrig sample
        val channel = channel[ch]
        if (tick % (channel.data and 0x0f) == 0) {
            channel.samplepos = 0.toDouble()
            channel.playdir = 1

            channel.trigramp = 0.0
            channel.trigrampfrom = channel.currentsample

            channel.fadeoutpos = 65535
            channel.volenvpos = 0
            channel.panenvpos = 0
        }
    }

    fun effect_t1_ea(ch: Int) { // ea
    }

    fun effect_t1_eb(ch: Int) { // eb
    }

    fun effect_t1_ec(ch: Int) { // ec cut sample
        val channel = channel[ch]
        if (tick == (channel.data and 0x0f))
            channel.voicevolume = 0
    }

    fun effect_t1_ed(ch: Int) { // ed delay sample
        effect_t0_ed(ch)
    }

    fun effect_t1_ee(ch: Int) { // ee
    }

    fun effect_t1_ef(ch: Int) { // ef
    }


}
