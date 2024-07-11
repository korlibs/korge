package korlibs.korge.view.vector

import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.BlendMode
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.geom.Line
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.getPoints2List
import korlibs.math.geom.shape.cachedPoints
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.measureTime
import kotlin.math.absoluteValue

//@KorgeExperimental
inline fun Container.gpuGraphics(
    build: ShapeBuilder.() -> Unit,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
): GpuShapeView = gpuShapeView(build, antialiased, callback)

inline fun Container.gpuGraphics(
    shape: Shape,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
): GpuShapeView = gpuShapeView(shape, antialiased, callback)

//@KorgeExperimental
inline fun Container.gpuGraphics(
    antialiased: Boolean = true,
    callback: @ViewDslMarker ShapeBuilder.(GpuShapeView) -> Unit = {}
): GpuShapeView = gpuShapeView(antialiased, callback)

//@KorgeExperimental
inline fun Container.gpuShapeView(
    build: ShapeBuilder.() -> Unit,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
): GpuShapeView = GpuShapeView(buildShape { build() }, antialiased).addTo(this, callback)

inline fun Container.gpuShapeView(
    shape: Shape,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
): GpuShapeView = GpuShapeView(shape, antialiased).addTo(this, callback)

//@KorgeExperimental
inline fun Container.gpuShapeView(
    antialiased: Boolean = true,
    callback: @ViewDslMarker ShapeBuilder.(GpuShapeView) -> Unit = {}
): GpuShapeView = GpuShapeView(EmptyShape, antialiased)
    .also { it.updateShape { callback(this, it) } }
    .addTo(this)

