package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.jvm.*

inline fun Container.graphics(autoScaling: Boolean = false, callback: @ViewDslMarker Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling).addTo(this, callback).apply { redrawIfRequired() }
inline fun Container.sgraphics(callback: @ViewDslMarker Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling = true).addTo(this, callback).apply { redrawIfRequired() }

open class Graphics @JvmOverloads constructor(
    autoScaling: Boolean = false
) : BaseGraphics(autoScaling), VectorBuilder {
    internal val graphicsPathPool = Pool(reset = { it.clear() }) { GraphicsPath() }
    private var shapeVersion = 0
	private val shapes = arrayListOf<Shape>()
    val allShapes: List<Shape> get() = shapes
    private val compoundShape = CompoundShape(shapes)
	private var fill: Paint? = null
	private var stroke: Paint? = null
	@PublishedApi
	internal var currentPath = graphicsPathPool.alloc()

    private var hitShapeVersion = -1
    private var hitShape2dVersion = -1
    private var hitShapeAnchorVersion = -1

    private var tempVectorPaths = arrayListOf<VectorPath>()
    private val tempMatrix = Matrix()
    private var customHitShape2d: Shape2d? = null
    private var customHitShapes: List<VectorPath>? = null

    override var hitShape: VectorPath?
        set(value) { customHitShapes = value?.let { listOf(it) } }
        get() = hitShapes?.firstOrNull()

    @PublishedApi
    internal inline fun dirty(callback: () -> Unit): Graphics {
        dirty()
        callback()
        return this
    }

    override var hitShapes: List<VectorPath>?
        set(value) {
            customHitShapes = value
        }
        get() {
            if (customHitShapes != null) return customHitShapes
            if (hitShapeVersion != shapeVersion) {
                hitShapeVersion = shapeVersion
                tempVectorPaths.clear()

                // @TODO: Try to combine polygons on KorGE 2.0 to have a single hitShape
                for (shape in shapes) {
                    //when (shape) {
                        //is StyledShape -> shape.path?.let { tempVectorPaths.add(it) }
                        //else ->
                            tempVectorPaths.add(shape.getPath())
                    //}
                }
            }

            //println("AAAAAAAAAAAAAAAA")
            return tempVectorPaths
        }

    override var hitShape2d: Shape2d
        set(value) {
            customHitShape2d = value
        }
        get() {
            if (customHitShape2d != null) return customHitShape2d!!
            if (hitShape2dVersion != shapeVersion) {
                hitShape2dVersion = shapeVersion
                customHitShape2d = hitShapes!!.toShape2d()
            }

            //println("AAAAAAAAAAAAAAAA")
            return customHitShape2d!!
        }

    /**
     * Ensure that after this function the [bitmap] property is up-to-date with all the drawings inside [block].
     */
    inline fun lock(block: Graphics.() -> Unit): Graphics {
        try {
            block()
        } finally {
            redrawIfRequired()
        }
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

    init {
        hitTestUsingShapes = true
    }

	override val lastX: Double get() = currentPath.lastX
	override val lastY: Double get() = currentPath.lastY
	override val totalPoints: Int get() = currentPath.totalPoints

	override fun close() = currentPath.close()
	override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) { currentPath.cubicTo(cx1, cy1, cx2, cy2, ax, ay) }
	override fun lineTo(x: Double, y: Double) { currentPath.lineTo(x, y) }
	override fun moveTo(x: Double, y: Double) { currentPath.moveTo(x, y) }
	override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) { currentPath.quadTo(cx, cy, ax, ay) }

    inline fun fill(color: RGBA, alpha: Double = 1.0, callback: @ViewDslMarker VectorBuilder.() -> Unit) = fill(toColorFill(color, alpha), callback)

	inline fun fill(paint: Paint, callback: @ViewDslMarker VectorBuilder.() -> Unit) {
		beginFill(paint)
		try {
			callback()
		} finally {
			endFill()
		}
	}

	inline fun stroke(
        paint: Paint,
        info: StrokeInfo,
        callback: @ViewDslMarker VectorBuilder.() -> Unit
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
        strokeInfo: StrokeInfo,
        callback: @ViewDslMarker VectorBuilder.() -> Unit
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
            strokeInfo: StrokeInfo
	) {
		this.fill = fill
		this.stroke = stroke
		this.setStrokeInfo(strokeInfo)
	}

	fun beginFill(paint: Paint) = dirty {
		fill = paint
		currentPath.clear()
	}

	private fun setStrokeInfo(info: StrokeInfo) {
		this.thickness = info.thickness
		this.pixelHinting = info.pixelHinting
		this.scaleMode = info.scaleMode
		this.startCap = info.startCap
		this.endCap = info.endCap
		this.lineJoin = info.lineJoin
		this.miterLimit = info.miterLimit
	}

	fun beginStroke(paint: Paint, info: StrokeInfo) = dirty {
		setStrokeInfo(info)
		stroke = paint
        currentPath.clear()
	}

    @PublishedApi
    internal fun toColorFill(color: RGBA, alpha: Double): ColorPaint =
        ColorPaint(RGBA(color.r, color.g, color.b, (color.a * alpha).toInt().clamp(0, 255)))

	fun beginFill(color: RGBA, alpha: Double) = beginFill(toColorFill(color, alpha))

    fun shape(shape: Shape) {
        shapes += shape
        currentPath = graphicsPathPool.alloc()
        dirtyShape()
    }
	inline fun shape(shape: VectorPath) = dirty { currentPath.write(shape) }
    inline fun shape(shape: VectorPath, matrix: Matrix) = dirty { currentPath.write(shape, matrix) }

    private fun dirtyShape() {
        shapeVersion++
        dirty()
    }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		currentPath = graphicsPathPool.alloc()
        dirtyShape()
	}

	fun endStroke() = dirty {
		shapes += PolylineShape(currentPath, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		//shapes += PolylineShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, joints, miterLimit)
		currentPath = graphicsPathPool.alloc()
        dirtyShape()
	}

	fun endFillStroke() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		shapes += PolylineShape(graphicsPathPool.alloc().also { it.write(currentPath) }, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		currentPath = graphicsPathPool.alloc()
        dirtyShape()
	}

    override fun drawShape(ctx: Context2d) {
        this@Graphics.compoundShape.draw(ctx)    }

    override fun getShapeBounds(bb: BoundsBuilder, includeStrokes: Boolean) {
        shapes.fastForEach {
            //println("getShapeBounds: $it")
            it.addBounds(bb, includeStrokes)
        }
    }
}
