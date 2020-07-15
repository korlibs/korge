package com.dragonbones.geom
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

/**
 * - The Point object represents a location in a two-dimensional coordinate system.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Point 对象表示二维坐标系统中的某个位置。
 * @version DragonBones 3.0
 * @language zh_CN
 */
/**
 * - The horizontal coordinate.
 * @default 0.0
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - The vertical coordinate.
 * @default 0.0
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Creates a new point. If you pass no parameters to this method, a point is created at (0,0).
 * @param x - The horizontal coordinate.
 * @param y - The vertical coordinate.
 * @version DragonBones 3.0
 * @language en_US
 */

class Point
/**
 * - 创建一个 egret.Point 对象.若不传入任何参数，将会创建一个位于（0，0）位置的点。
 * @param x - 该对象的x属性值，默认为 0.0。
 * @param y - 该对象的y属性值，默认为 0.0。
 * @version DragonBones 3.0
 * @language zh_CN
 */(
	/**
	 * - 该点的水平坐标。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	override var x: Float = 0f,
	/**
	 * - 该点的垂直坐标。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	override var y: Float = 0f
) : XY {
	/**
	 * @private
	 */
	fun copyFrom(value: Point): Unit {
		this.x = value.x
		this.y = value.y
	}

	/**
	 * @private
	 */
	fun clear(): Unit {
		this.x = 0f
		this.y = 0f
	}
}
