package samples

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.util.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.ds.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.random.*
import korlibs.number.*
import korlibs.time.*
import kotlin.random.*

private var SolidRect.movingDirection by extraProperty { -1 }

class MainBVH : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bvh = BVH2D<View>()
        val rand = Random(0)
        val rects = arrayListOf<SolidRect>()
        for (n in 0 until 2_000) {
            val x = rand[0.0, widthD]
            val y = rand[0.0, heightD]
            val width = rand[1.0, 50.0]
            val height = rand[1.0, 50.0]
            val view = solidRect(width, height, rand[Colors.RED, Colors.BLUE]).xy(x, y)
            view.movingDirection = if (rand.nextBoolean()) -1 else +1
            rects += view
            bvh.insertOrUpdate(view.getBounds(this), view)
        }
        addUpdater {
            for (n in rects.size - 100 until rects.size) {
                val view = rects[n]
                if (view.x < 0) {
                    view.movingDirection = +1
                }
                if (view.xD > stage!!.widthD) {
                    view.movingDirection = -1
                }
                view.xD += view.movingDirection
                bvh.insertOrUpdate(view.getBounds(this), view)
            }
        }
        val center = Point(widthD / 2, heightD / 2)
        var dir = Point(-1, -1)
        var ray = Ray(center, dir)
        val statusText = text("", font = DefaultTtfFontAsBitmap)
        var selectedRectangle = Rectangle(Point(100, 100) - Point(50, 50), Size(100, 100))
        val rayLine = line(center, center + (dir * 1000), Colors.WHITE)
        val selectedRect = outline(buildVectorPath(VectorPath()) {
            rect(selectedRectangle)
        })
        //outline(buildPath { star(5, 50.0, 100.0, x = 100.0, y = 100.0) })
        //debugLine(center, center + (dir * 1000), Colors.WHITE)
        fun updateRay() {
            var allObjectsSize = 0
            var rayObjectsSize = 0
            var rectangleObjectsSize = 0
            val allObjects = bvh.search(Rectangle(0.0, 0.0, widthD, heightD))
            val time = measureTime {
                val rayObjects = bvh.intersect(ray)
                val rectangleObjects = bvh.search(selectedRectangle)
                for (result in allObjects) result.value?.alphaF = 0.2f
                for (result in rectangleObjects) result.value?.alphaF = 0.8f
                for (result in rayObjects) result.obj.value?.alphaF = 1.0f
                allObjectsSize = allObjects.size
                rayObjectsSize = rayObjects.size
                rectangleObjectsSize = rectangleObjects.size
            }
            statusText.text = "All objects: ${allObjectsSize}, raycast = ${rayObjectsSize}, rect = ${rectangleObjectsSize}, time = ${time.milliseconds.niceStr(4)}ms"
        }
        updateRay()

        addUpdater {
            //println("moved")
            val mousePos = localMousePos(views)
            val angle = Point.angleFull(center, mousePos)
            //println("center=$center, mousePos=$mousePos, angle = $angle")
            dir = Vector2(angle.cosineD, angle.sineD)
            ray = Ray(center, dir)
            rayLine.setPoints(center, center + (dir * 1000))

            updateRay()
        }

        mouse {
            onDown {
                selectedRectangle = Rectangle(stage!!.mousePos - Point(50, 50), Size(100, 100))
                selectedRect.vectorPath = buildVectorPath(VectorPath()) {
                    rect(selectedRectangle)
                }
            }
            onMouseDrag {
                selectedRectangle = Rectangle(stage.mousePos - Point(50, 50), Size(100, 100))
                selectedRect.vectorPath = buildVectorPath(VectorPath()) {
                    rect(selectedRectangle)
                }
            }
        }
    }
}
