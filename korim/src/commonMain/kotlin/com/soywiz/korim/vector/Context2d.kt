package com.soywiz.korim.vector

import com.soywiz.kds.IDoubleArrayList
import com.soywiz.kds.Stack
import com.soywiz.kds.mapFloat
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.mipmap
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.FontRegistry
import com.soywiz.korim.font.TextMetrics
import com.soywiz.korim.font.drawText
import com.soywiz.korim.font.getTextBounds
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.TextRenderer
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.vector.renderer.Renderer
import com.soywiz.korio.lang.Disposable
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.radians
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.VectorBuilder
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.Winding
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.geom.vector.LineScaleMode
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.isEmpty
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil

open class Context2d constructor(
    val renderer: Renderer,
    val defaultFontRegistry: FontRegistry? = null,
    val defaultFont: Font? = null
) : VectorBuilder, Disposable {
    var debug: Boolean
        get() = renderer.debug
        set(value) { renderer.debug = value }

    protected open val rendererWidth get() = renderer.width
    protected open val rendererHeight get() = renderer.height
    protected open fun rendererRender(state: State, fill: Boolean, winding: Winding? = null) = renderer.render(state, fill, winding)
    protected open fun rendererDrawImage(image: Bitmap, x: Double, y: Double, width: Double = image.width.toDouble(), height: Double = image.height.toDouble(), transform: Matrix = Matrix()) = renderer.drawImage(image, x, y, width, height, transform)
    protected open fun rendererDispose() = renderer.dispose()
    protected open fun rendererBufferingStart() = renderer.bufferingStart()
    protected open fun rendererBufferingEnd() = renderer.bufferingEnd()
    protected open fun rendererRenderSystemText(state: State, font: Font?, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) {
        font?.drawText(this, fontSize, text, if (fill) state.fillStyle else state.strokeStyle, x, y, fill = fill)
    }

    fun fillText(text: String, x: Double, y: Double) = rendererRenderSystemText(state, font, fontSize, text, x, y, fill = true)
    fun strokeText(text: String, x: Double, y: Double) = rendererRenderSystemText(state, font, fontSize, text, x, y, fill = false)

    open val width: Int get() = rendererWidth
    open val height: Int get() = rendererHeight

	override fun dispose() {
		rendererDispose()
	}

    fun withScaledRenderer(scaleX: Double, scaleY: Double = scaleX): Context2d = if (scaleX == 1.0 && scaleY == 1.0) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))

	class ScaledRenderer(val parent: Renderer, val scaleX: Double, val scaleY: Double) : Renderer() {
		override val width: Int get() = (parent.width / scaleX).toInt()
		override val height: Int get() = (parent.height / scaleY).toInt()

		private inline fun <T> adjustMatrix(transform: Matrix, callback: () -> T): T = transform.keep {
            transform.scale(scaleX, scaleY)
            callback()
        }

		private inline fun <T> adjustState(state: State, callback: () -> T): T =
			adjustMatrix(state.transform) { callback() }

		override fun render(state: State, fill: Boolean, winding: Winding?): Unit = adjustState(state) { parent.render(state, fill, winding) }
		//override fun renderText(state: State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean): Unit =
		//	adjustState(state) { parent.renderText(state, font, fontSize, text, x, y, fill) }

		override fun drawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
			adjustMatrix(transform) { parent.drawImage(image, x, y, width, height, transform) }
		}
	}

	@PublishedApi
	internal fun _rendererBufferingStart() = rendererBufferingStart()
	@PublishedApi
	internal fun _rendererBufferingEnd() = rendererBufferingEnd()

    inline fun <T> buffering(callback: () -> T): T {
        _rendererBufferingStart()
        try {
            return callback()
        } finally {
            _rendererBufferingEnd()
        }
    }

	data class State constructor(
        var transform: Matrix = Matrix(),
        var clip: VectorPath? = null,
        var path: VectorPath = VectorPath(),
        var lineScaleMode: LineScaleMode = LineScaleMode.NORMAL,
        var lineWidth: Double = 1.0,
        var startLineCap: LineCap = LineCap.BUTT,
        var endLineCap: LineCap = LineCap.BUTT,
        var lineJoin: LineJoin = LineJoin.MITER,
        var miterLimit: Double = 10.0,
        var strokeStyle: Paint = DefaultPaint,
        var fillStyle: Paint = DefaultPaint,
        var fontRegistry: FontRegistry? = null,
        var font: Font? = null,
        var fontSize: Double = 24.0,
        var verticalAlign: VerticalAlign = VerticalAlign.BASELINE,
        var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT,
        var globalAlpha: Double = 1.0,
        var globalCompositeOperation: CompositeOperation = CompositeMode.SOURCE_OVER,
        var lineDash: IDoubleArrayList? = null,
        var lineDashOffset: Double = 0.0,
	) {
        val transformTransform by lazy { transform.toTransform() }
        val scaledLineWidth get() = lineWidth * transformTransform.scaleAvg.absoluteValue.toFloat()

        val lineDashFloatArray: FloatArray? get() = lineDash?.mapFloat { it.toFloat() }?.toFloatArray()

        var alignment: TextAlignment
            get() = TextAlignment.fromAlign(horizontalAlign, verticalAlign)
            set(value) {
                horizontalAlign = value.horizontal
                verticalAlign = value.vertical
            }

        var lineCap: LineCap
            get() = startLineCap
            set(value) {
                startLineCap = value
                endLineCap = value
            }

        fun fillOrStrokeStyle(fill: Boolean) = if (fill) fillStyle else strokeStyle

        fun clone(): State = this.copy(
			transform = transform.clone(),
			clip = clip?.clone(),
			path = path.clone(),
            lineDash = lineDash?.clone()
		)
	}

	var state = State(fontRegistry = defaultFontRegistry, font = defaultFont)
	private val stack = Stack<State>()

    // @TODO: Can we use `by state::lineScaleMode` already?
	var lineScaleMode: LineScaleMode ; get() = state.lineScaleMode ; set(value) { state.lineScaleMode = value }
	var lineWidth: Double ; get() = state.lineWidth ; set(value) { state.lineWidth = value }
	var lineCap: LineCap ; get() = state.lineCap ; set(value) { state.lineCap = value }
    var miterLimit: Double ; get() = state.miterLimit ; set(value) { state.miterLimit = value }
    var startLineCap: LineCap ; get() = state.startLineCap ; set(value) { state.startLineCap = value }
    var endLineCap: LineCap ; get() = state.endLineCap ; set(value) { state.endLineCap = value }
    var lineJoin: LineJoin ; get() = state.lineJoin ; set(value) { state.lineJoin = value }
	var strokeStyle: Paint; get() = state.strokeStyle ; set(value) { state.strokeStyle = value }
	var fillStyle: Paint; get() = state.fillStyle ; set(value) { state.fillStyle = value }
    var fontRegistry: FontRegistry? ; get() = state.fontRegistry ; set(value) { state.fontRegistry = value }
	var font: Font? ; get() = state.font ; set(value) { state.font = value }
    var fontName: String? ; get() = font?.name ; set(value) { font = fontRegistry?.get(value) }
    var fontSize: Double ; get() = state.fontSize ; set(value) { state.fontSize = value }
	var verticalAlign: VerticalAlign; get() = state.verticalAlign ; set(value) { state.verticalAlign = value }
	var horizontalAlign: HorizontalAlign; get() = state.horizontalAlign ; set(value) { state.horizontalAlign = value }
    var alignment: TextAlignment
        get() = TextAlignment.fromAlign(horizontalAlign, verticalAlign)
        set(value) {
            horizontalAlign = value.horizontal
            verticalAlign = value.vertical
        }
	var globalAlpha: Double ; get() = state.globalAlpha ; set(value) { state.globalAlpha = value }
    var globalCompositeOperation: CompositeOperation ; get() = state.globalCompositeOperation ; set(value) { state.globalCompositeOperation = value }
    var lineDash: IDoubleArrayList?; get() = state.lineDash ; set(value) { state.lineDash = value }
    var lineDashOffset: Double; get() = state.lineDashOffset ; set(value) { state.lineDashOffset = value }

    inline fun lineDash(lineDash: IDoubleArrayList?, lineDashOffset: Double = 0.0, callback: () -> Unit) {
        val oldLineDash = this.lineDash
        val oldLineDashOffset = this.lineDashOffset
        this.lineDash = lineDash
        this.lineDashOffset = lineDashOffset
        try {
            callback()
        } finally {
            this.lineDashOffset = oldLineDashOffset
            this.lineDash = oldLineDash
        }
    }

	inline fun fillStyle(paint: Paint, callback: () -> Unit) {
		val oldStyle = fillStyle
		fillStyle = paint
		try {
			callback()
		} finally {
			fillStyle = oldStyle
		}
	}

	inline fun strokeStyle(paint: Paint, callback: () -> Unit) {
		val oldStyle = strokeStyle
		strokeStyle = paint
		try {
			callback()
		} finally {
			strokeStyle = oldStyle
		}
	}

	inline fun font(
        font: Font? = this.font,
        halign: HorizontalAlign = this.horizontalAlign,
        valign: VerticalAlign = this.verticalAlign,
        fontSize: Double = this.fontSize,
        callback: () -> Unit
    ) {
		val oldFont = this.font
        val oldFontSize = this.fontSize
		val oldHalign = this.horizontalAlign
		val oldValign = this.verticalAlign
		try {
            this.font = font
            this.fontSize = fontSize
            this.horizontalAlign = halign
            this.verticalAlign = valign
			callback()
		} finally {
			this.font = oldFont
            this.fontSize = oldFontSize
			this.horizontalAlign = oldHalign
			this.verticalAlign = oldValign
		}
	}

	inline fun keepApply(callback: Context2d.() -> Unit) = this.apply { keep { callback() } }

	inline fun keep(callback: () -> Unit) {
		save()
		try {
			callback()
		} finally {
			restore()
		}
	}

	inline fun keepTransform(callback: () -> Unit) {
		val t = state.transform
		val a = t.a
		val b = t.b
		val c = t.c
		val d = t.d
		val tx = t.tx
		val ty = t.ty
		try {
			callback()
		} finally {
			t.setTo(a, b, c, d, tx, ty)
		}
	}

    // @TODO: preallocate states and use it as a pool, and mutate the ones there
	fun save() { stack.push(state.clone()) }
	fun restore() { state = stack.pop() }

	inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toDouble(), sy.toDouble())
	inline fun translate(tx: Number, ty: Number) = translate(tx.toDouble(), ty.toDouble())
	inline fun rotate(angle: Number) = rotate(angle.toDouble())
	inline fun rotateDeg(degs: Number) = rotateDeg(degs.toDouble())

    inline fun scale(sx: Int, sy: Int = sx) = scale(sx.toDouble(), sy.toDouble())
    inline fun translate(tx: Int, ty: Int) = translate(tx.toDouble(), ty.toDouble())
    inline fun rotate(angle: Int) = rotate(angle.toDouble())
    inline fun rotateDeg(degs: Int) = rotateDeg(degs.toDouble())

    inline fun scale(sx: Double, sy: Double = sx, block: () -> Unit) = keep { scale(sx, sy).also { block() } }
    inline fun rotate(angle: Angle, block: () -> Unit) = keep { rotate(angle).also { block() } }
    inline fun translate(tx: Double, ty: Double, block: () -> Unit) = keep { translate(tx, ty).also { block() } }

	fun scale(sx: Double, sy: Double = sx) { state.transform.prescale(sx, sy) }
    fun rotate(angle: Angle) { state.transform.prerotate(angle) }
	fun rotate(angle: Double) { state.transform.prerotate(angle.radians) }
	fun rotateDeg(degs: Double) { state.transform.prerotate(degs.degrees) }

	fun translate(tx: Double, ty: Double) { state.transform.pretranslate(tx, ty) }
	fun transform(m: Matrix) { state.transform.premultiply(m) }
	fun transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) { state.transform.premultiply(a, b, c, d, tx, ty) }

	fun setTransform(m: Matrix) { state.transform.copyFrom(m) }
	fun setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) { state.transform.setTo(a, b, c, d, tx, ty) }

	fun shear(sx: Double, sy: Double) = transform(1.0, sy, sx, 1.0, 0.0, 0.0)

    var moveX: Double = 0.0
    var moveY: Double = 0.0
    override var lastX: Double = 0.0
    override var lastY: Double = 0.0
    override val totalPoints: Int  get() = state.path.totalPoints

    override fun close() {
        state.path.close()
        lastX = moveX
        lastY = moveY
    }

    private fun transX(x: Double, y: Double) = state.transform.transformX(x, y)
    private fun transY(x: Double, y: Double) = state.transform.transformY(x, y)

    //private fun transX(x: Double, y: Double) = x
    //private fun transY(x: Double, y: Double) = y

    override fun moveTo(x: Double, y: Double) {
        state.path.moveTo(transX(x, y), transY(x, y))
        lastX = x
        lastY = y
        moveX = x
        moveY = y
    }
    override fun lineTo(x: Double, y: Double) {
        state.path.lineTo(transX(x, y), transY(x, y))
        lastX = x
        lastY = y
    }
    override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
        state.path.quadTo(
            transX(cx, cy), transY(cx, cy),
            transX(ax, ay), transY(ax, ay)
        )
        lastX = ax
        lastY = ay
    }
    override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
        state.path.cubicTo(
            transX(cx1, cy1), transY(cx1, cy1),
            transX(cx2, cy2), transY(cx2, cy2),
            transX(ax, ay), transY(ax, ay)
        )
        lastX = ax
        lastY = ay
    }

	inline fun strokeRect(x: Number, y: Number, width: Number, height: Number) =
		strokeRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRect(x: Number, y: Number, width: Number, height: Number) =
		fillRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number = rx) {
		beginPath()
		roundRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble())
		fill()
	}

	fun strokeDot(x: Double, y: Double) { beginPath(); moveTo(x, y); lineTo(x, y); stroke() }

	fun path(path: VectorPath) {
        if (this.isEmpty()) this.state.path.winding = path.winding
        this.write(path)
        //this.write(path, state.transform)
    }
	fun draw(d: Drawable) { d.draw(this) }

	fun strokeRect(x: Double, y: Double, width: Double, height: Double) { beginPath(); rect(x, y, width, height); stroke() }

	fun fillRect(x: Double, y: Double, width: Double, height: Double) { beginPath(); rect(x, y, width, height); fill() }

	fun beginPath() { state.path = VectorPath() }

	fun getBounds(out: Rectangle = Rectangle()) = state.path.getBounds(out)

	fun stroke() { if (state.strokeStyle != NonePaint) rendererRender(state, fill = false) }
    fun fill(winding: Winding? = null) { if (state.fillStyle != NonePaint) rendererRender(state, fill = true, winding = winding) }

    fun fill(paint: Paint, winding: Winding? = null) {
		this.fillStyle(paint) {
			this.fill(winding)
		}
	}

	fun stroke(paint: Paint) {
		this.strokeStyle(paint) {
			this.stroke()
		}
	}

    inline fun fill(paint: Paint, begin: Boolean = true, winding: Winding? = null, block: () -> Unit) {
        if (begin) beginPath()
        block()
        fill(paint, winding)
    }

    inline fun fill(color: RGBA, alpha: Double, begin: Boolean = true, winding: Winding? = null, block: () -> Unit) {
        fill(color.concatAd(alpha), begin, winding, block)
    }

    inline fun stroke(
        paint: Paint,
        lineWidth: Double = this.lineWidth,
        lineCap: LineCap = this.lineCap,
        lineJoin: LineJoin = this.lineJoin,
        miterLimit: Double = this.miterLimit,
        lineDash: IDoubleArrayList? = this.lineDash,
        lineDashOffset: Double = this.lineDashOffset,
        begin: Boolean = true,
        callback: () -> Unit = {}
    ) {
        if (begin) beginPath()
		callback()
        keep {
            this.lineWidth = lineWidth
            this.lineCap = lineCap
            this.lineJoin = lineJoin
            this.miterLimit = miterLimit
            this.lineDash = lineDash
            this.lineDashOffset = lineDashOffset
            stroke(paint)
        }
	}

    inline fun stroke(paint: Paint, info: StrokeInfo, begin: Boolean = true, callback: () -> Unit = {}) {
        stroke(paint, info.thickness, info.startCap, info.join, info.miterLimit, info.dash, info.dashOffset, begin, callback)
    }

    inline fun fillStroke(fill: Paint, stroke: Paint, strokeInfo: StrokeInfo? = null, callback: () -> Unit = {}) {
        callback()
        fill(fill)
        if (strokeInfo != null) stroke(stroke, strokeInfo, begin = false) else stroke(stroke, begin = false)
    }

    fun fillStroke() { fill(); stroke() }
    fun unclip() = clip(null)
    fun clip(path: VectorPath? = state.path, winding: Winding = Winding.NON_ZERO) {
        if (path != null) {
            if (state.clip == null) {
                state.clip = VectorPath()
            }
            state.clip!!.clear()
            state.clip!!.winding = winding
            state.clip!!.write(path)
            if (path === state.path) {
                path.clear()
            }
        } else {
            state.clip = null
        }
    }

    fun clip(buildClipShape: () -> Unit, useClipShape: () -> Unit) {
        val oldClip = state.clip
        try {
            buildClipShape()
            clip()
            useClipShape()
        } finally {
            state.clip = oldClip
        }
    }

    inline fun clip(path: VectorPath?, winding: Winding = Winding.NON_ZERO, block: () -> Unit) {
        val oldClip = state.clip
        state.clip = null
        try {
            clip(path, winding)
            block()
        } finally {
            state.clip = oldClip
        }
    }

    inline fun clip(path: VectorPath.() -> Unit, winding: Winding = Winding.NON_ZERO, block: () -> Unit) {
        clip(buildPath { path() }, winding, block)
    }

    fun drawShape(
		shape: Drawable,
		rasterizerMethod: ShapeRasterizerMethod = ShapeRasterizerMethod.X4,
        native: Boolean = true
	) {
		when (rasterizerMethod) {
			ShapeRasterizerMethod.NONE -> {
				shape.draw(this)
			}
			ShapeRasterizerMethod.X1, ShapeRasterizerMethod.X2, ShapeRasterizerMethod.X4 -> {
				val scale = rasterizerMethod.scale
                val oldState = state
				val newBi = NativeImageOrBitmap32(ceil(rendererWidth * scale).toInt(), ceil(rendererHeight * scale).toInt(), premultiplied = false, native = native).context2d(antialiased = false) {
                //val newBi = Bitmap32(ceil(rendererWidth * scale).toInt(), ceil(rendererHeight * scale).toInt(), premultiplied = false).context2d(antialiased = false) {
                    scale(scale)
                    transform(oldState.transform)
                    draw(shape)
                }
                val renderBi = when (rasterizerMethod) {
                    ShapeRasterizerMethod.X1 -> newBi
                    ShapeRasterizerMethod.X2 -> newBi.mipmap(1)
                    ShapeRasterizerMethod.X4 -> newBi.mipmap(2)
                    else -> newBi
                }
                keepTransform {
                    setTransform(Matrix())
                    this.rendererDrawImage(renderBi, 0.0, 0.0)
                }
				//} finally {
				//	bi.lineScale = oldLineScale
				//}
			}
		}
	}

    inline fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = LinearGradientPaint(x0, y0, x1, y1, cycle, transform, block)
    inline fun createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = RadialGradientPaint(x0, y0, r0, x1, y1, r1, cycle, transform, block)
    @Deprecated("Only available on Android or Bitmap32")
    inline fun createSweepGradient(x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = SweepGradientPaint(x0, y0, transform, block)
    inline fun createConicGradient(startAngle: Angle, x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = ConicGradientPaint(startAngle, x0, y0, transform, block)

    fun createColor(color: RGBA): RGBA = color
	fun createPattern(
		bitmap: Bitmap,
		repeat: Boolean = false,
		smooth: Boolean = true,
		transform: Matrix = Matrix()
	) = createPattern(
        bitmap, CycleMethod.fromRepeat(repeat), CycleMethod.fromRepeat(repeat), smooth, transform
    )

    fun createPattern(
        bitmap: Bitmap,
        cycleX: CycleMethod = CycleMethod.NO_CYCLE,
        cycleY: CycleMethod = cycleX,
        smooth: Boolean = true,
        transform: Matrix = Matrix()
    ) = BitmapPaint(bitmap, transform, cycleX, cycleY, smooth)

    fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics {
        val font = font
        if (font != null) {
            font.getTextBounds(fontSize, text, out = out)
        } else {
            out.clear()
        }
        return out
    }

    @Suppress("NOTHING_TO_INLINE") // Number inlining
    inline fun fillText(text: String, x: Number, y: Number): Unit =
        drawText(text, x.toDouble(), y.toDouble(), fill = true)

    @Suppress("NOTHING_TO_INLINE") // Number inlining
    inline fun strokeText(text: String, x: Number, y: Number): Unit =
        drawText(text, x.toDouble(), y.toDouble(), fill = false)

    @Suppress("NOTHING_TO_INLINE") // Number inlining
    inline fun fillText(
        text: String,
        x: Number,
        y: Number,
        font: Font? = this.font,
        textSize: Double = this.fontSize,
        halign: HorizontalAlign = this.horizontalAlign,
        valign: VerticalAlign = this.verticalAlign,
        color: Paint? = null
    ) {
        font(font, halign, valign, textSize) {
            fillStyle(color ?: fillStyle) {
                drawText(text, x.toDouble(), y.toDouble(), fill = true)
            }
        }
    }

    fun <T> drawText(text: T, x: Double = 0.0, y: Double = 0.0, fill: Boolean = true, paint: Paint? = null, font: Font? = this.font, size: Double = this.fontSize, renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>) {
        val paint = paint ?: (if (fill) this.fillStyle else this.strokeStyle)
        font?.drawText(this, size, text, paint, x, y, fill, renderer = renderer)
    }

    // @TODO: Fix this!
    open fun drawImage(image: Bitmap, x: Double, y: Double, width: Double = image.width.toDouble(), height: Double = image.height.toDouble()) =
        rendererDrawImage(image, x, y, width, height, state.transform)

    // @TODO: Fix this!
    inline fun drawImage(image: Bitmap, x: Number, y: Number, width: Number = image.width.toDouble(), height: Number = image.height.toDouble())
        = drawImage(image, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

}

fun RGBA.toFill() = ColorPaint(this)

fun Drawable.renderTo(ctx: Context2d) = ctx.draw(this)

fun SizedDrawable.filled(paint: Paint): SizedDrawable {
	return object : SizedDrawable by this {
		override fun draw(c: Context2d) {
			c.fillStyle = paint
			this@filled.draw(c)
			c.fill()
		}
	}
}

fun SizedDrawable.scaled(sx: Number = 1.0, sy: Number = sx): SizedDrawable {
	return object : SizedDrawable by this {
		override val width: Int = abs(this@scaled.width.toDouble() * sx.toDouble()).toInt()
		override val height: Int = abs(this@scaled.height.toDouble() * sy.toDouble()).toInt()

		override fun draw(c: Context2d) {
			c.scale(sx.toDouble(), sy.toDouble())
			this@scaled.draw(c)
		}
	}
}

fun SizedDrawable.translated(tx: Number = 0.0, ty: Number = tx): SizedDrawable {
	return object : SizedDrawable by this {
		override fun draw(c: Context2d) {
			c.translate(tx.toDouble(), ty.toDouble())
			this@translated.draw(c)
		}
	}
}

fun SizedDrawable.render(): NativeImage = render(native = true) as NativeImage
fun SizedDrawable.renderNoNative(): Bitmap32 = render(native = false) as Bitmap32

fun SizedDrawable.render(native: Boolean): Bitmap {
    return NativeImageOrBitmap32(this.width, this.height, native = native).context2d {
        this@render.draw(this)
    }
}

fun Drawable.renderToImage(width: Int, height: Int): NativeImage = renderToImage(width, height, native = true) as NativeImage

fun Drawable.renderToImage(width: Int, height: Int, native: Boolean): Bitmap {
    return NativeImageOrBitmap32(width, height, native = native).context2d {
        this@renderToImage.draw(this)
    }
}

private fun VectorBuilder.write(path: VectorPath) {
    path.visitCmds(
        moveTo = { x, y -> moveTo(x, y) },
        lineTo = { x, y -> lineTo(x, y) },
        quadTo = { x0, y0, x1, y1 -> quadTo(x0, y0, x1, y1) },
        cubicTo = { x0, y0, x1, y1, x2, y2 -> cubicTo(x0, y0, x1, y1, x2, y2) },
        close = { close() }
    )
}

private fun VectorBuilder.write(path: VectorPath, m: Matrix) {
    path.visitCmds(
        moveTo = { x, y -> moveTo(m.transformX(x, y), m.transformY(x, y)) },
        lineTo = { x, y -> lineTo(m.transformX(x, y), m.transformY(x, y)) },
        quadTo = { x0, y0, x1, y1 -> quadTo(m.transformX(x0, y0), m.transformY(x0, y0), m.transformX(x1, y1), m.transformY(x1, y1)) },
        cubicTo = { x0, y0, x1, y1, x2, y2 -> cubicTo(m.transformX(x0, y0), m.transformY(x0, y0), m.transformX(x1, y1), m.transformY(x1, y1), m.transformX(x2, y2), m.transformY(x2, y2)) },
        close = { close() }
    )
}
