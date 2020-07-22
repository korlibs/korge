/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine

import com.soywiz.korim.color.RGBAf

/** Stores the setup pose for a [Bone].  */
class BoneData {
    /** The index of the bone in [Skeleton.getBones].  */
    val index: Int

    /** The name of the bone, which is unique across all bones in the skeleton.  */
    val name: String

    /** @return May be null.
     */
    val parent: BoneData?

    /** The bone's length.  */
    var length: Float = 0.toFloat()

    /** The local x translation.  */
    var x: Float = 0.toFloat()

    /** The local y translation.  */
    var y: Float = 0.toFloat()

    /** The local rotation.  */
    var rotation: Float = 0.toFloat()

    /** The local scaleX.  */
    var scaleX = 1f

    /** The local scaleY.  */
    var scaleY = 1f

    /** The local shearX.  */
    var shearX: Float = 0.toFloat()

    /** The local shearX.  */
    var shearY: Float = 0.toFloat()
    internal var transformMode = TransformMode.normal

    /** When true, [Skeleton.updateWorldTransform] only updates this bone if the [Skeleton.getSkin] contains this
     * bone.
     * @see Skin.getBones
     */
    var skinRequired: Boolean = false

    // Nonessential.
    /** The color of the bone as it was in Spine. Available only when nonessential data was exported. Bones are not usually
     * rendered at runtime.  */
    val color = RGBAf(0.61f, 0.61f, 0.61f, 1f) // 9b9b9bff

    /** @param parent May be null.
     */
    constructor(index: Int, name: String, parent: BoneData?) {
        require(index >= 0) { "index must be >= 0." }
        this.index = index
        this.name = name
        this.parent = parent
    }

    /** Copy constructor.
     * @param parent May be null.
     */
    constructor(bone: BoneData, parent: BoneData) {
        index = bone.index
        name = bone.name
        this.parent = parent
        length = bone.length
        x = bone.x
        y = bone.y
        rotation = bone.rotation
        scaleX = bone.scaleX
        scaleY = bone.scaleY
        shearX = bone.shearX
        shearY = bone.shearY
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    /** The transform mode for how parent world transforms affect this bone.  */
    fun getTransformMode(): TransformMode {
        return transformMode
    }

    fun setTransformMode(transformMode: TransformMode) {
        this.transformMode = transformMode
    }

    override fun toString(): String {
        return name
    }

    /** Determines how a bone inherits world transforms from parent bones.  */
    enum class TransformMode {
        normal, onlyTranslation, noRotationOrReflection, noScale, noScaleOrReflection;


        companion object {

            val values = TransformMode.values()
        }
    }
}
