/*
Poly2Tri:Fast and Robust Simple Polygon triangulation with/without holes
                        by Sweep Line Algorithm
                                Liang, Wu
        http://www.mema.ucl.ac.be/~wu/Poly2Tri/poly2tri.html
        Copyright (C) 2003, 2004, 2005, ALL RIGHTS RESERVED.

---------------------------------------------------------------------
wu@mema.ucl.ac.be                           wuliang@femagsoft.com
Centre for Sys. Eng. & App. Mech.           FEMAGSoft S.A.
Universite Cathalique de Louvain            4, Avenue Albert Einstein
Batiment Euler, Avenue Georges Lemaitre, 4  B-1348 Louvain-la-Neuve
B-1348, Louvain-la-Neuve                    Belgium
Belgium
---------------------------------------------------------------------

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHAN-
TABILITY or FITNESS FOR A PARTICULAR PURPOSE.

This program may be freely redistributed under the condition that all
the copyright notices in all source files ( including the copyright
notice printed when the `-h' switch is selected) are not removed.Both
the binary and source codes may not be sold or included in any comme-
rcial products without a license from the corresponding author(s) &
entities.

1) Arbitrary precision floating-point arithmetic and fast robust geo-
    metric predicates (predicates.cc) is copyrighted by
    Jonathan Shewchuk (http://www.cs.berkeley.edu/~jrs) and you may get
    the source code from http://www.cs.cmu.edu/~quake/robust.html

2) The shell script mps2eps is copyrighted by Jon Edvardsson
    (http://www.ida.liu.se/~pelab/members/index.php4/?12) and you may
    get the copy from http://www.ida.liu.se/~joned/download/mps2eps/

3) All other source codes and exmaples files distributed in Poly2Tri
    are copyrighted by Liang, Wu (http://www.mema.ucl.ac.be/~wu) and
    FEMAGSoft S.A.
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package com.soywiz.korma.triangle.internal

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.triangle.*
import kotlin.math.*

internal class AdvancingFront(var head: Node, @Suppress("unused") var tail: Node) {
    var searchNode: Node = head

    /*fun findSearchNode(x) {
        return this.search_node;
    }*/

    fun locateNode(x: Double): Node? {
        var node: Node = this.searchNode

        if (x < node.value) {
            while (node.prev != null) {
                node = node.prev!!
                if (x >= node.value) {
                    this.searchNode = node
                    return node
                }
            }
        } else {
            while (node.next != null) {
                node = node.next!!
                if (x < node.value) {
                    this.searchNode = node.prev!!
                    return node.prev!!
                }
            }
        }
        return null
    }

    fun locatePoint(point: IPoint): Node? {
        val px: Double = point.x
        //var node:* = this.FindSearchNode(px);
        var node: Node? = this.searchNode
        val nx: Double = node!!.point.x

        when {
            px == nx -> {
                if (point != (node.point)) {
                    // We might have two nodes with same x value for a short time
                    node = when (point) {
                        (node.prev!!.point) -> node.prev
                        (node.next!!.point) -> node.next
                        else -> throw(Error("Invalid AdvancingFront.locatePoint call!"))
                    }
                }
            }
            px < nx -> {
                node = node.prev
                while (node != null) {
                    if (point == (node.point)) break
                    node = node.prev
                }
            }
            else -> {
                node = node.next
                while (node != null) {
                    if (point == (node.point)) break
                    node = node.next
                }
            }
        }

        if (node != null) this.searchNode = node
        return node
    }

}

internal class Basin {
    var leftNode: Node? = null
    var bottomNode: Node? = null
    var rightNode: Node? = null
    var width: Double = 0.0
    var leftHighest: Boolean = false

    @Suppress("unused")
    fun clear() {
        this.leftNode = null
        this.bottomNode = null
        this.rightNode = null
        this.width = 0.0
        this.leftHighest = false
    }
}

internal class EdgeEvent {
    var constrainedEdge: Edge? = null
    var right: Boolean = false
}

internal class Node(var point: IPoint, var triangle: PolyTriangle? = null) {
    var prev: Node? = null
    var next: Node? = null
    var value: Double = this.point.x

    /**
    *
    * @return the angle between 3 front nodes
    */
    val holeAngle: Double
        get() {
            /* Complex plane
            * ab = cosA +i*sinA
            * ab = (ax + ay*i)(bx + by*i) = (ax*bx + ay*by) + i(ax*by-ay*bx)
            * atan2(y,x) computes the principal value of the argument function
            * applied to the complex number x+iy
            * Where x = ax*bx + ay*by
            *       y = ax*by - ay*bx
            */
            val prev = this.prev ?: throw IllegalStateException("Not enough vertices")
            val next = this.next ?: throw IllegalStateException("Not enough vertices")
            val ax: Double = next.point.x - this.point.x
            val ay: Double = next.point.y - this.point.y
            val bx: Double = prev.point.x - this.point.x
            val by: Double = prev.point.y - this.point.y
            return atan2(
                ax * by - ay * bx,
                ax * bx + ay * by
            )
        }

