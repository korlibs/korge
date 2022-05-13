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
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * - The base class of bounding box data.
 * @see dragonBones.RectangleData
 * @see dragonBones.EllipseData
 * @see dragonBones.PolygonData
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 边界框数据基类。
 * @see dragonBones.RectangleData
 * @see dragonBones.EllipseData
 * @see dragonBones.PolygonData
 * @version DragonBones 5.0
 * @language zh_CN
 */
abstract class BoundingBoxData(pool: SingleObjectPool<out BoundingBoxData>) : BaseObject(pool) {
	/**
	 * - The bounding box type.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 边界框类型。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	var type: BoundingBoxType = BoundingBoxType.None
	/**
	 * @private
	 */
	var color: Int = 0x000000
	/**
	 * @private
	 */
	var width: Double = 0.0
	/**
	 * @private
	 */
	var height: Double = 0.0

	override fun _onClear() {
		this.color = 0x000000
		this.width = 0.0
		this.height = 0.0
	}
	/**
	 * - Check whether the bounding box contains a specific point. (Local coordinate system)
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查边界框是否包含特定点。（本地坐标系）
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	abstract fun containsPoint(pX: Double, pY: Double): Boolean
	/**
	 * - Check whether the bounding box intersects a specific segment. (Local coordinate system)
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查边界框是否与特定线段相交。（本地坐标系）
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	abstract fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point? = null,
		intersectionPointB: Point? = null,
		normalRadians: Point? = null
	): Int
}

/**
 * - Cohen–Sutherland algorithm https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm
 * ----------------------
 * | 0101 | 0100 | 0110 |
 * ----------------------
 * | 0001 | 0000 | 0010 |
 * ----------------------
 * | 1001 | 1000 | 1010 |
 * ----------------------
 */
//enum class OutCode(val id: Int) {
//	InSide(0), // 0000
//	Left(1),   // 0001
//	Right(2),  // 0010
//	Top(4),    // 0100
//	Bottom(8)  // 1000
//}

object OutCode {
	const val InSide = 0  // 0000
	const val Left = 1    // 0001
	const val Right = 2   // 0010
	const val Top = 4     // 0100
	const val Bottom = 8  // 1000
}
/**
 * - The rectangle bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 矩形边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class RectangleBoundingBoxData(pool: SingleObjectPool<RectangleBoundingBoxData>) : BoundingBoxData(pool) {
	override fun toString(): String {
		return "[class dragonBones.RectangleBoundingBoxData]"
	}

	companion object {
		/**
		 * - Compute the bit code for a point (x, y) using the clip rectangle
		 */
		private fun _computeOutCode(x: Double, y: Double, xMin: Double, yMin: Double, xMax: Double, yMax: Double): Int {
			var code = OutCode.InSide  // initialised as being inside of [[clip window]]

			if (x < xMin) {             // to the left of clip window
				code = code or OutCode.Left
			} else if (x > xMax) {        // to the right of clip window
				code = code or OutCode.Right
			}

			if (y < yMin) {             // below the clip window
				code = code or OutCode.Top
			} else if (y > yMax) {        // above the clip window
				code = code or OutCode.Bottom
			}

			return code
		}

