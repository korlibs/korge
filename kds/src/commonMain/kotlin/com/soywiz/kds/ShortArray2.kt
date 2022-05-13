package com.soywiz.kds


@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class ShortArray2(override val width: Int, override val height: Int, val data: ShortArray) :
    IArray2<Short> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Short): ShortArray2 =
            ShortArray2(width, height, ShortArray(width * height) { fill } as ShortArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Short): ShortArray2 =
            ShortArray2(width, height, ShortArray(width * height) { gen(it) } as ShortArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Short): ShortArray2 =
            ShortArray2(
                width,
                height,
                ShortArray(width * height) { gen(it % width, it / width) } as ShortArray)

        inline operator fun invoke(rows: List<List<Short>>): ShortArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (ShortArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Short
        ): ShortArray2 {
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

            return ShortArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Short,
            transform: Map<Char, Short>
        ): ShortArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Short>,
            default: Short,
            code: String,
            marginChar: Char = '\u0000'
        ): ShortArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Short = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Short) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Short? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Short) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Short) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Short): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Short = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is ShortArray2) && this.width == other.width && this.height == other.height &&
            this.data.contentEquals(
                other.data
            )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = ShortArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Short> = data.iterator()

    override fun toString(): String = asString()
}
