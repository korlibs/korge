package korlibs.image.vector

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.image.vector.renderer.*
import korlibs.io.lang.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.math.*

open class Context2d(
    val renderer: Renderer,
    val defaultFontRegistry: FontRegistry? = null,
    val defaultFont: Font? = null
) : VectorBuilder, Disposable {
    var debug: Boolean
        get() = renderer.debug
        set(value) {
            renderer.debug = value
        }

    protected open val rendererWidth get() = renderer.width
    protected open val rendererHeight get() = renderer.height
    protected open fun rendererRender(state: State, fill: Boolean, winding: Winding? = null) =
        renderer.render(state, fill, winding)

    protected open fun rendererDrawImage(
        image: Bitmap,
        pos: Point,
        size: Size = image.size.toFloat(),
        transform: Matrix = Matrix.IDENTITY
    ) = renderer.drawImage(image, pos, size, transform)

    protected open fun rendererDispose() = renderer.dispose()
    protected open fun rendererBufferingStart() = renderer.bufferingStart()
    protected open fun rendererBufferingEnd() = renderer.bufferingEnd()
    protected open fun rendererRenderSystemText(
        state: State,
        font: Font?,
        fontSize: Double,
        text: String,
        pos: Point,
        fill: Boolean
    ) {
        font?.drawText(this, fontSize, text, if (fill) state.fillStyle else state.strokeStyle, pos, fill = fill)
    }

    fun fillText(text: String, pos: Point) =
        rendererRenderSystemText(state, font, fontSize, text, pos, fill = true)

    fun strokeText(text: String, pos: Point) =
        rendererRenderSystemText(state, font, fontSize, text, pos, fill = false)

    open val width: Int get() = rendererWidth
    open val height: Int get() = rendererHeight

    override fun dispose() {
        rendererDispose()
    }

    fun withScaledRenderer(scaleX: Float, scaleY: Float = scaleX): Context2d =
        if (scaleX == 1f && scaleY == 1f) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))

    class ScaledRenderer(val parent: Renderer, val scaleX: Float, val scaleY: Float) : Renderer() {
        override val width: Int get() = (parent.width / scaleX).toInt()
        override val height: Int get() = (parent.height / scaleY).toInt()

        fun Matrix.adjusted(): Matrix = this.scaled(this@ScaledRenderer.scaleX, this@ScaledRenderer.scaleY)

        private inline fun <T> adjustState(state: State, callback: () -> T): T {
            val old = state.transform
            try {
                state.transform = state.transform.adjusted()
                return callback()
            } finally {
                state.transform = old
            }
        }

        override fun renderFinal(state: State, fill: Boolean, winding: Winding?): Unit =
            adjustState(state) { parent.render(state, fill, winding) }
        //override fun renderText(state: State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean): Unit =
        //	adjustState(state) { parent.renderText(state, font, fontSize, text, x, y, fill) }

        override fun drawImage(image: Bitmap, pos: Point, size: Size, transform: Matrix) {
            parent.drawImage(image, pos, size, transform.adjusted())
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
        var transform: Matrix = Matrix.IDENTITY,
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
        var lineDash: DoubleList? = null,
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
            transform = transform,
            clip = clip?.clone(),
            path = path.clone(),
            lineDash = lineDash?.clone()
        )
    }

    var state = State(fontRegistry = defaultFontRegistry, font = defaultFont)
    private val stack = Stack<State>()

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
    var lineDash: DoubleList?; get() = state.lineDash ; set(value) { state.lineDash = value }
    var lineDashOffset: Double; get() = state.lineDashOffset ; set(value) { state.lineDashOffset = value }

    inline fun lineDash(lineDash: DoubleList?, lineDashOffset: Double = 0.0, callback: () -> Unit) {
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
        this.fillStyle = paint
        try {
            callback()
        } finally {
            this.fillStyle = oldStyle
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
        align: TextAlignment = this.alignment,
        size: Double = this.fontSize,
        callback: () -> Unit
    ) {
        val oldFont = this.font
        val oldFontSize = this.fontSize
        val oldAlign = this.alignment
        try {
            this.font = font
            this.fontSize = size
            this.alignment = align
            callback()
        } finally {
            this.font = oldFont
            this.fontSize = oldFontSize
            this.alignment = oldAlign
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
        try {
            callback()
        } finally {
            state.transform = t
        }
    }

    // @TODO: preallocate states and use it as a pool, and mutate the ones there
    fun save() {
        stack.push(state.clone())
    }

    fun restore() {
        state = stack.pop()
    }

    inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toDouble(), sy.toDouble())
    inline fun translate(tx: Number, ty: Number) = translate(tx.toDouble(), ty.toDouble())
    inline fun translate(pos: Point) = translate(pos.x.toDouble(), pos.y.toDouble())

    inline fun scale(sx: Int, sy: Int = sx) = scale(sx.toDouble(), sy.toDouble())
    inline fun translate(tx: Int, ty: Int) = translate(tx.toDouble(), ty.toDouble())

    inline fun skew(skewX: Angle = Angle.ZERO, skewY: Angle = Angle.ZERO, block: () -> Unit) =
        keep { skew(skewX, skewY).also { block() } }

    inline fun scale(sx: Number, sy: Number = sx, block: () -> Unit) = keep { scale(sx.toDouble(), sy.toDouble()).also { block() } }
    inline fun rotate(angle: Angle, block: () -> Unit) = keep { rotate(angle).also { block() } }
    inline fun translate(tx: Number, ty: Number, block: () -> Unit) = keep { translate(tx.toDouble(), ty.toDouble()).also { block() } }

    fun skew(skewX: Angle = 0.degrees, skewY: Angle = 0.degrees) {
        state.transform = state.transform.preskewed(skewX, skewY)
    }

    fun scale(sx: Double, sy: Double = sx) {
        state.transform = state.transform.prescaled(sx, sy)
    }

    fun rotate(angle: Angle) {
        state.transform = state.transform.prerotated(angle)
    }

    fun translate(tx: Double, ty: Double) {
        state.transform = state.transform.pretranslated(tx, ty)
    }

    fun transform(m: Matrix) {
        state.transform = state.transform.premultiplied(m)
    }

    fun transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) {
        state.transform = state.transform.premultiplied(Matrix(a, b, c, d, tx, ty))
    }

    fun setTransform(m: Matrix) {
        state.transform = m
    }

    fun setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) {
        state.transform = Matrix(a, b, c, d, tx, ty)
    }

    fun shear(sx: Double, sy: Double) = transform(1.0, sy, sx, 1.0, 0.0, 0.0)

    override var lastMovePos: Point = Point()
    override var lastPos: Point = Point()
    override val totalPoints: Int get() = state.path.totalPoints

    override fun close() {
        state.path.close()
        lastPos = lastMovePos
    }

    private fun trans(p: Point): Point = state.transform.transform(p)
    private fun transX(x: Double, y: Double) = state.transform.transformX(x, y)
    private fun transY(x: Double, y: Double) = state.transform.transformY(x, y)

    //private fun transX(x: Double, y: Double) = x
    //private fun transY(x: Double, y: Double) = y

    override fun moveTo(p: Point) {
        state.path.moveTo(trans(p))
        lastPos = p
        lastMovePos = p
    }

    override fun lineTo(p: Point) {
        state.path.lineTo(trans(p))
        lastPos = p
    }

    override fun quadTo(c: Point, a: Point) {
        state.path.quadTo(trans(c), trans(a))
        lastPos = a
    }

    override fun cubicTo(c1: Point, c2: Point, a: Point) {
        state.path.cubicTo(trans(c1), trans(c2), trans(a))
        lastPos = a
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

    fun strokeDot(x: Double, y: Double) {
        beginPath(); moveTo(Point(x, y)); lineTo(Point(x, y)); stroke()
    }

    fun path(path: VectorPath) {
        if (this.isEmpty()) this.state.path.winding = path.winding
        this.write(path)
        //this.write(path, state.transform)
    }

    fun draw(d: Drawable) {
        d.draw(this)
    }

    fun strokeRect(x: Double, y: Double, width: Double, height: Double) {
        beginPath(); rect(x, y, width, height); stroke()
    }

    fun fillRect(x: Double, y: Double, width: Double, height: Double) {
        beginPath(); rect(x, y, width, height); fill()
    }

    fun beginPath() {
        state.path = VectorPath()
    }

    fun getBounds(): Rectangle = state.path.getBounds()

    fun stroke() {
        if (state.strokeStyle != NonePaint) rendererRender(state, fill = false)
    }

    fun fill(winding: Winding? = null) {
        if (state.fillStyle != NonePaint) rendererRender(state, fill = true, winding = winding)
    }

    fun fill(paint: Paint?, winding: Winding? = null) {
        if (paint == null) return
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
        lineWidth: Number = this.lineWidth,
        lineCap: LineCap = this.lineCap,
        lineJoin: LineJoin = this.lineJoin,
        miterLimit: Number = this.miterLimit,
        lineDash: DoubleList? = this.lineDash,
        lineDashOffset: Number = this.lineDashOffset,
        begin: Boolean = true,
        callback: () -> Unit = {}
    ) {
        if (begin) beginPath()
        callback()
        keep {
            this.lineWidth = lineWidth.toDouble()
            this.lineCap = lineCap
            this.lineJoin = lineJoin
            this.miterLimit = miterLimit.toDouble()
            this.lineDash = lineDash
            this.lineDashOffset = lineDashOffset.toDouble()
            stroke(paint)
        }
    }

    inline fun stroke(paint: Paint?, info: StrokeInfo?, begin: Boolean = true, callback: () -> Unit = {}) {
        if (paint == null || info == null) return
        stroke(
            paint,
            info.thickness,
            info.startCap,
            info.join,
            info.miterLimit,
            info.dash,
            info.dashOffset,
            begin,
            callback
        )
    }

    inline fun stroke(stroke: Stroke?, begin: Boolean = true, callback: () -> Unit = {}) {
        stroke(stroke?.paint, stroke?.info, begin, callback)
    }

    inline fun fillStroke(fill: Paint?, stroke: Paint?, strokeInfo: StrokeInfo? = null, callback: () -> Unit = {}) {
        callback()
        if (fill != null) fill(fill)
        if (stroke != null) {
            when {
                strokeInfo != null -> stroke(stroke, strokeInfo, begin = false)
                else -> stroke(stroke, begin = false)
            }
        }
    }

    inline fun fillStroke(fill: Paint?, stroke: Stroke?, callback: () -> Unit = {}) {
        fillStroke(fill, stroke?.paint, stroke?.info, callback)
    }

    fun fillStroke() {
        fill(); stroke()
    }

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
        clip(buildVectorPath(VectorPath()) {
            path()
        }, winding, block)
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
                val newBi = NativeImageOrBitmap32(
                    ceil(rendererWidth * scale).toInt(),
                    ceil(rendererHeight * scale).toInt(),
                    premultiplied = false,
                    native = native
                ).context2d(antialiased = false) {
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
                    setTransform(Matrix.IDENTITY)
                    this.rendererDrawImage(renderBi, Point.ZERO)
                }
                //} finally {
                //	bi.lineScale = oldLineScale
                //}
            }
        }
    }

    inline fun createLinearGradient(
        x0: Number,
        y0: Number,
        x1: Number,
        y1: Number,
        cycle: CycleMethod = CycleMethod.NO_CYCLE,
        transform: Matrix = Matrix.IDENTITY,
        block: GradientPaint.() -> Unit = {}
    ) = LinearGradientPaint(x0, y0, x1, y1, cycle, transform, block)

    inline fun createRadialGradient(
        x0: Number,
        y0: Number,
        r0: Number,
        x1: Number,
        y1: Number,
        r1: Number,
        cycle: CycleMethod = CycleMethod.NO_CYCLE,
        transform: Matrix = Matrix.IDENTITY,
        block: GradientPaint.() -> Unit = {}
    ) = RadialGradientPaint(x0, y0, r0, x1, y1, r1, cycle, transform, block)

    inline fun createSweepGradient(
        x0: Number,
        y0: Number,
        startAngle: Angle = Angle.ZERO,
        transform: Matrix = Matrix.IDENTITY,
        block: GradientPaint.() -> Unit = {}
    ) = SweepGradientPaint(x0, y0, startAngle, transform, block)

    fun createColor(color: RGBA): RGBA = color
    fun createPattern(
        bitmap: Bitmap,
        repeat: Boolean = false,
        smooth: Boolean = true,
        transform: Matrix = Matrix.IDENTITY,
    ) = createPattern(
        bitmap, CycleMethod.fromRepeat(repeat), CycleMethod.fromRepeat(repeat), smooth, transform
    )

    fun createPattern(
        bitmap: Bitmap,
        cycleX: CycleMethod = CycleMethod.NO_CYCLE,
        cycleY: CycleMethod = cycleX,
        smooth: Boolean = true,
        transform: Matrix = Matrix.IDENTITY,
    ) = BitmapPaint(bitmap, transform, cycleX, cycleY, smooth)

    fun getTextBounds(
        text: String,
        out: TextMetrics = TextMetrics(),
        fontSize: Double = this.fontSize,
        renderer: TextRenderer<String> = DefaultStringTextRenderer,
        align: TextAlignment = this.alignment,
    ): TextMetrics {
        val font = font
        if (font != null) {
            font.getTextBounds(fontSize, text, out = out, renderer = renderer, align = align)
        } else {
            out.clear()
        }
        return out
    }

    @Suppress("NOTHING_TO_INLINE") // Number inlining
    inline fun fillText(
        text: String,
        pos: Point,
        font: Font? = this.font,
        size: Number = this.fontSize,
        align: TextAlignment = this.alignment,
        color: Paint? = null
    ) {
        this.drawText(text, pos, fill = true, size = size.toDouble(), align = align, fillStyle = color, font = font)
    }

    fun <T> drawText(
        text: T,
        pos: Point = Point.ZERO,

        fill: Boolean = true, // Deprecated parameter
        paint: Paint? = null, // Deprecated parameter

        font: Font? = this.font,
        size: Double = this.fontSize,
        renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
        align: TextAlignment = this.alignment,
        outMetrics: TextMetricsResult? = null,

        fillStyle: Paint? = null,
        stroke: Stroke? = null,

        textRangeStart: Int = 0,
        textRangeEnd: Int = Int.MAX_VALUE,
    ): TextMetricsResult? {
        val paint = paint ?: (if (fill) this.fillStyle else this.strokeStyle)
        return font?.drawText(
            this, size, text, paint, pos, fill,
            renderer = renderer, align = align, outMetrics = outMetrics,
            fillStyle = fillStyle, stroke = stroke,
            textRangeStart = textRangeStart,
            textRangeEnd = textRangeEnd,
        )
    }

    open fun drawImage(image: Bitmap, pos: Point, size: Size = image.size.toFloat()) = rendererDrawImage(image, pos, size, state.transform)

    // @TODO: Implement this!
    // open fun drawImage(image: BmpSlice, pos: Point, size: Size = image.size.toFloat()) = rendererDrawImage(image, pos, size, state.transform)

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
        moveTo = { moveTo(it) },
        lineTo = { lineTo(it) },
        quadTo = { c, a -> quadTo(c, a) },
        cubicTo = { c1, c2, a -> cubicTo(c1, c2, a) },
        close = { close() }
    )
}

private fun VectorBuilder.write(path: VectorPath, m: Matrix) {
    path.visitCmds(
        moveTo = { moveTo(m.transform(it)) },
        lineTo = { lineTo(m.transform(it)) },
        quadTo = { c, a -> quadTo(m.transform(c), m.transform(a)) },
        cubicTo = { c1, c2, a -> cubicTo(m.transform(c1), m.transform(c2), m.transform(a)) },
        close = { close() }
    )
}

fun Paint.toBitmapPaint(state: Context2d.State): BitmapPaint {
    val filler: BaseFiller = this.toFiller(state)
    var bb = BoundsBuilder()
    bb += state.path.getBounds()
    bb += state.clip?.getBounds()
    val bounds = bb.bounds.transformed(state.transform)
    // @TODO: Make it work for negative x, y, and for other transforms
    //println("bounds=$bounds")
    val bmp = Bitmap32(bounds.width.toIntCeil(), bounds.height.toIntCeil(), premultiplied = true).also { filler.fill(it) }
    return BitmapPaint(bmp, Matrix().translated(-bounds.left, -bounds.top))
}
