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
/**
 * Created at 11:34:45 AM Jan 23, 2011
 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.SolverData
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

//Gear Joint:
//C0 = (coordinate1 + ratio * coordinate2)_initial
//C = (coordinate1 + ratio * coordinate2) - C0 = 0
//J = [J1 ratio * J2]
//K = J * invM * JT
//= J1 * invM1 * J1T + ratio * ratio * J2 * invM2 * J2T
//
//Revolute:
//coordinate = rotation
//Cdot = angularVelocity
//J = [0 0 1]
//K = J * invM * JT = invI
//
//Prismatic:
//coordinate = dot(p - pg, ug)
//Cdot = dot(v + cross(w, r), ug)
//J = [ug cross(r, ug)]
//K = J * invM * JT = invMass + invI * cross(r, ug)^2

/**
 * A gear joint is used to connect two joints together. Either joint can be a revolute or prismatic
 * joint. You specify a gear ratio to bind the motions together: coordinate1 + ratio * coordinate2 =
 * constant The ratio can be negative or positive. If one joint is a revolute joint and the other
 * joint is a prismatic joint, then the ratio will have units of length or units of 1/length.
 *
 * @warning The revolute and prismatic joints must be attached to fixed bodies (which must be body1
 * on those joints).
 * @warning You have to manually destroy the gear joint if joint1 or joint2 is destroyed.
 * @author Daniel Murphy
 */
class GearJoint(argWorldPool: IWorldPool, def: GearJointDef) : Joint(argWorldPool, def) {

    val joint1: Joint? = def.joint1

    val joint2: Joint? = def.joint2

    private val m_typeA: JointType
    private val m_typeB: JointType

    // Body A is connected to body C
    // Body B is connected to body D
    private val m_bodyC: Body
    private val m_bodyD: Body

    // Solver shared
    private val m_localAnchorA = Vec2()
    private val m_localAnchorB = Vec2()
    private val m_localAnchorC = Vec2()
    private val m_localAnchorD = Vec2()

    private val m_localAxisC = Vec2()
    private val m_localAxisD = Vec2()

    private var m_referenceAngleA: Float = 0.toFloat()
    private var m_referenceAngleB: Float = 0.toFloat()

    private val m_constant: Float

    var ratio: Float = 0.toFloat()

    private var m_impulse: Float = 0.toFloat()

    // Solver temp
    private var m_indexA: Int = 0
    private var m_indexB: Int = 0
    private var m_indexC: Int = 0
    private var m_indexD: Int = 0
    private val m_lcA = Vec2()
    private val m_lcB = Vec2()
    private val m_lcC = Vec2()
    private val m_lcD = Vec2()
    private var m_mA: Float = 0.toFloat()
    private var m_mB: Float = 0.toFloat()
    private var m_mC: Float = 0.toFloat()
    private var m_mD: Float = 0.toFloat()
    private var m_iA: Float = 0.toFloat()
    private var m_iB: Float = 0.toFloat()
    private var m_iC: Float = 0.toFloat()
    private var m_iD: Float = 0.toFloat()
    private val m_JvAC = Vec2()
    private val m_JvBD = Vec2()
    private var m_JwA: Float = 0.toFloat()
    private var m_JwB: Float = 0.toFloat()
    private var m_JwC: Float = 0.toFloat()
    private var m_JwD: Float = 0.toFloat()
    private var m_mass: Float = 0.toFloat()

