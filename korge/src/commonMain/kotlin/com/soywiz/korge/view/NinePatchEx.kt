package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

inline fun Container.ninePatch(
	ninePatch: NinePatchBmpSlice, width: Double = ninePatch.dwidth, height: Double = ninePatch.dheight,
	callback: @ViewDslMarker NinePatchEx.() -> Unit = {}
) = NinePatchEx(ninePatch, width, height).addTo(this, callback)

class NinePatchEx(
	ninePatch: NinePatchBmpSlice,
	override var width: Double = ninePatch.width.toDouble(),
	override var height: Double = ninePatch.height.toDouble()
) : View(), ViewFileRef by ViewFileRef.Mixin() {
    var ninePatch: NinePatchBmpSlice = ninePatch
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
			ninePatch.info.computeScale(bounds) { segment, x, y, width, height ->
				ctx.batch.drawQuad(
					ctx.getTex(ninePatch.getSegmentBmpSlice(segment)),
					x.toFloat(), y.toFloat(),
					width.toFloat(), height.toFloat(),
					m = m,
					colorMul = renderColorMul,
					colorAdd = renderColorAdd,
					filtering = smoothing,
					blendFactors = renderBlendMode.factors
				)
			}
		}
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
