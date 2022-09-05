package com.soywiz.korge.view

import com.soywiz.korge.debug.UiTextEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.bitmap.BaseBmpSlice
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC
import com.soywiz.korio.resources.Resourceable
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korui.UiContainer

inline fun Container.image(
	texture: Resourceable<out BaseBmpSlice>, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

inline fun Container.image(
	texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

//typealias Sprite = Image

open class BaseImage(
    bitmap: Resourceable<out BaseBmpSlice>,
    anchorX: Double = 0.0,
    anchorY: Double = anchorX,
    hitShape: VectorPath? = null,
    smoothing: Boolean = true
) : RectBase(anchorX, anchorY, hitShape, smoothing) {
    constructor(
        bitmap: Bitmap,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(bitmap.slice(), anchorX, anchorY, hitShape, smoothing)

    private var setBitmapSource: Boolean = false

    var bitmap: BaseBmpSlice
        get() = baseBitmap
        set(value) {
            if (baseBitmap == value) return
            setBitmapSource = true
            baseBitmap = value
            invalidate()
        }

    var bitmapSrc: Resourceable<out BaseBmpSlice> = bitmap
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
    override val bwidth: Double get() = baseBitmap.width.toDouble()
    override val bheight: Double get() = baseBitmap.height.toDouble()

    open val frameOffsetX: Double get() = baseBitmap.frameOffsetX.toDouble()
    open val frameOffsetY: Double get() = baseBitmap.frameOffsetY.toDouble()
    open val frameWidth: Double get() = baseBitmap.frameWidth.toDouble()
    open val frameHeight: Double get() = baseBitmap.frameHeight.toDouble()

    open val anchorDispXNoOffset get() = (anchorX * frameWidth)
    open val anchorDispYNoOffset get() = (anchorY * frameHeight)

    override val anchorDispX get() = (anchorDispXNoOffset - frameOffsetX)
    override val anchorDispY get() = (anchorDispYNoOffset - frameOffsetY)

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(-anchorDispXNoOffset, -anchorDispYNoOffset, frameWidth, frameHeight)
    }

    override fun createInstance(): View = BaseImage(bitmap, anchorX, anchorY, hitShape, smoothing)

    override fun toString(): String = super.toString() + ":bitmap=$bitmap"
}

interface SmoothedBmpSlice {
    var bitmap: BaseBmpSlice
    var smoothing: Boolean
}

class Image(
	bitmap: Resourceable<out BaseBmpSlice>,
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	hitShape: VectorPath? = null,
	smoothing: Boolean = true
) : BaseImage(bitmap, anchorX, anchorY, hitShape, smoothing), ViewFileRef by ViewFileRef.Mixin(), SmoothedBmpSlice {
	constructor(
		bitmap: Bitmap,
		anchorX: Double = 0.0,
		anchorY: Double = anchorX,
		hitShape: VectorPath? = null,
		smoothing: Boolean = true
	) : this(bitmap.slice(), anchorX, anchorY, hitShape, smoothing)

    override fun createInstance(): View = Image(bitmap, anchorX, anchorY, hitShape, smoothing)

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

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("Image") {
            uiEditableValue(this@Image::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "png" || it.extensionLC == "jpg"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
