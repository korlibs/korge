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
package org.jbox2d.dynamics.contacts

import org.jbox2d.collision.Manifold.ManifoldType
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2

class ContactPositionConstraint {

    internal var localPoints = Array<Vec2>(Settings.maxManifoldPoints) { Vec2() }

    internal val localNormal = Vec2()

    internal val localPoint = Vec2()

    internal var indexA: Int = 0

    internal var indexB: Int = 0

    internal var invMassA: Float = 0.toFloat()

    internal var invMassB: Float = 0.toFloat()

    internal val localCenterA = Vec2()

    internal val localCenterB = Vec2()

    internal var invIA: Float = 0.toFloat()

    internal var invIB: Float = 0.toFloat()

    internal var type: ManifoldType? = null

    internal var radiusA: Float = 0.toFloat()

    internal var radiusB: Float = 0.toFloat()

    internal var pointCount: Int = 0
}
