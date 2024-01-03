package korlibs.memoryinternal

import kotlin.jvm.*


/** View of [bytes] [ByteArray] reinterpreted as [Int] */
@JvmInline
internal value class UByteArrayInt(public val data: ByteArray) {
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