    init {

        m_typeA = joint1!!.type
        m_typeB = joint2!!.type

        assert(m_typeA === JointType.REVOLUTE || m_typeA === JointType.PRISMATIC)
        assert(m_typeB === JointType.REVOLUTE || m_typeB === JointType.PRISMATIC)

        val coordinateA: Float
        val coordinateB: Float

        // TODO_ERIN there might be some problem with the joint edges in Joint.

        m_bodyC = joint1.bodyA!!
        bodyA = joint1.bodyB

        // Get geometry of joint1
        val xfA = bodyA!!.xf
        val aA = bodyA!!.sweep.a
        val xfC = m_bodyC.xf
        val aC = m_bodyC.sweep.a

        if (m_typeA === JointType.REVOLUTE) {
            val revolute = def.joint1 as RevoluteJoint?
            m_localAnchorC.set(revolute!!.m_localAnchorA)
            m_localAnchorA.set(revolute.m_localAnchorB)
            m_referenceAngleA = revolute.m_referenceAngleRadians
            m_localAxisC.setZero()

            coordinateA = aA - aC - m_referenceAngleA
        } else {
            val pA = pool.popVec2()
            val temp = pool.popVec2()
            val prismatic = def.joint1 as PrismaticJoint?
            m_localAnchorC.set(prismatic!!.m_localAnchorA)
            m_localAnchorA.set(prismatic.m_localAnchorB)
            m_referenceAngleA = prismatic.m_referenceAngleRadians
            m_localAxisC.set(prismatic.m_localXAxisA)

            val pC = m_localAnchorC
            Rot.mulToOutUnsafe(xfA.q, m_localAnchorA, temp)
            temp.addLocal(xfA.p).subLocal(xfC.p)
            Rot.mulTransUnsafe(xfC.q, temp, pA)
            coordinateA = Vec2.dot(pA.subLocal(pC), m_localAxisC)
            pool.pushVec2(2)
        }

        m_bodyD = joint2.bodyA!!
        bodyB = joint2.bodyB

        // Get geometry of joint2
        val xfB = bodyB!!.xf
        val aB = bodyB!!.sweep.a
        val xfD = m_bodyD.xf
        val aD = m_bodyD.sweep.a

        if (m_typeB === JointType.REVOLUTE) {
            val revolute = def.joint2 as RevoluteJoint?
            m_localAnchorD.set(revolute!!.m_localAnchorA)
            m_localAnchorB.set(revolute.m_localAnchorB)
            m_referenceAngleB = revolute.m_referenceAngleRadians
            m_localAxisD.setZero()

            coordinateB = aB - aD - m_referenceAngleB
        } else {
            val pB = pool.popVec2()
            val temp = pool.popVec2()
            val prismatic = def.joint2 as PrismaticJoint?
            m_localAnchorD.set(prismatic!!.m_localAnchorA)
            m_localAnchorB.set(prismatic.m_localAnchorB)
            m_referenceAngleB = prismatic.m_referenceAngleRadians
            m_localAxisD.set(prismatic.m_localXAxisA)

            val pD = m_localAnchorD
            Rot.mulToOutUnsafe(xfB.q, m_localAnchorB, temp)
            temp.addLocal(xfB.p).subLocal(xfD.p)
            Rot.mulTransUnsafe(xfD.q, temp, pB)
            coordinateB = Vec2.dot(pB.subLocal(pD), m_localAxisD)
            pool.pushVec2(2)
        }

        ratio = def.ratio

        m_constant = coordinateA + ratio * coordinateB

        m_impulse = 0.0f
    }

