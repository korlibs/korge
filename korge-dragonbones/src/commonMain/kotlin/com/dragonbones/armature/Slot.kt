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
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.dragonbones.util.length
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * @private
 */
class DisplayFrame(pool: SingleObjectPool<DisplayFrame>) :  BaseObject(pool) {
	override fun toString(): String {
		return "[class dragonBones.DisplayFrame]"
	}

	var rawDisplayData: DisplayData? = null
	var displayData: DisplayData? = null
	var _textureData: TextureData? = null
	//var display: Armature? = null
	var display: Any? = null
	var deformVertices:  DoubleArray = DoubleArray(0)
	//var deformVertices:  FloatArray = FloatArray(0)

    val rawGeometryDisplayData get() = rawDisplayData as GeometryDisplayData
    val rawMeshDisplayData get() = rawDisplayData as MeshDisplayData
    val rawPathDisplayData get() = rawDisplayData as PathDisplayData

    val geometryDisplayData get() = displayData as GeometryDisplayData
    val meshDisplayData get() = displayData as MeshDisplayData
    val pathDisplayData get() = displayData as PathDisplayData

    override fun _onClear() {
		this.rawDisplayData = null
		this.displayData = null
		this._textureData = null
		this.display = null
		this.deformVertices = DoubleArray(0)
	}

	fun updateDeformVertices() {
		if (this.rawDisplayData == null || this.deformVertices.size != 0) {
			return
		}

        val rawGeometryData: GeometryData = when (this.rawDisplayData?.type) {
            DisplayType.Mesh -> (this.rawMeshDisplayData).geometry
            DisplayType.Path -> (this.rawPathDisplayData).geometry
            else -> return
        }

		var vertexCount = 0
		if (rawGeometryData.weight != null) {
			vertexCount = rawGeometryData.weight!!.count * 2
		}
		else {
			vertexCount = rawGeometryData.data!!.intArray!![rawGeometryData.offset + BinaryOffset.GeometryVertexCount] * 2
		}

		this.deformVertices = DoubleArray(vertexCount)
		//this.deformVertices = FloatArray(vertexCount)
		//for (var i = 0, l = this.deformVertices.length; i < l; ++i) {
		for (i in 0 until this.deformVertices.size) {
			this.deformVertices[i] = 0.0
		}
	}

	fun getGeometryData(): GeometryData? {
		if (this.displayData != null) {
			if (this.displayData?.type == DisplayType.Mesh) {
				return (this.displayData as MeshDisplayData).geometry
			}

			if (this.displayData?.type == DisplayType.Path) {
				return (this.displayData as PathDisplayData).geometry
			}
		}

		if (this.rawDisplayData != null) {
			if (this.rawDisplayData?.type == DisplayType.Mesh) {
				return (this.rawMeshDisplayData).geometry
			}

			if (this.rawDisplayData?.type == DisplayType.Path) {
				return (this.rawPathDisplayData).geometry
			}
		}

		return null
	}

	fun getBoundingBox(): BoundingBoxData? {
		if (this.displayData != null && this.displayData?.type == DisplayType.BoundingBox) {
			return (this.displayData as BoundingBoxDisplayData).boundingBox
		}

		if (this.rawDisplayData != null && this.rawDisplayData?.type == DisplayType.BoundingBox) {
			return (this.rawDisplayData as BoundingBoxDisplayData).boundingBox
		}

		return null
	}

	fun getTextureData(): TextureData? {
		if (this.displayData != null) {
			if (this.displayData?.type == DisplayType.Image) {
				return (this.displayData as ImageDisplayData).texture
			}

			if (this.displayData?.type == DisplayType.Mesh) {
				return (this.displayData as MeshDisplayData).texture
			}
		}

		if (this._textureData != null) {
			return this._textureData
		}

		if (this.rawDisplayData != null) {
			if (this.rawDisplayData?.type == DisplayType.Image) {
				return (this.rawDisplayData as ImageDisplayData).texture
			}

			if (this.rawDisplayData?.type == DisplayType.Mesh) {
				return (this.rawDisplayData as MeshDisplayData).texture
			}
		}

		return null
	}
}
/**
 * - The slot attached to the armature, controls the display status and properties of the display object.
 * A bone can contain multiple slots.
 * A slot can contain multiple display objects, displaying only one of the display objects at a time,
 * but you can toggle the display object into frame animation while the animation is playing.
 * The display object can be a normal texture, or it can be a display of a child armature, a grid display object,
 * and a custom other display object.
 * @see dragonBones.Armature
 * @see dragonBones.Bone
 * @see dragonBones.SlotData
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 插槽附着在骨骼上，控制显示对象的显示状态和属性。
 * 一个骨骼上可以包含多个插槽。
 * 一个插槽中可以包含多个显示对象，同一时间只能显示其中的一个显示对象，但可以在动画播放的过程中切换显示对象实现帧动画。
 * 显示对象可以是普通的图片纹理，也可以是子骨架的显示容器，网格显示对象，还可以是自定义的其他显示对象。
 * @see dragonBones.Armature
 * @see dragonBones.Bone
 * @see dragonBones.SlotData
 * @version DragonBones 3.0
 * @language zh_CN
 */
