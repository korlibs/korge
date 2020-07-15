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
 * Created at 12:12:02 PM Jan 23, 2011
 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.internal.*
import org.jbox2d.pooling.IWorldPool

/**
 * The pulley joint is connected to two bodies and two fixed ground points. The pulley supports a
 * ratio such that: length1 + ratio * length2 <= constant Yes, the force transmitted is scaled by
 * the ratio. Warning: the pulley joint can get a bit squirrelly by itself. They often work better
 * when combined with prismatic joints. You should also cover the the anchor points with static
 * shapes to prevent one side from going to zero length.
 *
 * @author Daniel Murphy
 */
class PulleyJoint constructor(argWorldPool: IWorldPool, def: PulleyJointDef) : Joint(argWorldPool, def) {

    val _groundAnchorA = Vec2()

    val _groundAnchorB = Vec2()

    fun getGroundAnchorA() = _groundAnchorA
    fun getGroundAnchorB() = _groundAnchorB


    val lengthA: Float

    val lengthB: Float

    // Solver shared

    val localAnchorA = Vec2()

    val localAnchorB = Vec2()
    private val m_constant: Float
    val ratio: Float
    private var m_impulse: Float = 0.toFloat()

    // Solver temp
    private var m_indexA: Int = 0
    private var m_indexB: Int = 0
    private val m_uA = Vec2()
    private val m_uB = Vec2()
    private val m_rA = Vec2()
    private val m_rB = Vec2()
    private val m_localCenterA = Vec2()
    private val m_localCenterB = Vec2()
    private var m_invMassA: Float = 0.toFloat()
    private var m_invMassB: Float = 0.toFloat()
    private var m_invIA: Float = 0.toFloat()
    private var m_invIB: Float = 0.toFloat()
    private var m_mass: Float = 0.toFloat()

    val currentLengthA: Float
        get() {
            val p = pool.popVec2()
            m_bodyA!!.getWorldPointToOut(localAnchorA, p)
            p.subLocal(_groundAnchorA)
            val length = p.length()
            pool.pushVec2(1)
            return length
        }

    val currentLengthB: Float
        get() {
            val p = pool.popVec2()
            m_bodyB!!.getWorldPointToOut(localAnchorB, p)
            p.subLocal(_groundAnchorB)
            val length = p.length()
            pool.pushVec2(1)
            return length
        }

    val length1: Float
        get() {
            val p = pool.popVec2()
            m_bodyA!!.getWorldPointToOut(localAnchorA, p)
            p.subLocal(_groundAnchorA)

            val len = p.length()
            pool.pushVec2(1)
            return len
        }

    val length2: Float
        get() {
            val p = pool.popVec2()
            m_bodyB!!.getWorldPointToOut(localAnchorB, p)
            p.subLocal(_groundAnchorB)

            val len = p.length()
            pool.pushVec2(1)
            return len
        }

    init {
        _groundAnchorA.set(def.groundAnchorA)
        _groundAnchorB.set(def.groundAnchorB)
        localAnchorA.set(def.localAnchorA)
        localAnchorB.set(def.localAnchorB)

        assert(def.ratio != 0.0f)
        ratio = def.ratio

        lengthA = def.lengthA
        lengthB = def.lengthB

        m_constant = def.lengthA + ratio * def.lengthB
        m_impulse = 0.0f
    }


    override fun getAnchorA(argOut: Vec2) {
        m_bodyA!!.getWorldPointToOut(localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        m_bodyB!!.getWorldPointToOut(localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        argOut.set(m_uB).mulLocal(m_impulse).mulLocal(inv_dt)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        return 0f
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
        val temp = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), m_rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), m_rB)

        m_uA.set(cA).addLocal(m_rA).subLocal(_groundAnchorA)
        m_uB.set(cB).addLocal(m_rB).subLocal(_groundAnchorB)

        val lengthA = m_uA.length()
        val lengthB = m_uB.length()

        if (lengthA > 10f * Settings.linearSlop) {
            m_uA.mulLocal(1.0f / lengthA)
        } else {
            m_uA.setZero()
        }

        if (lengthB > 10f * Settings.linearSlop) {
            m_uB.mulLocal(1.0f / lengthB)
        } else {
            m_uB.setZero()
        }

        // Compute effective mass.
        val ruA = Vec2.cross(m_rA, m_uA)
        val ruB = Vec2.cross(m_rB, m_uB)

        val mA = m_invMassA + m_invIA * ruA * ruA
        val mB = m_invMassB + m_invIB * ruB * ruB

        m_mass = mA + ratio * ratio * mB

