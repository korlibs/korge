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
 * Created at 3:38:38 AM Jan 15, 2011
 */
package org.jbox2d.dynamics.joints

import com.soywiz.korma.geom.*
import org.jbox2d.common.Mat33
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.common.Vec3
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

//Point-to-point constraint
//C = p2 - p1
//Cdot = v2 - v1
//   = v2 + cross(w2, r2) - v1 - cross(w1, r1)
//J = [-I -r1_skew I r2_skew ]
//Identity used:
//w k % (rx i + ry j) = w * (-ry i + rx j)

//Angle constraint
//C = angle2 - angle1 - referenceAngle
//Cdot = w2 - w1
//J = [0 0 -1 0 0 1]
//K = invI1 + invI2

/**
 * A weld joint essentially glues two bodies together. A weld joint may distort somewhat because the
 * island constraint solver is approximate.
 *
 * @author Daniel Murphy
 */
class WeldJoint(argWorld: IWorldPool, def: WeldJointDef) : Joint(argWorld, def) {


    var frequency: Float = def.frequencyHz

    var dampingRatio: Float = def.dampingRatio
    private var m_bias: Float = 0.toFloat()

    // Solver shared

    val localAnchorA: Vec2 = Vec2(def.localAnchorA)

    val localAnchorB: Vec2 = Vec2(def.localAnchorB)
    val referenceAngleRadians: Float = def.referenceAngleRadians
    val referenceAngleDegrees: Float get() = referenceAngleRadians * MathUtils.RAD2DEG
    val referenceAngle: Angle get() = referenceAngleRadians.radians
    private var m_gamma: Float = 0.toFloat()
    private val m_impulse: Vec3 = Vec3(0f, 0f, 0f)


    // Solver temp
    private var m_indexA: Int = 0
    private var m_indexB: Int = 0
    private val m_rA = Vec2()
    private val m_rB = Vec2()
    private val m_localCenterA = Vec2()
    private val m_localCenterB = Vec2()
    private var m_invMassA: Float = 0.toFloat()
    private var m_invMassB: Float = 0.toFloat()
    private var m_invIA: Float = 0.toFloat()
    private var m_invIB: Float = 0.toFloat()
    private val m_mass = Mat33()

    override fun getAnchorA(argOut: Vec2) {
        m_bodyA!!.getWorldPointToOut(localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        m_bodyB!!.getWorldPointToOut(localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        argOut.set(m_impulse.x, m_impulse.y)
        argOut.mulLocal(inv_dt)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        return inv_dt * m_impulse.z
    }

    override fun initVelocityConstraints(data: SolverData) {
        m_indexA = m_bodyA!!.m_islandIndex
        m_indexB = m_bodyB!!.m_islandIndex
        m_localCenterA.set(m_bodyA!!.m_sweep.localCenter)
        m_localCenterB.set(m_bodyB!!.m_sweep.localCenter)
        m_invMassA = m_bodyA!!.m_invMass
        m_invMassB = m_bodyB!!.m_invMass
        m_invIA = m_bodyA!!.m_invI
        m_invIB = m_bodyB!!.m_invI

        // Vec2 cA = data.positions[m_indexA].c;
        val aA = data.positions!![m_indexA].a
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w

        // Vec2 cB = data.positions[m_indexB].c;
        val aB = data.positions!![m_indexB].a
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        val qA = pool.popRot()
        val qB = pool.popRot()
        val temp = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), m_rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), m_rB)

        // J = [-I -r1_skew I r2_skew]
        // [ 0 -1 0 1]
        // r_skew = [-ry; rx]

        // Matlab
        // K = [ mA+r1y^2*iA+mB+r2y^2*iB, -r1y*iA*r1x-r2y*iB*r2x, -r1y*iA-r2y*iB]
        // [ -r1y*iA*r1x-r2y*iB*r2x, mA+r1x^2*iA+mB+r2x^2*iB, r1x*iA+r2x*iB]
        // [ -r1y*iA-r2y*iB, r1x*iA+r2x*iB, iA+iB]

        val mA = m_invMassA
        val mB = m_invMassB
        val iA = m_invIA
        val iB = m_invIB

        val K = pool.popMat33()

        K.ex.x = mA + mB + m_rA.y * m_rA.y * iA + m_rB.y * m_rB.y * iB
        K.ey.x = -m_rA.y * m_rA.x * iA - m_rB.y * m_rB.x * iB
        K.ez.x = -m_rA.y * iA - m_rB.y * iB
        K.ex.y = K.ey.x
        K.ey.y = mA + mB + m_rA.x * m_rA.x * iA + m_rB.x * m_rB.x * iB
        K.ez.y = m_rA.x * iA + m_rB.x * iB
        K.ex.z = K.ez.x
        K.ey.z = K.ez.y
        K.ez.z = iA + iB

        if (frequency > 0.0f) {
            K.getInverse22(m_mass)

            var invM = iA + iB
            val m = if (invM > 0.0f) 1.0f / invM else 0.0f

            val C = aB - aA - referenceAngleRadians

            // Frequency
            val omega = 2.0f * MathUtils.PI * frequency

            // Damping coefficient
            val d = 2.0f * m * dampingRatio * omega

            // Spring stiffness
            val k = m * omega * omega

            // magic formulas
            val h = data.step!!.dt
            m_gamma = h * (d + h * k)
            m_gamma = if (m_gamma != 0.0f) 1.0f / m_gamma else 0.0f
            m_bias = C * h * k * m_gamma

            invM += m_gamma
            m_mass.ez.z = if (invM != 0.0f) 1.0f / invM else 0.0f
        } else {
            K.getSymInverse33(m_mass)
            m_gamma = 0.0f
            m_bias = 0.0f
        }