    val basinAngle: Double
        get() {
            val nextNext = this.next?.next ?: throw IllegalStateException("Not enough vertices")
            return atan2(
                this.point.y - nextNext.point.y, // ay
                this.point.x - nextNext.point.x  // ax
            )
        }
}

internal class EdgeContext {
    val pointsToEdgeLists = hashMapOf<IPoint, ArrayList<Edge>>()
    fun getPointEdgeList(point: IPoint) = pointsToEdgeLists.getOrPut(point) { arrayListOf() }
    fun createEdge(p1: IPoint, p2: IPoint): Edge = Edge(p1, p2).also { getPointEdgeList(it.q).add(it) }
}

internal class Sweep(private var context: SweepContext) {
    val edgeContext get() = context.edgeContext
    /**
    * Triangulate simple polygon with holes.
    */
    fun triangulate() {
        context.initTriangulation()
        context.createAdvancingFront()
        sweepPoints()                    // Sweep points; build mesh
        finalizationPolygon()            // Clean up
    }

    fun sweepPoints() {
        for (i in 1 until this.context.points.size) {
            val point: IPoint = this.context.points.getPoint(i)
            val node: Node = this.pointEvent(point)
            val edgeList = edgeContext.getPointEdgeList(point)
            for (j in 0 until edgeList.size) {
                this.edgeEventByEdge(edgeList[j], node)
            }
        }
    }

    fun finalizationPolygon() {
        // Get an Internal triangle to start with
        val next = this.context.front.head.next!!
        var t: PolyTriangle = next.triangle!!
        val p: IPoint = next.point
        while (!t.getConstrainedEdgeCW(p)) t = t.neighborCCW(p)!!

        // Collect interior triangles constrained by edges
        this.context.meshClean(t)
    }

    /**
    * Find closes node to the left of the point and
    * create a triangle. If needed holes and basins
    * will be filled to.
    */
    fun pointEvent(point: IPoint): Node {
        val node = this.context.locateNode(point)!!
        val newNode = newFrontTriangle(point, node)

        // Only need to check +epsilon since point never have smaller
        // x value than node due to how we fetch nodes from the front
        if (point.x <= (node.point.x + Constants.EPSILON)) fill(node)

        //tcx.AddNode(new_node);

        fillAdvancingFront(newNode)
        return newNode
    }

    fun edgeEventByEdge(edge: Edge, node: Node) {
        val edgeEvent = this.context.edgeEvent
        edgeEvent.constrainedEdge = edge
        edgeEvent.right = (edge.p.x > edge.q.x)

        if (node.triangle!!.isEdgeSide(edge.p, edge.q)) return

        // For now we will do all needed filling
        // TODO: integrate with flip process might give some better performance
        //       but for now this avoid the issue with cases that needs both flips and fills
        this.fillEdgeEvent(edge, node)

        this.edgeEventByPoints(edge.p, edge.q, node.triangle!!, edge.q)
    }

    fun edgeEventByPoints(ep: IPoint, eq: IPoint, triangle: PolyTriangle, point: IPoint) {
        if (triangle.isEdgeSide(ep, eq)) return

        val p1: IPoint = triangle.pointCCW(point)
        val o1: Orientation = Orientation.orient2d(eq, p1, ep)
        if (o1 == Orientation.COLLINEAR) throw(Error("Sweep.edgeEvent: Collinear not supported!"))

        val p2: IPoint = triangle.pointCW(point)
        val o2: Orientation = Orientation.orient2d(eq, p2, ep)
        if (o2 == Orientation.COLLINEAR) throw(Error("Sweep.edgeEvent: Collinear not supported!"))

        if (o1 == o2) {
            // Need to decide if we are rotating CW or CCW to get to a triangle
            // that will cross edge
            edgeEventByPoints(
                ep,
                eq,
                if (o1 == Orientation.CLOCK_WISE) triangle.neighborCCW(point)!! else triangle.neighborCW(point)!!,
                point
            )
        } else {
            // This triangle crosses constraint so lets flippin start!
            flipEdgeEvent(ep, eq, triangle, point)
        }
    }

    fun newFrontTriangle(point: IPoint, node: Node): Node {
        val triangle = PolyTriangle(point, node.point, node.next!!.point)

        triangle.markNeighborTriangle(node.triangle!!)
        this.context.addToSet(triangle)

        val newNode = Node(point)
        newNode.next = node.next
        newNode.prev = node
        node.next!!.prev = newNode
        node.next = newNode

        if (!legalize(triangle)) this.context.mapTriangleToNodes(triangle)

        return newNode
    }

