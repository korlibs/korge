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
package org.jbox2d.collision

import org.jbox2d.collision.Distance.SimplexCache
import org.jbox2d.collision.Manifold.ManifoldType
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

/**
 * Functions used for computing contact points, distance queries, and TOI queries. Collision methods
 * are non-static for pooling speed, retrieve a collision object from the [SingletonPool].
 * Should not be finalructed.
 *
 * @author Daniel Murphy
 */
class Collision(private val pool: IWorldPool) {

    private val input = DistanceInput()
    private val cache = SimplexCache()
    private val output = DistanceOutput()

    // djm pooling, and from above
    private val temp = Vec2()
    private val xf = Transform()
    private val n = Vec2()
    private val v1 = Vec2()

    private val results1 = EdgeResults()
    private val results2 = EdgeResults()
    private val incidentEdge = Array<ClipVertex>(2) { ClipVertex() }
    private val localTangent = Vec2()
    private val localNormal = Vec2()
    private val planePoint = Vec2()
    private val tangent = Vec2()
    private val v11 = Vec2()
    private val v12 = Vec2()
    private val clipPoints1 = Array<ClipVertex>(2) { ClipVertex() }
    private val clipPoints2 = Array<ClipVertex>(2) { ClipVertex() }

    private val Q = Vec2()
    private val e = Vec2()
    private val cf = ContactID()
    private val e1 = Vec2()
    private val P = Vec2()

    private val collider = EPCollider()

    /**
     * Determine if two generic shapes overlap.
     *
     * @param shapeA
     * @param shapeB
     * @param xfA
     * @param xfB
     * @return
     */
    fun testOverlap(shapeA: Shape, indexA: Int, shapeB: Shape, indexB: Int,
                    xfA: Transform, xfB: Transform): Boolean {
        input.proxyA.set(shapeA, indexA)
        input.proxyB.set(shapeB, indexB)
        input.transformA.set(xfA)
        input.transformB.set(xfB)
        input.useRadii = true

        cache.count = 0

        pool.distance.distance(output, cache, input)
        // djm note: anything significant about 10.0f?
        return output.distance < 10.0f * Settings.EPSILON
    }

    /**
     * Compute the collision manifold between two circles.
     *
     * @param manifold
     * @param circle1
     * @param xfA
     * @param circle2
     * @param xfB
     */
    fun collideCircles(manifold: Manifold, circle1: CircleShape,
                       xfA: Transform, circle2: CircleShape, xfB: Transform) {
        manifold.pointCount = 0
        // before inline:
        // Transform.mulToOut(xfA, circle1.m_p, pA);
        // Transform.mulToOut(xfB, circle2.m_p, pB);
        // d.set(pB).subLocal(pA);
        // float distSqr = d.x * d.x + d.y * d.y;

        // after inline:
        val circle1p = circle1.p
        val circle2p = circle2.p
        val pAx = xfA.q.c * circle1p.x - xfA.q.s * circle1p.y + xfA.p.x
        val pAy = xfA.q.s * circle1p.x + xfA.q.c * circle1p.y + xfA.p.y
        val pBx = xfB.q.c * circle2p.x - xfB.q.s * circle2p.y + xfB.p.x
        val pBy = xfB.q.s * circle2p.x + xfB.q.c * circle2p.y + xfB.p.y
        val dx = pBx - pAx
        val dy = pBy - pAy
        val distSqr = dx * dx + dy * dy
        // end inline

        val radius = circle1.radius + circle2.radius
        if (distSqr > radius * radius) {
            return
        }

        manifold.type = ManifoldType.CIRCLES
        manifold.localPoint.set(circle1p)
        manifold.localNormal.setZero()
        manifold.pointCount = 1

        manifold.points[0].localPoint.set(circle2p)
        manifold.points[0].id.zero()
    }

    // djm pooling, and from above

