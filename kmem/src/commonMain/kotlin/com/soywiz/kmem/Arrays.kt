package com.soywiz.kmem

/** View of [bytes] [ByteArray] reinterpreted as [Int] */
public inline class UByteArrayInt(public val data: ByteArray) {
    val bytes: ByteArray get() = data

    /** Creates a new [UByteArrayInt] view of [size] bytes */
    constructor(size: Int) : this(ByteArray(size))
    constructor(size: Int, gen: (Int) -> Int) : this(ByteArray(size) { gen(it).toByte() })

    public val size: Int get() = data.size
    public operator fun get(index: Int): Int = data[index].toInt() and 0xFF
    public operator fun set(index: Int, value: Int) { data[index] = value.toByte() }
    public operator fun set(index: Int, value: UByte) { data[index] = value.toByte() }

    fun fill(value: Int, fromIndex: Int = 0, toIndex: Int = size) = data.fill(value.toByte(), fromIndex, toIndex)
}
/** Creates a view of [this] reinterpreted as [Int] */
public fun ByteArray.asUByteArrayInt(): UByteArrayInt = UByteArrayInt(this)
/** Gets the underlying array of [this] */
public fun UByteArrayInt.asByteArray(): ByteArray = this.data
fun arraycopy(src: UByteArrayInt, srcPos: Int, dst: UByteArrayInt, dstPos: Int, size: Int) = arraycopy(src.data, srcPos, dst.data, dstPos, size)
fun ubyteArrayIntOf(vararg values: Int): UByteArrayInt = UByteArrayInt(values.size) { values[it] }



/** View of [shorts] [ShortArray] reinterpreted as [Int] */
public inline class UShortArrayInt(public val data: ShortArray) {
    val shorts: ShortArray get() = data

    /** Creates a new [UShortArrayInt] view of [size] bytes */
    constructor(size: Int) : this(ShortArray(size))
    constructor(size: Int, gen: (Int) -> Int) : this(ShortArray(size) { gen(it).toShort() })

    public val size: Int get() = data.size
    public operator fun get(index: Int): Int = data[index].toInt() and 0xFFFF
    public operator fun set(index: Int, value: Int) { data[index] = value.toShort() }
    public operator fun set(index: Int, value: UShort) { data[index] = value.toShort() }

    fun fill(value: Int, fromIndex: Int = 0, toIndex: Int = size) = this.data.fill(value.toShort(), fromIndex, toIndex)
}
/** Creates a view of [this] reinterpreted as [Int] */
public fun ShortArray.asUShortArrayInt(): UShortArrayInt = UShortArrayInt(this)
/** Gets the underlying array of [this] */
public fun UShortArrayInt.asShortArray(): ShortArray = this.data
fun arraycopy(src: UShortArrayInt, srcPos: Int, dst: UShortArrayInt, dstPos: Int, size: Int) = arraycopy(src.data, srcPos, dst.data, dstPos, size)
fun ushortArrayIntOf(vararg values: Int): UShortArrayInt = UShortArrayInt(values.size) { values[it] }




/** View of [base] [IntArray] reinterpreted as [Float] */
public inline class FloatArrayFromIntArray(public val base: IntArray) {
    public operator fun get(i: Int): Float = base[i].reinterpretAsFloat()
    public operator fun set(i: Int, v: Float) { base[i] = v.reinterpretAsInt() }
}

/** Creates a view of [this] reinterpreted as [Float] */
public fun IntArray.asFloatArray(): FloatArrayFromIntArray = FloatArrayFromIntArray(this)
/** Gets the underlying array of [this] */
public fun FloatArrayFromIntArray.asIntArray(): IntArray = base
