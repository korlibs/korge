package korlibs.datastructure

open class BaseDelegatedStackedArray2(val other: IStackedArray2Base) : IStackedArray2Base by other

class StackedLongArray2FromIStackedIntArray2(val data: IStackedIntArray2) : BaseDelegatedStackedArray2(data), IStackedLongArray2 {
    override val empty: Long get() = StackedLongArray2.EMPTY
    override fun clone(): IStackedLongArray2 = StackedLongArray2FromIStackedIntArray2(data.clone())
    override fun set(x: Int, y: Int, level: Int, value: Long) { data[x, y, level] = value.toInt() }
    override fun get(x: Int, y: Int, level: Int): Long = data[x, y, level].toUInt().toLong()
}

class StackedIntArray2FromIStackedLongArray2(val data: IStackedLongArray2) : BaseDelegatedStackedArray2(data), IStackedIntArray2 {
    override val empty: Int get() = StackedIntArray2.EMPTY
    override fun clone(): IStackedIntArray2 = StackedIntArray2FromIStackedLongArray2(data.clone())
    override fun set(x: Int, y: Int, level: Int, value: Int) { data[x, y, level] = value.toLong() }
    override fun get(x: Int, y: Int, level: Int): Int = data[x, y, level].toInt()
}

fun IStackedIntArray2.asLong(): IStackedLongArray2 = StackedLongArray2FromIStackedIntArray2(this)
fun IStackedLongArray2.asInt(): IStackedIntArray2 = StackedIntArray2FromIStackedLongArray2(this)