//@KorgeExperimental
@OptIn(KorgeInternal::class)
//open class GpuGraphics(
open class GpuShapeView(
    shape: Shape = EmptyShape,
    antialiased: Boolean = true,
    // @TODO: Not used, but to be compatible with Graphics
    var autoScaling: Boolean = true
) : View(), Anchorable {
    /** Use for compatibility with [BaseGraphics] */
    var useNativeRendering: Boolean = true
    /** Use for compatibility with [BaseGraphics] */
    var smoothing: Boolean = true
    /** Use for compatibility with [BaseGraphics] */
    //var boundsIncludeStrokes: Boolean = false
    var boundsIncludeStrokes: Boolean = true
        set(value) {
            field = value
            invalidateLocalBounds()
        }

    private val gpuShapeViewCommands = GpuShapeViewCommands()
    var bufferWidth = 1000
    var bufferHeight = 1000
    //private var notifyAboutEvenOdd = false

    override var anchor: Anchor = Anchor.TOP_LEFT ; set(value) { field = value; invalidate() }

    var applyScissor: Boolean = true

    var antialiasedMasks: Boolean = false

    @ViewProperty
    var antialiased: Boolean = antialiased
        set(value) {
            field = value
            invalidateShape()
        }
    var shape: Shape = shape
        set(value) {
            field = value
            invalidateShape()
        }

    private var validShape = false
    private var validShapeBounds = false
    private var validShapeBoundsStrokes = false
    private var renderCount = 0
    private var _shapeBounds: Rectangle = Rectangle()
    private var _shapeBoundsStrokes: Rectangle = Rectangle()
    private val shapeBounds: Rectangle
        get() {
            val valid = if (boundsIncludeStrokes) validShapeBoundsStrokes else validShapeBounds
            if (!valid) {
                if (boundsIncludeStrokes) validShapeBoundsStrokes = true else validShapeBounds = true
                val result = shape.getBounds(includeStrokes = boundsIncludeStrokes)
                if (boundsIncludeStrokes) _shapeBoundsStrokes = result else _shapeBounds = result
                return result
            }
            return if (boundsIncludeStrokes) _shapeBoundsStrokes else _shapeBounds
        }

    val shapeWidth: Double get() = shapeBounds.width.toDouble()
    val shapeHeight: Double get() = shapeBounds.height.toDouble()
    private var lastCommandWasClipped: Boolean = false

    override val anchorDispX: Float get() = shapeBounds.width.toFloat() * anchor.sx.toFloat()
    override val anchorDispY: Float get() = shapeBounds.height.toFloat() * anchor.sy.toFloat()

    private fun invalidateShape() {
        renderCount = 0
        validShape = false
        validShapeBounds = false
        validShapeBoundsStrokes = false
        //strokeCache.clear()
        invalidateRender()
    }

    private fun requireShape() {
        if (validShape) return
        validShape = true
        gpuShapeViewCommands.clear()
        gpuShapeViewCommands.clearStencil()
        gpuShapeViewCommands.setScissor(AGScissor.NIL)
        lastCommandWasClipped = true
        renderShape(shape)
        gpuShapeViewCommands.finish()
        cachedScale = globalScale
    }

    override fun invalidate() {
        super.invalidate()
        invalidateShape()
    }

    override fun getLocalBoundsInternal(): Rectangle = Rectangle(
        shapeBounds.x - anchorDispX,
        shapeBounds.y - anchorDispY,
        shapeBounds.width,
        shapeBounds.height,
    )

    inline fun updateShape(block: ShapeBuilder.() -> Unit) {
        this.shape = buildShape { block() }
    }

    val Shape.requireStencil: Boolean
        get() {
            //return false
            return when (this) {
                EmptyShape -> false
                is CompoundShape -> this.components.any { it.requireStencil }
                is TextShape -> this.primitiveShapes.requireStencil
                is FillShape -> !this.isConvex
                is PolylineShape -> false
                else -> true // UNKNOWN
            }
        }

    private var globalScale: Float = 1f
    private var cachedScale: Float = Float.NaN

    override fun renderInternal(ctx: RenderContext) {
        globalScale = (globalMatrix.toTransform().scaleAvg * ctx.bp.globalToWindowScaleAvg).toFloat()
        //globalScale = ctx.bp.globalToWindowScaleAvg
        if (cachedScale != globalScale) {
            invalidateShape()
        }
        //invalidateShape()
        ctx.flush()

        val doRequireTexture = shape.requireStencil
        //val doRequireTexture = false

        //println("doRequireTexture=$doRequireTexture")

        val time = kotlin.time.measureTime {
            if (doRequireTexture) {
                //val currentRenderBuffer = ctx.ag.currentRenderBufferOrMain
                val currentFrameBuffer = ctx.ag.mainFrameBuffer
                //ctx.renderToTexture(currentRenderBuffer.width, currentRenderBuffer.height, {
                bufferWidth = currentFrameBuffer.width
                bufferHeight = currentFrameBuffer.height
                //println("bufferWidth=$bufferWidth, bufferHeight=$bufferHeight")
                //println("ctx.currentFrameBuffer: ${ctx.currentFrameBuffer}")
                ctx.renderToTexture(bufferWidth, bufferHeight, {
                    //println("!!Render to texture!! :: ctx.currentFrameBuffer: ${ctx.currentFrameBuffer}")
                    renderCommands(ctx, doRequireTexture)
                }, hasDepth = false, hasStencil = true, msamples = 1) { texture ->
                    ctx.useBatcher {
                        it.drawQuad(texture, m = ctx.bp.windowToGlobalMatrix.immutable)
                    }
                }
            } else {
                renderCommands(ctx, doRequireTexture)
            }
        }

        //println("GPU RENDER IN: $time, doRequireTexture=$doRequireTexture")
    }

    private var renderMat = Matrix()
    private fun renderCommands(ctx: RenderContext, doRequireTexture: Boolean) {
        val mat = if (doRequireTexture) globalMatrix * ctx.bp.globalToWindowMatrix.immutable else globalMatrix
        renderMat = mat.pretranslated(-anchorDispX, -anchorDispY)
        requireShape()
        gpuShapeViewCommands.render(ctx, renderMat, localMatrix, applyScissor, renderColorMul, doRequireTexture)
    }

    private fun renderShape(shape: Shape) {
        when (shape) {
            EmptyShape -> Unit
            is CompoundShape -> for (v in shape.components) renderShape(v)
            is TextShape -> renderShape(shape.primitiveShapes)
            is FillShape -> {
                renderCount++
                if (renderCount > maxRenderCount) return
                renderFill(shape)
            }
            is PolylineShape -> {
                renderCount++
                if (renderCount > maxRenderCount) return
                renderStroke(
                    shape.transform,
                    shape.path,
                    shape.paint,
                    shape.globalAlpha.toFloat(),
                    shape.strokeInfo,
                )
            }
            //is PolylineShape -> renderShape(ctx, shape.fillShape)
            else -> TODO("shape=$shape")
        }
    }

    class SegmentInfo {
        var s: Point = Point.ZERO // start
        var e: Point = Point.ZERO // end
        var line: Line = Line.ZERO
        var angleSE: Angle = 0.degrees
        var angleSE0: Angle = 0.degrees
        var angleSE1: Angle = 0.degrees
        var s0: Point = Point.ZERO
        var s1: Point = Point.ZERO
        var e0: Point = Point.ZERO
        var e1: Point = Point.ZERO
        var e0s: Point = Point.ZERO
        var e1s: Point = Point.ZERO
        var s0s: Point = Point.ZERO
        var s1s: Point = Point.ZERO

        fun p(index: Int) = if (index == 0) s else e
        fun p0(index: Int) = if (index == 0) s0 else e0
        fun p1(index: Int) = if (index == 0) s1 else e1

        fun setTo(s: Point, e: Point, lineWidth: Double) {
            this.s = s
            this.e = e
            line = Line(s, e)
            angleSE = Angle.between(s, e)
            angleSE0 = angleSE - 90.degrees
            angleSE1 = angleSE + 90.degrees
            s0 = Point.polar(s, angleSE0, length = lineWidth)
            s1 = Point.polar(s, angleSE1, length = lineWidth)
            e0 = Point.polar(e, angleSE0, length = lineWidth)
            e1 = Point.polar(e, angleSE1, length = lineWidth)

            s0s = Point.polar(s0, angleSE + 180.degrees, length = lineWidth)
            s1s = Point.polar(s1, angleSE + 180.degrees, length = lineWidth)

            e0s = Point.polar(e0, angleSE, length = lineWidth)
            e1s = Point.polar(e1, angleSE, length = lineWidth)
        }
    }

    private fun pointsAdd(p1: Point, p2: Point, lineWidth: Float) {
        //val lineWidth = 0f
        val p1x = p1.x.toFloat()
        val p1y = p1.y.toFloat()
        val p2x = p2.x.toFloat()
        val p2y = p2.y.toFloat()
        gpuShapeViewCommands.addVertex(p1x, p1y, len = -lineWidth, maxLen = lineWidth)
        gpuShapeViewCommands.addVertex(p2x, p2y, len = +lineWidth, maxLen = lineWidth)
    }

    private fun pointsAddCubicOrLine(
        fix: Point,
        p0: Point, p0s: Point, p1s: Point, p1: Point,
        lineWidth: Double,
        reverse: Boolean = false,
        start: Boolean = true,
    ) {
        val NPOINTS = 15
        Ratio.forEachRatio(NPOINTS) { ratio ->
            val pos = when {
                start -> Bezier.cubicCalc(p0, p0s, p1s, p1, ratio)
                else -> Bezier.cubicCalc(p1, p1s, p0s, p0, ratio)
            }
            when {
                reverse -> pointsAdd(fix, pos, lineWidth.toFloat())
                else -> pointsAdd(pos, fix, lineWidth.toFloat())
            }
        }
    }

    //data class StrokeRenderCacheKey(
    //    val lineWidth: Double,
    //    val path: VectorPath,
    //    // @TODO: we shouldn't require this matrix. We should be able to compute everything without the matrix, and apply it at the shader level
    //    val matrix: Matrix
    //)

    //private val strokeCache = HashMap<StrokeRenderCacheKey, StrokeRenderData>()

    private fun renderStroke(
        stateTransform: Matrix,
        strokePath: VectorPath,
        paint: Paint,
        globalAlpha: Float,
        strokeInfo: StrokeInfo,
        forceClosed: Boolean? = null,
        stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
    ) {
        val gpuShapeViewCommands = this.gpuShapeViewCommands

        val pointsList = strokePath.toCurvesList().toStrokePointsList(
            strokeInfo, StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH,
            forceClosed = forceClosed
        )

        gpuShapeViewCommands.setScissor(AGScissor.NIL)

        pointsList.fastForEach { points ->
            val startIndex = gpuShapeViewCommands.verticesStart()
            val vector = points.vector
            vector.fastForEachGeneric { index ->
                val x = vector[index, 0]
                val y = vector[index, 1]
                val dx = vector[index, 2]
                val dy = vector[index, 3]
                val len = vector[index, 4]
                val maxLen = vector[index, 5].absoluteValue

                val px = x + dx * len
                val py = y + dy * len

                //println("x=$x, y=$y, len=$len, maxLen=$maxLen")

                gpuShapeViewCommands.addVertex(
                    px.toFloat(), py.toFloat(),
                    if (antialiased) len.toFloat() else 0f,
                    if (antialiased) maxLen.toFloat() else BIG_MAX_LEN
                )
            }

            val endIndex = gpuShapeViewCommands.verticesEnd()

            val info = GpuShapeViewPrograms.paintToShaderInfo(
                stateTransform = stateTransform,
                paint = paint,
                globalAlpha = globalAlpha,
                lineWidth = strokeInfo.thickness.toFloat(),
            )

            //gpuShapeViewCommands.setScissor(null)
            gpuShapeViewCommands.draw(
                AGDrawType.TRIANGLE_STRIP, info, stencilOpFunc = stencilOpFunc, stencilRef = stencilRef, startIndex = startIndex, endIndex = endIndex,
                blendMode = BlendMode.NORMAL
            )
        }
    }

    class PointsResult(val bounds: AGScissor, val vertexCount: Int, val vertexStart: Int, val vertexEnd: Int)

    private fun getPointsForPath(points: PointList, type: AGDrawType): PointsResult? {
        if (points.size < 3) return null
        val vertexStart = gpuShapeViewCommands.verticesStart()
        val bb = BoundsBuilder() + points
        val xMid = ((bb.xmax + bb.xmin) / 2)
        val yMid = ((bb.ymax + bb.ymin) / 2)
        val pMid = Point(xMid, yMid)

        val isStrip = type == AGDrawType.TRIANGLE_STRIP

        val antialiased = this.antialiased
        val isStripAndAntialiased = antialiased && isStrip
        //val isStripAndAntialiased = isStrip

        if (!isStrip) {
            gpuShapeViewCommands.addVertex(xMid.toFloat(), yMid.toFloat(), len = 0f, maxLen = BIG_MAX_LEN)
        }
        for (n in 0 until points.size + 1) {
            val p = points[n % points.size]
            val (x, y) = p
            val len = if (isStripAndAntialiased) Point.distance(p, pMid) else 0.0
            val maxLen = if (isStripAndAntialiased) len else BIG_MAX_LEN.toDouble()
            if (isStrip) {
                gpuShapeViewCommands.addVertex(xMid.toFloat(), yMid.toFloat(), len = 0f, maxLen = maxLen.toFloat())
            }
            gpuShapeViewCommands.addVertex(x.toFloat(), y.toFloat(), len = len.toFloat(), maxLen = maxLen.toFloat())
        }
        val vertexEnd = gpuShapeViewCommands.verticesEnd()
        //println("bb.getBounds()=${bb.getBounds()} - ${bb.getBounds().toAGScissor()}")
        return PointsResult(bb.bounds.toAGScissor(), points.size + 2, vertexStart, vertexEnd)
    }

    private fun getPointsForPath(path: VectorPath, type: AGDrawType): PointsResult? {
        return getPointsForPath(path.cachedPoints, type)
    }

    private fun getPointsForPathList(path: VectorPath, type: AGDrawType): List<PointsResult> {
        return path.getPoints2List().mapNotNull { getPointsForPath(it, type) }
    }

    @ViewProperty(min = 1.0, max = 1.0)
    var maxRenderCount: Int = 100_000
    //var maxRenderCount: Int = 1
    //var maxRenderCount: Int = 14
        set(value) {
            field = value
            invalidateShape()
        }

    //init {
    //    keys {
    //        down(Key.UP) { maxRenderCount++ }
    //        down(Key.DOWN) { maxRenderCount-- }
    //    }
    //}

    @ViewProperty
    var debugDrawOnlyAntialiasedBorder = false
        set(value) {
            field = value
            invalidateShape()
        }

    private fun renderFill(shape: FillShape) {
        //println("maxRenderCount=$maxRenderCount")
        //println("renderCount=$renderCount")
        val paintShader = GpuShapeViewPrograms.paintToShaderInfo(
            shape.transform, shape.paint, shape.globalAlpha.toFloat(),
            lineWidth = 10000000f,
        ) ?: return

        val drawFill = !debugDrawOnlyAntialiasedBorder
        val drawAntialiasingBorder = if (debugDrawOnlyAntialiasedBorder) true else antialiased

        val shapeIsConvex = shape.isConvex
        val isSimpleDraw = shapeIsConvex && shape.clip == null && !debugDrawOnlyAntialiasedBorder
        //val isSimpleDraw = false
        val pathDataList = getPointsForPathList(shape.path, if (isSimpleDraw) AGDrawType.TRIANGLE_STRIP else AGDrawType.TRIANGLE_FAN)
        val pathBoundsNoExpanded: Rectangle = run {
            var bb = BoundsBuilder()
            pathDataList.fastForEach {
                //println("bounds=${it.bounds}")
                bb += it.bounds
            }
            bb.bounds
        }
        val pathBounds: AGScissor = pathBoundsNoExpanded.expanded(MarginInt(2)).toAGScissor()
        //println("pathBounds=$pathBounds")

        val clipDataStart = gpuShapeViewCommands.verticesStart()
        val clipData = shape.clip?.let { getPointsForPath(it, AGDrawType.TRIANGLE_FAN) }
        val clipDataEnd = gpuShapeViewCommands.verticesEnd()
        val clipBounds: AGScissor = clipData?.bounds ?: AGScissor.NIL

        //println("shapeIsConvex=$shapeIsConvex, isSimpleDraw=$isSimpleDraw, drawAntialiasingBorder=$drawAntialiasingBorder, pathBounds=$pathBounds")

        if (!isSimpleDraw || lastCommandWasClipped) {
            lastCommandWasClipped = true
            gpuShapeViewCommands.setScissor(when {
                isSimpleDraw -> AGScissor.NIL
                clipBounds != AGScissor.NIL -> AGScissor.combine(pathBounds, clipBounds)
                else -> pathBounds
            })
            //gpuShapeViewCommands.clearStencil(0)
        }

        if (isSimpleDraw) {
            lastCommandWasClipped = false
        //if (false) {
            //println("convex!")
            pathDataList.fastForEach { pathData ->
                gpuShapeViewCommands.draw(
                    AGDrawType.TRIANGLE_STRIP,
                    startIndex = pathData.vertexStart,
                    endIndex = pathData.vertexEnd,
                    paintShader = paintShader,
                    colorMask = AGColorMask(true),
                    blendMode = renderBlendMode,
                )
            }
            return
        }

        val winding = shape.path.winding
        //if (winding != Winding.EVEN_ODD) {
        //    if (!notifyAboutEvenOdd) {
        //        notifyAboutEvenOdd = true
        //        Console.error("ERROR: Currently only supported EVEN_ODD winding, but used $winding")
        //    }
        //}

        var stencilReferenceValue: Int = 0b00000001
        var stencilCompare: AGCompareMode = AGCompareMode.EQUAL
        if (drawFill) {
            when (winding) {
                Winding.EVEN_ODD -> {
                    stencilReferenceValue = 0b00000001
                    stencilCompare = AGCompareMode.EQUAL
                    val stencilOpFunc = AGStencilOpFunc.DEFAULT.withEnabled(enabled = true).withCompareMode(compareMode = AGCompareMode.ALWAYS).withActionOnBothPass(AGStencilOp.INVERT)
                    val stencilRef = AGStencilReference.DEFAULT.withWriteMask(writeMask = 0b00000001)
                    pathDataList.fastForEach { pathData ->
                        writeStencil(pathData.vertexStart, pathData.vertexEnd, stencilOpFunc, stencilRef, cullFace = AGCullFace.NONE)
                    }
                }
                Winding.NON_ZERO -> {
                    stencilReferenceValue = 0b00000000
                    stencilCompare = AGCompareMode.NOT_EQUAL
                    val stencilOpFunc = AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.ALWAYS)
                    val stencilRef = AGStencilReference.DEFAULT.withWriteMask(0xFF)
                    pathDataList.fastForEach { pathData ->
                        writeStencil(pathData.vertexStart, pathData.vertexEnd, stencilOpFunc.withActionOnBothPass(
                            actionOnBothPass = AGStencilOp.INCREMENT_WRAP,
                            actionOnBothPassBack = AGStencilOp.DECREMENT_WRAP,
                        ), stencilRef, cullFace = AGCullFace.NONE)
                    }
                }
            }

            // @TODO: Should we do clipping other way?
            if (clipData != null) {
                if (winding == Winding.NON_ZERO) {
                    writeStencil(
                        clipDataStart, clipDataEnd,
                        AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.NOT_EQUAL).withActionOnBothPass(AGStencilOp.INVERT),
                        AGStencilReference.DEFAULT.withReadMask(0).withWriteMask(0xFF),
                        cullFace = AGCullFace.FRONT
                    )
                }
                // @TODO: This won't work with NON_ZERO filling

                writeStencil(
                    clipDataStart, clipDataEnd,
                    AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.ALWAYS).withActionOnBothPass(AGStencilOp.INVERT),
                    AGStencilReference.DEFAULT.withWriteMask(0b00000010),
                    AGCullFace.BOTH
                )
                stencilReferenceValue = 0b00000011
            }
        }

        // Antialias when we don't have clipping
        // @TODO: How should we handle clipping antialiasing? Should we render the mask into a buffer first, and then do the masking?
        if (drawAntialiasingBorder && shape.clip == null) {
        //if (true) {
        //if (false) {
            //println("globalScale=$globalScale")
            renderStroke(
                stateTransform = shape.transform,
                strokePath = shape.path,
                paint = shape.paint,
                globalAlpha = shape.globalAlpha.toFloat(),
                strokeInfo = StrokeInfo(
                    thickness = (1.6 / globalScale),
                    scaleMode = LineScaleMode.NONE,
                    startCap = LineCap.BUTT,
                    endCap = LineCap.BUTT,
                    join = LineJoin.MITER,
                    miterLimit = 5.0,
                ),
                forceClosed = true,
                stencilOpFunc = if (!drawFill) AGStencilOpFunc.DEFAULT else AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(stencilCompare.inverted()),
                stencilRef = AGStencilReference.DEFAULT.withReferenceValue(stencilReferenceValue).withWriteMask(0)
            )
        }

        if (drawFill) {
            writeFill(paintShader, stencilReferenceValue, pathBounds, pathDataList, stencilCompare)
            gpuShapeViewCommands.clearStencil(0)
        }

        // renderFill
    }

    private fun writeStencil(pathDataStart: Int, pathDataEnd: Int, stencilOpFunc: AGStencilOpFunc, stencilRef: AGStencilReference, cullFace: AGCullFace) {
        gpuShapeViewCommands.draw(
            AGDrawType.TRIANGLE_FAN,
            startIndex = pathDataStart,
            endIndex = pathDataEnd,
            paintShader = GpuShapeViewPrograms.stencilPaintShader,
            colorMask = AGColorMask(false, false, false, false),
            blendMode = BlendMode.NONE,
            stencilOpFunc = stencilOpFunc,
            stencilRef = stencilRef,
            cullFace = cullFace
        )
    }

    val BIG_MAX_LEN = 10000f

    private fun writeFill(
        paintShader: GpuShapeViewPrograms.PaintShader,
        stencilReferenceValue: Int,
        pathBounds: AGScissor,
        pathDataList: List<PointsResult>,
        stencilCompare: AGCompareMode, //= AGCompareMode.EQUAL
    ) {
        val vstart = gpuShapeViewCommands.verticesStart()
        val x0 = pathBounds.left.toFloat()
        val y0 = pathBounds.top.toFloat()
        val x1 = pathBounds.right.toFloat()
        val y1 = pathBounds.bottom.toFloat()
        gpuShapeViewCommands.addVertex(x0, y0, 0f, BIG_MAX_LEN)
        gpuShapeViewCommands.addVertex(x1, y0, 0f, BIG_MAX_LEN)
        gpuShapeViewCommands.addVertex(x1, y1, 0f, BIG_MAX_LEN)
        gpuShapeViewCommands.addVertex(x0, y1, 0f, BIG_MAX_LEN)
        val vend = gpuShapeViewCommands.verticesEnd()

        //println("[($lx0,$ly0)-($lx1,$ly1)]")

        gpuShapeViewCommands.draw(
            AGDrawType.TRIANGLE_FAN,
            paintShader = paintShader,
            colorMask = AGColorMask(true),
            stencilOpFunc = AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(stencilCompare),
            stencilRef = AGStencilReference.DEFAULT.withReferenceValue(stencilReferenceValue).withWriteMask(0),
            startIndex = vstart,
            endIndex = vend,
            blendMode = renderBlendMode,
            //startIndex = pathDataStart,
            //endIndex = pathDataEnd,
        )
    }

    init {
        invalidateShape()
    }
}
