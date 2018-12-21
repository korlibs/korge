package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

inline fun Container.graphics(callback: Graphics.() -> Unit = {}): Graphics = Graphics().addTo(this).apply(callback)

class Graphics : Image(Bitmaps.transparent) {
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

	inline fun moveTo(x: Number, y: Number) = dirty { currentPath.moveTo(x.toDouble(), y.toDouble()) }
	inline fun lineTo(x: Number, y: Number) = dirty { currentPath.lineTo(x.toDouble(), y.toDouble()) }

	// Inline Class ERROR: Platform declaration clash: The following declarations have the same JVM signature (beginFill(ID)Lcom/soywiz/korge/view/Graphics;):
	//fun beginFill(color: Int, alpha: Double) = beginFill(RGBA(color), alpha)

	inline fun fill(color: RGBA, alpha: Number = 1.0, callback: () -> Unit) {
		beginFill(color, alpha.toDouble())
		try {
			callback()
		} finally {
			endFill()
		}
	}

	fun beginFill(color: RGBA, alpha: Double) = dirty {
		fill = Context2d.Color(RGBA(color.r, color.g, color.b, (alpha * 255).toInt()))
		currentPath = GraphicsPath()
	}

	inline fun drawCircle(x: Number, y: Number, r: Number) = dirty {
		currentPath.circle(x.toDouble(), y.toDouble(), r.toDouble())
	}

	inline fun drawRect(x: Number, y: Number, width: Number, height: Number) = dirty {
		currentPath.rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
	}

	inline fun drawShape(shape: VectorPath) = dirty { currentPath.write(shape) }

	inline fun drawRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number) = dirty {
		currentPath.roundRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble())
	}

	inline fun drawEllipse(x: Number, y: Number, rw: Number, rh: Number) = dirty { currentPath.ellipse(x.toDouble(), y.toDouble(), rw.toDouble(), rh.toDouble()) }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix2d())
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
