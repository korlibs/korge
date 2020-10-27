package com.dragonbones.geom

import com.soywiz.korma.geom.*
import kotlin.math.*

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
 * - Apply a matrix transformation to a specific point.
 * @param x - X coordinate.
 * @param y - Y coordinate.
 * @param result - The point after the transformation is applied.
 * @param delta - Whether to ignore tx, ty's conversion to point.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 将矩阵转换应用于特定点。
 * @param x - 横坐标。
 * @param y - 纵坐标。
 * @param result - 应用转换之后的点。
 * @param delta - 是否忽略 tx，ty 对点的转换。
 * @version DragonBones 3.0
 * @language zh_CN
 */
fun Matrix.transformPointDb(x: Float, y: Float, result: XYf, delta: Boolean = false): Unit {
    result.xf = this.af * x + this.cf * y
    result.yf = this.bf * x + this.df * y

    if (!delta) {
        result.xf += this.txf
        result.yf += this.tyf
    }
}

fun Matrix.transformPointDb(x: Double, y: Double, result: XYf, delta: Boolean = false): Unit = transformPointDb(x.toFloat(), y.toFloat(), result, delta)

fun Matrix.transformXDb(x: Float, y: Float): Float = (this.af * x + this.cf * y + this.txf)
fun Matrix.transformYDb(x: Float, y: Float): Float = (this.bf * x + this.df * y + this.tyf)

/**
 * @private
 */
fun Matrix.transformRectangleDb(rectangle: Rectangle, delta: Boolean = false): Unit {
    val a = this.af
    val b = this.bf
    val c = this.cf
    val d = this.df
    val tx = if (delta) 0f else this.txf
    val ty = if (delta) 0f else this.tyf

    val x = rectangle.x
    val y = rectangle.y
    val xMax = x + rectangle.width
    val yMax = y + rectangle.height

    var x0 = a * x + c * y + tx
    var y0 = b * x + d * y + ty
    var x1 = a * xMax + c * y + tx
    var y1 = b * xMax + d * y + ty
    var x2 = a * xMax + c * yMax + tx
    var y2 = b * xMax + d * yMax + ty
    var x3 = a * x + c * yMax + tx
    var y3 = b * x + d * yMax + ty

    var tmp = 0.0

    if (x0 > x1) {
        tmp = x0
        x0 = x1
        x1 = tmp
    }
    if (x2 > x3) {
        tmp = x2
        x2 = x3
        x3 = tmp
    }

    rectangle.x = floor(if (x0 < x2) x0 else x2)
    rectangle.width = ceil((if (x1 > x3) x1 else x3) - rectangle.x)

    if (y0 > y1) {
        tmp = y0
        y0 = y1
        y1 = tmp
    }
    if (y2 > y3) {
        tmp = y2
        y2 = y3
        y3 = tmp
    }

    rectangle.y = floor(if (y0 < y2) y0 else y2)
    rectangle.height = ceil((if (y1 > y3) y1 else y3) - rectangle.y)
}