        if (m_mass > 0.0f) {
            m_mass = 1.0f / m_mass
        }

        if (data.step!!.warmStarting) {

            // Scale impulses to support variable time steps.
            m_impulse *= data.step!!.dtRatio

            // Warm starting.
            val PA = pool.popVec2()
            val PB = pool.popVec2()

            PA.set(m_uA).mulLocal(-m_impulse)
            PB.set(m_uB).mulLocal(-ratio * m_impulse)

            vA.x += m_invMassA * PA.x
            vA.y += m_invMassA * PA.y
            wA += m_invIA * Vec2.cross(m_rA, PA)
            vB.x += m_invMassB * PB.x
            vB.y += m_invMassB * PB.y
            wB += m_invIB * Vec2.cross(m_rB, PB)

            pool.pushVec2(2)
        } else {
            m_impulse = 0.0f
        }
        //    data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(1)
        pool.pushRot(2)
    }

    override fun solveVelocityConstraints(data: SolverData) {
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        val vpA = pool.popVec2()
        val vpB = pool.popVec2()
        val PA = pool.popVec2()
        val PB = pool.popVec2()

        Vec2.crossToOutUnsafe(wA, m_rA, vpA)
        vpA.addLocal(vA)
        Vec2.crossToOutUnsafe(wB, m_rB, vpB)
        vpB.addLocal(vB)

        val Cdot = -Vec2.dot(m_uA, vpA) - ratio * Vec2.dot(m_uB, vpB)
        val impulse = -m_mass * Cdot
        m_impulse += impulse

        PA.set(m_uA).mulLocal(-impulse)
        PB.set(m_uB).mulLocal(-ratio * impulse)
        vA.x += m_invMassA * PA.x
        vA.y += m_invMassA * PA.y
        wA += m_invIA * Vec2.cross(m_rA, PA)
        vB.x += m_invMassB * PB.x
        vB.y += m_invMassB * PB.y
        wB += m_invIB * Vec2.cross(m_rB, PB)

        //    data.velocities[m_indexA].v.set(vA);
        data.velocities!![m_indexA].w = wA
        //    data.velocities[m_indexB].v.set(vB);
        data.velocities!![m_indexB].w = wB

        pool.pushVec2(4)
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        val qA = pool.popRot()
        val qB = pool.popRot()
        val rA = pool.popVec2()
        val rB = pool.popVec2()
        val uA = pool.popVec2()
        val uB = pool.popVec2()
        val temp = pool.popVec2()
        val PA = pool.popVec2()
        val PB = pool.popVec2()

        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a

        qA.setRadians(aA)
        qB.setRadians(aB)

        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), rB)

        uA.set(cA).addLocal(rA).subLocal(_groundAnchorA)
        uB.set(cB).addLocal(rB).subLocal(_groundAnchorB)

        val lengthA = uA.length()
        val lengthB = uB.length()

        if (lengthA > 10.0f * Settings.linearSlop) {
            uA.mulLocal(1.0f / lengthA)
        } else {
            uA.setZero()
        }

        if (lengthB > 10.0f * Settings.linearSlop) {
            uB.mulLocal(1.0f / lengthB)
        } else {
            uB.setZero()
        }

        // Compute effective mass.
        val ruA = Vec2.cross(rA, uA)
        val ruB = Vec2.cross(rB, uB)

        val mA = m_invMassA + m_invIA * ruA * ruA
        val mB = m_invMassB + m_invIB * ruB * ruB

        var mass = mA + ratio * ratio * mB

        if (mass > 0.0f) {
            mass = 1.0f / mass
        }

        val C = m_constant - lengthA - ratio * lengthB
        val linearError = MathUtils.abs(C)

        val impulse = -mass * C

        PA.set(uA).mulLocal(-impulse)
        PB.set(uB).mulLocal(-ratio * impulse)

        cA.x += m_invMassA * PA.x
        cA.y += m_invMassA * PA.y
        aA += m_invIA * Vec2.cross(rA, PA)
        cB.x += m_invMassB * PB.x
        cB.y += m_invMassB * PB.y
        aB += m_invIB * Vec2.cross(rB, PB)

        //    data.positions[m_indexA].c.set(cA);
        data.positions!![m_indexA].a = aA
        //    data.positions[m_indexB].c.set(cB);
        data.positions!![m_indexB].a = aB

        pool.pushRot(2)
        pool.pushVec2(7)

        return linearError < Settings.linearSlop
    }

    companion object {

        val MIN_PULLEY_LENGTH = 2.0f
    }
}
