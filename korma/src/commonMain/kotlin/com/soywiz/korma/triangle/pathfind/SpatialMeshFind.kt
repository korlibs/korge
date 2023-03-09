package com.soywiz.korma.triangle.pathfind

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.triangle.*
import kotlin.math.*

private typealias SpatialNode = SpatialMesh.Node

fun SpatialMesh.finder() = SpatialMeshFind(this)

class SpatialMeshFind(val spatialMesh: SpatialMesh) {
    private var openedList = PriorityQueue<SpatialNode> { l, r -> l.F.compareTo(r.F) }

    init {
        reset()
    }

    private fun reset() {
        openedList = PriorityQueue { l, r -> l.F.compareTo(r.F) }
        for (node in this.spatialMesh.nodes) {
            node.parent = null
            node.G = 0
            node.H = 0
            node.closed = false
        }
    }

    fun find(start: Point, end: Point): PointList {
        return Channel.channelToPortals(start, end, findNodes(spatialMesh.spatialNodeFromPoint(start), spatialMesh.spatialNodeFromPoint(end))).path
    }

    fun findNodes(start: Point, end: Point): List<SpatialNode> {
        return findNodes(spatialMesh.spatialNodeFromPoint(start), spatialMesh.spatialNodeFromPoint(end))
    }

    fun findNodes(startNode: SpatialNode?, endNode: SpatialNode?): List<SpatialNode> {
        val returnList = ArrayList<SpatialNode>()
        reset()
        var currentNode = startNode

        if (startNode !== null && endNode !== null) {
            addToOpenedList(startNode)

            while ((currentNode != endNode) && openedListHasItems()) {
                currentNode = getAndRemoveFirstFromOpenedList()
                addNodeToClosedList(currentNode)

                for (neighborNode in getNodeNeighbors(currentNode)) {
                    // Ignore invalid paths and the ones on the closed list.
                    if (inClosedList(neighborNode)) continue

                    val G = currentNode.G + neighborNode.distanceToSpatialNode(currentNode)
                    // Not in opened list yet.
                    if (!inOpenedList(neighborNode)) {
                        addToOpenedList(neighborNode)
                        neighborNode.G = G
                        neighborNode.H = neighborNode.distanceToSpatialNode(endNode)
                        neighborNode.parent = currentNode
                        updatedNodeOnOpenedList(neighborNode)
                    }
                    // In opened list but with a worse G than this one.
                    else if (G < neighborNode.G) {
                        neighborNode.G = G
                        neighborNode.parent = currentNode
                        updatedNodeOnOpenedList(neighborNode)
                    }
                }
            }
        }

        if (currentNode != endNode) throw(Exception("Can't find a path", 1))

        while (currentNode != startNode) {
            returnList.add(currentNode!!)
            //returnList.push(currentNode);
            currentNode = currentNode.parent
        }

        if (startNode != null) returnList.add(startNode)

        returnList.reverse()

        return returnList
    }

    private fun addToOpenedList(node: SpatialNode) { openedList.add(node) }
    private fun openedListHasItems(): Boolean = openedList.size > 0
    private fun getAndRemoveFirstFromOpenedList(): SpatialNode = openedList.removeHead()
    private fun addNodeToClosedList(node: SpatialNode) { node.closed = true }
    private fun inClosedList(node: SpatialNode): Boolean = node.closed
    private fun getNodeNeighbors(node: SpatialNode): ArrayList<SpatialNode> = node.neighbors
    private fun inOpenedList(node: SpatialNode): Boolean = openedList.contains(node)

    private fun updatedNodeOnOpenedList(node: SpatialNode) {
        openedList.updateObject(node)
    }

    /*public fun inClosedList(node:SpatialNode):Boolean {
        return (closedList[node] !== undefined);
    }*/

    object Channel {
        class EdgeContext {
            fun getCommonEdge(t1: Triangle, t2: Triangle): Edge {
                val commonIndexes = PointArrayList()
                for (n in 0 until 3) {
                    val point = t1.point(n)
                    if (t2.containsPoint(point)) commonIndexes.add(point)
                }
                if (commonIndexes.size != 2) throw Error("Triangles are not contiguous")
                return Edge(commonIndexes[0], commonIndexes[1])
            }

        }

