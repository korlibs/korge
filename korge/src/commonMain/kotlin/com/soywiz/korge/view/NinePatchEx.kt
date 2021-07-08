package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

inline fun Container.ninePatch(
	ninePatch: NinePatchBmpSlice?, width: Double = ninePatch?.dwidth ?: 16.0, height: Double = ninePatch?.dheight ?: 16.0,
	callback: @ViewDslMarker NinePatchEx.() -> Unit = {}
) = NinePatchEx(ninePatch, width, height).addTo(this, callback)

class NinePatchEx(
	ninePatch: NinePatchBmpSlice?,
	override var width: Double = ninePatch?.width?.toDouble() ?: 16.0,
	override var height: Double = ninePatch?.height?.toDouble() ?: 16.0
) : View(), ViewFileRef by ViewFileRef.Mixin() {
    var ninePatch: NinePatchBmpSlice? = ninePatch
	var smoothing = true

	private val bounds = RectangleInt()

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
        lazyLoadRenderInternal(ctx, this)

		val m = globalMatrix

		val xscale = m.a
		val yscale = m.d

		bounds.setTo(0, 0, (width * xscale).toInt(), (height * yscale).toInt())

		m.keep {
			prescale(1.0 / xscale, 1.0 / yscale)
            val ninePatch = ninePatch
            if (ninePatch != null) {
                recomputeVerticesIfRequired()
                val tva = this@NinePatchEx.tva
                if (tva != null) {
                    ctx.batch.drawVertices(tva, ctx.getTex(ninePatch.content.bmp), smoothing, blendMode.factors, matrix = m)
                }
            }
		}
	}

    private var tva: TexturedVertexArray? = null

    private var cachedNinePatch: NinePatchBmpSlice? = null
    private val cachedBounds = RectangleInt()

    private fun recomputeVerticesIfRequired() {
        val viewBounds = this.bounds
        if (cachedBounds.rect == viewBounds.rect && cachedNinePatch == ninePatch) return
        cachedBounds.rect.copyFrom(viewBounds.rect)
        cachedNinePatch = ninePatch
        val ninePatch = ninePatch
        if (ninePatch == null) {
            tva = null
            return
        }
        val numQuads = ninePatch.info.totalSegments
        val tva = TexturedVertexArray(numQuads * 4, TexturedVertexArray.quadIndices(numQuads))
        var index = 0
        val matrix = Matrix()
        ninePatch.info.computeScale(viewBounds) { segment, x, y, width, height ->
            val bmpSlice = ninePatch.getSegmentBmpSlice(segment)
            tva.quad(index++ * 4,
                x.toFloat(), y.toFloat(),
                width.toFloat(), height.toFloat(),
                matrix, bmpSlice, renderColorMul, renderColorAdd
            )
        }
        this.tva = tva
    }

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0.0, 0.0, width, height)
	}

    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        //println("### Trying to load sourceImage=$sourceImage")
        ninePatch = try {
            currentVfs["$sourceFile"].readNinePatch()
        } catch (e: Throwable) {
            NinePatchBitmap32(Bitmap32(62, 62))
        }
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("9-PatchImage") {
            uiEditableValue(::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.baseName.endsWith(".9.png")
            })
        }
        super.buildDebugComponent(views, container)
    }
}
