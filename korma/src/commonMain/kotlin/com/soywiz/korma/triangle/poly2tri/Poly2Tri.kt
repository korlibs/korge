// Port from .JS: https://github.com/r3mi/poly2tri.js/

// Bundled type definitions for poly2tri.js
// Project: http://github.com/r3mi/poly2tri.js/
// Definitions by: Elemar Junior <https://github.com/elemarjr/>
// Updated by: Rémi Turboult <https://github.com/r3mi>
// TypeScript Version: 2.0

/*
 * Poly2Tri Copyright (c) 2009-2014, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * poly2tri.js (JavaScript port) (c) 2009-2017, Poly2Tri Contributors
 * https://github.com/r3mi/poly2tri.js
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:Suppress("LiftReturnOrAssignment", "CanBeVal", "CascadeIf", "DEPRECATED_IDENTITY_EQUALS", "PropertyName",
    "unused", "UNUSED_PARAMETER", "NAME_SHADOWING", "MemberVisibilityCanBePrivate"
)

package com.soywiz.korma.triangle.poly2tri

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.SortOps
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.genericSort
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.swap
import com.soywiz.korma.geom.*
import kotlin.jvm.JvmName
import kotlin.math.abs

object Poly2Tri {

    /**
     * Triangulate the polygon with holes and Steiner points.
     * Do this AFTER you've added the polyline, holes, and Steiner points
     * @private
     * @param {!SweepContext} tcx - SweepContext object
     */
    fun triangulate(tcx: SweepContext) {
        tcx.initTriangulation()
        tcx.createAdvancingFront()
        // Sweep points; build mesh
        sweepPoints(tcx)
        // Clean up
        finalizationPolygon(tcx)
    }

    /**
     * Start sweeping the Y-sorted point set from bottom to top
     * @param {!SweepContext} tcx - SweepContext object
     */
    fun sweepPoints(tcx: SweepContext) {
        var len = tcx.pointCount()
        for (i in 1 until len) {
            var point = tcx.getPoint(i)
            var node = pointEvent(tcx, point)
            var edges = tcx._p2t_edge_lists.getOrNull(i)
            var j = 0
            while (edges != null && j < edges.size) {
                edgeEventByEdge(tcx, edges[j], node)
                ++j
            }
        }
    }

    /**
     * @param {!SweepContext} tcx - SweepContext object
     */
    fun finalizationPolygon(tcx: SweepContext) {
        // Get an Internal triangle to start with
        var t = tcx.front_!!.head.next!!.triangle!!
        var p = tcx.front_!!.head.next!!.point
        while (!t.getConstrainedEdgeCW(p)) {
            t = t.neighborCCW(p)
                ?: error("Can't find neighborCCW($p) in $t")
        }

        // Collect interior triangles constrained by edges
        tcx.meshClean(t)
    }

    /**
     * Find closes node to the left of the new point and
     * create a new triangle. If needed new holes and basins
     * will be filled to.
     * @param {!SweepContext} tcx - SweepContext object
     * @param {!XY} point   Point
     */
    fun pointEvent(tcx: SweepContext, point: MPoint): Node {
        var node = tcx.locateNode(point)!!
        var new_node = newFrontTriangle(tcx, point, node)

        // Only need to check +epsilon since point never have smaller
        // x value than node due to how we fetch nodes from the front
        if (point.x <= node.point.x + (POLY2TRI_EPSILON)) {
            fill(tcx, node)
        }

        //tcx.AddNode(new_node);

        fillAdvancingFront(tcx, new_node)
        return new_node
    }

    fun edgeEventByEdge(tcx: SweepContext, edge: Edge, node: Node) {
        tcx.edge_event.constrained_edge = edge
        tcx.edge_event.right = (edge.p.x > edge.q.x)

        if (isEdgeSideOfTriangle(node.triangle!!, edge.p, edge.q)) {
            return
        }

        // For now we will do all needed filling
        // TODO: integrate with flip process might give some better performance
        //       but for now this avoid the issue with cases that needs both flips and fills
        fillEdgeEvent(tcx, edge, node)
        edgeEventByPoints(tcx, edge.p, edge.q, node.triangle!!, edge.q)
    }

    fun edgeEventByPoints(tcx: SweepContext, ep: MPoint, eq: MPoint, triangle: Triangle, point: MPoint) {
        if (isEdgeSideOfTriangle(triangle, ep, eq)) {
            return
        }

        var triangle = triangle
        var p1 = triangle.pointCCW(point)!!
        var o1 = orient2d(eq, p1, ep)
        //println("$o1: $eq, $p1, $ep")
        if (o1 === Orientation.COLLINEAR) {
            // TODO integrate here changes from C++ version
            // (C++ repo revision 09880a869095 dated March 8, 2011)
            throw PointError("poly2tri EdgeEvent: Collinear not supported!", listOf(eq, p1, ep))
        }

        var p2 = triangle.pointCW(point)!!
        var o2 = orient2d(eq, p2, ep)
        if (o2 === Orientation.COLLINEAR) {
            // TODO integrate here changes from C++ version
            // (C++ repo revision 09880a869095 dated March 8, 2011)
            throw PointError("poly2tri EdgeEvent: Collinear not supported!", listOf(eq, p2, ep))
        }

        if (o1 === o2) {
            // Need to decide if we are rotating CW or CCW to get to a triangle
            // that will cross edge
            if (o1 === Orientation.CW) {
                triangle = triangle.neighborCCW(point)!!
            } else {
                triangle = triangle.neighborCW(point)!!
            }
            edgeEventByPoints(tcx, ep, eq, triangle, point)
        } else {
            // This triangle crosses constraint so lets flippin start!
            flipEdgeEvent(tcx, ep, eq, triangle, point)
        }
    }

    fun isEdgeSideOfTriangle(triangle: Triangle, ep: MPoint, eq: MPoint): Boolean {
        var index = triangle.edgeIndex(ep, eq)
        if (index != -1) {
            triangle.markConstrainedEdgeByIndex(index)
            var t = triangle.getNeighbor(index)
            if (t != null) {
                t.markConstrainedEdgeByPoints(ep, eq)
            }
            return true
        }
        return false
    }

    /**
     * Creates a new front triangle and legalize it
     * @param {!SweepContext} tcx - SweepContext object
     */
    fun newFrontTriangle(tcx: SweepContext, point: MPoint, node: Node): Node {
        var triangle = Triangle(point, node.point, node.next!!.point)

        triangle.markNeighbor(node.triangle!!)
        tcx.addToMap(triangle)

        var new_node = Node(point)
        new_node.next = node.next
        new_node.prev = node
        node.next!!.prev = new_node
        node.next = new_node

        if (!legalize(tcx, triangle)) {
            tcx.mapTriangleToNodes(triangle)
        }

        return new_node
    }

    /**
     * Adds a triangle to the advancing front to fill a hole.
     * @param {!SweepContext} tcx - SweepContext object
     * @param node - middle node, that is the bottom of the hole
     */
    fun fill(tcx: SweepContext, node: Node) {
        var triangle = Triangle(node.prev!!.point, node.point, node.next!!.point)

        // TODO: should copy the constrained_edge value from neighbor triangles
        //       for now constrained_edge values are copied during the legalize
        triangle.markNeighbor(node.prev!!.triangle!!)
        triangle.markNeighbor(node.triangle!!)

        tcx.addToMap(triangle)

        // Update the advancing front
        node.prev!!.next = node.next
        node.next!!.prev = node.prev


        // If it was legalized the triangle has already been mapped
        if (!legalize(tcx, triangle)) {
            tcx.mapTriangleToNodes(triangle)
        }

        //tcx.removeNode(node);
    }

    /**
     * Fills holes in the Advancing Front
     * @param {!SweepContext} tcx - SweepContext object
     */
    fun fillAdvancingFront(tcx: SweepContext, n: Node) {
        // Fill right holes
        var node: Node? = n.next
        while (node!!.next != null) {
            // TODO integrate here changes from C++ version
            // (C++ repo revision acf81f1f1764 dated April 7, 2012)
            if (isAngleObtuse(node.point, node.next!!.point, node.prev!!.point)) {
                break
            }
            fill(tcx, node)
            node = node.next
        }

        // Fill left holes
        node = n.prev
        while (node!!.prev != null) {
            // TODO integrate here changes from C++ version
            // (C++ repo revision acf81f1f1764 dated April 7, 2012)
            if (isAngleObtuse(node.point, node.next!!.point, node.prev!!.point)) {
                break
            }
            fill(tcx, node)
            node = node.prev
        }

        // Fill right basins
        if (n.next != null && n.next!!.next != null) {
            if (isBasinAngleRight(n)) {
                fillBasin(tcx, n)
            }
        }
    }

    /**
     * The basin angle is decided against the horizontal line [1,0].
     * @param {Node} node
     * @return {boolean} true if angle < 3*π/4
     */
    fun isBasinAngleRight(node: Node): Boolean {
        var ax = node.point.x - node.next!!.next!!.point.x
        var ay = node.point.y - node.next!!.next!!.point.y
        check(ay >= 0) { "unordered y" }
        return (ax >= 0 || abs(ax) < ay)
    }

    /**
     * Returns true if triangle was legalized
     * @param {!SweepContext} tcx - SweepContext object
     * @return {boolean}
     */
    fun legalize(tcx: SweepContext, t: Triangle): Boolean {
        // To legalize a triangle we start by finding if any of the three edges
        // violate the Delaunay condition
        for (i in 0 until 3) {
            if (t.delaunay_edge[i]) {
                continue
            }
            var ot = t.getNeighbor(i)
            if (ot != null) {
                var p = t.getPoint(i)
                var op = ot.oppositePoint(t, p)!!
                var oi = ot.index(op)

                // If this is a Constrained Edge or a Delaunay Edge(only during recursive legalization)
                // then we should not try to legalize
                if (ot.constrained_edge[oi] || ot.delaunay_edge[oi]) {
                    t.constrained_edge[i] = ot.constrained_edge[oi]
                    continue
                }

                var inside = inCircle(p, t.pointCCW(p)!!, t.pointCW(p)!!, op)
                if (inside) {
                    // Lets mark this shared edge as Delaunay
                    t.delaunay_edge[i] = true
                    ot.delaunay_edge[oi] = true

                    // Lets rotate shared edge one vertex CW to legalize it
                    rotateTrianglePair(t, p, ot, op)

                    // We now got one valid Delaunay Edge shared by two triangles
                    // This gives us 4 new edges to check for Delaunay

                    // Make sure that triangle to node mapping is done only one time for a specific triangle
                    var not_legalized = !legalize(tcx, t)
                    if (not_legalized) {
                        tcx.mapTriangleToNodes(t)
                    }

                    not_legalized = !legalize(tcx, ot)
                    if (not_legalized) {
                        tcx.mapTriangleToNodes(ot)
                    }
                    // Reset the Delaunay edges, since they only are valid Delaunay edges
                    // until we add a new triangle or point.
                    // XXX: need to think about this. Can these edges be tried after we
                    //      return to previous recursive level?
                    t.delaunay_edge[i] = false
                    ot.delaunay_edge[oi] = false

                    // If triangle have been legalized no need to check the other edges since
                    // the recursive legalization will handles those so we can end here.
                    return true
                }
            }
        }
        return false
    }

    /**
     * <b>Requirement</b>:<br>
     * 1. a,b and c form a triangle.<br>
     * 2. a and d is know to be on opposite side of bc<br>
     * <pre>
     *                a
     *                +
     *               / \
     *              /   \
     *            b/     \c
     *            +-------+
     *           /    d    \
     *          /           \
     * </pre>
     * <b>Fact</b>: d has to be in area B to have a chance to be inside the circle formed by
     *  a,b and c<br>
     *  d is outside B if orient2d(a,b,d) or orient2d(c,a,d) is CW<br>
     *  This preknowledge gives us a way to optimize the incircle test
     * @param pa - triangle point, opposite d
     * @param pb - triangle point
     * @param pc - triangle point
     * @param pd - point opposite a
     * @return {boolean} true if d is inside circle, false if on circle edge
     */
    fun inCircle(pa: MPoint, pb: MPoint, pc: MPoint, pd: MPoint): Boolean {
        var adx = pa.x - pd.x
        var ady = pa.y - pd.y
        var bdx = pb.x - pd.x
        var bdy = pb.y - pd.y

        var adxbdy = adx * bdy
        var bdxady = bdx * ady
        var oabd = adxbdy - bdxady
        if (oabd <= 0) {
            return false
        }

        var cdx = pc.x - pd.x
        var cdy = pc.y - pd.y

        var cdxady = cdx * ady
        var adxcdy = adx * cdy
        var ocad = cdxady - adxcdy
        if (ocad <= 0) {
            return false
        }

        var bdxcdy = bdx * cdy
        var cdxbdy = cdx * bdy

        var alift = adx * adx + ady * ady
        var blift = bdx * bdx + bdy * bdy
        var clift = cdx * cdx + cdy * cdy

        var det = alift * (bdxcdy - cdxbdy) + blift * ocad + clift * oabd
        return det > 0
    }

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
    fun rotateTrianglePair(t: Triangle, p: MPoint, ot: Triangle, op: MPoint) {
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
        t.clearNeighbors()
        ot.clearNeighbors()
        if (n1 != null) {
            ot.markNeighbor(n1)
        }
        if (n2 != null) {
            t.markNeighbor(n2)
        }
        if (n3 != null) {
            t.markNeighbor(n3)
        }
        if (n4 != null) {
            ot.markNeighbor(n4)
        }
        t.markNeighbor(ot)
    }

    /**
     * Fills a basin that has formed on the Advancing Front to the right
     * of given node.<br>
     * First we decide a left,bottom and right node that forms the
     * boundaries of the basin. Then we do a reqursive fill.
     *
     * @param {!SweepContext} tcx - SweepContext object
     * @param node - starting node, this or next node will be left node
     */
    fun fillBasin(tcx: SweepContext, node: Node) {
        if (orient2d(node.point, node.next!!.point, node.next!!.next!!.point) === Orientation.CCW) {
            tcx.basin.left_node = node.next!!.next
        } else {
            tcx.basin.left_node = node.next
        }

        // Find the bottom and right node
        tcx.basin.bottom_node = tcx.basin.left_node
        while (tcx.basin.bottom_node!!.next != null && tcx.basin.bottom_node!!.point.y >= tcx.basin.bottom_node!!.next!!.point.y) {
            tcx.basin.bottom_node = tcx.basin.bottom_node!!.next
        }
        if (tcx.basin.bottom_node === tcx.basin.left_node) {
            // No valid basin
            return
        }

        tcx.basin.right_node = tcx.basin.bottom_node
        while (tcx.basin.right_node!!.next != null && tcx.basin.right_node!!.point.y < tcx.basin.right_node!!.next!!.point.y) {
            tcx.basin.right_node = tcx.basin.right_node!!.next
        }
        if (tcx.basin.right_node === tcx.basin.bottom_node) {
            // No valid basins
            return
        }

        tcx.basin.width = tcx.basin.right_node!!.point.x - tcx.basin.left_node!!.point.x
        tcx.basin.left_highest = tcx.basin.left_node!!.point.y > tcx.basin.right_node!!.point.y

        fillBasinReq(tcx, tcx.basin.bottom_node!!)
    }

    /**
     * Recursive algorithm to fill a Basin with triangles
     *
     * @param {!SweepContext} tcx - SweepContext object
     * @param node - bottom_node
     */
    fun fillBasinReq(tcx: SweepContext, node: Node) {
        // if shallow stop filling
        if (isShallow(tcx, node)) {
            return
        }

        fill(tcx, node)

        var node: Node = node

        when {
            node.prev === tcx.basin.left_node && node.next === tcx.basin.right_node -> {
                return
            }
            node.prev === tcx.basin.left_node -> {
                val o = orient2d(node.point, node.next!!.point, node.next!!.next!!.point)
                if (o === Orientation.CW) {
                    return
                }
                node = node.next!!
            }
            node.next === tcx.basin.right_node -> {
                val o = orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point)
                if (o === Orientation.CCW) {
                    return
                }
                node = node.prev!!
            }
            else -> {
                // Continue with the neighbor node with lowest Y value
                if (node.prev!!.point.y < node.next!!.point.y) {
                    node = node.prev!!
                } else {
                    node = node.next!!
                }
            }
        }

        fillBasinReq(tcx, node)
    }

    fun isShallow(tcx: SweepContext, node: Node): Boolean {
        val height = if (tcx.basin.left_highest) {
            tcx.basin.left_node!!.point.y - node.point.y
        } else {
            tcx.basin.right_node!!.point.y - node.point.y
        }

        // if shallow stop filling
        if (tcx.basin.width > height) {
            return true
        }
        return false
    }

    fun fillEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        if (tcx.edge_event.right) {
            fillRightAboveEdgeEvent(tcx, edge, node)
        } else {
            fillLeftAboveEdgeEvent(tcx, edge, node)
        }
    }

    fun fillRightAboveEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        var node: Node = node
        while (node.next!!.point.x < edge.p.x) {
            // Check if next node is below the edge
            if (orient2d(edge.q, node.next!!.point, edge.p) === Orientation.CCW) {
                fillRightBelowEdgeEvent(tcx, edge, node)
            } else {
                node = node.next!!
            }
        }
    }

    fun fillRightBelowEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        if (node.point.x < edge.p.x) {
            if (orient2d(node.point, node.next!!.point, node.next!!.next!!.point) === Orientation.CCW) {
                // Concave
                fillRightConcaveEdgeEvent(tcx, edge, node)
            } else {
                // Convex
                fillRightConvexEdgeEvent(tcx, edge, node)
                // Retry this one
                fillRightBelowEdgeEvent(tcx, edge, node)
            }
        }
    }

    fun fillRightConcaveEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        fill(tcx, node.next!!)
        if (node.next!!.point !== edge.p) {
            // Next above or below edge?
            if (orient2d(edge.q, node.next!!.point, edge.p) === Orientation.CCW) {
                // Below
                if (orient2d(node.point, node.next!!.point, node.next!!.next!!.point) === Orientation.CCW) {
                    // Next is concave
                    fillRightConcaveEdgeEvent(tcx, edge, node)
                } else {
                    // Next is convex
                    /* jshint noempty:false */
                }
            }
        }
    }

    fun fillRightConvexEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        // Next concave or convex?
        if (orient2d(node.next!!.point, node.next!!.next!!.point, node.next!!.next!!.next!!.point) === Orientation.CCW) {
            // Concave
            fillRightConcaveEdgeEvent(tcx, edge, node.next!!)
        } else {
            // Convex
            // Next above or below edge?
            if (orient2d(edge.q, node.next!!.next!!.point, edge.p) === Orientation.CCW) {
                // Below
                fillRightConvexEdgeEvent(tcx, edge, node.next!!)
            } else {
                // Above
                /* jshint noempty:false */
            }
        }
    }

    fun fillLeftAboveEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        var node = node
        while (node.prev!!.point.x > edge.p.x) {
            // Check if next node is below the edge
            if (orient2d(edge.q, node.prev!!.point, edge.p) === Orientation.CW) {
                fillLeftBelowEdgeEvent(tcx, edge, node)
            } else {
                node = node.prev!!
            }
        }
    }

    fun fillLeftBelowEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        if (node.point.x > edge.p.x) {
            if (orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) === Orientation.CW) {
                // Concave
                fillLeftConcaveEdgeEvent(tcx, edge, node)
            } else {
                // Convex
                fillLeftConvexEdgeEvent(tcx, edge, node)
                // Retry this one
                fillLeftBelowEdgeEvent(tcx, edge, node)
            }
        }
    }

    fun fillLeftConvexEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        // Next concave or convex?
        if (orient2d(node.prev!!.point, node.prev!!.prev!!.point, node.prev!!.prev!!.prev!!.point) === Orientation.CW) {
            // Concave
            fillLeftConcaveEdgeEvent(tcx, edge, node.prev!!)
        } else {
            // Convex
            // Next above or below edge?
            if (orient2d(edge.q, node.prev!!.prev!!.point, edge.p) === Orientation.CW) {
                // Below
                fillLeftConvexEdgeEvent(tcx, edge, node.prev!!)
            } else {
                // Above
                /* jshint noempty:false */
            }
        }
    }

    fun fillLeftConcaveEdgeEvent(tcx: SweepContext, edge: Edge, node: Node) {
        fill(tcx, node.prev!!)
        if (node.prev!!.point !== edge.p) {
            // Next above or below edge?
            if (orient2d(edge.q, node.prev!!.point, edge.p) === Orientation.CW) {
                // Below
                if (orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) === Orientation.CW) {
                    // Next is concave
                    fillLeftConcaveEdgeEvent(tcx, edge, node)
                } else {
                    // Next is convex
                    /* jshint noempty:false */
                }
            }
        }
    }

    fun flipEdgeEvent(tcx: SweepContext, ep: MPoint, eq: MPoint, t: Triangle, p: MPoint) {
        var t = t
        var ot = t.neighborAcross(p) ?: error("FLIP failed due to missing triangle!")

        var op = ot.oppositePoint(t, p)!!

        // Additional check from Java version (see issue #88)
        if (t.getConstrainedEdgeAcross(p)) {
            var index = t.index(p)
            throw PointError("poly2tri Intersecting Constraints", listOf(p, op, t.getPoint((index + 1) % 3), t.getPoint((index + 2) % 3)))
        }

        if (inScanArea(p, t.pointCCW(p)!!, t.pointCW(p)!!, op)) {
            // Lets rotate shared edge one vertex CW
            rotateTrianglePair(t, p, ot, op)
            tcx.mapTriangleToNodes(t)
            tcx.mapTriangleToNodes(ot)

            // XXX: in the original C++ code for the next 2 lines, we are
            // comparing point values (and not pointers). In this JavaScript
            // code, we are comparing point references (pointers). This works
            // because we can't have 2 different points with the same values.
            // But to be really equivalent, we should use "Point.equals" here.
            if (p === eq && op === ep) {
                if (eq === tcx.edge_event.constrained_edge!!.q && ep === tcx.edge_event.constrained_edge!!.p) {
                    t.markConstrainedEdgeByPoints(ep, eq)
                    ot.markConstrainedEdgeByPoints(ep, eq)
                    legalize(tcx, t)
                    legalize(tcx, ot)
                } else {
                    // XXX: I think one of the triangles should be legalized here?
                    /* jshint noempty:false */
                }
            } else {
                var o = orient2d(eq, op, ep)
                t = nextFlipTriangle(tcx, o, t, ot, p, op)
                flipEdgeEvent(tcx, ep, eq, t, p)
            }
        } else {
            var newP = nextFlipPoint(ep, eq, ot, op)
            flipScanEdgeEvent(tcx, ep, eq, t, ot, newP!!)
            edgeEventByPoints(tcx, ep, eq, t, p)
        }
    }

    /**
     * After a flip we have two triangles and know that only one will still be
     * intersecting the edge. So decide which to contiune with and legalize the other
     *
     * @param {!SweepContext} tcx - SweepContext object
     * @param o - should be the result of an orient2d( eq, op, ep )
     * @param t - triangle 1
     * @param ot - triangle 2
     * @param p - a point shared by both triangles
     * @param op - another point shared by both triangles
     * @return returns the triangle still intersecting the edge
     */
    fun nextFlipTriangle(tcx: SweepContext, o: Orientation, t: Triangle, ot: Triangle, p: MPoint, op: MPoint): Triangle {
        if (o === Orientation.CCW) {
            // ot is not crossing edge after flip
            ot.delaunay_edge[ot.edgeIndex(p, op)] = true
            legalize(tcx, ot)
            ot.clearDelaunayEdges()
            return t
        }

        // t is not crossing edge after flip
        t.delaunay_edge[t.edgeIndex(p, op)] = true
        legalize(tcx, t)
        t.clearDelaunayEdges()
        return ot
    }

    /**
     * When we need to traverse from one triangle to the next we need
     * the point in current triangle that is the opposite point to the next
     * triangle.
     */
    fun nextFlipPoint(ep: MPoint, eq: MPoint, ot: Triangle, op: MPoint): MPoint? {
        val o2d = orient2d(eq, op, ep)
        return when {
            o2d === Orientation.CW -> ot.pointCCW(op) // Right
            o2d === Orientation.CCW -> ot.pointCW(op) // Left
            else -> throw PointError("poly2tri [Unsupported] nextFlipPoint: opposing point on constrained edge!", listOf(eq, op, ep))
        }
    }

    /**
     * Scan part of the FlipScan algorithm<br>
     * When a triangle pair isn't flippable we will scan for the next
     * point that is inside the flip triangle scan area. When found
     * we generate a new flipEdgeEvent
     *
     * @param {!SweepContext} tcx - SweepContext object
     * @param ep - last point on the edge we are traversing
     * @param eq - first point on the edge we are traversing
     * @param {!Triangle} flip_triangle - the current triangle sharing the point eq with edge
     * @param t
     * @param p
     */
    fun flipScanEdgeEvent(tcx: SweepContext, ep: MPoint, eq: MPoint, flip_triangle: Triangle, t: Triangle, p: MPoint) {
        val ot = t.neighborAcross(p) ?: error("FLIP failed due to missing triangle")

        val op = ot.oppositePoint(t, p)

        if (inScanArea(eq, flip_triangle.pointCCW(eq)!!, flip_triangle.pointCW(eq)!!, op!!)) {
            // flip with new edge op.eq
            flipEdgeEvent(tcx, eq, op, ot, op)
        } else {
            val newP = nextFlipPoint(ep, eq, ot, op)
            flipScanEdgeEvent(tcx, ep, eq, flip_triangle, ot, newP!!)
        }
    }


    /**
     * Formula to calculate signed area<br>
     * Positive if CCW<br>
     * Negative if CW<br>
     * 0 if collinear<br>
     * <pre>
     * A[P1,P2,P3]  =  (x1*y2 - y1*x2) + (x2*y3 - y2*x3) + (x3*y1 - y3*x1)
     *              =  (x1-x3)*(y2-y3) - (y1-y3)*(x2-x3)
     * </pre>
     *
     * @private
     * @param {!XY} pa  point object with {x,y}
     * @param {!XY} pb  point object with {x,y}
     * @param {!XY} pc  point object with {x,y}
     * @return {Orientation}
     */
    fun orient2d(pa: MPoint, pb: MPoint, pc: MPoint): Orientation {
        val detleft = (pa.x - pc.x) * (pb.y - pc.y)
        val detright = (pa.y - pc.y) * (pb.x - pc.x)
        val v = detleft - detright
        return when {
            v > -(POLY2TRI_EPSILON) && v < (POLY2TRI_EPSILON) -> Orientation.COLLINEAR
            v > 0 -> Orientation.CCW
            else -> Orientation.CW
        }
    }


    /**
     *
     * @private
     * @param {!XY} pa  point object with {x,y}
     * @param {!XY} pb  point object with {x,y}
     * @param {!XY} pc  point object with {x,y}
     * @param {!XY} pd  point object with {x,y}
     * @return {boolean}
     */
    fun inScanArea(pa: MPoint, pb: MPoint, pc: MPoint, pd: MPoint): Boolean {
        val oadb = (pa.x - pb.x) * (pd.y - pb.y) - (pd.x - pb.x) * (pa.y - pb.y)
        if (oadb >= -POLY2TRI_EPSILON) return false

        val oadc = (pa.x - pc.x) * (pd.y - pc.y) - (pd.x - pc.x) * (pa.y - pc.y)
        if (oadc <= POLY2TRI_EPSILON) return false
        return true
    }

    /**
     * Check if the angle between (pa,pb) and (pa,pc) is obtuse i.e. (angle > π/2 || angle < -π/2)
     *
     * @private
     * @param {!XY} pa  point object with {x,y}
     * @param {!XY} pb  point object with {x,y}
     * @param {!XY} pc  point object with {x,y}
     * @return {boolean} true if angle is obtuse
     */
    fun isAngleObtuse(pa: MPoint, pb: MPoint, pc: MPoint): Boolean {
        val ax = pb.x - pa.x
        val ay = pb.y - pa.y
        val bx = pc.x - pa.x
        val by = pc.y - pa.y
        return (ax * bx + ay * by) < 0
    }

    /**
     * Precision to detect repeated or collinear points
     * @private
     * @const {number}
     * @default
     */
    const val POLY2TRI_EPSILON = 1e-12


    /**
     * Advancing front node
     * @constructor
     * @private
     * @struct
     * @param {!XY} p - Point
     * @param {Triangle=} t triangle (optional)
     */
    class Node(var point: MPoint, var triangle: Triangle? = null) {
        var next: Node? = null
        var prev: Node? = null
        var value: Double = point.x
    }

    class AdvancingFront(var head: Node, var tail: Node) {
        var search: Node = head

        fun findSearchNode(x: Double): Node {
            // TODO: implement BST index
            return this.search
        }

        /**
         * @param {number} x value
         * @return {Node}
         */
        fun locateNode(x: Double): Node? {
            var node: Node? = this.search

            /* jshint boss:true */
            if (x < node!!.value) {
                while (true) {
                    node = node!!.prev
                    if (node == null) break
                    if (x >= node.value) {
                        this.search = node
                        return node
                    }
                }
            } else {
                while (true) {
                    node = node!!.next
                    if (node == null) break
                    if (x < node.value) {
                        this.search = node.prev!!
                        return node.prev
                    }
                }
            }
            return null
        }


        /**
         * @param {!XY} point - Point
         * @return {Node}
         */
        fun locatePoint(point: MPoint): Node? {
            val px = point.x
            var node: Node? = this.findSearchNode(px)
            val nx = node!!.point.x

            when {
                px == nx -> {
                    // Here we are comparing point references, not values
                    if (point !== node.point) {
                        // We might have two nodes with same x value for a short time
                        if (point === node.prev!!.point) {
                            node = node.prev
                        } else if (point === node.next!!.point) {
                            node = node.next
                        } else {
                            throw Error("poly2tri Invalid AdvancingFront.locatePoint() call")
                        }
                    }
                }
                px < nx -> {
                    /* jshint boss:true */
                    while (true) {
                        node = node!!.prev
                        if (node == null) break
                        if (point === node.point) {
                            break
                        }
                    }
                }
                else -> {
                    while (true) {
                        node = node!!.next
                        if (node == null) break
                        if (point === node.point) {
                            break
                        }
                    }
                }
            }

            if (node != null) {
                this.search = node
            }
            return node
        }

    }

    /**
     * Custom exception class to indicate invalid Point values
     * @constructor
     * @public
     * @extends Error
     * @struct
     * @param {string=} message - error message
     * @param {Array.<Point>=} points - invalid points
     */
    class PointError(message: String, points: Iterable<MPoint>) : Exception(message + " " + points.toList())

    enum class Orientation(val value: Int) {
        CW(+1), CCW(-1), COLLINEAR(0)
    }

    /**
     * Triangle class.<br>
     * Triangle-based data structures are known to have better performance than
     * quad-edge structures.
     * See: J. Shewchuk, "Triangle: Engineering a 2D Quality Mesh Generator and
     * Delaunay Triangulator", "Triangulations in CGAL"
     *
     * @constructor
     * @struct
     * @param {!XY} pa  point object with {x,y}
     * @param {!XY} pb  point object with {x,y}
     * @param {!XY} pc  point object with {x,y}
     */
    class Triangle(val a: MPoint, val b: MPoint, val c: MPoint) : com.soywiz.korma.geom.triangle.Triangle {
        /**
         * Triangle points
         * @private
         * @type {Array.<Point>}
         */
        val points_ = arrayOf(a, b, c)

        override val p0 get() = points_[0]
        override val p1 get() = points_[1]
        override val p2 get() = points_[2]

        /**
         * Neighbor list
         * @private
         * @type {Array.<Triangle>}
         */
        val neighbors_ = arrayOfNulls<Triangle>(3)

        /**
         * Has this triangle been marked as an interior triangle?
         * @private
         * @type {boolean}
         */
        var interior_ = false

        /**
         * Flags to determine if an edge is a Constrained edge
         * @private
         * @type {Array.<boolean>}
         */
        val constrained_edge = booleanArrayOf(false, false, false)

        /**
         * Flags to determine if an edge is a Delauney edge
         * @private
         * @type {Array.<boolean>}
         */
        val delaunay_edge = booleanArrayOf(false, false, false)

        /**
         * For pretty printing ex. <code>"[(5;42)(10;20)(21;30)]"</code>.
         * @public
         * @return {string}
         */
        override fun toString(): String {
            return ("Triangle(${this.points_[0]}, ${this.points_[1]}, ${this.points_[2]})")
        }


        /**
         * Get one vertice of the triangle.
         * The output triangles of a triangulation have vertices which are references
         * to the initial input points (not copies): any custom fields in the
         * initial points can be retrieved in the output triangles.
         * @example
         *      var contour = [{x:100, y:100, id:1}, {x:100, y:300, id:2}, {x:300, y:300, id:3}];
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.triangulate();
         *      var triangles = swctx.getTriangles();
         *      typeof triangles[0].getPoint(0).id
         *      // → "number"
         * @param {number} index - vertice index: 0, 1 or 2
         * @public
         * @returns {Point}
         */
        fun getPoint(index: Int): MPoint = this.points_[index]

        /**
         * Get all 3 vertices of the triangle as an array
         * @public
         * @return {Array.<Point>}
         */
// Method added in the JavaScript version (was not present in the c++ version)
        fun getPoints(): Array<MPoint> = this.points_

        /**
         * @private
         * @param {number} index
         * @returns {?Triangle}
         */
        fun getNeighbor(index: Int): Triangle? = this.neighbors_[index]

        /**
         * Test if this Triangle contains the Point object given as parameter as one of its vertices.
         * Only point references are compared, not values.
         * @public
         * @param {Point} point - point object with {x,y}
         * @return {boolean} <code>True</code> if the Point object is of the Triangle's vertices,
         *         <code>false</code> otherwise.
         */
        // Here we are comparing point references, not values
        fun containsPoint(point: MPoint): Boolean {
            var points = this.points_
            return (point === points[0] || point === points[1] || point === points[2])
        }

        /**
         * Test if this Triangle contains the Edge object given as parameter as its
         * bounding edges. Only point references are compared, not values.
         * @private
         * @param {Edge} edge
         * @return {boolean} <code>True</code> if the Edge object is of the Triangle's bounding
         *         edges, <code>false</code> otherwise.
         */
        fun containsEdge(edge: Edge): Boolean = this.containsPoint(edge.p) && this.containsPoint(edge.q)

        /**
         * Test if this Triangle contains the two Point objects given as parameters among its vertices.
         * Only point references are compared, not values.
         * @param {Point} p1 - point object with {x,y}
         * @param {Point} p2 - point object with {x,y}
         * @return {boolean}
         */
        fun containsPoints(p1: MPoint, p2: MPoint): Boolean = this.containsPoint(p1) && this.containsPoint(p2)

        /**
         * Has this triangle been marked as an interior triangle?
         * @returns {boolean}
         */
        fun isInterior(): Boolean = this.interior_

        /**
         * Mark this triangle as an interior triangle
         * @private
         * @param {boolean} interior
         */
        fun setInterior(interior: Boolean) {
            this.interior_ = interior
        }

        /**
         * Update neighbor pointers.
         * @private
         * @param {Point} p1 - point object with {x,y}
         * @param {Point} p2 - point object with {x,y}
         * @param {Triangle} t Triangle object.
         * @throws {Error} if can't find objects
         */
        fun markNeighborPointers(p1: MPoint, p2: MPoint, t: Triangle) {
            var points = this.points_
            // Here we are comparing point references, not values
            when {
                p1 === points[2] && p2 === points[1] || p1 === points[1] && p2 === points[2] -> this.neighbors_[0] = t
                p1 === points[0] && p2 === points[2] || p1 === points[2] && p2 === points[0] -> this.neighbors_[1] = t
                p1 === points[0] && p2 === points[1] || p1 === points[1] && p2 === points[0] -> this.neighbors_[2] = t
                else -> throw Error("poly2tri Invalid Triangle.markNeighborPointers() call")
            }
        }

        /**
         * Exhaustive search to update neighbor pointers
         * @private
         * @param {!Triangle} t
         */
        fun markNeighbor(t: Triangle) {
            var points = this.points_
            when {
                t.containsPoints(points[1], points[2]) -> {
                    this.neighbors_[0] = t
                    t.markNeighborPointers(points[1], points[2], this)
                }
                t.containsPoints(points[0], points[2]) -> {
                    this.neighbors_[1] = t
                    t.markNeighborPointers(points[0], points[2], this)
                }
                t.containsPoints(points[0], points[1]) -> {
                    this.neighbors_[2] = t
                    t.markNeighborPointers(points[0], points[1], this)
                }
            }
        }


        fun clearNeighbors() {
            this.neighbors_[0] = null
            this.neighbors_[1] = null
            this.neighbors_[2] = null
        }

        fun clearDelaunayEdges() {
            this.delaunay_edge[0] = false
            this.delaunay_edge[1] = false
            this.delaunay_edge[2] = false
        }

        /**
         * Returns the point clockwise to the given point.
         * @private
         * @param {Point} p - point object with {x,y}
         */
        fun pointCW(p: MPoint?): MPoint? {
            var points = this.points_
            // Here we are comparing point references, not values
            return when {
                p === points[0] -> points[2]
                p === points[1] -> points[0]
                p === points[2] -> points[1]
                else -> null
            }
        }

        /**
         * Returns the point counter-clockwise to the given point.
         * @private
         * @param {Point} p - point object with {x,y}
         */
        fun pointCCW(p: MPoint): MPoint? {
            var points = this.points_
            // Here we are comparing point references, not values
            return when {
                p === points[0] -> points[1]
                p === points[1] -> points[2]
                p === points[2] -> points[0]
                else -> null
            }
        }

        /**
         * Returns the neighbor clockwise to given point.
         * @private
         * @param {Point} p - point object with {x,y}
         */
        // Here we are comparing point references, not values
        fun neighborCW(p: MPoint): Triangle? = when {
            p === this.points_[0] -> this.neighbors_[1]
            p === this.points_[1] -> this.neighbors_[2]
            else -> this.neighbors_[0]
        }

        /**
         * Returns the neighbor counter-clockwise to given point.
         * @private
         * @param {Point} p - point object with {x,y}
         */
        // Here we are comparing point references, not values
        fun neighborCCW(p: MPoint): Triangle? = when {
            p === this.points_[0] -> this.neighbors_[2]
            p === this.points_[1] -> this.neighbors_[0]
            else -> this.neighbors_[1]
        }

        // Here we are comparing point references, not values
        fun getConstrainedEdgeCW(p: MPoint): Boolean = when {
            p === this.points_[0] -> this.constrained_edge[1]
            p === this.points_[1] -> this.constrained_edge[2]
            else -> this.constrained_edge[0]
        }

        // Here we are comparing point references, not values
        fun getConstrainedEdgeCCW(p: MPoint): Boolean = when {
            p === this.points_[0] -> this.constrained_edge[2]
            p === this.points_[1] -> this.constrained_edge[0]
            else -> this.constrained_edge[1]
        }

        // Additional check from Java version (see issue #88)
        // Here we are comparing point references, not values
        fun getConstrainedEdgeAcross(p: MPoint): Boolean = when {
            p === this.points_[0] -> this.constrained_edge[0]
            p === this.points_[1] -> this.constrained_edge[1]
            else -> this.constrained_edge[2]
        }

        // Here we are comparing point references, not values
        fun setConstrainedEdgeCW(p: MPoint, ce: Boolean) = when {
            p === this.points_[0] -> this.constrained_edge[1] = ce
            p === this.points_[1] -> this.constrained_edge[2] = ce
            else -> this.constrained_edge[0] = ce
        }

        // Here we are comparing point references, not values
        fun setConstrainedEdgeCCW(p: MPoint, ce: Boolean) = when {
            p === this.points_[0] -> this.constrained_edge[2] = ce
            p === this.points_[1] -> this.constrained_edge[0] = ce
            else -> this.constrained_edge[1] = ce
        }

        // Here we are comparing point references, not values
        fun getDelaunayEdgeCW(p: MPoint): Boolean = when {
            p === this.points_[0] -> this.delaunay_edge[1]
            p === this.points_[1] -> this.delaunay_edge[2]
            else -> this.delaunay_edge[0]
        }

        // Here we are comparing point references, not values
        fun getDelaunayEdgeCCW(p: MPoint): Boolean = when {
            p === this.points_[0] -> this.delaunay_edge[2]
            p === this.points_[1] -> this.delaunay_edge[0]
            else -> this.delaunay_edge[1]
        }

        // Here we are comparing point references, not values
        fun setDelaunayEdgeCW(p: MPoint, e: Boolean) = when {
            p === this.points_[0] -> this.delaunay_edge[1] = e
            p === this.points_[1] -> this.delaunay_edge[2] = e
            else -> this.delaunay_edge[0] = e
        }

        // Here we are comparing point references, not values
        fun setDelaunayEdgeCCW(p: MPoint, e: Boolean) = when {
            p === this.points_[0] -> this.delaunay_edge[2] = e
            p === this.points_[1] -> this.delaunay_edge[0] = e
            else -> this.delaunay_edge[1] = e
        }

        /**
         * The neighbor across to given point.
         * @private
         * @param {Point} p - point object with {x,y}
         * @returns {Triangle}
         */
        // Here we are comparing point references, not values
        fun neighborAcross(p: MPoint): Triangle? = when {
            p === this.points_[0] -> this.neighbors_[0]
            p === this.points_[1] -> this.neighbors_[1]
            else -> this.neighbors_[2]
        }

        /**
         * @private
         * @param {!Triangle} t Triangle object.
         * @param {Point} p - point object with {x,y}
         */
        fun oppositePoint(t: Triangle, p: MPoint): MPoint? = this.pointCW(t.pointCW(p))

        /**
         * Legalize triangle by rotating clockwise around oPoint
         * @private
         * @param {Point} opoint - point object with {x,y}
         * @param {Point} npoint - point object with {x,y}
         * @throws {Error} if oPoint can not be found
         */
        fun legalize(opoint: MPoint, npoint: MPoint) {
            var points = this.points_
            // Here we are comparing point references, not values
            when {
                opoint === points[0] -> {
                    points[1] = points[0]
                    points[0] = points[2]
                    points[2] = npoint
                }
                opoint === points[1] -> {
                    points[2] = points[1]
                    points[1] = points[0]
                    points[0] = npoint
                }
                opoint === points[2] -> {
                    points[0] = points[2]
                    points[2] = points[1]
                    points[1] = npoint
                }
                else -> {
                    throw Error("poly2tri Invalid Triangle.legalize() call")
                }
            }
        }

        /**
         * Returns the index of a point in the triangle.
         * The point *must* be a reference to one of the triangle's vertices.
         * @private
         * @param {Point} p - point object with {x,y}
         * @returns {number} index 0, 1 or 2
         * @throws {Error} if p can not be found
         */
        fun index(p: MPoint): Int {
            var points = this.points_
            // Here we are comparing point references, not values
            return when {
                p === points[0] -> 0
                p === points[1] -> 1
                p === points[2] -> 2
                else -> throw Error("poly2tri Invalid Triangle.index() call")
            }
        }

        /**
         * @private
         * @param {Point} p1 - point object with {x,y}
         * @param {Point} p2 - point object with {x,y}
         * @return {number} index 0, 1 or 2, or -1 if errror
         */
        fun edgeIndex(p1: MPoint, p2: MPoint): Int {
            var points = this.points_
            // Here we are comparing point references, not values
            if (p1 === points[0]) {
                if (p2 === points[1]) {
                    return 2
                } else if (p2 === points[2]) {
                    return 1
                }
            } else if (p1 === points[1]) {
                if (p2 === points[2]) {
                    return 0
                } else if (p2 === points[0]) {
                    return 2
                }
            } else if (p1 === points[2]) {
                if (p2 === points[0]) {
                    return 1
                } else if (p2 === points[1]) {
                    return 0
                }
            }
            return -1
        }

        /**
         * Mark an edge of this triangle as constrained.
         * @private
         * @param {number} index - edge index
         */
        fun markConstrainedEdgeByIndex(index: Int) {
            this.constrained_edge[index] = true
        }

        /**
         * Mark an edge of this triangle as constrained.
         * @private
         * @param {Edge} edge instance
         */
        fun markConstrainedEdgeByEdge(edge: Edge) {
            this.markConstrainedEdgeByPoints(edge.p, edge.q)
        }

        /**
         * Mark an edge of this triangle as constrained.
         * This method takes two Point instances defining the edge of the triangle.
         * @private
         * @param {Point} p - point object with {x,y}
         * @param {Point} q - point object with {x,y}
         */
        fun markConstrainedEdgeByPoints(p: MPoint, q: MPoint) {
            val points = this.points_
            // Here we are comparing point references, not values
            when {
                q === points[0] && p === points[1] || q === points[1] && p === points[0] -> this.constrained_edge[2] = true
                q === points[0] && p === points[2] || q === points[2] && p === points[0] -> this.constrained_edge[1] = true
                q === points[1] && p === points[2] || q === points[2] && p === points[1] -> this.constrained_edge[0] = true
            }
        }
    }

    /**
     * Represents a simple polygon's edge
     * @constructor
     * @struct
     * @private
     * @param {Point} p1
     * @param {Point} p2
     * @throw {PointError} if p1 is same as p2
     */
    class Edge constructor(p1: MPoint, p2: MPoint) {

        var p: MPoint = p1
        var q: MPoint = p2

        init {
            if (p1.y > p2.y) {
                this.q = p1
                this.p = p2
            } else if (p1.y === p2.y) {
                if (p1.x > p2.x) {
                    this.q = p1
                    this.p = p2
                } else if (p1.x === p2.x) {
                    throw PointError("poly2tri Invalid Edge constructor: repeated points!", listOf(p1))
                }
            }
        }
    }


    /**
     * @constructor
     * @struct
     * @private
     */
    class Basin {
        /** @type {Node} */
        var left_node: Node? = null

        /** @type {Node} */
        var bottom_node: Node? = null

        /** @type {Node} */
        var right_node: Node? = null

        /** @type {number} */
        var width = 0.0

        /** @type {boolean} */
        var left_highest = false

        fun clear() {
            this.left_node = null
            this.bottom_node = null
            this.right_node = null
            this.width = 0.0
            this.left_highest = false
        }
    }


    /**
     * @constructor
     * @struct
     * @private
     */
    class EdgeEvent {
        /** @type {Edge} */
        var constrained_edge: Edge? = null

        /** @type {boolean} */
        var right = false
    }

    /**
     * SweepContext constructor option
     * @typedef {Object} SweepContextOptions
     * @property {boolean=} cloneArrays - if <code>true</code>, do a shallow copy of the Array parameters
     *                  (contour, holes). Points inside arrays are never copied.
     *                  Default is <code>false</code> : keep a reference to the array arguments,
     *                  who will be modified in place.
     */
    /**
     * Constructor for the triangulation context.
     * It accepts a simple polyline (with non repeating points),
     * which defines the constrained edges.
     *
     * @example
     *          var contour = [
     *              new poly2tri.Point(100, 100),
     *              new poly2tri.Point(100, 300),
     *              new poly2tri.Point(300, 300),
     *              new poly2tri.Point(300, 100)
     *          ];
     *          var swctx = new poly2tri.SweepContext(contour, {cloneArrays: true});
     * @example
     *          var contour = [{x:100, y:100}, {x:100, y:300}, {x:300, y:300}, {x:300, y:100}];
     *          var swctx = new poly2tri.SweepContext(contour, {cloneArrays: true});
     * @constructor
     * @public
     * @struct
     * @param {Array.<Point>} contour - array of point objects. The points can be either {@linkcode Point} instances,
     *          or any "Point like" custom class with <code>{x, y}</code> attributes.
     * @param {SweepContextOptions=} options - constructor options
     */
    class SweepContext() {
        /**
         * Initial triangle factor, seed triangle will extend 30% of
         * PointSet width to both left and right.
         * @private
         * @const
         */
        var kAlpha = 0.3

        val triangles_ = FastArrayList<Triangle>()
        var map_ = FastArrayList<Triangle>()
        var points_ = FastArrayList<MPoint>()
        val _p2t_edge_lists = FastArrayList<FastArrayList<Edge>?>()

        var edge_list = FastArrayList<Edge>()

        // Bounding box of all points. Computed at the start of the triangulation,
        // it is stored in case it is needed by the caller.
        var pbounds: MRectangle = MRectangle()
        //var pmin_: Point? = null
        //var pmax_: Point? = null

        /**
         * Advancing front
         * @private
         * @type {AdvancingFront}
         */
        var front_: AdvancingFront? = null

        /**
         * head point used with advancing front
         * @private
         * @type {Point}
         */
        var head_: MPoint? = null

        /**
         * tail point used with advancing front
         * @private
         * @type {Point}
         */
        var tail_: MPoint? = null

        /**
         * @private
         * @type {Node}
         */
        var af_head_: Node? = null

        /**
         * @private
         * @type {Node}
         */
        var af_middle_: Node? = null

        /**
         * @private
         * @type {Node}
         */
        var af_tail_: Node? = null

        var basin = Basin()
        var edge_event = EdgeEvent()

        /**
         * Add a hole to the constraints
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      var hole = [
         *          new poly2tri.Point(200, 200),
         *          new poly2tri.Point(200, 250),
         *          new poly2tri.Point(250, 250)
         *      ];
         *      swctx.addHole(hole);
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.addHole([{x:200, y:200}, {x:200, y:250}, {x:250, y:250}]);
         * @public
         * @param {Array.<Point>} polyline - array of "Point like" objects with {x,y}
         */
        fun addHole(polyline: List<MPoint>) {
            var len = polyline.size
            val points = this.points_
            val start = points.size
            // Point addition
            points.addAll(polyline)

            while (_p2t_edge_lists.size < points.size) _p2t_edge_lists.add(null)

            // Edge initialization
            for (i in 0 until len) {
                val i1 = start + i
                val i2 = start + ((i + 1) % len)
                val p1 = points[i1]
                val p2 = points[i2]
                if (p1 != p2) {
                    val edge = Edge(p1, p2)
                    this.edge_list.add(edge)
                    val index = if (edge.q == p1) i1 else i2

                    if (_p2t_edge_lists[index] == null) _p2t_edge_lists[index] = FastArrayList()
                    _p2t_edge_lists[index]!!.add(edge)

                }
            }
        }

        @JvmName("addHolesListIPointArrayList")
        fun addHoles(polyline: List<PointList>) {
            for (p in polyline) addHole(p.map { x, y -> MPoint(x, y) })
        }


        /**
         * Add several holes to the constraints
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      var holes = [
         *          [ new poly2tri.Point(200, 200), new poly2tri.Point(200, 250), new poly2tri.Point(250, 250) ],
         *          [ new poly2tri.Point(300, 300), new poly2tri.Point(300, 350), new poly2tri.Point(350, 350) ]
         *      ];
         *      swctx.addHoles(holes);
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      var holes = [
         *          [{x:200, y:200}, {x:200, y:250}, {x:250, y:250}],
         *          [{x:300, y:300}, {x:300, y:350}, {x:350, y:350}]
         *      ];
         *      swctx.addHoles(holes);
         * @public
         * @param {Array.<Array.<Point>>} holes - array of array of "Point like" objects with {x,y}
         */
// Method added in the JavaScript version (was not present in the c++ version)
        fun addHoles(holes: List<List<MPoint>>) {
            holes.fastForEach { addHole(it) }
        }


        /**
         * Add a Steiner point to the constraints
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      var point = new poly2tri.Point(150, 150);
         *      swctx.addPoint(point);
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.addPoint({x:150, y:150});
         * @public
         * @param {Point} point - any "Point like" object with {x,y}
         */
        fun addPoint(point: MPoint) {
            this.points_.add(point)
        }

        /**
         * Add several Steiner points to the constraints
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      var points = [
         *          new poly2tri.Point(150, 150),
         *          new poly2tri.Point(200, 250),
         *          new poly2tri.Point(250, 250)
         *      ];
         *      swctx.addPoints(points);
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.addPoints([{x:150, y:150}, {x:200, y:250}, {x:250, y:250}]);
         * @public
         * @param {Array.<Point>} points - array of "Point like" object with {x,y}
         */
        // Method added in the JavaScript version (was not present in the c++ version)
        fun addPoints(points: List<MPoint>) {
            points.fastForEach { this.points_.add(MPoint(it.x, it.y)) }
        }


        /**
         * Triangulate the polygon with holes and Steiner points.
         * Do this AFTER you've added the polyline, holes, and Steiner points
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.triangulate();
         *      var triangles = swctx.getTriangles();
         * @public
         */
// Shortcut method for sweep.triangulate(SweepContext).
// Method added in the JavaScript version (was not present in the c++ version)
        fun triangulate() {
            Poly2Tri.triangulate(this)
        }


        /**
         * Get the bounding box of the provided constraints (contour, holes and
         * Steinter points). Warning : these values are not available if the triangulation
         * has not been done yet.
         * @public
         * @returns {{min:Point,max:Point}} object with 'min' and 'max' Point
         */
// Method added in the JavaScript version (was not present in the c++ version)
        fun getBoundingBox(): MRectangle {
            return pbounds
        }

        /**
         * Get result of triangulation.
         * The output triangles have vertices which are references
         * to the initial input points (not copies): any custom fields in the
         * initial points can be retrieved in the output triangles.
         * @example
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.triangulate();
         *      var triangles = swctx.getTriangles();
         * @example
         *      var contour = [{x:100, y:100, id:1}, {x:100, y:300, id:2}, {x:300, y:300, id:3}];
         *      var swctx = new poly2tri.SweepContext(contour);
         *      swctx.triangulate();
         *      var triangles = swctx.getTriangles();
         *      typeof triangles[0].getPoint(0).id
         *      // → "number"
         * @public
         * @returns {array<Triangle>}   array of triangles
         */
        fun getTriangles(): List<Triangle> {
            return this.triangles_
        }

        /** @private */
        fun pointCount(): Int {
            return this.points_.size
        }

        /** @private */
        fun getMap(): MutableList<Triangle> {
            return this.map_
        }

        /** @private */
        fun initTriangulation() {
            var xmax = this.points_[0].x
            var xmin = this.points_[0].x
            var ymax = this.points_[0].y
            var ymin = this.points_[0].y

            // Calculate bounds
            var len = this.points_.size
            for (i in 1 until len) {
                var p = this.points_[i]
                /* jshint expr:true */
                if (p.x > xmax) xmax = p.x
                if (p.x < xmin) xmin = p.x
                if (p.y > ymax) ymax = p.y
                if (p.y < ymin) ymin = p.y
            }
            this.pbounds.setBounds(xmin, ymin, xmax, ymax)

            var dx = kAlpha * (xmax - xmin)
            var dy = kAlpha * (ymax - ymin)
            this.head_ = MPoint(xmax + dx, ymin - dy)
            this.tail_ = MPoint(xmin - dx, ymin - dy)

            // Sort points along y-axis
            genericSort(this, 0, this.points_.size - 1, object : SortOps<SweepContext>() {
                override fun compare(subject: SweepContext, l: Int, r: Int): Int {
                    return subject.points_[l].compareTo(subject.points_[r])
                }

                override fun swap(subject: SweepContext, indexL: Int, indexR: Int) {
                    subject.points_.swap(indexL, indexR)
                    subject._p2t_edge_lists.swap(indexL, indexR)
                }
            })
        }

        /** @private */
        fun getPoint(index: Int): MPoint {
            return this.points_[index]
        }

        /** @private */
        fun addToMap(triangle: Triangle) {
            this.map_.add(triangle)
        }

        /** @private */
        fun locateNode(point: MPoint): Node? {
            return this.front_!!.locateNode(point.x)
        }

        /** @private */
        fun createAdvancingFront() {
            // Initial triangle
            var triangle = Triangle(this.points_[0], this.tail_!!, this.head_!!)

            this.map_.add(triangle)

            val head = Node(triangle.getPoint(1), triangle)
            val middle = Node(triangle.getPoint(0), triangle)
            val tail = Node(triangle.getPoint(2))

            this.front_ = AdvancingFront(head, tail)

            head.next = middle
            middle.next = tail
            middle.prev = head
            tail.prev = middle
        }

        /** @private */
        fun removeNode(node: Node?) {
            // do nothing
            /* jshint unused:false */
        }

        /** @private */
        fun mapTriangleToNodes(t: Triangle) {
            for (i in 0 until 3) {
                if (t.getNeighbor(i) == null) {
                    var n = this.front_!!.locatePoint(t.pointCW(t.getPoint(i))!!)
                    if (n != null) {
                        n.triangle = t
                    }
                }
            }
        }

        /** @private */
        fun removeFromMap(triangle: Triangle) {
            var map = this.map_
            var len = map.size
            for (i in 0 until len) {
                if (map[i] === triangle) {
                    map.removeAt(i)
                    break
                }
            }
        }

        /**
         * Do a depth first traversal to collect triangles
         * @private
         * @param {Triangle} triangle start
         */
        fun meshClean(triangle: Triangle) {
            // New implementation avoids recursive calls and use a loop instead.
            // Cf. issues # 57, 65 and 69.
            var triangles = fastArrayListOf(triangle)
            /* jshint boss:true */
            while (triangles.isNotEmpty()) {
                val t = triangles.removeLast()
                if (!t.isInterior()) {
                    t.setInterior(true)
                    this.triangles_.add(t)
                    for (i in 0 until 3) {
                        if (!t.constrained_edge[i]) {
                            triangles.add(t.getNeighbor(i) ?: throw PointError("Null pointer", points_))
                        }
                    }
                }
            }
        }

    }

}
