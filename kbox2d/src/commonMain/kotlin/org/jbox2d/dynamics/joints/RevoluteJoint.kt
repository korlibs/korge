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
package org.jbox2d.dynamics.joints

import com.soywiz.korma.geom.*
import org.jbox2d.common.Mat33
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.common.Vec3
import org.jbox2d.dynamics.SolverData
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

//Point-to-point constraint
//C = p2 - p1
//Cdot = v2 - v1
//   = v2 + cross(w2, r2) - v1 - cross(w1, r1)
//J = [-I -r1_skew I r2_skew ]
//Identity used:
//w k % (rx i + ry j) = w * (-ry i + rx j)

//Motor constraint
//Cdot = w2 - w1
//J = [0 0 -1 0 0 1]
//K = invI1 + invI2

/**
 * A revolute joint constrains two bodies to share a common point while they are free to rotate
 * about the point. The relative rotation about the shared point is the joint angle. You can limit
 * the relative rotation with a joint limit that specifies a lower and upper angle. You can use a
 * motor to drive the relative rotation about the shared point. A maximum motor torque is provided
 * so that infinite forces are not generated.
 *
 * @author Daniel Murphy
 */
class RevoluteJoint(argWorld: IWorldPool, def: RevoluteJointDef) : Joint(argWorld, def) {

    // Solver shared

    val m_localAnchorA = Vec2()

    val m_localAnchorB = Vec2()
    private val m_impulse = Vec3()
    private var m_motorImpulse: Float = 0.toFloat()

    var isMotorEnabled: Boolean = false
        private set
    private var m_maxMotorTorque: Float = 0.toFloat()
    private var m_motorSpeed: Float = 0.toFloat()

    var isLimitEnabled: Boolean = false
        private set
    var m_referenceAngleRadians: Float = 0.toFloat()
        protected set
    var m_referenceAngleDegrees: Float
        protected set(value) = run { m_referenceAngleRadians = value * MathUtils.DEG2RAD }
        get() = m_referenceAngleRadians * MathUtils.RAD2DEG
    var m_referenceAngle: Angle
        protected set(value) = run { m_referenceAngleRadians = value.radians.toFloat() }
        get() = m_referenceAngleRadians.radians
    var lowerLimit: Float = 0.toFloat()
        private set
    var upperLimit: Float = 0.toFloat()
        private set

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
    private val m_mass = Mat33() // effective mass for point-to-point constraint.
    private var m_motorMass: Float = 0.toFloat() // effective mass for motor/limit angular constraint.
    private var m_limitState: LimitState? = null

    val jointAngleRadians: Float
        get() {
            val b1 = bodyA
            val b2 = bodyB
            return b2!!.sweep.a - b1!!.sweep.a - m_referenceAngleRadians
        }

    val jointAngleDegrees: Float get() = jointAngleRadians * MathUtils.RAD2DEG

    val jointAngle: Angle get() = jointAngleRadians.radians

    val jointSpeed: Float
        get() {
            val b1 = bodyA
            val b2 = bodyB
            return b2!!._angularVelocity - b1!!._angularVelocity
        }

