package com.soywiz.korma.triangle.pathfind

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.triangle.internal.*
import kotlin.math.*

class SpatialMesh {
    private var mapTriangleToSpatialNode = hashMapOf<Triangle, Node>()
    var nodes = arrayListOf<Node>()

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(triangles: Iterable<Triangle>) {
        for (triangle in triangles) {
            val node = getNodeFromTriangle(triangle)
            if (node != null) nodes.add(node)
        }

        // Compute neighborhoods
        for (node in nodes) {
            for (edge in node.edges) {
                for (rnode in edge.nodes) {
                    if (rnode !== node) {
                        node.neighbors += rnode
                    }
                }
            }
        }
    }

    fun spatialNodeFromPoint(point: IPoint): Node {
        for (node in nodes) {
            if (node.triangle!!.pointInsideTriangle(point)) return node
        }
        throw Error("Point2d not inside triangles")
    }

    fun getNodeAt(point: IPoint): Node? {
        for (node in nodes) if (node.triangle!!.containsPoint(point)) return node
        return null
    }

    fun getNodeFromTriangle(triangle: Triangle?): Node? {
        if (triangle === null) return null

        if (!mapTriangleToSpatialNode.containsKey(triangle)) {
            val tp0 = triangle.p0
            val tp1 = triangle.p1
            val tp2 = triangle.p2
            val sn = Node(
                x = ((tp0.x + tp1.x + tp2.x) / 3).toInt().toDouble(),
                y = ((tp0.y + tp1.y + tp2.y) / 3).toInt().toDouble(),
                z = 0.0,
                triangle = triangle,
                G = 0,
                H = 0
            )
            mapTriangleToSpatialNode[triangle] = sn
        }
        return mapTriangleToSpatialNode[triangle]
    }

    fun getNodeEdge(p0: IPoint, p1: IPoint): NodeEdge {
        val edge = Edge(p0, p1)
        return nodeEdges.getOrPut(edge) { NodeEdge(edge) }
    }

    private val nodeEdges = LinkedHashMap<Edge, NodeEdge>()
    class NodeEdge(val edge: Edge) {
        val nodes = arrayListOf<Node>()
    }

    inner class Node(
        val x: Double,
        val y: Double,
        val z: Double,
        val triangle: Triangle,
        var G: Int = 0, // Cost
        var H: Int = 0, // Heuristic
        var parent: Node? = null,
        var closed: Boolean = false
    ) {
        val neighbors: ArrayList<Node> = ArrayList()

        // Edges
        val e0 = getNodeEdge(triangle.p0, triangle.p1).also { it.nodes += this }
        val e1 = getNodeEdge(triangle.p1, triangle.p2).also { it.nodes += this }
        val e2 = getNodeEdge(triangle.p2, triangle.p0).also { it.nodes += this }

        val edges = listOf(e0, e1, e2)

        val F: Int get() = G + H // F = G + H

        fun distanceToSpatialNode(that: Node): Int = hypot(this.x - that.x, this.y - that.y).toInt()

        override fun toString(): String = "SpatialNode(${x.niceStr2}, ${y.niceStr2})"
    }

    override fun toString() = "SpatialMesh(" + nodes.joinToString(",") + ")"
}
