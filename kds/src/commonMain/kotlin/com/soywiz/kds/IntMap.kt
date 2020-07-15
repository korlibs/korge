@file:Suppress("RemoveEmptyPrimaryConstructor")

package com.soywiz.kds

import com.soywiz.kds.internal.*

class IntMap<T> private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)

    companion object {
        @PublishedApi
        internal const val EOF = Int.MAX_VALUE - 1
        @PublishedApi
        internal const val ZERO_INDEX = Int.MAX_VALUE
        @PublishedApi
        internal const val EMPTY = 0
    }

    private var capacity = 1 shl nbits
    @PublishedApi
    internal var hasZero = false
    private var zeroValue: T? = null
    private var mask = capacity - 1
    private var stashSize = 1 + ilog2(capacity)
    @PublishedApi
    internal var _keys = IntArray(capacity + stashSize)
    private var _values = arrayOfNulls<Any>(capacity + stashSize) as Array<T?>
    private val stashStart get() = _keys.size - stashSize
    private var growSize: Int = (capacity * loadFactor).toInt()
    var size: Int = 0; private set

    private fun grow() {
        val new = IntMap<T>(nbits + 3, loadFactor)

        for (n in _keys.indices) {
            val k = _keys[n]
            if (k != EMPTY) new[k] = _values[n]
        }

        this.nbits = new.nbits
        this.capacity = new.capacity
        this.mask = new.mask
        this.stashSize = new.stashSize
        this._keys = new._keys
        this._values = new._values
        this.growSize = new.growSize
    }

    operator fun contains(key: Int): Boolean = _getKeyIndex(key) >= 0

    private fun _getKeyIndex(key: Int): Int {
        if (key == 0) return if (hasZero) ZERO_INDEX else -1
        val index1 = hash1(key); if (_keys[index1] == key) return index1
        val index2 = hash2(key); if (_keys[index2] == key) return index2
        val index3 = hash3(key); if (_keys[index3] == key) return index3
        for (n in stashStart until _keys.size) if (_keys[n] == key) return n
        return -1
    }

    fun remove(key: Int): Boolean {
        val index = _getKeyIndex(key)
        if (index < 0) return false
        if (index == ZERO_INDEX) {
            hasZero = false
            zeroValue = null
        } else {
            _keys[index] = EMPTY
        }
        size--
        return true
    }

    fun clear() {
        hasZero = false
        zeroValue = null
        _keys.fill(0)
        _values.fill(null)
        size = 0
    }

    @Suppress("LoopToCallChain")
    operator fun get(key: Int): T? {
        val index = _getKeyIndex(key)
        if (index < 0) return null
        if (index == ZERO_INDEX) return zeroValue
        return _values[index]
    }

    private fun setEmptySlot(index: Int, key: Int, value: T?): T? {
        if (_keys[index] != EMPTY) throw IllegalStateException()
        _keys[index] = key
        _values[index] = value
        size++
        return null
    }

    operator fun set(key: Int, value: T?): T? {
        retry@ while (true) {
            val index = _getKeyIndex(key)
            when {
                index < 0 -> {
                    if (key == 0) {
                        hasZero = true
                        zeroValue = value
                        size++
                        return null
                    }
                    if (size >= growSize) grow()
                    val index1 = hash1(key); if (_keys[index1] == EMPTY) return setEmptySlot(index1, key, value)
                    val index2 = hash2(key); if (_keys[index2] == EMPTY) return setEmptySlot(index2, key, value)
                    val index3 = hash3(key); if (_keys[index3] == EMPTY) return setEmptySlot(index3, key, value)
                    for (n in stashStart until _keys.size) if (_keys[n] == EMPTY) return setEmptySlot(n, key, value)
                    grow()
                    continue@retry
                }
                (index == ZERO_INDEX) -> return zeroValue.apply { zeroValue = value }
                else -> return _values[index].apply { _values[index] = value }
            }
        }
    }

    fun getOrPut(key: Int, callback: () -> T): T {
        val res = get(key)
        if (res == null) set(key, callback())
        return get(key)!!
    }

    private fun hash1(key: Int) = key and mask
    private fun hash2(key: Int) = (key * (-0x12477ce0)) and mask
    private fun hash3(key: Int) = (key * (-1262997959)) and mask

    fun removeRange(src: Int, dst: Int): Unit {
        //println("removeRange($src, $dst)")
        if (0 in src..dst && hasZero) {
            size--
            hasZero = false
            zeroValue = null
        }
        for (n in _keys.indices) {
            //println("$n: ${_keys[n]}")
            val key = _keys[n]
            if (key != EMPTY && key in src..dst) {
                _keys[n] = EMPTY
                _values[n] = null
                size--
            }
        }
        //for (key in keys.toList()) if (key in src..dst) remove(key)
    }

    data class Entry<T>(var key: Int, var value: T?)

    val keys get() = KeyIterable()
    val values get() = ValueIterable()
    val entries get() = EntryIterable()

    val pooledKeys get() = KeyIterable()
    val pooledValues get() = ValueIterable()
    val pooledEntries get() = EntryIterable()

    inner class KeyIterable() : Iterable<Int> {
        override operator fun iterator() = KeyIterator()
    }

    inner class ValueIterable() : Iterable<T> {
        override operator fun iterator() = ValueIterator()
    }

    inner class EntryIterable() : Iterable<Entry<T>> {
        override operator fun iterator() = EntryIterator()
    }

    inner class KeyIterator(private val it: Iterator = Iterator()) : kotlin.collections.Iterator<Int> {
        override operator fun hasNext() = it.hasNext()
        override operator fun next() = it.nextKey()
    }

    inner class ValueIterator(private val it: Iterator = Iterator()) : kotlin.collections.Iterator<T> {
        override operator fun hasNext() = it.hasNext()
        override operator fun next() = it.nextValue()!!
    }

    inner class EntryIterator(private val it: Iterator = Iterator()) : kotlin.collections.Iterator<Entry<T>> {
        override operator fun hasNext() = it.hasNext()
        override operator fun next() = it.nextEntry().copy() as Entry<T>
    }

    inner class Iterator {
        private var index: Int = if (hasZero) ZERO_INDEX else nextNonEmptyIndex(_keys, 0)
        private var entry = Entry<T?>(0, null)

        fun hasNext() = index != EOF

        fun nextEntry(): Entry<T?> = currentEntry().apply { next() }
        fun nextKey(): Int = currentKey().apply { next() }
        fun nextValue(): T? = currentValue().apply { next() }

        private fun currentEntry(): Entry<T?> {
            entry.key = currentKey()
            entry.value = currentValue()
            return entry
        }

        private fun currentKey(): Int = when (index) {
            ZERO_INDEX, EOF -> 0
            else -> _keys[index]
        }

        private fun currentValue(): T? = when (index) {
            ZERO_INDEX -> zeroValue
            EOF -> null
            else -> _values[index]
        }

        private fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
            for (n in offset until keys.size) if (keys[n] != EMPTY) return n
            return EOF
        }

        private fun next() {
            if (index != EOF) index = nextNonEmptyIndex(_keys, if (index == ZERO_INDEX) 0 else (index + 1))
        }
    }

    @PublishedApi
    internal fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
        for (n in offset until keys.size) if (keys[n] != EMPTY) return n
        return EOF
    }

    inline fun fastKeyForEach(callback: (key: Int) -> Unit) {
        var index: Int = if (hasZero) ZERO_INDEX else nextNonEmptyIndex(_keys, 0)
        while (index != EOF) {
            callback(
                when (index) {
                    ZERO_INDEX, EOF -> 0
                    else -> _keys[index]
                }
            )
            index = nextNonEmptyIndex(_keys, if (index == ZERO_INDEX) 0 else (index + 1))
        }
    }
    inline fun fastValueForEachNullable(callback: (value: T?) -> Unit): Unit {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEachNullable(callback: (key: Int, value: T?) -> Unit): Unit {
        fastKeyForEach { callback(it, this[it]) }
    }

    inline fun fastValueForEach(callback: (value: T) -> Unit): Unit {
        fastKeyForEach { callback(this[it]!!) }
    }
    inline fun fastForEach(callback: (key: Int, value: T) -> Unit): Unit {
        fastKeyForEach { callback(it, this[it]!!) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntMap<*>) return false
        fastForEachNullable { key, value -> if (other[key] != value) return false }
        return true
    }

    override fun hashCode(): Int {
        var out = 0
        fastForEachNullable { key, value -> out += key.hashCode() + value.hashCode() }
        return out
    }
}


fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
    val out = IntMap<T>()
    for ((k, v) in this) out[k] = v
    return out
}

class IntFloatMap {
    @PublishedApi
    internal val i = IntIntMap()

    val size: Int get() = i.size
    fun clear() = i.clear()
    fun remove(key: Int) = i.remove(key)

    val keys get() = i.keys
    val values
        get() = object {
            operator fun iterator() = object {
                val it = i.values.iterator()
                operator fun hasNext() = it.hasNext()
                operator fun next() = Float.fromBits(it.next())
            }
        }
    val entries
        get() = object {
            operator fun iterator() = object {
                val it = i.entries.iterator()
                operator fun hasNext() = it.hasNext()
                operator fun next() = it.next().let { Entry(it.key, Float.fromBits(it.value)) }
            }
        }

    data class Entry(val key: Int, val value: Float)

    operator fun contains(key: Int): Boolean = key in i
    operator fun get(key: Int): Float = Float.fromBits(i[key])
    operator fun set(key: Int, value: Float): Float = Float.fromBits(i.set(key, value.toRawBits()))

    inline fun fastKeyForEach(callback: (key: Int) -> Unit) {
        i.fastKeyForEach(callback)
    }

    inline fun fastValueForEach(callback: (value: Float) -> Unit): Unit {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEach(callback: (key: Int, value: Float) -> Unit): Unit {
        fastKeyForEach { callback(it, this[it]) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntMap<*>) return false
        fastForEach { key, value -> if (other[key] != value) return false }
        return true
    }

    override fun hashCode(): Int {
        var out = 0
        fastForEach { key, value -> out += key.hashCode() + value.hashCode() }
        return out
    }
}

class IntIntMap private constructor(private var nbits: Int, private val loadFactor: Double) {
    constructor(loadFactor: Double = 0.75) : this(4, loadFactor)

