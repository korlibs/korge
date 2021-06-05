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
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import kotlin.math.*

/**
 * @internal
 */
abstract class Constraint(pool: SingleObjectPool<out Constraint>) : BaseObject(pool) {
	/**
	 * - For timeline state.
	 * @internal
	 */
	var _constraintData: ConstraintData? = null
	protected lateinit var _armature: Armature
	/**
	 * - For sort bones.
	 * @internal
	 */
	lateinit var _target: Bone
	/**
	 * - For sort bones.
	 * @internal
	 */
	lateinit var _root: Bone
	protected var _bone: Bone? = null

	override fun _onClear() {
		//this._armature = null //
		//this._target = null //
		//this._root = null //
		this._bone = null
	}

	abstract fun init(constraintData: ConstraintData, armature: Armature)
	abstract fun update()
	abstract fun invalidUpdate()

	val name: String get() {
		return this._constraintData?.name ?: ""
	}
}
/**
 * @internal
 */
class IKConstraint(pool: SingleObjectPool<IKConstraint>) :  Constraint(pool) {
	override fun toString(): String {
		return "[class dragonBones.IKConstraint]"
	}

	private var _scaleEnabled: Boolean = false // TODO
	/**
	 * - For timeline state.
	 * @internal
	 */
	var _bendPositive: Boolean = false
	/**
	 * - For timeline state.
	 * @internal
	 */
	var _weight: Double = 1.0

	override fun _onClear() {
		super._onClear()

		this._scaleEnabled = false
		this._bendPositive = false
		this._weight = 1.0
		//this._constraintData = null
	}

	private fun _computeA() {
		val ikGlobal = this._target.global
		val global = this._root.global
		val globalTransformMatrix = this._root.globalTransformMatrix

		var radian = atan2(ikGlobal.yf - global.yf, ikGlobal.xf - global.xf)
		if (global.scaleX < 0.0) {
			radian += PI.toFloat()
		}

		global.rotation += (TransformDb.normalizeRadian((radian - global.rotation).toDouble()) * this._weight).toFloat()
		global.toMatrix(globalTransformMatrix)
	}

	private fun _computeB() {
		val boneLength = (this._bone!!)._boneData!!.length
		val parent = this._root
		val ikGlobal = this._target.global
		val parentGlobal = parent.global
		val global = (this._bone!!).global
		val globalTransformMatrix = (this._bone!!).globalTransformMatrix

		val x = globalTransformMatrix.af * boneLength
		val y = globalTransformMatrix.bf * boneLength
		val lLL = x * x + y * y
		val lL = sqrt(lLL)
		var dX = global.xf - parentGlobal.xf
		var dY = global.yf - parentGlobal.yf
		val lPP = dX * dX + dY * dY
		val lP = sqrt(lPP)
		val rawRadian = global.rotation
		val rawParentRadian = parentGlobal.rotation
		val rawRadianA = atan2(dY, dX)

		dX = ikGlobal.xf - parentGlobal.xf
		dY = ikGlobal.yf - parentGlobal.yf
		val lTT = dX * dX + dY * dY
		val lT = sqrt(lTT)

		var radianA: Double
		if (lL + lP <= lT || lT + lL <= lP || lT + lP <= lL) {
			radianA = atan2(ikGlobal.yf - parentGlobal.yf, ikGlobal.xf - parentGlobal.xf).toDouble()
			if (lL + lP <= lT) {
			}
			else if (lP < lL) {
				radianA += PI
			}
		}
		else {
			val h = (lPP - lLL + lTT) / (2.0 * lTT)
			val r = sqrt(lPP - h * h * lTT) / lT
			val hX = parentGlobal.xf + (dX * h)
			val hY = parentGlobal.yf + (dY * h)
			val rX = -dY * r
			val rY = dX * r

			var isPPR = false
			val parentParent = parent.parent
			if (parentParent != null) {
				val parentParentMatrix = parentParent.globalTransformMatrix
				isPPR = parentParentMatrix.af * parentParentMatrix.df - parentParentMatrix.bf * parentParentMatrix.cf < 0.0
			}

			if (isPPR != this._bendPositive) {
				global.xf = (hX - rX).toFloat()
				global.yf = (hY - rY).toFloat()
			}
			else {
				global.xf = (hX + rX).toFloat()
				global.yf = (hY + rY).toFloat()
			}

			radianA = atan2(global.yf - parentGlobal.yf, global.xf - parentGlobal.xf).toDouble()
		}

		val dR = TransformDb.normalizeRadian(radianA - rawRadianA)
		parentGlobal.rotation = (rawParentRadian + dR * this._weight).toFloat()
		parentGlobal.toMatrix(parent.globalTransformMatrix)
		//
		val currentRadianA = rawRadianA + dR * this._weight
		global.xf = (parentGlobal.xf + cos(currentRadianA) * lP).toFloat()
		global.yf = (parentGlobal.yf + sin(currentRadianA) * lP).toFloat()
		//
		var radianB = atan2(ikGlobal.yf - global.yf, ikGlobal.xf - global.xf)
		if (global.scaleX < 0.0) {
			radianB += PI.toFloat()
		}

		global.rotation = (parentGlobal.rotation + rawRadian - rawParentRadian + TransformDb.normalizeRadian(radianB - dR - rawRadian) * this._weight).toFloat()
		global.toMatrix(globalTransformMatrix)
	}

