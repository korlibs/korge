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
import com.soywiz.kds.iterators.*
import com.dragonbones.model.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * @internal
 */
class Surface(pool: SingleObjectPool<out Surface>) :  Bone(pool) {
	override fun toString(): String {
		return "[class dragonBones.Surface]"
	}

	private var _dX: Float = 0f
	private var _dY: Float = 0f
	private var _k: Float = 0f
	private var _kX: Float = 0f
	private var _kY: Float = 0f

	var _vertices:  FloatArray = FloatArray(0)
	var _deformVertices:  FloatArray = FloatArray(0)
	/**
	 * - x1, y1, x2, y2, x3, y3, x4, y4, d1X, d1Y, d2X, d2Y
	 */
	//private val _hullCache:  FloatArray = FloatArray(12)
	private var _hullCache0 = 0f
	private var _hullCache1 = 0f
	private var _hullCache2 = 0f
	private var _hullCache3 = 0f
	private var _hullCache4 = 0f
	private var _hullCache5 = 0f
	private var _hullCache6 = 0f
	private var _hullCache7 = 0f
	private var _hullCache8 = 0f
	private var _hullCache9 = 0f
	private var _hullCache10 = 0f
	private var _hullCache11 = 0f
	/**
	 * - Inside [flag, a, b, c, d, tx, ty], Outside [flag, a, b, c, d, tx, ty]
	 */
	private var _matrixCache = FloatArray(0)

	var _bone: Bone? = null

	override fun _onClear() {
		super._onClear()

		_dX = 0f
		_dY = 0f
		_k = 0f
		_kX = 0f
		_kY = 0f
		_vertices = FloatArray(0)
		_deformVertices = FloatArray(0)
		_matrixCache = FloatArray(0)
		_bone = null
	}

	private fun _getAffineTransform(
        x: Float, y: Float, lX: Float, lY: Float,
        aX: Float, aY: Float, bX: Float, bY: Float, cX: Float, cY: Float,
        transform: TransformDb, matrix: Matrix, isDown: Boolean
	) {
		val dabX = bX - aX
		val dabY = bY - aY
		val dacX = cX - aX
		val dacY = cY - aY

		transform.rotation = atan2(dabY, dabX)
		transform.skew = (atan2(dacY, dacX) - PI * 0.5 - transform.rotation).toFloat()

		if (isDown) {
			transform.rotation += PI.toFloat()
		}

		transform.scaleX = sqrt(dabX * dabX + dabY * dabY) / lX
		transform.scaleY = sqrt(dacX * dacX + dacY * dacY) / lY
		transform.toMatrix(matrix)
		val rx = aX - (matrix.af * x + matrix.cf * y)
		transform.xf = rx
		matrix.txf = rx
		val ry = aY - (matrix.bf * x + matrix.df * y)
		transform.yf = ry
		matrix.tyf = ry
	}