    /**
    * Adds a triangle to the advancing front to fill a hole.
    * @param node - middle node, that is the bottom of the hole
    */
    fun fill(node: Node) {
        val triangle = PolyTriangle(node.prev!!.point, node.point, node.next!!.point)

        // TODO: should copy the constrained_edge value from neighbor triangles
        //       for now constrained_edge values are copied during the legalize
        triangle.markNeighborTriangle(node.prev!!.triangle!!)
        triangle.markNeighborTriangle(node.triangle!!)

        this.context.addToSet(triangle)

        // Update the advancing front
        node.prev!!.next = node.next
        node.next!!.prev = node.prev

        // If it was legalized the triangle has already been mapped
        if (!legalize(triangle)) {
            this.context.mapTriangleToNodes(triangle)
        }

        this.context.removeNode(node)
    }

    /**
    * Fills holes in the Advancing Front
    */
    fun fillAdvancingFront(n: Node) {
        var node: Node
        var angle: Double

        // Fill right holes
        node = n.next!!
        while (node.next != null) {
            angle = node.holeAngle
            if ((angle > Constants.PI_2) || (angle < -Constants.PI_2)) break
            this.fill(node)
            node = node.next!!
        }

        // Fill left holes
        node = n.prev!!
        while (node.prev != null) {
            angle = node.holeAngle
            if ((angle > Constants.PI_2) || (angle < -Constants.PI_2)) break
            this.fill(node)
            node = node.prev!!
        }

        // Fill right basins
        if ((n.next != null) && (n.next!!.next != null)) {
            angle = n.basinAngle
            if (angle < Constants.PI_3div4) this.fillBasin(n)
        }
    }

    /**
    * Returns true if triangle was legalized
    */
    fun legalize(t: PolyTriangle): Boolean {
        // To legalize a triangle we start by finding if any of the three edges
        // violate the Delaunay condition
        for (i in 0 until 3) {
            if (t.delaunay_edge[i]) continue
            val ot: PolyTriangle = t.neighbors[i] ?: continue
            val p: IPoint = t.point(i)
            val op: IPoint = ot.oppositePoint(t, p)
            val oi: Int = ot.index(op)

            // If this is a Constrained Edge or a Delaunay Edge(only during recursive legalization)
            // then we should not try to legalize
            if (ot.constrained_edge[oi] || ot.delaunay_edge[oi]) {
                t.constrained_edge[i] = ot.constrained_edge[oi]
                continue
            }

            if (Triangle.insideIncircle(p, t.pointCCW(p), t.pointCW(p), op)) {
                // Lets mark this shared edge as Delaunay
                t.delaunay_edge[i] = true
                ot.delaunay_edge[oi] = true

                // Lets rotate shared edge one vertex CW to legalize it
                PolyTriangle.rotateTrianglePair(t, p, ot, op)

                // We now got one valid Delaunay Edge shared by two triangles
                // This gives us 4 edges to check for Delaunay

                // Make sure that triangle to node mapping is done only one time for a specific triangle
                if (!this.legalize(t)) this.context.mapTriangleToNodes(t)
                if (!this.legalize(ot)) this.context.mapTriangleToNodes(ot)

                // Reset the Delaunay edges, since they only are valid Delaunay edges
                // until we add a triangle or point.
                // XXX: need to think about this. Can these edges be tried after we
                //      return to previous recursive level?
                t.delaunay_edge[i] = false
                ot.delaunay_edge[oi] = false

                // If triangle have been legalized no need to check the other edges since
                // the recursive legalization will handles those so we can end here.
                return true
            }
        }
        return false
    }

    /**
    * Fills a basin that has formed on the Advancing Front to the right
    * of given node.<br>
    * First we decide a left,bottom and right node that forms the
    * boundaries of the basin. Then we do a reqursive fill.
    *
    * @param node - starting node, this or next node will be left node
    */
    fun fillBasin(node: Node) {
        val context = this.context
        val basin = context.basin
        basin.leftNode = if (Orientation.orient2d(
                node.point,
                node.next!!.point,
                node.next!!.next!!.point
            ) == Orientation.COUNTER_CLOCK_WISE
        ) node.next!!.next else node.next

        // Find the bottom and right node
        basin.bottomNode = basin.leftNode
        while ((basin.bottomNode!!.next != null) && (basin.bottomNode!!.point.y >= basin.bottomNode!!.next!!.point.y)) {
            basin.bottomNode = basin.bottomNode!!.next
        }

        // No valid basin
        if (basin.bottomNode == basin.leftNode) return

        basin.rightNode = basin.bottomNode
        while ((basin.rightNode!!.next != null) && (basin.rightNode!!.point.y < basin.rightNode!!.next!!.point.y)) {
            basin.rightNode = basin.rightNode!!.next
        }

        // No valid basins
        if (basin.rightNode == basin.bottomNode) return

        basin.width = (basin.rightNode!!.point.x - basin.leftNode!!.point.x)
        basin.leftHighest = (basin.leftNode!!.point.y > basin.rightNode!!.point.y)

        this.fillBasinReq(basin.bottomNode!!)
    }