	override fun init(constraintData: ConstraintData, armature: Armature) {
		if (this._constraintData != null) {
			return
		}

		this._constraintData = constraintData
		this._armature = armature
		this._target = this._armature.getBone(this._constraintData?.target?.name)!!
		this._root = this._armature.getBone(this._constraintData?.root?.name)!!
		this._bone = this._armature.getBone(this._constraintData?.bone?.name)

		run {
			val ikConstraintData = this._constraintData as IKConstraintData
			this._scaleEnabled = ikConstraintData.scaleEnabled
			this._bendPositive = ikConstraintData.bendPositive
			this._weight = ikConstraintData.weight
		}

		this._root._hasConstraint = true
	}

	override fun update() {
		this._root.updateByConstraint()

		if (this._bone != null) {
			this._bone?.updateByConstraint()
			this._computeB()
		}
		else {
			this._computeA()
		}
	}

	override fun invalidUpdate() {
		this._root.invalidUpdate()
		this._bone?.invalidUpdate()
	}
}

/**
 * @internal
 */
class PathConstraint(pool: SingleObjectPool<PathConstraint>) :  Constraint(pool) {

	var dirty: Boolean = false
	var pathOffset: Int = 0
	var position: Double = 0.0
	var spacing: Double = 0.0
	var rotateOffset: Double = 0.0
	var rotateMix: Double = 1.0
	var translateMix: Double = 1.0

	private var _pathSlot: Slot? = null
	private var _bones: FastArrayList<Bone> = FastArrayList()

	private var _spaces:  DoubleArray = DoubleArray(0)
	private var _positions:  DoubleArray = DoubleArray(0)
	private var _curves:  DoubleArray = DoubleArray(0)
	private var _boneLengths:  DoubleArray = DoubleArray(0)

	private var _pathGlobalVertices:  DoubleArray = DoubleArray(0)
	private var _segments:  DoubleArray = DoubleArray(1) { 10.0 }

	override fun toString(): String {
		return "[class dragonBones.PathConstraint]"
	}

	override fun _onClear() {
		super._onClear()

		this.dirty = false
		this.pathOffset = 0

		this.position = 0.0
		this.spacing = 0.0
		this.rotateOffset = 0.0
		this.rotateMix = 1.0
		this.translateMix = 1.0

		this._pathSlot = null
		this._bones.clear()

		this._spaces = DoubleArray(0)
		this._positions = DoubleArray(0)
		this._curves = DoubleArray(0)
		this._boneLengths = DoubleArray(0)

		this._pathGlobalVertices = DoubleArray(0)
	}

