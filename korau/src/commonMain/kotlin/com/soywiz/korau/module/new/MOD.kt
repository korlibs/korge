@file:Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate", "FunctionName")

package com.soywiz.korau.module.new

import com.soywiz.kds.IntDeque
import com.soywiz.kmem.MemBufferWrap
import com.soywiz.kmem.NewInt8Buffer
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8BufferAlloc
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.file.VfsFile
import kotlin.math.pow

/*
  (c) 2012-2021 Noora Halme et al. (see AUTHORS)

  This code is licensed under the MIT license:
  http://www.opensource.org/licenses/mit-license.php

  Protracker module player class

  todo:
  - pattern looping is broken (see mod.black_queen)
  - properly test EEx delay pattern
*/

// constructor for protracker player object
class Protracker {
    var playing = false
    var paused = false
    var repeat = false
    var filter = false
    var mixval = 4.0
    var syncqueue = IntDeque()
    var samplerate = 44100

    // paula period values
    val baseperiodtable = floatArrayOf(
        856f, 808f, 762f, 720f, 678f, 640f, 604f, 570f, 538f, 508f, 480f, 453f,
        428f, 404f, 381f, 360f, 339f, 320f, 302f, 285f, 269f, 254f, 240f, 226f,
        214f, 202f, 190f, 180f, 170f, 160f, 151f, 143f, 135f, 127f, 120f, 113f
    )

    // finetune multipliers
    val finetunetable = FloatArray(16) { 2f.pow((it - 8).toFloat() / 12f / 8f) }

    // calc tables for vibrato waveforms
    val vibratotable = arrayOf(
        FloatArray(64) { 127 * kotlin.math.sin(kotlin.math.PI.toFloat() * 2 * (it / 64f)) },
        FloatArray(64) { 127f - 4f * it },
        FloatArray(64) { if (it < 32) 127f else -127f },
        FloatArray(64) { (1 - 2 * kotlin.random.Random.nextFloat()) * 127 }
    )

    // effect jumptables
    val effects_t0 = arrayOf(
        ::effect_t0_0,
        ::effect_t0_1,
        ::effect_t0_2,
        ::effect_t0_3,
        ::effect_t0_4,
        ::effect_t0_5,
        ::effect_t0_6,
        ::effect_t0_7,
        ::effect_t0_8,
        ::effect_t0_9,
        ::effect_t0_a,
        ::effect_t0_b,
        ::effect_t0_c,
        ::effect_t0_d,
        ::effect_t0_e,
        ::effect_t0_f
    )
    val effects_t0_e = arrayOf(
        ::effect_t0_e0,
        ::effect_t0_e1,
        ::effect_t0_e2,
        ::effect_t0_e3,
        ::effect_t0_e4,
        ::effect_t0_e5,
        ::effect_t0_e6,
        ::effect_t0_e7,
        ::effect_t0_e8,
        ::effect_t0_e9,
        ::effect_t0_ea,
        ::effect_t0_eb,
        ::effect_t0_ec,
        ::effect_t0_ed,
        ::effect_t0_ee,
        ::effect_t0_ef
    )
    val effects_t1 = arrayOf(
        ::effect_t1_0,
        ::effect_t1_1,
        ::effect_t1_2,
        ::effect_t1_3,
        ::effect_t1_4,
        ::effect_t1_5,
        ::effect_t1_6,
        ::effect_t1_7,
        ::effect_t1_8,
        ::effect_t1_9,
        ::effect_t1_a,
        ::effect_t1_b,
        ::effect_t1_c,
        ::effect_t1_d,
        ::effect_t1_e,
        ::effect_t1_f
    )
    val effects_t1_e = arrayOf(
        ::effect_t1_e0,
        ::effect_t1_e1,
        ::effect_t1_e2,
        ::effect_t1_e3,
        ::effect_t1_e4,
        ::effect_t1_e5,
        ::effect_t1_e6,
        ::effect_t1_e7,
        ::effect_t1_e8,
        ::effect_t1_e9,
        ::effect_t1_ea,
        ::effect_t1_eb,
        ::effect_t1_ec,
        ::effect_t1_ed,
        ::effect_t1_ee,
        ::effect_t1_ef
    )

