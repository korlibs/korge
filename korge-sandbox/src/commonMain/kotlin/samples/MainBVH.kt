package samples

import com.soywiz.kds.extraProperty
import com.soywiz.klock.measureTime
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onMouseDrag
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.line
import com.soywiz.korge.view.outline
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Ray
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.ds.BVH2D
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.random.get
import kotlin.random.Random

class MainBVH : Scene() {
    var SolidRect.movingDirection by extraProperty { -1 }

    override suspend fun SContainer.sceneMain() {
        val bvh = BVH2D<View>()
        val rand = Random(0)
        val rects = arrayListOf<SolidRect>()
        for (n in 0 until 2_000) {
            val x = rand[0.0, width]
            val y = rand[0.0, height]
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
                if (view.x > stage!!.width) {
                    view.movingDirection = -1
                }
                view.x += view.movingDirection
                bvh.insertOrUpdate(view.getBounds(this), view)
            }
        }
        val center = Point(width / 2, height / 2)
        val dir = Point(-1, -1)
        val ray = Ray(center, dir)
        val statusText = text("", font = views.debugBmpFont)
        var selectedRectangle = Rectangle(Point(100, 100) - Point(50, 50), Size(100, 100))
        val rayLine = line(center, center + (dir * 1000), Colors.WHITE)
        val selectedRect = outline(buildPath { rect(selectedRectangle) })
        //outline(buildPath { star(5, 50.0, 100.0, x = 100.0, y = 100.0) })
        //debugLine(center, center + (dir * 1000), Colors.WHITE)
        fun updateRay() {
            var allObjectsSize = 0
            var rayObjectsSize = 0
            var rectangleObjectsSize = 0
            val allObjects = bvh.search(Rectangle(0.0, 0.0, width, height))
            val time = measureTime {
                val rayObjects = bvh.intersect(ray)
                val rectangleObjects = bvh.search(selectedRectangle)
                for (result in allObjects) result.value?.alpha = 0.2
                for (result in rectangleObjects) result.value?.alpha = 0.8
                for (result in rayObjects) result.obj.value?.alpha = 1.0
                allObjectsSize = allObjects.size
                rayObjectsSize = rayObjects.size
                rectangleObjectsSize = rectangleObjects.size
            }
            statusText.text = "All objects: ${allObjectsSize}, raycast = ${rayObjectsSize}, rect = ${rectangleObjectsSize}, time = $time"
        }
        updateRay()

        addUpdater {
            //println("moved")
            val mousePos = localMouseXY(views)
            val angle = Point.angleFull(center, mousePos)
            //println("center=$center, mousePos=$mousePos, angle = $angle")
            dir.setTo(angle.cosine, angle.sine)
            rayLine.setPoints(center, center + (dir * 1000))

            updateRay()
        }

        mouse {
            onDown {
                selectedRectangle = Rectangle(stage!!.mouseXY - Point(50, 50), Size(100, 100))
                selectedRect.vectorPath = buildPath { rect(selectedRectangle) }
            }
            onMouseDrag {
                selectedRectangle = Rectangle(stage.mouseXY - Point(50, 50), Size(100, 100))
                selectedRect.vectorPath = buildPath { rect(selectedRectangle) }
            }
        }
    }
}
