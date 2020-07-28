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
package org.jbox2d.dynamics.contacts

import org.jbox2d.collision.WorldManifold
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.TimeStep
import org.jbox2d.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint
import org.jbox2d.internal.*


/**
 * @author Daniel
 */
class ContactSolver {

    var m_step: TimeStep? = null
    var m_positions: Array<Position>? = null
    var m_velocities: Array<Velocity>? = null
    var m_positionConstraints: Array<ContactPositionConstraint> = Array(INITIAL_NUM_CONSTRAINTS) { ContactPositionConstraint() }
    var m_velocityConstraints: Array<ContactVelocityConstraint> = Array(INITIAL_NUM_CONSTRAINTS) { ContactVelocityConstraint() }
    var m_contacts: Array<Contact>? = null
    var m_count: Int = 0

    // djm pooling, and from above
    private val xfA = Transform()
    private val xfB = Transform()
    private val worldManifold = WorldManifold()

    /*
   * #if 0 // Sequential solver. bool ContactSolver::SolvePositionConstraints(float baumgarte) {
   * float minSeparation = 0.0f;
   *
   * for (int i = 0; i < m_constraintCount; ++i) { ContactConstraint* c = m_constraints + i; Body*
   * bodyA = c.bodyA; Body* bodyB = c.bodyB; float invMassA = bodyA.m_mass * bodyA.m_invMass; float
   * invIA = bodyA.m_mass * bodyA.m_invI; float invMassB = bodyB.m_mass * bodyB.m_invMass; float
   * invIB = bodyB.m_mass * bodyB.m_invI;
   *
   * Vec2 normal = c.normal;
   *
   * // Solve normal constraints for (int j = 0; j < c.pointCount; ++j) { ContactConstraintPoint*
   * ccp = c.points + j;
   *
   * Vec2 r1 = Mul(bodyA.GetXForm().R, ccp.localAnchorA - bodyA.GetLocalCenter()); Vec2 r2 =
   * Mul(bodyB.GetXForm().R, ccp.localAnchorB - bodyB.GetLocalCenter());
   *
   * Vec2 p1 = bodyA.m_sweep.c + r1; Vec2 p2 = bodyB.m_sweep.c + r2; Vec2 dp = p2 - p1;
   *
   * // Approximate the current separation. float separation = Dot(dp, normal) + ccp.separation;
   *
   * // Track max constraint error. minSeparation = Min(minSeparation, separation);
   *
   * // Prevent large corrections and allow slop. float C = Clamp(baumgarte * (separation +
   * _linearSlop), -_maxLinearCorrection, 0.0f);
   *
   * // Compute normal impulse float impulse = -ccp.equalizedMass * C;
   *
   * Vec2 P = impulse * normal;
   *
   * bodyA.m_sweep.c -= invMassA * P; bodyA.m_sweep.a -= invIA * Cross(r1, P);
   * bodyA.SynchronizeTransform();
   *
   * bodyB.m_sweep.c += invMassB * P; bodyB.m_sweep.a += invIB * Cross(r2, P);
   * bodyB.SynchronizeTransform(); } }
   *
   * // We can't expect minSpeparation >= -_linearSlop because we don't // push the separation above
   * -_linearSlop. return minSeparation >= -1.5f * _linearSlop; }
   */

    private val psolver = PositionSolverManifold()