    class Sample(
        var name: String = "",
        var length: Int = 0,
        var finetune: Int = 0,
        var volume: Int = 64,
        var loopstart: Int = 0,
        var looplength: Int = 0,
        var data: FloatArray = FloatArray(0),
    )

    data class Channel(
        var sample: Int = 0,
        var period: Int = 214,
        var voiceperiod: Double = 214.0,
        var note: Int = 24,
        var volume: Int = 64,
        var command: Int = 0,
        var data: Int = 0,
        var samplepos: Double = 0.0,
        var samplespeed: Double = 0.0,
        var flags: Int = 0,
        var noteon: Int = 0,
        var slidespeed: Int = 0,
        var slideto: Int = 214,
        var slidetospeed: Int = 0,
        var arpeggio: Int = 0,
        var semitone: Double = 12.0,
        var vibratospeed: Int = 0,
        var vibratodepth: Int = 0,
        var vibratopos: Int = 0,
        var vibratowave: Int = 0,
    )

    var title: String = ""
    var signature: String = ""
    var songlen = 1
    var repeatpos = 0
    var patterntable: Uint8Buffer = Uint8BufferAlloc(0)
    var channels: Int = 4
    var sample = emptyArray<Sample>()
    var samples: Int = 31
    var pattern = emptyArray<Uint8Buffer>()
    var note = emptyArray<Uint8Buffer>()
    var pattern_unpack = emptyArray<Uint8Buffer>()
    var patterns: Int = 0
    var chvu: FloatArray = FloatArray(0)
    var looprow: Int = 0
    var loopstart: Int = 0
    var loopcount: Int = 0
    var patterndelay: Int = 0
    var patternwait: Int = 0

    // clear song data
    fun clearsong() {
        this.title = ""
        this.signature = ""

        this.songlen = 1
        this.repeatpos = 0
        this.patterntable = Uint8BufferAlloc(128)

        this.channels = 4

        this.sample = Array(31) { Sample() }
        this.samples = 31

        this.patterns = 0
        this.pattern = emptyArray()
        this.note = emptyArray()
        this.pattern_unpack = emptyArray()

        this.looprow = 0
        this.loopstart = 0
        this.loopcount = 0

        this.patterndelay = 0
        this.patternwait = 0
    }

    var tick = 0
    var position = 0
    var row = 0
    var offset = 0
    var flags = 0
    var speed = 6
    var bpm = 125
    var breakrow = 0
    var patternjump = 0
    var endofsong = false
    var channel = emptyArray<Channel>()

    // initialize all player variables
    fun initialize() {
        this.syncqueue = IntDeque()

        this.tick = 0
        this.position = 0
        this.row = 0
        this.offset = 0
        this.flags = 0

        this.speed = 6
        this.bpm = 125
        this.breakrow = 0
        this.patternjump = 0
        this.patterndelay = 0
        this.patternwait = 0
        this.endofsong = false

        this.channel = Array(channels) { Channel() }
    }

    init {
        this.clearsong()
        this.initialize()
    }

