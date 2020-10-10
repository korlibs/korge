package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*

inline fun Container.image(
	texture: BmpSlice, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

inline fun Container.image(
	texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

//typealias Sprite = Image

open class BaseImage(
    bitmap: BmpSlice,
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

    var bitmap: BmpSlice get() = baseBitmap; set(v) { baseBitmap = v }
    var texture: BmpSlice get() = baseBitmap; set(v) { baseBitmap = v }

    init {
        this.baseBitmap = bitmap
    }

    override val bwidth: Double get() = bitmap.width.toDouble()
    override val bheight: Double get() = bitmap.height.toDouble()

    override fun createInstance(): View = BaseImage(bitmap, anchorX, anchorY, hitShape, smoothing)

    override fun toString(): String = super.toString() + ":bitmap=$bitmap"

}

class Image(
	bitmap: BmpSlice,
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

    override fun renderInternal(ctx: RenderContext) {
        lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsableSection("Image") {
            uiEditableValue(this@Image::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "png" || it.extensionLC == "jpg"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
