package samples

import com.soywiz.korag.AG
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.debug.debugVertexView
import com.soywiz.korge.view.text
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.vector.format.pathSvg
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bezier.StrokePointsMode
import com.soywiz.korma.geom.bezier.toStrokePointsList
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.shape.getPoints2List
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.arc
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.geom.vector.toCurves

class MainGpuVectorRendering3 : Scene() {
    override suspend fun SContainer.sceneMain() {
        fun Container.debugPath(desc: String, pos: IPoint, strokeInfo: StrokeInfo, path: VectorPath) {
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

            //debugVertexView(pointsList.map { it.vector }, type = AG.DrawType.POINTS)
            text(desc, alignment = TextAlignment.BASELINE_LEFT).xy(pos - Point(0, 8))

            debugVertexView(path.getPoints2List(), color = Colors.YELLOWGREEN, type = AG.DrawType.LINE_STRIP).xy(pos).apply {
                keys {
                    down(Key.N8) { visible = !visible }
                }
            }

            debugVertexView(pointsList.map { it.vector }, type = AG.DrawType.LINE_STRIP).xy(pos).apply {
                keys {
                    down(Key.N9) { visible = !visible }
                }
            }
        }

        //val strokeInfo = StrokeInfo(thickness = 10.0, join = LineJoin.MITER)
        for ((index, strokeInfo) in listOf(
            StrokeInfo(thickness = 10.0, join = LineJoin.BEVEL),
            StrokeInfo(thickness = 10.0, join = LineJoin.MITER),
            StrokeInfo(thickness = 10.0, join = LineJoin.ROUND),
        ).withIndex()) {
            val sx = index * 430 + 15

            fun getPos(x: Int, y: Int): IPoint {
                return Point(sx + x * 120, 50 + y * 130)
            }

            text("${strokeInfo.join}", color = Colors.YELLOWGREEN).xy(sx, 10)
            debugPath("Lines CW", getPos(0, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(100, 100)
            })

            debugPath("Lines CCW", getPos(1, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(0, 0)
                lineTo(0, 100)
                lineTo(100, 100)
            })

            debugPath("Lines2 CCW", getPos(2, 0), strokeInfo, buildVectorPath {
                //rect(10, 10, 100, 100)
                moveTo(0, 0)
                lineTo(0, 100)
                lineTo(100, 100)
                lineTo(100, 0)
            })
            debugPath("Lines2 CW", getPos(2, 1), strokeInfo, buildVectorPath {
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(100, 100)
                lineTo(0, 100)
            })

            debugPath("Rect closed", getPos(0, 1), strokeInfo, buildVectorPath {
                rect(Rectangle.fromBounds(0, 0, 100, 100))
            })

            debugPath("Rect not closed", getPos(1, 1), strokeInfo, buildVectorPath {
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(100, 100)
                lineTo(0, 100)
                lineTo(0, 0)
            })

            debugPath("Pointed CW", getPos(0, 2), strokeInfo, buildVectorPath {
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(0, 100)
            })

            debugPath("Pointed CW", getPos(1, 2), strokeInfo, buildVectorPath {
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(0, 30)
            })

            debugPath("Pointed CCW", getPos(0, 3), strokeInfo, buildVectorPath {
                moveTo(100, 0)
                lineTo(0, 0)
                lineTo(100, 100)
            })

            debugPath("Pointed CCW", getPos(1, 3), strokeInfo, buildVectorPath {
                moveTo(100, 0)
                lineTo(0, 0)
                lineTo(100, 30)
            })

            debugPath("Circle", getPos(0, 4), strokeInfo, buildVectorPath {
                circle(50.0, 50.0, 50.0)
            })

            debugPath("Arc", getPos(1, 4), strokeInfo, buildVectorPath {
                arc(50.0, 50.0, 50.0, (-64).degrees, (+180).degrees)
            })

            debugPath("Shape", getPos(2, 4), strokeInfo, buildVectorPath {
                pathSvg(
                    "m262.15-119.2s2.05-8-2.35-3.6c0,0-6.4,5.2-13.2,5.2,0,0-13.2,2-17.2,14,0,0-3.6,24.4,3.6,29.6,0,0,4.4,6.8,10.8,0.8s20.35-33.6,18.35-46z",
                    Matrix().setTransform(x = -200.0, y = 150.0).scale(1.2)
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
