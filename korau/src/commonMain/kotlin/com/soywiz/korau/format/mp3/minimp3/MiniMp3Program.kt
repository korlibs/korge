package com.soywiz.korau.format.mp3.minimp3

//ENTRY Program
//Program.main(arrayOf())
@Suppress("MemberVisibilityCanBePrivate", "FunctionName", "CanBeVal", "DoubleNegation", "LocalVariableName", "NAME_SHADOWING", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "RemoveRedundantCallsOfConversionMethods", "EXPERIMENTAL_IS_NOT_ENABLED", "RedundantExplicitType", "RemoveExplicitTypeArguments", "RedundantExplicitType", "unused", "UNCHECKED_CAST", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "NOTHING_TO_INLINE", "PropertyName", "ClassName", "USELESS_CAST", "PrivatePropertyName", "CanBeParameter", "UnusedMainParameter")
@OptIn(ExperimentalUnsignedTypes::class)
internal open class MiniMp3Program(HEAP_SIZE: Int = 0) : Runtime(HEAP_SIZE) {
    companion object {
        internal const val MINIMP3_IMPLEMENTATION = 1
        internal const val MINIMP3_MAX_SAMPLES_PER_FRAME = 2304
        internal const val MAX_FREE_FORMAT_FRAME_SIZE = 2304
        internal const val MAX_FRAME_SYNC_MATCHES = 10
        internal const val MAX_L3_FRAME_PAYLOAD_BYTES = 2304
        internal const val MAX_BITRESERVOIR_BYTES = 511
        internal const val SHORT_BLOCK_TYPE = 2
        internal const val STOP_BLOCK_TYPE = 3
        internal const val MODE_MONO = 3
        internal const val MODE_JOINT_STEREO = 1
        internal const val HDR_SIZE = 4
        internal const val BITS_DEQUANTIZER_OUT = -1
        internal const val MAX_SCF = 41
        internal const val HAVE_SSE = 0
        internal const val HAVE_SIMD = 0
        internal const val HAVE_ARMV6 = 0

        private val __STATIC_L3_imdct36_g_twid9: FloatArray = floatArrayOf(0.73727734f, 0.79335334f, 0.84339145f, 0.88701083f, 0.92387953f, 0.95371695f, 0.97629601f, 0.99144486f, 0.99904822f, 0.67559021f, 0.60876143f, 0.53729961f, 0.46174861f, 0.38268343f, 0.3007058f, 0.21643961f, 0.13052619f, 0.04361938f)
    }