	protected fun _updatePathVertices(verticesData: GeometryData) {
		//计算曲线的节点数据
		val armature = this._armature
		val dragonBonesData = armature.armatureData.parent!!
		val scale = armature.armatureData.scale
		val intArray = dragonBonesData.intArray!!
		val floatArray = dragonBonesData.floatArray!!

		val pathOffset = verticesData.offset
		val pathVertexCount = intArray[pathOffset + BinaryOffset.GeometryVertexCount]
		val pathVertexOffset = intArray[pathOffset + BinaryOffset.GeometryFloatOffset]

		this._pathGlobalVertices = DoubleArray(pathVertexCount * 2)

		val weightData = verticesData.weight
		//没有骨骼约束我,那节点只受自己的Bone控制
		if (weightData == null) {
			val parentBone = this._pathSlot!!.parent
			parentBone.updateByConstraint()

			val matrix = parentBone.globalTransformMatrix

			var iV = pathVertexOffset.toInt()
			for (i in 0 until pathVertexCount step 2) {
				val vx = floatArray[iV++] * scale
				val vy = floatArray[iV++] * scale

				val x = matrix.af * vx + matrix.cf * vy + matrix.txf
				val y = matrix.bf * vx + matrix.df * vy + matrix.tyf

				//
				this._pathGlobalVertices[i] = x
				this._pathGlobalVertices[i + 1] = y
			}
			return
		}

		//有骨骼约束我,那我的节点受骨骼权重控制
		val bones = this._pathSlot!!._geometryBones
		val weightBoneCount = weightData.bones.size

		val weightOffset = weightData.offset
		val floatOffset = intArray[weightOffset + BinaryOffset.WeigthFloatOffset].toInt()

		var iV = floatOffset
		var iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount

		var iW = 0
		for (i in 0 until pathVertexCount) {
			val vertexBoneCount = intArray[iB++] //

			var xG = 0.0
			var yG = 0.0
			for (ii in 0 until vertexBoneCount) {
				val boneIndex = intArray[iB++].toInt()
				val bone = bones[boneIndex] ?: continue

				bone.updateByConstraint()
				val matrix = bone.globalTransformMatrix
				val weight = floatArray[iV++]
				val vx = floatArray[iV++] * scale
				val vy = floatArray[iV++] * scale
				xG += (matrix.af * vx + matrix.cf * vy + matrix.txf) * weight
				yG += (matrix.bf * vx + matrix.df * vy + matrix.tyf) * weight
			}

			this._pathGlobalVertices[iW++] = xG
			this._pathGlobalVertices[iW++] = yG
		}
	}

	protected fun _computeVertices(start: Int, count: Int, offset: Int, out:  DoubleArrayList) {
		//TODO优化
		var iW = start
		for (i in offset until count step 2) {
			out[i] = this._pathGlobalVertices[iW++]
			out[i + 1] = this._pathGlobalVertices[iW++]
		}
	}

