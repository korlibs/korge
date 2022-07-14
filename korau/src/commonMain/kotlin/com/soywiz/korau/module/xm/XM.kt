@file:Suppress("LocalVariableName", "NAME_SHADOWING", "UNUSED_VARIABLE", "PropertyName", "MemberVisibilityCanBePrivate",
    "FunctionName", "UNUSED_PARAMETER", "ControlFlowWithEmptyBody", "ObjectPropertyName"
)

package com.soywiz.korau.module.xm

import com.soywiz.kds.IntArrayList
import com.soywiz.klogger.Console
import com.soywiz.kmem.DataBuffer
import com.soywiz.kmem.MemBufferWrap
import com.soywiz.kmem.NewInt8Buffer
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.extract4
import com.soywiz.kmem.getByte
import com.soywiz.kmem.getData
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korio.lang.substr
import kotlin.math.pow

/**
 * The MIT License (MIT)
 * Copyright (c) 2015 Andy Sloane <andy@a1k0n.net>
 */
class XM {
    val player = XMPlayer()

    var f_smp = 44100.0  // updated by play callback, default value here

    companion object {
        // for pretty-printing notes
        val _note_names = arrayOf(
            "C-", "C#", "D-", "D#", "E-", "F-",
            "F#", "G-", "G#", "A-", "A#", "B-")

        // per-sample exponential moving average for volume changes (to prevent pops
        // and clicks); evaluated every 8 samples
        const val popfilter_alpha = 0.9837

        fun prettify_note(note: Int): String {
            if (note < 0) return "---"
            if (note == 96) return "^^^"
            return _note_names[note%12] + (note/12)
        }

        fun prettify_number(num: Int): String {
            if (num == -1) return "--"
            return "0$num".substr(-2)
        }

        fun prettify_volume(num: Int): String {
            if (num < 0x10) return "--"
            return num.toString(16)
        }

        fun prettify_effect(t: Int, p: Int): String {
            return t.toString(16) + "0${p.toString(16)}".substr(-2)
        }

        fun prettify_notedata(data: IntArray): String {
            return (prettify_note(data[0]) + " " + prettify_number(data[1]) + " " +
                prettify_volume(data[2]) + " " +
                prettify_effect(data[3], data[4]))
        }

        fun getstring(dv: DataBuffer, offset: Int, len: Int): String {
            val str = StringBuilder()
            for (i in offset until (offset+len)) {
                val c = dv.getUint8(i)
                if (c == 0) break
                str.append(c.toChar())
            }
            return str.toString()
        }

        fun DataBuffer.getUint32(offset: Int, endian: Boolean): Int {
            val v0 = getByte(offset + 0).toInt() and 0xFF
            val v1 = getByte(offset + 1).toInt() and 0xFF
            val v2 = getByte(offset + 2).toInt() and 0xFF
            val v3 = getByte(offset + 3).toInt() and 0xFF
            return if (endian) (v0 shl 0) or (v1 shl 8) or (v2 shl 16) or (v3 shl 24) else (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
        }
        fun DataBuffer.getUint16(offset: Int, endian: Boolean): Int {
            val v0 = getByte(offset + 0).toInt() and 0xFF
            val v1 = getByte(offset + 1).toInt() and 0xFF
            return if (endian) (v0 shl 0) or (v1 shl 8) else (v0 shl 8) or (v1 shl 0)
        }
        fun DataBuffer.getUint8(offset: Int): Int = getByte(offset).toInt() and 0xFF
        fun DataBuffer.getInt8(offset: Int): Int = getByte(offset).toInt()
        fun Uint8Buffer(buf: ByteArray, offset: Int, size: Int): Uint8Buffer {
            return Uint8Buffer(NewInt8Buffer(MemBufferWrap(buf), offset, size))
        }
    }

    // Return 2-pole Butterworth lowpass filter coefficients for
// center frequncy f_c (relative to sampling frequency)
    fun filterCoeffs(f_c: Double): DoubleArray {
        var f_c = f_c
        if (f_c > 0.5) {  // we can't lowpass above the nyquist frequency...
            f_c = 0.5
        }
        val wct = kotlin.math.sqrt(2.0) * kotlin.math.PI * f_c
        val e = kotlin.math.exp(-wct)
        val c = e * kotlin.math.cos(wct)
        //val gain = (1 - 2*c + e*e) / 2
        //return doubleArrayOf(gain, 2*c, -e*e)
        val s = e * kotlin.math.sin(wct);
        val gain = (1.0 - 2*c + c*c + s*s) / 2.0;
        return doubleArrayOf(gain, 2*c, -c*c - s*s)

    }

    fun updateChannelPeriod(ch: XMChannelInfo, period: Double) {
        val freq: Double = 8363 * 2.0.pow((1152.0 - period) / 192.0)
        if (freq.isNaN()) {
            Console.log("invalid period!", period)
            return
        }
        ch.doff = freq / f_smp
        ch.filter = filterCoeffs(ch.doff / 2)
        //println("ch.doff=${ch.doff}, ch.filter=${ch.filter.toList().joinToString(",")}")
    }

    fun setCurrentPattern() {
        var nextPat = player.xm.songpats[player.cur_songpos]

        // check for out of range pattern index
        while (nextPat >= player.xm.patterns.size) {
            if (player.cur_songpos + 1 < player.xm.songpats.size) {
                // first try skipping the position
                player.cur_songpos++
            } else if ((player.cur_songpos == player.xm.song_looppos && player.cur_songpos != 0)
                || player.xm.song_looppos >= player.xm.songpats.size) {
                // if we allready tried song_looppos or if song_looppos
                // is out of range, go to the first position
                player.cur_songpos = 0
            } else {
                // try going to song_looppos
                player.cur_songpos = player.xm.song_looppos
            }

            nextPat = player.xm.songpats[player.cur_songpos]
        }

        player.cur_pat = nextPat
    }


    fun nextRow() {
        //if (player.next_row < 0) {
        //    player.next_row = player.cur_row + 1
        //}
        //player.cur_row = player.next_row
        //player.next_row++
        player.cur_row++

        if (player.cur_pat == -1 || player.cur_row >= player.xm.patterns[player.cur_pat].size) {
            player.cur_row = 0
            //player.next_row = 1
            player.cur_songpos++
            if (player.cur_songpos >= player.xm.songpats.size)
                player.cur_songpos = player.xm.song_looppos
            setCurrentPattern()
        }
        val p = player.xm.patterns[player.cur_pat]
        val r = p[player.cur_row]
        for (i in r.indices) {
            val ch = player.xm.channelinfo[i]
            var inst = ch.inst
            var triggernote = false
            // instrument trigger
            if (r[i][1] != -1) {
                inst = player.xm.instruments[r[i][1] - 1]
                if (inst.samplemap != null) {
                    ch.inst = inst
                    // retrigger unless overridden below
                    triggernote = true
                    if (inst.samplemap != null) {
                        ch.samp = inst.samples[inst.samplemap!![ch.note]]
                        ch.vol = ch.samp!!.vol
                        ch.pan = ch.samp!!.pan
                        ch.fine = ch.samp!!.fine
                    }
                } else {
                    // console.log("invalid inst", r[i][1], instruments.length);
                }
            }

            // note trigger
            if (r[i][0] != -1) {
                if (r[i][0] == 96) {
                    ch.release = true
                    triggernote = false
                } else {
                    if (inst?.samplemap != null) {
                        val note = r[i][0]
                        ch.note = note
                        ch.samp = inst.samples[inst.samplemap!![ch.note]]
                        if (triggernote) {
                            // if we were already triggering the note, reset vol/pan using
                            // (potentially) new sample
                            ch.pan = ch.samp!!.pan
                            ch.vol = ch.samp!!.vol
                            ch.fine = ch.samp!!.fine
                        }
                        triggernote = true
                    }
                }
            }

            ch.voleffectfn = null
            if (r[i][2] != -1) {  // volume column
                val v = r[i][2]
                ch.voleffectdata = v and 0x0f
                when {
                    v < 0x10 -> {
                        Console.log("channel", i, "invalid volume", v.toString(16))
                    }
                    v <= 0x50 -> {
                        ch.vol = v - 0x10
                    }
                    v in 0x60..0x6f -> {  // volume slide down
                        ch.voleffectfn = { ch, data ->
                            ch.vol = kotlin.math.max(0, ch.vol - ch.voleffectdata)
                        }
                    }
                    v in 0x70..0x7f -> {  // volume slide up
                        ch.voleffectfn = { ch, data ->
                            ch.vol = kotlin.math.min(64, ch.vol + ch.voleffectdata)
                        }
                    }
                    v in 0x80..0x8f -> {  // fine volume slide down
                        ch.vol = kotlin.math.max(0, ch.vol - (v and 0x0f))
                    }
                    v in 0x90..0x9f -> {  // fine volume slide up
                        ch.vol = kotlin.math.min(64, ch.vol + (v and 0x0f))
                    }
                    v in 0xa0..0xaf -> {  // vibrato speed
                        ch.vibratospeed = v and 0x0f
                    }
                    v in 0xb0..0xbf -> {  // vibrato w/ depth
                        ch.vibratodepth = v and 0x0f
                        ch.voleffectfn = player.filters.effects_t1[4]!!  // use vibrato effect directly
                        player.filters.effects_t1[4]!!(ch, 0)  // and also call it on tick 0
                    }
                    v in 0xc0..0xcf -> {  // set panning
                        ch.pan = (v and 0x0f) * 0x11
                    }
                    v in 0xf0..0xff -> {  // portamento
                        if ((v and 0x0f) != 0) {
                            ch.portaspeed = (v and 0x0f) shl 4
                        }
                        ch.voleffectfn = player.filters.effects_t1[3]  // just run 3x0
                    }
                    else -> {
                        Console.log("channel", i, "volume effect", v.toString(16))
                    }
                }
            }

            ch.effect = r[i][3]
            ch.effectdata = r[i][4]
            if (ch.effect < 36) {
                ch.effectfn = player.filters.effects_t1[ch.effect]
                val eff_t0 = player.filters.effects_t0[ch.effect]
                if (eff_t0 != null && eff_t0(ch, ch.effectdata) != Unit) {
                    triggernote = false
                }
            } else {
                Console.log("channel", i, "effect > 36", ch.effect)
            }

            // special handling for portamentos: don't trigger the note
            if (ch.effect == 3 || ch.effect == 5 || r[i][2] >= 0xf0) {
                if (r[i][0] != -1) {
                    ch.periodtarget = ch.periodForNote()
                }
                triggernote = false
                if (inst != null && inst.samplemap != null) {
                    if (ch.env_vol == null) {
                        // note wasn't already playing; we basically have to ignore the
                        // portamento and just trigger
                        triggernote = true
                    } else if (ch.release) {
                        // reset envelopes if note was released but leave offset/pitch/etc
                        // alone
                        ch.envtick = false
                        ch.release = false
                        ch.env_vol = EnvelopeFollower(inst.env_vol!!)
                        ch.env_pan = EnvelopeFollower(inst.env_pan!!)
                    }
                }
            }

            if (triggernote) {
                // there's gotta be a less hacky way to handle offset commands...
                if (ch.effect != 9) ch.off = 0.0
                ch.release = false
                ch.envtick = false
                ch.env_vol = EnvelopeFollower(inst!!.env_vol!!)
                ch.env_pan = EnvelopeFollower(inst.env_pan!!)
                if (ch.note != 0) {
                    ch.period = ch.periodForNote()
                }
                // waveforms 0-3 are retriggered on new notes while 4-7 are continuous
                if (ch.vibratotype < 4) {
                    ch.vibratopos = 0
                }
            }
        }
    }

    class Envelope(
        var points: IntArray,
        var type: Int,
        var sustain: Int,
        loopstartIndex: Int,
        loopendIndex: Int
    ) {
        var loopstart = points[loopstartIndex*2]
        var loopend = points[loopendIndex*2]

        fun Get(ticks: Int): Double {
            // TODO: optimize follower with ptr
            // or even do binary search here
            var y0 = 0
            val env = this.points
            for (i in env.indices step 2) {
                y0 = env[i+1]
                if (ticks < env[i]) {
                    val x0 = env[i-2]
                    y0 = env[i-1]
                    val dx: Int = env[i] - x0
                    val dy: Int = env[i+1] - y0
                    return y0 + (ticks - x0) * dy.toDouble() / dx.toDouble()
                }
            }
            return y0.toDouble()
        }
    }

    class EnvelopeFollower(
        val env: Envelope
    )  {
        var tick = 0

        fun Tick(release: Boolean): Double {
            val value = this.env.Get(this.tick)
            //println("value=$value")

            // if we're sustaining a note, stop advancing the tick counter
            //println("this.env.sustain=${this.env.sustain}, this.env.points=${this.env.points.size}")
            if (!release && this.tick >= this.env.points.getOrElse(this.env.sustain*2) { 0 }) {
                return this.env.points.getOrElse(this.env.sustain*2 + 1) { 0 }.toDouble()
            }

            this.tick++
            if ((this.env.type and 4) != 0) {  // envelope loop?
                if (!release && this.tick >= this.env.loopend) {
                    this.tick -= this.env.loopend - this.env.loopstart
                }
            }
            return value
        }
    }

    fun nextTick() {
        player.cur_tick++
        for (j in 0 until player.xm.nchan) {
            val ch = player.xm.channelinfo[j]
            ch.periodoffset = 0.0
        }
        if (player.cur_tick >= player.xm.tempo) {
            player.cur_tick = 0
            nextRow()
        }
        for (j in 0 until player.xm.nchan) {
            val ch = player.xm.channelinfo[j]
            val inst = ch.inst
            if (player.cur_tick != 0) {
                if(ch.voleffectfn != null) ch.voleffectfn!!(ch, 0)
                if(ch.effectfn != null) ch.effectfn!!(ch, 0)
            }
            if (ch.period.isNaN()) {
                Console.log(prettify_notedata(
                    player.xm.patterns[player.cur_pat][player.cur_row][j]),
                    "set channel", j, "period to NaN")
            }
            if (inst == null) continue
            if (ch.env_vol == null) {
                Console.log(prettify_notedata(
                    player.xm.patterns[player.cur_pat][player.cur_row][j]),
                    "set channel", j, "env_vol to undefined, but note is playing")
                continue
            }
            ch.volE = ch.env_vol!!.Tick(ch.release)
            ch.panE = ch.env_pan!!.Tick(ch.release)
            //println("volE=${ch.volE}, panE=${ch.panE}")
            updateChannelPeriod(ch, ch.period + ch.periodoffset)
        }
    }

    // This fun gradually brings the channel back down to zero if it isn't
    // already to avoid clicks and pops when samples end.
    fun MixSilenceIntoBuf(ch: XMChannelInfo, start: Int, end: Int, dataL: FloatArray, dataR: FloatArray): Float {
        var s: Double = ch.filterstate[1].toDouble()
        if (s.isNaN()) {
            Console.log("NaN filterstate?", ch.filterstate, ch.filter)
            return 0f
        }
        var i = start
        while (i < end) {
            if (kotlin.math.abs(s) < 1.526e-5) {  // == 1/65536.0
                s = 0.0
                break
            }
            dataL[i] += (s * ch.vL).toFloat()
            dataR[i] += (s * ch.vR).toFloat()
            s *= popfilter_alpha
            i++
        }
        ch.filterstate[1] = s.toFloat()
        ch.filterstate[2] = s.toFloat()
        if (s.isNaN()) {
            Console.log("NaN filterstate after adding silence?", ch.filterstate, ch.filter, i)
            return 0f
        }
        return 0f
    }

    fun MixChannelIntoBuf(ch: XMChannelInfo, start: Int, end: Int, dataL: FloatArray, dataR: FloatArray): Float {
        val inst = ch.inst
        val instsamp = ch.samp
        var loop = false
        var looplen = 0
        var loopstart = 0

        // nothing on this channel, just filter the last dc offset back down to zero
        if (instsamp == null || inst == null || ch.mute) {
            return MixSilenceIntoBuf(ch, start, end, dataL, dataR)
        }

        //println("inst=$inst, instsamp=$instsamp")

        val samp: FloatArray = instsamp.sampledata
        var sample_end: Int = instsamp.len
        if ((instsamp.type and 3) == 1 && instsamp.looplen > 0) {
            loop = true
            loopstart = instsamp.loop
            looplen = instsamp.looplen
            sample_end = loopstart + looplen
        }
        var samplen: Int = instsamp.len
        val volE: Double = ch.volE / 64.0    // current volume envelope
        val panE: Double = 4*(ch.panE - 32)  // current panning envelope
        val p: Double = panE + ch.pan - 128  // final pan
        var volL: Double = player.xm.global_volume.toDouble() * volE * (128 - p) * ch.vol.toDouble() / (64.0 * 128.0 * 128.0)
        var volR: Double = player.xm.global_volume.toDouble() * volE * (128 + p) * ch.vol.toDouble() / (64.0 * 128.0 * 128.0)
        if (volL < 0) volL = 0.0
        if (volR < 0) volR = 0.0
        if (volR == 0.0 && volL == 0.0)
            return 0f
        if (volR.isNaN() || volL.isNaN()) {
            Console.log("NaN volume!?", ch.number, volL, volR, volE, panE, ch.vol)
            return 0f
        }
        var k: Double = ch.off
        val dk: Double = ch.doff
        var Vrms = 0.0
        val f0: Double = ch.filter[0]
        val f1: Double = ch.filter[1]
        val f2: Double = ch.filter[2]
        var fs0: Float = ch.filterstate[0]
        var fs1: Float = ch.filterstate[1]
        var fs2: Float = ch.filterstate[2]

        // we also low-pass filter volume changes with a simple one-zero,
        // one-pole filter to avoid pops and clicks when volume changes.
        val vL = popfilter_alpha * ch.vL + (1 - popfilter_alpha) * (volL + ch.vLprev) * 0.5
        val vR = popfilter_alpha * ch.vR + (1 - popfilter_alpha) * (volR + ch.vRprev) * 0.5
        val pf_8 = popfilter_alpha.pow(8.0)
        ch.vLprev = volL
        ch.vRprev = volR

        // we can mix up to this many bytes before running into a sample end/loop
        var i: Int = start
        var failsafe = 100
        if (start == 1067220) {

        }
        while (i < end) {
            if (failsafe-- == 0) {
                Console.log("failsafe in mixing loop! channel", ch.number, k, sample_end, loopstart, looplen, dk)
                break
            }
            if (k >= sample_end) {  // TODO: implement pingpong looping
                if (loop) {
                    k = loopstart + ((k - loopstart) % looplen)
                } else {
                    // kill sample
                    ch.inst = null
                    // fill rest of buf with filtered dc offset using loop above
                    return (Vrms + MixSilenceIntoBuf(ch, i, end, dataL, dataR)).toFloat()
                }
            }
            val next_event: Double = kotlin.math.max(1.0, kotlin.math.min(end.toDouble(), i + (sample_end - k) / dk)).toDouble()
            // this is the inner loop of the player

            // unrolled 8x (removed the unroll)
            while (i < next_event) {
                val s = samp[k.toInt()]
                // we low-pass filter here since we are resampling some arbitrary
                // frequency to f_smp; this is an anti-aliasing filter and is
                // implemented as an IIR butterworth filter (usually we'd use an FIR
                // brick wall filter, but this is much simpler computationally and
                // sounds fine)
                val y = f0 * (s + fs0) + f1 * fs1 + f2 * fs2
                fs2 = fs1
                fs1 = y.toFloat()
                fs0 = s
                dataL[i] += (vL * y).toFloat()
                dataR[i] += (vR * y).toFloat()
                Vrms += (vL + vR) * y * y
                k += dk
                //println("dataL[$i]=${dataL[i]}, vL=$vL, y=$y, s=$s, f0=$f0, f1=$f1, f2=$f2")
                i++
            }
        }
        //println("steps: ${i}, failsafe: ${failsafe}, Vrms: ${(Vrms * 1000).toInt()}")
        ch.off = k
        ch.filterstate[0] = fs0
        ch.filterstate[1] = fs1
        ch.filterstate[2] = fs2
        ch.vL = vL
        ch.vR = vR
        return (Vrms * 0.5).toFloat()
    }

    class AudioBuffer(
        val channels: Array<FloatArray>
    ) {
        var size: Int = channels[0].size
        fun getChannelData(index: Int): FloatArray = channels[index]
    }

    class AudioEvent(
        val sampleRate: Int,
        val playbackTime: Double,
        val outputBuffer: AudioBuffer,
    )

    fun audio_cb(e: AudioEvent) {
        f_smp = e.sampleRate.toDouble()
        var buflen: Int = e.outputBuffer.size
        val dataL: FloatArray = e.outputBuffer.getChannelData(0)
        val dataR: FloatArray = e.outputBuffer.getChannelData(1)

        for (i in 0 until buflen) {
            dataL[i] = 0f
            dataR[i] = 0f
        }

        var offset = 0
        val ticklen = 0 or (f_smp.toDouble() * 2.5 / player.xm.bpm.toDouble()).toInt()
        val scopewidth = player.scope_width

        //println("ticklen=$ticklen")
        //println("scopewidth=$scopewidth")

        while (buflen > 0) {
            if (player.cur_pat == -1 || player.cur_ticksamp >= ticklen) {
                nextTick()
                player.cur_ticksamp -= ticklen
            }
            val tickduration = kotlin.math.min(buflen, ticklen - player.cur_ticksamp)
            val VU = FloatArray(player.xm.nchan)
            var scopes: ArrayList<FloatArray>? = null
            for (j in 0 until player.xm.nchan) {
                var scope: FloatArray? = null
                if (tickduration >= 4*scopewidth) {
                    scope = FloatArray(scopewidth)
                    for (k in 0 until scopewidth) {
                        scope[k] = -dataL[offset+k*4] - dataR[offset+k*4]
                    }
                }


                //val sum0 = dataL.sum()
                VU[j] = MixChannelIntoBuf(
                    player.xm.channelinfo[j], offset, offset + tickduration, dataL, dataR
                ) / tickduration
                //println("VU[$j]=${(VU[j].toDouble() * 1000).toInt()}, offset=$offset, tickduration=$tickduration, dataLsum0=${sum0.toInt()}, dataLsum1=${dataL.sum().toInt()}")

                if (tickduration >= 4*scopewidth) {
                    for (k in 0 until scopewidth) {
                        scope!![k] += dataL[offset+k*4] + dataR[offset+k*4]
                    }
                    if (scopes == null) scopes = arrayListOf()
                    scopes.add(scope!!)
                }
            }
            val playbackTime: Double = e.playbackTime
            player.pushEvent(
                t = e.playbackTime + (0.0 + offset) / f_smp.toDouble(),
                vu = VU,
                scopes = scopes,
                songpos = player.cur_songpos,
                pat = player.cur_pat,
                row = player.cur_row
            )
            offset += tickduration
            player.cur_ticksamp += tickduration
            buflen -= tickduration
        }
    }

    fun ConvertSample(array: Uint8Buffer, bits: Int): FloatArray {
        var len = array.size
        if (bits == 0) {  // 8 bit sample
            //println("8bit sample")
            val samp = FloatArray(len)
            var acc = 0
            for (k in 0 until len) {
                acc += array[k]
                //println("acc=$acc")
                var b = acc and 255
                if ((b and 128) != 0) b -= 256
                samp[k] = b.toFloat() / 128f
            }
            return samp
        } else {
            //println("16bit sample")
            len /= 2
            val samp = FloatArray(len)
            var acc = 0.0
            for (k in 0 until len) {
                var b: Int = array[k*2] or (array[k*2 + 1] shl 8)
                if ((b and 32768) != 0) b -= 65536
                acc = kotlin.math.max(-1.0, kotlin.math.min(1.0, acc + b.toDouble() / 32768.0))
                samp[k] = acc.toFloat()
            }
            return samp
        }
    }


    // optimization: unroll short sample loops so we can run our inner mixing loop
// uninterrupted for as long as possible; this also handles pingpong loops.
    fun UnrollSampleLoop(samp: XMSamp) {
        var nloops: Int = (2048 + samp.looplen - 1) / samp.looplen
        val pingpong = (samp.type and 2) != 0
        if (pingpong) {
            // make sure we have an even number of loops if we are pingponging
            nloops = (nloops + 1) and (1.inv())
        }
        val samplesiz = samp.loop + nloops * samp.looplen
        val data = FloatArray(samplesiz)
        for (i in 0 until samp.loop) {
            data[i] = samp.sampledata[i]
        }
        var i = samp.loop
        for (j in 0 until nloops) {
            if ((j and 1) != 0 && pingpong) {
                for (k in samp.looplen - 1 downTo 0) {
                    data[i++] = samp.sampledata[samp.loop + k]
                }
            } else {
                for (k in 0 until samp.looplen) {
                    data[i++] = samp.sampledata[samp.loop + k]
                }
            }
        }
        Console.log("unrolled sample loop; looplen", samp.looplen, "x", nloops, " = ", samplesiz)
        samp.sampledata = data
        samp.looplen = nloops * samp.looplen
        samp.type = 1
    }

    fun load(arrayBuf: ByteArray): Boolean {
        val dv = MemBufferWrap(arrayBuf).getData()

        player.xm.songname = getstring(dv, 17, 20)
        val hlen = dv.getUint32(0x3c, true) + 0x3c
        val songlen = dv.getUint16(0x40, true)
        player.xm.song_looppos = dv.getUint16(0x42, true)
        player.xm.nchan = dv.getUint16(0x44, true)
        val npat = dv.getUint16(0x46, true)
        val ninst = dv.getUint16(0x48, true)
        player.xm.flags = dv.getUint16(0x4a, true)
        player.xm.tempo = dv.getUint16(0x4c, true)
        player.xm.bpm = dv.getUint16(0x4e, true)
        player.xm.channelinfo.clear()
        player.xm.global_volume = player.max_global_volume

        for (i in 0 until player.xm.nchan) {
            player.xm.channelinfo.add(XMChannelInfo(
                number = i,
                filterstate = FloatArray(3),
                vol = 0,
                pan = 128,
                period = 1920.0 - 48*16,
                vL = 0.0, vR = 0.0,   // left right volume envelope followers (changes per sample)
                vLprev = 0.0, vRprev = 0.0,
                mute = false,
                volE = 0.0, panE = 0.0,
                retrig = 0,
                vibratopos = 0,
                vibratodepth = 1,
                vibratospeed = 1,
                vibratotype = 0,
            ))
        }
        Console.log("header len ", hlen)

        Console.log("songlen $songlen, ${player.xm.nchan} channels, $npat patterns, $ninst instruments")
        Console.log("loop @${player.xm.song_looppos}")
        Console.log("flags=${player.xm.flags} tempo ${player.xm.tempo} bpm ${player.xm.bpm}")

        player.xm.songpats.clear()
        for (i in 0 until songlen) {
            player.xm.songpats.add(dv.getUint8(0x50 + i))
        }
        Console.log("song patterns: " + player.xm.songpats)

        var idx: Int = hlen
        player.xm.patterns.clear()
        for (i in 0 until npat) {
            val pattern = arrayListOf<List<IntArray>>()
            var patheaderlen = dv.getUint32(idx, true)
            val patrows = dv.getUint16(idx + 5, true)
            val patsize = dv.getUint16(idx + 7, true)
            Console.log("pattern $i: $patsize bytes, $patrows rows")
            idx += 9
            var j = 0
            while (patsize > 0 && j < patrows) {
                val row = arrayListOf<IntArray>()
                for (k in 0 until player.xm.nchan) {
                    val byte0 = dv.getUint8(idx); idx++
                    var note = -1
                    var inst = -1
                    var vol = -1
                    var efftype = 0
                    var effparam = 0
                    if ((byte0 and 0x80) != 0) {
                        if ((byte0 and 0x01) != 0) {
                            note = dv.getUint8(idx) - 1; idx++
                        }
                        if ((byte0 and 0x02) != 0) {
                            inst = dv.getUint8(idx); idx++
                        }
                        if ((byte0 and 0x04) != 0) {
                            vol = dv.getUint8(idx); idx++
                        }
                        if ((byte0 and 0x08) != 0) {
                            efftype = dv.getUint8(idx); idx++
                        }
                        if ((byte0 and 0x10) != 0) {
                            effparam = dv.getUint8(idx); idx++
                        }
                    } else {
                        // byte0 is note from 1..96 or 0 for nothing or 97 for release
                        // so we subtract 1 so that C-0 is stored as 0
                        note = byte0 - 1
                        inst = dv.getUint8(idx); idx++
                        vol = dv.getUint8(idx); idx++
                        efftype = dv.getUint8(idx); idx++
                        effparam = dv.getUint8(idx); idx++
                    }
                    val notedata = intArrayOf(note, inst, vol, efftype, effparam)
                    row.add(notedata)
                }
                pattern.add(row)
                j++
            }
            player.xm.patterns.add(pattern)
        }

        player.xm.instruments.clear()
        // now load instruments
        for (i in 0 until ninst) {
            val hdrsiz = dv.getUint32(idx, true)
            val instname = getstring(dv, idx+0x4, 22)
            val nsamp = dv.getUint16(idx+0x1b, true)
            val inst = XMInst(
                name = instname,
                number = i,
            )
            if (nsamp > 0) {
                val samplemap = Uint8Buffer(arrayBuf, idx+33, 96)

                val env_nvol = dv.getUint8(idx+225)
                val env_vol_type = dv.getUint8(idx+233)
                var env_vol_sustain = dv.getUint8(idx+227)
                val env_vol_loop_start = dv.getUint8(idx+228)
                val env_vol_loop_end = dv.getUint8(idx+229)
                val env_npan = dv.getUint8(idx+226)
                val env_pan_type = dv.getUint8(idx+234)
                var env_pan_sustain = dv.getUint8(idx+230)
                val env_pan_loop_start = dv.getUint8(idx+231)
                val env_pan_loop_end = dv.getUint8(idx+232)
                val vol_fadeout = dv.getUint16(idx+239, true)
                val env_vol = IntArrayList()
                for (j in 0 until (env_nvol*2)) {
                    env_vol.add(dv.getUint16(idx+129+j*2, true))
                }
                val env_pan = IntArrayList()
                for (j in 0 until (env_npan*2)) {
                    env_pan.add(dv.getUint16(idx+177+j*2, true))
                }
                // FIXME: ignoring keymaps for now and assuming 1 sample / instrument
                // var keymap = getarray(dv, idx+0x21);
                val samphdrsiz = dv.getUint32(idx+0x1d, true)
                Console.log("hdrsiz $hdrsiz; instrument ${(i+1).toString(16)}: '$instname' $nsamp samples, samphdrsiz $samphdrsiz")
                idx += hdrsiz
                var totalsamples = 0
                val samps = arrayListOf<XMSamp>()
                for (j in 0 until nsamp) {
                    val samplen = dv.getUint32(idx, true)
                    val samploop = dv.getUint32(idx+4, true)
                    val samplooplen = dv.getUint32(idx+8, true)
                    val sampvol = dv.getUint8(idx+12)
                    val sampfinetune = dv.getInt8(idx+13)
                    var samptype = dv.getUint8(idx+14)
                    val samppan = dv.getUint8(idx+15)
                    val sampnote = dv.getInt8(idx+16)
                    val sampname = getstring(dv, idx+18, 22)
                    val sampleoffset = totalsamples
                    if (samplooplen == 0) {
                        samptype = samptype and 3.inv()
                    }
                    Console.log("sample $j: len $samplen name '$sampname' loop $samploop/$samplooplen vol $sampvol offset ${sampleoffset.toString(16)}")
                    Console.log("           type $samptype note ${prettify_note(sampnote + 12*4)}($sampnote) finetune $sampfinetune pan $samppan")
                    Console.log("           vol env", env_vol, env_vol_sustain, env_vol_loop_start, env_vol_loop_end, "type", env_vol_type, "fadeout", vol_fadeout)
                    Console.log("           pan env", env_pan, env_pan_sustain, env_pan_loop_start, env_pan_loop_end, "type", env_pan_type)
                    val samp = XMSamp(
                        len = samplen, loop = samploop,
                        looplen = samplooplen, note = sampnote, fine = sampfinetune,
                        pan = samppan, type = samptype, vol = sampvol,
                        fileoffset = sampleoffset
                    )
                    // length / pointers are all specified in bytes; fixup for 16-bit samples
                    samps.add(samp)
                    idx += samphdrsiz
                    totalsamples += samplen
                }
                for (j in 0 until nsamp) {
                    val samp = samps[j]
                    samp.sampledata = ConvertSample(
                        Uint8Buffer(arrayBuf, idx + samp.fileoffset, samp.len),
                        samp.type and 16
                    )
                    if ((samp.type and 16) != 0) {
                        samp.len /= 2
                        samp.loop /= 2
                        samp.looplen /= 2
                    }
                    // unroll short loops and any pingpong loops
                    if ((samp.type and 3) != 0 && (samp.looplen < 2048 || (samp.type and 2) != 0)) {
                        UnrollSampleLoop(samp)
                    }
                }
                idx += totalsamples
                inst.samplemap = samplemap
                inst.samples = samps
                if (env_vol_type != 0) {
                    // insert an automatic fadeout to 0 at the end of the envelope
                    val env_end_tick = env_vol[env_vol.size-2]
                    if ((env_vol_type and 2) == 0) {  // if there's no sustain point, create one
                        env_vol_sustain = env_vol.size / 2
                    }
                    if (vol_fadeout > 0) {
                        val fadeout_ticks = 65536.0 / vol_fadeout
                        env_vol.add((env_end_tick + fadeout_ticks).toInt())
                        env_vol.add(0)
                    }
                    inst.env_vol = Envelope(
                        env_vol.toIntArray(),
                        env_vol_type,
                        env_vol_sustain,
                        env_vol_loop_start,
                        env_vol_loop_end
                    )
                } else {
                    // no envelope, then just make a default full-volume envelope.
                    // i thought this would use fadeout, but apparently it doesn't.
                    inst.env_vol = Envelope(intArrayOf(0, 64, 1, 0), 2, 0, 0, 0)
                }
                if (env_pan_type != 0) {
                    if ((env_pan_type and 2) == 0) {  // if there's no sustain point, create one
                        env_pan_sustain = env_pan.size / 2
                    }
                    inst.env_pan = Envelope(
                        env_pan.toIntArray(),
                        env_pan_type,
                        env_pan_sustain,
                        env_pan_loop_start,
                        env_pan_loop_end
                    )
                } else {
                    // create a default empty envelope
                    inst.env_pan = Envelope(intArrayOf(0, 32), 0, 0, 0, 0)
                }
            } else {
                idx += hdrsiz
                Console.log("empty instrument", i, hdrsiz, idx)
            }
            player.xm.instruments.add(inst)
        }

        Console.log("loaded \"${player.xm.songname}\"")
        return true
    }

    fun play() {
        if (!player.playing) {
            // put paused events back into action, if any
            //player.view.resume()
            // start playing
            //jsNode.connect(gainNode);
        }
        player.playing = true
    }

    fun pause() {
        if (player.playing) {
            //jsNode.disconnect(gainNode);
            //player.view.pause()
        }
        player.playing = false
    }

    fun stop() {
        if (player.playing) {
            //jsNode.disconnect(gainNode);
            player.playing = false
        }
        player.cur_pat = -1
        player.cur_row = 64
        player.cur_songpos = -1
        player.cur_ticksamp = 0
        player.xm.global_volume = player.max_global_volume
        //player.view.stop()
        //init();
    }

    class XMPlayer(val xm: XMData = XMData()) {
        val filters = XMFilters(this)
        //val view = XMView()
        var cur_songpos = -1
        var cur_pat = -1
        var cur_row = 64
        var cur_ticksamp = 0
        var scope_width = 16
        var cur_tick = 6
        var next_row: Int = -1
        var playing: Boolean = false
        var max_global_volume: Int = 128

        fun pushEvent(t: Double, vu: FloatArray, scopes: List<FloatArray>?, songpos: Int, pat: Int, row: Int) {
        }
    }

    class XMData {
        var songname = ""
        val patterns = arrayListOf<List<List<IntArray>>>()
        val channelinfo = arrayListOf<XMChannelInfo>()
        val instruments = arrayListOf<XMInst>()
        var nchan: Int = 0
        val songpats = IntArrayList()
        var global_volume: Int = 128
        var global_volumeslide: Int? = null
        var song_looppos = 0
        var tempo: Int = 0
        var flags: Int = 0
        var bpm: Int = 0
    }

    class XMSamp(
        var len: Int = 0,
        var loop: Int = 0,
        var looplen: Int = 0,
        var note: Int = 0,
        var fine: Int = 0,
        var pan: Int = 0,
        var type: Int = 0,
        var vol: Int = 0,
        var fileoffset: Int = 0,
    ) {
        var sampledata: FloatArray = FloatArray(0)
    }

    class XMInst(val name: String, val number: Int) {
        var env_vol: Envelope? = null
        var env_pan: Envelope? = null
        var samplemap: Uint8Buffer? = null
        var samples = arrayListOf<XMSamp>()
    }


    class XMChannelInfo(
        var number: Int,
        val filterstate: FloatArray = FloatArray(3),
        var vol: Int = 0,
        var pan: Int = 128,
        var period: Double = (1920 - 48*16).toDouble(),
        var vL: Double = 0.0,
        var vR: Double = 0.0,   // left right volume envelope followers (changes per sample)
        var vLprev: Double = 0.0,
        var vRprev: Double = 0.0,
        var mute: Boolean = false,
        var volE: Double = 0.0,
        var panE: Double = 0.0,
        var retrig: Int = 0,
        var vibratopos: Int = 0,
        var vibratodepth: Int = 1,
        var vibratospeed: Int = 1,
        var vibratotype: Int = 0,
    ) {
        var voleffectfn: ((XMChannelInfo, Int) -> Unit)? = null
        var voleffectdata: Int = 0

        var effectfn: ((XMChannelInfo, Int) -> Unit)? = null
        var effect: Int = 0

        var periodoffset: Double = 0.0
        var volumeslide: Int? = null
        var effectdata: Int = 0
        var note: Int = 0
        var off: Double = 0.0
        var doff: Double = 0.0
        var fine: Int = 0
        var filter: DoubleArray = DoubleArray(3)
        var samp: XMSamp? = null
        var loopstart: Int? = null
        var loopend: Int? = null
        var loopremaining: Int = 0
        var inst: XMInst? = null
        var slideupspeed: Int? = null
        var slidedownspeed: Int? = null
        var portaspeed: Int? = null
        var periodtarget: Double? = null
        var finevolup: Int? = null
        var finevoldown: Int? = null
        var release: Boolean = false
        var envtick: Boolean = false
        var env_vol: EnvelopeFollower? = null
        var env_pan: EnvelopeFollower? = null

        fun periodForNote(note: Int = this.note): Double {
            return 1920.0 - (note.toDouble() + samp!!.note)*16 - fine.toDouble() / 8.0
        }
    }


    class XMFilters(val player: XMPlayer) {
        val effects_t0: Array<((XMChannelInfo, Int) -> Unit)?> = arrayOf(  // effect functions on tick 0
            ::eff_t1_0,  // 1, arpeggio is processed on all ticks
            ::eff_t0_1,
            ::eff_t0_2,
            ::eff_t0_3,
            ::eff_t0_4,  // 4
            ::eff_t0_a,  // 5, same as A on first tick
            ::eff_t0_a,  // 6, same as A on first tick
            ::eff_unimplemented_t0,  // 7
            ::eff_t0_8,  // 8
            ::eff_t0_9,  // 9
            ::eff_t0_a,  // a
            ::eff_t0_b,  // b
            ::eff_t0_c,  // c
            ::eff_t0_d,  // d
            ::eff_t0_e,  // e
            ::eff_t0_f,  // f
            ::eff_t0_g,  // g
            ::eff_t0_h,  // h
            ::eff_unimplemented_t0,  // i
            ::eff_unimplemented_t0,  // j
            ::eff_unimplemented_t0,  // k
            ::eff_unimplemented_t0,  // l
            ::eff_unimplemented_t0,  // m
            ::eff_unimplemented_t0,  // n
            ::eff_unimplemented_t0,  // o
            ::eff_unimplemented_t0,  // p
            ::eff_unimplemented_t0,  // q
            ::eff_t0_r,  // r
            ::eff_unimplemented_t0,  // s
            ::eff_unimplemented_t0,  // t
            ::eff_unimplemented_t0,  // u
            ::eff_unimplemented_t0,  // v
            ::eff_unimplemented_t0,  // w
            ::eff_unimplemented_t0,  // x
            ::eff_unimplemented_t0,  // y
            ::eff_unimplemented_t0,  // z
        )

        val effects_t1: Array<((XMChannelInfo, Int) -> Unit)?> = arrayOf(  // effect functions on tick 1+
            ::eff_t1_0,
            ::eff_t1_1,
            ::eff_t1_2,
            ::eff_t1_3,
            ::eff_t1_4,
            ::eff_t1_5,  // 5
            ::eff_t1_6,  // 6
            ::eff_unimplemented,  // 7
            null,   // 8
            null,   // 9
            ::eff_t1_a,  // a
            null,   // b
            null,   // c
            null,   // d
            ::eff_t1_e,  // e
            null,   // f
            null,  // g
            ::eff_t1_h,  // h
            ::eff_unimplemented,  // i
            ::eff_unimplemented,  // j
            ::eff_unimplemented,  // k
            ::eff_unimplemented,  // l
            ::eff_unimplemented,  // m
            ::eff_unimplemented,  // n
            ::eff_unimplemented,  // o
            ::eff_unimplemented,  // p
            ::eff_unimplemented,  // q
            ::eff_t1_r,  // r
            ::eff_unimplemented,  // s
            ::eff_unimplemented,  // t
            ::eff_unimplemented,  // u
            ::eff_unimplemented,  // v
            ::eff_unimplemented,  // w
            ::eff_unimplemented,  // x
            ::eff_unimplemented,  // y
            ::eff_unimplemented   // z
        )

        fun eff_t1_0(ch: XMChannelInfo, data: Int) {  // arpeggio
            if (ch.effectdata != 0 && ch.inst != null) {
                val arpeggioIndex = player.cur_tick % 3
                var note = ch.note
                when (arpeggioIndex) {
                    0 -> Unit
                    1 -> note += (ch.effectdata ushr 4)
                    2 -> note += (ch.effectdata and 15)
                }
                ch.period = ch.periodForNote(note)
            }
        }

        fun eff_t0_1(ch: XMChannelInfo, data: Int) {  // pitch slide up
            if (data != 0) {
                ch.slideupspeed = data
            }
        }

        fun eff_t1_1(ch: XMChannelInfo, data: Int) {  // pitch slide up
            if (ch.slideupspeed != null) {
                // is this limited? it appears not
                ch.period -= ch.slideupspeed!!
            }
        }

        fun eff_t0_2(ch: XMChannelInfo, data: Int) {  // pitch slide down
            if (data != 0) {
                ch.slidedownspeed = data
            }
        }

        fun eff_t1_2(ch: XMChannelInfo, data: Int) {  // pitch slide down
            if (ch.slidedownspeed != null) {
                // 1728 is the period for C-1
                ch.period = kotlin.math.min(1728.0, ch.period + ch.slidedownspeed!!)
            }
        }

        fun eff_t0_3(ch: XMChannelInfo, data: Int) {  // portamento
            if (data != 0) {
                ch.portaspeed = data
            }
        }

        fun eff_t1_3(ch: XMChannelInfo, data: Int) {  // portamento
            if (ch.periodtarget != null && ch.portaspeed != null) {
                if (ch.period > ch.periodtarget!!) {
                    ch.period = kotlin.math.max(ch.periodtarget!!, (ch.period - ch.portaspeed!!))
                } else {
                    ch.period = kotlin.math.min(ch.periodtarget!!, (ch.period + ch.portaspeed!!))
                }
            }
        }

        fun eff_t0_4(ch: XMChannelInfo, data: Int) {  // vibrato
            if ((data and 0x0f) != 0) {
                ch.vibratodepth = (data and 0x0f) * 2
            }
            if ((data ushr 4) != 0) {
                ch.vibratospeed = data ushr 4
            }
            eff_t1_4(ch, data)
        }

        fun eff_t1_4(ch: XMChannelInfo, data: Int) {  // vibrato
            ch.periodoffset = getVibratoDelta(ch.vibratotype, ch.vibratopos) * ch.vibratodepth
            if (ch.periodoffset.isNaN()) {
                Console.log("vibrato periodoffset NaN?", ch.vibratopos, ch.vibratospeed, ch.vibratodepth)
                ch.periodoffset = 0.0
            }
            // only updates on non-first ticks
            if (player.cur_tick > 0) {
                ch.vibratopos += ch.vibratospeed
                ch.vibratopos = ch.vibratopos and 63
            }
        }

        fun getVibratoDelta(type: Int, x: Int): Double {
            val delta: Double = when (type and 0x03) {
                1 -> ((1.0 + x.toDouble() * 2.0 / 64.0) % 2.0) - 1.0 // sawtooth (ramp-down)
                2, 3 -> if (x < 32) 1.0 else -1.0 // square, random (in FT2 these two are the same)
                else -> kotlin.math.sin(x.toDouble() * kotlin.math.PI / 32.0)
            }
            return delta
        }

        fun eff_t1_5(ch: XMChannelInfo, data: Int) {  // portamento + volume slide
            eff_t1_a(ch, data)
            eff_t1_3(ch, data)
        }

        fun eff_t1_6(ch: XMChannelInfo, data: Int) {  // vibrato + volume slide
            eff_t1_a(ch, data)
            eff_t1_4(ch, data)
        }

        fun eff_t0_8(ch: XMChannelInfo, data: Int) {  // set panning
            ch.pan = data
        }

        fun eff_t0_9(ch: XMChannelInfo, data: Int) {  // sample offset
            ch.off = (data * 256).toDouble()
        }

        fun eff_t0_a(ch: XMChannelInfo, data: Int) {  // volume slide
            if (data != 0) {
                ch.volumeslide = -(data and 0x0f) + (data ushr 4)
            }
        }

        fun eff_t1_a(ch: XMChannelInfo, data: Int) {  // volume slide
            if (ch.volumeslide != null) {
                ch.vol = kotlin.math.max(0, kotlin.math.min(64, ch.vol + ch.volumeslide!!))
            }
        }

        fun eff_t0_b(ch: XMChannelInfo, data: Int) {  // song jump (untested)
            if (data < player.xm.songpats.size) {
                //player.cur_songpos = data - 1
                //player.cur_pat = -1
                //player.cur_row = -1
                player.cur_songpos = data;
                player.cur_pat = player.xm.songpats[player.cur_songpos];
                player.cur_row = -1;
            }
        }

        fun eff_t0_c(ch: XMChannelInfo, data: Int) {  // set volume
            ch.vol = kotlin.math.min(64, data)
        }

        fun eff_t0_d(ch: XMChannelInfo, data: Int) {  // pattern jump
            player.cur_songpos++
            if (player.cur_songpos >= player.xm.songpats.size)
                player.cur_songpos = player.xm.song_looppos
            player.cur_pat = player.xm.songpats[player.cur_songpos]
            //player.next_row = (data ushr 4) * 10 + (data and 0x0f)
            player.cur_row = (data ushr 4) * 10 + (data and 0x0f) - 1;
        }

        fun eff_t0_e(ch: XMChannelInfo, data: Int) {  // extended effects!
            val eff = data.extract4(4)
            var data = data.extract4(0)
            when (eff) {
                1 -> ch.period -= data // fine porta up
                2 -> ch.period += data // fine porta down
                4 -> ch.vibratotype = data and 0x07 // set vibrato waveform
                5 -> ch.fine = (data shl 4) + data - 128 // finetune
                //6 -> {  // pattern loop
                //    if (data == 0) {
                //        ch.loopstart = player.cur_row
                //    } else {
                //        if (ch.loopend == null) {
                //            ch.loopend = player.cur_row
                //            ch.loopremaining = data
                //        }
                //        if (ch.loopremaining != 0) {
                //            ch.loopremaining--
                //            player.next_row = ch.loopstart ?: 0
                //        } else {
                //            ch.loopend = null
                //            ch.loopstart = null
                //        }
                //    }
                //}
                8 -> ch.pan = data * 0x11 // panning
                0x0a -> {  // fine vol slide up (with memory)
                    if (data == 0 && ch.finevolup != null) data = ch.finevolup!!
                    ch.vol = kotlin.math.min(64, ch.vol + data)
                    ch.finevolup = data
                }
                0x0b -> {  // fine vol slide down
                    if (data == 0 && ch.finevoldown != null) data = ch.finevoldown!!
                    ch.vol = kotlin.math.max(0, ch.vol - data)
                    ch.finevoldown = data
                }
                0x0c -> Unit  // note cut handled in eff_t1_e
                else -> {
                    Console.log("unimplemented extended effect E", ch.effectdata.toString(16))
                }
            }
        }

        fun eff_t1_e(ch: XMChannelInfo, data: Int) {  // note cut
            when (ch.effectdata ushr 4) {
                0x0c -> {
                    if (player.cur_tick == (ch.effectdata and 0x0f)) {
                        ch.vol = 0
                    }
                }
            }
        }

        fun eff_t0_f(ch: XMChannelInfo, data: Int) {  // set tempo
            when {
                data == 0 -> {
                    Console.log("tempo 0?")
                    return
                }
                data < 0x20 -> player.xm.tempo = data
                else -> player.xm.bpm = data
            }
        }

        fun eff_t0_g(ch: XMChannelInfo, data: Int) {  // set global volume
            if (data <= 0x40) {
                // volume gets multiplied by 2 to match
                // the initial max global volume of 128
                player.xm.global_volume = kotlin.math.max(0, data * 2)
            } else {
                player.xm.global_volume = player.max_global_volume
            }
        }

        fun eff_t0_h(ch: XMChannelInfo, data: Int) {  // global volume slide
            if (data != 0) {
                // same as Axy but multiplied by 2
                player.xm.global_volumeslide = (-(data and 0x0f) + (data ushr 4)) * 2
            }
        }

        fun eff_t1_h(ch: XMChannelInfo, data: Int) {  // global volume slide
            if (player.xm.global_volumeslide != null) {
                player.xm.global_volume = kotlin.math.max(0, kotlin.math.min(player.max_global_volume, player.xm.global_volume + player.xm.global_volumeslide!!))
            }
        }

        fun eff_t0_r(ch: XMChannelInfo, data: Int) {  // retrigger
            if ((data and 0x0f) != 0) ch.retrig = (ch.retrig and 0xf0) + (data and 0x0f)
            if ((data and 0xf0) != 0) ch.retrig = (ch.retrig and 0x0f) + (data and 0xf0)

            // retrigger volume table
            when (ch.retrig ushr 4) {
                1 -> ch.vol -= 1
                2 -> ch.vol -= 2
                3 -> ch.vol -= 4
                4 -> ch.vol -= 8
                5 -> ch.vol -= 16
                6 -> { ch.vol *= 2; ch.vol /= 3 }
                7 -> ch.vol /= 2
                9 -> ch.vol += 1
                0x0a -> ch.vol += 2
                0x0b -> ch.vol += 4
                0x0c -> ch.vol += 8
                0x0d -> ch.vol += 16
                0x0e -> { ch.vol *= 3; ch.vol /= 2 }
                0x0f -> ch.vol *= 2
            }
            ch.vol = kotlin.math.min(64, kotlin.math.max(0, ch.vol))
        }

        fun eff_t1_r(ch: XMChannelInfo, data: Int) {
            if ((player.cur_tick % (ch.retrig and 0x0f)) == 0) {
                ch.off = 0.0
            }
        }

        fun eff_unimplemented(ch: XMChannelInfo, data: Int) {}
        fun eff_unimplemented_t0(ch: XMChannelInfo, data: Int) {
            Console.log("unimplemented effect", prettify_effect(ch.effect, data))
        }
    }


    fun createAudioStream(): AudioStream {
        val RATE = 44100
        var ev: AudioEvent? = null
        return object : AudioStream(RATE, 2) {
            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                if (ev == null || ev!!.outputBuffer.size < length) {
                    ev = AudioEvent(
                        RATE, 0.0, AudioBuffer(arrayOf(FloatArray(length), FloatArray(length)))
                    )
                }
                val ev = ev!!
                ev.outputBuffer.size = length
                audio_cb(ev)
                for (nchannel in 0 until 2) {
                    val fchannel = ev.outputBuffer.channels[nchannel]
                    for (n in 0 until length) {
                        out.setFloat(nchannel, n, fchannel[n] * 0.5f)
                    }
                }

                return length
            }

            override suspend fun clone(): AudioStream {
                return createAudioStream()
            }
        }
    }
}
