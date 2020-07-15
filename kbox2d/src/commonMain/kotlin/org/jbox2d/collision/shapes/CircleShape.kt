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
 * A circle shape.
 */
class CircleShape : Shape(ShapeType.CIRCLE) {
    companion object {
        operator fun invoke(radius: Number): CircleShape = CircleShape().also { it.m_radius = radius.toFloat() }
    }

    val m_p: Vec2 = Vec2()

    /**
     * Get the vertex count.
     *
     * @return
     */
    val vertexCount: Int get() = 1

    override fun clone(): Shape {
        val shape = CircleShape()
        shape.m_p.x = m_p.x
        shape.m_p.y = m_p.y
        shape.m_radius = m_radius
        return shape
    }

    override fun getChildCount(): Int = 1

    /**
     * Get the supporting vertex index in the given direction.
     *
     * @param d
     * @return
     */
    fun getSupport(d: Vec2): Int = 0

    /**
     * Get the supporting vertex in the given direction.
     *
     * @param d
     * @return
     */
    fun getSupportVertex(d: Vec2): Vec2 = m_p

    /**
     * Get a vertex by index.
     *
     * @param index
     * @return
     */
    fun getVertex(index: Int): Vec2 {
        assert(index == 0)
        return m_p
    }

    override fun testPoint(transform: Transform, p: Vec2): Boolean {
        // Rot.mulToOutUnsafe(transform.q, m_p, center);
        // center.addLocal(transform.p);
        //
        // final Vec2 d = center.subLocal(p).negateLocal();
        // return Vec2.dot(d, d) <= m_radius * m_radius;
        val q = transform.q
        val tp = transform.p
        val centerx = -(q.c * m_p.x - q.s * m_p.y + tp.x - p.x)
        val centery = -(q.s * m_p.x + q.c * m_p.y + tp.y - p.y)

        return centerx * centerx + centery * centery <= m_radius * m_radius
    }

    override fun computeDistanceToOut(xf: Transform, p: Vec2, childIndex: Int, normalOut: Vec2): Float {
        val xfq = xf.q
        val centerx = xfq.c * m_p.x - xfq.s * m_p.y + xf.p.x
        val centery = xfq.s * m_p.x + xfq.c * m_p.y + xf.p.y
        val dx = p.x - centerx
        val dy = p.y - centery
        val d1 = MathUtils.sqrt(dx * dx + dy * dy)
        normalOut.x = dx * 1 / d1
        normalOut.y = dy * 1 / d1
        return d1 - m_radius
    }

    // Collision Detection in Interactive 3D Environments by Gino van den Bergen
    // From Section 3.1.2
    // x = s + a * r
    // norm(x) = radius
    override fun raycast(
        output: RayCastOutput, input: RayCastInput,
        transform: Transform, childIndex: Int
    ): Boolean {

        val inputp1 = input.p1
        val inputp2 = input.p2
        val tq = transform.q
        val tp = transform.p

        // Rot.mulToOutUnsafe(transform.q, m_p, position);
        // position.addLocal(transform.p);
        val positionx = tq.c * m_p.x - tq.s * m_p.y + tp.x
        val positiony = tq.s * m_p.x + tq.c * m_p.y + tp.y

        val sx = inputp1.x - positionx
        val sy = inputp1.y - positiony
        // final float b = Vec2.dot(s, s) - m_radius * m_radius;
        val b = sx * sx + sy * sy - m_radius * m_radius

        // Solve quadratic equation.
        val rx = inputp2.x - inputp1.x
        val ry = inputp2.y - inputp1.y
        // final float c = Vec2.dot(s, r);
        // final float rr = Vec2.dot(r, r);
        val c = sx * rx + sy * ry
        val rr = rx * rx + ry * ry
        val sigma = c * c - rr * b

        // Check for negative discriminant and short segment.
        if (sigma < 0.0f || rr < Settings.EPSILON) {
            return false
        }

        // Find the point of intersection of the line with the circle.
        var a = -(c + MathUtils.sqrt(sigma))

        // Is the intersection point on the segment?
        if (0.0f <= a && a <= input.maxFraction * rr) {
            a /= rr
            output.fraction = a
            output.normal.x = rx * a + sx
            output.normal.y = ry * a + sy
            output.normal.normalize()
            return true
        }

        return false
    }

    override fun computeAABB(aabb: AABB, transform: Transform, childIndex: Int) {
        val tq = transform.q
        val tp = transform.p
        val px = tq.c * m_p.x - tq.s * m_p.y + tp.x
        val py = tq.s * m_p.x + tq.c * m_p.y + tp.y

        aabb.lowerBound.x = px - m_radius
        aabb.lowerBound.y = py - m_radius
        aabb.upperBound.x = px + m_radius
        aabb.upperBound.y = py + m_radius
    }

    override fun computeMass(massData: MassData, density: Float) {
        massData.mass = density * Settings.PI * m_radius * m_radius
        massData.center.x = m_p.x
        massData.center.y = m_p.y

        // inertia about the local origin
        // massData.I = massData.mass * (0.5f * m_radius * m_radius + Vec2.dot(m_p, m_p));
        massData.I = massData.mass * (0.5f * m_radius * m_radius + (m_p.x * m_p.x + m_p.y * m_p.y))
    }
}