    override fun getAnchorA(argOut: Vec2) {
        bodyA!!.getWorldPointToOut(m_localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        bodyB!!.getWorldPointToOut(m_localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        argOut.set(m_JvAC).mulLocal(m_impulse)
        argOut.mulLocal(inv_dt)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        val L = m_impulse * m_JwA
        return inv_dt * L
    }

    override fun initVelocityConstraints(data: SolverData) {
        m_indexA = bodyA!!.islandIndex
        m_indexB = bodyB!!.islandIndex
        m_indexC = m_bodyC.islandIndex
        m_indexD = m_bodyD.islandIndex
        m_lcA.set(bodyA!!.sweep.localCenter)
        m_lcB.set(bodyB!!.sweep.localCenter)
        m_lcC.set(m_bodyC.sweep.localCenter)
        m_lcD.set(m_bodyD.sweep.localCenter)
        m_mA = bodyA!!.m_invMass
        m_mB = bodyB!!.m_invMass
        m_mC = m_bodyC.m_invMass
        m_mD = m_bodyD.m_invMass
        m_iA = bodyA!!.m_invI
        m_iB = bodyB!!.m_invI
        m_iC = m_bodyC.m_invI
        m_iD = m_bodyD.m_invI

        // Vec2 cA = data.positions[m_indexA].c;
        val aA = data.positions!![m_indexA].a
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w

        // Vec2 cB = data.positions[m_indexB].c;
        val aB = data.positions!![m_indexB].a
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        // Vec2 cC = data.positions[m_indexC].c;
        val aC = data.positions!![m_indexC].a
        val vC = data.velocities!![m_indexC].v
        var wC = data.velocities!![m_indexC].w

        // Vec2 cD = data.positions[m_indexD].c;
        val aD = data.positions!![m_indexD].a
        val vD = data.velocities!![m_indexD].v
        var wD = data.velocities!![m_indexD].w

        val qA = pool.popRot()
        val qB = pool.popRot()
        val qC = pool.popRot()
        val qD = pool.popRot()
        qA.setRadians(aA)
        qB.setRadians(aB)
        qC.setRadians(aC)
        qD.setRadians(aD)

        m_mass = 0.0f

        val temp = pool.popVec2()

        if (m_typeA === JointType.REVOLUTE) {
            m_JvAC.setZero()
            m_JwA = 1.0f
            m_JwC = 1.0f
            m_mass += m_iA + m_iC
        } else {
            val rC = pool.popVec2()
            val rA = pool.popVec2()
            Rot.mulToOutUnsafe(qC, m_localAxisC, m_JvAC)
            Rot.mulToOutUnsafe(qC, temp.set(m_localAnchorC).subLocal(m_lcC), rC)
            Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_lcA), rA)
            m_JwC = Vec2.cross(rC, m_JvAC)
            m_JwA = Vec2.cross(rA, m_JvAC)
            m_mass += m_mC + m_mA + m_iC * m_JwC * m_JwC + m_iA * m_JwA * m_JwA
            pool.pushVec2(2)
        }

        if (m_typeB === JointType.REVOLUTE) {
            m_JvBD.setZero()
            m_JwB = ratio
            m_JwD = ratio
            m_mass += ratio * ratio * (m_iB + m_iD)
        } else {
            val u = pool.popVec2()
            val rD = pool.popVec2()
            val rB = pool.popVec2()
            Rot.mulToOutUnsafe(qD, m_localAxisD, u)
            Rot.mulToOutUnsafe(qD, temp.set(m_localAnchorD).subLocal(m_lcD), rD)
            Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_lcB), rB)
            m_JvBD.set(u).mulLocal(ratio)
            m_JwD = ratio * Vec2.cross(rD, u)
            m_JwB = ratio * Vec2.cross(rB, u)
            m_mass += ratio * ratio * (m_mD + m_mB) + m_iD * m_JwD * m_JwD + m_iB * m_JwB * m_JwB
            pool.pushVec2(3)
        }

        // Compute effective mass.
        m_mass = if (m_mass > 0.0f) 1.0f / m_mass else 0.0f

        if (data.step!!.warmStarting) {
            vA.x += m_mA * m_impulse * m_JvAC.x
            vA.y += m_mA * m_impulse * m_JvAC.y
            wA += m_iA * m_impulse * m_JwA

            vB.x += m_mB * m_impulse * m_JvBD.x
            vB.y += m_mB * m_impulse * m_JvBD.y
            wB += m_iB * m_impulse * m_JwB

            vC.x -= m_mC * m_impulse * m_JvAC.x
            vC.y -= m_mC * m_impulse * m_JvAC.y
            wC -= m_iC * m_impulse * m_JwC

            vD.x -= m_mD * m_impulse * m_JvBD.x
            vD.y -= m_mD * m_impulse * m_JvBD.y
            wD -= m_iD * m_impulse * m_JwD
        } else {
            m_impulse = 0.0f
        }
        pool.pushVec2(1)
        pool.pushRot(4)

        // data.velocities[m_indexA].v = vA;
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v = vB;
        data.velocities!![m_indexB].w = wB
        // data.velocities[m_indexC].v = vC;
        data.velocities!![m_indexC].w = wC
        // data.velocities[m_indexD].v = vD;
        data.velocities!![m_indexD].w = wD
    }

    override fun solveVelocityConstraints(data: SolverData) {
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w
        val vC = data.velocities!![m_indexC].v
        var wC = data.velocities!![m_indexC].w
        val vD = data.velocities!![m_indexD].v
        var wD = data.velocities!![m_indexD].w

        val temp1 = pool.popVec2()
        val temp2 = pool.popVec2()
        var Cdot = Vec2.dot(m_JvAC, temp1.set(vA).subLocal(vC)) + Vec2.dot(m_JvBD, temp2.set(vB).subLocal(vD))
        Cdot += m_JwA * wA - m_JwC * wC + (m_JwB * wB - m_JwD * wD)
        pool.pushVec2(2)

        val impulse = -m_mass * Cdot
        m_impulse += impulse

        vA.x += m_mA * impulse * m_JvAC.x
        vA.y += m_mA * impulse * m_JvAC.y
        wA += m_iA * impulse * m_JwA

        vB.x += m_mB * impulse * m_JvBD.x
        vB.y += m_mB * impulse * m_JvBD.y
        wB += m_iB * impulse * m_JwB

        vC.x -= m_mC * impulse * m_JvAC.x
        vC.y -= m_mC * impulse * m_JvAC.y
        wC -= m_iC * impulse * m_JwC

        vD.x -= m_mD * impulse * m_JvBD.x
        vD.y -= m_mD * impulse * m_JvBD.y
        wD -= m_iD * impulse * m_JwD


        // data.velocities[m_indexA].v = vA;
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v = vB;
        data.velocities!![m_indexB].w = wB
        // data.velocities[m_indexC].v = vC;
        data.velocities!![m_indexC].w = wC
        // data.velocities[m_indexD].v = vD;
        data.velocities!![m_indexD].w = wD
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a
        val cC = data.positions!![m_indexC].c
        var aC = data.positions!![m_indexC].a
        val cD = data.positions!![m_indexD].c
        var aD = data.positions!![m_indexD].a

        val qA = pool.popRot()
        val qB = pool.popRot()
        val qC = pool.popRot()
        val qD = pool.popRot()
        qA.setRadians(aA)
        qB.setRadians(aB)
        qC.setRadians(aC)
        qD.setRadians(aD)

        val linearError = 0.0f

        val coordinateA: Float
        val coordinateB: Float

        val temp = pool.popVec2()
        val JvAC = pool.popVec2()
        val JvBD = pool.popVec2()
        val JwA: Float
        val JwB: Float
        val JwC: Float
        val JwD: Float
        var mass = 0.0f

        if (m_typeA === JointType.REVOLUTE) {
            JvAC.setZero()
            JwA = 1.0f
            JwC = 1.0f
            mass += m_iA + m_iC

            coordinateA = aA - aC - m_referenceAngleA
        } else {
            val rC = pool.popVec2()
            val rA = pool.popVec2()
            val pC = pool.popVec2()
            val pA = pool.popVec2()
            Rot.mulToOutUnsafe(qC, m_localAxisC, JvAC)
            Rot.mulToOutUnsafe(qC, temp.set(m_localAnchorC).subLocal(m_lcC), rC)
            Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_lcA), rA)
            JwC = Vec2.cross(rC, JvAC)
            JwA = Vec2.cross(rA, JvAC)
            mass += m_mC + m_mA + m_iC * JwC * JwC + m_iA * JwA * JwA

            pC.set(m_localAnchorC).subLocal(m_lcC)
            Rot.mulTransUnsafe(qC, temp.set(rA).addLocal(cA).subLocal(cC), pA)
            coordinateA = Vec2.dot(pA.subLocal(pC), m_localAxisC)
            pool.pushVec2(4)
        }

        if (m_typeB === JointType.REVOLUTE) {
            JvBD.setZero()
            JwB = ratio
            JwD = ratio
            mass += ratio * ratio * (m_iB + m_iD)

            coordinateB = aB - aD - m_referenceAngleB
        } else {
            val u = pool.popVec2()
            val rD = pool.popVec2()
            val rB = pool.popVec2()
            val pD = pool.popVec2()
            val pB = pool.popVec2()
            Rot.mulToOutUnsafe(qD, m_localAxisD, u)
            Rot.mulToOutUnsafe(qD, temp.set(m_localAnchorD).subLocal(m_lcD), rD)
            Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_lcB), rB)
            JvBD.set(u).mulLocal(ratio)
            JwD = Vec2.cross(rD, u)
            JwB = Vec2.cross(rB, u)
            mass += ratio * ratio * (m_mD + m_mB) + m_iD * JwD * JwD + m_iB * JwB * JwB

            pD.set(m_localAnchorD).subLocal(m_lcD)
            Rot.mulTransUnsafe(qD, temp.set(rB).addLocal(cB).subLocal(cD), pB)
            coordinateB = Vec2.dot(pB.subLocal(pD), m_localAxisD)
            pool.pushVec2(5)
        }

        val C = coordinateA + ratio * coordinateB - m_constant

        var impulse = 0.0f
        if (mass > 0.0f) {
            impulse = -C / mass
        }
        pool.pushVec2(3)
        pool.pushRot(4)

        cA.x += m_mA * impulse * JvAC.x
        cA.y += m_mA * impulse * JvAC.y
        aA += m_iA * impulse * JwA

        cB.x += m_mB * impulse * JvBD.x
        cB.y += m_mB * impulse * JvBD.y
        aB += m_iB * impulse * JwB

        cC.x -= m_mC * impulse * JvAC.x
        cC.y -= m_mC * impulse * JvAC.y
        aC -= m_iC * impulse * JwC

        cD.x -= m_mD * impulse * JvBD.x
        cD.y -= m_mD * impulse * JvBD.y
        aD -= m_iD * impulse * JwD

        // data.positions[m_indexA].c = cA;
        data.positions!![m_indexA].a = aA
        // data.positions[m_indexB].c = cB;
        data.positions!![m_indexB].a = aB
        // data.positions[m_indexC].c = cC;
        data.positions!![m_indexC].a = aC
        // data.positions[m_indexD].c = cD;
        data.positions!![m_indexD].a = aD

        // TODO_ERIN not implemented
        return linearError < Settings.linearSlop
    }
}