abstract class Slot(pool: SingleObjectPool<out Slot>) :  TransformObject(pool) {
	/**
	 * - Displays the animated state or mixed group name controlled by the object, set to null to be controlled by all animation states.
	 * @default null
	 * @see dragonBones.AnimationState#displayControl
	 * @see dragonBones.AnimationState#name
	 * @see dragonBones.AnimationState#group
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 显示对象受到控制的动画状态或混合组名称，设置为 null 则表示受所有的动画状态控制。
	 * @default null
	 * @see dragonBones.AnimationState#displayControl
	 * @see dragonBones.AnimationState#name
	 * @see dragonBones.AnimationState#group
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var displayController: String? = null
	protected var _displayDataDirty: Boolean = false
	protected var _displayDirty: Boolean = false
	protected var _geometryDirty: Boolean = false
	protected var _textureDirty: Boolean = false
	protected var _visibleDirty: Boolean = false
	protected var _blendModeDirty: Boolean = false
	protected var _zOrderDirty: Boolean = false
	/**
	 * @internal
	 */
	var _colorDirty: Boolean = false
	/**
	 * @internal
	 */
	var _verticesDirty: Boolean = false
	protected var _transformDirty: Boolean = false
	protected var _visible: Boolean = true
	protected var _blendMode: BlendMode = BlendMode.Normal
	protected var _displayIndex: Int = -1
	protected var _animationDisplayIndex: Int = -1
	protected var _cachedFrameIndex: Int = -1
	/**
	 * @internal
	 */
	var _zOrder = 0
	/**
	 * @internal
	 */
	var _zIndex: Int = 0
	/**
	 * @internal
	 */
	var _pivotX: Double = 0.0
	/**
	 * @internal
	 */
	var _pivotY: Double = 0.0
	protected val _localMatrix: Matrix = Matrix()
	/**
	 * @internal
	 */
	val _colorTransform: ColorTransform = ColorTransform()
	/**
	 * @internal
	 */
	val _displayFrames: FastArrayList<DisplayFrame> = FastArrayList()
	/**
	 * @internal
	 */
	val _geometryBones: FastArrayList<Bone?> = FastArrayList()
	/**
	 * @internal
	 */
	var _slotData: SlotData? = null
	/**
	 * @internal
	 */
	var _displayFrame: DisplayFrame? = null
	/**
	 * @internal
	 */
	var _geometryData: GeometryData? = null
	protected var _boundingBoxData: BoundingBoxData? = null
	protected var _textureData: TextureData? = null
	protected var _rawDisplay: Any? = null // Initial value.
	protected var _meshDisplay: Any? = null // Initial value.
	protected var _display: Any? = null
	protected var _childArmature: Armature? = null
	/**
	 * @private
	 */
	internal var _parent: Bone? = null
	/**
	 * @internal
	 */
	//var _cachedFrameIndices:  DoubleArray? = null
	var _cachedFrameIndices:  IntArrayList? = null

	override fun _onClear() {
		super._onClear()

		val disposeDisplayList: FastArrayList<Any> = FastArrayList()
		this._displayFrames.fastForEach { dispayFrame ->
			val display = dispayFrame.display
			if (
				display != this._rawDisplay && display != this._meshDisplay &&
				disposeDisplayList.indexOf(display) < 0
			) {
                disposeDisplayList.add(display!!)
			}

			dispayFrame.returnToPool()
		}

		disposeDisplayList.fastForEach { eachDisplay ->
			if (eachDisplay is Armature) {
				eachDisplay.dispose()
			} else {
				this._disposeDisplay(eachDisplay, true)
			}
		}

		if (this._meshDisplay != null && this._meshDisplay != this._rawDisplay) { // May be _meshDisplay and _rawDisplay is the same one.
			this._disposeDisplay(this._meshDisplay!!, false)
		}

		if (this._rawDisplay != null) {
			this._disposeDisplay(this._rawDisplay!!, false)
		}

		this.displayController = null

		this._displayDataDirty = false
		this._displayDirty = false
		this._geometryDirty = false
		this._textureDirty = false
		this._visibleDirty = false
		this._blendModeDirty = false
		this._zOrderDirty = false
		this._colorDirty = false
		this._verticesDirty = false
		this._transformDirty = false
		this._visible = true
		this._blendMode = BlendMode.Normal
		this._displayIndex = -1
		this._animationDisplayIndex = -1
		this._zOrder = 0
		this._zIndex = 0
		this._cachedFrameIndex = -1
		this._pivotX = 0.0
		this._pivotY = 0.0
		this._localMatrix.identity()
		this._colorTransform.identity()
		this._displayFrames.clear()
		this._geometryBones.clear()
		this._slotData = null //
		this._displayFrame = null
		this._geometryData = null
		this._boundingBoxData = null
		this._textureData = null
		this._rawDisplay = null
		this._meshDisplay = null
		this._display = null
		this._childArmature = null
		this._parent = null //
		this._cachedFrameIndices = null
	}

