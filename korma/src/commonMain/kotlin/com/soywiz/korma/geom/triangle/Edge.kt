package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*

data class Edge internal constructor(
    val dummy: Boolean,
    val p: MPoint,
    val q: MPoint
) {
    @Suppress("unused")
    fun hasPoint(point: MPoint): Boolean = (p == point) || (q == point)

    companion object {
        operator fun invoke(p1: MPoint, p2: MPoint): Edge {
            val comp = MPoint.compare(p1, p2)
            if (comp == 0) throw Error("Repeat points")
            val p = if (comp < 0) p1 else p2
            val q = if (comp < 0) p2 else p1
            return Edge(true, p, q)
        }

        fun getUniquePointsFromEdges(edges: Iterable<Edge>): List<MPoint> =
            edges.flatMap { listOf(it.p, it.q) }.distinct()
    }


    override fun toString(): String = "Edge(${this.p}, ${this.q})"
}
