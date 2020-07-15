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

import org.jbox2d.common.Mat22
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

/**
 * A mouse joint is used to make a point on a body track a specified world point. This a soft
 * constraint with a maximum force. This allows the constraint to stretch and without applying huge
 * forces. NOTE: this joint is not documented in the manual because it was developed to be used in
 * the testbed. If you want to learn how to use the mouse joint, look at the testbed.
 *
 * @author Daniel
 */
class MouseJoint constructor(argWorld: IWorldPool, def: MouseJointDef) : Joint(argWorld, def) {
    private val m_localAnchorB = Vec2()
    var target = Vec2()
        set(target) {
            if (m_bodyB!!.isAwake == false) {
                m_bodyB!!.isAwake = true
            }
            this.target.set(target)
        }
    // / set/get the frequency in Hertz.

    var frequency: Float = 0.toFloat()
    // / set/get the damping ratio (dimensionless).

    var dampingRatio: Float = 0.toFloat()
    private var m_beta: Float = 0.toFloat()

    // Solver shared
    private val m_impulse = Vec2()
    // / set/get the maximum force in Newtons.

    var maxForce: Float = 0.toFloat()
    private var m_gamma: Float = 0.toFloat()

    // Solver temp
    private var m_indexB: Int = 0
    private val m_rB = Vec2()
    private val m_localCenterB = Vec2()
    private var m_invMassB: Float = 0.toFloat()
    private var m_invIB: Float = 0.toFloat()
    private val m_mass = Mat22()
    private val m_C = Vec2()

    init {
        assert(def.target.isValid)
        assert(def.maxForce >= 0)
        assert(def.frequencyHz >= 0)
        assert(def.dampingRatio >= 0)

        target.set(def.target)
        Transform.mulTransToOutUnsafe(m_bodyB!!.m_xf, target, m_localAnchorB)

        maxForce = def.maxForce
        m_impulse.setZero()

        frequency = def.frequencyHz
        dampingRatio = def.dampingRatio

        m_beta = 0f
        m_gamma = 0f
    }

    override fun getAnchorA(argOut: Vec2) {
        argOut.set(target)
    }

    override fun getAnchorB(argOut: Vec2) {
        m_bodyB!!.getWorldPointToOut(m_localAnchorB, argOut)
    }

    override fun getReactionForce(invDt: Float, argOut: Vec2) {
        argOut.set(m_impulse).mulLocal(invDt)
    }

    override fun getReactionTorque(invDt: Float): Float {
        return invDt * 0.0f
    }

    override fun initVelocityConstraints(data: SolverData) {
        m_indexB = m_bodyB!!.m_islandIndex
        m_localCenterB.set(m_bodyB!!.m_sweep.localCenter)
        m_invMassB = m_bodyB!!.m_invMass
        m_invIB = m_bodyB!!.m_invI

        val cB = data.positions!![m_indexB].c
        val aB = data.positions!![m_indexB].a
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        val qB = pool.popRot()

        qB.setRadians(aB)

        val mass = m_bodyB!!.m_mass

        // Frequency
        val omega = 2.0f * MathUtils.PI * frequency

        // Damping coefficient
        val d = 2.0f * mass * dampingRatio * omega

        // Spring stiffness
        val k = mass * (omega * omega)

        // magic formulas
        // gamma has units of inverse mass.
        // beta has units of inverse time.
        val h = data.step!!.dt
        assert(d + h * k > Settings.EPSILON)
        m_gamma = h * (d + h * k)
        if (m_gamma != 0.0f) {
            m_gamma = 1.0f / m_gamma
        }
        m_beta = h * k * m_gamma

        val temp = pool.popVec2()

        // Compute the effective mass matrix.
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), m_rB)

        // K = [(1/m1 + 1/m2) * eye(2) - skew(r1) * invI1 * skew(r1) - skew(r2) * invI2 * skew(r2)]
        // = [1/m1+1/m2 0 ] + invI1 * [r1.y*r1.y -r1.x*r1.y] + invI2 * [r1.y*r1.y -r1.x*r1.y]
        // [ 0 1/m1+1/m2] [-r1.x*r1.y r1.x*r1.x] [-r1.x*r1.y r1.x*r1.x]
        val K = pool.popMat22()
        K.ex.x = m_invMassB + m_invIB * m_rB.y * m_rB.y + m_gamma
        K.ex.y = -m_invIB * m_rB.x * m_rB.y
        K.ey.x = K.ex.y
        K.ey.y = m_invMassB + m_invIB * m_rB.x * m_rB.x + m_gamma

        K.invertToOut(m_mass)

        m_C.set(cB).addLocal(m_rB).subLocal(target)
        m_C.mulLocal(m_beta)

        // Cheat with some damping
        wB *= 0.98f

        if (data.step!!.warmStarting) {
            m_impulse.mulLocal(data.step!!.dtRatio)
            vB.x += m_invMassB * m_impulse.x
            vB.y += m_invMassB * m_impulse.y
            wB += m_invIB * Vec2.cross(m_rB, m_impulse)
        } else {
            m_impulse.setZero()
        }

        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(1)
        pool.pushMat22(1)
        pool.pushRot(1)
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        return true
    }

    override fun solveVelocityConstraints(data: SolverData) {

        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        // Cdot = v + cross(w, r)
        val Cdot = pool.popVec2()
        Vec2.crossToOutUnsafe(wB, m_rB, Cdot)
        Cdot.addLocal(vB)

        val impulse = pool.popVec2()
        val temp = pool.popVec2()

        temp.set(m_impulse).mulLocal(m_gamma).addLocal(m_C).addLocal(Cdot).negateLocal()
        Mat22.mulToOutUnsafe(m_mass, temp, impulse)

        val oldImpulse = temp
        oldImpulse.set(m_impulse)
        m_impulse.addLocal(impulse)
        val maxImpulse = data.step!!.dt * maxForce
        if (m_impulse.lengthSquared() > maxImpulse * maxImpulse) {
            m_impulse.mulLocal(maxImpulse / m_impulse.length())
        }
        impulse.set(m_impulse).subLocal(oldImpulse)

        vB.x += m_invMassB * impulse.x
        vB.y += m_invMassB * impulse.y
        wB += m_invIB * Vec2.cross(m_rB, impulse)

        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(3)
    }

}