    var motorSpeed: Float
        get() = m_motorSpeed
        set(speed) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            m_motorSpeed = speed
        }

    var maxMotorTorque: Float
        get() = m_maxMotorTorque
        set(torque) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            m_maxMotorTorque = torque
        }

    init {
        m_localAnchorA.set(def.localAnchorA)
        m_localAnchorB.set(def.localAnchorB)
        m_referenceAngleRadians = def.referenceAngleRadians

        m_motorImpulse = 0f

        lowerLimit = def.lowerAngleRadians
        upperLimit = def.upperAngleRadians
        m_maxMotorTorque = def.maxMotorTorque
        m_motorSpeed = def.motorSpeed
        isLimitEnabled = def.enableLimit
        isMotorEnabled = def.enableMotor
        m_limitState = LimitState.INACTIVE
    }

    override fun initVelocityConstraints(data: SolverData) {
        m_indexA = bodyA!!.islandIndex
        m_indexB = bodyB!!.islandIndex
        m_localCenterA.set(bodyA!!.sweep.localCenter)
        m_localCenterB.set(bodyB!!.sweep.localCenter)
        m_invMassA = bodyA!!.m_invMass
        m_invMassB = bodyB!!.m_invMass
        m_invIA = bodyA!!.m_invI
        m_invIB = bodyB!!.m_invI

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
        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_localCenterA), m_rA)
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), m_rB)

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

        val fixedRotation = iA + iB == 0.0f

        m_mass.ex.x = mA + mB + m_rA.y * m_rA.y * iA + m_rB.y * m_rB.y * iB
        m_mass.ey.x = -m_rA.y * m_rA.x * iA - m_rB.y * m_rB.x * iB
        m_mass.ez.x = -m_rA.y * iA - m_rB.y * iB
        m_mass.ex.y = m_mass.ey.x
        m_mass.ey.y = mA + mB + m_rA.x * m_rA.x * iA + m_rB.x * m_rB.x * iB
        m_mass.ez.y = m_rA.x * iA + m_rB.x * iB
        m_mass.ex.z = m_mass.ez.x
        m_mass.ey.z = m_mass.ez.y
        m_mass.ez.z = iA + iB

        m_motorMass = iA + iB
        if (m_motorMass > 0.0f) {
            m_motorMass = 1.0f / m_motorMass
        }

        if (isMotorEnabled == false || fixedRotation) {
            m_motorImpulse = 0.0f
        }

        if (isLimitEnabled && fixedRotation == false) {
            val jointAngle = aB - aA - m_referenceAngleRadians
            if (MathUtils.abs(upperLimit - lowerLimit) < 2.0f * Settings.angularSlop) {
                m_limitState = LimitState.EQUAL
            } else if (jointAngle <= lowerLimit) {
                if (m_limitState !== LimitState.AT_LOWER) {
                    m_impulse.z = 0.0f
                }
                m_limitState = LimitState.AT_LOWER
            } else if (jointAngle >= upperLimit) {
                if (m_limitState !== LimitState.AT_UPPER) {
                    m_impulse.z = 0.0f
                }
                m_limitState = LimitState.AT_UPPER
            } else {
                m_limitState = LimitState.INACTIVE
                m_impulse.z = 0.0f
            }
        } else {
            m_limitState = LimitState.INACTIVE
        }

        if (data.step!!.warmStarting) {
            val P = pool.popVec2()
            // Scale impulses to support a variable time step.
            m_impulse.x *= data.step!!.dtRatio
            m_impulse.y *= data.step!!.dtRatio
            m_motorImpulse *= data.step!!.dtRatio

            P.x = m_impulse.x
            P.y = m_impulse.y

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * (Vec2.cross(m_rA, P) + m_motorImpulse + m_impulse.z)

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * (Vec2.cross(m_rB, P) + m_motorImpulse + m_impulse.z)
            pool.pushVec2(1)
        } else {
            m_impulse.setZero()
            m_motorImpulse = 0.0f
        }
        // data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(1)
        pool.pushRot(2)
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

        val fixedRotation = iA + iB == 0.0f

        // Solve motor constraint.
        if (isMotorEnabled && m_limitState !== LimitState.EQUAL && fixedRotation == false) {
            val Cdot = wB - wA - m_motorSpeed
            var impulse = -m_motorMass * Cdot
            val oldImpulse = m_motorImpulse
            val maxImpulse = data.step!!.dt * m_maxMotorTorque
            m_motorImpulse = MathUtils.clamp(m_motorImpulse + impulse, -maxImpulse, maxImpulse)
            impulse = m_motorImpulse - oldImpulse

            wA -= iA * impulse
            wB += iB * impulse
        }
        val temp = pool.popVec2()

        // Solve limit constraint.
        if (isLimitEnabled && m_limitState !== LimitState.INACTIVE && fixedRotation == false) {

            val Cdot1 = pool.popVec2()
            val Cdot = pool.popVec3()

            // Solve point-to-point constraint
            Vec2.crossToOutUnsafe(wA, m_rA, temp)
            Vec2.crossToOutUnsafe(wB, m_rB, Cdot1)
            Cdot1.addLocal(vB).subLocal(vA).subLocal(temp)
            val Cdot2 = wB - wA
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2)

            val impulse = pool.popVec3()
            m_mass.solve33ToOut(Cdot, impulse)
            impulse.negateLocal()

            if (m_limitState === LimitState.EQUAL) {
                m_impulse.addLocal(impulse)
            } else if (m_limitState === LimitState.AT_LOWER) {
                val newImpulse = m_impulse.z + impulse.z
                if (newImpulse < 0.0f) {
                    val rhs = pool.popVec2()
                    rhs.set(m_mass.ez.x, m_mass.ez.y).mulLocal(m_impulse.z).subLocal(Cdot1)
                    m_mass.solve22ToOut(rhs, temp)
                    impulse.x = temp.x
                    impulse.y = temp.y
                    impulse.z = -m_impulse.z
                    m_impulse.x += temp.x
                    m_impulse.y += temp.y
                    m_impulse.z = 0.0f
                    pool.pushVec2(1)
                } else {
                    m_impulse.addLocal(impulse)
                }
            } else if (m_limitState === LimitState.AT_UPPER) {
                val newImpulse = m_impulse.z + impulse.z
                if (newImpulse > 0.0f) {
                    val rhs = pool.popVec2()
                    rhs.set(m_mass.ez.x, m_mass.ez.y).mulLocal(m_impulse.z).subLocal(Cdot1)
                    m_mass.solve22ToOut(rhs, temp)
                    impulse.x = temp.x
                    impulse.y = temp.y
                    impulse.z = -m_impulse.z
                    m_impulse.x += temp.x
                    m_impulse.y += temp.y
                    m_impulse.z = 0.0f
                    pool.pushVec2(1)
                } else {
                    m_impulse.addLocal(impulse)
                }
            }
            val P = pool.popVec2()

            P.set(impulse.x, impulse.y)

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * (Vec2.cross(m_rA, P) + impulse.z)

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * (Vec2.cross(m_rB, P) + impulse.z)

            pool.pushVec2(2)
            pool.pushVec3(2)
        } else {

            // Solve point-to-point constraint
            val Cdot = pool.popVec2()
            val impulse = pool.popVec2()

            Vec2.crossToOutUnsafe(wA, m_rA, temp)
            Vec2.crossToOutUnsafe(wB, m_rB, Cdot)
            Cdot.addLocal(vB).subLocal(vA).subLocal(temp)
            m_mass.solve22ToOut(Cdot.negateLocal(), impulse) // just leave negated

            m_impulse.x += impulse.x
            m_impulse.y += impulse.y

            vA.x -= mA * impulse.x
            vA.y -= mA * impulse.y
            wA -= iA * Vec2.cross(m_rA, impulse)

            vB.x += mB * impulse.x
            vB.y += mB * impulse.y
            wB += iB * Vec2.cross(m_rB, impulse)

            pool.pushVec2(2)
        }

        // data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(1)
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        val qA = pool.popRot()
        val qB = pool.popRot()
        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a

        qA.setRadians(aA)
        qB.setRadians(aB)

        var angularError = 0.0f
        var positionError = 0.0f

        val fixedRotation = m_invIA + m_invIB == 0.0f

        // Solve angular limit constraint.
        if (isLimitEnabled && m_limitState !== LimitState.INACTIVE && fixedRotation == false) {
            val angle = aB - aA - m_referenceAngleRadians
            var limitImpulse = 0.0f

            if (m_limitState === LimitState.EQUAL) {
                // Prevent large angular corrections
                val C = MathUtils.clamp(angle - lowerLimit, -Settings.maxAngularCorrection,
                        Settings.maxAngularCorrection)
                limitImpulse = -m_motorMass * C
                angularError = MathUtils.abs(C)
            } else if (m_limitState === LimitState.AT_LOWER) {
                var C = angle - lowerLimit
                angularError = -C

                // Prevent large angular corrections and allow some slop.
                C = MathUtils.clamp(C + Settings.angularSlop, -Settings.maxAngularCorrection, 0.0f)
                limitImpulse = -m_motorMass * C
            } else if (m_limitState === LimitState.AT_UPPER) {
                var C = angle - upperLimit
                angularError = C

                // Prevent large angular corrections and allow some slop.
                C = MathUtils.clamp(C - Settings.angularSlop, 0.0f, Settings.maxAngularCorrection)
                limitImpulse = -m_motorMass * C
            }

            aA -= m_invIA * limitImpulse
            aB += m_invIB * limitImpulse
        }
        // Solve point-to-point constraint.
        run {
            qA.setRadians(aA)
            qB.setRadians(aB)

            val rA = pool.popVec2()
            val rB = pool.popVec2()
            val C = pool.popVec2()
            val impulse = pool.popVec2()

            Rot.mulToOutUnsafe(qA, C.set(m_localAnchorA).subLocal(m_localCenterA), rA)
            Rot.mulToOutUnsafe(qB, C.set(m_localAnchorB).subLocal(m_localCenterB), rB)
            C.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
            positionError = C.length()

            val mA = m_invMassA
            val mB = m_invMassB
            val iA = m_invIA
            val iB = m_invIB

            val K = pool.popMat22()
            K.ex.x = mA + mB + iA * rA.y * rA.y + iB * rB.y * rB.y
            K.ex.y = -iA * rA.x * rA.y - iB * rB.x * rB.y
            K.ey.x = K.ex.y
            K.ey.y = mA + mB + iA * rA.x * rA.x + iB * rB.x * rB.x
            K.solveToOut(C, impulse)
            impulse.negateLocal()

            cA.x -= mA * impulse.x
            cA.y -= mA * impulse.y
            aA -= iA * Vec2.cross(rA, impulse)

            cB.x += mB * impulse.x
            cB.y += mB * impulse.y
            aB += iB * Vec2.cross(rB, impulse)

            pool.pushVec2(4)
            pool.pushMat22(1)
        }
        // data.positions[m_indexA].c.set(cA);
        data.positions!![m_indexA].a = aA
        // data.positions[m_indexB].c.set(cB);
        data.positions!![m_indexB].a = aB

        pool.pushRot(2)

        return positionError <= Settings.linearSlop && angularError <= Settings.angularSlop
    }

    override fun getAnchorA(argOut: Vec2) {
        bodyA!!.getWorldPointToOut(m_localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        bodyB!!.getWorldPointToOut(m_localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        argOut.set(m_impulse.x, m_impulse.y).mulLocal(inv_dt)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        return inv_dt * m_impulse.z
    }

    fun enableMotor(flag: Boolean) {
        bodyA!!.isAwake = true
        bodyB!!.isAwake = true
        isMotorEnabled = flag
    }

    fun getMotorTorque(inv_dt: Float): Float {
        return m_motorImpulse * inv_dt
    }

    fun enableLimit(flag: Boolean) {
        if (flag != isLimitEnabled) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            isLimitEnabled = flag
            m_impulse.z = 0.0f
        }
    }

    fun setLimits(lower: Float, upper: Float) {
        assert(lower <= upper)
        if (lower != lowerLimit || upper != upperLimit) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            m_impulse.z = 0.0f
            lowerLimit = lower
            upperLimit = upper
        }
    }
}
