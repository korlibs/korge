package com.soywiz.kds


@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class CharArray2(override val width: Int, override val height: Int, val data: CharArray) :
    IArray2<Char> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Char): CharArray2 =
            CharArray2(width, height, CharArray(width * height) { fill } as CharArray)

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Char): CharArray2 =
            CharArray2(width, height, CharArray(width * height) { gen(it) } as CharArray)

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Char): CharArray2 =
            CharArray2(
                width,
                height,
                CharArray(width * height) { gen(it % width, it / width) } as CharArray)

        inline operator fun invoke(rows: List<List<Char>>): CharArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (CharArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Char
        ): CharArray2 {
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

            return CharArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Char,
            transform: Map<Char, Char>
        ): CharArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Char>,
            default: Char,
            code: String,
            marginChar: Char = '\u0000'
        ): CharArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Char = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Char) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Char? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Char) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Char) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Char): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Char = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is CharArray2) && this.width == other.width && this.height == other.height &&
            this.data.contentEquals(
                other.data
            )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = CharArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Char> = data.iterator()

    override fun toString(): String = asString()
}