	protected fun _computeBezierCurve(pathDisplayDta: PathDisplayData, spaceCount: Int, tangents: Boolean, percentPosition: Boolean, percentSpacing: Boolean) {
		//计算当前的骨骼在曲线上的位置
		val armature = this._armature
		val intArray = armature.armatureData.parent!!.intArray!!
		val vertexCount = intArray[pathDisplayDta.geometry.offset + BinaryOffset.GeometryVertexCount].toInt()

		this._positions = DoubleArray(spaceCount * 3 + 2)
		val positions = this._positions
		val spaces = this._spaces
		val isClosed = pathDisplayDta.closed
		val curveVertices =  DoubleArrayList()
		var verticesLength = vertexCount * 2
		var curveCount = verticesLength / 6
		var preCurve = -1
		var position: Double = this.position


		var pathLength: Double
		//不需要匀速运动，效率高些
		if (!pathDisplayDta.constantSpeed) {
			val lenghts = pathDisplayDta.curveLengths
			curveCount -= if (isClosed) 1 else 2
			pathLength = lenghts[curveCount]

			if (percentPosition) {
				position *= pathLength
			}

			if (percentSpacing) {
				for (i in 0 until spaceCount) {
					spaces[i] *= pathLength
				}
			}

			curveVertices.lengthSet = 8
			//for (var i = 0, o = 0, curve = 0; i < spaceCount; i++ , o += 3) {
			var curve = 0
			for (i in 0 until spaceCount) {
				val o = i * 3
				val space = spaces[i]
				position += space

				if (isClosed) {
					position %= pathLength
					if (position < 0) {
						position += pathLength
					}
					curve = 0
				}
				else if (position < 0) {
					//TODO
					continue
				}
				else if (position > pathLength) {
					//TODO
					continue
				}

				var percent: Double

				//for (; ; curve++) {
				//	val len = lenghts[curve]
				//	if (position > len) {
				//			continue
				//	}
				//	if (curve == 0) {
				//		percent = position / len
				//	}
				//	else {
				//		val preLen = lenghts[curve - 1]
				//		percent = (position - preLen) / (len - preLen)
				//	}
				//	break
				//}

				while (true) {
					val len = lenghts[curve]
					if (position > len) {
						curve++
						continue
					}
					if (curve == 0) {
						percent = position / len
					}
					else {
						val preLen = lenghts[curve - 1]
						percent = (position - preLen) / (len - preLen)
					}
					break
				}

				if (curve != preCurve) {
					preCurve = curve
					if (isClosed && curve == curveCount) {
						//计算曲线
						this._computeVertices(verticesLength - 4, 4, 0, curveVertices)
						this._computeVertices(0, 4, 4, curveVertices)
					}
					else {
						this._computeVertices(curve * 6 + 2, 8, 0, curveVertices)
					}
				}

				//
				this.addCurvePosition(percent, curveVertices[0], curveVertices[1], curveVertices[2], curveVertices[3], curveVertices[4], curveVertices[5], curveVertices[6], curveVertices[7], positions, o, tangents)
			}

			return
		}

		//匀速的
		if (isClosed) {
			verticesLength += 2
			curveVertices.lengthSet = vertexCount
			this._computeVertices(2, verticesLength - 4, 0, curveVertices)
			this._computeVertices(0, 2, verticesLength - 4, curveVertices)

			curveVertices[verticesLength - 2] = curveVertices[0]
			curveVertices[verticesLength - 1] = curveVertices[1]
		}
		else {
			curveCount--
			verticesLength -= 4
			curveVertices.lengthSet = verticesLength
			this._computeVertices(2, verticesLength, 0, curveVertices)
		}
		//
		val curves = DoubleArrayList(curveCount)
		pathLength = 0.0
		var x1 = curveVertices[0]
		var y1 = curveVertices[1]
		var cx1 = 0.0
		var cy1 = 0.0
		var cx2 = 0.0
		var cy2 = 0.0
		var x2 = 0.0
		var y2 = 0.0
		var tmpx: Double
		var tmpy: Double
		var dddfx: Double
		var dddfy: Double
		var ddfx: Double
		var ddfy: Double
		var dfx: Double
		var dfy: Double

		//for (var i = 0, w = 2; i < curveCount; i++ , w += 6) {
		for (i in 0 until curveCount) {
			val w = 2 + i * 6
			cx1 = curveVertices[w]
			cy1 = curveVertices[w + 1]
			cx2 = curveVertices[w + 2]
			cy2 = curveVertices[w + 3]
			x2 = curveVertices[w + 4]
			y2 = curveVertices[w + 5]
			tmpx = (x1 - cx1 * 2 + cx2) * 0.1875
			tmpy = (y1 - cy1 * 2 + cy2) * 0.1875
			dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.09375
			dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.09375
			ddfx = tmpx * 2 + dddfx
			ddfy = tmpy * 2 + dddfy
			dfx = (cx1 - x1) * 0.75 + tmpx + dddfx * 0.16666667
			dfy = (cy1 - y1) * 0.75 + tmpy + dddfy * 0.16666667
			pathLength += sqrt(dfx * dfx + dfy * dfy)
			dfx += ddfx
			dfy += ddfy
			ddfx += dddfx
			ddfy += dddfy
			pathLength += sqrt(dfx * dfx + dfy * dfy)
			dfx += ddfx
			dfy += ddfy
			pathLength += sqrt(dfx * dfx + dfy * dfy)
			dfx += ddfx + dddfx
			dfy += ddfy + dddfy
			pathLength += sqrt(dfx * dfx + dfy * dfy)
			curves[i] = pathLength
			x1 = x2
			y1 = y2
		}

		if (percentPosition) {
			position *= pathLength
		}
		if (percentSpacing) {
			for (i in 0 until spaceCount) {
				spaces[i] *= pathLength
			}
		}

		val segments = this._segments
		var curveLength = 0.0
		//for (var i = 0, o = 0, curve = 0, segment = 0; i < spaceCount; i++ , o += 3) {
		var curve = 0
		var segment = 0
		for (i in 0 until spaceCount) {
			val o = i * 3
			val space = spaces[i]
			position += space
			var p = position

			if (isClosed) {
				p %= pathLength
				if (p < 0) p += pathLength
				curve = 0
			} else if (p < 0) {
				continue
			} else if (p > pathLength) {
				continue
			}

			// Determine curve containing position.
			//for (; ; curve++) {
			//	val length = curves[curve]
			//	if (p > length) continue
			//	if (curve == 0)
			//			p /= length
			//	else {
			//			val prev = curves[curve - 1]
			//		p = (p - prev) / (length - prev)
			//	}
			//		break
			//}

			while (true) {
				val length = curves[curve]
				if (p > length) {
					curve++
					continue
				}
				if (curve == 0)
						p /= length
				else {
						val prev = curves[curve - 1]
					p = (p - prev) / (length - prev)
				}
					break
			}

			if (curve != preCurve) {
				preCurve = curve
				val ii = curve * 6
				x1 = curveVertices[ii]
				y1 = curveVertices[ii + 1]
				cx1 = curveVertices[ii + 2]
				cy1 = curveVertices[ii + 3]
				cx2 = curveVertices[ii + 4]
				cy2 = curveVertices[ii + 5]
				x2 = curveVertices[ii + 6]
				y2 = curveVertices[ii + 7]
				tmpx = (x1 - cx1 * 2 + cx2) * 0.03
				tmpy = (y1 - cy1 * 2 + cy2) * 0.03
				dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.006
				dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.006
				ddfx = tmpx * 2 + dddfx
				ddfy = tmpy * 2 + dddfy
				dfx = (cx1 - x1) * 0.3 + tmpx + dddfx * 0.16666667
				dfy = (cy1 - y1) * 0.3 + tmpy + dddfy * 0.16666667
				curveLength = sqrt(dfx * dfx + dfy * dfy)
				segments[0] = curveLength
				//for (ii = 1; ii < 8; ii++) {
				for (@Suppress("NAME_SHADOWING") ii in  1 until 8) {
					dfx += ddfx
					dfy += ddfy
					ddfx += dddfx
					ddfy += dddfy
					curveLength += sqrt(dfx * dfx + dfy * dfy)
					segments[ii] = curveLength
				}
				dfx += ddfx
				dfy += ddfy
				curveLength += sqrt(dfx * dfx + dfy * dfy)
				segments[8] = curveLength
				dfx += ddfx + dddfx
				dfy += ddfy + dddfy
				curveLength += sqrt(dfx * dfx + dfy * dfy)
				segments[9] = curveLength
				segment = 0
			}

			// Weight by segment length.
			p *= curveLength
			//for (; ; segment++) {
			//	val length = segments[segment]
			//	if (p > length) continue
			//	if (segment == 0)
			//		p /= length
			//	else {
			//		val prev = segments[segment - 1]
			//		p = segment + (p - prev) / (length - prev)
			//	}
			//	break
			//}

			while (true) {
				val length = segments[segment]
				if (p > length) {
					segment++
					continue
				}
				if (segment == 0)
					p /= length
				else {
					val prev = segments[segment - 1]
					p = segment + (p - prev) / (length - prev)
				}
				break
			}

			this.addCurvePosition(p * 0.1, x1, y1, cx1, cy1, cx2, cy2, x2, y2, positions, o, tangents)
		}
	}

