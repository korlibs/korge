package korlibs.memory

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

// Unsigned
@Deprecated("", ReplaceWith("getU8(o)")) public fun ByteArray.readU8(o: Int): Int = getU8(o)
@Deprecated("", ReplaceWith("getU16LE(o)")) public fun ByteArray.readU16LE(o: Int): Int = getU16LE(o)
@Deprecated("", ReplaceWith("getU24LE(o)")) public fun ByteArray.readU24LE(o: Int): Int = getU24LE(o)
@Deprecated("", ReplaceWith("getU32LE(o)")) public fun ByteArray.readU32LE(o: Int): Long = getU32LE(o)
@Deprecated("", ReplaceWith("getU16BE(o)")) public fun ByteArray.readU16BE(o: Int): Int = getU16BE(o)
@Deprecated("", ReplaceWith("getU24BE(o)")) public fun ByteArray.readU24BE(o: Int): Int = getU24BE(o)
@Deprecated("", ReplaceWith("getU32BE(o)")) public fun ByteArray.readU32BE(o: Int): Long = getU32BE(o)

// Signed
@Deprecated("", ReplaceWith("getS8(o)")) public fun ByteArray.readS8(o: Int): Int = getS8(o)
@Deprecated("", ReplaceWith("getS16LE(o)")) public fun ByteArray.readS16LE(o: Int): Int = getS16LE(o)
@Deprecated("", ReplaceWith("getS24LE(o)")) public fun ByteArray.readS24LE(o: Int): Int = getS24LE(o)
@Deprecated("", ReplaceWith("getS32LE(o)")) public fun ByteArray.readS32LE(o: Int): Int = getS32LE(o)
@Deprecated("", ReplaceWith("getS64LE(o)")) public fun ByteArray.readS64LE(o: Int): Long = getS64LE(o)
@Deprecated("", ReplaceWith("getF32LE(o)")) public fun ByteArray.readF32LE(o: Int): Float = getF32LE(o)
@Deprecated("", ReplaceWith("getF64LE(o)")) public fun ByteArray.readF64LE(o: Int): Double = getF64LE(o)
@Deprecated("", ReplaceWith("getS16BE(o)")) public fun ByteArray.readS16BE(o: Int): Int = getS16BE(o)
@Deprecated("", ReplaceWith("getS24BE(o)")) public fun ByteArray.readS24BE(o: Int): Int = getS24BE(o)
@Deprecated("", ReplaceWith("getS32BE(o)")) public fun ByteArray.readS32BE(o: Int): Int = getS32BE(o)
@Deprecated("", ReplaceWith("getS64BE(o)")) public fun ByteArray.readS64BE(o: Int): Long = getS64BE(o)
@Deprecated("", ReplaceWith("getF32BE(o)")) public fun ByteArray.readF32BE(o: Int): Float = getF32BE(o)
@Deprecated("", ReplaceWith("getF64BE(o)")) public fun ByteArray.readF64BE(o: Int): Double = getF64BE(o)

// Custom Endian
@Deprecated("", ReplaceWith("getU16(o, little)")) public fun ByteArray.readU16(o: Int, little: Boolean): Int = getU16(o, little)
@Deprecated("", ReplaceWith("getU24(o, little)")) public fun ByteArray.readU24(o: Int, little: Boolean): Int = getU24(o, little)
@Deprecated("", ReplaceWith("getU32(o, little)")) public fun ByteArray.readU32(o: Int, little: Boolean): Long = getU32(o, little)
@Deprecated("", ReplaceWith("getS16(o, little)")) public fun ByteArray.readS16(o: Int, little: Boolean): Int = getS16(o, little)
@Deprecated("", ReplaceWith("getS24(o, little)")) public fun ByteArray.readS24(o: Int, little: Boolean): Int = getS24(o, little)
@Deprecated("", ReplaceWith("getS32(o, little)")) public fun ByteArray.readS32(o: Int, little: Boolean): Int = getS32(o, little)
@Deprecated("", ReplaceWith("getS64(o, little)")) public fun ByteArray.readS64(o: Int, little: Boolean): Long = getS64(o, little)
@Deprecated("", ReplaceWith("getF32(o, little)")) public fun ByteArray.readF32(o: Int, little: Boolean): Float = getF32(o, little)
@Deprecated("", ReplaceWith("getF64(o, little)")) public fun ByteArray.readF64(o: Int, little: Boolean): Double = getF64(o, little)

