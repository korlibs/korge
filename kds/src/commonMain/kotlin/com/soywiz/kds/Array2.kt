package com.soywiz.kds

inline fun <TGen : Any, RGen : Any> Array2<TGen>.map2(gen: (x: Int, y: Int, v: TGen) -> RGen): Array2<RGen> =
    Array2<RGen>(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this[x, y])
    }

inline fun IntArray2.map2(gen: (x: Int, y: Int, v: Int) -> Int): IntArray2 =
    IntArray2(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this[x, y])
    }

inline fun FloatArray2.map2(gen: (x: Int, y: Int, v: Float) -> Float): FloatArray2 =
    FloatArray2(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this[x, y])
    }

inline fun DoubleArray2.map2(gen: (x: Int, y: Int, v: Double) -> Double): DoubleArray2 =
    DoubleArray2(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this[x, y])
    }

// Note: Due to autoboxing, the get()/set() methods are implemented in the typed implementations
// without an override (meaning we don't require the interface to have get/set methods).
//
// https://discuss.kotlinlang.org/t/performance-question-related-to-boxing-and-interface-implementation/17387
interface IArray2<E> : Iterable<E> {
    val width: Int
    val height: Int

    val size: Int
        get() = width * height

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    // Prints the value at the given index.
    fun printAt(idx: Int)

    fun printAt(x: Int, y: Int) = printAt(index(x, y))

    fun setAt(idx: Int, value: E)

    // Returns true if the value at `idx` equals the `value`.
    fun equalsAt(idx: Int, value: E): Boolean

    fun getAt(idx: Int): E

    fun getAt(x: Int, y: Int) = getAt(index(x, y))

    fun setAt(x: Int, y: Int, value: E): Unit {
        setAt(index(x, y), value)
    }

    fun set(rows: List<List<E>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                setAt(n++, row[x])
            }
        }
    }

    operator fun contains(v: E): Boolean {
        return this.iterator().asSequence().any { it == v }
    }

    fun getPositionsWithValue(value: E) =
        (0 until size).filter { equalsAt(it, value) }.map { Pair(it % width, it / width) }

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                printAt(x, y)
            }
            println()
        }
    }

    fun toStringList(charMap: (E) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + CharArray(width) { x -> charMap(getAt(x, y)) }.concatToString()
        }
    }

    fun asString(margin: String = "", charMap: (E) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun asString(map: Map<E, Char>, margin: String = ""): String =
        asString(margin = margin) { map[it] ?: ' ' }

    fun asString(): String = (0 until height)
        .joinToString("\n") { y ->
            (0 until width).map { x -> getAt(x, y) }.joinToString(", ")
        }
}

inline fun <E> IArray2<E>.fill(gen: (old: E) -> E) {
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            setAt(n, gen(getAt(n)))
            n++
        }
    }
}

inline fun <E> IArray2<E>.each(callback: (x: Int, y: Int, v: E) -> Unit) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            callback(x, y, getAt(x, y))
        }
    }
}

