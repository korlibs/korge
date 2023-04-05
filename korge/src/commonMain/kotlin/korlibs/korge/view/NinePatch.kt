package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.file.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

inline fun Container.ninePatch(
	ninePatch: NinePatchBmpSlice?, size: Size = Size(ninePatch?.widthF ?: 16f, ninePatch?.heightF ?: 16f),
	callback: @ViewDslMarker NinePatch.() -> Unit = {}
): NinePatch = NinePatch(ninePatch, size).addTo(this, callback)

inline fun Container.ninePatch(
    tex: BmpSlice, size: Size, left: Float, top: Float, right: Float, bottom: Float,
    callback: @ViewDslMarker NinePatch.() -> Unit = {}
): NinePatch = ninePatch(tex.asNinePatchSimpleRatio(left, top, right, bottom), size, callback)

class NinePatch(
	ninePatch: NinePatchBmpSlice?,
    size: Size = Size(ninePatch?.width?.toFloat() ?: 16f, ninePatch?.height?.toFloat() ?: 16f),
) : View(), ViewFileRef by ViewFileRef.Mixin() {

    override var unscaledSize: Size = size
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

		bounds = RectangleInt(0, 0, (widthD * xscale).toInt(), (heightD * yscale).toInt())

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

	override fun getLocalBoundsInternal() = Rectangle(0.0, 0.0, widthD, heightD)

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
