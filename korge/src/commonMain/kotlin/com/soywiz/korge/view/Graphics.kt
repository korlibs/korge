package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.jvm.*
import kotlin.math.*

inline fun Container.graphics(autoScaling: Boolean = false, callback: Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling).addTo(this).apply(callback)
inline fun Container.sgraphics(callback: Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling = true).addTo(this).apply(callback)

open class Graphics @JvmOverloads constructor(
    var autoScaling: Boolean = false
) : Image(Bitmaps.transparent), VectorBuilder {
    internal val graphicsPathPool = Pool(reset = { it.clear() }) { GraphicsPath() }
	private val shapes = arrayListOf<Shape>()
	private val compoundShape = CompoundShape(shapes)
	private var fill: Paint? = null
	private var stroke: Paint? = null
	@PublishedApi
	internal var currentPath = graphicsPathPool.alloc()
	@PublishedApi
	internal var dirty = true

	inline fun dirty(callback: () -> Unit): Graphics {
		this.dirty = true
		callback()
        return this
	}

	fun clear() {
        shapes.forEach { (it as? StyledShape)?.path?.let { path -> graphicsPathPool.free(path) } }
		shapes.clear()
        currentPath.clear()
	}

	private var thickness: Double = 1.0
	private var pixelHinting: Boolean = false
	private var scaleMode: LineScaleMode = LineScaleMode.NORMAL
	private var startCap: LineCap = LineCap.BUTT
	private var endCap: LineCap = LineCap.BUTT
	private var lineJoin: LineJoin = LineJoin.MITER
	private var miterLimit: Double = 4.0
    var hitTestUsingShapes = true

	@Deprecated("This doesn't do anything")
	fun lineStyle(thickness: Double, color: RGBA, alpha: Double): Unit = TODO()

	override val lastX: Double get() = currentPath.lastX
	override val lastY: Double get() = currentPath.lastY
	override val totalPoints: Int get() = currentPath.totalPoints

	override fun close() = currentPath.close()
	override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) { currentPath.cubicTo(cx1, cy1, cx2, cy2, ax, ay) }
	override fun lineTo(x: Double, y: Double) { currentPath.lineTo(x, y) }
	override fun moveTo(x: Double, y: Double) { currentPath.moveTo(x, y) }
	override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) { currentPath.quadTo(cx, cy, ax, ay) }

    inline fun fill(color: RGBA, alpha: Double = 1.0, callback: () -> Unit) = fill(toColorFill(color, alpha), callback)
    @Deprecated("Kotlin/Native boxes inline+Number")
    inline fun fill(color: RGBA, alpha: Number, callback: () -> Unit) = fill(color, alpha.toDouble(), callback)

	inline fun fill(paint: Paint, callback: () -> Unit) {
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
		ColorPaint(color),
		info, callback
	)

	inline fun stroke(
		paint: Paint,
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
		fill: Paint,
		stroke: Paint,
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
		fill: Paint,
		stroke: Paint,
		strokeInfo: Context2d.StrokeInfo
	) {
		this.fill = fill
		this.stroke = stroke
		this.setStrokeInfo(strokeInfo)
	}

	fun beginFill(paint: Paint) = dirty {
		fill = paint
		currentPath.clear()
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

	fun beginStroke(paint: Paint, info: Context2d.StrokeInfo) = dirty {
		setStrokeInfo(info)
		stroke = paint
        currentPath.clear()
	}

    @PublishedApi
    internal fun toColorFill(color: RGBA, alpha: Double): ColorPaint =
        ColorPaint(RGBA(color.r, color.g, color.b, (color.a * alpha).toInt().clamp(0, 255)))

	fun beginFill(color: RGBA, alpha: Double) = beginFill(toColorFill(color, alpha))

	inline fun shape(shape: VectorPath) = dirty { currentPath.write(shape) }
    inline fun shape(shape: VectorPath, matrix: Matrix) = dirty { currentPath.write(shape, matrix) }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		currentPath = graphicsPathPool.alloc()
	}

	fun endStroke() = dirty {
		shapes += PolylineShape(currentPath, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		//shapes += PolylineShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, joints, miterLimit)
		currentPath = graphicsPathPool.alloc()
	}

	fun endFillStroke() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		shapes += PolylineShape(graphicsPathPool.alloc().also { it.write(currentPath) }, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		currentPath = graphicsPathPool.alloc()
	}

	internal val _sLeft get() = sLeft
	internal val _sTop get() = sTop

    override val sLeft get() = bounds.x - anchorDispX
    override val sTop get() = bounds.y - anchorDispY

	private val bb = BoundsBuilder()
	private val bounds = Rectangle()

    private var renderedAtScaleX = 1.0
    private var renderedAtScaleY = 1.0
    private val matrixTransform = Matrix.Transform()

    override val bwidth: Double get() = bitmap.width.toDouble() / renderedAtScaleX
    override val bheight: Double get() = bitmap.height.toDouble() / renderedAtScaleY

    var useNativeRendering = true

    private fun createImage(width: Int, height: Int): Bitmap {
        return if (useNativeRendering) NativeImage(width, height) else Bitmap32(width, height, premultiplied = true)
    }

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

            getLocalBoundsInternalNoAnchor(bounds)

            // Removes old image
            run {
                ctx.agBitmapTextureManager.removeBitmap(this.bitmap.bmp)
            }
            // Generates new image
            run {
                val image = createImage(
                    (bounds.width * renderedAtScaleX).toInt().coerceAtLeast(1),
                    (bounds.height * renderedAtScaleY).toInt().coerceAtLeast(1)
                )
                image.context2d {
                    scale(renderedAtScaleX, renderedAtScaleY)
                    translate(-bounds.x, -bounds.y)
                    compoundShape.draw(this)
                }
                this.bitmap = image.slice()
            }
		}
		super.renderInternal(ctx)
	}

    fun getLocalBoundsInternalNoAnchor(out: Rectangle) {
        bb.reset()
        shapes.fastForEach { it.addBounds(bb) }
        bb.getBounds(out)
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        getLocalBoundsInternalNoAnchor(out)
        out.displace(-anchorDispX, -anchorDispY)
    }

    override fun hitTest(x: Double, y: Double): View? = hitTestInternal(x, y)

    override fun hitTestInternal(x: Double, y: Double): View? {
        val anchorDispX = this.anchorDispX
        val anchorDispY = this.anchorDispY
        val sLeft = this.sLeft
        val sTop = this.sTop
        val bwidth = this.bwidth
        val bheight = this.bheight

        val lx = globalToLocalX(x, y) + anchorDispX
        val ly = globalToLocalY(x, y) + anchorDispY

        val centerX = sLeft + bwidth * 0.5 + anchorDispX
        val centerY = sTop + bheight * 0.5 + anchorDispY

        val manhattanDist = (lx - centerX).absoluteValue + (ly - centerY).absoluteValue
        val manhattanDist2 = bwidth * 0.5 + bheight * 0.5
        //println("($centerX, $centerY)-($lx, $ly): $manhattanDist > $manhattanDist2")
        if (manhattanDist > manhattanDist2) return null
        val outerCircleRadius = hypot(bwidth * 0.5, bheight * 0.5)
        val dist = Point.distance(lx, ly, centerX, centerY)
        //println("($centerX, $centerY)-($lx, $ly): $dist > $outerCircleRadius")
        if (dist > outerCircleRadius) return null

        if (hitTestUsingShapes) {
            shapes.fastForEach { shape ->
                if (shape.containsPoint(lx, ly)) return this
            }
            return null
        } else {
            return if (bounds.contains(lx, ly)) return this else null
        }
	}
}
