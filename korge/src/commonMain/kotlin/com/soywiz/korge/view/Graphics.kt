package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.jvm.*

inline fun Container.graphics(autoScaling: Boolean = false, callback: @ViewDslMarker Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling).addTo(this, callback).apply { redrawIfRequired() }
inline fun Container.sgraphics(callback: @ViewDslMarker Graphics.() -> Unit = {}): Graphics = Graphics(autoScaling = true).addTo(this, callback).apply { redrawIfRequired() }

open class Graphics @JvmOverloads constructor(
    var autoScaling: Boolean = false
) : BaseImage(Bitmaps.transparent), VectorBuilder {
    internal val graphicsPathPool = Pool(reset = { it.clear() }) { GraphicsPath() }
    private var shapeVersion = 0
	private val shapes = arrayListOf<Shape>()
	private val compoundShape = CompoundShape(shapes)
	private var fill: Paint? = null
	private var stroke: Paint? = null
	@PublishedApi
	internal var currentPath = graphicsPathPool.alloc()
	@PublishedApi
	internal var dirty = true

    private var hitShapeVersion = -1
    private var hitShapeAnchorVersion = -1

    private var tempVectorPaths = arrayListOf<VectorPath>()
    private val tempMatrix = Matrix()
    private var customHitShapes: List<VectorPath>? = null

    override var hitShape: VectorPath?
        set(value) = run { customHitShapes = value?.let { listOf(it) } }
        get() = hitShapes?.firstOrNull()

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

    @PublishedApi
    internal inline fun dirty(callback: () -> Unit): Graphics {
		this.dirty = true
		callback()
        return this
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
		info: Context2d.StrokeInfo,
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
		strokeInfo: Context2d.StrokeInfo,
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

    fun shape(shape: Shape) {
        shapes += shape
        currentPath = graphicsPathPool.alloc()
        shapeVersion++
    }
	inline fun shape(shape: VectorPath) = dirty { currentPath.write(shape) }
    inline fun shape(shape: VectorPath, matrix: Matrix) = dirty { currentPath.write(shape, matrix) }

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		currentPath = graphicsPathPool.alloc()
        shapeVersion++
	}

	fun endStroke() = dirty {
		shapes += PolylineShape(currentPath, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		//shapes += PolylineShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, joints, miterLimit)
		currentPath = graphicsPathPool.alloc()
        shapeVersion++
	}

	fun endFillStroke() = dirty {
		shapes += FillShape(currentPath, null, fill ?: ColorPaint(Colors.RED), Matrix())
		shapes += PolylineShape(graphicsPathPool.alloc().also { it.write(currentPath) }, null, stroke ?: ColorPaint(Colors.RED), Matrix(), thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)
		currentPath = graphicsPathPool.alloc()
        shapeVersion++
	}

	internal val _sLeft get() = sLeft
	internal val _sTop get() = sTop

    override val sLeft: Double get() {
        var out = bounds.x - anchorDispX
        if (bwidth < 0) out -= bwidth
        return out
    }
    override val sTop: Double get() {
        var out = bounds.y - anchorDispY
        if (bheight < 0) out -= bheight
        return out
    }

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
        bitmapsToRemove.fastForEach {
            ctx.agBitmapTextureManager.removeBitmap(it)
        }
        bitmapsToRemove.clear()

        redrawIfRequired()
		super.renderInternal(ctx)
	}

    @PublishedApi
    internal val bitmapsToRemove = arrayListOf<Bitmap>()

    @PublishedApi
    internal fun redrawIfRequired() {
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
                bitmapsToRemove.add(this.bitmap.bmp)
            }
            // Generates new image
            run {
                //println("Regenerate image: bounds=${bounds}, renderedAtScale=${renderedAtScaleX},${renderedAtScaleY}, sLeft=$sLeft, sTop=$sTop, bwidth=$bwidth, bheight=$bheight")

                val image = createImage(
                    (bounds.width * renderedAtScaleX).toIntCeil().coerceAtLeast(1) + 1,
                    (bounds.height * renderedAtScaleY).toIntCeil().coerceAtLeast(1) + 1
                )
                image.context2d {
                    scale(this@Graphics.renderedAtScaleX, this@Graphics.renderedAtScaleY)
                    translate(-this@Graphics.bounds.x, -this@Graphics.bounds.y)
                    this@Graphics.compoundShape.draw(this)
                }
                this.bitmap = image.slice()
            }
        }
    }

    fun getLocalBoundsInternalNoAnchor(out: Rectangle) {
        bb.reset()
        shapes.fastForEach { it.addBounds(bb) }
        bb.getBounds(out)
        //println("Graphics.BOUNDS: $out")
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        getLocalBoundsInternalNoAnchor(out)
        out.displace(-anchorDispX, -anchorDispY)
    }
}