	//Calculates a point on the curve, for a given t value between 0 and 1.
	private fun addCurvePosition(t: Double, x1: Double, y1: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, x2: Double, y2: Double, out:  DoubleArray, offset: Int, tangents: Boolean) {
		if (t == 0.0) {
			out[offset] = x1
			out[offset + 1] = y1
			out[offset + 2] = 0.0
			return
		}

		if (t == 1.0) {
			out[offset] = x2
			out[offset + 1] = y2
			out[offset + 2] = 0.0
			return
		}

		val mt = 1 - t
		val mt2 = mt * mt
		val t2 = t * t
		val a = mt2 * mt
		val b = mt2 * t * 3
		val c = mt * t2 * 3
		val d = t * t2

		val x = a * x1 + b * cx1 + c * cx2 + d * x2
		val y = a * y1 + b * cy1 + c * cy2 + d * y2

		out[offset] = x
		out[offset + 1] = y
		if (tangents) {
			//Calculates the curve tangent at the specified t value
			out[offset + 2] = atan2(y - (a * y1 + b * cy1 + c * cy2), x - (a * x1 + b * cx1 + c * cx2))
		}
		else {
			out[offset + 2] = 0.0
		}
	}

	override fun init(constraintData: ConstraintData, armature: Armature) {
		this._constraintData = constraintData
		this._armature = armature

		val data = constraintData as PathConstraintData

		this.pathOffset = data.pathDisplayData!!.geometry.offset

		//
		this.position = data.position
		this.spacing = data.spacing
		this.rotateOffset = data.rotateOffset
		this.rotateMix = data.rotateMix
		this.translateMix = data.translateMix

		//
		this._root = this._armature.getBone(data.root?.name)!!
		this._target = this._armature.getBone(data.target?.name)!!
		this._pathSlot = this._armature.getSlot(data.pathSlot?.name)

		for (i in 0 until data.bones.length) {
			val bone = this._armature.getBone(data.bones[i].name)
			if (bone != null) {
                this._bones.add(bone)
			}
		}

		if (data.rotateMode == RotateMode.ChainScale) {
			this._boneLengths = DoubleArray(this._bones.length)
		}

		this._root._hasConstraint = true
	}

