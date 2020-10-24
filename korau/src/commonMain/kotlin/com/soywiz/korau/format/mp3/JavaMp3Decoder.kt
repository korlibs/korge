package com.soywiz.korau.format.mp3

import com.soywiz.kmem.*
import com.soywiz.korau.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

/*
https://github.com/kevinstadler/JavaMP3
his is a fork of delthas' Java MP3 decoding library, incorporating fixes by josephx86, GlaDOSik as well as myself.
The build from this repository is the basis for the MP3 decoding dependency that is shipped with the Processing Sound library.
Currently supports MPEG-1 Layer I/II/III (that is, most MP1, MP2, and MP3 files)
 */

object JavaMp3Decoder {
    const val L3_NSAMPLES = JavaMp3DecoderTables.L3_NSAMPLES
    internal val INV_SQUARE_2 = JavaMp3DecoderTables.INV_SQUARE_2
    internal val BITRATE_LAYER_I = JavaMp3DecoderTables.BITRATE_LAYER_I
    internal val BITRATE_LAYER_II = JavaMp3DecoderTables.BITRATE_LAYER_II
    internal val BITRATE_LAYER_III = JavaMp3DecoderTables.BITRATE_LAYER_III
    internal val SAMPLING_FREQUENCY = JavaMp3DecoderTables.SAMPLING_FREQUENCY
    internal val SCALEFACTORS = JavaMp3DecoderTables.SCALEFACTORS
    internal val SCALEFACTOR_SIZES_LAYER_III = JavaMp3DecoderTables.SCALEFACTOR_SIZES_LAYER_III
    internal val SCALEFACTOR_BAND_INDICES_LAYER_III = JavaMp3DecoderTables.SCALEFACTOR_BAND_INDICES_LAYER_III
    internal val CS_ALIASING_LAYER_III = JavaMp3DecoderTables.CS_ALIASING_LAYER_III
    internal val CA_ALIASING_LAYER_III = JavaMp3DecoderTables.CA_ALIASING_LAYER_III
    internal val POWTAB_LAYER_III = JavaMp3DecoderTables.POWTAB_LAYER_III
    internal val IS_RATIOS_LAYER_III = JavaMp3DecoderTables.IS_RATIOS_LAYER_III
    internal val IMDCT_WINDOW_LAYER_III = JavaMp3DecoderTables.IMDCT_WINDOW_LAYER_III
    internal val PRE_FRACTOR_LAYER_I = JavaMp3DecoderTables.PRE_FRACTOR_LAYER_I
    internal val NIK_COEFFICIENTS = JavaMp3DecoderTables.NIK_COEFFICIENTS
    internal val SYNTH_WINDOW_TABLE_LAYER_III = JavaMp3DecoderTables.SYNTH_WINDOW_TABLE_LAYER_III
    internal val DI_COEFFICIENTS = JavaMp3DecoderTables.DI_COEFFICIENTS
    internal val SHIFT_ENDIANESS = JavaMp3DecoderTables.SHIFT_ENDIANESS
    internal val SB_LIMIT = JavaMp3DecoderTables.SB_LIMIT
    internal val NBAL = JavaMp3DecoderTables.NBAL
    internal val QUANTIZATION_INDEX_LAYER_II = JavaMp3DecoderTables.QUANTIZATION_INDEX_LAYER_II
    internal val NLEVELS = JavaMp3DecoderTables.NLEVELS
    internal val C_LAYER_II = JavaMp3DecoderTables.C_LAYER_II
    internal val D_LAYER_II = JavaMp3DecoderTables.D_LAYER_II
    internal val GROUPING_LAYER_II = JavaMp3DecoderTables.GROUPING_LAYER_II
    internal val BITS_LAYER_II = JavaMp3DecoderTables.BITS_LAYER_II
    internal val HUFFMAN_TABLE_LAYER_III = JavaMp3DecoderTables.HUFFMAN_TABLE_LAYER_III
    internal val HUFFMAN_TABLE_OFFSET_LAYER_III = JavaMp3DecoderTables.HUFFMAN_TABLE_OFFSET_LAYER_III
    internal val HUFFMAN_TREELEN_LAYER_III = JavaMp3DecoderTables.HUFFMAN_TREELEN_LAYER_III
    internal val HUFFMAN_LINBITS_LAYER_III = JavaMp3DecoderTables.HUFFMAN_LINBITS_LAYER_III
    internal val REQUANTIZE_LONG_PRETAB_LAYER_III = JavaMp3DecoderTables.REQUANTIZE_LONG_PRETAB_LAYER_III
    internal val COS_12_LAYER_III = JavaMp3DecoderTables.COS_12_LAYER_III
    internal val COS_36_LAYER_III = JavaMp3DecoderTables.COS_36_LAYER_III

    fun init(inp: ByteArray): SoundData? = init(inp.openSync())

    fun SyncStream.readSyncSafeS28(): Int {
        val v0 = readU8()
        val v1 = readU8()
        val v2 = readU8()
        val v3 = readU8()
        return (v3 and 0x7F) or ((v2 and 0x7F) shl 7) or ((v1 and 0x7F) shl 14) or ((v0 and 0x7F) shl 21)
    }

    fun init(inp: SyncStream): SoundData? {
        val oldPos = inp.position
        // Skips ID3v2
        if (inp.readStringz(3, Charsets.LATIN1) == "ID3") {
            val major = inp.readU8()
            val revision = inp.readU8()
            val flags = inp.readU8()
            val size = inp.readSyncSafeS28()
            inp.position += size
            //println("SIZE: $size")
        } else {
            inp.position = oldPos
        }

        val buffer = Buffer(inp)
        while (buffer.lastByte != -1) {
            val soundData = SoundData(buffer)
            if (decodeFrame(soundData) == DecodeStatus.OK) {
                // require directly adjacent second frame (actually allow up to two bytes
                // away because of some quirks with Layer III decoding)
                val adjacentHeader: FrameHeader? = findNextHeader(soundData, 1)
                if (adjacentHeader != null) {
                    adjacentHeader.unRead(soundData)
                    return soundData
                }
            }
        }
        return null
    }

    internal fun findNextHeader(soundData: SoundData): FrameHeader? {
        return findNextHeader(soundData, Int.MAX_VALUE)
    }

    internal fun findNextHeader(soundData: SoundData, maxBytesSkipped: Int): FrameHeader? {
        // read header
        //try {
            val header = FrameHeader(soundData)
            var skipped = 0
            while (!header.isValid) {
                if (soundData.buffer.lastByte == -1 || skipped >= maxBytesSkipped) {
                    return null
                }
                skipped++
                soundData.buffer.reset()
                // skip to next byte
                soundData.buffer.lastByte = soundData.buffer.inp.read()
                if (soundData.buffer.lastByte == -1) error("EOF")
                header.set(soundData)
            }
            return header
        //} catch (e: Throwable) {
        //    // read error or EOF
        //    return null
        //}
    }

    enum class DecodeStatus { OK, ERROR, COMPLETED }

