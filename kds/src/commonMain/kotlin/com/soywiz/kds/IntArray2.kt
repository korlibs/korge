package com.soywiz.kds

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
    operator fun set(x: Int, y: Int, value: Int) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Int? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Int) {
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