	protected abstract fun _initDisplay(value: Any, isRetain: Boolean): Unit
	protected abstract fun _disposeDisplay(value: Any, isRelease: Boolean): Unit
	protected abstract fun _onUpdateDisplay(): Unit
	protected abstract fun _addDisplay(): Unit
	protected abstract fun _replaceDisplay(value: Any): Unit
	protected abstract fun _removeDisplay(): Unit
	protected abstract fun _updateZOrder(): Unit
	/**
	 * @internal
	 */
	abstract fun _updateVisible(): Unit

	protected abstract fun _updateBlendMode(): Unit
	protected abstract fun _updateColor(): Unit
	protected abstract fun _updateFrame(): Unit
	protected abstract fun _updateMesh(): Unit
	protected abstract fun _updateTransform(): Unit
	protected abstract fun _identityTransform()

	protected fun _hasDisplay(display: Any?): Boolean {
		this._displayFrames.fastForEach { displayFrame ->
			if (displayFrame.display == display) {
				return true
			}
		}

		return false
	}

	/**
	 * @internal
	 */
	fun _isBonesUpdate(): Boolean {
		this._geometryBones.fastForEach { bone ->
			if (bone != null && bone._childrenTransformDirty) {
				return true
			}
		}

		return false
	}

	/**
	 * @internal
	 */
	fun _updateAlpha() {
		val globalAlpha = this._alpha * this._parent!!._globalAlpha

		if (this._globalAlpha != globalAlpha) {
			this._globalAlpha = globalAlpha
			this._colorDirty = true
		}
	}

	protected fun _updateDisplayData() {
		//println("_updateDisplayData:$name,_displayIndex=$_displayIndex")
		val prevDisplayFrame = this._displayFrame
		val prevGeometryData = this._geometryData
		val prevTextureData = _textureData
		var rawDisplayData: DisplayData? = null
		var displayData: DisplayData? = null

		this._displayFrame = null
		this._geometryData = null
		this._boundingBoxData = null
		this._textureData = null

		if (this._displayIndex >= 0 && this._displayIndex < this._displayFrames.lengthSet) {
			this._displayFrame = this._displayFrames[this._displayIndex]
			rawDisplayData = this._displayFrame!!.rawDisplayData
			displayData = this._displayFrame!!.displayData

			this._geometryData = this._displayFrame!!.getGeometryData()
			this._boundingBoxData = this._displayFrame!!.getBoundingBox()
			this._textureData = this._displayFrame!!.getTextureData()
		}

		val _textureData = this._textureData
		if (
			this._displayFrame != prevDisplayFrame ||
			this._geometryData != prevGeometryData || _textureData != prevTextureData
		) {
			// Update pivot offset.
			if (this._geometryData == null && _textureData != null) {
				val imageDisplayData = (if (displayData != null && displayData.type == DisplayType.Image) displayData else rawDisplayData) as ImageDisplayData //
				val scale = _textureData.parent!!.scale * this._armature!!._armatureData!!.scale
				val frame = _textureData.frame

				this._pivotX = imageDisplayData.pivot.xf.toDouble()
				this._pivotY = imageDisplayData.pivot.yf.toDouble()

				val rect = frame ?: _textureData.region
				var width = rect.width
				var height = rect.height

				if (_textureData.rotated && frame == null) {
					width = rect.height
					height = rect.width
				}

				this._pivotX *= width * scale
				this._pivotY *= height * scale

				if (frame != null) {
					this._pivotX += frame.x * scale
					this._pivotY += frame.y * scale
				}

				// Update replace pivot. TODO
				if (rawDisplayData != null && imageDisplayData != rawDisplayData) {
					rawDisplayData.transform.toMatrix(_helpMatrix)
					_helpMatrix.invert()
					_helpMatrix.transform(0f, 0f, _helpPoint)
					this._pivotX -= _helpPoint.xf
					this._pivotY -= _helpPoint.yf

					imageDisplayData.transform.toMatrix(_helpMatrix)
					_helpMatrix.invert()
					_helpMatrix.transform(0f, 0f, _helpPoint)
					this._pivotX += _helpPoint.xf
					this._pivotY += _helpPoint.yf
				}

				if (!DragonBones.yDown) {
					this._pivotY = (if (_textureData.rotated) _textureData.region.width else _textureData.region.height) * scale - this._pivotY
				}
			}
			else {
				this._pivotX = 0.0
				this._pivotY = 0.0
			}

			// Update original transform.
			if (rawDisplayData != null) { // Compatible.
				this.origin = rawDisplayData.transform
			}
			else if (displayData != null) { // Compatible.
				this.origin = displayData.transform
			}
			else {
				this.origin = null
			}

			// TODO remove slot offset.
			if (this.origin != null) {
				this.global.copyFrom(this.origin!!).add(this.offset).toMatrix(this._localMatrix)
			}
			else {
				this.global.copyFrom(this.offset).toMatrix(this._localMatrix)
			}

			// Update geometry.
			if (this._geometryData != prevGeometryData) {
				this._geometryDirty = true
				this._verticesDirty = true

				if (this._geometryData != null) {
					this._geometryBones.clear()
					val gd = this._geometryData!!.weight
					if (gd != null) {
						for (i in 0 until gd.bones.lengthSet) {
							val bone = this._armature!!.getBone(gd.bones[i].name)
                            this._geometryBones.add(bone)
						}
					}
				}
				else {
					this._geometryBones.lengthSet = 0
					this._geometryData = null
				}
			}

			this._textureDirty = _textureData != prevTextureData
			this._transformDirty = true
		}
	}

