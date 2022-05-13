/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.model

import com.dragonbones.core.*
import com.dragonbones.util.*
import com.soywiz.kds.*

/**
 * @private
 */
abstract class ConstraintData(pool: SingleObjectPool<out ConstraintData>) : BaseObject(pool) {
	var order: Int = 0
	var name: String = ""
	var type: ConstraintType = ConstraintType.IK
	var target: BoneData? = null
	var root: BoneData? = null
	var bone: BoneData? = null

	override fun _onClear() {
		this.order = 0
		this.name = ""
		this.type = ConstraintType.IK
		this.target = null //
		this.root = null //
		this.bone = null
	}
}

/**
 * @internal
 */
class IKConstraintData(pool: SingleObjectPool<IKConstraintData>) : ConstraintData(pool) {
	override fun toString(): String = "[class dragonBones.IKConstraintData]"

	var scaleEnabled: Boolean = false
	var bendPositive: Boolean = false
	var weight: Double = 1.0

	override fun _onClear() {
		super._onClear()

		this.scaleEnabled = false
		this.bendPositive = false
		this.weight = 1.0
	}
}

/**
 * @internal
 */
class PathConstraintData(pool: SingleObjectPool<PathConstraintData>) : ConstraintData(pool) {
	override fun toString(): String = "[class dragonBones.PathConstraintData]"

	var pathSlot: SlotData? = null
	var pathDisplayData: PathDisplayData? = null
	var bones: FastArrayList<BoneData> = FastArrayList()

	var positionMode: PositionMode = PositionMode.Fixed
	var spacingMode: SpacingMode = SpacingMode.Fixed
	var rotateMode: RotateMode = RotateMode.Chain

	var position: Double = 0.0
	var spacing: Double = 0.0
	var rotateOffset: Double = 0.0
	var rotateMix: Double = 0.0
	var translateMix: Double = 0.0

	override fun _onClear() {
		super._onClear()

		this.pathSlot = null
		this.pathDisplayData = null
		this.bones.lengthSet = 0

		this.positionMode = PositionMode.Fixed
		this.spacingMode = SpacingMode.Fixed
		this.rotateMode = RotateMode.Chain

		this.position = 0.0
		this.spacing = 0.0
		this.rotateOffset = 0.0
		this.rotateMix = 0.0
		this.translateMix = 0.0
	}

	fun AddBone(value: BoneData) {
        this.bones.add(value)
	}
}
