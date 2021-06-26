package com.soywiz.korge.view.grid

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

interface Grid {
    fun snap(point: Point, out: Point = Point()): Point
    fun draw(ctx: RenderContext, width: Double, height: Double, m: Matrix)
}

open class OrthographicGrid(
    var width: Int,
    var height: Int,
) : Grid {
    var size: Int
        get() = (width + height) / 2
        set(value) {
            width = value
            height = value
        }

    override fun snap(point: Point, out: Point): Point {
        return out.setTo(
            point.x.nearestAlignedTo(width.toDouble()),
            point.y.nearestAlignedTo(height.toDouble()),
        )
    }

    override fun draw(ctx: RenderContext, width: Double, height: Double, m: Matrix) {
        val gridWidth = this.width
        val gridHeight = this.height
        val width = width.toInt()
        val height = height.toInt()
        val ctxLine = ctx.debugLineRenderContext
        val matrix = m
        val transform = matrix.toTransform()
        ctxLine.draw(matrix) {
            val rect = RectangleInt(0, 0, width.toInt(), height.toInt())
            val dx = transform.scaleX * gridWidth
            val dy = transform.scaleY * gridHeight
            //println("dxy: $dx, $dy")
            val smallX = dx < 3
            val smallY = dy < 3
            if (!smallX && !smallY) {
                ctxLine.drawVector(Colors["#d3d3d367"]) {
                    //ctxLine.drawVector(Colors.RED) {
                    for (x in rect.left until rect.right step gridWidth) ctxLine.line(x, rect.top, x, rect.bottom)
                    for (y in rect.top until rect.bottom step gridHeight) ctxLine.line(rect.left, y, rect.right, y)
                }
            }
            ctxLine.drawVector(Colors.BLACK) {
                rect(rect)
            }
            ctxLine.drawVector(Colors.WHITE) {
                rect(rect.left - 1, rect.top - 1, rect.width + 2, rect.height + 2)
            }
        }
    }
}