	protected fun _updateDisplay() {
		val prevDisplay = this._display ?: this._rawDisplay
		val prevChildArmature = _childArmature

		// Update display and child armature.
		val _displayFrame1 = this._displayFrame
		if (_displayFrame1 != null) {
			this._display = _displayFrame1.display
			if (this._display != null && this._display is Armature) {
				this._childArmature = this._display as Armature
				this._display = _childArmature?.display
			}
			else {
				this._childArmature = null
			}
		}
		else {
			this._display = null
			this._childArmature = null
		}

		// Update display.
		val currentDisplay = this._display ?: this._rawDisplay
		if (currentDisplay != prevDisplay) {
			this._textureDirty = true
			this._visibleDirty = true
			this._blendModeDirty = true
			// this._zOrderDirty = true;
			this._colorDirty = true
			this._transformDirty = true

			this._onUpdateDisplay()
			this._replaceDisplay(prevDisplay!!)
		}

		// Update child armature.
		val _childArmature = this._childArmature
		if (_childArmature != prevChildArmature) {
			if (prevChildArmature != null) {
				prevChildArmature._parent = null // Update child armature parent.
				prevChildArmature.clock = null
				if (prevChildArmature.inheritAnimation) {
					prevChildArmature.animation.reset()
				}
			}

			if (_childArmature != null) {
				_childArmature._parent = this // Update child armature parent.
				_childArmature.clock = this._armature!!.clock
				if (_childArmature.inheritAnimation) { // Set child armature cache frameRate.
					if (_childArmature.cacheFrameRate == 0) {
						val cacheFrameRate = this._armature!!.cacheFrameRate
						if (cacheFrameRate != 0) {
							_childArmature.cacheFrameRate = cacheFrameRate
						}
					}

					// Child armature action.
					if (_displayFrame1 != null) {
						var actions: FastArrayList<ActionData>? = null
						val displayData = this._displayFrame!!.displayData ?: this._displayFrame!!.rawDisplayData
						if (displayData != null && displayData.type == DisplayType.Armature) {
							actions = (displayData as ArmatureDisplayData).actions
						}

						if (actions != null && actions.lengthSet > 0) {
							actions.fastForEach { action ->
								val eventObject = pool.eventObject.borrow()
								EventObject.actionDataToInstance(action, eventObject, this._armature!!)
								eventObject.slot = this
								this._armature!!._bufferAction(eventObject, false)
							}
						}
						else {
							_childArmature.animation.play()
						}
					}
				}
			}
		}
	}

	protected fun _updateGlobalTransformMatrix(isCache: Boolean) {
		val parentMatrix = if (this._parent!!._boneData!!.isBone) this._parent!!.globalTransformMatrix else (this._parent as Surface)._getGlobalTransformMatrix(this.global.xf, this.global.yf)
		this.globalTransformMatrix.copyFrom(this._localMatrix)
		this.globalTransformMatrix.concat(parentMatrix)

		if (isCache) {
			this.global.fromMatrix(this.globalTransformMatrix)
		}
		else {
			this._globalDirty = true
		}
	}

	/**
	 * @internal
	 */
	fun _setDisplayIndex(value: Int, isAnimation: Boolean = false) {
		if (isAnimation) {
			if (this._animationDisplayIndex == value) {
				return
			}

			this._animationDisplayIndex = value
		}

		if (this._displayIndex == value) {
			return
		}

		this._displayIndex = if (value < this._displayFrames.lengthSet) value else this._displayFrames.lengthSet - 1
		this._displayDataDirty = true
		this._displayDirty = this._displayIndex < 0 || this._display != this._displayFrames[this._displayIndex].display
	}