		/**
		 * @private
		 */
		fun rectangleIntersectsSegment(
			xA: Double, yA: Double, xB: Double, yB: Double,
			xMin: Double, yMin: Double, xMax: Double, yMax: Double,
			intersectionPointA:
			Point? = null,
			intersectionPointB:
			Point? = null,
			normalRadians:
			Point? = null
		): Int {
			var xA = xA
			var yA = yA
			var xB = xB
			var yB = yB
			val inSideA = xA > xMin && xA < xMax && yA > yMin && yA < yMax
			val inSideB = xB > xMin && xB < xMax && yB > yMin && yB < yMax

			if (inSideA && inSideB) {
				return -1
			}

			var intersectionCount = 0
			var outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax)
			var outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax)

			while (true) {
				if ((outcode0 or outcode1) == 0) {
					// Bitwise OR is 0. Trivially accept and get out of loop
					intersectionCount = 2
					break
				} else if ((outcode0 and outcode1) != 0) {
					// Bitwise AND is not 0. Trivially reject and get out of loop
					break
				}

				// failed both tests, so calculate the line segment to clip
				// from an outside point to an intersection with clip edge
				var x = 0.0
				var y = 0.0
				var normalRadian = 0.0

				// At least one endpoint is outside the clip rectangle; pick it.
				val outcodeOut = if (outcode0 != 0) outcode0 else outcode1

				// Now find the intersection point;
				if ((outcodeOut and OutCode.Top) != 0) {
					// point is above the clip rectangle
					x = xA + (xB - xA) * (yMin - yA) / (yB - yA)
					y = yMin

					if (normalRadians != null) {
						normalRadian = -PI * 0.5
					}
				} else if ((outcodeOut and OutCode.Bottom) != 0) {
					// point is below the clip rectangle
					x = xA + (xB - xA) * (yMax - yA) / (yB - yA)
					y = yMax

					if (normalRadians != null) {
						normalRadian = PI * 0.5
					}
				} else if ((outcodeOut and OutCode.Right) != 0) {
					// point is to the right of clip rectangle
					y = yA + (yB - yA) * (xMax - xA) / (xB - xA)
					x = xMax

					if (normalRadians != null) {
						normalRadian = 0.0
					}
				} else if ((outcodeOut and OutCode.Left) != 0) {
					// point is to the left of clip rectangle
					y = yA + (yB - yA) * (xMin - xA) / (xB - xA)
					x = xMin

					if (normalRadians != null) {
						normalRadian = PI
					}
				}

				// Now we move outside point to intersection point to clip
				// and get ready for next pass.
				if (outcodeOut == outcode0) {
					xA = x
					yA = y
					outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax)

					if (normalRadians != null) {
						normalRadians.xf = normalRadian.toFloat()
					}
				} else {
					xB = x
					yB = y
					outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax)

					if (normalRadians != null) {
						normalRadians.yf = normalRadian.toFloat()
					}
				}
			}

			if (intersectionCount != 0) {
				if (inSideA) {
					intersectionCount = 2 // 10

					if (intersectionPointA != null) {
						intersectionPointA.xf = xB.toFloat()
						intersectionPointA.yf = yB.toFloat()
					}

					if (intersectionPointB != null) {
						intersectionPointB.xf = xB.toFloat()
						intersectionPointB.yf = xB.toFloat()
					}

					if (normalRadians != null) {
						normalRadians.xf = (normalRadians.yf + PI).toFloat()
					}
				} else if (inSideB) {
					intersectionCount = 1 // 01

					if (intersectionPointA != null) {
						intersectionPointA.xf = xA.toFloat()
						intersectionPointA.yf = yA.toFloat()
					}

					if (intersectionPointB != null) {
						intersectionPointB.xf = xA.toFloat()
						intersectionPointB.yf = yA.toFloat()
					}

					if (normalRadians != null) {
						normalRadians.yf = (normalRadians.xf + PI).toFloat()
					}
				} else {
					intersectionCount = 3 // 11
					if (intersectionPointA != null) {
						intersectionPointA.xf = xA.toFloat()
						intersectionPointA.yf = yA.toFloat()
					}

					if (intersectionPointB != null) {
						intersectionPointB.xf = xB.toFloat()
						intersectionPointB.yf = yB.toFloat()
					}
				}
			}

			return intersectionCount
		}
	}

	override fun _onClear() {
		super._onClear()

		this.type = BoundingBoxType.Rectangle
	}

	/**
	 * @inheritDoc
	 */
	override fun containsPoint(pX: Double, pY: Double): Boolean {
		val widthH = this.width * 0.5
		if (pX >= -widthH && pX <= widthH) {
			val heightH = this.height * 0.5
			if (pY >= -heightH && pY <= heightH) {
				return true
			}
		}

		return false
	}

	/**
	 * @inheritDoc
	 */
	override fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point?,
		intersectionPointB: Point?,
		normalRadians: Point?
	): Int {
		val widthH = this.width * 0.5
		val heightH = this.height * 0.5
		val intersectionCount = RectangleBoundingBoxData.rectangleIntersectsSegment(
			xA, yA, xB, yB,
			-widthH, -heightH, widthH, heightH,
			intersectionPointA, intersectionPointB, normalRadians
		)

		return intersectionCount
	}
}
/**
 * - The ellipse bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 椭圆边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class EllipseBoundingBoxData(pool: SingleObjectPool<EllipseBoundingBoxData>) : BoundingBoxData(pool) {
	override fun toString(): String {
		return "[class dragonBones.EllipseData]"
	}

	companion object {
		/**
		 * @private
		 */
		fun ellipseIntersectsSegment(
			xA: Double, yA: Double, xB: Double, yB: Double,
			xC: Double, yC: Double, widthH: Double, heightH: Double,
			intersectionPointA: Point? = null,
			intersectionPointB: Point? = null,
			normalRadians: Point? = null
		): Int {
			var xA = xA
			var xB = xB
			var yA = yA
			var yB = yB
			val d = widthH / heightH
			val dd = d * d

			yA *= d
			yB *= d

			val dX = xB - xA
			val dY = yB - yA
			val lAB = sqrt(dX * dX + dY * dY)
			val xD = dX / lAB
			val yD = dY / lAB
			val a = (xC - xA) * xD + (yC - yA) * yD
			val aa = a * a
			val ee = xA * xA + yA * yA
			val rr = widthH * widthH
			val dR = rr - ee + aa
			var intersectionCount = 0

			if (dR >= 0.0) {
				val dT = sqrt(dR)
				val sA = a - dT
				val sB = a + dT
				val inSideA = if (sA < 0.0) -1 else if (sA <= lAB) 0 else 1
				val inSideB = if (sB < 0.0) -1 else if (sB <= lAB) 0 else 1
				val sideAB = inSideA * inSideB

				if (sideAB < 0) {
					return -1
				} else if (sideAB == 0) {
					if (inSideA == -1) {
						intersectionCount = 2 // 10
						xB = xA + sB * xD
						yB = (yA + sB * yD) / d

						if (intersectionPointA != null) {
							intersectionPointA.xf = xB.toFloat()
							intersectionPointA.yf = yB.toFloat()
						}

						if (intersectionPointB != null) {
							intersectionPointB.xf = xB.toFloat()
							intersectionPointB.yf = yB.toFloat()
						}

						if (normalRadians != null) {
							normalRadians.xf = atan2(yB / rr * dd, xB / rr).toFloat()
							normalRadians.yf = (normalRadians.xf + PI).toFloat()
						}
					} else if (inSideB == 1) {
						intersectionCount = 1 // 01
						xA = xA + sA * xD
						yA = (yA + sA * yD) / d

						if (intersectionPointA != null) {
							intersectionPointA.xf = xA.toFloat()
							intersectionPointA.yf = yA.toFloat()
						}

						if (intersectionPointB != null) {
							intersectionPointB.xf = xA.toFloat()
							intersectionPointB.yf = yA.toFloat()
						}

						if (normalRadians != null) {
							normalRadians.xf = atan2(yA / rr * dd, xA / rr).toFloat()
							normalRadians.yf = (normalRadians.xf + PI).toFloat()
						}
					} else {
						intersectionCount = 3 // 11

						if (intersectionPointA != null) {
							intersectionPointA.xf = (xA + sA * xD).toFloat()
							intersectionPointA.yf = ((yA + sA * yD) / d).toFloat()

							if (normalRadians != null) {
								normalRadians.xf = atan2(intersectionPointA.yf / rr * dd, intersectionPointA.xf / rr).toFloat()
							}
						}

						if (intersectionPointB != null) {
							intersectionPointB.xf = (xA + sB * xD).toFloat()
							intersectionPointB.yf = ((yA + sB * yD) / d).toFloat()

							if (normalRadians != null) {
								normalRadians.yf = atan2(intersectionPointB.yf / rr * dd, intersectionPointB.xf / rr).toFloat()
							}
						}
					}
				}
			}

			return intersectionCount
		}
	}

	override fun _onClear() {
		super._onClear()

		this.type = BoundingBoxType.Ellipse
	}

	/**
	 * @inheritDoc
	 */
	override fun containsPoint(pX: Double, pY: Double): Boolean {
		var pY = pY
		val widthH = this.width * 0.5
		if (pX >= -widthH && pX <= widthH) {
			val heightH = this.height * 0.5
			if (pY >= -heightH && pY <= heightH) {
				pY *= widthH / heightH
				return sqrt(pX * pX + pY * pY) <= widthH
			}
		}

		return false
	}

	/**
	 * @inheritDoc
	 */
	override fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point?,
		intersectionPointB: Point?,
		normalRadians: Point?
	): Int {
		val intersectionCount = EllipseBoundingBoxData.ellipseIntersectsSegment(
			xA, yA, xB, yB,
			0.0, 0.0, this.width * 0.5, this.height * 0.5,
			intersectionPointA, intersectionPointB, normalRadians
		)

		return intersectionCount
	}
}
/**
 * - The polygon bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 多边形边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class PolygonBoundingBoxData(pool: SingleObjectPool<PolygonBoundingBoxData>) : BoundingBoxData(pool) {
	override fun toString(): String {
		return "[class dragonBones.PolygonBoundingBoxData]"
	}

	/**
	 * @private
	 */
	fun polygonIntersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		vertices: DoubleArray,
		intersectionPointA: Point? = null,
		intersectionPointB: Point? = null,
		normalRadians: Point? = null
	): Int {
		var xA = xA
		var yA = yA
		if (xA == xB) xA = xB + 0.000001
		if (yA == yB) yA = yB + 0.000001

		val count = vertices.size
		val dXAB = xA - xB
		val dYAB = yA - yB
		val llAB = xA * yB - yA * xB
		var intersectionCount = 0
		var xC = vertices[count - 2].toDouble()
		var yC = vertices[count - 1].toDouble()
		var dMin = 0.0
		var dMax = 0.0
		var xMin = 0.0
		var yMin = 0.0
		var xMax = 0.0
		var yMax = 0.0

		for (i in 0 until count step 2) {
			val xD = vertices[i + 0].toDouble()
			val yD = vertices[i + 1].toDouble()

			if (xC == xD) {
				xC = xD + 0.0001
			}

			if (yC == yD) {
				yC = yD + 0.0001
			}

			val dXCD = xC - xD
			val dYCD = yC - yD
			val llCD = xC * yD - yC * xD
			val ll = dXAB * dYCD - dYAB * dXCD
			val x = (llAB * dXCD - dXAB * llCD) / ll

			if (((x >= xC && x <= xD) || (x >= xD && x <= xC)) && (dXAB == 0.0 || (x >= xA && x <= xB) || (x >= xB && x <= xA))) {
				val y = (llAB * dYCD - dYAB * llCD) / ll
				if (((y >= yC && y <= yD) || (y >= yD && y <= yC)) && (dYAB == 0.0 || (y >= yA && y <= yB) || (y >= yB && y <= yA))) {
					if (intersectionPointB != null) {
						var d = x - xA
						if (d < 0.0) {
							d = -d
						}

						if (intersectionCount == 0) {
							dMin = d
							dMax = d
							xMin = x
							yMin = y
							xMax = x
							yMax = y

							if (normalRadians != null) {
								normalRadians.xf = (atan2(yD - yC, xD - xC) - PI * 0.5).toFloat()
								normalRadians.yf = normalRadians.xf
							}
						} else {
							if (d < dMin) {
								dMin = d
								xMin = x
								yMin = y

								if (normalRadians != null) {
									normalRadians.xf = (atan2(yD - yC, xD - xC) - PI * 0.5).toFloat()
								}
							}

							if (d > dMax) {
								dMax = d
								xMax = x
								yMax = y

								if (normalRadians != null) {
									normalRadians.yf = (atan2(yD - yC, xD - xC) - PI * 0.5).toFloat()
								}
							}
						}

						intersectionCount++
					} else {
						xMin = x
						yMin = y
						xMax = x
						yMax = y
						intersectionCount++

						if (normalRadians != null) {
							normalRadians.xf = (atan2(yD - yC, xD - xC) - PI * 0.5).toFloat()
							normalRadians.yf = normalRadians.xf
						}
						break
					}
				}
			}

			xC = xD
			yC = yD
		}

		if (intersectionCount == 1) {
			if (intersectionPointA != null) {
				intersectionPointA.xf = xMin.toFloat()
				intersectionPointA.yf = yMin.toFloat()
			}

			if (intersectionPointB != null) {
				intersectionPointB.xf = xMin.toFloat()
				intersectionPointB.yf = yMin.toFloat()
			}

			if (normalRadians != null) {
				normalRadians.yf = (normalRadians.xf + PI).toFloat()
			}
		} else if (intersectionCount > 1) {
			intersectionCount++

			if (intersectionPointA != null) {
				intersectionPointA.xf = xMin.toFloat()
				intersectionPointA.yf = yMin.toFloat()
			}

			if (intersectionPointB != null) {
				intersectionPointB.xf = xMax.toFloat()
				intersectionPointB.yf = yMax.toFloat()
			}
		}

		return intersectionCount
	}

	/**
	 * @private
	 */
	var x: Double = 0.0
	/**
	 * @private
	 */
	var y: Double = 0.0
	/**
	 * - The polygon vertices.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 多边形顶点。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	var vertices: DoubleArray = DoubleArray(0)

	override fun _onClear() {
		super._onClear()

		this.type = BoundingBoxType.Polygon
		this.x = 0.0
		this.y = 0.0
		this.vertices = DoubleArray(0)
	}

	/**
	 * @inheritDoc
	 */
	override fun containsPoint(pX: Double, pY: Double): Boolean {
		var isInSide = false
		if (pX >= this.x && pX <= this.width && pY >= this.y && pY <= this.height) {
			var iP = this.vertices.size - 2
			for (i in 0 until this.vertices.size step 2) {
				val yA = this.vertices[iP + 1]
				val yB = this.vertices[i + 1]
				if ((yB < pY && yA >= pY) || (yA < pY && yB >= pY)) {
					val xA = this.vertices[iP]
					val xB = this.vertices[i]
					if ((pY - yB) * (xA - xB) / (yA - yB) + xB < pX) {
						isInSide = !isInSide
					}
				}

				iP = i
			}
		}

		return isInSide
	}

	/**
	 * @inheritDoc
	 */
	override fun intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: Point?,
		intersectionPointB: Point?,
		normalRadians: Point?
	): Int {
		var intersectionCount = 0
		if (RectangleBoundingBoxData.rectangleIntersectsSegment(
				xA,
				yA,
				xB,
				yB,
				this.x,
				this.y,
				this.x + this.width,
				this.y + this.height,
				null,
				null,
				null
			) != 0
		) {
			intersectionCount = polygonIntersectsSegment(
				xA, yA, xB, yB,
				this.vertices,
				intersectionPointA, intersectionPointB, normalRadians
			)
		}

		return intersectionCount
	}
}
