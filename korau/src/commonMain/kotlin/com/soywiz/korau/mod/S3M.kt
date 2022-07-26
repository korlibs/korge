@file:Suppress("unused", "NAME_SHADOWING", "UNUSED_PARAMETER", "FunctionName", "MemberVisibilityCanBePrivate")

package com.soywiz.korau.mod

import com.soywiz.kds.IntDeque
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8BufferAlloc
import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.mem
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.file.VfsFile
import kotlin.random.Random

/*
  (c) 2012-2021 Noora Halme et al. (see AUTHORS)

  This code is licensed under the MIT license:
  http://www.opensource.org/licenses/mit-license.php

  Scream Tracker 3 module player class

  todo:
  - are Exx, Fxx and Gxx supposed to share a single
    command data memory?
*/
suspend fun VfsFile.readS3M(soundProvider: NativeSoundProvider = nativeSoundProvider): Sound =
    Screamtracker().createSoundFromFile(this)

class Screamtracker : BaseModuleTracker() {
    var paused = false
    var repeat = false

    var filter = false

    var syncqueue = IntDeque()

    var title: String = ""
    var signature: String = ""
    var songlen: Int = 1
    var repeatpos: Int = 0
    var patterntable: Uint8Buffer = Uint8BufferAlloc(0)
    var channels: Int = 0
    var ordNum: Int = 0
    var insNum: Int = 0
    var patNum: Int = 0
    var globalVol: Int = 64
    var initSpeed: Int = 6
    var initBPM: Int = 125
    var fastslide: Int = 0
    var mixval: Double = 8.0
    var sample: Array<Sample> = emptyArray()
    var pattern: Array<Uint8Buffer> = emptyArray()
    var channel: Array<Channel> = emptyArray()
    var looprow: Int = 0
    var loopstart: Int = 0
    var loopcount: Int = 0
    var patterndelay: Int = 0
    var patternwait: Int = 0

    var tick: Int = -1
    var position: Int = 0
    var row: Int = 0
    var flags: Int = 0

    var patterns: Int = 0

    var chvu: FloatArray = FloatArray(0)

    var volume: Int = this.globalVol
    var speed: Int = this.initSpeed
    var bpm: Int = this.initBPM
    var stt: Double = 0.0
    var breakrow: Int = 0
    var patternjump: Int = 0

    val mod = this

    val periodtable = floatArrayOf(
        27392f, 25856f, 24384f, 23040f, 21696f, 20480f, 19328f, 18240f, 17216f, 16256f, 15360f, 14496f,
        13696f, 12928f, 12192f, 11520f, 10848f, 10240f, 9664f, 9120f, 8608f, 8128f, 7680f, 7248f,
        6848f, 6464f, 6096f, 5760f, 5424f, 5120f, 4832f, 4560f, 4304f, 4064f, 3840f, 3624f,
        3424f, 3232f, 3048f, 2880f, 2712f, 2560f, 2416f, 2280f, 2152f, 2032f, 1920f, 1812f,
        1712f, 1616f, 1524f, 1440f, 1356f, 1280f, 1208f, 1140f, 1076f, 1016f, 960f, 906f,
        856f, 808f, 762f, 720f, 678f, 640f, 604f, 570f, 538f, 508f, 480f, 453f,
        428f, 404f, 381f, 360f, 339f, 320f, 302f, 285f, 269f, 254f, 240f, 226f,
        214f, 202f, 190f, 180f, 170f, 160f, 151f, 143f, 135f, 127f, 120f, 113f,
        107f, 101f, 95f, 90f, 85f, 80f, 75f, 71f, 67f, 63f, 60f, 56f
    )

    val retrigvoltab = floatArrayOf(
        0f, -1f, -2f, -4f, -8f, -16f, 0.66f, 0.5f,
        0f, 1f, 2f, 4f, 8f, 16f, 1.50f, 2.0f
    )

    val pan_r = FloatArray(32) { 0.5f }
    val pan_l = FloatArray(32) { 0.5f }

    // calc tables for vibrato waveforms
    val vibratotable = arrayOf(
        FloatArray(256) { 127f * kotlin.math.sin(kotlin.math.PI * 2 * (it / 256f)).toFloat() },
        FloatArray(256) { 127f - it },
        FloatArray(256) { if (it < 128) 127f else -128f },
        FloatArray(256) { Random.nextFloat() * 255 - 128 },
    )

