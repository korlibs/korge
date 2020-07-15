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


    var m_vertices: Array<Vec2>? = null

    var m_count: Int = 0

    val m_prevVertex = Vec2()

    val m_nextVertex = Vec2()

    var m_hasPrevVertex = false

    var m_hasNextVertex = false

    private val pool0 = EdgeShape()

    init {
        m_vertices = null
        m_radius = Settings.polygonRadius
        m_count = 0
    }

    fun clear() {
        m_vertices = null
        m_count = 0
    }

    override fun getChildCount(): Int {
        return m_count - 1
    }

    /**
     * Get a child edge.
     */
    fun getChildEdge(edge: EdgeShape, index: Int) {
        assert(0 <= index && index < m_count - 1)
        edge.m_radius = m_radius

        val v0 = m_vertices!![index + 0]
        val v1 = m_vertices!![index + 1]
        edge.m_vertex1.x = v0.x
        edge.m_vertex1.y = v0.y
        edge.m_vertex2.x = v1.x
        edge.m_vertex2.y = v1.y

        if (index > 0) {
            val v = m_vertices!![index - 1]
            edge.m_vertex0.x = v.x
            edge.m_vertex0.y = v.y
            edge.m_hasVertex0 = true
        } else {
            edge.m_vertex0.x = m_prevVertex.x
            edge.m_vertex0.y = m_prevVertex.y
            edge.m_hasVertex0 = m_hasPrevVertex
        }

        if (index < m_count - 2) {
            val v = m_vertices!![index + 2]
            edge.m_vertex3.x = v.x
            edge.m_vertex3.y = v.y
            edge.m_hasVertex3 = true
        } else {
            edge.m_vertex3.x = m_nextVertex.x
            edge.m_vertex3.y = m_nextVertex.y
            edge.m_hasVertex3 = m_hasNextVertex
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
        assert(childIndex < m_count)

        val edgeShape = pool0

        val i1 = childIndex
        var i2 = childIndex + 1
        if (i2 == m_count) {
            i2 = 0
        }
        val v = m_vertices!![i1]
        edgeShape.m_vertex1.x = v.x
        edgeShape.m_vertex1.y = v.y
        val v1 = m_vertices!![i2]
        edgeShape.m_vertex2.x = v1.x
        edgeShape.m_vertex2.y = v1.y

        return edgeShape.raycast(output, input, xf, 0)
    }

    override fun computeAABB(aabb: AABB, xf: Transform, childIndex: Int) {
        assert(childIndex < m_count)
        val lower = aabb.lowerBound
        val upper = aabb.upperBound

        val i1 = childIndex
        var i2 = childIndex + 1
        if (i2 == m_count) {
            i2 = 0
        }

        val vi1 = m_vertices!![i1]
        val vi2 = m_vertices!![i2]
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
        clone.createChain(m_vertices, m_count)
        clone.m_prevVertex.set(m_prevVertex)
        clone.m_nextVertex.set(m_nextVertex)
        clone.m_hasPrevVertex = m_hasPrevVertex
        clone.m_hasNextVertex = m_hasNextVertex
        return clone
    }

    /**
     * Create a loop. This automatically adjusts connectivity.
     *
     * @param vertices an array of vertices, these are copied
     * @param count the vertex count
     */
    fun createLoop(vertices: Array<Vec2>, count: Int) {
        assert(m_vertices == null && m_count == 0)
        assert(count >= 3)
        m_count = count + 1
        m_vertices = Array(m_count) { Vec2.dummy }
        for (i in 1 until count) {
            val v1 = vertices[i - 1]
            val v2 = vertices[i]
            // If the code crashes here, it means your vertices are too close together.
            if (MathUtils.distanceSquared(v1, v2) < Settings.linearSlop * Settings.linearSlop) {
                throw RuntimeException("Vertices of chain shape are too close together")
            }
        }
        for (i in 0 until count) {
            m_vertices!![i] = Vec2(vertices[i])
        }
        m_vertices!![count] = Vec2(m_vertices!![0])
        m_prevVertex.set(m_vertices!![m_count - 2])
        m_nextVertex.set(m_vertices!![1])
        m_hasPrevVertex = true
        m_hasNextVertex = true
    }

    /**
     * Create a chain with isolated end vertices.
     *
     * @param vertices an array of vertices, these are copied
     * @param count the vertex count
     */
    fun createChain(vertices: Array<Vec2>?, count: Int) {
        assert(m_vertices == null && m_count == 0)
        assert(count >= 2)
        m_count = count
        m_vertices = Array(m_count) { Vec2.dummy }
        for (i in 1 until m_count) {
            val v1 = vertices!![i - 1]
            val v2 = vertices[i]
            // If the code crashes here, it means your vertices are too close together.
            if (MathUtils.distanceSquared(v1, v2) < Settings.linearSlop * Settings.linearSlop) {
                throw RuntimeException("Vertices of chain shape are too close together")
            }
        }
        for (i in 0 until m_count) {
            m_vertices!![i] = Vec2(vertices!![i])
        }
        m_hasPrevVertex = false
        m_hasNextVertex = false

        m_prevVertex.setZero()
        m_nextVertex.setZero()
    }

    /**
     * Establish connectivity to a vertex that precedes the first vertex. Don't call this for loops.
     *
     * @param prevVertex
     */
    fun setPrevVertex(prevVertex: Vec2) {
        m_prevVertex.set(prevVertex)
        m_hasPrevVertex = true
    }

    /**
     * Establish connectivity to a vertex that follows the last vertex. Don't call this for loops.
     *
     * @param nextVertex
     */
    fun setNextVertex(nextVertex: Vec2) {
        m_nextVertex.set(nextVertex)
        m_hasNextVertex = true
    }
}
