package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*

data class Edge internal constructor(
    val dummy: Boolean,
    val p: Point,
    val q: Point
) {
    @Suppress("unused")
    fun hasPoint(point: MPoint): Boolean = (p == point.point) || (q == point.point)
    fun hasPoint(point: Point): Boolean = (p == point) || (q == point)

    companion object {
        operator fun invoke(p1: Point, p2: Point): Edge {
            val comp = Point.compare(p1, p2)
            if (comp == 0) throw Error("Repeat points")
            val p = if (comp < 0) p1 else p2
            val q = if (comp < 0) p2 else p1
            return Edge(true, p, q)
        }

        fun getUniquePointsFromEdges(edges: Iterable<Edge>): PointList =
            edges.flatMap { listOf(it.p, it.q) }.distinct().toPointArrayList()
    }


    override fun toString(): String = "Edge(${this.p}, ${this.q})"
}