    fun decodeFrame(soundData: SoundData): DecodeStatus {
        if (soundData.buffer.lastByte == -1) return DecodeStatus.COMPLETED
        val header: FrameHeader = findNextHeader(soundData) ?: return DecodeStatus.COMPLETED

        //if (header.bitrateIndex == 0) {
        //  System.err.println("MP3 decoder warning: files with free bitrate not supported");
        //}
        if (soundData.frequency == -1) {
            soundData.frequency = SAMPLING_FREQUENCY[header.samplingFrequency]
        }
        if (soundData.stereo == -1) {
            /* single_channel */
            soundData.stereo = if (header.mode == 3) 0 else 1
            if (header.layer == 1 /* layer III */) {
                //soundData.mainData = ByteArray(header.nchannels * 1024)
                //soundData.store = FloatArray(header.nchannels * 32 * 18)
                //soundData.v = FloatArray(header.nchannels * 1024)
                //soundData.mainDataReader = MainDataReader(soundData.mainData)
            } else {
                //soundData.synthOffset = IntArray(header.nchannels) { 64 }
                //soundData.synthBuffer = FloatArray(header.nchannels * 1024)
            }
        }
        val bound: Int =
            if (header.modeExtension == 0) 4 else if (header.modeExtension == 1) 8 else if (header.modeExtension == 2) 12 else if (header.modeExtension == 3) 16 else -1
        if (header.protectionBit == 0) {
            // TODO CRC CHECK
            read(soundData.buffer, 16)
        }
        if (header.layer == 3 /* layer I */) {
            val sampleDecoded = when (header.mode) {
                3 /* single_channel */ -> samples_I(soundData.buffer, 1, -1, soundData.s1d)
                0 /* stereo */, 2 /* dual_channel */ -> samples_I(soundData.buffer, 2, -1, soundData.s1d)
                1 /* intensity_stereo */ -> samples_I(soundData.buffer, 2, bound, soundData.s1d)
                else -> null
            } ?: return DecodeStatus.ERROR

            synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, if (header.mode == 3 /* single_channel */) 1 else 2)
        } else if (header.layer == 2 /* layer II */) {
            val bitrate: Int = BITRATE_LAYER_II[header.bitrateIndex]
            val sampleDecoded: FloatArray = /* single_channel */when (header.mode) {
                3 /* single_channel */ -> samples_II(soundData.buffer, 1, -1, bitrate, soundData.frequency, soundData.s2d)
                0 /* stereo */, 2 /* dual_channel */ -> samples_II(soundData.buffer, 2, -1, bitrate, soundData.frequency, soundData.s2d)
                1 /* intensity_stereo */ -> samples_II(soundData.buffer, 2, bound, bitrate, soundData.frequency, soundData.s2d)
                else -> error("Invalid header.mode")
            }
            synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, if (header.mode == 3 /* single_channel */) 1 else 2)
        } else if (header.layer == 1 /* layer III */) {
            val frameSize: Int = (144 * BITRATE_LAYER_III[header.bitrateIndex]) / SAMPLING_FREQUENCY[header.samplingFrequency] + header.paddingBit
            if (frameSize > 2000) {
                println("Frame too large! $frameSize")
            }
            try {
                samples_III(
                    soundData.buffer,
                    if (soundData.stereo == 1) 2 else 1,
                    soundData.mainDataReader,
                    frameSize,
                    header.samplingFrequency,
                    header.mode,
                    header.modeExtension,
                    soundData.store,
                    soundData.v,
                    soundData
                )
            } catch (e: IndexOutOfBoundsException) {
                // @TODO: This shouldn't be necessary
                e.printStackTrace()
            }
        }
        if (soundData.buffer.current != 0) {
            read(soundData.buffer, 8 - soundData.buffer.current)
        }
        return DecodeStatus.OK
    }

    internal fun samples_III(
        buffer: Buffer,
        stereo: Int,
        mainDataReader: MainDataReader,
        frameSize: Int,
        samplingFrequency: Int,
        mode: Int,
        modeExtension: Int,
        store: FloatArray,
        v: FloatArray,
        soundData: SoundData
    ) {
        val s3d = soundData.s3d
        s3d.reset()
        val scfsi = s3d.scfsi
        val part2_3_length = s3d.part2_3_length
        val big_values = s3d.big_values
        val global_gain = s3d.global_gain
        val scalefac_compress = s3d.scalefac_compress
        val win_switch_flag = s3d.win_switch_flag
        val block_type = s3d.block_type
        val mixed_block_flag = s3d.mixed_block_flag
        val table_select = s3d.table_select
        val subblock_gain = s3d.subblock_gain
        val region0_count = s3d.region0_count
        val region1_count = s3d.region1_count
        val preflag = s3d.preflag
        val scalefac_scale = s3d.scalefac_scale
        val count1table_select = s3d.count1table_select
        val count1 = s3d.count1
        val scalefac_l = s3d.scalefac_l
        val scalefac_s = s3d.scalefac_s
        val iss = s3d.`is`

        val mainDataBegin: Int = read(buffer, 9)
        read(buffer, if (stereo == 1) 5 else 3)
        for (ch in 0 until stereo) {
            for (scaleband in 0..3) {
                scfsi[ch * 4 + scaleband] = read(buffer, 1)
            }
        }
        for (gr in 0..1) {
            for (ch in 0 until stereo) {
                part2_3_length[ch * 2 + gr] = read(buffer, 12)
                big_values[ch * 2 + gr] = read(buffer, 9)
                global_gain[ch * 2 + gr] = read(buffer, 8)
                scalefac_compress[ch * 2 + gr] = read(buffer, 4)
                win_switch_flag[ch * 2 + gr] = read(buffer, 1)
                if (win_switch_flag[ch * 2 + gr] == 1) {
                    block_type[ch * 2 + gr] = read(buffer, 2)
                    mixed_block_flag[ch * 2 + gr] = read(buffer, 1)
                    for (region in 0..1) {
                        table_select[(ch * 2 * 3) + (gr * 3) + region] = read(buffer, 5)
                    }
                    for (window in 0..2) {
                        subblock_gain[(ch * 2 * 3) + (gr * 3) + window] = read(buffer, 3)
                    }
                    if ((block_type[ch * 2 + gr] == 2) && (mixed_block_flag[ch * 2 + gr] == 0)) {
                        region0_count[ch * 2 + gr] = 8
                    } else {
                        region0_count[ch * 2 + gr] = 7
                    }
                    region1_count[ch * 2 + gr] = 20 - region0_count[ch * 2 + gr]
                } else {
                    for (region in 0..2) {
                        table_select[(ch * 2 * 3) + (gr * 3) + region] = read(buffer, 5)
                    }
                    region0_count[ch * 2 + gr] = read(buffer, 4)
                    region1_count[ch * 2 + gr] = read(buffer, 3)
                    block_type[ch * 2 + gr] = 0
                }
                preflag[ch * 2 + gr] = read(buffer, 1)
                scalefac_scale[ch * 2 + gr] = read(buffer, 1)
                count1table_select[ch * 2 + gr] = read(buffer, 1)
            }
        }
        arraycopy(mainDataReader.array, mainDataReader.top - mainDataBegin, mainDataReader.array, 0, mainDataBegin)
        val mainDataSize: Int = frameSize - (if (stereo == 2) 32 else 17) - 4
        readInto(buffer, mainDataReader.array, mainDataBegin, mainDataSize)
        mainDataReader.index = 0
        mainDataReader.current = 0
        mainDataReader.top = mainDataBegin + mainDataSize
        for (gr in 0..1) {
            for (ch in 0 until stereo) {
                val rsample = (ch * 2 * 576) + (gr * 576)

                val part_2_start: Int = mainDataReader.index * 8 + mainDataReader.current

                /* Number of bits in the bitstream for the bands */
                val slen1: Int = SCALEFACTOR_SIZES_LAYER_III[scalefac_compress[ch * 2 + gr] * 2]
                val slen2: Int = SCALEFACTOR_SIZES_LAYER_III[scalefac_compress[ch * 2 + gr] * 2 + 1]
                if ((win_switch_flag[ch * 2 + gr] != 0) && (block_type[ch * 2 + gr] == 2)) {
                    if (mixed_block_flag[ch * 2 + gr] != 0) {
                        for (sfb in 0..7) {
                            scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] = read(mainDataReader, slen1)
                        }
                        for (sfb in 3..11) {
                            /* slen1 is for bands 3-5, slen2 for 6-11 */
                            val nbits = if (sfb < 6) slen1 else slen2
                            for (win in 0..2) scalefac_s[(ch * 2 * 12 * 3) + (gr * 12 * 3) + (sfb * 3) + win] = read(mainDataReader, nbits)
                        }
                    } else {
                        for (sfb in 0..11) {
                            /* slen1 is for bands 3-5, slen2 for 6-11 */
                            val nbits = if (sfb < 6) slen1 else slen2
                            for (win in 0..2) scalefac_s[(ch * 2 * 12 * 3) + (gr * 12 * 3) + (sfb * 3) + win] = read(mainDataReader, nbits)
                        }
                    }
                } else { /* block_type == 0 if winswitch == 0 */

                    /* Scale factor bands 0-5 */
                    if ((scfsi[ch * 4 + 0] == 0) || (gr == 0)) {
                        for (sfb in 0..5) {
                            scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] = read(mainDataReader, slen1)
                        }
                    } else if ((scfsi[ch * 4 + 0] == 1) && (gr == 1)) {
                        /* Copy scalefactors from granule 0 to granule 1 */
                        for (sfb in 0..5) {
                            scalefac_l[(ch * 2 * 21) + (1 * 21) + sfb] = scalefac_l[(ch * 2 * 21) + (0 * 21) + sfb]
                        }
                    }

                    /* Scale factor bands 6-10 */
                    if ((scfsi[ch * 4 + 1] == 0) || (gr == 0)) {
                        for (sfb in 6..10) {
                            scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] = read(mainDataReader, slen1)
                        }
                    } else if ((scfsi[ch * 4 + 1] == 1) && (gr == 1)) {
                        /* Copy scalefactors from granule 0 to granule 1 */
                        for (sfb in 6..10) {
                            scalefac_l[(ch * 2 * 21) + (1 * 21) + sfb] = scalefac_l[(ch * 2 * 21) + (0 * 21) + sfb]
                        }
                    }

                    /* Scale factor bands 11-15 */
                    if ((scfsi[ch * 4 + 2] == 0) || (gr == 0)) {
                        for (sfb in 11..15) {
                            scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] = read(mainDataReader, slen2)
                        }
                    } else if ((scfsi[ch * 4 + 2] == 1) && (gr == 1)) {
                        /* Copy scalefactors from granule 0 to granule 1 */
                        for (sfb in 11..15) {
                            scalefac_l[(ch * 2 * 21) + (1 * 21) + sfb] = scalefac_l[(ch * 2 * 21) + (0 * 21) + sfb]
                        }
                    }

                    /* Scale factor bands 16-20 */
                    if ((scfsi[ch * 4 + 3] == 0) || (gr == 0)) {
                        for (sfb in 16..20) {
                            scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] = read(mainDataReader, slen2)
                        }
                    } else if ((scfsi[ch * 4 + 3] == 1) && (gr == 1)) {
                        /* Copy scalefactors from granule 0 to granule 1 */
                        for (sfb in 16..20) {
                            scalefac_l[(ch * 2 * 21) + (1 * 21) + sfb] = scalefac_l[(ch * 2 * 21) + (0 * 21) + sfb]
                        }
                    }
                }

                // Check that there is any data to decode. If not, zero the array.
                if (part2_3_length[ch * 2 + gr] != 0) {

                    // Calculate bit_pos_end which is the index of the last bit for this part.
                    val bit_pos_end: Int = part_2_start + part2_3_length[ch * 2 + gr] - 1
                    var region_1_start: Int
                    var region_2_start: Int
                    var table_num: Int
                    var is_pos: Int
                    val huffman: IntArray = s3d.huffmanTemp

                    // Determine region boundaries
                    if ((win_switch_flag[ch * 2 + gr] == 1) && (block_type[ch * 2 + gr] == 2)) {
                        region_1_start = 36 /* sfb[9/3]*3=36 */
                        region_2_start = L3_NSAMPLES /* No Region2 for short block case. */
                    } else {
                        region_1_start = SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + region0_count[ch * 2 + gr] + 1]
                        region_2_start = SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + region0_count[ch * 2 + gr] + region1_count[ch * 2 + gr] + 2]
                    }

                    /* Read big_values using tables according to region_x_start */is_pos = 0
                    while (is_pos < big_values[ch * 2 + gr] * 2) {
                        table_num = when {
                            is_pos < region_1_start -> table_select[(ch * 2 * 3) + (gr * 3) + 0]
                            is_pos < region_2_start -> table_select[(ch * 2 * 3) + (gr * 3) + 1]
                            else -> table_select[(ch * 2 * 3) + (gr * 3) + 2]
                        }

                        // Get next Huffman coded words
                        huffman_III(mainDataReader, table_num, huffman)

                        // In the big_values area there are two freq lines per Huffman word
                        if (is_pos >= L3_NSAMPLES) break
                        iss[rsample + is_pos++] = huffman[0].toFloat()
                        if (is_pos >= L3_NSAMPLES) break
                        iss[rsample + is_pos++] = huffman[1].toFloat()
                    }

                    /* Read small values until is_pos = 576 or we run out of huffman data */table_num = count1table_select[ch * 2 + gr] + 32
                    is_pos = big_values[ch * 2 + gr] * 2
                    while ((is_pos <= L3_NSAMPLES - 4) && (mainDataReader.index * 8 + mainDataReader.current <= bit_pos_end)) {

                        // Get next Huffman coded words
                        huffman_III(mainDataReader, table_num, huffman)
                        iss[rsample + is_pos++] = huffman[2].toFloat()
                        if (is_pos >= L3_NSAMPLES) break
                        iss[rsample + is_pos++] = huffman[3].toFloat()
                        if (is_pos >= L3_NSAMPLES) break
                        iss[rsample + is_pos++] = huffman[0].toFloat()
                        if (is_pos >= L3_NSAMPLES) break
                        iss[rsample + is_pos] = huffman[1].toFloat()
                        is_pos++
                    }

                    // Check that we didn't read past the end of this section
                    if (mainDataReader.index * 8 + mainDataReader.current > (bit_pos_end + 1)) {
                        // Remove last words read
                        is_pos -= 4
                    }

                    /* Setup count1 which is the index of the first sample in the rzero reg. */
                    count1[ch * 2 + gr] = is_pos

                    /* Zero out the last part if necessary */
                    /* is_pos comes from last for-loop */
                    while (is_pos < L3_NSAMPLES) {
                        iss[rsample + is_pos++] = 0.0f
                    }

                    /* Set the bitpos to point to the next part to read */mainDataReader.index = (bit_pos_end + 1) / 8
                    mainDataReader.current = (bit_pos_end + 1) % 8
                }
            } /* end for (gr... */
        }

        val samplesBuffer = soundData.getSamplesBuffer(18 * 32 * 2 * stereo * 2)

        for (gr in 0..1) {
            for (ch in 0 until stereo) {

                // requantize ===================================================

                /* Determine type of block to process */
                if ((win_switch_flag[ch * 2 + gr] == 1) &&
                    (block_type[ch * 2 + gr] == 2)
                ) { /* Short blocks */

                    // Check if the first two subbands * (=2*18 samples = 8 long or 3 short sfb's) uses long blocks
                    if (mixed_block_flag[ch * 2 + gr] != 0) { /* 2 longbl. sb  first */

                        // First process the 2 long block subbands at the start
                        var sfb: Int = 0
                        var next_sfb: Int =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb + 1]
                        for (i in 0..35) {
                            if (i == next_sfb) {
                                sfb++
                                next_sfb =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb + 1]
                            } /* end if */
                            requantize_long_III(gr, ch, scalefac_scale, preflag, global_gain, scalefac_l, iss, i, sfb)
                        }

                        /** And next the remaining, non-zero, bands which uses short blocks*/
                        sfb = 3
                        next_sfb =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                        var win_len: Int =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                        var i: Int = 36
                        while (i < count1[ch * 2 + gr] /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */
                            if (i == next_sfb) {
                                /* Yes */
                                sfb++
                                next_sfb =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                                win_len =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                        SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                            } /* end if (next_sfb) */
                            for (win in 0..2) {
                                for (j in 0 until win_len) {
                                    requantize_short_III(
                                        gr,
                                        ch,
                                        scalefac_scale,
                                        subblock_gain,
                                        global_gain,
                                        scalefac_s,
                                        iss,
                                        i,
                                        sfb,
                                        win
                                    )
                                    i++
                                } /* end for (win... */
                            } /* end for (j... */
                        }
                    } else {            /* Only short blocks */
                        var sfb: Int = 0
                        var next_sfb: Int =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                        var win_len: Int =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                        var i: Int = 0
                        while (i < count1[ch * 2 + gr] /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */if (i == next_sfb) {    /* Yes */
                                sfb++
                                next_sfb =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                                win_len =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                        SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                            } /* end if (next_sfb) */
                            for (win in 0..2) {
                                for (j in 0 until win_len) {
                                    requantize_short_III(
                                        gr,
                                        ch,
                                        scalefac_scale,
                                        subblock_gain,
                                        global_gain,
                                        scalefac_s,
                                        iss,
                                        i,
                                        sfb,
                                        win
                                    )
                                    i++
                                } /* end for (win... */
                            } /* end for (j... */
                        }
                    } /* end else (only short blocks) */
                } else {            /* Only long blocks */
                    var sfb: Int = 0
                    var next_sfb: Int =
                        SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb + 1]
                    for (i in 0 until count1[ch * 2 + gr]) {
                        if (i == next_sfb) {
                            sfb++
                            next_sfb =
                                SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb + 1]
                        } /* end if */
                        requantize_long_III(gr, ch, scalefac_scale, preflag, global_gain, scalefac_l, iss, i, sfb)
                    }
                } /* end else (only long blocks) */

                // reorder ================================================
                outer@ while (true) {

                    /* Only reorder short blocks */
                    if ((win_switch_flag[ch * 2 + gr] == 1) &&
                        (block_type[ch * 2 + gr] == 2)
                    ) { /* Short blocks */
                        val re: FloatArray = s3d.tempFloatL3NSamples
                        var i: Int = 0
                        var sfb: Int = 0
                        var next_sfb: Int
                        var win_len: Int

                        /* Check if the first two subbands* (=2*18 samples = 8 long or 3 short sfb's) uses long blocks */
                        if (mixed_block_flag[ch * 2 + gr] != 0) { /* 2 longbl. sb  first */
                            sfb = 3
                            i = 36
                        }
                        next_sfb =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                        win_len =
                            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                        while (i < L3_NSAMPLES /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */if (i == next_sfb) {    /* Yes */

                                /* Copy reordered data back to the original vector */
                                for (j in 0 until 3 * win_len) {
                                    iss[
                                        (ch * 2 * 576) + (gr * 576) + (3 * (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb])) + j
                                    ] = re[j]
                                }

                                /* Check if this band is above the rzero region, if so we're done */if (i >= count1[ch * 2 + gr]) {
                                    /* Done */
                                    break@outer
                                }
                                sfb++
                                next_sfb =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] * 3
                                win_len =
                                    SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] -
                                        SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]
                            } /* end if (next_sfb) */

                            /* Do the actual reordering */
                            for (win in 0..2) {
                                for (j in 0 until win_len) {
                                    re[j * 3 + win] = iss[(ch * 2 * 576) + (gr * 576) + i]
                                    i++
                                } /* end for (j... */
                            } /* end for (win... */
                        }

                        /* Copy reordered data of the last band back to the original vector */
                        for (j in 0 until 3 * win_len) {
                            iss[(ch * 2 * 576) + (gr * 576) + (3 * (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + 12])) + j] = re[j]
                        }
                    }
                    break
                }
            }
            // stereo ==============================================

            // Do nothing if joint stereo is not enabled
            if ((mode == 1) && (modeExtension != 0)) {

                // Do Middle/Side ("normal") stereo processing
                if ((modeExtension and 0x2) != 0) {
                    // Determine how many frequency lines to transform
                    val max_pos = (if (count1[0 * 2 + gr] > count1[1 * 2 + gr]) count1[0 * 2 + gr] else count1[1 * 2 + gr])

                    // Do the actual processing
                    for (i in 0 until max_pos) {
                        val left: Float = ((iss[(0 * 2 * 576) + (gr * 576) + i] + iss[(1 * 2 * 576) + (gr * 576) + i]) * (INV_SQUARE_2))
                        val right: Float = ((iss[(0 * 2 * 576) + (gr * 576) + i] - iss[(1 * 2 * 576) + (gr * 576) + i]) * (INV_SQUARE_2))
                        iss[(0 * 2 * 576) + (gr * 576) + i] = left
                        iss[(1 * 2 * 576) + (gr * 576) + i] = right
                    } // end for (i...
                } // end if (ms_stereo...

                /* Do intensity stereo processing */
                if ((modeExtension and 0x1) != 0) {

                    /* The first band that is intensity stereo encoded is the first band
                     * scale factor band on or above the count1 frequency line.
                     * N.B.: Intensity stereo coding is only done for the higher subbands,
                     * but the logic is still included to process lower subbands.
                     */

                    /* Determine type of block to process */
                    if ((win_switch_flag[0 * 2 + gr] == 1) && (block_type[0 * 2 + gr] == 2)) { /* Short blocks */

                        /* Check if the first two subbands* (=2*18 samples = 8 long or 3 short sfb's) uses long blocks */
                        if (mixed_block_flag[0 * 2 + gr] != 0) { /* 2 longbl. sb  first */

                            /** First process the 8 sfb's at the start*/
                            for (sfb in 0..7) {

                                /* Is this scale factor band above count1 for the right channel? */
                                if (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb] >= count1[1 * 2 + gr]) {
                                    stereo_long_III(iss, scalefac_l, gr, sfb, samplingFrequency)
                                }
                            } /* end if (sfb... */

                            /** And next the remaining bands which uses short blocks*/
                            for (sfb in 3..11) {
                                // Is this scale factor band above count1 for the right channel?
                                if (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb] * 3 >= count1[1 * 2 + gr]) {
                                    // Perform the intensity stereo processing
                                    stereo_short_III(iss, scalefac_s, gr, sfb, samplingFrequency)
                                }
                            }
                        } else {
                            // Only short blocks
                            for (sfb in 0..11) {

                                /* Is this scale factor band above count1 for the right channel? */
                                if (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb] * 3 >= count1[1 * 2 + gr]) {
                                    /* Perform the intensity stereo processing */
                                    stereo_short_III(iss, scalefac_s, gr, sfb, samplingFrequency)
                                }
                            }
                        } /* end else (only short blocks) */
                    } else {            /* Only long blocks */
                        for (sfb in 0..20) {
                            /* Is this scale factor band above count1 for the right channel? */
                            if (SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb] >= count1[1 * 2 + gr]) {

                                /* Perform the intensity stereo processing */
                                stereo_long_III(iss, scalefac_l, gr, sfb, samplingFrequency)
                            }
                        }
                    } /* end else (only long blocks) */
                } /* end if (intensity_stereo processing) */
            }
            for (ch in 0 until stereo) {

                // antialiasing ==============================================

                /* No antialiasing is done for short blocks */
                if (!(((win_switch_flag[ch * 2 + gr] == 1) && (block_type[ch * 2 + gr] == 2) && ((mixed_block_flag[ch * 2 + gr]) == 0)))) {
                    // Setup the limit for how many subbands to transform
                    val sblim = if (((win_switch_flag[ch * 2 + gr] == 1) && (block_type[ch * 2 + gr] == 2) && ((mixed_block_flag[ch * 2 + gr]) == 1))) 2 else 32

                    /* Do the actual antialiasing */for (sb in 1 until sblim) {
                        for (i in 0..7) {
                            val li: Int = (18 * sb) - 1 - i
                            val ui: Int = 18 * sb + i
                            val lb: Float = iss[(ch * 2 * 576) + (gr * 576) + li] * CS_ALIASING_LAYER_III[i] - iss[(ch * 2 * 576) + (gr * 576) + ui] * CA_ALIASING_LAYER_III[i]
                            val ub: Float = iss[(ch * 2 * 576) + (gr * 576) + ui] * CS_ALIASING_LAYER_III[i] + iss[(ch * 2 * 576) + (gr * 576) + li] * CA_ALIASING_LAYER_III[i]
                            iss[(ch * 2 * 576) + (gr * 576) + li] = lb
                            iss[(ch * 2 * 576) + (gr * 576) + ui] = ub
                        }
                    }
                }
                // hybrid synthesis ===========================================

                // Loop through all 32 subbands
                for (sb in 0..31) {
                    // Determine blocktype for this subband
                    // Long blocks in first 2 subbands
                    val bt = (if (((win_switch_flag[ch * 2 + gr] == 1) && (mixed_block_flag[ch * 2 + gr] == 1) && (sb < 2))) 0 else block_type[ch * 2 + gr])
                    val rawout: FloatArray = s3d.tempFloatArray36

                    // ----
                    /* Do the inverse modified DCT and windowing */
                    // MPG_IMDCT_Win(& (is[ch * 2 + gr][sb * 18]), rawout, bt);
                    val offset: Int = (ch * 2 * 576) + (gr * 576) + (sb * 18)
                    if (bt == 2) {
                        for (j in 0..2) {
                            for (p in 0..11) {
                                var sum: Float = 0f
                                for (m in 0..5) {
                                    sum += iss[offset + j + (3 * m)] * COS_12_LAYER_III[m * 12 + p]
                                }
                                rawout[(6 * j) + p + 6] += sum * IMDCT_WINDOW_LAYER_III[bt * 36 + p]
                            }
                        }
                    } else {
                        for (p in 0..35) {
                            var sum: Float = 0f
                            for (m in 0..17) {
                                sum += iss[offset + m] * COS_36_LAYER_III[m * 36 + p]
                            }
                            rawout[p] = sum * IMDCT_WINDOW_LAYER_III[bt * 36 + p]
                        }
                    }

                    /* Overlapp add with stored vector into main_data vector */
                    for (i in 0..17) {
                        iss[(ch * 2 * 576) + (gr * 576) + (sb * 18) + i] = rawout[i] + store[(ch * 32 * 18) + (sb * 18) + i]
                        store[(ch * 32 * 18) + (sb * 18) + i] = rawout[i + 18]
                    } /* end for (i... */
                } /* end for (sb... */

                // frequency inversion ================================================
                var sb = 1
                while (sb < 32) {
                    var i = 1
                    while (i < 18) {
                        iss[(ch * 2 * 576) + (gr * 576) + (sb * 18) + i] = -iss[(ch * 2 * 576) + (gr * 576) + (sb * 18) + i]
                        i += 2
                    }
                    sb += 2
                }

                // polyphase subband synthesis
                val u = s3d.tempFloatArray512
                val s = s3d.tempFloatArray32

                /* Loop through the 18 samples in each of the 32 subbands */
                for (ss in 0..17) {
                    for (i in 1023 downTo 64)  /* Shift up the V vector */ {
                        v[ch * 1024 + i] = v[ch * 1024 + i - 64]
                    }

                    /* Copy the next 32 time samples to a temp vector */
                    for (i in 0..31) {
                        s[i] = iss[(ch * 2 * 576) + (gr * 576) + (i * 18) + ss]
                    }
                    for (i in 0..63) { /* Matrix multiply input with n_win[][] matrix */
                        var sum: Float = 0.0f
                        for (j in 0..31) {
                            sum += SYNTH_WINDOW_TABLE_LAYER_III[i * 32 + j] * s[j]
                        }
                        v[ch * 1024 + i] = sum
                    } /* end for(i... */

                    /* Build the U vector */
                    for (i in 0..7) {
                        for (j in 0..31) {
                            u[i * 64 + j] = v[(ch * 1024) + (i * 128) + j]
                            u[(i * 64) + j + 32] = v[(ch * 1024) + (i * 128) + j + 96]
                        }
                    } /* end for (i... */

                    /* Window by u_vec[i] with g_synth_dtbl[i] */
                    for (i in 0..511) u[i] *= DI_COEFFICIENTS[i]

                    /* Calculate 32 samples and store them in the outdata vector */for (i in 0..31) {
                        var sum: Float = 0.0f
                        for (j in 0..15) {
                            sum += u[j * 32 + i]
                        }

                        /* sum now contains time sample 32*ss+i. Convert to 16-bit signed int */
                        var samp: Int = (sum * 32767.0f).toInt()
                        if (samp > 32767) {
                            samp = 32767
                        } else if (samp < -32767) {
                            samp = -32767
                        }
                        samp = samp and 0xffff
                        if (stereo > 1) {
                            samplesBuffer[(gr * 18 * 32 * 2 * 2) + (ss * 32 * 2 * 2) + (i * 2 * 2) + (ch * 2)] = samp.toByte()
                            samplesBuffer[(gr * 18 * 32 * 2 * 2) + (ss * 32 * 2 * 2) + (i * 2 * 2) + (ch * 2) + 1] = (samp ushr 8).toByte()
                        } else {
                            samplesBuffer[(gr * 18 * 32 * 2) + (ss * 32 * 2) + (i * 2)] = samp.toByte()
                            samplesBuffer[(gr * 18 * 32 * 2) + (ss * 32 * 2) + (i * 2) + 1] = (samp ushr 8).toByte()
                        }
                    } /* end for (i... */
                } /* end for (ss... */
            }
        }
    }

    internal fun stereo_short_III(`is`: FloatArray, scalefac_s: IntArray, gr: Int, sfb: Int, samplingFrequency: Int) {
        // The window length
        val win_len: Int =
            SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb + 1] - SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb]

        // The three windows within the band has different scalefactors
        for (win in 0..2) {
            var is_pos: Int

            // Check that ((is_pos[sfb]=scalefac) != 7) => no intensity stereo
            if ((scalefac_s[(0 * 2 * 12 * 3) + (gr * 12 * 3) + (sfb * 3) + win].also { is_pos = it }) != 7) {
                val sfb_start: Int = SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 23 + sfb] * 3 + win_len * win
                val sfb_stop: Int = sfb_start + win_len
                // tan((6*PI)/12 = PI/2) needs special treatment!
                val is_ratio_l = if (is_pos == 6) 1f else IS_RATIOS_LAYER_III[is_pos] / (1.0f + IS_RATIOS_LAYER_III[is_pos])
                val is_ratio_r = if (is_pos == 6) 0f else 1.0f / (1.0f + IS_RATIOS_LAYER_III[is_pos])

                // Now decode all samples in this scale factor band
                for (i in sfb_start until sfb_stop) {
                    `is`[(0 * 2 * 576) + (gr * 576) + i] *= is_ratio_l
                    `is`[(1 * 2 * 576) + (gr * 576) + i] *= is_ratio_r
                }
            } /* end if (not illegal is_pos) */
        } /* end for (win... */
    }

    internal fun stereo_long_III(`is`: FloatArray, scalefac_l: IntArray, gr: Int, sfb: Int, samplingFrequency: Int) {
        var is_pos: Int
        /* Check that ((is_pos[sfb]=scalefac) != 7) => no intensity stereo */if ((scalefac_l[(0 * 2 * 21) + (gr * 21) + sfb]
                .also { is_pos = it }) != 7
        ) {
            val sfb_start: Int = SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb]
            val sfb_stop: Int = SCALEFACTOR_BAND_INDICES_LAYER_III[(samplingFrequency * (23 + 14)) + 0 + sfb + 1]
            val is_ratio_l: Float
            val is_ratio_r: Float

            /* tan((6*PI)/12 = PI/2) needs special treatment! */if (is_pos == 6) {
                is_ratio_l = 1.0f
                is_ratio_r = 0.0f
            } else {
                is_ratio_l =
                    IS_RATIOS_LAYER_III[is_pos] / (1.0f + IS_RATIOS_LAYER_III[is_pos])
                is_ratio_r = 1.0f / (1.0f + IS_RATIOS_LAYER_III[is_pos])
            }

            /* Now decode all samples in this scale factor band */for (i in sfb_start until sfb_stop) {
                `is`[(0 * 2 * 576) + (gr * 576) + i] *= is_ratio_l
                `is`[(1 * 2 * 576) + (gr * 576) + i] *= is_ratio_r
            }
        }
    }

    internal fun requantize_short_III(
        gr: Int,
        ch: Int,
        scalefac_scale: IntArray,
        subblock_gain: IntArray,
        global_gain: IntArray,
        scalefac_s: IntArray,
        `is`: FloatArray,
        is_pos: Int,
        sfb: Int,
        win: Int
    ) {
        val sf_mult: Float = if (scalefac_scale[ch * 2 + gr] != 0) 1.0f else 0.5f
        val tmp1 = if (sfb < 12) pow(2.0, -(sf_mult * scalefac_s[(ch * 2 * 12 * 3) + (gr * 12 * 3) + (sfb * 3) + win]).toDouble()).toFloat() else 1.0f
        val tmp2: Float = pow(2.0, 0.25f * (global_gain[ch * 2 + gr] - 210.0f - (8.0f * (subblock_gain[(ch * 2 * 3) + (gr * 3) + win]))).toDouble()).toFloat()
        val tmp3: Float = if (`is`[(ch * 2 * 576) + (gr * 576) + is_pos] < 0.0) -POWTAB_LAYER_III[(-`is`[(ch * 2 * 576) + (gr * 576) + is_pos]).toInt()] else POWTAB_LAYER_III[`is`[(ch * 2 * 576) + (gr * 576) + is_pos].toInt()]
        `is`[(ch * 2 * 576) + (gr * 576) + is_pos] = tmp1 * tmp2 * tmp3
    }

    internal fun requantize_long_III(
        gr: Int,
        ch: Int,
        scalefac_scale: IntArray,
        preflag: IntArray,
        global_gain: IntArray,
        scalefac_l: IntArray,
        `is`: FloatArray,
        is_pos: Int,
        sfb: Int
    ) {
        val sf_mult: Float = if (scalefac_scale[ch * 2 + gr] != 0) 1.0f else 0.5f

        // TODO table cache Math.pow 2 ? faster alternative?
        val tmp1 = if (sfb < 21) {
            val pf_x_pt: Float = preflag[ch * 2 + gr] * REQUANTIZE_LONG_PRETAB_LAYER_III[sfb]
            pow(2.0, -(sf_mult * (scalefac_l[(ch * 2 * 21) + (gr * 21) + sfb] + pf_x_pt)).toDouble()).toFloat()
        } else {
            1.0f
        }
        val tmp2: Float = pow(2.0, 0.25f * (global_gain[ch * 2 + gr] - 210).toDouble()).toFloat()
        val tmp3 = if (`is`[(ch * 2 * 576) + (gr * 576) + is_pos] < 0.0) {
            -POWTAB_LAYER_III[(-`is`[(ch * 2 * 576) + (gr * 576) + is_pos]).toInt()]
        } else {
            POWTAB_LAYER_III[`is`[(ch * 2 * 576) + (gr * 576) + is_pos].toInt()]
        }
        `is`[(ch * 2 * 576) + (gr * 576) + is_pos] = tmp1 * tmp2 * tmp3
    }

    internal fun huffman_III(mainDataReader: MainDataReader, table_num: Int, array: IntArray) {
        /* Table entries are 16 bits each:
        * Bit(s)
        * 15     hit/miss (1/0)
        * 14-13  codeword size (1-4 bits)
        * 7-0    codeword (bits 4-7=x, 0-3=y) if hit
        * 12-0   start offset of next table if miss
        */
        var point: Int = 0
        var currpos: Int

        /* Check for empty tables */
        if (HUFFMAN_TREELEN_LAYER_III[table_num] == 0) {
            array[3] = 0
            array[2] = array[3]
            array[1] = array[2]
            array[0] = array[1]
            return
        }
        val treelen: Int = HUFFMAN_TREELEN_LAYER_III[table_num]
        val linbits: Int = HUFFMAN_LINBITS_LAYER_III[table_num]
        val offset: Int = HUFFMAN_TABLE_OFFSET_LAYER_III[table_num]
        var error: Int = 1
        var bitsleft: Int = 32
        do {   /* Start reading the Huffman code word,bit by bit */
            /* Check if we've matched a code word */
            if ((HUFFMAN_TABLE_LAYER_III[offset + point] and 0xff00) == 0) {
                error = 0
                array[0] = (HUFFMAN_TABLE_LAYER_III[offset + point] shr 4) and 0xf
                array[1] = HUFFMAN_TABLE_LAYER_III[offset + point] and 0xf
                break
            }
            if (read(mainDataReader, 1) != 0) { /* Go right in tree */
                while ((HUFFMAN_TABLE_LAYER_III[offset + point] and 0xff) >= 250) {
                    point += HUFFMAN_TABLE_LAYER_III[offset + point] and 0xff
                }
                point += HUFFMAN_TABLE_LAYER_III[offset + point] and 0xff
            } else { /* Go left in tree */
                while ((HUFFMAN_TABLE_LAYER_III[offset + point] shr 8) >= 250) {
                    point += HUFFMAN_TABLE_LAYER_III[offset + point] shr 8
                }
                point += HUFFMAN_TABLE_LAYER_III[offset + point] shr 8
            }
        } while ((--bitsleft > 0) && (point < treelen))
        if (error != 0) {  /* Check for error. */
            array[1] = 0
            array[0] = array[1]
            throw IllegalStateException(
                ("Illegal Huff code in data. bleft = %d,point = %d. tab = %d." +
                    bitsleft + " " + point + " " + table_num)
            )
        }
        /* Process sign encodings for quadruples tables. */if (table_num > 31) {
            array[2] = (array[1] shr 3) and 1
            array[3] = (array[1] shr 2) and 1
            array[0] = (array[1] shr 1) and 1
            array[1] = (array[1] shr 0) and 1
            if (array[2] > 0) if (read(mainDataReader, 1) == 1) array[2] = -array[2]
            if (array[3] > 0) if (read(mainDataReader, 1) == 1) array[3] = -array[3]
            if (array[0] > 0) if (read(mainDataReader, 1) == 1) array[0] = -array[0]
            if (array[1] > 0) if (read(mainDataReader, 1) == 1) array[1] = -array[1]
        } else {
            /* Get linbits */
            if ((linbits > 0) && (array[0] == 15)) {
                array[0] += read(mainDataReader, linbits)
            }

            /* Get sign bit */
            if (array[0] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[0] = -array[0]
                }
            }

            /* Get linbits */if ((linbits > 0) && (array[1] == 15)) {
                array[1] += read(mainDataReader, linbits)
            }

            /* Get sign bit */if (array[1] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[1] = -array[1]
                }
            }
        }
    }

    internal fun samples_I(buffer: Buffer, stereo: Int, bound: Int, s1d: SamplesIData): FloatArray? {
        var bound: Int = bound
        if (bound < 0) {
            bound = 32
        }

        s1d.reset()
        val allocation = s1d.allocation
        val allocationChannel = s1d.allocationChannel
        val scalefactorChannel = s1d.scalefactorChannel
        val sampleDecoded = s1d.sampleDecoded

        for (sb in 0 until bound) {
            for (ch in 0 until stereo) {
                allocationChannel[ch * bound + sb] = read(buffer, 4)
            }
        }
        for (sb in bound..31) {
            allocation[sb - bound] = read(buffer, 4)
        }
        for (sb in 0 until bound) {
            for (ch in 0 until stereo) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    scalefactorChannel[ch * 32 + sb] = read(buffer, 6)
                }
            }
        }
        for (sb in bound..31) {
            for (ch in 0 until stereo) {
                if (allocation[sb - bound] != 0) {
                    scalefactorChannel[ch * 32 + sb] = read(buffer, 6)
                }
            }
        }
        for (s in 0..11) {
            for (sb in 0 until bound) {
                for (ch in 0 until stereo) {
                    val n: Int = allocationChannel[ch * bound + sb]
                    if (n == 0) {
                        sampleDecoded[(ch * 32 * 12) + (sb * 12) + s] = 0f
                    } else {
                        val read: Int = read(buffer, n + 1)
                        var fraction: Float = 0f
                        if (((read shr n) and 1) == 0) {
                            fraction = -1f
                        }
                        fraction += (read and ((1 shl n) - 1)).toFloat() / (1 shl n) + 1f / (1 shl n)
                        val sfc = scalefactorChannel[ch * 32 + sb]
                        //println("sfc: $sfc, n+1: ${n + 1}")
                        if (n + 1 >= PRE_FRACTOR_LAYER_I.size) return null
                        sampleDecoded[(ch * 32 * 12) + (sb * 12) + s] = SCALEFACTORS[sfc] * PRE_FRACTOR_LAYER_I[n + 1] * fraction
                    }
                }
            }
            for (sb in bound..31) {
                val sbb = sb - bound
                if (sbb < 0 || sbb >= allocationChannel.size) return null
                val n: Int = allocationChannel[sbb]
                if (n == 0) {
                    sampleDecoded[(1 * 32 * 12) + (sb * 12) + s] = 0f
                    sampleDecoded[(0 * 32 * 12) + (sb * 12) + s] = sampleDecoded[(1 * 32 * 12) + (sb * 12) + s]
                } else {
                    val read: Int = read(buffer, n + 1)
                    var fraction: Float = 0f
                    if (((read shr n) and 1) == 0) {
                        fraction = -1f
                    }
                    fraction += (read and ((1 shl n) - 1)).toFloat() / (1 shl n) + 1f / (1 shl n)
                    for (ch in 0..1) {
                        if (n + 1 >= PRE_FRACTOR_LAYER_I.size) return null
                        sampleDecoded[(ch * 32 * 12) + (sb * 12) + s] =
                            SCALEFACTORS[scalefactorChannel[ch * 32 + sb]] * PRE_FRACTOR_LAYER_I[n + 1] * fraction
                    }
                }
            }
        }
        return sampleDecoded
    }

    internal fun samples_II(buffer: Buffer, stereo: Int, bound: Int, bitrate: Int, frequency: Int, s2d: SamplesIIData): FloatArray {
        var bound: Int = bound
        val sbIndex = when {
            frequency != 48000 && (bitrate >= 96000 || bitrate == 0) -> 1
            frequency != 32000 && (bitrate in 1..48000) -> 2
            frequency == 32000 && (bitrate in 1..48000) -> 3
            else -> 0
        }
        val sbLimit: Int = SB_LIMIT[sbIndex]
        if (bound < 0) bound = sbLimit

        s2d.reset()
        val allocation = s2d.allocation
        val allocationChannel = s2d.allocationChannel
        val scfsi = s2d.scfsi
        val scalefactorChannel = s2d.scalefactorChannel
        val sampleDecoded = s2d.sampleDecoded

        for (sb in 0 until bound) for (ch in 0 until stereo) allocationChannel[ch * bound + sb] = read(buffer, NBAL[sbIndex][sb])
        for (sb in bound until sbLimit) allocation[sb - bound] = read(buffer, NBAL[sbIndex][sb])
        for (sb in 0 until bound) {
            for (ch in 0 until stereo) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    scfsi[ch * sbLimit + sb] = read(buffer, 2)
                }
            }
        }
        for (sb in bound until sbLimit) {
            for (ch in 0 until stereo) {
                if (allocation[sb - bound] != 0) {
                    scfsi[ch * sbLimit + sb] = read(buffer, 2)
                }
            }
        }
        for (sb in 0 until bound) {
            for (ch in 0 until stereo) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    val offset: Int = ch * sbLimit * 3 + sb * 3
                    when (scfsi[ch * sbLimit + sb]) {
                        0 -> {
                            scalefactorChannel[offset + 0] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = read(buffer, 6)
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                        }
                        1 -> {
                            scalefactorChannel[offset + 1] = read(buffer, 6)
                            scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1]
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                        }
                        2 -> {
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2]
                            scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1]
                        }
                        3 -> {
                            scalefactorChannel[offset + 0] = read(buffer, 6)
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2]
                        }
                    }
                }
            }
        }
        for (sb in bound until sbLimit) {
            for (ch in 0 until stereo) {
                if (allocation[sb - bound] != 0) {
                    val offset: Int = ch * sbLimit * 3 + sb * 3
                    when (scfsi[ch * sbLimit + sb]) {
                        0 -> {
                            scalefactorChannel[offset + 0] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = read(buffer, 6)
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                        }
                        1 -> {
                            scalefactorChannel[offset + 1] = read(buffer, 6)
                            scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1]
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                        }
                        2 -> {
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2]
                            scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1]
                        }
                        3 -> {
                            scalefactorChannel[offset + 0] = read(buffer, 6)
                            scalefactorChannel[offset + 2] = read(buffer, 6)
                            scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2]
                        }
                    }
                }
            }
        }
        for (gr in 0..11) {
            for (sb in 0 until bound) {
                for (ch in 0 until stereo) {
                    val n: Int = allocationChannel[ch * bound + sb]
                    val offset: Int = (ch * 32 * 12 * 3) + (sb * 12 * 3) + (gr * 3)
                    if (n == 0) {
                        sampleDecoded[offset + 2] = 0f
                        sampleDecoded[offset + 1] = sampleDecoded[offset + 2]
                        sampleDecoded[offset] = sampleDecoded[offset + 1]
                    } else {
                        val index: Int = QUANTIZATION_INDEX_LAYER_II[sbIndex][sb][n - 1]
                        val sampleInt: IntArray = s2d.tempSampleInt
                        val sampleBits: Int = BITS_LAYER_II[index]
                        val nlevels: Int = NLEVELS[index]
                        if (GROUPING_LAYER_II[index]) {
                            var samplecode: Int = read(buffer, sampleBits)
                            sampleInt[0] = samplecode % nlevels
                            samplecode /= nlevels
                            sampleInt[1] = samplecode % nlevels
                            samplecode /= nlevels
                            sampleInt[2] = samplecode % nlevels
                        } else {
                            sampleInt[0] = read(buffer, sampleBits)
                            sampleInt[1] = read(buffer, sampleBits)
                            sampleInt[2] = read(buffer, sampleBits)
                        }
                        var msb: Int = 0
                        while ((1 shl msb) <= nlevels) {
                            msb++
                        }
                        msb--
                        for (i in 0..2) {
                            var sample: Float = 0f
                            if (((sampleInt[i] shr msb) and 1) == 0) {
                                sample = -1f
                            }
                            sample += (sampleInt[i] and ((1 shl msb) - 1)).toFloat() / (1 shl msb)
                            sample += D_LAYER_II[index]
                            sample *= C_LAYER_II[index]
                            sample *= SCALEFACTORS[scalefactorChannel[(ch * sbLimit * 3) + (sb * 3) + (gr / 4)]]
                            sampleDecoded[offset + i] = sample
                        }
                    }
                }
            }
            for (sb in bound until sbLimit) {
                val n: Int = allocation[sb - bound]
                val offset: Int = sb * 12 * 3 + gr * 3
                if (n == 0) {
                    for (ch in 0 until stereo) {
                        sampleDecoded[offset + (ch * 32 * 12 * 3) + 2] = 0f
                        sampleDecoded[offset + (ch * 32 * 12 * 3) + 1] = sampleDecoded[offset + (ch * 32 * 12 * 3) + 2]
                        sampleDecoded[offset + ch * 32 * 12 * 3] = sampleDecoded[offset + (ch * 32 * 12 * 3) + 1]
                    }
                } else {
                    val index: Int = QUANTIZATION_INDEX_LAYER_II[sbIndex][sb][n - 1]
                    val sampleInt: IntArray = s2d.tempSampleInt
                    val sampleBits: Int = BITS_LAYER_II[index]
                    val nlevels: Int = NLEVELS[index]
                    if (GROUPING_LAYER_II[index]) {
                        var samplecode: Int = read(buffer, sampleBits)
                        sampleInt[0] = samplecode % nlevels
                        samplecode /= nlevels
                        sampleInt[1] = samplecode % nlevels
                        samplecode /= nlevels
                        sampleInt[2] = samplecode % nlevels
                    } else {
                        sampleInt[0] = read(buffer, sampleBits)
                        sampleInt[1] = read(buffer, sampleBits)
                        sampleInt[2] = read(buffer, sampleBits)
                    }
                    var msb: Int = 0
                    while ((1 shl msb) <= nlevels) {
                        msb++
                    }
                    msb--
                    for (i in 0..2) {
                        var sample: Float = 0f
                        if (((sampleInt[i] shr msb) and 1) == 0) {
                            sample = -1f
                        }
                        sample += (sampleInt[i] and ((1 shl msb) - 1)).toFloat() / (1 shl msb)
                        sample += D_LAYER_II[index]
                        sample *= C_LAYER_II[index]
                        for (ch in 0 until stereo) {
                            sampleDecoded[offset + (ch * 32 * 12 * 3) + i] =
                                sample * SCALEFACTORS[scalefactorChannel[(ch * sbLimit * 3) + (sb * 3) + (gr / 4)]]
                        }
                    }
                }
            }
        }
        return sampleDecoded
    }

    internal fun synth(soundData: SoundData, samples: FloatArray, synthOffset: IntArray, synthBuffer: FloatArray, stereo: Int) {
        val size: Int = samples.size / stereo / 32
        val pcm: FloatArray = soundData.pcmTemp
        for (ch in 0 until stereo) {
            for (s in 0 until size) {
                synthOffset[ch] = (synthOffset[ch] - 64) and 0x3ff
                for (i in 0..63) {
                    var sum: Float = 0f
                    for (k in 0..31) {
                        sum += NIK_COEFFICIENTS[i * 32 + k] * samples[(ch * 32 * size) + (k * size) + s]
                    }
                    synthBuffer[(ch * 1024) + synthOffset[ch] + i] = sum
                }
                for (j in 0..31) {
                    var sum: Float = 0f
                    for (i in 0..15) {
                        val k: Int = j + (i shl 5)
                        sum += DI_COEFFICIENTS[k] * synthBuffer[ch * 1024 + ((synthOffset[ch] + (k + (((i + 1) shr 1) shl 6))) and 0x3FF)]
                    }
                    pcm[(s * 32 * stereo) + (j * stereo) + ch] = sum
                }
            }
        }
        val samplesBuffer = soundData.getSamplesBuffer(size * 32 * stereo * 2)
        for (i in 0 until size * 32 * stereo) {
            val sample = SampleConvert.floatToShort(pcm[i]).toInt()
            samplesBuffer[i * 2 + 0] = sample.toByte()
            samplesBuffer[i * 2 + 1] = (sample ushr 8).toByte()
        }
    }

    private fun pow(b: Double, e: Double): Double {
        return b.pow(e)
    }

    internal fun read(reader: MainDataReader, bits: Int): Int {
        var bits: Int = bits
        var number: Int = 0
        while (bits > 0) {
            val advance: Int = min(bits, 8 - reader.current)
            bits -= advance
            reader.current += advance
            number = number or ((((reader.array[reader.index].toInt() and 0xFF) ushr (8 - reader.current)) and (0xFF ushr (8 - advance))) shl bits)
            if (reader.current == 8) {
                reader.current = 0
                reader.index++
            }
        }
        return number
    }

    internal fun read(buffer: Buffer, bits: Int): Int {
        var bits: Int = bits
        var number: Int = 0
        while (bits > 0) {
            val advance: Int = min(bits, 8 - buffer.current)
            bits -= advance
            buffer.current += advance
            if (bits != 0 && buffer.lastByte == -1) {
                throw EOFException("Unexpected EOF reached in MPEG data")
            }
            number = number or (((buffer.lastByte ushr (8 - buffer.current)) and (0xFF ushr (8 - advance))) shl bits)
            if (buffer.current == 8) {
                buffer.current = 0
                buffer.lastByte = buffer.inp.read()
            }
        }
        return number
    }

    internal fun readInto(buffer: Buffer, array: ByteArray, offset: Int, length: Int) {
        if (buffer.current != 0) // TODO remove
        {
            throw IllegalStateException("buffer current is " + buffer.current)
        }
        if (length == 0) {
            return
        }
        if (buffer.lastByte == -1) {
            throw EOFException("Unexpected EOF reached in MPEG data")
        }
        array[offset] = buffer.lastByte.toByte()
        var read: Int = 1
        while (read < length) {
            read += buffer.inp.read(array, offset + read, length - read)
        }
        buffer.lastByte = buffer.inp.read()
    }

    internal class  FrameHeader internal constructor(soundData: SoundData) {
        var sigBytes: Int = 0
        var version: Int = 0
        var layer: Int = 0
        var protectionBit: Int = 0
        var bitrateIndex: Int = 0
        var samplingFrequency: Int = 0
        var paddingBit: Int = 0
        var privateBit: Int = 0
        var mode: Int = 0 // 0=stereo, 1=intensity_stereo, 2=dual_channel, 3=single_channel
        val nchannels get() = if (mode == 3) 1 else 2
        var modeExtension: Int = 0

        internal fun set(soundData: SoundData) {
            // previously aborted data reads might have left the Buffer off a byte
            // boundary, so reset back to reading from the beginning of the byte
            soundData.buffer.current = 0
            // read 4 byte header with possibility of rollback
            soundData.buffer.mark(4)
            try {
                sigBytes = read(soundData.buffer, 12)
                version = read(soundData.buffer, 1)
                layer = read(soundData.buffer, 2)
                protectionBit = read(soundData.buffer, 1)
                bitrateIndex = read(soundData.buffer, 4)
                samplingFrequency = read(soundData.buffer, 2)
                paddingBit = read(soundData.buffer, 1)
                privateBit = read(soundData.buffer, 1)
                mode = read(soundData.buffer, 2)
                modeExtension = read(soundData.buffer, 2)
                // last 4 bits ignored
                read(soundData.buffer, 4)
            } catch (e: EOFException) {
                // not enough data for a full header, so just mark header as invalid
                sigBytes = 0
            }
        }

        internal fun unRead(soundData: SoundData) {
            soundData.buffer.reset()
            soundData.buffer.lastByte = sigBytes ushr 4
        }

        // version currently ignored, even though decoder only supports MPEG V1 (version == 1)
        internal val isValid: Boolean
            get() = (sigBytes == 4095) && ( // version currently ignored, even though decoder only supports MPEG V1 (version == 1)
                layer != 0) && (
                bitrateIndex != 15) && (
                samplingFrequency != 3)

        init {
            this.set(soundData)
        }
    }

    internal class  MainDataReader(val array: ByteArray) {
        var top: Int = 0
        var index: Int = 0
        var current: Int = 0
    }

    internal class Buffer(val inp: SyncStream) {
        var current: Int = 0
        var lastByte: Int = inp.read()

        private var markedPos = 0L
        fun mark(count: Int) {
            markedPos = inp.position
        }

        fun reset() {
            inp.position = markedPos
        }

        fun seek(pos: Long) {
            inp.position = pos
            markedPos = 0L
            current = 0
            lastByte = inp.read()
        }
    }

    class SamplesIIIData {
        private val MAX_CHANNELS = 2
        val scfsi = IntArray(MAX_CHANNELS * 4)
        val part2_3_length = IntArray(MAX_CHANNELS * 2)
        val big_values = IntArray(MAX_CHANNELS * 2)
        val global_gain: IntArray = IntArray(MAX_CHANNELS * 2)
        val scalefac_compress: IntArray = IntArray(MAX_CHANNELS * 2)
        val win_switch_flag: IntArray = IntArray(MAX_CHANNELS * 2)
        val block_type: IntArray = IntArray(MAX_CHANNELS * 2)
        val mixed_block_flag: IntArray = IntArray(MAX_CHANNELS * 2)
        val table_select: IntArray = IntArray(MAX_CHANNELS * 2 * 3)
        val subblock_gain: IntArray = IntArray(MAX_CHANNELS * 2 * 3)
        val region0_count = IntArray(MAX_CHANNELS * 2)
        val region1_count = IntArray(MAX_CHANNELS * 2)
        val preflag = IntArray(MAX_CHANNELS * 2)
        val scalefac_scale = IntArray(MAX_CHANNELS * 2)
        val count1table_select = IntArray(MAX_CHANNELS * 2)
        val count1 = IntArray(MAX_CHANNELS * 2)
        val scalefac_l = IntArray(MAX_CHANNELS * 2 * 21)
        val scalefac_s = IntArray(MAX_CHANNELS * 2 * 12 * 3)
        val `is` = FloatArray(MAX_CHANNELS * 2 * 576)

        val huffmanTemp = IntArray(4)
            get() {
                field.fill(0)
                return field
            }

        val tempFloatL3NSamples: FloatArray = FloatArray(L3_NSAMPLES)
            get() {
                field.fill(0f)
                return field
            }

        val tempFloatArray36: FloatArray = FloatArray(36)
            get() {
                field.fill(0f)
                return field
            }

        val tempFloatArray512: FloatArray = FloatArray(512)
            get() {
                field.fill(0f)
                return field
            }

        val tempFloatArray32: FloatArray = FloatArray(32)
            get() {
                field.fill(0f)
                return field
            }

        fun reset() {
            scfsi.fill(0)
            part2_3_length.fill(0)
            big_values.fill(0)
            global_gain.fill(0)
            scalefac_compress.fill(0)
            win_switch_flag.fill(0)
            block_type.fill(0)
            mixed_block_flag.fill(0)
            table_select.fill(0)
            subblock_gain.fill(0)
            region0_count.fill(0)
            region1_count.fill(0)
            preflag.fill(0)
            scalefac_scale.fill(0)
            count1table_select.fill(0)
            count1.fill(0)
            scalefac_l.fill(0)
            scalefac_s.fill(0)
            `is`.fill(0f)
        }
    }

    class SamplesIIData {
        private val stereo = 2
        private val sbLimit = 30
        val allocation = IntArray(sbLimit)
        val allocationChannel = IntArray(stereo * sbLimit)
        val scfsi = IntArray(stereo * sbLimit)
        val scalefactorChannel = IntArray(stereo * sbLimit * 3)
        val sampleDecoded = FloatArray(stereo * 32 * 12 * 3)
        val tempSampleInt: IntArray = IntArray(3)
            get() {
                field.fill(0)
                return field
            }

        //val allocation = IntArray(sbLimit - bound)
        //val allocationChannel = IntArray(stereo * bound)
        //val scfsi = IntArray(stereo * sbLimit)
        //val scalefactorChannel = IntArray(stereo * sbLimit * 3)
        //val sampleDecoded = FloatArray(stereo * 32 * 12 * 3)

        fun reset() {
            allocation.fill(0)
            allocationChannel.fill(0)
            scfsi.fill(0)
            scalefactorChannel.fill(0)
            sampleDecoded.fill(0f)
        }
    }

    class SamplesIData {
        private val stereo = 2
        private val bound = 32
        val allocation = IntArray(32)
        val allocationChannel = IntArray(stereo * bound)
        val scalefactorChannel = IntArray(stereo * 32)
        val sampleDecoded = FloatArray(stereo * 32 * 12)

        fun reset() {
            allocation.fill(0)
            allocationChannel.fill(0)
            scalefactorChannel.fill(0)
            sampleDecoded.fill(0f)
        }
    }

    class SoundData internal constructor(internal val buffer: Buffer) {
        val s3d by lazy { SamplesIIIData() }
        val s2d by lazy { SamplesIIData() }
        val s1d by lazy { SamplesIData() }

        var frequency: Int = -1
        var stereo: Int = -1
        val nchannels get() = if (stereo == 1) 2 else 1
        val pcmTemp = FloatArray(2 * 32 * 12 * 3) //FloatArray(size * 32 * stereo)
        internal val synthOffset: IntArray = intArrayOf(64, 64)
        internal val synthBuffer: FloatArray = FloatArray(2 * 1024)
        internal val mainData = ByteArray(2 * 1024)
        internal val store = FloatArray(2 * 32 * 18)
        internal val v = FloatArray(2 * 1024)
        internal val mainDataReader = MainDataReader(mainData)
        //internal lateinit var mainData: ByteArray
        //internal lateinit var mainDataReader: MainDataReader
        //internal lateinit var store: FloatArray
        //internal lateinit var v: FloatArray
        var _samplesBuffer: ByteArray? = null

        fun getSamplesBuffer(size: Int): ByteArray {
            if (_samplesBuffer == null) {
                _samplesBuffer = ByteArray(size)
            }
            return _samplesBuffer!!
        }

        fun seek(pos: Long): Unit {
            //frequency = -1
            //stereo = -1
            //mainDataReader.top = 0

            //if (false) {
            if (true) {
                mainDataReader.index = 0
                mainDataReader.current = 0
                buffer.seek(pos)
            }
        }
    }
}
