package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.vector.*
import korlibs.io.file.*
import korlibs.math.geom.*

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
                scaleD = 1.0
            }
        }

    override fun drawShape(ctx: Context2d) {
        ctx.draw(shape)
    }

    override fun getShapeBounds(includeStrokes: Boolean): Rectangle {
        return Rectangle(0, 0, shape.width, shape.height)
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

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["svg"])
    private var imageSourceFile: String? by this::sourceFile
}