    /**
     * Compute the collision manifold between a polygon and a circle.
     *
     * @param manifold
     * @param polygon
     * @param xfA
     * @param circle
     * @param xfB
     */
    fun collidePolygonAndCircle(manifold: Manifold, polygon: PolygonShape,
                                xfA: Transform, circle: CircleShape, xfB: Transform) {
        manifold.pointCount = 0
        // Vec2 v = circle.m_p;

        // Compute circle position in the frame of the polygon.
        // before inline:
        // Transform.mulToOutUnsafe(xfB, circle.m_p, c);
        // Transform.mulTransToOut(xfA, c, cLocal);
        // final float cLocalx = cLocal.x;
        // final float cLocaly = cLocal.y;
        // after inline:
        val circlep = circle.p
        val xfBq = xfB.q
        val xfAq = xfA.q
        val cx = xfBq.c * circlep.x - xfBq.s * circlep.y + xfB.p.x
        val cy = xfBq.s * circlep.x + xfBq.c * circlep.y + xfB.p.y
        val px = cx - xfA.p.x
        val py = cy - xfA.p.y
        val cLocalx = xfAq.c * px + xfAq.s * py
        val cLocaly = -xfAq.s * px + xfAq.c * py
        // end inline

        // Find the min separating edge.
        var normalIndex = 0
        var separation = -Float.MAX_VALUE
        val radius = polygon.radius + circle.radius
        val vertexCount = polygon.count
        var s: Float
        val vertices = polygon.vertices
        val normals = polygon.normals

        for (i in 0 until vertexCount) {
            // before inline
            // temp.set(cLocal).subLocal(vertices[i]);
            // float s = Vec2.dot(normals[i], temp);
            // after inline
            val vertex = vertices[i]
            val tempx = cLocalx - vertex.x
            val tempy = cLocaly - vertex.y
            s = normals[i].x * tempx + normals[i].y * tempy


            if (s > radius) {
                // early out
                return
            }

            if (s > separation) {
                separation = s
                normalIndex = i
            }
        }

        // Vertices that subtend the incident face.
        val vertIndex1 = normalIndex
        val vertIndex2 = if (vertIndex1 + 1 < vertexCount) vertIndex1 + 1 else 0
        val v1 = vertices[vertIndex1]
        val v2 = vertices[vertIndex2]

        // If the center is inside the polygon ...
        if (separation < Settings.EPSILON) {
            manifold.pointCount = 1
            manifold.type = ManifoldType.FACE_A

            // before inline:
            // manifold.localNormal.set(normals[normalIndex]);
            // manifold.localPoint.set(v1).addLocal(v2).mulLocal(.5f);
            // manifold.points[0].localPoint.set(circle.m_p);
            // after inline:
            val normal = normals[normalIndex]
            manifold.localNormal.x = normal.x
            manifold.localNormal.y = normal.y
            manifold.localPoint.x = (v1.x + v2.x) * .5f
            manifold.localPoint.y = (v1.y + v2.y) * .5f
            val mpoint = manifold.points[0]
            mpoint.localPoint.x = circlep.x
            mpoint.localPoint.y = circlep.y
            mpoint.id.zero()
            // end inline

            return
        }

        // Compute barycentric coordinates
        // before inline:
        // temp.set(cLocal).subLocal(v1);
        // temp2.set(v2).subLocal(v1);
        // float u1 = Vec2.dot(temp, temp2);
        // temp.set(cLocal).subLocal(v2);
        // temp2.set(v1).subLocal(v2);
        // float u2 = Vec2.dot(temp, temp2);
        // after inline:
        val tempX = cLocalx - v1.x
        val tempY = cLocaly - v1.y
        val temp2X = v2.x - v1.x
        val temp2Y = v2.y - v1.y
        val u1 = tempX * temp2X + tempY * temp2Y

        val temp3X = cLocalx - v2.x
        val temp3Y = cLocaly - v2.y
        val temp4X = v1.x - v2.x
        val temp4Y = v1.y - v2.y
        val u2 = temp3X * temp4X + temp3Y * temp4Y
        // end inline

        if (u1 <= 0f) {
            // inlined
            val dx = cLocalx - v1.x
            val dy = cLocaly - v1.y
            if (dx * dx + dy * dy > radius * radius) {
                return
            }

            manifold.pointCount = 1
            manifold.type = ManifoldType.FACE_A
            // before inline:
            // manifold.localNormal.set(cLocal).subLocal(v1);
            // after inline:
            manifold.localNormal.x = cLocalx - v1.x
            manifold.localNormal.y = cLocaly - v1.y
            // end inline
            manifold.localNormal.normalize()
            manifold.localPoint.set(v1)
            manifold.points[0].localPoint.set(circlep)
            manifold.points[0].id.zero()
        } else if (u2 <= 0.0f) {
            // inlined
            val dx = cLocalx - v2.x
            val dy = cLocaly - v2.y
            if (dx * dx + dy * dy > radius * radius) {
                return
            }

            manifold.pointCount = 1
            manifold.type = ManifoldType.FACE_A
            // before inline:
            // manifold.localNormal.set(cLocal).subLocal(v2);
            // after inline:
            manifold.localNormal.x = cLocalx - v2.x
            manifold.localNormal.y = cLocaly - v2.y
            // end inline
            manifold.localNormal.normalize()
            manifold.localPoint.set(v2)
            manifold.points[0].localPoint.set(circlep)
            manifold.points[0].id.zero()
        } else {
            // Vec2 faceCenter = 0.5f * (v1 + v2);
            // (temp is faceCenter)
            // before inline:
            // temp.set(v1).addLocal(v2).mulLocal(.5f);
            //
            // temp2.set(cLocal).subLocal(temp);
            // separation = Vec2.dot(temp2, normals[vertIndex1]);
            // if (separation > radius) {
            // return;
            // }
            // after inline:
            val fcx = (v1.x + v2.x) * .5f
            val fcy = (v1.y + v2.y) * .5f

            val tx = cLocalx - fcx
            val ty = cLocaly - fcy
            val normal = normals[vertIndex1]
            separation = tx * normal.x + ty * normal.y
            if (separation > radius) {
                return
            }
            // end inline

            manifold.pointCount = 1
            manifold.type = ManifoldType.FACE_A
            manifold.localNormal.set(normals[vertIndex1])
            manifold.localPoint.x = fcx // (faceCenter)
            manifold.localPoint.y = fcy
            manifold.points[0].localPoint.set(circlep)
            manifold.points[0].id.zero()
        }
    }

    private val poolVec2 = Vec2()

    /**
     * Find the max separation between poly1 and poly2 using edge normals from poly1.
     *
     * @param edgeIndex
     * @param poly1
     * @param xf1
     * @param poly2
     * @param xf2
     * @return
     */
    fun findMaxSeparation(results: EdgeResults, poly1: PolygonShape,
                          xf1: Transform, poly2: PolygonShape, xf2: Transform) {
        val count1 = poly1.count
        val count2 = poly2.count
        val n1s = poly1.normals
        val v1s = poly1.vertices
        val v2s = poly2.vertices

        Transform.mulTransToOutUnsafe(xf2, xf1, xf, poolVec2)
        val xfq = xf.q

        var bestIndex = 0
        var maxSeparation = -Float.MAX_VALUE
        for (i in 0 until count1) {
            // Get poly1 normal in frame2.
            Rot.mulToOutUnsafe(xfq, n1s[i], n)
            Transform.mulToOutUnsafe(xf, v1s[i], v1)

            // Find deepest point for normal i.
            var si = Float.MAX_VALUE
            for (j in 0 until count2) {
                val v2sj = v2s[j]
                val sij = n.x * (v2sj.x - v1.x) + n.y * (v2sj.y - v1.y)
                if (sij < si) {
                    si = sij
                }
            }

            if (si > maxSeparation) {
                maxSeparation = si
                bestIndex = i
            }
        }

        results.edgeIndex = bestIndex
        results.separation = maxSeparation
    }

