package samples

import com.soywiz.kds.fastArrayListOf
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Key
import com.soywiz.korge.input.draggable
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.moveWithCollisions
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors

class MainCircles : Scene() {
    override suspend fun SContainer.sceneMain() {
        // @TODO: USe BVH2D to limit collision view checkings
        lateinit var collisionViews: List<View>
        val rect1 = circle(50.0, fill = Colors.RED).xy(300, 300).centered
        val rect1b = circle(50.0, fill = Colors.RED).xy(520, 300).centered
        val rect2 = circle(50.0, fill = Colors.GREEN).xy(120, 0).draggable(autoMove = false) {
            //it.view.xy(it.viewPrevXY)
            it.view.moveWithCollisions(collisionViews, it.viewDeltaXY)
        }
        collisionViews = fastArrayListOf<View>(rect1, rect1b, rect2)
        println(rect1.hitShape2d)
        println(rect2.hitShape2d)
        addUpdater { dt ->
            val dx = input.keys.getDeltaAxis(Key.LEFT, Key.RIGHT)
            val dy = input.keys.getDeltaAxis(Key.UP, Key.DOWN)
            //if (dx != 0.0 || dy != 0.0) {
            val speed = (dt / 16.milliseconds) * 5.0
            rect2.moveWithCollisions(collisionViews, dx * speed, dy * speed)
            //}
            //rect2.alpha = if (rect1.collidesWith(rect2, kind = CollisionKind.SHAPE)) 1.0 else 0.3
        }
    }
}