        fun channelToPortals(startPoint: Point, endPoint: Point, channel: List<SpatialNode>): Funnel {
            val portals = Funnel()

            portals.push(startPoint)

            if (channel.size >= 2) {
                val firstTriangle = channel[0].triangle!!
                val secondTriangle = channel[1].triangle!!
                val lastTriangle = channel[channel.size - 1].triangle!!

                assert(firstTriangle.pointInsideTriangle(startPoint))
                assert(lastTriangle.pointInsideTriangle(endPoint))

                val startVertex = Triangle.getNotCommonVertex(firstTriangle, secondTriangle).point

                var vertexCW0: Point = startVertex
                var vertexCCW0: Point = startVertex

                //trace(startVertex);

                val ctxEdge = EdgeContext()

                for (n in 0 until channel.size - 1) {
                    val triangleCurrent = channel[n + 0].triangle!!
                    val triangleNext = channel[n + 1].triangle!!
                    val commonEdge = ctxEdge.getCommonEdge(triangleCurrent, triangleNext)
                    val vertexCW1 = triangleCurrent.pointCW(vertexCW0)
                    val vertexCCW1 = triangleCurrent.pointCCW(vertexCCW0)
                    if (!commonEdge.hasPoint(vertexCW0)) vertexCW0 = vertexCW1
                    if (!commonEdge.hasPoint(vertexCCW0)) vertexCCW0 = vertexCCW1
                    portals.push(vertexCW0, vertexCCW0)
                    //trace(vertexCW0, vertexCCW0);
                }
            }

            portals.push(endPoint)

            portals.stringPull()

            return portals
        }

        private fun assert(test: Boolean) {
            if (!test) throw(Error("Assert error"))
        }

        class Funnel {
            private val portals = ArrayList<Portal>()
            var path = PointArrayList()

            override fun toString(): String = "Funnel($path)"

            companion object {
                private fun triarea2(a: Point, b: Point, c: Point): Float {
                    val ax = b.x - a.x
                    val ay = b.y - a.y
                    val bx = c.x - a.x
                    val by = c.y - a.y
                    return bx * ay - ax * by
                }

                private fun vdistsqr(a: Point, b: Point): Float = hypot(b.x - a.x, b.y - a.y)
                private fun vdistsqr(ax: Double, ay: Double, bx: Double, by: Double): Double = hypot(bx - ax, by - ay)

                private fun vequal(a: Point, b: Point): Boolean = vdistsqr(a, b) < (0.001 * 0.001)
                private fun vequal(ax: Double, ay: Double, bx: Double, by: Double): Boolean = vdistsqr(ax, ay, bx, by) < (0.001 * 0.001)
            }

            fun push(p1: Point, p2: Point = p1) {
                this.portals.add(Portal(p1, p2))
                /*if (p2 == p1) {
                    trace('channel.push(' + p1 + ');');
                } else {
                    trace('channel.push(' + p1 + ', ' + p2 + ');');
                }*/
            }

            fun stringPull(): PointList {
                val pts = PointArrayList()
                // Init scan state
                var apexIndex: Int = 0
                var leftIndex: Int = 0
                var rightIndex: Int = 0

                var portalApex = portals[0].left
                var portalLeft = portals[0].left
                var portalRight = portals[0].right

                // Add start point.
                pts.add(portalApex)

                var i = 0
                while (i < portals.size) {
                    val left = portals[i].left
                    val right = portals[i].right
                    i++

                    // Update right vertex.
                    if (triarea2(portalApex, portalRight, right) <= 0.0) {
                        if (vequal(portalApex, portalRight) || triarea2(portalApex, portalLeft, right) > 0.0) {
                            // Tighten the funnel.
                            portalRight = right
                            rightIndex = i
                        } else {
                            // Right over left, insert left to path and restart scan from portal left point.
                            pts.add(portalLeft)
                            // Make current left the apex.
                            portalApex = portalLeft
                            apexIndex = leftIndex
                            // Reset portal
                            portalLeft = portalApex
                            portalRight = portalApex
                            leftIndex = apexIndex
                            rightIndex = apexIndex
                            // Restart scan
                            i = apexIndex
                            continue
                        }
                    }

                    // Update left vertex.
                    if (triarea2(portalApex, portalLeft, left) >= 0.0) {
                        if (vequal(portalApex, portalLeft) || triarea2(portalApex, portalRight, left) < 0.0) {
                            // Tighten the funnel.
                            portalLeft = left
                            leftIndex = i
                        } else {
                            // Left over right, insert right to path and restart scan from portal right point.
                            pts.add(portalRight)
                            // Make current right the apex.
                            portalApex = portalRight
                            apexIndex = rightIndex
                            // Reset portal
                            portalLeft = portalApex
                            portalRight = portalApex
                            leftIndex = apexIndex
                            rightIndex = apexIndex
                            // Restart scan
                            i = apexIndex
                            continue
                        }
                    }
                }

                if ((pts.size == 0) || (!vequal(pts[pts.size - 1], portals[portals.size - 1].left))) {
                    // Append last point to path.
                    pts.add(portals[portals.size - 1].left)
                }

                this.path = pts
                return pts
            }

            data class Portal(val left: Point, val right: Point)
        }
    }

    class Exception(message: String = "", val index: Int = 0) : kotlin.Exception(message)
}
