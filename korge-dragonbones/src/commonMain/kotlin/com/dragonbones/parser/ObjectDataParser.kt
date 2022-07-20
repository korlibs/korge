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
package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.dragonbones.util.length
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * @private
 */
enum class FrameValueType(val index: kotlin.Int) {
	STEP(0),
	INT(1),
	FLOAT(2),
}

/**
 * @private
 */
@Suppress("UNCHECKED_CAST", "NAME_SHADOWING", "UNUSED_CHANGED_VALUE")
open class ObjectDataParser(pool: BaseObjectPool = BaseObjectPool()) : DataParser(pool) {
	companion object {
		// dynamic tools
		internal fun Any?.getDynamic(key: String): Any? = when (this) {
			null -> null
			is Map<*, *> -> (this as Map<String, Any?>)[key]
			else -> error("Can't getDynamic $this['$key'] (${this::class})")
		}
		internal inline fun Any?.containsDynamic(key: String): Boolean = this.getDynamic(key) != null

        internal fun Any?.asFastArrayList() = (this as List<Any?>).toFastList() as FastArrayList<Any?>

		internal val Any?.dynKeys: List<String> get() = when (this) {
			null -> listOf()
			is Map<*, *> -> keys.map { it.toString() }
			else -> error("Can't get keys of $this (${this::class})")
		}
		internal val Any?.dynList: List<Any?> get() = when (this) {
			null -> listOf()
			is List<*> -> this
			is Iterable<*> -> this.toList()
			else -> error("Not a list $this (${this::class})")
		}
		internal val Any?.doubleArray: DoubleArray get() {
			if (this is DoubleArray) return this
			if (this is DoubleArrayList) return this.toDoubleArray()
			if (this is List<*>) return this.map { (it as Number).toDouble() }.toDoubleArray()
			error("Can't cast '$this' to doubleArray")
		}
		internal val Any?.doubleArrayList: DoubleArrayList get() {
			if (this is DoubleArray) return DoubleArrayList(*this)
			if (this is DoubleArrayList) return this
            if (this is DoubleArrayList) return this.toDoubleList()
			if (this is List<*>) return DoubleArrayList(*this.map { (it as Number).toDouble() }.toDoubleArray())
			error("Can't cast '$this' to doubleArrayList")
		}
		internal val Any?.intArrayList: IntArrayList get() {
			if (this is IntArray) return IntArrayList(*this)
			if (this is IntArrayList) return this
            if (this is DoubleArrayList) return this.toIntArrayList()
			if (this is List<*>) return IntArrayList(*this.map { (it as Number).toInt() }.toIntArray())
			error("Can't '$this' cast to intArrayList")
		}

		fun _getBoolean(rawData: Any?, key: String, defaultValue: Boolean): Boolean {
			val value = rawData.getDynamic(key)

			return when (value) {
				null -> defaultValue
				is Boolean -> value
				is Number -> value.toDouble() != 0.0
				is String -> when (value) {
					"0", "NaN", "", "false", "null", "undefined" -> false
					else -> true
				}
				else -> defaultValue
			}
		}

		fun _getNumber(rawData: Any?, key: String, defaultValue: Double): Double {
			val value = rawData.getDynamic(key) as? Number?
			return if (value != null && value != Double.NaN) {
				value.toDouble()
			} else {
				defaultValue
			}
		}

		fun _getInt(rawData: Any?, key: String, defaultValue: Int): Int {
			val value = rawData.getDynamic(key) as? Number?
			return if (value != null && value != Double.NaN) {
				value.toInt()
			} else {
				defaultValue
			}
		}

		fun _getString(rawData: Any?, key: String, defaultValue: String): String {
			return rawData.getDynamic(key)?.toString() ?: defaultValue
		}

		//private var _objectDataParserInstance = ObjectDataParser()
		///**
		// * - Deprecated, please refer to {@link dragonBones.BaseFactory#parseDragonBonesData()}.
		// * deprecated
		// * @language en_US
		// */
		///**
		// * - 已废弃，请参考 {@link dragonBones.BaseFactory#parseDragonBonesData()}。
		// * deprecated
		// * @language zh_CN
		// */
		//fun getInstance(): ObjectDataParser = ObjectDataParser._objectDataParserInstance

	}

	protected var _rawTextureAtlasIndex: Int = 0
	protected val _rawBones: FastArrayList<BoneData> = FastArrayList()
	protected var _data: DragonBonesData? = null //
	protected var _armature: ArmatureData? = null //
	protected var _bone: BoneData? = null //
	protected var _geometry: GeometryData? = null //
	protected var _slot: SlotData? = null //
	protected var _skin: SkinData? = null //
	protected var _mesh: MeshDisplayData? = null //
	protected var _animation: AnimationData? = null //
	protected var _timeline: TimelineData? = null //
	protected var _rawTextureAtlases: FastArrayList<Any?>? = null

	private var _frameValueType: FrameValueType = FrameValueType.STEP
	private var _defaultColorOffset: Int = -1
	//private var _prevClockwise: Int = 0
	private var _prevClockwise: Double = 0.0
	private var _prevRotation: Double = 0.0
	private var _frameDefaultValue: Double = 0.0
	private var _frameValueScale: Double = 1.0
	private val _helpMatrixA: Matrix = Matrix()
	private val _helpMatrixB: Matrix = Matrix()
	private val _helpTransform: TransformDb = TransformDb()
	private val _helpColorTransform: ColorTransform = ColorTransform()
	private val _helpPoint: Point = Point()
	private val _helpArray: DoubleArrayList = DoubleArrayList()
	private val _intArray: IntArrayList = IntArrayList()
	private val _floatArray: DoubleArrayList = DoubleArrayList()
	//private val _frameIntArray:  DoubleArrayList = DoubleArrayList()
	private val _frameIntArray: IntArrayList = IntArrayList()
	private val _frameFloatArray: DoubleArrayList = DoubleArrayList()
	private val _frameArray: DoubleArrayList = DoubleArrayList()
	private val _timelineArray: DoubleArrayList = DoubleArrayList()
	//private val _colorArray:  DoubleArrayList = DoubleArrayList()
	private val _colorArray: IntArrayList = IntArrayList()
	private val _cacheRawMeshes: FastArrayList<Any> = FastArrayList()
	private val _cacheMeshes: FastArrayList<MeshDisplayData> = FastArrayList()
	private val _actionFrames: FastArrayList<ActionFrame> = FastArrayList()
	private val _weightSlotPose: LinkedHashMap<String, DoubleArrayList> = LinkedHashMap()
	private val _weightBonePoses: LinkedHashMap<String, DoubleArrayList> = LinkedHashMap()
	private val _cacheBones: LinkedHashMap<String, FastArrayList<BoneData>> = LinkedHashMap()
	private val _slotChildActions: LinkedHashMap<String, FastArrayList<ActionData>> = LinkedHashMap()

	private fun _getCurvePoint(
		x1: Double,
		y1: Double,
		x2: Double,
		y2: Double,
		x3: Double,
		y3: Double,
		x4: Double,
		y4: Double,
		t: Double,
		result: Point
	) {
		val l_t = 1.0 - t
		val powA = l_t * l_t
		val powB = t * t
		val kA = l_t * powA
		val kB = 3.0 * t * powA
		val kC = 3.0 * l_t * powB
		val kD = t * powB

		result.xf = (kA * x1 + kB * x2 + kC * x3 + kD * x4).toFloat()
		result.yf = (kA * y1 + kB * y2 + kC * y3 + kD * y4).toFloat()
	}

	private fun _samplingEasingCurve(curve: DoubleArrayList, samples: DoubleArrayList): Boolean {
		val curveCount = curve.size

		if (curveCount % 3 == 1) {
			var stepIndex = -2
			val l = samples.size
			for (i in 0 until samples.size) {
				val t: Double = (i + 1) / (l.toDouble() + 1) // float
				while ((if (stepIndex + 6 < curveCount) curve[stepIndex + 6] else 1.0) < t) { // stepIndex + 3 * 2
					stepIndex += 6
				}

				val isInCurve = stepIndex >= 0 && stepIndex + 6 < curveCount
				val x1 = if (isInCurve) curve[stepIndex] else 0.0
				val y1 = if (isInCurve) curve[stepIndex + 1] else 0.0
				val x2 = curve[stepIndex + 2]
				val y2 = curve[stepIndex + 3]
				val x3 = curve[stepIndex + 4]
				val y3 = curve[stepIndex + 5]
				val x4 = if (isInCurve) curve[stepIndex + 6] else 1.0
				val y4 = if (isInCurve) curve[stepIndex + 7] else 1.0

				var lower = 0.0
				var higher = 1.0
				while (higher - lower > 0.0001) {
					val percentage = (higher + lower) * 0.5
					this._getCurvePoint(x1, y1, x2, y2, x3, y3, x4, y4, percentage, this._helpPoint)
					if (t - this._helpPoint.xf > 0.0) {
						lower = percentage
					} else {
						higher = percentage
					}
				}

				samples[i] = this._helpPoint.yf.toDouble()
			}

			return true
		} else {
			var stepIndex = 0
			val l = samples.size
			for (i in 0 until samples.size) {
				val t = (i + 1) / (l + 1) // float
				while (curve[stepIndex + 6] < t) { // stepIndex + 3 * 2
					stepIndex += 6
				}

				val x1 = curve[stepIndex]
				val y1 = curve[stepIndex + 1]
				val x2 = curve[stepIndex + 2]
				val y2 = curve[stepIndex + 3]
				val x3 = curve[stepIndex + 4]
				val y3 = curve[stepIndex + 5]
				val x4 = curve[stepIndex + 6]
				val y4 = curve[stepIndex + 7]

				var lower = 0.0
				var higher = 1.0
				while (higher - lower > 0.0001) {
					val percentage = (higher + lower) * 0.5
					this._getCurvePoint(x1, y1, x2, y2, x3, y3, x4, y4, percentage, this._helpPoint)
					if (t - this._helpPoint.xf > 0.0) {
						lower = percentage
					} else {
						higher = percentage
					}
				}

				samples[i] = this._helpPoint.yf.toDouble()
			}

			return false
		}
	}

	private fun _parseActionDataInFrame(rawData: Any?, frameStart: Int, bone: BoneData?, slot: SlotData?) {
		if (rawData.containsDynamic(DataParser.EVENT)) {
			this._mergeActionFrame(rawData.getDynamic(DataParser.EVENT)!!, frameStart, ActionType.Frame, bone, slot)
		}

		if (rawData.containsDynamic(DataParser.SOUND)) {
			this._mergeActionFrame(rawData.getDynamic(DataParser.SOUND)!!, frameStart, ActionType.Sound, bone, slot)
		}

		if (rawData.containsDynamic(DataParser.ACTION)) {
			this._mergeActionFrame(rawData.getDynamic(DataParser.ACTION)!!, frameStart, ActionType.Play, bone, slot)
		}

		if (rawData.containsDynamic(DataParser.EVENTS)) {
			this._mergeActionFrame(rawData.getDynamic(DataParser.EVENTS)!!, frameStart, ActionType.Frame, bone, slot)
		}

		if (rawData.containsDynamic(DataParser.ACTIONS)) {
			this._mergeActionFrame(rawData.getDynamic(DataParser.ACTIONS)!!, frameStart, ActionType.Play, bone, slot)
		}
	}

