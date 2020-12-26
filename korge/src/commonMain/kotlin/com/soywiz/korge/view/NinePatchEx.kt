package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

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
			ninePatch.info.computeScaleFixed(bounds) { segment, x, y, width, height ->
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

    private val xComputed = IntArray(64)
    private val yComputed = IntArray(64)
    // @TODO: Apply this fix to KorIM
    // @TODO: Refactor this
    fun NinePatchInfo.computeScaleFixed(
        bounds: RectangleInt,
        callback: (segment: NinePatchInfo.Segment, x: Int, y: Int, width: Int, height: Int) -> Unit
    ) {
        //println("scaleFixed=($scaleFixedX,$scaleFixedY)")

        ysegments.fastForEachWithIndex { index, _ -> yComputed[index] = Int.MAX_VALUE }
        xsegments.fastForEachWithIndex { index, _ -> xComputed[index] = Int.MAX_VALUE }

        ysegments.fastForEachWithIndex { yindex, y ->
            val segHeight = y.computedLength(this.yaxis, bounds.height)
            xsegments.fastForEachWithIndex { xindex, x ->
                val segWidth = x.computedLength(this.xaxis, bounds.width)
                if (x.fixed && y.fixed) {
                    val xScale = segWidth / x.length.toDouble()
                    val yScale = segHeight / y.length.toDouble()
                    val minScale = min(xScale, yScale)
                    xComputed[xindex] = min(xComputed[xindex], (x.length * minScale).toInt())
                    yComputed[yindex] = min(yComputed[yindex], (y.length * minScale).toInt())
                } else {
                    xComputed[xindex] = min(xComputed[xindex], segWidth.toInt())
                    yComputed[yindex] = min(yComputed[yindex], segHeight.toInt())
                }
            }
        }

        val denormalizedWidth = xComputed.sum()
        val denormalizedHeight = yComputed.sum()
        val denormalizedScaledWidth = xsegments.mapIndexed { index, it -> if (it.scaled) xComputed[index] else 0 }.sum()
        val denormalizedScaledHeight = ysegments.mapIndexed { index, it -> if (it.scaled) yComputed[index] else 0 }.sum()
        val xScaledRatio = if (denormalizedWidth > 0) denormalizedScaledWidth.toDouble() / denormalizedWidth.toDouble() else 1.0
        val yScaledRatio = if (denormalizedWidth > 0) denormalizedScaledHeight.toDouble() / denormalizedHeight.toDouble() else 1.0

        for (n in 0 until 2) {
            val segments = if (n == 0) ysegments else xsegments
            val computed = if (n == 0) yComputed else xComputed
            val denormalizedScaledLen = if (n == 0) denormalizedScaledHeight else denormalizedScaledWidth
            val side = if (n == 0) bounds.height else bounds.width
            val scaledRatio = if (n == 0) yScaledRatio else xScaledRatio
            val scaledSide = side * scaledRatio

            segments.fastForEachWithIndex { index, v ->
                if (v.scaled) {
                    computed[index] = (scaledSide * (computed[index].toDouble() / denormalizedScaledLen.toDouble())).toInt()
                }
            }
        }

        val xRemaining = bounds.width - xComputed.sum()
        val yRemaining = bounds.height - yComputed.sum()
        val xScaledFirst = xsegments.indexOfFirst { it.scaled }
        val yScaledFirst = ysegments.indexOfFirst { it.scaled }
        if (xRemaining > 0 && xScaledFirst >= 0) xComputed[xScaledFirst] += xRemaining
        if (yRemaining > 0 && yScaledFirst >= 0) yComputed[yScaledFirst] += yRemaining

        var ry = 0
        for (yindex in ysegments.indices) {
            val segHeight = yComputed[yindex].toInt()
            var rx = 0
            for (xindex in xsegments.indices) {
                val segWidth = xComputed[xindex].toInt()

                val seg = segments[yindex][xindex]
                val segLeft = (rx + bounds.left).toInt()
                val segTop = (ry + bounds.top).toInt()

                //println("($x,$y):($segWidth,$segHeight)")
                callback(seg, segLeft, segTop, segWidth.toInt(), segHeight.toInt())

                rx += segWidth
            }
            ry += segHeight
        }
    }

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0.0, 0.0, width, height)
	}

	class Tex(val tex: BitmapSlice<Bitmap>, val info: NinePatchInfo) {
		val width get() = info.width
		val height get() = info.height

		constructor(ninePatch: NinePatchBitmap32) : this(ninePatch.content, ninePatch.info)

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
