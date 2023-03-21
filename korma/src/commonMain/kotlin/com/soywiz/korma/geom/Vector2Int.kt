package com.soywiz.korma.geom

import com.soywiz.kds.pack.*

typealias PointInt = Vector2Int

//@KormaValueApi
inline class Vector2Int internal constructor(internal val raw: Int2Pack) {
    val x: Int get() = raw.x
    val y: Int get() = raw.y

    constructor() : this(Int2Pack(0, 0))
    constructor(x: Int, y: Int) : this(Int2Pack(x, y))

    val mutable: MPointInt get() = MPointInt(x, y)
    operator fun component1(): Int = x
    operator fun component2(): Int = y

    fun copy(x: Int = this.x, y: Int = this.y): Vector2Int = Vector2Int(x, y)

    operator fun plus(that: Vector2Int): Vector2Int = Vector2Int(this.x + that.x, this.y + that.y)
    operator fun minus(that: Vector2Int): Vector2Int = Vector2Int(this.x - that.x, this.y - that.y)
    operator fun times(that: Vector2Int): Vector2Int = Vector2Int(this.x * that.x, this.y * that.y)
    operator fun div(that: Vector2Int): Vector2Int = Vector2Int(this.x / that.x, this.y / that.y)
    operator fun rem(that: Vector2Int): Vector2Int = Vector2Int(this.x % that.x, this.y % that.y)
}