@Deprecated("", ReplaceWith("getS8Array(o, count)")) public fun ByteArray.readByteArray(o: Int, count: Int): ByteArray = getS8Array(o, count)
@Deprecated("", ReplaceWith("getS16LEArray(o, count)")) public fun ByteArray.readShortArrayLE(o: Int, count: Int): ShortArray = getS16ArrayLE(o, count)
@Deprecated("", ReplaceWith("getU16LEArray(o, count)")) public fun ByteArray.readCharArrayLE(o: Int, count: Int): CharArray = getU16ArrayLE(o, count)
@Deprecated("", ReplaceWith("getS32LEArray(o, count)")) public fun ByteArray.readIntArrayLE(o: Int, count: Int): IntArray = getS32ArrayLE(o, count)
@Deprecated("", ReplaceWith("getS64LEArray(o, count)")) public fun ByteArray.readLongArrayLE(o: Int, count: Int): LongArray = getS64ArrayLE(o, count)
@Deprecated("", ReplaceWith("getF32LEArray(o, count)")) public fun ByteArray.readFloatArrayLE(o: Int, count: Int): FloatArray = getF32ArrayLE(o, count)
@Deprecated("", ReplaceWith("getF64LEArray(o, count)")) public fun ByteArray.readDoubleArrayLE(o: Int, count: Int): DoubleArray = getF64ArrayLE(o, count)
@Deprecated("", ReplaceWith("getS16BEArray(o, count)")) public fun ByteArray.readShortArrayBE(o: Int, count: Int): ShortArray = getS16ArrayBE(o, count)
@Deprecated("", ReplaceWith("getU16BEArray(o, count)")) public fun ByteArray.readCharArrayBE(o: Int, count: Int): CharArray = getU16ArrayBE(o, count)
@Deprecated("", ReplaceWith("getS32BEArray(o, count)")) public fun ByteArray.readIntArrayBE(o: Int, count: Int): IntArray = getS32ArrayBE(o, count)
@Deprecated("", ReplaceWith("getS64BEArray(o, count)")) public fun ByteArray.readLongArrayBE(o: Int, count: Int): LongArray = getS64ArrayBE(o, count)
@Deprecated("", ReplaceWith("getF32BEArray(o, count)")) public fun ByteArray.readFloatArrayBE(o: Int, count: Int): FloatArray = getF32ArrayBE(o, count)
@Deprecated("", ReplaceWith("getF64BEArray(o, count)")) public fun ByteArray.readDoubleArrayBE(o: Int, count: Int): DoubleArray = getF64ArrayBE(o, count)

@Deprecated("", ReplaceWith("getS16Array(o, count, little)")) public fun ByteArray.readShortArray(o: Int, count: Int, little: Boolean): ShortArray = getS16Array(o, count, little)
@Deprecated("", ReplaceWith("getU16Array(o, count, little)")) public fun ByteArray.readCharArray(o: Int, count: Int, little: Boolean): CharArray = getU16Array(o, count, little)
@Deprecated("", ReplaceWith("getS32Array(o, count, little)")) public fun ByteArray.readIntArray(o: Int, count: Int, little: Boolean): IntArray = getS32Array(o, count, little)
@Deprecated("", ReplaceWith("getS64Array(o, count, little)")) public fun ByteArray.readLongArray(o: Int, count: Int, little: Boolean): LongArray = getS64Array(o, count, little)
@Deprecated("", ReplaceWith("getF32Array(o, count, little)")) public fun ByteArray.readFloatArray(o: Int, count: Int, little: Boolean): FloatArray = getF32Array(o, count, little)
@Deprecated("", ReplaceWith("getF64Array(o, count, little)")) public fun ByteArray.readDoubleArray(o: Int, count: Int, little: Boolean): DoubleArray = getF64Array(o, count, little)

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

