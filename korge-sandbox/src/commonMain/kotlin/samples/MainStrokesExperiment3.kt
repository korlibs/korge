package samples

import com.soywiz.kds.doubleArrayListOf
import com.soywiz.kds.forEachRatio01
import com.soywiz.kds.getCyclic
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.seconds
import com.soywiz.korag.AG
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delayFrame
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.container
import com.soywiz.korge.view.debug.DebugVertexView
import com.soywiz.korge.view.debug.debugVertexView
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.aroundPath
import com.soywiz.korim.text.text
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.StrokePointsMode
import com.soywiz.korma.geom.bezier.toDashes
import com.soywiz.korma.geom.bezier.toNonCurveSimplePointList
import com.soywiz.korma.geom.bezier.toStrokePointsList
import com.soywiz.korma.geom.fastForEach
import com.soywiz.korma.geom.fastForEachWithIndex
import com.soywiz.korma.geom.firstPoint
import com.soywiz.korma.geom.lastPoint
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.shape.toPolygon
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.line
import com.soywiz.korma.geom.vector.path
import com.soywiz.korma.geom.vector.quadTo
import com.soywiz.korma.geom.vector.star
import com.soywiz.korma.geom.vector.toCurves
import com.soywiz.korma.interpolation.Easing

class MainStrokesExperiment3 : Scene() {
    override suspend fun Container.sceneMain() {
        graphics {
            val path = buildVectorPath { circle(200, 200, 100) }
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
                points?.fastForEach { x, y ->
                    circle(x, y, 5.0)
                }
            }
            fill(Colors.WHITE) {
                var n = 0
                points?.fastForEach { x, y ->
                    text("${n++}", DefaultTtfFont, x = x + 2.0, y = y - 5.0)
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
    override suspend fun Container.sceneMain() {
        val path = buildVectorPath {}
        val curves = path.getCurves()
        val points = curves.toStrokePointsList(10.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
        //addChild(DebugVertexView(points.vector, type = AG.DrawType.LINE_STRIP).also { it.color = Colors.WHITE })
        val dbv = debugVertexView(points.map { it.vector }, type = AG.DrawType.TRIANGLE_STRIP) { color = Colors.WHITE }
        val dbv3 = debugVertexView(type = AG.DrawType.LINE_STRIP) { color = Colors.BLUE.withAd(0.1) }
        val dbv2 = debugVertexView(type = AG.DrawType.POINTS) { color = Colors.RED }
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
            up(Key.UP) { pathScale *= 1.1 }
            up(Key.DOWN) { pathScale *= 0.9 }
            down(Key.LEFT) { strokeWidth *= 0.9 }
            down(Key.RIGHT) { strokeWidth *= 1.1 }
        }

        var startX = 100.0
        var startY = 300.0

        var endX = 200.0
        var endY = 300.0

        launchImmediately {
            while (true) {
                if (alternate) {
                    startX = this@MainStrokesExperiment2.stage.mouseX
                    startY = this@MainStrokesExperiment2.stage.mouseY
                } else {
                    endX = this@MainStrokesExperiment2.stage.mouseX
                    endY = this@MainStrokesExperiment2.stage.mouseY
                }

                val path = buildVectorPath {
                    moveTo(startX, startY)
                    quadTo(100, 600, 300, 400)
                    when {
                        quad -> quadTo(endX - 50, endY - 50, endX, endY)
                        else -> lineTo(endX, endY)
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
                        pointsInfo.debugPoints.fastForEachWithIndex { index, x, y ->
                            val color = debugPointColors.getCyclic(index)
                            fill(color.withAd(0.5)) {
                                this.circle(x, y, 3.0)
                            }
                        }
                    }

                    PointArrayList().also {
                        for (c in curves.beziers) {
                            val bc = c as Bezier
                            it.add(bc.points.firstPoint())
                            it.add(bc.points.lastPoint())
                        }
                    }.fastForEach { x, y ->
                        fill(Colors.RED.withAd(0.5)) {
                            this.circle(x, y, 2.0)
                        }
                    }

                }
                dbv2.pointsList = listOf(PointArrayList().also {
                    for (c in curves.beziers) {
                        val bc = c as Bezier
                        it.add(bc.points.firstPoint())
                        it.add(bc.points.lastPoint())
                    }
                })
                //delay(0.3.seconds)
                delayFrame()
            }
        }
    }
}

class MainStrokesExperiment : Scene() {
    override suspend fun Container.sceneMain() {
        class PathData(
            val path: VectorPath,
            val curves: Curves,
            val points: IPointArrayList
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

        val circle = circle(16.0, Colors.PURPLE).centered
        launchImmediately {
            while (true) {
                circle.tween(circle::pos.get(path, includeLastPoint = true, reversed = false), time = 5.seconds, easing = Easing.LINEAR)
                circle.tween(circle::pos.get(path, includeLastPoint = true, reversed = true), time = 5.seconds, easing = Easing.LINEAR)
            }
        }

        if (true) {
            //if (false) {
            graphics {
                //stroke(Colors.RED, StrokeInfo(thickness = 3.0)) {
                //    forEachRatio01(200) { ratio ->
                //        val p = curves.calc(ratio)
                //        if (ratio == 0.0) moveTo(p) else lineTo(p)
                //    }
                //}
                stroke(Colors.GREEN, StrokeInfo(thickness = 2.0)) {
                    forEachRatio01(200) { ratio ->
                        val t = curves.ratioFromLength(ratio * curves.length)
                        //println("t=$t")
                        val p = curves.calc(t)
                        val n = curves.normal(t)
                        line(p, p + n * 10)
                    }
                }
                fill(Colors.RED) {
                    this.text("Hello, this is a test. Oh nice! Text following paths! How cool is that? Really cool? or not at all?\nCOOL, COOL, COOL, let's rock this path a bit more because it is cool, yeah!", DefaultTtfFont, textSize = 32.0, x = 0.0, y = 0.0, renderer = DefaultStringTextRenderer.aroundPath(path))
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
            AG.DrawType.TRIANGLE_STRIP,
            GpuShapeViewPrograms.paintToShaderInfo(Matrix(), Colors.RED, 1.0, 6.0)
        )
        ctx.flush()
        gpuShapeViewCommands.render(ctx, globalMatrix, Matrix(), false, renderColorMul)
        ctx.flush()
    }
}
*/