	override fun update() {
		val pathSlot = this._pathSlot

		if (
			pathSlot?._geometryData == null ||
			pathSlot._geometryData?.offset != this.pathOffset
		) {
			return
		}

		val constraintData = this._constraintData as PathConstraintData

		//

		//曲线节点数据改变:父亲bone改变，权重bones改变，变形顶点改变
		var isPathVerticeDirty = false
		if (this._root._childrenTransformDirty) {
			this._updatePathVertices(pathSlot._geometryData!!)
			isPathVerticeDirty = true
		}
		else if (pathSlot._verticesDirty || pathSlot._isBonesUpdate()) {
			this._updatePathVertices(pathSlot._geometryData!!)
			pathSlot._verticesDirty = false
			isPathVerticeDirty = true
		}

		if (!isPathVerticeDirty && !this.dirty) {
			return
		}

		//
		val positionMode = constraintData.positionMode
		val spacingMode = constraintData.spacingMode
		val rotateMode = constraintData.rotateMode

		val bones = this._bones

		val isLengthMode = spacingMode == SpacingMode.Length
		val isChainScaleMode = rotateMode == RotateMode.ChainScale
		val isTangentMode = rotateMode == RotateMode.Tangent
		val boneCount = bones.length
		val spacesCount = if (isTangentMode) boneCount else boneCount + 1

		val spacing = this.spacing
		this._spaces = DoubleArray(spacesCount)
		val spaces = this._spaces

		//计曲线间隔和长度
		if (isChainScaleMode || isLengthMode) {
			//Bone改变和spacing改变触发
			spaces[0] = 0.0
			//for (var i = 0, l = spacesCount - 1; i < l; i++) {
			for (i in 0 until spacesCount - 1) {
				val bone = bones[i]
				bone.updateByConstraint()
				val boneLength = bone._boneData!!.length.toInt()
				val matrix = bone.globalTransformMatrix
				val x = boneLength * matrix.af
				val y = boneLength * matrix.bf

				val len = sqrt(x * x + y * y)
				if (isChainScaleMode) {
					this._boneLengths[i] = len.toDouble()
				}
				spaces[i + 1] = (boneLength + spacing) * len / boneLength
			}
		}
		else {
			for (i in 0 until spacesCount) {
				spaces[i] = spacing
			}
		}

		//
		this._computeBezierCurve(((pathSlot._displayFrame as DisplayFrame).rawDisplayData as PathDisplayData), spacesCount, isTangentMode, positionMode == PositionMode.Percent, spacingMode == SpacingMode.Percent)

		//根据新的节点数据重新采样
		val positions = this._positions
		var rotateOffset = this.rotateOffset
		var boneX = positions[0]
		var boneY = positions[1]
		val tip: Boolean
		if (rotateOffset == 0.0) {
			tip = rotateMode == RotateMode.Chain
		}
		else {
			tip = false
			val bone = pathSlot._parent
			if (bone != null) {
				val matrix = bone.globalTransformMatrix
				rotateOffset *= if (matrix.af * matrix.df - matrix.bf * matrix.cf > 0) TransformDb.DEG_RAD else -TransformDb.DEG_RAD
			}
		}

		//
		val rotateMix = this.rotateMix
		val translateMix = this.translateMix
		//for (var i = 0, p = 3; i < boneCount; i++ , p += 3) {
		for (i in 0 until boneCount) {
			val p = i * 3
			val bone = bones[i]
			bone.updateByConstraint()
			val matrix = bone.globalTransformMatrix
			matrix.txf += ((boneX - matrix.txf) * translateMix).toFloat()
			matrix.tyf += ((boneY - matrix.tyf) * translateMix).toFloat()

			val x = positions[p]
			val y = positions[p + 1]
			val dx = x - boneX
			val dy = y - boneY
			if (isChainScaleMode) {
				val lenght = this._boneLengths[i]

				val s = (sqrt(dx * dx + dy * dy) / lenght - 1) * rotateMix + 1
				matrix.af *= s.toFloat()
				matrix.bf *= s.toFloat()
			}

			boneX = x
			boneY = y
			if (rotateMix > 0) {
				val a = matrix.af
				val b = matrix.bf
				val c = matrix.cf
				val d = matrix.df
				var cos: Double
				var sin: Double
				var r: Double = if (isTangentMode) {
					positions[p - 1]
				} else {
					atan2(dy, dx)
				}

				r -= atan2(b, a)

				if (tip) {
					cos = cos(r)
					sin = sin(r)

					val length = bone._boneData!!.length
					boneX += (length * (cos * a - sin * b) - dx) * rotateMix
					boneY += (length * (sin * a + cos * b) - dy) * rotateMix
				}
				else {
					r += rotateOffset
				}

				if (r > TransformDb.PI) {
					r -= TransformDb.PI_D
				}
				else if (r < - TransformDb.PI) {
					r += TransformDb.PI_D
				}

				r *= rotateMix

				cos = cos(r)
				sin = sin(r)

				matrix.af = (cos * a - sin * b).toFloat()
				matrix.bf = (sin * a + cos * b).toFloat()
				matrix.cf = (cos * c - sin * d).toFloat()
				matrix.df = (sin * c + cos * d).toFloat()
			}

			bone.global.fromMatrix(matrix)
		}

		this.dirty = false
	}

	override fun invalidUpdate() {

	}
}