@Deprecated("", ReplaceWith("set8(o, v)")) public fun ByteArray.write8(o: Int, v: Int) = set8(o, v)
@Deprecated("", ReplaceWith("set8(o, v)")) public fun ByteArray.write8(o: Int, v: Long) = set8(o, v)
@Deprecated("", ReplaceWith("set16(o, v, little)")) public fun ByteArray.write16(o: Int, v: Int, little: Boolean) = set16(o, v, little)
@Deprecated("", ReplaceWith("set24(o, v, little)")) public fun ByteArray.write24(o: Int, v: Int, little: Boolean) = set24(o, v, little)
@Deprecated("", ReplaceWith("set32(o, v, little)")) public fun ByteArray.write32(o: Int, v: Int, little: Boolean) = set32(o, v, little)
@Deprecated("", ReplaceWith("set64(o, v, little)")) public fun ByteArray.write64(o: Int, v: Long, little: Boolean) = set64(o, v, little)
@Deprecated("", ReplaceWith("setF32(o, v, little)")) public fun ByteArray.writeF32(o: Int, v: Float, little: Boolean) = setF32(o, v, little)
@Deprecated("", ReplaceWith("setF64(o, v, little)")) public fun ByteArray.writeF64(o: Int, v: Double, little: Boolean) = setF64(o, v, little)

@Deprecated("", ReplaceWith("set16LE(o, v)")) public fun ByteArray.write16LE(o: Int, v: Int) = set16LE(o, v)
@Deprecated("", ReplaceWith("set24LE(o, v)")) public fun ByteArray.write24LE(o: Int, v: Int) = set24LE(o, v)
@Deprecated("", ReplaceWith("set32LE(o, v)")) public fun ByteArray.write32LE(o: Int, v: Int) = set32LE(o, v)
@Deprecated("", ReplaceWith("set32LE(o, v)")) public fun ByteArray.write32LE(o: Int, v: Long) = set32LE(o, v)
@Deprecated("", ReplaceWith("set64LE(o, v)")) public fun ByteArray.write64LE(o: Int, v: Long) = set64LE(o, v)
@Deprecated("", ReplaceWith("setF32LE(o, v)")) public fun ByteArray.writeF32LE(o: Int, v: Float) = setF32LE(o, v)
@Deprecated("", ReplaceWith("setF64LE(o, v)")) public fun ByteArray.writeF64LE(o: Int, v: Double) = setF64LE(o, v)

@Deprecated("", ReplaceWith("set16BE(o, v)")) public fun ByteArray.write16BE(o: Int, v: Int) = set16BE(o, v)
@Deprecated("", ReplaceWith("set24BE(o, v)")) public fun ByteArray.write24BE(o: Int, v: Int) = set24BE(o, v)
@Deprecated("", ReplaceWith("set32BE(o, v)")) public fun ByteArray.write32BE(o: Int, v: Int) = set32BE(o, v)
@Deprecated("", ReplaceWith("set32BE(o, v)")) public fun ByteArray.write32BE(o: Int, v: Long) = set32BE(o, v)
@Deprecated("", ReplaceWith("set64BE(o, v)")) public fun ByteArray.write64BE(o: Int, v: Long) = set64BE(o, v)
@Deprecated("", ReplaceWith("setF32BE(o, v)")) public fun ByteArray.writeF32BE(o: Int, v: Float) = setF32BE(o, v)
@Deprecated("", ReplaceWith("setF64BE(o, v)")) public fun ByteArray.writeF64BE(o: Int, v: Double) = setF64BE(o, v)

@Deprecated("", ReplaceWith("setBytes(o, bytes)")) public fun ByteArray.writeBytes(o: Int, bytes: ByteArray): Unit = setBytes(o, bytes)

@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: CharArray): Unit = setArrayLE(o, array)
@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: ShortArray): Unit = setArrayLE(o, array)
@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: IntArray): Unit = setArrayLE(o, array)
@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: LongArray): Unit = setArrayLE(o, array)
@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: FloatArray): Unit = setArrayLE(o, array)
@Deprecated("", ReplaceWith("setArrayLE(o, array)")) public fun ByteArray.writeArrayLE(o: Int, array: DoubleArray): Unit = setArrayLE(o, array)

@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: CharArray): Unit = setArrayBE(o, array)
@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: ShortArray): Unit = setArrayBE(o, array)
@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: IntArray): Unit = setArrayBE(o, array)
@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: LongArray): Unit = setArrayBE(o, array)
@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: FloatArray): Unit = setArrayBE(o, array)
@Deprecated("", ReplaceWith("setArrayBE(o, array)")) public fun ByteArray.writeArrayBE(o: Int, array: DoubleArray): Unit = setArrayBE(o, array)
