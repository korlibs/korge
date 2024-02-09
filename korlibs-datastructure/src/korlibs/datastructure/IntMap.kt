@file:Suppress("RemoveEmptyPrimaryConstructor")

package korlibs.datastructure

import korlibs.datastructure.internal.*
import korlibs.math.ilog2Ceil
import korlibs.datastructure.iterators.fastForEach
import kotlin.collections.Collection
import kotlin.collections.Iterable
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.associateWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.copyOf
import kotlin.collections.indices
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max

private fun _mask(value: Int, mask: Int) = (value + ((value ushr 8) and 0xFF) + ((value ushr 16) and 0xFF) + ((value shr 24) and 0xFF)) and mask
//private fun _mask(value: Int, mask: Int) = (value + (value shr 16)) and mask
private fun _hash1(key: Int, mask: Int) = _mask(key, mask)
private fun _hash2(key: Int, mask: Int) = _mask(key * 0x4d2fa52d, mask)
private fun _hash3(key: Int, mask: Int) = _mask((key * 0x1194e069), mask)

class IntMap<T> internal constructor(private var nbits: Int, private val loadFactor: Double, dummy: Boolean = false) {
    constructor(initialCapacity: Int = 16, loadFactor: Double = 0.75) : this(max(4, ilog2Ceil(initialCapacity)), loadFactor, true)

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
    //private var stashSize = 1 + nbits * nbits * nbits
    internal var stashSize = 1 + nbits * nbits; private set
    internal val backSize get() = capacity + stashSize
    @PublishedApi
    internal var _keys = IntArray(backSize)
    private var _values = arrayOfNulls<Any>(backSize) as Array<T?>
    private val stashStart get() = _keys.size - stashSize
    private var growSize: Int = (capacity * loadFactor).toInt()
    var size: Int = 0; private set