	private fun _mergeActionFrame(rawData: Any?, frameStart: Int, type: ActionType, bone: BoneData?, slot: SlotData?) {
		val actionOffset = this._armature!!.actions.size
		val actions = this._parseActionData(rawData, type, bone, slot)
		var frameIndex = 0
		var frame: ActionFrame? = null

		actions.fastForEach { action ->
			this._armature?.addAction(action, false)
		}

		if (this._actionFrames.size == 0) { // First frame.
			frame = ActionFrame()
			frame.frameStart = 0
			this._actionFrames.add(frame)
			frame = null
		}

		for (eachFrame in this._actionFrames) { // Get same frame.
			if (eachFrame.frameStart == frameStart) {
				frame = eachFrame
				break
			} else if (eachFrame.frameStart > frameStart) {
				break
			}

			frameIndex++
		}

		if (frame == null) { // Create and cache frame.
			frame = ActionFrame()
			frame.frameStart = frameStart
			this._actionFrames.splice(frameIndex, 0, frame)
		}

		for (i in 0 until actions.size) { // Cache action offsets.
			frame.actions.push(actionOffset + i)
		}
	}

	protected fun _parseArmature(rawData: Any?, scale: Double): ArmatureData {
		val armature = pool.armatureData.borrow()
		armature.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		armature.frameRate = ObjectDataParser._getInt(rawData, DataParser.FRAME_RATE, this._data!!.frameRate)
		armature.scale = scale

		if (rawData.containsDynamic(DataParser.TYPE) && rawData.getDynamic(DataParser.TYPE) is String) {
			armature.type = DataParser._getArmatureType(rawData.getDynamic(DataParser.TYPE)?.toString())
		} else {
			armature.type = ArmatureType[ObjectDataParser._getInt(rawData, DataParser.TYPE, ArmatureType.Armature.id)]
		}

		if (armature.frameRate == 0) { // Data error.
			armature.frameRate = 24
		}

		this._armature = armature

		if (rawData.containsDynamic(DataParser.CANVAS)) {
			val rawCanvas = rawData.getDynamic(DataParser.CANVAS)
			val canvas = pool.canvasData.borrow()

			canvas.hasBackground = rawCanvas.containsDynamic(DataParser.COLOR)

			canvas.color = ObjectDataParser._getInt(rawCanvas, DataParser.COLOR, 0)
			canvas.x = (ObjectDataParser._getInt(rawCanvas, DataParser.X, 0) * armature.scale).toInt()
			canvas.y = (ObjectDataParser._getInt(rawCanvas, DataParser.Y, 0) * armature.scale).toInt()
			canvas.width = (ObjectDataParser._getInt(rawCanvas, DataParser.WIDTH, 0) * armature.scale).toInt()
			canvas.height = (ObjectDataParser._getInt(rawCanvas, DataParser.HEIGHT, 0) * armature.scale).toInt()
			armature.canvas = canvas
		}

		if (rawData.containsDynamic(DataParser.AABB)) {
			val rawAABB = rawData.getDynamic(DataParser.AABB)
			armature.aabb.x = ObjectDataParser._getNumber(rawAABB, DataParser.X, 0.0) * armature.scale
			armature.aabb.y = ObjectDataParser._getNumber(rawAABB, DataParser.Y, 0.0) * armature.scale
			armature.aabb.width = ObjectDataParser._getNumber(rawAABB, DataParser.WIDTH, 0.0) * armature.scale
			armature.aabb.height = ObjectDataParser._getNumber(rawAABB, DataParser.HEIGHT, 0.0) * armature.scale
		}

		if (rawData.containsDynamic(DataParser.BONE)) {
			val rawBones = rawData.getDynamic(DataParser.BONE)
			(rawBones as List<Any?>).fastForEach { rawBone ->
				val parentName = ObjectDataParser._getString(rawBone, DataParser.PARENT, "")
				val bone = this._parseBone(rawBone)

				if (parentName.isNotEmpty()) { // Get bone parent.
					val parent = armature.getBone(parentName)
					if (parent != null) {
						bone.parent = parent
					} else { // Cache.
						if (!(this._cacheBones.containsDynamic(parentName))) {
							this._cacheBones[parentName] = FastArrayList()
						}

                        this._cacheBones[parentName]?.add(bone)
					}
				}

				if (this._cacheBones.containsDynamic(bone.name)) {
					for (child in this._cacheBones[bone.name]!!) {
						child.parent = bone
					}

					this._cacheBones.remove(bone.name)
				}

				armature.addBone(bone)
                this._rawBones.add(bone) // Cache raw bones sort.
			}
		}

		if (rawData.containsDynamic(DataParser.IK)) {
			val rawIKS = rawData.getDynamic(DataParser.IK) as List<Map<String, Any?>>
			rawIKS.fastForEach { rawIK ->
				val constraint = this._parseIKConstraint(rawIK)
				if (constraint != null) {
					armature.addConstraint(constraint)
				}
			}
		}

		armature.sortBones()

		if (rawData.containsDynamic(DataParser.SLOT)) {
			var zOrder = 0
			val rawSlots = rawData.getDynamic(DataParser.SLOT) as List<Map<String, Any?>>
			rawSlots.fastForEach { rawSlot ->
				armature.addSlot(this._parseSlot(rawSlot, zOrder++))
			}
		}

		if (rawData.containsDynamic(DataParser.SKIN)) {
			val rawSkins = rawData.getDynamic(DataParser.SKIN) as List<Any?>
			rawSkins.fastForEach { rawSkin ->
				armature.addSkin(this._parseSkin(rawSkin))
			}
		}

		if (rawData.containsDynamic(DataParser.PATH_CONSTRAINT)) {
			val rawPaths = rawData.getDynamic(DataParser.PATH_CONSTRAINT) as List<Any?>
			rawPaths.fastForEach { rawPath ->
				val constraint = this._parsePathConstraint(rawPath)
				if (constraint != null) {
					armature.addConstraint(constraint)
				}
			}
		}

		//for (var i = 0, l = this._cacheRawMeshes.length; i < l; ++i) { // Link mesh.
		for (i in 0 until this._cacheRawMeshes.length) {
			val rawData = this._cacheRawMeshes[i]
			val shareName = ObjectDataParser._getString(rawData, DataParser.SHARE, "")
			if (shareName.isEmpty()) {
				continue
			}

			var skinName = ObjectDataParser._getString(rawData, DataParser.SKIN, DataParser.DEFAULT_NAME)
			if (skinName.isEmpty()) { //
				skinName = DataParser.DEFAULT_NAME
			}

			val shareMesh = armature.getMesh(skinName, "", shareName) // TODO slot;
			if (shareMesh == null) {
				continue // Error.
			}

			val mesh = this._cacheMeshes[i]
			mesh.geometry.shareFrom(shareMesh.geometry)
		}

		if (rawData.containsDynamic(DataParser.ANIMATION)) {
			val rawAnimations = rawData.getDynamic(DataParser.ANIMATION) as List<Any?>
			rawAnimations.fastForEach { rawAnimation ->
				val animation = this._parseAnimation(rawAnimation)
				armature.addAnimation(animation)
			}
		}

		if (rawData.containsDynamic(DataParser.DEFAULT_ACTIONS)) {
			val actions = this._parseActionData(rawData.getDynamic(DataParser.DEFAULT_ACTIONS), ActionType.Play, null, null)
			actions.fastForEach { action ->
				armature.addAction(action, true)

				if (action.type == ActionType.Play) { // Set default animation from default action.
					val animation = armature.getAnimation(action.name)
					if (animation != null) {
						armature.defaultAnimation = animation
					}
				}
			}
		}

		if (rawData.containsDynamic(DataParser.ACTIONS)) {
			val actions = this._parseActionData(rawData.getDynamic(DataParser.ACTIONS), ActionType.Play, null, null)
			actions.fastForEach { action ->
				armature.addAction(action, false)
			}
		}

		// Clear helper.
		this._rawBones.lengthSet = 0
		this._cacheRawMeshes.length = 0
		this._cacheMeshes.length = 0
		this._armature = null

		this._weightSlotPose.clear()
		this._weightBonePoses.clear()
		this._cacheBones.clear()
		this._slotChildActions.clear()

		return armature
	}

