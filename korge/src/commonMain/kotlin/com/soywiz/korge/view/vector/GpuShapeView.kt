package com.soywiz.korge.view.vector

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*


@KorgeExperimental
inline fun Container.gpuShapeView(build: ShapeBuilder.() -> Unit, antialiased: Boolean = true, callback: @ViewDslMarker GpuShapeView.() -> Unit = {}) =
    GpuShapeView(buildShape { build() }, antialiased).addTo(this, callback)

@KorgeExperimental
inline fun Container.gpuShapeView(shape: Shape, antialiased: Boolean = true, callback: @ViewDslMarker GpuShapeView.() -> Unit = {}) =
    GpuShapeView(shape, antialiased).addTo(this, callback)

@KorgeExperimental
class GpuShapeView(shape: Shape, antialiased: Boolean = true) : View() {
    private val pointCache = FastIdentityMap<VectorPath, PointArrayList>()

    var applyScissor: Boolean = true

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
    }

    private val bb = BoundsBuilder()
    override fun getLocalBoundsInternal(out: Rectangle) {
        shape.getBounds(out, bb)
    }

    var bufferWidth = 1000
    var bufferHeight = 1000

    inline fun updateShape(block: ShapeBuilder.() -> Unit) {
        this.shape = buildShape { block() }
    }

    val Shape.requireStencil: Boolean get() {
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

    override fun renderInternal(ctx: RenderContext) {
        ctx.flush()

        val doRequireTexture = shape.requireStencil

        val time = measureTime {
            if (doRequireTexture) {
                val currentRenderBuffer = ctx.ag.currentRenderBufferOrMain
                //ctx.renderToTexture(currentRenderBuffer.width, currentRenderBuffer.height, {
                bufferWidth = currentRenderBuffer.width
                bufferHeight = currentRenderBuffer.height
                ctx.renderToTexture(bufferWidth, bufferHeight, {
                    renderShape(ctx, shape)
                    ctx.ag.clearStencil()
                }, hasDepth = false, hasStencil = true, msamples = 1) { texture ->
                    ctx.ag.clearStencil()
                    ctx.useBatcher {
                        it.drawQuad(texture, x = 0f, y = 0f)
                    }
                }
            } else {
                renderShape(ctx, shape)
            }
        }
        //println("GPU RENDER IN: $time, doRequireTexture=$doRequireTexture")
    }

    private fun renderShape(ctx: RenderContext, shape: Shape) {
        when (shape) {
            EmptyShape -> Unit
            is CompoundShape -> for (v in shape.components) renderShape(ctx, v)
            is TextShape -> renderShape(ctx, shape.primitiveShapes)
            is FillShape -> renderShape(ctx, shape)
            is PolylineShape -> renderStroke(ctx, shape.transform, shape.path, shape.paint, shape.globalAlpha, shape.thickness, shape.scaleMode, shape.startCaps, shape.endCaps, shape.lineJoin, shape.miterLimit, scissor = if (applyScissor) ctx.batch.scissor else null)
            //is PolylineShape -> renderShape(ctx, shape.fillShape)
            else -> TODO("shape=$shape")
        }
    }

    private val pointsScope = PointPool(128)

    class RenderStrokePoints(
        val points: FloatArrayList = FloatArrayList(),
        val distValues: FloatArrayList = FloatArrayList(),
    ) {
        var antialiased = true
        val pointCount: Int get() = points.size / 2

        override fun toString(): String = "RenderStrokePoints(points=$points, dists=$distValues)"

        fun clear() {
            //println("------------")
            points.clear()
            distValues.clear()
        }

        fun add(p: IPoint, lineWidth: Float) {
            points.add(p.x.toFloat())
            points.add(p.y.toFloat())
            distValues.add(if (antialiased) lineWidth else 0f)
        }

        fun add(p1: IPoint, p2: IPoint, lineWidth: Float) {
            add(p1, -lineWidth)
            add(p2, +lineWidth)
            //println("ADD: $p1, $p2")
        }

        fun addCubicOrLine(
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
                        reverse -> add(fix, pos, lineWidth.toFloat())
                        else -> add(pos, fix, lineWidth.toFloat())
                    }
                }
            }
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

        fun setTo(s: Point, e: Point, lineWidth: Double, scope: PointPool): Unit {
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

    private val points = RenderStrokePoints()
    private val ab = SegmentInfo()
    private val bc = SegmentInfo()

    private fun renderStroke(
        ctx: RenderContext,
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
        scissor: AG.Scissor? = null,
        stencil: AG.StencilState = AG.StencilState(),
    ) {
        points.antialiased = this.antialiased

        //val m0 = root.globalMatrix
        //val mt0 = m0.toTransform()
        val m = globalMatrix
        val mt = m.toTransform()
        val st2 = stateTransform.clone()
        st2.premultiply(stage!!.globalMatrix)

        val scaleWidth = scaleMode.anyScale
        //val lineScale = mt0.scaleAvg.absoluteValue / mt.scaleAvg.absoluteValue
        val lineScale = mt.scaleAvg.absoluteValue
        //println("lineScale=$lineScale")
        val flineWidth = if (scaleWidth) lineWidth * lineScale else lineWidth
        val lineWidth = if (antialiased) (flineWidth * 0.5) + 0.25 else flineWidth * 0.5

        //val lineWidth = 0.2
        //val lineWidth = 20.0
        val fLineWidth = max((lineWidth).toFloat(), 1.5f)

        val pathList = strokePath.toPathPointList(m, emitClosePoint = false)
        //println(pathList.size)
        for (ppath in pathList) {
            points.clear()
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
                            LineCap.BUTT -> points.add(p1, p0, fLineWidth)
                            LineCap.SQUARE -> {
                                val p1s = Point(p1, iangle, lineWidth)
                                val p0s = Point(p0, iangle, lineWidth)
                                points.add(p1s, p0s, fLineWidth)
                            }
                            LineCap.ROUND -> {
                                val p1s = Point(p1, iangle, lineWidth * 1.5)
                                val p0s = Point(p0, iangle, lineWidth * 1.5)
                                points.addCubicOrLine(this, p0, p0, p0s, p1s, p1, lineWidth, reverse = false, start = start)
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
                                -1 -> points.add(e1, bc.s0, fLineWidth)
                                0 -> points.add(e1, e0, fLineWidth)
                                +1 -> points.add(bc.s1, e0, fLineWidth)
                            }
                        } else {
                            //println("dorientation=$dorientation")
                            when (dorientation) {
                                // Turn right
                                -1 -> {
                                    val fp = m1 ?: ab.e1
                                    //points.addCubicOrLine(this, true, fp, p0, p0, p1, p1, lineWidth, cubic = false)
                                    if (round) {
                                        points.addCubicOrLine(
                                            this, fp,
                                            ab.e0, ab.e0s, bc.s0s, bc.s0,
                                            lineWidth, reverse = true
                                        )
                                    } else {
                                        points.add(fp, ab.e0, fLineWidth)
                                        points.add(fp, bc.s0, fLineWidth)
                                    }
                                }
                                // Miter
                                0 -> points.add(e1, e0, fLineWidth)
                                // Turn left
                                1 -> {
                                    val fp = m0 ?: ab.e0
                                    if (round) {
                                        points.addCubicOrLine(
                                            this, fp,
                                            ab.e1, ab.e1s, bc.s1s, bc.s1,
                                            lineWidth, reverse = false
                                        )
                                    } else {
                                        points.add(ab.e1, fp, fLineWidth)
                                        points.add(bc.s1, fp, fLineWidth)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //run {
            //    val pointsString = "$points"
            //    if (lastPointsString != pointsString) {
            //        lastPointsString = pointsString
            //        println("pointsString=$pointsString")
            //    }
            //}
            drawTriangleStrip(ctx, globalAlpha, paint, points.points, points.distValues, points.pointCount, lineWidth.toFloat(), st2, scissor, stencil)
        }

        //println("vertexCount=$vertexCount")
    }
    //private var lastPointsString = ""

    private fun drawTriangleStrip(
        ctx: RenderContext,
        globalAlpha: Double,
        paint: Paint,
        points: FloatArrayList,
        distValues: FloatArrayList,
        vertexCount: Int,
        lineWidth: Float,
        stateTransform: Matrix,
        scissor: AG.Scissor? = null,
        stencil: AG.StencilState = AG.StencilState(),
    ) {
        val info = paintToShaderInfo(
            ctx,
            stateTransform = stateTransform,
            paint = paint,
            globalAlpha = globalAlpha,
            lineWidth = lineWidth.toDouble(),
            out = tempPaintShader
        ) ?: return

        ctx.dynamicVertexBufferPool.allocMultiple(3) { (vertices, textures, dists) ->
            vertices.upload(points.data)
            textures.upload(points.data)
            dists.upload(distValues.data)
            ctx.useBatcher { batcher ->
                batcher.updateStandardUniforms()
                batcher.simulateBatchStats(vertexCount)

                batcher.setTemporalUniforms(info.uniforms) {
                    //ctx.ag.clearStencil(0, scissor = null)
                    ctx.ag.drawV2(
                        vertexData = fastArrayListOf(
                            AG.VertexData(vertices, LAYOUT),
                            AG.VertexData(textures, LAYOUT_TEX),
                            AG.VertexData(dists, LAYOUT_DIST),
                        ),
                        program = info.program,
                        type = AG.DrawType.TRIANGLE_STRIP,
                        vertexCount = vertexCount,
                        uniforms = batcher.uniforms,
                        scissor = scissor,
                        stencil = stencil,
                    )
                }
            }
        }
    }

    private var notifyAboutEvenOdd = false

    class PointsResult(val bounds: Rectangle, val data: FloatArray, val vertexCount: Int)

    private fun getPointsForPath(path: VectorPath, m: Matrix): PointsResult {
        val points: PointArrayList = pointCache.getOrPut(path) {
            val points = PointArrayList()
            path.emitPoints2 { x, y, move ->
                points.add(x, y)
            }
            points
        }

        val bb = BoundsBuilder()
        bb.reset()

        val data = FloatArray(points.size * 2 + 4)
        for (n in 0 until points.size + 1) {
            val x = points.getX(n % points.size).toFloat()
            val y = points.getY(n % points.size).toFloat()
            val tx = m.transformXf(x, y)
            val ty = m.transformYf(x, y)
            data[(n + 1) * 2 + 0] = tx
            data[(n + 1) * 2 + 1] = ty
            bb.add(tx, ty)
        }
        data[0] = ((bb.xmax + bb.xmin) / 2).toFloat()
        data[1] = ((bb.ymax + bb.ymin) / 2).toFloat()
        val bounds = bb.getBounds()
        return PointsResult(bounds, data, points.size + 2)
    }

    private fun renderShape(ctx: RenderContext, shape: FillShape) {
        //val m = localMatrix
        //val stage = stage!!
        //val stageMatrix = stage.localMatrix
        //println("stage=$stage, globalMatrix=$stageMatrix")

        if (shape.path.winding != Winding.EVEN_ODD) {
            if (!notifyAboutEvenOdd) {
                notifyAboutEvenOdd = true
                Console.error("ERROR: Currently only supported EVEN_ODD winding, but used ${shape.path.winding}")
            }
        }

        val pathData = getPointsForPath(shape.path, globalMatrix)
        val pathBounds = pathData.bounds

        val clipData = shape.clip?.let { getPointsForPath(it, globalMatrix) }
        val clipBounds = clipData?.bounds

        val scissorBounds: Rectangle = when {
            clipBounds != null -> Rectangle().also { it.setToIntersection(pathBounds, clipBounds) }
            else -> pathBounds
        }

        // @TODO: Scissor should be the intersection between the path bounds and the clipping bounds

        val rscissor: AG.Scissor = AG.Scissor().setTo(scissorBounds)
        val scissor = if (applyScissor) AG.Scissor.combine(ctx.batch.scissor, rscissor) else rscissor
        //val scissor: AG.Scissor? = null

        ctx.ag.clearStencil(0, scissor = scissor)

        var stencilEqualsValue = 0b00000001
        ctx.dynamicVertexBufferPool { vertices ->
            writeStencil(
                ctx, pathData, scissor, AG.StencilState(
                    enabled = true,
                    compareMode = AG.CompareMode.ALWAYS,
                    writeMask = 0b00000001,
                    actionOnBothPass = AG.StencilOp.INVERT,
                )
            )
        }
        if (clipData != null) {
            writeStencil(
                ctx, clipData, scissor, AG.StencilState(
                    enabled = true,
                    compareMode = AG.CompareMode.ALWAYS,
                    writeMask = 0b00000010,
                    actionOnBothPass = AG.StencilOp.INVERT,
                )
            )
            stencilEqualsValue = 0b00000011
        }

        // Antialias
        if (antialiased) {
            renderStroke(
                ctx = ctx,
                //transform = shape.transform,
                stateTransform = Matrix(),
                strokePath = shape.path,
                paint = shape.paint,
                globalAlpha = shape.globalAlpha,
                lineWidth = 2.0,
                scaleMode = LineScaleMode.NONE,
                startCap = LineCap.BUTT,
                endCap = LineCap.BUTT,
                join = LineJoin.MITER,
                miterLimit = 0.5,
                forceClosed = true,
                scissor = scissor,
                stencil = AG.StencilState(
                    enabled = true,
                    compareMode = AG.CompareMode.NOT_EQUAL,
                    referenceValue = stencilEqualsValue,
                    writeMask = 0,
                )
            )
        }

        renderFill(
            ctx,
            shape.paint,
            shape.transform,
            scissor,
            shape.globalAlpha,
            stencilEqualsValue = stencilEqualsValue
        )
    }

    private fun writeStencil(ctx: RenderContext, points: PointsResult, scissor: AG.Scissor?, stencil: AG.StencilState) {
        ctx.dynamicVertexBufferPool { vertices ->
            vertices.upload(points.data)
            ctx.batch.updateStandardUniforms()
            ctx.batch.simulateBatchStats(points.vertexCount)
            //ctx.ag.clearStencil(0, scissor = null)
            ctx.ag.draw(
                vertices = vertices,
                program = PROGRAM_STENCIL,
                type = AG.DrawType.TRIANGLE_FAN,
                vertexLayout = LAYOUT,
                vertexCount = points.vertexCount,
                uniforms = ctx.batch.uniforms,
                stencil = stencil,
                blending = BlendMode.NONE.factors,
                colorMask = AG.ColorMaskState(false, false, false, false),
                scissor = scissor,
            )
        }
    }

    private val colorUniforms = AG.UniformValues()
    private val bitmapUniforms = AG.UniformValues()
    private val gradientUniforms = AG.UniformValues()
    private val gradientBitmap = Bitmap32(256, 1)

    private val colorF = FloatArray(4)
    private val tempPaintShader = PaintShader()

    private fun renderFill(
        ctx: RenderContext,
        paint: Paint,
        stateTransform: Matrix,
        scissor: AG.Scissor?,
        globalAlpha: Double,
        stencilEqualsValue: Int,
    ) {
        val info = paintToShaderInfo(
            ctx,
            stateTransform,
            paint,
            globalAlpha,
            lineWidth = 10000000.0,
            out = tempPaintShader
        ) ?: return

        ctx.dynamicVertexBufferPool { vertices ->
            val data = FloatArray(4 * 4)
            var n = 0

            val x0 = 0f
            val y0 = 0f
            val x1 = bufferWidth.toFloat()
            val y1 = bufferHeight.toFloat()

            val vm = Matrix()
            vm.copyFrom(stage!!.globalMatrixInv)

            val l0 = vm.transform(0f, 0f)
            val l1 = vm.transform(bufferWidth.toFloat(), bufferHeight.toFloat())
            val lx0 = l0.xf
            val ly0 = l0.yf
            val lx1 = l1.xf
            val ly1 = l1.yf

            data[n++] = x0; data[n++] = y0; data[n++] = lx0; data[n++] = ly0
            data[n++] = x1; data[n++] = y0; data[n++] = lx1; data[n++] = ly0
            data[n++] = x1; data[n++] = y1; data[n++] = lx1; data[n++] = ly1
            data[n++] = x0; data[n++] = y1; data[n++] = lx0; data[n++] = ly1

            //println("[($lx0,$ly0)-($lx1,$ly1)]")

            vertices.upload(data)
            ctx.useBatcher { batch ->
                batch.updateStandardUniforms()

                val program = info.program
                val uniforms = info.uniforms

                uniforms[u_GlobalAlpha] = globalAlpha.toFloat()
                batch.setTemporalUniforms(uniforms) {
                    ctx.batch.simulateBatchStats(4)
                    //println("ctx.batch.uniforms=${ctx.batch.uniforms}")
                    ctx.ag.draw(
                        vertices = vertices,
                        program = program,
                        type = AG.DrawType.TRIANGLE_FAN,
                        vertexLayout = LAYOUT_FILL,
                        vertexCount = 4,
                        uniforms = ctx.batch.uniforms,
                        stencil = AG.StencilState(
                            enabled = true,
                            compareMode = AG.CompareMode.EQUAL,
                            referenceValue = stencilEqualsValue,
                            writeMask = 0,
                        ),
                        blending = BlendMode.NORMAL.factors,
                        colorMask = AG.ColorMaskState(true, true, true, true),
                        scissor = scissor,
                    )
                }
            }
        }
    }


    fun paintToShaderInfo(
        ctx: RenderContext,
        stateTransform: Matrix,
        paint: Paint,
        globalAlpha: Double,
        lineWidth: Double,
        out: PaintShader = PaintShader()
    ): PaintShader? = when (paint) {
        is NonePaint -> {
            null
        }
        is ColorPaint -> {
            val color = paint
            color.writeFloat(colorF)
            out.setTo(colorUniforms.also { uniforms ->
                uniforms[u_Color] = colorF
                uniforms[u_GlobalAlpha] = globalAlpha.toFloat()
                uniforms[u_LineWidth] = lineWidth.toFloat()
            }, PROGRAM_COLOR)
        }
        is BitmapPaint -> {
            val mat = Matrix().apply {
                identity()
                preconcat(paint.transform)
                preconcat(stateTransform)
                preconcat(localMatrix)
                invert()
                scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
            }

            //val mat = (paint.transform * stateTransform)
            //mat.scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
            //println("mat=$mat")
            out.setTo(bitmapUniforms.also { uniforms ->
                uniforms[DefaultShaders.u_Tex] = AG.TextureUnit(ctx.getTex(paint.bitmap).base)
                uniforms[u_Transform] = mat.toMatrix3D() // @TODO: Why is this transposed???
                uniforms[u_GlobalAlpha] = globalAlpha.toFloat()
                uniforms[u_LineWidth] = lineWidth.toFloat()
            }, PROGRAM_BITMAP)
        }
        is GradientPaint -> {
            gradientBitmap.lock {
                paint.fillColors(gradientBitmap.dataPremult)
            }

            val npaint = paint.copy(transform = Matrix().apply {
                identity()
                preconcat(paint.transform)
                preconcat(stateTransform)
                preconcat(localMatrix)
            })
            //val mat = stateTransform * paint.gradientMatrix
            val mat = when (paint.kind) {
                GradientKind.LINEAR -> npaint.gradientMatrix
                else -> npaint.transform.inverted()
            }
            out.setTo(
                gradientUniforms.also { uniforms ->
                    uniforms[DefaultShaders.u_Tex] = AG.TextureUnit(ctx.getTex(gradientBitmap).base)
                    uniforms[u_Transform] = mat.toMatrix3D()
                    uniforms[u_Gradientp0] = floatArrayOf(paint.x0.toFloat(), paint.y0.toFloat(), paint.r0.toFloat())
                    uniforms[u_Gradientp1] = floatArrayOf(paint.x1.toFloat(), paint.y1.toFloat(), paint.r1.toFloat())
                    uniforms[u_GlobalAlpha] = globalAlpha.toFloat()
                    uniforms[u_LineWidth] = lineWidth.toFloat()
                }, when (paint.kind) {
                    GradientKind.RADIAL -> PROGRAM_RADIAL_GRADIENT
                    GradientKind.SWEEP -> PROGRAM_SWEEP_GRADIENT
                    else -> PROGRAM_LINEAR_GRADIENT
                }
            )
        }
        else -> {
            TODO("paint=$paint")
        }
    }

    data class PaintShader(
        var uniforms: AG.UniformValues = AG.UniformValues(),
        var program: Program = DefaultShaders.PROGRAM_DEFAULT
    ) {
        fun setTo(uniforms: AG.UniformValues, program: Program): PaintShader {
            this.uniforms = uniforms
            this.program = program
            return this
        }
    }

    companion object {
        val u_LineWidth = Uniform("u_LineWidth", VarType.Float1)
        val u_Color = Uniform("u_Color", VarType.Float4)
        val u_GlobalAlpha = Uniform("u_GlobalAlpha", VarType.Float1)
        val u_Transform = Uniform("u_Transform", VarType.Mat4)
        val u_Gradientp0 = Uniform("u_Gradientp0", VarType.Float3)
        val u_Gradientp1 = Uniform("u_Gradientp1", VarType.Float3)
        val a_Dist: Attribute = Attribute("a_Dist", VarType.Float1, normalized = false, precision = Precision.MEDIUM)
        val v_Dist: Varying = Varying("v_Dist", VarType.Float1, precision = Precision.MEDIUM)
        val LAYOUT = VertexLayout(DefaultShaders.a_Pos)
        val LAYOUT_TEX = VertexLayout(DefaultShaders.a_Tex)
        val LAYOUT_DIST = VertexLayout(a_Dist)
        val LAYOUT_FILL = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)

        val LW = (u_LineWidth)
        val LW1 = Program.ExpressionBuilder { (LW - 1f.lit) }

        val VERTEX_FILL = VertexShaderDefault {
            SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
            //SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
            SET(v_Tex, DefaultShaders.a_Tex)
            SET(v_Dist, a_Dist)
        }
        val PROGRAM_STENCIL = Program(
            vertex = VertexShaderDefault { SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit)) },
            fragment = FragmentShaderDefault { SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)) },
        )

        fun Program.Builder.PREFIX() {
            IF(abs(v_Dist) ge LW) {
                DISCARD()
            }
        }

        fun Program.Builder.UPDATE_GLOBAL_ALPHA() {
            SET(out.a, out.a * u_GlobalAlpha)
            IF(abs(v_Dist) ge LW1) {
                //run {
                val aaAlpha = 1f.lit - (abs(v_Dist) - LW1)
                SET(out["a"], out["a"] * aaAlpha)
                //SET(out["a"], out["a"] * clamp(aaAlpha, 0f.lit, 1f.lit))
            }
        }

        val Operand.pow2: Operand get() = Program.ExpressionBuilder { pow(this@pow2, 2f.lit) }

        val PROGRAM_COLOR = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                PREFIX()
                SET(out, u_Color)
                UPDATE_GLOBAL_ALPHA()
            },
        )
        val PROGRAM_BITMAP = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                PREFIX()
                // @TODO: we should convert 0..1 to texture slice coordinates
                SET(out, texture2D(u_Tex, fract(vec2((u_Transform * vec4(v_Tex, 0f.lit, 1f.lit))["xy"]))))
                UPDATE_GLOBAL_ALPHA()
                //SET(out, vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit))
            },
        )
        val PROGRAM_LINEAR_GRADIENT = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                PREFIX()
                SET(out, texture2D(u_Tex, (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"]))
                UPDATE_GLOBAL_ALPHA()
            },
        )
        val PROGRAM_RADIAL_GRADIENT = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                PREFIX()
                val rpoint = createTemp(VarType.Float2)
                SET(rpoint["xy"], (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"])
                val x = rpoint.x
                val y = rpoint.y
                val x0 = u_Gradientp0.x
                val y0 = u_Gradientp0.y
                val r0 = u_Gradientp0.z
                val x1 = u_Gradientp1.x
                val y1 = u_Gradientp1.y
                val r1 = u_Gradientp1.z
                val ratio = t_Temp0.x
                val r0r1_2 = t_Temp0.y
                val r0pow2 = t_Temp0.z
                val r1pow2 = t_Temp0.w
                val y0_y1 = t_Temp1.x
                val x0_x1 = t_Temp1.y
                val r0_r1 = t_Temp1.z
                val radial_scale = t_Temp1.w

                SET(r0r1_2, 2f.lit * r0 * r1)
                SET(r0pow2, r0.pow2)
                SET(r1pow2, r1.pow2)
                SET(x0_x1, x0 - x1)
                SET(y0_y1, y0 - y1)
                SET(r0_r1, r0 - r1)
                SET(radial_scale, 1f.lit / ((r0 - r1).pow2 - (x0 - x1).pow2 - (y0 - y1).pow2))

                SET(
                    ratio,
                    1f.lit - (-r1 * r0_r1 + x0_x1 * (x1 - x) + y0_y1 * (y1 - y) - sqrt(r1pow2 * ((x0 - x).pow2 + (y0 - y).pow2) - r0r1_2 * ((x0 - x) * (x1 - x) + (y0 - y) * (y1 - y)) + r0pow2 * ((x1 - x).pow2 + (y1 - y).pow2) - (x1 * y0 - x * y0 - x0 * y1 + x * y1 + x0 * y - x1 * y).pow2)) * radial_scale
                )
                SET(out, texture2D(u_Tex, vec2(ratio, 0f.lit)))
                UPDATE_GLOBAL_ALPHA()
            },
        )
        val PROGRAM_SWEEP_GRADIENT = Program(
            vertex = VERTEX_FILL,
            fragment = FragmentShaderDefault {
                PREFIX()
                val rpoint = createTemp(VarType.Float2)
                SET(rpoint["xy"], (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"])
                val x = rpoint.x
                val y = rpoint.y
                val ratio = t_Temp0.x
                val angle = t_Temp0.y
                val x0 = u_Gradientp0.x
                val y0 = u_Gradientp0.y
                val PI2 = (PI * 2).toFloat().lit

                SET(angle, atan(y - y0, x - x0))
                IF(angle lt 0f.lit) { SET(angle, angle + PI2) }
                SET(ratio, angle / PI2)
                SET(out, texture2D(u_Tex, fract(vec2(ratio, 0f.lit))))
                UPDATE_GLOBAL_ALPHA()
            },
        )
    }
}
