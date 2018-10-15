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
package com.dragonbones.geom

/**
 * @private
 */
class ColorTransform(
	var alphaMultiplier: Double = 1.0,
	var redMultiplier: Double = 1.0,
	var greenMultiplier: Double = 1.0,
	var blueMultiplier: Double = 1.0,
	var alphaOffset: Int = 0,
	var redOffset: Int = 0,
	var greenOffset: Int = 0,
	var blueOffset: Int = 0
) {

	fun copyFrom(value: ColorTransform) {
		this.alphaMultiplier = value.alphaMultiplier
		this.redMultiplier = value.redMultiplier
		this.greenMultiplier = value.greenMultiplier
		this.blueMultiplier = value.blueMultiplier
		this.alphaOffset = value.alphaOffset
		this.redOffset = value.redOffset
		this.greenOffset = value.greenOffset
		this.blueOffset = value.blueOffset
	}

	fun identity() {
		this.alphaMultiplier = 1.0
		this.redMultiplier = 1.0
		this.greenMultiplier = 1.0
		this.blueMultiplier = 1.0
		this.alphaOffset = 0
		this.redOffset = 0
		this.greenOffset = 0
		this.blueOffset = 0
	}
}
