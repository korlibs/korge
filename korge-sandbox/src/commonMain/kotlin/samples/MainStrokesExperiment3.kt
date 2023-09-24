package samples

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.debug.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainStrokesExperiment3 : Scene() {
    override suspend fun SContainer.sceneMain() {
        cpuGraphics {
            val path = buildVectorPath { circle(Point(200, 200), 100f) }
            val points = path.toCurves().toNonCurveSimplePointList()
            val path2 = points?.toPolygon()

            stroke(Colors.BLUE, StrokeInfo(thickness = 3.0)) {
                path(path)
            }

            stroke(Colors.RED, StrokeInfo(thickness = 2.0)) {
                path(path2)
            }

            fill(Colors.PURPLE) {
                var n = 0
                points?.fastForEach { (x, y) ->
                    circle(Point(x, y), 5f)
                }
            }
            fill(Colors.WHITE) {
                var n = 0
                points?.fastForEach { (x, y) ->
                    text("${n++}", DefaultTtfFont, pos = Point(x + 2.0, y - 5.0))
                }
            }
        }
        //return

        //graphics {
        //    val curve = BezierCurve(0.0, 0.0, 33.33333333333333, 66.66666666666666, 66.66666666666667, 100.0, 100.0, 100.0)
        //    stroke(Colors.RED, StrokeInfo(thickness = 3.0)) {
        //        //curve(BezierCurve(0, 0, 50, 100, 100, 100))
        //        curve(curve)
        //    }
        //    stroke(Colors.BLUE, StrokeInfo(thickness = 2.0)) {
        //        curve(curve.toQuad().translate(0.0, 0.0))
        //    }
        //    //println(BezierCurve(0, 0, 50, 100, 100, 100).toCubic())
        //}
        //return
    }
}

