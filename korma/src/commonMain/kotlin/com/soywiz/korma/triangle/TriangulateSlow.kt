/*
package com.soywiz.korma.triangle

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

object TriangulateSlow {
    class EdgeBuilder : EdgeList() {
        override val edges = arrayListOf<Edge>()
        var movePos = Point(0, 0)
        var pos = Point(0, 0)

        fun moveTo(x: Number, y: Number) = moveTo(Point(x, y))
        fun lineTo(x: Number, y: Number) = lineTo(Point(x, y))

        fun moveTo(p: Point) {
            movePos = p
            pos = p
        }

        fun lineTo(p: Point) {
            edges.add(Edge(pos, p))
            pos = p
        }

        fun close() {
            lineTo(movePos)
        }
    }

    data class Triangle(val p1: Point, val p2: Point, val p3: Point, val dummy: Boolean) {
        companion object {
            operator fun invoke(p1: Point, p2: Point, p3: Point): Triangle {
                val points = listOf(p1, p2, p3).sorted()
                return Triangle(points[0], points[1], points[2], true)
            }
        }

        val e1 = Edge(p1, p2)
        val e2 = Edge(p1, p3)
        val e3 = Edge(p2, p3)

        fun has(edge: Edge) = edge == e1 || edge == e2 || edge == e3
        fun contains(point: Point): Boolean {
            if (point == p1 || point == p2 || point == p3) return false

            val d1 = sign(point, p1, p2)
            val d2 = sign(point, p2, p3)
            val d3 = sign(point, p3, p1)

            val has_neg = d1 < 0 || d2 < 0 || d3 < 0
            val has_pos = d1 > 0 || d2 > 0 || d3 > 0

            return !(has_neg && has_pos)
        }

        fun sign(p1: Point, p2: Point, p3: Point): Double {
            return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
        }


        val center: Point = Point(
            (p1.x + p2.x + p3.x) / 3,
            (p1.y + p2.y + p3.y) / 3,
        )
    }

    fun normalizeOverlaps(edges: List<Edge>): List<Edge> {
        val newEdges = arrayListOf<Edge>()
        val edges = edges.toMutableList()
        while (edges.isNotEmpty()) {
            val edge = edges.removeLast()
            val overlappingEdge = edges.firstOrNull { edge != it && edge.intersects(it) }
            if (overlappingEdge != null) {
                val point = Edge.getIntersectXY(overlappingEdge, edge)
                if (point != null) {
                    val e1 = Edge(edge.p1, point)
                    val e2 = Edge(point, edge.p2)

                    //println("overlappingEdge: $edge, $overlappingEdge, $point")
                    edges.add(e1)
                    edges.add(e2)
                    newEdges.add(e1)
                    newEdges.add(e2)
                    continue
                }
            }
            newEdges.add(edge)
        }
        return newEdges
    }

    abstract class EdgeList {
        abstract val edges: List<Edge>

        fun triangulate(): List<Triangle> {
            val triangles = linkedSetOf<Triangle>()
            val edges = normalizeOverlaps(edges).toMutableList()
            //val edges = edges.toMutableList()
            val allEdges = linkedSetOf<Edge>()
            allEdges += edges
            while (edges.isNotEmpty()) {
                val edge = edges.removeLast()
                val availablePoints = edges.flatMap { listOf(it.p1, it.p2) }.filter { !edge.hasPoint(it) }.toSet()
                var createdTriangle = false
                for (point in availablePoints) {
                    val edge1 = Edge(edge.p1, point)
                    val edge2 = Edge(edge.p2, point)
                    val triangle = Triangle(edge.p1, edge.p2, point)
                    val anyIntersectionEdge1 = allEdges.firstOrNull { !triangle.has(it) && edge1.intersects(it) }
                    val anyIntersectionEdge2 = allEdges.firstOrNull { !triangle.has(it) && edge2.intersects(it) }
                    val anyIntersectionEdge = anyIntersectionEdge1 ?: anyIntersectionEdge2

                    val triangleContainsPoint = availablePoints.firstOrNull { triangle.contains(it) }


                    val triangleExists = triangle in triangles
                    val insideEvenOdd = insideEvenOdd(triangle.center)
                    if (anyIntersectionEdge == null && triangleContainsPoint == null && insideEvenOdd && !triangleExists) {
                        if (edge !in allEdges) edges.add(edge)
                        if (edge1 !in allEdges) edges.add(edge1)
                        if (edge2 !in allEdges) edges.add(edge2)
                        //allEdges += edge
                        allEdges += edge
                        allEdges += edge1
                        allEdges += edge2
                        triangles.add(triangle)
                        //println("ADD TRIANGLE: $triangle")
                        createdTriangle = true
                        break
                    }
                }
                if (!createdTriangle) {
                    //println("EDGE $edge doesn't have any triangle")
                }
            }
            return triangles.toList()
        }

        fun insideEvenOdd(p: Point): Boolean {
            var count = 0
            for (edge in edges) {
                if (edge.isCoplanarX) continue
                if (!edge.containsY(p.y)) continue
                //println("edge: $edge, orientation: ${edge.orientation(p)}, p:$p")
                if (edge.orientation(p) > 0) {
                    count++
                }
            }
            return count % 2 == 1
        }


    }

    data class Point(val x: Double, val y: Double) : Comparable<Point> {
        constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())
        constructor() : this(0, 0)

        override fun toString(): String = "($x,$y)"
        override fun equals(other: Any?): Boolean = other is Point && this.x == other.x && this.y == other.y
        override fun compareTo(other: Point): Int {
            val result = this.y.compareTo(other.y)
            return if (result != 0) result else this.x.compareTo(other.x)
        }
    }

    fun Double.almostEqual(other: Double) = (other - this).absoluteValue <= 0.000001
    fun Double.normalize(): Double {
        if (this.absoluteValue < 0.00000001) return 0.0
        return this
    }

    data class Edge(val p1: Point, val p2: Point, val dummy: Boolean) {
        fun intersects(other: Edge) = Edge.intersects(this, other)

        override fun toString(): String = "Edge($p1, $p2)"

        init {
            assert(p1.y <= p2.y)
        }

        fun hasPoint(p: Point) = p1 == p || p2 == p

        val ax = p1.x
        val ay = p1.y

        val bx = p2.x
        val by = p2.y

        val minX = kotlin.math.min(p1.x, p2.x)
        val maxX = kotlin.math.max(p1.x, p2.x)

        val minY = kotlin.math.min(p1.y, p2.y)
        val maxY = kotlin.math.max(p1.y, p2.y)

        fun containsX(x: Double) = x in minX..maxX
        fun containsY(y: Double) = y in minY..maxY

        //fun containsX(x: Double) = x >= minX && x < maxX
        //fun containsY(y: Double) = y >= minY && y < maxY

        fun orientation(p: Point): Int {
            val ix = intersectX(p.y)
            //println(ix)
            //if (ix.isNaN()) println("- h=$h, dx=$dx, dy=$dy")
            return ix.compareTo(p.x)
        }

        val isCoplanarX = p1.y == p2.y
        val isCoplanarY = p1.x == p2.x
        val dx = bx - ax
        val dy = by - ay
        val h = if (isCoplanarY) 0.0 else ay - (ax * dy) / dx

        fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * dx) / dy

        companion object {
            operator fun invoke(p1: Point, p2: Point): Edge {
                return when {
                    p1.y < p2.y -> Edge(p1, p2, true)
                    p1.y == p2.y -> if (p1.x < p2.x) Edge(p1, p2, true) else Edge(p2, p1, true)
                    else -> Edge(p2, p1, true)
                }
            }

            fun getIntersectY(a: Edge, b: Edge): Double {
                return getIntersectXY(a, b)?.y ?: Double.NaN
            }

            fun getIntersectX(a: Edge, b: Edge): Double {
                return getIntersectXY(a, b)?.x ?: Double.NaN
            }

            fun areParallel(a: Edge, b: Edge) = ((a.by - a.ay) * (b.ax - b.bx)) - ((b.by - b.ay) * (a.ax - a.bx)) == 0.0

            fun intersects(a: Edge, b: Edge): Boolean {
                if (a == b) return false
                val point = getIntersectXY(a, b) ?: return false
                if (a.isCoplanarX && !a.containsX(point.x)) return false
                if (b.isCoplanarX && !b.containsX(point.x)) return false
                //println(a.isCoplanarX)
                //println(b.isCoplanarX)
                val intersects = a.containsY(point.y) && b.containsY(point.y)
                if (!intersects) return false
                //if (a.hasPoint(point) && b.hasPoint(point)) return false
                if (point == a.p1 || point == a.p2 || point == b.p1 || point == b.p2) return false
                return true
            }

            fun getIntersectXY(a: Edge, b: Edge): Point? {
                return getIntersectXY(a.ax, a.ay, a.bx, a.by, b.ax, b.ay, b.bx, b.by)
            }

            fun getIntersectXY(
                Ax: Double,
                Ay: Double,
                Bx: Double,
                By: Double,
                Cx: Double,
                Cy: Double,
                Dx: Double,
                Dy: Double
            ): Point? {
                val a1 = By - Ay
                val b1 = Ax - Bx
                val c1 = a1 * (Ax) + b1 * (Ay)
                val a2 = Dy - Cy
                val b2 = Cx - Dx
                val c2 = a2 * (Cx) + b2 * (Cy)
                val determinant = a1 * b2 - a2 * b1
                if (determinant == 0.0) return null
                val x = (b2 * c1 - b1 * c2) / determinant
                val y = (a1 * c2 - a2 * c1) / determinant
                return Point(x.normalize(), y.normalize())
            }
        }
    }
}

fun VectorPath.triangulateSlow() = this.toPathList().triangulateSlow()

fun List<MPointArrayList>.triangulateSlow(): TriangleList {
    val builder = TriangulateSlow.EdgeBuilder()
    for (path in this) {
        path.fastForEachWithIndex { index, x, y ->
            if (index == 0) builder.moveTo(x, y) else builder.lineTo(x, y)
        }
        builder.close()
    }
    val triangles = builder.triangulate()
}
*/
