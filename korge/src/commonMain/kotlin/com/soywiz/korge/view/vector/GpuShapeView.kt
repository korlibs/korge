package com.soywiz.korge.view.vector

import com.soywiz.kds.FastIdentityMap
import com.soywiz.kds.clear
import com.soywiz.kds.getOrPut
import com.soywiz.klock.measureTime
import com.soywiz.klogger.Console
import com.soywiz.kmem.clamp
import com.soywiz.korag.AG
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Anchorable
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.vector.CompoundShape
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.FillShape
import com.soywiz.korim.vector.LineScaleMode
import com.soywiz.korim.vector.PolylineShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korim.vector.ShapeBuilder
import com.soywiz.korim.vector.TextShape
import com.soywiz.korim.vector.buildShape
import com.soywiz.korim.vector.getBounds
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.PointPool
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.distanceTo
import com.soywiz.korma.geom.expand
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.shape.emitPoints2
import com.soywiz.korma.geom.shape.toPathPointList
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.Winding
import kotlin.math.max
import kotlin.math.sign

@KorgeExperimental
inline fun Container.gpuShapeView(
    build: ShapeBuilder.() -> Unit,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
) =
    GpuShapeView(buildShape { build() }, antialiased).addTo(this, callback)

@KorgeExperimental
inline fun Container.gpuShapeView(
    shape: Shape,
    antialiased: Boolean = true,
    callback: @ViewDslMarker GpuShapeView.() -> Unit = {}
) =
    GpuShapeView(shape, antialiased).addTo(this, callback)