    fun findIncidentEdge(c: Array<ClipVertex>, poly1: PolygonShape,
                         xf1: Transform, edge1: Int, poly2: PolygonShape, xf2: Transform) {
        val count1 = poly1.count
        val normals1 = poly1.normals

        val count2 = poly2.count
        val vertices2 = poly2.vertices
        val normals2 = poly2.normals

        assert(0 <= edge1 && edge1 < count1)

        val c0 = c[0]
        val c1 = c[1]
        val xf1q = xf1.q
        val xf2q = xf2.q

        // Get the normal of the reference edge in poly2's frame.
        // Vec2 normal1 = MulT(xf2.R, Mul(xf1.R, normals1[edge1]));
        // before inline:
        // Rot.mulToOutUnsafe(xf1.q, normals1[edge1], normal1); // temporary
        // Rot.mulTrans(xf2.q, normal1, normal1);
        // after inline:
        val v = normals1[edge1]
        val tempx = xf1q.c * v.x - xf1q.s * v.y
        val tempy = xf1q.s * v.x + xf1q.c * v.y
        val normal1x = xf2q.c * tempx + xf2q.s * tempy
        val normal1y = -xf2q.s * tempx + xf2q.c * tempy

        // end inline

        // Find the incident edge on poly2.
        var index = 0
        var minDot = Float.MAX_VALUE
        for (i in 0 until count2) {
            val b = normals2[i]
            val dot = normal1x * b.x + normal1y * b.y
            if (dot < minDot) {
                minDot = dot
                index = i
            }
        }

        // Build the clip vertices for the incident edge.
        val i1 = index
        val i2 = if (i1 + 1 < count2) i1 + 1 else 0

        // c0.v = Mul(xf2, vertices2[i1]);
        val v1 = vertices2[i1]
        val out = c0.v
        out.x = xf2q.c * v1.x - xf2q.s * v1.y + xf2.p.x
        out.y = xf2q.s * v1.x + xf2q.c * v1.y + xf2.p.y
        c0.id.indexA = edge1.toByte()
        c0.id.indexB = i1.toByte()
        c0.id.typeA = ContactID.Type.FACE.ordinal.toByte()
        c0.id.typeB = ContactID.Type.VERTEX.ordinal.toByte()

        // c1.v = Mul(xf2, vertices2[i2]);
        val v2 = vertices2[i2]
        val out1 = c1.v
        out1.x = xf2q.c * v2.x - xf2q.s * v2.y + xf2.p.x
        out1.y = xf2q.s * v2.x + xf2q.c * v2.y + xf2.p.y
        c1.id.indexA = edge1.toByte()
        c1.id.indexB = i2.toByte()
        c1.id.typeA = ContactID.Type.FACE.ordinal.toByte()
        c1.id.typeB = ContactID.Type.VERTEX.ordinal.toByte()
    }

    /**
     * Compute the collision manifold between two polygons.
     *
     * @param manifold
     * @param polygon1
     * @param xf1
     * @param polygon2
     * @param xf2
     */
    fun collidePolygons(manifold: Manifold, polyA: PolygonShape,
                        xfA: Transform, polyB: PolygonShape, xfB: Transform) {
        // Find edge normal of max separation on A - return if separating axis is found
        // Find edge normal of max separation on B - return if separation axis is found
        // Choose reference edge as min(minA, minB)
        // Find incident edge
        // Clip

        // The normal points from 1 to 2

        manifold.pointCount = 0
        val totalRadius = polyA.radius + polyB.radius

        findMaxSeparation(results1, polyA, xfA, polyB, xfB)
        if (results1.separation > totalRadius) {
            return
        }

        findMaxSeparation(results2, polyB, xfB, polyA, xfA)
        if (results2.separation > totalRadius) {
            return
        }

        val poly1: PolygonShape  // reference polygon
        val poly2: PolygonShape  // incident polygon
        val xf1: Transform
        val xf2: Transform
        val edge1: Int                 // reference edge
        val flip: Boolean
        val k_tol = 0.1f * Settings.linearSlop

        if (results2.separation > results1.separation + k_tol) {
            poly1 = polyB
            poly2 = polyA
            xf1 = xfB
            xf2 = xfA
            edge1 = results2.edgeIndex
            manifold.type = ManifoldType.FACE_B
            flip = true
        } else {
            poly1 = polyA
            poly2 = polyB
            xf1 = xfA
            xf2 = xfB
            edge1 = results1.edgeIndex
            manifold.type = ManifoldType.FACE_A
            flip = false
        }
        val xf1q = xf1.q

        findIncidentEdge(incidentEdge, poly1, xf1, edge1, poly2, xf2)

        val count1 = poly1.count
        val vertices1 = poly1.vertices

        val iv1 = edge1
        val iv2 = if (edge1 + 1 < count1) edge1 + 1 else 0
        v11.set(vertices1[iv1])
        v12.set(vertices1[iv2])
        localTangent.x = v12.x - v11.x
        localTangent.y = v12.y - v11.y
        localTangent.normalize()

        // Vec2 localNormal = Vec2.cross(dv, 1.0f);
        localNormal.x = 1f * localTangent.y
        localNormal.y = -1f * localTangent.x

        // Vec2 planePoint = 0.5f * (v11+ v12);
        planePoint.x = (v11.x + v12.x) * .5f
        planePoint.y = (v11.y + v12.y) * .5f

        // Rot.mulToOutUnsafe(xf1.q, localTangent, tangent);
        tangent.x = xf1q.c * localTangent.x - xf1q.s * localTangent.y
        tangent.y = xf1q.s * localTangent.x + xf1q.c * localTangent.y

        // Vec2.crossToOutUnsafe(tangent, 1f, normal);
        val normalx = 1f * tangent.y
        val normaly = -1f * tangent.x


        Transform.mulToOut(xf1, v11, v11)
        Transform.mulToOut(xf1, v12, v12)
        // v11 = Mul(xf1, v11);
        // v12 = Mul(xf1, v12);

        // Face offset
        // float frontOffset = Vec2.dot(normal, v11);
        val frontOffset = normalx * v11.x + normaly * v11.y

        // Side offsets, extended by polytope skin thickness.
        // float sideOffset1 = -Vec2.dot(tangent, v11) + totalRadius;
        // float sideOffset2 = Vec2.dot(tangent, v12) + totalRadius;
        val sideOffset1 = -(tangent.x * v11.x + tangent.y * v11.y) + totalRadius
        val sideOffset2 = tangent.x * v12.x + tangent.y * v12.y + totalRadius

        // Clip incident edge against extruded edge1 side edges.
        // ClipVertex clipPoints1[2];
        // ClipVertex clipPoints2[2];
        var np: Int

        // Clip to box side 1
        // np = ClipSegmentToLine(clipPoints1, incidentEdge, -sideNormal, sideOffset1);
        tangent.negateLocal()
        np = clipSegmentToLine(clipPoints1, incidentEdge, tangent, sideOffset1, iv1)
        tangent.negateLocal()

        if (np < 2) {
            return
        }

        // Clip to negative box side 1
        np = clipSegmentToLine(clipPoints2, clipPoints1, tangent, sideOffset2, iv2)

        if (np < 2) {
            return
        }

        // Now clipPoints2 contains the clipped points.
        manifold.localNormal.set(localNormal)
        manifold.localPoint.set(planePoint)

        var pointCount = 0
        for (i in 0 until Settings.maxManifoldPoints) {
            // float separation = Vec2.dot(normal, clipPoints2[i].v) - frontOffset;
            val separation = normalx * clipPoints2[i].v.x + normaly * clipPoints2[i].v.y - frontOffset

            if (separation <= totalRadius) {
                val cp = manifold.points[pointCount]
                // cp.m_localPoint = MulT(xf2, clipPoints2[i].v);
                val out = cp.localPoint
                val px = clipPoints2[i].v.x - xf2.p.x
                val py = clipPoints2[i].v.y - xf2.p.y
                out.x = xf2.q.c * px + xf2.q.s * py
                out.y = -xf2.q.s * px + xf2.q.c * py
                cp.id.set(clipPoints2[i].id)
                if (flip) {
                    // Swap features
                    cp.id.flip()
                }
                ++pointCount
            }
        }

        manifold.pointCount = pointCount
    }

