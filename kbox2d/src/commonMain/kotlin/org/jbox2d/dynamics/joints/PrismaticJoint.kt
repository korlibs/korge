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

//Linear constraint (point-to-line)
//d = p2 - p1 = x2 + r2 - x1 - r1
//C = dot(perp, d)
//Cdot = dot(d, cross(w1, perp)) + dot(perp, v2 + cross(w2, r2) - v1 - cross(w1, r1))
//   = -dot(perp, v1) - dot(cross(d + r1, perp), w1) + dot(perp, v2) + dot(cross(r2, perp), v2)
//J = [-perp, -cross(d + r1, perp), perp, cross(r2,perp)]
//
//Angular constraint
//C = a2 - a1 + a_initial
//Cdot = w2 - w1
//J = [0 0 -1 0 0 1]
//
//K = J * invM * JT
//
//J = [-a -s1 a s2]
//  [0  -1  0  1]
//a = perp
//s1 = cross(d + r1, a) = cross(p2 - x1, a)
//s2 = cross(r2, a) = cross(p2 - x2, a)


//Motor/Limit linear constraint
//C = dot(ax1, d)
//Cdot = = -dot(ax1, v1) - dot(cross(d + r1, ax1), w1) + dot(ax1, v2) + dot(cross(r2, ax1), v2)
//J = [-ax1 -cross(d+r1,ax1) ax1 cross(r2,ax1)]

//Block Solver
//We develop a block solver that includes the joint limit. This makes the limit stiff (inelastic) even
//when the mass has poor distribution (leading to large torques about the joint anchor points).
//
//The Jacobian has 3 rows:
//J = [-uT -s1 uT s2] // linear
//  [0   -1   0  1] // angular
//  [-vT -a1 vT a2] // limit
//
//u = perp
//v = axis
//s1 = cross(d + r1, u), s2 = cross(r2, u)
//a1 = cross(d + r1, v), a2 = cross(r2, v)

//M * (v2 - v1) = JT * df
//J * v2 = bias
//
//v2 = v1 + invM * JT * df
//J * (v1 + invM * JT * df) = bias
//K * df = bias - J * v1 = -Cdot
//K = J * invM * JT
//Cdot = J * v1 - bias
//
//Now solve for f2.
//df = f2 - f1
//K * (f2 - f1) = -Cdot
//f2 = invK * (-Cdot) + f1
//
//Clamp accumulated limit impulse.
//lower: f2(3) = max(f2(3), 0)
//upper: f2(3) = min(f2(3), 0)
//
//Solve for correct f2(1:2)
//K(1:2, 1:2) * f2(1:2) = -Cdot(1:2) - K(1:2,3) * f2(3) + K(1:2,1:3) * f1
//                    = -Cdot(1:2) - K(1:2,3) * f2(3) + K(1:2,1:2) * f1(1:2) + K(1:2,3) * f1(3)
//K(1:2, 1:2) * f2(1:2) = -Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3)) + K(1:2,1:2) * f1(1:2)
//f2(1:2) = invK(1:2,1:2) * (-Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3))) + f1(1:2)
//
//Now compute impulse to be applied:
//df = f2 - f1

/**
 * A prismatic joint. This joint provides one degree of freedom: translation along an axis fixed in
 * bodyA. Relative rotation is prevented. You can use a joint limit to restrict the range of motion
 * and a joint motor to drive the motion or to model joint friction.
 *
 * @author Daniel
 */
class PrismaticJoint(argWorld: IWorldPool, def: PrismaticJointDef) : Joint(argWorld, def) {

    // Solver shared

    val m_localAnchorA: Vec2 = Vec2(def.localAnchorA)

    val m_localAnchorB: Vec2 = Vec2(def.localAnchorB)

    val m_localXAxisA: Vec2 = Vec2(def.localAxisA)
    protected val m_localYAxisA: Vec2 = Vec2()

