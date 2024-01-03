package korlibs.memoryinternal

import kotlin.jvm.*

/** View of [shorts] [ShortArray] reinterpreted as [Int] */
@JvmInline
internal value class UShortArrayInt(public val data: ShortArray) {
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
