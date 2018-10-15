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
package com.dragonbones.armature

import com.dragonbones.core.*
import com.dragonbones.geom.*
import com.dragonbones.model.*
import com.soywiz.kds.*
import kotlin.math.*

/**
 * - Bone is one of the most important logical units in the armature animation system,
 * and is responsible for the realization of translate, rotation, scaling in the animations.
 * A armature can contain multiple bones.
 * @see dragonBones.BoneData
 * @see dragonBones.Armature
 * @see dragonBones.Slot
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨骼在骨骼动画体系中是最重要的逻辑单元之一，负责动画中的平移、旋转、缩放的实现。
 * 一个骨架中可以包含多个骨骼。
 * @see dragonBones.BoneData
 * @see dragonBones.Armature
 * @see dragonBones.Slot
 * @version DragonBones 3.0
 * @language zh_CN
 */
open class Bone(pool: BaseObjectPool) :  TransformObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.Bone]"
	}
	/**
	 * - The offset mode.
	 * @see #offset
	 * @version DragonBones 5.5
	 * @language en_US
	 */
	/**
	 * - 偏移模式。
	 * @see #offset
	 * @version DragonBones 5.5
	 * @language zh_CN
	 */
	var offsetMode: OffsetMode = OffsetMode.Additive
	/**
	 * @internal
	 */
	val animationPose: Transform = Transform()
	/**
	 * @internal
	 */
	var _transformDirty: Boolean = false
	/**
	 * @internal
	 */
	var _childrenTransformDirty: Boolean = false
	protected var _localDirty: Boolean = true
	/**
	 * @internal
	 */
	var _hasConstraint: Boolean = false
	protected var _visible: Boolean = true
	protected var _cachedFrameIndex: Int = -1
	/**
	 * @internal
	 */
	var _boneData: BoneData? = null
	/**
	 * @private
	 */
	protected var _parent: Bone? = null
	/**
	 * @internal
	 */
	var _cachedFrameIndices:  IntArrayList? = null

	override fun _onClear(): Unit {
		super._onClear()

		this.offsetMode = OffsetMode.Additive
		this.animationPose.identity()

		this._transformDirty = false
		this._childrenTransformDirty = false
		this._localDirty = true
		this._hasConstraint = false
		this._visible = true
		this._cachedFrameIndex = -1
		this._boneData = null //
		this._parent = null //
		this._cachedFrameIndices = null
	}

	protected open fun _updateGlobalTransformMatrix(isCache: Boolean): Unit {
		// For typescript.
		val boneData = this._boneData
		val global = this.global
		val globalTransformMatrix = this.globalTransformMatrix
		val origin = this.origin
		val offset = this.offset
		val animationPose = this.animationPose
		val parent = this._parent //

		val flipX = this._armature!!.flipX
		val flipY = this._armature!!.flipY == DragonBones.yDown
		var inherit = parent != null
		var rotation = 0.0

		if (this.offsetMode == OffsetMode.Additive) {
			if (origin != null) {
				// global.copyFrom(this.origin).add(this.offset).add(this.animationPose);
				global.x = origin.x + offset.x + animationPose.x
				global.scaleX = origin.scaleX * offset.scaleX * animationPose.scaleX
				global.scaleY = origin.scaleY * offset.scaleY * animationPose.scaleY

				if (DragonBones.yDown) {
					global.y = origin.y + offset.y + animationPose.y
					global.skew = origin.skew + offset.skew + animationPose.skew
					global.rotation = origin.rotation + offset.rotation + animationPose.rotation
				}
				else {
					global.y = origin.y - offset.y + animationPose.y
					global.skew = origin.skew - offset.skew + animationPose.skew
					global.rotation = origin.rotation - offset.rotation + animationPose.rotation
				}
			}
			else {
				global.copyFrom(offset)

				if (!DragonBones.yDown) {
					global.y = -global.y
					global.skew = -global.skew
					global.rotation = -global.rotation
				}

				global.add(animationPose)
			}
		}
		else if (this.offsetMode == OffsetMode.None) {
			if (origin != null) {
				global.copyFrom(origin).add(animationPose)
			}
			else {
				global.copyFrom(animationPose)
			}
		}
		else {
			inherit = false
			global.copyFrom(offset)

			if (!DragonBones.yDown) {
				global.y = -global.y
				global.skew = -global.skew
				global.rotation = -global.rotation
			}
		}

		if (inherit) {
			val parent = parent!!
			val isSurface = parent._boneData!!.isSurface
			val surfaceBone = if (isSurface) (parent as Surface)._bone else null
			val parentMatrix = if (isSurface) (parent as Surface)._getGlobalTransformMatrix(global.x, global.y) else parent.globalTransformMatrix

			if (boneData!!.inheritScale && (!isSurface || surfaceBone != null)) {
				if (isSurface) {
					if (boneData.inheritRotation) {
						global.rotation += parent.global.rotation
					}

					(surfaceBone as Bone).updateGlobalTransform()
					global.scaleX *= surfaceBone.global.scaleX
					global.scaleY *= surfaceBone.global.scaleY
					parentMatrix.transformPoint(global.x, global.y, global)
					global.toMatrix(globalTransformMatrix)

					if (boneData.inheritTranslation) {
						global.x = globalTransformMatrix.tx
						global.y = globalTransformMatrix.ty
					}
					else {
						globalTransformMatrix.tx = global.x
						globalTransformMatrix.ty = global.y
					}
				}
				else {
					if (!boneData.inheritRotation) {
						parent.updateGlobalTransform()

						if (flipX && flipY) {
							rotation = global.rotation - (parent.global.rotation + PI)
						}
						else if (flipX) {
							rotation = global.rotation + parent.global.rotation + PI
						}
						else if (flipY) {
							rotation = (global.rotation + parent.global.rotation).toDouble()
						}
						else {
							rotation = (global.rotation - parent.global.rotation).toDouble()
						}

						global.rotation = rotation.toFloat()
					}

					global.toMatrix(globalTransformMatrix)
					globalTransformMatrix.concat(parentMatrix)

					if (boneData.inheritTranslation) {
						global.x = globalTransformMatrix.tx
						global.y = globalTransformMatrix.ty
					}
					else {
						globalTransformMatrix.tx = global.x
						globalTransformMatrix.ty = global.y
					}

					if (isCache) {
						global.fromMatrix(globalTransformMatrix)
					}
					else {
						this._globalDirty = true
					}
				}
			}
			else {
				if (boneData.inheritTranslation) {
					val x = global.x
					val y = global.y
					global.x = parentMatrix.a * x + parentMatrix.c * y + parentMatrix.tx
					global.y = parentMatrix.b * x + parentMatrix.d * y + parentMatrix.ty
				}
				else {
					if (flipX) {
						global.x = -global.x
					}

					if (flipY) {
						global.y = -global.y
					}
				}

				if (boneData.inheritRotation) {
					parent.updateGlobalTransform()

					if (parent.global.scaleX < 0.0) {
						rotation = global.rotation + parent.global.rotation + PI
					}
					else {
						rotation = (global.rotation + parent.global.rotation).toDouble()
					}

					if (parentMatrix.a * parentMatrix.d - parentMatrix.b * parentMatrix.c < 0.0) {
						rotation -= global.rotation * 2.0

						if (flipX != flipY || boneData.inheritReflection) {
							global.skew += PI.toFloat()
						}

						if (!DragonBones.yDown) {
							global.skew = -global.skew
						}
					}

					global.rotation = rotation.toFloat()
				}
				else if (flipX || flipY) {
					if (flipX && flipY) {
						rotation = global.rotation + PI
					}
					else {
						if (flipX) {
							rotation = PI - global.rotation
						}
						else {
							rotation = (-global.rotation).toDouble()
						}

						global.skew += PI.toFloat()
					}

					global.rotation = rotation.toFloat()
				}

				global.toMatrix(globalTransformMatrix)
			}
		}
		else {
			if (flipX || flipY) {
				if (flipX) {
					global.x = -global.x
				}

				if (flipY) {
					global.y = -global.y
				}

				if (flipX && flipY) {
					rotation = global.rotation + PI
				}
				else {
					if (flipX) {
						rotation = PI - global.rotation
					}
					else {
						rotation = (-global.rotation).toDouble()
					}

					global.skew += PI.toFloat()
				}

				global.rotation = rotation.toFloat()
			}

			global.toMatrix(globalTransformMatrix)
		}
	}

	/**
	 * @internal
	 */
	fun _updateAlpha() {
		if (this._parent != null) {
			this._globalAlpha = this._alpha * this._parent!!._globalAlpha
		}
		else {
			this._globalAlpha = this._alpha * this._armature!!._globalAlpha
		}
	}

	/**
	 * @internal
	 */
	open fun init(boneData: BoneData, armatureValue: Armature) {
		if (this._boneData != null) {
			return
		}

		this._boneData = boneData
		this._armature = armatureValue
		this._alpha = this._boneData!!.alpha

		if (this._boneData?.parent != null) {
			this._parent = this._armature?.getBone(this._boneData?.parent?.name)
		}

		this._armature?._addBone(this)
		//
		this.origin = this._boneData?.transform
	}

	/**
	 * @internal
	 */
	open fun update(cacheFrameIndex: Int) {
		var cacheFrameIndex = cacheFrameIndex
		if (cacheFrameIndex >= 0 && this._cachedFrameIndices != null) {
			val cachedFrameIndex = this._cachedFrameIndices!![cacheFrameIndex]
			if (cachedFrameIndex >= 0 && this._cachedFrameIndex == cachedFrameIndex) { // Same cache.
				this._transformDirty = false
			}
			else if (cachedFrameIndex >= 0) { // Has been Cached.
				this._transformDirty = true
				this._cachedFrameIndex = cachedFrameIndex
			}
			else {
				if (this._hasConstraint) { // Update constraints.
					for (constraint in this._armature!!._constraints) {
						if (constraint._root == this) {
							constraint.update()
						}
					}
				}

				if (
					this._transformDirty ||
					(this._parent != null && this._parent!!._childrenTransformDirty)
				) { // Dirty.
					this._transformDirty = true
					this._cachedFrameIndex = -1
				}
				else if (this._cachedFrameIndex >= 0) { // Same cache, but not set index yet.
					this._transformDirty = false
					this._cachedFrameIndices!![cacheFrameIndex] = this._cachedFrameIndex
				}
				else { // Dirty.
					this._transformDirty = true
					this._cachedFrameIndex = -1
				}
			}
		}
		else {
			if (this._hasConstraint) { // Update constraints.
				for (constraint in this._armature!!._constraints) {
					if (constraint._root == this) {
						constraint.update()
					}
				}
			}

			if (this._transformDirty || (this._parent != null && this._parent!!._childrenTransformDirty)) { // Dirty.
				cacheFrameIndex = -1
				this._transformDirty = true
				this._cachedFrameIndex = -1
			}
		}

		if (this._transformDirty) {
			this._transformDirty = false
			this._childrenTransformDirty = true
			//
			if (this._cachedFrameIndex < 0) {
				val isCache = cacheFrameIndex >= 0
				if (this._localDirty) {
					this._updateGlobalTransformMatrix(isCache)
				}

				if (isCache && this._cachedFrameIndices != null) {
					val res = this._armature!!._armatureData!!.setCacheFrame(this.globalTransformMatrix, this.global)
					this._cachedFrameIndex = res
					this._cachedFrameIndices!![cacheFrameIndex] = res
				}
			}
			else {
				this._armature?._armatureData?.getCacheFrame(this.globalTransformMatrix, this.global, this._cachedFrameIndex)
			}
			//
		}
		else if (this._childrenTransformDirty) {
			this._childrenTransformDirty = false
		}

		this._localDirty = true
	}

	/**
	 * @internal
	 */
	fun updateByConstraint() {
		if (this._localDirty) {
			this._localDirty = false

			if (this._transformDirty || (this._parent != null && this._parent!!._childrenTransformDirty)) {
				this._updateGlobalTransformMatrix(true)
			}

			this._transformDirty = true
		}
	}
	/**
	 * - Forces the bone to update the transform in the next frame.
	 * When the bone is not animated or its animation state is finished, the bone will not continue to update,
	 * and when the skeleton must be updated for some reason, the method needs to be called explicitly.
	 * @example
	 * <pre>
	 *     var bone = armature.getBone("arm");
	 *     bone.offset.scaleX = 2.0;
	 *     bone.invalidUpdate();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 强制骨骼在下一帧更新变换。
	 * 当该骨骼没有动画状态或其动画状态播放完成时，骨骼将不在继续更新，而此时由于某些原因必须更新骨骼时，则需要显式调用该方法。
	 * @example
	 * <pre>
	 *     var bone = armature.getBone("arm");
	 *     bone.offset.scaleX = 2.0;
	 *     bone.invalidUpdate();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun invalidUpdate() {
		this._transformDirty = true
	}
	/**
	 * - Check whether the bone contains a specific bone.
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查该骨骼是否包含特定的骨骼。
	 * @see dragonBones.Bone
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun contains(value: Bone): Boolean {
		if (value == this) {
			return false
		}

		var ancestor: Bone? = value
		while (ancestor != this && ancestor != null) {
			ancestor = ancestor.parent
		}

		return ancestor == this
	}
	/**
	 * - The bone data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 骨骼数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val boneData: BoneData get() = this._boneData!!
	/**
	 * - The visible of all slots in the bone.
	 * @default true
	 * @see dragonBones.Slot#visible
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 此骨骼所有插槽的可见。
	 * @default true
	 * @see dragonBones.Slot#visible
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var visible: Boolean
		get() = this._visible
		set(value) {
			if (this._visible == value) {
				return
			}

			this._visible = value

			for (slot in this._armature!!.getSlots()) {
				if (slot.parent == this) {
					slot._updateVisible()
				}
			}
		}
	/**
	 * - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val name: String get() = this._boneData?.name ?: ""
	/**
	 * - The parent bone to which it belongs.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所属的父骨骼。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val parent: Bone? get() = this._parent
}