    /**
    * Recursive algorithm to fill a Basin with triangles
    *
    * @param node - bottom_node
    */
    fun fillBasinReq(node: Node) {
        @Suppress("NAME_SHADOWING")
        var node = node
        // if shallow stop filling
        if (this.isShallow(node)) return

        this.fill(node)

        when {
            node.prev == this.context.basin.leftNode && node.next == this.context.basin.rightNode -> {
                return
            }
            node.prev == this.context.basin.leftNode -> {
                if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.CLOCK_WISE) {
                    return
                }
                node = node.next!!
            }
            node.next == this.context.basin.rightNode -> {
                if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.COUNTER_CLOCK_WISE) {
                    return
                }
                node = node.prev!!
            }
            else -> {
                // Continue with the neighbor node with lowest Y value
                node = if (node.prev!!.point.y < node.next!!.point.y) node.prev!! else node.next!!
            }
        }

        this.fillBasinReq(node)
    }

    fun isShallow(node: Node): Boolean {
        val height: Double = if (this.context.basin.leftHighest) {
            this.context.basin.leftNode!!.point.y - node.point.y
        } else {
            this.context.basin.rightNode!!.point.y - node.point.y
        }

        // if shallow stop filling
        return (this.context.basin.width > height)
    }

    fun fillEdgeEvent(edge: Edge, node: Node) {
        if (this.context.edgeEvent.right) {
            this.fillRightAboveEdgeEvent(edge, node)
        } else {
            this.fillLeftAboveEdgeEvent(edge, node)
        }
    }

    fun fillRightAboveEdgeEvent(edge: Edge, node: Node) {
        var n = node
        while (n.next!!.point.x < edge.p.x) {
            // Check if next node is below the edge
            if (Orientation.orient2d(edge.q, n.next!!.point, edge.p) == Orientation.COUNTER_CLOCK_WISE) {
                this.fillRightBelowEdgeEvent(edge, n)
            } else {
                n = n.next!!
            }
        }
    }

    fun fillRightBelowEdgeEvent(edge: Edge, node: Node) {
        if (node.point.x >= edge.p.x) return
        if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.COUNTER_CLOCK_WISE) {
            // Concave
            this.fillRightConcaveEdgeEvent(edge, node)
        } else {
            this.fillRightConvexEdgeEvent(edge, node) // Convex
            this.fillRightBelowEdgeEvent(edge, node) // Retry this one
        }
    }

    fun fillRightConcaveEdgeEvent(edge: Edge, node: Node) {
        this.fill(node.next!!)
        if (node.next!!.point != edge.p) {
            // Next above or below edge?
            if (Orientation.orient2d(edge.q, node.next!!.point, edge.p) == Orientation.COUNTER_CLOCK_WISE) {
                // Below
                if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.COUNTER_CLOCK_WISE) {
                    // Next is concave
                    this.fillRightConcaveEdgeEvent(edge, node)
                } else {
                    // Next is convex
                }
            }
        }
    }

    fun fillRightConvexEdgeEvent(edge: Edge, node: Node) {
        // Next concave or convex?
        if (Orientation.orient2d(
                node.next!!.point,
                node.next!!.next!!.point,
                node.next!!.next!!.next!!.point
            ) == Orientation.COUNTER_CLOCK_WISE
        ) {
            // Concave
            this.fillRightConcaveEdgeEvent(edge, node.next!!)
        } else {
            // Convex
            // Next above or below edge?
            if (Orientation.orient2d(edge.q, node.next!!.next!!.point, edge.p) == Orientation.COUNTER_CLOCK_WISE) {
                // Below
                this.fillRightConvexEdgeEvent(edge, node.next!!)
            } else {
                // Above
            }
        }
    }

    fun fillLeftAboveEdgeEvent(edge: Edge, node: Node) {
        var n = node
        while (n.prev!!.point.x > edge.p.x) {
            // Check if next node is below the edge
            if (Orientation.orient2d(edge.q, n.prev!!.point, edge.p) == Orientation.CLOCK_WISE) {
                this.fillLeftBelowEdgeEvent(edge, n)
            } else {
                n = n.prev!!
            }
        }
    }

    fun fillLeftBelowEdgeEvent(edge: Edge, node: Node) {
        if (node.point.x > edge.p.x) {
            if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.CLOCK_WISE) {
                // Concave
                this.fillLeftConcaveEdgeEvent(edge, node)
            } else {
                // Convex
                this.fillLeftConvexEdgeEvent(edge, node)
                // Retry this one
                this.fillLeftBelowEdgeEvent(edge, node)
            }
        }
    }

    fun fillLeftConvexEdgeEvent(edge: Edge, node: Node) {
        // Next concave or convex?
        if (Orientation.orient2d(
                node.prev!!.point,
                node.prev!!.prev!!.point,
                node.prev!!.prev!!.prev!!.point
            ) == Orientation.CLOCK_WISE
        ) {
            // Concave
            this.fillLeftConcaveEdgeEvent(edge, node.prev!!)
        } else {
            // Convex
            // Next above or below edge?
            if (Orientation.orient2d(edge.q, node.prev!!.prev!!.point, edge.p) == Orientation.CLOCK_WISE) {
                // Below
                this.fillLeftConvexEdgeEvent(edge, node.prev!!)
            } else {
                // Above
            }
        }
    }

    fun fillLeftConcaveEdgeEvent(edge: Edge, node: Node) {
        this.fill(node.prev!!)
        if (node.prev!!.point != edge.p) {
            // Next above or below edge?
            if (Orientation.orient2d(edge.q, node.prev!!.point, edge.p) == Orientation.CLOCK_WISE) {
                // Below
                if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.CLOCK_WISE) {
                    // Next is concave
                    this.fillLeftConcaveEdgeEvent(edge, node)
                } else {
                    // Next is convex
                }
            }
        }
    }

    fun flipEdgeEvent(ep: IPoint, eq: IPoint, t: PolyTriangle, p: IPoint) {
        var tt = t
        val ot: PolyTriangle = tt.neighborAcross(p) ?: throw Error("[BUG:FIXME] FLIP failed due to missing triangle!")
        // If we want to integrate the fillEdgeEvent do it here
        // With current implementation we should never get here

        val op: IPoint = ot.oppositePoint(tt, p)

        if (Triangle.inScanArea(p, tt.pointCCW(p), tt.pointCW(p), op)) {
            // Lets rotate shared edge one vertex CW
            PolyTriangle.rotateTrianglePair(tt, p, ot, op)
            this.context.mapTriangleToNodes(tt)
            this.context.mapTriangleToNodes(ot)

            // @TODO: equals?
            if ((p == eq) && (op == ep)) {
                if ((eq == this.context.edgeEvent.constrainedEdge!!.q) && (ep == this.context.edgeEvent.constrainedEdge!!.p)) {
                    tt.markConstrainedEdgeByPoints(ep, eq)
                    ot.markConstrainedEdgeByPoints(ep, eq)
                    this.legalize(tt)
                    this.legalize(ot)
                } else {
                    // XXX: I think one of the triangles should be legalized here?
                }
            } else {
                val o: Orientation = Orientation.orient2d(eq, op, ep)
                tt = this.nextFlipTriangle(o, tt, ot, p, op)
                this.flipEdgeEvent(ep, eq, tt, p)
            }
        } else {
            val newP: IPoint = nextFlipPoint(ep, eq, ot, op)
            this.flipScanEdgeEvent(ep, eq, tt, ot, newP)
            this.edgeEventByPoints(ep, eq, tt, p)
        }
    }

    fun nextFlipTriangle(o: Orientation, t: PolyTriangle, ot: PolyTriangle, p: IPoint, op: IPoint): PolyTriangle {
        val tt = if (o == Orientation.COUNTER_CLOCK_WISE) ot else t
        // ot is not crossing edge after flip
        tt.delaunay_edge[tt.edgeIndex(p, op)] = true
        this.legalize(tt)
        tt.clearDelunayEdges()
        return if (o == Orientation.COUNTER_CLOCK_WISE) t else ot
    }

    companion object {
        fun nextFlipPoint(ep: IPoint, eq: IPoint, ot: Triangle, op: IPoint): IPoint {
            return when (Orientation.orient2d(eq, op, ep)) {
                Orientation.CLOCK_WISE -> ot.pointCCW(op) // Right
                Orientation.COUNTER_CLOCK_WISE -> ot.pointCW(op) // Left
                else -> throw Error("[Unsupported] Sweep.NextFlipPoint: opposing point on constrained edge!")
            }
        }
    }

    fun flipScanEdgeEvent(ep: IPoint, eq: IPoint, flip_triangle: Triangle, t: PolyTriangle, p: IPoint) {
        val ot = t.neighborAcross(p)
            ?: throw Error("[BUG:FIXME] FLIP failed due to missing triangle") // If we want to integrate the fillEdgeEvent do it here With current implementation we should never get here

        val op = ot.oppositePoint(t, p)

        if (Triangle.inScanArea(eq, flip_triangle.pointCCW(eq), flip_triangle.pointCW(eq), op)) {
            // flip with edge op.eq
            this.flipEdgeEvent(eq, op, ot, op)
            // TODO: Actually I just figured out that it should be possible to
            //       improve this by getting the next ot and op before the the above
            //       flip and continue the flipScanEdgeEvent here
            // set ot and op here and loop back to inScanArea test
            // also need to set a flip_triangle first
            // Turns out at first glance that this is somewhat complicated
            // so it will have to wait.
        } else {
            val newP: IPoint = nextFlipPoint(ep, eq, ot, op)
            this.flipScanEdgeEvent(ep, eq, flip_triangle, ot, newP)
        }
    }
}

