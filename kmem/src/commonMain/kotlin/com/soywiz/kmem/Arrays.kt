package com.soywiz.kmem

/** View of [bytes] [ByteArray] reinterpreted as [Int] */
public inline class UByteArrayInt(public val bytes: ByteArray) {
    public val size: Int get() = bytes.size
    public operator fun get(index: Int): Int = bytes[index].toInt() and 0xFF
    public operator fun set(index: Int, value: Int) { bytes[index] = value.toByte() }
    public operator fun set(index: Int, value: UByte) { bytes[index] = value.toByte() }
}

fun UByteArrayInt.fill(value: Int, fromIndex: Int = 0, toIndex: Int = size) = this.bytes.fill(value.toByte(), fromIndex, toIndex)
fun arraycopy(src: UByteArrayInt, srcPos: Int, dst: UByteArrayInt, dstPos: Int, size: Int) = arraycopy(src.bytes, srcPos, dst.bytes, dstPos, size)

/** Creates a new [UByteArrayInt] view of [size] bytes */
public fun UByteArrayInt(size: Int): UByteArrayInt = UByteArrayInt(ByteArray(size))
public fun UByteArrayInt(size: Int, gen: (Int) -> Int): UByteArrayInt = UByteArrayInt(ByteArray(size) { gen(it).toByte() })

/** Creates a view of [this] reinterpreted as [Int] */
public fun ByteArray.asUByteArrayInt(): UByteArrayInt = UByteArrayInt(this)
/** Gets the underlying array of [this] */
public fun UByteArrayInt.asByteArray(): ByteArray = this.bytes

/** View of [base] [IntArray] reinterpreted as [Float] */
public inline class FloatArrayFromIntArray(public val base: IntArray) {
    public operator fun get(i: Int): Float = base[i].reinterpretAsFloat()
    public operator fun set(i: Int, v: Float) { base[i] = v.reinterpretAsInt() }
}

/** Creates a view of [this] reinterpreted as [Float] */
public fun IntArray.asFloatArray(): FloatArrayFromIntArray = FloatArrayFromIntArray(this)
/** Gets the underlying array of [this] */
public fun FloatArrayFromIntArray.asIntArray(): IntArray = base
