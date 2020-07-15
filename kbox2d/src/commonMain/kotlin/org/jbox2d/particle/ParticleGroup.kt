package org.jbox2d.particle

import com.soywiz.korma.geom.*
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2

class ParticleGroup {

    internal var m_system: ParticleSystem? = null

    var m_firstIndex: Int = 0

    internal var m_lastIndex: Int = 0

    var m_groupFlags: Int = 0

    internal var m_strength: Float = 0.toFloat()

    internal var m_prev: ParticleGroup? = null

    var m_next: ParticleGroup? = null

    fun getNext() = m_next


    internal var m_timestamp: Int = 0

    internal var m_mass: Float = 0.toFloat()

    internal var m_inertia: Float = 0.toFloat()

    internal val m_center = Vec2()

    internal val m_linearVelocity = Vec2()

    internal var m_angularVelocity: Float = 0.toFloat()

    val m_transform = Transform()


    internal var m_destroyAutomatically: Boolean = false

    internal var m_toBeDestroyed: Boolean = false

    internal var m_toBeSplit: Boolean = false


    var m_userData: Any? = null

    val particleCount: Int
        get() = m_lastIndex - m_firstIndex

    val mass: Float
        get() {
            updateStatistics()
            return m_mass
        }

    val inertia: Float
        get() {
            updateStatistics()
            return m_inertia
        }

    val center: Vec2
        get() {
            updateStatistics()
            return m_center
        }

    val linearVelocity: Vec2
        get() {
            updateStatistics()
            return m_linearVelocity
        }

    val angularVelocity: Float
        get() {
            updateStatistics()
            return m_angularVelocity
        }

    val position: Vec2
        get() = m_transform.p

    val angleRadians: Float get() = m_transform.q.angleRadians
    val angleDegrees: Float get() = m_transform.q.angleDegrees
    val angle: Angle get() = m_transform.q.angle

    init {
        // m_system = null;
        m_firstIndex = 0
        m_lastIndex = 0
        m_groupFlags = 0
        m_strength = 1.0f

        m_timestamp = -1
        m_mass = 0f
        m_inertia = 0f
        m_angularVelocity = 0f
        m_transform.setIdentity()

        m_destroyAutomatically = true
        m_toBeDestroyed = false
        m_toBeSplit = false
    }


    fun updateStatistics() {
        if (m_timestamp != m_system!!.m_timestamp) {
            val m = m_system!!.particleMass
            m_mass = 0f
            m_center.setZero()
            m_linearVelocity.setZero()
            for (i in m_firstIndex until m_lastIndex) {
                m_mass += m
                val pos = m_system!!.m_positionBuffer!!.data!![i]
                m_center.x += m * pos.x
                m_center.y += m * pos.y
                val vel = m_system!!.m_velocityBuffer.data!![i]
                m_linearVelocity.x += m * vel.x
                m_linearVelocity.y += m * vel.y
            }
            if (m_mass > 0) {
                m_center.x *= 1 / m_mass
                m_center.y *= 1 / m_mass
                m_linearVelocity.x *= 1 / m_mass
                m_linearVelocity.y *= 1 / m_mass
            }
            m_inertia = 0f
            m_angularVelocity = 0f
            for (i in m_firstIndex until m_lastIndex) {
                val pos = m_system!!.m_positionBuffer.data!![i]
                val vel = m_system!!.m_velocityBuffer.data!![i]
                val px = pos.x - m_center.x
                val py = pos.y - m_center.y
                val vx = vel.x - m_linearVelocity.x
                val vy = vel.y - m_linearVelocity.y
                m_inertia += m * (px * px + py * py)
                m_angularVelocity += m * (px * vy - py * vx)
            }
            if (m_inertia > 0) {
                m_angularVelocity *= 1 / m_inertia
            }
            m_timestamp = m_system!!.m_timestamp
        }
    }
}
