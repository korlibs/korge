package samples

import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.image.vector.format.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.debug.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

class MainGpuVectorRendering3 : Scene() {
    override suspend fun SContainer.sceneMain() {
        fun Container.debugPath(desc: String, pos: Point, strokeInfo: StrokeInfo, path: VectorPath) {
            val pointsList = path.toCurves().toStrokePointsList(strokeInfo, generateDebug = true, mode = StrokePointsMode.NON_SCALABLE_POS)

            gpuShapeView({
                stroke(Colors.RED, strokeInfo) {
                    path(path)
                }
            }) {
                //rotation = 5.degrees
                keys {
                    down(Key.N0) { antialiased = !antialiased }
                }
            }.xy(pos)

            //debugVertexView(pointsList.map { it.vector }, type = AGDrawType.POINTS)
            text(desc, alignment = TextAlignment.BASELINE_LEFT).xy(pos - Point.ZERO)

            debugVertexView(path.getPoints2List(), color = Colors.YELLOWGREEN, type = AGDrawType.LINE_STRIP).xy(pos).apply {
                keys {
                    down(Key.N8) { visible = !visible }
                }
            }

            debugVertexView(pointsList.map { it.vector }, type = AGDrawType.LINE_STRIP).xy(pos).apply {
                keys {
                    down(Key.N9) { visible = !visible }
                }
            }
        }

        //val strokeInfo = StrokeInfo(thickness = 10.0, join = LineJoin.MITER)
        for ((index, strokeInfo) in listOf(
            StrokeInfo(thickness = 10f, join = LineJoin.BEVEL),
            StrokeInfo(thickness = 10f, join = LineJoin.MITER),
            StrokeInfo(thickness = 10f, join = LineJoin.ROUND),
        ).withIndex()) {
            val sx = index * 430 + 15

            fun getPos(x: Int, y: Int): Point = Point(sx + x * 120, 50 + y * 130)

            text("${strokeInfo.join}", color = Colors.YELLOWGREEN).xy(sx, 10)
            debugPath("Lines CW", getPos(0, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(100, 100))
            })

            debugPath("Lines CCW", getPos(1, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(Point(0, 0))
                lineTo(Point(0, 100))
                lineTo(Point(100, 100))
            })

            debugPath("Lines2 CCW", getPos(2, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(Point(0, 0))
                lineTo(Point(0, 100))
                lineTo(Point(100, 100))
                lineTo(Point(100, 0))
            })
            debugPath("Lines2 CW", getPos(2, 1), strokeInfo, buildVectorPath {
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(100, 100))
                lineTo(Point(0, 100))
            })

            debugPath("Rect closed", getPos(0, 1), strokeInfo, buildVectorPath {
                rect(Rectangle.fromBounds(0, 0, 100, 100))
            })

            debugPath("Rect not closed", getPos(1, 1), strokeInfo, buildVectorPath {
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(100, 100))
                lineTo(Point(0, 100))
                lineTo(Point(0, 0))
            })

            debugPath("Pointed CW", getPos(0, 2), strokeInfo, buildVectorPath {
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(0, 100))
            })

            debugPath("Pointed CW", getPos(1, 2), strokeInfo, buildVectorPath {
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(0, 30))
            })

            debugPath("Pointed CCW", getPos(0, 3), strokeInfo, buildVectorPath {
                moveTo(Point(100, 0))
                lineTo(Point(0, 0))
                lineTo(Point(100, 100))
            })

            debugPath("Pointed CCW", getPos(1, 3), strokeInfo, buildVectorPath {
                moveTo(Point(100, 0))
                lineTo(Point(0, 0))
                lineTo(Point(100, 30))
            })

            debugPath("Circle", getPos(0, 4), strokeInfo, buildVectorPath {
                circle(Point(50.0, 50.0), 50.0f)
            })

            debugPath("Arc", getPos(1, 4), strokeInfo, buildVectorPath {
                arc(Point(50.0, 50.0), 50f, (-64).degrees, (+180).degrees)
            })

            debugPath("Shape", getPos(2, 4), strokeInfo, buildVectorPath {
                pathSvg(
                    "m262.15-119.2s2.05-8-2.35-3.6c0,0-6.4,5.2-13.2,5.2,0,0-13.2,2-17.2,14,0,0-3.6,24.4,3.6,29.6,0,0,4.4,6.8,10.8,0.8s20.35-33.6,18.35-46z",
                    Matrix.fromTransform(x = -200f, y = 150f).scaled(1.2f)
                )
            })
        }

        /*
         gpuShapeView({
             keep {
                 translate(100, 100)
                 fill(Colors.WHITE) {
                     rect(-10, -10, 120, 120)
                     rectHole(40, 40, 80, 80)
                 }
             }
         }) {
             rotation = 5.degrees
             //debugDrawOnlyAntialiasedBorder = true
             keys {
                 down(Key.N0) { antialiased = !antialiased }
                 down(Key.N1) { debugDrawOnlyAntialiasedBorder = !debugDrawOnlyAntialiasedBorder }
             }
         }
         */
    }
}