    // parse the module from local buffer
    fun parse(buffer: Uint8Buffer): Boolean {
        this.signature = (0 until 4).map { buffer[1080 + it].toChar() }.joinToString("")
        when (this.signature) {
            "M.K.", "M!K!", "4CHN", "FLT4" -> Unit
            "6CHN" -> this.channels = 6
            "8CHN", "FLT8" -> this.channels = 8
            "28CH" -> this.channels = 28
            else -> return false
        }
        this.chvu = FloatArray(this.channels)

        run {
            var i = 0
            while (buffer[i] != 0 && i < 20)
                this.title = this.title + buffer[i++].toChar()
        }

        for (i in 0 until this.samples) {
            val st = 20 + i * 30
            var j = 0
            while (buffer[st + j] != 0 && j < 22) {
                this.sample[i].name += if ((buffer[st + j] > 0x1f) && (buffer[st + j] < 0x7f)) (buffer[st + j].toChar()) else ' '
                j++
            }
            this.sample[i].length = 2 * (buffer[st + 22] * 256 + buffer[st + 23])
            this.sample[i].finetune = buffer[st + 24]
            if (this.sample[i].finetune > 7) this.sample[i].finetune = this.sample[i].finetune - 16
            this.sample[i].volume = buffer[st + 25]
            this.sample[i].loopstart = 2 * (buffer[st + 26] * 256 + buffer[st + 27])
            this.sample[i].looplength = 2 * (buffer[st + 28] * 256 + buffer[st + 29])
            if (this.sample[i].looplength == 2) this.sample[i].looplength = 0
            if (this.sample[i].loopstart > this.sample[i].length) {
                this.sample[i].loopstart = 0
                this.sample[i].looplength = 0
            }
        }

        this.songlen = buffer[950]
        if (buffer[951] != 127) this.repeatpos = buffer[951]
        for (i in 0 until 128) {
            this.patterntable[i] = buffer[952 + i]
            if (this.patterntable[i] > this.patterns) this.patterns = this.patterntable[i]
        }
        this.patterns += 1
        val patlen = 4 * 64 * this.channels

        this.pattern = Array(this.patterns) { Uint8BufferAlloc(patlen) }
        this.note = Array(this.patterns) { Uint8BufferAlloc(this.channels * 64) }
        this.pattern_unpack = Array(this.patterns) { Uint8BufferAlloc(this.channels * 64 * 5) }
        for (i in 0 until this.patterns) {
            for (j in 0 until patlen) this.pattern[i][j] = buffer[1084 + i * patlen + j]
            for (j in 0 until 64) for (c in 0 until this.channels) {
                this.note[i][j * this.channels + c] = 0
                val n: Int =
                    ((this.pattern[i][j * 4 * this.channels + c * 4] and 0x0f) shl 8) or this.pattern[i][j * 4 * this.channels + c * 4 + 1]
                for (np in 0 until this.baseperiodtable.size)
                    if (n.toFloat() == this.baseperiodtable[np]) this.note[i][j * this.channels + c] = np
            }
            for (j in 0 until 64) {
                for (c in 0 until this.channels) {
                    val pp = j * 4 * this.channels + c * 4
                    val ppu = j * 5 * this.channels + c * 5
                    var n = ((this.pattern[i][pp] and 0x0f) shl 8) or this.pattern[i][pp + 1]
                    if (n != 0) {
                        n = this.note[i][j * this.channels + c]; n =
                            (n % 12) or ((kotlin.math.floor(n.toDouble() / 12) + 2).toInt() shl 4); }
                    this.pattern_unpack[i][ppu + 0] = if (n != 0) n else 255
                    this.pattern_unpack[i][ppu + 1] =
                        (this.pattern[i][pp + 0] and 0xf0) or (this.pattern[i][pp + 2] ushr 4)
                    this.pattern_unpack[i][ppu + 2] = 255
                    this.pattern_unpack[i][ppu + 3] = (this.pattern[i][pp + 2] and 0x0f)
                    this.pattern_unpack[i][ppu + 4] = this.pattern[i][pp + 3]
                }
            }
        }

        var sst = 1084 + this.patterns * patlen
        for (i in 0 until this.samples) {
            this.sample[i].data = FloatArray(this.sample[i].length)
            for (j in 0 until this.sample[i].length) {
                var q: Float = buffer[sst + j].toFloat()
                if (q < 128) {
                    q = q / 128f
                } else {
                    q = ((q - 128) / 128f) - 1f
                }
                this.sample[i].data[j] = q
            }
            sst += this.sample[i].length
        }

        // look ahead at very first row to see if filter gets enabled
        this.filter = false
        for (ch in 0 until this.channels) {
            val p = this.patterntable[0]
            val pp = ch * 4
            val cmd = this.pattern[p][pp + 2] and 0x0f
            val data = this.pattern[p][pp + 3]
            if (cmd == 0x0e && ((data and 0xf0) == 0x00)) {
                this.filter = (data and 0x01) == 0
            }
        }

        // set lowpass cutoff
        //if (this.context) {
        //  if (this.filter) {
        //    this.lowpassNode.frequency.value=3275;
        //  } else {
        //    this.lowpassNode.frequency.value=28867;
        //  }
        //}

        this.chvu = FloatArray(this.channels)

        return true
    }


