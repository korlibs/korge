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

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.SolverData
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.contacts.Position

class ConstantVolumeJoint(
         private val world: World,
        def: ConstantVolumeJointDef
) : Joint(world.pool, def) {

    val bodies: Array<Body>
    private val targetLengths: FloatArray
    private var targetVolume: Float = 0.toFloat()

    private val normals: Array<Vec2>
    private var m_impulse = 0.0f

    var joints: Array<DistanceJoint>; private set

    private val bodyArea: Float
        get() {
            var area = 0.0f
            for (i in bodies.indices) {
                val next = if (i == bodies.size - 1) 0 else i + 1
                area += bodies[i].worldCenter.x * bodies[next].worldCenter.y - bodies[next].worldCenter.x * bodies[i].worldCenter.y
            }
            area *= .5f
            return area
        }

    fun inflate(factor: Float) {
        targetVolume *= factor
    }

    init {
        if (def.bodies.size <= 2) {
            throw IllegalArgumentException(
                    "You cannot create a constant volume joint with less than three bodies.")
        }
        bodies = def.bodies.toTypedArray()

        targetLengths = FloatArray(bodies.size)
        for (i in targetLengths.indices) {
            val next = if (i == targetLengths.size - 1) 0 else i + 1
            val dist = bodies[i].worldCenter.sub(bodies[next].worldCenter).length()
            targetLengths[i] = dist
        }
        targetVolume = bodyArea

        if (def.joints != null && def.joints!!.size != def.bodies.size) {
            throw IllegalArgumentException(
                    "Incorrect joint definition.  Joints have to correspond to the bodies")
        }
        if (def.joints == null) {
            val djd = DistanceJointDef()
            joints = arrayOfNulls<DistanceJoint>(bodies.size) as Array<DistanceJoint>
            for (i in targetLengths.indices) {
                val next = if (i == targetLengths.size - 1) 0 else i + 1
                djd.frequencyHz = def.frequencyHz// 20.0f;
                djd.dampingRatio = def.dampingRatio// 50.0f;
                djd.collideConnected = def.collideConnected
                djd.initialize(bodies[i], bodies[next], bodies[i].worldCenter,
                        bodies[next].worldCenter)
                joints[i] = world.createJoint(djd) as DistanceJoint
            }
        } else {
            joints = def.joints!!.toTypedArray()
        }

        normals = Array(bodies.size) { Vec2() }
    }

    override fun destructor() {
        for (i in joints!!.indices) {
            world.destroyJoint(joints!![i])
        }
    }

    private fun getSolverArea(positions: Array<Position>): Float {
        var area = 0.0f
        for (i in bodies.indices) {
            val next = if (i == bodies.size - 1) 0 else i + 1
            area += positions[bodies[i].islandIndex].c.x * positions[bodies[next].islandIndex].c.y - positions[bodies[next].islandIndex].c.x * positions[bodies[i].islandIndex].c.y
        }
        area *= .5f
        return area
    }

    private fun constrainEdges(positions: Array<Position>): Boolean {
        var perimeter = 0.0f
        for (i in bodies.indices) {
            val next = if (i == bodies.size - 1) 0 else i + 1
            val dx = positions!![bodies[next].islandIndex].c.x - positions[bodies[i].islandIndex].c.x
            val dy = positions[bodies[next].islandIndex].c.y - positions[bodies[i].islandIndex].c.y
            var dist = MathUtils.sqrt(dx * dx + dy * dy)
            if (dist < Settings.EPSILON) {
                dist = 1.0f
            }
            normals[i].x = dy / dist
            normals[i].y = -dx / dist
            perimeter += dist
        }

        val delta = pool.popVec2()

        val deltaArea = targetVolume - getSolverArea(positions)
        val toExtrude = 0.5f * deltaArea / perimeter // *relaxationFactor
        // float sumdeltax = 0.0f;
        var done = true
        for (i in bodies.indices) {
            val next = if (i == bodies.size - 1) 0 else i + 1
            delta.set(toExtrude * (normals[i].x + normals[next].x), toExtrude * (normals[i].y + normals[next].y))
            // sumdeltax += dx;
            val normSqrd = delta.lengthSquared()
            if (normSqrd > Settings.maxLinearCorrection * Settings.maxLinearCorrection) {
                delta.mulLocal(Settings.maxLinearCorrection / MathUtils.sqrt(normSqrd))
            }
            if (normSqrd > Settings.linearSlop * Settings.linearSlop) {
                done = false
            }
            positions!![bodies[next].islandIndex].c.x += delta.x
            positions!![bodies[next].islandIndex].c.y += delta.y
            // bodies[next].m_linearVelocity.x += delta.x * step.inv_dt;
            // bodies[next].m_linearVelocity.y += delta.y * step.inv_dt;
        }

        pool.pushVec2(1)
        // System.out.println(sumdeltax);
        return done
    }

    override fun initVelocityConstraints(step: SolverData) {
        val velocities = step.velocities
        val positions = step.positions
        val d = pool.getVec2Array(bodies.size)

        for (i in bodies.indices) {
            val prev = if (i == 0) bodies.size - 1 else i - 1
            val next = if (i == bodies.size - 1) 0 else i + 1
            d[i].set(positions!![bodies[next].islandIndex].c)
            d[i].subLocal(positions[bodies[prev].islandIndex].c)
        }

        if (step.step!!.warmStarting) {
            m_impulse *= step.step!!.dtRatio
            // float lambda = -2.0f * crossMassSum / dotMassSum;
            // System.out.println(crossMassSum + " " +dotMassSum);
            // lambda = MathUtils.clamp(lambda, -Settings.maxLinearCorrection,
            // Settings.maxLinearCorrection);
            // m_impulse = lambda;
            for (i in bodies.indices) {
                velocities!![bodies[i].islandIndex].v.x += bodies[i].m_invMass * d[i].y * .5f * m_impulse
                velocities!![bodies[i].islandIndex].v.y += bodies[i].m_invMass * -d[i].x * .5f * m_impulse
            }
        } else {
            m_impulse = 0.0f
        }
    }

    override fun solvePositionConstraints(step: SolverData): Boolean {
        return constrainEdges(step.positions!!)
    }

    override fun solveVelocityConstraints(step: SolverData) {
        var crossMassSum = 0.0f
        var dotMassSum = 0.0f

        val velocities = step.velocities
        val positions = step.positions
        val d = pool.getVec2Array(bodies.size)

        for (i in bodies.indices) {
            val prev = if (i == 0) bodies.size - 1 else i - 1
            val next = if (i == bodies.size - 1) 0 else i + 1
            d[i].set(positions!![bodies[next].islandIndex].c)
            d[i].subLocal(positions[bodies[prev].islandIndex].c)
            dotMassSum += d[i].lengthSquared() / bodies[i].m_mass
            crossMassSum += Vec2.cross(velocities!![bodies[i].islandIndex].v, d[i])
        }
        val lambda = -2.0f * crossMassSum / dotMassSum
        // System.out.println(crossMassSum + " " +dotMassSum);
        // lambda = MathUtils.clamp(lambda, -Settings.maxLinearCorrection,
        // Settings.maxLinearCorrection);
        m_impulse += lambda
        // System.out.println(m_impulse);
        for (i in bodies.indices) {
            velocities!![bodies[i].islandIndex].v.x += bodies[i].m_invMass * d[i].y * .5f * lambda
            velocities!![bodies[i].islandIndex].v.y += bodies[i].m_invMass * -d[i].x * .5f * lambda
        }
    }

    /** No-op  */
    override fun getAnchorA(argOut: Vec2) {}

    /** No-op  */
    override fun getAnchorB(argOut: Vec2) {}

    /** No-op  */
    override fun getReactionForce(inv_dt: Float, argOut: Vec2) {}

    /** No-op  */
    override fun getReactionTorque(inv_dt: Float): Float {
        return 0f
    }
}