    fun init(def: ContactSolverDef) {
        // System.out.println("Initializing contact solver");
        m_step = def.step
        m_count = def.count

        if (m_positionConstraints.size < m_count) {
            val old = m_positionConstraints
            m_positionConstraints = arrayOfNulls<ContactPositionConstraint>(MathUtils.max(old.size * 2, m_count)) as Array<ContactPositionConstraint>
            arraycopy(old, 0, m_positionConstraints, 0, old.size)
            for (i in old.size until m_positionConstraints.size) {
                m_positionConstraints[i] = ContactPositionConstraint()
            }
        }

        if (m_velocityConstraints.size < m_count) {
            val old = m_velocityConstraints
            m_velocityConstraints = arrayOfNulls<ContactVelocityConstraint>(MathUtils.max(old.size * 2, m_count)) as Array<ContactVelocityConstraint>
            arraycopy(old, 0, m_velocityConstraints, 0, old.size)
            for (i in old.size until m_velocityConstraints.size) {
                m_velocityConstraints[i] = ContactVelocityConstraint()
            }
        }

        m_positions = def.positions
        m_velocities = def.velocities
        m_contacts = def.contacts

        for (i in 0 until m_count) {
            // System.out.println("contacts: " + m_count);
            val contact = m_contacts!![i]

            val fixtureA = contact.m_fixtureA
            val fixtureB = contact.m_fixtureB
            val shapeA = fixtureA!!.getShape()
            val shapeB = fixtureB!!.getShape()
            val radiusA = shapeA!!.radius
            val radiusB = shapeB!!.radius
            val bodyA = fixtureA.getBody()
            val bodyB = fixtureB.getBody()
            val manifold = contact.getManifold()

            val pointCount = manifold.pointCount
            //assert (pointCount > 0);

            val vc = m_velocityConstraints[i]
            vc.friction = contact.m_friction
            vc.restitution = contact.m_restitution
            vc.tangentSpeed = contact.m_tangentSpeed
            vc.indexA = bodyA!!.islandIndex
            vc.indexB = bodyB!!.islandIndex
            vc.invMassA = bodyA.m_invMass
            vc.invMassB = bodyB.m_invMass
            vc.invIA = bodyA.m_invI
            vc.invIB = bodyB.m_invI
            vc.contactIndex = i
            vc.pointCount = pointCount
            vc.K.setZero()
            vc.normalMass.setZero()

            val pc = m_positionConstraints[i]
            pc.indexA = bodyA.islandIndex
            pc.indexB = bodyB.islandIndex
            pc.invMassA = bodyA.m_invMass
            pc.invMassB = bodyB.m_invMass
            pc.localCenterA.set(bodyA.sweep.localCenter)
            pc.localCenterB.set(bodyB.sweep.localCenter)
            pc.invIA = bodyA.m_invI
            pc.invIB = bodyB.m_invI
            pc.localNormal.set(manifold.localNormal)
            pc.localPoint.set(manifold.localPoint)
            pc.pointCount = pointCount
            pc.radiusA = radiusA
            pc.radiusB = radiusB
            pc.type = manifold.type

            // System.out.println("contact point count: " + pointCount);
            for (j in 0 until pointCount) {
                val cp = manifold.points[j]
                val vcp = vc.points[j]

                if (m_step!!.warmStarting) {
                    // assert(cp.normalImpulse == 0);
                    // System.out.println("contact normal impulse: " + cp.normalImpulse);
                    vcp.normalImpulse = m_step!!.dtRatio * cp.normalImpulse
                    vcp.tangentImpulse = m_step!!.dtRatio * cp.tangentImpulse
                } else {
                    vcp.normalImpulse = 0f
                    vcp.tangentImpulse = 0f
                }

                vcp.rA.setZero()
                vcp.rB.setZero()
                vcp.normalMass = 0f
                vcp.tangentMass = 0f
                vcp.velocityBias = 0f
                pc.localPoints[j].x = cp.localPoint.x
                pc.localPoints[j].y = cp.localPoint.y
            }
        }
    }