	/**
	 * @internal
	 */
	fun _setZOrder(value: Int): Boolean {
		if (this._zOrder == value) {
			// return false;
		}

		this._zOrder = value
		this._zOrderDirty = true

		return this._zOrderDirty
	}

	/**
	 * @internal
	 */
	fun _setColor(value: ColorTransform): Boolean {
		this._colorTransform.copyFrom(value)
		this._colorDirty = true
		return true
	}

	/**
	 * @internal
	 */
	fun init(slotData: SlotData, armatureValue: Armature, rawDisplay: Any, meshDisplay: Any) {
		if (this._slotData != null) {
			return
		}

		this._slotData = slotData
		this._colorDirty = true //
		this._blendModeDirty = true //
		this._blendMode = slotData.blendMode
		this._zOrder = slotData.zOrder
		this._zIndex = slotData.zIndex
		this._alpha = slotData.alpha
		this._colorTransform.copyFrom(slotData.color!!)
		this._rawDisplay = rawDisplay
		this._meshDisplay = meshDisplay
		//
		this._armature = armatureValue
		val slotParent = this._armature?.getBone(slotData.parent?.name)

		if (slotParent != null) {
			this._parent = slotParent
		}
		else {
			// Never;
		}

		this._armature?._addSlot(this)
		//
		this._initDisplay(this._rawDisplay!!, false)
		if (this._rawDisplay != this._meshDisplay) {
			this._initDisplay(this._meshDisplay!!, false)
		}

		this._onUpdateDisplay()
		this._addDisplay()
	}

