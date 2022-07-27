@file:Suppress("unused", "NAME_SHADOWING", "UNUSED_PARAMETER", "FunctionName", "MemberVisibilityCanBePrivate")

package com.soywiz.korau.mod

import com.soywiz.kds.IntDeque
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8BufferAlloc
import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.clamp
import com.soywiz.kmem.mem
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.util.niceStr
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

    var volume: Int = globalVol
    var speed: Int = initSpeed
    var bpm: Int = initBPM
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
        FloatArray(256) { 127f * kotlin.math.sin(kotlin.math.PI * 2f * (it / 256f)).toFloat() },
        FloatArray(256) { 127f - it },
        FloatArray(256) { if (it < 128) 127f else -128f },
        FloatArray(256) { Random.nextFloat() * 255f - 128f },
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
        clearsong()
        initialize()
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
        title = ""
        signature = ""

        songlen = 1
        repeatpos = 0
        patterntable = Uint8BufferAlloc(256)

        channels = 0
        ordNum = 0
        insNum = 0
        patNum = 0

        globalVol = 64
        initSpeed = 6
        initBPM = 125

        fastslide = 0

        mixval = 8.0

        sample = Array(256) { Sample() }

        pattern = emptyArray()

        looprow = 0
        loopstart = 0
        loopcount = 0

        patterndelay = 0
        patternwait = 0
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
        syncqueue = IntDeque()

        tick = -1
        position = 0
        row = 0
        flags = 0

        volume = globalVol
        speed = initSpeed
        bpm = initBPM
        stt = 0.0
        breakrow = 0
        patternjump = 0
        patterndelay = 0
        patternwait = 0
        endofsong = false

        channel = Array(channels) { Channel() }
    }

    // parse the module from local buffer
    override fun parse(buffer: Uint8Buffer): Boolean {
        // check s3m signature and type
        signature = CharArray(4) { buffer[0x002c + it].toChar() }.concatToString()
        if (signature != "SCRM") return false
        if (buffer[0x001d] != 0x10) return false

        // get channel count
        channels = 0
        for (i in 0 until 32) {
            if ((buffer[0x0040 + i] and 0x80) != 0) break
            channels++
        }

        // default panning 3/C/3/...
        for (i in 0 until 32) {
            if ((buffer[0x0040 + i] and 0x80) == 0) {
                val c = buffer[0x0040 + i] and 15
                pan_r[i] = if (c < 8) 0.2f else 0.8f
                pan_l[i] = if (c < 8) 0.8f else 0.2f
            }
        }

        title = CharArray(0x1c) { dos2utf(buffer[it]) }.concatToString().trimEnd('\u0000')

        ordNum = buffer[0x0020] or (buffer[0x0021] shl 8)
        insNum = buffer[0x0022] or (buffer[0x0023] shl 8)
        patNum = buffer[0x0024] or (buffer[0x0025] shl 8)

        globalVol = buffer[0x0030]
        initSpeed = buffer[0x0031]
        initBPM = buffer[0x0032]

        fastslide = if ((buffer[0x0026] and 64) != 0) 1 else 0

        speed = initSpeed
        bpm = initBPM

        // check for additional panning info
        if (buffer[0x0035] == 0xfc) {
            for (i in 0 until 32) {
                var c = buffer[0x0070 + ordNum + insNum * 2 + patNum * 2 + i]
                if ((c and 0x10) != 0) {
                    c = c and 0x0f
                    pan_r[i] = c / 15f
                    pan_l[i] = 1f - pan_r[i]
                }
            }
        }

        // check for mono panning
        mixval = buffer[0x0033].toDouble()
        if ((mixval.toInt() and 0x80) == 0x80) {
            for (i in 0 until 32) {
                pan_r[i] = 0.5f
                pan_l[i] = 0.5f
            }
        }

        // calculate master mix scaling factor
        mixval = 128.0 / kotlin.math.max(0x10, mixval.toInt() and 0x7f).toDouble() // (8.0 when mastervol is 0x10, 1.0 when mastervol is 0x7f)

        // load orders
        for (i in 0 until ordNum) patterntable[i] = buffer[0x0060 + i]
        songlen = 0
        for (i in 0 until ordNum) if (patterntable[i] != 255) songlen++

        // load instruments
        sample = Array(insNum) { i ->
            val offset = (buffer[0x0060 + ordNum + i * 2] or (buffer[0x0060 + ordNum + i * 2 + 1] shl 8)) * 16
            // sample data
            val smpoffset = (((buffer[offset + 0x0d] shl 16) or (buffer[offset + 0x0e]) or (buffer[offset + 0x0f] shl 8)) * 16)
            val loopstart = buffer[offset + 0x14] or (buffer[offset + 0x15] shl 8)
            val loopend = buffer[offset + 0x18] or (buffer[offset + 0x19] shl 8)
            val length = buffer[offset + 0x10] or (buffer[offset + 0x11] shl 8)
            //println("SAMPLE:offset=${offset},smpoffset=$smpoffset,loopstart=$loopstart,loopend=$loopend,length=$length")
            Sample(
                name = CharArray(28) { dos2utf(buffer[offset + 0x0030 + it]) }.concatToString().trimEnd('\u0000'),
                length = length,
                loopstart = loopstart,
                loopend = loopend,
                looplength = loopend - loopstart,
                volume = buffer[offset + 0x1c],
                loop = buffer[offset + 0x1f] and 1,
                stereo = (buffer[offset + 0x1f] and 2) ushr 1,
                bits = if ((buffer[offset + 0x1f] and 4) != 0) 16 else 8,
                c2spd = buffer[offset + 0x20] or (buffer[offset + 0x21] shl 8),
                data = FloatArray(length) { (buffer[smpoffset + it] - 128).toFloat() / 128f } // convert to mono float signed,
            )
        }

        // load and unpack patterns
        var max_ch = 0
        pattern = Array(patNum) { Uint8BufferAlloc(channels * 64 * 5) }
        for (i in 0 until patNum) {
            val boffset = 0x0060 + ordNum + insNum * 2 + i * 2
            var offset = (buffer[boffset] or (buffer[boffset + 1] shl 8)) * 16
            var patlen = buffer[offset] or (buffer[offset + 1] shl 8)

            val pattern = pattern[i]
            for (row in 0 until 64) {
                for (ch in 0 until channels) {
                    val opattern = row * channels * 5 + ch * 5
                    pattern[opattern + 0] = 255
                    pattern[opattern + 1] = 0
                    pattern[opattern + 2] = 255
                    pattern[opattern + 3] = 255
                    pattern[opattern + 4] = 0
                }
            }

            if (offset == 0) continue // fix for control_e.s3m
            var row = 0
            var pos = 0
            offset += 2
            while (row < 64) {
                val c = buffer[offset + pos++]
                if (c == 0) {
                    row++
                    continue
                }
                val ch = c and 31
                if (ch < channels) {
                    if (ch > max_ch) {
                        for (j in 0 until songlen) {
                            if (patterntable[j] == i) max_ch = ch
                        } // only if pattern is actually used
                    }
                    val opattern = row * channels * 5 + ch * 5
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
            }
        }
        patterns = patNum

        // how many channels had actually pattern data on them? trim off the extra channels
        val oldch = channels
        channels = max_ch + 1
        for (i in 0 until patNum) {
            val oldpat = Uint8BufferAlloc(pattern[i].size)
            arraycopy(pattern[i].b.mem, 0, oldpat.b.mem, 0, pattern[i].size)
            pattern[i] = Uint8BufferAlloc(channels * 64 * 5)
            val pattern = pattern[i]
            for (j in 0 until 64) {
                for (c in 0 until channels) {
                    val op = j * channels * 5 + c * 5
                    val oop = j * oldch * 5 + c * 5
                    pattern[op + 0] = oldpat[oop + 0]
                    pattern[op + 1] = oldpat[oop + 1]
                    pattern[op + 2] = oldpat[oop + 2]
                    pattern[op + 3] = oldpat[oop + 3]
                    pattern[op + 4] = oldpat[oop + 4]
                }
            }
        }

        chvu = FloatArray(channels) { 0f }

        return true
    }


    // advance player
    fun advance(mod: Screamtracker) {
        stt = (((samplerate * 60).toDouble() / bpm.toDouble()) / 4.0) / 6.0 // samples to tick

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
                    row++
                    tick = 0
                    flags = flags or 2
                }
            }
        }

        // step to new pattern?
        if (row >= 64) {
            position++
            row = 0
            flags = flags or 4
            while (patterntable[position] == 254) position++ // skip markers
        }

        // end of song?
        if (position >= songlen || patterntable[position] == 255) {
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
        val pp: Int = row * 5 * channels + ch * 5

        val n = pattern[p][pp]
        val s = pattern[p][pp + 1]
        val channel = channel[ch]
        if (s != 0) {
            channel.sample = s - 1
            channel.volume = sample[s - 1].volume
            channel.voicevolume = channel.volume
            if (n == 255 && (channel.samplepos > sample[s - 1].length)) {
                channel.trigramp = 0.0
                channel.trigrampfrom = channel.currentsample
                channel.samplepos = 0.0
            }
        }

        if (n < 254) {
            // calc period for note
            val n = (n and 0x0f) + (n ushr 4) * 12
            val pv = (8363.0 * periodtable[n]) / sample[channel.sample].c2spd.toDouble()

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

        if (pattern[p][pp + 2] <= 64) {
            channel.volume = pattern[p][pp + 2]
            channel.voicevolume = channel.volume
        }
    }


    // advance player and all channels by a tick
    fun process_tick(mod: Screamtracker) {

        // advance global player state by a tick
        advance(mod)

        // advance all channels
        for (ch in 0 until channels) {

            // calculate playback position
            val p = patterntable[position]
            val pp = row * 5 * channels + ch * 5

            val channel = channel[ch]
            channel.oldvoicevolume = channel.voicevolume

            if ((flags and 2) != 0) { // new row
                channel.command = pattern[p][pp + 3]
                channel.data = pattern[p][pp + 4]
                if (!(channel.command == 0x13 && (channel.data and 0xf0) == 0xd0)) { // note delay?
                    process_note(p, ch)
                }
            }

            // kill empty samples
            if (sample[channel.sample].length == 0) channel.noteon = 0

            // run effects on each new tick
            if (channel.command < 27) {
                if (tick == 0) {
                    // process only on tick 0 effects
                    effects_t0[channel.command]?.invoke(ch)
                } else {
                    effects_t1[channel.command]?.invoke(ch)
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
            if (((channel.flags and 1) != 0 || (flags and 2) != 0) && channel.voiceperiod != 0.0) {
                channel.samplespeed = (14317056.0 / channel.voiceperiod) / samplerate.toDouble()
            }

            // clear channel flags
            channel.flags = 0
        }

        // clear global flags after all channels are processed
        flags = flags and 0x70
    }

    // mix an audio buffer with data
    override fun mix(bufs: Array<FloatArray>, buflen: Int) {
        val outp = FloatArray(2)

        // return a buffer of silence if not playing
        if (paused || endofsong || !playing) {
            for (s in 0 until buflen) {
                bufs[0][s] = 0f
                bufs[1][s] = 0f
                chvu.fill(0f)
            }
            return
        }

        // fill audiobuffer
        for (s in 0 until buflen) {
            outp[0] = 0f
            outp[1] = 0f

            // if STT has run out, step player forward by tick
            if (stt <= 0) process_tick(mod)

            // mix channels
            //var count = 0
            //var line = ""
            //sampleCount++
            //val doLog = sampleCount >= 5294;
            for (ch in 0 until channels) {
                var fl = 0.0
                var fr = 0.0
                var fs = 0.0
                val channel = channel[ch]
                val si = channel.sample

                // add channel output to left/right master outputs
                channel.currentsample = 0.0 // assume note is off
                if (channel.noteon != 0 || (channel.noteon == 0 && channel.volramp < 1.0)) {
                    if (sample[si].length > channel.samplepos) {
                        fl = channel.lastsample

                        // interpolate towards current sample
                        var f = channel.samplepos - kotlin.math.floor(channel.samplepos)
                        fs = sample[si].data[kotlin.math.floor(channel.samplepos).toInt()].toDouble()
                        fl = f * fs + (1.0 - f) * fl
                        //if (doLog) {
                        //    count++
                        //    line += "${fl.niceStr},"
                        //}
                        //println(fl)

                        // smooth out discontinuities from retrig and sample offset
                        f = channel.trigramp
                        fl = f * fl + (1.0 - f) * channel.trigrampfrom
                        f += 1.0 / 128.0
                        channel.trigramp = kotlin.math.min(1.0, f)
                        channel.currentsample = fl

                        // ramp volume changes over 64 samples to avoid clicks
                        fr = fl * (channel.voicevolume.toDouble() / 64.0)
                        f = channel.volramp
                        fl = f * fr + (1.0 - f) * (fl * (channel.volrampfrom / 64.0))
                        f += (1.0 / 64.0)
                        channel.volramp = kotlin.math.min(1.0, f)

                        // pan samples
                        fr = fl * pan_r[ch]
                        fl *= pan_l[ch]
                    }
                    outp[0] = (outp[0] + fl).toFloat()
                    outp[1] = (outp[1] + fr).toFloat()

                    val oldpos = channel.samplepos
                    channel.samplepos += channel.samplespeed
                    if (kotlin.math.floor(channel.samplepos) > kotlin.math.floor(oldpos)) {
                        channel.lastsample = fs
                    }

                    // loop or stop sample?
                    val sample = sample[channel.sample]
                    when {
                        sample.loop != 0 -> {
                            if (channel.samplepos >= sample.loopend) {
                                channel.samplepos -= sample.looplength
                                channel.lastsample = channel.currentsample
                            }
                        }
                        channel.samplepos >= sample.length -> channel.noteon = 0
                    }
                }
                chvu[ch] = kotlin.math.max(chvu[ch].toDouble(), kotlin.math.abs(fl + fr)).toFloat()
                //print("${chvu[ch].niceStr},")
            }
            //if (doLog) println("$sampleCount:$count:${line}channels=$channels")

            // done - store to output buffer
            val t = volume / 64.0
            bufs[0][s] = (outp[0] * t).toFloat()
            bufs[1][s] = (outp[1] * t).toFloat()
            stt--
        }
    }
    var sampleCount = 0

    //
    // tick 0 effect functions
    //
    fun effect_t0_a(ch: Int) { // set speed
        val channel = channel[ch]
        if (channel.data > 0) speed = channel.data
    }

    fun effect_t0_b(ch: Int) { // pattern jump
        val channel = channel[ch]
        breakrow = 0
        patternjump = channel.data
        flags = flags or 16
    }

    fun effect_t0_c(ch: Int) { // pattern break
        val channel = channel[ch]
        breakrow = ((channel.data and 0xf0) ushr 4) * 10 + (channel.data and 0x0f)
        if ((flags and 16) == 0) patternjump = position + 1
        flags = flags or 16
    }

    fun effect_t0_d(ch: Int) { // volume slide
        val channel = channel[ch]
        if (channel.data != 0) channel.volslide = channel.data
        // DxF fine up
        // DFx fine down
        when {
            (channel.volslide and 0x0f) == 0x0f -> channel.voicevolume += channel.volslide ushr 4
            (channel.volslide ushr 4) == 0x0f -> channel.voicevolume -= channel.volslide and 0x0f
            else -> if (fastslide != 0) effect_t1_d(ch)
        }

        if (channel.voicevolume < 0) channel.voicevolume = 0
        if (channel.voicevolume > 64) channel.voicevolume = 64
    }

    fun effect_t0_e(ch: Int) { // slide down
        val channel = channel[ch]
        if (channel.data != 0) channel.slidespeed = channel.data
        if ((channel.slidespeed and 0xf0) == 0xf0) channel.voiceperiod += (channel.slidespeed and 0x0f) shl 2
        if ((channel.slidespeed and 0xf0) == 0xe0) channel.voiceperiod += (channel.slidespeed and 0x0f)
        if (channel.voiceperiod > 27392) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_f(ch: Int) { // slide up
        val channel = channel[ch]
        if (channel.data != 0) channel.slidespeed = channel.data
        if ((channel.slidespeed and 0xf0) == 0xf0) channel.voiceperiod -= (channel.slidespeed and 0x0f) shl 2
        if ((channel.slidespeed and 0xf0) == 0xe0) channel.voiceperiod -= (channel.slidespeed and 0x0f)
        if (channel.voiceperiod < 56) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_g(ch: Int) { // slide to note
        val channel = channel[ch]
        //  if (channel.data) channel.slidetospeed=channel.data;
        if (channel.data != 0) channel.slidespeed = channel.data
    }

    fun effect_t0_h(ch: Int) { // vibrato
        val channel = channel[ch]
        if ((channel.data and 0x0f) != 0 && (channel.data and 0xf0) != 0) {
            channel.vibratodepth = (channel.data and 0x0f)
            channel.vibratospeed = (channel.data and 0xf0) ushr 4
        }
    }

    fun effect_t0_i(ch: Int) { // tremor
    }

    fun effect_t0_j(ch: Int) { // arpeggio
        val channel = channel[ch]
        if (channel.data != 0) channel.arpeggio = channel.data
        channel.voiceperiod = channel.period
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t0_k(ch: Int) { // vibrato + volslide
        effect_t0_d(ch)
    }

    fun effect_t0_l(ch: Int) { // slide to note + volslide
        effect_t0_d(ch)
    }

    fun effect_t0_m(ch: Int) { // -
    }

    fun effect_t0_n(ch: Int) { // -
    }

    fun effect_t0_o(ch: Int) { // set sample offset
        val channel = channel[ch]
        if (channel.data != 0) channel.lastoffset = channel.data

        if (channel.lastoffset * 256 < sample[channel.sample].length) {
            channel.samplepos = (channel.lastoffset * 256).toDouble()
            channel.trigramp = 0.0
            channel.trigrampfrom = channel.currentsample
        }
    }

    fun effect_t0_p(ch: Int) { // -
    }

    fun effect_t0_q(ch: Int) { // retrig note
        val channel = channel[ch]
        if (channel.data != 0) channel.lastretrig = channel.data
        effect_t1_q(ch) // to retrig also on lines with no note but Qxy command
    }

    fun effect_t0_r(ch: Int) { // tremolo
    }

    fun effect_t0_s(ch: Int) { // Sxy effects
        val channel = channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        effects_t0_s[i](ch)
    }

    fun effect_t0_t(ch: Int) { // set tempo
        val channel = channel[ch]
        if (channel.data > 32) bpm = channel.data
    }

    fun effect_t0_u(ch: Int) { // fine vibrato
    }

    fun effect_t0_v(ch: Int) { // set global volume
        val channel = channel[ch]
        volume = channel.data
    }

    fun effect_t0_w(ch: Int) { // -
    }

    fun effect_t0_x(ch: Int) { // -
    }

    fun effect_t0_y(ch: Int) { // -
    }

    fun effect_t0_z(ch: Int) { // sync for FMOD (was: unused)
        val channel = channel[ch]
        syncqueue.addFirst(channel.data and 0x0f)
    }


    //
    // tick 0 special Sxy effect functions
    //
    fun effect_t0_s0(ch: Int) { // set filter (not implemented)
    }

    fun effect_t0_s1(ch: Int) { // set glissando control
    }

    fun effect_t0_s2(ch: Int) { // sync for BASS (was: set finetune)
        val channel = channel[ch]
        syncqueue.addFirst(channel.data and 0x0f)
    }

    fun effect_t0_s3(ch: Int) { // set vibrato waveform
        val channel = channel[ch]
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
        val channel = channel[ch]
        pan_r[ch] = ((channel.data and 0x0f) / 15.0).toFloat()
        pan_l[ch] = (1.0 - pan_r[ch]).toFloat()
    }

    fun effect_t0_s9(ch: Int) { // -
    }

    fun effect_t0_sa(ch: Int) { // old stereo control (not implemented)
    }

    fun effect_t0_sb(ch: Int) { // loop pattern
        val channel = channel[ch]
        when {
            (channel.data and 0x0f) != 0 -> {
                when {
                    loopcount != 0 -> loopcount--
                    else -> loopcount = channel.data and 0x0f
                }
                if (loopcount != 0) {
                    flags = flags or 64
                }
            }
            else -> {
                looprow = row
            }
        }
    }

    fun effect_t0_sc(ch: Int) { // note cut
    }

    fun effect_t0_sd(ch: Int) { // note delay
        val channel = channel[ch]
        if (tick == (channel.data and 0x0f)) {
            process_note(patterntable[position], ch)
        }
    }

    fun effect_t0_se(ch: Int) { // pattern delay
        val channel = channel[ch]
        patterndelay = channel.data and 0x0f
        patternwait = 0
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
        val channel = channel[ch]
        if ((channel.volslide and 0x0f) == 0) {
            // slide up
            channel.voicevolume += channel.volslide ushr 4
        } else if ((channel.volslide ushr 4) == 0) {
            // slide down
            channel.voicevolume -= channel.volslide and 0x0f
        }
        channel.voicevolume = channel.voicevolume.clamp(0, 64)
    }

    fun effect_t1_e(ch: Int) { // slide down
        val channel = channel[ch]
        if (channel.slidespeed < 0xe0) channel.voiceperiod += channel.slidespeed * 4
        if (channel.voiceperiod > 27392) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_f(ch: Int) { // slide up
        val channel = channel[ch]
        if (channel.slidespeed < 0xe0) channel.voiceperiod -= channel.slidespeed * 4
        if (channel.voiceperiod < 56) channel.noteon = 0
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_g(ch: Int) { // slide to note
        val channel = channel[ch]
        if (channel.voiceperiod < channel.slideto) {
            //channelvoiceperiod+=4*channel.slidetospeed;
            channel.voiceperiod += 4 * channel.slidespeed
            if (channel.voiceperiod > channel.slideto) channel.voiceperiod = channel.slideto
        } else if (channel.voiceperiod > channel.slideto) {
            //channel.voiceperiod-=4*channel.slidetospeed;
            channel.voiceperiod -= 4 * channel.slidespeed
            if (channel.voiceperiod < channel.slideto) channel.voiceperiod = channel.slideto
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_h(ch: Int) { // vibrato
        val channel = channel[ch]
        channel.voiceperiod = (channel.voiceperiod + vibratotable[channel.vibratowave and 3][channel.vibratopos] * channel.vibratodepth / 128.0).clamp(56.0, 27392.0)
        channel.flags = channel.flags or 1
    }

    fun effect_t1_i(ch: Int) { // tremor
    }

    fun effect_t1_j(ch: Int) { // arpeggio
        val channel = channel[ch]
        var n = channel.note
        if ((tick and 3) == 1) n += channel.arpeggio ushr 4
        if ((tick and 3) == 2) n += channel.arpeggio and 0x0f
        channel.voiceperiod = (8363.0 * periodtable[n]) / sample[channel.sample].c2spd.toDouble()
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_k(ch: Int) { // vibrato + volslide
        effect_t1_h(ch)
        effect_t1_d(ch)
    }

    fun effect_t1_l(ch: Int) { // slide to note + volslide
        effect_t1_g(ch)
        effect_t1_d(ch)
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
        val channel = channel[ch]
        if ((tick % (channel.lastretrig and 0x0f)) == 0) {
            channel.samplepos = 0.0
            channel.trigramp = 0.0
            channel.trigrampfrom = channel.currentsample
            val v = channel.lastretrig ushr 4
            if ((v and 7) >= 6) {
                channel.voicevolume = kotlin.math.floor(channel.voicevolume * retrigvoltab[v]).toInt()
            } else {
                channel.voicevolume += retrigvoltab[v].toInt()
            }
            channel.voicevolume = channel.voicevolume.clamp(0, 64)
        }
    }

    fun effect_t1_r(ch: Int) { // tremolo
    }

    fun effect_t1_s(ch: Int) { // special effects
        val channel = channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        effects_t1_s[i](ch)
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
        val channel = channel[ch]
        if (tick == (channel.data and 0x0f)) {
            channel.volume = 0
            channel.voicevolume = 0
        }
    }

    fun effect_t1_sd(ch: Int) { // note delay
        effect_t0_sd(ch)
    }

    fun effect_t1_se(ch: Int) { // pattern delay
    }

    fun effect_t1_sf(ch: Int) { // funkrepeat (not implemented)
    }

}