    companion object {
        @PublishedApi
        internal const val EOF = Int.MAX_VALUE - 1
        @PublishedApi
        internal const val ZERO_INDEX = Int.MAX_VALUE
        @PublishedApi
        internal const val EMPTY = 0
    }

    private var capacity = 1 shl nbits
    @PublishedApi
    internal var hasZero = false
    private var zeroValue: Int = 0
    private var mask = capacity - 1
    private var stashSize = 1 + ilog2(capacity)
    @PublishedApi internal var _keys = IntArray(capacity + stashSize)
    private var _values = IntArray(capacity + stashSize)
    private val stashStart get() = _keys.size - stashSize
    private var growSize: Int = (capacity * loadFactor).toInt()
    var size: Int = 0; private set

    private fun grow() {
        val new = IntIntMap(nbits + 3, loadFactor)

        for (n in _keys.indices) {
            val k = _keys[n]
            if (k != EMPTY) new[k] = _values[n]
        }

        this.nbits = new.nbits
        this.capacity = new.capacity
        this.mask = new.mask
        this.stashSize = new.stashSize
        this._keys = new._keys
        this._values = new._values
        this.growSize = new.growSize
    }

    operator fun contains(key: Int): Boolean = _getKeyIndex(key) >= 0

    private fun _getKeyIndex(key: Int): Int {
        if (key == 0) return if (hasZero) ZERO_INDEX else -1
        val index1 = hash1(key); if (_keys[index1] == key) return index1
        val index2 = hash2(key); if (_keys[index2] == key) return index2
        val index3 = hash3(key); if (_keys[index3] == key) return index3
        for (n in stashStart until _keys.size) if (_keys[n] == key) return n
        return -1
    }

    fun remove(key: Int): Boolean {
        val index = _getKeyIndex(key)
        if (index < 0) return false
        if (index == ZERO_INDEX) {
            hasZero = false
            zeroValue = 0
        } else {
            _keys[index] = EMPTY
        }
        size--
        return true
    }

    fun clear() {
        hasZero = false
        zeroValue = 0
        _keys.fill(0)
        _values.fill(0)
        size = 0
    }

    @Suppress("LoopToCallChain")
    operator fun get(key: Int): Int {
        val index = _getKeyIndex(key)
        if (index < 0) return 0
        if (index == ZERO_INDEX) return zeroValue
        return _values[index]
    }

    private fun setEmptySlot(index: Int, key: Int, value: Int): Int {
        if (_keys[index] != EMPTY) throw IllegalStateException()
        _keys[index] = key
        _values[index] = value
        size++
        return 0
    }

