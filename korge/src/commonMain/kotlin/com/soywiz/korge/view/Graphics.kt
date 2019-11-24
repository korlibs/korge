package com.soywiz.korge.view

import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.jvm.*

inline fun Container.graphics(autoScaling: Boolean = false, callback: Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling).addTo(this).apply(callback)
inline fun Container.sgraphics(callback: Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling = true).addTo(this).apply(callback)

open class Graphics @JvmOverloads constructor(var autoScaling: Boolean = false) : Image(Bitmaps.transparent), VectorBuilder {
	private val shapes = arrayListOf<Shape>()
	private val compoundShape = CompoundShape(shapes)
	private var fill: Context2d.Paint? = null
	private var stroke: Context2d.Paint? = null
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

	private var thickness: Double = 1.0
	private var pixelHinting: Boolean = false
	private var scaleMode: Context2d.ScaleMode = Context2d.ScaleMode.NORMAL
	private var startCap: Context2d.LineCap = Context2d.LineCap.BUTT
	private var endCap: Context2d.LineCap = Context2d.LineCap.BUTT
	private var lineJoin: Context2d.LineJoin = Context2d.LineJoin.MITER
	private var miterLimit: Double = 4.0
    var hitTestUsingShapes = false

	@Deprecated("This doesn't do anything")
	fun lineStyle(thickness: Double, color: RGBA, alpha: Double): Unit = TODO()

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

	inline fun fill(color: RGBA, alpha: Number = 1.0, callback: () -> Unit) = fill(toColorFill(color, alpha), callback)

	inline fun fill(paint: Context2d.Paint, callback: () -> Unit) {
		beginFill(paint)
		try {
			callback()
		} finally {
			endFill()
		}
	}

	inline fun stroke(
		color: RGBA, info: Context2d.StrokeInfo, callback: () -> Unit
	) = stroke(
		Context2d.Color(color),
		info, callback
	)

	inline fun stroke(
		paint: Context2d.Paint,
		info: Context2d.StrokeInfo,
		callback: () -> Unit
	) {
		beginStroke(paint, info)
		try {
			callback()
		} finally {
			endStroke()
		}
	}

	inline fun fillStroke(
		fill: Context2d.Paint,
		stroke: Context2d.Paint,
		strokeInfo: Context2d.StrokeInfo,
		callback: () -> Unit
	) {
		beginFillStroke(fill, stroke, strokeInfo)
		try {
			callback()
		} finally {
			endFillStroke()
		}
	}

	fun beginFillStroke(
		fill: Context2d.Paint,
		stroke: Context2d.Paint,
		strokeInfo: Context2d.StrokeInfo
	) {
		this.fill = fill
		this.stroke = stroke
		this.setStrokeInfo(strokeInfo)
	}

	fun beginFill(paint: Context2d.Paint) = dirty {
		fill = paint
		currentPath = GraphicsPath()
	}

	private fun setStrokeInfo(info: Context2d.StrokeInfo) {
		this.thickness = info.thickness
		this.pixelHinting = info.pixelHinting
		this.scaleMode = info.scaleMode
		this.startCap = info.startCap
		this.endCap = info.endCap
		this.lineJoin = info.lineJoin
		this.miterLimit = info.miterLimit
	}

	fun beginStroke(paint: Context2d.Paint, info: Context2d.StrokeInfo) = dirty {
		setStrokeInfo(info)
		stroke = paint
		currentPath = GraphicsPath()
	}

	@PublishedApi
	internal inline fun toColorFill(color: RGBA, alpha: Number): Context2d.Color {
		return Context2d.Color(RGBA(color.r, color.g, color.b, (alpha.toDouble() * 255).toInt()))
		//return Context2d.Color(color.withAd(alpha.toDouble())
	}

	fun beginFill(color: RGBA, alpha: Double) = beginFill(toColorFill(color, alpha))

	inline fun shape(shape: VectorPath) = dirty { currentPath.write(shape) }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix())
		currentPath = GraphicsPath()
	}

	fun endStroke() = dirty {
		shapes += PolylineShape(currentPath, null, stroke ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		//shapes += PolylineShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, joints, miterLimit)
		currentPath = GraphicsPath()
	}

	fun endFillStroke() = dirty {
		shapes += FillShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix())
		shapes += PolylineShape(currentPath, null, stroke ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		currentPath = GraphicsPath()
	}

	internal val _sLeft get() = sLeft
	internal val _sTop get() = sTop

	override var sLeft = 0.0
	override var sTop = 0.0

	private val bb = BoundsBuilder()
	private val bounds = Rectangle()

    private var renderedAtScaleX = 1.0
    private var renderedAtScaleY = 1.0
    private val matrixTransform = Matrix.Transform()

    override val bwidth: Double get() = bitmap.width.toDouble() / renderedAtScaleX
    override val bheight: Double get() = bitmap.height.toDouble() / renderedAtScaleY

    override fun renderInternal(ctx: RenderContext) {
        if (autoScaling) {
            matrixTransform.setMatrix(this.globalMatrix)
            //val sx = kotlin.math.abs(matrixTransform.scaleX / this.scaleX)
            //val sy = kotlin.math.abs(matrixTransform.scaleY / this.scaleY)

            val sx = kotlin.math.abs(matrixTransform.scaleX)
            val sy = kotlin.math.abs(matrixTransform.scaleY)

            val diffX = kotlin.math.abs((sx / renderedAtScaleX) - 1.0)
            val diffY = kotlin.math.abs((sy / renderedAtScaleY) - 1.0)

            if (diffX >= 0.1 || diffY >= 0.1) {
                renderedAtScaleX = sx
                renderedAtScaleY = sy
                //println("renderedAtScale: $renderedAtScaleX, $renderedAtScaleY")
                dirty = true
            }
        }

		if (dirty) {
			dirty = false

			bb.reset()
			shapes.fastForEach { it.addBounds(bb) }
			bb.getBounds(bounds)

			val image = NativeImage((bounds.width * renderedAtScaleX).toInt(), (bounds.height * renderedAtScaleY).toInt())
			image.context2d {
                scale(renderedAtScaleX, renderedAtScaleY)
				translate(-bounds.x, -bounds.y)
				compoundShape.draw(this)
			}
			this.bitmap = image.slice()
			sLeft = bounds.x
			sTop = bounds.y
		}
		super.renderInternal(ctx)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
        if (hitTestUsingShapes) {
            val lx = globalToLocalX(x, y)
            val ly = globalToLocalY(x, y)
            for (shape in shapes) {
                if (shape.containsPoint(lx, ly)) return this
            }
            return null
        } else {
            return super.hitTestInternal(x, y)
        }
	}
}
