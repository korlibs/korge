package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.file.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

inline fun Container.ninePatch(
	ninePatch: NinePatchBmpSlice?, width: Double = ninePatch?.dwidth ?: 16.0, height: Double = ninePatch?.dheight ?: 16.0,
	callback: @ViewDslMarker NinePatch.() -> Unit = {}
): NinePatch = NinePatch(ninePatch, width, height).addTo(this, callback)

inline fun Container.ninePatch(
    tex: BmpSlice, width: Double, height: Double, left: Double, top: Double, right: Double, bottom: Double,
    callback: @ViewDslMarker NinePatch.() -> Unit = {}
): NinePatch = ninePatch(tex.asNinePatchSimpleRatio(left, top, right, bottom), width, height, callback)

class NinePatch(
	ninePatch: NinePatchBmpSlice?,
	width: Double = ninePatch?.width?.toDouble() ?: 16.0,
	height: Double = ninePatch?.height?.toDouble() ?: 16.0
) : View(), ViewFileRef by ViewFileRef.Mixin() {

    override var width: Double = width
        set(value) {
            if (field == value) return
            field = value
            invalidateRender()
        }
    override var height: Double = height
        set(value) {
            if (field == value) return
            field = value
            invalidateRender()
        }

    var ninePatch: NinePatchBmpSlice? = ninePatch
        set(value) {
            if (field === value) return
            field = value
            invalidateRender()
        }
	var smoothing = true

	private var bounds = RectangleInt()

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
        lazyLoadRenderInternal(ctx, this)

		val gm = globalMatrix

		val xscale = gm.a
		val yscale = gm.d

		bounds = RectangleInt(0, 0, (width * xscale).toInt(), (height * yscale).toInt())

        val m = gm.prescaled(1.0 / xscale, 1.0 / yscale)

        val ninePatch = ninePatch
        if (ninePatch != null) {
            recomputeVerticesIfRequired()
            val tvaCached = this@NinePatch.tvaCached
            if (tvaCached != null) {
                if (cachedMatrix != m) {
                    cachedMatrix = m
                    tvaCached.copyFrom(tva!!)
                    tvaCached.applyMatrix(m)
                }
                ctx.useBatcher { batch ->
                    batch.drawVertices(tvaCached, ctx.getTex(ninePatch.content.bmp), smoothing, blendMode)
                }
            }
        }
	}

    internal var renderedVersion = 0
    private var tva: TexturedVertexArray? = null
    private var tvaCached: TexturedVertexArray? = null
    private var cachedMatrix: Matrix = Matrix.IDENTITY
    private var cachedRenderColorMul = Colors.WHITE

    private var cachedNinePatch: NinePatchBmpSlice? = null
    private var cachedBounds = RectangleInt()

    private fun recomputeVerticesIfRequired() {
        val viewBounds = this.bounds
        if (cachedBounds == viewBounds && cachedNinePatch == ninePatch && cachedRenderColorMul == renderColorMul) return
        cachedBounds = viewBounds
        cachedNinePatch = ninePatch
        cachedRenderColorMul = renderColorMul
        val ninePatch = ninePatch
        if (ninePatch == null) {
            tva = null
            tvaCached = null
            return
        }
        val numQuads = ninePatch.info.totalSegments
        val indices = TexturedVertexArray.quadIndices(numQuads)
        val tva = TexturedVertexArray(numQuads * 4, indices)
        var index = 0
        ninePatch.info.computeScale(viewBounds) { segment, x, y, width, height ->
            val bmpSlice = ninePatch.getSegmentBmpSlice(segment)
            tva.quad(index++ * 4,
                x.toFloat(), y.toFloat(),
                width.toFloat(), height.toFloat(),
                Matrix.IDENTITY, bmpSlice, renderColorMul
            )
        }
        this.tva = tva
        this.tvaCached = TexturedVertexArray(numQuads * 4, indices)
        this.cachedMatrix = Matrix.NaN
        renderedVersion++
    }

	override fun getLocalBoundsInternal() = Rectangle(0.0, 0.0, width, height)

    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        //println("### Trying to load sourceImage=$sourceImage")
        ninePatch = try {
            currentVfs["$sourceFile"].readNinePatch()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            NinePatchBitmap32(Bitmap32(62, 62, premultiplied = true))
        }
    }

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["9.png"])
    private var imageSourceFile: String? by this::sourceFile
}
