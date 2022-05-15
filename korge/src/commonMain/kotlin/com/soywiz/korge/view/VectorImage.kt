package com.soywiz.korge.view

import com.soywiz.korge.debug.UiTextEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readVectorImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.SizedDrawable
import com.soywiz.korim.vector.buildShape
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korui.UiContainer

inline fun Container.vectorImage(shape: SizedDrawable, autoScaling: Boolean = true, callback: @ViewDslMarker VectorImage.() -> Unit = {}): VectorImage = VectorImage(shape, autoScaling).addTo(this, callback).apply { redrawIfRequired() }

class VectorImage(
    shape: SizedDrawable,
    autoScaling: Boolean = true,
) : BaseGraphics(autoScaling), ViewFileRef by ViewFileRef.Mixin() {
    companion object {
        fun createDefault() = VectorImage(buildShape { fill(Colors.WHITE) { rect(0, 0, 100, 100) } })
    }

    var shape: SizedDrawable = shape
        set(value) {
            if (field !== value) {
                field = value
                dirty()
                redrawIfRequired()
                scale = 1.0
            }
        }

    override fun drawShape(ctx: Context2d) {
        ctx.draw(shape)
    }

    override fun getShapeBounds(bb: BoundsBuilder, includeStrokes: Boolean) {
        bb.add(0.0, 0.0)
        bb.add(shape.width, shape.height)
    }

    override fun renderInternal(ctx: RenderContext) {
        lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        val vector = currentVfs["$sourceFile"].readVectorImage()
        println("VECTOR: $vector")
        shape = vector
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("VectorImage") {
            uiEditableValue(this@VectorImage::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "svg"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