internal class SweepContext() {
    var triangles: ArrayList<PolyTriangle> = ArrayList()
    var points: PointArrayList = PointArrayList()
    var edgeList: ArrayList<Edge> = ArrayList()
    val edgeContext = EdgeContext()

    val set = LinkedHashSet<PolyTriangle>()

    lateinit var front: AdvancingFront
    lateinit var head: IPoint
    lateinit var tail: IPoint

    val basin: Basin = Basin()
    var edgeEvent = EdgeEvent()

    constructor(polyline: List<IPoint>) : this() {
        this.addPolyline(polyline)
    }

    private fun addPoints(points: List<IPoint>) {
        for (point in points) this.points.add(point)
    }

    fun addPolyline(polyline: List<IPoint>) {
        this.initEdges(polyline)
        this.addPoints(polyline)
    }

    /**
    * An alias of addPolyline.
    *
    * @param    polyline
    */
    fun addHole(polyline: List<IPoint>) {
        addPolyline(polyline)
    }

    private fun initEdges(polyline: List<IPoint>) {
        for (n in 0 until polyline.size) {
            this.edgeList.add(edgeContext.createEdge(polyline[n], polyline[(n + 1) % polyline.size]))
        }
    }

    fun addToSet(triangle: PolyTriangle) {
        this.set += triangle
    }