	private fun _updateVertices() {
		val data = _armature!!.armatureData.parent!!
		val geometry = _boneData!!.geometry
		val intArray = data.intArray!!
		val floatArray = data.floatArray!!
		val vertexCount = intArray[geometry.offset + BinaryOffset.GeometryVertexCount]
		val verticesOffset = intArray[geometry.offset + BinaryOffset.GeometryFloatOffset]
		val vertices = _vertices
		val animationVertices = _deformVertices
        val _parent = _parent

		if (_parent != null) {
			if (_parent._boneData?.isSurface == true) {
				//for (var i = 0, l = vertexCount; i < l; ++i) {
				val surface = _parent as Surface
				for (i in 0 until vertexCount) {
					val iD = i * 2
					val x = floatArray[verticesOffset + iD + 0] + animationVertices[iD + 0]
					val y = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1]
					val matrix = surface._getGlobalTransformMatrix(x, y)
					vertices[iD + 0] = matrix.transformXf(x, y)
					vertices[iD + 1] = matrix.transformYf(x, y)
				}
			}
			else {
				val parentMatrix = _parent.globalTransformMatrix
				//for (var i = 0, l = vertexCount; i < l; ++i) {
				for (i in 0 until vertexCount) {
					val iD = i * 2
					val x = floatArray[verticesOffset + iD + 0] + animationVertices[iD + 0]
					val y = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1]
					vertices[iD + 0] = parentMatrix.transformXf(x, y)
					vertices[iD + 1] = parentMatrix.transformYf(x, y)
				}
			}
		}
		else {
			//for (var i = 0, l = vertexCount; i < l; ++i) {
			for (i in 0 until vertexCount) {
				val iD = i * 2
				vertices[iD + 0] = floatArray[verticesOffset + iD + 0] + animationVertices[iD + 0]
				vertices[iD + 1] = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1]
			}
		}
	}

	override fun _updateGlobalTransformMatrix(isCache: Boolean) {
		// tslint:disable-next-line:no-unused-expression
		//isCache

		val segmentXD = _boneData!!.segmentX * 2
		val lastIndex = _vertices.size - 2
		val lA = 200f
		//
		val raX = _vertices[0]
		val raY = _vertices[1]
		val rbX = _vertices[segmentXD + 0]
		val rbY = _vertices[segmentXD + 1]
		val rcX = _vertices[lastIndex + 0]
		val rcY = _vertices[lastIndex + 1]
		val rdX = _vertices[lastIndex - segmentXD + 0]
		val rdY = _vertices[lastIndex - segmentXD + 1]
		//
		val dacX = raX + (rcX - raX) * 0.5f
		val dacY = raY + (rcY - raY) * 0.5f
		val dbdX = rbX + (rdX - rbX) * 0.5f
		val dbdY = rbY + (rdY - rbY) * 0.5f
		val aX = dacX + (dbdX - dacX) * 0.5f
		val aY = dacY + (dbdY - dacY) * 0.5f
		val bX = rbX + (rcX - rbX) * 0.5f
		val bY = rbY + (rcY - rbY) * 0.5f
		val cX = rdX + (rcX - rdX) * 0.5f
		val cY = rdY + (rcY - rdY) * 0.5f
		// TODO interpolation
		_getAffineTransform(0f, 0f,
			lA, lA, aX, aY, bX, bY, cX, cY,
			global, globalTransformMatrix, false
		)
		_globalDirty = false
	}

	fun _getGlobalTransformMatrix(x: Float, y: Float): Matrix {
		val lA = 200f
		val lB = 1000f
		if (x < -lB || lB < x || y < -lB || lB < y) {
			return globalTransformMatrix
		}

		val isDown: Boolean
		val surfaceData = _boneData!!
		val segmentX = surfaceData.segmentX
		val segmentY = surfaceData.segmentY
		val segmentXD = surfaceData.segmentX * 2
		val dX = _dX
		val dY = _dY
		val indexX = ((x + lA) / dX).toInt() // -1 ~ segmentX - 1
		val indexY = ((y + lA) / dY).toInt() // -1 ~ segmentY - 1
		//println("x=" + x + "lA=" + lA + "dX=" + dX);
		val matrixIndex: Int
		val pX = indexX * dX - lA
		val pY = indexY * dY - lA

		//
		val matrices = _matrixCache
		val helpMatrix = _helpMatrix

		if (x < -lA) {
			if (y < -lA || y >= lA) { // Out.
				return globalTransformMatrix
			}
			// Left.
			isDown = y > _kX * (x + lA) + pY
			matrixIndex = ((segmentX * segmentY + segmentX + segmentY + segmentY + indexY) * 2 + (if (isDown) 1 else 0)) * 7

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1)
			}
			else {
				val vertexIndex = indexY * (segmentXD + 2)
				val ddX = _hullCache4
				val ddY = _hullCache5
				val sX = _hullCache2 - (segmentY - indexY) * ddX
				val sY = _hullCache3 - (segmentY - indexY) * ddY
				val vertices = _vertices

				if (isDown) {
					_getAffineTransform(
						(-lA), (pY + dY), (lB - lA), dY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						_helpTransform, helpMatrix, true)
				}
				else {
					_getAffineTransform(
						(-lB), pY, (lB - lA), dY,
						sX,
						sY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX + ddX,
						sY + ddY,
						_helpTransform, helpMatrix, false)
				}

				setMatricesFromHelp(matrices, matrixIndex, helpMatrix)
			}
		}
		else if (x >= lA) {
			if (y < -lA || y >= lA) { // Out.
				return globalTransformMatrix
			}
			// Right.
			isDown = y > _kX * (x - lB) + pY
			matrixIndex = ((segmentX * segmentY + segmentX + indexY) * 2 + (if (isDown) 1 else 0)) * 7

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1)
			}
			else {
				val vertexIndex = (indexY + 1) * (segmentXD + 2) - 2
				val ddX = _hullCache4
				val ddY = _hullCache5
				val sX = _hullCache0 + indexY * ddX
				val sY = _hullCache1 + indexY * ddY
				val vertices = _vertices

				if (isDown) {
					_getAffineTransform(
						lB, (pY + dY), (lB - lA), dY,
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						sX,
						sY,
						_helpTransform, helpMatrix, true)
				}
				else {
					_getAffineTransform(
						lA, pY, (lB - lA), dY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX,
						sY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						_helpTransform, helpMatrix, false)
				}

				setMatricesFromHelp(matrices, matrixIndex, helpMatrix)
			}
		}
		else if (y < -lA) {
			if (x < -lA || x >= lA) { // Out.
				return globalTransformMatrix
			}
			// Up.
			isDown = y > _kY * (x - pX - dX) - lB
			matrixIndex = ((segmentX * segmentY + indexX) * 2 + (if (isDown) 1 else 0)) * 7

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1)
			}
			else {
				val vertexIndex = indexX * 2
				val ddX = _hullCache10
				val ddY = _hullCache11
				val sX = _hullCache8 + indexX * ddX
				val sY = _hullCache9 + indexX * ddY
				val vertices = _vertices

				if (isDown) {
					_getAffineTransform(
						(pX + dX), (-lA), dX, (lB - lA),
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX + ddX,
						sY + ddY,
						_helpTransform, helpMatrix, true)
				}
				else {
					_getAffineTransform(
						pX, (-lB), dX, (lB - lA),
						sX,
						sY,
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						_helpTransform, helpMatrix, false)
				}

				setMatricesFromHelp(matrices, matrixIndex, helpMatrix)
			}
		}
		else if (y >= lA) {
			if (x < -lA || x >= lA) { //  Out.
				return globalTransformMatrix
			}
			// Down
			isDown = y > _kY * (x - pX - dX) + lA
			matrixIndex = ((segmentX * segmentY + segmentX + segmentY + indexX) * 2 + (if (isDown) 1 else 0)) * 7

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1)
			}
			else {
				val vertexIndex = segmentY * (segmentXD + 2) + indexX * 2
				val ddX = _hullCache10
				val ddY = _hullCache11
				val sX = _hullCache6 - (segmentX - indexX) * ddX
				val sY = _hullCache7 - (segmentX - indexX) * ddY
				val vertices = _vertices

				if (isDown) {
					_getAffineTransform(
						(pX + dX), lB, dX, (lB - lA),
						sX + ddX,
						sY + ddY,
						sX,
						sY,
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						_helpTransform, helpMatrix, true)
				}
				else {
					_getAffineTransform(
						pX, lA, dX, (lB - lA),
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						sX,
						sY,
						_helpTransform, helpMatrix, false)
				}

				setMatricesFromHelp(matrices, matrixIndex, helpMatrix)
			}
		}
		else { // Center.
			isDown = y > _k * (x - pX - dX) + pY
			matrixIndex = ((segmentX * indexY + indexX) * 2 + (if (isDown) 1 else 0)) * 7

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1)
			}
			else {
				val vertexIndex = indexX * 2 + indexY * (segmentXD + 2)
				val vertices = _vertices

				if (isDown) {
					_getAffineTransform(
						(pX + dX), (pY + dY), dX, dY,
						vertices[vertexIndex + segmentXD + 4],
						vertices[vertexIndex + segmentXD + 5],
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						_helpTransform, helpMatrix, true)
				}
				else {
					_getAffineTransform(
						pX, pY, dX, dY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						_helpTransform, helpMatrix, false)
				}

				setMatricesFromHelp(matrices, matrixIndex, helpMatrix)
			}
		}

		return helpMatrix
	}

	private fun setMatricesFromHelp(
		matrices: FloatArray,
		matrixIndex: Int,
		helpMatrix: Matrix
	) {
		matrices[matrixIndex] = 1f
		matrices[matrixIndex + 1] = helpMatrix.af
		matrices[matrixIndex + 2] = helpMatrix.bf
		matrices[matrixIndex + 3] = helpMatrix.cf
		matrices[matrixIndex + 4] = helpMatrix.df
		matrices[matrixIndex + 5] = helpMatrix.txf
		matrices[matrixIndex + 6] = helpMatrix.tyf
	}

	/**
	 * @internal
	 * @private
	 */
	override fun init(boneData: BoneData, armatureValue: Armature) {
		val surfaceData = boneData
		if (_boneData != null) {
			return
		}

		super.init(surfaceData, armatureValue)

		val segmentX = surfaceData.segmentX
		val segmentY = surfaceData.segmentY
		val vertexCount =
			_armature!!.armatureData.parent!!.intArray!![surfaceData.geometry.offset + BinaryOffset.GeometryVertexCount]
		val lB = 1000f
		val lA = 200f
		//
		_dX = lA * 2f / segmentX
		_dY = lA * 2f / segmentY
		//println("Surface.Init: dX=$_dX, dY=$_dY")
		_k = -_dY / _dX
		_kX = -_dY / (lB - lA)
		_kY = -(lB - lA) / _dX
		_vertices = FloatArray(vertexCount * 2)
		_deformVertices = FloatArray(vertexCount * 2)
		_matrixCache = FloatArray((segmentX * segmentY + segmentX * 2 + segmentY * 2) * 2 * 7)

		for (i in 0 until vertexCount * 2) {
			_deformVertices[i] = 0f
		}

		if (_parent != null) {
			if (_parent?.boneData?.isBone == true) {
				_bone = _parent
			}
			else {
				_bone = (_parent as Surface)._bone
			}
		}
	}

	/**
	 * @internal
	 */
	override fun update(cacheFrameIndex: Int) {
		@Suppress("NAME_SHADOWING")
		var cacheFrameIndex = cacheFrameIndex
		if (cacheFrameIndex >= 0 && _cachedFrameIndices != null) {
			val cachedFrameIndex = _cachedFrameIndices!![cacheFrameIndex]
			if (cachedFrameIndex >= 0 && _cachedFrameIndex == cachedFrameIndex) { // Same cache.
				_transformDirty = false
			}
			else if (cachedFrameIndex >= 0) { // Has been Cached.
				_transformDirty = true
				_cachedFrameIndex = cachedFrameIndex
			}
			else {
				if (_hasConstraint) { // Update constraints.
					_armature!!._constraints.fastForEach { constraint ->
						if (constraint._root == this) {
							constraint.update()
						}
					}
				}

				if (
					_transformDirty ||
					(_parent != null && _parent!!._childrenTransformDirty)
				) { // Dirty.
					_transformDirty = true
					_cachedFrameIndex = -1
				}
				else if (_cachedFrameIndex >= 0) { // Same cache, but not set index yet.
					_transformDirty = false
					_cachedFrameIndices!![cacheFrameIndex] = _cachedFrameIndex
				}
				else { // Dirty.
					_transformDirty = true
					_cachedFrameIndex = -1
				}
			}
		}
		else {
			if (_hasConstraint) { // Update constraints.
				_armature!!._constraints.fastForEach { constraint ->
					if (constraint._root == this) {
						constraint.update()
					}
				}
			}

			if (_transformDirty || (_parent != null && _parent!!._childrenTransformDirty)) { // Dirty.
				cacheFrameIndex = -1
				_transformDirty = true
				_cachedFrameIndex = -1
			}
		}

		if (_transformDirty) {
			_transformDirty = false
			_childrenTransformDirty = true
			//
			for (i in 0 until _matrixCache.size step 7) {
				_matrixCache[i] = -1f
			}
			//
			_updateVertices()
			//
			if (_cachedFrameIndex < 0) {
				val isCache = cacheFrameIndex >= 0
				if (_localDirty) {
					_updateGlobalTransformMatrix(isCache)
				}

				if (isCache && _cachedFrameIndices != null) {
					val res = _armature!!._armatureData!!.setCacheFrame(globalTransformMatrix, global)
					_cachedFrameIndex = res
					_cachedFrameIndices!![cacheFrameIndex] = res
				}
			}
			else {
				_armature?._armatureData?.getCacheFrame(globalTransformMatrix, global, _cachedFrameIndex)
			}
			// Update hull vertices.
			val lB = 1000f
			val lA = 200f
			val ddX = 2 * global.xf
			val ddY = 2 * global.yf
			//
			val helpPoint = _helpPoint
			globalTransformMatrix.transform(lB, -lA, helpPoint)
			_hullCache0 = helpPoint.xf
			_hullCache1 = helpPoint.yf
			_hullCache2 = ddX - helpPoint.xf
			_hullCache3 = ddY - helpPoint.yf
			globalTransformMatrix.deltaTransformPoint(0f, _dY, helpPoint)
			_hullCache4 = helpPoint.xf
			_hullCache5 = helpPoint.yf
			//
			globalTransformMatrix.transform(lA, lB, helpPoint)
			_hullCache6 = helpPoint.xf
			_hullCache7 = helpPoint.yf
			_hullCache8 = ddX - helpPoint.xf
			_hullCache9 = ddY - helpPoint.yf
			globalTransformMatrix.deltaTransformPoint(_dX, 0f, helpPoint)
			_hullCache10 = helpPoint.xf
			_hullCache11 = helpPoint.yf
		}
		else if (_childrenTransformDirty) {
			_childrenTransformDirty = false
		}

		_localDirty = true
	}
}
