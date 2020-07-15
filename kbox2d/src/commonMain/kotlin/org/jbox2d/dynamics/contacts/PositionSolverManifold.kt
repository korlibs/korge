package org.jbox2d.dynamics.contacts

import org.jbox2d.collision.*
import org.jbox2d.common.Rot
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*

internal class PositionSolverManifold {

    val normal = Vec2()

    val point = Vec2()

    var separation: Float = 0.toFloat()

    fun initialize(pc: ContactPositionConstraint, xfA: Transform, xfB: Transform, index: Int) {
        assert(pc.pointCount > 0)

        val xfAq = xfA.q
        val xfBq = xfB.q
        val pcLocalPointsI = pc.localPoints[index]
        when (pc.type) {
            Manifold.ManifoldType.CIRCLES -> {
                // Transform.mulToOutUnsafe(xfA, pc.localPoint, pointA);
                // Transform.mulToOutUnsafe(xfB, pc.localPoints[0], pointB);
                // normal.set(pointB).subLocal(pointA);
                // normal.normalize();
                //
                // point.set(pointA).addLocal(pointB).mulLocal(.5f);
                // temp.set(pointB).subLocal(pointA);
                // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
                val plocalPoint = pc.localPoint
                val pLocalPoints0 = pc.localPoints[0]
                val pointAx = xfAq.c * plocalPoint.x - xfAq.s * plocalPoint.y + xfA.p.x
                val pointAy = xfAq.s * plocalPoint.x + xfAq.c * plocalPoint.y + xfA.p.y
                val pointBx = xfBq.c * pLocalPoints0.x - xfBq.s * pLocalPoints0.y + xfB.p.x
                val pointBy = xfBq.s * pLocalPoints0.x + xfBq.c * pLocalPoints0.y + xfB.p.y
                normal.x = pointBx - pointAx
                normal.y = pointBy - pointAy
                normal.normalize()

                point.x = (pointAx + pointBx) * .5f
                point.y = (pointAy + pointBy) * .5f
                val tempx = pointBx - pointAx
                val tempy = pointBy - pointAy
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
            }

            Manifold.ManifoldType.FACE_A -> {
                // Rot.mulToOutUnsafe(xfAq, pc.localNormal, normal);
                // Transform.mulToOutUnsafe(xfA, pc.localPoint, planePoint);
                //
                // Transform.mulToOutUnsafe(xfB, pc.localPoints[index], clipPoint);
                // temp.set(clipPoint).subLocal(planePoint);
                // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
                // point.set(clipPoint);
                val pcLocalNormal = pc.localNormal
                val pcLocalPoint = pc.localPoint
                normal.x = xfAq.c * pcLocalNormal.x - xfAq.s * pcLocalNormal.y
                normal.y = xfAq.s * pcLocalNormal.x + xfAq.c * pcLocalNormal.y
                val planePointx = xfAq.c * pcLocalPoint.x - xfAq.s * pcLocalPoint.y + xfA.p.x
                val planePointy = xfAq.s * pcLocalPoint.x + xfAq.c * pcLocalPoint.y + xfA.p.y

                val clipPointx = xfBq.c * pcLocalPointsI.x - xfBq.s * pcLocalPointsI.y + xfB.p.x
                val clipPointy = xfBq.s * pcLocalPointsI.x + xfBq.c * pcLocalPointsI.y + xfB.p.y
                val tempx = clipPointx - planePointx
                val tempy = clipPointy - planePointy
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
                point.x = clipPointx
                point.y = clipPointy
            }

            Manifold.ManifoldType.FACE_B -> {
                // Rot.mulToOutUnsafe(xfBq, pc.localNormal, normal);
                // Transform.mulToOutUnsafe(xfB, pc.localPoint, planePoint);
                //
                // Transform.mulToOutUnsafe(xfA, pcLocalPointsI, clipPoint);
                // temp.set(clipPoint).subLocal(planePoint);
                // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
                // point.set(clipPoint);
                //
                // // Ensure normal points from A to B
                // normal.negateLocal();
                val pcLocalNormal = pc.localNormal
                val pcLocalPoint = pc.localPoint
                normal.x = xfBq.c * pcLocalNormal.x - xfBq.s * pcLocalNormal.y
                normal.y = xfBq.s * pcLocalNormal.x + xfBq.c * pcLocalNormal.y
                val planePointx = xfBq.c * pcLocalPoint.x - xfBq.s * pcLocalPoint.y + xfB.p.x
                val planePointy = xfBq.s * pcLocalPoint.x + xfBq.c * pcLocalPoint.y + xfB.p.y

                val clipPointx = xfAq.c * pcLocalPointsI.x - xfAq.s * pcLocalPointsI.y + xfA.p.x
                val clipPointy = xfAq.s * pcLocalPointsI.x + xfAq.c * pcLocalPointsI.y + xfA.p.y
                val tempx = clipPointx - planePointx
                val tempy = clipPointy - planePointy
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
                point.x = clipPointx
                point.y = clipPointy
                normal.x *= -1f
                normal.y *= -1f
            }
        }
    }
}
