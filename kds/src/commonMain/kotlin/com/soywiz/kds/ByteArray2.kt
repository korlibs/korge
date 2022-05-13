package com.soywiz.kds


@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class ByteArray2(override val width: Int, override val height: Int, val data: ByteArray) :
    IArray2<Byte> {
    companion object {
        inline operator fun invoke(width: Int, height: Int, fill: Byte): ByteArray2 =
            ByteArray2(width, height, ByteArray(width * height) { fill })

        inline operator fun invoke(width: Int, height: Int, gen: (n: Int) -> Byte): ByteArray2 =
            ByteArray2(width, height, ByteArray(width * height) { gen(it) })

        inline fun withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Byte): ByteArray2 =
            ByteArray2(
                width,
                height,
                ByteArray(width * height) { gen(it % width, it / width) })

        inline operator fun invoke(rows: List<List<Byte>>): ByteArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (ByteArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Byte
        ): ByteArray2 {
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
            val width = lines.maxOfOrNull { it.length } ?: 0
            val height = lines.size

            return ByteArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun invoke(
            map: String,
            default: Byte,
            transform: Map<Char, Byte>
        ): ByteArray2 {
            return invoke(map) { c, _, _ -> transform[c] ?: default }
        }

        inline fun fromString(
            maps: Map<Char, Byte>,
            default: Byte,
            code: String,
            marginChar: Char = '\u0000'
        ): ByteArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    operator fun get(x: Int, y: Int): Byte = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Byte) {
        data[index(x, y)] = value
    }

    fun tryGet(x: Int, y: Int): Byte? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Byte) {
        if (inside(x, y)) data[index(x, y)] = value
    }

    override fun setAt(idx: Int, value: Byte) {
        this.data[idx] = value
    }

    override fun printAt(idx: Int) {
        print(this.data[idx])
    }

    override fun equalsAt(idx: Int, value: Byte): Boolean {
        return this.data[idx] == value
    }

    override fun getAt(idx: Int): Byte = this.data[idx]

    override fun equals(other: Any?): Boolean {
        return (other is ByteArray2) && this.width == other.width && this.height == other.height &&
            this.data.contentEquals(
                other.data
            )
    }

    override fun hashCode(): Int = width + height + data.contentHashCode()

    fun clone() = ByteArray2(width, height, data.copyOf())

    override fun iterator(): Iterator<Byte> = data.iterator()

    override fun toString(): String = asString()
}