    fun warmStart() {
        // Warm start.
        for (i in 0 until m_count) {
            val vc = m_velocityConstraints[i]

            val indexA = vc.indexA
            val indexB = vc.indexB
            val mA = vc.invMassA
            val iA = vc.invIA
            val mB = vc.invMassB
            val iB = vc.invIB
            val pointCount = vc.pointCount

            val vA = m_velocities!![indexA].v
            var wA = m_velocities!![indexA].w
            val vB = m_velocities!![indexB].v
            var wB = m_velocities!![indexB].w

            val normal = vc.normal
            val tangentx = 1.0f * normal.y
            val tangenty = -1.0f * normal.x

            for (j in 0 until pointCount) {
                val vcp = vc.points[j]
                val Px = tangentx * vcp.tangentImpulse + normal.x * vcp.normalImpulse
                val Py = tangenty * vcp.tangentImpulse + normal.y * vcp.normalImpulse

                wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px)
                vA.x -= Px * mA
                vA.y -= Py * mA
                wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px)
                vB.x += Px * mB
                vB.y += Py * mB
            }
            m_velocities!![indexA].w = wA
            m_velocities!![indexB].w = wB
        }
    }

    fun initializeVelocityConstraints() {

        // Warm start.
        for (i in 0 until m_count) {
            val vc = m_velocityConstraints[i]
            val pc = m_positionConstraints[i]

            val radiusA = pc.radiusA
            val radiusB = pc.radiusB
            val manifold = m_contacts!![vc.contactIndex].getManifold()

            val indexA = vc.indexA
            val indexB = vc.indexB

            val mA = vc.invMassA
            val mB = vc.invMassB
            val iA = vc.invIA
            val iB = vc.invIB
            val localCenterA = pc.localCenterA
            val localCenterB = pc.localCenterB

            val cA = m_positions!![indexA].c
            val aA = m_positions!![indexA].a
            val vA = m_velocities!![indexA].v
            val wA = m_velocities!![indexA].w

            val cB = m_positions!![indexB].c
            val aB = m_positions!![indexB].a
            val vB = m_velocities!![indexB].v
            val wB = m_velocities!![indexB].w

            //assert (manifold.pointCount > 0);

            val xfAq = xfA.q
            val xfBq = xfB.q
            xfAq.setRadians(aA)
            xfBq.setRadians(aB)
            xfA.p.x = cA.x - (xfAq.c * localCenterA.x - xfAq.s * localCenterA.y)
            xfA.p.y = cA.y - (xfAq.s * localCenterA.x + xfAq.c * localCenterA.y)
            xfB.p.x = cB.x - (xfBq.c * localCenterB.x - xfBq.s * localCenterB.y)
            xfB.p.y = cB.y - (xfBq.s * localCenterB.x + xfBq.c * localCenterB.y)

            worldManifold.initialize(manifold, xfA, radiusA, xfB, radiusB)

            val vcnormal = vc.normal
            vcnormal.x = worldManifold.normal.x
            vcnormal.y = worldManifold.normal.y

            val pointCount = vc.pointCount
            for (j in 0 until pointCount) {
                val vcp = vc.points[j]
                val wmPj = worldManifold.points[j]
                val vcprA = vcp.rA
                val vcprB = vcp.rB
                vcprA.x = wmPj.x - cA.x
                vcprA.y = wmPj.y - cA.y
                vcprB.x = wmPj.x - cB.x
                vcprB.y = wmPj.y - cB.y

                val rnA = vcprA.x * vcnormal.y - vcprA.y * vcnormal.x
                val rnB = vcprB.x * vcnormal.y - vcprB.y * vcnormal.x

                val kNormal = mA + mB + iA * rnA * rnA + iB * rnB * rnB

                vcp.normalMass = if (kNormal > 0.0f) 1.0f / kNormal else 0.0f

                val tangentx = 1.0f * vcnormal.y
                val tangenty = -1.0f * vcnormal.x

                val rtA = vcprA.x * tangenty - vcprA.y * tangentx
                val rtB = vcprB.x * tangenty - vcprB.y * tangentx

                val kTangent = mA + mB + iA * rtA * rtA + iB * rtB * rtB

                vcp.tangentMass = if (kTangent > 0.0f) 1.0f / kTangent else 0.0f

                // Setup a velocity bias for restitution.
                vcp.velocityBias = 0.0f
                val tempx = vB.x + -wB * vcprB.y - vA.x - -wA * vcprA.y
                val tempy = vB.y + wB * vcprB.x - vA.y - wA * vcprA.x
                val vRel = vcnormal.x * tempx + vcnormal.y * tempy
                if (vRel < -Settings.velocityThreshold) {
                    vcp.velocityBias = -vc.restitution * vRel
                }
            }

            // If we have two points, then prepare the block solver.
            if (vc.pointCount == 2) {
                val vcp1 = vc.points[0]
                val vcp2 = vc.points[1]
                val rn1A = vcp1.rA.x * vcnormal.y - vcp1.rA.y * vcnormal.x
                val rn1B = vcp1.rB.x * vcnormal.y - vcp1.rB.y * vcnormal.x
                val rn2A = vcp2.rA.x * vcnormal.y - vcp2.rA.y * vcnormal.x
                val rn2B = vcp2.rB.x * vcnormal.y - vcp2.rB.y * vcnormal.x

                val k11 = mA + mB + iA * rn1A * rn1A + iB * rn1B * rn1B
                val k22 = mA + mB + iA * rn2A * rn2A + iB * rn2B * rn2B
                val k12 = mA + mB + iA * rn1A * rn2A + iB * rn1B * rn2B
                if (k11 * k11 < k_maxConditionNumber * (k11 * k22 - k12 * k12)) {
                    // K is safe to invert.
                    vc.K.ex.x = k11
                    vc.K.ex.y = k12
                    vc.K.ey.x = k12
                    vc.K.ey.y = k22
                    vc.K.invertToOut(vc.normalMass)
                } else {
                    // The constraints are redundant, just use one.
                    // TODO_ERIN use deepest?
                    vc.pointCount = 1
                }
            }
        }
    }

    fun storeImpulses() {
        for (i in 0 until m_count) {
            val vc = m_velocityConstraints[i]
            val manifold = m_contacts!![vc.contactIndex].getManifold()

            for (j in 0 until vc.pointCount) {
                manifold.points[j].normalImpulse = vc.points[j].normalImpulse
                manifold.points[j].tangentImpulse = vc.points[j].tangentImpulse
            }
        }
    }

    /**
     * Sequential solver.
     */
    fun solvePositionConstraints(): Boolean {
        var minSeparation = 0.0f

        for (i in 0 until m_count) {
            val pc = m_positionConstraints[i]

            val indexA = pc.indexA
            val indexB = pc.indexB

            val mA = pc.invMassA
            val iA = pc.invIA
            val localCenterA = pc.localCenterA
            val localCenterAx = localCenterA.x
            val localCenterAy = localCenterA.y
            val mB = pc.invMassB
            val iB = pc.invIB
            val localCenterB = pc.localCenterB
            val localCenterBx = localCenterB.x
            val localCenterBy = localCenterB.y
            val pointCount = pc.pointCount

            val cA = m_positions!![indexA].c
            var aA = m_positions!![indexA].a
            val cB = m_positions!![indexB].c
            var aB = m_positions!![indexB].a

            // Solve normal constraints
            for (j in 0 until pointCount) {
                val xfAq = xfA.q
                val xfBq = xfB.q
                xfAq.setRadians(aA)
                xfBq.setRadians(aB)
                xfA.p.x = cA.x - xfAq.c * localCenterAx + xfAq.s * localCenterAy
                xfA.p.y = cA.y - xfAq.s * localCenterAx - xfAq.c * localCenterAy
                xfB.p.x = cB.x - xfBq.c * localCenterBx + xfBq.s * localCenterBy
                xfB.p.y = cB.y - xfBq.s * localCenterBx - xfBq.c * localCenterBy

                val psm = psolver
                psm.initialize(pc, xfA, xfB, j)
                val normal = psm.normal
                val point = psm.point
                val separation = psm.separation

                val rAx = point.x - cA.x
                val rAy = point.y - cA.y
                val rBx = point.x - cB.x
                val rBy = point.y - cB.y

                // Track max constraint error.
                minSeparation = MathUtils.min(minSeparation, separation)

                // Prevent large corrections and allow slop.
                val C = MathUtils.clamp(Settings.baumgarte * (separation + Settings.linearSlop),
                        -Settings.maxLinearCorrection, 0.0f)

                // Compute the effective mass.
                val rnA = rAx * normal.y - rAy * normal.x
                val rnB = rBx * normal.y - rBy * normal.x
                val K = mA + mB + iA * rnA * rnA + iB * rnB * rnB

                // Compute normal impulse
                val impulse = if (K > 0.0f) -C / K else 0.0f

                val Px = normal.x * impulse
                val Py = normal.y * impulse

                cA.x -= Px * mA
                cA.y -= Py * mA
                aA -= iA * (rAx * Py - rAy * Px)

                cB.x += Px * mB
                cB.y += Py * mB
                aB += iB * (rBx * Py - rBy * Px)
            }

            // m_positions[indexA].c.set(cA);
            m_positions!![indexA].a = aA

            // m_positions[indexB].c.set(cB);
            m_positions!![indexB].a = aB
        }

        // We can't expect minSpeparation >= -linearSlop because we don't
        // push the separation above -linearSlop.
        return minSeparation >= -3.0f * Settings.linearSlop
    }

    // Sequential position solver for position constraints.
    fun solveTOIPositionConstraints(toiIndexA: Int, toiIndexB: Int): Boolean {
        var minSeparation = 0.0f

        for (i in 0 until m_count) {
            val pc = m_positionConstraints[i]

            val indexA = pc.indexA
            val indexB = pc.indexB
            val localCenterA = pc.localCenterA
            val localCenterB = pc.localCenterB
            val localCenterAx = localCenterA.x
            val localCenterAy = localCenterA.y
            val localCenterBx = localCenterB.x
            val localCenterBy = localCenterB.y
            val pointCount = pc.pointCount

            var mA = 0.0f
            var iA = 0.0f
            if (indexA == toiIndexA || indexA == toiIndexB) {
                mA = pc.invMassA
                iA = pc.invIA
            }

            var mB = 0f
            var iB = 0f
            if (indexB == toiIndexA || indexB == toiIndexB) {
                mB = pc.invMassB
                iB = pc.invIB
            }

            val cA = m_positions!![indexA].c
            var aA = m_positions!![indexA].a

            val cB = m_positions!![indexB].c
            var aB = m_positions!![indexB].a

            // Solve normal constraints
            for (j in 0 until pointCount) {
                val xfAq = xfA.q
                val xfBq = xfB.q
                xfAq.setRadians(aA)
                xfBq.setRadians(aB)
                xfA.p.x = cA.x - xfAq.c * localCenterAx + xfAq.s * localCenterAy
                xfA.p.y = cA.y - xfAq.s * localCenterAx - xfAq.c * localCenterAy
                xfB.p.x = cB.x - xfBq.c * localCenterBx + xfBq.s * localCenterBy
                xfB.p.y = cB.y - xfBq.s * localCenterBx - xfBq.c * localCenterBy

                val psm = psolver
                psm.initialize(pc, xfA, xfB, j)
                val normal = psm.normal

                val point = psm.point
                val separation = psm.separation

                val rAx = point.x - cA.x
                val rAy = point.y - cA.y
                val rBx = point.x - cB.x
                val rBy = point.y - cB.y

                // Track max constraint error.
                minSeparation = MathUtils.min(minSeparation, separation)

                // Prevent large corrections and allow slop.
                val C = MathUtils.clamp(Settings.toiBaugarte * (separation + Settings.linearSlop),
                        -Settings.maxLinearCorrection, 0.0f)

                // Compute the effective mass.
                val rnA = rAx * normal.y - rAy * normal.x
                val rnB = rBx * normal.y - rBy * normal.x
                val K = mA + mB + iA * rnA * rnA + iB * rnB * rnB

                // Compute normal impulse
                val impulse = if (K > 0.0f) -C / K else 0.0f

                val Px = normal.x * impulse
                val Py = normal.y * impulse

                cA.x -= Px * mA
                cA.y -= Py * mA
                aA -= iA * (rAx * Py - rAy * Px)

                cB.x += Px * mB
                cB.y += Py * mB
                aB += iB * (rBx * Py - rBy * Px)
            }

            // m_positions[indexA].c.set(cA);
            m_positions!![indexA].a = aA

            // m_positions[indexB].c.set(cB);
            m_positions!![indexB].a = aB
        }

        // We can't expect minSpeparation >= -_linearSlop because we don't
        // push the separation above -_linearSlop.
        return minSeparation >= -1.5f * Settings.linearSlop
    }


    private var vA: Vec2? = null
    private var vB: Vec2? = null
    private var wA: Float = 0.toFloat()
    private var wB: Float = 0.toFloat()

    fun solveVelocityConstraints() {
        for (i in 0 until m_count) {
            val vc = m_velocityConstraints[i]

            val indexA = vc.indexA
            val indexB = vc.indexB

            val mA = vc.invMassA
            val mB = vc.invMassB
            val iA = vc.invIA
            val iB = vc.invIB
            val pointCount = vc.pointCount

            vA = m_velocities!![indexA].v
            wA = m_velocities!![indexA].w
            vB = m_velocities!![indexB].v
            wB = m_velocities!![indexB].w

            val normal = vc.normal
            val normalx = normal.x
            val normaly = normal.y
            val tangentx = 1.0f * vc.normal.y
            val tangenty = -1.0f * vc.normal.x
            val friction = vc.friction

            //assert (pointCount == 1 || pointCount == 2);
            solveVelocityConstraints0(vc, mA, mB, iA, iB, pointCount, tangentx, tangenty, friction)

            // Solve normal constraints
            if (vc.pointCount == 1) {
                solveVelocityConstraints1(vc.points[0], mA, mB, iA, iB, normalx, normaly)
            } else {
                solveVelocityConstraints2(vc, mA, mB, iA, iB, normal, normalx, normaly)
            }

            // m_velocities[indexA].v.set(vA);
            m_velocities!![indexA].w = wA
            // m_velocities[indexB].v.set(vB);
            m_velocities!![indexB].w = wB
        }
    }

    private fun solveVelocityConstraints2(vc: ContactVelocityConstraint, mA: Float, mB: Float, iA: Float, iB: Float, normal: Vec2, normalx: Float, normaly: Float) {
        // Block solver developed in collaboration with Dirk Gregorius (back in 01/07 on
        // Box2D_Lite).
        // Build the mini LCP for this contact patch
        //
        // vn = A * x + b, vn >= 0, , vn >= 0, x >= 0 and vn_i * x_i = 0 with i = 1..2
        //
        // A = J * W * JT and J = ( -n, -r1 x n, n, r2 x n )
        // b = vn_0 - velocityBias
        //
        // The system is solved using the "Total enumeration method" (s. Murty). The complementary
        // constraint vn_i * x_i
        // implies that we must have in any solution either vn_i = 0 or x_i = 0. So for the 2D
        // contact problem the cases
        // vn1 = 0 and vn2 = 0, x1 = 0 and x2 = 0, x1 = 0 and vn2 = 0, x2 = 0 and vn1 = 0 need to be
        // tested. The first valid
        // solution that satisfies the problem is chosen.
        //
        // In order to account of the accumulated impulse 'a' (because of the iterative nature of
        // the solver which only requires
        // that the accumulated impulse is clamped and not the incremental impulse) we change the
        // impulse variable (x_i).
        //
        // Substitute:
        //
        // x = a + d
        //
        // a := old total impulse
        // x := new total impulse
        // d := incremental impulse
        //
        // For the current iteration we extend the formula for the incremental impulse
        // to compute the new total impulse:
        //
        // vn = A * d + b
        // = A * (x - a) + b
        // = A * x + b - A * a
        // = A * x + b'
        // b' = b - A * a;

        val cp1 = vc.points[0]
        val cp2 = vc.points[1]
        val cp1rA = cp1.rA
        val cp1rB = cp1.rB
        val cp2rA = cp2.rA
        val cp2rB = cp2.rB
        val ax = cp1.normalImpulse
        val ay = cp2.normalImpulse

        //assert (ax >= 0.0f && ay >= 0.0f);
        // Relative velocity at contact
        // Vec2 dv1 = vB + Cross(wB, cp1.rB) - vA - Cross(wA, cp1.rA);
        val dv1x = -wB * cp1rB.y + vB!!.x - vA!!.x + wA * cp1rA.y
        val dv1y = wB * cp1rB.x + vB!!.y - vA!!.y - wA * cp1rA.x

        // Vec2 dv2 = vB + Cross(wB, cp2.rB) - vA - Cross(wA, cp2.rA);
        val dv2x = -wB * cp2rB.y + vB!!.x - vA!!.x + wA * cp2rA.y
        val dv2y = wB * cp2rB.x + vB!!.y - vA!!.y - wA * cp2rA.x

        // Compute normal velocity
        var vn1 = dv1x * normalx + dv1y * normaly
        var vn2 = dv2x * normalx + dv2y * normaly

        var bx = vn1 - cp1.velocityBias
        var by = vn2 - cp2.velocityBias

        // Compute b'
        val R = vc.K
        bx -= R.ex.x * ax + R.ey.x * ay
        by -= R.ex.y * ax + R.ey.y * ay

        // final float k_errorTol = 1e-3f;
        // B2_NOT_USED(k_errorTol);
        //
        // Case 1: vn = 0
        //
        // 0 = A * x' + b'
        //
        // Solve for x':
        //
        // x' = - inv(A) * b'
        //
        // Vec2 x = - Mul(c.normalMass, b);
        val R1 = vc.normalMass
        var xx = R1.ex.x * bx + R1.ey.x * by
        var xy = R1.ex.y * bx + R1.ey.y * by
        xx *= -1f
        xy *= -1f

        if (xx >= 0.0f && xy >= 0.0f) {
            solveVelocityConstraints2a(mA, mB, iA, iB, normal, normalx, normaly, cp1, cp2, cp1rA, cp1rB, cp2rA, cp2rB, ax, ay, xx, xy)
        } else {
            //
            // Case 2: vn1 = 0 and x2 = 0
            //
            // 0 = a11 * x1' + a12 * 0 + b1'
            // vn2 = a21 * x1' + a22 * 0 + '
            //
            xx = -cp1.normalMass * bx
            xy = 0.0f
            vn1 = 0.0f
            vn2 = vc.K.ex.y * xx + by

            if (xx >= 0.0f && vn2 >= 0.0f) {
                solveVelocityConstraints2b(mA, mB, iA, iB, normal, normalx, normaly, cp1, cp2, cp1rA, cp1rB, cp2rA, cp2rB, ax, ay, xx, xy)
            } else {

                //
                // Case 3: wB = 0 and x1 = 0
                //
                // vn1 = a11 * 0 + a12 * x2' + b1'
                // 0 = a21 * 0 + a22 * x2' + '
                //
                xx = 0.0f
                xy = -cp2.normalMass * by
                vn1 = vc.K.ey.x * xy + bx
                vn2 = 0.0f

                if (xy >= 0.0f && vn1 >= 0.0f) {
                    solveVelocityConstraints2c(mA, mB, iA, iB, normal, normalx, normaly, cp1, cp2, cp1rA, cp1rB, cp2rA, cp2rB, ax, ay, xx, xy)
                } else {

                    //
                    // Case 4: x1 = 0 and x2 = 0
                    //
                    // vn1 = b1
                    // vn2 = ;
                    xx = 0.0f
                    xy = 0.0f
                    vn1 = bx
                    vn2 = by

                    if (vn1 >= 0.0f && vn2 >= 0.0f) {
                        solveVelocityConstraints2d(mA, mB, iA, iB, normalx, normaly, cp1, cp2, cp1rA, cp1rB, cp2rA, cp2rB, ax, ay, xx, xy)
                    } else {
                        // No solution, give up. This is hit sometimes, but it doesn't seem to matter.
                    }
                }
            }
        }
    }

    private fun solveVelocityConstraints2d(mA: Float, mB: Float, iA: Float, iB: Float, normalx: Float, normaly: Float, cp1: VelocityConstraintPoint, cp2: VelocityConstraintPoint, cp1rA: Vec2, cp1rB: Vec2, cp2rA: Vec2, cp2rB: Vec2, ax: Float, ay: Float, xx: Float, xy: Float) {
        // Resubstitute for the incremental impulse
        val dx = xx - ax
        val dy = xy - ay

        // Apply incremental impulse
        /*
     * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
     * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
     *
     * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
     */

        val P1x = normalx * dx
        val P1y = normaly * dx
        val P2x = normalx * dy
        val P2y = normaly * dy

        vA!!.x -= mA * (P1x + P2x)
        vA!!.y -= mA * (P1y + P2y)
        vB!!.x += mB * (P1x + P2x)
        vB!!.y += mB * (P1y + P2y)

        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x))
        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x))

        // Accumulate
        cp1.normalImpulse = xx
        cp2.normalImpulse = xy
    }

    private fun solveVelocityConstraints2c(mA: Float, mB: Float, iA: Float, iB: Float, normal: Vec2, normalx: Float, normaly: Float, cp1: VelocityConstraintPoint, cp2: VelocityConstraintPoint, cp1rA: Vec2, cp1rB: Vec2, cp2rA: Vec2, cp2rB: Vec2, ax: Float, ay: Float, xx: Float, xy: Float) {
        val vn2: Float// Resubstitute for the incremental impulse
        val dx = xx - ax
        val dy = xy - ay

        // Apply incremental impulse
        /*
     * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
     * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
     *
     * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
     */

        val P1x = normalx * dx
        val P1y = normaly * dx
        val P2x = normalx * dy
        val P2y = normaly * dy

        vA!!.x -= mA * (P1x + P2x)
        vA!!.y -= mA * (P1y + P2y)
        vB!!.x += mB * (P1x + P2x)
        vB!!.y += mB * (P1y + P2y)

        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x))
        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x))

        // Accumulate
        cp1.normalImpulse = xx
        cp2.normalImpulse = xy

        /*
     * #if B2_DEBUG_SOLVER == 1 // Postconditions dv2 = vB + Cross(wB, cp2.rB) - vA -
     * Cross(wA, cp2.rA);
     *
     * // Compute normal velocity vn2 = Dot(dv2, normal);
     *
     * assert(Abs(vn2 - cp2.velocityBias) < k_errorTol); #endif
     */
        if (DEBUG_SOLVER) {
            // Postconditions
            val dv2 = vB!!.add(Vec2.cross(wB, cp2rB).subLocal(vA!!).subLocal(Vec2.cross(wA, cp2rA)))
            // Compute normal velocity
            vn2 = Vec2.dot(dv2, normal)

            //assert (MathUtils.abs(vn2 - cp2.velocityBias) < k_errorTol);
        }
    }

    private fun solveVelocityConstraints2b(mA: Float, mB: Float, iA: Float, iB: Float, normal: Vec2, normalx: Float, normaly: Float, cp1: VelocityConstraintPoint, cp2: VelocityConstraintPoint, cp1rA: Vec2, cp1rB: Vec2, cp2rA: Vec2, cp2rB: Vec2, ax: Float, ay: Float, xx: Float, xy: Float) {
        val vn1: Float// Get the incremental impulse
        val dx = xx - ax
        val dy = xy - ay

        // Apply incremental impulse
        // Vec2 P1 = d.x * normal;
        // Vec2 P2 = d.y * normal;
        val P1x = normalx * dx
        val P1y = normaly * dx
        val P2x = normalx * dy
        val P2y = normaly * dy

        /*
     * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
     * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
     *
     * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
     */

        vA!!.x -= mA * (P1x + P2x)
        vA!!.y -= mA * (P1y + P2y)
        vB!!.x += mB * (P1x + P2x)
        vB!!.y += mB * (P1y + P2y)

        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x))
        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x))

        // Accumulate
        cp1.normalImpulse = xx
        cp2.normalImpulse = xy

        /*
     * #if B2_DEBUG_SOLVER == 1 // Postconditions dv1 = vB + Cross(wB, cp1.rB) - vA -
     * Cross(wA, cp1.rA);
     *
     * // Compute normal velocity vn1 = Dot(dv1, normal);
     *
     * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); #endif
     */
        if (DEBUG_SOLVER) {
            // Postconditions
            val dv1 = vB!!.add(Vec2.cross(wB, cp1rB).subLocal(vA!!).subLocal(Vec2.cross(wA, cp1rA)))
            // Compute normal velocity
            vn1 = Vec2.dot(dv1, normal)

            //assert (MathUtils.abs(vn1 - cp1.velocityBias) < k_errorTol);
        }
    }

    private fun solveVelocityConstraints2a(mA: Float, mB: Float, iA: Float, iB: Float, normal: Vec2, normalx: Float, normaly: Float, cp1: VelocityConstraintPoint, cp2: VelocityConstraintPoint, cp1rA: Vec2, cp1rB: Vec2, cp2rA: Vec2, cp2rB: Vec2, ax: Float, ay: Float, xx: Float, xy: Float) {
        val vn1: Float
        val vn2: Float// Get the incremental impulse
        // Vec2 d = x - a;
        val dx = xx - ax
        val dy = xy - ay

        // Apply incremental impulse
        // Vec2 P1 = d.x * normal;
        // Vec2 P2 = d.y * normal;
        val P1x = dx * normalx
        val P1y = dx * normaly
        val P2x = dy * normalx
        val P2y = dy * normaly

        /*
     * vA -= invMassA * (P1 + P2); wA -= invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
     *
     * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
     */

        vA!!.x -= mA * (P1x + P2x)
        vA!!.y -= mA * (P1y + P2y)
        vB!!.x += mB * (P1x + P2x)
        vB!!.y += mB * (P1y + P2y)

        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x))
        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x))

        // Accumulate
        cp1.normalImpulse = xx
        cp2.normalImpulse = xy

        /*
     * #if B2_DEBUG_SOLVER == 1 // Postconditions dv1 = vB + Cross(wB, cp1.rB) - vA -
     * Cross(wA, cp1.rA); dv2 = vB + Cross(wB, cp2.rB) - vA - Cross(wA, cp2.rA);
     *
     * // Compute normal velocity vn1 = Dot(dv1, normal); vn2 = Dot(dv2, normal);
     *
     * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); assert(Abs(vn2 - cp2.velocityBias)
     * < k_errorTol); #endif
     */
        if (DEBUG_SOLVER) {
            // Postconditions
            val dv1 = vB!!.add(Vec2.cross(wB, cp1rB).subLocal(vA!!).subLocal(Vec2.cross(wA, cp1rA)))
            val dv2 = vB!!.add(Vec2.cross(wB, cp2rB).subLocal(vA!!).subLocal(Vec2.cross(wA, cp2rA)))
            // Compute normal velocity
            vn1 = Vec2.dot(dv1, normal)
            vn2 = Vec2.dot(dv2, normal)

            //assert (MathUtils.abs(vn1 - cp1.velocityBias) < k_errorTol);
            //assert (MathUtils.abs(vn2 - cp2.velocityBias) < k_errorTol);
        }
    }

    private fun solveVelocityConstraints1(point: VelocityConstraintPoint, mA: Float, mB: Float, iA: Float, iB: Float, normalx: Float, normaly: Float) {

        // Relative velocity at contact
        // Vec2 dv = vB + Cross(wB, vcp.rB) - vA - Cross(wA, vcp.rA);

        val dvx = -wB * point.rB.y + vB!!.x - vA!!.x + wA * point.rA.y
        val dvy = wB * point.rB.x + vB!!.y - vA!!.y - wA * point.rA.x

        // Compute normal impulse
        val vn = dvx * normalx + dvy * normaly
        var lambda = -point.normalMass * (vn - point.velocityBias)

        // Clamp the accumulated impulse
        val a = point.normalImpulse + lambda
        val newImpulse = if (a > 0.0f) a else 0.0f
        lambda = newImpulse - point.normalImpulse
        point.normalImpulse = newImpulse

        // Apply contact impulse
        val Px = normalx * lambda
        val Py = normaly * lambda

        // vA -= invMassA * P;
        vA!!.x -= Px * mA
        vA!!.y -= Py * mA
        wA -= iA * (point.rA.x * Py - point.rA.y * Px)

        // vB += invMassB * P;
        vB!!.x += Px * mB
        vB!!.y += Py * mB
        wB += iB * (point.rB.x * Py - point.rB.y * Px)
    }

    private fun solveVelocityConstraints0(vc: ContactVelocityConstraint, mA: Float, mB: Float, iA: Float, iB: Float, pointCount: Int, tangentx: Float, tangenty: Float, friction: Float) {
        // Solve tangent constraints
        for (j in 0 until pointCount) {
            val vcp = vc.points[j]
            val a = vcp.rA
            val dvx = -wB * vcp.rB.y + vB!!.x - vA!!.x + wA * a.y
            val dvy = wB * vcp.rB.x + vB!!.y - vA!!.y - wA * a.x

            // Compute tangent force
            val vt = dvx * tangentx + dvy * tangenty - vc.tangentSpeed
            var lambda = vcp.tangentMass * -vt

            // Clamp the accumulated force
            val maxFriction = friction * vcp.normalImpulse
            val newImpulse = MathUtils.clamp(vcp.tangentImpulse + lambda, -maxFriction, maxFriction)
            lambda = newImpulse - vcp.tangentImpulse
            vcp.tangentImpulse = newImpulse

            // Apply contact impulse
            // Vec2 P = lambda * tangent;

            val Px = tangentx * lambda
            val Py = tangenty * lambda

            // vA -= invMassA * P;
            vA!!.x -= Px * mA
            vA!!.y -= Py * mA
            wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px)

            // vB += invMassB * P;
            vB!!.x += Px * mB
            vB!!.y += Py * mB
            wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px)
        }
    }


    class ContactSolverDef {
        var step: TimeStep? = null
        var contacts: Array<Contact>? = null
        var count: Int = 0
        var positions: Array<Position>? = null
        var velocities: Array<Velocity>? = null
    }

    companion object {

        val DEBUG_SOLVER = false
        val k_errorTol = 1e-3f
        /**
         * For each solver, this is the initial number of constraints in the array, which expands as
         * needed.
         */
        val INITIAL_NUM_CONSTRAINTS = 256

        /**
         * Ensure a reasonable condition number. for the block solver
         */
        val k_maxConditionNumber = 100.0f
    }
}