	/**
	 * @internal
	 */
	fun update(cacheFrameIndex: Int) {
		var cacheFrameIndex = cacheFrameIndex
		if (this._displayDataDirty) {
			this._updateDisplayData()
			this._displayDataDirty = false
		}

		if (this._displayDirty) {
			this._updateDisplay()
			this._displayDirty = false
		}

		if (this._geometryDirty || this._textureDirty) {
			if (this._display == null || this._display == this._rawDisplay || this._display == this._meshDisplay) {
				this._updateFrame()
			}

			this._geometryDirty = false
			this._textureDirty = false
		}

		if (this._display == null) {
			return
		}

		if (this._visibleDirty) {
			this._updateVisible()
			this._visibleDirty = false
		}

		if (this._blendModeDirty) {
			this._updateBlendMode()
			this._blendModeDirty = false
		}

		if (this._colorDirty) {
			this._updateColor()
			this._colorDirty = false
		}

		if (this._zOrderDirty) {
			this._updateZOrder()
			this._zOrderDirty = false
		}

		if (this._geometryData != null && this._display == this._meshDisplay) {
			val isSkinned = this._geometryData!!.weight != null
			val isSurface = this._parent!!._boneData?.isSurface ?: false

			if (
				this._verticesDirty ||
				(isSkinned && this._isBonesUpdate()) ||
				(isSurface && this._parent!!._childrenTransformDirty)
			) {
				this._verticesDirty = false // Allow update mesh to reset the dirty value.
				this._updateMesh()
			}

			if (isSkinned || isSurface) { // Compatible.
				return
			}
		}

		if (cacheFrameIndex >= 0 && this._cachedFrameIndices != null) {
			val cachedFrameIndex = this._cachedFrameIndices!![cacheFrameIndex]
			if (cachedFrameIndex >= 0 && this._cachedFrameIndex == cachedFrameIndex) { // Same cache.
				this._transformDirty = false
			}
			else if (cachedFrameIndex >= 0) { // Has been Cached.
				this._transformDirty = true
				this._cachedFrameIndex = cachedFrameIndex
			}
			else if (this._transformDirty || this._parent!!._childrenTransformDirty) { // Dirty.
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
		else if (this._transformDirty || this._parent!!._childrenTransformDirty) { // Dirty.
			cacheFrameIndex = -1
			this._transformDirty = true
			this._cachedFrameIndex = -1
		}

		if (this._transformDirty) {
			if (this._cachedFrameIndex < 0) {
				val isCache = cacheFrameIndex >= 0
				this._updateGlobalTransformMatrix(isCache)

				if (isCache && this._cachedFrameIndices != null) {
					val res = this._armature!!._armatureData!!.setCacheFrame(this.globalTransformMatrix, this.global)
					this._cachedFrameIndex = res
					this._cachedFrameIndices!![cacheFrameIndex] = res
				}
			}
			else {
				this._armature?._armatureData?.getCacheFrame(this.globalTransformMatrix, this.global, this._cachedFrameIndex)
			}

			this._updateTransform()
			this._transformDirty = false
		}
	}
	/**
	 * - Forces the slot to update the state of the display object in the next frame.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 强制插槽在下一帧更新显示对象的状态。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun invalidUpdate() {
		this._displayDataDirty = true
		this._displayDirty = true
		//
		this._transformDirty = true
	}

	/**
	 * @private
	 */
	fun updateTransformAndMatrix() {
		if (this._transformDirty) {
			this._updateGlobalTransformMatrix(false)
			this._transformDirty = false
		}
	}

	/**
	 * @private
	 */
	fun replaceRawDisplayData(displayData: DisplayData?, index: Int = -1) {
		var index = index
		if (index < 0) {
			index = if (this._displayIndex < 0) 0 else this._displayIndex
		}
		else if (index >= this._displayFrames.lengthSet) {
			return
		}

		val displayFrame = this._displayFrames[index]
		if (displayFrame.rawDisplayData != displayData) {
			displayFrame.deformVertices = DoubleArray(0)
			displayFrame.rawDisplayData = displayData
			if (displayFrame.rawDisplayData == null) {
				val defaultSkin = this._armature?._armatureData?.defaultSkin
				if (defaultSkin != null) {
					val defaultRawDisplayDatas = defaultSkin.getDisplays(this._slotData?.name)
					if (defaultRawDisplayDatas != null && index < defaultRawDisplayDatas.lengthSet) {
						displayFrame.rawDisplayData = defaultRawDisplayDatas[index]
					}
				}
			}

			if (index == this._displayIndex) {
				this._displayDataDirty = true
			}
		}
	}

	/**
	 * @private
	 */
	fun replaceDisplayData(displayData: DisplayData?, index: Int = -1) {
		var index = index
		if (index < 0) {
			index = if (this._displayIndex < 0) 0 else this._displayIndex
		}
		else if (index >= this._displayFrames.lengthSet) {
			return
		}

		val displayFrame = this._displayFrames[index]
		if (displayFrame.displayData != displayData && displayFrame.rawDisplayData != displayData) {
			displayFrame.displayData = displayData

			if (index == this._displayIndex) {
				this._displayDataDirty = true
			}
		}
	}

	/**
	 * @private
	 */
	fun replaceTextureData(textureData: TextureData?, index: Int = -1) {
		var index = index
		if (index < 0) {
			index = if (this._displayIndex < 0) 0 else this._displayIndex
		}
		else if (index >= this._displayFrames.lengthSet) {
			return
		}

		val displayFrame = this._displayFrames[index]
		if (displayFrame._textureData != textureData) {
			displayFrame._textureData = textureData

			if (index == this._displayIndex) {
				this._displayDataDirty = true
			}
		}
	}

	/**
	 * @private
	 */
	//fun replaceDisplay(value: Armature?, index: Int = -1) {
	fun replaceDisplay(value: Any?, index: Int = -1) {
		var index = index

		if (index < 0) {
			index = if (this._displayIndex < 0) 0 else this._displayIndex
		}
		else if (index >= this._displayFrames.lengthSet) {
			return
		}

		val displayFrame = this._displayFrames[index]
		if (displayFrame.display != value) {
			val prevDisplay = displayFrame.display
			displayFrame.display = value

			if (
				prevDisplay != null &&
				prevDisplay != this._rawDisplay && prevDisplay != this._meshDisplay &&
				!this._hasDisplay(prevDisplay)
			) {
				if (prevDisplay is Armature) {
					// (eachDisplay as Armature).dispose();
				}
				else {
					this._disposeDisplay(prevDisplay, true)
				}
			}

			if (
				value != null &&
				value != this._rawDisplay && value != this._meshDisplay &&
				!this._hasDisplay(prevDisplay) &&
				(value !is Armature)
			) {
				this._initDisplay(value, true)
			}

			if (index == this._displayIndex) {
				this._displayDirty = true
			}
		}
	}
	/**
	 * - Check whether a specific point is inside a custom bounding box in the slot.
	 * The coordinate system of the point is the inner coordinate system of the armature.
	 * Custom bounding boxes need to be customized in Dragonbones Pro.
	 * @param x - The horizontal coordinate of the point.
	 * @param y - The vertical coordinate of the point.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查特定点是否在插槽的自定义边界框内。
	 * 点的坐标系为骨架内坐标系。
	 * 自定义边界框需要在 DragonBones Pro 中自定义。
	 * @param x - 点的水平坐标。
	 * @param y - 点的垂直坐标。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun containsPoint(x: Double, y: Double): Boolean {
		if (this._boundingBoxData == null) {
			return false
		}

		this.updateTransformAndMatrix()

		_helpMatrix.copyFrom(this.globalTransformMatrix)
		_helpMatrix.invert()
		_helpMatrix.transform(x, y, _helpPoint)

		return this._boundingBoxData!!.containsPoint(_helpPoint.xf.toDouble(), _helpPoint.yf.toDouble())
	}
	/**
	 * - Check whether a specific segment intersects a custom bounding box for the slot.
	 * The coordinate system of the segment and intersection is the inner coordinate system of the armature.
	 * Custom bounding boxes need to be customized in Dragonbones Pro.
	 * @param xA - The horizontal coordinate of the beginning of the segment.
	 * @param yA - The vertical coordinate of the beginning of the segment.
	 * @param xB - The horizontal coordinate of the end point of the segment.
	 * @param yB - The vertical coordinate of the end point of the segment.
	 * @param intersectionPointA - The first intersection at which a line segment intersects the bounding box from the beginning to the end. (If not set, the intersection point will not calculated)
	 * @param intersectionPointB - The first intersection at which a line segment intersects the bounding box from the end to the beginning. (If not set, the intersection point will not calculated)
	 * @param normalRadians - The normal radians of the tangent of the intersection boundary box. [x: Normal radian of the first intersection tangent, y: Normal radian of the second intersection tangent] (If not set, the normal will not calculated)
	 * @returns Intersection situation. [1: Disjoint and segments within the bounding box, 0: Disjoint, 1: Intersecting and having a nodal point and ending in the bounding box, 2: Intersecting and having a nodal point and starting at the bounding box, 3: Intersecting and having two intersections, N: Intersecting and having N intersections]
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查特定线段是否与插槽的自定义边界框相交。
	 * 线段和交点的坐标系均为骨架内坐标系。
	 * 自定义边界框需要在 DragonBones Pro 中自定义。
	 * @param xA - 线段起点的水平坐标。
	 * @param yA - 线段起点的垂直坐标。
	 * @param xB - 线段终点的水平坐标。
	 * @param yB - 线段终点的垂直坐标。
	 * @param intersectionPointA - 线段从起点到终点与边界框相交的第一个交点。 （如果未设置，则不计算交点）
	 * @param intersectionPointB - 线段从终点到起点与边界框相交的第一个交点。 （如果未设置，则不计算交点）
	 * @param normalRadians - 交点边界框切线的法线弧度。 [x: 第一个交点切线的法线弧度, y: 第二个交点切线的法线弧度] （如果未设置，则不计算法线）
	 * @returns 相交的情况。 [-1: 不相交且线段在包围盒内, 0: 不相交, 1: 相交且有一个交点且终点在包围盒内, 2: 相交且有一个交点且起点在包围盒内, 3: 相交且有两个交点, N: 相交且有 N 个交点]
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point? = null,
		intersectionPointB: Point? = null,
		normalRadians: Point? = null
	): Int {
		if (this._boundingBoxData == null) {
			return 0
		}

		this.updateTransformAndMatrix()
		_helpMatrix.copyFrom(this.globalTransformMatrix)
		_helpMatrix.invert()
		_helpMatrix.transform(xA, yA, _helpPoint)
		val xA = _helpPoint.xf
		val yA = _helpPoint.yf
		_helpMatrix.transform(xB, yB, _helpPoint)
		val xB = _helpPoint.xf
		val yB = _helpPoint.yf

		val intersectionCount = this._boundingBoxData!!.intersectsSegment(xA.toDouble(),
			yA.toDouble(), xB.toDouble(), yB.toDouble(), intersectionPointA, intersectionPointB, normalRadians)
		if (intersectionCount > 0) {
			if (intersectionCount == 1 || intersectionCount == 2) {
				if (intersectionPointA != null) {
					this.globalTransformMatrix.transform(intersectionPointA.xf, intersectionPointA.yf, intersectionPointA)
					if (intersectionPointB != null) {
						intersectionPointB.xf = intersectionPointA.xf
						intersectionPointB.yf = intersectionPointA.yf
					}
				}
				else if (intersectionPointB != null) {
					this.globalTransformMatrix.transform(intersectionPointB.xf, intersectionPointB.yf, intersectionPointB)
				}
			}
			else {
				if (intersectionPointA != null) {
					this.globalTransformMatrix.transform(intersectionPointA.xf, intersectionPointA.yf, intersectionPointA)
				}

				if (intersectionPointB != null) {
					this.globalTransformMatrix.transform(intersectionPointB.xf, intersectionPointB.yf, intersectionPointB)
				}
			}

			if (normalRadians != null) {
				this.globalTransformMatrix.deltaTransformPoint(cos(normalRadians.xf), sin(normalRadians.xf), _helpPoint)
				normalRadians.xf = atan2(_helpPoint.yf, _helpPoint.xf)

				this.globalTransformMatrix.deltaTransformPoint(cos(normalRadians.yf), sin(normalRadians.yf), _helpPoint)
				normalRadians.yf = atan2(_helpPoint.yf, _helpPoint.xf)
			}
		}

		return intersectionCount
	}

	/**
	 * @private
	 */
	fun getDisplayFrameAt(index: Int): DisplayFrame {
		return this._displayFrames[index]
	}
	/**
	 * - The visible of slot's display object.
	 * @default true
	 * @version DragonBones 5.6
	 * @language en_US
	 */
	/**
	 * - 插槽的显示对象的可见。
	 * @default true
	 * @version DragonBones 5.6
	 * @language zh_CN
	 */
	var visible: Boolean get() {
		return this._visible
	}
	set (value: Boolean) {
		if (this._visible == value) {
			return
		}

		this._visible = value
		this._updateVisible()
	}
	/**
	 * @private
	 */
	var displayFrameCount: Int get() {
		return this._displayFrames.lengthSet
	}
	set(value) {
		val prevCount = this._displayFrames.lengthSet
		if (prevCount < value) {
			this._displayFrames.lengthSet = value

			//for (var i = prevCount; i < value; ++i) {
			for (i in prevCount until value) {
				this._displayFrames[i] = pool.displayFrame.borrow()
			}
		}
		else if (prevCount > value) {
			var i = prevCount - 1
			while (i < value) {
				this.replaceDisplay(null, i)
				this._displayFrames[i].returnToPool()
				--i
			}

			this._displayFrames.lengthSet = value
		}
	}
	/**
	 * - The index of the display object displayed in the display list.
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     slot.displayIndex = 3;
	 *     slot.displayController = "none";
	 * </pre>
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 此时显示的显示对象在显示列表中的索引。
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     slot.displayIndex = 3;
	 *     slot.displayController = "none";
	 * </pre>
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	var displayIndex: Int get() {
		return this._displayIndex
	}
	set(value) {
		this._setDisplayIndex(value)
		this.update(-1)
	}
	/**
	 * - The slot name.
	 * @see dragonBones.SlotData#name
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 插槽名称。
	 * @see dragonBones.SlotData#name
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	val name: String get() {
		return this._slotData?.name ?: ""
	}
	/**
	 * - Contains a display list of display objects or child armatures.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 包含显示对象或子骨架的显示列表。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var displayList: FastArrayList<Any?> get() {
		val displays = FastArrayList<Any?>()
		this._displayFrames.fastForEach { displayFrame ->
            displays.add(displayFrame.display)
		}

		return displays
	}
	set(value: FastArrayList<Any?>) {
		this.displayFrameCount = value.length
		var index = 0
		value.fastForEach { eachDisplay ->
			this.replaceDisplay(eachDisplay, index++)
		}
	}
	/**
	 * - The slot data.
	 * @see dragonBones.SlotData
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 插槽数据。
	 * @see dragonBones.SlotData
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val slotData: SlotData get() = this._slotData!!
	/**
	 * - The custom bounding box data for the slot at current time.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 插槽此时的自定义包围盒数据。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	val boundingBoxData: BoundingBoxData? get()

	{
		return this._boundingBoxData
	}

	/**
	 * @private
	 */
	val rawDisplay: Any get() = this._rawDisplay!!

	/**
	 * @private
	 */
	val meshDisplay: Any get() = this._meshDisplay!!
	/**
	 * - The display object that the slot displays at this time.
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("text");
	 *     slot.display = new yourEngine.TextField();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 插槽此时显示的显示对象。
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("text");
	 *     slot.display = new yourEngine.TextField();
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var display: Any get() = this._display!!
	set(value: Any) {
		if (this._display == value) {
			return
		}

		if (this._displayFrames.lengthSet == 0) {
			this.displayFrameCount = 1
			this._displayIndex = 0
		}

		this.replaceDisplay(value, this._displayIndex)
	}
	/**
	 * - The child armature that the slot displayed at current time.
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     var prevChildArmature = slot.childArmature;
	 *     if (prevChildArmature) {
	 *         prevChildArmature.dispose();
	 *     }
	 *     slot.childArmature = factory.buildArmature("weapon_blabla", "weapon_blabla_project");
	 * </pre>
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 插槽此时显示的子骨架。
	 * 注意，被替换的对象或子骨架并不会被回收，根据语言和引擎的不同，需要额外处理。
	 * @example
	 * <pre>
	 *     var slot = armature.getSlot("weapon");
	 *     var prevChildArmature = slot.childArmature;
	 *     if (prevChildArmature) {
	 *         prevChildArmature.dispose();
	 *     }
	 *     slot.childArmature = factory.buildArmature("weapon_blabla", "weapon_blabla_project");
	 * </pre>
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var childArmature: Armature?
		get() = this._childArmature
		set(value: Armature?) {
			if (this._childArmature == value) {
				return
			}

			this.display = value!!
		}
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
	val parent: Bone get() = this._parent!!

	///**
	// * - Deprecated, please refer to {@link #display}.
	// * deprecated
	// * @language en_US
	// */
	///**
	// * - 已废弃，请参考 {@link #display}。
	// * deprecated
	// * @language zh_CN
	// */
	//fun getDisplay(): Any {
	//	return this._display!!
	//}
	///**
	// * - Deprecated, please refer to {@link #display}.
	// * deprecated
	// * @language en_US
	// */
	///**
	// * - 已废弃，请参考 {@link #display}。
	// * deprecated
	// * @language zh_CN
	// */
	//fun setDisplay(value: Any) {
	//	this.display = value
	//}
}