    private var __STATIC_hdr_bitrate_kbps_halfrate: Array2Array3Array15UByte = Array2Array3Array15UByteAlloc(arrayOf(Array3Array15UByteAlloc(arrayOf(
        Array15UByte(fixedArrayOfUByte("\u0000\u0004\u0008\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050").ptr), Array15UByte(fixedArrayOfUByte("\u0000\u0004\u0008\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050").ptr), Array15UByte(fixedArrayOfUByte("\u0000\u0010\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050\u0058\u0060\u0070\u0080").ptr)
    )), Array3Array15UByteAlloc(arrayOf(Array15UByte(fixedArrayOfUByte("\u0000\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0050\u0060\u0070\u0080\u00a0").ptr), Array15UByte(fixedArrayOfUByte("\u0000\u0010\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0050\u0060\u0070\u0080\u00a0\u00c0").ptr), Array15UByte(fixedArrayOfUByte("\u0000\u0010\u0020\u0030\u0040\u0050\u0060\u0070\u0080\u0090\u00a0\u00b0\u00c0\u00d0\u00e0").ptr)))))
    private var __STATIC_hdr_sample_rate_hz_g_hz: Array3UInt = Array3UInt(fixedArrayOfUInt(44100u, 48000u, 32000u).ptr)
    private var __STATIC_L12_subband_alloc_table_g_alloc_L1: CPointer<L12_subband_alloc_t> = fixedArrayOfL12_subband_alloc_t(1) { this[0] = L12_subband_alloc_tAlloc(tab_offset = (76.toUByte()), code_tab_width = (4.toUByte()), band_count = (32.toUByte())) }
    private var __STATIC_L12_subband_alloc_table_g_alloc_L2M2: CPointer<L12_subband_alloc_t> = fixedArrayOfL12_subband_alloc_t(3) { this[0] = L12_subband_alloc_tAlloc(tab_offset = (60.toUByte()), code_tab_width = (4.toUByte()), band_count = (4.toUByte())); this[1] = L12_subband_alloc_tAlloc(tab_offset = (44.toUByte()), code_tab_width = (3.toUByte()), band_count = (7.toUByte())); this[2] = L12_subband_alloc_tAlloc(tab_offset = (44.toUByte()), code_tab_width = (2.toUByte()), band_count = (19.toUByte())) }
    private var __STATIC_L12_subband_alloc_table_g_alloc_L2M1: CPointer<L12_subband_alloc_t> = fixedArrayOfL12_subband_alloc_t(4) { this[0] = L12_subband_alloc_tAlloc(tab_offset = (0.toUByte()), code_tab_width = (4.toUByte()), band_count = (3.toUByte())); this[1] = L12_subband_alloc_tAlloc(tab_offset = (16.toUByte()), code_tab_width = (4.toUByte()), band_count = (8.toUByte())); this[2] = L12_subband_alloc_tAlloc(tab_offset = (32.toUByte()), code_tab_width = (3.toUByte()), band_count = (12.toUByte())); this[3] = L12_subband_alloc_tAlloc(tab_offset = (40.toUByte()), code_tab_width = (2.toUByte()), band_count = (7.toUByte())) }
    private var __STATIC_L12_subband_alloc_table_g_alloc_L2M1_lowrate: CPointer<L12_subband_alloc_t> = fixedArrayOfL12_subband_alloc_t(2) { this[0] = L12_subband_alloc_tAlloc(tab_offset = (44.toUByte()), code_tab_width = (4.toUByte()), band_count = (2.toUByte())); this[1] = L12_subband_alloc_tAlloc(tab_offset = (44.toUByte()), code_tab_width = (3.toUByte()), band_count = (10.toUByte())) }
    private var __STATIC_L12_read_scalefactors_g_deq_L12: Array54Float = Array54Float(fixedArrayOfFloat((9.53674316e-7f / 3f), (7.56931807e-7f / 3f), (6.00777173e-7f / 3f), (9.53674316e-7f / 7f), (7.56931807e-7f / 7f), (6.00777173e-7f / 7f), (9.53674316e-7f / 15f), (7.56931807e-7f / 15f), (6.00777173e-7f / 15f), (9.53674316e-7f / 31f), (7.56931807e-7f / 31f), (6.00777173e-7f / 31f), (9.53674316e-7f / 63f), (7.56931807e-7f / 63f), (6.00777173e-7f / 63f), (9.53674316e-7f / 127f), (7.56931807e-7f / 127f), (6.00777173e-7f / 127f), (9.53674316e-7f / 255f), (7.56931807e-7f / 255f), (6.00777173e-7f / 255f), (9.53674316e-7f / 511f), (7.56931807e-7f / 511f), (6.00777173e-7f / 511f), (9.53674316e-7f / 1023f), (7.56931807e-7f / 1023f), (6.00777173e-7f / 1023f), (9.53674316e-7f / 2047f), (7.56931807e-7f / 2047f), (6.00777173e-7f / 2047f), (9.53674316e-7f / 4095f), (7.56931807e-7f / 4095f), (6.00777173e-7f / 4095f), (9.53674316e-7f / 8191f), (7.56931807e-7f / 8191f), (6.00777173e-7f / 8191f), (9.53674316e-7f / 16383f), (7.56931807e-7f / 16383f), (6.00777173e-7f / 16383f), (9.53674316e-7f / 32767f), (7.56931807e-7f / 32767f), (6.00777173e-7f / 32767f), (9.53674316e-7f / 65535f), (7.56931807e-7f / 65535f), (6.00777173e-7f / 65535f), (9.53674316e-7f / 3f), (7.56931807e-7f / 3f), (6.00777173e-7f / 3f), (9.53674316e-7f / 5f), (7.56931807e-7f / 5f), (6.00777173e-7f / 5f), (9.53674316e-7f / 9f), (7.56931807e-7f / 9f), (6.00777173e-7f / 9f)).ptr)
    private var __STATIC_L12_read_scale_info_g_bitalloc_code_tab: CPointer<UByte> = fixedArrayOfUByte("\u0000\u0011\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0010\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u0010\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0010\u0000\u0011\u0012\u0010\u0000\u0011\u0012\u0013\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u0000\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0010")
    private var __STATIC_L3_read_side_info_g_scf_long: Array8Array23UByte = Array8Array23UByteAlloc(arrayOf(
        Array23UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u000c\u000c\u000c\u000c\u000c\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u004c\u005a\u0002\u0002\u0002\u0002\u0002\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0012\u0016\u001a\u0020\u0026\u002e\u0036\u003e\u0046\u004c\u0024\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0008\u0008\u000a\u000c\u0010\u0014\u0018\u001c\u0022\u002a\u0032\u0036\u004c\u009e\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u000a\u000c\u0010\u0012\u0016\u001c\u0022\u0028\u002e\u0036\u0036\u00c0\u0000").ptr), Array23UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0008\u000a\u000c\u0010\u0014\u0018\u001e\u0026\u002e\u0038\u0044\u0054\u0066\u001a\u0000").ptr)
    ))
    private var __STATIC_L3_read_side_info_g_scf_short: Array8Array40UByte = Array8Array40UByteAlloc(arrayOf(
        Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u0018\u0018\u0018\u001c\u001c\u001c\u0024\u0024\u0024\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u001a\u001a\u001a\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000e\u000e\u000e\u0012\u0012\u0012\u001a\u001a\u001a\u0020\u0020\u0020\u002a\u002a\u002a\u0012\u0012\u0012\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u0020\u0020\u0020\u002c\u002c\u002c\u000c\u000c\u000c\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0016\u0016\u0016\u001e\u001e\u001e\u0038\u0038\u0038\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0042\u0042\u0042\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0022\u0022\u0022\u002a\u002a\u002a\u000c\u000c\u000c\u0000").ptr)
    ))
    private var __STATIC_L3_read_side_info_g_scf_mixed: Array8Array40UByte = Array8Array40UByteAlloc(arrayOf(
        Array40UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u000c\u000c\u000c\u0004\u0004\u0004\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u0018\u0018\u0018\u001c\u001c\u001c\u0024\u0024\u0024\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u001a\u001a\u001a\u0000").ptr), Array40UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000e\u000e\u000e\u0012\u0012\u0012\u001a\u001a\u001a\u0020\u0020\u0020\u002a\u002a\u002a\u0012\u0012\u0012\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u0020\u0020\u0020\u002c\u002c\u002c\u000c\u000c\u000c\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0016\u0016\u0016\u001e\u001e\u001e\u0038\u0038\u0038\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0042\u0042\u0042\u0000", size = 40).ptr), Array40UByte(fixedArrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0022\u0022\u0022\u002a\u002a\u002a\u000c\u000c\u000c\u0000", size = 40).ptr)
    ))
    private var __STATIC_L3_ldexp_q2_g_expfrac: Array4Float = Array4Float(fixedArrayOfFloat(9.31322575e-10f, 7.83145814e-10f, 6.58544508e-10f, 5.53767716e-10f).ptr)
    private var __STATIC_L3_decode_scalefactors_g_scf_partitions: Array3Array28UByte = Array3Array28UByteAlloc(arrayOf(
        Array28UByte(fixedArrayOfUByte("\u0006\u0005\u0005\u0005\u0006\u0005\u0005\u0005\u0006\u0005\u0007\u0003\u000b\u000a\u0000\u0000\u0007\u0007\u0007\u0000\u0006\u0006\u0006\u0003\u0008\u0008\u0005\u0000").ptr), Array28UByte(fixedArrayOfUByte("\u0008\u0009\u0006\u000c\u0006\u0009\u0009\u0009\u0006\u0009\u000c\u0006\u000f\u0012\u0000\u0000\u0006\u000f\u000c\u0000\u0006\u000c\u0009\u0006\u0006\u0012\u0009\u0000").ptr), Array28UByte(fixedArrayOfUByte("\u0009\u0009\u0006\u000c\u0009\u0009\u0009\u0009\u0009\u0009\u000c\u0006\u0012\u0012\u0000\u0000\u000c\u000c\u000c\u0000\u000c\u0009\u0009\u0006\u000f\u000c\u0009\u0000").ptr)
    ))
    private var __STATIC_L3_decode_scalefactors_g_scfc_decode: Array16UByte = Array16UByte(fixedArrayOfUByte("\u0000\u0001\u0002\u0003\u000c\u0005\u0006\u0007\u0009\u000a\u000b\u000d\u000e\u000f\u0012\u0013").ptr)
    private var __STATIC_L3_decode_scalefactors_g_mod: Array24UByte = Array24UByte(fixedArrayOfUByte("\u0005\u0005\u0004\u0004\u0005\u0005\u0004\u0001\u0004\u0003\u0001\u0001\u0005\u0006\u0006\u0001\u0004\u0004\u0004\u0001\u0004\u0003\u0001\u0001").ptr)
    private var __STATIC_L3_decode_scalefactors_g_preamp: Array10UByte = Array10UByte(fixedArrayOfUByte("\u0001\u0001\u0001\u0001\u0002\u0002\u0003\u0003\u0003\u0002").ptr)
    private var __STATIC_L3_huffman_tabs: CPointer<Short> = fixedArrayOfShort("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0201\u0201\u0201\u0201\u0201\u0201\u0201\u0201\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\uff01\u0521\u0512\u0502\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0122\u0120\uff01\u0521\u0512\u0502\u0301\u0301\u0301\u0301\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0122\u0120\uff03\ufec2\ufea1\ufe91\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0333\u0332\u0223\u0223\u0113\u0113\u0113\u0113\u0231\u0230\u0203\u0222\u0121\u0112\u0120\u0102\uff02\ufee1\u0531\u0513\u0522\u0520\u0421\u0421\u0412\u0412\u0402\u0402\u0310\u0310\u0310\u0310\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0301\u0301\u0301\u0301\u0300\u0300\u0300\u0300\u0233\u0230\u0132\u0132\u0123\u0103\uff04\ufe63\ufe23\ufde2\u0512\ufdc1\u0411\u0411\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe81\ufe71\u0453\u0444\u0452\u0425\u0351\u0351\u0315\u0315\u0450\u0443\u0305\u0305\u0434\u0433\u0155\u0154\u0145\u0135\u0342\u0324\u0241\u0241\u0214\u0214\u0204\u0204\u0340\u0332\u0323\u0330\u0231\u0231\u0213\u0213\u0203\u0222\u0121\u0121\u0120\u0102\uff04\ufe53\ufe13\ufdd1\u0421\u0421\u0412\u0412\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0200\ufe82\u0435\ufe61\u0452\u0425\u0450\u0351\u0351\u0315\u0315\u0443\u0434\u0405\u0433\u0342\u0342\u0255\u0245\u0154\u0154\u0153\u0144\u0324\u0341\u0214\u0214\u0340\u0304\u0332\u0323\u0331\u0313\u0330\u0303\u0122\u0122\u0122\u0122\u0120\u0102\uff03\ufea3\ufe62\ufe41\ufe31\u0531\u0513\ufe21\u0522\u0520\u0421\u0421\u0412\u0412\u0402\u0402\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0300\u0300\u0300\u0300\ufec1\u0353\u0335\ufeb1\u0344\u0352\u0325\u0351\u0155\u0154\u0145\u0150\u0215\u0215\u0243\u0243\u0234\u0234\u0305\u0340\u0242\u0224\u0233\u0204\u0141\u0114\u0132\u0123\u0130\u0103\uff05\ufdc4\ufd23\ufcc2\ufca1\ufc91\u0411\u0411\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe01\ufdf1\ufde1\u0574\u0547\u0565\u0556\u0573\u0537\u0564\ufdd1\u0536\u0472\u0472\u0427\u0427\u0546\u0570\u0407\u0407\u0426\u0426\u0554\u0553\u0460\u0460\u0535\u0544\u0371\u0371\u0371\u0371\u0177\u0176\u0167\u0175\u0157\u0166\u0155\u0145\u0317\u0317\u0463\u0462\ufd41\u0451\u0415\ufd31\u0361\u0361\u0316\u0316\u0306\u0306\u0450\u0405\u0152\u0125\u0143\u0134\ufce1\ufcd1\u0341\u0314\u0304\u0332\u0323\u0330\u0142\u0124\u0133\u0140\u0231\u0213\u0203\u0222\u0121\u0112\u0120\u0102\uff05\ufdf3\ufda3\ufd53\ufd03\ufcc1\ufcb2\u0512\u0421\u0421\u0520\u0502\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0577\u0576\u0567\u0557\u0566\u0574\u0547\ufe01\u0565\u0556\u0473\u0473\u0437\u0437\u0464\u0464\u0554\u0545\u0553\u0535\u0372\u0372\u0372\u0372\u0327\u0327\u0327\u0327\u0446\u0446\u0470\u0470\u0175\u0155\u0217\u0217\u0371\u0307\u0363\u0336\u0306\ufdb1\u0144\u0152\ufd61\u0351\u0226\u0226\u0362\u0360\u0261\u0261\u0125\u0150\u0216\u0216\u0315\u0343\u0305\ufd11\u0342\u0324\u0134\u0133\u0341\u0314\u0340\u0304\u0232\u0232\u0223\u0223\u0131\u0113\u0230\u0203\u0122\u0122\uff04\ufe73\ufe23\ufdd3\ufd92\ufd73\ufd31\ufd21\ufd12\u0531\u0513\u0522\u0421\u0421\u0412\u0412\u0520\u0502\u0400\u0400\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\ufe81\u0467\u0475\u0457\u0466\u0474\u0447\u0456\u0365\u0365\u0373\u0373\u0437\u0455\u0372\u0372\u0177\u0176\u0327\u0364\u0346\u0371\u0317\ufe31\u0363\u0336\u0170\u0107\u0354\u0345\u0344\ufde1\u0262\u0262\u0226\u0226\u0160\u0150\u0216\u0216\u0361\u0306\u0353\u0335\u0352\u0325\u0251\u0215\u0243\u0234\u0305\u0340\u0242\u0242\u0224\u0224\u0241\u0241\u0133\u0114\u0132\u0123\u0204\u0230\u0103\u0103\uff06\uf7c5\uf635\uf534\uf4a3\uf462\uf441\uf431\u0411\u0411\u0410\u0410\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufd01\ufbe4\ufb43\ufb03\ufab2\ufa83\ufa43\ufa01\uf9f2\uf9d2\uf9b2\uf991\uf982\uf962\uf942\uf921\uf912\uf8f1\uf8e2\uf8c2\uf8a2\u061d\uf881\uf871\uf861\uf851\u06c3\u06c2\u062c\u06b5\uf841\u06c1\u061c\uf831\u060c\uf821\uf811\u06b3\u063b\uf801\u06b2\uf7f1\u064a\uf7e1\u0649\uf7d1\u052b\u052b\u05b1\u05b1\u051b\u051b\u06b0\u060b\u0669\u06a4\u06a3\u063a\u0695\u0659\u05a2\u05a2\u052a\u052a\ufcf4\ufc33\ufc72\u04ff\u04fe\u04fd\u04ee\u04fc\u04ed\u04fb\u04bf\u04ec\u04cd\ufc41\u03ce\u03ce\u03dd\u03dd\ufc51\u02df\u01de\u01de\u01ef\u01cf\u01fa\u019e\ufbf1\u03eb\u03be\u03f9\u039f\u03ae\u03db\u03bd\u01af\u01dc\u04f8\u048f\u04cc\ufb61\u04e8\ufb51\u037f\u037f\u03ad\u03ad\u04da\u04cb\u04bc\u046f\u03f6\u03f6\u01ea\u01e9\u01f7\u01e7\u038e\u03f5\u03d9\u039d\u035f\u037e\u03ca\u03bb\u03f4\u034f\ufac1\u033f\u02f3\u02f3\u03d8\u038d\u01ac\u016e\u02f2\u022f\ufa91\u02f0\u01e6\u01c9\u039c\u03e5\u02ba\u02ba\u03d7\u037d\u02e4\u02e4\u038c\u036d\u02e3\u02e3\u029b\u029b\u03b9\u03aa\u01f1\u011f\u010f\u010f\u02ab\u025e\u024e\u02c8\u02d6\u023e\u012e\u012e\u02e2\u02e0\u01e1\u011e\u020e\u02d5\u025d\u02c7\u027c\u02d4\u02b8\u028b\u024d\u02a9\u029a\u02c6\u016c\u01d3\u023d\u02b7\u01d2\u01d2\u012d\u01d1\u017b\u017b\u02c5\u025c\u0299\u02a7\u013c\u013c\u027a\u0279\u01b4\u01b4\u01d0\u010d\u01a8\u018a\u01c4\u014c\u01b6\u016b\u015b\u0198\u0189\u01c0\u014b\u01a6\u016a\u0197\u0188\u01a5\u015a\u0196\u0187\u0178\u0177\u0167\u05a1\u051a\uf6c1\u050a\uf6b1\u0539\uf6a1\uf691\u0592\u0529\uf681\u0583\u0538\uf671\uf661\uf651\u0491\u0491\u0419\u0419\u0590\u0509\u0584\u0548\u0527\uf641\u0482\u0482\u0428\u0428\u0481\u0481\u01a0\u0186\u0168\u0194\u0193\u0185\u0158\u0176\u0175\u0157\u0166\u0174\u0147\u0165\u0156\u0137\u0164\u0146\u0573\u0572\u0471\u0471\u0417\u0417\u0555\u0570\u0507\u0563\u0536\u0554\u0545\u0562\u0526\u0553\u0318\u0318\u0318\u0318\u0480\u0480\u0408\u0408\u0461\u0461\u0416\u0416\u0460\u0460\u0406\u0406\uf4b1\u0452\u0425\u0450\u0351\u0351\u0315\u0315\u0443\u0434\u0405\u0442\u0424\u0433\u0341\u0341\u0135\u0144\u0214\u0214\u0340\u0304\u0332\u0323\u0231\u0231\u0213\u0230\u0203\u0222\u0121\u0112\u0120\u0102\uff06\ufb65\uf9d5\uf8d4\uf834\uf7b4\uf733\uf6e3\uf693\uf653\uf612\uf5f2\uf5d1\uf5c2\uf5a1\u0522\u0521\u0512\u0520\u0502\u0311\u0311\u0311\u0311\u0410\u0410\u0401\u0401\u0300\u0300\u0300\u0300\ufd02\ufce2\ufcc2\ufca2\ufc81\ufc71\ufc61\ufc51\ufc41\ufc31\ufc21\ufc11\ufc01\ufbf1\ufbe1\ufbd2\u06bc\u066f\ufbb1\ufba1\u065f\u06e7\u067e\u06ca\u06ac\u06bb\ufb91\u06f4\u064f\u06f3\u063f\u068d\u066e\u06f2\u062f\ufb81\u06f1\u061f\u06c9\u069c\u06e5\u06ba\u06ab\u065e\u06d7\u067d\u06e4\u064e\u06c8\u068c\u06e3\u06d6\u066d\u063e\u06b9\u069b\u06e2\u06aa\u062e\u06e1\u061e\ufb71\u06d5\u065d\u02ff\u02fe\u02ef\u02fd\u01ee\u01ee\u02df\u02fc\u02cf\u02ed\u02de\u02fb\u01bf\u01bf\u02ec\u02ce\u01dd\u01fa\u01af\u01eb\u01be\u01dc\u01cd\u01f9\u019f\u01ae\u01db\u01bd\u01f8\u018f\u01cc\u01e9\u019e\u01f7\u017f\u01da\u01ad\u01cb\u01f6\u01f6\u02ea\u02f0\u01e8\u018e\u01f5\u01d9\u019d\u01d8\u01e6\u010f\u01e0\u010e\ufa61\ufa51\u054d\ufa41\ufa31\ufa21\u053d\u052d\ufa11\u05d1\u05b7\u057b\u051d\ufa01\u055c\u05a8\u058a\u05c4\u054c\u05b6\u056b\uf9f1\u05c3\u053c\u05a7\u057a\u056a\uf9e1\u042c\u042c\u05c2\u05b5\u01c7\u017c\u01d4\u01b8\u018b\u01a9\u019a\u01c6\u016c\u01d3\u01d2\u01d0\u01c5\u010d\u0199\u01c0\u010c\u01b0\u055b\u05c1\u0598\u0589\u051c\u05b4\u054b\u05a6\u05b3\u0597\u043b\u043b\u0579\u0588\u05b2\u05a5\u042b\u042b\u055a\u05b1\u041b\u041b\u050b\u0596\u0569\u05a4\u054a\u0587\u0578\u05a3\u043a\u043a\u0495\u0459\u04a2\u042a\u04a1\u041a\uf851\u0486\u0468\u0494\u0449\u0493\u0439\uf841\u0485\u0458\u01a0\u010a\u0177\u0190\u0492\u0476\u0467\u0429\u0319\u0319\u0491\u0409\u0484\u0448\u0475\u0457\u0483\u0438\u0466\u0474\u0382\u0382\u0328\u0328\u0381\u0381\u0318\u0318\u0447\u0480\u0408\u0465\u0456\u0473\u0437\u0464\u0372\u0327\u0346\u0371\u0355\u0317\uf6f1\u0363\u0170\u0107\u0336\u0354\u0345\u0362\u0326\u0361\uf6a1\u0353\u0160\u0106\u0216\u0216\u0335\u0344\u0252\u0252\u0225\u0225\u0251\u0251\u0215\u0215\u0350\u0305\u0243\u0243\u0234\u0242\u0224\u0233\u0114\u0114\u0241\u0240\u0132\u0123\u0204\u0230\u0131\u0131\u0113\u0103\uff05\ufc84\uf7f6\uf5c4\uf4f4\uf473\uf431\uf421\u0411\u0411\u0410\u0410\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe01\ufdf1\ufde1\ufdd1\u05fa\ufdc1\ufdb1\u05f8\u05f7\u057f\u05f6\u056f\u03ff\u03ff\u03ff\u03ff\u05f5\u055f\u04f4\u04f4\u044f\u044f\u043f\u043f\u040f\u040f\u05f3\ufda4\u032f\u032f\u032f\u032f\u01fe\u01ef\u01fd\u01df\u01fc\u01cf\u01fb\u01bf\u01af\u01f9\u019f\u018f\ufd22\ufcf2\u04ee\ufcd1\u04eb\u04dc\ufcc1\u04ea\u04cc\ufcb1\ufca1\u04ac\ufc91\u04e5\u03db\u03db\u02ec\ufd01\u01ed\u01ed\u01ce\u01dd\u019e\u019e\u02ae\u029d\u01de\u01be\u01cd\u01bd\u01da\u01ad\u01e7\u01ca\u019c\u01d7\u04f2\u04f0\u03f1\u03f1\u031f\u031f\ufc05\ufb04\ufa54\uf9d3\uf973\uf923\uf8e3\uf8a2\uf873\uf833\u04e9\u04e9\u05cb\u05bc\u05e8\u058e\u05d9\u057e\u05bb\u05d8\u058d\u05e6\u046e\u046e\u04c9\u04c9\u05ba\u05ab\u055e\u057d\u04e4\u04e4\u054e\u05c8\u048c\u048c\u04e3\u04e3\u04d6\u04d6\u056d\u05b9\ufa81\u041e\u044d\ufa71\u04b7\ufa61\u033e\u033e\u04e0\u040e\u04d5\u045d\u04c7\u047c\u04d4\u04b8\u019b\u01aa\u018b\u019a\u017b\u010d\u04a9\u04c6\u046c\u04d3\u04c5\u045c\u03d0\u03d0\u04a8\u048a\u0499\u04c4\u046b\u04a7\u03c3\u03c3\uf991\u03c1\u030c\uf981\u022e\u022e\u03e2\u03e1\u01b5\u0198\u0189\u0197\u033d\u03d2\u032d\u031d\u03b3\uf931\u02d1\u02d1\u0179\u0188\u034c\u03b6\u033c\u037a\u02c2\u02c2\u032c\u035b\u031c\u03c0\u03b4\u034b\u03a6\u036a\u023b\u023b\uf881\u02b2\u022b\u02b1\u01a5\u015a\u021b\u021b\u03b0\u030b\u0396\u0369\u03a4\u034a\u0387\u0378\u023a\u023a\u03a3\u0395\u02a2\u02a2\uf5f1\u061a\uf5e1\u0649\uf5d1\u0676\u052a\u052a\u05a1\u05a1\u06a0\u060a\u0693\u0639\u0685\u0658\u0592\u0592\u0529\u0529\u0667\u0690\u0591\u0591\u0519\u0519\u0609\u0684\u0648\u0657\u0683\u0638\u0666\u0682\u0528\u0528\u0674\u0647\u0581\u0581\u0518\u0518\u0508\u0508\u0680\u0665\u0573\u0573\u0537\u0537\u0656\u0664\u0572\u0572\u0527\u0527\u0646\u0655\u0570\u0570\u0471\u0471\u0471\u0471\u0159\u0186\u0168\u0177\u0194\u0175\u0417\uf541\uf531\uf521\u0426\u0461\u0416\uf511\u0435\uf501\u0452\u0425\u0315\u0315\u0451\u0450\u0107\u0163\u0136\u0154\u0145\u0162\u0160\u0106\u0153\u0144\u0443\u0434\u0405\u0442\u0424\u0433\u0341\u0341\u0314\u0314\u0440\u0404\u0332\u0332\u0323\u0323\u0231\u0231\u0213\u0213\u0330\u0303\u0222\u0222\u0121\u0112\u0120\u0102\uff03\ufec3\ufe83\ufe42\ufe22\ufe03\u04ff\u04ff\ufcd5\ufb65\ufa55\uf924\uf894\uf814\uf773\uf733\uf6e3\uf692\uf673\uf631\uf622\u0521\u0512\uf601\u0411\u0411\u0410\u0410\u0401\u0401\u0400\u0400\u03fe\u03ef\u03fd\u03df\u03fc\u03cf\u03fb\u03bf\u02af\u02af\u03fa\u03f9\u029f\u029f\u028f\u028f\u03f8\u03f7\u027f\u027f\u02f6\u02f6\u026f\u026f\u02f5\u025f\u02f4\u024f\u02f3\u023f\u02f2\u022f\u021f\u021f\u03f1\u030f\ufdc1\ufd93\ufd53\ufd13\u01f0\ufdb2\u02ee\u02ed\u02de\u02ec\u03ce\u03dd\u03eb\u03be\u03dc\u03cd\u03ea\u03ae\u03db\u03bd\u03cc\u03e9\u039e\u03da\u03ad\u03cb\u03bc\u03e8\u038e\u03d9\u039d\u03e7\u037e\u03ca\ufbd1\ufbc1\ufbb2\u056e\ufb91\u059c\u05e5\u05ab\u055e\ufb81\u057d\u054e\u05c8\u058c\ufb71\u05e3\u05d6\u056d\u053e\u05b9\u059b\u05aa\u052e\u05e1\u051e\u05d5\u055d\u05c7\u057c\u05d4\u05b8\u058b\u01ac\u01bb\u01d8\u018d\u02e0\u020e\u01d0\u01d0\u01e6\u01c9\u01ba\u01d7\u01e4\u01e2\u054d\u05a9\u059a\u05c6\u056c\u05d3\u053d\u05d2\u052d\u05d1\u05b7\u057b\u051d\u05c5\u055c\u05a8\u058a\u0599\u05c4\u054c\u05b6\u056b\ufa61\u05c3\u053c\u05a7\u057a\u05c2\u052c\u05b5\u055b\u05c1\u010d\u01c0\u0598\u0589\u051c\u05b4\uf951\u05b3\uf941\u05a1\u044b\u044b\u05a6\u056a\u0597\u0579\uf931\u0509\u043b\u043b\u0488\u0488\u05b2\u05a5\u042b\u042b\u055a\u05b1\u051b\u0596\u0469\u0469\u044a\u044a\u010c\u01b0\u010b\u01a0\u010a\u0190\uf8a1\u0478\u04a3\u043a\u0495\u0459\u04a2\u042a\u041a\u0486\u0468\u0477\u0494\u0449\u0493\u0439\u01a4\u0187\u0485\u0458\u0492\u0476\u0467\u0429\u0491\u0419\u0484\u0448\u0475\u0457\u0483\u0438\u0466\u0482\u0428\u0481\u0474\u0447\u0418\uf791\u0465\u0456\u0471\uf781\u0337\u0337\u0473\u0472\u0327\u0327\u0180\u0108\u0170\u0107\u0364\u0346\u0355\u0317\u0363\u0336\u0354\u0345\u0362\u0326\u0361\u0316\uf6f1\u0353\u0335\u0344\u0160\u0106\u0352\u0325\u0351\uf6a1\u0215\u0215\u0343\u0334\u0150\u0105\u0242\u0224\u0233\u0241\u0214\u0214\u0340\u0304\u0232\u0232\u0223\u0223\u0131\u0113\u0230\u0203\u0122\u0122\u0120\u0102")
    private var __STATIC_L3_huffman_tab32: CPointer<UByte> = fixedArrayOfUByte("\u0082\u00a2\u00c1\u00d1\u002c\u001c\u004c\u008c\u0009\u0009\u0009\u0009\u0009\u0009\u0009\u0009\u00be\u00fe\u00de\u00ee\u007e\u005e\u009d\u009d\u006d\u003d\u00ad\u00cd")
    private var __STATIC_L3_huffman_tab33: CPointer<UByte> = fixedArrayOfUByte("\u00fc\u00ec\u00dc\u00cc\u00bc\u00ac\u009c\u008c\u007c\u006c\u005c\u004c\u003c\u002c\u001c\u000c")
    private var __STATIC_L3_huffman_tabindex: Array32Short = Array32Short(fixedArrayOfShort("\u0000\u0020\u0040\u0062\u0000\u0084\u00b4\u00da\u0124\u016c\u01aa\u021a\u0288\u02ea\u0000\u0466\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u0732\u0732\u0732\u0732\u0732\u0732\u0732\u0732").ptr)
    private var __STATIC_L3_huffman_g_linbits: CPointer<UByte> = fixedArrayOfUByte("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0003\u0004\u0006\u0008\u000a\u000d\u0004\u0005\u0006\u0007\u0008\u0009\u000b\u000d")
    private var __STATIC_L3_stereo_process_g_pan: Array14Float = Array14Float(fixedArrayOfFloat(0f, 1f, 0.21132487f, 0.78867513f, 0.3660254f, 0.6339746f, 0.5f, 0.5f, 0.6339746f, 0.3660254f, 0.78867513f, 0.21132487f, 1f, 0f).ptr)
    private var __STATIC_L3_antialias_g_aa: Array2Array8Float = Array2Array8FloatAlloc(arrayOf(Array8Float(fixedArrayOfFloat(0.85749293f, 0.881742f, 0.94962865f, 0.98331459f, 0.99551782f, 0.99916056f, 0.9998992f, 0.99999316f).ptr), Array8Float(fixedArrayOfFloat(0.51449576f, 0.47173197f, 0.31337745f, 0.1819132f, 0.09457419f, 0.04096558f, 0.01419856f, 0.00369997f).ptr)))
    private var __STATIC_L3_imdct12_g_twid3: Array6Float = Array6Float(fixedArrayOfFloat(0.79335334f, 0.92387953f, 0.99144486f, 0.60876143f, 0.38268343f, 0.13052619f).ptr)
    private var __STATIC_L3_imdct_gr_g_mdct_window: Array2Array18Float = Array2Array18FloatAlloc(arrayOf(Array18Float(fixedArrayOfFloat(0.99904822f, 0.99144486f, 0.97629601f, 0.95371695f, 0.92387953f, 0.88701083f, 0.84339145f, 0.79335334f, 0.73727734f, 0.04361938f, 0.13052619f, 0.21643961f, 0.3007058f, 0.38268343f, 0.46174861f, 0.53729961f, 0.60876143f, 0.67559021f).ptr), Array18Float(fixedArrayOfFloat(1f, 1f, 1f, 1f, 1f, 1f, 0.99144486f, 0.92387953f, 0.79335334f, 0f, 0f, 0f, 0f, 0f, 0f, 0.13052619f, 0.38268343f, 0.60876143f).ptr)))
    private var __STATIC_mp3d_DCT_II_g_sec: Array24Float = Array24Float(fixedArrayOfFloat(10.19000816f, 0.50060302f, 0.50241929f, 3.40760851f, 0.50547093f, 0.52249861f, 2.05778098f, 0.51544732f, 0.56694406f, 1.4841646f, 0.53104258f, 0.6468218f, 1.16943991f, 0.55310392f, 0.7881546f, 0.97256821f, 0.58293498f, 1.06067765f, 0.83934963f, 0.62250412f, 1.72244716f, 0.74453628f, 0.67480832f, 5.10114861f).ptr)
    private var __STATIC_mp3d_synth_g_win: FloatPointer = fixedArrayOfFloat(-1f, 26f, -31f, 208f, 218f, 401f, -519f, 2063f, 2000f, 4788f, -5517f, 7134f, 5959f, 35640f, -39336f, 74992f, -1f, 24f, -35f, 202f, 222f, 347f, -581f, 2080f, 1952f, 4425f, -5879f, 7640f, 5288f, 33791f, -41176f, 74856f, -1f, 21f, -38f, 196f, 225f, 294f, -645f, 2087f, 1893f, 4063f, -6237f, 8092f, 4561f, 31947f, -43006f, 74630f, -1f, 19f, -41f, 190f, 227f, 244f, -711f, 2085f, 1822f, 3705f, -6589f, 8492f, 3776f, 30112f, -44821f, 74313f, -1f, 17f, -45f, 183f, 228f, 197f, -779f, 2075f, 1739f, 3351f, -6935f, 8840f, 2935f, 28289f, -46617f, 73908f, -1f, 16f, -49f, 176f, 228f, 153f, -848f, 2057f, 1644f, 3004f, -7271f, 9139f, 2037f, 26482f, -48390f, 73415f, -2f, 14f, -53f, 169f, 227f, 111f, -919f, 2032f, 1535f, 2663f, -7597f, 9389f, 1082f, 24694f, -50137f, 72835f, -2f, 13f, -58f, 161f, 224f, 72f, -991f, 2001f, 1414f, 2330f, -7910f, 9592f, 70f, 22929f, -51853f, 72169f, -2f, 11f, -63f, 154f, 221f, 36f, -1064f, 1962f, 1280f, 2006f, -8209f, 9750f, -998f, 21189f, -53534f, 71420f, -2f, 10f, -68f, 147f, 215f, 2f, -1137f, 1919f, 1131f, 1692f, -8491f, 9863f, -2122f, 19478f, -55178f, 70590f, -3f, 9f, -73f, 139f, 208f, -29f, -1210f, 1870f, 970f, 1388f, -8755f, 9935f, -3300f, 17799f, -56778f, 69679f, -3f, 8f, -79f, 132f, 200f, -57f, -1283f, 1817f, 794f, 1095f, -8998f, 9966f, -4533f, 16155f, -58333f, 68692f, -4f, 7f, -85f, 125f, 189f, -83f, -1356f, 1759f, 605f, 814f, -9219f, 9959f, -5818f, 14548f, -59838f, 67629f, -4f, 7f, -91f, 117f, 177f, -106f, -1428f, 1698f, 402f, 545f, -9416f, 9916f, -7154f, 12980f, -61289f, 66494f, -5f, 6f, -97f, 111f, 163f, -127f, -1498f, 1634f, 185f, 288f, -9585f, 9838f, -8540f, 11455f, -62684f, 65290f)
    // typealias uint8_t = UByte
    // typealias uint16_t = UShort
    // typealias uint32_t = UInt
    // typealias uint64_t = ULong
    // typealias int8_t = Byte
    // typealias int16_t = Short
    // typealias int32_t = Int
    // typealias int64_t = Long
    // typealias mp3dec_frame_info_t = mp3dec_frame_info_t
    // typealias mp3dec_t = mp3dec_t
    // var mp3dec_init: CFunction<(CPointer<struct null>) -> Unit> = 0 /*CFunction<(CPointer<mp3dec_t>) -> Unit>*/
    // typealias mp3d_sample_t = Short
    // var mp3dec_decode_frame: CFunction<(CPointer<struct null>, CPointer<UByte>, Int, CPointer<Short>, CPointer<struct null>) -> Int> = 0 /*CFunction<(CPointer<mp3dec_t>, CPointer<uint8_t>, Int, CPointer<mp3d_sample_t>, CPointer<mp3dec_frame_info_t>) -> Int>*/
    // typealias size_t = Int
    // var malloc: CFunction<(Int) -> CPointer<Unit>> = 0 /*CFunction<(size_t) -> CPointer<Unit>>*/
    // var realloc: CFunction<(CPointer<Unit>, Int) -> CPointer<Unit>> = 0 /*CFunction<(CPointer<Unit>, size_t) -> CPointer<Unit>>*/
    // var free: CFunction<(CPointer<Unit>) -> Unit> = 0 /*CFunction<(CPointer<Unit>) -> Unit>*/
    // var exit: CFunction<(Int) -> Unit> = 0 /*CFunction<(Int) -> Unit>*/
    // var strlen: CFunction<(CPointer<Byte>) -> Int> = 0 /*CFunction<(CPointer<Byte>) -> size_t>*/
    // var memset: CFunction<(CPointer<Unit>, Int, Int) -> CPointer<Unit>> = 0 /*CFunction<(CPointer<Unit>, Int, size_t) -> CPointer<Unit>>*/
    // var memcpy: CFunction<(CPointer<Unit>, CPointer<Unit>, Int) -> CPointer<Unit>> = 0 /*CFunction<(CPointer<Unit>, CPointer<Unit>, size_t) -> CPointer<Unit>>*/
    // var memmove: CFunction<(CPointer<Unit>, CPointer<Unit>, Int) -> CPointer<Unit>> = 0 /*CFunction<(CPointer<Unit>, CPointer<Unit>, size_t) -> CPointer<Unit>>*/
    // var memcmp: CFunction<(CPointer<Unit>, CPointer<Unit>, Int) -> Int> = 0 /*CFunction<(CPointer<Unit>, CPointer<Unit>, size_t) -> Int>*/
    // typealias bs_t = bs_t
    // typealias L12_scale_info = L12_scale_info
    // typealias L12_subband_alloc_t = L12_subband_alloc_t
    // typealias L3_gr_info_t = L3_gr_info_t
    // typealias mp3dec_scratch_t = mp3dec_scratch_t
    fun bs_init(bs: CPointer<bs_t>, data: CPointer<UByte>, bytes: Int) {
        bs.value.buf = data
        bs.value.pos = 0
        bs.value.limit = bytes * 8

    }
    fun get_bits(bs: CPointer<bs_t>, n: Int): UInt {
        var next: UInt = 0u
        var cache: UInt = 0u
        var s: UInt = ((bs.value.pos and 7)).toUInt()
        var shl: Int = n + (s.toInt())
        var p: CPointer<UByte> = bs.value.buf + ((bs.value.pos shr 3))
        bs.value.pos += n
        if (bs.value.pos > bs.value.limit) return 0u
        next = p.value.toUInt() and ((255 shr (s.toInt()))).toUInt()
        p += 1
        while (true) {
            shl -= 8
            if (shl <= 0) break
            cache = cache or (next shl shl)
            next = p.value.toUInt()
            p += 1
        }
        return cache or (next shr (-shl))

    }
    fun hdr_valid(h: CPointer<UByte>): Int {
        return ((((((h[0] == (255.toUByte())) && ((((((h[1].toUInt()) and 240u)).toInt()) == 240) || (((((h[1].toUInt()) and 254u)).toInt()) == 226))) && ((((((h[1].toUInt()) shr 1) and 3u)).toInt()) != 0)) && (((((h[2].toUInt()) shr 4)).toInt()) != 15)) && ((((((h[2].toUInt()) shr 2) and 3u)).toInt()) != 3))).toInt().toInt()

    }
    fun hdr_compare(h1: CPointer<UByte>, h2: CPointer<UByte>): Int {
        return (((((hdr_valid(h2).toBool()) && ((((((h1[1].toUInt()) xor (h2[1].toUInt())) and 254u)).toInt()) == 0)) && ((((((h1[2].toUInt()) xor (h2[2].toUInt())) and 12u)).toInt()) == 0)) && (((((((((h1[2].toUInt()) and 240u)).toInt()) == 0)).toInt().toInt()) xor (((((((h2[2].toUInt()) and 240u)).toInt()) == 0)).toInt().toInt())) == 0))).toInt().toInt()

    }
    fun hdr_bitrate_kbps(h: CPointer<UByte>): UInt {
        var halfrate: Array2Array3Array15UByte = __STATIC_hdr_bitrate_kbps_halfrate
        return ((2 * (halfrate[((!(((h[1].toUInt()) and 8u) == 0u))).toInt().toInt()][(((((h[1].toUInt()) shr 1) and 3u)).toInt()) - 1][(((h[2].toUInt()) shr 4)).toInt()].toInt()))).toUInt()

    }
    fun hdr_sample_rate_hz(h: CPointer<UByte>): UInt {
        var g_hz: Array3UInt = __STATIC_hdr_sample_rate_hz_g_hz
        return (g_hz[((((h[2].toUInt()) shr 2) and 3u)).toInt()] shr (((((h[1].toUInt()) and 8u) == 0u)).toInt().toInt())) shr (((((h[1].toUInt()) and 16u) == 0u)).toInt().toInt())

    }
    fun hdr_frame_samples(h: CPointer<UByte>): UInt {
        return ((if (((((h[1].toUInt()) and 6u)).toInt()) == 6) 384 else (1152 shr (((((((h[1].toUInt()) and 14u)).toInt()) == 2)).toInt().toInt())))).toUInt()

    }
    fun hdr_frame_bytes(h: CPointer<UByte>, free_format_size: Int): Int {
        var frame_bytes: Int = ((((hdr_frame_samples(h) * hdr_bitrate_kbps(h)) * 125u) / hdr_sample_rate_hz(h))).toInt()
        if (((((h[1].toUInt()) and 6u)).toInt()) == 6) {
            frame_bytes = frame_bytes and ((3).inv())
        }
        return if (frame_bytes.toBool()) frame_bytes else free_format_size

    }
    fun hdr_padding(h: CPointer<UByte>): Int {
        return (if ((((h[2].toUInt()) and 2u)).toBool()) (if (((((h[1].toUInt()) and 6u)).toInt()) == 6) 4 else 1) else 0)

    }
    fun L12_subband_alloc_table(hdr: CPointer<UByte>, sci: CPointer<L12_scale_info>): CPointer<L12_subband_alloc_t> {
        var alloc: CPointer<L12_subband_alloc_t> = CPointer<L12_subband_alloc_t>(0)
        var mode: Int = ((((hdr[3].toUInt()) shr 6) and 3u)).toInt()
        var nbands: Int = 0
        var stereo_bands: Int = (if (mode == 3) 0 else (if (mode == 1) (((((((hdr[3].toUInt()) shr 4) and 3u) shl 2)).toInt()) + 4) else 32))
        if (((((hdr[1].toUInt()) and 6u)).toInt()) == 6) {
            var g_alloc_L1: CPointer<L12_subband_alloc_t> = __STATIC_L12_subband_alloc_table_g_alloc_L1
            alloc = CPointer<L12_subband_alloc_t>(g_alloc_L1.ptr)
            nbands = 32

        } else {
            if (((hdr[1].toUInt()) and 8u) == 0u) {
                var g_alloc_L2M2: CPointer<L12_subband_alloc_t> = __STATIC_L12_subband_alloc_table_g_alloc_L2M2
                alloc = CPointer<L12_subband_alloc_t>(g_alloc_L2M2.ptr)
                nbands = 30

            } else {
                var g_alloc_L2M1: CPointer<L12_subband_alloc_t> = __STATIC_L12_subband_alloc_table_g_alloc_L2M1
                var sample_rate_idx: Int = ((((hdr[2].toUInt()) shr 2) and 3u)).toInt()
                var kbps: UInt = hdr_bitrate_kbps(hdr) shr (((mode != 3)).toInt().toInt())
                if (kbps == 0u) {
                    kbps = 192u
                }
                alloc = CPointer<L12_subband_alloc_t>(g_alloc_L2M1.ptr)
                nbands = 27
                if ((kbps.toInt()) < 56) {
                    var g_alloc_L2M1_lowrate: CPointer<L12_subband_alloc_t> = __STATIC_L12_subband_alloc_table_g_alloc_L2M1_lowrate
                    alloc = CPointer<L12_subband_alloc_t>(g_alloc_L2M1_lowrate.ptr)
                    nbands = (if (sample_rate_idx == 2) 12 else 8)

                } else {
                    if (((kbps.toInt()) >= 96) && (sample_rate_idx != 1)) {
                        nbands = 30
                    }
                }

            }
        }
        sci.value.total_bands = nbands.toUByte()
        sci.value.stereo_bands = ((if (stereo_bands > nbands) nbands else stereo_bands)).toUByte()
        return alloc

    }
    fun L12_read_scalefactors(bs: CPointer<bs_t>, pba: CPointer<UByte>, scfcod: CPointer<UByte>, bands: Int, scf: FloatPointer) {
        var pba: CPointer<UByte> = pba // Mutating parameter
        var scf: FloatPointer = scf // Mutating parameter
        var g_deq_L12: Array54Float = __STATIC_L12_read_scalefactors_g_deq_L12
        var i: Int = 0
        var m: Int = 0
        i = 0
        while (i < bands) {
            var s: Float = 0f
            var ba: Int = run { val `-` = pba; pba = pba + 1; `-` }.value.toInt()
            var mask: Int = (if (ba.toBool()) (4 + ((19 shr (scfcod[i].toInt())) and 3)) else 0)
            m = 4
            while (m.toBool()) {
                if (((mask and m)).toBool()) {
                    var b: Int = get_bits(bs, 6).toInt()
                    s = g_deq_L12[((ba * 3) - 6) + (b % 3)] * ((((1 shl 21) shr (b / 3))).toFloat())

                }
                scf++.value = s
                m = m shr 1
            }
            i = i + 1
        }

    }
    fun L12_read_scale_info(hdr: CPointer<UByte>, bs: CPointer<bs_t>, sci: CPointer<L12_scale_info>) {
        var g_bitalloc_code_tab: CPointer<UByte> = __STATIC_L12_read_scale_info_g_bitalloc_code_tab
        var subband_alloc: CPointer<L12_subband_alloc_t> = L12_subband_alloc_table(hdr, sci)
        var i: Int = 0
        var k: Int = 0
        var ba_bits: Int = 0
        var ba_code_tab: CPointer<UByte> = CPointer<UByte>(g_bitalloc_code_tab.ptr)
        i = 0
        while (i < (sci.value.total_bands.toInt())) {
            var ba: UByte = 0u
            if (i == k) {
                k += (subband_alloc.value.band_count.toInt())
                ba_bits = subband_alloc.value.code_tab_width.toInt()
                ba_code_tab = CPointer<UByte>(((g_bitalloc_code_tab + (subband_alloc.value.tab_offset.toInt()))).ptr)
                subband_alloc += 1
            }
            ba = ba_code_tab[get_bits(bs, ba_bits).toInt()]
            sci.value.bitalloc[2 * i] = ba
            if (i < (sci.value.stereo_bands.toInt())) {
                ba = ba_code_tab[get_bits(bs, ba_bits).toInt()]
            }
            sci.value.bitalloc[(2 * i) + 1] = (if (sci.value.stereo_bands.toBool()) ba else (0.toUByte()))
            i += 1
        }
        i = 0
        while (i < (2 * (sci.value.total_bands.toInt()))) {
            sci.value.scfcod[i] = ((if (sci.value.bitalloc[i].toBool()) (if (((((hdr[1].toUInt()) and 6u)).toInt()) == 6) 2 else (get_bits(bs, 2).toInt())) else 6)).toUByte()
            i += 1
        }
        L12_read_scalefactors(bs, (CPointer<UByte>(sci.value.bitalloc.ptr)), (CPointer<UByte>(sci.value.scfcod.ptr)), ((((sci.value.total_bands.toUInt()) * 2u)).toInt()), (FloatPointer(sci.value.scf.ptr)))
        i = sci.value.stereo_bands.toInt()
        while (i < (sci.value.total_bands.toInt())) {
            sci.value.bitalloc[(2 * i) + 1] = 0.toUByte()
            i += 1
        }

    }
    fun L12_dequantize_granule(grbuf: FloatPointer, bs: CPointer<bs_t>, sci: CPointer<L12_scale_info>, group_size: Int): Int {
        var i: Int = 0
        var j: Int = 0
        var k: Int = 0
        var choff: Int = 576
        j = 0
        while (j < 4) {
            var dst: FloatPointer = grbuf + ((group_size * j))
            i = 0
            while (i < (2 * (sci.value.total_bands.toInt()))) {
                var ba: Int = sci.value.bitalloc[i].toInt()
                if (ba != 0) {
                    if (ba < 17) {
                        var half: Int = (1 shl (ba - 1)) - 1
                        k = 0
                        while (k < group_size) {
                            dst[k] = (((get_bits(bs, ba).toInt()) - half)).toFloat()
                            k = k + 1
                        }

                    } else {
                        var mod: UInt = (((2 shl (ba - 17)) + 1)).toUInt()
                        var code: UInt = get_bits(bs, (((mod.toInt()) + 2) - (((mod shr 3)).toInt())))
                        k = 0
                        while (k < group_size) {
                            dst[k] = ((((code % mod) - (mod / 2u))).toInt()).toFloat()
                            k++
                            code /= mod
                        }

                    }
                }
                dst += choff
                choff = 18 - choff
                i += 1
            }
            j += 1
        }
        return group_size * 4

    }
    fun L12_apply_scf_384(sci: CPointer<L12_scale_info>, scf: FloatPointer, dst: FloatPointer) {
        var scf: FloatPointer = scf // Mutating parameter
        var dst: FloatPointer = dst // Mutating parameter
        var i: Int = 0
        var k: Int = 0
        memcpy((CPointer<Unit>((((dst + 576) + (((((sci.value.stereo_bands.toUInt()) * 18u)).toInt())))).ptr)), (CPointer<Unit>(((dst + (((((sci.value.stereo_bands.toUInt()) * 18u)).toInt())))).ptr)), ((((((sci.value.total_bands.toUInt()) - (sci.value.stereo_bands.toUInt())) * 18u) * ((Float.SIZE_BYTES).toUInt()))).toInt()))
        i = 0
        while (i < (sci.value.total_bands.toInt())) {
            k = 0
            while (k < 12) {
                dst[k + 0] = dst[k + 0] * scf[0]
                dst[k + 576] = dst[k + 576] * scf[3]
                k += 1
            }
            i++
            dst += 18
            scf += 6
        }

    }
    fun L3_read_side_info(bs: CPointer<bs_t>, gr: CPointer<L3_gr_info_t>, hdr: CPointer<UByte>): Int {
        var gr: CPointer<L3_gr_info_t> = gr // Mutating parameter
        var g_scf_long: Array8Array23UByte = __STATIC_L3_read_side_info_g_scf_long
        var g_scf_short: Array8Array40UByte = __STATIC_L3_read_side_info_g_scf_short
        var g_scf_mixed: Array8Array40UByte = __STATIC_L3_read_side_info_g_scf_mixed
        var tables: UInt = 0u
        var scfsi: UInt = 0u
        var main_data_begin: Int = 0
        var part_23_sum: Int = 0
        var sr_idx: Int = (((((hdr[2].toUInt()) shr 2) and 3u) + (((((hdr[1].toUInt()) shr 3) and 1u) + (((hdr[1].toUInt()) shr 4) and 1u)) * 3u))).toInt()
        sr_idx -= (((sr_idx != 0)).toInt().toInt())
        var gr_count: Int = (if (((((hdr[3].toUInt()) and 192u)).toInt()) == 192) 1 else 2)
        if ((((hdr[1].toUInt()) and 8u)).toBool()) {
            gr_count = gr_count * 2
            main_data_begin = get_bits(bs, 9).toInt()
            scfsi = get_bits(bs, (7 + gr_count))
        } else {
            main_data_begin = ((get_bits(bs, (8 + gr_count)) shr gr_count)).toInt()
        }
        do0@do {
            if (((((hdr[3].toUInt()) and 192u)).toInt()) == 192) {
                scfsi = scfsi shl 4
            }
            gr.value.part_23_length = get_bits(bs, 12).toInt().toUShort()
            part_23_sum += (gr.value.part_23_length.toInt())
            gr.value.big_values = get_bits(bs, 9).toInt().toUShort()
            if (gr.value.big_values > (288.toInt().toUShort())) {
                return -1
            }
            gr.value.global_gain = get_bits(bs, 8).toUByte()
            gr.value.scalefac_compress = get_bits(bs, (if ((((hdr[1].toUInt()) and 8u)).toBool()) 4 else 9)).toInt().toUShort()
            gr.value.sfbtab = CPointer<UByte>(g_scf_long[sr_idx].ptr)
            gr.value.n_long_sfb = 22.toUByte()
            gr.value.n_short_sfb = 0.toUByte()
            if (get_bits(bs, 1).toBool()) {
                gr.value.block_type = get_bits(bs, 2).toUByte()
                if (!gr.value.block_type.toBool()) {
                    return -1
                }
                gr.value.mixed_block_flag = get_bits(bs, 1).toUByte()
                gr.value.region_count[0] = 7.toUByte()
                gr.value.region_count[1] = 255.toUByte()
                if (gr.value.block_type == (2.toUByte())) {
                    scfsi = scfsi and 3855u
                    if (!gr.value.mixed_block_flag.toBool()) {
                        gr.value.region_count[0] = 8.toUByte()
                        gr.value.sfbtab = CPointer<UByte>(g_scf_short[sr_idx].ptr)
                        gr.value.n_long_sfb = 0.toUByte()
                        gr.value.n_short_sfb = 39.toUByte()
                    } else {
                        gr.value.sfbtab = CPointer<UByte>(g_scf_mixed[sr_idx].ptr)
                        gr.value.n_long_sfb = ((if ((((hdr[1].toUInt()) and 8u)).toBool()) 8 else 6)).toUByte()
                        gr.value.n_short_sfb = 30.toUByte()
                    }
                }
                tables = get_bits(bs, 10)
                tables = tables shl 5
                gr.value.subblock_gain[0] = get_bits(bs, 3).toUByte()
                gr.value.subblock_gain[1] = get_bits(bs, 3).toUByte()
                gr.value.subblock_gain[2] = get_bits(bs, 3).toUByte()
            } else {
                gr.value.block_type = 0.toUByte()
                gr.value.mixed_block_flag = 0.toUByte()
                tables = get_bits(bs, 15)
                gr.value.region_count[0] = get_bits(bs, 4).toUByte()
                gr.value.region_count[1] = get_bits(bs, 3).toUByte()
                gr.value.region_count[2] = 255.toUByte()
            }
            gr.value.table_select[0] = ((tables shr 10)).toUByte()
            gr.value.table_select[1] = (((tables shr 5) and 31u)).toUByte()
            gr.value.table_select[2] = ((tables and 31u)).toUByte()
            gr.value.preflag = ((if ((((hdr[1].toUInt()) and 8u)).toBool()) get_bits(bs, 1) else (((gr.value.scalefac_compress >= (500.toInt().toUShort()))).toInt().toUInt()))).toUByte()
            gr.value.scalefac_scale = get_bits(bs, 1).toUByte()
            gr.value.count1_table = get_bits(bs, 1).toUByte()
            gr.value.scfsi = (((scfsi shr 12) and 15u)).toUByte()
            scfsi = scfsi shl 4
            gr += 1
        } while (((--gr_count)).toBool())
        if ((part_23_sum + bs.value.pos) > (bs.value.limit + (main_data_begin * 8))) {
            return -1
        }
        return main_data_begin

    }
    fun L3_read_scalefactors(scf: CPointer<UByte>, ist_pos: CPointer<UByte>, scf_size: CPointer<UByte>, scf_count: CPointer<UByte>, bitbuf: CPointer<bs_t>, scfsi: Int) {
        var scf: CPointer<UByte> = scf // Mutating parameter
        var ist_pos: CPointer<UByte> = ist_pos // Mutating parameter
        var scfsi: Int = scfsi // Mutating parameter
        var i: Int = 0
        var k: Int = 0
        i = 0
        while ((i < 4) && (scf_count[i].toBool())) {
            var cnt: Int = scf_count[i].toInt()
            if (((scfsi and 8)).toBool()) {
                memcpy((CPointer<Unit>(scf.ptr)), (CPointer<Unit>(ist_pos.ptr)), cnt)
            } else {
                var bits: Int = scf_size[i].toInt()
                if (bits == 0) {
                    memset((CPointer<Unit>(scf.ptr)), 0, cnt)
                    memset((CPointer<Unit>(ist_pos.ptr)), 0, cnt)
                } else {
                    var max_scf: Int = ((if (scfsi < 0) ((((1 shl bits) - 1)).toLong()) else -1L)).toInt()
                    k = 0
                    while (k < cnt) {
                        var s: Int = get_bits(bitbuf, bits).toInt()
                        ist_pos[k] = ((if (s == max_scf) -1L else (s.toLong()))).toUByte()
                        scf[k] = s.toUByte()
                        k += 1
                    }

                }

            }
            ist_pos += cnt
            scf += cnt

            i++
            scfsi *= 2
        }
        scf[0] = 0u
        scf[1] = 0u
        scf[2] = 0u

    }
    fun L3_ldexp_q2(y: Float, exp_q2: Int): Float {
        var y: Float = y // Mutating parameter
        var exp_q2: Int = exp_q2 // Mutating parameter
        var g_expfrac: Array4Float = __STATIC_L3_ldexp_q2_g_expfrac
        var e: Int = 0
        do0@do {
            e = (if ((30 * 4) > exp_q2) exp_q2 else (30 * 4))
            y *= (g_expfrac[e and 3] * ((((1 shl 30) shr (e shr 2))).toFloat()))
        } while ((run { val `-` = exp_q2 - e; exp_q2 = `-`; `-` }) > 0)
        return y

    }
    fun L3_decode_scalefactors(hdr: CPointer<UByte>, ist_pos: CPointer<UByte>, bs: CPointer<bs_t>, gr: CPointer<L3_gr_info_t>, scf: FloatPointer, ch: Int) {
        var g_scf_partitions: Array3Array28UByte = __STATIC_L3_decode_scalefactors_g_scf_partitions
        var scf_partition: CPointer<UByte> = CPointer<UByte>(g_scf_partitions[(((!(!gr.value.n_short_sfb.toBool()))).toInt().toInt()) + (((!gr.value.n_long_sfb.toBool())).toInt().toInt())].ptr)
        var scf_size: Array4UByte = Array4UByte(fixedArrayOfUByte("\u0000", size = 4).ptr)
        var iscf: Array40UByte = Array40UByte(fixedArrayOfUByte("\u0000", size = 40).ptr)
        var i: Int = 0
        var scf_shift: Int = (((gr.value.scalefac_scale.toUInt()) + 1u)).toInt()
        var gain_exp: Int = 0
        var scfsi: Int = gr.value.scfsi.toInt()
        var gain: Float = 0f
        if ((((hdr[1].toUInt()) and 8u)).toBool()) {
            var g_scfc_decode: Array16UByte = __STATIC_L3_decode_scalefactors_g_scfc_decode
            var part: Int = g_scfc_decode[gr.value.scalefac_compress.toInt()].toInt()
            scf_size[0] = ((part shr 2)).toUByte()
            scf_size[1] = scf_size[0]
            scf_size[2] = ((part and 3)).toUByte()
            scf_size[3] = scf_size[2]

        } else {
            var g_mod: Array24UByte = __STATIC_L3_decode_scalefactors_g_mod
            var k: Int = 0
            var modprod: Int = 0
            var sfc: Int = 0
            var ist: Int = ((((((hdr[3].toUInt()) and 16u)).toBool()) && (ch.toBool()))).toInt().toInt()
            sfc = (((gr.value.scalefac_compress.toUInt()) shr ist)).toInt()
            k = (ist * 3) * 4
            while (sfc >= 0) {
                modprod = 1
                i = 3
                while (i >= 0) {
                    scf_size[i] = (((sfc / modprod) % (g_mod[k + i].toInt()))).toUByte()
                    modprod *= (g_mod[k + i].toInt())
                    i -= 1
                }
                sfc -= modprod
                k += 4
            }
            scf_partition += k
            scfsi = -16

        }
        L3_read_scalefactors((CPointer<UByte>(iscf.ptr)), ist_pos, (CPointer<UByte>(scf_size.ptr)), scf_partition, bs, scfsi)
        if (gr.value.n_short_sfb.toBool()) {
            var sh: Int = 3 - scf_shift
            i = 0
            while (i < (gr.value.n_short_sfb.toInt())) {
                iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 0] = (((iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 0].toUInt()) + ((gr.value.subblock_gain[0].toUInt()) shl sh))).toUByte()
                iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 1] = (((iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 1].toUInt()) + ((gr.value.subblock_gain[1].toUInt()) shl sh))).toUByte()
                iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 2] = (((iscf[((((gr.value.n_long_sfb.toUInt()) + (i.toUInt()))).toInt()) + 2].toUInt()) + ((gr.value.subblock_gain[2].toUInt()) shl sh))).toUByte()
                i += 3
            }

        } else {
            if (gr.value.preflag.toBool()) {
                var g_preamp: Array10UByte = __STATIC_L3_decode_scalefactors_g_preamp
                i = 0
                while (i < 10) {
                    iscf[11 + i] = (((iscf[11 + i].toUInt()) + (g_preamp[i].toUInt()))).toUByte()
                    i += 1
                }

            }
        }
        gain_exp = (((((gr.value.global_gain.toUInt()) + (((-1L * 4L)).toUInt()))).toInt()) - 210) - (if (((((hdr[3].toUInt()) and 224u)).toInt()) == 96) 2 else 0)
        gain = L3_ldexp_q2((((1 shl (((((((255L + (-1L * 4L)) - 210L) + 3L) and ((((3).inv())).toLong())) / 4L)).toInt()))).toFloat()), (((((((255L + (-1L * 4L)) - 210L) + 3L) and ((((3).inv())).toLong())) - (gain_exp.toLong()))).toInt()))
        i = 0
        while (i < ((((gr.value.n_long_sfb.toUInt()) + (gr.value.n_short_sfb.toUInt()))).toInt())) {
            scf[i] = L3_ldexp_q2(gain, ((((iscf[i].toUInt()) shl scf_shift)).toInt()))
            i += 1
        }

    }
    var g_pow43: Array145Float /*static*/ = Array145Float(fixedArrayOfFloat(0f, -1f, -2.519842f, -4.326749f, -6.349604f, -8.54988f, -10.902724f, -13.390518f, -16f, -18.720754f, -21.544347f, -24.463781f, -27.473142f, -30.567351f, -33.741992f, -36.993181f, 0f, 1f, 2.519842f, 4.326749f, 6.349604f, 8.54988f, 10.902724f, 13.390518f, 16f, 18.720754f, 21.544347f, 24.463781f, 27.473142f, 30.567351f, 33.741992f, 36.993181f, 40.317474f, 43.711787f, 47.173345f, 50.699631f, 54.288352f, 57.937408f, 61.644865f, 65.408941f, 69.227979f, 73.100443f, 77.024898f, 81f, 85.024491f, 89.097188f, 93.216975f, 97.3828f, 101.593667f, 105.848633f, 110.146801f, 114.487321f, 118.869381f, 123.292209f, 127.755065f, 132.257246f, 136.798076f, 141.376907f, 145.993119f, 150.646117f, 155.335327f, 160.060199f, 164.820202f, 169.614826f, 174.443577f, 179.30598f, 184.201575f, 189.129918f, 194.09058f, 199.083145f, 204.10721f, 209.162385f, 214.248292f, 219.364564f, 224.510845f, 229.686789f, 234.892058f, 240.126328f, 245.38928f, 250.680604f, 256f, 261.347174f, 266.721841f, 272.123723f, 277.552547f, 283.008049f, 288.489971f, 293.99806f, 299.532071f, 305.091761f, 310.676898f, 316.287249f, 321.922592f, 327.582707f, 333.267377f, 338.976394f, 344.70955f, 350.466646f, 356.247482f, 362.051866f, 367.879608f, 373.730522f, 379.604427f, 385.501143f, 391.420496f, 397.362314f, 403.326427f, 409.312672f, 415.320884f, 421.350905f, 427.402579f, 433.47575f, 439.570269f, 445.685987f, 451.822757f, 457.980436f, 464.158883f, 470.35796f, 476.57753f, 482.817459f, 489.077615f, 495.357868f, 501.65809f, 507.978156f, 514.317941f, 520.677324f, 527.056184f, 533.454404f, 539.871867f, 546.308458f, 552.764065f, 559.238575f, 565.731879f, 572.24387f, 578.77444f, 585.323483f, 591.890898f, 598.476581f, 605.080431f, 611.702349f, 618.342238f, 625f, 631.67554f, 638.368763f, 645.079578f).ptr)
    fun L3_pow_43(x: Int): Float {
        var x: Int = x // Mutating parameter
        var frac: Float = 0f
        var sign: Int = 0
        var mult: Int = 256
        if (x < 129) {
            return g_pow43[16 + x]
        }
        if (x < 1024) {
            mult = 16
            x = x shl 3
        }
        sign = (2 * x) and 64
        frac = ((((x and 63) - sign)).toFloat()) / ((((x and ((63).inv())) + sign)).toFloat())
        return (g_pow43[16 + ((x + sign) shr 6)] * (1f + (frac * ((4f / 3f) + (frac * (2f / 9f)))))) * (mult.toFloat())

    }
    fun L3_huffman(dst: FloatPointer, bs: CPointer<bs_t>, gr_info: CPointer<L3_gr_info_t>, scf: FloatPointer, layer3gr_limit: Int) {
        var dst: FloatPointer = dst // Mutating parameter
        var scf: FloatPointer = scf // Mutating parameter
        var tabs: CPointer<Short> = __STATIC_L3_huffman_tabs
        var tab32: CPointer<UByte> = __STATIC_L3_huffman_tab32
        var tab33: CPointer<UByte> = __STATIC_L3_huffman_tab33
        var tabindex: Array32Short = __STATIC_L3_huffman_tabindex
        var g_linbits: CPointer<UByte> = __STATIC_L3_huffman_g_linbits
        var one: Float = 0f
        var ireg: Int = 0
        var big_val_cnt: Int = gr_info.value.big_values.toInt()
        var sfb: CPointer<UByte> = gr_info.value.sfbtab
        var bs_next_ptr: CPointer<UByte> = bs.value.buf + ((bs.value.pos / 8))
        var bs_cache: UInt = (((((((bs_next_ptr[0].toUInt()) * 256u) + (bs_next_ptr[1].toUInt())) * 256u) + (bs_next_ptr[2].toUInt())) * 256u) + (bs_next_ptr[3].toUInt())) shl (bs.value.pos and 7)
        var pairs_to_decode: Int = 0
        var np: Int = 0
        var bs_sh: Int = (bs.value.pos and 7) - 8
        bs_next_ptr += 4
        while (big_val_cnt > 0) {
            var tab_num: Int = gr_info.value.table_select[ireg].toInt()
            var sfb_cnt: Int = gr_info.value.region_count[ireg++].toInt()
            var codebook: CPointer<Short> = CPointer<Short>(((tabs + (tabindex[tab_num].toInt()))).ptr)
            var linbits: Int = g_linbits[tab_num].toInt()
            if (linbits.toBool()) {
                do0@do {
                    np = (sfb.value.toUInt() / 2u).toInt()
                    sfb += 1
                    pairs_to_decode = if (big_val_cnt > np) np else big_val_cnt
                    one = scf++.value
                    do1@do {
                        var j: Int = 0
                        var w: Int = 5
                        var leaf: Int = codebook[((bs_cache shr (32 - w))).toInt()].toInt()
                        while (leaf < 0) {
                            bs_cache = bs_cache shl w
                            bs_sh += w
                            w = leaf and 7
                            leaf = codebook[(((bs_cache shr (32 - w))).toInt()) - (leaf shr 3)].toInt()
                        }
                        bs_cache = bs_cache shl (leaf shr 8)
                        bs_sh += (leaf shr 8)
                        j = 0
                        while (j < 2) {
                            var lsb: Int = leaf and 15
                            if (lsb == 15) {
                                lsb += (((bs_cache shr (32 - linbits))).toInt())
                                bs_cache = bs_cache shl linbits
                                bs_sh += linbits
                                while (bs_sh >= 0) {
                                    bs_cache = bs_cache or ((run { val `-` = bs_next_ptr; bs_next_ptr = bs_next_ptr + 1; `-` }.value.toUInt()) shl bs_sh)
                                    bs_sh -= 8
                                }
                                dst.value = (one * L3_pow_43(lsb)) * (((if ((bs_cache.toInt()) < 0) -1L else 1L)).toFloat())
                            } else {
                                dst.value = g_pow43[(16 + lsb) - (16 * (((bs_cache shr 31)).toInt()))] * one
                            }
                            bs_cache = bs_cache shl (if (lsb.toBool()) 1 else 0)
                            bs_sh += (if (lsb.toBool()) 1 else 0)
                            j++
                            dst++
                            leaf = leaf shr 4
                        }
                        while (bs_sh >= 0) {
                            bs_cache = bs_cache or (bs_next_ptr.value.toUInt() shl bs_sh)
                            bs_next_ptr += 1
                            bs_sh -= 8
                        }

                    } while (((--pairs_to_decode)).toBool())
                } while (((run { val `-` = big_val_cnt - np; big_val_cnt = `-`; `-` }) > 0) && ((--sfb_cnt) >= 0))
            } else {
                do0@do {
                    np = (((sfb.value.toUInt()) / 2u)).toInt()
                    sfb += 1
                    pairs_to_decode = if (big_val_cnt > np) np else big_val_cnt
                    one = scf++.value
                    do1@do {
                        var j: Int = 0
                        var w: Int = 5
                        var leaf: Int = codebook[((bs_cache shr (32 - w))).toInt()].toInt()
                        while (leaf < 0) {
                            bs_cache = bs_cache shl w
                            bs_sh += w
                            w = leaf and 7
                            leaf = codebook[(((bs_cache shr (32 - w))).toInt()) - (leaf shr 3)].toInt()
                        }
                        bs_cache = bs_cache shl (leaf shr 8)
                        bs_sh += (leaf shr 8)
                        j = 0
                        while (j < 2) {
                            var lsb: Int = leaf and 15
                            dst.value = g_pow43[(16 + lsb) - (16 * (((bs_cache shr 31)).toInt()))] * one
                            bs_cache = bs_cache shl (if (lsb.toBool()) 1 else 0)
                            bs_sh += (if (lsb.toBool()) 1 else 0)
                            j++
                            dst++
                            leaf = leaf shr 4
                        }
                        while (bs_sh >= 0) {
                            bs_cache = bs_cache or (bs_next_ptr.value.toUInt() shl bs_sh)
                            bs_next_ptr += 1
                            bs_sh -= 8
                        }

                    } while (((--pairs_to_decode)).toBool())
                } while (((run { val `-` = big_val_cnt - np; big_val_cnt = `-`; `-` }) > 0) && ((--sfb_cnt) >= 0))
            }

        }
        np = 1 - big_val_cnt
        while0@while (1.toBool()) {
            val __oldPos0 = STACK_PTR
            try {
                var codebook_count1: CPointer<UByte> = CPointer<UByte>(((if (gr_info.value.count1_table.toBool()) tab33 else tab32)).ptr)
                var leaf: Int = codebook_count1[((bs_cache shr (32 - 4))).toInt()].toInt()
                if ((leaf and 8) == 0) {
                    leaf = codebook_count1[(leaf shr 3) + ((((bs_cache shl 4) shr (32 - (leaf and 3)))).toInt())].toInt()
                }
                bs_cache = bs_cache shl (leaf and 7)
                bs_sh = bs_sh + (leaf and 7)
                if (((((bs_next_ptr.minusPtrUByte(bs.value.buf)) * 8) - 24) + bs_sh) > layer3gr_limit) {
                    break@while0
                }
                if ((--np) == 0) {
                    np = (((run { val `-` = sfb; sfb = sfb + 1; `-` }.value.toUInt()) / 2u)).toInt()
                    if (np == 0) {
                        break@while0
                    }
                    one = scf++.value
                }
                if (((leaf and (128 shr 0))).toBool()) {
                    dst[0] = (if ((bs_cache.toInt()) < 0) (-one) else one)
                    bs_cache = bs_cache shl 1
                    bs_sh += 1
                }
                if (((leaf and (128 shr 1))).toBool()) {
                    dst[1] = (if ((bs_cache.toInt()) < 0) (-one) else one)
                    bs_cache = bs_cache shl 1
                    bs_sh += 1
                }
                if ((--np) == 0) {
                    np = (((sfb.value.toUInt()) / 2u)).toInt()
                    sfb = sfb + 1
                    if (np == 0) {
                        break@while0
                    }
                    one = scf++.value
                }
                if (((leaf and (128 shr 2))).toBool()) {
                    dst[2] = (if ((bs_cache.toInt()) < 0) (-one) else one)
                    bs_cache = bs_cache shl 1
                    bs_sh = bs_sh + 1
                }
                if (((leaf and (128 shr 3))).toBool()) {
                    dst[3] = (if ((bs_cache.toInt()) < 0) (-one) else one)
                    bs_cache = bs_cache shl 1
                    bs_sh = bs_sh + 1
                }
                while (bs_sh >= 0) {
                    bs_cache = bs_cache or ((run { val `-` = bs_next_ptr; bs_next_ptr = bs_next_ptr + 1; `-` }.value.toUInt()) shl bs_sh)
                    bs_sh = bs_sh - 8
                }

            }
            finally {
                STACK_PTR = __oldPos0
            }
            dst = dst + 4
        }
        bs.value.pos = layer3gr_limit

    }
    fun L3_midside_stereo(left: FloatPointer, n: Int) {
        var i: Int = 0
        var right: FloatPointer = left + 576
        while (i < n) {
            var a: Float = left[i]
            var b: Float = right[i]
            left[i] = a + b
            right[i] = a - b
            i = i + 1
        }

    }
    fun L3_intensity_stereo_band(left: FloatPointer, n: Int, kl: Float, kr: Float) {
        var i: Int = 0
        i = 0
        while (i < n) {
            left[i + 576] = left[i] * kr
            left[i] = left[i] * kl
            i = i + 1
        }

    }
    fun L3_stereo_top_band(right: FloatPointer, sfb: CPointer<UByte>, nbands: Int, max_band: Array3Int) {
        var right: FloatPointer = right // Mutating parameter
        var i: Int = 0
        var k: Int = 0
        max_band[0] = run { val `-` = run { val `-` = -1; max_band[2] = `-`; `-` }; max_band[1] = `-`; `-` }
        i = 0
        while0@while (i < nbands) {
            k = 0
            while1@while (k < (sfb[i].toInt())) {
                if ((right[k] != 0f) || (right[k + 1] != 0f)) {
                    max_band[i % 3] = i
                    break@while1
                }
                k = k + 2
            }
            right = right + (sfb[i].toInt())
            i = i + 1
        }

    }
    fun L3_stereo_process(left: FloatPointer, ist_pos: CPointer<UByte>, sfb: CPointer<UByte>, hdr: CPointer<UByte>, max_band: Array3Int, mpeg2_sh: Int) {
        var left: FloatPointer = left // Mutating parameter
        var g_pan: Array14Float = __STATIC_L3_stereo_process_g_pan
        var i: UInt = 0u
        var max_pos: UInt = ((if ((((hdr[1].toUInt()) and 8u)).toBool()) 7 else 64)).toUInt()
        i = 0u
        while (sfb[i.toInt()].toBool()) {
            var ipos: UInt = ist_pos[i.toInt()].toUInt()
            if (((i.toInt()) > max_band[((i % 3u)).toInt()]) && (ipos < max_pos)) {
                var kl: Float = 0f
                var kr: Float = 0f
                var s: Float = (if ((((hdr[3].toUInt()) and 32u)).toBool()) 1.41421356f else 1f)
                if ((((hdr[1].toUInt()) and 8u)).toBool()) {
                    kl = g_pan[2 * (ipos.toInt())]
                    kr = g_pan[(2 * (ipos.toInt())) + 1]
                } else {
                    kl = 1f
                    kr = L3_ldexp_q2(1f, ((((ipos.toInt()) + 1) shr 1) shl mpeg2_sh))
                    if (((ipos and 1u)).toBool()) {
                        kl = kr
                        kr = 1f
                    }
                }
                L3_intensity_stereo_band(left, (sfb[i.toInt()].toInt()), (kl * s), (kr * s))

            } else {
                if ((((hdr[3].toUInt()) and 32u)).toBool()) {
                    L3_midside_stereo(left, (sfb[i.toInt()].toInt()))
                }
            }
            left = left + (sfb[i.toInt()].toInt())
            i = i + 1u
        }

    }
    fun L3_intensity_stereo(left: FloatPointer, ist_pos: CPointer<UByte>, gr: CPointer<L3_gr_info_t>, hdr: CPointer<UByte>) {
        var max_band: Array3Int = Array3Int(fixedArrayOfInt(0, size = 3).ptr)
        var n_sfb: Int = (((gr.value.n_long_sfb.toUInt()) + (gr.value.n_short_sfb.toUInt()))).toInt()
        var i: Int = 0
        var max_blocks: Int = (if (gr.value.n_short_sfb.toBool()) 3 else 1)
        L3_stereo_top_band((left + 576), gr.value.sfbtab, n_sfb, max_band)
        if (gr.value.n_long_sfb.toBool()) {
            max_band[0] = run { val `-` = run { val `-` = (if ((if (max_band[0] < max_band[1]) max_band[1] else max_band[0]) < max_band[2]) max_band[2] else (if (max_band[0] < max_band[1]) max_band[1] else max_band[0])); max_band[2] = `-`; `-` }; max_band[1] = `-`; `-` }
        }
        i = 0
        while (i < max_blocks) {
            var default_pos: Int = (if ((((hdr[1].toUInt()) and 8u)).toBool()) 3 else 0)
            var itop: Int = (n_sfb - max_blocks) + i
            var prev: Int = itop - max_blocks
            ist_pos[itop] = ((if (max_band[i] >= prev) default_pos else (ist_pos[prev].toInt()))).toUByte()
            i++
        }
        L3_stereo_process(left, ist_pos, gr.value.sfbtab, hdr, max_band, ((((gr[1].scalefac_compress.toUInt()) and 1u)).toInt()))

    }
    fun L3_reorder(grbuf: FloatPointer, scratch: FloatPointer, sfb: CPointer<UByte>) {
        var sfb: CPointer<UByte> = sfb // Mutating parameter
        var i: Int = 0
        var len: Int = 0
        var src: FloatPointer = grbuf
        var dst: FloatPointer = scratch
        while (true) {
            len = sfb.value.toInt()
            if (len == 0) break
            i = 0
            while (i < len) {
                dst++.value = src[0 * len]
                dst++.value = src[1 * len]
                dst++.value = src[2 * len]
                i++
                src++
            }
            sfb += 3
            src += (2 * len)
        }
        memcpy((CPointer<Unit>(grbuf.ptr)), (CPointer<Unit>(scratch.ptr)), ((dst - scratch) * Float.SIZE_BYTES))

    }
    fun L3_antialias(grbuf: FloatPointer, nbands: Int) {
        var grbuf: FloatPointer = grbuf // Mutating parameter
        var nbands: Int = nbands // Mutating parameter
        var g_aa: Array2Array8Float = __STATIC_L3_antialias_g_aa
        while (nbands > 0) {
            var i: Int = 0
            while (i < 8) {
                var u: Float = grbuf[18 + i]
                var d: Float = grbuf[17 - i]
                grbuf[18 + i] = (u * g_aa[0][i]) - (d * g_aa[1][i])
                grbuf[17 - i] = (u * g_aa[1][i]) + (d * g_aa[0][i])
                i++
            }
            nbands--
            grbuf += 18
        }

    }
    fun L3_dct3_9(y: FloatPointer) {
        var s0: Float = 0f
        var s1: Float = 0f
        var s2: Float = 0f
        var s3: Float = 0f
        var s4: Float = 0f
        var s5: Float = 0f
        var s6: Float = 0f
        var s7: Float = 0f
        var s8: Float = 0f
        var t0: Float = 0f
        var t2: Float = 0f
        var t4: Float = 0f
        s0 = y[0]
        s2 = y[2]
        s4 = y[4]
        s6 = y[6]
        s8 = y[8]
        t0 = s0 + (s6 * 0.5f)
        s0 = s0 - s6
        t4 = (s4 + s2) * 0.93969262f
        t2 = (s8 + s2) * 0.76604444f
        s6 = (s4 - s8) * 0.17364818f
        s4 = s4 + (s8 - s2)
        s2 = s0 - (s4 * 0.5f)
        y[4] = s4 + s0
        s8 = (t0 - t2) + s6
        s0 = (t0 - t4) + t2
        s4 = (t0 + t4) - s6
        s1 = y[1]
        s3 = y[3]
        s5 = y[5]
        s7 = y[7]
        s3 = s3 * 0.8660254f
        t0 = (s5 + s1) * 0.98480775f
        t4 = (s5 - s7) * 0.34202014f
        t2 = (s1 + s7) * 0.64278761f
        s1 = ((s1 - s5) - s7) * 0.8660254f
        s5 = (t0 - s3) - t2
        s7 = (t4 - s3) - t0
        s3 = (t4 + s3) - t2
        y[0] = s4 - s7
        y[1] = s2 + s1
        y[2] = s0 - s3
        y[3] = s8 + s5
        y[5] = s8 - s5
        y[6] = s0 + s3
        y[7] = s2 - s1
        y[8] = s4 + s7

    }
    fun L3_imdct36(grbuf: FloatPointer, overlap: FloatPointer, window: FloatPointer, nbands: Int) {
        var grbuf: FloatPointer = grbuf // Mutating parameter
        var overlap: FloatPointer = overlap // Mutating parameter
        var i: Int = 0
        var j: Int = 0
        var g_twid9 = __STATIC_L3_imdct36_g_twid9
        j = 0
        while (j < nbands) {
            var co: Array9Float = Array9Float(fixedArrayOfFloat(0f, size = 9).ptr)
            var si: Array9Float = Array9Float(fixedArrayOfFloat(0f, size = 9).ptr)
            co[0] = -grbuf[0]
            si[0] = grbuf[17]
            i = 0
            while (i < 4) {
                si[8 - (2 * i)] = grbuf[(4 * i) + 1] - grbuf[(4 * i) + 2]
                co[1 + (2 * i)] = grbuf[(4 * i) + 1] + grbuf[(4 * i) + 2]
                si[7 - (2 * i)] = grbuf[(4 * i) + 4] - grbuf[(4 * i) + 3]
                co[2 + (2 * i)] = -(grbuf[(4 * i) + 3] + grbuf[(4 * i) + 4])
                i++
            }
            L3_dct3_9((FloatPointer(co.ptr)))
            L3_dct3_9((FloatPointer(si.ptr)))
            si[1] = -si[1]
            si[3] = -si[3]
            si[5] = -si[5]
            si[7] = -si[7]
            i = 0
            while (i < 9) {
                var ovl: Float = overlap[i]
                var sum: Float = (co[i] * g_twid9[9 + i]) + (si[i] * g_twid9[0 + i])
                overlap[i] = (co[i] * g_twid9[0 + i]) - (si[i] * g_twid9[9 + i])
                grbuf[i] = (ovl * window[0 + i]) - (sum * window[9 + i])
                grbuf[17 - i] = (ovl * window[9 + i]) + (sum * window[0 + i])
                i++
            }

            j++
            grbuf += 18
            overlap += 9
        }

    }
    fun L3_idct3(x0: Float, x1: Float, x2: Float, dst: FloatArray) {
        var m1: Float = x1 * 0.8660254f
        var a1: Float = x0 - (x2 * 0.5f)
        dst[1] = x0 + x2
        dst[0] = a1 + m1
        dst[2] = a1 - m1
    }
    private val temp1F3 = FloatArray(3)
    private val temp2F3 = FloatArray(3)
    fun L3_imdct12(x: FloatPointer, dst: FloatPointer, overlap: FloatPointer) {
        var g_twid3: Array6Float = __STATIC_L3_imdct12_g_twid3
        var co = temp1F3
        var si = temp2F3
        var i: Int = 0
        L3_idct3((-x[0]), (x[6] + x[3]), (x[12] + x[9]), co)
        L3_idct3(x[15], (x[12] - x[9]), (x[6] - x[3]), si)
        si[1] = -si[1]
        i = 0
        while (i < 3) {
            var ovl: Float = overlap[i]
            var sum: Float = (co[i] * g_twid3[3 + i]) + (si[i] * g_twid3[0 + i])
            overlap[i] = (co[i] * g_twid3[0 + i]) - (si[i] * g_twid3[3 + i])
            dst[i] = (ovl * g_twid3[2 - i]) - (sum * g_twid3[5 - i])
            dst[5 - i] = (ovl * g_twid3[5 - i]) + (sum * g_twid3[2 - i])
            i++
        }

    }
    fun L3_imdct_short(grbuf: FloatPointer, overlap: FloatPointer, nbands: Int) {
        var grbuf: FloatPointer = grbuf // Mutating parameter
        var overlap: FloatPointer = overlap // Mutating parameter
        var nbands: Int = nbands // Mutating parameter
        while (nbands > 0) {
            var tmp: Array18Float = Array18Float(fixedArrayOfFloat(0f, size = 18).ptr)
            memcpy((CPointer<Unit>(tmp.ptr)), (CPointer<Unit>(grbuf.ptr)), 72)
            memcpy((CPointer<Unit>(grbuf.ptr)), (CPointer<Unit>(overlap.ptr)), (6 * Float.SIZE_BYTES))
            L3_imdct12((FloatPointer(tmp.ptr)), (grbuf + 6), (overlap + 6))
            L3_imdct12((tmp + 1), (grbuf + 12), (overlap + 6))
            L3_imdct12((tmp + 2), overlap, (overlap + 6))
            nbands--
            overlap += 9
            grbuf += 18
        }

    }
    fun L3_change_sign(grbuf: FloatPointer) {
        var grbuf: FloatPointer = grbuf // Mutating parameter
        var b: Int = 0
        var i: Int = 0
        run { run { val `-` = 0; b = `-`; `-` }; run { val `-` = grbuf + 18; grbuf = `-`; `-` } }
        while (b < 32) {
            i = 1
            while (i < 18) {
                grbuf[i] = -grbuf[i]
                i += 2
            }
            b += 2
            grbuf += 36
        }

    }
    fun L3_imdct_gr(grbuf: FloatPointer, overlap: FloatPointer, block_type: UInt, n_long_bands: UInt) {
        var grbuf: FloatPointer = grbuf // Mutating parameter
        var overlap: FloatPointer = overlap // Mutating parameter
        var g_mdct_window: Array2Array18Float = __STATIC_L3_imdct_gr_g_mdct_window
        if (n_long_bands.toBool()) {
            L3_imdct36(grbuf, overlap, (FloatPointer(g_mdct_window[0].ptr)), (n_long_bands.toInt()))
            grbuf += ((18 * (n_long_bands.toInt())))
            overlap += ((9 * (n_long_bands.toInt())))
        }
        if ((block_type.toInt()) == 2) {
            L3_imdct_short(grbuf, overlap, (32 - (n_long_bands.toInt())))
        } else {
            L3_imdct36(grbuf, overlap, (FloatPointer(g_mdct_window[(((block_type.toInt()) == 3)).toInt().toInt()].ptr)), (32 - (n_long_bands.toInt())))
        }

    }
    fun L3_save_reservoir(h: CPointer<mp3dec_t>, s: CPointer<mp3dec_scratch_t>) {
        var pos: Int = (s.value.bs.pos + 7) / 8
        var remains: Int = (s.value.bs.limit / 8) - pos
        if (remains > 511) {
            pos += (remains - 511)
            remains = 511
        }
        if (remains > 0) {
            memmove((CPointer<Unit>(h.value.reserv_buf.ptr)), (CPointer<Unit>(((s.value.maindata + pos)).ptr)), remains)
        }
        h.value.reserv = remains

    }
    fun L3_restore_reservoir(h: CPointer<mp3dec_t>, bs: CPointer<bs_t>, s: CPointer<mp3dec_scratch_t>, main_data_begin: Int): Int {
        var frame_bytes: Int = (bs.value.limit - bs.value.pos) / 8
        var bytes_have: Int = (if (h.value.reserv > main_data_begin) main_data_begin else h.value.reserv)
        memcpy((CPointer<Unit>(s.value.maindata.ptr)), (CPointer<Unit>(((h.value.reserv_buf + ((if (0 < (h.value.reserv - main_data_begin)) (h.value.reserv - main_data_begin) else 0)))).ptr)), (if (h.value.reserv > main_data_begin) main_data_begin else h.value.reserv))
        memcpy((CPointer<Unit>(((s.value.maindata + bytes_have)).ptr)), (CPointer<Unit>(((bs.value.buf + ((bs.value.pos / 8)))).ptr)), frame_bytes)
        bs_init((CPointer<bs_t>((s).ptr + mp3dec_scratch_t__OFFSET_bs)), (CPointer<UByte>(s.value.maindata.ptr)), (bytes_have + frame_bytes))
        return ((h.value.reserv >= main_data_begin)).toInt().toInt()

    }
    fun L3_decode(h: CPointer<mp3dec_t>, s: CPointer<mp3dec_scratch_t>, gr_info: CPointer<L3_gr_info_t>, nch: Int) {
        var gr_info: CPointer<L3_gr_info_t> = gr_info // Mutating parameter
        var ch: Int = 0
        ch = 0
        while (ch < nch) {
            var layer3gr_limit: Int = s.value.bs.pos + (gr_info[ch].part_23_length.toInt())
            L3_decode_scalefactors((CPointer<UByte>(h.value.header.ptr)), (CPointer<UByte>(s.value.ist_pos[ch].ptr)), (CPointer<bs_t>((s).ptr + mp3dec_scratch_t__OFFSET_bs)), (gr_info + ch), (FloatPointer(s.value.scf.ptr)), ch)
            L3_huffman((FloatPointer(s.value.grbuf[ch].ptr)), (CPointer<bs_t>((s).ptr + mp3dec_scratch_t__OFFSET_bs)), (gr_info + ch), (FloatPointer(s.value.scf.ptr)), layer3gr_limit)
            ch++
        }
        if ((((h.value.header[3].toUInt()) and 16u)).toBool()) {
            L3_intensity_stereo((FloatPointer(s.value.grbuf[0].ptr)), (CPointer<UByte>(s.value.ist_pos[1].ptr)), gr_info, (CPointer<UByte>(h.value.header.ptr)))
        } else {
            if (((((h.value.header[3].toUInt()) and 224u)).toInt()) == 96) {
                L3_midside_stereo((FloatPointer(s.value.grbuf[0].ptr)), 576)
            }
        }
        ch = 0
        while (ch < nch) {
            var aa_bands: Int = 31
            var n_long_bands: Int = (if (gr_info.value.mixed_block_flag.toBool()) 2 else 0) shl (((((((((h.value.header[2].toUInt()) shr 2) and 3u) + (((((h.value.header[1].toUInt()) shr 3) and 1u) + (((h.value.header[1].toUInt()) shr 4) and 1u)) * 3u))).toInt()) == 2)).toInt().toInt())
            if (gr_info.value.n_short_sfb.toBool()) {
                aa_bands = n_long_bands - 1
                L3_reorder((s.value.grbuf[ch] + ((n_long_bands * 18))), (FloatPointer(s.value.syn[0].ptr)), (gr_info.value.sfbtab + (gr_info.value.n_long_sfb.toInt())))
            }
            L3_antialias((FloatPointer(s.value.grbuf[ch].ptr)), aa_bands)
            L3_imdct_gr((FloatPointer(s.value.grbuf[ch].ptr)), (FloatPointer(h.value.mdct_overlap[ch].ptr)), (gr_info.value.block_type.toUInt()), (n_long_bands.toUInt()))
            L3_change_sign((FloatPointer(s.value.grbuf[ch].ptr)))
            ch++
            gr_info += 1
        }

    }
    fun mp3d_DCT_II(grbuf: FloatPointer, n: Int) {
        var g_sec: Array24Float = __STATIC_mp3d_DCT_II_g_sec
        var i: Int = 0
        var k: Int = 0
        while (k < n) {
            stackFrame {
                var t: Array4Array8Float = Array4Array8FloatAlloc(arrayOf((Array8Float(0))))
                var x: FloatPointer = FloatPointer(0)
                var y: FloatPointer = grbuf + k
                run { run { val `-` = FloatPointer(t[0].ptr); x = `-`; `-` }; run { val `-` = 0; i = `-`; `-` } }
                while (i < 8) {
                    var x0: Float = y[i * 18]
                    var x1: Float = y[(15 - i) * 18]
                    var x2: Float = y[(16 + i) * 18]
                    var x3: Float = y[(31 - i) * 18]
                    var t0: Float = x0 + x3
                    var t1: Float = x1 + x2
                    var t2: Float = (x1 - x2) * g_sec[(3 * i) + 0]
                    var t3: Float = (x0 - x3) * g_sec[(3 * i) + 1]
                    x[0] = t0 + t1
                    x[8] = (t0 - t1) * g_sec[(3 * i) + 2]
                    x[16] = t3 + t2
                    x[24] = (t3 - t2) * g_sec[(3 * i) + 2]
                    i++
                    x++
                }
                run { run { val `-` = FloatPointer(t[0].ptr); x = `-`; `-` }; run { val `-` = 0; i = `-`; `-` } }
                while (i < 4) {
                    var x0: Float = x[0]
                    var x1: Float = x[1]
                    var x2: Float = x[2]
                    var x3: Float = x[3]
                    var x4: Float = x[4]
                    var x5: Float = x[5]
                    var x6: Float = x[6]
                    var x7: Float = x[7]
                    var xt: Float = 0f
                    xt = x0 - x7
                    x0 = x0 + x7
                    x7 = x1 - x6
                    x1 = x1 + x6
                    x6 = x2 - x5
                    x2 = x2 + x5
                    x5 = x3 - x4
                    x3 = x3 + x4
                    x4 = x0 - x3
                    x0 = x0 + x3
                    x3 = x1 - x2
                    x1 = x1 + x2
                    x[0] = x0 + x1
                    x[4] = (x0 - x1) * 0.70710677f
                    x5 = x5 + x6
                    x6 = (x6 + x7) * 0.70710677f
                    x7 = x7 + xt
                    x3 = (x3 + x4) * 0.70710677f
                    x5 = x5 - (x7 * 0.198912367f)
                    x7 = x7 + (x5 * 0.382683432f)
                    x5 = x5 - (x7 * 0.198912367f)
                    x0 = xt - x6
                    xt = xt + x6
                    x[1] = (xt + x7) * 0.50979561f
                    x[2] = (x4 + x3) * 0.54119611f
                    x[3] = (x0 - x5) * 0.60134488f
                    x[5] = (x0 + x5) * 0.89997619f
                    x[6] = (x4 - x3) * 1.30656302f
                    x[7] = (xt - x7) * 2.56291556f
                    i++
                    x += 8
                }
                i = 0
                while (i < 7) {
                    y[0 * 18] = t[0][i]
                    y[1 * 18] = (t[2][i] + t[3][i]) + t[3][i + 1]
                    y[2 * 18] = t[1][i] + t[1][i + 1]
                    y[3 * 18] = (t[2][i + 1] + t[3][i]) + t[3][i + 1]
                    i++
                    y += 4 * 18
                }
                y[0 * 18] = t[0][7]
                y[1 * 18] = t[2][7] + t[3][7]
                y[2 * 18] = t[1][7]
                y[3 * 18] = t[3][7]

            }
            k++
        }

    }
    fun mp3d_scale_pcm(sample: Float): Short {
        if ((sample.toDouble()) >= 32766.5) {
            return 32767.toInt().toShort()
        }
        if ((sample.toDouble()) <= -32767.5) {
            return (-32768L).toInt().toShort()
        }
        var s: Short = ((sample + 0.5f)).toInt().toShort()
        s = (((s.toInt()) - (((s < (0.toInt().toShort()))).toInt().toInt()))).toInt().toShort()
        return s

    }
    fun mp3d_synth_pair(pcm: CPointer<Short>, nch: Int, z: FloatPointer) {
        var z: FloatPointer = z // Mutating parameter
        var a: Float = 0f
        a = (z[14 * 64] - z[0]) * 29f
        a = a + ((z[1 * 64] + z[13 * 64]) * 213f)
        a = a + ((z[12 * 64] - z[2 * 64]) * 459f)
        a = a + ((z[3 * 64] + z[11 * 64]) * 2037f)
        a = a + ((z[10 * 64] - z[4 * 64]) * 5153f)
        a = a + ((z[5 * 64] + z[9 * 64]) * 6574f)
        a = a + ((z[8 * 64] - z[6 * 64]) * 37489f)
        a = a + (z[7 * 64] * 75038f)
        pcm[0] = mp3d_scale_pcm(a)
        z = z + 2
        a = z[14 * 64] * 104f
        a = a + (z[12 * 64] * 1567f)
        a = a + (z[10 * 64] * 9727f)
        a = a + (z[8 * 64] * 64019f)
        a = a + (z[6 * 64] * -9975f)
        a = a + (z[4 * 64] * -45f)
        a = a + (z[2 * 64] * 146f)
        a = a + (z[0 * 64] * -5f)
        pcm[16 * nch] = mp3d_scale_pcm(a)

    }
    fun mp3d_synth(xl: FloatPointer, dstl: CPointer<Short>, nch: Int, lins: FloatPointer) {
        var i: Int = 0
        var xr: FloatPointer = xl + ((576 * (nch - 1)))
        var dstr: CPointer<Short> = dstl + ((nch - 1))
        var g_win: FloatPointer = __STATIC_mp3d_synth_g_win
        var zlin: FloatPointer = lins + ((15 * 64))
        var w: FloatPointer = FloatPointer(g_win.ptr)
        zlin[4 * 15] = xl[18 * 16]
        zlin[(4 * 15) + 1] = xr[18 * 16]
        zlin[(4 * 15) + 2] = xl[0]
        zlin[(4 * 15) + 3] = xr[0]
        zlin[4 * 31] = xl[1 + (18 * 16)]
        zlin[(4 * 31) + 1] = xr[1 + (18 * 16)]
        zlin[(4 * 31) + 2] = xl[1]
        zlin[(4 * 31) + 3] = xr[1]
        mp3d_synth_pair(dstr, nch, ((lins + ((4 * 15))) + 1))
        mp3d_synth_pair((dstr + ((32 * nch))), nch, (((lins + ((4 * 15))) + 64) + 1))
        mp3d_synth_pair(dstl, nch, (lins + ((4 * 15))))
        mp3d_synth_pair((dstl + ((32 * nch))), nch, ((lins + ((4 * 15))) + 64))
        i = 14
        while (i >= 0) {

            var a: Array4Float = Array4Float(fixedArrayOfFloat(0f, size = 4).ptr)
            var b: Array4Float = Array4Float(fixedArrayOfFloat(0f, size = 4).ptr)
            zlin[4 * i] = xl[18 * (31 - i)]
            zlin[(4 * i) + 1] = xr[18 * (31 - i)]
            zlin[(4 * i) + 2] = xl[1 + (18 * (31 - i))]
            zlin[(4 * i) + 3] = xr[1 + (18 * (31 - i))]
            zlin[4 * (i + 16)] = xl[1 + (18 * (1 + i))]
            zlin[(4 * (i + 16)) + 1] = xr[1 + (18 * (1 + i))]
            zlin[(4 * (i - 16)) + 2] = xl[18 * (1 + i)]
            zlin[(4 * (i - 16)) + 3] = xr[18 * (1 + i)]

            for (j in 0 until 4) {
                b[j] = 0f
                a[j] = 0f
            }

            for (n in 0 until 8) {
                var w0: Float = w++.value
                var w1: Float = w++.value
                var vz: FloatPointer = ((zlin) + ((4 * i) - (n * 64)))
                var vy: FloatPointer = ((zlin) + ((4 * i) - ((15 - n) * 64)))
                if (n % 2 == 0) {
                    for (j in 0 until 4) {
                        b[j] = b[j] + (vz[j] * w1) + (vy[j] * w0)
                        a[j] = a[j] + (vz[j] * w0) - (vy[j] * w1)
                    }
                } else {
                    for (j in 0 until 4) {
                        b[j] = b[j] + ((vz[j] * w1) + (vy[j] * w0))
                        a[j] = a[j] + ((vy[j] * w1) - (vz[j] * w0))
                    }
                }
            }

            dstr[(15 - i) * nch] = mp3d_scale_pcm(a[1])
            dstr[(17 + i) * nch] = mp3d_scale_pcm(b[1])
            dstl[(15 - i) * nch] = mp3d_scale_pcm(a[0])
            dstl[(17 + i) * nch] = mp3d_scale_pcm(b[0])
            dstr[(47 - i) * nch] = mp3d_scale_pcm(a[3])
            dstr[(49 + i) * nch] = mp3d_scale_pcm(b[3])
            dstl[(47 - i) * nch] = mp3d_scale_pcm(a[2])
            dstl[(49 + i) * nch] = mp3d_scale_pcm(b[2])

            i -= 1
        }

    }
    fun mp3d_synth_granule(qmf_state: FloatPointer, grbuf: FloatPointer, nbands: Int, nch: Int, pcm: CPointer<Short>, lins: FloatPointer) {
        var i: Int = 0
        i = 0
        while (i < nch) {
            mp3d_DCT_II((grbuf + ((576 * i))), nbands)
            i++
        }
        memcpy((CPointer<Unit>(lins.ptr)), (CPointer<Unit>(qmf_state.ptr)), ((Float.SIZE_BYTES * 15) * 64))
        i = 0
        while (i < nbands) {
            mp3d_synth((grbuf + i), (pcm + ((32 * (nch * i)))), nch, (lins + ((i * 64))))
            i += 2
        }
        if (nch == 1) {
            i = 0
            while (i < (15 * 64)) {
                qmf_state[i] = lins[(nbands * 64) + i]
                i += 2
            }
        } else {
            memcpy((CPointer<Unit>(qmf_state.ptr)), (CPointer<Unit>(((lins + ((nbands * 64)))).ptr)), ((Float.SIZE_BYTES * 15) * 64))
        }

    }
    fun mp3d_match_frame(hdr: CPointer<UByte>, mp3_bytes: Int, frame_bytes: Int): Int {
        var i: Int = 0
        var nmatch: Int = 0
        run { run { val `-` = 0; i = `-`; `-` }; run { val `-` = 0; nmatch = `-`; `-` } }
        while (nmatch < 10) {
            i += (hdr_frame_bytes((hdr + i), frame_bytes) + hdr_padding((hdr + i)))
            if ((i + 4) > mp3_bytes) {
                return ((nmatch > 0)).toInt().toInt()
            }
            if (hdr_compare(hdr, (hdr + i)) == 0) {
                return 0
            }
            nmatch += 1
        }
        return 1

    }
    fun mp3d_find_frame(mp3: CPointer<UByte>, mp3_bytes: Int, free_format_bytes: IntPointer, ptr_frame_bytes: IntPointer): Int {
        var mp3: CPointer<UByte> = mp3 // Mutating parameter
        var i: Int = 0
        var k: Int = 0
        i = 0
        while0@while (i < (mp3_bytes - 4)) {
            if (hdr_valid(mp3).toBool()) {
                val __oldPos2 = STACK_PTR
                try {
                    var frame_bytes: Int = hdr_frame_bytes(mp3, free_format_bytes.value)
                    var frame_and_padding: Int = frame_bytes + hdr_padding(mp3)
                    k = 4
                    while1@while (((frame_bytes == 0) && (k < 2304)) && (((i + (2 * (((k < (mp3_bytes - 4))).toInt().toInt())))).toBool())) {
                        if (hdr_compare(mp3, (mp3 + k)).toBool()) {
                            val __oldPos1 = STACK_PTR
                            try {
                                var fb: Int = k - hdr_padding(mp3)
                                var nextfb: Int = fb + hdr_padding((mp3 + k))
                                if (((((i + k) + nextfb) + 4) > mp3_bytes) || (hdr_compare(mp3, ((mp3 + k) + nextfb)) == 0)) {
                                    k = k + 1
                                    continue@while1
                                }
                                frame_and_padding = k
                                frame_bytes = fb
                                free_format_bytes.value = fb

                            }
                            finally {
                                STACK_PTR = __oldPos1
                            }
                        }
                        k++
                    }
                    if ((((frame_bytes.toBool()) && (((i + (((frame_and_padding <= mp3_bytes)).toInt().toInt()))).toBool())) && (mp3d_match_frame(mp3, (mp3_bytes - i), frame_bytes).toBool())) || ((i == 0) && (frame_and_padding == mp3_bytes))) {
                        ptr_frame_bytes.value = frame_and_padding
                        return i
                    }
                    free_format_bytes.value = 0

                }
                finally {
                    STACK_PTR = __oldPos2
                }
            }
            run { i++; run { val `-` = mp3; mp3 = mp3 + 1; `-` } }
        }
        ptr_frame_bytes.value = 0
        return mp3_bytes

    }
    fun mp3dec_init(dec: CPointer<mp3dec_t>) {
        dec.value.header[0] = 0.toUByte()

    }
    fun mp3dec_decode_frame(dec: CPointer<mp3dec_t>, mp3: CPointer<UByte>, mp3_bytes: Int, pcm: CPointer<Short>, info: CPointer<mp3dec_frame_info_t>): Int = stackFrame {
        // Require alloc in stack to get pointer: frame_size
        // Require alloc in stack to get pointer: scratch
        var pcm: CPointer<Short> = pcm // Mutating parameter
        var i: Int = 0
        var igr: Int = 0
        var frame_size: IntPointer = IntPointer(alloca(4).ptr).also { it.value = 0 }
        var success: Int = 1
        var hdr: CPointer<UByte> = CPointer<UByte>(0)
        var bs_frame: Array1bs_t = Array1bs_tAlloc(arrayOf((bs_t(0))))
        var scratch: mp3dec_scratch_t = mp3dec_scratch_tAlloc().copyFrom(mp3dec_scratch_tAlloc())
        if (((mp3_bytes > 4) && ((dec.value.header[0].toInt()) == 255)) && (hdr_compare((CPointer<UByte>(dec.value.header.ptr)), mp3).toBool())) {
            frame_size.value = hdr_frame_bytes(mp3, dec.value.free_format_bytes) + hdr_padding(mp3)
            if ((frame_size.value != mp3_bytes) && (((frame_size.value + 4) > mp3_bytes) || (hdr_compare(mp3, (mp3 + frame_size.value)) == 0))) {
                frame_size.value = 0
            }
        }
        if (frame_size.value == 0) {
            memset((CPointer<Unit>(dec.ptr)), 0, mp3dec_t__SIZE_BYTES)
            i = mp3d_find_frame(mp3, mp3_bytes, (IntPointer((dec).ptr + mp3dec_t__OFFSET_free_format_bytes)), (IntPointer(frame_size.ptr)))
            if ((frame_size.value == 0) || (((i + (((frame_size.value > mp3_bytes)).toInt().toInt()))).toBool())) {
                info.value.frame_bytes = i
                return 0
            }
        }
        hdr = mp3 + i
        memcpy((CPointer<Unit>(dec.value.header.ptr)), (CPointer<Unit>(hdr.ptr)), 4)
        info.value.frame_bytes = i + frame_size.value
        info.value.frame_offset = i
        info.value.channels = (if (((((hdr[3].toUInt()) and 192u)).toInt()) == 192) 1 else 2)
        info.value.hz = hdr_sample_rate_hz(hdr).toInt()
        info.value.layer = 4 - (((((hdr[1].toUInt()) shr 1) and 3u)).toInt())
        info.value.bitrate_kbps = hdr_bitrate_kbps(hdr).toInt()
        if (!pcm.toBool()) {
            return hdr_frame_samples(hdr).toInt()
        }
        bs_init((CPointer<bs_t>(bs_frame.ptr)), (hdr + 4), (frame_size.value - 4))
        if (((hdr[1].toUInt()) and 1u) == 0u) {
            get_bits((CPointer<bs_t>(bs_frame.ptr)), 16)
        }
        if (info.value.layer == 3) {
            var main_data_begin: Int = L3_read_side_info((CPointer<bs_t>(bs_frame.ptr)), (CPointer<L3_gr_info_t>(scratch.gr_info.ptr)), hdr)
            if ((main_data_begin < 0) || (bs_frame.value.pos > bs_frame.value.limit)) {
                mp3dec_init(dec)
                return 0
            }
            success = L3_restore_reservoir(dec, (CPointer<bs_t>(bs_frame.ptr)), (CPointer<mp3dec_scratch_t>(scratch.ptr)), main_data_begin)
            if (success.toBool()) {
                igr = 0
                while (igr < (if ((((hdr[1].toUInt()) and 8u)).toBool()) 2 else 1)) {
                    memset((CPointer<Unit>(scratch.grbuf[0].ptr)), 0, ((576 * 2) * Float.SIZE_BYTES))
                    L3_decode(dec, (CPointer<mp3dec_scratch_t>(scratch.ptr)), (scratch.gr_info + ((igr * info.value.channels))), info.value.channels)
                    mp3d_synth_granule((FloatPointer(dec.value.qmf_state.ptr)), (FloatPointer(scratch.grbuf[0].ptr)), 18, info.value.channels, pcm, (FloatPointer(scratch.syn[0].ptr)))
                    run { igr++; run { val `-` = pcm + ((576 * info.value.channels)); pcm = `-`; `-` } }
                }
            }
            L3_save_reservoir(dec, (CPointer<mp3dec_scratch_t>(scratch.ptr)))

        } else {
            stackFrame {
                var sci: Array1L12_scale_info = Array1L12_scale_infoAlloc(arrayOf((L12_scale_info(0))))
                L12_read_scale_info(hdr, (CPointer<bs_t>(bs_frame.ptr)), (CPointer<L12_scale_info>(sci.ptr)))
                memset((CPointer<Unit>(scratch.grbuf[0].ptr)), 0, ((576 * 2) * Float.SIZE_BYTES))
                run { run { val `-` = 0; i = `-`; `-` }; run { val `-` = 0; igr = `-`; `-` } }
                while (igr < 3) {
                    if (12 == (run { val `-` = i + L12_dequantize_granule((scratch.grbuf[0] + i), (CPointer<bs_t>(bs_frame.ptr)), (CPointer<L12_scale_info>(sci.ptr)), (info.value.layer or 1)); i = `-`; `-` })) {
                        i = 0
                        L12_apply_scf_384((CPointer<L12_scale_info>(sci.ptr)), (sci.value.scf + igr), (FloatPointer(scratch.grbuf[0].ptr)))
                        mp3d_synth_granule((FloatPointer(dec.value.qmf_state.ptr)), (FloatPointer(scratch.grbuf[0].ptr)), 12, info.value.channels, pcm, (FloatPointer(scratch.syn[0].ptr)))
                        memset((CPointer<Unit>(scratch.grbuf[0].ptr)), 0, ((576 * 2) * Float.SIZE_BYTES))
                        pcm = pcm + ((384 * info.value.channels))
                    }
                    if (bs_frame.value.pos > bs_frame.value.limit) {
                        mp3dec_init(dec)
                        return 0
                    }
                    igr = igr + 1
                }

            }
        }
        return success * (hdr_frame_samples((CPointer<UByte>(dec.value.header.ptr))).toInt())

    }
    class ExitError(val code: Int) : Error()
    fun exit(status: Int) {

        throw ExitError(status)

    }
    fun strlen(str: CPointer<Byte>): Int {
        var str: CPointer<Byte> = str // Mutating parameter
        var out: Int = 0
        while ((run { val `-` = str; str = str + 1; `-` }.value.toInt()) != 0) {
            out = out + 1
        }
        return out

    }
    fun memcmp(ptr1: CPointer<Unit>, ptr2: CPointer<Unit>, num: Int): Int {
        var a: CPointer<Byte> = CPointer<Byte>(ptr1.ptr)
        var b: CPointer<Byte> = CPointer<Byte>(ptr2.ptr)
        var n: Int = 0
        while (n < num) {
            var res: Int = (a[n].toInt()) - (b[n].toInt())
            if (res < 0) return -1
            if (res > 0) return 1
            n++
        }
        return 0

    }

    //////////////////
    // C STRUCTURES //
    //////////////////

    //////////////////
    fun mp3dec_frame_info_tAlloc(): mp3dec_frame_info_t = mp3dec_frame_info_t(alloca(mp3dec_frame_info_t__SIZE_BYTES).ptr)
    fun mp3dec_frame_info_tAlloc(frame_bytes: Int, frame_offset: Int, channels: Int, hz: Int, layer: Int, bitrate_kbps: Int): mp3dec_frame_info_t = mp3dec_frame_info_tAlloc().apply { this.frame_bytes = frame_bytes; this.frame_offset = frame_offset; this.channels = channels; this.hz = hz; this.layer = layer; this.bitrate_kbps = bitrate_kbps }
    fun mp3dec_frame_info_t.copyFrom(src: mp3dec_frame_info_t): mp3dec_frame_info_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), mp3dec_frame_info_t__SIZE_BYTES) }
    inline fun fixedArrayOfmp3dec_frame_info_t(size: Int, setItems: CPointer<mp3dec_frame_info_t>.() -> Unit): CPointer<mp3dec_frame_info_t> = CPointer<mp3dec_frame_info_t>(alloca_zero(size * mp3dec_frame_info_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getmp3dec_frame_info_t") operator fun CPointer<mp3dec_frame_info_t>.get(index: Int): mp3dec_frame_info_t = mp3dec_frame_info_t(this.ptr + index * mp3dec_frame_info_t__SIZE_BYTES)
    operator fun CPointer<mp3dec_frame_info_t>.set(index: Int, value: mp3dec_frame_info_t) = mp3dec_frame_info_t(this.ptr + index * mp3dec_frame_info_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusmp3dec_frame_info_t") operator fun CPointer<mp3dec_frame_info_t>.plus(offset: Int): CPointer<mp3dec_frame_info_t> = CPointer<mp3dec_frame_info_t>(this.ptr + offset * mp3dec_frame_info_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusmp3dec_frame_info_t") operator fun CPointer<mp3dec_frame_info_t>.minus(offset: Int): CPointer<mp3dec_frame_info_t> = CPointer<mp3dec_frame_info_t>(this.ptr - offset * mp3dec_frame_info_t__SIZE_BYTES)
    fun CPointer<mp3dec_frame_info_t>.minusPtrmp3dec_frame_info_t(other: CPointer<mp3dec_frame_info_t>) = (this.ptr - other.ptr) / mp3dec_frame_info_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getmp3dec_frame_info_t") var CPointer<mp3dec_frame_info_t>.value: mp3dec_frame_info_t get() = this[0]; set(value) { this[0] = value }
    /// mp3dec_frame_info_t fields {
    var mp3dec_frame_info_t.frame_bytes: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_frame_bytes); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_frame_bytes, value)
    var mp3dec_frame_info_t.frame_offset: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_frame_offset); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_frame_offset, value)
    var mp3dec_frame_info_t.channels: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_channels); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_channels, value)
    var mp3dec_frame_info_t.hz: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_hz); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_hz, value)
    var mp3dec_frame_info_t.layer: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_layer); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_layer, value)
    var mp3dec_frame_info_t.bitrate_kbps: Int get() = lw(ptr + mp3dec_frame_info_t__OFFSET_bitrate_kbps); set(value) = sw(ptr + mp3dec_frame_info_t__OFFSET_bitrate_kbps, value)
    /// }

    //////////////////
    fun mp3dec_tAlloc(): mp3dec_t = mp3dec_t(alloca(mp3dec_t__SIZE_BYTES).ptr)
    fun mp3dec_tAlloc(mdct_overlap: Array2Array288Float, qmf_state: Array960Float, reserv: Int, free_format_bytes: Int, header: Array4UByte, reserv_buf: Array511UByte): mp3dec_t = mp3dec_tAlloc().apply { this.mdct_overlap = mdct_overlap; this.qmf_state = qmf_state; this.reserv = reserv; this.free_format_bytes = free_format_bytes; this.header = header; this.reserv_buf = reserv_buf }
    fun mp3dec_t.copyFrom(src: mp3dec_t): mp3dec_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), mp3dec_t__SIZE_BYTES) }
    inline fun fixedArrayOfmp3dec_t(size: Int, setItems: CPointer<mp3dec_t>.() -> Unit): CPointer<mp3dec_t> = CPointer<mp3dec_t>(alloca_zero(size * mp3dec_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getmp3dec_t") operator fun CPointer<mp3dec_t>.get(index: Int): mp3dec_t = mp3dec_t(this.ptr + index * mp3dec_t__SIZE_BYTES)
    operator fun CPointer<mp3dec_t>.set(index: Int, value: mp3dec_t) = mp3dec_t(this.ptr + index * mp3dec_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusmp3dec_t") operator fun CPointer<mp3dec_t>.plus(offset: Int): CPointer<mp3dec_t> = CPointer<mp3dec_t>(this.ptr + offset * mp3dec_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusmp3dec_t") operator fun CPointer<mp3dec_t>.minus(offset: Int): CPointer<mp3dec_t> = CPointer<mp3dec_t>(this.ptr - offset * mp3dec_t__SIZE_BYTES)
    fun CPointer<mp3dec_t>.minusPtrmp3dec_t(other: CPointer<mp3dec_t>) = (this.ptr - other.ptr) / mp3dec_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getmp3dec_t") var CPointer<mp3dec_t>.value: mp3dec_t get() = this[0]; set(value) { this[0] = value }
    /// mp3dec_t fields {
    var mp3dec_t.mdct_overlap: Array2Array288Float get() = Array2Array288Float(ptr + mp3dec_t__OFFSET_mdct_overlap); set(value) { TODO("Unsupported setting ftype=Float[288][2]") }
    var mp3dec_t.qmf_state: Array960Float get() = Array960Float(ptr + mp3dec_t__OFFSET_qmf_state); set(value) { TODO("Unsupported setting ftype=Float[960]") }
    var mp3dec_t.reserv: Int get() = lw(ptr + mp3dec_t__OFFSET_reserv); set(value) = sw(ptr + mp3dec_t__OFFSET_reserv, value)
    var mp3dec_t.free_format_bytes: Int get() = lw(ptr + mp3dec_t__OFFSET_free_format_bytes); set(value) = sw(ptr + mp3dec_t__OFFSET_free_format_bytes, value)
    var mp3dec_t.header: Array4UByte get() = Array4UByte(ptr + mp3dec_t__OFFSET_header); set(value) { TODO("Unsupported setting ftype=UByte[4]") }
    var mp3dec_t.reserv_buf: Array511UByte get() = Array511UByte(ptr + mp3dec_t__OFFSET_reserv_buf); set(value) { TODO("Unsupported setting ftype=UByte[511]") }
    /// }

    //////////////////
    fun bs_tAlloc(): bs_t = bs_t(alloca(bs_t__SIZE_BYTES).ptr)
    fun bs_tAlloc(buf: CPointer<UByte>, pos: Int, limit: Int): bs_t = bs_tAlloc().apply { this.buf = buf; this.pos = pos; this.limit = limit }
    fun bs_t.copyFrom(src: bs_t): bs_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), bs_t__SIZE_BYTES) }
    inline fun fixedArrayOfbs_t(size: Int, setItems: CPointer<bs_t>.() -> Unit): CPointer<bs_t> = CPointer<bs_t>(alloca_zero(size * bs_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getbs_t") operator fun CPointer<bs_t>.get(index: Int): bs_t = bs_t(this.ptr + index * bs_t__SIZE_BYTES)
    operator fun CPointer<bs_t>.set(index: Int, value: bs_t) = bs_t(this.ptr + index * bs_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusbs_t") operator fun CPointer<bs_t>.plus(offset: Int): CPointer<bs_t> = CPointer<bs_t>(this.ptr + offset * bs_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusbs_t") operator fun CPointer<bs_t>.minus(offset: Int): CPointer<bs_t> = CPointer<bs_t>(this.ptr - offset * bs_t__SIZE_BYTES)
    fun CPointer<bs_t>.minusPtrbs_t(other: CPointer<bs_t>) = (this.ptr - other.ptr) / bs_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getbs_t") var CPointer<bs_t>.value: bs_t get() = this[0]; set(value) { this[0] = value }
    /// bs_t fields {
    var bs_t.buf: CPointer<UByte> get() = CPointer<UByte>(lw(ptr + bs_t__OFFSET_buf)); set(value) { sw(ptr + bs_t__OFFSET_buf, value.ptr) }
    var bs_t.pos: Int get() = lw(ptr + bs_t__OFFSET_pos); set(value) = sw(ptr + bs_t__OFFSET_pos, value)
    var bs_t.limit: Int get() = lw(ptr + bs_t__OFFSET_limit); set(value) = sw(ptr + bs_t__OFFSET_limit, value)
    /// }

    //////////////////
    fun L12_scale_infoAlloc(): L12_scale_info = L12_scale_info(alloca(L12_scale_info__SIZE_BYTES).ptr)
    fun L12_scale_infoAlloc(scf: Array192Float, total_bands: UByte, stereo_bands: UByte, bitalloc: Array64UByte, scfcod: Array64UByte): L12_scale_info = L12_scale_infoAlloc().apply { this.scf = scf; this.total_bands = total_bands; this.stereo_bands = stereo_bands; this.bitalloc = bitalloc; this.scfcod = scfcod }
    fun L12_scale_info.copyFrom(src: L12_scale_info): L12_scale_info = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), L12_scale_info__SIZE_BYTES) }
    inline fun fixedArrayOfL12_scale_info(size: Int, setItems: CPointer<L12_scale_info>.() -> Unit): CPointer<L12_scale_info> = CPointer<L12_scale_info>(alloca_zero(size * L12_scale_info__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getL12_scale_info") operator fun CPointer<L12_scale_info>.get(index: Int): L12_scale_info = L12_scale_info(this.ptr + index * L12_scale_info__SIZE_BYTES)
    operator fun CPointer<L12_scale_info>.set(index: Int, value: L12_scale_info) = L12_scale_info(this.ptr + index * L12_scale_info__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusL12_scale_info") operator fun CPointer<L12_scale_info>.plus(offset: Int): CPointer<L12_scale_info> = CPointer<L12_scale_info>(this.ptr + offset * L12_scale_info__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusL12_scale_info") operator fun CPointer<L12_scale_info>.minus(offset: Int): CPointer<L12_scale_info> = CPointer<L12_scale_info>(this.ptr - offset * L12_scale_info__SIZE_BYTES)
    fun CPointer<L12_scale_info>.minusPtrL12_scale_info(other: CPointer<L12_scale_info>) = (this.ptr - other.ptr) / L12_scale_info__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getL12_scale_info") var CPointer<L12_scale_info>.value: L12_scale_info get() = this[0]; set(value) { this[0] = value }
    /// L12_scale_info fields {
    var L12_scale_info.scf: Array192Float get() = Array192Float(ptr + L12_scale_info__OFFSET_scf); set(value) { TODO("Unsupported setting ftype=Float[192]") }
    var L12_scale_info.total_bands: UByte get() = lb(ptr + L12_scale_info__OFFSET_total_bands).toUByte(); set(value) = sb(ptr + L12_scale_info__OFFSET_total_bands, (value).toByte())
    var L12_scale_info.stereo_bands: UByte get() = lb(ptr + L12_scale_info__OFFSET_stereo_bands).toUByte(); set(value) = sb(ptr + L12_scale_info__OFFSET_stereo_bands, (value).toByte())
    var L12_scale_info.bitalloc: Array64UByte get() = Array64UByte(ptr + L12_scale_info__OFFSET_bitalloc); set(value) { TODO("Unsupported setting ftype=UByte[64]") }
    var L12_scale_info.scfcod: Array64UByte get() = Array64UByte(ptr + L12_scale_info__OFFSET_scfcod); set(value) { TODO("Unsupported setting ftype=UByte[64]") }
    /// }

    //////////////////
    fun L12_subband_alloc_tAlloc(): L12_subband_alloc_t = L12_subband_alloc_t(alloca(L12_subband_alloc_t__SIZE_BYTES).ptr)
    fun L12_subband_alloc_tAlloc(tab_offset: UByte, code_tab_width: UByte, band_count: UByte): L12_subband_alloc_t = L12_subband_alloc_tAlloc().apply { this.tab_offset = tab_offset; this.code_tab_width = code_tab_width; this.band_count = band_count }
    fun L12_subband_alloc_t.copyFrom(src: L12_subband_alloc_t): L12_subband_alloc_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), L12_subband_alloc_t__SIZE_BYTES) }
    inline fun fixedArrayOfL12_subband_alloc_t(size: Int, setItems: CPointer<L12_subband_alloc_t>.() -> Unit): CPointer<L12_subband_alloc_t> = CPointer<L12_subband_alloc_t>(alloca_zero(size * L12_subband_alloc_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getL12_subband_alloc_t") operator fun CPointer<L12_subband_alloc_t>.get(index: Int): L12_subband_alloc_t = L12_subband_alloc_t(this.ptr + index * L12_subband_alloc_t__SIZE_BYTES)
    operator fun CPointer<L12_subband_alloc_t>.set(index: Int, value: L12_subband_alloc_t) = L12_subband_alloc_t(this.ptr + index * L12_subband_alloc_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusL12_subband_alloc_t") operator fun CPointer<L12_subband_alloc_t>.plus(offset: Int): CPointer<L12_subband_alloc_t> = CPointer<L12_subband_alloc_t>(this.ptr + offset * L12_subband_alloc_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusL12_subband_alloc_t") operator fun CPointer<L12_subband_alloc_t>.minus(offset: Int): CPointer<L12_subband_alloc_t> = CPointer<L12_subband_alloc_t>(this.ptr - offset * L12_subband_alloc_t__SIZE_BYTES)
    fun CPointer<L12_subband_alloc_t>.minusPtrL12_subband_alloc_t(other: CPointer<L12_subband_alloc_t>) = (this.ptr - other.ptr) / L12_subband_alloc_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getL12_subband_alloc_t") var CPointer<L12_subband_alloc_t>.value: L12_subband_alloc_t get() = this[0]; set(value) { this[0] = value }
    /// L12_subband_alloc_t fields {
    var L12_subband_alloc_t.tab_offset: UByte get() = lb(ptr + L12_subband_alloc_t__OFFSET_tab_offset).toUByte(); set(value) = sb(ptr + L12_subband_alloc_t__OFFSET_tab_offset, (value).toByte())
    var L12_subband_alloc_t.code_tab_width: UByte get() = lb(ptr + L12_subband_alloc_t__OFFSET_code_tab_width).toUByte(); set(value) = sb(ptr + L12_subband_alloc_t__OFFSET_code_tab_width, (value).toByte())
    var L12_subband_alloc_t.band_count: UByte get() = lb(ptr + L12_subband_alloc_t__OFFSET_band_count).toUByte(); set(value) = sb(ptr + L12_subband_alloc_t__OFFSET_band_count, (value).toByte())
    /// }

    //////////////////
    fun L3_gr_info_tAlloc(): L3_gr_info_t = L3_gr_info_t(alloca(L3_gr_info_t__SIZE_BYTES).ptr)
    fun L3_gr_info_tAlloc(sfbtab: CPointer<UByte>, part_23_length: UShort, big_values: UShort, scalefac_compress: UShort, global_gain: UByte, block_type: UByte, mixed_block_flag: UByte, n_long_sfb: UByte, n_short_sfb: UByte, table_select: Array3UByte, region_count: Array3UByte, subblock_gain: Array3UByte, preflag: UByte, scalefac_scale: UByte, count1_table: UByte, scfsi: UByte): L3_gr_info_t = L3_gr_info_tAlloc().apply { this.sfbtab = sfbtab; this.part_23_length = part_23_length; this.big_values = big_values; this.scalefac_compress = scalefac_compress; this.global_gain = global_gain; this.block_type = block_type; this.mixed_block_flag = mixed_block_flag; this.n_long_sfb = n_long_sfb; this.n_short_sfb = n_short_sfb; this.table_select = table_select; this.region_count = region_count; this.subblock_gain = subblock_gain; this.preflag = preflag; this.scalefac_scale = scalefac_scale; this.count1_table = count1_table; this.scfsi = scfsi }
    fun L3_gr_info_t.copyFrom(src: L3_gr_info_t): L3_gr_info_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), L3_gr_info_t__SIZE_BYTES) }
    inline fun fixedArrayOfL3_gr_info_t(size: Int, setItems: CPointer<L3_gr_info_t>.() -> Unit): CPointer<L3_gr_info_t> = CPointer<L3_gr_info_t>(alloca_zero(size * L3_gr_info_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getL3_gr_info_t") operator fun CPointer<L3_gr_info_t>.get(index: Int): L3_gr_info_t = L3_gr_info_t(this.ptr + index * L3_gr_info_t__SIZE_BYTES)
    operator fun CPointer<L3_gr_info_t>.set(index: Int, value: L3_gr_info_t) = L3_gr_info_t(this.ptr + index * L3_gr_info_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusL3_gr_info_t") operator fun CPointer<L3_gr_info_t>.plus(offset: Int): CPointer<L3_gr_info_t> = CPointer<L3_gr_info_t>(this.ptr + offset * L3_gr_info_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusL3_gr_info_t") operator fun CPointer<L3_gr_info_t>.minus(offset: Int): CPointer<L3_gr_info_t> = CPointer<L3_gr_info_t>(this.ptr - offset * L3_gr_info_t__SIZE_BYTES)
    fun CPointer<L3_gr_info_t>.minusPtrL3_gr_info_t(other: CPointer<L3_gr_info_t>) = (this.ptr - other.ptr) / L3_gr_info_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getL3_gr_info_t") var CPointer<L3_gr_info_t>.value: L3_gr_info_t get() = this[0]; set(value) { this[0] = value }
    /// L3_gr_info_t fields {
    var L3_gr_info_t.sfbtab: CPointer<UByte> get() = CPointer<UByte>(lw(ptr + L3_gr_info_t__OFFSET_sfbtab)); set(value) { sw(ptr + L3_gr_info_t__OFFSET_sfbtab, value.ptr) }
    var L3_gr_info_t.part_23_length: UShort get() = lh(ptr + L3_gr_info_t__OFFSET_part_23_length).toUShort(); set(value) = sh(ptr + L3_gr_info_t__OFFSET_part_23_length, (value).toShort())
    var L3_gr_info_t.big_values: UShort get() = lh(ptr + L3_gr_info_t__OFFSET_big_values).toUShort(); set(value) = sh(ptr + L3_gr_info_t__OFFSET_big_values, (value).toShort())
    var L3_gr_info_t.scalefac_compress: UShort get() = lh(ptr + L3_gr_info_t__OFFSET_scalefac_compress).toUShort(); set(value) = sh(ptr + L3_gr_info_t__OFFSET_scalefac_compress, (value).toShort())
    var L3_gr_info_t.global_gain: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_global_gain).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_global_gain, (value).toByte())
    var L3_gr_info_t.block_type: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_block_type).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_block_type, (value).toByte())
    var L3_gr_info_t.mixed_block_flag: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_mixed_block_flag).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_mixed_block_flag, (value).toByte())
    var L3_gr_info_t.n_long_sfb: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_n_long_sfb).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_n_long_sfb, (value).toByte())
    var L3_gr_info_t.n_short_sfb: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_n_short_sfb).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_n_short_sfb, (value).toByte())
    var L3_gr_info_t.table_select: Array3UByte get() = Array3UByte(ptr + L3_gr_info_t__OFFSET_table_select); set(value) { TODO("Unsupported setting ftype=UByte[3]") }
    var L3_gr_info_t.region_count: Array3UByte get() = Array3UByte(ptr + L3_gr_info_t__OFFSET_region_count); set(value) { TODO("Unsupported setting ftype=UByte[3]") }
    var L3_gr_info_t.subblock_gain: Array3UByte get() = Array3UByte(ptr + L3_gr_info_t__OFFSET_subblock_gain); set(value) { TODO("Unsupported setting ftype=UByte[3]") }
    var L3_gr_info_t.preflag: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_preflag).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_preflag, (value).toByte())
    var L3_gr_info_t.scalefac_scale: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_scalefac_scale).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_scalefac_scale, (value).toByte())
    var L3_gr_info_t.count1_table: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_count1_table).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_count1_table, (value).toByte())
    var L3_gr_info_t.scfsi: UByte get() = lb(ptr + L3_gr_info_t__OFFSET_scfsi).toUByte(); set(value) = sb(ptr + L3_gr_info_t__OFFSET_scfsi, (value).toByte())
    /// }

    //////////////////
    fun mp3dec_scratch_tAlloc(): mp3dec_scratch_t = mp3dec_scratch_t(alloca(mp3dec_scratch_t__SIZE_BYTES).ptr)
    fun mp3dec_scratch_tAlloc(bs: bs_t, maindata: Array2815UByte, gr_info: Array4L3_gr_info_t, grbuf: Array2Array576Float, scf: Array40Float, syn: Array33Array64Float, ist_pos: Array2Array39UByte): mp3dec_scratch_t = mp3dec_scratch_tAlloc().apply { this.bs = bs; this.maindata = maindata; this.gr_info = gr_info; this.grbuf = grbuf; this.scf = scf; this.syn = syn; this.ist_pos = ist_pos }
    fun mp3dec_scratch_t.copyFrom(src: mp3dec_scratch_t): mp3dec_scratch_t = this.apply { memcpy(CPointer<Unit>(this.ptr), CPointer<Unit>(src.ptr), mp3dec_scratch_t__SIZE_BYTES) }
    inline fun fixedArrayOfmp3dec_scratch_t(size: Int, setItems: CPointer<mp3dec_scratch_t>.() -> Unit): CPointer<mp3dec_scratch_t> = CPointer<mp3dec_scratch_t>(alloca_zero(size * mp3dec_scratch_t__SIZE_BYTES).ptr).apply(setItems)
    @kotlin.jvm.JvmName("getmp3dec_scratch_t") operator fun CPointer<mp3dec_scratch_t>.get(index: Int): mp3dec_scratch_t = mp3dec_scratch_t(this.ptr + index * mp3dec_scratch_t__SIZE_BYTES)
    operator fun CPointer<mp3dec_scratch_t>.set(index: Int, value: mp3dec_scratch_t) = mp3dec_scratch_t(this.ptr + index * mp3dec_scratch_t__SIZE_BYTES).copyFrom(value)
    @kotlin.jvm.JvmName("plusmp3dec_scratch_t") operator fun CPointer<mp3dec_scratch_t>.plus(offset: Int): CPointer<mp3dec_scratch_t> = CPointer<mp3dec_scratch_t>(this.ptr + offset * mp3dec_scratch_t__SIZE_BYTES)
    @kotlin.jvm.JvmName("minusmp3dec_scratch_t") operator fun CPointer<mp3dec_scratch_t>.minus(offset: Int): CPointer<mp3dec_scratch_t> = CPointer<mp3dec_scratch_t>(this.ptr - offset * mp3dec_scratch_t__SIZE_BYTES)
    fun CPointer<mp3dec_scratch_t>.minusPtrmp3dec_scratch_t(other: CPointer<mp3dec_scratch_t>) = (this.ptr - other.ptr) / mp3dec_scratch_t__SIZE_BYTES
    @get:kotlin.jvm.JvmName("getmp3dec_scratch_t") var CPointer<mp3dec_scratch_t>.value: mp3dec_scratch_t get() = this[0]; set(value) { this[0] = value }
    /// mp3dec_scratch_t fields {
    var mp3dec_scratch_t.bs: bs_t get() = bs_t(ptr + mp3dec_scratch_t__OFFSET_bs); set(value) { bs_t(ptr + mp3dec_scratch_t__OFFSET_bs).copyFrom(value) }
    var mp3dec_scratch_t.maindata: Array2815UByte get() = Array2815UByte(ptr + mp3dec_scratch_t__OFFSET_maindata); set(value) { TODO("Unsupported setting ftype=UByte[2815]") }
    var mp3dec_scratch_t.gr_info: Array4L3_gr_info_t get() = Array4L3_gr_info_t(ptr + mp3dec_scratch_t__OFFSET_gr_info); set(value) { TODO("Unsupported setting ftype=struct null[4]") }
    var mp3dec_scratch_t.grbuf: Array2Array576Float get() = Array2Array576Float(ptr + mp3dec_scratch_t__OFFSET_grbuf); set(value) { TODO("Unsupported setting ftype=Float[576][2]") }
    var mp3dec_scratch_t.scf: Array40Float get() = Array40Float(ptr + mp3dec_scratch_t__OFFSET_scf); set(value) { TODO("Unsupported setting ftype=Float[40]") }
    var mp3dec_scratch_t.syn: Array33Array64Float get() = Array33Array64Float(ptr + mp3dec_scratch_t__OFFSET_syn); set(value) { TODO("Unsupported setting ftype=Float[64][33]") }
    var mp3dec_scratch_t.ist_pos: Array2Array39UByte get() = Array2Array39UByte(ptr + mp3dec_scratch_t__OFFSET_ist_pos); set(value) { TODO("Unsupported setting ftype=UByte[39][2]") }
    /// }

    /////////////
    operator fun Array2Array288Float.get(index: Int): Array288Float = Array288Float(addr(index))
    operator fun Array2Array288Float.set(index: Int, value: Array288Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array288Float__ELEMENT_SIZE_BYTES) }
    var Array2Array288Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array288FloatAlloc(setItems: Array2Array288Float.() -> Unit): Array2Array288Float = Array2Array288Float(alloca_zero(
        Array2Array288Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array288FloatAlloc(items: Array<Array288Float>, size: Int = items.size): Array2Array288Float = Array2Array288FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array288Float.plus(offset: Int): CPointer<Array288Float> = CPointer<Array288Float>(addr(offset))
    operator fun Array2Array288Float.minus(offset: Int): CPointer<Array288Float> = CPointer<Array288Float>(addr(-offset))
    /////////////
    operator fun Array288Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array288Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array288Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array288FloatAlloc(setItems: Array288Float.() -> Unit): Array288Float = Array288Float(alloca_zero(
        Array288Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array288FloatAlloc(items: Array<Float>, size: Int = items.size): Array288Float = Array288FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array288Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array288Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array960Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array960Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array960Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array960FloatAlloc(setItems: Array960Float.() -> Unit): Array960Float = Array960Float(alloca_zero(
        Array960Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array960FloatAlloc(items: Array<Float>, size: Int = items.size): Array960Float = Array960FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array960Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array960Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array4UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array4UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array4UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array4UByteAlloc(setItems: Array4UByte.() -> Unit): Array4UByte = Array4UByte(alloca_zero(
        Array4UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array4UByteAlloc(items: Array<UByte>, size: Int = items.size): Array4UByte = Array4UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array4UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array4UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array511UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array511UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array511UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array511UByteAlloc(setItems: Array511UByte.() -> Unit): Array511UByte = Array511UByte(alloca_zero(
        Array511UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array511UByteAlloc(items: Array<UByte>, size: Int = items.size): Array511UByte = Array511UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array511UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array511UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array192Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array192Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array192Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array192FloatAlloc(setItems: Array192Float.() -> Unit): Array192Float = Array192Float(alloca_zero(
        Array192Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array192FloatAlloc(items: Array<Float>, size: Int = items.size): Array192Float = Array192FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array192Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array192Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array64UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array64UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array64UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array64UByteAlloc(setItems: Array64UByte.() -> Unit): Array64UByte = Array64UByte(alloca_zero(
        Array64UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array64UByteAlloc(items: Array<UByte>, size: Int = items.size): Array64UByte = Array64UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array64UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array64UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array3UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array3UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array3UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3UByteAlloc(setItems: Array3UByte.() -> Unit): Array3UByte = Array3UByte(alloca_zero(
        Array3UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3UByteAlloc(items: Array<UByte>, size: Int = items.size): Array3UByte = Array3UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array3UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array2815UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array2815UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array2815UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2815UByteAlloc(setItems: Array2815UByte.() -> Unit): Array2815UByte = Array2815UByte(alloca_zero(
        Array2815UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2815UByteAlloc(items: Array<UByte>, size: Int = items.size): Array2815UByte = Array2815UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2815UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array2815UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array4L3_gr_info_t.get(index: Int): L3_gr_info_t = L3_gr_info_t(addr(index))
    operator fun Array4L3_gr_info_t.set(index: Int, value: L3_gr_info_t) { L3_gr_info_t(addr(index)).copyFrom(value) }
    var Array4L3_gr_info_t.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array4L3_gr_info_tAlloc(setItems: Array4L3_gr_info_t.() -> Unit): Array4L3_gr_info_t = Array4L3_gr_info_t(alloca_zero(
        Array4L3_gr_info_t__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array4L3_gr_info_tAlloc(items: Array<L3_gr_info_t>, size: Int = items.size): Array4L3_gr_info_t = Array4L3_gr_info_tAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array4L3_gr_info_t.plus(offset: Int): CPointer<L3_gr_info_t> = CPointer<L3_gr_info_t>(addr(offset))
    operator fun Array4L3_gr_info_t.minus(offset: Int): CPointer<L3_gr_info_t> = CPointer<L3_gr_info_t>(addr(-offset))
    /////////////
    operator fun Array2Array576Float.get(index: Int): Array576Float = Array576Float(addr(index))
    operator fun Array2Array576Float.set(index: Int, value: Array576Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array576Float__ELEMENT_SIZE_BYTES) }
    var Array2Array576Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array576FloatAlloc(setItems: Array2Array576Float.() -> Unit): Array2Array576Float = Array2Array576Float(alloca_zero(
        Array2Array576Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array576FloatAlloc(items: Array<Array576Float>, size: Int = items.size): Array2Array576Float = Array2Array576FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array576Float.plus(offset: Int): CPointer<Array576Float> = CPointer<Array576Float>(addr(offset))
    operator fun Array2Array576Float.minus(offset: Int): CPointer<Array576Float> = CPointer<Array576Float>(addr(-offset))
    /////////////
    operator fun Array576Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array576Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array576Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array576FloatAlloc(setItems: Array576Float.() -> Unit): Array576Float = Array576Float(alloca_zero(
        Array576Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array576FloatAlloc(items: Array<Float>, size: Int = items.size): Array576Float = Array576FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array576Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array576Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array40Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array40Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array40Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array40FloatAlloc(setItems: Array40Float.() -> Unit): Array40Float = Array40Float(alloca_zero(
        Array40Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array40FloatAlloc(items: Array<Float>, size: Int = items.size): Array40Float = Array40FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array40Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array40Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array33Array64Float.get(index: Int): Array64Float = Array64Float(addr(index))
    operator fun Array33Array64Float.set(index: Int, value: Array64Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array33Array64Float__ELEMENT_SIZE_BYTES) }
    var Array33Array64Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array33Array64FloatAlloc(setItems: Array33Array64Float.() -> Unit): Array33Array64Float = Array33Array64Float(alloca_zero(
        Array33Array64Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array33Array64FloatAlloc(items: Array<Array64Float>, size: Int = items.size): Array33Array64Float = Array33Array64FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array33Array64Float.plus(offset: Int): CPointer<Array64Float> = CPointer<Array64Float>(addr(offset))
    operator fun Array33Array64Float.minus(offset: Int): CPointer<Array64Float> = CPointer<Array64Float>(addr(-offset))
    /////////////
    operator fun Array64Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array64Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array64Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array64FloatAlloc(setItems: Array64Float.() -> Unit): Array64Float = Array64Float(alloca_zero(
        Array64Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array64FloatAlloc(items: Array<Float>, size: Int = items.size): Array64Float = Array64FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array64Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array64Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array2Array39UByte.get(index: Int): Array39UByte = Array39UByte(addr(index))
    operator fun Array2Array39UByte.set(index: Int, value: Array39UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array39UByte__ELEMENT_SIZE_BYTES) }
    var Array2Array39UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array39UByteAlloc(setItems: Array2Array39UByte.() -> Unit): Array2Array39UByte = Array2Array39UByte(alloca_zero(
        Array2Array39UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array39UByteAlloc(items: Array<Array39UByte>, size: Int = items.size): Array2Array39UByte = Array2Array39UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array39UByte.plus(offset: Int): CPointer<Array39UByte> = CPointer<Array39UByte>(addr(offset))
    operator fun Array2Array39UByte.minus(offset: Int): CPointer<Array39UByte> = CPointer<Array39UByte>(addr(-offset))
    /////////////
    operator fun Array39UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array39UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array39UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array39UByteAlloc(setItems: Array39UByte.() -> Unit): Array39UByte = Array39UByte(alloca_zero(
        Array39UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array39UByteAlloc(items: Array<UByte>, size: Int = items.size): Array39UByte = Array39UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array39UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array39UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array2Array3Array15UByte.get(index: Int): Array3Array15UByte = Array3Array15UByte(addr(index))
    operator fun Array2Array3Array15UByte.set(index: Int, value: Array3Array15UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array3Array15UByte__ELEMENT_SIZE_BYTES) }
    var Array2Array3Array15UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array3Array15UByteAlloc(setItems: Array2Array3Array15UByte.() -> Unit): Array2Array3Array15UByte = Array2Array3Array15UByte(alloca_zero(
        Array2Array3Array15UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array3Array15UByteAlloc(items: Array<Array3Array15UByte>, size: Int = items.size): Array2Array3Array15UByte = Array2Array3Array15UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array3Array15UByte.plus(offset: Int): CPointer<Array3Array15UByte> = CPointer<Array3Array15UByte>(addr(offset))
    operator fun Array2Array3Array15UByte.minus(offset: Int): CPointer<Array3Array15UByte> = CPointer<Array3Array15UByte>(addr(-offset))
    /////////////
    operator fun Array3Array15UByte.get(index: Int): Array15UByte = Array15UByte(addr(index))
    operator fun Array3Array15UByte.set(index: Int, value: Array15UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array3Array15UByte__ELEMENT_SIZE_BYTES) }
    var Array3Array15UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3Array15UByteAlloc(setItems: Array3Array15UByte.() -> Unit): Array3Array15UByte = Array3Array15UByte(alloca_zero(
        Array3Array15UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3Array15UByteAlloc(items: Array<Array15UByte>, size: Int = items.size): Array3Array15UByte = Array3Array15UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3Array15UByte.plus(offset: Int): CPointer<Array15UByte> = CPointer<Array15UByte>(addr(offset))
    operator fun Array3Array15UByte.minus(offset: Int): CPointer<Array15UByte> = CPointer<Array15UByte>(addr(-offset))
    /////////////
    operator fun Array15UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array15UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array15UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array15UByteAlloc(setItems: Array15UByte.() -> Unit): Array15UByte = Array15UByte(alloca_zero(
        Array15UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array15UByteAlloc(items: Array<UByte>, size: Int = items.size): Array15UByte = Array15UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array15UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array15UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array3UInt.get(index: Int): UInt = lw(addr(index)).toUInt()
    operator fun Array3UInt.set(index: Int, value: UInt) { sw(addr(index), (value).toInt()) }
    var Array3UInt.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3UIntAlloc(setItems: Array3UInt.() -> Unit): Array3UInt = Array3UInt(alloca_zero(
        Array3UInt__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3UIntAlloc(items: Array<UInt>, size: Int = items.size): Array3UInt = Array3UIntAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3UInt.plus(offset: Int): CPointer<UInt> = CPointer<UInt>(addr(offset))
    operator fun Array3UInt.minus(offset: Int): CPointer<UInt> = CPointer<UInt>(addr(-offset))
    /////////////
    operator fun Array54Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array54Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array54Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array54FloatAlloc(setItems: Array54Float.() -> Unit): Array54Float = Array54Float(alloca_zero(
        Array54Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array54FloatAlloc(items: Array<Float>, size: Int = items.size): Array54Float = Array54FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array54Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array54Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array8Array23UByte.get(index: Int): Array23UByte = Array23UByte(addr(index))
    operator fun Array8Array23UByte.set(index: Int, value: Array23UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array8Array23UByte__ELEMENT_SIZE_BYTES) }
    var Array8Array23UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array8Array23UByteAlloc(setItems: Array8Array23UByte.() -> Unit): Array8Array23UByte = Array8Array23UByte(alloca_zero(
        Array8Array23UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array8Array23UByteAlloc(items: Array<Array23UByte>, size: Int = items.size): Array8Array23UByte = Array8Array23UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array8Array23UByte.plus(offset: Int): CPointer<Array23UByte> = CPointer<Array23UByte>(addr(offset))
    operator fun Array8Array23UByte.minus(offset: Int): CPointer<Array23UByte> = CPointer<Array23UByte>(addr(-offset))
    /////////////
    operator fun Array23UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array23UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array23UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array23UByteAlloc(setItems: Array23UByte.() -> Unit): Array23UByte = Array23UByte(alloca_zero(
        Array23UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array23UByteAlloc(items: Array<UByte>, size: Int = items.size): Array23UByte = Array23UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array23UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array23UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array8Array40UByte.get(index: Int): Array40UByte = Array40UByte(addr(index))
    operator fun Array8Array40UByte.set(index: Int, value: Array40UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array8Array40UByte__ELEMENT_SIZE_BYTES) }
    var Array8Array40UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array8Array40UByteAlloc(setItems: Array8Array40UByte.() -> Unit): Array8Array40UByte = Array8Array40UByte(alloca_zero(
        Array8Array40UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array8Array40UByteAlloc(items: Array<Array40UByte>, size: Int = items.size): Array8Array40UByte = Array8Array40UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array8Array40UByte.plus(offset: Int): CPointer<Array40UByte> = CPointer<Array40UByte>(addr(offset))
    operator fun Array8Array40UByte.minus(offset: Int): CPointer<Array40UByte> = CPointer<Array40UByte>(addr(-offset))
    /////////////
    operator fun Array40UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array40UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array40UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array40UByteAlloc(setItems: Array40UByte.() -> Unit): Array40UByte = Array40UByte(alloca_zero(
        Array40UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array40UByteAlloc(items: Array<UByte>, size: Int = items.size): Array40UByte = Array40UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array40UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array40UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array4Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array4Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array4Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array4FloatAlloc(setItems: Array4Float.() -> Unit): Array4Float = Array4Float(alloca_zero(
        Array4Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array4FloatAlloc(items: Array<Float>, size: Int = items.size): Array4Float = Array4FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array4Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array4Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array3Array28UByte.get(index: Int): Array28UByte = Array28UByte(addr(index))
    operator fun Array3Array28UByte.set(index: Int, value: Array28UByte) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array3Array28UByte__ELEMENT_SIZE_BYTES) }
    var Array3Array28UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3Array28UByteAlloc(setItems: Array3Array28UByte.() -> Unit): Array3Array28UByte = Array3Array28UByte(alloca_zero(
        Array3Array28UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3Array28UByteAlloc(items: Array<Array28UByte>, size: Int = items.size): Array3Array28UByte = Array3Array28UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3Array28UByte.plus(offset: Int): CPointer<Array28UByte> = CPointer<Array28UByte>(addr(offset))
    operator fun Array3Array28UByte.minus(offset: Int): CPointer<Array28UByte> = CPointer<Array28UByte>(addr(-offset))
    /////////////
    operator fun Array28UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array28UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array28UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array28UByteAlloc(setItems: Array28UByte.() -> Unit): Array28UByte = Array28UByte(alloca_zero(
        Array28UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array28UByteAlloc(items: Array<UByte>, size: Int = items.size): Array28UByte = Array28UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array28UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array28UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array16UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array16UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array16UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array16UByteAlloc(setItems: Array16UByte.() -> Unit): Array16UByte = Array16UByte(alloca_zero(
        Array16UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array16UByteAlloc(items: Array<UByte>, size: Int = items.size): Array16UByte = Array16UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array16UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array16UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array24UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array24UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array24UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array24UByteAlloc(setItems: Array24UByte.() -> Unit): Array24UByte = Array24UByte(alloca_zero(
        Array24UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array24UByteAlloc(items: Array<UByte>, size: Int = items.size): Array24UByte = Array24UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array24UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array24UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array10UByte.get(index: Int): UByte = lb(addr(index)).toUByte()
    operator fun Array10UByte.set(index: Int, value: UByte) { sb(addr(index), (value).toByte()) }
    var Array10UByte.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array10UByteAlloc(setItems: Array10UByte.() -> Unit): Array10UByte = Array10UByte(alloca_zero(
        Array10UByte__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array10UByteAlloc(items: Array<UByte>, size: Int = items.size): Array10UByte = Array10UByteAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array10UByte.plus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(offset))
    operator fun Array10UByte.minus(offset: Int): CPointer<UByte> = CPointer<UByte>(addr(-offset))
    /////////////
    operator fun Array145Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array145Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array145Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array145FloatAlloc(setItems: Array145Float.() -> Unit): Array145Float = Array145Float(alloca_zero(
        Array145Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array145FloatAlloc(items: Array<Float>, size: Int = items.size): Array145Float = Array145FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array145Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array145Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array32Short.get(index: Int): Short = lh(addr(index))
    operator fun Array32Short.set(index: Int, value: Short) { sh(addr(index), value) }
    var Array32Short.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array32ShortAlloc(setItems: Array32Short.() -> Unit): Array32Short = Array32Short(alloca_zero(
        Array32Short__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array32ShortAlloc(items: Array<Short>, size: Int = items.size): Array32Short = Array32ShortAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array32Short.plus(offset: Int): CPointer<Short> = CPointer<Short>(addr(offset))
    operator fun Array32Short.minus(offset: Int): CPointer<Short> = CPointer<Short>(addr(-offset))
    /////////////
    operator fun Array3Int.get(index: Int): Int = lw(addr(index))
    operator fun Array3Int.set(index: Int, value: Int) { sw(addr(index), value) }
    var Array3Int.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3IntAlloc(setItems: Array3Int.() -> Unit): Array3Int = Array3Int(alloca_zero(
        Array3Int__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3IntAlloc(items: Array<Int>, size: Int = items.size): Array3Int = Array3IntAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3Int.plus(offset: Int): IntPointer = IntPointer(addr(offset))
    operator fun Array3Int.minus(offset: Int): IntPointer = IntPointer(addr(-offset))
    /////////////
    operator fun Array14Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array14Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array14Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array14FloatAlloc(setItems: Array14Float.() -> Unit): Array14Float = Array14Float(alloca_zero(
        Array14Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array14FloatAlloc(items: Array<Float>, size: Int = items.size): Array14Float = Array14FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array14Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array14Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array2Array8Float.get(index: Int): Array8Float = Array8Float(addr(index))
    operator fun Array2Array8Float.set(index: Int, value: Array8Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array8Float__ELEMENT_SIZE_BYTES) }
    var Array2Array8Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array8FloatAlloc(setItems: Array2Array8Float.() -> Unit): Array2Array8Float = Array2Array8Float(alloca_zero(
        Array2Array8Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array8FloatAlloc(items: Array<Array8Float>, size: Int = items.size): Array2Array8Float = Array2Array8FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array8Float.plus(offset: Int): CPointer<Array8Float> = CPointer<Array8Float>(addr(offset))
    operator fun Array2Array8Float.minus(offset: Int): CPointer<Array8Float> = CPointer<Array8Float>(addr(-offset))
    /////////////
    operator fun Array8Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array8Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array8Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array8FloatAlloc(setItems: Array8Float.() -> Unit): Array8Float = Array8Float(alloca_zero(
        Array8Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array8FloatAlloc(items: Array<Float>, size: Int = items.size): Array8Float = Array8FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array8Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array8Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array18Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array18Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array18Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array18FloatAlloc(setItems: Array18Float.() -> Unit): Array18Float = Array18Float(alloca_zero(
        Array18Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array18FloatAlloc(items: Array<Float>, size: Int = items.size): Array18Float = Array18FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array18Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array18Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array9Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array9Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array9Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array9FloatAlloc(setItems: Array9Float.() -> Unit): Array9Float = Array9Float(alloca_zero(
        Array9Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array9FloatAlloc(items: Array<Float>, size: Int = items.size): Array9Float = Array9FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array9Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array9Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array6Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array6Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array6Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array6FloatAlloc(setItems: Array6Float.() -> Unit): Array6Float = Array6Float(alloca_zero(
        Array6Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array6FloatAlloc(items: Array<Float>, size: Int = items.size): Array6Float = Array6FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array6Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array6Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array3Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array3Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array3Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array3FloatAlloc(setItems: Array3Float.() -> Unit): Array3Float = Array3Float(alloca_zero(
        Array3Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array3FloatAlloc(items: Array<Float>, size: Int = items.size): Array3Float = Array3FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array3Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array3Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array2Array18Float.get(index: Int): Array18Float = Array18Float(addr(index))
    operator fun Array2Array18Float.set(index: Int, value: Array18Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array2Array18Float__ELEMENT_SIZE_BYTES) }
    var Array2Array18Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array2Array18FloatAlloc(setItems: Array2Array18Float.() -> Unit): Array2Array18Float = Array2Array18Float(alloca_zero(
        Array2Array18Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array2Array18FloatAlloc(items: Array<Array18Float>, size: Int = items.size): Array2Array18Float = Array2Array18FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array2Array18Float.plus(offset: Int): CPointer<Array18Float> = CPointer<Array18Float>(addr(offset))
    operator fun Array2Array18Float.minus(offset: Int): CPointer<Array18Float> = CPointer<Array18Float>(addr(-offset))
    /////////////
    operator fun Array24Float.get(index: Int): Float = lwf(addr(index))
    operator fun Array24Float.set(index: Int, value: Float) { swf(addr(index), (value)) }
    var Array24Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array24FloatAlloc(setItems: Array24Float.() -> Unit): Array24Float = Array24Float(alloca_zero(
        Array24Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array24FloatAlloc(items: Array<Float>, size: Int = items.size): Array24Float = Array24FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array24Float.plus(offset: Int): FloatPointer = FloatPointer(addr(offset))
    operator fun Array24Float.minus(offset: Int): FloatPointer = FloatPointer(addr(-offset))
    /////////////
    operator fun Array4Array8Float.get(index: Int): Array8Float = Array8Float(addr(index))
    operator fun Array4Array8Float.set(index: Int, value: Array8Float) { memcpy(CPointer(addr(index)), CPointer(value.ptr), Array4Array8Float__ELEMENT_SIZE_BYTES) }
    var Array4Array8Float.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array4Array8FloatAlloc(setItems: Array4Array8Float.() -> Unit): Array4Array8Float = Array4Array8Float(alloca_zero(
        Array4Array8Float__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array4Array8FloatAlloc(items: Array<Array8Float>, size: Int = items.size): Array4Array8Float = Array4Array8FloatAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array4Array8Float.plus(offset: Int): CPointer<Array8Float> = CPointer<Array8Float>(addr(offset))
    operator fun Array4Array8Float.minus(offset: Int): CPointer<Array8Float> = CPointer<Array8Float>(addr(-offset))
    /////////////
    operator fun Array1bs_t.get(index: Int): bs_t = bs_t(addr(index))
    operator fun Array1bs_t.set(index: Int, value: bs_t) { bs_t(addr(index)).copyFrom(value) }
    var Array1bs_t.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array1bs_tAlloc(setItems: Array1bs_t.() -> Unit): Array1bs_t = Array1bs_t(alloca_zero(
        Array1bs_t__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array1bs_tAlloc(items: Array<bs_t>, size: Int = items.size): Array1bs_t = Array1bs_tAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array1bs_t.plus(offset: Int): CPointer<bs_t> = CPointer<bs_t>(addr(offset))
    operator fun Array1bs_t.minus(offset: Int): CPointer<bs_t> = CPointer<bs_t>(addr(-offset))
    /////////////
    operator fun Array1L12_scale_info.get(index: Int): L12_scale_info = L12_scale_info(addr(index))
    operator fun Array1L12_scale_info.set(index: Int, value: L12_scale_info) { L12_scale_info(addr(index)).copyFrom(value) }
    var Array1L12_scale_info.value get() = this[0]; set(value) { this[0] = value }
    inline fun Array1L12_scale_infoAlloc(setItems: Array1L12_scale_info.() -> Unit): Array1L12_scale_info = Array1L12_scale_info(alloca_zero(
        Array1L12_scale_info__TOTAL_SIZE_BYTES
    ).ptr).apply(setItems)
    fun Array1L12_scale_infoAlloc(items: Array<L12_scale_info>, size: Int = items.size): Array1L12_scale_info = Array1L12_scale_infoAlloc { for (n in 0 until size) this[n] = items[n] }
    operator fun Array1L12_scale_info.plus(offset: Int): CPointer<L12_scale_info> = CPointer<L12_scale_info>(addr(offset))
    operator fun Array1L12_scale_info.minus(offset: Int): CPointer<L12_scale_info> = CPointer<L12_scale_info>(addr(-offset))
}

//////////////////
// C STRUCTURES //
//////////////////

//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class mp3dec_frame_info_t(val ptr: Int)
internal const val mp3dec_frame_info_t__SIZE_BYTES = 24
internal const val mp3dec_frame_info_t__OFFSET_frame_bytes = 0
internal const val mp3dec_frame_info_t__OFFSET_frame_offset = 4
internal const val mp3dec_frame_info_t__OFFSET_channels = 8
internal const val mp3dec_frame_info_t__OFFSET_hz = 12
internal const val mp3dec_frame_info_t__OFFSET_layer = 16
internal const val mp3dec_frame_info_t__OFFSET_bitrate_kbps = 20
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class mp3dec_t(val ptr: Int)
internal const val mp3dec_t__SIZE_BYTES = 6667
internal const val mp3dec_t__OFFSET_mdct_overlap = 0
internal const val mp3dec_t__OFFSET_qmf_state = 2304
internal const val mp3dec_t__OFFSET_reserv = 6144
internal const val mp3dec_t__OFFSET_free_format_bytes = 6148
internal const val mp3dec_t__OFFSET_header = 6152
internal const val mp3dec_t__OFFSET_reserv_buf = 6156
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class bs_t(val ptr: Int)
internal const val bs_t__SIZE_BYTES = 12
internal const val bs_t__OFFSET_buf = 0
internal const val bs_t__OFFSET_pos = 4
internal const val bs_t__OFFSET_limit = 8
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class L12_scale_info(val ptr: Int)
internal const val L12_scale_info__SIZE_BYTES = 898
internal const val L12_scale_info__OFFSET_scf = 0
internal const val L12_scale_info__OFFSET_total_bands = 768
internal const val L12_scale_info__OFFSET_stereo_bands = 769
internal const val L12_scale_info__OFFSET_bitalloc = 770
internal const val L12_scale_info__OFFSET_scfcod = 834
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class L12_subband_alloc_t(val ptr: Int)
internal const val L12_subband_alloc_t__SIZE_BYTES = 3
internal const val L12_subband_alloc_t__OFFSET_tab_offset = 0
internal const val L12_subband_alloc_t__OFFSET_code_tab_width = 1
internal const val L12_subband_alloc_t__OFFSET_band_count = 2
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class L3_gr_info_t(val ptr: Int)
internal const val L3_gr_info_t__SIZE_BYTES = 28
internal const val L3_gr_info_t__OFFSET_sfbtab = 0
internal const val L3_gr_info_t__OFFSET_part_23_length = 4
internal const val L3_gr_info_t__OFFSET_big_values = 6
internal const val L3_gr_info_t__OFFSET_scalefac_compress = 8
internal const val L3_gr_info_t__OFFSET_global_gain = 10
internal const val L3_gr_info_t__OFFSET_block_type = 11
internal const val L3_gr_info_t__OFFSET_mixed_block_flag = 12
internal const val L3_gr_info_t__OFFSET_n_long_sfb = 13
internal const val L3_gr_info_t__OFFSET_n_short_sfb = 14
internal const val L3_gr_info_t__OFFSET_table_select = 15
internal const val L3_gr_info_t__OFFSET_region_count = 18
internal const val L3_gr_info_t__OFFSET_subblock_gain = 21
internal const val L3_gr_info_t__OFFSET_preflag = 24
internal const val L3_gr_info_t__OFFSET_scalefac_scale = 25
internal const val L3_gr_info_t__OFFSET_count1_table = 26
internal const val L3_gr_info_t__OFFSET_scfsi = 27
//////////////////
internal @kotlin.jvm.JvmInline value/*!*/ class mp3dec_scratch_t(val ptr: Int)
internal const val mp3dec_scratch_t__SIZE_BYTES = 16233
internal const val mp3dec_scratch_t__OFFSET_bs = 0
internal const val mp3dec_scratch_t__OFFSET_maindata = 12
internal const val mp3dec_scratch_t__OFFSET_gr_info = 2827
internal const val mp3dec_scratch_t__OFFSET_grbuf = 2939
internal const val mp3dec_scratch_t__OFFSET_scf = 7547
internal const val mp3dec_scratch_t__OFFSET_syn = 7707
internal const val mp3dec_scratch_t__OFFSET_ist_pos = 16155
internal const val Array2Array288Float__NUM_ELEMENTS = 2
internal const val Array2Array288Float__ELEMENT_SIZE_BYTES = 1152
internal const val Array2Array288Float__TOTAL_SIZE_BYTES = 2304
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array288Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array288Float__ELEMENT_SIZE_BYTES
}
internal const val Array288Float__NUM_ELEMENTS = 288
internal const val Array288Float__ELEMENT_SIZE_BYTES = 4
internal const val Array288Float__TOTAL_SIZE_BYTES = 1152
internal @kotlin.jvm.JvmInline value/*!*/ class Array288Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array288Float__ELEMENT_SIZE_BYTES
}
internal const val Array960Float__NUM_ELEMENTS = 960
internal const val Array960Float__ELEMENT_SIZE_BYTES = 4
internal const val Array960Float__TOTAL_SIZE_BYTES = 3840
internal @kotlin.jvm.JvmInline value/*!*/ class Array960Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array960Float__ELEMENT_SIZE_BYTES
}
internal const val Array4UByte__NUM_ELEMENTS = 4
internal const val Array4UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array4UByte__TOTAL_SIZE_BYTES = 4
internal @kotlin.jvm.JvmInline value/*!*/ class Array4UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array4UByte__ELEMENT_SIZE_BYTES
}
internal const val Array511UByte__NUM_ELEMENTS = 511
internal const val Array511UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array511UByte__TOTAL_SIZE_BYTES = 511
internal @kotlin.jvm.JvmInline value/*!*/ class Array511UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array511UByte__ELEMENT_SIZE_BYTES
}
internal const val Array192Float__NUM_ELEMENTS = 192
internal const val Array192Float__ELEMENT_SIZE_BYTES = 4
internal const val Array192Float__TOTAL_SIZE_BYTES = 768
internal @kotlin.jvm.JvmInline value/*!*/ class Array192Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array192Float__ELEMENT_SIZE_BYTES
}
internal const val Array64UByte__NUM_ELEMENTS = 64
internal const val Array64UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array64UByte__TOTAL_SIZE_BYTES = 64
internal @kotlin.jvm.JvmInline value/*!*/ class Array64UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array64UByte__ELEMENT_SIZE_BYTES
}
internal const val Array3UByte__NUM_ELEMENTS = 3
internal const val Array3UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array3UByte__TOTAL_SIZE_BYTES = 3
internal @kotlin.jvm.JvmInline value/*!*/ class Array3UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3UByte__ELEMENT_SIZE_BYTES
}
internal const val Array2815UByte__NUM_ELEMENTS = 2815
internal const val Array2815UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array2815UByte__TOTAL_SIZE_BYTES = 2815
internal @kotlin.jvm.JvmInline value/*!*/ class Array2815UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2815UByte__ELEMENT_SIZE_BYTES
}
internal const val Array4L3_gr_info_t__NUM_ELEMENTS = 4
internal const val Array4L3_gr_info_t__ELEMENT_SIZE_BYTES = 28
internal const val Array4L3_gr_info_t__TOTAL_SIZE_BYTES = 112
internal @kotlin.jvm.JvmInline value/*!*/ class Array4L3_gr_info_t(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array4L3_gr_info_t__ELEMENT_SIZE_BYTES
}
internal const val Array2Array576Float__NUM_ELEMENTS = 2
internal const val Array2Array576Float__ELEMENT_SIZE_BYTES = 2304
internal const val Array2Array576Float__TOTAL_SIZE_BYTES = 4608
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array576Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array576Float__ELEMENT_SIZE_BYTES
}
internal const val Array576Float__NUM_ELEMENTS = 576
internal const val Array576Float__ELEMENT_SIZE_BYTES = 4
internal const val Array576Float__TOTAL_SIZE_BYTES = 2304
internal @kotlin.jvm.JvmInline value/*!*/ class Array576Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array576Float__ELEMENT_SIZE_BYTES
}
internal const val Array40Float__NUM_ELEMENTS = 40
internal const val Array40Float__ELEMENT_SIZE_BYTES = 4
internal const val Array40Float__TOTAL_SIZE_BYTES = 160
internal @kotlin.jvm.JvmInline value/*!*/ class Array40Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array40Float__ELEMENT_SIZE_BYTES
}
internal const val Array33Array64Float__NUM_ELEMENTS = 33
internal const val Array33Array64Float__ELEMENT_SIZE_BYTES = 256
internal const val Array33Array64Float__TOTAL_SIZE_BYTES = 8448
internal @kotlin.jvm.JvmInline value/*!*/ class Array33Array64Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array33Array64Float__ELEMENT_SIZE_BYTES
}
internal const val Array64Float__NUM_ELEMENTS = 64
internal const val Array64Float__ELEMENT_SIZE_BYTES = 4
internal const val Array64Float__TOTAL_SIZE_BYTES = 256
internal @kotlin.jvm.JvmInline value/*!*/ class Array64Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array64Float__ELEMENT_SIZE_BYTES
}
internal const val Array2Array39UByte__NUM_ELEMENTS = 2
internal const val Array2Array39UByte__ELEMENT_SIZE_BYTES = 39
internal const val Array2Array39UByte__TOTAL_SIZE_BYTES = 78
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array39UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array39UByte__ELEMENT_SIZE_BYTES
}
internal const val Array39UByte__NUM_ELEMENTS = 39
internal const val Array39UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array39UByte__TOTAL_SIZE_BYTES = 39
internal @kotlin.jvm.JvmInline value/*!*/ class Array39UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array39UByte__ELEMENT_SIZE_BYTES
}
internal const val Array2Array3Array15UByte__NUM_ELEMENTS = 2
internal const val Array2Array3Array15UByte__ELEMENT_SIZE_BYTES = 45
internal const val Array2Array3Array15UByte__TOTAL_SIZE_BYTES = 90
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array3Array15UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array3Array15UByte__ELEMENT_SIZE_BYTES
}
internal const val Array3Array15UByte__NUM_ELEMENTS = 3
internal const val Array3Array15UByte__ELEMENT_SIZE_BYTES = 15
internal const val Array3Array15UByte__TOTAL_SIZE_BYTES = 45
internal @kotlin.jvm.JvmInline value/*!*/ class Array3Array15UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3Array15UByte__ELEMENT_SIZE_BYTES
}
internal const val Array15UByte__NUM_ELEMENTS = 15
internal const val Array15UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array15UByte__TOTAL_SIZE_BYTES = 15
internal @kotlin.jvm.JvmInline value/*!*/ class Array15UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array15UByte__ELEMENT_SIZE_BYTES
}
internal const val Array3UInt__NUM_ELEMENTS = 3
internal const val Array3UInt__ELEMENT_SIZE_BYTES = 4
internal const val Array3UInt__TOTAL_SIZE_BYTES = 12
internal @kotlin.jvm.JvmInline value/*!*/ class Array3UInt(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3UInt__ELEMENT_SIZE_BYTES
}
internal const val Array54Float__NUM_ELEMENTS = 54
internal const val Array54Float__ELEMENT_SIZE_BYTES = 4
internal const val Array54Float__TOTAL_SIZE_BYTES = 216
internal @kotlin.jvm.JvmInline value/*!*/ class Array54Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array54Float__ELEMENT_SIZE_BYTES
}
internal const val Array8Array23UByte__NUM_ELEMENTS = 8
internal const val Array8Array23UByte__ELEMENT_SIZE_BYTES = 23
internal const val Array8Array23UByte__TOTAL_SIZE_BYTES = 184
internal @kotlin.jvm.JvmInline value/*!*/ class Array8Array23UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array8Array23UByte__ELEMENT_SIZE_BYTES
}
internal const val Array23UByte__NUM_ELEMENTS = 23
internal const val Array23UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array23UByte__TOTAL_SIZE_BYTES = 23
internal @kotlin.jvm.JvmInline value/*!*/ class Array23UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array23UByte__ELEMENT_SIZE_BYTES
}
internal const val Array8Array40UByte__NUM_ELEMENTS = 8
internal const val Array8Array40UByte__ELEMENT_SIZE_BYTES = 40
internal const val Array8Array40UByte__TOTAL_SIZE_BYTES = 320
internal @kotlin.jvm.JvmInline value/*!*/ class Array8Array40UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array8Array40UByte__ELEMENT_SIZE_BYTES
}
internal const val Array40UByte__NUM_ELEMENTS = 40
internal const val Array40UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array40UByte__TOTAL_SIZE_BYTES = 40
internal @kotlin.jvm.JvmInline value/*!*/ class Array40UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array40UByte__ELEMENT_SIZE_BYTES
}
internal const val Array4Float__NUM_ELEMENTS = 4
internal const val Array4Float__ELEMENT_SIZE_BYTES = 4
internal const val Array4Float__TOTAL_SIZE_BYTES = 16
internal @kotlin.jvm.JvmInline value/*!*/ class Array4Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array4Float__ELEMENT_SIZE_BYTES
}
internal const val Array3Array28UByte__NUM_ELEMENTS = 3
internal const val Array3Array28UByte__ELEMENT_SIZE_BYTES = 28
internal const val Array3Array28UByte__TOTAL_SIZE_BYTES = 84
internal @kotlin.jvm.JvmInline value/*!*/ class Array3Array28UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3Array28UByte__ELEMENT_SIZE_BYTES
}
internal const val Array28UByte__NUM_ELEMENTS = 28
internal const val Array28UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array28UByte__TOTAL_SIZE_BYTES = 28
internal @kotlin.jvm.JvmInline value/*!*/ class Array28UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array28UByte__ELEMENT_SIZE_BYTES
}
internal const val Array16UByte__NUM_ELEMENTS = 16
internal const val Array16UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array16UByte__TOTAL_SIZE_BYTES = 16
internal @kotlin.jvm.JvmInline value/*!*/ class Array16UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array16UByte__ELEMENT_SIZE_BYTES
}
internal const val Array24UByte__NUM_ELEMENTS = 24
internal const val Array24UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array24UByte__TOTAL_SIZE_BYTES = 24
internal @kotlin.jvm.JvmInline value/*!*/ class Array24UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array24UByte__ELEMENT_SIZE_BYTES
}
internal const val Array10UByte__NUM_ELEMENTS = 10
internal const val Array10UByte__ELEMENT_SIZE_BYTES = 1
internal const val Array10UByte__TOTAL_SIZE_BYTES = 10
internal @kotlin.jvm.JvmInline value/*!*/ class Array10UByte(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array10UByte__ELEMENT_SIZE_BYTES
}
internal const val Array145Float__NUM_ELEMENTS = 145
internal const val Array145Float__ELEMENT_SIZE_BYTES = 4
internal const val Array145Float__TOTAL_SIZE_BYTES = 580
internal @kotlin.jvm.JvmInline value/*!*/ class Array145Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array145Float__ELEMENT_SIZE_BYTES
}
internal const val Array32Short__NUM_ELEMENTS = 32
internal const val Array32Short__ELEMENT_SIZE_BYTES = 2
internal const val Array32Short__TOTAL_SIZE_BYTES = 64
internal @kotlin.jvm.JvmInline value/*!*/ class Array32Short(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array32Short__ELEMENT_SIZE_BYTES
}
internal const val Array3Int__NUM_ELEMENTS = 3
internal const val Array3Int__ELEMENT_SIZE_BYTES = 4
internal const val Array3Int__TOTAL_SIZE_BYTES = 12
internal @kotlin.jvm.JvmInline value/*!*/ class Array3Int(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3Int__ELEMENT_SIZE_BYTES
}
internal const val Array14Float__NUM_ELEMENTS = 14
internal const val Array14Float__ELEMENT_SIZE_BYTES = 4
internal const val Array14Float__TOTAL_SIZE_BYTES = 56
internal @kotlin.jvm.JvmInline value/*!*/ class Array14Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array14Float__ELEMENT_SIZE_BYTES
}
internal const val Array2Array8Float__NUM_ELEMENTS = 2
internal const val Array2Array8Float__ELEMENT_SIZE_BYTES = 32
internal const val Array2Array8Float__TOTAL_SIZE_BYTES = 64
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array8Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array8Float__ELEMENT_SIZE_BYTES
}
internal const val Array8Float__NUM_ELEMENTS = 8
internal const val Array8Float__ELEMENT_SIZE_BYTES = 4
internal const val Array8Float__TOTAL_SIZE_BYTES = 32
internal @kotlin.jvm.JvmInline value/*!*/ class Array8Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array8Float__ELEMENT_SIZE_BYTES
}
internal const val Array18Float__NUM_ELEMENTS = 18
internal const val Array18Float__ELEMENT_SIZE_BYTES = 4
internal const val Array18Float__TOTAL_SIZE_BYTES = 72
internal @kotlin.jvm.JvmInline value/*!*/ class Array18Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array18Float__ELEMENT_SIZE_BYTES
}
internal const val Array9Float__NUM_ELEMENTS = 9
internal const val Array9Float__ELEMENT_SIZE_BYTES = 4
internal const val Array9Float__TOTAL_SIZE_BYTES = 36
internal @kotlin.jvm.JvmInline value/*!*/ class Array9Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array9Float__ELEMENT_SIZE_BYTES
}
internal const val Array6Float__NUM_ELEMENTS = 6
internal const val Array6Float__ELEMENT_SIZE_BYTES = 4
internal const val Array6Float__TOTAL_SIZE_BYTES = 24
internal @kotlin.jvm.JvmInline value/*!*/ class Array6Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array6Float__ELEMENT_SIZE_BYTES
}
internal const val Array3Float__NUM_ELEMENTS = 3
internal const val Array3Float__ELEMENT_SIZE_BYTES = 4
internal const val Array3Float__TOTAL_SIZE_BYTES = 12
internal @kotlin.jvm.JvmInline value/*!*/ class Array3Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array3Float__ELEMENT_SIZE_BYTES
}
internal const val Array2Array18Float__NUM_ELEMENTS = 2
internal const val Array2Array18Float__ELEMENT_SIZE_BYTES = 72
internal const val Array2Array18Float__TOTAL_SIZE_BYTES = 144
internal @kotlin.jvm.JvmInline value/*!*/ class Array2Array18Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array2Array18Float__ELEMENT_SIZE_BYTES
}
internal const val Array24Float__NUM_ELEMENTS = 24
internal const val Array24Float__ELEMENT_SIZE_BYTES = 4
internal const val Array24Float__TOTAL_SIZE_BYTES = 96
internal @kotlin.jvm.JvmInline value/*!*/ class Array24Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array24Float__ELEMENT_SIZE_BYTES
}
internal const val Array4Array8Float__NUM_ELEMENTS = 4
internal const val Array4Array8Float__ELEMENT_SIZE_BYTES = 32
internal const val Array4Array8Float__TOTAL_SIZE_BYTES = 128
internal @kotlin.jvm.JvmInline value/*!*/ class Array4Array8Float(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array4Array8Float__ELEMENT_SIZE_BYTES
}
internal const val Array1bs_t__NUM_ELEMENTS = 1
internal const val Array1bs_t__ELEMENT_SIZE_BYTES = 12
internal const val Array1bs_t__TOTAL_SIZE_BYTES = 12
internal @kotlin.jvm.JvmInline value/*!*/ class Array1bs_t(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array1bs_t__ELEMENT_SIZE_BYTES
}
internal const val Array1L12_scale_info__NUM_ELEMENTS = 1
internal const val Array1L12_scale_info__ELEMENT_SIZE_BYTES = 898
internal const val Array1L12_scale_info__TOTAL_SIZE_BYTES = 898
internal @kotlin.jvm.JvmInline value/*!*/ class Array1L12_scale_info(val ptr: Int) {
    fun addr(index: Int) = ptr + index * Array1L12_scale_info__ELEMENT_SIZE_BYTES
}
// KTCC RUNTIME ///////////////////////////////////////////////////

@Suppress("MemberVisibilityCanBePrivate", "FunctionName", "CanBeVal", "DoubleNegation", "LocalVariableName", "NAME_SHADOWING", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "RemoveRedundantCallsOfConversionMethods", "EXPERIMENTAL_IS_NOT_ENABLED", "RedundantExplicitType", "RemoveExplicitTypeArguments", "RedundantExplicitType", "unused", "UNCHECKED_CAST", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "NOTHING_TO_INLINE", "PropertyName", "ClassName", "USELESS_CAST", "PrivatePropertyName", "CanBeParameter", "UnusedMainParameter")
@OptIn(ExperimentalUnsignedTypes::class)
internal abstract class AbstractRuntime(val REQUESTED_HEAP_SIZE: Int = 0, val REQUESTED_STACK_PTR: Int = 0, val __syscalls: RuntimeSyscalls = DummyRuntimeSyscalls) : RuntimeSyscalls by __syscalls {
    val HEAP_SIZE: Int = if (REQUESTED_HEAP_SIZE <= 0) 16 * 1024 * 1024 else REQUESTED_HEAP_SIZE // 16 MB default
    var STACK_PTR: Int = if (REQUESTED_STACK_PTR == 0) HEAP_SIZE else REQUESTED_STACK_PTR // 0.5 MB
    var HEAP_PTR: Int = 128

    ///////////////////////////////////
    // MEMORY TRANSFER / STRING/MEMORY OPERATIONS
    ///////////////////////////////////

    abstract fun lb(ptr: Int): Byte
    abstract fun sb(ptr: Int, value: Byte): Unit
    abstract fun memset(ptr: CPointer<*>, value: Int, num: Int): CPointer<Unit>
    abstract fun memmove(dest: CPointer<Unit>, src: CPointer<Unit>, num: Int): CPointer<Unit>
    abstract fun memcpy(dest: CPointer<Unit>, src: CPointer<Unit>, num: Int): CPointer<Unit>

    fun lbu(ptr: Int): Int = lb(ptr).toInt() and 0xFF
    fun lhu(ptr: Int): Int = lh(ptr).toInt() and 0xFFFF
    fun lwu(ptr: Int): Long = lw(ptr).toLong() and 0xFFFFFFFFL

    open fun lh(ptr: Int): Short = ((lbu(ptr) shl 0) or (lbu(ptr + 1) shl 8)).toShort()
    open fun sh(ptr: Int, value: Short) { sb(ptr, (value.toInt() ushr 0).toByte()); sb(ptr + 1, (value.toInt() ushr 8).toByte()) }

    open fun lw(ptr: Int): Int = ((lbu(ptr) shl 0) or (lbu(ptr + 1) shl 8) or (lbu(ptr + 2) shl 16) or (lbu(ptr + 3) shl 24))
    open fun sw(ptr: Int, value: Int) { sb(ptr + 0, (value ushr 0).toByte()); sb(ptr + 1, (value ushr 8).toByte()); sb(ptr + 2, (value ushr 16).toByte()); sb(ptr + 3, (value ushr 24).toByte()) }

    open fun ld(ptr: Int): Long = (lwu(ptr) shl 0) or (lwu(ptr + 4) shl 32)
    open fun sd(ptr: Int, value: Long) { sw(ptr, (value ushr 0).toInt()); sw(ptr + 4, (value ushr 32).toInt()) }

    open fun lwf(ptr: Int): Float = Float.fromBits(lw(ptr))
    open fun swf(ptr: Int, value: Float) { sw(ptr, value.toRawBits()) }

    open fun ldf(ptr: Int): Double = Double.fromBits(ld(ptr))
    open fun sdf(ptr: Int, value: Double) { sd(ptr, value.toRawBits()) }

    open fun memWrite(ptr: CPointer<*>, data: ByteArray, offset: Int = 0, size: Int = data.size) { for (n in offset until offset + size) sb(ptr.ptr + n, data[n]) }
    open fun memRead(ptr: CPointer<*>, data: ByteArray, offset: Int = 0, size: Int = data.size) { for (n in offset until offset + size) data[n] = lb(ptr.ptr + n) }
    open fun memWrite(ptr: CPointer<*>, data: ShortArray, offset: Int = 0, size: Int = data.size) { for (n in offset until offset + size) sh(ptr.ptr + n * 2, data[n]) }
    open fun memRead(ptr: CPointer<*>, data: ShortArray, offset: Int = 0, size: Int = data.size) { for (n in offset until offset + size) data[n] = lh(ptr.ptr + n * 2) }

    ///////////////////////////////////
    // CASTING OPERATIONS
    ///////////////////////////////////

    fun Boolean.toInt(): Int = if (this) 1 else 0
    fun CPointer<*>.toInt(): Int = ptr
    fun CPointer<*>.toBool(): Boolean = ptr != 0
    fun Byte.toBool(): Boolean = this.toInt() != 0
    fun Short.toBool(): Boolean = this.toInt() != 0
    fun Int.toBool(): Boolean = this != 0
    fun Long.toBool(): Boolean = this != 0L
    fun Float.toBool(): Boolean = this != 0f
    fun Double.toBool(): Boolean = this != 0.0
    fun UByte.toBool(): Boolean = this.toInt() != 0
    fun UShort.toBool(): Boolean = this.toInt() != 0
    fun UInt.toBool(): Boolean = this.toInt() != 0
    fun ULong.toBool(): Boolean = this.toInt() != 0
    fun Boolean.toBool(): Boolean = this

    ///////////////////////////////////
    // POINTER OPERATIONS
    ///////////////////////////////////

    fun <T> CPointer<T>.addPtr(offset: Int, elementSize: Int): CPointer<T> = CPointer<T>(this.ptr + offset * elementSize)

    // void**
    @kotlin.jvm.JvmName("plusPtr") operator fun <T> CPointer<CPointer<T>>.plus(offset: Int): CPointer<CPointer<T>> = addPtr<CPointer<T>>(offset, 4)
    @kotlin.jvm.JvmName("minusPtr") operator fun <T> CPointer<CPointer<T>>.minus(offset: Int): CPointer<CPointer<T>> = addPtr<CPointer<T>>(-offset, 4)
    @get:kotlin.jvm.JvmName("getPtrValue") var <T> CPointer<CPointer<T>>.value: CPointer<T> get() = this[0]; set(value) { this[0] = value }
    fun <T> CPointer<CPointer<T>>.minusPtrPtr(other: CPointer<CPointer<T>>): Int = (this.ptr - other.ptr) / 4
    operator fun <T> CPointer<CPointer<T>>.set(offset: Int, value: CPointer<T>): Unit = sw(this.ptr + offset * 4, value.ptr)
    @kotlin.jvm.JvmName("getPtrPtr") operator fun <T> CPointer<CPointer<T>>.get(offset: Int): CPointer<T> = CPointer(lw(this.ptr + offset * 4))

    // char*
    @kotlin.jvm.JvmName("getterByte") operator fun CPointer<Byte>.get(offset: Int): Byte = lb(this.ptr + offset * 1)
    @kotlin.jvm.JvmName("setterByte") operator fun CPointer<Byte>.set(offset: Int, value: Byte): Unit = sb(this.ptr + offset * 1, value)
    @set:kotlin.jvm.JvmName("setter_Byte_value") @get:kotlin.jvm.JvmName("getter_Byte_value") var CPointer<Byte>.value: Byte get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusByte") operator fun CPointer<Byte>.plus(offset: Int): CPointer<Byte> = addPtr<Byte>(offset, 1)
    @kotlin.jvm.JvmName("minusByte") operator fun CPointer<Byte>.minus(offset: Int): CPointer<Byte> = addPtr<Byte>(-offset, 1)
    fun CPointer<Byte>.minusPtrByte(other: CPointer<Byte>): Int = (this.ptr - other.ptr) / 1
    inline fun fixedArrayOfByte(size: Int, setItems: CPointer<Byte>.() -> Unit): CPointer<Byte> = CPointer<Byte>(alloca_zero(size * 1).ptr).apply(setItems)
    fun fixedArrayOfByte(vararg values: Byte, size: Int = values.size): CPointer<Byte> = fixedArrayOfByte(size) { for (n in 0 until values.size) this[n] = values[n] }
    fun fixedArrayOfByte(values: String, size: Int = values.length): CPointer<Byte> = fixedArrayOfByte(size) { for (n in 0 until values.length) this[n] = values[n].code.toByte() }

    // short*
    @kotlin.jvm.JvmName("getterShort") operator fun CPointer<Short>.get(offset: Int): Short = lh(this.ptr + offset * 2)
    @kotlin.jvm.JvmName("setterShort") operator fun CPointer<Short>.set(offset: Int, value: Short): Unit = sh(this.ptr + offset * 2, value)
    @set:kotlin.jvm.JvmName("setter_Short_value") @get:kotlin.jvm.JvmName("getter_Short_value") var CPointer<Short>.value: Short get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusShort") operator fun CPointer<Short>.plus(offset: Int): CPointer<Short> = addPtr<Short>(offset, 2)
    @kotlin.jvm.JvmName("minusShort") operator fun CPointer<Short>.minus(offset: Int): CPointer<Short> = addPtr<Short>(-offset, 2)
    fun CPointer<Short>.minusPtrShort(other: CPointer<Short>): Int = (this.ptr - other.ptr) / 2
    inline fun fixedArrayOfShort(size: Int, setItems: CPointer<Short>.() -> Unit): CPointer<Short> = CPointer<Short>(alloca_zero(size * 2).ptr).apply(setItems)
    fun fixedArrayOfShort(vararg values: Short, size: Int = values.size): CPointer<Short> = fixedArrayOfShort(size) { for (n in 0 until values.size) this[n] = values[n] }
    fun fixedArrayOfShort(values: String, size: Int = values.length): CPointer<Short> = fixedArrayOfShort(size) { for (n in 0 until values.length) this[n] = values[n].code.toShort() }

    // int*
    @kotlin.jvm.JvmName("getterInt") operator fun IntPointer.get(offset: Int): Int = lw(this.ptr + offset * 4)
    @kotlin.jvm.JvmName("setterInt") operator fun IntPointer.set(offset: Int, value: Int): Unit = sw(this.ptr + offset * 4, value)
    @set:kotlin.jvm.JvmName("setter_Int_value") @get:kotlin.jvm.JvmName("getter_Int_value") var IntPointer.value: Int get() = this[0]; set(value) { this[0] = value }
    inline fun fixedArrayOfInt(size: Int, setItems: IntPointer.() -> Unit): IntPointer = IntPointer(alloca_zero(size * 4).ptr).apply(setItems)
    fun fixedArrayOfInt(vararg values: Int, size: Int = values.size): IntPointer = fixedArrayOfInt(size) { for (n in 0 until values.size) this[n] = values[n] }
    ///////////////////////////////////////

    // long*
    @kotlin.jvm.JvmName("getterLong") operator fun CPointer<Long>.get(offset: Int): Long = ld(this.ptr + offset * 8)
    @kotlin.jvm.JvmName("setterLong") operator fun CPointer<Long>.set(offset: Int, value: Long): Unit = sd(this.ptr + offset * 8, value)
    @set:kotlin.jvm.JvmName("setter_Long_value") @get:kotlin.jvm.JvmName("getter_Long_value") var CPointer<Long>.value: Long get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusLong") operator fun CPointer<Long>.plus(offset: Int): CPointer<Long> = addPtr<Long>(offset, 8)
    @kotlin.jvm.JvmName("minusLong") operator fun CPointer<Long>.minus(offset: Int): CPointer<Long> = addPtr<Long>(-offset, 8)
    fun CPointer<Long>.minusPtrLong(other: CPointer<Long>): Int = (this.ptr - other.ptr) / 8
    inline fun fixedArrayOfLong(size: Int, setItems: CPointer<Long>.() -> Unit): CPointer<Long> = CPointer<Long>(alloca_zero(size * 8).ptr).apply(setItems)
    fun fixedArrayOfLong(vararg values: Long, size: Int = values.size): CPointer<Long> = fixedArrayOfLong(size) { for (n in 0 until values.size) this[n] = values[n] }

    operator fun CPointer<UByte>.get(offset: Int): UByte = lb(this.ptr + offset * 1).toUByte()
    operator fun CPointer<UByte>.set(offset: Int, value: UByte): Unit = sb(this.ptr + offset * 1, (value).toByte())
    var CPointer<UByte>.value: UByte get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusUByte") operator fun CPointer<UByte>.plus(offset: Int): CPointer<UByte> = addPtr<UByte>(offset, 1)
    @kotlin.jvm.JvmName("minusUByte") operator fun CPointer<UByte>.minus(offset: Int): CPointer<UByte> = addPtr<UByte>(-offset, 1)
    fun CPointer<UByte>.minusPtrUByte(other: CPointer<UByte>): Int = (this.ptr - other.ptr) / 1
    inline fun fixedArrayOfUByte(size: Int, setItems: CPointer<UByte>.() -> Unit): CPointer<UByte> = CPointer<UByte>(alloca_zero(size * 1).ptr).apply(setItems)
    fun fixedArrayOfUByte(vararg values: UByte, size: Int = values.size): CPointer<UByte> = fixedArrayOfUByte(size) { for (n in 0 until values.size) this[n] = values[n] }
    fun fixedArrayOfUByte(values: String, size: Int = values.length): CPointer<UByte> = fixedArrayOfUByte(size) { for (n in 0 until values.length) this[n] = values[n].code.toUByte() }

    operator fun CPointer<UShort>.get(offset: Int): UShort = lh(this.ptr + offset * 2).toUShort()
    operator fun CPointer<UShort>.set(offset: Int, value: UShort): Unit = sh(this.ptr + offset * 2, (value).toShort())
    var CPointer<UShort>.value: UShort get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusUShort") operator fun CPointer<UShort>.plus(offset: Int): CPointer<UShort> = addPtr<UShort>(offset, 2)
    @kotlin.jvm.JvmName("minusUShort") operator fun CPointer<UShort>.minus(offset: Int): CPointer<UShort> = addPtr<UShort>(-offset, 2)
    fun CPointer<UShort>.minusPtrUShort(other: CPointer<UShort>): Int = (this.ptr - other.ptr) / 2
    inline fun fixedArrayOfUShort(size: Int, setItems: CPointer<UShort>.() -> Unit): CPointer<UShort> = CPointer<UShort>(alloca_zero(size * 2).ptr).apply(setItems)
    fun fixedArrayOfUShort(vararg values: UShort, size: Int = values.size): CPointer<UShort> = fixedArrayOfUShort(size) { for (n in 0 until values.size) this[n] = values[n] }
    fun fixedArrayOfUShort(values: String, size: Int = values.length): CPointer<UShort> = fixedArrayOfUShort(size) { for (n in 0 until values.length) this[n] = values[n].code.toUShort() }

    operator fun CPointer<UInt>.get(offset: Int): UInt = lw(this.ptr + offset * 4).toUInt()
    operator fun CPointer<UInt>.set(offset: Int, value: UInt): Unit = sw(this.ptr + offset * 4, (value).toInt())
    var CPointer<UInt>.value: UInt get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusUInt") operator fun CPointer<UInt>.plus(offset: Int): CPointer<UInt> = addPtr<UInt>(offset, 4)
    @kotlin.jvm.JvmName("minusUInt") operator fun CPointer<UInt>.minus(offset: Int): CPointer<UInt> = addPtr<UInt>(-offset, 4)
    fun CPointer<UInt>.minusPtrUInt(other: CPointer<UInt>): Int = (this.ptr - other.ptr) / 4
    inline fun fixedArrayOfUInt(size: Int, setItems: CPointer<UInt>.() -> Unit): CPointer<UInt> = CPointer<UInt>(alloca_zero(size * 4).ptr).apply(setItems)
    fun fixedArrayOfUInt(vararg values: UInt, size: Int = values.size): CPointer<UInt> = fixedArrayOfUInt(size) { for (n in 0 until values.size) this[n] = values[n] }

    operator fun CPointer<ULong>.get(offset: Int): ULong = ld(this.ptr + offset * 8).toULong()
    operator fun CPointer<ULong>.set(offset: Int, value: ULong): Unit = sd(this.ptr + offset * 8, (value).toLong())
    var CPointer<ULong>.value: ULong get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusULong") operator fun CPointer<ULong>.plus(offset: Int): CPointer<ULong> = addPtr<ULong>(offset, 8)
    @kotlin.jvm.JvmName("minusULong") operator fun CPointer<ULong>.minus(offset: Int): CPointer<ULong> = addPtr<ULong>(-offset, 8)
    fun CPointer<ULong>.minusPtrULong(other: CPointer<ULong>): Int = (this.ptr - other.ptr) / 8
    inline fun fixedArrayOfULong(size: Int, setItems: CPointer<ULong>.() -> Unit): CPointer<ULong> = CPointer<ULong>(alloca_zero(size * 8).ptr).apply(setItems)
    fun fixedArrayOfULong(vararg values: ULong, size: Int = values.size): CPointer<ULong> = fixedArrayOfULong(size) { for (n in 0 until values.size) this[n] = values[n] }

    ///////////////////////////////////////
    operator fun FloatPointer.get(offset: Int): Float = lwf(this.ptr + offset * 4)
    operator fun FloatPointer.set(offset: Int, value: Float): Unit = swf(this.ptr + offset * 4, (value))
    var FloatPointer.value: Float get() = this[0]; set(value) { this[0] = value }
    inline fun fixedArrayOfFloat(size: Int, setItems: FloatPointer.() -> Unit): FloatPointer = FloatPointer(alloca_zero(size * 4).ptr).apply(setItems)
    fun fixedArrayOfFloat(vararg values: Float, size: Int = values.size): FloatPointer = fixedArrayOfFloat(size) { for (n in 0 until values.size) this[n] = values[n] }
    ///////////////////////////////////////

    @kotlin.jvm.JvmName("getterDouble") operator fun CPointer<Double>.get(offset: Int): Double = ldf(this.ptr + offset * 4)
    @kotlin.jvm.JvmName("setterDouble") operator fun CPointer<Double>.set(offset: Int, value: Double): Unit = sdf(this.ptr + offset * 4, (value))
    @set:kotlin.jvm.JvmName("setter_Double_value") @get:kotlin.jvm.JvmName("getter_Double_value") var CPointer<Double>.value: Double get() = this[0]; set(value) { this[0] = value }
    @kotlin.jvm.JvmName("plusDouble") operator fun CPointer<Double>.plus(offset: Int): CPointer<Double> = addPtr<Double>(offset, 4)
    @kotlin.jvm.JvmName("minusDouble") operator fun CPointer<Double>.minus(offset: Int): CPointer<Double> = addPtr<Double>(-offset, 4)
    fun CPointer<Double>.minusPtrDouble(other: CPointer<Double>): Int = (this.ptr - other.ptr) / 4
    inline fun fixedArrayOfDouble(size: Int, setItems: CPointer<Double>.() -> Unit): CPointer<Double> = CPointer<Double>(alloca_zero(size * 4).ptr).apply(setItems)
    fun fixedArrayOfDouble(vararg values: Double, size: Int = values.size): CPointer<Double> = fixedArrayOfDouble(size) { for (n in 0 until values.size) this[n] = values[n] }

    ///////////////////////////////////////
    // STACK ALLOC
    ///////////////////////////////////////
    inline fun <T> stackFrame(callback: () -> T): T {
        val oldPos = STACK_PTR
        return try { callback() } finally { STACK_PTR = oldPos }
    }
    fun alloca(size: Int): CPointer<Unit> = CPointer<Unit>(STACK_PTR - size).also { STACK_PTR -= size }
    fun alloca_zero(size: Int): CPointer<Unit> = alloca(size).also { memset(it, 0, size) }

    ///////////////////////////////////////
    // HEAP ALLOC
    ///////////////////////////////////////
    // @TODO: OPTIMIZE!
    // Pair<head: Int, size: Int>
    private val chunks: LinkedHashMap<Int, Pair<Int, Int>> = LinkedHashMap<Int, Pair<Int, Int>>()
    private val freeChunks: ArrayList<Pair<Int, Int>> = arrayListOf<Pair<Int, Int>>()
    fun malloc(size: Int): CPointer<Unit> {
        val chunk = freeChunks.firstOrNull { it.second >= size }
        if (chunk != null) {
            freeChunks.remove(chunk)
            chunks[chunk.first] = chunk
            return CPointer(chunk.first)
        } else {
            val head = HEAP_PTR
            HEAP_PTR += size
            chunks[head] = Pair(head, size)
            return CPointer(head)
        }
    }
    fun free(ptr: CPointer<*>) { chunks.remove(ptr.ptr)?.let { freeChunks += it } }

    ///////////////////////////////////////
    // I/O
    ///////////////////////////////////////
    fun putchar(c: Int): Int {
        print(c.toChar())
        return c
    }

    ///////////////////////////////////////
    // STRINGS
    ///////////////////////////////////////
    private val STRINGS: LinkedHashMap<String, CPointer<Byte>> = LinkedHashMap<String, CPointer<Byte>>()

    val String.ptr: CPointer<Byte>
        get() = STRINGS.getOrPut(this) {
        val bytes = this.encodeToByteArray()
        val ptr = CPointer<Byte>(malloc(bytes.size + 1).ptr)
        val p = ptr.ptr
        for (n in 0 until bytes.size) sb(p + n, bytes[n])
        sb(p + bytes.size, 0)
        ptr
    }

    val Array<String>.ptr: CPointer<CPointer<Byte>>
        get() {
        val POINTER_SIZE: Int = 4
        val array = this
        val ptr = CPointer<CPointer<Byte>>(malloc(POINTER_SIZE * array.size).ptr)
        for (n in 0 until array.size) sw(ptr.ptr + n * POINTER_SIZE, array[n].ptr.ptr)
        return ptr
    }

    fun CPointer<Byte>.strlenz(): Int {
        for (n in 0 until Int.MAX_VALUE) if (this[n] == 0.toByte()) return n
        return Int.MAX_VALUE
    }

    fun CPointer<Byte>.readStringz(): String {
        val len = this.strlenz()
        val ba = ByteArray(len)
        var pos = this.ptr
        for (n in 0 until len) ba[n] = lb(pos++)
        return ba.decodeToString()
    }

    fun CPointer<Byte>.writeStringz(str: String) {
        val bytes = str.encodeToByteArray()
        memWrite(this, bytes)
        this[bytes.size] = 0.toByte()
    }

    ///////////////////////////////////////
    // FUNCTIONS
    ///////////////////////////////////////
    val FUNCTIONS: ArrayList<kotlin.reflect.KFunction<*>> = arrayListOf<kotlin.reflect.KFunction<*>>()
    val FUNCTION_PTRS: LinkedHashMap<kotlin.reflect.KFunction<*>, Int> = LinkedHashMap<kotlin.reflect.KFunction<*>, Int>()

    val <T> CFunction<T>.func: T get() = (FUNCTIONS[this.ptr] as T)
    val <T : kotlin.reflect.KFunction<*>> T.cfunc: CFunction<T> get() = CFunction<T>(FUNCTION_PTRS.getOrPut(this) { FUNCTIONS.add(this); FUNCTIONS.size - 1 })

    inline operator fun <TR> CFunction<() -> TR>.invoke(): TR = func.invoke()
    inline operator fun <T0, TR> CFunction<(T0) -> TR>.invoke(v0: T0): TR = func.invoke(v0)
    inline operator fun <T0, T1, TR> CFunction<(T0, T1) -> TR>.invoke(v0: T0, v1: T1): TR = func.invoke(v0, v1)
    inline operator fun <T0, T1, T2, TR> CFunction<(T0, T1, T2) -> TR>.invoke(v0: T0, v1: T1, v2: T2): TR = func.invoke(v0, v1, v2)
    inline operator fun <T0, T1, T2, T3, TR> CFunction<(T0, T1, T2, T3) -> TR>.invoke(v0: T0, v1: T1, v2: T2, v3: T3): TR = func.invoke(v0, v1, v2, v3)
    inline operator fun <T0, T1, T2, T3, T4, TR> CFunction<(T0, T1, T2, T3, T4) -> TR>.invoke(v0: T0, v1: T1, v2: T2, v3: T3, v4: T4): TR = func.invoke(v0, v1, v2, v3, v4)
    inline operator fun <T0, T1, T2, T3, T4, T5, TR> CFunction<(T0, T1, T2, T3, T4, T5) -> TR>.invoke(v0: T0, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5): TR = func.invoke(v0, v1, v2, v3, v4, v5)
}

@Suppress("UNCHECKED_CAST")
internal open class Runtime(REQUESTED_HEAP_SIZE: Int = 0, REQUESTED_STACK_PTR: Int = 0, __syscalls: RuntimeSyscalls = DummyRuntimeSyscalls) : AbstractRuntime(REQUESTED_HEAP_SIZE, REQUESTED_STACK_PTR, __syscalls) {
    private val HEAP = ByteArray(HEAP_SIZE)

    final override fun lb(ptr: Int): Byte = HEAP[ptr]
    final override fun sb(ptr: Int, value: Byte) { HEAP[ptr] = value }

    override fun memset(ptr: CPointer<*>, value: Int, num: Int): CPointer<Unit> {
        this.HEAP.fill(value.toByte(), ptr.ptr, ptr.ptr + num)
        return (ptr as CPointer<Unit>)
    }
    override fun memmove(dest: CPointer<Unit>, src: CPointer<Unit>, num: Int): CPointer<Unit> {
        this.HEAP.copyInto(this.HEAP, dest.ptr, src.ptr, src.ptr + num)
        return dest
    }
    override fun memcpy(dest: CPointer<Unit>, src: CPointer<Unit>, num: Int): CPointer<Unit> {
        return memmove(dest, src, num)
    }
}

internal interface RuntimeSyscalls {
    fun AbstractRuntime.fopen(file: CPointer<Byte>, mode: CPointer<Byte>): CPointer<CPointer<Unit>> = TODO()
    fun AbstractRuntime.fread(ptr: CPointer<Unit>, size: Int, nmemb: Int, stream: CPointer<CPointer<Unit>>): Int = TODO()
    fun AbstractRuntime.fwrite(ptr: CPointer<Unit>, size: Int, nmemb: Int, stream: CPointer<CPointer<Unit>>): Int = TODO()
    fun AbstractRuntime.fflush(stream: CPointer<CPointer<Unit>>): Int = TODO()
    fun AbstractRuntime.ftell(stream: CPointer<CPointer<Unit>>): Long = TODO()
    fun AbstractRuntime.fsetpos(stream: CPointer<CPointer<Unit>>, ptrHolder: CPointer<Long>): Int = TODO()
    fun AbstractRuntime.fgetpos(stream: CPointer<CPointer<Unit>>, ptrHolder: CPointer<Long>): Int = TODO()
    fun AbstractRuntime.fseek(stream: CPointer<CPointer<Unit>>, offset: Long, whence: Int): Int = TODO()
    fun AbstractRuntime.fclose(stream: CPointer<CPointer<Unit>>): Unit = TODO()
}

internal object DummyRuntimeSyscalls : RuntimeSyscalls

//////////////////////

@kotlin.jvm.JvmInline internal value/*!*/ class CPointer<T>(val ptr: Int)
@kotlin.jvm.JvmInline internal value/*!*/ class CFunction<T>(val ptr: Int)

@kotlin.jvm.JvmInline internal value/*!*/ class FloatPointer(val ptr: Int) {
    operator fun plus(offset: Int): FloatPointer = FloatPointer(this.ptr + offset * 4)
    operator fun minus(other: FloatPointer): Int = (this.ptr - other.ptr) / 4
    operator fun minus(offset: Int): FloatPointer = this + (-offset)
    operator fun inc(): FloatPointer = this + 1
    operator fun dec(): FloatPointer = this - 1
}

@kotlin.jvm.JvmInline internal value/*!*/ class IntPointer(val ptr: Int) {
    operator fun plus(offset: Int): IntPointer = IntPointer(this.ptr + offset * 4)
    operator fun minus(other: IntPointer): Int = (this.ptr - other.ptr) / 4
    operator fun minus(offset: Int): IntPointer = this + (-offset)
    operator fun inc(): IntPointer = this + 1
    operator fun dec(): IntPointer = this - 1
}
