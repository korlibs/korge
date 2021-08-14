package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

inline fun Container.outline(vectorPath: VectorPath, x: Double = 0.0, y: Double = 0.0, color: RGBA = Colors.WHITE, callback: @ViewDslMarker Outline.() -> Unit = {})
    = Outline(vectorPath, x, y, color).addTo(this, callback)

class Outline(
    vectorPath: VectorPath,
    x: Double = 0.0,
    y: Double = 0.0,
    color: RGBA = Colors.WHITE,
) : View() {
    var vectorPath: VectorPath = vectorPath
        set(value) {
            field = value
            invalidateVectorPath()
        }

    init {
        this.x = x
        this.y = y
        colorMul = color
    }

    override fun renderInternal(ctx: RenderContext) {
        ctx.useLineBatcher { debugLine ->
            debugLine.color(renderColorMul) {
                debugLine.drawVector(vectorPath)
            }
        }
    }

    private fun invalidateVectorPath() {
    }
}