    // effect jumptables for tick 0 and tick 1+
    val effects_t0 = arrayOf(
        null, // zero is ignored
        ::effect_t0_a, ::effect_t0_b, ::effect_t0_c, ::effect_t0_d, ::effect_t0_e,
        ::effect_t0_f, ::effect_t0_g, ::effect_t0_h, ::effect_t0_i, ::effect_t0_j,
        ::effect_t0_k, ::effect_t0_l, ::effect_t0_m, ::effect_t0_n, ::effect_t0_o,
        ::effect_t0_p, ::effect_t0_q, ::effect_t0_r, ::effect_t0_s, ::effect_t0_t,
        ::effect_t0_u, ::effect_t0_v, ::effect_t0_w, ::effect_t0_x, ::effect_t0_y,
        ::effect_t0_z
    )
    val effects_t0_s = arrayOf(
        ::effect_t0_s0, ::effect_t0_s1, ::effect_t0_s2, ::effect_t0_s3, ::effect_t0_s4,
        ::effect_t0_s5, ::effect_t0_s6, ::effect_t0_s7, ::effect_t0_s8, ::effect_t0_s9,
        ::effect_t0_sa, ::effect_t0_sb, ::effect_t0_sc, ::effect_t0_sd, ::effect_t0_se,
        ::effect_t0_sf
    )
    val effects_t1 = arrayOf(
        null, // zero is ignored
        ::effect_t1_a, ::effect_t1_b, ::effect_t1_c, ::effect_t1_d, ::effect_t1_e,
        ::effect_t1_f, ::effect_t1_g, ::effect_t1_h, ::effect_t1_i, ::effect_t1_j,
        ::effect_t1_k, ::effect_t1_l, ::effect_t1_m, ::effect_t1_n, ::effect_t1_o,
        ::effect_t1_p, ::effect_t1_q, ::effect_t1_r, ::effect_t1_s, ::effect_t1_t,
        ::effect_t1_u, ::effect_t1_v, ::effect_t1_w, ::effect_t1_x, ::effect_t1_y,
        ::effect_t1_z
    )
    val effects_t1_s = arrayOf(
        ::effect_t1_s0, ::effect_t1_s1, ::effect_t1_s2, ::effect_t1_s3, ::effect_t1_s4,
        ::effect_t1_s5, ::effect_t1_s6, ::effect_t1_s7, ::effect_t1_s8, ::effect_t1_s9,
        ::effect_t1_sa, ::effect_t1_sb, ::effect_t1_sc, ::effect_t1_sd, ::effect_t1_se,
        ::effect_t1_sf
    )

    init {
        this.clearsong()
        this.initialize()
    }

    class Sample(
        var length: Int = 0,
        var loopstart: Int = 0,
        var loopend: Int = 0,
        var looplength: Int = 0,
        var volume: Int = 64,
        var loop: Int = 0,
        var c2spd: Int = 8363,
        var name: String = "",
        var data: FloatArray = FloatArray(0),
        var stereo: Int = 0,
        var bits: Int = 8,
    )

    // clear song data
    fun clearsong() {
        this.title = ""
        this.signature = ""

        this.songlen = 1
        this.repeatpos = 0
        this.patterntable = Uint8BufferAlloc(256)

        this.channels = 0
        this.ordNum = 0
        this.insNum = 0
        this.patNum = 0

        this.globalVol = 64
        this.initSpeed = 6
        this.initBPM = 125

        this.fastslide = 0

        this.mixval = 8.0

        this.sample = Array(256) { Sample() }

        this.pattern = emptyArray()

        this.looprow = 0
        this.loopstart = 0
        this.loopcount = 0

        this.patterndelay = 0
        this.patternwait = 0
    }

    class Channel(
        var sample: Int = 0,
        var note: Int = 24,
        var command: Int = 0,
        var data: Int = 0,
        var samplepos: Double = 0.0,
        var samplespeed: Double = 0.0,
        var flags: Int = 0,
        var noteon: Int = 0,

        var slidespeed: Int = 0,
        var slideto: Double = 0.0,
        var slidetospeed: Int = 0,
        var arpeggio: Int = 0,

        var period: Double = 0.0,
        var volume: Int = 64,
        var voiceperiod: Double = 0.0,
        var voicevolume: Int = 0,
        var oldvoicevolume: Int = 0,

        var semitone: Int = 12,
        var vibratospeed: Int = 0,
        var vibratodepth: Int = 0,
        var vibratopos: Int = 0,
        var vibratowave: Int = 0,

        var lastoffset: Int = 0,
        var lastretrig: Int = 0,

        var volramp: Double = 0.0,
        var volrampfrom: Int = 0,

        var trigramp: Double = 0.0,
        var trigrampfrom: Double = 0.0,

        var currentsample: Double = 0.0,
        var lastsample: Double = 0.0,

        var volslide: Int = 0,
    )

    // initialize all player variables to defaults prior to starting playback
    override fun initialize() {
        this.syncqueue = IntDeque()

        this.tick = -1
        this.position = 0
        this.row = 0
        this.flags = 0

        this.volume = this.globalVol
        this.speed = this.initSpeed
        this.bpm = this.initBPM
        this.stt = 0.0
        this.breakrow = 0
        this.patternjump = 0
        this.patterndelay = 0
        this.patternwait = 0
        this.endofsong = false

        this.channel = Array(this.channels) { Channel() }
    }

