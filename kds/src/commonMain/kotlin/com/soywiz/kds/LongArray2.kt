package com.soywiz.kds


@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class LongArray2(override val width: Int, override val height: Int, val data: LongArray) :
    IArray2<Long> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Long): LongArray2 =
            LongArray2(width, height, LongArray(width * height) { fill } as LongArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Long): LongArray2 =
            LongArray2(width, height, LongArray(width * height) { gen(it) } as LongArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Long): LongArray2 =
            LongArray2(
                width,
                height,
                LongArray(width * height) { gen(it % width, it / width) } as LongArray)

        inline operator fun invoke(rows: List<List<Long>>): LongArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (LongArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Long
        ): LongArray2 {
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

            return LongArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Long,
            transform: Map<Char, Long>
        ): LongArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Long>,
            default: Long,
            code: String,
            marginChar: Char = '\u0000'
        ): LongArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Long = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Long) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Long? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Long) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Long) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Long): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Long = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is LongArray2) && this.width == other.width && this.height == other.height &&
            this.data.contentEquals(
                other.data
            )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = LongArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Long> = data.iterator()

    override fun toString(): String = asString()
}
