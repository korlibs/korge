package com.soywiz.kds

inline fun <TGen : Any, RGen : Any> IArray2<TGen>.map2(gen: (x: Int, y: Int, v: TGen) -> RGen) =
    Array2<RGen>(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this.getAt(x, y))
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
            return invoke(map) { c, _, _ -> transform[c] ?: default }
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
    operator fun set(x: Int, y: Int, value: TGen) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): TGen? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: TGen) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = Array2<TGen>(width, height, data.copyOf())

    override fun iterator(): Iterator<TGen> = data.iterator()

    override fun toString(): String = asString()
}
