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
@file:Suppress("KDocUnresolvedReference")

package com.dragonbones.armature

import com.dragonbones.core.*
import com.soywiz.korma.geom.*

/**
 * - The base class of the transform object.
 * @see dragonBones.Transform
 * @version DragonBones 4.5
 * @language en_US
 */
/**
 * - 变换对象的基类。
 * @see dragonBones.Transform
 * @version DragonBones 4.5
 * @language zh_CN
 */
abstract class TransformObject(pool: SingleObjectPool<out TransformObject>) : BaseObject(pool) {
	internal val _helpMatrix: Matrix get() = pool._helpMatrix
	internal val _helpTransform: TransformDb get() = pool._helpTransform
	internal val _helpPoint: Point get() = pool._helpPoint

	/**
	 * - A matrix relative to the armature coordinate system.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 相对于骨架坐标系的矩阵。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val globalTransformMatrix: Matrix = Matrix()
	/**
	 * - A transform relative to the armature coordinate system.
	 * @see #updateGlobalTransform()
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 相对于骨架坐标系的变换。
	 * @see #updateGlobalTransform()
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val global: TransformDb = TransformDb()
	/**
	 * - The offset transform relative to the armature or the parent bone coordinate system.
	 * @see #dragonBones.Bone#invalidUpdate()
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 相对于骨架或父骨骼坐标系的偏移变换。
	 * @see #dragonBones.Bone#invalidUpdate()
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val offset: TransformDb = TransformDb()
	/**
	 * @private
	 */
	var origin: TransformDb? = null
	/**
	 * @private
	 */
	var userData: Any? = null
	protected var _globalDirty: Boolean = false
	/**
	 * @internal
	 */
	var _alpha: Double = 1.0
	/**
	 * @internal
	 */
	var _globalAlpha: Double = 1.0
	/**
	 * @internal
	 */
	var _armature: Armature? = null

	/**
	 */
	override fun _onClear() {
		this.globalTransformMatrix.identity()
		this.global.identity()
		this.offset.identity()
		this.origin = null
		this.userData = null

		this._globalDirty = false
		this._alpha = 1.0
		this._globalAlpha = 1.0
		//this._armature = null //
		this._armature = null //
	}
	/**
	 * - For performance considerations, rotation or scale in the {@link #global} attribute of the bone or slot is not always properly accessible,
	 * some engines do not rely on these attributes to update rendering, such as Egret.
	 * The use of this method ensures that the access to the {@link #global} property is correctly rotation or scale.
	 * @example
	 * <pre>
	 *     bone.updateGlobalTransform();
	 *     var rotation = bone.global.rotation;
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 出于性能的考虑，骨骼或插槽的 {@link #global} 属性中的旋转或缩放并不总是正确可访问的，有些引擎并不依赖这些属性更新渲染，比如 Egret。
	 * 使用此方法可以保证访问到 {@link #global} 属性中正确的旋转或缩放。
	 * @example
	 * <pre>
	 *     bone.updateGlobalTransform();
	 *     var rotation = bone.global.rotation;
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun updateGlobalTransform() {
		if (this._globalDirty) {
			this._globalDirty = false
			this.global.fromMatrix(this.globalTransformMatrix)
		}
	}
	/**
	 * - The armature to which it belongs.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所属的骨架。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val armature: Armature get() = this._armature!!
}