    // advance player
    fun advance(mod: Protracker) {
        var spd = (((mod.samplerate * 60) / mod.bpm) / 4) / 6

        // advance player
        if (mod.offset > spd) {
            mod.tick++; mod.offset = 0; mod.flags = mod.flags or 1; }
        if (mod.tick >= mod.speed) {

            if (mod.patterndelay != 0) { // delay pattern
                if (mod.tick < ((mod.patternwait + 1) * mod.speed)) {
                    mod.patternwait++
                } else {
                    mod.row++; mod.tick = 0; mod.flags = mod.flags or 2; mod.patterndelay = 0
                }
            } else {
                if ((mod.flags and (16 + 32 + 64)) != 0) {
                    if ((mod.flags and 64) != 0) { // loop pattern?
                        mod.row = mod.looprow
                        mod.flags = mod.flags and 0xa1
                        mod.flags = mod.flags or 2
                    } else {
                        if ((mod.flags and 16) != 0) { // pattern jump/break?
                            mod.position = mod.patternjump
                            mod.row = mod.breakrow
                            mod.patternjump = 0
                            mod.breakrow = 0
                            mod.flags = mod.flags and 0xe1
                            mod.flags = mod.flags or 2
                        }
                    }
                    mod.tick = 0
                } else {
                    mod.row++; mod.tick = 0; mod.flags = mod.flags or 2
                }
            }
        }
        if (mod.row >= 64) {
            mod.position++; mod.row = 0; mod.flags = mod.flags or 4; }
        if (mod.position >= mod.songlen) {
            if (mod.repeat) {
                mod.position = 0
            } else {
                this.endofsong = true
                //mod.stop();
            }
            return
        }
    }

    // mix an audio buffer with data
    fun mix(mod: Protracker, bufs: Array<FloatArray>, buflen: Int = bufs[0].size) {
        var f: Double
        var p: Int
        var pp: Int
        var n: Int
        var nn: Int

        val outp = FloatArray(2)
        for (s in 0 until buflen) {
            outp[0] = 0f
            outp[1] = 0f

            if (!mod.paused && !mod.endofsong && mod.playing) {
                mod.advance(mod)

                var och = 0
                for (ch in 0 until mod.channels) {

                    // calculate playback position
                    p = mod.patterntable[mod.position]
                    pp = mod.row * 4 * mod.channels + ch * 4
                    if ((mod.flags and 2) != 0) { // row
                        mod.channel[ch].command = mod.pattern[p][pp + 2] and 0x0f
                        mod.channel[ch].data = mod.pattern[p][pp + 3]

                        if (!(mod.channel[ch].command == 0x0e && (mod.channel[ch].data and 0xf0) == 0xd0)) {
                            n = ((mod.pattern[p][pp] and 0x0f) shl 8) or mod.pattern[p][pp + 1]
                            if (n != 0) {
                                // noteon, except if command=3 (porta to note)
                                if ((mod.channel[ch].command != 0x03) && (mod.channel[ch].command != 0x05)) {
                                    mod.channel[ch].period = n
                                    mod.channel[ch].samplepos = 0.0
                                    if (mod.channel[ch].vibratowave > 3) mod.channel[ch].vibratopos = 0
                                    mod.channel[ch].flags = mod.channel[ch].flags or 3 // recalc speed
                                    mod.channel[ch].noteon = 1
                                }
                                // in either case, set the slide to note target
                                mod.channel[ch].slideto = n
                            }
                            nn = (mod.pattern[p][pp + 0] and 0xf0) or (mod.pattern[p][pp + 2] ushr 4)
                            if (nn != 0) {
                                mod.channel[ch].sample = nn - 1
                                mod.channel[ch].volume = mod.sample[nn - 1].volume
                                if (n == 0 && (mod.channel[ch].samplepos > mod.sample[nn - 1].length)) mod.channel[ch].samplepos =
                                    0.0
                            }
                        }
                    }
                    mod.channel[ch].voiceperiod = mod.channel[ch].period.toDouble()

                    // kill empty samples
                    if (mod.sample[mod.channel[ch].sample].length == 0) mod.channel[ch].noteon = 0

                    // effects
                    if ((mod.flags and 1) != 0) {
                        if (mod.tick == 0) {
                            // process only on tick 0
                            mod.effects_t0[mod.channel[ch].command](mod, ch)
                        } else {
                            mod.effects_t1[mod.channel[ch].command](mod, ch)
                        }
                    }

                    // recalc note number from period
                    if ((mod.channel[ch].flags and 2) != 0) {
                        for (np in 0 until mod.baseperiodtable.size)
                            if (mod.baseperiodtable[np] >= mod.channel[ch].period) mod.channel[ch].note = np
                        mod.channel[ch].semitone = 7.0
                        if (mod.channel[ch].period >= 120)
                            mod.channel[ch].semitone =
                                ((mod.baseperiodtable[mod.channel[ch].note] - mod.baseperiodtable[mod.channel[ch].note + 1]).toDouble())
                    }

                    // recalc sample speed and apply finetune
                    if (((mod.channel[ch].flags and 1) != 0 || (mod.flags and 2) != 0) && mod.channel[ch].voiceperiod != 0.0)
                        mod.channel[ch].samplespeed =
                            7093789.2 / (mod.channel[ch].voiceperiod * 2) * mod.finetunetable[mod.sample[mod.channel[ch].sample].finetune + 8] / mod.samplerate

                    // advance vibrato on each tick
                    if ((mod.flags and 1) != 0) {
                        mod.channel[ch].vibratopos += mod.channel[ch].vibratospeed
                        mod.channel[ch].vibratopos = mod.channel[ch].vibratopos and 0x3f
                    }

                    // mix channel to output
                    och = och xor (ch and 1)
                    f = 0.0
                    if (mod.channel[ch].noteon != 0) {
                        if (mod.sample[mod.channel[ch].sample].length > mod.channel[ch].samplepos)
                            f = (mod.sample[mod.channel[ch].sample].data[kotlin.math.floor(mod.channel[ch].samplepos)
                                .toInt()] * mod.channel[ch].volume) / 64.0
                        outp[och] += f.toFloat()
                        mod.channel[ch].samplepos += mod.channel[ch].samplespeed
                    }
                    mod.chvu[ch] = kotlin.math.max(mod.chvu[ch].toDouble(), kotlin.math.abs(f)).toFloat()

                    // loop or end samples
                    if (mod.channel[ch].noteon != 0) {
                        if (mod.sample[mod.channel[ch].sample].loopstart != 0 || mod.sample[mod.channel[ch].sample].looplength != 0) {
                            if (mod.channel[ch].samplepos >= (mod.sample[mod.channel[ch].sample].loopstart + mod.sample[mod.channel[ch].sample].looplength)) {
                                mod.channel[ch].samplepos -= mod.sample[mod.channel[ch].sample].looplength
                            }
                        } else {
                            if (mod.channel[ch].samplepos >= mod.sample[mod.channel[ch].sample].length) {
                                mod.channel[ch].noteon = 0
                            }
                        }
                    }

                    // clear channel flags
                    mod.channel[ch].flags = 0
                }
                mod.offset++
                mod.flags = mod.flags and 0x70
            }

            // done - store to output buffer
            bufs[0][s] = outp[0]
            bufs[1][s] = outp[1]
        }
    }

