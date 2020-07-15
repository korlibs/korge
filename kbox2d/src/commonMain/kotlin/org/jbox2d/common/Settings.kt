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
package org.jbox2d.common

import org.jbox2d.internal.*
import kotlin.native.concurrent.ThreadLocal

/**
 * Global tuning constants based on MKS units and various integer maximums (vertices per shape,
 * pairs, etc.).
 */
object Settings {

    /** A "close to zero" float epsilon value for use  */

    val EPSILON = 1.1920928955078125E-7f

    /** Pi.  */

    val PI = kotlin.math.PI.toFloat()

    // JBox2D specific settings

    @ThreadLocal
    var FAST_ABS = true

    @ThreadLocal
    var FAST_FLOOR = true

    @ThreadLocal
    var FAST_CEIL = true

    @ThreadLocal
    //var FAST_ROUND = true
    var FAST_ROUND = false

    @ThreadLocal
    var FAST_ATAN2 = true

    @ThreadLocal
    var FAST_POW = true

    @ThreadLocal
    var CONTACT_STACK_INIT_SIZE = 10

    @ThreadLocal
    var SINCOS_LUT_ENABLED = true
    /**
     * smaller the precision, the larger the table. If a small table is used (eg, precision is .006 or
     * greater), make sure you set the table to lerp it's results. Accuracy chart is in the MathUtils
     * source. Or, run the tests yourself in [SinCosTest].  Good lerp precision
     * values:
     *
     *  * .0092
     *  * .008201
     *  * .005904
     *  * .005204
     *  * .004305
     *  * .002807
     *  * .001508
     *  * 9.32500E-4
     *  * 7.48000E-4
     *  * 8.47000E-4
     *  * .0005095
     *  * .0001098
     *  * 9.50499E-5
     *  * 6.08500E-5
     *  * 3.07000E-5
     *  * 1.53999E-5
     *
     */

    val SINCOS_LUT_PRECISION = .00011f

    val SINCOS_LUT_LENGTH = kotlin.math.ceil(kotlin.math.PI * 2 / SINCOS_LUT_PRECISION).toInt()
    /**
     * Use if the table's precision is large (eg .006 or greater). Although it is more expensive, it
     * greatly increases accuracy. Look in the MathUtils source for some test results on the accuracy
     * and speed of lerp vs non lerp. Or, run the tests yourself in [SinCosTest].
     */

    @ThreadLocal
    var SINCOS_LUT_LERP = false


    // Collision

    /**
     * The maximum number of contact points between two convex shapes.
     */

    @ThreadLocal
    var maxManifoldPoints = 2

    /**
     * The maximum number of vertices on a convex polygon.
     */

    @ThreadLocal
    var maxPolygonVertices = 8

    /**
     * This is used to fatten AABBs in the dynamic tree. This allows proxies to move by a small amount
     * without triggering a tree adjustment. This is in meters.
     */

    @ThreadLocal
    var aabbExtension = 0.1f

    /**
     * This is used to fatten AABBs in the dynamic tree. This is used to predict the future position
     * based on the current displacement. This is a dimensionless multiplier.
     */

    @ThreadLocal
    var aabbMultiplier = 2.0f

    /**
     * A small length used as a collision and constraint tolerance. Usually it is chosen to be
     * numerically significant, but visually insignificant.
     */

    @ThreadLocal
    var linearSlop = 0.005f

    /**
     * A small angle used as a collision and constraint tolerance. Usually it is chosen to be
     * numerically significant, but visually insignificant.
     */

    @ThreadLocal
    var angularSlop = 2.0f / 180.0f * PI

    /**
     * The radius of the polygon/edge shape skin. This should not be modified. Making this smaller
     * means polygons will have and insufficient for continuous collision. Making it larger may create
     * artifacts for vertex collision.
     */

    @ThreadLocal
    var polygonRadius = 2.0f * linearSlop

    /** Maximum number of sub-steps per contact in continuous physics simulation.  */

    @ThreadLocal
    var maxSubSteps = 8

    // Dynamics

    /**
     * Maximum number of contacts to be handled to solve a TOI island.
     */

    @ThreadLocal
    var maxTOIContacts = 32

    /**
     * A velocity threshold for elastic collisions. Any collision with a relative linear velocity
     * below this threshold will be treated as inelastic.
     */

    @ThreadLocal
    var velocityThreshold = 1.0f

    /**
     * The maximum linear position correction used when solving constraints. This helps to prevent
     * overshoot.
     */

    @ThreadLocal
    var maxLinearCorrection = 0.2f

    /**
     * The maximum angular position correction used when solving constraints. This helps to prevent
     * overshoot.
     */

    @ThreadLocal
    var maxAngularCorrection = 8.0f / 180.0f * PI

    /**
     * The maximum linear velocity of a body. This limit is very large and is used to prevent
     * numerical problems. You shouldn't need to adjust this.
     */

    @ThreadLocal
    var maxTranslation = 2.0f

    @ThreadLocal
    var maxTranslationSquared = maxTranslation * maxTranslation

    /**
     * The maximum angular velocity of a body. This limit is very large and is used to prevent
     * numerical problems. You shouldn't need to adjust this.
     */

    @ThreadLocal
    var maxRotation = 0.5f * PI

    @ThreadLocal
    var maxRotationSquared = maxRotation * maxRotation

    /**
     * This scale factor controls how fast overlap is resolved. Ideally this would be 1 so that
     * overlap is removed in one time step. However using values close to 1 often lead to overshoot.
     */

    @ThreadLocal
    var baumgarte = 0.2f

    @ThreadLocal
    var toiBaugarte = 0.75f


    // Sleep

    /**
     * The time that a body must be still before it will go to sleep.
     */

    @ThreadLocal
    var timeToSleep = 0.5f

    /**
     * A body cannot sleep if its linear velocity is above this tolerance.
     */

    @ThreadLocal
    var linearSleepTolerance = 0.01f

    /**
     * A body cannot sleep if its angular velocity is above this tolerance.
     */

    @ThreadLocal
    var angularSleepTolerance = 2.0f / 180.0f * PI

    // Particle

    /**
     * A symbolic constant that stands for particle allocation error.
     */

    val invalidParticleIndex = -1

    /**
     * The standard distance between particles, divided by the particle radius.
     */

    val particleStride = 0.75f

    /**
     * The minimum particle weight that produces pressure.
     */

    val minParticleWeight = 1.0f

    /**
     * The upper limit for particle weight used in pressure calculation.
     */

    val maxParticleWeight = 5.0f

    /**
     * The maximum distance between particles in a triad, divided by the particle radius.
     */

    val maxTriadDistance = 2

    val maxTriadDistanceSquared = maxTriadDistance * maxTriadDistance

    /**
     * The initial size of particle data buffers.
     */

    val minParticleBufferCapacity = 256


    /**
     * Friction mixing law. Feel free to customize this. TODO djm: add customization
     *
     * @param friction1
     * @param friction2
     * @return
     */

    fun mixFriction(friction1: Float, friction2: Float): Float {
        return MathUtils.sqrt(friction1 * friction2)
    }

    /**
     * Restitution mixing law. Feel free to customize this. TODO djm: add customization
     *
     * @param restitution1
     * @param restitution2
     * @return
     */

    fun mixRestitution(restitution1: Float, restitution2: Float): Float {
        return if (restitution1 > restitution2) restitution1 else restitution2
    }
}