    var m_referenceAngleRadians: Float = 0.toFloat()
        protected set
    var m_referenceAngleDegrees: Float
        protected set(value) = run { m_referenceAngleRadians = value * MathUtils.DEG2RAD }
        get() = m_referenceAngleRadians * MathUtils.RAD2DEG
    var m_referenceAngle: Angle
        protected set(value) = run { m_referenceAngleRadians = value.radians.toFloat() }
        get() = m_referenceAngleRadians.radians

    private val m_impulse: Vec3 = Vec3()
    private var m_motorImpulse: Float = 0.toFloat()
    /**
     * Get the lower joint limit, usually in meters.
     *
     * @return
     */
    var lowerLimit: Float = 0.toFloat()
        private set
    /**
     * Get the upper joint limit, usually in meters.
     *
     * @return
     */
    var upperLimit: Float = 0.toFloat()
        private set
    private var m_maxMotorForce: Float = 0.toFloat()
    private var m_motorSpeed: Float = 0.toFloat()
    /**
     * Is the joint limit enabled?
     *
     * @return
     */
    var isLimitEnabled: Boolean = false
        private set
    /**
     * Is the joint motor enabled?
     *
     * @return
     */
    var isMotorEnabled: Boolean = false
        private set
    private var m_limitState: LimitState? = null

    // Solver temp
    private var m_indexA: Int = 0
    private var m_indexB: Int = 0
    private val m_localCenterA = Vec2()
    private val m_localCenterB = Vec2()
    private var m_invMassA: Float = 0.toFloat()
    private var m_invMassB: Float = 0.toFloat()
    private var m_invIA: Float = 0.toFloat()
    private var m_invIB: Float = 0.toFloat()
    private val m_axis: Vec2
    private val m_perp: Vec2
    private var m_s1: Float = 0.toFloat()
    private var m_s2: Float = 0.toFloat()
    private var m_a1: Float = 0.toFloat()
    private var m_a2: Float = 0.toFloat()
    private val m_K: Mat33
    private var m_motorMass: Float = 0.toFloat() // effective mass for motor/limit translational constraint.

    /**
     * Get the current joint translation, usually in meters.
     */
    val jointSpeed: Float
        get() {
            val bA = bodyA
            val bB = bodyB

            val temp = pool.popVec2()
            val rA = pool.popVec2()
            val rB = pool.popVec2()
            val p1 = pool.popVec2()
            val p2 = pool.popVec2()
            val d = pool.popVec2()
            val axis = pool.popVec2()
            val temp2 = pool.popVec2()
            val temp3 = pool.popVec2()

            temp.set(m_localAnchorA).subLocal(bA!!.sweep.localCenter)
            Rot.mulToOutUnsafe(bA.xf.q, temp, rA)

            temp.set(m_localAnchorB).subLocal(bB!!.sweep.localCenter)
            Rot.mulToOutUnsafe(bB.xf.q, temp, rB)

            p1.set(bA.sweep.c).addLocal(rA)
            p2.set(bB.sweep.c).addLocal(rB)

            d.set(p2).subLocal(p1)
            Rot.mulToOutUnsafe(bA.xf.q, m_localXAxisA, axis)

            val vA = bA._linearVelocity
            val vB = bB._linearVelocity
            val wA = bA._angularVelocity
            val wB = bB._angularVelocity


            Vec2.crossToOutUnsafe(wA, axis, temp)
            Vec2.crossToOutUnsafe(wB, rB, temp2)
            Vec2.crossToOutUnsafe(wA, rA, temp3)

            temp2.addLocal(vB).subLocal(vA).subLocal(temp3)
            val speed = Vec2.dot(d, temp) + Vec2.dot(axis, temp2)

            pool.pushVec2(9)

            return speed
        }

    val jointTranslation: Float
        get() {
            val pA = pool.popVec2()
            val pB = pool.popVec2()
            val axis = pool.popVec2()
            bodyA!!.getWorldPointToOut(m_localAnchorA, pA)
            bodyB!!.getWorldPointToOut(m_localAnchorB, pB)
            bodyA!!.getWorldVectorToOutUnsafe(m_localXAxisA, axis)
            pB.subLocal(pA)
            val translation = Vec2.dot(pB, axis)
            pool.pushVec2(3)
            return translation
        }

