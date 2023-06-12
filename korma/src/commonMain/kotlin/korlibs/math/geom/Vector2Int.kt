package korlibs.math.geom

import korlibs.memory.pack.*

typealias PointInt = Vector2Int

//@KormaValueApi
data class Vector2Int(val x: Int, val y: Int) {
    //operator fun component1(): Int = x
    //operator fun component2(): Int = y
    //fun copy(x: Int = this.x, y: Int = this.y): Vector2Int = Vector2Int(x, y)

//inline class Vector2Int(internal val raw: Int2Pack) {

    companion object {
        val ZERO = Vector2Int(0, 0)
    }

    //val x: Int get() = raw.i0
    //val y: Int get() = raw.i1

    constructor() : this(0, 0)
    //constructor(x: Int, y: Int) : this(int2PackOf(x, y))

    val mutable: MPointInt get() = MPointInt(x, y)

    operator fun plus(that: Vector2Int): Vector2Int = Vector2Int(this.x + that.x, this.y + that.y)
    operator fun minus(that: Vector2Int): Vector2Int = Vector2Int(this.x - that.x, this.y - that.y)
    operator fun times(that: Vector2Int): Vector2Int = Vector2Int(this.x * that.x, this.y * that.y)
    operator fun div(that: Vector2Int): Vector2Int = Vector2Int(this.x / that.x, this.y / that.y)
    operator fun rem(that: Vector2Int): Vector2Int = Vector2Int(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}
