package com.soywiz.kds

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class BooleanArray2(override val width: Int, override val height: Int, val data: BooleanArray) :
    IArray2<Boolean> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Boolean): BooleanArray2 =
            BooleanArray2(width, height, BooleanArray(width * height) { fill } as BooleanArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Boolean): BooleanArray2 =
            BooleanArray2(width, height, BooleanArray(width * height) { gen(it) } as BooleanArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Boolean): BooleanArray2 =
            BooleanArray2(
                width,
                height,
                BooleanArray(width * height) { gen(it % width, it / width) } as BooleanArray)

        inline operator fun invoke(rows: List<List<Boolean>>): BooleanArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (BooleanArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Boolean
        ): BooleanArray2 {
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

            return BooleanArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Boolean,
            transform: Map<Char, Boolean>
        ): BooleanArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Boolean>,
            default: Boolean,
            code: String,
            marginChar: Char = '\u0000'
        ): BooleanArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Boolean = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Boolean) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Boolean? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Boolean) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Boolean) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Boolean): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Boolean = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is BooleanArray2) && this.width == other.width && this.height == other.height &&
            this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = BooleanArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Boolean> = data.iterator()

    override fun toString(): String = asString()
}