    /**
     * Get the motor speed, usually in meters per second.
     *
     * @return
     */
    /**
     * Set the motor speed, usually in meters per second.
     *
     * @param speed
     */
    var motorSpeed: Float
        get() = m_motorSpeed
        set(speed) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            m_motorSpeed = speed
        }

    /**
     * Set the maximum motor force, usually in N.
     *
     * @param force
     */
    var maxMotorForce: Float
        get() = m_maxMotorForce
        set(force) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            m_maxMotorForce = force
        }

    init {
        lowerLimit = def.lowerTranslation
        upperLimit = def.upperTranslation
        m_maxMotorForce = def.maxMotorForce
        m_motorSpeed = def.motorSpeed
        isLimitEnabled = def.enableLimit
        isMotorEnabled = def.enableMotor
        m_limitState = LimitState.INACTIVE
        m_K = Mat33()
        m_axis = Vec2()
        m_perp = Vec2()
        m_motorMass = 0.0f
        m_motorImpulse = 0.0f

        m_localXAxisA.normalize()
        Vec2.crossToOutUnsafe(1f, m_localXAxisA, m_localYAxisA)
        m_referenceAngleRadians = def.referenceAngleRadians


    }

    override fun getAnchorA(argOut: Vec2) {
        bodyA!!.getWorldPointToOut(m_localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        bodyB!!.getWorldPointToOut(m_localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        val temp = pool.popVec2()
        temp.set(m_axis).mulLocal(m_motorImpulse + m_impulse.z)
        argOut.set(m_perp).mulLocal(m_impulse.x).addLocal(temp).mulLocal(inv_dt)
        pool.pushVec2(1)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        return inv_dt * m_impulse.y
    }

    /**
     * Enable/disable the joint limit.
     *
     * @param flag
     */
    fun enableLimit(flag: Boolean) {
        if (flag != isLimitEnabled) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            isLimitEnabled = flag
            m_impulse.z = 0.0f
        }
    }

    /**
     * Set the joint limits, usually in meters.
     *
     * @param lower
     * @param upper
     */
    fun setLimits(lower: Float, upper: Float) {
        assert(lower <= upper)
        if (lower != lowerLimit || upper != upperLimit) {
            bodyA!!.isAwake = true
            bodyB!!.isAwake = true
            lowerLimit = lower
            upperLimit = upper
            m_impulse.z = 0.0f
        }
    }

    /**
     * Enable/disable the joint motor.
     *
     * @param flag
     */
    fun enableMotor(flag: Boolean) {
        bodyA!!.isAwake = true
        bodyB!!.isAwake = true
        isMotorEnabled = flag
    }

    /**
     * Get the current motor force, usually in N.
     *
     * @param inv_dt
     * @return
     */
    fun getMotorForce(inv_dt: Float): Float {
        return m_motorImpulse * inv_dt
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

        val cA = data.positions!![m_indexA].c
        val aA = data.positions!![m_indexA].a
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w

        val cB = data.positions!![m_indexB].c
        val aB = data.positions!![m_indexB].a
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        val qA = pool.popRot()
        val qB = pool.popRot()
        val d = pool.popVec2()
        val temp = pool.popVec2()
        val rA = pool.popVec2()
        val rB = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, d.set(m_localAnchorA).subLocal(m_localCenterA), rA)
        Rot.mulToOutUnsafe(qB, d.set(m_localAnchorB).subLocal(m_localCenterB), rB)
        d.set(cB).subLocal(cA).addLocal(rB).subLocal(rA)

        val mA = m_invMassA
        val mB = m_invMassB
        val iA = m_invIA
        val iB = m_invIB

        // Compute motor Jacobian and effective mass.
        run {
            Rot.mulToOutUnsafe(qA, m_localXAxisA, m_axis)
            temp.set(d).addLocal(rA)
            m_a1 = Vec2.cross(temp, m_axis)
            m_a2 = Vec2.cross(rB, m_axis)

            m_motorMass = mA + mB + iA * m_a1 * m_a1 + iB * m_a2 * m_a2
            if (m_motorMass > 0.0f) {
                m_motorMass = 1.0f / m_motorMass
            }
        }

        // Prismatic constraint.
        run {
            Rot.mulToOutUnsafe(qA, m_localYAxisA, m_perp)

            temp.set(d).addLocal(rA)
            m_s1 = Vec2.cross(temp, m_perp)
            m_s2 = Vec2.cross(rB, m_perp)

            val k11 = mA + mB + iA * m_s1 * m_s1 + iB * m_s2 * m_s2
            val k12 = iA * m_s1 + iB * m_s2
            val k13 = iA * m_s1 * m_a1 + iB * m_s2 * m_a2
            var k22 = iA + iB
            if (k22 == 0.0f) {
                // For bodies with fixed rotation.
                k22 = 1.0f
            }
            val k23 = iA * m_a1 + iB * m_a2
            val k33 = mA + mB + iA * m_a1 * m_a1 + iB * m_a2 * m_a2

            m_K.ex.set(k11, k12, k13)
            m_K.ey.set(k12, k22, k23)
            m_K.ez.set(k13, k23, k33)
        }

        // Compute motor and limit terms.
        if (isLimitEnabled) {

            val jointTranslation = Vec2.dot(m_axis, d)
            if (MathUtils.abs(upperLimit - lowerLimit) < 2.0f * Settings.linearSlop) {
                m_limitState = LimitState.EQUAL
            } else if (jointTranslation <= lowerLimit) {
                if (m_limitState !== LimitState.AT_LOWER) {
                    m_limitState = LimitState.AT_LOWER
                    m_impulse.z = 0.0f
                }
            } else if (jointTranslation >= upperLimit) {
                if (m_limitState !== LimitState.AT_UPPER) {
                    m_limitState = LimitState.AT_UPPER
                    m_impulse.z = 0.0f
                }
            } else {
                m_limitState = LimitState.INACTIVE
                m_impulse.z = 0.0f
            }
        } else {
            m_limitState = LimitState.INACTIVE
            m_impulse.z = 0.0f
        }

        if (isMotorEnabled == false) {
            m_motorImpulse = 0.0f
        }

        if (data.step!!.warmStarting) {
            // Account for variable time step.
            m_impulse.mulLocal(data.step!!.dtRatio)
            m_motorImpulse *= data.step!!.dtRatio

            val P = pool.popVec2()
            temp.set(m_axis).mulLocal(m_motorImpulse + m_impulse.z)
            P.set(m_perp).mulLocal(m_impulse.x).addLocal(temp)

            val LA = m_impulse.x * m_s1 + m_impulse.y + (m_motorImpulse + m_impulse.z) * m_a1
            val LB = m_impulse.x * m_s2 + m_impulse.y + (m_motorImpulse + m_impulse.z) * m_a2

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * LA

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * LB

            pool.pushVec2(1)
        } else {
            m_impulse.setZero()
            m_motorImpulse = 0.0f
        }

        // data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushRot(2)
        pool.pushVec2(4)
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

        val temp = pool.popVec2()

        // Solve linear motor constraint.
        if (isMotorEnabled && m_limitState !== LimitState.EQUAL) {
            temp.set(vB).subLocal(vA)
            val Cdot = Vec2.dot(m_axis, temp) + m_a2 * wB - m_a1 * wA
            var impulse = m_motorMass * (m_motorSpeed - Cdot)
            val oldImpulse = m_motorImpulse
            val maxImpulse = data.step!!.dt * m_maxMotorForce
            m_motorImpulse = MathUtils.clamp(m_motorImpulse + impulse, -maxImpulse, maxImpulse)
            impulse = m_motorImpulse - oldImpulse

            val P = pool.popVec2()
            P.set(m_axis).mulLocal(impulse)
            val LA = impulse * m_a1
            val LB = impulse * m_a2

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * LA

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * LB

            pool.pushVec2(1)
        }

        val Cdot1 = pool.popVec2()
        temp.set(vB).subLocal(vA)
        Cdot1.x = Vec2.dot(m_perp, temp) + m_s2 * wB - m_s1 * wA
        Cdot1.y = wB - wA
        // System.out.println(Cdot1);

        if (isLimitEnabled && m_limitState !== LimitState.INACTIVE) {
            // Solve prismatic and limit constraint in block form.
            val Cdot2: Float
            temp.set(vB).subLocal(vA)
            Cdot2 = Vec2.dot(m_axis, temp) + m_a2 * wB - m_a1 * wA

            val Cdot = pool.popVec3()
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2)

            val f1 = pool.popVec3()
            val df = pool.popVec3()

            f1.set(m_impulse)
            m_K.solve33ToOut(Cdot.negateLocal(), df)
            // Cdot.negateLocal(); not used anymore
            m_impulse.addLocal(df)

            if (m_limitState === LimitState.AT_LOWER) {
                m_impulse.z = MathUtils.max(m_impulse.z, 0.0f)
            } else if (m_limitState === LimitState.AT_UPPER) {
                m_impulse.z = MathUtils.min(m_impulse.z, 0.0f)
            }

            // f2(1:2) = invK(1:2,1:2) * (-Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3))) +
            // f1(1:2)
            val b = pool.popVec2()
            val f2r = pool.popVec2()

            temp.set(m_K.ez.x, m_K.ez.y).mulLocal(m_impulse.z - f1.z)
            b.set(Cdot1).negateLocal().subLocal(temp)

            m_K.solve22ToOut(b, f2r)
            f2r.addLocal(f1.x, f1.y)
            m_impulse.x = f2r.x
            m_impulse.y = f2r.y

            df.set(m_impulse).subLocal(f1)

            val P = pool.popVec2()
            temp.set(m_axis).mulLocal(df.z)
            P.set(m_perp).mulLocal(df.x).addLocal(temp)

            val LA = df.x * m_s1 + df.y + df.z * m_a1
            val LB = df.x * m_s2 + df.y + df.z * m_a2

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * LA

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * LB

            pool.pushVec2(3)
            pool.pushVec3(3)
        } else {
            // Limit is inactive, just solve the prismatic constraint in block form.
            val df = pool.popVec2()
            m_K.solve22ToOut(Cdot1.negateLocal(), df)
            Cdot1.negateLocal()

            m_impulse.x += df.x
            m_impulse.y += df.y

            val P = pool.popVec2()
            P.set(m_perp).mulLocal(df.x)
            val LA = df.x * m_s1 + df.y
            val LB = df.x * m_s2 + df.y

            vA.x -= mA * P.x
            vA.y -= mA * P.y
            wA -= iA * LA

            vB.x += mB * P.x
            vB.y += mB * P.y
            wB += iB * LB

            pool.pushVec2(2)
        }

        // data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(2)
    }


    override fun solvePositionConstraints(data: SolverData): Boolean {

        val qA = pool.popRot()
        val qB = pool.popRot()
        val rA = pool.popVec2()
        val rB = pool.popVec2()
        val d = pool.popVec2()
        val axis = pool.popVec2()
        val perp = pool.popVec2()
        val temp = pool.popVec2()
        val C1 = pool.popVec2()

        val impulse = pool.popVec3()

        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a

        qA.setRadians(aA)
        qB.setRadians(aB)

        val mA = m_invMassA
        val mB = m_invMassB
        val iA = m_invIA
        val iB = m_invIB

        // Compute fresh Jacobians
        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_localCenterA), rA)
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), rB)
        d.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)

        Rot.mulToOutUnsafe(qA, m_localXAxisA, axis)
        val a1 = Vec2.cross(temp.set(d).addLocal(rA), axis)
        val a2 = Vec2.cross(rB, axis)
        Rot.mulToOutUnsafe(qA, m_localYAxisA, perp)

        val s1 = Vec2.cross(temp.set(d).addLocal(rA), perp)
        val s2 = Vec2.cross(rB, perp)

        C1.x = Vec2.dot(perp, d)
        C1.y = aB - aA - m_referenceAngleRadians

        var linearError = MathUtils.abs(C1.x)
        val angularError = MathUtils.abs(C1.y)

        var active = false
        var C2 = 0.0f
        if (isLimitEnabled) {
            val translation = Vec2.dot(axis, d)
            if (MathUtils.abs(upperLimit - lowerLimit) < 2.0f * Settings.linearSlop) {
                // Prevent large angular corrections
                C2 = MathUtils.clamp(translation, -Settings.maxLinearCorrection,
                        Settings.maxLinearCorrection)
                linearError = MathUtils.max(linearError, MathUtils.abs(translation))
                active = true
            } else if (translation <= lowerLimit) {
                // Prevent large linear corrections and allow some slop.
                C2 = MathUtils.clamp(translation - lowerLimit + Settings.linearSlop,
                        -Settings.maxLinearCorrection, 0.0f)
                linearError = MathUtils.max(linearError, lowerLimit - translation)
                active = true
            } else if (translation >= upperLimit) {
                // Prevent large linear corrections and allow some slop.
                C2 = MathUtils.clamp(translation - upperLimit - Settings.linearSlop, 0.0f,
                        Settings.maxLinearCorrection)
                linearError = MathUtils.max(linearError, translation - upperLimit)
                active = true
            }
        }

        if (active) {
            val k11 = mA + mB + iA * s1 * s1 + iB * s2 * s2
            val k12 = iA * s1 + iB * s2
            val k13 = iA * s1 * a1 + iB * s2 * a2
            var k22 = iA + iB
            if (k22 == 0.0f) {
                // For fixed rotation
                k22 = 1.0f
            }
            val k23 = iA * a1 + iB * a2
            val k33 = mA + mB + iA * a1 * a1 + iB * a2 * a2

            val K = pool.popMat33()
            K.ex.set(k11, k12, k13)
            K.ey.set(k12, k22, k23)
            K.ez.set(k13, k23, k33)

            val C = pool.popVec3()
            C.x = C1.x
            C.y = C1.y
            C.z = C2

            K.solve33ToOut(C.negateLocal(), impulse)
            pool.pushVec3(1)
            pool.pushMat33(1)
        } else {
            val k11 = mA + mB + iA * s1 * s1 + iB * s2 * s2
            val k12 = iA * s1 + iB * s2
            var k22 = iA + iB
            if (k22 == 0.0f) {
                k22 = 1.0f
            }

            val K = pool.popMat22()
            K.ex.set(k11, k12)
            K.ey.set(k12, k22)

            // temp is impulse1
            K.solveToOut(C1.negateLocal(), temp)
            C1.negateLocal()

            impulse.x = temp.x
            impulse.y = temp.y
            impulse.z = 0.0f

            pool.pushMat22(1)
        }

        val Px = impulse.x * perp.x + impulse.z * axis.x
        val Py = impulse.x * perp.y + impulse.z * axis.y
        val LA = impulse.x * s1 + impulse.y + impulse.z * a1
        val LB = impulse.x * s2 + impulse.y + impulse.z * a2

        cA.x -= mA * Px
        cA.y -= mA * Py
        aA -= iA * LA
        cB.x += mB * Px
        cB.y += mB * Py
        aB += iB * LB

        // data.positions[m_indexA].c.set(cA);
        data.positions!![m_indexA].a = aA
        // data.positions[m_indexB].c.set(cB);
        data.positions!![m_indexB].a = aB

        pool.pushVec2(7)
        pool.pushVec3(1)
        pool.pushRot(2)

        return linearError <= Settings.linearSlop && angularError <= Settings.angularSlop
    }
}
