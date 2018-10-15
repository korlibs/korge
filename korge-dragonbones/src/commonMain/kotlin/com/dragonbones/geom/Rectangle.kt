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
 * - A Rectangle object is an area defined by its position, as indicated by its top-left corner point (x, y) and by its
 * width and its height.<br/>
 * The x, y, width, and height properties of the Rectangle class are independent of each other; changing the value of
 * one property has no effect on the others. However, the right and bottom properties are integrally related to those
 * four properties. For example, if you change the value of the right property, the value of the width property changes;
 * if you change the bottom property, the value of the height property changes.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - Rectangle 对象是按其位置（由它左上角的点 (x, y) 确定）以及宽度和高度定义的区域。<br/>
 * Rectangle 类的 x、y、width 和 height 属性相互独立；更改一个属性的值不会影响其他属性。
 * 但是，right 和 bottom 属性与这四个属性是整体相关的。例如，如果更改 right 属性的值，则 width
 * 属性的值将发生变化；如果更改 bottom 属性，则 height 属性的值将发生变化。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Rectangle
/**
 * @private
 */(
	/**
	 * - 矩形左上角的 x 坐标。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - The x coordinate of the top-left corner of the rectangle.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var x: Double = 0.0,
	/**
	 * - 矩形左上角的 y 坐标。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - The y coordinate of the top-left corner of the rectangle.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var y: Double = 0.0,
	/**
	 * - 矩形的宽度（以像素为单位）。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - The width of the rectangle, in pixels.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var width: Double = 0.0,
	/**
	 * - The height of the rectangle, in pixels.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - 矩形的高度（以像素为单位）。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var height: Double = 0.0
) {
	/**
	 * @private
	 */
	fun copyFrom(value: Rectangle) {
		this.x = value.x
		this.y = value.y
		this.width = value.width
		this.height = value.height
	}

	/**
	 * @private
	 */
	fun clear() {
		this.x = 0.0
		this.y = 0.0
		this.width = 0.0
		this.height = 0.0
	}
}