    //
    // tick 0 effect functions
    //
    fun effect_t0_0(mod: Protracker, ch: Int) { // 0 arpeggio
        mod.channel[ch].arpeggio = mod.channel[ch].data
    }

    fun effect_t0_1(mod: Protracker, ch: Int) { // 1 slide up
        if (mod.channel[ch].data != 0) mod.channel[ch].slidespeed = mod.channel[ch].data
    }

    fun effect_t0_2(mod: Protracker, ch: Int) { // 2 slide down
        if (mod.channel[ch].data != 0) mod.channel[ch].slidespeed = mod.channel[ch].data
    }

    fun effect_t0_3(mod: Protracker, ch: Int) { // 3 slide to note
        if (mod.channel[ch].data != 0) mod.channel[ch].slidetospeed = mod.channel[ch].data
    }

    fun effect_t0_4(mod: Protracker, ch: Int) { // 4 vibrato
        if ((mod.channel[ch].data and 0x0f) != 0 && (mod.channel[ch].data and 0xf0) != 0) {
            mod.channel[ch].vibratodepth = (mod.channel[ch].data and 0x0f)
            mod.channel[ch].vibratospeed = (mod.channel[ch].data and 0xf0) ushr 4
        }
        mod.effects_t1[4](mod, ch)
    }

    fun effect_t0_5(mod: Protracker, ch: Int) { // 5
    }

    fun effect_t0_6(mod: Protracker, ch: Int) { // 6
    }

    fun effect_t0_7(mod: Protracker, ch: Int) { // 7
    }

