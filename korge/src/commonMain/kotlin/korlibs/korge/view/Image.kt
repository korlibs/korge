package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.io.resources.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

inline fun Container.image(
	texture: Resourceable<out BaseBmpSlice>, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchor).addTo(this, callback)

inline fun Container.image(
    texture: BitmapCoords, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchor).addTo(this, callback)

inline fun Container.image(
	texture: Bitmap, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchor).addTo(this, callback)

//typealias Sprite = Image

open class BaseImage(
    bitmap: Resourceable<out BitmapCoords>,
    anchor: Anchor = Anchor.TOP_LEFT,
    hitShape: VectorPath? = null,
    smoothing: Boolean = true
) : RectBase(anchor, hitShape, smoothing) {
    constructor(
        bitmap: Bitmap,
        anchor: Anchor = Anchor.TOP_LEFT,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(Resourceable(bitmap.slice()), anchor, hitShape, smoothing)
    constructor(
        bmpCoords: BitmapCoords,
        anchor: Anchor = Anchor.TOP_LEFT,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(Resourceable(bmpCoords), anchor, hitShape, smoothing)

    private var setBitmapSource: Boolean = false

    var bitmap: BitmapCoords
        get() = baseBitmap
        set(value) {
            if (baseBitmap == value) return
            setBitmapSource = true
            baseBitmap = value
            //invalidate() // Already done in baseBitmap
        }

    // @TODO: We might want to repaint when the source has been loaded
    var bitmapSrc: Resourceable<out BitmapCoords> = bitmap
        set(value) {
            if (field == value) return
            setBitmapSource = false
            field = value
            trySetSource()
            invalidate()
        }

    fun trySetSource() {
        if (setBitmapSource) return
        trySetSourceActually()
    }

    private fun trySetSourceActually() {
        val source = bitmapSrc.getOrNull()
        if (source != null) {
            setBitmapSource = true
            this.baseBitmap = source
        }
    }

    init {
        trySetSource()
    }

    override fun renderInternal(ctx: RenderContext) {
        trySetSource()
        super.renderInternal(ctx)
    }

    /*
    override val bwidth: Double get() = baseBitmap.width.toDouble()
    override val bheight: Double get() = baseBitmap.height.toDouble()
    override val anchorDispX get() = (anchorX * baseBitmap.frameWidth.toDouble() - baseBitmap.frameOffsetX.toDouble())
    override val anchorDispY get() = (anchorY * baseBitmap.frameHeight.toDouble() - baseBitmap.frameOffsetY.toDouble())
     */

    //override val bwidth: Double get() = baseBitmap.frameWidth.toDouble()
    //override val bheight: Double get() = baseBitmap.frameHeight.toDouble()
    override val bwidth: Float get() = baseBitmap.width.toFloat()
    override val bheight: Float get() = baseBitmap.height.toFloat()

    open val frameOffsetX: Float get() = baseBitmap.frameOffsetX.toFloat()
    open val frameOffsetY: Float get() = baseBitmap.frameOffsetY.toFloat()
    open val frameWidth: Float get() = baseBitmap.frameWidth.toFloat()
    open val frameHeight: Float get() = baseBitmap.frameHeight.toFloat()

    open val anchorDispXNoOffset: Float get() = (anchor.sx * frameWidth)
    open val anchorDispYNoOffset: Float get() = (anchor.sy * frameHeight)

    override val anchorDispX: Float get() = (anchorDispXNoOffset - frameOffsetX)
    override val anchorDispY: Float get() = (anchorDispYNoOffset - frameOffsetY)

    override fun getLocalBoundsInternal() = Rectangle(-anchorDispXNoOffset, -anchorDispYNoOffset, frameWidth, frameHeight)

    override fun createInstance(): View = BaseImage(bitmap, anchor, hitShape, smoothing)

    override fun toString(): String = super.toString() + ":bitmap=$bitmap"
}

interface SmoothedBmpSlice {
    var bitmap: BitmapCoords
    var smoothing: Boolean
}

class Image(
	bitmap: Resourceable<out BitmapCoords>,
    anchor: Anchor = Anchor.TOP_LEFT,
	hitShape: VectorPath? = null,
	smoothing: Boolean = true
) : BaseImage(bitmap, anchor, hitShape, smoothing), ViewFileRef by ViewFileRef.Mixin(), SmoothedBmpSlice {
    constructor(
        bitmap: BitmapCoords,
        anchor: Anchor = Anchor.TOP_LEFT,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(Resourceable(bitmap), anchor, hitShape, smoothing)

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["png", "jpg"])
    private var imageSourceFile: String? by this::sourceFile

    constructor(
		bitmap: Bitmap,
        anchor: Anchor = Anchor.TOP_LEFT,
		hitShape: VectorPath? = null,
		smoothing: Boolean = true
	) : this(bitmap.slice(), anchor, hitShape, smoothing)

    override fun createInstance(): View = Image(bitmap, anchor, hitShape, smoothing)

    override fun renderInternal(ctx: RenderContext) {
        lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        //println("### Trying to load sourceImage=$sourceImage")
        try {
            bitmap = currentVfs["$sourceFile"].readBitmapSlice()
            scale = 1.0
        } catch (e: Throwable) {
            bitmap = Bitmaps.white
            scale = 100.0
        }
    }
}