	protected fun _parseBone(rawData: Any?): BoneData {
		val isSurface: Boolean

		if (rawData.containsDynamic(DataParser.TYPE) && rawData.getDynamic(DataParser.TYPE) is String) {
			isSurface = DataParser._getBoneTypeIsSurface(rawData.getDynamic(DataParser.TYPE)?.toString())
		} else {
			isSurface = ObjectDataParser._getInt(rawData, DataParser.TYPE, 0) == 1
		}

		if (!isSurface) {
			val scale = this._armature!!.scale
			val bone = pool.boneData.borrow()
			bone.inheritTranslation = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_TRANSLATION, true)
			bone.inheritRotation = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_ROTATION, true)
			bone.inheritScale = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_SCALE, true)
			bone.inheritReflection = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_REFLECTION, true)
			bone.length = ObjectDataParser._getNumber(rawData, DataParser.LENGTH, 0.0) * scale
			bone.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
			bone.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")

			if (rawData.containsDynamic(DataParser.TRANSFORM)) {
				this._parseTransform(rawData.getDynamic(DataParser.TRANSFORM), bone.transform, scale)
			}

			return bone
		} else {

			val surface = pool.surfaceData.borrow()
			surface.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
			surface.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
			surface.segmentX = ObjectDataParser._getInt(rawData, DataParser.SEGMENT_X, 0)
			surface.segmentY = ObjectDataParser._getInt(rawData, DataParser.SEGMENT_Y, 0)
			this._parseGeometry(rawData, surface.geometry)

			return surface
		}
	}

	protected fun _parseIKConstraint(rawData: Any?): ConstraintData? {
		val bone = this._armature?.getBone(ObjectDataParser._getString(rawData, DataParser.BONE, "")) ?: return null
		val target = this._armature?.getBone(ObjectDataParser._getString(rawData, DataParser.TARGET, "")) ?: return null

		val chain = ObjectDataParser._getInt(rawData, DataParser.CHAIN, 0)
		val constraint = pool.iKConstraintData.borrow()
		constraint.scaleEnabled = ObjectDataParser._getBoolean(rawData, DataParser.SCALE, false)
		constraint.bendPositive = ObjectDataParser._getBoolean(rawData, DataParser.BEND_POSITIVE, true)
		constraint.weight = ObjectDataParser._getNumber(rawData, DataParser.WEIGHT, 1.0)
		constraint.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		constraint.type = ConstraintType.IK
		constraint.target = target

		if (chain > 0 && bone.parent != null) {
			constraint.root = bone.parent
			constraint.bone = bone
		} else {
			constraint.root = bone
			constraint.bone = null
		}

		return constraint
	}

	protected fun _parsePathConstraint(rawData: Any?): ConstraintData? {
		val target = this._armature?.getSlot(ObjectDataParser._getString(rawData, DataParser.TARGET, "")) ?: return null
		val defaultSkin = this._armature?.defaultSkin ?: return null
		//TODO
		val targetDisplay = defaultSkin.getDisplay(
			target.name,
			ObjectDataParser._getString(rawData, DataParser.TARGET_DISPLAY, target.name)
		)
		if (targetDisplay == null || !(targetDisplay is PathDisplayData)) {
			return null
		}

		val bones = rawData.getDynamic(DataParser.BONES) as? List<String>?
		if (bones == null || bones.isEmpty()) {
			return null
		}

		val constraint = pool.pathConstraintData.borrow()
		constraint.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		constraint.type = ConstraintType.Path
		constraint.pathSlot = target
		constraint.pathDisplayData = targetDisplay
		constraint.target = target.parent
		constraint.positionMode =
				DataParser._getPositionMode(ObjectDataParser._getString(rawData, DataParser.POSITION_MODE, ""))
		constraint.spacingMode =
				DataParser._getSpacingMode(ObjectDataParser._getString(rawData, DataParser.SPACING_MODE, ""))
		constraint.rotateMode =
				DataParser._getRotateMode(ObjectDataParser._getString(rawData, DataParser.ROTATE_MODE, ""))
		constraint.position = ObjectDataParser._getNumber(rawData, DataParser.POSITION, 0.0)
		constraint.spacing = ObjectDataParser._getNumber(rawData, DataParser.SPACING, 0.0)
		constraint.rotateOffset = ObjectDataParser._getNumber(rawData, DataParser.ROTATE_OFFSET, 0.0)
		constraint.rotateMix = ObjectDataParser._getNumber(rawData, DataParser.ROTATE_MIX, 1.0)
		constraint.translateMix = ObjectDataParser._getNumber(rawData, DataParser.TRANSLATE_MIX, 1.0)
		//
		bones.fastForEach { boneName ->
			val bone = this._armature?.getBone(boneName)
			if (bone != null) {
				constraint.AddBone(bone)

				if (constraint.root == null) {
					constraint.root = bone
				}
			}
		}

		return constraint
	}

	protected fun _parseSlot(rawData: Any?, zOrder: Int): SlotData {
		val slot = pool.slotData.borrow()
		slot.displayIndex = ObjectDataParser._getInt(rawData, DataParser.DISPLAY_INDEX, 0)
		slot.zOrder = zOrder
		slot.zIndex = ObjectDataParser._getInt(rawData, DataParser.Z_INDEX, 0)
		slot.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
		slot.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		slot.parent = this._armature?.getBone(ObjectDataParser._getString(rawData, DataParser.PARENT, "")) //

		if (rawData.containsDynamic(DataParser.BLEND_MODE) && rawData.getDynamic(DataParser.BLEND_MODE) is String) {
			slot.blendMode = DataParser._getBlendMode(rawData.getDynamic(DataParser.BLEND_MODE)?.toString())
		} else {
			slot.blendMode = BlendMode[ObjectDataParser._getInt(rawData, DataParser.BLEND_MODE, BlendMode.Normal.id)]
		}

		if (rawData.containsDynamic(DataParser.COLOR)) {
			slot.color = SlotData.createColor()
			this._parseColorTransform(rawData.getDynamic(DataParser.COLOR) as Map<String, Any?>, slot.color!!)
		} else {
			slot.color = pool.DEFAULT_COLOR
		}

		if (rawData.containsDynamic(DataParser.ACTIONS)) {
			this._slotChildActions[slot.name] =
					this._parseActionData(rawData.getDynamic(DataParser.ACTIONS), ActionType.Play, null, null)
		}

		return slot
	}

	protected fun _parseSkin(rawData: Any?): SkinData {
		val skin = pool.skinData.borrow()
		skin.name = ObjectDataParser._getString(rawData, DataParser.NAME, DataParser.DEFAULT_NAME)

		if (skin.name.isEmpty()) {
			skin.name = DataParser.DEFAULT_NAME
		}

		if (rawData.containsDynamic(DataParser.SLOT)) {
			val rawSlots = rawData.getDynamic(DataParser.SLOT)
			this._skin = skin

			rawSlots.dynList.fastForEach { rawSlot ->
				val slotName = ObjectDataParser._getString(rawSlot, DataParser.NAME, "")
				val slot = this._armature?.getSlot(slotName)

				if (slot != null) {
					this._slot = slot

					if (rawSlot.containsDynamic(DataParser.DISPLAY)) {
						val rawDisplays = rawSlot.getDynamic(DataParser.DISPLAY)
						for (rawDisplay in rawDisplays.dynList) {
							if (rawDisplay != null) {
								skin.addDisplay(slotName, this._parseDisplay(rawDisplay))
							} else {
								skin.addDisplay(slotName, null)
							}
						}
					}

					this._slot = null //
				}
			}

			this._skin = null //
		}

		return skin
	}

	protected fun _parseDisplay(rawData: Any?): DisplayData? {
		val name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		val path = ObjectDataParser._getString(rawData, DataParser.PATH, "")
		var type = DisplayType.Image
		var display: DisplayData? = null

		if (rawData.containsDynamic(DataParser.TYPE) && rawData.getDynamic(DataParser.TYPE) is String) {
			type = DataParser._getDisplayType(rawData.getDynamic(DataParser.TYPE)?.toString())
		} else {
			type = DisplayType[ObjectDataParser._getInt(rawData, DataParser.TYPE, type.id)]
		}

		when (type) {
			DisplayType.Image -> {
				display = pool.imageDisplayData.borrow()
				val imageDisplay = display
				imageDisplay.name = name
				imageDisplay.path = if (path.length > 0) path else name
				this._parsePivot(rawData, imageDisplay)
			}
			DisplayType.Armature -> {
				display = pool.armatureDisplayData.borrow()
				val armatureDisplay = display
				armatureDisplay.name = name
				armatureDisplay.path = if (path.length > 0) path else name
				armatureDisplay.inheritAnimation = true

				if (rawData.containsDynamic(DataParser.ACTIONS)) {
					val actions = this._parseActionData(rawData.getDynamic(DataParser.ACTIONS), ActionType.Play, null, null)
					actions.fastForEach { action ->
						armatureDisplay.addAction(action)
					}
				} else if (this._slot?.name in this._slotChildActions) {
					val displays = this._skin?.getDisplays(this._slot?.name)
					if (if (displays == null) this._slot!!.displayIndex == 0 else this._slot!!.displayIndex == displays.length) {
						this._slotChildActions[this._slot?.name]!!.fastForEach { action ->
							armatureDisplay.addAction(action)
						}

						this._slotChildActions.remove(this._slot?.name)
					}
				}
			}
			DisplayType.Mesh -> {
				val meshDisplay = pool.meshDisplayData.borrow()
				display = meshDisplay
				meshDisplay.geometry.inheritDeform =
						ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_DEFORM, true)
				meshDisplay.name = name
				meshDisplay.path = if (path.isNotEmpty()) path else name

				if (rawData.containsDynamic(DataParser.SHARE)) {
					meshDisplay.geometry.data = this._data
                    this._cacheRawMeshes.add(rawData!!)
                    this._cacheMeshes.add(meshDisplay)
				} else {
					this._parseMesh(rawData, meshDisplay)
				}
			}
			DisplayType.BoundingBox -> {
				val boundingBox = this._parseBoundingBox(rawData)
				if (boundingBox != null) {
					val boundingBoxDisplay = pool.boundingBoxDisplayData.borrow()
					display = boundingBoxDisplay
					boundingBoxDisplay.name = name
					boundingBoxDisplay.path = if (path.isNotEmpty()) path else name
					boundingBoxDisplay.boundingBox = boundingBox
				}
			}
			DisplayType.Path -> {
				val rawCurveLengths = rawData.getDynamic(DataParser.LENGTHS).doubleArray
				val pathDisplay = pool.pathDisplayData.borrow()
				display = pathDisplay
				pathDisplay.closed = ObjectDataParser._getBoolean(rawData, DataParser.CLOSED, false)
				pathDisplay.constantSpeed = ObjectDataParser._getBoolean(rawData, DataParser.CONSTANT_SPEED, false)
				pathDisplay.name = name
				pathDisplay.path = if (path.isNotEmpty()) path else name
				pathDisplay.curveLengths = DoubleArray(rawCurveLengths.size)

				//for (var i = 0, l = rawCurveLengths.length; i < l; ++i) {
				for (i in 0 until rawCurveLengths.size) {
					pathDisplay.curveLengths[i] = rawCurveLengths[i]
				}

				this._parsePath(rawData, pathDisplay)
			}
			else -> {
			}
		}

		if (display != null && rawData.containsDynamic(DataParser.TRANSFORM)) {
			this._parseTransform(rawData.getDynamic(DataParser.TRANSFORM), display.transform, this._armature!!.scale)
		}

		return display
	}

	protected fun _parsePath(rawData: Any?, display: PathDisplayData) {
		this._parseGeometry(rawData, display.geometry)
	}

	protected fun _parsePivot(rawData: Any?, display: ImageDisplayData) {
		if (rawData.containsDynamic(DataParser.PIVOT)) {
			val rawPivot = rawData.getDynamic(DataParser.PIVOT)
			display.pivot.xf = ObjectDataParser._getNumber(rawPivot, DataParser.X, 0.0).toFloat()
			display.pivot.yf = ObjectDataParser._getNumber(rawPivot, DataParser.Y, 0.0).toFloat()
		} else {
			display.pivot.xf = 0.5f
			display.pivot.yf = 0.5f
		}
	}

	protected fun _parseMesh(rawData: Any?, mesh: MeshDisplayData) {
		this._parseGeometry(rawData, mesh.geometry)

		if (rawData.containsDynamic(DataParser.WEIGHTS)) { // Cache pose data.
			val rawSlotPose = rawData.getDynamic(DataParser.SLOT_POSE).doubleArrayList
			val rawBonePoses = rawData.getDynamic(DataParser.BONE_POSE).doubleArrayList
			val meshName = "" + this._skin?.name + "_" + this._slot?.name + "_" + mesh.name
			this._weightSlotPose[meshName] = rawSlotPose
			this._weightBonePoses[meshName] = rawBonePoses
		}
	}

	protected fun _parseBoundingBox(rawData: Any?): BoundingBoxData? {
		var boundingBox: BoundingBoxData? = null
		var type = BoundingBoxType.Rectangle

		if (rawData.containsDynamic(DataParser.SUB_TYPE) && rawData.getDynamic(DataParser.SUB_TYPE) is String) {
			type = DataParser._getBoundingBoxType(rawData.getDynamic(DataParser.SUB_TYPE)?.toString())
		} else {
			type = BoundingBoxType[ObjectDataParser._getInt(rawData, DataParser.SUB_TYPE, type.id)]
		}

		when (type) {
			BoundingBoxType.Rectangle -> {
				boundingBox = pool.rectangleBoundingBoxData.borrow()
			}

			BoundingBoxType.Ellipse -> {
				boundingBox = pool.ellipseBoundingBoxData.borrow()
			}

			BoundingBoxType.Polygon -> {
				boundingBox = this._parsePolygonBoundingBox(rawData)
			}
			else -> {
			}
		}

		if (boundingBox != null) {
			boundingBox.color = ObjectDataParser._getNumber(rawData, DataParser.COLOR, 0x000000.toDouble()).toInt()
			if (boundingBox.type == BoundingBoxType.Rectangle || boundingBox.type == BoundingBoxType.Ellipse) {
				boundingBox.width = ObjectDataParser._getNumber(rawData, DataParser.WIDTH, 0.0)
				boundingBox.height = ObjectDataParser._getNumber(rawData, DataParser.HEIGHT, 0.0)
			}
		}

		return boundingBox
	}

	protected fun _parsePolygonBoundingBox(rawData: Any?): PolygonBoundingBoxData {
		val polygonBoundingBox = pool.polygonBoundingBoxData.borrow()

		if (rawData.containsDynamic(DataParser.VERTICES)) {
			val scale = this._armature!!.scale
			val rawVertices = rawData.getDynamic(DataParser.VERTICES).doubleArray
			polygonBoundingBox.vertices = DoubleArray(rawVertices.size)
			val vertices = polygonBoundingBox.vertices

			//for (var i = 0, l = rawVertices.length; i < l; i += 2) {
			for (i in 0 until rawVertices.size step 2) {
				val x = rawVertices[i] * scale
				val y = rawVertices[i + 1] * scale
				vertices[i] = x
				vertices[i + 1] = y

				// AABB.
				if (i == 0) {
					polygonBoundingBox.x = x
					polygonBoundingBox.y = y
					polygonBoundingBox.width = x
					polygonBoundingBox.height = y
				} else {
					if (x < polygonBoundingBox.x) {
						polygonBoundingBox.x = x
					} else if (x > polygonBoundingBox.width) {
						polygonBoundingBox.width = x
					}

					if (y < polygonBoundingBox.y) {
						polygonBoundingBox.y = y
					} else if (y > polygonBoundingBox.height) {
						polygonBoundingBox.height = y
					}
				}
			}

			polygonBoundingBox.width -= polygonBoundingBox.x
			polygonBoundingBox.height -= polygonBoundingBox.y
		} else {
			Console.warn("Data error.\n Please reexport DragonBones Data to fixed the bug.")
		}

		return polygonBoundingBox
	}

	private fun findGeometryInTimeline(timelineName: String): GeometryData? {
		this._armature!!.skins.fastKeyForEach { skinName ->
			val skin = this._armature!!.skins.getNull(skinName)
			skin!!.displays.fastKeyForEach { slontName ->
				val displays = skin.displays.getNull(slontName)!!
				displays.fastForEach { display ->
					if (display != null && display.name == timelineName) {
						return (display as MeshDisplayData).geometry
					}
				}
			}
		}
		return null
	}

	protected open fun _parseAnimation(rawData: Any?): AnimationData {
		val animation = pool.animationData.borrow()
		animation.blendType = DataParser._getAnimationBlendType(ObjectDataParser._getString(rawData, DataParser.BLEND_TYPE, ""))
		animation.frameCount = ObjectDataParser._getInt(rawData, DataParser.DURATION, 0)
		animation.playTimes = ObjectDataParser._getInt(rawData, DataParser.PLAY_TIMES, 1)
		animation.duration = animation.frameCount.toDouble() / this._armature!!.frameRate.toDouble() // float
		animation.fadeInTime = ObjectDataParser._getNumber(rawData, DataParser.FADE_IN_TIME, 0.0)
		animation.scale = ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0)
		animation.name = ObjectDataParser._getString(rawData, DataParser.NAME, DataParser.DEFAULT_NAME)

		if (animation.name.length == 0) {
			animation.name = DataParser.DEFAULT_NAME
		}

		animation.frameIntOffset = this._frameIntArray.length
		animation.frameFloatOffset = this._frameFloatArray.length
		animation.frameOffset = this._frameArray.length
		this._animation = animation

		if (rawData.containsDynamic(DataParser.FRAME)) {
			val rawFrames = rawData.getDynamic(DataParser.FRAME) as List<Any?>
			val keyFrameCount = rawFrames.size

			if (keyFrameCount > 0) {
				//for (var i = 0, frameStart = 0; i < keyFrameCount; ++i) {
				var frameStart = 0
				for (i in 0 until keyFrameCount) {
					val rawFrame = rawFrames[i]
					this._parseActionDataInFrame(rawFrame, frameStart, null, null)
					frameStart += ObjectDataParser._getInt(rawFrame, DataParser.DURATION, 1)
				}
			}
		}

		if (rawData.containsDynamic(DataParser.Z_ORDER)) {
			this._animation!!.zOrderTimeline = this._parseTimeline(
				rawData.getDynamic(DataParser.Z_ORDER), null, DataParser.FRAME, TimelineType.ZOrder,
				FrameValueType.STEP, 0,
				this::_parseZOrderFrame
			)
		}

		if (rawData.containsDynamic(DataParser.BONE)) {
			val rawTimelines = rawData.getDynamic(DataParser.BONE) as List<Any?>
			rawTimelines.fastForEach { rawTimeline ->
				this._parseBoneTimeline(rawTimeline)
			}
		}

		if (rawData.containsDynamic(DataParser.SLOT)) {
			val rawTimelines = rawData.getDynamic(DataParser.SLOT) as List<Any?>
			rawTimelines.fastForEach { rawTimeline ->
				this._parseSlotTimeline(rawTimeline)
			}
		}

		if (rawData.containsDynamic(DataParser.FFD)) {
			val rawTimelines = rawData.getDynamic(DataParser.FFD) as List<Any?>
			rawTimelines.fastForEach { rawTimeline ->
				var skinName = ObjectDataParser._getString(rawTimeline, DataParser.SKIN, DataParser.DEFAULT_NAME)
				val slotName = ObjectDataParser._getString(rawTimeline, DataParser.SLOT, "")
				val displayName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")

				if (skinName.isEmpty()) { //
					skinName = DataParser.DEFAULT_NAME
				}

				this._slot = this._armature?.getSlot(slotName)
				this._mesh = this._armature?.getMesh(skinName, slotName, displayName)
				if (this._slot == null || this._mesh == null) {
					return@fastForEach
				}

				val timeline = this._parseTimeline(
					rawTimeline, null, DataParser.FRAME, TimelineType.SlotDeform,
					FrameValueType.FLOAT, 0,
					this::_parseSlotDeformFrame
				)

				if (timeline != null) {
					this._animation?.addSlotTimeline(slotName, timeline)
				}

				this._slot = null //
				this._mesh = null //
			}
		}

		if (rawData.containsDynamic(DataParser.IK)) {
			val rawTimelines = rawData.getDynamic(DataParser.IK) as List<Any?>
			rawTimelines.fastForEach { rawTimeline ->
				val constraintName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")
				@Suppress("UNUSED_VARIABLE")
				val constraint = this._armature!!.getConstraint(constraintName) ?: return@fastForEach

				val timeline = this._parseTimeline(
					rawTimeline, null, DataParser.FRAME, TimelineType.IKConstraint,
					FrameValueType.INT, 2,
					this::_parseIKConstraintFrame
				)

				if (timeline != null) {
					this._animation?.addConstraintTimeline(constraintName, timeline)
				}
			}
		}

		if (this._actionFrames.length > 0) {
			this._animation!!.actionTimeline = this._parseTimeline(
				null, this._actionFrames.asFastArrayList(), "", TimelineType.Action,
				FrameValueType.STEP, 0,
				this::_parseActionFrameRaw
			)
			this._actionFrames.length = 0
		}

		if (rawData.containsDynamic(DataParser.TIMELINE)) {
			val rawTimelines = rawData.getDynamic(DataParser.TIMELINE)
			rawTimelines.dynList.fastForEach { rawTimeline ->
				val timelineType =
					TimelineType[ObjectDataParser._getInt(rawTimeline, DataParser.TYPE, TimelineType.Action.id)]
				val timelineName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")
				var timeline: TimelineData? = null

				when (timelineType) {
					TimelineType.Action -> {
						// TODO
					}

					TimelineType.SlotDisplay, // TODO
					TimelineType.SlotZIndex,
					TimelineType.BoneAlpha,
					TimelineType.SlotAlpha,
					TimelineType.AnimationProgress,
					TimelineType.AnimationWeight -> {
						if (
							timelineType == TimelineType.SlotDisplay
						) {
							this._frameValueType = FrameValueType.STEP
							this._frameValueScale = 1.0
						} else {
							this._frameValueType = FrameValueType.INT

							if (timelineType == TimelineType.SlotZIndex) {
								this._frameValueScale = 1.0
							} else if (
								timelineType == TimelineType.AnimationProgress ||
								timelineType == TimelineType.AnimationWeight
							) {
								this._frameValueScale = 10000.0
							} else {
								this._frameValueScale = 100.0
							}
						}

						if (
							timelineType == TimelineType.BoneAlpha ||
							timelineType == TimelineType.SlotAlpha ||
							timelineType == TimelineType.AnimationWeight
						) {
							this._frameDefaultValue = 1.0
						} else {
							this._frameDefaultValue = 0.0
						}

						if (timelineType == TimelineType.AnimationProgress && animation.blendType != AnimationBlendType.None) {
							timeline = pool.animationTimelineData.borrow()
							val animaitonTimeline = timeline
							animaitonTimeline.x = ObjectDataParser._getNumber(rawTimeline, DataParser.X, 0.0)
							animaitonTimeline.y = ObjectDataParser._getNumber(rawTimeline, DataParser.Y, 0.0)
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							this._frameValueType, 1,
							this::_parseSingleValueFrame, timeline
						)
					}

					TimelineType.BoneTranslate,
					TimelineType.BoneRotate,
					TimelineType.BoneScale,
					TimelineType.IKConstraint,
					TimelineType.AnimationParameter -> {
						if (
							timelineType == TimelineType.IKConstraint ||
							timelineType == TimelineType.AnimationParameter
						) {
							this._frameValueType = FrameValueType.INT

							if (timelineType == TimelineType.AnimationParameter) {
								this._frameValueScale = 10000.0
							} else {
								this._frameValueScale = 100.0
							}
						} else {
							if (timelineType == TimelineType.BoneRotate) {
								this._frameValueScale = TransformDb.DEG_RAD.toDouble()
							} else {
								this._frameValueScale = 1.0
							}

							this._frameValueType = FrameValueType.FLOAT
						}

						if (
							timelineType == TimelineType.BoneScale ||
							timelineType == TimelineType.IKConstraint
						) {
							this._frameDefaultValue = 1.0
						} else {
							this._frameDefaultValue = 0.0
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							this._frameValueType, 2,
							this::_parseDoubleValueFrame
						)
					}

					TimelineType.ZOrder -> {
						// TODO
					}

					TimelineType.Surface -> {
						val surface = this._armature?.getBone(timelineName) ?: return@fastForEach

						this._geometry = surface.geometry
						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.FLOAT, 0,
							this::_parseDeformFrame
						)

						this._geometry = null //
					}

					TimelineType.SlotDeform -> {
						this._geometry = findGeometryInTimeline(timelineName)

						if (this._geometry == null) {
							return@fastForEach
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.FLOAT, 0,
							this::_parseDeformFrame
						)

						this._geometry = null //
					}

					TimelineType.SlotColor -> {
						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.INT, 1,
							this::_parseSlotColorFrame
						)
					}
					else -> {
					}
				}

				if (timeline != null) {
					when (timelineType) {
						TimelineType.Action -> {
							// TODO
						}

						TimelineType.ZOrder -> {
							// TODO
						}

						TimelineType.BoneTranslate,
						TimelineType.BoneRotate,
						TimelineType.BoneScale,
						TimelineType.Surface,
						TimelineType.BoneAlpha -> {
							this._animation?.addBoneTimeline(timelineName, timeline)
						}

						TimelineType.SlotDisplay,
						TimelineType.SlotColor,
						TimelineType.SlotDeform,
						TimelineType.SlotZIndex,
						TimelineType.SlotAlpha -> {
							this._animation?.addSlotTimeline(timelineName, timeline)
						}

						TimelineType.IKConstraint -> {
							this._animation?.addConstraintTimeline(timelineName, timeline)
						}

						TimelineType.AnimationProgress,
						TimelineType.AnimationWeight,
						TimelineType.AnimationParameter -> {
							this._animation?.addAnimationTimeline(timelineName, timeline)
						}
						else -> {
						}
					}
				}
			}
		}

		this._animation = null //

		return animation
	}

	protected fun _parseTimeline(
		rawData: Any?, rawFrames: FastArrayList<Any?>?, framesKey: String,
		timelineType: TimelineType, frameValueType: FrameValueType, frameValueCount: Int,
		frameParser: (rawData: Any?, frameStart: Int, frameCount: Int) -> Int, timeline: TimelineData? = null
	): TimelineData? {
		var timeline = timeline
		val frameParser = frameParser
		var rawFrames = rawFrames
		if (rawData != null && framesKey.isNotEmpty() && rawData.containsDynamic(framesKey)) {
			rawFrames = (rawData.getDynamic(framesKey) as List<Any?>?)?.toFastList() as? FastArrayList<Any?>
		}

		if (rawFrames == null) {
			return null
		}

		val keyFrameCount = rawFrames.length
		if (keyFrameCount == 0) {
			return null
		}

		val frameIntArrayLength = this._frameIntArray.length
		val frameFloatArrayLength = this._frameFloatArray.length
		val timelineOffset = this._timelineArray.length
		if (timeline == null) {
			timeline = pool.timelineData.borrow()
		}

		timeline.type = timelineType
		timeline.offset = timelineOffset
		this._frameValueType = frameValueType
		this._timeline = timeline
		this._timelineArray.length += 1 + 1 + 1 + 1 + 1 + keyFrameCount

		if (rawData != null) {
			this._timelineArray[timelineOffset + BinaryOffset.TimelineScale] =
					round(ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0) * 100)
			this._timelineArray[timelineOffset + BinaryOffset.TimelineOffset] =
					round(ObjectDataParser._getNumber(rawData, DataParser.OFFSET, 0.0) * 100)
		} else {
			this._timelineArray[timelineOffset + BinaryOffset.TimelineScale] = 100.0
			this._timelineArray[timelineOffset + BinaryOffset.TimelineOffset] = 0.0
		}

		this._timelineArray[timelineOffset + BinaryOffset.TimelineKeyFrameCount] = keyFrameCount.toDouble()
		this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueCount] = frameValueCount.toDouble()

		when (this._frameValueType) {
			FrameValueType.STEP -> {
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] = 0.0
			}

			FrameValueType.INT -> {
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] =
						(frameIntArrayLength - this._animation!!.frameIntOffset).toDouble()
			}

			FrameValueType.FLOAT -> {
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] =
						(frameFloatArrayLength - this._animation!!.frameFloatOffset).toDouble()
			}
		}

		if (keyFrameCount == 1) { // Only one frame.
			timeline.frameIndicesOffset = -1
			this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameOffset + 0] =
					(frameParser(rawFrames[0], 0, 0) - this._animation!!.frameOffset).toDouble()
		} else {
			val totalFrameCount = this._animation!!.frameCount + 1 // One more frame than animation.
			val frameIndices = this._data!!.frameIndices
			val frameIndicesOffset = frameIndices.length
			frameIndices.length += totalFrameCount
			timeline.frameIndicesOffset = frameIndicesOffset

			//for (var i = 0, iK = 0, frameStart = 0, frameCount = 0;i < totalFrameCount; ++i) {
			var iK = 0
			var frameStart = 0
			var frameCount = 0
			for (i in 0 until totalFrameCount) {
				if (frameStart + frameCount <= i && iK < keyFrameCount) {
					val rawFrame = rawFrames[iK]
					frameStart = i // frame.frameStart;

					if (iK == keyFrameCount - 1) {
						frameCount = this._animation!!.frameCount - frameStart
					} else {
						if (rawFrame is ActionFrame) {
							frameCount = this._actionFrames[iK + 1].frameStart - frameStart
						} else {
							frameCount = ObjectDataParser._getNumber(rawFrame, DataParser.DURATION, 1.0).toInt()
						}
					}

					this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameOffset + iK] =
							(frameParser(rawFrame, frameStart, frameCount) - this._animation!!.frameOffset).toDouble()
					iK++
				}

				frameIndices[frameIndicesOffset + i] = iK - 1
			}
		}

		this._timeline = null //

		return timeline
	}

	protected fun _parseBoneTimeline(rawData: Any?) {
		val bone = this._armature?.getBone(ObjectDataParser._getString(rawData, DataParser.NAME, "")) ?: return

		this._bone = bone
		this._slot = this._armature?.getSlot(this._bone?.name)

		if (rawData.containsDynamic(DataParser.TRANSLATE_FRAME)) {
			this._frameDefaultValue = 0.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.TRANSLATE_FRAME, TimelineType.BoneTranslate,
				FrameValueType.FLOAT, 2,
				this::_parseDoubleValueFrame
			)

			if (timeline != null) {
				this._animation?.addBoneTimeline(bone.name, timeline)
			}
		}

		if (rawData.containsDynamic(DataParser.ROTATE_FRAME)) {
			this._frameDefaultValue = 0.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.ROTATE_FRAME, TimelineType.BoneRotate,
				FrameValueType.FLOAT, 2,
				this::_parseBoneRotateFrame
			)

			if (timeline != null) {
				this._animation?.addBoneTimeline(bone.name, timeline)
			}
		}

		if (rawData.containsDynamic(DataParser.SCALE_FRAME)) {
			this._frameDefaultValue = 1.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.SCALE_FRAME, TimelineType.BoneScale,
				FrameValueType.FLOAT, 2,
				this::_parseBoneScaleFrame
			)

			if (timeline != null) {
				this._animation?.addBoneTimeline(bone.name, timeline)
			}
		}

		if (rawData.containsDynamic(DataParser.FRAME)) {
			val timeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.BoneAll,
				FrameValueType.FLOAT, 6,
				this::_parseBoneAllFrame
			)

			if (timeline != null) {
				this._animation?.addBoneTimeline(bone.name, timeline)
			}
		}

		this._bone = null //
		this._slot = null //
	}

	protected fun _parseSlotTimeline(rawData: Any?) {
		val slot = this._armature?.getSlot(ObjectDataParser._getString(rawData, DataParser.NAME, "")) ?: return

		val displayTimeline: TimelineData?
		val colorTimeline: TimelineData?
		this._slot = slot

		if (rawData.containsDynamic(DataParser.DISPLAY_FRAME)) {
			displayTimeline = this._parseTimeline(
				rawData, null, DataParser.DISPLAY_FRAME, TimelineType.SlotDisplay,
				FrameValueType.STEP, 0,
				this::_parseSlotDisplayFrame
			)
		} else {
			displayTimeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.SlotDisplay,
				FrameValueType.STEP, 0,
				this::_parseSlotDisplayFrame
			)
		}

		if (rawData.containsDynamic(DataParser.COLOR_FRAME)) {
			colorTimeline = this._parseTimeline(
				rawData, null, DataParser.COLOR_FRAME, TimelineType.SlotColor,
				FrameValueType.INT, 1,
				this::_parseSlotColorFrame
			)
		} else {
			colorTimeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.SlotColor,
				FrameValueType.INT, 1,
				this::_parseSlotColorFrame
			)
		}

		if (displayTimeline != null) {
			this._animation?.addSlotTimeline(slot.name, displayTimeline)
		}

		if (colorTimeline != null) {
			this._animation?.addSlotTimeline(slot.name, colorTimeline)
		}

		this._slot = null //
	}

	@Suppress("UNUSED_PARAMETER")
	protected fun _parseFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._frameArray.length
		this._frameArray.length += 1
		this._frameArray[frameOffset + BinaryOffset.FramePosition] = frameStart.toDouble()

		return frameOffset
	}

	protected fun _parseTweenFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)

		if (frameCount > 0) {
			if (rawData.containsDynamic(DataParser.CURVE)) {
				val sampleCount = frameCount + 1
				this._helpArray.length = sampleCount
				val isOmited = this._samplingEasingCurve(rawData.getDynamic(DataParser.CURVE).doubleArrayList, this._helpArray)

				this._frameArray.length += 1 + 1 + this._helpArray.length
				this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.Curve.id.toDouble()
				this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] =
						(if (isOmited) sampleCount else -sampleCount).toDouble()
				//for (var i = 0; i < sampleCount; ++i) {
				for (i in 0 until sampleCount) {
					this._frameArray[frameOffset + BinaryOffset.FrameCurveSamples + i] =
							round(this._helpArray[i] * 10000.0)
				}
			} else {
				val noTween = -2.0
				var tweenEasing = noTween
				if (rawData.containsDynamic(DataParser.TWEEN_EASING)) {
					tweenEasing = ObjectDataParser._getNumber(rawData, DataParser.TWEEN_EASING, noTween)
				}

				if (tweenEasing == noTween) {
					this._frameArray.length += 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.None.id.toDouble()
				} else if (tweenEasing == 0.0) {
					this._frameArray.length += 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.Line.id.toDouble()
				} else if (tweenEasing < 0.0) {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.QuadIn.id.toDouble()
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] =
							round(-tweenEasing * 100.0)
				} else if (tweenEasing <= 1.0) {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.QuadOut.id.toDouble()
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] =
							round(tweenEasing * 100.0)
				} else {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] =
							TweenType.QuadInOut.id.toDouble()
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] =
							round(tweenEasing * 100.0 - 100.0)
				}
			}
		} else {
			this._frameArray.length += 1
			this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.None.id.toDouble()
		}

		return frameOffset
	}

	protected fun _parseSingleValueFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		var frameOffset = 0
		when (this._frameValueType) {
			FrameValueType.STEP -> {
				frameOffset = this._parseFrame(rawData, frameStart, frameCount)
				this._frameArray.length += 1
				this._frameArray[frameOffset + 1] =
						ObjectDataParser._getNumber(rawData, DataParser.VALUE, this._frameDefaultValue)
			}

			FrameValueType.INT -> {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
				val frameValueOffset = this._frameIntArray.length
				this._frameIntArray.length += 1
				this._frameIntArray[frameValueOffset] = round(
					ObjectDataParser._getNumber(
						rawData,
						DataParser.VALUE,
						this._frameDefaultValue
					) * this._frameValueScale
				).toInt()
			}

			FrameValueType.FLOAT -> {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
				val frameValueOffset = this._frameFloatArray.length
				this._frameFloatArray.length += 1
				this._frameFloatArray[frameValueOffset] = ObjectDataParser._getNumber(
					rawData,
					DataParser.VALUE,
					this._frameDefaultValue
				) * this._frameValueScale
			}
		}

		return frameOffset
	}

	protected fun _parseDoubleValueFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		var frameOffset = 0
		when (this._frameValueType) {
			FrameValueType.STEP -> {
				frameOffset = this._parseFrame(rawData, frameStart, frameCount)
				this._frameArray.length += 2
				this._frameArray[frameOffset + 1] =
						ObjectDataParser._getNumber(rawData, DataParser.X, this._frameDefaultValue)
				this._frameArray[frameOffset + 2] =
						ObjectDataParser._getNumber(rawData, DataParser.Y, this._frameDefaultValue)
			}

			FrameValueType.INT -> {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
				val frameValueOffset = this._frameIntArray.length
				this._frameIntArray.length += 2
				this._frameIntArray[frameValueOffset] = round(
					ObjectDataParser._getNumber(
						rawData,
						DataParser.X,
						this._frameDefaultValue
					) * this._frameValueScale
				).toInt()
				this._frameIntArray[frameValueOffset + 1] = round(
					ObjectDataParser._getNumber(
						rawData,
						DataParser.Y,
						this._frameDefaultValue
					) * this._frameValueScale
				).toInt()
			}

			FrameValueType.FLOAT -> {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
				val frameValueOffset = this._frameFloatArray.length
				this._frameFloatArray.length += 2
				this._frameFloatArray[frameValueOffset] = ObjectDataParser._getNumber(
					rawData,
					DataParser.X,
					this._frameDefaultValue
				) * this._frameValueScale
				this._frameFloatArray[frameValueOffset + 1] = ObjectDataParser._getNumber(
					rawData,
					DataParser.Y,
					this._frameDefaultValue
				) * this._frameValueScale
			}
		}

		return frameOffset
	}

	protected fun _parseActionFrameRaw(frame: Any?, frameStart: Int, frameCount: Int): Int =
		_parseActionFrame(frame as ActionFrame, frameStart, frameCount)

	protected fun _parseActionFrame(frame: ActionFrame, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._frameArray.length
		val actionCount = frame.actions.length
		this._frameArray.length += 1 + 1 + actionCount
		this._frameArray[frameOffset + BinaryOffset.FramePosition] = frameStart.toDouble()
		this._frameArray[frameOffset + BinaryOffset.FramePosition + 1] = actionCount.toDouble() // Action count.

		//for (var i = 0; i < actionCount; ++i) { // Action offsets.
		for (i in 0 until actionCount) { // Action offsets.
			this._frameArray[frameOffset + BinaryOffset.FramePosition + 2 + i] = frame.actions[i].toDouble()
		}

		return frameOffset
	}

	protected fun _parseZOrderFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val rawData = rawData as Map<String, Any?>
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)

		if (rawData.containsDynamic(DataParser.Z_ORDER)) {
			val rawZOrder = rawData[DataParser.Z_ORDER] .doubleArray
			if (rawZOrder.size > 0) {
				val slotCount = this._armature!!.sortedSlots.length
				val unchanged = IntArray(slotCount - rawZOrder.size / 2)
				val zOrders = IntArray(slotCount)

				//for (var i = 0; i < unchanged.length; ++i) {
				for (i in 0 until unchanged.size) {
					unchanged[i] = 0
				}

				//for (var i = 0; i < slotCount; ++i) {
				for (i in 0 until slotCount) {
					zOrders[i] = -1
				}

				var originalIndex = 0
				var unchangedIndex = 0
				//for (var i = 0, l = rawZOrder.length; i < l; i += 2) {
				for (i in 0 until rawZOrder.size step 2) {
					val slotIndex = rawZOrder[i].toInt()
					val zOrderOffset = rawZOrder[i + 1].toInt()

					while (originalIndex != slotIndex) {
						unchanged[unchangedIndex++] = originalIndex++
					}

					val index = originalIndex + zOrderOffset
                    if (index < 0 || index >= zOrders.size) {
                        originalIndex++
                        continue
                    }
					zOrders[index] = originalIndex++
				}

				while (originalIndex < slotCount) {
					unchanged[unchangedIndex++] = originalIndex++
				}

				this._frameArray.length += 1 + slotCount
				this._frameArray[frameOffset + 1] = slotCount.toDouble()

				var i = slotCount
				while (i-- > 0) {
					if (zOrders[i] == -1) {
                        this._frameArray[frameOffset + 2 + i] = if (unchangedIndex > 0) unchanged[--unchangedIndex].toDouble() else 0.0
					} else {
						this._frameArray[frameOffset + 2 + i] = zOrders[i].toDouble()
					}
				}

				return frameOffset
			}
		}

		this._frameArray.length += 1
		this._frameArray[frameOffset + 1] = 0.0

		return frameOffset
	}

	protected fun _parseBoneAllFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		this._helpTransform.identity()
		if (rawData.containsDynamic(DataParser.TRANSFORM)) {
			this._parseTransform(rawData.getDynamic(DataParser.TRANSFORM), this._helpTransform, 1.0)
		}

		// Modify rotation.
		var rotation = this._helpTransform.rotation
		if (frameStart != 0) {
			if (this._prevClockwise == 0.0) {
				rotation = (this._prevRotation + TransformDb.normalizeRadian(rotation - this._prevRotation)).toFloat()
			} else {
				if (if (this._prevClockwise > 0) rotation >= this._prevRotation else rotation <= this._prevRotation) {
					this._prevClockwise =
							if (this._prevClockwise > 0) this._prevClockwise - 1 else this._prevClockwise + 1
				}

				rotation = (this._prevRotation + rotation - this._prevRotation + TransformDb.PI_D * this._prevClockwise).toFloat()
			}
		}

		this._prevClockwise = ObjectDataParser._getNumber(rawData, DataParser.TWEEN_ROTATE, 0.0)
		this._prevRotation = rotation.toDouble()
		//
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 6
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.xf.toDouble()
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.yf.toDouble()
		this._frameFloatArray[frameFloatOffset++] = rotation.toDouble()
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.skew.toDouble()
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.scaleX.toDouble()
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.scaleY.toDouble()
		this._parseActionDataInFrame(rawData, frameStart, this._bone, this._slot)

		return frameOffset
	}

	protected fun _parseBoneTranslateFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.X, 0.0)
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.Y, 0.0)

		return frameOffset
	}

	protected fun _parseBoneRotateFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		// Modify rotation.
		var rotation = ObjectDataParser._getNumber(rawData, DataParser.ROTATE, 0.0) * TransformDb.DEG_RAD

		if (frameStart != 0) {
			if (this._prevClockwise == 0.0) {
				rotation = this._prevRotation + TransformDb.normalizeRadian(rotation - this._prevRotation)
			} else {
				if (if (this._prevClockwise > 0) rotation >= this._prevRotation else rotation <= this._prevRotation) {
					this._prevClockwise =
							if (this._prevClockwise > 0) this._prevClockwise - 1 else this._prevClockwise + 1
				}

				rotation = this._prevRotation + rotation - this._prevRotation + TransformDb.PI_D * this._prevClockwise
			}
		}

		this._prevClockwise = ObjectDataParser._getNumber(rawData, DataParser.CLOCK_WISE, 0.0)
		this._prevRotation = rotation
		//
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = rotation
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.SKEW, 0.0) *
				TransformDb.DEG_RAD

		return frameOffset
	}

	protected fun _parseBoneScaleFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.X, 1.0)
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.Y, 1.0)

		return frameOffset
	}

	protected fun _parseSlotDisplayFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)
		this._frameArray.length += 1

		if (rawData.containsDynamic(DataParser.VALUE)) {
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.VALUE, 0.0)
		} else {
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.DISPLAY_INDEX, 0.0)
		}

		this._parseActionDataInFrame(rawData, frameStart, this._slot?.parent, this._slot)

		return frameOffset
	}

	protected fun _parseSlotColorFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var colorOffset = -1

		if (rawData.containsDynamic(DataParser.VALUE) || rawData.containsDynamic(DataParser.COLOR)) {
			val rawColor = rawData.getDynamic(DataParser.VALUE) ?: rawData.getDynamic(DataParser.COLOR)
			// @TODO: Kotlin-JS: Caused by: java.lang.IllegalStateException: Value at LOOP_RANGE_ITERATOR_RESOLVED_CALL must not be null for BINARY_WITH_TYPE
			//for (k in (rawColor as List<Any?>)) { // Detects the presence of color.
			//for (let k in rawColor) { // Detects the presence of color.
			for (k in rawColor.dynKeys) { // Detects the presence of color.
				this._parseColorTransform(rawColor, this._helpColorTransform)
				colorOffset = this._colorArray.length
				this._colorArray.length += 8
				this._colorArray[colorOffset++] = round(this._helpColorTransform.alphaMultiplier * 100).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.redMultiplier * 100).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.greenMultiplier * 100).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.blueMultiplier * 100).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.alphaOffset.toDouble()).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.redOffset.toDouble()).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.greenOffset.toDouble()).toInt()
				this._colorArray[colorOffset++] = round(this._helpColorTransform.blueOffset.toDouble()).toInt()
				colorOffset -= 8
				break
			}
		}

		if (colorOffset < 0) {
			if (this._defaultColorOffset < 0) {
				colorOffset = this._colorArray.length
				this._defaultColorOffset = colorOffset
				this._colorArray.length += 8
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
			}

			colorOffset = this._defaultColorOffset
		}

		val frameIntOffset = this._frameIntArray.length
		this._frameIntArray.length += 1
		this._frameIntArray[frameIntOffset] = colorOffset

		return frameOffset
	}

	protected fun _parseSlotDeformFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameFloatOffset = this._frameFloatArray.length
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		val rawVertices = rawData.getDynamic(DataParser.VERTICES)?.doubleArray
		val offset = ObjectDataParser._getInt(rawData, DataParser.OFFSET, 0) // uint
		val vertexCount = this._intArray[this._mesh!!.geometry.offset + BinaryOffset.GeometryVertexCount]
		val meshName = "" + this._mesh?.parent?.name + "_" + this._slot?.name + "_" + this._mesh?.name
		val weight = this._mesh?.geometry!!.weight

		var x: Float
		var y: Float
		var iB = 0
		var iV = 0
		if (weight != null) {
			val rawSlotPose = this._weightSlotPose[meshName]
			this._helpMatrixA.copyFromArray(rawSlotPose!!.data, 0)
			this._frameFloatArray.length += weight.count * 2
			iB = weight.offset + BinaryOffset.WeigthBoneIndices + weight.bones.length
		} else {
			this._frameFloatArray.length += vertexCount * 2
		}

		//for (var i = 0; i < vertexCount * 2; i += 2) {
		for (i in 0 until vertexCount * 2 step 2) {
			if (rawVertices == null) { // Fill 0.
				x = 0f
				y = 0f
			} else {
				if (i < offset || i - offset >= rawVertices.size) {
					x = 0f
				} else {
					x = rawVertices[i - offset].toFloat()
				}

				if (i + 1 < offset || i + 1 - offset >= rawVertices.size) {
					y = 0f
				} else {
					y = rawVertices[i + 1 - offset].toFloat()
				}
			}

			if (weight != null) { // If mesh is skinned, transform point by bone bind pose.
				val rawBonePoses = this._weightBonePoses[meshName]!!
				val vertexBoneCount = this._intArray[iB++]

				this._helpMatrixA.deltaTransformPoint(x, y, this._helpPoint)
				x = this._helpPoint.xf
				y = this._helpPoint.yf

				//for (var j = 0; j < vertexBoneCount; ++j) {
				for (j in 0 until vertexBoneCount) {
					val boneIndex = this._intArray[iB++]
					this._helpMatrixB.copyFromArray(rawBonePoses.data, boneIndex * 7 + 1)
					this._helpMatrixB.invert()
					this._helpMatrixB.deltaTransformPoint(x, y, this._helpPoint)

					this._frameFloatArray[frameFloatOffset + iV++] = this._helpPoint.xf.toDouble()
					this._frameFloatArray[frameFloatOffset + iV++] = this._helpPoint.yf.toDouble()
				}
			} else {
				this._frameFloatArray[frameFloatOffset + i] = x.toDouble()
				this._frameFloatArray[frameFloatOffset + i + 1] = y.toDouble()
			}
		}

		if (frameStart == 0) {
			val frameIntOffset = this._frameIntArray.length
			this._frameIntArray.length += 1 + 1 + 1 + 1 + 1
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformVertexOffset] = this._mesh!!.geometry.offset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformCount] = this._frameFloatArray.length -
					frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount] = this._frameFloatArray.length -
					frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset] = 0
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset] = frameFloatOffset -
					this._animation!!.frameFloatOffset
			this._timelineArray[this._timeline!!.offset + BinaryOffset.TimelineFrameValueCount] =
					(frameIntOffset - this._animation!!.frameIntOffset).toDouble()
		}

		return frameOffset
	}

	protected fun _parseIKConstraintFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameIntOffset = this._frameIntArray.length
		this._frameIntArray.length += 2
		this._frameIntArray[frameIntOffset++] =
				if (ObjectDataParser._getBoolean(rawData, DataParser.BEND_POSITIVE, true)) 1 else 0
		this._frameIntArray[frameIntOffset++] =
				round(ObjectDataParser._getNumber(rawData, DataParser.WEIGHT, 1.0) * 100.0).toInt()

		return frameOffset
	}

	protected fun _parseActionData(
		rawData: Any?,
		type: ActionType,
		bone: BoneData?,
		slot: SlotData?
	): FastArrayList<ActionData> {
		val actions = FastArrayList<ActionData>()

		if (rawData is String) {
			val action = pool.actionData.borrow()
			action.type = type
			action.name = rawData
			action.bone = bone
			action.slot = slot
            actions.add(action)
		} else if (rawData is List<*>) {
			(rawData as List<Map<String, Any?>>).fastForEach { rawAction ->
				val action = pool.actionData.borrow()

				if (rawAction.containsDynamic(DataParser.GOTO_AND_PLAY)) {
					action.type = ActionType.Play
					action.name = ObjectDataParser._getString(rawAction, DataParser.GOTO_AND_PLAY, "")
				} else {
					if (rawAction.containsDynamic(DataParser.TYPE) && rawAction[DataParser.TYPE] is String) {
						action.type = DataParser._getActionType(rawAction[DataParser.TYPE]?.toString())
					} else {
						action.type = ActionType[ObjectDataParser._getInt(rawAction, DataParser.TYPE, type.id)]
					}

					action.name = ObjectDataParser._getString(rawAction, DataParser.NAME, "")
				}

				if (rawAction.containsDynamic(DataParser.BONE)) {
					val boneName = ObjectDataParser._getString(rawAction, DataParser.BONE, "")
					action.bone = this._armature?.getBone(boneName)
				} else {
					action.bone = bone
				}

				if (rawAction.containsDynamic(DataParser.SLOT)) {
					val slotName = ObjectDataParser._getString(rawAction, DataParser.SLOT, "")
					action.slot = this._armature?.getSlot(slotName)
				} else {
					action.slot = slot
				}

				var userData: UserData? = null

				if (rawAction.containsDynamic(DataParser.INTS)) {
					if (userData == null) {
						userData = pool.userData.borrow()
					}

					val rawInts = rawAction[DataParser.INTS] .intArrayList
					for (rawValue in rawInts) {
						userData.addInt(rawValue)
					}
				}

				if (rawAction.containsDynamic(DataParser.FLOATS)) {
					if (userData == null) {
						userData = pool.userData.borrow()
					}

					val rawFloats = rawAction[DataParser.FLOATS].doubleArrayList
					for (rawValue in rawFloats) {
						userData.addFloat(rawValue)
					}
				}

				if (rawAction.containsDynamic(DataParser.STRINGS)) {
					if (userData == null) {
						userData = pool.userData.borrow()
					}

					val rawStrings = rawAction[DataParser.STRINGS] as List<String>
					for (rawValue in rawStrings) {
						userData.addString(rawValue)
					}
				}

				action.data = userData
                actions.add(action)
			}
		}

		return actions
	}

	protected fun _parseDeformFrame(rawData: Any?, frameStart: Int, frameCount: Int): Int {
		val frameFloatOffset = this._frameFloatArray.length
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		val rawVertices = if (rawData.containsDynamic(DataParser.VERTICES))
			rawData.getDynamic(DataParser.VERTICES)?.doubleArrayList else
			rawData.getDynamic(DataParser.VALUE)?.doubleArrayList
		val offset = ObjectDataParser._getNumber(rawData, DataParser.OFFSET, 0.0).toInt() // uint
		val vertexCount = this._intArray[this._geometry!!.offset + BinaryOffset.GeometryVertexCount]
		val weight = this._geometry!!.weight
		var x: Double
		var y: Double

		if (weight != null) {
			// TODO
		} else {
			this._frameFloatArray.length += vertexCount * 2

			//for (var i = 0;i < vertexCount * 2;i += 2) {
			for (i in 0 until (vertexCount * 2) step 2) {
				if (rawVertices != null) {
					if (i < offset || i - offset >= rawVertices.length) {
						x = 0.0
					} else {
						x = rawVertices[i - offset]
					}

					if (i + 1 < offset || i + 1 - offset >= rawVertices.length) {
						y = 0.0
					} else {
						y = rawVertices[i + 1 - offset]
					}
				} else {
					x = 0.0
					y = 0.0
				}

				this._frameFloatArray[frameFloatOffset + i] = x
				this._frameFloatArray[frameFloatOffset + i + 1] = y
			}
		}

		if (frameStart == 0) {
			val frameIntOffset = this._frameIntArray.length
			this._frameIntArray.length += 1 + 1 + 1 + 1 + 1
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformVertexOffset] = this._geometry!!.offset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformCount] = this._frameFloatArray.length -
					frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount] = this._frameFloatArray.length -
					frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset] = 0
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset] = frameFloatOffset -
					this._animation!!.frameFloatOffset
			this._timelineArray[this._timeline!!.offset + BinaryOffset.TimelineFrameValueCount] =
					(frameIntOffset - this._animation!!.frameIntOffset).toDouble()
		}

		return frameOffset
	}

	protected fun _parseTransform(rawData: Any?, transform: TransformDb, scale: Double) {
		transform.xf = (ObjectDataParser._getNumber(rawData, DataParser.X, 0.0) * scale).toFloat()
		transform.yf = (ObjectDataParser._getNumber(rawData, DataParser.Y, 0.0) * scale).toFloat()

		if (rawData.containsDynamic(DataParser.ROTATE) || rawData.containsDynamic(DataParser.SKEW)) {
			transform.rotation = TransformDb.normalizeRadian(
				ObjectDataParser._getNumber(
					rawData,
					DataParser.ROTATE,
					0.0
				) * TransformDb.DEG_RAD
			).toFloat()
			transform.skew = TransformDb.normalizeRadian(
				ObjectDataParser._getNumber(
					rawData,
					DataParser.SKEW,
					0.0
				) * TransformDb.DEG_RAD
			).toFloat()
		} else if (rawData.containsDynamic(DataParser.SKEW_X) || rawData.containsDynamic(DataParser.SKEW_Y)) {
			transform.rotation = TransformDb.normalizeRadian(
				ObjectDataParser._getNumber(
					rawData,
					DataParser.SKEW_Y,
					0.0
				) * TransformDb.DEG_RAD
			).toFloat()
			transform.skew = (TransformDb.normalizeRadian(
				ObjectDataParser._getNumber(
					rawData,
					DataParser.SKEW_X,
					0.0
				) * TransformDb.DEG_RAD
			) - transform.rotation).toFloat()
		}

		transform.scaleX = ObjectDataParser._getNumber(rawData, DataParser.SCALE_X, 1.0).toFloat()
		transform.scaleY = ObjectDataParser._getNumber(rawData, DataParser.SCALE_Y, 1.0).toFloat()
	}

	protected fun _parseColorTransform(rawData: Any?, color: ColorTransform) {
		color.alphaMultiplier = ObjectDataParser._getNumber(rawData, DataParser.ALPHA_MULTIPLIER, 100.0) * 0.01
		color.redMultiplier = ObjectDataParser._getNumber(rawData, DataParser.RED_MULTIPLIER, 100.0) * 0.01
		color.greenMultiplier = ObjectDataParser._getNumber(rawData, DataParser.GREEN_MULTIPLIER, 100.0) * 0.01
		color.blueMultiplier = ObjectDataParser._getNumber(rawData, DataParser.BLUE_MULTIPLIER, 100.0) * 0.01
		color.alphaOffset = ObjectDataParser._getInt(rawData, DataParser.ALPHA_OFFSET, 0)
		color.redOffset = ObjectDataParser._getInt(rawData, DataParser.RED_OFFSET, 0)
		color.greenOffset = ObjectDataParser._getInt(rawData, DataParser.GREEN_OFFSET, 0)
		color.blueOffset = ObjectDataParser._getInt(rawData, DataParser.BLUE_OFFSET, 0)
	}

	open protected fun _parseGeometry(rawData: Any?, geometry: GeometryData) {
		val rawVertices = rawData.getDynamic(DataParser.VERTICES).doubleArray
		val vertexCount: Int = rawVertices.size / 2 // uint
		var triangleCount = 0
		val geometryOffset = this._intArray.length
		val verticesOffset = this._floatArray.length
		//
		geometry.offset = geometryOffset
		geometry.data = this._data
		//
		this._intArray.length += 1 + 1 + 1 + 1
		this._intArray[geometryOffset + BinaryOffset.GeometryVertexCount] = vertexCount
		this._intArray[geometryOffset + BinaryOffset.GeometryFloatOffset] = verticesOffset
		this._intArray[geometryOffset + BinaryOffset.GeometryWeightOffset] = -1 //
		//
		this._floatArray.length += vertexCount * 2
		//for (var i = 0, l = vertexCount * 2; i < l; ++i) {
		for (i in 0 until vertexCount * 2) {
			this._floatArray[verticesOffset + i] = rawVertices[i]
		}

		if (rawData.containsDynamic(DataParser.TRIANGLES)) {
			val rawTriangles = rawData.getDynamic(DataParser.TRIANGLES).doubleArray
			triangleCount = rawTriangles.size / 3 // uint
			//
			this._intArray.length += triangleCount * 3
			//for (var i = 0, l = triangleCount * 3; i < l; ++i) {
			for (i in 0 until triangleCount * 3) {
				this._intArray[geometryOffset + BinaryOffset.GeometryVertexIndices + i] = rawTriangles[i].toInt()
			}
		}
		// Fill triangle count.
		this._intArray[geometryOffset + BinaryOffset.GeometryTriangleCount] = triangleCount

		if (rawData.containsDynamic(DataParser.UVS)) {
			val rawUVs = rawData.getDynamic(DataParser.UVS).doubleArray
			val uvOffset = verticesOffset + vertexCount * 2
			this._floatArray.length += vertexCount * 2
			//for (var i = 0, l = vertexCount * 2; i < l; ++i) {
			for (i in 0 until vertexCount * 2) {
				this._floatArray[uvOffset + i] = rawUVs[i]
			}
		}

		if (rawData.containsDynamic(DataParser.WEIGHTS)) {
			val rawWeights = rawData.getDynamic(DataParser.WEIGHTS).doubleArray
			val weightCount = (rawWeights.size - vertexCount) / 2 // uint
			val weightOffset = this._intArray.length
			val floatOffset = this._floatArray.length
			var weightBoneCount = 0
			val sortedBones = this._armature?.sortedBones
			val weight = pool.weightData.borrow()
			weight.count = weightCount
			weight.offset = weightOffset

			this._intArray.length += 1 + 1 + weightBoneCount + vertexCount + weightCount
			this._intArray[weightOffset + BinaryOffset.WeigthFloatOffset] = floatOffset

			if (rawData.containsDynamic(DataParser.BONE_POSE)) {
				val rawSlotPose = rawData.getDynamic(DataParser.SLOT_POSE).doubleArray
				val rawBonePoses = rawData.getDynamic(DataParser.BONE_POSE).doubleArray
				val weightBoneIndices = IntArrayList()

				weightBoneCount = (rawBonePoses.size / 7) // uint
				weightBoneIndices.length = weightBoneCount

				//for (var i = 0; i < weightBoneCount; ++i) {
				for (i in 0 until weightBoneCount) {
					val rawBoneIndex = rawBonePoses[i * 7].toInt() // uint
					val bone = this._rawBones[rawBoneIndex]
					weight.addBone(bone)
					weightBoneIndices[i] = rawBoneIndex
					this._intArray[weightOffset + BinaryOffset.WeigthBoneIndices + i] =
							sortedBones!!.indexOf(bone)
				}

				this._floatArray.length += weightCount * 3
				this._helpMatrixA.copyFromArray(rawSlotPose, 0)

				// for (var i = 0, iW = 0, iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount, iV = floatOffset; i < vertexCount; ++i) {
				var iW = 0
				var iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount
				var iV = floatOffset
				for (i in 0 until vertexCount) {
					val iD = i * 2
					val vertexBoneCount = rawWeights[iW++].toInt() // uint
					this._intArray[iB++] = vertexBoneCount

					var x = this._floatArray[verticesOffset + iD].toFloat()
					var y = this._floatArray[verticesOffset + iD + 1].toFloat()
					this._helpMatrixA.transform(x, y, this._helpPoint)
					x = this._helpPoint.xf
					y = this._helpPoint.yf

					//for (var j = 0; j < vertexBoneCount; ++j) {
					for (j in 0 until vertexBoneCount) {
						val rawBoneIndex = rawWeights[iW++].toInt() // uint
						val boneIndex = weightBoneIndices.indexOf(rawBoneIndex)
						this._helpMatrixB.copyFromArray(rawBonePoses, boneIndex * 7 + 1)
						this._helpMatrixB.invert()
						this._helpMatrixB.transform(x, y, this._helpPoint)
						this._intArray[iB++] = boneIndex
						this._floatArray[iV++] = rawWeights[iW++]
						this._floatArray[iV++] = this._helpPoint.xf.toDouble()
						this._floatArray[iV++] = this._helpPoint.yf.toDouble()
					}
				}
			} else {
				val rawBones = rawData.getDynamic(DataParser.BONES).doubleArray
				weightBoneCount = rawBones.size

				//for (var i = 0; i < weightBoneCount; i++) {
				for (i in 0 until weightBoneCount) {
					val rawBoneIndex = rawBones[i].toInt()
					val bone = this._rawBones[rawBoneIndex]
					weight.addBone(bone)
					this._intArray[weightOffset + BinaryOffset.WeigthBoneIndices + i] =
							sortedBones!!.indexOf(bone)
				}

				this._floatArray.length += weightCount * 3
				//for (var i = 0, iW = 0, iV = 0, iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount, iF = floatOffset; i < weightCount; i++) {
				var iW = 0
				var iV = 0
				var iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount
				var iF = floatOffset
				for (i in 0 until weightCount) {
					val vertexBoneCount = rawWeights[iW++].toInt()
					this._intArray[iB++] = vertexBoneCount

					//for (var j = 0; j < vertexBoneCount; j++) {
					for (j in 0 until vertexBoneCount) {
						val boneIndex = rawWeights[iW++]
						val boneWeight = rawWeights[iW++]
						val x = rawVertices[iV++]
						val y = rawVertices[iV++]

						this._intArray[iB++] = rawBones.indexOfFirst { it == boneIndex }
						this._floatArray[iF++] = boneWeight
						this._floatArray[iF++] = x
						this._floatArray[iF++] = y
					}
				}
			}

			geometry.weight = weight
		}
	}

	protected open fun _parseArray(@Suppress("UNUSED_PARAMETER") rawData: Any?) {
		this._intArray.length = 0
		this._floatArray.length = 0
		this._frameIntArray.length = 0
		this._frameFloatArray.length = 0
		this._frameArray.length = 0
		this._timelineArray.length = 0
		this._colorArray.length = 0
	}

	protected fun _modifyArray() {
		// Align.
		if ((this._intArray.length % 2) != 0) {
			this._intArray.push(0)
		}

		if ((this._frameIntArray.length % 2) != 0) {
			this._frameIntArray.push(0)
		}

		if ((this._frameArray.length % 2) != 0) {
			this._frameArray.push(0.0)
		}

		if ((this._timelineArray.length % 2) != 0) {
			//this._timelineArray.push(0)
			this._timelineArray.push(0.0)
		}

		if ((this._timelineArray.length % 2) != 0) {
			this._colorArray.push(0)
		}

		val l1 = this._intArray.length * 2
		val l2 = this._floatArray.length * 4
		val l3 = this._frameIntArray.length * 2
		val l4 = this._frameFloatArray.length * 4
		val l5 = this._frameArray.length * 2
		val l6 = this._timelineArray.length * 2
		val l7 = this._colorArray.length * 2
		val lTotal = l1 + l2 + l3 + l4 + l5 + l6 + l7
		//
		val binary = MemBufferAlloc(lTotal)
		val intArray = binary.sliceInt16BufferByteOffset(0, this._intArray.length)
		val floatArray = binary.sliceFloat32BufferByteOffset(l1, this._floatArray.length)
		val frameIntArray = binary.sliceInt16BufferByteOffset(l1 + l2, this._frameIntArray.length)
		val frameFloatArray = binary.sliceFloat32BufferByteOffset(l1 + l2 + l3, this._frameFloatArray.length)
		val frameArray = binary.sliceInt16BufferByteOffset(l1 + l2 + l3 + l4, this._frameArray.length)
		val timelineArray = binary.sliceUint16BufferByteOffset(l1 + l2 + l3 + l4 + l5, this._timelineArray.length)
		val colorArray = binary.sliceInt16BufferByteOffset(l1 + l2 + l3 + l4 + l5 + l6, this._colorArray.length)

		for (i in 0 until this._intArray.length) {
			intArray[i] = this._intArray[i].toShort()
		}

		for (i in 0 until this._floatArray.length) {
			floatArray[i] = this._floatArray[i].toFloat()
		}

		//for (var i = 0, l = this._frameIntArray.length; i < l; ++i) {
		for (i in 0 until this._frameIntArray.length) {
			frameIntArray[i] = this._frameIntArray[i].toShort()
		}

		//for (var i = 0, l = this._frameFloatArray.length; i < l; ++i) {
		for (i in 0 until this._frameFloatArray.length) {
			frameFloatArray[i] = this._frameFloatArray[i].toFloat()
		}

		//for (var i = 0, l = this._frameArray.length; i < l; ++i) {
		for (i in 0 until this._frameArray.length) {
			frameArray[i] = this._frameArray[i].toInt().toShort()
		}

		//for (var i = 0, l = this._timelineArray.length; i < l; ++i) {
		for (i in 0 until this._timelineArray.length) {
			timelineArray[i] = this._timelineArray[i].toInt()
		}

		//for (var i = 0, l = this._colorArray.length; i < l; ++i) {
		for (i in 0 until this._colorArray.length) {
			colorArray[i] = this._colorArray[i].toShort()
		}

		this._data?.binary = binary
		this._data?.intArray = intArray
		this._data?.floatArray = floatArray
		this._data?.frameIntArray = frameIntArray.toFloat()
		this._data?.frameFloatArray = frameFloatArray
		this._data?.frameArray = frameArray
		this._data?.timelineArray = timelineArray
		this._data?.colorArray = colorArray
		this._defaultColorOffset = -1
	}

	override fun parseDragonBonesData(rawData: Any?, scale: Double): DragonBonesData? {
		//console.assert(rawData != null && rawData != null, "Data error.")

		val version = ObjectDataParser._getString(rawData, DataParser.VERSION, "")
		val compatibleVersion = ObjectDataParser._getString(rawData, DataParser.COMPATIBLE_VERSION, "")

		if (
			DataParser.DATA_VERSIONS.indexOf(version) >= 0 ||
			DataParser.DATA_VERSIONS.indexOf(compatibleVersion) >= 0
		) {
			val data = pool.dragonBonesData.borrow()
			data.version = version
			data.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
			data.frameRate = ObjectDataParser._getInt(rawData, DataParser.FRAME_RATE, 24)

			if (data.frameRate == 0) { // Data error.
				data.frameRate = 24
			}

			if (rawData.containsDynamic(DataParser.ARMATURE)) {
				this._data = data
				this._parseArray(rawData)

				val rawArmatures = rawData.getDynamic(DataParser.ARMATURE) as List<Any?>
				rawArmatures.fastForEach { rawArmature ->
					data.addArmature(this._parseArmature(rawArmature, scale))
				}

				if (this._data?.binary == null) { // DragonBones.webAssembly ? 0 : null;
					this._modifyArray()
				}

				if (rawData.containsDynamic(DataParser.STAGE)) {
					data.stage = data.getArmature(ObjectDataParser._getString(rawData, DataParser.STAGE, ""))
				} else if (data.armatureNames.length > 0) {
					data.stage = data.getArmature(data.armatureNames[0])
				}

				this._data = null
			}

			if (rawData.containsDynamic(DataParser.TEXTURE_ATLAS)) {
				this._rawTextureAtlases = rawData.getDynamic(DataParser.TEXTURE_ATLAS).asFastArrayList()
			}

			return data
		} else {
			Console.assert(
				false,
				"Nonsupport data version: " + version + "\n" +
						"Please convert DragonBones data to support version.\n" +
						"Read more: https://github.com/DragonBones/Tools/"
			)
		}

		return null
	}

	override fun parseTextureAtlasData(rawData: Any?, textureAtlasData: TextureAtlasData, scale: Double): Boolean {
		if (rawData == null) {
			if (this._rawTextureAtlases == null || this._rawTextureAtlases!!.length == 0) {
				return false
			}

			val rawTextureAtlas = this._rawTextureAtlases!![this._rawTextureAtlasIndex++]
			this.parseTextureAtlasData(rawTextureAtlas, textureAtlasData, scale)

			if (this._rawTextureAtlasIndex >= this._rawTextureAtlases!!.length) {
				this._rawTextureAtlasIndex = 0
				this._rawTextureAtlases = null
			}

			return true
		}

		// Texture format.
		textureAtlasData.width = ObjectDataParser._getInt(rawData, DataParser.WIDTH, 0)
		textureAtlasData.height = ObjectDataParser._getInt(rawData, DataParser.HEIGHT, 0)
		textureAtlasData.scale =
				if (scale == 1.0) (1.0 / ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0)) else scale
		textureAtlasData.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		textureAtlasData.imagePath = ObjectDataParser._getString(rawData, DataParser.IMAGE_PATH, "")

		if (rawData.containsDynamic(DataParser.SUB_TEXTURE)) {
			val rawTextures = rawData.getDynamic(DataParser.SUB_TEXTURE).asFastArrayList()
			//for (var i = 0, l = rawTextures.length; i < l; ++i) {
			for (i in 0 until rawTextures.length) {
				val rawTexture = rawTextures[i]
				val frameWidth = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_WIDTH, -1.0)
				val frameHeight = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_HEIGHT, -1.0)
				val textureData = textureAtlasData.createTexture()

				textureData.rotated = ObjectDataParser._getBoolean(rawTexture, DataParser.ROTATED, false)
				textureData.name = ObjectDataParser._getString(rawTexture, DataParser.NAME, "")
				textureData.region.x = ObjectDataParser._getNumber(rawTexture, DataParser.X, 0.0)
				textureData.region.y = ObjectDataParser._getNumber(rawTexture, DataParser.Y, 0.0)
				textureData.region.width = ObjectDataParser._getNumber(rawTexture, DataParser.WIDTH, 0.0)
				textureData.region.height = ObjectDataParser._getNumber(rawTexture, DataParser.HEIGHT, 0.0)

				if (frameWidth > 0.0 && frameHeight > 0.0) {
					textureData.frame = TextureData.createRectangle()
					textureData.frame!!.x = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_X, 0.0)
					textureData.frame!!.y = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_Y, 0.0)
					textureData.frame!!.width = frameWidth
					textureData.frame!!.height = frameHeight
				}

				textureAtlasData.addTexture(textureData)
			}
		}

		return true
	}
}

/**
 * @private
 */
class ActionFrame {
	var frameStart: Int = 0
	//public val actions:  DoubleArrayList = DoubleArrayList()
	val actions: IntArrayList = IntArrayList()
}