    // parse the module from local buffer
    override fun parse(buffer: Uint8Buffer): Boolean {
        // check s3m signature and type
        this.signature = CharArray(4) { buffer[0x002c + it].toChar() }.concatToString()
        if (this.signature != "SCRM") return false
        if (buffer[0x001d] != 0x10) return false

        // get channel count
        this.channels = 0
        for (i in 0 until 32) {
            if ((buffer[0x0040 + i] and 0x80) != 0) break
            this.channels++
        }

        // default panning 3/C/3/...
        for (i in 0 until 32) {
            if ((buffer[0x0040 + i] and 0x80) == 0) {
                val c = buffer[0x0040 + i] and 15
                if (c < 8) {
                    this.pan_r[i] = 0.2f
                    this.pan_l[i] = 0.8f
                } else {
                    this.pan_r[i] = 0.8f
                    this.pan_l[i] = 0.2f
                }
            }
        }

        this.title = CharArray(0x1c) { dos2utf(buffer[it]) }.concatToString().trimEnd('\u0000')

        this.ordNum = buffer[0x0020] or (buffer[0x0021] shl 8)
        this.insNum = buffer[0x0022] or (buffer[0x0023] shl 8)
        this.patNum = buffer[0x0024] or (buffer[0x0025] shl 8)

        this.globalVol = buffer[0x0030]
        this.initSpeed = buffer[0x0031]
        this.initBPM = buffer[0x0032]

        this.fastslide = if ((buffer[0x0026] and 64) != 0) 1 else 0

        this.speed = this.initSpeed
        this.bpm = this.initBPM

        // check for additional panning info
        if (buffer[0x0035] == 0xfc) {
            for (i in 0 until 32) {
                var c = buffer[0x0070 + this.ordNum + this.insNum * 2 + this.patNum * 2 + i]
                if ((c and 0x10) != 0) {
                    c = c and 0x0f
                    this.pan_r[i] = (c / 15.0).toFloat()
                    this.pan_l[i] = (1.0 - this.pan_r[i]).toFloat()
                }
            }
        }

        // check for mono panning
        this.mixval = buffer[0x0033].toDouble()
        if ((this.mixval.toInt() and 0x80) == 0x80) {
            for (i in 0 until 32) {
                this.pan_r[i] = 0.5f
                this.pan_l[i] = 0.5f
            }
        }

        // calculate master mix scaling factor
        this.mixval = 128.0 / kotlin.math.max(0x10, this.mixval.toInt() and 0x7f)
            .toDouble() // (8.0 when mastervol is 0x10, 1.0 when mastervol is 0x7f)

        // load orders
        for (i in 0 until this.ordNum) this.patterntable[i] = buffer[0x0060 + i]
        this.songlen = 0
        for (i in 0 until this.ordNum) if (this.patterntable[i] != 255) this.songlen++

        // load instruments
        this.sample = Array(this.insNum) { Sample() }
        for (i in 0 until this.insNum) {

            val offset = (buffer[0x0060 + this.ordNum + i * 2] or (buffer[0x0060 + this.ordNum + i * 2 + 1] shl 8)) * 16
            var j = 0
            val sample = this.sample[i]
            sample.name = CharArray(28) { dos2utf(buffer[offset + 0x0030 + it]) }.concatToString().trimEnd('\u0000')
            sample.length = buffer[offset + 0x10] or (buffer[offset + 0x11] shl 8)
            sample.loopstart = buffer[offset + 0x14] or (buffer[offset + 0x15] shl 8)
            sample.loopend = buffer[offset + 0x18] or (buffer[offset + 0x19] shl 8)
            sample.looplength = sample.loopend - sample.loopstart
            sample.volume = buffer[offset + 0x1c]
            sample.loop = buffer[offset + 0x1f] and 1
            sample.stereo = (buffer[offset + 0x1f] and 2) ushr 1
            sample.bits = if ((buffer[offset + 0x1f] and 4) != 0) 16 else 8
            sample.c2spd = buffer[offset + 0x20] or (buffer[offset + 0x21] shl 8)

            // sample data
            val smpoffset =
                ((buffer[offset + 0x0d] shl 16) or (buffer[offset + 0x0e]) or (buffer[offset + 0x0f] shl 8) * 16)
            sample.data = FloatArray(sample.length)
            for (j in 0 until sample.length) {
                sample.data[j] =
                    (buffer[smpoffset + j] - 128).toFloat() / 128f
            } // convert to mono float signed
        }

        // load and unpack patterns
        var max_ch = 0
        this.pattern = Array(this.patNum) { Uint8BufferAlloc(this.channels * 64 * 5) }
        for (i in 0 until this.patNum) {
            val boffset = 0x0060 + this.ordNum + this.insNum * 2 + i * 2
            var offset = (buffer[boffset] or (buffer[boffset + 1] shl 8)) * 16
            var patlen = buffer[offset] or (buffer[offset + 1] shl 8)
            var row = 0
            var pos = 0
            var ch = 0

            val pattern = this.pattern[i]
            for (row in 0 until 64) {
                for (ch in 0 until this.channels) {
                    val opattern = row * this.channels * 5 + ch * 5
                    pattern[opattern + 0] = 255
                    pattern[opattern + 1] = 0
                    pattern[opattern + 2] = 255
                    pattern[opattern + 3] = 255
                    pattern[opattern + 4] = 0
                }
            }

            if (offset == 0) continue // fix for control_e.s3m
            row = 0
            ch = 0
            offset += 2
            while (row < 64) {
                val c = buffer[offset + pos++]
                if (c != 0) {
                    ch = c and 31
                    if (ch < this.channels) {
                        if (ch > max_ch) {
                            for (j in 0 until this.songlen) {
                                if (this.patterntable[j] == i) max_ch = ch
                            } // only if pattern is actually used
                        }
                        val opattern = row * this.channels * 5 + ch * 5
                        if ((c and 32) != 0) {
                            pattern[opattern + 0] = buffer[offset + pos++] // note
                            pattern[opattern + 1] = buffer[offset + pos++] // instrument
                        }
                        if ((c and 64) != 0) {
                            pattern[opattern + 2] = buffer[offset + pos++]
                        } // volume
                        if ((c and 128) != 0) {
                            pattern[opattern + 3] = buffer[offset + pos++] // command
                            pattern[opattern + 4] = buffer[offset + pos++] // parameter
                            if (pattern[opattern + 3] == 0 || pattern[opattern + 3] > 26) {
                                pattern[opattern + 3] = 255
                            }
                        }
                    } else {
                        if ((c and 32) != 0) pos += 2
                        if ((c and 64) != 0) pos++
                        if ((c and 128) != 0) pos += 2
                    }
                } else row++
            }
        }
        this.patterns = this.patNum

        // how many channels had actually pattern data on them? trim off the extra channels
        val oldch = this.channels
        this.channels = max_ch + 1
        for (i in 0 until this.patNum) {
            val oldpat = Uint8BufferAlloc(this.pattern[i].size)
            arraycopy(this.pattern[i].b.mem, 0, oldpat.b.mem, 0, this.pattern[i].size)
            this.pattern[i] = Uint8BufferAlloc(this.channels * 64 * 5)
            val pattern = this.pattern[i]
            for (j in 0 until 64) {
                for (c in 0 until this.channels) {
                    val offsetpat = j * this.channels * 5 + c * 5
                    val offsetoldpat = j * oldch * 5 + c * 5
                    pattern[offsetpat + 0] = oldpat[offsetoldpat + 0]
                    pattern[offsetpat + 1] = oldpat[offsetoldpat + 1]
                    pattern[offsetpat + 2] = oldpat[offsetoldpat + 2]
                    pattern[offsetpat + 3] = oldpat[offsetoldpat + 3]
                    pattern[offsetpat + 4] = oldpat[offsetoldpat + 4]
                }
            }
        }

        this.chvu = FloatArray(this.channels) { 0f }

        return true
    }


