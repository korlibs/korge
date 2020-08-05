package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.vector.*

inline fun Container.image(
	texture: BmpSlice, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

inline fun Container.image(
	texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this, callback)

//typealias Sprite = Image

open class Image(
	bitmap: BmpSlice,
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	hitShape: VectorPath? = null,
	smoothing: Boolean = true
) : RectBase(anchorX, anchorY, hitShape, smoothing), KorgeDebugNode {
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

	override fun createInstance(): View = Image(bitmap, anchorX, anchorY, hitShape, smoothing)

    private var sourceImageLoaded: Boolean = false
    var sourceImage: String? = null
        set(value) {
            sourceImageLoaded = false
            field = value
        }

    suspend fun forceLoadSourceImage(currentVfs: VfsFile, sourceImage: String? = null) {
        //println("### Trying to load sourceImage=$sourceImage")
        this.sourceImage = sourceImage
        sourceImageLoaded = true
        bitmap = currentVfs["$sourceImage"].readBitmapSlice()
        scale = 1.0
    }

    override fun renderInternal(ctx: RenderContext) {
        if (!sourceImageLoaded && sourceImage != null) {
            sourceImageLoaded = true
            launchImmediately(ctx.coroutineContext) {
                forceLoadSourceImage(ctx.views?.currentVfs ?: resourcesVfs, sourceImage)
            }
        }
        super.renderInternal(ctx)
    }

    override fun getDebugProperties(): EditableNode = EditableSection("Image") {
        add(this@Image::sourceImage.toEditableProperty())
    }

    override fun toString(): String = super.toString() + ":bitmap=$bitmap"

}
