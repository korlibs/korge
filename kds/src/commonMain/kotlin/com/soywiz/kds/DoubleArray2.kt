package com.soywiz.kds

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class DoubleArray2(override val width: Int, override val height: Int, val data: DoubleArray) : IArray2<Double> {
    init {
        IArray2.checkArraySize(width, height, data.size)
    }
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
    operator fun set(x: Int, y: Int, value: Double) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Double? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Double) {
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
