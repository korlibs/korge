package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.resources.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*

inline fun Container.image(
	texture: Resourceable<out BmpSlice>, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

inline fun Container.image(
	texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

//typealias Sprite = Image

open class BaseImage(
    bitmap: Resourceable<out BmpSlice>,
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

    var bitmap: BmpSlice
        get() = baseBitmap
        set(value) {
            bitmapSrc = value
        }

    var bitmapSrc: Resourceable<out BmpSlice> = bitmap
        set(value) {
            setBitmapSource = false
            field = value
            trySetSource()
        }

    fun trySetSource() {
        if (setBitmapSource) return
        bitmapSrc.getOrNull()?.let {
            setBitmapSource = true
            this.baseBitmap = it
        }
    }

    init {
        trySetSource()
    }

    override fun renderInternal(ctx: RenderContext) {
        trySetSource()
        super.renderInternal(ctx)
    }

    override val bwidth: Double get() = baseBitmap.width.toDouble()
    override val bheight: Double get() = baseBitmap.height.toDouble()

    override fun createInstance(): View = BaseImage(bitmap, anchorX, anchorY, hitShape, smoothing)

    override fun toString(): String = super.toString() + ":bitmap=$bitmap"

}

class Image(
	bitmap: Resourceable<out BmpSlice>,
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	hitShape: VectorPath? = null,
	smoothing: Boolean = true
) : BaseImage(bitmap, anchorX, anchorY, hitShape, smoothing), ViewFileRef by ViewFileRef.Mixin() {
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