    companion object {
        /*
        * Inital triangle factor, seed triangle will extend 30% of
        * PointSet width to both left and right.
        */
        private const val kAlpha: Double = 0.3
    }

    fun initTriangulation() {
        var xmin: Double = this.points.getX(0)
        var xmax: Double = this.points.getX(0)
        var ymin: Double = this.points.getY(0)
        var ymax: Double = this.points.getY(0)

        // Calculate bounds
        for (n in 0 until this.points.size) {
            val px = this.points.getX(n)
            val py = this.points.getY(n)
            if (px > xmax) xmax = px
            if (px < xmin) xmin = px
            if (py > ymax) ymax = py
            if (py < ymin) ymin = py
        }

        val dx: Double = kAlpha * (xmax - xmin)
        val dy: Double = kAlpha * (ymax - ymin)
        this.head = IPoint(xmax + dx, ymin - dy)
        this.tail = IPoint(xmin - dy, ymin - dy)

        // Sort points along y-axis
        this.points.sort()
        //throw(Error("@TODO Implement 'Sort points along y-axis' @see class SweepContext"));
    }

    fun locateNode(point: IPoint): Node? = this.front.locateNode(point.x)

    fun createAdvancingFront() {
        // Initial triangle
        val triangle = PolyTriangle(this.points.getPoint(0), this.tail, this.head)

        addToSet(triangle)

        val head = Node(triangle.p1, triangle)
        val middle = Node(triangle.p0, triangle)
        val tail = Node(triangle.p2)

        this.front = AdvancingFront(head, tail)

        head.next = middle
        middle.next = tail
        middle.prev = head
        tail.prev = middle
    }

    fun removeNode(@Suppress("UNUSED_PARAMETER") node: Node) {
        // do nothing
    }

    fun mapTriangleToNodes(triangle: PolyTriangle) {
        for (n in 0 until 3) {
            if (triangle.neighbors[n] == null) {
                val neighbor: Node? = this.front.locatePoint(triangle.pointCW(triangle.point(n)))
                if (neighbor != null) neighbor.triangle = triangle
            }
        }
    }

    @Suppress("unused")
    fun removeFromMap(triangle: PolyTriangle) {
        this.set -= triangle
    }

    fun meshClean(triangle: PolyTriangle?, level: Int = 0) {
        if (level == 0) {
            //for each (var mappedTriangle:Triangle in this.map) println(mappedTriangle);
        }
        if (triangle == null || triangle.interior) return
        triangle.interior = true
        this.triangles.add(triangle)
        for (n in 0 until 3) {
            if (!triangle.constrained_edge[n]) {
                this.meshClean(triangle.neighbors[n], level + 1)
            }
        }
    }
}

/**
 * Return the point clockwise to the given point.
 * Return the point counter-clockwise to the given point.
 *
 * Return the neighbor clockwise to given point.
 * Return the neighbor counter-clockwise to given point.
 */

//private const CCW_OFFSET:Int = +1;
//private const CW_OFFSET:Int = -1;