class MainStrokesExperiment2 : Scene() {
    override suspend fun SContainer.sceneMain() {
        val path = buildVectorPath {}
        val curves = path.getCurves()
        val points = curves.toStrokePointsList(10.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
        //addChild(DebugVertexView(points.vector, type = AGDrawType.LINE_STRIP).also { it.color = Colors.WHITE })
        val dbv = debugVertexView(points.map { it.vector }, type = AGDrawType.TRIANGLE_STRIP) { color = Colors.WHITE }
        val dbv3 = debugVertexView(type = AGDrawType.LINE_STRIP) { color = Colors.BLUE.withAd(0.1) }
        val dbv2 = debugVertexView(type = AGDrawType.POINTS) { color = Colors.RED }
        val dbv4 = gpuShapeView {  }
        //val dbv4 = graphics {  }

        var alternate = false
        var pathScale = 1.0
        var strokeWidth = 20.0
        var debug = true
        var closed = false
        var quad = false
        var dashes = true
        keys {
            up(Key.SPACE) { alternate = !alternate }
            up(Key.N0) { debug = !debug }
            up(Key.N1) { closed = !closed }
            up(Key.N2) { quad = !quad }
            up(Key.N3) { dashes = !dashes }
            up(Key.UP) { pathScale *= 1.1f }
            up(Key.DOWN) { pathScale *= 0.9f }
            down(Key.LEFT) { strokeWidth *= 0.9f }
            down(Key.RIGHT) { strokeWidth *= 1.1f }
        }

        var startX = 100.0
        var startY = 300.0

        var endX = 200.0
        var endY = 300.0

        launchImmediately {
            while (true) {
                if (alternate) {
                    startX = this@MainStrokesExperiment2.stage.mousePos.x
                    startY = this@MainStrokesExperiment2.stage.mousePos.y
                } else {
                    endX = this@MainStrokesExperiment2.stage.mousePos.x
                    endY = this@MainStrokesExperiment2.stage.mousePos.y
                }

                val path = buildVectorPath {
                    moveTo(Point(startX, startY))
                    quadTo(Point(100, 600), Point(300, 400))
                    when {
                        quad -> quadTo(Point(endX - 50, endY - 50), Point(endX, endY))
                        else -> lineTo(Point(endX, endY))
                    }
                    if (closed) this.close()
                }
                //val path = buildVectorPath {
                //    //this.circle(400, 300, 200)
                //    moveTo(100, 300)
                //    lineTo(300, 400)
                //    //lineTo(500, 300)
                //    lineTo(200, 300)
                //    //moveTo(100, 300)
                //    //quadTo(100, 500, 500, 500)
                //    //lineTo(500, 200)
                //    //lineTo(800, 200)
                //    //quadTo(600, 300, 800, 500)
                //}
                val curves = path.getCurves()
                //dbv.points = curves.toStrokePoints(5.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).vector
                //delay(0.3.seconds)
                //dbv.points = curves.toStrokePoints(10.0, endCap = LineCap.SQUARE, startCap = LineCap.SQUARE, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).vector
                //delay(0.3.seconds)
                //dbv.points = curves.toStrokePoints(5.0, endCap = LineCap.ROUND, startCap = LineCap.ROUND, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).vector
                //dbv.points = curves.toStrokePoints(5.0, endCap = LineCap.ROUND, startCap = LineCap.ROUND, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).also {
                val pointsInfoList = curves
                    .toStrokePointsList(strokeWidth, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH, lineDash = if (dashes) doubleArrayListOf(30.0, 10.0) else null, generateDebug = debug)
                dbv.pointsList = pointsInfoList.map { it.vector }
                dbv3.pointsList = pointsInfoList.map { it.vector }
                dbv4.updateShape {
                    pointsInfoList.fastForEach { pointsInfo ->
                        pointsInfo.debugSegments.fastForEach { line ->
                            stroke(Colors.GREEN, lineWidth = 1.5) {
                                //println("line=$line")
                                this.line(line.a, line.b)
                            }
                        }

                        val debugPointColors = listOf(Colors.RED, Colors.PURPLE, Colors.MAGENTA)
                        pointsInfo.debugPoints.fastForEachIndexed { index, (x, y) ->
                            val color = debugPointColors.getCyclic(index)
                            fill(color.withAd(0.5)) {
                                this.circle(Point(x, y), 3f)
                            }
                        }
                    }

                    PointArrayList().also {
                        for (c in curves.beziers) {
                            it.add(c.points.first)
                            it.add(c.points.last)
                        }
                    }.fastForEach { (x, y) ->
                        fill(Colors.RED.withAd(0.5)) {
                            this.circle(Point(x, y), 2f)
                        }
                    }

                }
                dbv2.pointsList = listOf(PointArrayList().also {
                    for (c in curves.beziers) {
                        val bc = c as Bezier
                        it.add(bc.points.first)
                        it.add(bc.points.last)
                    }
                })
                //delay(0.3.seconds)
                delayFrame()
            }
        }
    }
}

class MainStrokesExperiment : Scene() {
    override suspend fun SContainer.sceneMain() {
        class PathData(
            val path: VectorPath,
            val curves: Curves,
            val points: PointList
        )

        val path = buildVectorPath {
            //this.circle(400, 300, 200)
            this.star(6, 200.0, 300.0, x = 400.0, y = 300.0)
            //moveTo(100, 300)
            //quadTo(100, 500, 500, 500)
            //lineTo(500, 200)
            //lineTo(800, 200)
            //quadTo(600, 300, 800, 500)
        }
        //.applyTransform(Matrix().translate(-100, -200))
        val curves = path.getCurves()
        val points = curves.toStrokePointsList(10.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
        //Bezier(10.0, 10.0).inflections()
        //points.scale(2.0)

        println("path=$path")

        addChild(DebugVertexView(points.map { it.vector }).also { it.color = Colors.WHITE })

        fun generateDashes(offset: Double): Container = Container().apply {
            addChild(DebugVertexView(curves
                .toDashes(doubleArrayOf(180.0, 50.0), offset = offset)
                .toStrokePointsList(20.0)
                .map { it.vector }
            ).also { it.color = Colors.BLUEVIOLET })
        }

        class OffsetInfo {
            var offset = 0.0
        }

        val container = container {
        }
        val offsetInfo = OffsetInfo()
        addUpdater {
            container.removeChildren()
            container.addChild(generateDashes(offsetInfo.offset))
        }

        launchImmediately {
            while (true) {
                tween(offsetInfo::offset[200.0], time = 5.seconds)
                tween(offsetInfo::offset[0.0], time = 5.seconds)
            }
        }

        val circle = circle(16f, Colors.PURPLE).centered
        launchImmediately {
            while (true) {
                circle.tween(circle::pos.get(path, includeLastPoint = true, reversed = false), time = 5.seconds, easing = Easing.LINEAR)
                circle.tween(circle::pos.get(path, includeLastPoint = true, reversed = true), time = 5.seconds, easing = Easing.LINEAR)
            }
        }

        if (true) {
            //if (false) {
            cpuGraphics {
                stroke(Colors.GREEN, StrokeInfo(thickness = 2f)) {
                    Ratio.forEachRatio(200) { ratio ->
                        val t = curves.ratioFromLength(ratio.convertToRange(0.0, curves.length))
                        //println("t=$t")
                        val p = curves.calc(t)
                        val n = curves.normal(t)
                        line(p, p + n * 10)
                    }
                }
                fill(Colors.RED) {
                    this.text(
                        "Hello, this is a test. Oh nice! Text following paths! How cool is that? Really cool? or not at all?\nCOOL, COOL, COOL, let's rock this path a bit more because it is cool, yeah!",
                        DefaultTtfFont, textSize = 32.0, pos = Point.ZERO, renderer = DefaultStringTextRenderer.aroundPath(path)
                    )
                }
            }
        }
    }
}

/*
@Suppress("OPT_IN_USAGE")
class StrokeView(val points: IVectorArrayList) : View() {
    private val gpuShapeViewCommands = GpuShapeViewCommands()

    override fun renderInternal(ctx: RenderContext) {
        gpuShapeViewCommands.clear()
        gpuShapeViewCommands.verticesStart()
        println("--------- ${points.size}")
        for (n in 0 until points.size) {
            val x = points.get(n, 0).toFloat()
            val y = points.get(n, 1).toFloat()
            val u = points.get(n, 2).toFloat()
            val v = points.get(n, 3).toFloat()
            val len = points.get(n, 4).toFloat()
            val px = x + u * len
            val py = y + v * len
            //println("x=$x, y=$y, u=$u, v=$v, len=$len")
            println("px=$px, py=$py")
            gpuShapeViewCommands.addVertex(
                px, py, 0f, 0f,
                100f
            )
        }
        gpuShapeViewCommands.verticesEnd()
        gpuShapeViewCommands.draw(
            AGDrawType.TRIANGLE_STRIP,
            GpuShapeViewPrograms.paintToShaderInfo(Matrix(), Colors.RED, 1.0, 6.0)
        )
        ctx.flush()
        gpuShapeViewCommands.render(ctx, globalMatrix, Matrix(), false, renderColorMul)
        ctx.flush()
    }
}
*/