    fun effect_t0_8(mod: Protracker, ch: Int) { // 8 unused, used for syncing
        mod.syncqueue.addFirst(mod.channel[ch].data and 0x0f)
    }

    fun effect_t0_9(mod: Protracker, ch: Int) { // 9 set sample offset
        mod.channel[ch].samplepos = (mod.channel[ch].data * 256).toDouble()
    }

    fun effect_t0_a(mod: Protracker, ch: Int) { // a
    }

    fun effect_t0_b(mod: Protracker, ch: Int) { // b pattern jump
        mod.breakrow = 0
        mod.patternjump = mod.channel[ch].data
        mod.flags = mod.flags or 16
    }

    fun effect_t0_c(mod: Protracker, ch: Int) { // c set volume
        mod.channel[ch].volume = mod.channel[ch].data
    }

    fun effect_t0_d(mod: Protracker, ch: Int) { // d pattern break
        mod.breakrow = ((mod.channel[ch].data and 0xf0) ushr 4) * 10 + (mod.channel[ch].data and 0x0f)
        if ((mod.flags and 16) == 0) mod.patternjump = mod.position + 1
        mod.flags = mod.flags or 16
    }

    fun effect_t0_e(mod: Protracker, ch: Int) { // e
        val i = (mod.channel[ch].data and 0xf0) ushr 4
        mod.effects_t0_e[i](mod, ch)
    }

    fun effect_t0_f(mod: Protracker, ch: Int) { // f set speed
        if (mod.channel[ch].data > 32) {
            mod.bpm = mod.channel[ch].data
        } else {
            if (mod.channel[ch].data != 0) mod.speed = mod.channel[ch].data
        }
    }


    //
    // tick 0 effect e functions
    //
    fun effect_t0_e0(mod: Protracker, ch: Int) { // e0 filter on/off
        if (mod.channels > 4) return // use only for 4ch amiga tunes
        mod.filter = (mod.channel[ch].data and 0x01) == 0
    }

    fun effect_t0_e1(mod: Protracker, ch: Int) { // e1 fine slide up
        mod.channel[ch].period -= mod.channel[ch].data and 0x0f
        if (mod.channel[ch].period < 113) mod.channel[ch].period = 113
    }

    fun effect_t0_e2(mod: Protracker, ch: Int) { // e2 fine slide down
        mod.channel[ch].period += mod.channel[ch].data and 0x0f
        if (mod.channel[ch].period > 856) mod.channel[ch].period = 856
        mod.channel[ch].flags = mod.channel[ch].flags or 1
    }

    fun effect_t0_e3(mod: Protracker, ch: Int) { // e3 set glissando
    }

    fun effect_t0_e4(mod: Protracker, ch: Int) { // e4 set vibrato waveform
        mod.channel[ch].vibratowave = mod.channel[ch].data and 0x07
    }

    fun effect_t0_e5(mod: Protracker, ch: Int) { // e5 set finetune
    }

    fun effect_t0_e6(mod: Protracker, ch: Int) { // e6 loop pattern
        if ((mod.channel[ch].data and 0x0f) != 0) {
            if (mod.loopcount != 0) {
                mod.loopcount--
            } else {
                mod.loopcount = mod.channel[ch].data and 0x0f
            }
            if (mod.loopcount != 0) mod.flags = mod.flags or 64
        } else {
            mod.looprow = mod.row
        }
    }

    fun effect_t0_e7(mod: Protracker, ch: Int) { // e7
    }

    fun effect_t0_e8(mod: Protracker, ch: Int) { // e8, use for syncing
        mod.syncqueue.addFirst(mod.channel[ch].data and 0x0f)
    }

    fun effect_t0_e9(mod: Protracker, ch: Int) { // e9
    }

    fun effect_t0_ea(mod: Protracker, ch: Int) { // ea fine volslide up
        mod.channel[ch].volume += mod.channel[ch].data and 0x0f
        if (mod.channel[ch].volume > 64) mod.channel[ch].volume = 64
    }

    fun effect_t0_eb(mod: Protracker, ch: Int) { // eb fine volslide down
        mod.channel[ch].volume -= mod.channel[ch].data and 0x0f
        if (mod.channel[ch].volume < 0) mod.channel[ch].volume = 0
    }

    fun effect_t0_ec(mod: Protracker, ch: Int) { // ec
    }