    // Compute contact points for edge versus circle.
    // This accounts for edge connectivity.
    fun collideEdgeAndCircle(manifold: Manifold, edgeA: EdgeShape, xfA: Transform,
                             circleB: CircleShape, xfB: Transform) {
        manifold.pointCount = 0


        // Compute circle in frame of edge
        // Vec2 Q = MulT(xfA, Mul(xfB, circleB.m_p));
        Transform.mulToOutUnsafe(xfB, circleB.p, temp)
        Transform.mulTransToOutUnsafe(xfA, temp, Q)

        val A = edgeA.vertex1
        val B = edgeA.vertex2
        e.set(B).subLocal(A)

        // Barycentric coordinates
        val u = Vec2.dot(e, temp.set(B).subLocal(Q))
        val v = Vec2.dot(e, temp.set(Q).subLocal(A))

        val radius = edgeA.radius + circleB.radius

        // ContactFeature cf;
        cf.indexB = 0
        cf.typeB = ContactID.Type.VERTEX.ordinal.toByte()

        // Region A
        if (v <= 0.0f) {
            val P = A
            d.set(Q).subLocal(P)
            val dd = Vec2.dot(d, d)
            if (dd > radius * radius) {
                return
            }

            // Is there an edge connected to A?
            if (edgeA.hasVertex0) {
                val A1 = edgeA.vertex0
                val B1 = A
                e1.set(B1).subLocal(A1)
                val u1 = Vec2.dot(e1, temp.set(B1).subLocal(Q))

                // Is the circle in Region AB of the previous edge?
                if (u1 > 0.0f) {
                    return
                }
            }

            cf.indexA = 0
            cf.typeA = ContactID.Type.VERTEX.ordinal.toByte()
            manifold.pointCount = 1
            manifold.type = Manifold.ManifoldType.CIRCLES
            manifold.localNormal.setZero()
            manifold.localPoint.set(P)
            // manifold.points[0].id.key = 0;
            manifold.points[0].id.set(cf)
            manifold.points[0].localPoint.set(circleB.p)
            return
        }

        // Region B
        if (u <= 0.0f) {
            val P = B
            d.set(Q).subLocal(P)
            val dd = Vec2.dot(d, d)
            if (dd > radius * radius) {
                return
            }

            // Is there an edge connected to B?
            if (edgeA.hasVertex3) {
                val B2 = edgeA.vertex3
                val A2 = B
                val e2 = e1
                e2.set(B2).subLocal(A2)
                val v2 = Vec2.dot(e2, temp.set(Q).subLocal(A2))

                // Is the circle in Region AB of the next edge?
                if (v2 > 0.0f) {
                    return
                }
            }

            cf.indexA = 1
            cf.typeA = ContactID.Type.VERTEX.ordinal.toByte()
            manifold.pointCount = 1
            manifold.type = Manifold.ManifoldType.CIRCLES
            manifold.localNormal.setZero()
            manifold.localPoint.set(P)
            // manifold.points[0].id.key = 0;
            manifold.points[0].id.set(cf)
            manifold.points[0].localPoint.set(circleB.p)
            return
        }

        // Region AB
        val den = Vec2.dot(e, e)
        assert(den > 0.0f)

        // Vec2 P = (1.0f / den) * (u * A + v * B);
        P.set(A).mulLocal(u).addLocal(temp.set(B).mulLocal(v))
        P.mulLocal(1.0f / den)
        d.set(Q).subLocal(P)
        val dd = Vec2.dot(d, d)
        if (dd > radius * radius) {
            return
        }

        n.x = -e.y
        n.y = e.x
        if (Vec2.dot(n, temp.set(Q).subLocal(A)) < 0.0f) {
            n.set(-n.x, -n.y)
        }
        n.normalize()

        cf.indexA = 0
        cf.typeA = ContactID.Type.FACE.ordinal.toByte()
        manifold.pointCount = 1
        manifold.type = Manifold.ManifoldType.FACE_A
        manifold.localNormal.set(n)
        manifold.localPoint.set(A)
        // manifold.points[0].id.key = 0;
        manifold.points[0].id.set(cf)
        manifold.points[0].localPoint.set(circleB.p)
    }

