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
package org.jbox2d.collision

import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2

/**
 * A manifold for two touching convex shapes. Box2D supports multiple types of contact:
 *
 *  * clip point versus plane with radius
 *  * point versus point with radius (circles)
 *
 * The local point usage depends on the manifold type:
 *
 *  * e_circles: the local center of circleA
 *  * e_faceA: the center of faceA
 *  * e_faceB: the center of faceB
 *
 * Similarly the local normal usage:
 *
 *  * e_circles: not used
 *  * e_faceA: the normal on polygonA
 *  * e_faceB: the normal on polygonB
 *
 * We store contacts in this way so that position correction can account for movement, which is
 * critical for continuous physics. All contact scenarios must be expressed in one of these types.
 * This structure is stored across time steps, so we keep it small.
 */
class Manifold {

    /** The points of contact.  */

    val points: Array<ManifoldPoint>

    /** not use for Type::e_points  */

    val localNormal: Vec2

    /** usage depends on manifold type  */

    val localPoint: Vec2


    var type: ManifoldType = ManifoldType.CIRCLES

    /** The number of manifold points.  */

    var pointCount: Int = 0

    enum class ManifoldType {
        CIRCLES, FACE_A, FACE_B
    }

    /**
     * creates a manifold with 0 points, with it's points array full of instantiated ManifoldPoints.
     */
    constructor() {
        points = Array(Settings.maxManifoldPoints) { ManifoldPoint() }
        localNormal = Vec2()
        localPoint = Vec2()
        pointCount = 0
    }

    /**
     * Creates this manifold as a copy of the other
     *
     * @param other
     */
    constructor(other: Manifold) {
        localNormal = other.localNormal.clone()
        localPoint = other.localPoint.clone()
        pointCount = other.pointCount
        type = other.type
        // djm: this is correct now
        points = Array(Settings.maxManifoldPoints) { ManifoldPoint(other.points[it]) }
    }

    /**
     * copies this manifold from the given one
     *
     * @param cp manifold to copy from
     */
    fun set(cp: Manifold) {
        for (i in 0 until cp.pointCount) {
            points[i].set(cp.points[i])
        }

        type = cp.type
        localNormal.set(cp.localNormal)
        localPoint.set(cp.localPoint)
        pointCount = cp.pointCount
    }
}
