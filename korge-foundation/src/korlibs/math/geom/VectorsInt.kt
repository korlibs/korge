package korlibs.math.geom

typealias PointInt = Vector2I

data class Vector3I(val x: Int, val y: Int, val z: Int)
data class Vector4I(val x: Int, val y: Int, val z: Int, val w: Int)

//@KormaValueApi
data class Vector2I(val x: Int, val y: Int) {
    //operator fun component1(): Int = x
    //operator fun component2(): Int = y
    //fun copy(x: Int = this.x, y: Int = this.y): Vector2Int = Vector2Int(x, y)

//inline class Vector2Int(internal val raw: Int2Pack) {

    companion object {
        val ZERO = Vector2I(0, 0)
    }

    //val x: Int get() = raw.i0
    //val y: Int get() = raw.i1

    constructor() : this(0, 0)
    //constructor(x: Int, y: Int) : this(int2PackOf(x, y))

    operator fun plus(that: Vector2I): Vector2I = Vector2I(this.x + that.x, this.y + that.y)
    operator fun minus(that: Vector2I): Vector2I = Vector2I(this.x - that.x, this.y - that.y)
    operator fun times(that: Vector2I): Vector2I = Vector2I(this.x * that.x, this.y * that.y)
    operator fun div(that: Vector2I): Vector2I = Vector2I(this.x / that.x, this.y / that.y)
    operator fun rem(that: Vector2I): Vector2I = Vector2I(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}

fun Vector2I.toFloat(): Vector2F = Vector2F(x, y)
fun Vector2I.toDouble(): Vector2D = Vector2D(x, y)
