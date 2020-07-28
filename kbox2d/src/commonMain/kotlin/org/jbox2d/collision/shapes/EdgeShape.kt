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

/**
 * A line segment (edge) shape. These can be connected in chains or loops to other edge shapes. The
 * connectivity information is used to ensure correct contact normals.
 *
 * @author Daniel
 */
class EdgeShape() : Shape(ShapeType.EDGE) {

    /**
     * edge vertex 1
     */

    val vertex1 = Vec2()
    /**
     * edge vertex 2
     */

    val vertex2 = Vec2()

    /**
     * optional adjacent vertex 1. Used for smooth collision
     */

    val vertex0 = Vec2()
    /**
     * optional adjacent vertex 2. Used for smooth collision
     */

    val vertex3 = Vec2()

    var hasVertex0 = false

    var hasVertex3 = false

    // for pooling
    private val normal = Vec2()

    constructor(x1: Number, y1: Number, x2: Number, y2: Number) : this() {
        set(Vec2(x1.toFloat(), y1.toFloat()), Vec2(x2.toFloat(), y2.toFloat()))
    }

    init {
        radius = Settings.polygonRadius
    }

    override fun getChildCount(): Int {
        return 1
    }

    fun set(v1: Vec2, v2: Vec2) {
        vertex1.set(v1)
        vertex2.set(v2)
        hasVertex3 = false
        hasVertex0 = hasVertex3
    }

    override fun testPoint(xf: Transform, p: Vec2): Boolean {
        return false
    }

    override fun computeDistanceToOut(xf: Transform, p: Vec2, childIndex: Int, normalOut: Vec2): Float {
        val xfqc = xf.q.c
        val xfqs = xf.q.s
        val xfpx = xf.p.x
        val xfpy = xf.p.y
        val v1x = xfqc * vertex1.x - xfqs * vertex1.y + xfpx
        val v1y = xfqs * vertex1.x + xfqc * vertex1.y + xfpy
        val v2x = xfqc * vertex2.x - xfqs * vertex2.y + xfpx
        val v2y = xfqs * vertex2.x + xfqc * vertex2.y + xfpy

        var dx = p.x - v1x
        var dy = p.y - v1y
        val sx = v2x - v1x
        val sy = v2y - v1y
        val ds = dx * sx + dy * sy
        if (ds > 0) {
            val s2 = sx * sx + sy * sy
            if (ds > s2) {
                dx = p.x - v2x
                dy = p.y - v2y
            } else {
                dx -= ds / s2 * sx
                dy -= ds / s2 * sy
            }
        }

        val d1 = MathUtils.sqrt(dx * dx + dy * dy)
        if (d1 > 0) {
            normalOut.x = 1 / d1 * dx
            normalOut.y = 1 / d1 * dy
        } else {
            normalOut.x = 0f
            normalOut.y = 0f
        }
        return d1
    }

    // p = p1 + t * d
    // v = v1 + s * e
    // p1 + t * d = v1 + s * e
    // s * e - t * d = p1 - v1
    override fun raycast(output: RayCastOutput, input: RayCastInput, xf: Transform, childIndex: Int): Boolean {

        var tempx: Float
        var tempy: Float
        val v1 = vertex1
        val v2 = vertex2
        val xfq = xf.q
        val xfp = xf.p

        // Put the ray into the edge's frame of reference.
        // b2Vec2 p1 = b2MulT(xf.q, input.p1 - xf.p);
        // b2Vec2 p2 = b2MulT(xf.q, input.p2 - xf.p);
        tempx = input.p1.x - xfp.x
        tempy = input.p1.y - xfp.y
        val p1x = xfq.c * tempx + xfq.s * tempy
        val p1y = -xfq.s * tempx + xfq.c * tempy

        tempx = input.p2.x - xfp.x
        tempy = input.p2.y - xfp.y
        val p2x = xfq.c * tempx + xfq.s * tempy
        val p2y = -xfq.s * tempx + xfq.c * tempy

        val dx = p2x - p1x
        val dy = p2y - p1y

        // final Vec2 normal = pool2.set(v2).subLocal(v1);
        // normal.set(normal.y, -normal.x);
        normal.x = v2.y - v1.y
        normal.y = v1.x - v2.x
        normal.normalize()
        val normalx = normal.x
        val normaly = normal.y

        // q = p1 + t * d
        // dot(normal, q - v1) = 0
        // dot(normal, p1 - v1) + t * dot(normal, d) = 0
        tempx = v1.x - p1x
        tempy = v1.y - p1y
        val numerator = normalx * tempx + normaly * tempy
        val denominator = normalx * dx + normaly * dy

        if (denominator == 0.0f) {
            return false
        }

        val t = numerator / denominator
        if (t < 0.0f || 1.0f < t) {
            return false
        }

        // Vec2 q = p1 + t * d;
        val qx = p1x + t * dx
        val qy = p1y + t * dy

        // q = v1 + s * r
        // s = dot(q - v1, r) / dot(r, r)
        // Vec2 r = v2 - v1;
        val rx = v2.x - v1.x
        val ry = v2.y - v1.y
        val rr = rx * rx + ry * ry
        if (rr == 0.0f) {
            return false
        }
        tempx = qx - v1.x
        tempy = qy - v1.y
        // float s = Vec2.dot(pool5, r) / rr;
        val s = (tempx * rx + tempy * ry) / rr
        if (s < 0.0f || 1.0f < s) {
            return false
        }

        output.fraction = t
        if (numerator > 0.0f) {
            // output.normal = -b2Mul(xf.q, normal);
            output.normal.x = -xfq.c * normal.x + xfq.s * normal.y
            output.normal.y = -xfq.s * normal.x - xfq.c * normal.y
        } else {
            // output->normal = b2Mul(xf.q, normal);
            output.normal.x = xfq.c * normal.x - xfq.s * normal.y
            output.normal.y = xfq.s * normal.x + xfq.c * normal.y
        }
        return true
    }

    override fun computeAABB(aabb: AABB, xf: Transform, childIndex: Int) {
        val lowerBound = aabb.lowerBound
        val upperBound = aabb.upperBound
        val xfq = xf.q

        val v1x = xfq.c * vertex1.x - xfq.s * vertex1.y + xf.p.x
        val v1y = xfq.s * vertex1.x + xfq.c * vertex1.y + xf.p.y
        val v2x = xfq.c * vertex2.x - xfq.s * vertex2.y + xf.p.x
        val v2y = xfq.s * vertex2.x + xfq.c * vertex2.y + xf.p.y

        lowerBound.x = if (v1x < v2x) v1x else v2x
        lowerBound.y = if (v1y < v2y) v1y else v2y
        upperBound.x = if (v1x > v2x) v1x else v2x
        upperBound.y = if (v1y > v2y) v1y else v2y

        lowerBound.x -= radius
        lowerBound.y -= radius
        upperBound.x += radius
        upperBound.y += radius
    }

    override fun computeMass(massData: MassData, density: Float) {
        massData.mass = 0.0f
        massData.center.set(vertex1).addLocal(vertex2).mulLocal(0.5f)
        massData.I = 0.0f
    }

    override fun clone(): Shape {
        val edge = EdgeShape()
        edge.radius = this.radius
        edge.hasVertex0 = this.hasVertex0
        edge.hasVertex3 = this.hasVertex3
        edge.vertex0.set(this.vertex0)
        edge.vertex1.set(this.vertex1)
        edge.vertex2.set(this.vertex2)
        edge.vertex3.set(this.vertex3)
        return edge
    }
}
