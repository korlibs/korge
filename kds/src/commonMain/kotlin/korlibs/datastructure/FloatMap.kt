package korlibs.datastructure

class FloatMap<T> private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)
    companion object {
        @PublishedApi
        internal fun Float.adapt(): Int = this.toRawBits()
        @PublishedApi
        internal fun Int.adapt(): Float = Float.fromBits(this)
    }

    @PublishedApi
    internal val map = IntMap<T>(nbits, loadFactor)


    operator fun contains(key: Float): Boolean = map.contains(key.adapt())
    fun remove(key: Float): Boolean = map.remove(key.adapt())
    fun clear() = map.clear()
    operator fun get(key: Float): T? = map[key.adapt()]
    operator fun set(key: Float, value: T?): T? = map.set(key.adapt(), value)
    inline fun getOrPut(key: Float, callback: (Int) -> T): T = map.getOrPut(key.adapt(), callback)

    data class Entry<T>(var key: Float, var value: T?)

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator<T>(val map: FloatMap<T>) {
        val intIterator = IntMap.Iterator(map.map)
        fun hasNext() = intIterator.hasNext()
        fun nextEntry(): Entry<T?> = intIterator.nextEntry().let { Entry(it.key.adapt(), it.value) }
        fun nextKey(): Float = intIterator.nextKey().adapt()
        fun nextValue(): T? = intIterator.nextValue()
    }

    inline fun fastKeyForEach(callback: (key: Float) -> Unit) = map.fastKeyForEach { callback(it.adapt()) }
    inline fun fastValueForEachNullable(callback: (value: T?) -> Unit): Unit = map.fastValueForEachNullable { callback(it) }
    inline fun fastForEachNullable(callback: (key: Float, value: T?) -> Unit): Unit = map.fastForEachNullable { key, value -> callback(key.adapt(), value) }
    inline fun fastValueForEach(callback: (value: T) -> Unit): Unit = map.fastValueForEach { callback(it) }
    inline fun fastForEach(callback: (key: Float, value: T) -> Unit): Unit = map.fastForEach { key, value -> callback(key.adapt(), value) }
    override fun equals(other: Any?): Boolean = (other is FloatMap<*>) && this.map == other.map
    override fun hashCode(): Int = this.map.hashCode()
}

class FloatFloatMap private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)
    companion object {
        @PublishedApi
        internal fun Float.adapt(): Int = this.toRawBits()
        @PublishedApi
        internal fun Int.adapt(): Float = Float.fromBits(this)
    }

    @PublishedApi
    internal val map = IntIntMap(nbits, loadFactor)

    operator fun contains(key: Float): Boolean = map.contains(key.adapt())
    fun remove(key: Float): Boolean = map.remove(key.adapt())
    fun clear() = map.clear()
    operator fun get(key: Float): Float = map[key.adapt()].adapt()
    operator fun set(key: Float, value: Float): Float = map.set(key.adapt(), value.adapt()).adapt()
    inline fun getOrPut(key: Float, crossinline callback: () -> Float): Float = map.getOrPut(key.adapt()) { callback().adapt() }.adapt()

    data class Entry(var key: Float, var value: Float)

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator(val map: FloatFloatMap) {
        val intIterator = IntIntMap.Iterator(map.map)
        fun hasNext() = intIterator.hasNext()
        fun nextEntry(): Entry = intIterator.nextEntry().let { Entry(it.key.adapt(), it.value.adapt()) }
        fun nextKey(): Float = intIterator.nextKey().adapt()
        fun nextValue(): Float = intIterator.nextValue().adapt()
    }

    inline fun fastKeyForEach(callback: (key: Float) -> Unit) = map.fastKeyForEach { callback(it.adapt()) }
    //inline fun fastValueForEachNullable(callback: (value: Float) -> Unit): Unit = map.fastValueForEachNullable { callback(it) }
    //inline fun fastForEachNullable(callback: (key: Float, value: Float) -> Unit): Unit = map.fastForEachNullable { key, value -> callback(key.adapt(), value) }
    inline fun fastValueForEach(callback: (value: Float) -> Unit): Unit = map.fastValueForEach { callback(it.adapt()) }
    inline fun fastForEach(callback: (key: Float, value: Float) -> Unit): Unit = map.fastForEach { key, value -> callback(key.adapt(), value.adapt()) }
    override fun equals(other: Any?): Boolean = (other is FloatFloatMap) && this.map == other.map
    override fun hashCode(): Int = this.map.hashCode()
}

