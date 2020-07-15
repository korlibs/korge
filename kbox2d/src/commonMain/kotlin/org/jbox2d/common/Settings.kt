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

import kotlin.native.concurrent.*

@ThreadLocal private var _FAST_ABS = true
@ThreadLocal private var _FAST_FLOOR = true
@ThreadLocal private var _FAST_CEIL = true
@ThreadLocal private var _FAST_ROUND = false
@ThreadLocal private var _FAST_ATAN2 = true
@ThreadLocal private var _FAST_POW = true
@ThreadLocal private var _CONTACT_STACK_INIT_SIZE = 10
@ThreadLocal private var _SINCOS_LUT_ENABLED = true
@ThreadLocal private var _SINCOS_LUT_LERP = false
@ThreadLocal private var _maxManifoldPoints = 2
@ThreadLocal private var _maxPolygonVertices = 8
@ThreadLocal private var _aabbExtension = 0.1f
@ThreadLocal private var _aabbMultiplier = 2.0f
@ThreadLocal private var _linearSlop = 0.005f
@ThreadLocal private var _angularSlop = 2.0f / 180.0f * Settings.PI
@ThreadLocal private var _polygonRadius = 2.0f * _linearSlop
@ThreadLocal private var _maxSubSteps = 8
@ThreadLocal private var _maxTOIContacts = 32
@ThreadLocal private var _velocityThreshold = 1.0f
@ThreadLocal private var _maxLinearCorrection = 0.2f
@ThreadLocal private var _maxAngularCorrection = 8.0f / 180.0f * Settings.PI
@ThreadLocal private var _maxTranslation = 2.0f
@ThreadLocal private var _maxTranslationSquared = _maxTranslation * _maxTranslation
@ThreadLocal private var _maxRotation = 0.5f * Settings.PI
@ThreadLocal private var _maxRotationSquared = _maxRotation * _maxRotation
@ThreadLocal private var _baumgarte = 0.2f
@ThreadLocal private var _toiBaugarte = 0.75f
@ThreadLocal private var _timeToSleep = 0.5f
@ThreadLocal private var _linearSleepTolerance = 0.01f
@ThreadLocal private var _angularSleepTolerance = 2.0f / 180.0f * Settings.PI

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

    var FAST_ABS: Boolean
		get() = _FAST_ABS
		set(value) = run { _FAST_ABS = value }

    var FAST_FLOOR: Boolean
		get() = _FAST_FLOOR
		set(value) = run { _FAST_FLOOR = value }

    var FAST_CEIL
		get() = _FAST_CEIL
		set(value) = run { _FAST_CEIL = value }

    var FAST_ROUND
		get() = _FAST_ROUND
		set(value) = run { _FAST_ROUND = value }

    var FAST_ATAN2
		get() = _FAST_ATAN2
		set(value) = run { _FAST_ATAN2 = value }

    var FAST_POW
		get() = _FAST_POW
		set(value) = run { _FAST_POW = value }

    var CONTACT_STACK_INIT_SIZE
		get() = _CONTACT_STACK_INIT_SIZE
		set(value) = run { _CONTACT_STACK_INIT_SIZE = value }

    var SINCOS_LUT_ENABLED
		get() = _SINCOS_LUT_ENABLED
		set(value) = run { _SINCOS_LUT_ENABLED = value }
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

    var SINCOS_LUT_LERP
		get() = _SINCOS_LUT_LERP
		set(value) = run { _SINCOS_LUT_LERP = value }


    // Collision

    /**
     * The maximum number of contact points between two convex shapes.
     */

    var maxManifoldPoints
		get() = _maxManifoldPoints
		set(value) = run { _maxManifoldPoints = value }

    /**
     * The maximum number of vertices on a convex polygon.
     */

    var maxPolygonVertices
		get() = _maxPolygonVertices
		set(value) = run { _maxPolygonVertices = value }

    /**
     * This is used to fatten AABBs in the dynamic tree. This allows proxies to move by a small amount
     * without triggering a tree adjustment. This is in meters.
     */

    var aabbExtension
		get() = _aabbExtension
		set(value) = run { _aabbExtension = value }

    /**
     * This is used to fatten AABBs in the dynamic tree. This is used to predict the future position
     * based on the current displacement. This is a dimensionless multiplier.
     */

    var aabbMultiplier
		get() = _aabbMultiplier
		set(value) = run { _aabbMultiplier = value }

    /**
     * A small length used as a collision and constraint tolerance. Usually it is chosen to be
     * numerically significant, but visually insignificant.
     */

    var linearSlop
		get() = _linearSlop
		set(value) = run { _linearSlop = value }

    /**
     * A small angle used as a collision and constraint tolerance. Usually it is chosen to be
     * numerically significant, but visually insignificant.
     */

    var angularSlop
		get() = _angularSlop
		set(value) = run { _angularSlop = value }

    /**
     * The radius of the polygon/edge shape skin. This should not be modified. Making this smaller
     * means polygons will have and insufficient for continuous collision. Making it larger may create
     * artifacts for vertex collision.
     */

    var polygonRadius
		get() = _polygonRadius
		set(value) = run { _polygonRadius = value }

    /** Maximum number of sub-steps per contact in continuous physics simulation.  */

    var maxSubSteps
		get() = _maxSubSteps
		set(value) = run { _maxSubSteps = value }

    // Dynamics

    /**
     * Maximum number of contacts to be handled to solve a TOI island.
     */

    var maxTOIContacts
		get() = _maxTOIContacts
		set(value) = run { _maxTOIContacts = value }

    /**
     * A velocity threshold for elastic collisions. Any collision with a relative linear velocity
     * below this threshold will be treated as inelastic.
     */

    var velocityThreshold
		get() = _velocityThreshold
		set(value) = run { _velocityThreshold = value }

    /**
     * The maximum linear position correction used when solving constraints. This helps to prevent
     * overshoot.
     */

    var maxLinearCorrection
		get() = _maxLinearCorrection
		set(value) = run { _maxLinearCorrection = value }

    /**
     * The maximum angular position correction used when solving constraints. This helps to prevent
     * overshoot.
     */

    var maxAngularCorrection
		get() = _maxAngularCorrection
		set(value) = run { _maxAngularCorrection = value }

    /**
     * The maximum linear velocity of a body. This limit is very large and is used to prevent
     * numerical problems. You shouldn't need to adjust this.
     */

    var maxTranslation
		get() = _maxTranslation
		set(value) = run { _maxTranslation = value }

    var maxTranslationSquared
		get() = _maxTranslationSquared
		set(value) = run { _maxTranslationSquared = value }

    /**
     * The maximum angular velocity of a body. This limit is very large and is used to prevent
     * numerical problems. You shouldn't need to adjust this.
     */

    var maxRotation
		get() = _maxRotation
		set(value) = run { _maxRotation = value }

    var maxRotationSquared
		get() = _maxRotationSquared
		set(value) = run { _maxRotationSquared = value }

    /**
     * This scale factor controls how fast overlap is resolved. Ideally this would be 1 so that
     * overlap is removed in one time step. However using values close to 1 often lead to overshoot.
     */

    var baumgarte
		get() = _baumgarte
		set(value) = run { _baumgarte = value }

    var toiBaugarte
		get() = _toiBaugarte
		set(value) = run { _toiBaugarte = value }


    // Sleep

    /**
     * The time that a body must be still before it will go to sleep.
     */

    var timeToSleep
		get() = _timeToSleep
		set(value) = run { _timeToSleep = value }

    /**
     * A body cannot sleep if its linear velocity is above this tolerance.
     */

    var linearSleepTolerance
		get() = _linearSleepTolerance
		set(value) = run { _linearSleepTolerance = value }

    /**
     * A body cannot sleep if its angular velocity is above this tolerance.
     */

    var angularSleepTolerance
		get() = _angularSleepTolerance
		set(value) = run { _angularSleepTolerance = value }

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
