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

/**
 * This is the viewport transform used from drawing. Use yFlip if you are drawing from the top-left
 * corner.
 *
 * @author Daniel
 */
interface IViewportTransform {

    /**
     * @return if the transform flips the y axis
     */
    /**
     * @param yFlip if we flip the y axis when transforming
     */
    var isYFlip: Boolean

    /**
     * This is the half-width and half-height. This should be the actual half-width and half-height,
     * not anything transformed or scaled. Not a copy.
     */
    /**
     * This sets the half-width and half-height. This should be the actual half-width and half-height,
     * not anything transformed or scaled.
     */
    var extents: Vec2

    /**
     * center of the viewport. Not a copy.
     */
    /**
     * sets the center of the viewport.
     */
    var center: Vec2

    val mat22Representation: Mat22

    /**
     * This sets the half-width and half-height of the viewport. This should be the actual half-width
     * and half-height, not anything transformed or scaled.
     */
    fun setExtents(halfWidth: Float, halfHeight: Float)

    /**
     * sets the center of the viewport.
     */
    fun setCenter(x: Float, y: Float)

    /**
     * Sets the transform's center to the given x and y coordinates, and using the given scale.
     */
    fun setCamera(x: Float, y: Float, scale: Float)

    /**
     * Transforms the given directional vector by the viewport transform (not positional)
     */
    fun getWorldVectorToScreen(world: Vec2, screen: Vec2)


    /**
     * Transforms the given directional screen vector back to the world direction.
     */
    fun getScreenVectorToWorld(screen: Vec2, world: Vec2)


    /**
     * takes the world coordinate (world) puts the corresponding screen coordinate in screen. It
     * should be safe to give the same object as both parameters.
     */
    fun getWorldToScreen(world: Vec2, screen: Vec2)


    /**
     * takes the screen coordinates (screen) and puts the corresponding world coordinates in world. It
     * should be safe to give the same object as both parameters.
     */
    fun getScreenToWorld(screen: Vec2, world: Vec2)

    /**
     * Multiplies the viewport transform by the given Mat22
     */
    fun mulByTransform(transform: Mat22)
}
