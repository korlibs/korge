package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

inline fun Container.graphics(callback: Graphics.() -> Unit = {}): Graphics = Graphics().addTo(this).apply(callback)

class Graphics : Image(Bitmaps.transparent), VectorBuilder {
	private val shapes = arrayListOf<Shape>()
	private var fill: Context2d.Paint? = null
	@PublishedApi
	internal var currentPath = GraphicsPath()
	@PublishedApi
	internal var dirty = true

	inline fun dirty(callback: () -> Unit) = this.apply {
		this.dirty = true
		callback()
	}

	fun clear() {
		shapes.clear()
	}

	fun lineStyle(thickness: Double, color: RGBA, alpha: Double) = dirty {
	}

	override val lastX: Double get() = currentPath.lastX
	override val lastY: Double get() = currentPath.lastY
	override val totalPoints: Int get() = currentPath.totalPoints

	override fun close() = currentPath.close()
	override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
		currentPath.cubicTo(cx1, cy1, cx2, cy2, ax, ay)
	}

	override fun lineTo(x: Double, y: Double) {
		currentPath.lineTo(x, y)
	}

	override fun moveTo(x: Double, y: Double) {
		currentPath.moveTo(x, y)
	}

	override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
		currentPath.quadTo(cx, cy, ax, ay)
	}

	inline fun fill(color: RGBA, alpha: Number = 1.0, callback: () -> Unit) {
		beginFill(color, alpha.toDouble())
		try {
			callback()
		} finally {
			endFill()
		}
	}

	inline fun fill(paint: Context2d.Paint, callback: () -> Unit) {
		beginFill(paint)
		try {
			callback()
		} finally {
			endFill()
		}
	}

	fun beginFill(paint: Context2d.Paint) = dirty {
		fill = paint
		currentPath = GraphicsPath()
	}

	fun beginFill(color: RGBA, alpha: Double) = dirty {
		fill = Context2d.Color(RGBA(color.r, color.g, color.b, (alpha * 255).toInt()))
		currentPath = GraphicsPath()
	}


	inline fun shape(shape: VectorPath) = dirty { currentPath.write(shape) }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix())
		currentPath = GraphicsPath()
	}

	internal val _sLeft get() = sLeft
	internal val _sTop get() = sTop

	override var sLeft = 0.0
	override var sTop = 0.0

	override fun renderInternal(ctx: RenderContext) {
		if (dirty) {
			dirty = false
			val bounds = shapes.map { it.getBounds() }.bounds()
			val image = NativeImage(bounds.width.toInt(), bounds.height.toInt())
			image.context2d {
				translate(-bounds.x, -bounds.y)
				for (shape in shapes) {
					shape.draw(this)
				}
			}
			this.bitmap = image.slice()
			sLeft = bounds.x
			sTop = bounds.y
		}
		super.renderInternal(ctx)
	}

	//override fun hitTestInternal(x: Double, y: Double): View? {
	//	val lx = globalToLocalX(x, y)
	//	val ly = globalToLocalY(x, y)
	//	for (shape in shapes) {
	//		if (shape.containsPoint(lx, ly)) return this
	//	}
	//	return null
	//}
}
