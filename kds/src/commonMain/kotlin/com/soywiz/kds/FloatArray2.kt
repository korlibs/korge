package com.soywiz.kds

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class FloatArray2(override val width: Int, override val height: Int, val data: FloatArray) :
    IArray2<Float> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { fill })

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { gen(it) })

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
    operator fun set(x: Int, y: Int, value: Float) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Float? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Float) {
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
