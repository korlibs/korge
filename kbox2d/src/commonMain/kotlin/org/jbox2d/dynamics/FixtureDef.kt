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
package org.jbox2d.dynamics

import org.jbox2d.collision.shapes.*
import org.jbox2d.userdata.*

/**
 * A fixture definition is used to create a fixture. This class defines an abstract fixture
 * definition. You can reuse fixture definitions safely.
 *
 * @author daniel
 */
data class FixtureDef(
    /**
     * The shape, this must be set. The shape will be cloned, so you can create the shape on the
     * stack.
     */
    var shape: Shape? = null,

    /**
     * Use this to store application specific fixture data.
     */
    var userData: Any? = null,

    /**
     * The friction coefficient, usually in the range [0,1].
     */
    var friction: Float = .2f,

    /**
     * The restitution (elasticity) usually in the range [0,1].
     */
    var restitution: Float = 0f,

    /**
     * The density, usually in kg/m^2
     */
    var density: Float = 0f,

    /**
     * A sensor shape collects contact information but never generates a collision response.
     */
    var isSensor: Boolean = false,

    /**
     * Contact filtering data;
     */
    var filter: Filter = Filter()
) : Box2dTypedUserData by Box2dTypedUserData.Mixin()
