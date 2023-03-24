package samples

import korlibs.event.Key
import korlibs.korge.input.keys
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors

class MainZIndex : Scene() {
    override suspend fun SContainer.sceneMain() {
        val rect1 = solidRect(100, 100, Colors.RED).also { it.y = 50.0 }.also { it.zIndex = it.y }
        val rect2 = solidRect(100, 100, Colors.BLUE).also { it.y = 100.0 }.also { it.zIndex = it.y }
        rect1.keys {
            down(Key.RETURN) { rect1.zIndex = -rect1.zIndex }
            downFrame(Key.UP) { rect1.y--; rect1.zIndex = rect1.y }
            downFrame(Key.DOWN) { rect1.y++; rect1.zIndex = rect1.y }
        }
    }
}