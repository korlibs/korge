package com.soywiz.korge.view

import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*

inline fun Container.vectorImage(shape: SizedDrawable, autoScaling: Boolean = true, callback: @ViewDslMarker VectorImage.() -> Unit = {}): VectorImage = VectorImage(shape, autoScaling).addTo(this, callback).apply { redrawIfRequired() }

class VectorImage(
    shape: SizedDrawable,
    autoScaling: Boolean = true,
) : BaseGraphics(autoScaling) {
    var shape: SizedDrawable = shape
        set(value) {
            field = value
            dirty = true
            redrawIfRequired()
        }

    override fun drawShape(ctx: Context2d) {
        ctx.draw(shape)
    }

    override fun getShapeBounds(bb: BoundsBuilder) {
        bb.add(0.0, 0.0)
        bb.add(shape.width, shape.height)
    }
}
