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

package com.soywiz.korge.dragonbones

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * - The Dragonbones slot.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Dragonbones 插槽。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class KorgeDbSlot(pool: BaseObjectPool) : Slot(pool) {
	override fun toString(): String {
		return "[class DragonbonesSlot]"
	}

	private var _textureScale: Double = 1.0
	private var _renderDisplay: DisplayObject? = null

	override fun _onClear() {
		super._onClear()

		this._textureScale = 1.0
		this._renderDisplay = null
	}

	override fun _initDisplay(value: Any, isRetain: Boolean) {
	}

	override fun _disposeDisplay(value: Any, isRelease: Boolean) {
		if (!isRelease) {
			//(value as DisplayObject).destroy()
		}
	}

	override fun _onUpdateDisplay() {
		this._renderDisplay = (if (this._display != null) this._display else this._rawDisplay) as DisplayObject
	}

	override fun _addDisplay() {
		val container = this._armature?.display as KorgeDbArmatureDisplay
		container.addChild(this._renderDisplay!!)
	}

	override fun _replaceDisplay(value: Any) {
		val container = this._armature?.display as KorgeDbArmatureDisplay
		val prevDisplay = value as DisplayObject
		container.addChild(this._renderDisplay!!)
		container.swapChildren(this._renderDisplay!!, prevDisplay)
		container.removeChild(prevDisplay)
		this._textureScale = 1.0
	}

	override fun _removeDisplay() {
		this._renderDisplay?.parent?.removeChild(this._renderDisplay)
	}

	override fun _updateZOrder() {
		val container = this._armature?.display as KorgeDbArmatureDisplay
		val index = container.getChildIndex(this._renderDisplay!!)
		if (index == this._zOrder) {
			return
		}

		container.addChildAt(this._renderDisplay!!, this._zOrder)
	}

	/**
	 * @internal
	 */
	override fun _updateVisible() {
		this._renderDisplay?.visible = this._parent!!.visible && this._visible
	}

	override fun _updateBlendMode() {
		if (this._renderDisplay is Container) {
			when (this._blendMode) {
				com.dragonbones.core.BlendMode.Normal -> this._renderDisplay?.blendMode = BlendMode.INHERIT
				com.dragonbones.core.BlendMode.Add -> this._renderDisplay?.blendMode = BlendMode.ADD
				com.dragonbones.core.BlendMode.Darken -> this._renderDisplay?.blendMode = BlendMode.DARKEN
				com.dragonbones.core.BlendMode.Difference -> this._renderDisplay?.blendMode = BlendMode.DIFFERENCE
				com.dragonbones.core.BlendMode.HardLight -> this._renderDisplay?.blendMode = BlendMode.HARDLIGHT
				com.dragonbones.core.BlendMode.Lighten -> this._renderDisplay?.blendMode = BlendMode.LIGHTEN
				com.dragonbones.core.BlendMode.Multiply -> this._renderDisplay?.blendMode = BlendMode.MULTIPLY
				com.dragonbones.core.BlendMode.Overlay -> this._renderDisplay?.blendMode = BlendMode.OVERLAY
				com.dragonbones.core.BlendMode.Screen -> this._renderDisplay?.blendMode = BlendMode.SCREEN
				else -> Unit
			}
		}
		// TODO child armature.
	}

	override fun _updateColor() {
		val alpha = this._colorTransform.alphaMultiplier * this._globalAlpha
		//this._renderDisplay?.alpha = alpha
		//this._renderDisplay?.alpha = 1.0

		if (this._renderDisplay is Image || this._renderDisplay is Mesh) {
			val color = (round(this._colorTransform.redMultiplier * 0xFF).toInt() shl 16) +
					(round(this._colorTransform.greenMultiplier * 0xFF).toInt() shl 8) +
					round(this._colorTransform.blueMultiplier * 0xFF).toInt()
			this._renderDisplay?.tint = RGBA(color, (alpha * 255.0).toInt())
		} else {
			this._renderDisplay?.alpha = alpha
		}
		// TODO child armature.
	}

	override fun _updateFrame() {
		var currentTextureData = this._textureData as KorgeDbTextureData?

		if (this._displayIndex >= 0 && this._display !== null && currentTextureData !== null) {
			var currentTextureAtlasData = currentTextureData.parent as KorgeDbTextureAtlasData

			if (this._armature?.replacedTexture != null) { // Update replaced texture atlas.
				if (this._armature?._replaceTextureAtlasData === null) {
					currentTextureAtlasData = pool.borrowObject<KorgeDbTextureAtlasData>()
					currentTextureAtlasData.copyFrom(currentTextureData.parent as KorgeDbTextureAtlasData)
					currentTextureAtlasData.renderTexture = this._armature?.replacedTexture as? Bitmap?
					this._armature?._replaceTextureAtlasData = currentTextureAtlasData
				} else {
					currentTextureAtlasData = this._armature?._replaceTextureAtlasData as KorgeDbTextureAtlasData
				}

				currentTextureData = currentTextureAtlasData.getTexture(currentTextureData.name) as KorgeDbTextureData
			}

			val renderTexture = currentTextureData.renderTexture
			if (renderTexture !== null) {
				if (this._geometryData !== null) { // Mesh.
					val data = this._geometryData!!.data!!
					val intArray = data.intArray!!
					val floatArray = data.floatArray!!
					val vertexCount =
						intArray[this._geometryData!!.offset + BinaryOffset.GeometryVertexCount].toInt()
					val triangleCount =
						intArray[this._geometryData!!.offset + BinaryOffset.GeometryTriangleCount].toInt()
					var vertexOffset =
						intArray[this._geometryData!!.offset + BinaryOffset.GeometryFloatOffset].toInt()

					if (vertexOffset < 0) {
						vertexOffset += 65536 // Fixed out of bounds bug.
					}

					val uvOffset = vertexOffset + vertexCount * 2
					val scale = this._armature!!._armatureData!!.scale

					val meshDisplay = this._renderDisplay as Mesh
					val textureAtlasWidth =
						if (currentTextureAtlasData.width > 0.0) currentTextureAtlasData.width else renderTexture.bmp.width
					val textureAtlasHeight =
						if (currentTextureAtlasData.height > 0.0) currentTextureAtlasData.height else renderTexture.bmp.height
					val region = currentTextureData.region

					meshDisplay.vertices = Float32BufferAlloc(vertexCount * 2)
					meshDisplay.uvs = Float32BufferAlloc(vertexCount * 2)
					meshDisplay.indices = Uint16BufferAlloc(triangleCount * 3)
					meshDisplay.name = name

					//println("Slot[$name]=(vertexCount=$vertexCount,triangleCount=$triangleCount,vertexOffset=$vertexOffset)")

					//for (let i = 0, l = vertexCount * 2; i < l; ++i) {
					for (i in 0 until vertexCount * 2) {
						meshDisplay.vertices[i] = (floatArray[vertexOffset + i] * scale).toFloat()
					}

					//for (let i = 0; i < triangleCount * 3; ++i) {
					for (i in 0 until triangleCount * 3) {
						meshDisplay.indices[i] =
								intArray[this._geometryData!!.offset + BinaryOffset.GeometryVertexIndices + i].toInt()
					}

					//for (let i = 0, l = vertexCount * 2; i < l; i += 2) {
					for (i in 0 until vertexCount * 2 step 2) {
						val u = floatArray[uvOffset + i]
						val v = floatArray[uvOffset + i + 1]

						if (currentTextureData.rotated) {
							meshDisplay.uvs[i] = ((region.x + (1.0 - v) * region.width) / textureAtlasWidth).toFloat()
							meshDisplay.uvs[i + 1] = ((region.y + u * region.height) / textureAtlasHeight).toFloat()
						} else {
							meshDisplay.uvs[i] = ((region.x + u * region.width) / textureAtlasWidth).toFloat()
							meshDisplay.uvs[i + 1] = ((region.y + v * region.height) / textureAtlasHeight).toFloat()
						}
					}

					this._textureScale = 1.0
					meshDisplay.texture = renderTexture
					meshDisplay.dirty++
					meshDisplay.indexDirty++
					meshDisplay.name = this.name

					val isSkinned = this._geometryData!!.weight !== null
					val isSurface = this._parent?._boneData?.isSurface ?: false
					if (isSkinned || isSurface) {
						this._identityTransform()
					}
				} else { // Normal texture.
					this._textureScale = currentTextureData.parent!!.scale * this._armature!!._armatureData!!.scale
					val normalDisplay = this._renderDisplay as Image
					normalDisplay.texture = renderTexture
					//normalDisplay.name = renderTexture.name
					normalDisplay.name = this.name
				}

				this._visibleDirty = true

				return
			}
		}

		if (this._geometryData !== null) {
			val meshDisplay = this._renderDisplay as Mesh
			//meshDisplay.texture = null as any
			meshDisplay.texture = null
			meshDisplay.x = 0.0
			meshDisplay.y = 0.0
			meshDisplay.visible = false
		} else {
			val normalDisplay = this._renderDisplay as Image
			//normalDisplay.bitmap = null as any
			normalDisplay.bitmap = Bitmaps.transparent
			normalDisplay.x = 0.0
			normalDisplay.y = 0.0
			normalDisplay.visible = false
		}
	}

	override fun _updateMesh() {
		val scale = this._armature!!._armatureData!!.scale
		val deformVertices = (this._displayFrame as DisplayFrame).deformVertices
		val bones = this._geometryBones
		val geometryData = this._geometryData as GeometryData
		val weightData = geometryData.weight

		val hasDeform = deformVertices.size > 0 && geometryData.inheritDeform
		val meshDisplay = this._renderDisplay as Mesh

		//println("_updateMesh:" + this.name + ", hasDeform=$hasDeform, weightData=${weightData?.count}, deformVertices=${deformVertices.size}")

		if (weightData !== null) {
			val data = geometryData.data!!
			val intArray = data.intArray!!
			val floatArray = data.floatArray!!
			val vertexCount = intArray[geometryData.offset + BinaryOffset.GeometryVertexCount].toInt()
			var weightFloatOffset = intArray[weightData.offset + BinaryOffset.WeigthFloatOffset].toInt()

			if (weightFloatOffset < 0) {
				weightFloatOffset += 65536 // Fixed out of bounds bug.
			}

			//for (let i = 0, iD = 0, iB = weightData.offset + BinaryOffset.WeigthBoneIndices + bones.length, iV = weightFloatOffset, iF = 0;i < vertexCount;++i) {
			var iD = 0
			var iB = weightData.offset + BinaryOffset.WeigthBoneIndices + bones.length
			var iV = weightFloatOffset
			var iF = 0
			for (i in 0 until vertexCount) {
				if (iD >= meshDisplay.vertices.size) break // @TODO: This shouldn't be required!

				val boneCount = intArray[iB++]
				var xG = 0f
				var yG = 0f

				//for (let j = 0; j < boneCount; ++j) {
				for (j in 0 until boneCount) {
					val boneIndex = intArray[iB++].toInt()
					val bone = bones[boneIndex] ?: continue

					val matrix = bone.globalTransformMatrix
					val weight = floatArray[iV++]
					var xL: Float = (floatArray[iV++] * scale).toFloat()
					var yL: Float = (floatArray[iV++] * scale).toFloat()

					if (hasDeform) {
						xL += deformVertices[iF++].toFloat()
						yL += deformVertices[iF++].toFloat()
					}

					xG += matrix.transformX(xL.toFloat(), yL.toFloat()) * weight
					yG += matrix.transformY(xL.toFloat(), yL.toFloat()) * weight
				}

				meshDisplay.vertices[iD++] = xG
				meshDisplay.vertices[iD++] = yG
			}
		} else {
			val isSurface = this._parent?._boneData?.isSurface ?: false
			val data = geometryData.data!!
			val intArray = data.intArray!!
			val floatArray = data.floatArray!!
			val vertexCount = intArray[geometryData.offset + BinaryOffset.GeometryVertexCount].toInt()
			var vertexOffset = intArray[geometryData.offset + BinaryOffset.GeometryFloatOffset].toInt()

			if (vertexOffset < 0) {
				vertexOffset += 65536 // Fixed out of bounds bug.
			}

			//for (let i = 0, l = vertexCount * 2; i < l; i += 2) {
			for (i in 0 until vertexCount * 2 step 2) {
				if (i + 1 >= meshDisplay.vertices.size) break // @TODO: This shouldn't be required!

				var x: Float = (floatArray[vertexOffset + i + 0] * scale).toFloat()
				var y: Float = (floatArray[vertexOffset + i + 1] * scale).toFloat()

				if (hasDeform) {
					x += deformVertices[i + 0].toFloat()
					y += deformVertices[i + 1].toFloat()
				}

				if (isSurface) {
					val matrix = (this._parent as Surface)._getGlobalTransformMatrix(x, y)

					meshDisplay.vertices[i + 0] = matrix.transformX(x, y).toFloat()
					meshDisplay.vertices[i + 1] = matrix.transformY(x, y).toFloat()
				} else {
					meshDisplay.vertices[i + 0] = x.toFloat()
					meshDisplay.vertices[i + 1] = y.toFloat()
				}
			}
		}

		meshDisplay.updatedVertices()
	}

	private val m = com.soywiz.korma.geom.Matrix()

	override fun _updateTransform() {
		this.updateGlobalTransform() // Update transform.

		val transform = this.global

		if (this._renderDisplay === this._rawDisplay || this._renderDisplay === this._meshDisplay) {
			val x =
				transform.x - (this.globalTransformMatrix.a * this._pivotX + this.globalTransformMatrix.c * this._pivotY)
			val y =
				transform.y - (this.globalTransformMatrix.b * this._pivotX + this.globalTransformMatrix.d * this._pivotY)
			this._renderDisplay
				?.position(x, y)
				?.scale(transform.scaleX * this._textureScale, transform.scaleY * this._textureScale)
				?.rotation(transform.rotation)
				?.skew(-transform.skew, 0.0)
		} else {
			this._renderDisplay
				?.position(transform.x, transform.y)
				?.scale(transform.scaleX, transform.scaleY)
				?.rotation(transform.rotation)
				?.skew(-transform.skew, 0.0)
		}
	}

	override fun _identityTransform() {
		m.identity()
		this._renderDisplay?.setMatrix(m)
	}
}
