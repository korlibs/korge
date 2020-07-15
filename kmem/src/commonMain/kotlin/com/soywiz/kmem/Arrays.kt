package com.soywiz.kmem

/** View of [bytes] [ByteArray] reinterpreted as [Int] */
inline class UByteArrayInt(val bytes: ByteArray) {
    val size: Int get() = bytes.size
    operator fun get(index: Int) = bytes[index].toInt() and 0xFF
    operator fun set(index: Int, value: Int) = run { bytes[index] = value.toByte() }
}

/** Creates a new [UByteArrayInt] view of [size] bytes */
fun UByteArrayInt(size: Int) = UByteArrayInt(ByteArray(size))

/** Creates a view of [this] reinterpreted as [Int] */
fun ByteArray.asUByteArrayInt() = UByteArrayInt(this)
/** Gets the underlying array of [this] */
fun UByteArrayInt.asByteArray() = this.bytes

/** View of [base] [IntArray] reinterpreted as [Float] */
inline class FloatArrayFromIntArray(val base: IntArray) {
    operator fun get(i: Int) = base[i].reinterpretAsFloat()
    operator fun set(i: Int, v: Float) = run { base[i] = v.reinterpretAsInt() }
}

/** Creates a view of [this] reinterpreted as [Float] */
fun IntArray.asFloatArray(): FloatArrayFromIntArray = FloatArrayFromIntArray(this)
/** Gets the underlying array of [this] */
fun FloatArrayFromIntArray.asIntArray(): IntArray = base