        if (data.step!!.warmStarting) {
            val P = pool.popVec2()
            // Scale impulses to support a variable time step.
            m_impulse.mulLocal(data.step!!.dtRatio)

            P.set(m_impulse.x, m_impulse.y)

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * (Vec2.cross(m_rA, P) + m_impulse.z)

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * (Vec2.cross(m_rB, P) + m_impulse.z)
            pool.pushVec2(1)
        } else {
            m_impulse.setZero()
        }

        //    data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(1)
        pool.pushRot(2)
        pool.pushMat33(1)
    }

    override fun solveVelocityConstraints(data: SolverData) {
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        val mA = m_invMassA
        val mB = m_invMassB
        val iA = m_invIA
        val iB = m_invIB

        val Cdot1 = pool.popVec2()
        val P = pool.popVec2()
        val temp = pool.popVec2()
        if (frequency > 0.0f) {
            val Cdot2 = wB - wA

            val impulse2 = -m_mass.ez.z * (Cdot2 + m_bias + m_gamma * m_impulse.z)
            m_impulse.z += impulse2

            wA -= iA * impulse2
            wB += iB * impulse2

            Vec2.crossToOutUnsafe(wB, m_rB, Cdot1)
            Vec2.crossToOutUnsafe(wA, m_rA, temp)
            Cdot1.addLocal(vB).subLocal(vA).subLocal(temp)

            val impulse1 = P
            Mat33.mul22ToOutUnsafe(m_mass, Cdot1, impulse1)
            impulse1.negateLocal()

            m_impulse.x += impulse1.x
            m_impulse.y += impulse1.y

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * Vec2.cross(m_rA, P)

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * Vec2.cross(m_rB, P)
        } else {
            Vec2.crossToOutUnsafe(wA, m_rA, temp)
            Vec2.crossToOutUnsafe(wB, m_rB, Cdot1)
            Cdot1.addLocal(vB).subLocal(vA).subLocal(temp)
            val Cdot2 = wB - wA

            val Cdot = pool.popVec3()
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2)

            val impulse = pool.popVec3()
            Mat33.mulToOutUnsafe(m_mass, Cdot, impulse)
            impulse.negateLocal()
            m_impulse.addLocal(impulse)

            P.set(impulse.x, impulse.y)

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * (Vec2.cross(m_rA, P) + impulse.z)

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * (Vec2.cross(m_rB, P) + impulse.z)

            pool.pushVec3(2)
        }

        //    data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(3)
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a
        val qA = pool.popRot()
        val qB = pool.popRot()
        val temp = pool.popVec2()
        val rA = pool.popVec2()
        val rB = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        val mA = m_invMassA
        val mB = m_invMassB
        val iA = m_invIA
        val iB = m_invIB

        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), rB)
        val positionError: Float
        val angularError: Float

        val K = pool.popMat33()
        val C1 = pool.popVec2()
        val P = pool.popVec2()

        K.ex.x = mA + mB + rA.y * rA.y * iA + rB.y * rB.y * iB
        K.ey.x = -rA.y * rA.x * iA - rB.y * rB.x * iB
        K.ez.x = -rA.y * iA - rB.y * iB
        K.ex.y = K.ey.x
        K.ey.y = mA + mB + rA.x * rA.x * iA + rB.x * rB.x * iB
        K.ez.y = rA.x * iA + rB.x * iB
        K.ex.z = K.ez.x
        K.ey.z = K.ez.y
        K.ez.z = iA + iB
        if (frequency > 0.0f) {
            C1.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)

            positionError = C1.length()
            angularError = 0.0f

            K.solve22ToOut(C1, P)
            P.negateLocal()

            cA.x -= mA * P.x
            cA.y -= mA * P.y
            aA -= iA * Vec2.cross(rA, P)

            cB.x += mB * P.x
            cB.y += mB * P.y
            aB += iB * Vec2.cross(rB, P)
        } else {
            C1.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
            val C2 = aB - aA - referenceAngleRadians

            positionError = C1.length()
            angularError = MathUtils.abs(C2)

            val C = pool.popVec3()
            val impulse = pool.popVec3()
            C.set(C1.x, C1.y, C2)

            K.solve33ToOut(C, impulse)
            impulse.negateLocal()
            P.set(impulse.x, impulse.y)

            cA.x -= mA * P.x
            cA.y -= mA * P.y
            aA -= iA * (Vec2.cross(rA, P) + impulse.z)

            cB.x += mB * P.x
            cB.y += mB * P.y
            aB += iB * (Vec2.cross(rB, P) + impulse.z)
            pool.pushVec3(2)
        }

        //    data.positions[m_indexA].c.set(cA);
        data.positions!![m_indexA].a = aA
        //    data.positions[m_indexB].c.set(cB);
        data.positions!![m_indexB].a = aB

        pool.pushVec2(5)
        pool.pushRot(2)
        pool.pushMat33(1)

        return positionError <= Settings.linearSlop && angularError <= Settings.angularSlop
    }
}