class IntFloatMap private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)
    companion object {
        @PublishedApi
        internal fun Float.adapt(): Int = this.toRawBits()
        @PublishedApi
        internal fun Int.adapt(): Float = Float.fromBits(this)
    }

    @PublishedApi
    internal val map = IntIntMap(nbits, loadFactor)

    val size get() = map.size
    operator fun contains(key: Int): Boolean = map.contains(key)
    fun remove(key: Int): Boolean = map.remove(key)
    fun clear() = map.clear()
    operator fun get(key: Int): Float = map[key].adapt()
    operator fun set(key: Int, value: Float): Float = map.set(key, value.adapt()).adapt()
    inline fun getOrPut(key: Int, crossinline callback: () -> Float): Float = map.getOrPut(key) { callback().adapt() }.adapt()

    data class Entry(var key: Int, var value: Float)

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator(val map: IntFloatMap) {
        val intIterator = IntIntMap.Iterator(map.map)
        fun hasNext() = intIterator.hasNext()
        fun nextEntry(): Entry = intIterator.nextEntry().let { Entry(it.key, it.value.adapt()) }
        fun nextKey(): Float = intIterator.nextKey().adapt()
        fun nextValue(): Float = intIterator.nextValue().adapt()
    }

    inline fun fastKeyForEach(callback: (key: Int) -> Unit) = map.fastKeyForEach { callback(it) }
    //inline fun fastValueForEachNullable(callback: (value: Float) -> Unit): Unit = map.fastValueForEachNullable { callback(it) }
    //inline fun fastForEachNullable(callback: (key: Int, value: Float) -> Unit): Unit = map.fastForEachNullable { key, value -> callback(key.adapt(), value) }
    inline fun fastValueForEach(callback: (value: Float) -> Unit): Unit = map.fastValueForEach { callback(it.adapt()) }
    inline fun fastForEach(callback: (key: Int, value: Float) -> Unit): Unit = map.fastForEach { key, value -> callback(key, value.adapt()) }
    override fun equals(other: Any?): Boolean = (other is IntFloatMap) && this.map == other.map
    override fun hashCode(): Int = this.map.hashCode()
}

class FloatIntMap private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)
    companion object {
        @PublishedApi
        internal fun Float.adapt(): Int = this.toRawBits()
        @PublishedApi
        internal fun Int.adapt(): Float = Float.fromBits(this)
    }

    @PublishedApi
    internal val map = IntIntMap(nbits, loadFactor)

    operator fun contains(key: Float): Boolean = map.contains(key.adapt())
    fun remove(key: Float): Boolean = map.remove(key.adapt())
    fun clear() = map.clear()
    operator fun get(key: Float): Float = map[key.adapt()].adapt()
    operator fun set(key: Float, value: Int): Int = map.set(key.adapt(), value)
    inline fun getOrPut(key: Float, crossinline callback: () -> Int): Int = map.getOrPut(key.adapt()) { callback() }

    data class Entry(var key: Float, var value: Int)

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator(val map: FloatIntMap) {
        val intIterator = IntIntMap.Iterator(map.map)
        fun hasNext() = intIterator.hasNext()
        fun nextEntry(): Entry = intIterator.nextEntry().let { Entry(it.key.adapt(), it.value) }
        fun nextKey(): Float = intIterator.nextKey().adapt()
        fun nextValue(): Float = intIterator.nextValue().adapt()
    }

    inline fun fastKeyForEach(callback: (key: Float) -> Unit) = map.fastKeyForEach { callback(it.adapt()) }
    //inline fun fastValueForEachNullable(callback: (value: Int) -> Unit): Unit = map.fastValueForEachNullable { callback(it) }
    //inline fun fastForEachNullable(callback: (key: Float, value: Int) -> Unit): Unit = map.fastForEachNullable { key, value -> callback(key.adapt(), value) }
    inline fun fastValueForEach(callback: (value: Int) -> Unit): Unit = map.fastValueForEach { callback(it) }
    inline fun fastForEach(callback: (key: Float, value: Int) -> Unit): Unit = map.fastForEach { key, value -> callback(key.adapt(), value) }
    override fun equals(other: Any?): Boolean = (other is FloatIntMap) && this.map == other.map
    override fun hashCode(): Int = this.map.hashCode()
}
