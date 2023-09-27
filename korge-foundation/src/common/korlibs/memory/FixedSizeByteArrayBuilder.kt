package korlibs.memory

public class FixedSizeByteArrayBuilder(public val data: ByteArray) {
    public val capacity: Int get() = data.size
    public var size: Int = 0
        private set
    public constructor(size: Int) : this(ByteArray(size))

    public fun clear() {
        size = 0
    }

    public fun append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) {
        arraycopy(array, offset, this.data, size, len)
        this.size += len
    }

    public fun appendFast(v: Byte) {
        data[size++] = v
    }

    public inline fun append(v: Byte): FixedSizeByteArrayBuilder {
        appendFast(v)
        return this
    }

    public fun append(vararg v: Byte): Unit = append(v)
    public fun append(vararg v: Int) {
        for (n in 0 until v.size) this.data[this.size + n] = v[n].toByte()
        this.size += v.size
    }

    public fun toByteArray(): ByteArray = data.copyOf(size)
}
