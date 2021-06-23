package com.soywiz.korge.view.grid

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

interface Grid {
    fun snap(point: Point, out: Point = Point()): Point
    fun draw(ctx: DebugLineRenderContext, rect: RectangleInt)
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

    override fun draw(ctx: DebugLineRenderContext, rect: RectangleInt) {
        for (x in rect.left until rect.right step width) ctx.line(x, rect.top, x, rect.bottom)
        for (y in rect.top until rect.bottom step height) ctx.line(rect.left, y, rect.right, y)
    }
}