// @TODO: Optimize convex shapes (for example a circle, a rect, a rounded rect, shouldn't require stencils)
@KorgeExperimental
@OptIn(KorgeInternal::class)
open class GpuShapeView(
    shape: Shape = EmptyShape,
    antialiased: Boolean = true,
    // @TODO: Not used, but to be compatible with Graphics
    var autoScaling: Boolean = true
) : View(), Anchorable {
    private val pointCache = FastIdentityMap<VectorPath, PointArrayList>()
    private val gpuShapeViewCommands = GpuShapeViewCommands()
    private val bb = BoundsBuilder()
    var bufferWidth = 1000
    var bufferHeight = 1000
    private val pointsScope = PointPool(128)
    private val ab = SegmentInfo()
    private val bc = SegmentInfo()
    private var notifyAboutEvenOdd = false

    override var anchorX: Double = 0.0 ; set(value) { field = value; invalidate() }
    override var anchorY: Double = 0.0 ; set(value) { field = value; invalidate() }

    var applyScissor: Boolean = true

    var antialiasedMasks: Boolean = false

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

    private fun invalidateShape() {
        pointCache.clear()

        gpuShapeViewCommands.clear()
        gpuShapeViewCommands.clearStencil()
        renderShape(shape)
        gpuShapeViewCommands.finish()
        cachedScale = globalScale
        //strokeCache.clear()
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        shape.getBounds(out, bb)
        out.setXY(
            out.x - out.width * anchorX,
            out.y - out.height * anchorY
        )
    }


    inline fun updateShape(block: ShapeBuilder.() -> Unit) {
        this.shape = buildShape { block() }
    }

    val Shape.requireStencil: Boolean
        get() {
            return when (this) {
                EmptyShape -> false
                is CompoundShape -> this.components.any { it.requireStencil }
                is TextShape -> this.primitiveShapes.requireStencil
                is FillShape -> {
                    // @TODO: Check if the shape is convex. If it is context we might not need the stencil
                    true
                }
                is PolylineShape -> {
                    false
                }
                else -> true // UNKNOWN
            }
        }

    private val globalTransform = Matrix.Transform()
    private var globalScale: Double = 1.0
    private var cachedScale: Double = Double.NaN

    override fun renderInternal(ctx: RenderContext) {
        //globalScale = globalTransform.setMatrix(globalMatrix).setMatrix(globalMatrix).scaleAvg * ctx.bp.globalToWindowScaleAvg
        //globalScale = ctx.bp.globalToWindowScaleAvg
        //if (cachedScale != globalScale) {
        //    invalidateShape()
        //}
        ctx.flush()

        val doRequireTexture = shape.requireStencil
        //val doRequireTexture = false

        val time = measureTime {
            if (doRequireTexture) {
                //val currentRenderBuffer = ctx.ag.currentRenderBufferOrMain
                val currentRenderBuffer = ctx.ag.mainRenderBuffer
                //ctx.renderToTexture(currentRenderBuffer.width, currentRenderBuffer.height, {
                bufferWidth = currentRenderBuffer.width
                bufferHeight = currentRenderBuffer.height
                //println("bufferWidth=$bufferWidth, bufferHeight=$bufferHeight")
                ctx.renderToTexture(bufferWidth, bufferHeight, {
                    renderCommands(ctx, doRequireTexture)
                }, hasDepth = false, hasStencil = true, msamples = 1) { texture ->
                    ctx.useBatcher {
                        it.drawQuad(texture, m = ctx.bp.windowToGlobalMatrix)
                    }
                }
            } else {
                renderCommands(ctx, doRequireTexture)
            }
        }

        //println("GPU RENDER IN: $time, doRequireTexture=$doRequireTexture")
    }

    private fun renderCommands(ctx: RenderContext, doRequireTexture: Boolean) {
        gpuShapeViewCommands.render(ctx, if (doRequireTexture) globalMatrix * ctx.bp.globalToWindowMatrix else globalMatrix, localMatrix, renderAlpha)
    }

    private fun renderShape(shape: Shape) {
        when (shape) {
            EmptyShape -> Unit
            is CompoundShape -> for (v in shape.components) renderShape(v)
            is TextShape -> renderShape(shape.primitiveShapes)
            is FillShape -> renderFill(shape)
            is PolylineShape -> renderStroke(
                shape.transform,
                shape.path,
                shape.paint,
                shape.globalAlpha,
                shape.thickness, //* (1.0 / globalScale), // This depends if we want to keep line width when scaling
                shape.scaleMode,
                shape.startCaps,
                shape.endCaps,
                shape.lineJoin,
                shape.miterLimit,
            )
            //is PolylineShape -> renderShape(ctx, shape.fillShape)
            else -> TODO("shape=$shape")
        }
    }

    class SegmentInfo {
        lateinit var s: IPoint // start
        lateinit var e: IPoint // end
        lateinit var line: Line
        var angleSE: Angle = 0.degrees
        var angleSE0: Angle = 0.degrees
        var angleSE1: Angle = 0.degrees
        lateinit var s0: IPoint
        lateinit var s1: IPoint
        lateinit var e0: IPoint
        lateinit var e1: IPoint
        lateinit var e0s: IPoint
        lateinit var e1s: IPoint
        lateinit var s0s: IPoint
        lateinit var s1s: IPoint

        fun p(index: Int) = if (index == 0) s else e
        fun p0(index: Int) = if (index == 0) s0 else e0
        fun p1(index: Int) = if (index == 0) s1 else e1

        fun setTo(s: Point, e: Point, lineWidth: Double, scope: PointPool) {
            this.s = s
            this.e = e
            scope.apply {
                line = Line(s, e)
                angleSE = Angle.between(s, e)
                angleSE0 = angleSE - 90.degrees
                angleSE1 = angleSE + 90.degrees
                s0 = Point(s, angleSE0, length = lineWidth)
                s1 = Point(s, angleSE1, length = lineWidth)
                e0 = Point(e, angleSE0, length = lineWidth)
                e1 = Point(e, angleSE1, length = lineWidth)

                s0s = Point(s0, angleSE + 180.degrees, length = lineWidth)
                s1s = Point(s1, angleSE + 180.degrees, length = lineWidth)

                e0s = Point(e0, angleSE, length = lineWidth)
                e1s = Point(e1, angleSE, length = lineWidth)
            }
        }
    }

    private fun pointsAdd(p1: IPoint, p2: IPoint, lineWidth: Float) {
        //val lineWidth = 0f
        val p1x = p1.x.toFloat()
        val p1y = p1.y.toFloat()
        val p2x = p2.x.toFloat()
        val p2y = p2.y.toFloat()
        gpuShapeViewCommands.addVertex(p1x, p1y, p1x, p1y, -lineWidth)
        gpuShapeViewCommands.addVertex(p2x, p2y, p2x, p2y, +lineWidth)
    }

    private fun pointsAddCubicOrLine(
        scope: PointPool, fix: IPoint,
        p0: IPoint, p0s: IPoint, p1s: IPoint, p1: IPoint,
        lineWidth: Double,
        reverse: Boolean = false,
        start: Boolean = true,
    ) {
        val NPOINTS = 15
        scope.apply {
            for (i in 0..NPOINTS) {
                val ratio = i.toDouble() / NPOINTS.toDouble()
                val pos = when {
                    start -> Bezier.cubicCalc(p0, p0s, p1s, p1, ratio, MPoint())
                    else -> Bezier.cubicCalc(p1, p1s, p0s, p0, ratio, MPoint())
                }
                when {
                    reverse -> pointsAdd(fix, pos, lineWidth.toFloat())
                    else -> pointsAdd(pos, fix, lineWidth.toFloat())
                }
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
        globalAlpha: Double,
        lineWidth: Double,
        scaleMode: LineScaleMode,
        startCap: LineCap,
        endCap: LineCap,
        join: LineJoin,
        miterLimit: Double,
        forceClosed: Boolean? = null,
        stencil: AG.StencilState? = null
    ) {
        //val m0 = root.globalMatrix
        //val mt0 = m0.toTransform()
        //val m = globalMatrix
        //val mt = m.toTransform()

        val scaleWidth = scaleMode.anyScale
        //val lineScale = mt.scaleAvg.absoluteValue

        val lineScale = 1.0
        //println("lineScale=$lineScale")
        val flineWidth = if (scaleWidth) lineWidth * lineScale else lineWidth
        val lineWidth = if (antialiased) (flineWidth * 0.5) + 0.25 else flineWidth * 0.5

        //val lineWidth = 0.2
        //val lineWidth = 20.0
        val fLineWidth = max((lineWidth).toFloat(), 1.5f)

        // @TODO: Curve points aren't joints and shouldn't require extra computations! Let's handle paths manually

        /*
        var startX = 0.0
        var startY = 0.0
        var lastX = 0.0
        var lastY = 0.0

        strokePath.visitCmds(
            moveTo = { x, y ->
                startX = x
                startY = y
                lastX = x
                lastY = y
            },
            lineTo = { x, y ->
                lastX = x
                lastY = y
            },
            quadTo = { x1, y1, x2, y2 ->
                lastX = x2
                lastY = y2
            },
            cubicTo = { x1, y1, x2, y2, x3, y3 ->
                lastX = x3
                lastY = y3
            },
            close = {

            }
        )
        */

        //val cacheKey = StrokeRenderCacheKey(lineWidth, strokePath, m)

        //val data = strokeCache.getOrPut(cacheKey) {
        val data = run {
            //val pathList = strokePath.toPathPointList(m, emitClosePoint = false)
            val pathList = strokePath.toPathPointList(null, emitClosePoint = false)
            //println(pathList.size)
            for (ppath in pathList) {
                gpuShapeViewCommands.verticesStart()
                val loop = forceClosed ?: ppath.closed
                //println("Points: " + ppath.toList())
                val end = if (loop) ppath.size + 1 else ppath.size
                //val end = if (loop) ppath.size else ppath.size

                for (n in 0 until end) pointsScope {
                    val isFirst = n == 0
                    val isLast = n == ppath.size - 1
                    val isFirstOrLast = isFirst || isLast
                    val a = ppath.getCyclic(n - 1)
                    val b = ppath.getCyclic(n) // Current point
                    val c = ppath.getCyclic(n + 1)
                    val orientation = Point.orientation(a, b, c).sign.toInt()
                    //val angle = Angle.between(b - a, c - a)
                    //println("angle = $angle")

                    ab.setTo(a, b, lineWidth, this)
                    bc.setTo(b, c, lineWidth, this)

                    when {
                        // Start/End caps
                        !loop && isFirstOrLast -> {
                            val start = n == 0

                            val cap = if (start) startCap else endCap
                            val index = if (start) 0 else 1
                            val segment = if (start) bc else ab
                            val p1 = segment.p1(index)
                            val p0 = segment.p0(index)
                            val iangle = if (start) segment.angleSE - 180.degrees else segment.angleSE

                            when (cap) {
                                LineCap.BUTT -> pointsAdd(p1, p0, fLineWidth)
                                LineCap.SQUARE -> {
                                    val p1s = Point(p1, iangle, lineWidth)
                                    val p0s = Point(p0, iangle, lineWidth)
                                    pointsAdd(p1s, p0s, fLineWidth)
                                }
                                LineCap.ROUND -> {
                                    val p1s = Point(p1, iangle, lineWidth * 1.5)
                                    val p0s = Point(p0, iangle, lineWidth * 1.5)
                                    pointsAddCubicOrLine(
                                        this, p0, p0, p0s, p1s, p1, lineWidth,
                                        reverse = false,
                                        start = start
                                    )
                                }
                            }
                        }
                        // Joins
                        else -> {
                            val m0 = Line.getIntersectXY(ab.s0, ab.e0, bc.s0, bc.e0, MPoint()) // Outer (CW)
                            val m1 = Line.getIntersectXY(ab.s1, ab.e1, bc.s1, bc.e1, MPoint()) // Inner (CW)
                            val e1 = m1 ?: ab.e1
                            val e0 = m0 ?: ab.e0
                            val round = join == LineJoin.ROUND
                            val dorientation = when {
                                (join == LineJoin.MITER && e1.distanceTo(b) <= (miterLimit * lineWidth)) -> 0
                                else -> orientation
                            }

                            if (loop && isFirst) {
                                //println("forientation=$forientation")
                                when (dorientation) {
                                    -1 -> pointsAdd(e1, bc.s0, fLineWidth)
                                    0 -> pointsAdd(e1, e0, fLineWidth)
                                    +1 -> pointsAdd(bc.s1, e0, fLineWidth)
                                }
                            } else {
                                //println("dorientation=$dorientation")
                                when (dorientation) {
                                    // Turn right
                                    -1 -> {
                                        val fp = m1 ?: ab.e1
                                        //points.addCubicOrLine(this, true, fp, p0, p0, p1, p1, lineWidth, cubic = false)
                                        if (round) {
                                            pointsAddCubicOrLine(
                                                this, fp,
                                                ab.e0, ab.e0s, bc.s0s, bc.s0,
                                                lineWidth, reverse = true
                                            )
                                        } else {
                                            pointsAdd(fp, ab.e0, fLineWidth)
                                            pointsAdd(fp, bc.s0, fLineWidth)
                                        }
                                    }
                                    // Miter
                                    0 -> pointsAdd(e1, e0, fLineWidth)
                                    // Turn left
                                    1 -> {
                                        val fp = m0 ?: ab.e0
                                        if (round) {
                                            pointsAddCubicOrLine(
                                                this, fp,
                                                ab.e1, ab.e1s, bc.s1s, bc.s1,
                                                lineWidth, reverse = false
                                            )
                                        } else {
                                            pointsAdd(ab.e1, fp, fLineWidth)
                                            pointsAdd(bc.s1, fp, fLineWidth)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val info = GpuShapeViewPrograms.paintToShaderInfo(
                    stateTransform = stateTransform,
                    paint = paint,
                    globalAlpha = globalAlpha,
                    lineWidth = lineWidth,
                )

                gpuShapeViewCommands.draw(AG.DrawType.TRIANGLE_STRIP, info, stencil = stencil)
            }
        }

        //println("vertexCount=$vertexCount")
    }
    //private var lastPointsString = ""

    class PointsResult(val bounds: Rectangle, val vertexCount: Int)

    private fun getPointsForPath(path: VectorPath): PointsResult {
        val points: PointArrayList = pointCache.getOrPut(path) {
            PointArrayList().also { points -> path.emitPoints2 { x, y, move -> points.add(x, y) } }
        }
        val bb = BoundsBuilder()
        bb.reset()
        val startIndex = gpuShapeViewCommands.addVertex(0f, 0f)
        for (n in 0 until points.size + 1) {
            val x = points.getX(n % points.size).toFloat()
            val y = points.getY(n % points.size).toFloat()
            gpuShapeViewCommands.addVertex(x, y)
            bb.add(x, y)
        }
        gpuShapeViewCommands.updateVertex(startIndex, ((bb.xmax + bb.xmin) / 2).toFloat(), ((bb.ymax + bb.ymin) / 2).toFloat())
        return PointsResult(bb.getBounds(), points.size + 2)
    }

    private fun renderFill(shape: FillShape) {
        if (shape.path.winding != Winding.EVEN_ODD) {
            if (!notifyAboutEvenOdd) {
                notifyAboutEvenOdd = true
                Console.error("ERROR: Currently only supported EVEN_ODD winding, but used ${shape.path.winding}")
            }
        }

        val paintShader = GpuShapeViewPrograms.paintToShaderInfo(
            shape.transform, shape.paint, shape.globalAlpha,
            lineWidth = 10000000.0,
        ) ?: return

        val pathDataStart = gpuShapeViewCommands.verticesStart()
        val pathData = getPointsForPath(shape.path)
        val pathDataEnd = gpuShapeViewCommands.verticesEnd()
        val pathBounds = pathData.bounds.clone().expand(2, 2, 2, 2)

        if (!shape.requireStencil && shape.clip == null) {
            gpuShapeViewCommands.draw(
                AG.DrawType.TRIANGLE_FAN,
                startIndex = pathDataStart,
                endIndex = pathDataEnd,
                paintShader = paintShader,
                colorMask = AG.ColorMaskState(true),
                blendMode = BlendMode.NONE.factors,
            )
            return
        }

        val clipDataStart = gpuShapeViewCommands.verticesStart()
        val clipData = shape.clip?.let { getPointsForPath(it) }
        val clipDataEnd = gpuShapeViewCommands.verticesEnd()
        val clipBounds = clipData?.bounds

        gpuShapeViewCommands.setScissor(when {
            clipBounds != null -> Rectangle().also { it.setToIntersection(pathBounds, clipBounds) }
            else -> pathBounds
        })
        //gpuShapeViewCommands.clearStencil(0)

        var stencilEqualsValue = 0b00000001
        writeStencil(pathDataStart, pathDataEnd, AG.StencilState(
            enabled = true,
            compareMode = AG.CompareMode.ALWAYS,
            writeMask = 0b00000001,
            actionOnBothPass = AG.StencilOp.INVERT,
        ))

        // @TODO: Should we do clipping other way?
        if (clipData != null) {
            writeStencil(clipDataStart, clipDataEnd, AG.StencilState(
                enabled = true,
                compareMode = AG.CompareMode.ALWAYS,
                writeMask = 0b00000010,
                actionOnBothPass = AG.StencilOp.INVERT,
            ))
            stencilEqualsValue = 0b00000011
        }

        // Antialias when we don't have clipping
        // @TODO: How should we handle clipping antialiasing? Should we render the mask into a buffer first, and then do the masking?
        if (antialiased && shape.clip == null) {
        //if (true) {
        //if (false) {
            //println("globalScale=$globalScale")
            renderStroke(
                stateTransform = shape.transform,
                strokePath = shape.path,
                paint = shape.paint,
                globalAlpha = shape.globalAlpha,
                lineWidth = (1.6 * globalScale).clamp(1.4, 1.8),
                scaleMode = LineScaleMode.NONE,
                startCap = LineCap.BUTT,
                endCap = LineCap.BUTT,
                join = LineJoin.MITER,
                miterLimit = 0.5,
                forceClosed = true,
                stencil = AG.StencilState(
                    enabled = true,
                    compareMode = AG.CompareMode.NOT_EQUAL,
                    referenceValue = stencilEqualsValue,
                    writeMask = 0,
                )
            )
        }

        writeFill(paintShader, stencilEqualsValue, pathBounds, pathDataStart, pathDataEnd)

        gpuShapeViewCommands.clearStencil(0)

        // renderFill
    }

    private fun writeStencil(pathDataStart: Int, pathDataEnd: Int, stencil: AG.StencilState) {
        gpuShapeViewCommands.draw(
            AG.DrawType.TRIANGLE_FAN,
            startIndex = pathDataStart,
            endIndex = pathDataEnd,
            paintShader = GpuShapeViewPrograms.stencilPaintShader,
            colorMask = AG.ColorMaskState(false, false, false, false),
            blendMode = BlendMode.NONE.factors,
            stencil = stencil
        )
    }

    private fun writeFill(
        paintShader: GpuShapeViewPrograms.PaintShader,
        stencilEqualsValue: Int,
        pathBounds: Rectangle,
        pathDataStart: Int,
        pathDataEnd: Int
    ) {
        //val vstart = gpuShapeViewCommands.verticesStart()
        //val x0 = pathBounds.left.toFloat()
        //val y0 = pathBounds.top.toFloat()
        //val x1 = pathBounds.right.toFloat()
        //val y1 = pathBounds.bottom.toFloat()
        //gpuShapeViewCommands.addVertex(x0, y0)
        //gpuShapeViewCommands.addVertex(x1, y0)
        //gpuShapeViewCommands.addVertex(x1, y1)
        //gpuShapeViewCommands.addVertex(x0, y1)
        //val vend = gpuShapeViewCommands.verticesEnd()

        //println("[($lx0,$ly0)-($lx1,$ly1)]")

        gpuShapeViewCommands.draw(
            AG.DrawType.TRIANGLE_FAN,
            paintShader = paintShader,
            colorMask = AG.ColorMaskState(true),
            stencil = AG.StencilState(
                enabled = true,
                compareMode = AG.CompareMode.EQUAL,
                referenceValue = stencilEqualsValue,
                writeMask = 0,
            ),
            //startIndex = vstart,
            //endIndex = vend,
            startIndex = pathDataStart,
            endIndex = pathDataEnd,
        )
    }

    init {
        invalidateShape()
    }
}