    // advance player
    fun advance(mod: Screamtracker) {
        this.stt = (((this.samplerate * 60) / this.bpm.toDouble()) / 4.0) / 6.0 // samples to tick

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
                    if ((this.flags and 64) != 0) { // loop pattern?
                        this.row = this.looprow
                        this.flags = this.flags and 0xa1
                        this.flags = this.flags or 2
                    } else {
                        if ((this.flags and 16) != 0) { // pattern jump/break?
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
        if (this.row >= 64) {
            this.position++
            this.row = 0
            this.flags = this.flags or 4
            while (this.patterntable[this.position] == 254) this.position++ // skip markers
        }

        // end of song?
        if (this.position >= this.songlen || this.patterntable[this.position] == 255) {
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

        val pp: Int = this.row * 5 * this.channels + ch * 5

        val n = this.pattern[p][pp]
        val s = this.pattern[p][pp + 1]
        val channel = this.channel[ch]
        if (s != 0) {
            channel.sample = s - 1
            channel.volume = this.sample[s - 1].volume
            channel.voicevolume = channel.volume
            if (n == 255 && (channel.samplepos > this.sample[s - 1].length)) {
                channel.trigramp = 0.0
                channel.trigrampfrom = channel.currentsample
                channel.samplepos = 0.0
            }
        }

        if (n < 254) {
            // calc period for note
            val n = (n and 0x0f) + (n ushr 4) * 12
            val pv = (8363.0 * this.periodtable[n]) / this.sample[channel.sample].c2spd.toDouble()

            // noteon, except if command=0x07 ('G') (porta to note) or 0x0c ('L') (porta+volslide)
            if ((channel.command != 0x07) && (channel.command != 0x0c)) {
                channel.note = n
                channel.period = pv
                channel.voiceperiod = channel.period
                channel.samplepos = 0.0
                if (channel.vibratowave > 3) channel.vibratopos = 0

                channel.trigramp = 0.0
                channel.trigrampfrom = channel.currentsample

                channel.flags = channel.flags or 3 // force sample speed recalc
                channel.noteon = 1
            }
            // in either case, set the slide to note target to note period
            channel.slideto = pv
        } else if (n == 254) {
            channel.noteon = 0 // sample off
            channel.voicevolume = 0
        }

        if (this.pattern[p][pp + 2] <= 64) {
            channel.volume = this.pattern[p][pp + 2]
            channel.voicevolume = channel.volume
        }
    }


    // advance player and all channels by a tick
    fun process_tick(mod: Screamtracker) {

        // advance global player state by a tick
        this.advance(mod)

        // advance all channels
        for (ch in 0 until this.channels) {

            // calculate playback position
            val p = this.patterntable[this.position]
            val pp = this.row * 5 * this.channels + ch * 5

            val channel = this.channel[ch]
            channel.oldvoicevolume = channel.voicevolume

            if ((this.flags and 2) != 0) { // new row
                channel.command = this.pattern[p][pp + 3]
                channel.data = this.pattern[p][pp + 4]
                if (!(channel.command == 0x13 && (channel.data and 0xf0) == 0xd0)) { // note delay?
                    this.process_note(p, ch)
                }
            }

            // kill empty samples
            if (this.sample[channel.sample].length == 0) channel.noteon = 0

            // run effects on each new tick
            if (channel.command < 27) {
                if (this.tick == 0) {
                    // process only on tick 0 effects
                    this.effects_t0[channel.command]?.invoke(ch)
                } else {
                    this.effects_t1[channel.command]?.invoke(ch)
                }
            }

            // advance vibrato on each new tick
            channel.vibratopos += channel.vibratospeed * 2
            channel.vibratopos = channel.vibratopos and 0xff

            if (channel.oldvoicevolume != channel.voicevolume) {
                channel.volrampfrom = channel.oldvoicevolume
                channel.volramp = 0.0
            }

            // recalc sample speed if voiceperiod has changed
            if (((channel.flags and 1) != 0 || (this.flags and 2) != 0) && channel.voiceperiod != 0.0)
                channel.samplespeed = (14317056.0 / channel.voiceperiod) / this.samplerate.toDouble()

            // clear channel flags
            channel.flags = 0
        }

        // clear global flags after all channels are processed
        this.flags = this.flags and 0x70
    }


    // mix an audio buffer with data
    override fun mix(bufs: Array<FloatArray>, buflen: Int) {
        val outp = FloatArray(2)

        // return a buffer of silence if not playing
        if (this.paused || this.endofsong || !this.playing) {
            for (s in 0 until buflen) {
                bufs[0][s] = 0f
                bufs[1][s] = 0f
                for (ch in 0 until this.chvu.size) this.chvu[ch] = 0f
            }
            return
        }

        // fill audiobuffer
        for (s in 0 until buflen) {
            outp[0] = 0f
            outp[1] = 0f

            // if STT has run out, step player forward by tick
            if (this.stt <= 0) this.process_tick(mod)

            // mix channels
            for (ch in 0 until this.channels) {
                var fl = 0.0
                var fr = 0.0
                var fs = 0.0
                val channel = this.channel[ch]
                val si = channel.sample

                // add channel output to left/right master outputs
                channel.currentsample = 0.0 // assume note is off
                if (channel.noteon != 0 || (channel.noteon == 0 && channel.volramp < 1.0)) {
                    if (this.sample[si].length > channel.samplepos) {
                        fl = channel.lastsample

                        // interpolate towards current sample
                        var f = channel.samplepos - kotlin.math.floor(channel.samplepos)
                        fs = this.sample[si].data[kotlin.math.floor(channel.samplepos).toInt()].toDouble()
                        fl = f * fs + (1.0 - f) * fl

                        // smooth out discontinuities from retrig and sample offset
                        f = channel.trigramp
                        fl = f * fl + (1.0 - f) * channel.trigrampfrom
                        f += 1.0 / 128.0
                        channel.trigramp = kotlin.math.min(1.0, f)
                        channel.currentsample = fl

                        // ramp volume changes over 64 samples to avoid clicks
                        fr = fl * (channel.voicevolume / 64.0)
                        f = channel.volramp
                        fl = f * fr + (1.0 - f) * (fl * (channel.volrampfrom / 64.0))
                        f += (1.0 / 64.0)
                        channel.volramp = kotlin.math.min(1.0, f)

                        // pan samples
                        fr = fl * this.pan_r[ch]
                        fl *= this.pan_l[ch]
                    }
                    outp[0] = (outp[0] + fl).toFloat()
                    outp[1] = (outp[1] + fr).toFloat()

                    val oldpos = channel.samplepos
                    channel.samplepos += channel.samplespeed
                    if (kotlin.math.floor(channel.samplepos) > kotlin.math.floor(oldpos)) {
                        channel.lastsample = fs
                    }

                    // loop or stop sample?
                    val sample = this.sample[channel.sample]
                    when {
                        sample.loop != 0 -> {
                            if (channel.samplepos >= sample.loopend) {
                                channel.samplepos -= sample.looplength
                                channel.lastsample = channel.currentsample
                            }
                        }
                        channel.samplepos >= sample.length -> {
                            channel.noteon = 0
                        }
                    }
                }
                this.chvu[ch] = kotlin.math.max(this.chvu[ch].toDouble(), kotlin.math.abs(fl + fr)).toFloat()
            }

            // done - store to output buffer
            val t = this.volume / 64.0
            bufs[0][s] = (outp[0] * t).toFloat()
            bufs[1][s] = (outp[1] * t).toFloat()
            this.stt--
        }
    }


    //
    // tick 0 effect functions
    //
    fun effect_t0_a(ch: Int) { // set speed
        val channel = this.channel[ch]
        if (channel.data > 0) this.speed = channel.data
    }

    fun effect_t0_b(ch: Int) { // pattern jump
        val channel = this.channel[ch]
        this.breakrow = 0
        this.patternjump = channel.data
        this.flags = this.flags or 16
    }

    fun effect_t0_c(ch: Int) { // pattern break
        val channel = this.channel[ch]
        this.breakrow = ((channel.data and 0xf0) ushr 4) * 10 + (channel.data and 0x0f)
        if ((this.flags and 16) == 0) this.patternjump = this.position + 1
        this.flags = this.flags or 16
    }

    fun effect_t0_d(ch: Int) { // volume slide
        val channel = this.channel[ch]
        if (channel.data != 0) channel.volslide = channel.data
        // DxF fine up
        // DFx fine down
        when {
            (channel.volslide and 0x0f) == 0x0f -> channel.voicevolume += channel.volslide ushr 4
            (channel.volslide ushr 4) == 0x0f -> channel.voicevolume -= channel.volslide and 0x0f
            else -> if (this.fastslide != 0) this.effect_t1_d(ch)
        }

        if (channel.voicevolume < 0) channel.voicevolume = 0
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_t0_e(ch: Int) { // slide down
        val channel = this.channel[ch]
        if (channel.data != 0) {
            channel.slidespeed = channel.data
        }
        if ((channel.slidespeed and 0xf0) == 0xf0) {
            channel.voiceperiod += (channel.slidespeed and 0x0f) shl 2
        }
        if ((channel.slidespeed and 0xf0) == 0xe0) {
            channel.voiceperiod += (channel.slidespeed and 0x0f)
        }
        if (channel.voiceperiod > 27392) {
            channel.noteon = 0
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_f(ch: Int) { // slide up
        val channel = this.channel[ch]
        if (channel.data != 0) channel.slidespeed = channel.data
        if ((channel.slidespeed and 0xf0) == 0xf0) {
            channel.voiceperiod -= (channel.slidespeed and 0x0f) shl 2
        }
        if ((channel.slidespeed and 0xf0) == 0xe0) {
            channel.voiceperiod -= (channel.slidespeed and 0x0f)
        }
        if (channel.voiceperiod < 56) {
            channel.noteon = 0
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_g(ch: Int) { // slide to note
        val channel = this.channel[ch]
        //  if (channel.data) channel.slidetospeed=channel.data;
        if (channel.data != 0) channel.slidespeed = channel.data
    }

    fun effect_t0_h(ch: Int) { // vibrato
        val channel = this.channel[ch]
        if ((channel.data and 0x0f) != 0 && (channel.data and 0xf0) != 0) {
            channel.vibratodepth = (channel.data and 0x0f)
            channel.vibratospeed = (channel.data and 0xf0) ushr 4
        }
    }

    fun effect_t0_i(ch: Int) { // tremor
    }

    fun effect_t0_j(ch: Int) { // arpeggio
        val channel = this.channel[ch]
        if (channel.data != 0) channel.arpeggio = channel.data
        channel.voiceperiod = channel.period
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_k(ch: Int) { // vibrato + volslide
        this.effect_t0_d(ch)
    }

    fun effect_t0_l(ch: Int) { // slide to note + volslide
        this.effect_t0_d(ch)
    }

    fun effect_t0_m(ch: Int) { // -
    }

    fun effect_t0_n(ch: Int) { // -
    }

    fun effect_t0_o(ch: Int) { // set sample offset
        val channel = this.channel[ch]
        if (channel.data != 0) channel.lastoffset = channel.data

        if (channel.lastoffset * 256 < this.sample[channel.sample].length) {
            channel.samplepos = (channel.lastoffset * 256).toDouble()
            channel.trigramp = 0.0
            channel.trigrampfrom = channel.currentsample
        }
    }

    fun effect_t0_p(ch: Int) { // -
    }

    fun effect_t0_q(ch: Int) { // retrig note
        val channel = this.channel[ch]
        if (channel.data != 0) channel.lastretrig = channel.data
        this.effect_t1_q(ch) // to retrig also on lines with no note but Qxy command
    }

    fun effect_t0_r(ch: Int) { // tremolo
    }

    fun effect_t0_s(ch: Int) { // Sxy effects
        val channel = this.channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        this.effects_t0_s[i](ch)
    }

    fun effect_t0_t(ch: Int) { // set tempo
        val channel = this.channel[ch]
        if (channel.data > 32) this.bpm = channel.data
    }

    fun effect_t0_u(ch: Int) { // fine vibrato
    }

    fun effect_t0_v(ch: Int) { // set global volume
        val channel = this.channel[ch]
        this.volume = channel.data
    }

    fun effect_t0_w(ch: Int) { // -
    }

    fun effect_t0_x(ch: Int) { // -
    }

    fun effect_t0_y(ch: Int) { // -
    }

    fun effect_t0_z(ch: Int) { // sync for FMOD (was: unused)
        val channel = this.channel[ch]
        this.syncqueue.addFirst(channel.data and 0x0f)
    }


    //
// tick 0 special Sxy effect functions
//
    fun effect_t0_s0(ch: Int) { // set filter (not implemented)
    }

    fun effect_t0_s1(ch: Int) { // set glissando control
    }

    fun effect_t0_s2(ch: Int) { // sync for BASS (was: set finetune)
        val channel = this.channel[ch]
        this.syncqueue.addFirst(channel.data and 0x0f)
    }

    fun effect_t0_s3(ch: Int) { // set vibrato waveform
        val channel = this.channel[ch]
        channel.vibratowave = channel.data and 0x07
    }

    fun effect_t0_s4(ch: Int) { // set tremolo waveform
    }

    fun effect_t0_s5(ch: Int) { // -
    }

    fun effect_t0_s6(ch: Int) { // -
    }

    fun effect_t0_s7(ch: Int) { // -
    }

    fun effect_t0_s8(ch: Int) { // set panning position
        val channel = this.channel[ch]
        this.pan_r[ch] = ((channel.data and 0x0f) / 15.0).toFloat()
        this.pan_l[ch] = (1.0 - this.pan_r[ch]).toFloat()
    }

    fun effect_t0_s9(ch: Int) { // -
    }

    fun effect_t0_sa(ch: Int) { // old stereo control (not implemented)
    }

    fun effect_t0_sb(ch: Int) { // loop pattern
        val channel = this.channel[ch]
        if ((channel.data and 0x0f) != 0) {
            if (this.loopcount != 0) {
                this.loopcount--
            } else {
                this.loopcount = channel.data and 0x0f
            }
            if (this.loopcount != 0) this.flags = this.flags or 64
        } else {
            this.looprow = this.row
        }
    }

    fun effect_t0_sc(ch: Int) { // note cut
    }

    fun effect_t0_sd(ch: Int) { // note delay
        val channel = this.channel[ch]
        if (this.tick == (channel.data and 0x0f)) {
            this.process_note(this.patterntable[this.position], ch)
        }
    }

    fun effect_t0_se(ch: Int) { // pattern delay
        val channel = this.channel[ch]
        this.patterndelay = channel.data and 0x0f
        this.patternwait = 0
    }

    fun effect_t0_sf(ch: Int) {  // funkrepeat (not implemented)
    }


    //
    // tick 1+ effect functions
    //
    fun effect_t1_a(ch: Int) { // set speed
    }

    fun effect_t1_b(ch: Int) { // order jump
    }

    fun effect_t1_c(ch: Int) { // jump to row
    }

    fun effect_t1_d(ch: Int) { // volume slide
        val channel = this.channel[ch]
        if ((channel.volslide and 0x0f) == 0) {
            // slide up
            channel.voicevolume += channel.volslide ushr 4
        } else if ((channel.volslide ushr 4) == 0) {
            // slide down
            channel.voicevolume -= channel.volslide and 0x0f
        }
        if (channel.voicevolume < 0) channel.voicevolume = 0
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_t1_e(ch: Int) { // slide down
        val channel = this.channel[ch]
        if (channel.slidespeed < 0xe0) {
            channel.voiceperiod += channel.slidespeed * 4
        }
        if (channel.voiceperiod > 27392) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_f(ch: Int) { // slide up
        val channel = this.channel[ch]
        if (channel.slidespeed < 0xe0) {
            channel.voiceperiod -= channel.slidespeed * 4
        }
        if (channel.voiceperiod < 56) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_g(ch: Int) { // slide to note
        val channel = this.channel[ch]
        if (channel.voiceperiod < channel.slideto) {
            //    channelvoiceperiod+=4*channel.slidetospeed;
            channel.voiceperiod += 4 * channel.slidespeed
            if (channel.voiceperiod > channel.slideto) channel.voiceperiod = channel.slideto
        } else if (channel.voiceperiod > channel.slideto) {
            //    channel.voiceperiod-=4*channel.slidetospeed;
            channel.voiceperiod -= 4 * channel.slidespeed
            if (channel.voiceperiod < channel.slideto) channel.voiceperiod = channel.slideto
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_h(ch: Int) { // vibrato
        val channel = this.channel[ch]
        channel.voiceperiod += this.vibratotable[channel.vibratowave and 3][channel.vibratopos] * channel.vibratodepth / 128.0
        if (channel.voiceperiod > 27392) channel.voiceperiod = 27392.0
        if (channel.voiceperiod < 56) channel.voiceperiod = 56.0
        channel.flags = channel.flags or 1
    }

    fun effect_t1_i(ch: Int) { // tremor
    }

    fun effect_t1_j(ch: Int) { // arpeggio
        val channel = this.channel[ch]
        var n = channel.note
        if ((this.tick and 3) == 1) n += channel.arpeggio ushr 4
        if ((this.tick and 3) == 2) n += channel.arpeggio and 0x0f
        channel.voiceperiod = (8363.0 * this.periodtable[n]) / this.sample[channel.sample].c2spd.toDouble()
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_k(ch: Int) { // vibrato + volslide
        this.effect_t1_h(ch)
        this.effect_t1_d(ch)
    }

    fun effect_t1_l(ch: Int) { // slide to note + volslide
        this.effect_t1_g(ch)
        this.effect_t1_d(ch)
    }

    fun effect_t1_m(ch: Int) { // -
    }

    fun effect_t1_n(ch: Int) { // -
    }

    fun effect_t1_o(ch: Int) { // set sample offset
    }

    fun effect_t1_p(ch: Int) { // -
    }

    fun effect_t1_q(ch: Int) { // retrig note
        val channel = this.channel[ch]
        if ((this.tick % (channel.lastretrig and 0x0f)) == 0) {
            channel.samplepos = 0.0
            channel.trigramp = 0.0
            channel.trigrampfrom = channel.currentsample
            val v = channel.lastretrig ushr 4
            if ((v and 7) >= 6) {
                channel.voicevolume =
                    kotlin.math.floor(channel.voicevolume * this.retrigvoltab[v]).toInt()
            } else {
                channel.voicevolume += this.retrigvoltab[v].toInt()
            }
            if (channel.voicevolume < 0) channel.voicevolume = 0
            if (channel.voicevolume > 64) channel.voicevolume = 64
        }
    }

    fun effect_t1_r(ch: Int) { // tremolo
    }

    fun effect_t1_s(ch: Int) { // special effects
        val channel = this.channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        this.effects_t1_s[i](ch)
    }

    fun effect_t1_t(ch: Int) { // set tempo
    }

    fun effect_t1_u(ch: Int) { // fine vibrato
    }

    fun effect_t1_v(ch: Int) { // set global volume
    }

    fun effect_t1_w(ch: Int) { // -
    }

    fun effect_t1_x(ch: Int) { // -
    }

    fun effect_t1_y(ch: Int) { // -
    }

    fun effect_t1_z(ch: Int) { // -
    }


    //
// tick 1+ special Sxy effect functions
//
    fun effect_t1_s0(ch: Int) { // set filter (not implemented)
    }

    fun effect_t1_s1(ch: Int) { // set glissando control
    }

    fun effect_t1_s2(ch: Int) { // set finetune
    }

    fun effect_t1_s3(ch: Int) { // set vibrato waveform
    }

    fun effect_t1_s4(ch: Int) { // set tremolo waveform
    }

    fun effect_t1_s5(ch: Int) { // -
    }

    fun effect_t1_s6(ch: Int) { // -
    }

    fun effect_t1_s7(ch: Int) { // -
    }

    fun effect_t1_s8(ch: Int) { // set panning position
    }

    fun effect_t1_s9(ch: Int) { // -
    }

    fun effect_t1_sa(ch: Int) { // old stereo control (not implemented)
    }

    fun effect_t1_sb(ch: Int) { // loop pattern
    }

    fun effect_t1_sc(ch: Int) { // note cut
        val channel = this.channel[ch]
        if (this.tick == (channel.data and 0x0f)) {
            channel.volume = 0
            channel.voicevolume = 0
        }
    }

    fun effect_t1_sd(ch: Int) { // note delay
        this.effect_t0_sd(ch)
    }

    fun effect_t1_se(ch: Int) { // pattern delay
    }

    fun effect_t1_sf(ch: Int) { // funkrepeat (not implemented)
    }

}