    fun effect_t0_ed(mod: Protracker, ch: Int) { // ed delay sample
        if (mod.tick == (mod.channel[ch].data and 0x0f)) {
            // start note
            val p = mod.patterntable[mod.position]
            val pp = mod.row * 4 * mod.channels + ch * 4
            var n: Int = 0
            n = ((mod.pattern[p][pp] and 0x0f) shl 8) or mod.pattern[p][pp + 1]
            if (n != 0) {
                mod.channel[ch].period = n
                mod.channel[ch].voiceperiod = mod.channel[ch].period.toDouble()
                mod.channel[ch].samplepos = 0.0
                if (mod.channel[ch].vibratowave > 3) mod.channel[ch].vibratopos = 0
                mod.channel[ch].flags = mod.channel[ch].flags or 3 // recalc speed
                mod.channel[ch].noteon = 1
            }
            n = (mod.pattern[p][pp + 0] and 0xf0) or (mod.pattern[p][pp + 2] ushr 4)
            if (n != 0) {
                mod.channel[ch].sample = n - 1
                mod.channel[ch].volume = mod.sample[n - 1].volume
            }
        }
    }

    fun effect_t0_ee(mod: Protracker, ch: Int) { // ee delay pattern
        mod.patterndelay = mod.channel[ch].data and 0x0f
        mod.patternwait = 0
    }

    fun effect_t0_ef(mod: Protracker, ch: Int) { // ef
    }


    //
    // tick 1+ effect functions
    //
    fun effect_t1_0(mod: Protracker, ch: Int) { // 0 arpeggio
        if (mod.channel[ch].data != 0) {
            var apn = mod.channel[ch].note
            if ((mod.tick % 3) == 1) apn += mod.channel[ch].arpeggio ushr 4
            if ((mod.tick % 3) == 2) apn += mod.channel[ch].arpeggio and 0x0f
            if (apn >= 0 && apn <= mod.baseperiodtable.size)
                mod.channel[ch].voiceperiod = mod.baseperiodtable[apn].toDouble()
            mod.channel[ch].flags = mod.channel[ch].flags or 1
        }
    }

    fun effect_t1_1(mod: Protracker, ch: Int) { // 1 slide up
        mod.channel[ch].period -= mod.channel[ch].slidespeed
        if (mod.channel[ch].period < 113) mod.channel[ch].period = 113
        mod.channel[ch].flags = mod.channel[ch].flags or 3 // recalc speed
    }

    fun effect_t1_2(mod: Protracker, ch: Int) { // 2 slide down
        mod.channel[ch].period += mod.channel[ch].slidespeed
        if (mod.channel[ch].period > 856) mod.channel[ch].period = 856
        mod.channel[ch].flags = mod.channel[ch].flags or 3 // recalc speed
    }

    fun effect_t1_3(mod: Protracker, ch: Int) { // 3 slide to note
        if (mod.channel[ch].period < mod.channel[ch].slideto) {
            mod.channel[ch].period += mod.channel[ch].slidetospeed
            if (mod.channel[ch].period > mod.channel[ch].slideto)
                mod.channel[ch].period = mod.channel[ch].slideto
        }
        if (mod.channel[ch].period > mod.channel[ch].slideto) {
            mod.channel[ch].period -= mod.channel[ch].slidetospeed
            if (mod.channel[ch].period < mod.channel[ch].slideto)
                mod.channel[ch].period = mod.channel[ch].slideto
        }
        mod.channel[ch].flags = mod.channel[ch].flags or 3 // recalc speed
    }

    fun effect_t1_4(mod: Protracker, ch: Int) { // 4 vibrato
        val waveform = mod.vibratotable[mod.channel[ch].vibratowave and 3][mod.channel[ch].vibratopos] / 63.0 //127.0;

        // two different implementations for vibrato
        //  var a=(mod.channel[ch].vibratodepth/32)*mod.channel[ch].semitone*waveform; // non-linear vibrato +/- semitone
        val a = mod.channel[ch].vibratodepth * waveform // linear vibrato, depth has more effect high notes

        mod.channel[ch].voiceperiod += a
        mod.channel[ch].flags = mod.channel[ch].flags or 1
    }

    fun effect_t1_5(mod: Protracker, ch: Int) { // 5 volslide + slide to note
        mod.effect_t1_3(mod, ch) // slide to note
        mod.effect_t1_a(mod, ch) // volslide
    }

