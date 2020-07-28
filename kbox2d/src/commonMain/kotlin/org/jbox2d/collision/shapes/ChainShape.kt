/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.collision.shapes


import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.collision.RayCastOutput
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*

/**
 * A chain shape is a free form sequence of line segments. The chain has two-sided collision, so you
 * can use inside and outside collision. Therefore, you may use any winding order. Connectivity
 * information is used to create smooth collisions. WARNING: The chain will not collide properly if
 * there are self-intersections.
 *
 * @author Daniel
 */
class ChainShape : Shape(ShapeType.CHAIN) {

    var vertices: Array<Vec2>? = null

    var count: Int = 0

    val prevVertex = Vec2()
    val nextVertex = Vec2()

    var hasPrevVertex = false
    var hasNextVertex = false

    private val pool0 = EdgeShape()

    init {
        vertices = null
        radius = Settings.polygonRadius
        count = 0
    }

    fun clear() {
        vertices = null
        count = 0
    }

    override fun getChildCount(): Int {
        return count - 1
    }

    /**
     * Get a child edge.
     */
    fun getChildEdge(edge: EdgeShape, index: Int) {
        assert(0 <= index && index < count - 1)
        edge.radius = radius

        val v0 = vertices!![index + 0]
        val v1 = vertices!![index + 1]
        edge.vertex1.x = v0.x
        edge.vertex1.y = v0.y
        edge.vertex2.x = v1.x
        edge.vertex2.y = v1.y

        if (index > 0) {
            val v = vertices!![index - 1]
            edge.vertex0.x = v.x
            edge.vertex0.y = v.y
            edge.hasVertex0 = true
        } else {
            edge.vertex0.x = prevVertex.x
            edge.vertex0.y = prevVertex.y
            edge.hasVertex0 = hasPrevVertex
        }

        if (index < count - 2) {
            val v = vertices!![index + 2]
            edge.vertex3.x = v.x
            edge.vertex3.y = v.y
            edge.hasVertex3 = true
        } else {
            edge.vertex3.x = nextVertex.x
            edge.vertex3.y = nextVertex.y
            edge.hasVertex3 = hasNextVertex
        }
    }

    override fun computeDistanceToOut(xf: Transform, p: Vec2, childIndex: Int, normalOut: Vec2): Float {
        val edge = pool0
        getChildEdge(edge, childIndex)
        return edge.computeDistanceToOut(xf, p, 0, normalOut)
    }

    override fun testPoint(xf: Transform, p: Vec2): Boolean {
        return false
    }

    override fun raycast(output: RayCastOutput, input: RayCastInput, xf: Transform, childIndex: Int): Boolean {
        assert(childIndex < count)

        val edgeShape = pool0

        val i1 = childIndex
        var i2 = childIndex + 1
        if (i2 == count) {
            i2 = 0
        }
        val v = vertices!![i1]
        edgeShape.vertex1.x = v.x
        edgeShape.vertex1.y = v.y
        val v1 = vertices!![i2]
        edgeShape.vertex2.x = v1.x
        edgeShape.vertex2.y = v1.y

        return edgeShape.raycast(output, input, xf, 0)
    }

    override fun computeAABB(aabb: AABB, xf: Transform, childIndex: Int) {
        assert(childIndex < count)
        val lower = aabb.lowerBound
        val upper = aabb.upperBound

        val i1 = childIndex
        var i2 = childIndex + 1
        if (i2 == count) {
            i2 = 0
        }

        val vi1 = vertices!![i1]
        val vi2 = vertices!![i2]
        val xfq = xf.q
        val xfp = xf.p
        val v1x = xfq.c * vi1.x - xfq.s * vi1.y + xfp.x
        val v1y = xfq.s * vi1.x + xfq.c * vi1.y + xfp.y
        val v2x = xfq.c * vi2.x - xfq.s * vi2.y + xfp.x
        val v2y = xfq.s * vi2.x + xfq.c * vi2.y + xfp.y

        lower.x = if (v1x < v2x) v1x else v2x
        lower.y = if (v1y < v2y) v1y else v2y
        upper.x = if (v1x > v2x) v1x else v2x
        upper.y = if (v1y > v2y) v1y else v2y
    }

    override fun computeMass(massData: MassData, density: Float) {
        massData.mass = 0.0f
        massData.center.setZero()
        massData.I = 0.0f
    }

    override fun clone(): Shape {
        val clone = ChainShape()
        clone.createChain(vertices, count)
        clone.prevVertex.set(prevVertex)
        clone.nextVertex.set(nextVertex)
        clone.hasPrevVertex = hasPrevVertex
        clone.hasNextVertex = hasNextVertex
        return clone
    }

    /**
     * Create a loop. This automatically adjusts connectivity.
     *
     * @param vertices an array of vertices, these are copied
     * @param count the vertex count
     */
    fun createLoop(vertices: Array<Vec2>, count: Int) {
        assert(this.vertices == null && this.count == 0)
        assert(count >= 3)
        this.count = count + 1
        this.vertices = Array(this.count) { Vec2.dummy }
        for (i in 1 until count) {
            val v1 = vertices[i - 1]
            val v2 = vertices[i]
            // If the code crashes here, it means your vertices are too close together.
            if (MathUtils.distanceSquared(v1, v2) < Settings.linearSlop * Settings.linearSlop) {
                throw RuntimeException("Vertices of chain shape are too close together")
            }
        }
        for (i in 0 until count) {
            this.vertices!![i] = Vec2(vertices[i])
        }
        this.vertices!![count] = Vec2(this.vertices!![0])
        prevVertex.set(this.vertices!![this.count - 2])
        nextVertex.set(this.vertices!![1])
        hasPrevVertex = true
        hasNextVertex = true
    }

    /**
     * Create a chain with isolated end vertices.
     *
     * @param vertices an array of vertices, these are copied
     * @param count the vertex count
     */
    fun createChain(vertices: Array<Vec2>?, count: Int) {
        assert(this.vertices == null && this.count == 0)
        assert(count >= 2)
        this.count = count
        this.vertices = Array(this.count) { Vec2.dummy }
        for (i in 1 until this.count) {
            val v1 = vertices!![i - 1]
            val v2 = vertices[i]
            // If the code crashes here, it means your vertices are too close together.
            if (MathUtils.distanceSquared(v1, v2) < Settings.linearSlop * Settings.linearSlop) {
                throw RuntimeException("Vertices of chain shape are too close together")
            }
        }
        for (i in 0 until this.count) {
            this.vertices!![i] = Vec2(vertices!![i])
        }
        hasPrevVertex = false
        hasNextVertex = false

        prevVertex.setZero()
        nextVertex.setZero()
    }

    /**
     * Establish connectivity to a vertex that precedes the first vertex. Don't call this for loops.
     *
     * @param prevVertex
     */
    fun setPrevVertex(prevVertex: Vec2) {
        this.prevVertex.set(prevVertex)
        hasPrevVertex = true
    }

    /**
     * Establish connectivity to a vertex that follows the last vertex. Don't call this for loops.
     *
     * @param nextVertex
     */
    fun setNextVertex(nextVertex: Vec2) {
        this.nextVertex.set(nextVertex)
        hasNextVertex = true
    }
}