    private fun grow() {
        val inc = if (nbits < 20) 3 else 1
        val newnbits = nbits + inc
        //println("newnbits=${newnbits}")
        //if (newnbits >= 23) {
        //    println("!!!!")
        //}
        val new = IntMap<T>(newnbits, loadFactor, true)

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

    private fun growStash() {
        this.stashSize = this.stashSize * 2
        this._keys = this._keys.copyOf(backSize)
        this._values = this._values.copyOf(backSize)
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
                    if (stashSize > 512) {
                        grow()
                    } else {
                        growStash()
                    }
                    continue@retry
                }
                (index == ZERO_INDEX) -> return zeroValue.apply { zeroValue = value }
                else -> return _values[index].apply { _values[index] = value }
            }
        }
    }

    inline fun getOrPut(key: Int, callback: (Int) -> T): T {
        val res = get(key)
        if (res == null) set(key, callback(key))
        return get(key)!!
    }

    private fun hash1(key: Int) = _hash1(key, mask)
    private fun hash2(key: Int) = _hash2(key, mask)
    private fun hash3(key: Int) = _hash3(key, mask)

    fun removeRange(src: Int, dst: Int) {
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

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator<T>(val map: IntMap<T>) {
        private var index: Int = if (map.hasZero) ZERO_INDEX else nextNonEmptyIndex(map._keys, 0)
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
            else -> map._keys[index]
        }

        private fun currentValue(): T? = when (index) {
            ZERO_INDEX -> map.zeroValue
            EOF -> null
            else -> map._values[index]
        }

        private fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
            for (n in offset until keys.size) if (keys[n] != EMPTY) return n
            return EOF
        }

        private fun next() {
            if (index != EOF) index = nextNonEmptyIndex(map._keys, if (index == ZERO_INDEX) 0 else (index + 1))
        }
    }

    @PublishedApi
    internal fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
        for (n in offset until keys.size) if (keys[n] != EMPTY) return n
        return EOF
    }

    @OptIn(ExperimentalContracts::class)
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
    inline fun fastValueForEachNullable(callback: (value: T?) -> Unit) {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEachNullable(callback: (key: Int, value: T?) -> Unit) {
        fastKeyForEach { callback(it, this[it]) }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun fastValueForEach(callback: (value: T) -> Unit) {
        fastKeyForEach { callback(this[it]!!) }
    }
    inline fun fastForEach(callback: (key: Int, value: T) -> Unit) {
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

    fun putAll(other: IntMap<T>) {
        other.fastForEach { key, value ->
            this[key] = value
        }
    }

    fun firstKey(): Int {
        fastKeyForEach { return it }
        error("firstKey on empty IntMap")
    }

    fun firstValue(): T {
        fastValueForEach { return it }
        error("firstValue on empty IntMap")
    }

    fun clone(): IntMap<T> = IntMap<T>(nbits, loadFactor, false).also { it.putAll(this) }
}


fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
    val out = IntMap<T>((this.size * 1.25).toInt())
    for ((k, v) in this) out[k] = v
    return out
}

fun <T> Collection<Pair<Int, T>>.toIntMap(): IntMap<T> {
    val out = IntMap<T>((this.size * 1.25).toInt())
    for ((k, v) in this) out[k] = v
    return out
}

fun <T> Iterable<T>.associateByInt(block: (index: Int, value: T) -> Int): IntMap<T> {
    var n = 0
    val out = IntMap<T>()
    for (it in this) {
        out[block(n++, it)] = it
    }
    return out
}

/*
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

    inline fun fastValueForEach(callback: (value: Float) -> Unit) {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEach(callback: (key: Int, value: Float) -> Unit) {
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
*/

class IntIntMap internal constructor(private var nbits: Int, private val loadFactor: Double, dummy: Boolean) {
    constructor(initialCapacity: Int = 16, loadFactor: Double = 0.75) : this(max(4, ilog2Ceil(initialCapacity)), loadFactor, true)

    override fun toString(): String = this.toMap().toString()

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
    internal var stashSize = 1 + nbits * nbits; private set
    internal val backSize get() = capacity + stashSize
    @PublishedApi internal var _keys = IntArray(backSize)
    private var _values = IntArray(backSize)
    private val stashStart get() = _keys.size - stashSize
    private var growSize: Int = (capacity * loadFactor).toInt()
    var size: Int = 0; private set

    fun clone(): IntIntMap {
        val out = IntIntMap()
        out.capacity = this.capacity
        out.hasZero = this.hasZero
        out.mask = this.mask
        out.stashSize = this.stashSize
        out._keys = this._keys.copyOf()
        out._values = this._values.copyOf()
        out.growSize = this.growSize
        out.size = this.size
        return out
    }

    fun toMap(out: MutableMap<Int, Int> = linkedHashMapOf()): Map<Int, Int> {
        fastForEach { key, value -> out[key] = value }
        return out
    }

    private fun grow() {
        val inc = if (nbits < 20) 3 else 1
        val newnbits = nbits + inc
        //println("newnbits=${newnbits}")
        //if (newnbits >= 23) {
        //    println("!!!!")
        //}
        val new = IntIntMap(newnbits, loadFactor, true)

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

    private fun growStash() {
        this.stashSize = this.stashSize * 2
        this._keys = this._keys.copyOf(backSize)
        this._values = this._values.copyOf(backSize)
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
                    if (stashSize > 512) {
                        grow()
                    } else {
                        growStash()
                    }
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

    private fun hash1(key: Int) = _hash1(key, mask)
    private fun hash2(key: Int) = _hash2(key, mask)
    private fun hash3(key: Int) = _hash3(key, mask)

    data class Entry(var key: Int, var value: Int)

    val keys get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextKey()} ) } }
    val values get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextValue()} ) } }
    val entries get() = Iterable { Iterator(this).let { Iterator({ it.hasNext() }, { it.nextEntry()} ) } }

    val pooledKeys get() = keys
    val pooledValues get() = values
    val pooledEntries get() = entries

    class Iterator(val map: IntIntMap) {
        private var index: Int = if (map.hasZero) ZERO_INDEX else nextNonEmptyIndex(map._keys, 0)
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
            else -> map._keys[index]
        }

        private fun currentValue(): Int = when (index) {
            ZERO_INDEX -> map.zeroValue
            EOF -> 0
            else -> map._values[index]
        }

        private fun nextNonEmptyIndex(keys: IntArray, offset: Int): Int {
            for (n in offset until keys.size) if (keys[n] != EMPTY) return n
            return EOF
        }

        private fun next() {
            if (index != EOF) index = nextNonEmptyIndex(map._keys, if (index == ZERO_INDEX) 0 else (index + 1))
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

    inline fun fastValueForEach(callback: (value: Int) -> Unit) {
        fastKeyForEach { callback(this[it]) }
    }
    inline fun fastForEach(callback: (key: Int, value: Int) -> Unit) {
        fastKeyForEach { callback(it, this[it]) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntIntMap) return false
        fastForEach { key, value -> if (other[key] != value) return false }
        return true
    }

    override fun hashCode(): Int {
        var out = 0
        fastForEach { key, value -> out += key.hashCode() + value.hashCode() }
        return out
    }
}

fun <T> IntMap<T>.toMap(): Map<Int, T> = keys.associateWith { this[it].fastCastTo<T>() }

fun <T> intMapOf(vararg pairs: Pair<Int, T>) = IntMap<T>(pairs.size).also { map ->
    pairs.fastForEach {
        map[it.first] = it.second
    }
}

fun intIntMapOf(vararg pairs: Pair<Int, Int>): IntIntMap = IntIntMap(pairs.size).also { map ->
    pairs.fastForEach {
        map[it.first] = it.second
    }
}