internal data class PolyTriangle internal constructor(
    val dummy: Boolean,
    override var p0: IPoint,
    override var p1: IPoint,
    override var p2: IPoint
) : Triangle {
    val neighbors: FastArrayList<PolyTriangle?> = FastArrayList<PolyTriangle?>(3).apply { add(null); add(null); add(null) } // Neighbor list
    var interior: Boolean = false // Has this triangle been marked as an interior triangle?
    val constrained_edge = BooleanArray(3) // Flags to determine if an edge is a Constrained edge
    val delaunay_edge = BooleanArray(3) // Flags to determine if an edge is a Delauney edge
    
    fun neighborCW(p: IPoint): PolyTriangle? = this.neighbors[getPointIndexOffset(p, CW_OFFSET)]
    fun neighborCCW(p: IPoint): PolyTriangle? = this.neighbors[getPointIndexOffset(p, CCW_OFFSET)]

    fun getConstrainedEdgeCW(p: IPoint): Boolean = this.constrained_edge[getPointIndexOffset(p, CW_OFFSET)]
    fun setConstrainedEdgeCW(p: IPoint, ce: Boolean): Boolean =
        ce.also { this.constrained_edge[getPointIndexOffset(p, CW_OFFSET)] = ce }

    fun getConstrainedEdgeCCW(p: IPoint): Boolean = this.constrained_edge[getPointIndexOffset(p, CCW_OFFSET)]
    fun setConstrainedEdgeCCW(p: IPoint, ce: Boolean): Boolean =
        ce.also { this.constrained_edge[getPointIndexOffset(p, CCW_OFFSET)] = ce }

    fun getDelaunayEdgeCW(p: IPoint): Boolean = this.delaunay_edge[getPointIndexOffset(p, CW_OFFSET)]
    fun setDelaunayEdgeCW(p: IPoint, e: Boolean): Boolean =
        e.also { this.delaunay_edge[getPointIndexOffset(p, CW_OFFSET)] = e }

    fun getDelaunayEdgeCCW(p: IPoint): Boolean = this.delaunay_edge[getPointIndexOffset(p, CCW_OFFSET)]
    fun setDelaunayEdgeCCW(p: IPoint, e: Boolean): Boolean =
        e.also { this.delaunay_edge[getPointIndexOffset(p, CCW_OFFSET)] = e }

    fun clearNeigbors() {
        this.neighbors[0] = null
        this.neighbors[1] = null
        this.neighbors[2] = null
    }

    fun clearDelunayEdges() {
        this.delaunay_edge[0] = false
        this.delaunay_edge[1] = false
        this.delaunay_edge[2] = false
    }

    /**
    * Legalize triangle by rotating clockwise.<br>
    * This method takes either 1 parameter (then the triangle is rotated around
    * points(0)) or 2 parameters (then the triangle is rotated around the first
    * parameter).
    */
    fun legalize(opoint: IPoint, npoint: IPoint? = null) {
        if (npoint == null) return this.legalize(this.point(0), opoint)

        when (opoint) {
            this.point(0) -> {
                this.p1 = this.point(0)
                this.p0 = this.point(2)
                this.p2 = npoint
            }
            this.point(1) -> {
                this.p2 = this.point(1)
                this.p1 = this.point(0)
                this.p0 = npoint
            }
            this.point(2) -> {
                this.p0 = this.point(2)
                this.p2 = this.point(1)
                this.p1 = npoint
            }
            else -> throw kotlin.Error("Invalid js.poly2tri.Triangle.Legalize call!")
        }
    }

    /**
    * Update neighbor pointers.<br>
    * This method takes either 3 parameters (<code>p1</code>, <code>p2</code> and
    * <code>t</code>) or 1 parameter (<code>t</code>).
    * @param   t   Triangle object.
    * @param   p1  Point2d object.
    * @param   p2  Point2d object.
    */
    fun markNeighbor(t: PolyTriangle, p1: IPoint, p2: IPoint) {
        if ((p1 == (this.point(2)) && p2 == (this.point(1))) || (p1 == (this.point(1)) && p2 == (this.point(2)))) {
            this.neighbors[0] = t
            return
        }
        if ((p1 == (this.point(0)) && p2 == (this.point(2))) || (p1 == (this.point(2)) && p2 == (this.point(0)))) {
            this.neighbors[1] = t
            return
        }
        if ((p1 == (this.point(0)) && p2 == (this.point(1))) || (p1 == (this.point(1)) && p2 == (this.point(0)))) {
            this.neighbors[2] = t
            return
        }
        throw kotlin.Error("Invalid markNeighbor call (1)!")
    }

    fun markNeighborTriangle(that: PolyTriangle) {
        // exhaustive search to update neighbor pointers
        if (that.containsEdgePoints(this.point(1), this.point(2))) {
            this.neighbors[0] = that
            that.markNeighbor(this, this.point(1), this.point(2))
            return
        }

        if (that.containsEdgePoints(this.point(0), this.point(2))) {
            this.neighbors[1] = that
            that.markNeighbor(this, this.point(0), this.point(2))
            return
        }

        if (that.containsEdgePoints(this.point(0), this.point(1))) {
            this.neighbors[2] = that
            that.markNeighbor(this, this.point(0), this.point(1))
            return
        }
    }


    /**
    * Mark an edge of this triangle as constrained.<br>
    * This method takes either 1 parameter (an edge index or an Edge instance) or
    * 2 parameters (two Point2d instances defining the edge of the triangle).
    */
    fun markConstrainedEdgeByIndex(index: Int): Unit = run { this.constrained_edge[index] = true }

    fun markConstrainedEdgeByEdge(edge: Edge): Unit = this.markConstrainedEdgeByPoints(edge.p, edge.q)

    fun markConstrainedEdgeByPoints(p: IPoint, q: IPoint) {
        if ((q == (this.point(0)) && p == (this.point(1))) || (q == (this.point(1)) && p == (this.point(0)))) {
            this.constrained_edge[2] = true
        } else if ((q == (this.point(0)) && p == (this.point(2))) || (q == (this.point(2)) && p == (this.point(0)))) {
            this.constrained_edge[1] = true
        } else if ((q == (this.point(1)) && p == (this.point(2))) || (q == (this.point(2)) && p == (this.point(1)))) {
            this.constrained_edge[0] = true
        }
    }

// isEdgeSide
    /**
    * Checks if a side from this triangle is an edge side.
    * If sides are not marked they will be marked.
    *
    * @param    ep
    * @param    eq
    * @return
    */
    fun isEdgeSide(ep: IPoint, eq: IPoint): Boolean {
        val index = this.edgeIndex(ep, eq)
        if (index == -1) return false
        this.markConstrainedEdgeByIndex(index)
        this.neighbors[index]?.markConstrainedEdgeByPoints(ep, eq)
        return true
    }
    /**
    * The neighbor across to given point.
    */
    fun neighborAcross(p: IPoint): PolyTriangle? = this.neighbors[getPointIndexOffset(p, 0)]

    override fun hashCode(): Int = p0.hashCode() + p1.hashCode() * 3 + p2.hashCode() * 5

    override fun equals(other: Any?): Boolean =
        (other is PolyTriangle) && (this.p0 == other.p0) && (this.p1 == other.p1) && (this.p2 == other.p2)

    override fun toString(): String = "Triangle(${this.point(0)}, ${this.point(1)}, ${this.point(2)})"

    companion object {
        internal const val CW_OFFSET: Int = +1
        internal const val CCW_OFFSET: Int = -1

        /**
        * Rotates a triangle pair one vertex CW
        *<pre>
        *       n2                    n2
        *  P +-----+             P +-----+
        *    | t  /|               |\  t |
        *    |   / |               | \   |
        *  n1|  /  |n3           n1|  \  |n3
        *    | /   |    after CW   |   \ |
        *    |/ oT |               | oT \|
        *    +-----+ oP            +-----+
        *       n4                    n4
        * </pre>
        */
        fun rotateTrianglePair(t: PolyTriangle, p: IPoint, ot: PolyTriangle, op: IPoint) {
            val n1 = t.neighborCCW(p)
            val n2 = t.neighborCW(p)
            val n3 = ot.neighborCCW(op)
            val n4 = ot.neighborCW(op)

            val ce1 = t.getConstrainedEdgeCCW(p)
            val ce2 = t.getConstrainedEdgeCW(p)
            val ce3 = ot.getConstrainedEdgeCCW(op)
            val ce4 = ot.getConstrainedEdgeCW(op)

            val de1 = t.getDelaunayEdgeCCW(p)
            val de2 = t.getDelaunayEdgeCW(p)
            val de3 = ot.getDelaunayEdgeCCW(op)
            val de4 = ot.getDelaunayEdgeCW(op)

            t.legalize(p, op)
            ot.legalize(op, p)

            // Remap delaunay_edge
            ot.setDelaunayEdgeCCW(p, de1)
            t.setDelaunayEdgeCW(p, de2)
            t.setDelaunayEdgeCCW(op, de3)
            ot.setDelaunayEdgeCW(op, de4)

            // Remap constrained_edge
            ot.setConstrainedEdgeCCW(p, ce1)
            t.setConstrainedEdgeCW(p, ce2)
            t.setConstrainedEdgeCCW(op, ce3)
            ot.setConstrainedEdgeCW(op, ce4)

            // Remap neighbors
            // XXX: might optimize the markNeighbor by keeping track of
            //      what side should be assigned to what neighbor after the
            //      rotation. Now mark neighbor does lots of testing to find
            //      the right side.
            t.clearNeigbors()
            ot.clearNeigbors()
            if (n1 != null) ot.markNeighborTriangle(n1)
            if (n2 != null) t.markNeighborTriangle(n2)
            if (n3 != null) t.markNeighborTriangle(n3)
            if (n4 != null) ot.markNeighborTriangle(n4)
            t.markNeighborTriangle(ot)
        }

    }
}

internal fun PolyTriangle(p0: IPoint, p1: IPoint, p2: IPoint, fixOrientation: Boolean = false, checkOrientation: Boolean = true): PolyTriangle {
    @Suppress("NAME_SHADOWING")
    var p1 = p1
    @Suppress("NAME_SHADOWING")
    var p2 = p2
    if (fixOrientation) {
        if (Orientation.orient2d(p0, p1, p2) == Orientation.CLOCK_WISE) {
            val pt = p2
            p2 = p1
            p1 = pt
            //println("Fixed orientation");
        }
    }
    if (checkOrientation && Orientation.orient2d(p2, p1, p0) != Orientation.CLOCK_WISE) throw(Error("Triangle must defined with Orientation.CW"))
    return PolyTriangle(true, p0, p1, p2)
}
