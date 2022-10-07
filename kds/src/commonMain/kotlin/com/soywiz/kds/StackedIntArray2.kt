package com.soywiz.kds

class StackedIntArray2(val width: Int, val height: Int, val empty: Int = -1) {
    val level = IntArray2(width, height, 0)
    val data = fastArrayListOf<IntArray2>()

    val maxLevel: Int get() = data.size

    companion object {
        operator fun invoke(vararg layers: IntArray2, width: Int = layers.first().width, height: Int = layers.first().height): StackedIntArray2 {
            val stacked = StackedIntArray2(width, height)
            stacked.level.fill { layers.size }
            stacked.data.addAll(layers)
            return stacked
        }
    }

    fun ensureLevel(level: Int) {
        while (level >= data.size) data.add(IntArray2(width, height, empty))
    }

    fun setLayer(level: Int, data: IntArray2) {
        ensureLevel(level)
        this.data[level] = data
    }

    operator fun set(level: Int, x: Int, y: Int, value: Int) {
        ensureLevel(level)
        data[level][x, y] = value
    }

    operator fun get(x: Int, y: Int, level: Int): Int {
        if (level > this.level[x, y]) return empty
        return data[level][x, y]
    }

    fun getStackLevel(x: Int, y: Int): Int {
        return this.level[x, y]
    }

    fun getFirst(x: Int, y: Int): Int {
        val level = this.level[x, y]
        if (level == 0) return empty
        return data[0][x, y]
    }

    fun getLast(x: Int, y: Int): Int {
        val level = this.level[x, y]
        if (level == 0) return empty
        return data[level - 1][x, y]
    }

    fun push(x: Int, y: Int, value: Int) {
        set(level[x, y]++, x, y, value)
    }

    fun removeLast(x: Int, y: Int) {
        level[x, y] = (level[x, y] - 1).coerceAtLeast(0)
    }
}

fun IntArray2.toStacked(): StackedIntArray2 = StackedIntArray2(this)