fun <E> IArray2<E>.index(x: Int, y: Int): Int {
    if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
    return y * width + x
}

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class Array2<TGen>(override val width: Int, override val height: Int, val data: Array<TGen>) :
    IArray2<TGen> {
    companion object {
        inline operator fun <TGen : Any> invoke(width: Int, height: Int, fill: TGen): Array2<TGen> =
            Array2<TGen>(width, height, Array<Any>(width * height) { fill } as Array<TGen>)

        inline operator fun <TGen : Any> invoke(
            width: Int,
            height: Int,
            gen: (n: Int) -> TGen
        ): Array2<TGen> =
            Array2<TGen>(width, height, Array<Any>(width * height) { gen(it) } as Array<TGen>)

        inline fun <TGen : Any> withGen(
            width: Int,
            height: Int,
            gen: (x: Int, y: Int) -> TGen
        ): Array2<TGen> =
            Array2<TGen>(
                width,
                height,
                Array<Any>(width * height) { gen(it % width, it / width) } as Array<TGen>)

        inline operator fun <TGen : Any> invoke(rows: List<List<TGen>>): Array2<TGen> {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (Array2<TGen>(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun <TGen : Any> invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> TGen
        ): Array2<TGen> {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.maxOrNull() ?: 0
            val height = lines.size

            return Array2<TGen>(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun <TGen : Any> invoke(
            map: String,
            default: TGen,
            transform: Map<Char, TGen>
        ): Array2<TGen> {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun <TGen : Any> fromString(
            maps: Map<Char, TGen>,
            default: TGen,
            code: String,
            marginChar: Char = '\u0000'
        ): Array2<TGen> {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    override fun setAt(idx: Int, value: TGen) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: TGen): Boolean {
        return this.data[idx]?.equals(value) ?: false
    }

    override fun getAt(idx: Int): TGen = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is Array2<*/*TGen*/>) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    operator fun get(x: Int, y: Int): TGen = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: TGen): Unit {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): TGen? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: TGen): Unit {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = Array2<TGen>(width, height, data.copyOf())

    override fun iterator(): Iterator<TGen> = data.iterator()

    override fun toString(): String = asString()
}


// Int

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class IntArray2(override val width: Int, override val height: Int, val data: IntArray) :
    IArray2<Int> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Int): IntArray2 =
            IntArray2(width, height, IntArray(width * height) { fill } as IntArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Int): IntArray2 =
            IntArray2(width, height, IntArray(width * height) { gen(it) } as IntArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Int): IntArray2 =
            IntArray2(
                width,
                height,
                IntArray(width * height) { gen(it % width, it / width) } as IntArray)

        inline operator fun invoke(rows: List<List<Int>>): IntArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (IntArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Int
        ): IntArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.maxOrNull() ?: 0
            val height = lines.size

            return IntArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Int,
            transform: Map<Char, Int>
        ): IntArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Int>,
            default: Int,
            code: String,
            marginChar: Char = '\u0000'
        ): IntArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Int = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Int): Unit {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Int? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Int): Unit {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Int) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Int): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Int = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is IntArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = IntArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Int> = data.iterator()

    override fun toString(): String = asString()
}


// Double

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class DoubleArray2(override val width: Int, override val height: Int, val data: DoubleArray) :
    IArray2<Double> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Double): DoubleArray2 =
            DoubleArray2(width, height, DoubleArray(width * height) { fill } as DoubleArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Double): DoubleArray2 =
            DoubleArray2(width, height, DoubleArray(width * height) { gen(it) } as DoubleArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Double): DoubleArray2 =
            DoubleArray2(
                width,
                height,
                DoubleArray(width * height) { gen(it % width, it / width) } as DoubleArray)

        inline operator fun invoke(rows: List<List<Double>>): DoubleArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (DoubleArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Double
        ): DoubleArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.maxOrNull() ?: 0
            val height = lines.size

            return DoubleArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Double,
            transform: Map<Char, Double>
        ): DoubleArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Double>,
            default: Double,
            code: String,
            marginChar: Char = '\u0000'
        ): DoubleArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Double = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Double): Unit {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Double? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Double): Unit {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Double) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Double): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Double = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is DoubleArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = DoubleArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Double> = data.iterator()

    override fun toString(): String = asString()
}


// Float

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class FloatArray2(override val width: Int, override val height: Int, val data: FloatArray) :
    IArray2<Float> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { fill } as FloatArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { gen(it) } as FloatArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Float): FloatArray2 =
            FloatArray2(
                width,
                height,
                FloatArray(width * height) { gen(it % width, it / width) } as FloatArray)

        inline operator fun invoke(rows: List<List<Float>>): FloatArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (FloatArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Float
        ): FloatArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.maxOrNull() ?: 0
            val height = lines.size

            return FloatArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Float,
            transform: Map<Char, Float>
        ): FloatArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Float>,
            default: Float,
            code: String,
            marginChar: Char = '\u0000'
        ): FloatArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Float = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Float): Unit {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Float? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Float): Unit {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Float) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Float): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Float = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is FloatArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = FloatArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Float> = data.iterator()

    override fun toString(): String = asString()
}
