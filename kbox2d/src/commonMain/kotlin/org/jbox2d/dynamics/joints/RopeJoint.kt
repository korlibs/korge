package org.jbox2d.dynamics.joints

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

/**
 * A rope joint enforces a maximum distance between two points on two bodies. It has no other
 * effect. Warning: if you attempt to change the maximum length during the simulation you will get
 * some non-physical behavior. A model that would allow you to dynamically modify the length would
 * have some sponginess, so I chose not to implement it that way. See DistanceJoint if you want to
 * dynamically control length.
 *
 * @author Daniel Murphy
 */
class RopeJoint(worldPool: IWorldPool, def: RopeJointDef) : Joint(worldPool, def) {
    // Solver shared

    val localAnchorA = Vec2()

    val localAnchorB = Vec2()

    var maxLength: Float = 0.toFloat()
    private var m_length: Float = 0.toFloat()
    private var m_impulse: Float = 0.toFloat()

    // Solver temp
    private var m_indexA: Int = 0
    private var m_indexB: Int = 0
    private val m_u = Vec2()
    private val m_rA = Vec2()
    private val m_rB = Vec2()
    private val m_localCenterA = Vec2()
    private val m_localCenterB = Vec2()
    private var m_invMassA: Float = 0.toFloat()
    private var m_invMassB: Float = 0.toFloat()
    private var m_invIA: Float = 0.toFloat()
    private var m_invIB: Float = 0.toFloat()
    private var m_mass: Float = 0.toFloat()
    var limitState: LimitState? = null
        private set

    init {
        localAnchorA.set(def.localAnchorA)
        localAnchorB.set(def.localAnchorB)

        maxLength = def.maxLength

        m_mass = 0.0f
        m_impulse = 0.0f
        limitState = LimitState.INACTIVE
        m_length = 0.0f
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
        val temp = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), m_rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), m_rB)

        m_u.set(cB).addLocal(m_rB).subLocal(cA).subLocal(m_rA)

        m_length = m_u.length()

        val C = m_length - maxLength
        if (C > 0.0f) {
            limitState = LimitState.AT_UPPER
        } else {
            limitState = LimitState.INACTIVE
        }

        if (m_length > Settings.linearSlop) {
            m_u.mulLocal(1.0f / m_length)
        } else {
            m_u.setZero()
            m_mass = 0.0f
            m_impulse = 0.0f
            pool.pushRot(2)
            pool.pushVec2(1)
            return
        }

        // Compute effective mass.
        val crA = Vec2.cross(m_rA, m_u)
        val crB = Vec2.cross(m_rB, m_u)
        val invMass = m_invMassA + m_invIA * crA * crA + m_invMassB + m_invIB * crB * crB

        m_mass = if (invMass != 0.0f) 1.0f / invMass else 0.0f

        if (data.step!!.warmStarting) {
            // Scale the impulse to support a variable time step.
            m_impulse *= data.step!!.dtRatio

            val Px = m_impulse * m_u.x
            val Py = m_impulse * m_u.y
            vA.x -= m_invMassA * Px
            vA.y -= m_invMassA * Py
            wA -= m_invIA * (m_rA.x * Py - m_rA.y * Px)

            vB.x += m_invMassB * Px
            vB.y += m_invMassB * Py
            wB += m_invIB * (m_rB.x * Py - m_rB.y * Px)
        } else {
            m_impulse = 0.0f
        }

        pool.pushRot(2)
        pool.pushVec2(1)

        // data.velocities[m_indexA].v = vA;
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v = vB;
        data.velocities!![m_indexB].w = wB
    }

    override fun solveVelocityConstraints(data: SolverData) {
        val vA = data.velocities!![m_indexA].v
        var wA = data.velocities!![m_indexA].w
        val vB = data.velocities!![m_indexB].v
        var wB = data.velocities!![m_indexB].w

        // Cdot = dot(u, v + cross(w, r))
        val vpA = pool.popVec2()
        val vpB = pool.popVec2()
        val temp = pool.popVec2()

        Vec2.crossToOutUnsafe(wA, m_rA, vpA)
        vpA.addLocal(vA)
        Vec2.crossToOutUnsafe(wB, m_rB, vpB)
        vpB.addLocal(vB)

        val C = m_length - maxLength
        var Cdot = Vec2.dot(m_u, temp.set(vpB).subLocal(vpA))

        // Predictive constraint.
        if (C < 0.0f) {
            Cdot += data.step!!.inv_dt * C
        }

        var impulse = -m_mass * Cdot
        val oldImpulse = m_impulse
        m_impulse = MathUtils.min(0.0f, m_impulse + impulse)
        impulse = m_impulse - oldImpulse

        val Px = impulse * m_u.x
        val Py = impulse * m_u.y
        vA.x -= m_invMassA * Px
        vA.y -= m_invMassA * Py
        wA -= m_invIA * (m_rA.x * Py - m_rA.y * Px)
        vB.x += m_invMassB * Px
        vB.y += m_invMassB * Py
        wB += m_invIB * (m_rB.x * Py - m_rB.y * Px)

        pool.pushVec2(3)

        // data.velocities[m_indexA].v = vA;
        data.velocities!![m_indexA].w = wA
        // data.velocities[m_indexB].v = vB;
        data.velocities!![m_indexB].w = wB
    }

    override fun solvePositionConstraints(data: SolverData): Boolean {
        val cA = data.positions!![m_indexA].c
        var aA = data.positions!![m_indexA].a
        val cB = data.positions!![m_indexB].c
        var aB = data.positions!![m_indexB].a

        val qA = pool.popRot()
        val qB = pool.popRot()
        val u = pool.popVec2()
        val rA = pool.popVec2()
        val rB = pool.popVec2()
        val temp = pool.popVec2()

        qA.setRadians(aA)
        qB.setRadians(aB)

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(m_localCenterA), rA)
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(m_localCenterB), rB)
        u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)

        val length = u.normalize()
        var C = length - maxLength

        C = MathUtils.clamp(C, 0.0f, Settings.maxLinearCorrection)

        val impulse = -m_mass * C
        val Px = impulse * u.x
        val Py = impulse * u.y

        cA.x -= m_invMassA * Px
        cA.y -= m_invMassA * Py
        aA -= m_invIA * (rA.x * Py - rA.y * Px)
        cB.x += m_invMassB * Px
        cB.y += m_invMassB * Py
        aB += m_invIB * (rB.x * Py - rB.y * Px)

        pool.pushRot(2)
        pool.pushVec2(4)

        // data.positions[m_indexA].c = cA;
        data.positions!![m_indexA].a = aA
        // data.positions[m_indexB].c = cB;
        data.positions!![m_indexB].a = aB

        return length - maxLength < Settings.linearSlop
    }

    override fun getAnchorA(argOut: Vec2) {
        bodyA!!.getWorldPointToOut(localAnchorA, argOut)
    }

    override fun getAnchorB(argOut: Vec2) {
        bodyB!!.getWorldPointToOut(localAnchorB, argOut)
    }

    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {
        argOut.set(m_u).mulLocal(inv_dt).mulLocal(m_impulse)
    }

    override fun getReactionTorque(inv_dt: Float): Float {
        return 0f
    }

}