    fun effect_t1_6(mod: Protracker, ch: Int) { // 6 volslide + vibrato
        mod.effect_t1_4(mod, ch) // vibrato
        mod.effect_t1_a(mod, ch) // volslide
    }

    fun effect_t1_7(mod: Protracker, ch: Int) { // 7
    }

    fun effect_t1_8(mod: Protracker, ch: Int) { // 8 unused
    }

    fun effect_t1_9(mod: Protracker, ch: Int) { // 9 set sample offset
    }

    fun effect_t1_a(mod: Protracker, ch: Int) { // a volume slide
        if ((mod.channel[ch].data and 0x0f) == 0) {
            // y is zero, slide up
            mod.channel[ch].volume += (mod.channel[ch].data ushr 4)
            if (mod.channel[ch].volume > 64) mod.channel[ch].volume = 64
        }
        if ((mod.channel[ch].data and 0xf0) == 0) {
            // x is zero, slide down
            mod.channel[ch].volume -= (mod.channel[ch].data and 0x0f)
            if (mod.channel[ch].volume < 0) mod.channel[ch].volume = 0
        }
    }

    fun effect_t1_b(mod: Protracker, ch: Int) { // b pattern jump
    }

    fun effect_t1_c(mod: Protracker, ch: Int) { // c set volume
    }

    fun effect_t1_d(mod: Protracker, ch: Int) { // d pattern break
    }

    fun effect_t1_e(mod: Protracker, ch: Int) { // e
        val i = (mod.channel[ch].data and 0xf0) ushr 4
        mod.effects_t1_e[i](mod, ch)
    }

    fun effect_t1_f(mod: Protracker, ch: Int) { // f
    }


    //
    // tick 1+ effect e functions
    //
    fun effect_t1_e0(mod: Protracker, ch: Int) { // e0
    }

    fun effect_t1_e1(mod: Protracker, ch: Int) { // e1
    }

    fun effect_t1_e2(mod: Protracker, ch: Int) { // e2
    }

    fun effect_t1_e3(mod: Protracker, ch: Int) { // e3
    }

    fun effect_t1_e4(mod: Protracker, ch: Int) { // e4
    }

    fun effect_t1_e5(mod: Protracker, ch: Int) { // e5
    }

    fun effect_t1_e6(mod: Protracker, ch: Int) { // e6
    }

    fun effect_t1_e7(mod: Protracker, ch: Int) { // e7
    }

    fun effect_t1_e8(mod: Protracker, ch: Int) { // e8
    }

    fun effect_t1_e9(mod: Protracker, ch: Int) { // e9 retrig sample
        if (mod.tick % (mod.channel[ch].data and 0x0f) == 0)
            mod.channel[ch].samplepos = 0.0
    }

    fun effect_t1_ea(mod: Protracker, ch: Int) { // ea
    }

    fun effect_t1_eb(mod: Protracker, ch: Int) { // eb
    }

    fun effect_t1_ec(mod: Protracker, ch: Int) { // ec cut sample
        if (mod.tick == (mod.channel[ch].data and 0x0f))
            mod.channel[ch].volume = 0
    }

    fun effect_t1_ed(mod: Protracker, ch: Int) { // ed delay sample
        mod.effect_t0_ed(mod, ch)
    }

    fun effect_t1_ee(mod: Protracker, ch: Int) { // ee
    }

    fun effect_t1_ef(mod: Protracker, ch: Int) { // ef
    }

    suspend fun createSound(soundProvider: NativeSoundProvider = nativeSoundProvider): Sound {
        return soundProvider.createStreamingSound(createAudioStream())
    }

    fun createAudioStream(): AudioStream {
        playing = true
        var fch = Array(2) { FloatArray(0) }
        return object : AudioStream(44100, 2) {
            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                if (fch[0].size < length) fch = Array(2) { FloatArray(length) }
                mix(this@Protracker, fch, length)
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

suspend fun VfsFile.readMOD(soundProvider: NativeSoundProvider = nativeSoundProvider): Sound {
    val tracker = Protracker()
    val bytes = readBytes()
    val buf = Uint8Buffer(NewInt8Buffer(MemBufferWrap(bytes), 0, bytes.size))
    tracker.parse(buf)
    return tracker.createSound(soundProvider)
}
