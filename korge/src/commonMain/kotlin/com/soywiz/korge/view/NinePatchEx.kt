package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.min2
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

inline fun Container.ninePatch(
	tex: NinePatchEx.Tex, width: Double, height: Double, callback: @ViewDslMarker NinePatchEx.() -> Unit
) = NinePatchEx(tex, width, height).addTo(this, callback)

inline fun Container.ninePatch(
	ninePatch: NinePatchBitmap32, width: Double = ninePatch.dwidth, height: Double = ninePatch.dheight,
	callback: @ViewDslMarker NinePatchEx.() -> Unit
) = NinePatchEx(ninePatch, width, height).addTo(this, callback)

class NinePatchEx(
	ninePatch: Tex,
	override var width: Double,
	override var height: Double
) : View(), ViewFileRef by ViewFileRef.Mixin() {
    var ninePatch: Tex = ninePatch
        set(value) {
            field = value
        }
	var smoothing = true

	private val bounds = RectangleInt()

    companion object {
		operator fun invoke(
			ninePatch: NinePatchBitmap32,
			width: Double = ninePatch.width.toDouble(), height: Double = ninePatch.height.toDouble()
		): NinePatchEx = NinePatchEx(Tex(ninePatch), width, height)
	}

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
					ctx.getTex(ninePatch.getSliceTex(segment)),
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

	class Tex(val tex: BmpSlice, val info: NinePatchInfo) {
		val width get() = info.width
		val height get() = info.height

		constructor(ninePatch: NinePatchBmpSlice) : this(ninePatch.content, ninePatch.info)

		val NinePatchInfo.Segment.tex by Extra.PropertyThis<NinePatchInfo.Segment, BmpSlice> {
			this@Tex.tex.slice(this.rect)
		}

		fun getSliceTex(s: NinePatchInfo.Segment): BmpSlice = s.tex
	}


    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        //println("### Trying to load sourceImage=$sourceImage")
        ninePatch = try {
            Tex(currentVfs["$sourceFile"].readNinePatch())
        } catch (e: Throwable) {
            Tex(NinePatchBitmap32(Bitmap32(62, 62)))
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