    operator fun set(key: Int, value: Int): Int {
        retry@ while (true) {
            val index = _getKeyIndex(key)
            when {
                index < 0 -> {
                    if (key == 0) {
                        hasZero = true
                        zeroValue = value
                        size++
                        return 0
                    }
                    if (size >= growSize) grow()
                    val index1 = hash1(key); if (_keys[index1] == EMPTY) return setEmptySlot(index1, key, value)
                    val index2 = hash2(key); if (_keys[index2] == EMPTY) return setEmptySlot(index2, key, value)
                    val index3 = hash3(key); if (_keys[index3] == EMPTY) return setEmptySlot(index3, key, value)
                    for (n in stashStart until _keys.size) if (_keys[n] == EMPTY) return setEmptySlot(n, key, value)
                    grow()
                    continue@retry
                }
                (index == ZERO_INDEX) -> return zeroValue.apply { zeroValue = value }
                else -> return _values[index].apply { _values[index] = value }
            }
        }
    }

    fun getOrPut(key: Int, callback: () -> Int): Int {
        if (key !in this) set(key, callback())
        return get(key)
    }

    private fun hash1(key: Int) = key and mask
    private fun hash2(key: Int) = (key * (-0x12477ce0)) and mask
    private fun hash3(key: Int) = (key * (-1262997959)) and mask

    data class Entry(var key: Int, var value: Int)

    val keys get() = KeyIterable()
    val values get() = ValueIterable()
    val entries get() = EntryIterable()

    val pooledKeys get() = KeyIterable()
    val pooledValues get() = ValueIterable()
    val pooledEntries get() = EntryIterable()

    inner class KeyIterable() {
        operator fun iterator() = KeyIterator()
    }

    inner class ValueIterable() {
        operator fun iterator() = ValueIterator()
    }

    inner class EntryIterable() {
        operator fun iterator() = EntryIterator()
    }

    inner class KeyIterator(private val it: Iterator = Iterator()) {
        operator fun hasNext() = it.hasNext()
        operator fun next() = it.nextKey()
    }

    inner class ValueIterator(private val it: Iterator = Iterator()) {
        operator fun hasNext() = it.hasNext()
        operator fun next() = it.nextValue()
    }

    inner class EntryIterator(private val it: Iterator = Iterator()) {
        operator fun hasNext() = it.hasNext()
        operator fun next() = it.nextEntry().copy()
    }

    inner class Iterator {
        private var index: Int = if (hasZero) ZERO_INDEX else nextNonEmptyIndex(_keys, 0)
        private var entry = Entry(0, 0)

        fun hasNext() = index != EOF

        fun nextEntry(): Entry = currentEntry().apply { next() }
        fun nextKey(): Int = currentKey().apply { next() }
        fun nextValue(): Int = currentValue().apply { next() }

        private fun currentEntry(): Entry {
            entry.key = currentKey()
            entry.value = currentValue()
            return entry
        }

        private fun currentKey(): Int = when (index) {
            ZERO_INDEX, EOF -> 0
            else -> _keys[index]
        }

        private fun currentValue(): Int = when (index) {
            ZERO_INDEX -> zeroValue
            EOF -> 0
            else -> _values[index]
        }

        private fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
            for (n in offset until keys.size) if (keys[n] != EMPTY) return n
            return EOF
        }

        private fun next() {
            if (index != EOF) index = nextNonEmptyIndex(_keys, if (index == ZERO_INDEX) 0 else (index + 1))
        }
    }

    @PublishedApi
    internal fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
        for (n in offset until keys.size) if (keys[n] != EMPTY) return n
        return EOF
    }

    inline fun fastKeyForEach(callback: (key: Int) -> Unit) {
        var index: Int = if (hasZero) IntMap.ZERO_INDEX else nextNonEmptyIndex(_keys, 0)
        while (index != IntMap.EOF) {
            callback(
                when (index) {
                    IntMap.ZERO_INDEX, IntMap.EOF -> 0
                    else -> _keys[index]
                }
            )
            index = nextNonEmptyIndex(_keys, if (index == IntMap.ZERO_INDEX) 0 else (index + 1))
        }
    }

    inline fun fastValueForEach(callback: (value: Int) -> Unit): Unit {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEach(callback: (key: Int, value: Int) -> Unit): Unit {
        fastKeyForEach { callback(it, this[it]) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntMap<*>) return false
        fastForEach { key, value -> if (other[key] != value) return false }
        return true
    }

    override fun hashCode(): Int {
        var out = 0
        fastForEach { key, value -> out += key.hashCode() + value.hashCode() }
        return out
    }
}
