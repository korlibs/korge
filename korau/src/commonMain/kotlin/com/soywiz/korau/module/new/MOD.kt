@file:Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate", "FunctionName")

package com.soywiz.korau.module.new

import com.soywiz.kds.IntDeque
import com.soywiz.klock.measureTime
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
import kotlin.math.min
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
        856f, 808f, 762f, 720f, 678f, 640f, 604f, 570f, 538f, 508f, 480f, 453f, 428f, 404f, 381f, 360f, 339f, 320f,
        302f, 285f, 269f, 254f, 240f, 226f, 214f, 202f, 190f, 180f, 170f, 160f, 151f, 143f, 135f, 127f, 120f, 113f
    )

    // finetune multipliers
    val finetunetable = FloatArray(16) { 2f.pow((it - 8).toFloat() / 12f / 8f) }

    // calc tables for vibrato waveforms
    val vibratotable = arrayOf(FloatArray(64) { 127 * kotlin.math.sin(kotlin.math.PI.toFloat() * 2 * (it / 64f)) },
        FloatArray(64) { 127f - 4f * it },
        FloatArray(64) { if (it < 32) 127f else -127f },
        FloatArray(64) { (1 - 2 * kotlin.random.Random.nextFloat()) * 127 })

    // effect jumptables
    val effects_t0 = arrayOf(
        ::effect_t0_0, ::effect_t0_1, ::effect_t0_2, ::effect_t0_3, ::effect_t0_4, ::effect_t0_5, ::effect_t0_6,
        ::effect_t0_7, ::effect_t0_8, ::effect_t0_9, ::effect_t0_a, ::effect_t0_b, ::effect_t0_c, ::effect_t0_d,
        ::effect_t0_e, ::effect_t0_f
    )
    val effects_t0_e = arrayOf(
        ::effect_t0_e0, ::effect_t0_e1, ::effect_t0_e2, ::effect_t0_e3, ::effect_t0_e4, ::effect_t0_e5,
        ::effect_t0_e6, ::effect_t0_e7, ::effect_t0_e8, ::effect_t0_e9, ::effect_t0_ea, ::effect_t0_eb,
        ::effect_t0_ec, ::effect_t0_ed, ::effect_t0_ee, ::effect_t0_ef
    )
    val effects_t1 = arrayOf(
        ::effect_t1_0, ::effect_t1_1, ::effect_t1_2, ::effect_t1_3, ::effect_t1_4, ::effect_t1_5, ::effect_t1_6,
        ::effect_t1_7, ::effect_t1_8, ::effect_t1_9, ::effect_t1_a, ::effect_t1_b, ::effect_t1_c, ::effect_t1_d,
        ::effect_t1_e, ::effect_t1_f
    )
    val effects_t1_e = arrayOf(
        ::effect_t1_e0, ::effect_t1_e1, ::effect_t1_e2, ::effect_t1_e3, ::effect_t1_e4, ::effect_t1_e5,
        ::effect_t1_e6, ::effect_t1_e7, ::effect_t1_e8, ::effect_t1_e9, ::effect_t1_ea, ::effect_t1_eb,
        ::effect_t1_ec, ::effect_t1_ed, ::effect_t1_ee, ::effect_t1_ef
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
        title = ""
        signature = ""

        songlen = 1
        repeatpos = 0
        patterntable = Uint8BufferAlloc(128)

        channels = 4

        sample = Array(31) { Sample() }
        samples = 31

        patterns = 0
        pattern = emptyArray()
        note = emptyArray()
        pattern_unpack = emptyArray()

        looprow = 0
        loopstart = 0
        loopcount = 0

        patterndelay = 0
        patternwait = 0
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
        syncqueue = IntDeque()

        tick = 0
        position = 0
        row = 0
        offset = 0
        flags = 0

        speed = 6
        bpm = 125
        breakrow = 0
        patternjump = 0
        patterndelay = 0
        patternwait = 0
        endofsong = false

        channel = Array(channels) { Channel() }
    }

    init {
        clearsong()
        initialize()
    }

    // parse the module from local buffer
    fun parse(buffer: Uint8Buffer): Boolean {
        signature = (0 until 4).map { buffer[1080 + it].toChar() }.joinToString("")
        when (signature) {
            "M.K.", "M!K!", "4CHN", "FLT4" -> Unit
            "6CHN" -> channels = 6
            "8CHN", "FLT8" -> channels = 8
            "28CH" -> channels = 28
            else -> return false
        }
        chvu = FloatArray(channels)

        run {
            var i = 0
            while (buffer[i] != 0 && i < 20) this.title = this.title + buffer[i++].toChar()
        }

        for (i in 0 until samples) {
            val st = 20 + i * 30
            var j = 0
            val sample = sample[i]
            while (buffer[st + j] != 0 && j < 22) {
                sample.name += if ((buffer[st + j] > 0x1f) && (buffer[st + j] < 0x7f)) (buffer[st + j].toChar()) else ' '
                j++
            }
            sample.length = 2 * (buffer[st + 22] * 256 + buffer[st + 23])
            sample.finetune = buffer[st + 24]
            if (sample.finetune > 7) sample.finetune = sample.finetune - 16
            sample.volume = buffer[st + 25]
            sample.loopstart = 2 * (buffer[st + 26] * 256 + buffer[st + 27])
            sample.looplength = 2 * (buffer[st + 28] * 256 + buffer[st + 29])
            if (sample.looplength == 2) sample.looplength = 0
            if (sample.loopstart > sample.length) {
                sample.loopstart = 0
                sample.looplength = 0
            }
        }

        songlen = buffer[950]
        if (buffer[951] != 127) repeatpos = buffer[951]
        for (i in 0 until 128) {
            patterntable[i] = buffer[952 + i]
            if (patterntable[i] > patterns) patterns = patterntable[i]
        }
        patterns += 1
        val patlen = 4 * 64 * channels

        pattern = Array(patterns) { Uint8BufferAlloc(patlen) }
        note = Array(patterns) { Uint8BufferAlloc(channels * 64) }
        pattern_unpack = Array(patterns) { Uint8BufferAlloc(channels * 64 * 5) }
        for (i in 0 until patterns) {
            for (j in 0 until patlen) pattern[i][j] = buffer[1084 + i * patlen + j]
            for (j in 0 until 64) {
                for (c in 0 until channels) {
                    note[i][j * channels + c] = 0
                    val n: Int =
                        ((pattern[i][j * 4 * channels + c * 4] and 0x0f) shl 8) or pattern[i][j * 4 * channels + c * 4 + 1]
                    for (np in baseperiodtable.indices) {
                        if (n.toFloat() == baseperiodtable[np]) {
                            note[i][j * channels + c] = np
                        }
                    }
                }
            }
            for (j in 0 until 64) {
                for (c in 0 until channels) {
                    val pp = j * 4 * channels + c * 4
                    val ppu = j * 5 * channels + c * 5
                    var n = ((pattern[i][pp] and 0x0f) shl 8) or pattern[i][pp + 1]
                    if (n != 0) {
                        n = note[i][j * channels + c]
                        n = (n % 12) or ((kotlin.math.floor(n.toDouble() / 12) + 2).toInt() shl 4);
                    }
                    val patternu = pattern_unpack[i]
                    patternu[ppu + 0] = if (n != 0) n else 255
                    patternu[ppu + 1] = (pattern[i][pp + 0] and 0xf0) or (pattern[i][pp + 2] ushr 4)
                    patternu[ppu + 2] = 255
                    patternu[ppu + 3] = (pattern[i][pp + 2] and 0x0f)
                    patternu[ppu + 4] = pattern[i][pp + 3]
                }
            }
        }

        var sst = 1084 + patterns * patlen
        for (i in 0 until samples) {
            sample[i].data = FloatArray(sample[i].length)
            for (j in 0 until sample[i].length) {
                var q: Float = buffer[sst + j].toFloat()
                when {
                    q < 128 -> q /= 128f
                    else -> q = ((q - 128) / 128f) - 1f
                }
                sample[i].data[j] = q
            }
            sst += sample[i].length
        }

        // look ahead at very first row to see if filter gets enabled
        filter = false
        for (ch in 0 until channels) {
            val p = patterntable[0]
            val pp = ch * 4
            val cmd = pattern[p][pp + 2] and 0x0f
            val data = pattern[p][pp + 3]
            if (cmd == 0x0e && ((data and 0xf0) == 0x00)) {
                filter = (data and 0x01) == 0
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

        chvu = FloatArray(channels)

        val computeTime = measureTime {
            songlenInTicks = computeTime()
        }
        println("Computed song length in...$computeTime")
        //println("timeInTicks=$timeInTicks")

        return true
    }

    var songlenInTicks: Int = 0

    val spd get() = (((samplerate * 60) / bpm.toDouble()) / 4) / 6

    fun computeTime(): Int {
        var ticks = 0
        val spd = this.spd
        while (!endofsong) {
            advance()
            //println("tick=$tick, offset=$offset, row=$row, position=$position, songlen=$songlen")
            ticks++
            offset += spd.toInt()
        }
        //playing = false
        initialize()
        return ticks
    }

    // advance player
    fun advance() {
        val spd = this.spd

        // advance player
        if (offset > spd) {
            tick++
            offset = 0
            flags = flags or 1
        }
        if (tick >= speed) {
            if (patterndelay != 0) { // delay pattern
                if (tick < ((patternwait + 1) * speed)) {
                    patternwait++
                } else {
                    row++
                    tick = 0
                    flags = flags or 2
                    patterndelay = 0
                }
            } else {
                if ((flags and (16 + 32 + 64)) != 0) {
                    when {
                        (flags and 64) != 0 -> { // loop pattern?
                            row = looprow
                            flags = flags and 0xa1
                            flags = flags or 2
                        }
                        (flags and 16) != 0 -> { // pattern jump/break?
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
        if (row >= 64) {
            position++
            row = 0
            flags = flags or 4
        }
        if (position >= songlen) {
            if (repeat) {
                position = 0
            } else {
                endofsong = true
                //mod.stop();
            }
            return
        }
    }

    // mix an audio buffer with data
    fun mix(bufs: Array<FloatArray>, buflen: Int = bufs[0].size) {
        var f: Double
        var p: Int
        var pp: Int
        var n: Int
        var nn: Int

        val outp = FloatArray(2)
        for (s in 0 until buflen) {
            outp[0] = 0f
            outp[1] = 0f

            if (!paused && !endofsong && playing) {
                advance()

                var och = 0
                for (ch in 0 until channels) {

                    // calculate playback position
                    p = patterntable[position]
                    pp = row * 4 * channels + ch * 4
                    val channel = channel[ch]
                    if ((flags and 2) != 0) { // row
                        channel.command = pattern[p][pp + 2] and 0x0f
                        channel.data = pattern[p][pp + 3]

                        if (!(channel.command == 0x0e && (channel.data and 0xf0) == 0xd0)) {
                            n = ((pattern[p][pp] and 0x0f) shl 8) or pattern[p][pp + 1]
                            if (n != 0) {
                                // noteon, except if command=3 (porta to note)
                                if ((channel.command != 0x03) && (channel.command != 0x05)) {
                                    channel.period = n
                                    channel.samplepos = 0.0
                                    if (channel.vibratowave > 3) {
                                        channel.vibratopos = 0
                                    }
                                    channel.flags = channel.flags or 3 // recalc speed
                                    channel.noteon = 1
                                }
                                // in either case, set the slide to note target
                                channel.slideto = n
                            }
                            nn = (pattern[p][pp + 0] and 0xf0) or (pattern[p][pp + 2] ushr 4)
                            if (nn != 0) {
                                channel.sample = nn - 1
                                channel.volume = sample[nn - 1].volume
                                if (n == 0 && (channel.samplepos > sample[nn - 1].length)) {
                                    channel.samplepos = 0.0
                                }
                            }
                        }
                    }
                    channel.voiceperiod = channel.period.toDouble()

                    // kill empty samples
                    if (sample[channel.sample].length == 0) channel.noteon = 0

                    // effects
                    if ((flags and 1) != 0) {
                        if (tick == 0) {
                            // process only on tick 0
                            effects_t0[channel.command](this, ch)
                        } else {
                            effects_t1[channel.command](this, ch)
                        }
                    }

                    // recalc note number from period
                    if ((channel.flags and 2) != 0) {
                        val baseperiodtable1 = baseperiodtable
                        for (np in baseperiodtable1.indices) {
                            if (baseperiodtable1[np] >= channel.period) {
                                channel.note = np
                            }
                        }
                        channel.semitone = 7.0
                        if (channel.period >= 120) {
                            channel.semitone =
                                ((baseperiodtable1[channel.note] - baseperiodtable1[channel.note + 1]).toDouble())
                        }
                    }

                    // recalc sample speed and apply finetune
                    if (((channel.flags and 1) != 0 || (flags and 2) != 0) && channel.voiceperiod != 0.0) {
                        channel.samplespeed = 7093789.2 / (channel.voiceperiod * 2) * finetunetable[sample[channel.sample].finetune + 8] / samplerate
                    }

                    // advance vibrato on each tick
                    if ((flags and 1) != 0) {
                        channel.vibratopos += channel.vibratospeed
                        channel.vibratopos = channel.vibratopos and 0x3f
                    }

                    // mix channel to output
                    och = och xor (ch and 1)
                    f = 0.0
                    if (channel.noteon != 0) {
                        if (sample[channel.sample].length > channel.samplepos) {
                            f = (sample[channel.sample].data[kotlin.math.floor(channel.samplepos).toInt()] * channel.volume) / 64.0
                        }
                        outp[och] += f.toFloat()
                        channel.samplepos += channel.samplespeed
                    }
                    chvu[ch] = kotlin.math.max(chvu[ch].toDouble(), kotlin.math.abs(f)).toFloat()

                    // loop or end samples
                    if (channel.noteon != 0) {
                        if (sample[channel.sample].loopstart != 0 || sample[channel.sample].looplength != 0) {
                            if (channel.samplepos >= (sample[channel.sample].loopstart + sample[channel.sample].looplength)) {
                                channel.samplepos -= sample[channel.sample].looplength
                            }
                        } else {
                            if (channel.samplepos >= sample[channel.sample].length) {
                                channel.noteon = 0
                            }
                        }
                    }

                    // clear channel flags
                    channel.flags = 0
                }
                offset++
                flags = flags and 0x70
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
        val channel = mod.channel[ch]
        channel.arpeggio = channel.data
    }

    fun effect_t0_1(mod: Protracker, ch: Int) { // 1 slide up
        val channel = mod.channel[ch]
        if (channel.data != 0) channel.slidespeed = channel.data
    }

    fun effect_t0_2(mod: Protracker, ch: Int) { // 2 slide down
        val channel = mod.channel[ch]
        if (channel.data != 0) channel.slidespeed = channel.data
    }

    fun effect_t0_3(mod: Protracker, ch: Int) { // 3 slide to note
        val channel = mod.channel[ch]
        if (channel.data != 0) channel.slidetospeed = channel.data
    }

    fun effect_t0_4(mod: Protracker, ch: Int) { // 4 vibrato
        val channel = mod.channel[ch]
        if ((channel.data and 0x0f) != 0 && (channel.data and 0xf0) != 0) {
            channel.vibratodepth = (channel.data and 0x0f)
            channel.vibratospeed = (channel.data and 0xf0) ushr 4
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
        val channel = mod.channel[ch]
        channel.samplepos = (channel.data * 256).toDouble()
    }

    fun effect_t0_a(mod: Protracker, ch: Int) { // a
    }

    fun effect_t0_b(mod: Protracker, ch: Int) { // b pattern jump
        mod.breakrow = 0
        mod.patternjump = mod.channel[ch].data
        mod.flags = mod.flags or 16
    }

    fun effect_t0_c(mod: Protracker, ch: Int) { // c set volume
        val channel = mod.channel[ch]
        channel.volume = channel.data
    }

    fun effect_t0_d(mod: Protracker, ch: Int) { // d pattern break
        val channel = mod.channel[ch]
        mod.breakrow = ((channel.data and 0xf0) ushr 4) * 10 + (channel.data and 0x0f)
        if ((mod.flags and 16) == 0) mod.patternjump = mod.position + 1
        mod.flags = mod.flags or 16
    }

    fun effect_t0_e(mod: Protracker, ch: Int) { // e
        val channel = mod.channel[ch]
        val i = (channel.data and 0xf0) ushr 4
        mod.effects_t0_e[i](mod, ch)
    }

    fun effect_t0_f(mod: Protracker, ch: Int) { // f set speed
        val channel = mod.channel[ch]
        if (channel.data > 32) {
            mod.bpm = channel.data
        } else {
            if (channel.data != 0) mod.speed = channel.data
        }
    }


    //
    // tick 0 effect e functions
    //
    fun effect_t0_e0(mod: Protracker, ch: Int) { // e0 filter on/off
        if (mod.channels > 4) return // use only for 4ch amiga tunes
        val channel = mod.channel[ch]
        mod.filter = (channel.data and 0x01) == 0
    }

    fun effect_t0_e1(mod: Protracker, ch: Int) { // e1 fine slide up
        val channel = mod.channel[ch]
        channel.period -= channel.data and 0x0f
        if (channel.period < 113) channel.period = 113
    }

    fun effect_t0_e2(mod: Protracker, ch: Int) { // e2 fine slide down
        val channel = mod.channel[ch]
        channel.period += channel.data and 0x0f
        if (channel.period > 856) channel.period = 856
        channel.flags = channel.flags or 1
    }

    fun effect_t0_e3(mod: Protracker, ch: Int) { // e3 set glissando
    }

    fun effect_t0_e4(mod: Protracker, ch: Int) { // e4 set vibrato waveform
        val channel = mod.channel[ch]
        channel.vibratowave = channel.data and 0x07
    }

    fun effect_t0_e5(mod: Protracker, ch: Int) { // e5 set finetune
    }

    fun effect_t0_e6(mod: Protracker, ch: Int) { // e6 loop pattern
        val channel = mod.channel[ch]
        if ((channel.data and 0x0f) != 0) {
            when {
                mod.loopcount != 0 -> mod.loopcount--
                else -> mod.loopcount = channel.data and 0x0f
            }
            if (mod.loopcount != 0) mod.flags = mod.flags or 64
        } else {
            mod.looprow = mod.row
        }
    }

    fun effect_t0_e7(mod: Protracker, ch: Int) { // e7
    }

    fun effect_t0_e8(mod: Protracker, ch: Int) { // e8, use for syncing
        val channel = mod.channel[ch]
        mod.syncqueue.addFirst(channel.data and 0x0f)
    }

    fun effect_t0_e9(mod: Protracker, ch: Int) { // e9
    }

    fun effect_t0_ea(mod: Protracker, ch: Int) { // ea fine volslide up
        val channel = mod.channel[ch]
        channel.volume += channel.data and 0x0f
        if (channel.volume > 64) channel.volume = 64
    }

    fun effect_t0_eb(mod: Protracker, ch: Int) { // eb fine volslide down
        val channel = mod.channel[ch]
        channel.volume -= channel.data and 0x0f
        if (channel.volume < 0) channel.volume = 0
    }

    fun effect_t0_ec(mod: Protracker, ch: Int) { // ec
    }

    fun effect_t0_ed(mod: Protracker, ch: Int) { // ed delay sample
        val channel = mod.channel[ch]
        if (mod.tick == (channel.data and 0x0f)) {
            // start note
            val p = mod.patterntable[mod.position]
            val pp = mod.row * 4 * mod.channels + ch * 4
            val pattern = mod.pattern[p]
            run {
                val n = ((pattern[pp] and 0x0f) shl 8) or pattern[pp + 1]
                if (n != 0) {
                    channel.period = n
                    channel.voiceperiod = channel.period.toDouble()
                    channel.samplepos = 0.0
                    if (channel.vibratowave > 3) channel.vibratopos = 0
                    channel.flags = channel.flags or 3 // recalc speed
                    channel.noteon = 1
                }
            }
            run {
                val n = (pattern[pp + 0] and 0xf0) or (pattern[pp + 2] ushr 4)
                if (n != 0) {
                    channel.sample = n - 1
                    channel.volume = mod.sample[n - 1].volume
                }
            }
        }
    }

    fun effect_t0_ee(mod: Protracker, ch: Int) { // ee delay pattern
        val channel = mod.channel[ch]
        mod.patterndelay = channel.data and 0x0f
        mod.patternwait = 0
    }

    fun effect_t0_ef(mod: Protracker, ch: Int) { // ef
    }


    //
    // tick 1+ effect functions
    //
    fun effect_t1_0(mod: Protracker, ch: Int) { // 0 arpeggio
        val channel = mod.channel[ch]
        if (channel.data != 0) {
            var apn = channel.note
            if ((mod.tick % 3) == 1) apn += channel.arpeggio ushr 4
            if ((mod.tick % 3) == 2) apn += channel.arpeggio and 0x0f
            if (apn >= 0 && apn <= mod.baseperiodtable.size) channel.voiceperiod =
                mod.baseperiodtable[apn].toDouble()
            channel.flags = channel.flags or 1
        }
    }

    fun effect_t1_1(mod: Protracker, ch: Int) { // 1 slide up
        val channel = mod.channel[ch]
        channel.period -= channel.slidespeed
        if (channel.period < 113) channel.period = 113
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_2(mod: Protracker, ch: Int) { // 2 slide down
        val channel = mod.channel[ch]
        channel.period += channel.slidespeed
        if (channel.period > 856) channel.period = 856
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_3(mod: Protracker, ch: Int) { // 3 slide to note
        val channel = mod.channel[ch]
        if (channel.period < channel.slideto) {
            channel.period += channel.slidetospeed
            if (channel.period > channel.slideto) channel.period = channel.slideto
        }
        if (channel.period > channel.slideto) {
            channel.period -= channel.slidetospeed
            if (channel.period < channel.slideto) channel.period = channel.slideto
        }
        channel.flags = channel.flags or 3 // recalc speed
    }

    fun effect_t1_4(mod: Protracker, ch: Int) { // 4 vibrato
        val channel = mod.channel[ch]
        val waveform = mod.vibratotable[channel.vibratowave and 3][channel.vibratopos] / 63.0 //127.0;

        // two different implementations for vibrato
        //  var a=(mod.channel[ch].vibratodepth/32)*mod.channel[ch].semitone*waveform; // non-linear vibrato +/- semitone
        val a = channel.vibratodepth * waveform // linear vibrato, depth has more effect high notes

        channel.voiceperiod += a
        channel.flags = channel.flags or 1
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
        val channel = mod.channel[ch]
        if ((channel.data and 0x0f) == 0) {
            // y is zero, slide up
            channel.volume += (channel.data ushr 4)
            if (channel.volume > 64) channel.volume = 64
        }
        if ((channel.data and 0xf0) == 0) {
            // x is zero, slide down
            channel.volume -= (channel.data and 0x0f)
            if (channel.volume < 0) channel.volume = 0
        }
    }

    fun effect_t1_b(mod: Protracker, ch: Int) { // b pattern jump
    }

    fun effect_t1_c(mod: Protracker, ch: Int) { // c set volume
    }

    fun effect_t1_d(mod: Protracker, ch: Int) { // d pattern break
    }

    fun effect_t1_e(mod: Protracker, ch: Int) { // e
        val channel = mod.channel[ch]
        val i = (channel.data and 0xf0) ushr 4
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
        val channel = mod.channel[ch]
        if (mod.tick % (channel.data and 0x0f) == 0) channel.samplepos = 0.0
    }

    fun effect_t1_ea(mod: Protracker, ch: Int) { // ea
    }

    fun effect_t1_eb(mod: Protracker, ch: Int) { // eb
    }

    fun effect_t1_ec(mod: Protracker, ch: Int) { // ec cut sample
        val channel = mod.channel[ch]
        if (mod.tick == (channel.data and 0x0f)) channel.volume = 0
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
        var fch = Array(2) { FloatArray(1024) }
        return object : AudioStream(44100, 2) {
            override val finished: Boolean get() = endofsong

            // @TODO: we should figure out how to compute the length in samples/time
            override val totalLengthInSamples: Long?
                get() = songlenInTicks.toLong() * spd.toLong() / 2

            var _currentPositionInSamples: Long = 0L

            private fun skipUntil(newPosition: Long) {
                while (_currentPositionInSamples < newPosition) {
                    val available = newPosition - _currentPositionInSamples
                    val skip = min(available.toInt(), fch[0].size)
                    mix(fch, skip)
                    _currentPositionInSamples += skip
                }
            }

            override var currentPositionInSamples: Long
                get() = _currentPositionInSamples
                set(value) {
                    if (_currentPositionInSamples == value) return
                    if (value > _currentPositionInSamples) {
                        skipUntil(value)
                    } else {
                        //if (value != 0L) error("only supported rewind in MOD value=$value")
                        _currentPositionInSamples = 0L
                        initialize()
                        if (value != 0L) {
                            println("SLOW SEEK")
                            skipUntil(value)
                        }
                    }
                }

            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                if (fch[0].size < length) fch = Array(2) { FloatArray(length) }
                mix(fch, length)
                _currentPositionInSamples += length
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