    fun collideEdgeAndPolygon(manifold: Manifold, edgeA: EdgeShape, xfA: Transform,
                              polygonB: PolygonShape, xfB: Transform) {
        collider.collide(manifold, edgeA, xfA, polygonB, xfB)
    }


    /**
     * Java-specific class for returning edge results
     */
    class EdgeResults {
        var separation: Float = 0.toFloat()
        var edgeIndex: Int = 0
    }

    /**
     * Used for computing contact manifolds.
     */
    class ClipVertex {
        val v: Vec2 = Vec2()
        val id: ContactID = ContactID()

        fun set(cv: ClipVertex) {
            val v1 = cv.v
            v.x = v1.x
            v.y = v1.y
            val c = cv.id
            id.indexA = c.indexA
            id.indexB = c.indexB
            id.typeA = c.typeA
            id.typeB = c.typeB
        }
    }

    /**
     * This is used for determining the state of contact points.
     *
     * @author Daniel Murphy
     */
    enum class PointState {
        /**
         * point does not exist
         */
        NULL_STATE,
        /**
         * point was added in the update
         */
        ADD_STATE,
        /**
         * point persisted across the update
         */
        PERSIST_STATE,
        /**
         * point was removed in the update
         */
        REMOVE_STATE
    }

    /**
     * This structure is used to keep track of the best separating axis.
     */
    internal class EPAxis {

        var type: Type? = null
        var index: Int = 0
        var separation: Float = 0.toFloat()

        internal enum class Type {
            UNKNOWN, EDGE_A, EDGE_B
        }
    }

    /**
     * This holds polygon B expressed in frame A.
     */
    internal class TempPolygon {
        val vertices = Array<Vec2>(Settings.maxPolygonVertices) { Vec2() }
        val normals = Array<Vec2>(Settings.maxPolygonVertices) { Vec2() }
        var count: Int = 0
    }

    /**
     * Reference face used for clipping
     */
    internal class ReferenceFace {
        var i1: Int = 0
        var i2: Int = 0
        val v1 = Vec2()
        val v2 = Vec2()
        val normal = Vec2()

        val sideNormal1 = Vec2()
        var sideOffset1: Float = 0.toFloat()

        val sideNormal2 = Vec2()
        var sideOffset2: Float = 0.toFloat()
    }

    /**
     * This class collides and edge and a polygon, taking into account edge adjacency.
     */
    internal class EPCollider {

        val m_polygonB = TempPolygon()

        val m_xf = Transform()
        val m_centroidB = Vec2()
        var m_v0 = Vec2()
        var m_v1 = Vec2()
        var m_v2 = Vec2()
        var m_v3 = Vec2()
        val m_normal0 = Vec2()
        val m_normal1 = Vec2()
        val m_normal2 = Vec2()
        val m_normal = Vec2()

        var m_type1: VertexType? = null
        var m_type2: VertexType? = null

        val m_lowerLimit = Vec2()
        val m_upperLimit = Vec2()
        var m_radius: Float = 0.toFloat()
        var m_front: Boolean = false

        private val edge1 = Vec2()
        private val temp = Vec2()
        private val edge0 = Vec2()
        private val edge2 = Vec2()
        private val ie = Array<ClipVertex>(2) { ClipVertex() }
        private val clipPoints1 = Array<ClipVertex>(2) { ClipVertex() }
        private val clipPoints2 = Array<ClipVertex>(2) { ClipVertex() }
        private val rf = ReferenceFace()
        private val edgeAxis = EPAxis()
        private val polygonAxis = EPAxis()

        private val perp = Vec2()
        private val n = Vec2()

        internal enum class VertexType {
            ISOLATED, CONCAVE, CONVEX
        }

        private val poolVec2 = Vec2()

