package org.jbox2d.dynamics.joints

import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body

/**
 * Motor joint definition.
 *
 * @author dmurph
 */
class MotorJointDef : JointDef(JointType.MOTOR) {
    /**
     * Position of bodyB minus the position of bodyA, in bodyA's frame, in meters.
     */

    val linearOffset = Vec2()

    /**
     * The bodyB angle minus bodyA angle in radians.
     */

    var angularOffset: Float = 0f

    /**
     * The maximum motor force in N.
     */

    var maxForce: Float = 1f

    /**
     * The maximum motor torque in N-m.
     */

    var maxTorque: Float = 1f

    /**
     * Position correction factor in the range [0,1].
     */

    var correctionFactor: Float = .3f

    fun initialize(bA: Body, bB: Body) {
        bodyA = bA
        bodyB = bB
        val xB = bodyB!!.position
        bodyA!!.getLocalPointToOut(xB, linearOffset)

        val angleA = bodyA!!.angleRadians
        val angleB = bodyB!!.angleRadians
        angularOffset = angleB - angleA
    }
}
