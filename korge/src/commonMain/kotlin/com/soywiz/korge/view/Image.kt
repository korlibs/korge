package com.soywiz.korge.view

import com.soywiz.korim.bitmap.*
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

	override fun createInstance(): View = Image(bitmap, anchorX, anchorY, hitShape, smoothing)

	override fun toString(): String = super.toString() + ":bitmap=$bitmap"

}
