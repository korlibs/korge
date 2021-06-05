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
import com.dragonbones.util.length
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

/**
 * @private
 */
class GeometryData {
	var isShared: Boolean = false
	var inheritDeform: Boolean = false
	var offset: Int = 0
	var data: DragonBonesData? = null
	var weight: WeightData? = null // Initial value.

	fun clear() {
		if (!this.isShared && this.weight != null) {
			this.weight?.returnToPool()
		}

		this.isShared = false
		this.inheritDeform = false
		this.offset = 0
		//this.data = null
		this.data = null
		this.weight = null
	}

	fun shareFrom(value: GeometryData) {
		this.isShared = true
		this.offset = value.offset
		this.weight = value.weight
	}

	val vertexCount: Int get() {
		val intArray = this.data!!.intArray
		return intArray!![this.offset + BinaryOffset.GeometryVertexCount].toInt()
	}

	val triangleCount: Int get() {
		val intArray = this.data!!.intArray
		return intArray!![this.offset + BinaryOffset.GeometryTriangleCount].toInt()
	}
}
/**
 * @private
 */
abstract class DisplayData (pool: SingleObjectPool<out DisplayData>) : BaseObject(pool) {
	var type: DisplayType = DisplayType.None
	var name: String = ""
	var path: String = ""
	val transform: TransformDb = TransformDb()
	lateinit var parent: SkinData

	override fun _onClear() {
		this.name = ""
		this.path = ""
		this.transform.identity()
		//this.parent = null //
	}
}

abstract class GeometryDisplayData (pool: SingleObjectPool<out GeometryDisplayData>) : DisplayData(pool) {
    val geometry: GeometryData = GeometryData()
}

/**
 * @private
 */
class ImageDisplayData(pool: SingleObjectPool<ImageDisplayData>) :  DisplayData(pool) {
	override fun toString(): String {
		return "[class dragonBones.ImageDisplayData]"
	}

	val pivot: Point = Point()
	var texture: TextureData? = null

	override fun _onClear() {
		super._onClear()

		this.type = DisplayType.Image
		this.pivot.clear()
		this.texture = null
	}
}
/**
 * @private
 */
class ArmatureDisplayData(pool: SingleObjectPool<ArmatureDisplayData>) :  DisplayData(pool) {
	override fun toString(): String {
		return "[class dragonBones.ArmatureDisplayData]"
	}

	var inheritAnimation: Boolean = false
	val actions: FastArrayList<ActionData> = FastArrayList()
	var armature: ArmatureData? = null

	override fun _onClear() {
		super._onClear()

		this.actions.fastForEach { action ->
			action.returnToPool()
		}

		this.type = DisplayType.Armature
		this.inheritAnimation = false
		this.actions.length = 0
		this.armature = null
	}

	/**
	 * @private
	 */
	fun addAction(value: ActionData) {
        this.actions.add(value)
	}
}
/**
 * @private
 */
class MeshDisplayData(pool: SingleObjectPool<MeshDisplayData>) :  GeometryDisplayData(pool) {
	override fun toString(): String {
		return "[class dragonBones.MeshDisplayData]"
	}

	var texture: TextureData? = null

	override fun _onClear() {
		super._onClear()

		this.type = DisplayType.Mesh
		this.geometry.clear()
		this.texture = null
	}
}
/**
 * @private
 */
class BoundingBoxDisplayData(pool: SingleObjectPool<BoundingBoxDisplayData>) :  DisplayData(pool) {
	override fun toString(): String {
		return "[class dragonBones.BoundingBoxDisplayData]"
	}

	var boundingBox: BoundingBoxData? = null // Initial value.

	override fun _onClear() {
		super._onClear()

		if (this.boundingBox != null) {
			this.boundingBox?.returnToPool()
		}

		this.type = DisplayType.BoundingBox
		this.boundingBox = null
	}
}
/**
 * @private
 */
class PathDisplayData(pool: SingleObjectPool<PathDisplayData>) :  GeometryDisplayData(pool) {
	override fun toString(): String {
		return "[class dragonBones.PathDisplayData]"
	}

	var closed: Boolean = false
	var constantSpeed: Boolean = false
	var curveLengths: DoubleArray = DoubleArray(0)

	override fun _onClear() {
		super._onClear()

		this.type = DisplayType.Path
		this.closed = false
		this.constantSpeed = false
		this.geometry.clear()
		this.curveLengths = DoubleArray(0)
	}
}
/**
 * @private
 */
class WeightData(pool: SingleObjectPool<WeightData>) :  BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.WeightData]"
	}

	var count: Int = 0
	var offset: Int = 0
	val bones: FastArrayList<BoneData> = FastArrayList()

	override fun _onClear() {
		this.count = 0
		this.offset = 0
		this.bones.lengthSet = 0
	}

	fun addBone(value: BoneData) {
        this.bones.add(value)
	}
}
