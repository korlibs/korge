package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.image.color.*

class MainColorTransformFilter : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val rect = solidRect(100, 100, )
        val rect = solidRect(100, 100, Colors.DARKGRAY)
        //rect.colorAdd = ColorAdd(+100, 0, 0, 0)
        rect.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+127, 0, +127, +255)))
    }
}