        fun collide(manifold: Manifold, edgeA: EdgeShape, xfA: Transform,
                    polygonB: PolygonShape, xfB: Transform) {

            Transform.mulTransToOutUnsafe(xfA, xfB, m_xf, poolVec2)
            Transform.mulToOutUnsafe(m_xf, polygonB.centroid, m_centroidB)

            m_v0 = edgeA.vertex0
            m_v1 = edgeA.vertex1
            m_v2 = edgeA.vertex2
            m_v3 = edgeA.vertex3

            val hasVertex0 = edgeA.hasVertex0
            val hasVertex3 = edgeA.hasVertex3

            edge1.set(m_v2).subLocal(m_v1)
            edge1.normalize()
            m_normal1.set(edge1.y, -edge1.x)
            val offset1 = Vec2.dot(m_normal1, temp.set(m_centroidB).subLocal(m_v1))
            var offset0 = 0.0f
            var offset2 = 0.0f
            var convex1 = false
            var convex2 = false

            // Is there a preceding edge?
            if (hasVertex0) {
                edge0.set(m_v1).subLocal(m_v0)
                edge0.normalize()
                m_normal0.set(edge0.y, -edge0.x)
                convex1 = Vec2.cross(edge0, edge1) >= 0.0f
                offset0 = Vec2.dot(m_normal0, temp.set(m_centroidB).subLocal(m_v0))
            }

            // Is there a following edge?
            if (hasVertex3) {
                edge2.set(m_v3).subLocal(m_v2)
                edge2.normalize()
                m_normal2.set(edge2.y, -edge2.x)
                convex2 = Vec2.cross(edge1, edge2) > 0.0f
                offset2 = Vec2.dot(m_normal2, temp.set(m_centroidB).subLocal(m_v2))
            }

            // Determine front or back collision. Determine collision normal limits.
            if (hasVertex0 && hasVertex3) {
                if (convex1 && convex2) {
                    m_front = offset0 >= 0.0f || offset1 >= 0.0f || offset2 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal0.x
                        m_lowerLimit.y = m_normal0.y
                        m_upperLimit.x = m_normal2.x
                        m_upperLimit.y = m_normal2.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal1.x
                        m_lowerLimit.y = -m_normal1.y
                        m_upperLimit.x = -m_normal1.x
                        m_upperLimit.y = -m_normal1.y
                    }
                } else if (convex1) {
                    m_front = offset0 >= 0.0f || offset1 >= 0.0f && offset2 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal0.x
                        m_lowerLimit.y = m_normal0.y
                        m_upperLimit.x = m_normal1.x
                        m_upperLimit.y = m_normal1.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal2.x
                        m_lowerLimit.y = -m_normal2.y
                        m_upperLimit.x = -m_normal1.x
                        m_upperLimit.y = -m_normal1.y
                    }
                } else if (convex2) {
                    m_front = offset2 >= 0.0f || offset0 >= 0.0f && offset1 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal1.x
                        m_lowerLimit.y = m_normal1.y
                        m_upperLimit.x = m_normal2.x
                        m_upperLimit.y = m_normal2.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal1.x
                        m_lowerLimit.y = -m_normal1.y
                        m_upperLimit.x = -m_normal0.x
                        m_upperLimit.y = -m_normal0.y
                    }
                } else {
                    m_front = offset0 >= 0.0f && offset1 >= 0.0f && offset2 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal1.x
                        m_lowerLimit.y = m_normal1.y
                        m_upperLimit.x = m_normal1.x
                        m_upperLimit.y = m_normal1.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal2.x
                        m_lowerLimit.y = -m_normal2.y
                        m_upperLimit.x = -m_normal0.x
                        m_upperLimit.y = -m_normal0.y
                    }
                }
            } else if (hasVertex0) {
                if (convex1) {
                    m_front = offset0 >= 0.0f || offset1 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal0.x
                        m_lowerLimit.y = m_normal0.y
                        m_upperLimit.x = -m_normal1.x
                        m_upperLimit.y = -m_normal1.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = m_normal1.x
                        m_lowerLimit.y = m_normal1.y
                        m_upperLimit.x = -m_normal1.x
                        m_upperLimit.y = -m_normal1.y
                    }
                } else {
                    m_front = offset0 >= 0.0f && offset1 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = m_normal1.x
                        m_lowerLimit.y = m_normal1.y
                        m_upperLimit.x = -m_normal1.x
                        m_upperLimit.y = -m_normal1.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = m_normal1.x
                        m_lowerLimit.y = m_normal1.y
                        m_upperLimit.x = -m_normal0.x
                        m_upperLimit.y = -m_normal0.y
                    }
                }
            } else if (hasVertex3) {
                if (convex2) {
                    m_front = offset1 >= 0.0f || offset2 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = -m_normal1.x
                        m_lowerLimit.y = -m_normal1.y
                        m_upperLimit.x = m_normal2.x
                        m_upperLimit.y = m_normal2.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal1.x
                        m_lowerLimit.y = -m_normal1.y
                        m_upperLimit.x = m_normal1.x
                        m_upperLimit.y = m_normal1.y
                    }
                } else {
                    m_front = offset1 >= 0.0f && offset2 >= 0.0f
                    if (m_front) {
                        m_normal.x = m_normal1.x
                        m_normal.y = m_normal1.y
                        m_lowerLimit.x = -m_normal1.x
                        m_lowerLimit.y = -m_normal1.y
                        m_upperLimit.x = m_normal1.x
                        m_upperLimit.y = m_normal1.y
                    } else {
                        m_normal.x = -m_normal1.x
                        m_normal.y = -m_normal1.y
                        m_lowerLimit.x = -m_normal2.x
                        m_lowerLimit.y = -m_normal2.y
                        m_upperLimit.x = m_normal1.x
                        m_upperLimit.y = m_normal1.y
                    }
                }
            } else {
                m_front = offset1 >= 0.0f
                if (m_front) {
                    m_normal.x = m_normal1.x
                    m_normal.y = m_normal1.y
                    m_lowerLimit.x = -m_normal1.x
                    m_lowerLimit.y = -m_normal1.y
                    m_upperLimit.x = -m_normal1.x
                    m_upperLimit.y = -m_normal1.y
                } else {
                    m_normal.x = -m_normal1.x
                    m_normal.y = -m_normal1.y
                    m_lowerLimit.x = m_normal1.x
                    m_lowerLimit.y = m_normal1.y
                    m_upperLimit.x = m_normal1.x
                    m_upperLimit.y = m_normal1.y
                }
            }

            // Get polygonB in frameA
            m_polygonB.count = polygonB.count
            for (i in 0 until polygonB.count) {
                Transform.mulToOutUnsafe(m_xf, polygonB.vertices[i], m_polygonB.vertices[i])
                Rot.mulToOutUnsafe(m_xf.q, polygonB.normals[i], m_polygonB.normals[i])
            }

            m_radius = 2.0f * Settings.polygonRadius

            manifold.pointCount = 0

            computeEdgeSeparation(edgeAxis)

            // If no valid normal can be found than this edge should not collide.
            if (edgeAxis.type == EPAxis.Type.UNKNOWN) {
                return
            }

            if (edgeAxis.separation > m_radius) {
                return
            }

            computePolygonSeparation(polygonAxis)
            if (polygonAxis.type != EPAxis.Type.UNKNOWN && polygonAxis.separation > m_radius) {
                return
            }

            // Use hysteresis for jitter reduction.
            val k_relativeTol = 0.98f
            val k_absoluteTol = 0.001f

            val primaryAxis: EPAxis
            if (polygonAxis.type == EPAxis.Type.UNKNOWN) {
                primaryAxis = edgeAxis
            } else if (polygonAxis.separation > k_relativeTol * edgeAxis.separation + k_absoluteTol) {
                primaryAxis = polygonAxis
            } else {
                primaryAxis = edgeAxis
            }

            val ie0 = ie[0]
            val ie1 = ie[1]

            if (primaryAxis.type == EPAxis.Type.EDGE_A) {
                manifold.type = Manifold.ManifoldType.FACE_A

                // Search for the polygon normal that is most anti-parallel to the edge normal.
                var bestIndex = 0
                var bestValue = Vec2.dot(m_normal, m_polygonB.normals[0])
                for (i in 1 until m_polygonB.count) {
                    val value = Vec2.dot(m_normal, m_polygonB.normals[i])
                    if (value < bestValue) {
                        bestValue = value
                        bestIndex = i
                    }
                }

                val i1 = bestIndex
                val i2 = if (i1 + 1 < m_polygonB.count) i1 + 1 else 0

                ie0.v.set(m_polygonB.vertices[i1])
                ie0.id.indexA = 0
                ie0.id.indexB = i1.toByte()
                ie0.id.typeA = ContactID.Type.FACE.ordinal.toByte()
                ie0.id.typeB = ContactID.Type.VERTEX.ordinal.toByte()

                ie1.v.set(m_polygonB.vertices[i2])
                ie1.id.indexA = 0
                ie1.id.indexB = i2.toByte()
                ie1.id.typeA = ContactID.Type.FACE.ordinal.toByte()
                ie1.id.typeB = ContactID.Type.VERTEX.ordinal.toByte()

                if (m_front) {
                    rf.i1 = 0
                    rf.i2 = 1
                    rf.v1.set(m_v1)
                    rf.v2.set(m_v2)
                    rf.normal.set(m_normal1)
                } else {
                    rf.i1 = 1
                    rf.i2 = 0
                    rf.v1.set(m_v2)
                    rf.v2.set(m_v1)
                    rf.normal.set(m_normal1).negateLocal()
                }
            } else {
                manifold.type = Manifold.ManifoldType.FACE_B

                ie0.v.set(m_v1)
                ie0.id.indexA = 0
                ie0.id.indexB = primaryAxis.index.toByte()
                ie0.id.typeA = ContactID.Type.VERTEX.ordinal.toByte()
                ie0.id.typeB = ContactID.Type.FACE.ordinal.toByte()

                ie1.v.set(m_v2)
                ie1.id.indexA = 0
                ie1.id.indexB = primaryAxis.index.toByte()
                ie1.id.typeA = ContactID.Type.VERTEX.ordinal.toByte()
                ie1.id.typeB = ContactID.Type.FACE.ordinal.toByte()

                rf.i1 = primaryAxis.index
                rf.i2 = if (rf.i1 + 1 < m_polygonB.count) rf.i1 + 1 else 0
                rf.v1.set(m_polygonB.vertices[rf.i1])
                rf.v2.set(m_polygonB.vertices[rf.i2])
                rf.normal.set(m_polygonB.normals[rf.i1])
            }

            rf.sideNormal1.set(rf.normal.y, -rf.normal.x)
            rf.sideNormal2.set(rf.sideNormal1).negateLocal()
            rf.sideOffset1 = Vec2.dot(rf.sideNormal1, rf.v1)
            rf.sideOffset2 = Vec2.dot(rf.sideNormal2, rf.v2)

            // Clip incident edge against extruded edge1 side edges.
            var np: Int

            // Clip to box side 1
            np = clipSegmentToLine(clipPoints1, ie, rf.sideNormal1, rf.sideOffset1, rf.i1)

            if (np < Settings.maxManifoldPoints) {
                return
            }

            // Clip to negative box side 1
            np = clipSegmentToLine(clipPoints2, clipPoints1, rf.sideNormal2, rf.sideOffset2, rf.i2)

            if (np < Settings.maxManifoldPoints) {
                return
            }

            // Now clipPoints2 contains the clipped points.
            if (primaryAxis.type == EPAxis.Type.EDGE_A) {
                manifold.localNormal.set(rf.normal)
                manifold.localPoint.set(rf.v1)
            } else {
                manifold.localNormal.set(polygonB.normals[rf.i1])
                manifold.localPoint.set(polygonB.vertices[rf.i1])
            }

            var pointCount = 0
            for (i in 0 until Settings.maxManifoldPoints) {
                val separation: Float

                separation = Vec2.dot(rf.normal, temp.set(clipPoints2[i].v).subLocal(rf.v1))

                if (separation <= m_radius) {
                    val cp = manifold.points[pointCount]

                    if (primaryAxis.type == EPAxis.Type.EDGE_A) {
                        // cp.localPoint = MulT(m_xf, clipPoints2[i].v);
                        Transform.mulTransToOutUnsafe(m_xf, clipPoints2[i].v, cp.localPoint)
                        cp.id.set(clipPoints2[i].id)
                    } else {
                        cp.localPoint.set(clipPoints2[i].v)
                        cp.id.typeA = clipPoints2[i].id.typeB
                        cp.id.typeB = clipPoints2[i].id.typeA
                        cp.id.indexA = clipPoints2[i].id.indexB
                        cp.id.indexB = clipPoints2[i].id.indexA
                    }

                    ++pointCount
                }
            }

            manifold.pointCount = pointCount
        }


        fun computeEdgeSeparation(axis: EPAxis) {
            axis.type = EPAxis.Type.EDGE_A
            axis.index = if (m_front) 0 else 1
            axis.separation = Float.MAX_VALUE
            val nx = m_normal.x
            val ny = m_normal.y

            for (i in 0 until m_polygonB.count) {
                val v = m_polygonB.vertices[i]
                val tempx = v.x - m_v1.x
                val tempy = v.y - m_v1.y
                val s = nx * tempx + ny * tempy
                if (s < axis.separation) {
                    axis.separation = s
                }
            }
        }

        fun computePolygonSeparation(axis: EPAxis) {
            axis.type = EPAxis.Type.UNKNOWN
            axis.index = -1
            axis.separation = -Float.MAX_VALUE

            perp.x = -m_normal.y
            perp.y = m_normal.x

            for (i in 0 until m_polygonB.count) {
                val normalB = m_polygonB.normals[i]
                val vB = m_polygonB.vertices[i]
                n.x = -normalB.x
                n.y = -normalB.y

                // float s1 = Vec2.dot(n, temp.set(vB).subLocal(m_v1));
                // float s2 = Vec2.dot(n, temp.set(vB).subLocal(m_v2));
                var tempx = vB.x - m_v1.x
                var tempy = vB.y - m_v1.y
                val s1 = n.x * tempx + n.y * tempy
                tempx = vB.x - m_v2.x
                tempy = vB.y - m_v2.y
                val s2 = n.x * tempx + n.y * tempy
                val s = MathUtils.min(s1, s2)

                if (s > m_radius) {
                    // No collision
                    axis.type = EPAxis.Type.EDGE_B
                    axis.index = i
                    axis.separation = s
                    return
                }

                // Adjacency
                if (n.x * perp.x + n.y * perp.y >= 0.0f) {
                    if (Vec2.dot(temp.set(n).subLocal(m_upperLimit), m_normal) < -Settings.angularSlop) {
                        continue
                    }
                } else {
                    if (Vec2.dot(temp.set(n).subLocal(m_lowerLimit), m_normal) < -Settings.angularSlop) {
                        continue
                    }
                }

                if (s > axis.separation) {
                    axis.type = EPAxis.Type.EDGE_B
                    axis.index = i
                    axis.separation = s
                }
            }
        }
    }

    // #### COLLISION STUFF (not from collision.h or collision.cpp) ####

    // djm pooling
    private val d = Vec2()

    companion object {

        val NULL_FEATURE = Int.MAX_VALUE

        /**
         * Compute the point states given two manifolds. The states pertain to the transition from
         * manifold1 to manifold2. So state1 is either persist or remove while state2 is either add or
         * persist.
         *
         * @param state1
         * @param state2
         * @param manifold1
         * @param manifold2
         */

        fun getPointStates(state1: Array<PointState>, state2: Array<PointState>,
                           manifold1: Manifold, manifold2: Manifold) {

            for (i in 0 until Settings.maxManifoldPoints) {
                state1[i] = PointState.NULL_STATE
                state2[i] = PointState.NULL_STATE
            }

            // Detect persists and removes.
            for (i in 0 until manifold1.pointCount) {
                val id = manifold1.points[i].id

                state1[i] = PointState.REMOVE_STATE

                for (j in 0 until manifold2.pointCount) {
                    if (manifold2.points[j].id.isEqual(id)) {
                        state1[i] = PointState.PERSIST_STATE
                        break
                    }
                }
            }

            // Detect persists and adds
            for (i in 0 until manifold2.pointCount) {
                val id = manifold2.points[i].id

                state2[i] = PointState.ADD_STATE

                for (j in 0 until manifold1.pointCount) {
                    if (manifold1.points[j].id.isEqual(id)) {
                        state2[i] = PointState.PERSIST_STATE
                        break
                    }
                }
            }
        }

        /**
         * Clipping for contact manifolds. Sutherland-Hodgman clipping.
         *
         * @param vOut
         * @param vIn
         * @param normal
         * @param offset
         * @return
         */

        fun clipSegmentToLine(vOut: Array<ClipVertex>, vIn: Array<ClipVertex>,
                              normal: Vec2, offset: Float, vertexIndexA: Int): Int {

            // Start with no output points
            var numOut = 0
            val vIn0 = vIn[0]
            val vIn1 = vIn[1]
            val vIn0v = vIn0.v
            val vIn1v = vIn1.v

            // Calculate the distance of end points to the line
            val distance0 = Vec2.dot(normal, vIn0v) - offset
            val distance1 = Vec2.dot(normal, vIn1v) - offset

            // If the points are behind the plane
            if (distance0 <= 0.0f) {
                vOut[numOut++].set(vIn0)
            }
            if (distance1 <= 0.0f) {
                vOut[numOut++].set(vIn1)
            }

            // If the points are on different sides of the plane
            if (distance0 * distance1 < 0.0f) {
                // Find intersection point of edge and plane
                val interp = distance0 / (distance0 - distance1)

                val vOutNO = vOut[numOut]
                // vOut[numOut].v = vIn[0].v + interp * (vIn[1].v - vIn[0].v);
                vOutNO.v.x = vIn0v.x + interp * (vIn1v.x - vIn0v.x)
                vOutNO.v.y = vIn0v.y + interp * (vIn1v.y - vIn0v.y)

                // VertexA is hitting edgeB.
                vOutNO.id.indexA = vertexIndexA.toByte()
                vOutNO.id.indexB = vIn0.id.indexB
                vOutNO.id.typeA = ContactID.Type.VERTEX.ordinal.toByte()
                vOutNO.id.typeB = ContactID.Type.FACE.ordinal.toByte()
                ++numOut
            }

            return numOut
        }
    }
}
